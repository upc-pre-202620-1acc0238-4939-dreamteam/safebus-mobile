package com.dreamteam.safebus.trip.domain

import com.dreamteam.safebus.shared.domain.*
import java.time.Instant
import kotlin.math.*

data class Assignment(val id: Long, val status: String, val plate: String, val route: String,
    val origin: String, val destination: String, val startsAt: Instant, val endsAt: Instant) {
    init { require(id > 0 && plate.isNotBlank() && endsAt > startsAt) }
}

data class ActiveShift(val id: Long, val assignmentId: Long, val busId: Long, val startedAt: Instant) {
    init { require(id > 0 && assignmentId > 0 && busId > 0) }
}

data class Position(val latitude: Double, val longitude: Double, val accuracy: Double, val capturedAt: Instant) {
    init { require(latitude in -90.0..90.0 && longitude in -180.0..180.0 && accuracy.isFinite() && accuracy >= 0) }
    fun reliableAt(now: Instant) = accuracy <= 30 && capturedAt <= now && capturedAt >= now.minusSeconds(60)
    fun distanceTo(other: Position): Double {
        val deltaLat = Math.toRadians(other.latitude - latitude)
        val deltaLon = Math.toRadians(other.longitude - longitude)
        val a = sin(deltaLat / 2).pow(2) + cos(Math.toRadians(latitude)) * cos(Math.toRadians(other.latitude)) * sin(deltaLon / 2).pow(2)
        return 6_371_000 * 2 * atan2(sqrt(a.coerceIn(0.0, 1.0)), sqrt((1 - a).coerceIn(0.0, 1.0)))
    }
}

interface TripRepository {
    suspend fun assignment(): DomainResult<Assignment>
    suspend fun restore(): DomainResult<ActiveShift?>
    suspend fun activate(assignmentId: Long, credential: String): DomainResult<ActiveShift>
    suspend fun close(shift: ActiveShift): DomainResult<Unit>
    suspend fun locate(shift: ActiveShift, position: Position): DomainResult<Delivery>
}
