package com.dreamteam.safebus.fleet.data

import com.dreamteam.safebus.fleet.domain.FleetChange
import com.dreamteam.safebus.shared.data.*
import com.dreamteam.safebus.shared.domain.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class RemoteFleetRepositoryTest {
    @Test fun surfacesForbiddenAndRejectsInvalidCapacityWithoutSending() = runBlocking {
        var requests = 0
        val api = ApiClient("https://example.com", false, { "token" }, {}, HttpEngine { requests++; ApiResponse(403, byteArrayOf()) },
            Dispatchers.Unconfined, { "COMPANY_ACCESS_DENIED" })
        val repo = RemoteFleetRepository(api)
        assertEquals(FailureKind.VALIDATION, (repo.save(FleetChange.Capacity(1, -1, "ref")) as DomainResult.Error).failure.kind)
        assertEquals(0, requests)
        assertEquals(FailureKind.FORBIDDEN, (repo.overview() as DomainResult.Error).failure.kind)
        assertEquals(1, requests)
    }
}
