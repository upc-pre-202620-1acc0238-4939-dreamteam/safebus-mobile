package com.dreamteam.safebus.shared.ui

import com.dreamteam.safebus.shared.domain.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class TaskViewModelTest {
    private class Model : TaskViewModel<List<String>>(emptyList(), Dispatchers.Unconfined) {
        fun perform(action: suspend (List<String>) -> DomainResult<List<String>>) = execute(action)
    }
    @Test fun failureRemovesLoadingAndPreservesOnlyAcknowledgedData() {
        val model = Model()
        model.perform { DomainResult.Success(listOf("acknowledged")) }
        for (kind in FailureKind.entries) {
            model.perform { DomainResult.Error(Failure(kind)) }
            assertFalse(model.state.value.busy)
            assertEquals(kind, model.state.value.failure?.kind)
            assertEquals(listOf("acknowledged"), model.state.value.value)
        }
    }
    @Test fun ignoresDuplicateActionsWhileInFlightAndClearsErrorOnRetry() = runBlocking {
        val model = Model()
        val response = CompletableDeferred<DomainResult<List<String>>>()
        model.perform { response.await() }
        assertTrue(model.state.value.busy)
        model.perform { fail("Duplicate request"); DomainResult.Success(emptyList()) }
        response.complete(DomainResult.Error(Failure(FailureKind.NETWORK)))
        assertEquals(FailureKind.NETWORK, model.state.value.failure?.kind)
        assertTrue(model.state.value.value.isEmpty())
        model.perform { DomainResult.Success(listOf("retry")) }
        assertNull(model.state.value.failure)
        assertEquals(listOf("retry"), model.state.value.value)
    }
}
