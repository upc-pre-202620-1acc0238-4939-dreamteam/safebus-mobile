package com.dreamteam.safebus.safetycase.data

import com.dreamteam.safebus.safetycase.domain.*
import com.dreamteam.safebus.shared.data.*
import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.trip.domain.Position
import kotlinx.coroutines.*
import org.json.JSONObject
import java.time.Clock
import java.time.Instant
import java.util.UUID

fun JSONObject.toEmergency(priority: String? = null): Emergency = Emergency(getString("id"), getString("status"),
    priority ?: getString("priority"), optionalLong("busId"), Instant.parse(getString("activatedAt")),
    optionalString("response"), optionalLong("responsibleSupervisorId"))

fun JSONObject.toPassengerRequest(review: Boolean = false): PassengerRequest {
    val decision = optJSONObject("decision")
    return PassengerRequest(getString("requestId"), getString("message"), getString("status"),
        Instant.parse(getString(if (review) "activatedAt" else "submittedAt")), optionalInt("contributorCount"),
        optionalString("groupReference"), decision?.optionalString("result"), decision?.optionalString("response"), decision?.optionalString("decidedAt")?.let(Instant::parse))
}

class RemoteSafetyRepository(private val api: ApiClient, private val outbox: Outbox, private val store: SecureStore,
    owner: String, private val clock: Clock = Clock.systemUTC(), private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val driverPosition: () -> Position? = { null }) : SafetyRepository {
    private val key = "driver-alerts:$owner"
    private fun localAlerts(): List<Pair<String, Instant>> = store.read(key)?.toString(Charsets.UTF_8)?.lines()?.filter(String::isNotBlank)?.map {
        val parts = it.split('|'); UUID.fromString(parts[0]); parts[0] to Instant.parse(parts[1])
    } ?: emptyList()
    private fun saveAlerts(items: List<Pair<String, Instant>>) {
        if (items.isEmpty()) store.remove(key)
        else store.write(key, items.takeLast(100).joinToString("\n") { "${it.first}|${it.second}" }.toByteArray())
    }
    override suspend fun panic(shiftId: Long): DomainResult<Receipt> = withContext(dispatcher) {
        if (shiftId <= 0) return@withContext invalid("SHIFT_REQUIRED")
        val id = UUID.randomUUID().toString(); val at = clock.instant()
        val position = driverPosition()?.takeIf { it.capturedAt <= at && it.capturedAt >= at.minusSeconds(60) }
        try {
            saveAlerts(localAlerts() + (id to at))
            when (val result = outbox.submit(PendingRequest(id, "/api/v1/driver-emergencies",
                jsonBody("id" to id, "shiftId" to shiftId, "activatedAt" to at.toString(), "latitude" to position?.latitude, "longitude" to position?.longitude), "application/json"))) {
                is DomainResult.Success -> DomainResult.Success(Receipt(id, result.value))
                is DomainResult.Error -> {
                    if (result.failure.kind != FailureKind.UNAUTHORIZED) saveAlerts(localAlerts().filterNot { it.first == id })
                    result
                }
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { DomainResult.Error(Failure(FailureKind.STORAGE)) }
    }
    override suspend fun driverAlerts(): DomainResult<List<Emergency>> = withContext(dispatcher) {
        val records = try { localAlerts() } catch (_: Exception) { return@withContext DomainResult.Error(Failure(FailureKind.STORAGE)) }
        val pending = when (val result = outbox.identifiers()) {
            is DomainResult.Error -> return@withContext result
            is DomainResult.Success -> result.value
        }
        val alerts = mutableListOf<Emergency>()
        for ((id, at) in records.reversed()) {
            if (id in pending) { alerts.add(Emergency(id, "PENDING", "CRITICAL", null, at)); continue }
            when (val result = api.call("GET", "/api/v1/driver-emergencies/$id") { it.json().toEmergency("CRITICAL") }) {
                is DomainResult.Success -> alerts.add(result.value)
                is DomainResult.Error -> {
                    if (result.failure.kind == FailureKind.NOT_FOUND) {
                        try { saveAlerts(localAlerts().filterNot { it.first == id }) }
                        catch (_: Exception) { return@withContext DomainResult.Error(Failure(FailureKind.STORAGE)) }
                        return@withContext invalid("ALERT_NOT_DELIVERED")
                    }
                    return@withContext result
                }
            }
        }
        DomainResult.Success(alerts)
    }
    override suspend fun evidence(requestId: String): DomainResult<Photo> {
        if (!validId(requestId)) return invalid("INVALID_REQUEST_ID")
        return api.call("GET", "/api/v1/passenger-requests/$requestId/photo") { Photo(it.bytes, it.contentType.substringBefore(';').trim()) }
    }
    override suspend fun queue() = api.call("GET", "/api/v1/review-queue") {
        val dto = it.json()
        ReviewQueue((dto.objects("driverEmergencies") + dto.objects("passengerEmergencies")).map(JSONObject::toEmergency).distinctBy(Emergency::id),
            dto.objects("groupsAwaitingApproval").map { group -> ReviewGroup(group.getLong("groupId"), group.getString("reference"),
                group.getLong("busId"), group.getInt("contributorCount"), group.objects("requests").map { request -> request.toPassengerRequest(true) }) },
            Instant.parse(dto.getString("generatedAt")))
    }
    override suspend fun approve(groupId: Long): DomainResult<Unit> = if (groupId <= 0) invalid("INVALID_GROUP") else api.call("POST", "/api/v1/request-groups/$groupId/approve") {}
    override suspend fun decline(groupId: Long, reason: String, response: String): DomainResult<Unit> =
        if (groupId <= 0 || !validDecision(reason, response)) invalid("DECISION_REQUIRED")
        else api.call("POST", "/api/v1/request-groups/$groupId/decline", jsonBody("reason" to reason.trim(), "response" to response.trim().ifEmpty { null }), "application/json") {}
    override suspend fun attend(id: String): DomainResult<Unit> = if (!validId(id)) invalid("INVALID_EMERGENCY") else api.call("POST", "/api/v1/emergencies/$id/start-attention") {}
    override suspend fun close(id: String, outcome: String, response: String): DomainResult<Unit> =
        if (!validId(id) || !validDecision(outcome, response)) invalid("DECISION_REQUIRED")
        else api.call("POST", "/api/v1/emergencies/$id/close", jsonBody("outcome" to outcome.trim(), "response" to response.trim().ifEmpty { null }), "application/json") {}

    companion object {
        fun validDecision(reason: String, response: String) = reason.trim().length in 1..500 && response.trim().length <= 500
        private fun validId(id: String) = runCatching { UUID.fromString(id).toString() == id }.getOrDefault(false)
    }
}
