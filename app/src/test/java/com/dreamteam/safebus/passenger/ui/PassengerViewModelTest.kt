package com.dreamteam.safebus.passenger.ui

import com.dreamteam.safebus.passenger.domain.*
import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.trip.domain.Position
import kotlinx.coroutines.Dispatchers
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class PassengerViewModelTest {
    @Test fun failedJourneyDoesNotBindBusAndOfflineEndStopsTrackingOnlyAfterDurableSave() {
        val journey = Journey(1, 1, "ABC", "Company", "VERIFIED", "Route", "A", "B", "Name", Instant.EPOCH)
        var persisted: Journey? = null
        var rejected = true
        val repo = object : PassengerRepository {
            override suspend fun register(input: Registration) = DomainResult.Success(1L)
            override suspend fun restore() = DomainResult.Success(persisted)
            override suspend fun start(qr: BusQr): DomainResult<Journey> = if (rejected) DomainResult.Error(Failure.fromStatus(422)) else DomainResult.Success(journey).also { persisted = journey }
            override suspend fun end(journey: Journey, reason: EndReason, separation: Separation?): DomainResult<Delivery> =
                if (rejected) DomainResult.Error(Failure(FailureKind.STORAGE)) else DomainResult.Success(Delivery.PENDING).also { persisted = null }
            override suspend fun location(busId: Long): DomainResult<Position> = DomainResult.Error(Failure.fromStatus(404))
        }
        var tracked: Journey? = null
        val model = PassengerViewModel(repo, { tracked = it }, Dispatchers.Unconfined)
        model.start("invalid")
        assertEquals(FailureKind.VALIDATION, model.state.value.failure?.kind)
        assertNull(model.state.value.value.journey); assertNull(persisted); assertNull(tracked)
        rejected = false; model.start("valid")
        assertEquals(journey, tracked)
        rejected = true; model.end()
        assertEquals(FailureKind.STORAGE, model.state.value.failure?.kind)
        assertEquals(journey, persisted); assertEquals(journey, model.state.value.value.journey)
        rejected = false; model.end()
        assertEquals(Delivery.PENDING, model.state.value.value.ended)
        assertNull(tracked); assertNull(persisted); assertNull(model.state.value.value.journey)
    }
}
