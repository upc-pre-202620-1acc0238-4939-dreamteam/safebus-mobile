package com.dreamteam.safebus.shared.ui

import com.dreamteam.safebus.shared.domain.*
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class ServiceViewModelTest {
    @Test fun failureRemovesHealthyIndicatorAndNeverInventsContactReceipt() {
        var fail = false
        var writes = 0
        val service = object : ServiceContact {
            override suspend fun health() = if (fail) DomainResult.Error(Failure(FailureKind.NETWORK)) else DomainResult.Success(true)
            override suspend fun contact(input: ContactSubmission) = DomainResult.Error(Failure.fromStatus(422))
        }
        val repository = object : ContactRepository {
            override suspend fun restore() = DomainResult.Success(ContactState())
            override suspend fun submit(input: ContactSubmission): DomainResult<ContactState> { return DomainResult.Error(Failure.fromStatus(422)) }
            override suspend fun clear() = DomainResult.Success(ContactState()).also { writes++ }
        }
        val model = ServiceViewModel(service, repository, Dispatchers.Unconfined)
        model.health(); assertTrue(model.state.value.value.available)
        fail = true; model.health()
        assertEquals(FailureKind.NETWORK, model.state.value.failure?.kind); assertFalse(model.state.value.value.available); assertEquals(0, writes)
        model.submit(ContactSubmission(UUID.randomUUID().toString(), "Company", "Name", "name@example.com", true))
        assertEquals(FailureKind.VALIDATION, model.state.value.failure?.kind)
        assertNull(model.state.value.value.contact.receipt); assertNull(model.state.value.value.contact.pending); assertEquals(0, writes)
    }
}
