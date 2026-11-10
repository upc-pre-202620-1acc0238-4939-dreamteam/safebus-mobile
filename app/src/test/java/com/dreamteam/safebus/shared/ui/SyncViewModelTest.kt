package com.dreamteam.safebus.shared.ui

import com.dreamteam.safebus.shared.domain.*
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Test

class SyncViewModelTest {
    @Test fun rejectedPendingRecordIsRemovedWithoutClaimingDelivery() {
        var records = 1
        val model = SyncViewModel({ DomainResult.Success(records) }, { records = 0; DomainResult.Error(Failure.fromStatus(422)) }, Dispatchers.Unconfined)
        model.count(); assertEquals(1, model.state.value.value)
        model.retry()
        assertEquals(FailureKind.VALIDATION, model.state.value.failure?.kind)
        assertEquals(0, model.state.value.value); assertEquals(0, records)
        assertFalse(model.state.value.busy)
    }
}
