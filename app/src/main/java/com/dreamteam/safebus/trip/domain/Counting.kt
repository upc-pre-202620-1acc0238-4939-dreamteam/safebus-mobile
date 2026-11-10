package com.dreamteam.safebus.trip.domain

import com.dreamteam.safebus.shared.domain.*
import java.time.Instant
import java.util.UUID

data class CountingSource(val id: Long, val busId: Long, val label: String?, val status: String,
    val createdAt: Instant, val rotatedAt: Instant?, val revokedAt: Instant?) {
    init { require(id > 0 && busId > 0 && status in setOf("ACTIVE", "REVOKED")) }
}
data class SourceCredential(val source: CountingSource, val key: String) {
    init { require(key.isNotBlank() && !key.contains('\n') && !key.contains('\r')) }
    override fun toString() = "SourceCredential(sourceId=${source.id}, redacted)"
}
data class Occupancy(val busId: Long, val status: String, val reason: String?, val count: Int?, val capacity: Int?,
    val load: String?, val lastCountAt: Instant?, val lastHeartbeatAt: Instant?, val generatedAt: Instant) {
    init { require(busId > 0 && (count == null || count >= 0) && (capacity == null || capacity > 0)) }
}
interface CountingRepository {
    suspend fun sources(busId: Long): DomainResult<List<CountingSource>>
    suspend fun register(busId: Long, label: String): DomainResult<SourceCredential>
    suspend fun rotate(busId: Long, sourceId: Long): DomainResult<SourceCredential>
    suspend fun revoke(busId: Long, sourceId: Long): DomainResult<CountingSource>
    suspend fun occupancy(busId: Long): DomainResult<Occupancy>
}
enum class CountAction { INITIALIZE, ENTRY, EXIT }
data class CountCommand(val id: String, val busId: Long, val shiftId: Long, val action: CountAction,
    val sequence: Long, val capturedAt: Instant) {
    init { require(UUID.fromString(id).toString() == id && busId > 0 && shiftId > 0 && sequence > 0) }
}
data class CountReceipt(val count: Int, val nextSequence: Long, val receivedAt: Instant?) {
    init { require(count >= 0 && nextSequence > 0) }
}
data class CounterState(val pending: CountCommand? = null, val receipt: CountReceipt? = null)
interface CounterRepository {
    suspend fun restore(): DomainResult<CounterState>
    suspend fun submit(command: CountCommand, key: String): DomainResult<CounterState>
    suspend fun retry(key: String): DomainResult<CounterState>
    suspend fun heartbeat(busId: Long, key: String): DomainResult<Instant>
}
