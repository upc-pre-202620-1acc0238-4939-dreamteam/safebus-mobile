package com.dreamteam.safebus.safetycase.data

import com.dreamteam.safebus.BackendIntegrationTest
import com.dreamteam.safebus.shared.data.*
import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.trip.domain.Position
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class PanicLocationTest {
    @Test fun includesOnlyFreshDriverPositionAndKeepsOriginalCoordinatesForRetry() = runBlocking {
        val store = BackendIntegrationTest.MemoryStore()
        val now = Instant.parse("2026-11-10T12:00:00Z")
        var position = Position(-12.0, -77.0, 10.0, now)
        val requests = mutableListOf<PendingRequest>()
        val outbox = Outbox(store, "owner", PendingTransport { requests.add(it); DomainResult.Success(Unit) })
        val api = ApiClient("https://example.com", false, { "token" }, {})
        val repository = RemoteSafetyRepository(api, outbox, store, "owner", Clock.fixed(now, ZoneOffset.UTC), driverPosition = { position })
        assertTrue(repository.panic(1) is DomainResult.Success)
        val fresh = JSONObject(requests.last().body.toString(Charsets.UTF_8))
        assertEquals(-12.0, fresh.getDouble("latitude"), 0.0)
        assertEquals(-77.0, fresh.getDouble("longitude"), 0.0)
        position = position.copy(capturedAt = now.minusSeconds(61))
        repository.panic(1)
        val stale = JSONObject(requests.last().body.toString(Charsets.UTF_8))
        assertTrue(stale.isNull("latitude")); assertTrue(stale.isNull("longitude"))
    }
}
