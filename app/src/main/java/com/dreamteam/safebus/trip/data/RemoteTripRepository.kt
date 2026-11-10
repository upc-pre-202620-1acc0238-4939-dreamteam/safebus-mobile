package com.dreamteam.safebus.trip.data

import com.dreamteam.safebus.shared.data.*
import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.trip.domain.*
import kotlinx.coroutines.*
import java.io.*
import java.time.Instant
import java.util.UUID

data class ShiftDto(val id: Long, val assignmentId: Long, val busId: Long, val status: String, val startedAt: String) {
    fun toDomain(): ActiveShift { require(status == "ACTIVE"); return ActiveShift(id, assignmentId, busId, Instant.parse(startedAt)) }
}

interface TripService {
    suspend fun assignment(): DomainResult<Assignment>
    suspend fun activate(id: Long, credential: String): DomainResult<ActiveShift>
    suspend fun close(id: Long): DomainResult<Unit>
    suspend fun locate(shift: ActiveShift, position: Position): DomainResult<Delivery>
}

class HttpTripService(private val api: ApiClient, private val outbox: Outbox) : TripService {
    override suspend fun assignment() = api.call("GET", "/api/v1/shifts/me/assignment") {
        val dto = it.json()
        Assignment(dto.getLong("assignmentId"), dto.getString("status"), dto.getString("busPlate"), dto.getString("routeName"),
            dto.getString("origin"), dto.getString("destination"), Instant.parse(dto.getString("plannedStart")), Instant.parse(dto.getString("plannedEnd")))
    }
    override suspend fun activate(id: Long, credential: String) = api.call("POST", "/api/v1/shifts/activate",
        jsonBody("assignmentId" to id, "qrCredential" to credential), "application/json") {
        val dto = it.json()
        ShiftDto(dto.getLong("id"), dto.getLong("assignmentId"), dto.getLong("busId"), dto.getString("status"), dto.getString("startedAt")).toDomain()
    }
    override suspend fun close(id: Long) = api.call("POST", "/api/v1/shifts/$id/close") {
        require(it.json().getString("status") == "CLOSED"); Unit
    }
    override suspend fun locate(shift: ActiveShift, position: Position): DomainResult<Delivery> {
        val id = UUID.randomUUID().toString()
        return outbox.submit(PendingRequest(id, "/api/v1/location-events", jsonBody("eventId" to id, "shiftId" to shift.id,
            "busId" to shift.busId, "capturedAt" to position.capturedAt.toString(), "accuracyMeters" to position.accuracy,
            "latitude" to position.latitude, "longitude" to position.longitude), "application/json"))
    }
}

object ShiftCodec {
    fun encode(value: ActiveShift): ByteArray = ByteArrayOutputStream().also { bytes ->
        DataOutputStream(bytes).use { it.writeLong(value.id); it.writeLong(value.assignmentId); it.writeLong(value.busId); it.writeUTF(value.startedAt.toString()) }
    }.toByteArray()
    fun decode(bytes: ByteArray): ActiveShift = DataInputStream(ByteArrayInputStream(bytes)).use {
        ActiveShift(it.readLong(), it.readLong(), it.readLong(), Instant.parse(it.readUTF())).also { _ -> require(it.available() == 0) }
    }
}

class RemoteTripRepository(private val service: TripService, private val store: SecureStore, owner: String,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO) : TripRepository {
    private val key = "shift:$owner"
    override suspend fun assignment() = service.assignment()
    override suspend fun restore(): DomainResult<ActiveShift?> = storage { DomainResult.Success(store.read(key)?.let(ShiftCodec::decode)) }
    override suspend fun activate(assignmentId: Long, credential: String): DomainResult<ActiveShift> = storage {
        if (assignmentId <= 0 || credential.isBlank()) return@storage invalid("QR_REQUIRED")
        when (val result = service.activate(assignmentId, credential.trim())) {
            is DomainResult.Error -> result
            is DomainResult.Success -> { store.write(key, ShiftCodec.encode(result.value)); result }
        }
    }
    override suspend fun close(shift: ActiveShift): DomainResult<Unit> = storage {
        when (val result = service.close(shift.id)) {
            is DomainResult.Error -> result
            is DomainResult.Success -> { store.remove(key); result }
        }
    }
    override suspend fun locate(shift: ActiveShift, position: Position) = service.locate(shift, position)
    private suspend fun <T> storage(block: suspend () -> DomainResult<T>) = withContext(dispatcher) {
        try { block() } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { DomainResult.Error(Failure(FailureKind.STORAGE)) }
    }
}
