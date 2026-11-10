package com.dreamteam.safebus.fleet.domain

import com.dreamteam.safebus.shared.domain.*
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class FleetTest {
    @Test fun contributorAndAttachedCountsRemainDistinctAndNonNegative() {
        val group = GroupCount("AWAITING_APPROVAL", 3, 2)
        assertEquals(3, group.contributors); assertEquals(2, group.attached)
        assertThrows(IllegalArgumentException::class.java) { GroupCount("COLLECTING", -1, 0) }
    }
    @Test fun validatesAllProvisioningFormsWithoutPersistingInvalidChanges() = runBlocking {
        val saved = mutableListOf<FleetChange>()
        val repo = object : FleetRepository {
            override suspend fun overview() = DomainResult.Success(FleetOverview(emptyList(), Instant.EPOCH))
            override suspend fun save(change: FleetChange): DomainResult<ProvisionReceipt> { saved.add(change); return DomainResult.Success(ProvisionReceipt(1)) }
        }
        val save = SaveFleetResource(repo)
        listOf(FleetChange.Bus(" "), FleetChange.Driver("Name", "login", "short"), FleetChange.Route("route", "", "end"),
            FleetChange.Assignment(1, 1, 1, Instant.EPOCH, Instant.EPOCH), FleetChange.Capacity(1, 0, "reference"),
            FleetChange.Capacity(1, 20, " ")).forEach {
            assertTrue(save(it) is DomainResult.Error); assertTrue(saved.isEmpty())
        }
        assertTrue(save(FleetChange.Capacity(1, 20, "record-1")) is DomainResult.Success)
        assertEquals(1, saved.size)
        assertFalse(ProvisionReceipt(1, "secret").toString().contains("secret"))
        assertFalse(FleetChange.Driver("name", "login", "password").toString().contains("password"))
    }
}
