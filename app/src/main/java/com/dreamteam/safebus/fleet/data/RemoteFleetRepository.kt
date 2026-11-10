package com.dreamteam.safebus.fleet.data

import com.dreamteam.safebus.fleet.domain.*
import com.dreamteam.safebus.shared.data.*
import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.trip.domain.Position
import org.json.JSONObject
import java.time.Instant

fun JSONObject.toFleetBus(): FleetBus {
    val location = getJSONObject("location"); val occupancy = getJSONObject("occupancy")
    val latitude = location.optionalDouble("latitude"); val longitude = location.optionalDouble("longitude")
    val accuracy = location.optionalDouble("accuracyMeters"); val captured = location.optionalString("capturedAt")
    val position = if (latitude != null && longitude != null && accuracy != null && captured != null)
        Position(latitude, longitude, accuracy, Instant.parse(captured)) else null
    return FleetBus(getLong("busId"), getString("plate"), getBoolean("enabled"), optionalInt("capacity"),
        optJSONObject("shift")?.optionalLong("shiftId"), optJSONObject("route")?.optionalString("name"), optJSONObject("driver")?.optionalString("fullName"),
        location.getString("status"), position, occupancy.getString("status"), occupancy.optionalString("reason"),
        occupancy.optionalInt("count"), occupancy.optionalString("load"), getJSONArray("emergencies").length(),
        objects("passengerGroups").count { it.getString("status") == "AWAITING_APPROVAL" },
        occupancy.optionalString("lastCountAt")?.let(Instant::parse), occupancy.optionalString("lastHeartbeatAt")?.let(Instant::parse),
        objects("passengerGroups").map { GroupCount(it.getString("status"), it.getInt("requestCount"), it.getInt("attachedCount")) })
}

class RemoteFleetRepository(private val api: ApiClient) : FleetRepository {
    override suspend fun overview() = api.call("GET", "/api/v1/fleet/overview") {
        val dto = it.json()
        FleetOverview(dto.objects("buses").map(JSONObject::toFleetBus), Instant.parse(dto.getString("generatedAt")))
    }
    override suspend fun save(change: FleetChange): DomainResult<ProvisionReceipt> {
        if (!change.valid()) return invalid("FLEET_FIELDS_REQUIRED")
        val (path, body) = when (change) {
            is FleetChange.Bus -> "/buses" to jsonBody("plate" to change.plate.trim())
            is FleetChange.Driver -> "/drivers" to jsonBody("fullName" to change.name.trim(), "loginId" to change.login.trim(), "initialPassword" to change.password)
            is FleetChange.Route -> "/routes" to jsonBody("name" to change.name.trim(), "origin" to change.origin.trim(), "destination" to change.destination.trim())
            is FleetChange.Assignment -> "/shift-assignments" to jsonBody("driverId" to change.driverId, "busId" to change.busId, "routeId" to change.routeId,
                "plannedStart" to change.startsAt.toString(), "plannedEnd" to change.endsAt.toString())
            is FleetChange.Capacity -> "/buses/${change.busId}/capacity" to jsonBody("capacity" to change.capacity, "technicalRecordReference" to change.reference.trim())
        }
        return api.call(if (change is FleetChange.Capacity) "PUT" else "POST", "/api/v1$path", body, "application/json") {
            val dto = it.json()
            ProvisionReceipt(dto.getLong(if (change is FleetChange.Capacity) "busId" else "id"),
                dto.optionalString("qrCredential") ?: dto.optionalString("qrCode"), dto.optionalString("qrCredentialExpiresAt")?.let(Instant::parse))
        }
    }
}
