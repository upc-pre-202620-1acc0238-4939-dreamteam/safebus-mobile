package com.dreamteam.safebus.shared.domain

import com.dreamteam.safebus.trip.domain.Position
import kotlinx.coroutines.runBlocking
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class TrackingEngineTest {
    @Test fun sendsOnlyFreshDriverSamplesAndStopsAfterClosure() = runBlocking {
        var sent = 0
        var fail = true
        val engine = TrackingEngine(sendPosition = {
            if (fail) DomainResult.Error(Failure(FailureKind.STORAGE)) else { sent++; DomainResult.Success(Delivery.PENDING) }
        })
        val sample = Position(-12.0, -77.0, 10.0, Instant.EPOCH)
        engine.tick(sample, Instant.EPOCH)
        assertEquals(TrackingStatus.FAILED, engine.state.value.status); assertEquals(0, sent)
        fail = false; engine.tick(sample, Instant.EPOCH)
        assertEquals(TrackingStatus.PENDING, engine.state.value.status); assertEquals(1, sent)
        engine.tick(sample, Instant.EPOCH.plusSeconds(30)); assertEquals(1, sent)
        engine.tick(sample, Instant.EPOCH.plusSeconds(61)); assertEquals(TrackingStatus.WAITING, engine.state.value.status); assertEquals(1, sent)
        engine.stop(); engine.tick(sample.copy(capturedAt = Instant.EPOCH.plusSeconds(90)), Instant.EPOCH.plusSeconds(90))
        assertEquals(TrackingStatus.STOPPED, engine.state.value.status); assertEquals(1, sent)
    }
    @Test fun missingBusDataNeverEndsJourneyAndDurableAutomaticClosureStopsComparisons() = runBlocking {
        var now = Instant.EPOCH
        var fail = true
        var ended = 0
        val engine = TrackingEngine(vehiclePosition = {
            if (fail) DomainResult.Error(Failure(FailureKind.NETWORK)) else DomainResult.Success(Position(-12.002, -77.0, 10.0, now))
        }, endJourney = { ended++; DomainResult.Success(Delivery.PENDING) })
        suspend fun tick(seconds: Long) { now = Instant.EPOCH.plusSeconds(seconds); engine.tick(Position(-12.0, -77.0, 10.0, now), now) }
        tick(0); assertEquals(TrackingStatus.MANUAL_ONLY, engine.state.value.status); assertEquals(0, ended)
        fail = false; tick(30); tick(60); assertEquals(0, ended)
        tick(90); assertEquals(TrackingStatus.AUTO_ENDED, engine.state.value.status); assertEquals(1, ended)
        tick(120); assertEquals(1, ended)
    }
}
