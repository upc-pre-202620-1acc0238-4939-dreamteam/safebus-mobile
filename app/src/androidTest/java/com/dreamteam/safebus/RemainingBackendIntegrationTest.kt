package com.dreamteam.safebus

import androidx.test.platform.app.InstrumentationRegistry
import com.dreamteam.safebus.fleet.domain.FleetChange
import com.dreamteam.safebus.shared.data.*
import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.trip.data.*
import com.dreamteam.safebus.trip.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.IOException
import java.time.Instant
import java.util.UUID

class RemainingBackendIntegrationTest {
    private fun <T> value(result: DomainResult<T>): T = when (result) {
        is DomainResult.Success -> result.value
        is DomainResult.Error -> throw AssertionError("Backend failure: ${result.failure.kind}/${result.failure.code}")
    }
    @Test fun connectsAllRemainingOperationalEndpointsAndPreservesDeviceOrdering() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("backend") == "true")
        val endpoint = "http://10.0.2.2:8080"
        val supervisor = BackendIntegrationTest.Client(endpoint)
        value(supervisor.auth.signIn("sup-001", "Safebus2024!"))
        val suffix = UUID.randomUUID().toString().take(8)
        val bus = value(supervisor.fleet.save(FleetChange.Bus("COUNT-$suffix")))
        val driver = value(supervisor.fleet.save(FleetChange.Driver("Counter driver", "counter-$suffix", "Integration2026!")))
        val route = value(supervisor.fleet.save(FleetChange.Route("Counter route", "A", "B")))
        val assignment = value(supervisor.fleet.save(FleetChange.Assignment(driver.id, bus.id, route.id, Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600))))
        val conductor = BackendIntegrationTest.Client(endpoint)
        value(conductor.auth.signIn("counter-$suffix", "Integration2026!"))
        val shift = value(conductor.trip.activate(assignment.id, requireNotNull(driver.credential)))
        val sources = RemoteCountingRepository(supervisor.api)
        assertTrue(value(sources.sources(bus.id)).isEmpty())
        val source = value(sources.register(bus.id, "Mobile authorized source"))
        assertEquals(source.source.id, value(sources.sources(bus.id)).single().id)
        assertEquals("UNAVAILABLE", value(sources.occupancy(bus.id)).status)
        assertTrue(sources.register(bus.id, "duplicate") is DomainResult.Error)
        assertEquals(1, value(sources.sources(bus.id)).size)
        var offline = false
        val service = HttpCounterService(endpoint, true, { supervisor.auth.session.value != null }, HttpEngine {
            if (offline) throw IOException("Offline test") else UrlConnectionEngine().execute(it)
        })
        val store = BackendIntegrationTest.MemoryStore()
        fun counter() = DurableCounterRepository(service, store, "source:${source.source.id}", bus.id, shift.id)
        fun command(action: CountAction, sequence: Long) = CountCommand(UUID.randomUUID().toString(), bus.id, shift.id, action, sequence, Instant.now())
        val initial = command(CountAction.INITIALIZE, 1)
        assertEquals(0, value(counter().submit(initial, source.key)).receipt?.count)
        assertEquals(0, value(service.send(initial, source.key)).count)
        value(service.heartbeat(bus.id, source.key))
        assertEquals("CURRENT", value(sources.occupancy(bus.id)).status)
        offline = true
        val first = command(CountAction.ENTRY, 1)
        assertEquals(FailureKind.NETWORK, (counter().submit(first, source.key) as DomainResult.Error).failure.kind)
        assertEquals(first, value(counter().restore()).pending)
        assertEquals(0, value(counter().restore()).receipt?.count)
        offline = false
        assertEquals(1, value(counter().retry(source.key)).receipt?.count)
        assertNull(value(counter().restore()).pending)
        assertEquals(1, value(service.send(first, source.key)).count)
        val gap = counter().submit(command(CountAction.ENTRY, 3), source.key) as DomainResult.Error
        assertEquals(2L, gap.failure.expectedSequence); assertNull(value(counter().restore()).pending)
        assertEquals(1, value(counter().restore()).receipt?.count)
        assertEquals("UNAVAILABLE", value(sources.occupancy(bus.id)).status)
        value(counter().submit(command(CountAction.ENTRY, 2), source.key))
        value(counter().submit(command(CountAction.EXIT, 3), source.key))
        assertEquals(1, value(sources.occupancy(bus.id)).count)
        val rotated = value(sources.rotate(bus.id, source.source.id))
        assertNotEquals(source.key, rotated.key)
        assertEquals(FailureKind.FORBIDDEN, (service.heartbeat(bus.id, source.key) as DomainResult.Error).failure.kind)
        assertNotNull(supervisor.auth.session.value); assertEquals(1, value(sources.occupancy(bus.id)).count)
        value(service.heartbeat(bus.id, rotated.key))
        assertEquals("REVOKED", value(sources.revoke(bus.id, source.source.id)).status)
        assertEquals("REVOKED", value(sources.revoke(bus.id, source.source.id)).status)
        assertEquals(FailureKind.FORBIDDEN, (service.heartbeat(bus.id, rotated.key) as DomainResult.Error).failure.kind)
        assertNotNull(supervisor.auth.session.value); assertNull(value(counter().restore()).pending)
        value(conductor.trip.close(shift))

        val public = HttpServiceContact(supervisor.api)
        assertTrue(value(public.health()))
        val contact = ContactSubmission(UUID.randomUUID().toString(), "Test company", "Test contact", "$suffix@example.com", true)
        val contactStore = BackendIntegrationTest.MemoryStore()
        val contacts = DurableContactRepository(public, contactStore, endpoint)
        val receipt = value(contacts.submit(contact)).receipt!!
        assertEquals(receipt, value(public.contact(contact)))
        assertNull(value(contacts.restore()).pending)
        assertTrue(public.contact(contact.copy(consent = false)) is DomainResult.Error)
        assertEquals(receipt, value(contacts.restore()).receipt)
        value(contacts.clear()); assertTrue(contactStore.data.isEmpty())
        value(conductor.auth.signOut()); value(supervisor.auth.signOut())
    }
}
