package com.dreamteam.safebus.trip.ui

import com.dreamteam.safebus.trip.domain.*
import com.dreamteam.safebus.shared.domain.*
import kotlinx.coroutines.Dispatchers
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class TripViewModelTest {
    @Test fun rejectedActivationAndCloseRetainOnlyConfirmedState() {
        val start = Instant.EPOCH
        val shift = ActiveShift(1, 1, 1, start)
        var persisted: ActiveShift? = null
        var rejected = true
        val repo = object : TripRepository {
            override suspend fun assignment() = DomainResult.Success(Assignment(1, "ASSIGNED", "ABC", "R", "A", "B", start, start.plusSeconds(60)))
            override suspend fun restore() = DomainResult.Success(persisted)
            override suspend fun activate(assignmentId: Long, credential: String): DomainResult<ActiveShift> =
                if (rejected) DomainResult.Error(Failure.fromStatus(422)) else DomainResult.Success(shift).also { persisted = shift }
            override suspend fun close(shift: ActiveShift): DomainResult<Unit> = if (rejected) DomainResult.Error(Failure(FailureKind.NETWORK)) else DomainResult.Success(Unit).also { persisted = null }
            override suspend fun locate(shift: ActiveShift, position: Position) = DomainResult.Success(Delivery.ACKNOWLEDGED)
        }
        var tracked: ActiveShift? = null
        val model = TripViewModel(repo, { tracked = it }, Dispatchers.Unconfined)
        model.refresh(); model.activate("bad")
        assertEquals(FailureKind.VALIDATION, model.state.value.failure?.kind)
        assertNull(model.state.value.value.shift); assertNull(persisted); assertNull(tracked)
        rejected = false; model.activate("valid")
        assertEquals(shift, model.state.value.value.shift); assertEquals(shift, tracked)
        rejected = true; model.close()
        assertEquals(FailureKind.NETWORK, model.state.value.failure?.kind)
        assertEquals(shift, model.state.value.value.shift); assertEquals(shift, persisted)
        rejected = false; model.close()
        assertNull(model.state.value.value.shift); assertNull(persisted); assertNull(tracked)
    }
}
