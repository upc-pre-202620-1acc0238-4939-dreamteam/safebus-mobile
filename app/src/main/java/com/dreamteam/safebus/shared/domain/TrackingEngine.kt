package com.dreamteam.safebus.shared.domain

import com.dreamteam.safebus.trip.domain.Position
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant

enum class TrackingStatus { STOPPED, WAITING, TRACKING, PENDING, MANUAL_ONLY, FAILED }
data class TrackingState(val status: TrackingStatus = TrackingStatus.STOPPED, val failure: Failure? = null, val lastSample: Instant? = null)

fun trackingDelayMillis(elapsedMillis: Long): Long =
    (30_000L - elapsedMillis.coerceAtLeast(0)).coerceAtLeast(1)

class TrackingEngine(private val sendPosition: suspend (Position) -> DomainResult<Delivery>) {
    private val mutable = MutableStateFlow(TrackingState(TrackingStatus.WAITING))
    val state = mutable.asStateFlow()
    private var sentAt: Instant? = null
    private var stopped = false
    val shouldCollectLocation: Boolean get() = !stopped
    fun stop() { stopped = true; mutable.value = TrackingState() }
    fun unavailable() { mutable.value = TrackingState(TrackingStatus.MANUAL_ONLY) }
    suspend fun tick(position: Position?, now: Instant) {
        if (stopped) return
        if (position == null || position.capturedAt > now || position.capturedAt < now.minusSeconds(60)) {
            mutable.value = TrackingState(TrackingStatus.WAITING); return
        }
        if (sentAt == position.capturedAt) return
        when (val result = sendPosition.invoke(position)) {
            is DomainResult.Error -> mutable.value = TrackingState(TrackingStatus.FAILED, result.failure)
            is DomainResult.Success -> {
                sentAt = position.capturedAt
                mutable.value = TrackingState(if (result.value == Delivery.PENDING) TrackingStatus.PENDING else TrackingStatus.TRACKING, lastSample = sentAt)
            }
        }
    }
}
