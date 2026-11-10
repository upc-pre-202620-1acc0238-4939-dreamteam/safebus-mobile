package com.dreamteam.safebus.trip.data

import com.dreamteam.safebus.shared.data.*
import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.trip.domain.*
import java.time.Instant

class HttpCounterService(private val endpoint: String, private val localHttp: Boolean,
    private val authorized: () -> Boolean, private val engine: HttpEngine = UrlConnectionEngine()) : CounterService {
    private suspend fun <T> call(path: String, body: ByteArray, key: String, decode: (ApiResponse) -> T): DomainResult<T> {
        if (key.isBlank()) return invalid("COUNTING_FIELDS_REQUIRED")
        var expected: Long? = null
        val api = ApiClient(endpoint, localHttp, { key.takeIf { authorized() } }, {}, HttpEngine { request ->
            engine.execute(request).also { response ->
                if (response.status == 409) expected = runCatching { response.json().optionalLong("expectedSequence")?.takeIf { it > 0 } }.getOrNull()
            }
        }, deviceAuthentication = true)
        return when (val result = api.call("POST", path, body, "application/json", decode = decode)) {
            is DomainResult.Success -> result
            is DomainResult.Error -> result.copy(failure = result.failure.copy(expectedSequence = expected))
        }
    }
    override suspend fun send(command: CountCommand, key: String): DomainResult<CountReceipt> {
        val initialize = command.action == CountAction.INITIALIZE
        val body = if (initialize) jsonBody("initializationId" to command.id, "busId" to command.busId, "shiftId" to command.shiftId,
            "confirmedCount" to 0, "confirmedAt" to command.capturedAt.toString())
        else jsonBody("eventId" to command.id, "busId" to command.busId, "shiftId" to command.shiftId, "sequence" to command.sequence,
            "type" to command.action.name, "capturedAt" to command.capturedAt.toString())
        return call(if (initialize) "/api/v1/occupancy-initializations" else "/api/v1/occupancy-events", body, key) {
            val dto = it.json()
            if (initialize) {
                require(dto.getString("initializationId") == command.id && dto.getLong("busId") == command.busId && dto.getLong("shiftId") == command.shiftId && dto.getInt("confirmedCount") == 0)
                CountReceipt(0, 1, Instant.parse(dto.getString("confirmedAt")))
            } else {
                require(dto.getString("eventId") == command.id && dto.getLong("sequence") == command.sequence && dto.getString("type") == command.action.name)
                CountReceipt(dto.getInt("count"), Math.addExact(command.sequence, 1), null)
            }
        }
    }
    override suspend fun heartbeat(busId: Long, key: String): DomainResult<Instant> = if (busId <= 0) invalid("INVALID_BUS") else
        call("/api/v1/occupancy-heartbeats", jsonBody("busId" to busId), key) {
            val dto = it.json(); require(dto.getLong("busId") == busId); Instant.parse(dto.getString("receivedAt"))
        }
}
