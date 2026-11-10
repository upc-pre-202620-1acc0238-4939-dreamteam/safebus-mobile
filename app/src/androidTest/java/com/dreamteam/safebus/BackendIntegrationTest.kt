package com.dreamteam.safebus

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import com.dreamteam.safebus.fleet.data.RemoteFleetRepository
import com.dreamteam.safebus.fleet.domain.FleetChange
import com.dreamteam.safebus.iam.data.*
import com.dreamteam.safebus.passenger.data.*
import com.dreamteam.safebus.passenger.domain.*
import com.dreamteam.safebus.safetycase.data.*
import com.dreamteam.safebus.shared.data.*
import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.trip.data.*
import com.dreamteam.safebus.trip.domain.Position
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.IOException
import java.time.Instant
import java.util.UUID
import kotlin.random.Random

class BackendIntegrationTest {
    internal class MemoryStore : SecureStore {
        val data = mutableMapOf<String, ByteArray>()
        @Synchronized override fun read(key: String) = data[key]?.copyOf()
        @Synchronized override fun write(key: String, value: ByteArray) { data[key] = value.copyOf() }
        @Synchronized override fun remove(key: String) { data.remove(key) }
    }
    internal class Client(endpoint: String) {
        val store = MemoryStore()
        @Volatile var offline = false
        private val engine = HttpEngine { if (offline) throw IOException("Test connection interrupted") else UrlConnectionEngine().execute(it) }
        val auth: RemoteAuthRepository
        val api = ApiClient(endpoint, true, { authToken() }, { invalidate(it) }, engine)
        init { auth = RemoteAuthRepository(HttpAuthService(api), store, endpoint) }
        private fun authToken() = auth.token()
        private fun invalidate(token: String) = auth.invalidate(token)
        val outbox = Outbox(store, UUID.randomUUID().toString(), PendingTransport { api.call("POST", it.path, it.body, it.contentType) { Unit } })
        val fleet = RemoteFleetRepository(api)
        val trip = RemoteTripRepository(HttpTripService(api, outbox), store, "test")
        val passenger = RemotePassengerRepository(HttpPassengerService(api, outbox), store, "test")
        val safety = RemoteSafetyRepository(api, outbox, store, "test")
        val events = SupervisorEvents(endpoint, { authToken() }, { invalidate(it) })
    }
    private fun <T> value(result: DomainResult<T>): T = when (result) {
        is DomainResult.Success -> result.value
        is DomainResult.Error -> throw AssertionError("Backend failure: ${result.failure.kind}/${result.failure.code}")
    }
    private fun photo(color: Int): Photo {
        val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
        val output = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output); bitmap.recycle()
        return Photo(output.toByteArray(), "image/png")
    }

    @Test fun completeBackendLifecycleWithPrivateEvidenceOfflineRetryAndRoleIsolation() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("Run against an isolated development backend with -e backend true", args.getString("backend") == "true")
        val endpoint = args.getString("backendUrl") ?: "http://10.0.2.2:8080"
        val suffix = UUID.randomUUID().toString().take(8)
        val supervisor = Client(endpoint)
        value(supervisor.auth.signIn("sup-001", "Safebus2024!"))
        val bus = value(supervisor.fleet.save(FleetChange.Bus("M-$suffix")))
        val driver = value(supervisor.fleet.save(FleetChange.Driver("Mobile integration driver", "driver-$suffix", "Integration2026!")))
        val route = value(supervisor.fleet.save(FleetChange.Route("Mobile integration route", "North", "South")))
        value(supervisor.fleet.save(FleetChange.Capacity(bus.id, 40, "TEST-$suffix")))
        val now = Instant.now()
        val assignment = value(supervisor.fleet.save(FleetChange.Assignment(driver.id, bus.id, route.id, now.minusSeconds(60), now.plusSeconds(3600))))
        val overlap = supervisor.fleet.save(FleetChange.Assignment(driver.id, bus.id, route.id, now, now.plusSeconds(600)))
        assertTrue(overlap is DomainResult.Error)
        assertEquals(FailureKind.CONFLICT, (overlap as DomainResult.Error).failure.kind)
        assertEquals(0, value(supervisor.outbox.count()))

        val conductor = Client(endpoint)
        assertTrue(conductor.auth.signIn("driver-$suffix", "wrong-password") is DomainResult.Error)
        assertNull(conductor.auth.session.value); assertTrue(conductor.store.data.isEmpty())
        value(conductor.auth.signIn("driver-$suffix", "Integration2026!"))
        assertEquals(assignment.id, value(conductor.trip.assignment()).id)
        assertTrue(conductor.trip.activate(assignment.id, "invalid-qr") is DomainResult.Error)
        assertNull(value(conductor.trip.restore()))
        val shift = value(conductor.trip.activate(assignment.id, requireNotNull(driver.credential)))
        assertEquals(shift, value(conductor.trip.restore()))
        assertEquals(FailureKind.FORBIDDEN, (conductor.fleet.overview() as DomainResult.Error).failure.kind)
        assertEquals(Delivery.ACKNOWLEDGED, value(conductor.trip.locate(shift, Position(-12.0464, -77.0428, 8.0, Instant.now()))))
        val monitored = value(supervisor.fleet.overview()).buses.single { it.id == bus.id }
        assertEquals(40, monitored.capacity); assertEquals("CURRENT", monitored.locationStatus); assertNull(monitored.count)

        val connected = CompletableDeferred<Unit>()
        val event = async { withTimeout(10_000) { supervisor.events.observe().onEach { if (it == LiveEvent.Connected) connected.complete(Unit) }.first { it == LiveEvent.Changed } } }
        withTimeout(10_000) { connected.await() }
        conductor.offline = true
        val panic = value(conductor.safety.panic(shift.id))
        assertEquals(Delivery.PENDING, panic.delivery); assertEquals(1, value(conductor.outbox.count()))
        conductor.offline = false
        assertEquals(0, value(conductor.outbox.flush()))
        assertEquals(LiveEvent.Changed, event.await())
        val critical = value(supervisor.safety.queue()).emergencies.single { it.id == panic.id }
        assertEquals("CRITICAL", critical.priority)
        value(supervisor.safety.attend(panic.id))
        assertEquals("IN_PROGRESS", value(conductor.safety.driverAlerts()).single { it.id == panic.id }.status)
        value(supervisor.safety.close(panic.id, "Driver safe", "Resolved by company"))
        assertEquals("CLOSED", value(conductor.safety.driverAlerts()).single { it.id == panic.id }.status)

        val face = photo(android.graphics.Color.GREEN)
        val evidence = photo(android.graphics.Color.RED)
        val passengers = List(3) { Client(endpoint) }
        val journeys = passengers.map { client ->
            val dni = Random.nextInt(10_000_000, 99_999_999).toString()
            val registration = Registration(dni, "Integration2026!", true, face)
            value(client.passenger.register(registration))
            assertTrue(client.passenger.register(registration) is DomainResult.Error)
            assertNull(client.auth.session.value); assertTrue(client.store.data.isEmpty())
            value(client.auth.signIn(dni, "Integration2026!"))
            val qr = BusQr.parse(BusQr(requireNotNull(bus.credential), bus.id).encoded())
            val journey = value(client.passenger.start(qr))
            assertEquals(journey.id, value(client.passenger.start(qr)).id)
            assertEquals(bus.id, journey.busId)
            assertEquals(-12.0464, value(client.passenger.location(bus.id)).latitude, 0.00001)
            journey
        }
        val requests = passengers.mapIndexed { index, client -> value(client.safety.request(journeys[index].id, "Private incident $index", evidence)) }
        val duplicate = passengers[0].safety.request(journeys[0].id, "Duplicate contribution", evidence)
        assertEquals(FailureKind.CONFLICT, (duplicate as DomainResult.Error).failure.kind)
        assertEquals(0, value(passengers[0].outbox.count()))
        assertEquals(1, value(passengers[0].safety.mine()).size)
        assertEquals(FailureKind.FORBIDDEN, (passengers[1].safety.evidence(requests[0].id, false) as DomainResult.Error).failure.kind)
        assertEquals(1, value(passengers[1].safety.mine()).size)
        assertArrayEquals(evidence.bytes, value(passengers[0].safety.evidence(requests[0].id, false)).bytes)
        val group = value(supervisor.safety.queue()).groups.single { it.busId == bus.id }
        assertEquals(3, group.contributors)
        assertArrayEquals(evidence.bytes, value(supervisor.safety.evidence(requests[0].id, true)).bytes)
        assertTrue(value(passengers[0].safety.history(bus.id)).isNotEmpty())
        value(supervisor.safety.approve(group.id))
        val high = value(supervisor.safety.queue()).emergencies.single { it.busId == bus.id }
        assertEquals("HIGH", high.priority)
        value(supervisor.safety.attend(high.id))
        value(supervisor.safety.close(high.id, "Case resolved", "Passengers safe"))
        assertEquals("CLOSED", value(passengers[0].safety.mine()).single().status)
        assertEquals("Passengers safe", value(passengers[0].safety.mine()).single().response)

        passengers.forEachIndexed { index, client -> value(client.safety.request(journeys[index].id, "Second incident $index", evidence)) }
        val declinedGroup = value(supervisor.safety.queue()).groups.single { it.busId == bus.id }
        value(supervisor.safety.decline(declinedGroup.id, "No active threat", "Reviewed by company"))
        assertTrue(value(passengers[0].safety.mine()).any { it.status == "NOT_APPROVED" && it.response == "Reviewed by company" })

        passengers[0].offline = true
        assertEquals(Delivery.PENDING, value(passengers[0].passenger.end(journeys[0], EndReason.MANUAL)))
        assertNull(value(passengers[0].passenger.restore()))
        passengers[0].offline = false
        assertEquals(0, value(passengers[0].outbox.flush()))
        assertEquals(2, value(passengers[0].safety.mine()).size)
        assertEquals(requests[0].id, value(passengers[0].safety.detail(requests[0].id)).id)
        assertEquals(FailureKind.FORBIDDEN, (passengers[1].safety.detail(requests[0].id) as DomainResult.Error).failure.kind)
        assertEquals(2, value(passengers[1].safety.mine()).size)
        val reserved = passengers[0].api.call("GET", "/api/v1/passenger-requests/mine/photo") { Unit }
        assertEquals(FailureKind.FORBIDDEN, (reserved as DomainResult.Error).failure.kind)
        assertEquals(2, value(passengers[0].safety.mine()).size)
        assertEquals(FailureKind.FORBIDDEN, (passengers[0].safety.history(bus.id) as DomainResult.Error).failure.kind)
        assertNull(value(passengers[0].passenger.restore()))
        value(conductor.trip.close(shift))
        assertNull(value(conductor.trip.restore()))
        assertNull(value(supervisor.fleet.overview()).buses.single { it.id == bus.id }.shiftId)
        passengers.forEach { value(it.auth.signOut()); assertNull(it.auth.session.value) }
        value(conductor.auth.signOut()); value(supervisor.auth.signOut())
    }
}
