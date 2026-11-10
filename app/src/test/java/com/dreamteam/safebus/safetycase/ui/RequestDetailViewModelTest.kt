package com.dreamteam.safebus.safetycase.ui

import com.dreamteam.safebus.safetycase.domain.*
import com.dreamteam.safebus.shared.domain.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.util.UUID

class RequestDetailViewModelTest {
    @Test fun forbiddenRefreshClearsPrivateDetailAndCannotPersistAForeignRecord() {
        var fail = false
        val request = PassengerRequest(UUID.randomUUID().toString(), "private", "CLOSED", Instant.EPOCH, 3, null, null, "response")
        val repo = object : RequestDetailRepository {
            override suspend fun detail(id: String) = if (fail) DomainResult.Error(Failure.fromStatus(403)) else DomainResult.Success(request)
        }
        val model = RequestDetailViewModel(repo, Dispatchers.Unconfined)
        model.load(request.id); assertEquals(request, model.state.value.value)
        fail = true; model.load(request.id)
        assertEquals(FailureKind.FORBIDDEN, model.state.value.failure?.kind); assertNull(model.state.value.value)
    }
}
