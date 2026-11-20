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
    override suspend fun evidence(requestId: String) = answer(Photo(byteArrayOf(1), "image/png"))
    override suspend fun queue() = answer(ReviewQueue(emptyList(), emptyList(), Instant.EPOCH))
    override suspend fun approve(groupId: Long) = answer(Unit).also { if (failure == null) writes++ }
    override suspend fun decline(groupId: Long, reason: String, response: String) = answer(Unit)
    override suspend fun attend(id: String) = answer(Unit)
    override suspend fun close(id: String, outcome: String, response: String) = answer(Unit)
}

class SafetyViewModelTest {
    @Test fun deniedSupervisorEvidenceClearsPreviousPhotoWithoutPersistingInvalidData() {
        val repo = SafetyFake()
        val model = SafetyViewModel(repo, Dispatchers.Unconfined)
        model.evidence("own"); assertNotNull(model.state.value.value.evidence)
        repo.failure = Failure.fromStatus(403); model.evidence("foreign")
        assertEquals(FailureKind.FORBIDDEN, model.state.value.failure?.kind)
        assertNull(model.state.value.value.evidence); assertEquals(0, repo.writes)
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
