package com.dreamteam.safebus.fleet.ui

import com.dreamteam.safebus.fleet.domain.*
import com.dreamteam.safebus.shared.domain.*
import kotlinx.coroutines.Dispatchers
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class FleetViewModelTest {
    @Test fun failedCapacityAndAssignmentNeverReplaceConfirmedFleetOrReceipt() {
        val overview = FleetOverview(emptyList(), Instant.EPOCH)
        var writes = 0
        var reject = true
        val repo = object : FleetRepository {
            override suspend fun overview() = DomainResult.Success(overview)
            override suspend fun save(change: FleetChange): DomainResult<ProvisionReceipt> =
                if (reject) DomainResult.Error(Failure.fromStatus(409)) else DomainResult.Success(ProvisionReceipt(8)).also { writes++ }
        }
        val model = FleetViewModel(repo, Dispatchers.Unconfined)
        model.refresh()
        model.save(FleetChange.Capacity(1, 20, "reference"))
        assertEquals(FailureKind.CONFLICT, model.state.value.failure?.kind)
        assertEquals(overview, model.state.value.value.overview); assertNull(model.state.value.value.receipt); assertEquals(0, writes)
        model.save(FleetChange.Capacity(1, 0, "reference"))
        assertEquals(FailureKind.VALIDATION, model.state.value.failure?.kind)
        assertEquals(overview, model.state.value.value.overview); assertNull(model.state.value.value.receipt); assertEquals(0, writes)
        reject = false
        model.save(FleetChange.Driver("Name", "login", "private-password"))
        assertEquals(8L, model.state.value.value.receipt?.id)
        assertEquals("", (model.state.value.value.savedChange as FleetChange.Driver).password)
        assertEquals(1, writes)
    }
}
