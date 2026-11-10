package com.dreamteam.safebus.safetycase.domain

import com.dreamteam.safebus.shared.domain.*
import java.time.Instant
import java.util.UUID

data class Emergency(val id: String, val status: String, val priority: String, val busId: Long?,
    val activatedAt: Instant, val response: String? = null, val responsibleSupervisorId: Long? = null) {
    init { UUID.fromString(id); require(busId == null || busId > 0) }
}
data class PassengerRequest(val id: String, val message: String, val status: String, val submittedAt: Instant,
    val contributorCount: Int?, val groupReference: String?, val decision: String?, val response: String?) {
    init { UUID.fromString(id); require(contributorCount == null || contributorCount >= 0) }
}
data class ReviewGroup(val id: Long, val reference: String, val busId: Long, val contributors: Int, val requests: List<PassengerRequest>) {
    init { require(id > 0 && busId > 0 && contributors >= 0) }
}
data class ReviewQueue(val emergencies: List<Emergency>, val groups: List<ReviewGroup>, val updatedAt: Instant)
data class SafetyEntry(val reference: String, val source: String, val state: String, val recordedAt: Instant, val count: Int?)
data class Receipt(val id: String, val delivery: Delivery)

interface SafetyRepository {
    suspend fun panic(shiftId: Long): DomainResult<Receipt>
    suspend fun driverAlerts(): DomainResult<List<Emergency>>
    suspend fun request(journeyId: Long, message: String, photo: Photo): DomainResult<Receipt>
    suspend fun mine(): DomainResult<List<PassengerRequest>>
    suspend fun history(busId: Long): DomainResult<List<SafetyEntry>>
    suspend fun evidence(requestId: String, supervisor: Boolean): DomainResult<Photo>
    suspend fun queue(): DomainResult<ReviewQueue>
    suspend fun approve(groupId: Long): DomainResult<Unit>
    suspend fun decline(groupId: Long, reason: String, response: String): DomainResult<Unit>
    suspend fun attend(id: String): DomainResult<Unit>
    suspend fun close(id: String, outcome: String, response: String): DomainResult<Unit>
}

class SubmitRequest(private val repository: SafetyRepository) {
    suspend operator fun invoke(journeyId: Long?, message: String, photo: Photo?): DomainResult<Receipt> =
        if (journeyId == null || journeyId <= 0 || message.trim().length !in 1..500 || photo == null) invalid("REQUEST_INVALID")
        else repository.request(journeyId, message.trim(), photo)
}
