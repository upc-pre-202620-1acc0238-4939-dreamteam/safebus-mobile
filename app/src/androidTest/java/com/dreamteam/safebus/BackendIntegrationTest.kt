package com.dreamteam.safebus

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import com.dreamteam.safebus.fleet.data.RemoteFleetRepository
import com.dreamteam.safebus.fleet.domain.FleetChange
import com.dreamteam.safebus.iam.data.*
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
        val outbox = Outbox(store, UUID.randomUUID().toString(), PendingTransport { api.call("POST", it.path, it.body, it.contentType) {} })
        val fleet = RemoteFleetRepository(api)
        val trip = RemoteTripRepository(HttpTripService(api, outbox), store, "test")
        val safety = RemoteSafetyRepository(api, outbox, store, "test")
        val events = SupervisorEvents(endpoint, { authToken() }, { invalidate(it) })
    }
    private fun <T> value(result: DomainResult<T>): T = when (result) {
        is DomainResult.Success -> result.value
        is DomainResult.Error -> throw AssertionError("Backend failure: ${result.failure.kind}/${result.failure.code}")
    }
    @Test fun supervisorModeratesExternalPassengerReportsWhileAndroidRejectsTheirLogin() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("backend") == "true")
        val endpoint = "http://10.0.2.2:8080"
        val supervisor = Client(endpoint)
        value(supervisor.auth.signIn("sup-001", "Safebus2024!"))
        val suffix = UUID.randomUUID().toString().take(8)
        val bus = value(supervisor.fleet.save(FleetChange.Bus("EXT-$suffix")))
        val driver = value(supervisor.fleet.save(FleetChange.Driver("External reports driver", "ext-$suffix", "Integration2026!")))
        val route = value(supervisor.fleet.save(FleetChange.Route("External reports route", "A", "B")))
        val assignment = value(supervisor.fleet.save(FleetChange.Assignment(driver.id, bus.id, route.id, Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600))))
        val conductor = Client(endpoint)
        value(conductor.auth.signIn("ext-$suffix", "Integration2026!"))
        val shift = value(conductor.trip.activate(assignment.id, requireNotNull(driver.credential)))
        val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.RED) }
        val output = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output); bitmap.recycle()
        val image = output.toByteArray()
        val requests = List(3) {
            var externalToken: String? = null
            val external = ApiClient(endpoint, true, { externalToken }, {}, UrlConnectionEngine())
            val dni = kotlin.random.Random.nextInt(10_000_000, 99_999_999).toString()
            val registration = multipart(mapOf("dni" to dni, "password" to "Integration2026!", "termsAccepted" to "true", "termsVersion" to "2026-10"), "facePhoto", image, "image/png")
            value(external.call("POST", "/api/v1/passengers", registration.bytes, registration.contentType, authenticated = false) {})
            externalToken = value(HttpAuthService(external).signIn(dni, "Integration2026!")).accessToken
            val android = Client(endpoint)
            assertEquals("ANDROID_ROLE_UNSUPPORTED", (android.auth.signIn(dni, "Integration2026!") as DomainResult.Error).failure.code)
            assertNull(android.auth.session.value)
            assertTrue(android.store.data.isEmpty())
            val journeyId = value(external.call("POST", "/api/v1/journeys", jsonBody("busQrCode" to bus.credential), "application/json") { it.json().getLong("journeyId") })
            val id = UUID.randomUUID().toString()
            val report = multipart(mapOf("id" to id, "journeyId" to journeyId.toString(), "message" to "Report from external passenger client", "activatedAt" to Instant.now().toString()), "photo", image, "image/png")
            value(external.call("POST", "/api/v1/passenger-requests", report.bytes, report.contentType) {})
            id
        }
        val group = value(supervisor.safety.queue()).groups.single { it.busId == bus.id }
        assertEquals(3, group.contributors)
        assertEquals(requests.toSet(), group.requests.map { it.id }.toSet())
        assertArrayEquals(image, value(supervisor.safety.evidence(requests.first())).bytes)
        value(supervisor.safety.approve(group.id))
        val emergency = value(supervisor.safety.queue()).emergencies.single { it.busId == bus.id }
        assertEquals("HIGH", emergency.priority)
        value(supervisor.safety.attend(emergency.id))
        value(supervisor.safety.close(emergency.id, "Resolved", "Attended by company"))
        assertTrue(value(supervisor.safety.queue()).emergencies.none { it.id == emergency.id })
        value(conductor.trip.close(shift))
        value(conductor.auth.signOut()); value(supervisor.auth.signOut())
    }

    @Test fun staffBackendLifecycleWithOfflineRetryAndRoleIsolation() = runBlocking {
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

        value(conductor.trip.close(shift))
        assertNull(value(conductor.trip.restore()))
        assertNull(value(supervisor.fleet.overview()).buses.single { it.id == bus.id }.shiftId)
        value(conductor.auth.signOut()); value(supervisor.auth.signOut())
    }
}
