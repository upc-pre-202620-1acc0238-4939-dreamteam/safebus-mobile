package com.dreamteam.safebus.safetycase.ui

import com.dreamteam.safebus.safetycase.domain.*
import com.dreamteam.safebus.shared.domain.*
import kotlinx.coroutines.Dispatchers
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

open class SafetyFake : SafetyRepository {
    var failure: Failure? = null
    var writes = 0
    private fun <T> answer(value: T): DomainResult<T> = failure?.let { DomainResult.Error(it) } ?: DomainResult.Success(value)
    override suspend fun panic(shiftId: Long) = answer(Receipt("00000000-0000-0000-0000-000000000001", Delivery.PENDING))
    override suspend fun driverAlerts() = answer(emptyList<Emergency>())
    override suspend fun request(journeyId: Long, message: String, photo: Photo): DomainResult<Receipt> {
        if (failure == null) writes++
        return answer(Receipt("00000000-0000-0000-0000-000000000002", Delivery.ACKNOWLEDGED))
    }
    override suspend fun mine() = answer(emptyList<PassengerRequest>())
    override suspend fun history(busId: Long) = answer(emptyList<SafetyEntry>())
    override suspend fun evidence(requestId: String, supervisor: Boolean) = answer(Photo(byteArrayOf(1), "image/png"))
    override suspend fun queue() = answer(ReviewQueue(emptyList(), emptyList(), Instant.EPOCH))
    override suspend fun approve(groupId: Long) = answer(Unit).also { if (failure == null) writes++ }
    override suspend fun decline(groupId: Long, reason: String, response: String) = answer(Unit)
    override suspend fun attend(id: String) = answer(Unit)
    override suspend fun close(id: String, outcome: String, response: String) = answer(Unit)
}

class SafetyViewModelTest {
    @Test fun requiresEvidenceAndReflectsRejectedRequestsWithoutSaving() {
        val repo = SafetyFake(); val model = SafetyViewModel(repo, Dispatchers.Unconfined)
        model.submit(1, "Message", null)
        assertEquals(FailureKind.VALIDATION, model.state.value.failure?.kind)
        assertNull(model.state.value.value.receipt); assertEquals(0, repo.writes)
        repo.failure = Failure.fromStatus(409)
        model.submit(1, "Message", Photo(byteArrayOf(1), "image/png"))
        assertEquals(FailureKind.CONFLICT, model.state.value.failure?.kind)
        assertNull(model.state.value.value.receipt); assertEquals(0, repo.writes)
        repo.failure = null; model.submit(1, "Message", Photo(byteArrayOf(1), "image/png"))
        assertEquals(Delivery.ACKNOWLEDGED, model.state.value.value.receipt?.delivery)
        assertEquals(1, repo.writes)
    }
    @Test fun failedReviewDoesNotClaimADecisionAndPanicDoesNotWaitForApproval() {
        val repo = SafetyFake(); val model = SafetyViewModel(repo, Dispatchers.Unconfined)
        repo.failure = Failure.fromStatus(403); model.approve(1)
        assertEquals(FailureKind.FORBIDDEN, model.state.value.failure?.kind)
        assertFalse(model.state.value.value.decisionSaved); assertEquals(0, repo.writes)
        repo.failure = null; model.approve(1)
        assertTrue(model.state.value.value.decisionSaved); assertEquals(1, repo.writes)
        val panic = PanicViewModel(repo, Dispatchers.Unconfined)
        panic.panic(1)
        assertEquals(Delivery.PENDING, panic.state.value.value?.delivery)
    }
}
