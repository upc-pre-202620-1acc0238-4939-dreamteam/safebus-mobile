package com.dreamteam.safebus.fleet.domain

import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.trip.domain.Position
import java.time.Instant

data class FleetBus(val id: Long, val plate: String, val enabled: Boolean, val capacity: Int?, val shiftId: Long?,
    val route: String?, val driver: String?, val locationStatus: String, val position: Position?, val occupancyStatus: String,
    val occupancyReason: String?, val count: Int?, val load: String?, val emergencyCount: Int, val pendingGroups: Int) {
    init { require(id > 0 && (capacity == null || capacity > 0) && (count == null || count >= 0)) }
}
data class FleetOverview(val buses: List<FleetBus>, val updatedAt: Instant)
data class ProvisionReceipt(val id: Long, val credential: String? = null, val expiresAt: Instant? = null) {
    init { require(id > 0) }
    override fun toString() = "ProvisionReceipt(id=$id)"
}

sealed interface FleetChange {
    fun valid(): Boolean
    data class Bus(val plate: String) : FleetChange { override fun valid() = plate.trim().isNotEmpty() }
    data class Driver(val name: String, val login: String, val password: String) : FleetChange {
        override fun valid() = name.isNotBlank() && login.isNotBlank() && password.length >= 8
        override fun toString() = "Driver(redacted)"
    }
    data class Route(val name: String, val origin: String, val destination: String) : FleetChange {
        override fun valid() = name.isNotBlank() && origin.isNotBlank() && destination.isNotBlank()
    }
    data class Assignment(val driverId: Long, val busId: Long, val routeId: Long, val startsAt: Instant, val endsAt: Instant) : FleetChange {
        override fun valid() = driverId > 0 && busId > 0 && routeId > 0 && endsAt > startsAt
    }
    data class Capacity(val busId: Long, val capacity: Int, val reference: String) : FleetChange {
        override fun valid() = busId > 0 && capacity > 0 && reference.isNotBlank()
    }
}
interface FleetRepository {
    suspend fun overview(): DomainResult<FleetOverview>
    suspend fun save(change: FleetChange): DomainResult<ProvisionReceipt>
}
class SaveFleetResource(private val repository: FleetRepository) {
    suspend operator fun invoke(change: FleetChange): DomainResult<ProvisionReceipt> =
        if (change.valid()) repository.save(change) else invalid("FLEET_FIELDS_REQUIRED")
}
