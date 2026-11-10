package com.dreamteam.safebus.trip.data

import com.dreamteam.safebus.shared.data.*
import com.dreamteam.safebus.shared.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class RemoteCountingRepositoryTest {
    @Test fun rejectsInvalidIdentifiersAndSurfacesForbiddenWithoutStoringKeys() = runBlocking {
        var calls = 0
        val repo = RemoteCountingRepository(ApiClient("https://example.com", false, { "user" }, {}, HttpEngine {
            calls++; ApiResponse(403, byteArrayOf())
        }, Dispatchers.Unconfined, { "BUS_ACCESS_DENIED" }))
        assertTrue(repo.register(0, "label") is DomainResult.Error)
        assertTrue(repo.register(1, "x".repeat(61)) is DomainResult.Error)
        assertTrue(repo.rotate(1, 0) is DomainResult.Error)
        assertTrue(repo.revoke(0, 1) is DomainResult.Error)
        assertEquals(0, calls)
        assertEquals(FailureKind.FORBIDDEN, (repo.sources(1) as DomainResult.Error).failure.kind)
        assertEquals(FailureKind.FORBIDDEN, (repo.occupancy(1) as DomainResult.Error).failure.kind)
        assertEquals(2, calls)
    }
}
