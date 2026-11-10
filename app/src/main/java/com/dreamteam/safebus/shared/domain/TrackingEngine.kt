package com.dreamteam.safebus.shared.domain

import com.dreamteam.safebus.passenger.domain.Separation
import com.dreamteam.safebus.passenger.domain.SeparationDetector
import com.dreamteam.safebus.trip.domain.Position
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant

enum class TrackingStatus { STOPPED, WAITING, TRACKING, PENDING, MANUAL_ONLY, FAILED, AUTO_ENDED }
data class TrackingState(val status: TrackingStatus = TrackingStatus.STOPPED, val failure: Failure? = null, val lastSample: Instant? = null)

fun trackingDelayMillis(driver: Boolean, elapsedMillis: Long): Long =
    ((if (driver) 30_000L else 15_000L) - elapsedMillis.coerceAtLeast(0)).coerceAtLeast(1)

class TrackingEngine(private val sendPosition: (suspend (Position) -> DomainResult<Delivery>)? = null,
    private val vehiclePosition: (suspend () -> DomainResult<Position>)? = null,
    private val endJourney: (suspend (Separation) -> DomainResult<Delivery>)? = null) {
    private val mutable = MutableStateFlow(TrackingState(TrackingStatus.WAITING))
    val state = mutable.asStateFlow()
    private val separation = SeparationDetector()
    private var sentAt: Instant? = null
    private var stopped = false
    val shouldCollectLocation: Boolean get() = !stopped
    fun stop() { stopped = true; separation.reset(); mutable.value = TrackingState() }
    fun unavailable() { separation.reset(); mutable.value = TrackingState(TrackingStatus.MANUAL_ONLY) }
    suspend fun tick(position: Position?, now: Instant) {
        if (stopped) return
        if (position == null || position.capturedAt > now || position.capturedAt < now.minusSeconds(60)) {
            separation.reset(); mutable.value = TrackingState(TrackingStatus.WAITING); return
        }
        if (sendPosition != null) {
            if (sentAt == position.capturedAt) return
            when (val result = sendPosition.invoke(position)) {
                is DomainResult.Error -> mutable.value = TrackingState(TrackingStatus.FAILED, result.failure)
                is DomainResult.Success -> {
                    sentAt = position.capturedAt
                    mutable.value = TrackingState(if (result.value == Delivery.PENDING) TrackingStatus.PENDING else TrackingStatus.TRACKING, lastSample = sentAt)
                }
            }
            return
        }
        if (vehiclePosition == null || endJourney == null) { unavailable(); stopped = true; return }
        when (val result = vehiclePosition.invoke()) {
            is DomainResult.Error -> {
                separation.reset()
                if (result.failure.kind in setOf(FailureKind.FORBIDDEN, FailureKind.UNAUTHORIZED)) stopped = true
                mutable.value = TrackingState(TrackingStatus.MANUAL_ONLY, result.failure)
            }
            is DomainResult.Success -> {
                val summary = separation.compare(position, result.value, now)
                mutable.value = TrackingState(if (position.reliableAt(now) && result.value.reliableAt(now)) TrackingStatus.TRACKING else TrackingStatus.MANUAL_ONLY, lastSample = position.capturedAt)
                if (summary != null) when (val end = endJourney.invoke(summary)) {
                    is DomainResult.Error -> mutable.value = TrackingState(TrackingStatus.FAILED, end.failure)
                    is DomainResult.Success -> { stopped = true; mutable.value = TrackingState(TrackingStatus.AUTO_ENDED) }
                }
            }
        }
    }
}
