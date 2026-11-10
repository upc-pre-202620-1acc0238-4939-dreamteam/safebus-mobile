package com.dreamteam.safebus.safetycase.data

import com.dreamteam.safebus.safetycase.domain.*
import com.dreamteam.safebus.shared.data.*
import com.dreamteam.safebus.shared.domain.*
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

fun PendingRequest.toPendingPassengerRequest(): PassengerRequest {
    require(path == "/api/v1/passenger-requests")
    val boundary = contentType.substringAfter("boundary=").also { require(it.startsWith("SafeBus")) }
    val prefix = body.copyOfRange(0, minOf(body.size, 8192)).toString(Charsets.UTF_8)
    fun field(name: String): String = requireNotNull(Regex("Content-Disposition: form-data; name=\"$name\"\\r\\n\\r\\n(.*?)\\r\\n--${Regex.escape(boundary)}", RegexOption.DOT_MATCHES_ALL).find(prefix)).groupValues[1]
    return PassengerRequest(id, field("message"), "PENDING", Instant.parse(field("activatedAt")), null, null, null, null)
}

class RemoteSafetyRepository(private val api: ApiClient, private val outbox: Outbox, private val store: SecureStore,
    owner: String, private val clock: Clock = Clock.systemUTC(), private val dispatcher: CoroutineDispatcher = Dispatchers.IO) : SafetyRepository {
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
        try {
            saveAlerts(localAlerts() + (id to at))
            when (val result = outbox.submit(PendingRequest(id, "/api/v1/driver-emergencies",
                jsonBody("id" to id, "shiftId" to shiftId, "activatedAt" to at.toString()), "application/json"))) {
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
    override suspend fun request(journeyId: Long, message: String, photo: Photo): DomainResult<Receipt> {
        if (journeyId <= 0 || message.trim().length !in 1..500) return invalid("REQUEST_INVALID")
        val id = UUID.randomUUID().toString()
        val upload = multipart(mapOf("id" to id, "journeyId" to journeyId.toString(), "message" to message.trim(),
            "activatedAt" to clock.instant().toString()), "photo", photo.bytes, photo.mime)
        return outbox.submit(PendingRequest(id, "/api/v1/passenger-requests", upload.bytes, upload.contentType)).map { Receipt(id, it) }
    }
    override suspend fun mine(): DomainResult<List<PassengerRequest>> {
        val pending = when (val records = outbox.requests("/api/v1/passenger-requests")) {
            is DomainResult.Error -> return records
            is DomainResult.Success -> try { records.value.map(PendingRequest::toPendingPassengerRequest) }
                catch (_: Exception) { return DomainResult.Error(Failure(FailureKind.STORAGE)) }
        }
        return when (val remote = api.call("GET", "/api/v1/passenger-requests/mine") { it.json().objects("items").map { dto -> dto.toPassengerRequest() } }) {
            is DomainResult.Success -> DomainResult.Success((remote.value + pending).distinctBy { it.id }.sortedByDescending { it.submittedAt })
            is DomainResult.Error -> if (remote.failure.retryable && pending.isNotEmpty()) DomainResult.Success(pending) else remote
        }
    }
    override suspend fun history(busId: Long) = api.call("GET", "/api/v1/vehicles/$busId/safety-history") {
        it.json().objects("items").map { dto -> SafetyEntry(dto.getString("reference"), dto.getString("source"), dto.getString("state"),
            Instant.parse(dto.getString("recordedAt")), dto.optionalInt("requestCount")) }
    }
    override suspend fun evidence(requestId: String, supervisor: Boolean): DomainResult<Photo> {
        if (!validId(requestId)) return invalid("INVALID_REQUEST_ID")
        val prefix = if (supervisor) "" else "mine/"
        return api.call("GET", "/api/v1/passenger-requests/$prefix$requestId/photo") { Photo(it.bytes, it.contentType.substringBefore(';').trim()) }
    }
    override suspend fun queue() = api.call("GET", "/api/v1/review-queue") {
        val dto = it.json()
        ReviewQueue((dto.objects("driverEmergencies") + dto.objects("passengerEmergencies")).map(JSONObject::toEmergency).distinctBy(Emergency::id),
            dto.objects("groupsAwaitingApproval").map { group -> ReviewGroup(group.getLong("groupId"), group.getString("reference"),
                group.getLong("busId"), group.getInt("contributorCount"), group.objects("requests").map { request -> request.toPassengerRequest(true) }) },
            Instant.parse(dto.getString("generatedAt")))
    }
    override suspend fun approve(groupId: Long): DomainResult<Unit> = if (groupId <= 0) invalid("INVALID_GROUP") else api.call("POST", "/api/v1/request-groups/$groupId/approve") { Unit }
    override suspend fun decline(groupId: Long, reason: String, response: String): DomainResult<Unit> =
        if (groupId <= 0 || !validDecision(reason, response)) invalid("DECISION_REQUIRED")
        else api.call("POST", "/api/v1/request-groups/$groupId/decline", jsonBody("reason" to reason.trim(), "response" to response.trim().ifEmpty { null }), "application/json") { Unit }
    override suspend fun attend(id: String): DomainResult<Unit> = if (!validId(id)) invalid("INVALID_EMERGENCY") else api.call("POST", "/api/v1/emergencies/$id/start-attention") { Unit }
    override suspend fun close(id: String, outcome: String, response: String): DomainResult<Unit> =
        if (!validId(id) || !validDecision(outcome, response)) invalid("DECISION_REQUIRED")
        else api.call("POST", "/api/v1/emergencies/$id/close", jsonBody("outcome" to outcome.trim(), "response" to response.trim().ifEmpty { null }), "application/json") { Unit }

    companion object {
        fun validDecision(reason: String, response: String) = reason.trim().length in 1..500 && response.trim().length <= 500
        private fun validId(id: String) = runCatching { UUID.fromString(id).toString() == id }.getOrDefault(false)
    }
}
