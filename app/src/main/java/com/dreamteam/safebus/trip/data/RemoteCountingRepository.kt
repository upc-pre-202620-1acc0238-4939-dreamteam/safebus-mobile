package com.dreamteam.safebus.trip.data

import com.dreamteam.safebus.shared.data.*
import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.trip.domain.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

fun JSONObject.toCountingSource() = CountingSource(getLong("sourceId"), getLong("busId"), optionalString("label"),
    getString("status"), Instant.parse(getString("createdAt")), optionalString("rotatedAt")?.let(Instant::parse), optionalString("revokedAt")?.let(Instant::parse))
fun JSONObject.toOccupancy() = Occupancy(getLong("busId"), getString("status"), optionalString("reason"), optionalInt("count"),
    optionalInt("capacity"), optionalString("load"), optionalString("lastCountAt")?.let(Instant::parse),
    optionalString("lastHeartbeatAt")?.let(Instant::parse), Instant.parse(getString("generatedAt")))

class RemoteCountingRepository(private val api: ApiClient) : CountingRepository {
    override suspend fun sources(busId: Long): DomainResult<List<CountingSource>> = if (busId <= 0) invalid("INVALID_BUS") else
        api.call("GET", "/api/v1/buses/$busId/counting-sources") {
            val array = JSONArray(it.bytes.toString(Charsets.UTF_8))
            (0 until array.length()).map { index -> array.getJSONObject(index).toCountingSource().also { source -> require(source.busId == busId) } }
        }
    override suspend fun register(busId: Long, label: String): DomainResult<SourceCredential> =
        if (busId <= 0 || label.trim().length > 60) invalid("COUNTING_FIELDS_REQUIRED") else
            api.call("POST", "/api/v1/buses/$busId/counting-sources", jsonBody("label" to label.trim()), "application/json") {
                it.json().let { dto -> SourceCredential(dto.toCountingSource().also { source -> require(source.busId == busId) }, dto.getString("key")) }
            }
    override suspend fun rotate(busId: Long, sourceId: Long): DomainResult<SourceCredential> =
        if (busId <= 0 || sourceId <= 0) invalid("COUNTING_FIELDS_REQUIRED") else
            api.call("POST", "/api/v1/buses/$busId/counting-sources/$sourceId/rotation") {
                it.json().let { dto -> SourceCredential(dto.toCountingSource().also { source -> require(source.busId == busId && source.id == sourceId) }, dto.getString("key")) }
            }
    override suspend fun revoke(busId: Long, sourceId: Long): DomainResult<CountingSource> =
        if (busId <= 0 || sourceId <= 0) invalid("COUNTING_FIELDS_REQUIRED") else
            api.call("POST", "/api/v1/buses/$busId/counting-sources/$sourceId/revocation") {
                it.json().toCountingSource().also { source -> require(source.busId == busId && source.id == sourceId && source.status == "REVOKED") }
            }
    override suspend fun occupancy(busId: Long): DomainResult<Occupancy> = if (busId <= 0) invalid("INVALID_BUS") else
        api.call("GET", "/api/v1/buses/$busId/occupancy") { it.json().toOccupancy().also { value -> require(value.busId == busId) } }
}
