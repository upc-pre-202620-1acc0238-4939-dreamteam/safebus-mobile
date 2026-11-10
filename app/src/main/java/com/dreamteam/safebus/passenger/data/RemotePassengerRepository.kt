package com.dreamteam.safebus.passenger.data

import com.dreamteam.safebus.passenger.domain.*
import com.dreamteam.safebus.shared.data.*
import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.trip.domain.Position
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.*
import java.time.Clock
import java.time.Instant

data class JourneyDto(val id: Long, val status: String, val plate: String, val company: String, val validation: String,
    val route: String, val origin: String, val destination: String, val driver: String, val startedAt: String) {
    fun toDomain(busId: Long?): Journey {
        require(status == "ACTIVE")
        return Journey(id, busId, plate, company, validation, route, origin, destination, driver, Instant.parse(startedAt))
    }
}

interface PassengerService {
    suspend fun register(input: Registration): DomainResult<Long>
    suspend fun start(qr: BusQr): DomainResult<Journey>
    suspend fun end(journey: Journey, reason: EndReason, separation: Separation?): DomainResult<Delivery>
    suspend fun location(busId: Long): DomainResult<Position>
}

class HttpPassengerService(private val api: ApiClient, private val outbox: Outbox, private val clock: Clock = Clock.systemUTC()) : PassengerService {
    override suspend fun register(input: Registration): DomainResult<Long> {
        val upload = multipart(mapOf("dni" to input.dni, "password" to input.password, "termsAccepted" to input.acceptedTerms.toString(),
            "termsVersion" to "2026-10"), "facePhoto", input.face.bytes, input.face.mime)
        return api.call("POST", "/api/v1/passengers", upload.bytes, upload.contentType, authenticated = false) { it.json().getLong("id").also { id -> require(id > 0) } }
    }
    override suspend fun start(qr: BusQr) = api.call("POST", "/api/v1/journeys", jsonBody("busQrCode" to qr.code), "application/json") {
        val dto = it.json(); val bus = dto.getJSONObject("bus"); val route = dto.getJSONObject("route")
        JourneyDto(dto.getLong("journeyId"), dto.getString("status"), bus.getString("plate"), bus.getString("companyName"),
            bus.getString("companyValidationStatus"), route.getString("name"), route.getString("origin"), route.getString("destination"),
            dto.getString("driverPublicName"), dto.getString("startedAt")).toDomain(qr.busId)
    }
    override suspend fun end(journey: Journey, reason: EndReason, separation: Separation?): DomainResult<Delivery> {
        val summary = separation?.let { JSONObject().put("separationDistanceMeters", it.meters).put("separationDurationSeconds", it.seconds) }
        return outbox.submit(PendingRequest("end:${journey.id}", "/api/v1/journeys/${journey.id}/end",
            jsonBody("reason" to reason.name, "endedAt" to journeyEndTime(journey.startedAt, clock.instant(), separation).toString(), "summary" to summary), "application/json"), preserveExistingEnd = true)
    }
    override suspend fun location(busId: Long) = api.call("GET", "/api/v1/vehicles/$busId/location") {
        val dto = it.json()
        Position(dto.getDouble("latitude"), dto.getDouble("longitude"), dto.getDouble("accuracyMeters"), Instant.parse(dto.getString("capturedAt")))
    }
}

object JourneyCodec {
    fun encode(value: Journey): ByteArray = ByteArrayOutputStream().also { bytes ->
        DataOutputStream(bytes).use { output ->
            output.writeLong(value.id); output.writeLong(value.busId ?: 0)
            listOf(value.plate, value.company, value.validation, value.route, value.origin, value.destination, value.driver, value.startedAt.toString()).forEach(output::writeUTF)
        }
    }.toByteArray()
    fun decode(bytes: ByteArray): Journey = DataInputStream(ByteArrayInputStream(bytes)).use {
        Journey(it.readLong(), it.readLong().takeIf { id -> id > 0 }, it.readUTF(), it.readUTF(), it.readUTF(), it.readUTF(),
            it.readUTF(), it.readUTF(), it.readUTF(), Instant.parse(it.readUTF())).also { _ -> require(it.available() == 0) }
    }
}

class RemotePassengerRepository(private val service: PassengerService, private val store: SecureStore, owner: String,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO) : PassengerRepository {
    private val key = "journey:$owner"
    override suspend fun register(input: Registration) = if (input.valid()) service.register(input) else invalid("REGISTRATION_INVALID")
    override suspend fun restore(): DomainResult<Journey?> = storage { DomainResult.Success(store.read(key)?.let(JourneyCodec::decode)) }
    override suspend fun start(qr: BusQr): DomainResult<Journey> = storage {
        when (val result = service.start(qr)) {
            is DomainResult.Error -> result
            is DomainResult.Success -> { store.write(key, JourneyCodec.encode(result.value)); result }
        }
    }
    override suspend fun end(journey: Journey, reason: EndReason, separation: Separation?): DomainResult<Delivery> = storage {
        when (val result = service.end(journey, reason, separation)) {
            is DomainResult.Error -> result
            is DomainResult.Success -> { store.remove(key); result }
        }
    }
    override suspend fun location(busId: Long) = service.location(busId)
    private suspend fun <T> storage(block: suspend () -> DomainResult<T>) = withContext(dispatcher) {
        try { block() } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { DomainResult.Error(Failure(FailureKind.STORAGE)) }
    }
}
