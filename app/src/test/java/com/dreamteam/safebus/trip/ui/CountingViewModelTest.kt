package com.dreamteam.safebus.trip.ui

import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.trip.domain.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class CountingViewModelTest {
    @Test fun leavingDuringRegistrationNeverResurrectsTheOneTimeKey() = runBlocking {
        val source = CountingSource(1, 2, null, "ACTIVE", Instant.EPOCH, null, null)
        val response = CompletableDeferred<DomainResult<SourceCredential>>()
        val repo = object : CountingRepository {
            override suspend fun sources(busId: Long) = DomainResult.Success(listOf(source))
            override suspend fun occupancy(busId: Long) = DomainResult.Success(Occupancy(2, "UNAVAILABLE", null, null, null, null, null, null, Instant.EPOCH))
            override suspend fun register(busId: Long, label: String) = response.await()
            override suspend fun rotate(busId: Long, sourceId: Long) = response.await()
            override suspend fun revoke(busId: Long, sourceId: Long) = DomainResult.Success(source.copy(status = "REVOKED"))
        }
        val model = CountingViewModel(repo, 2, Dispatchers.Unconfined)
        model.register("source"); model.clearCredential()
        response.complete(DomainResult.Success(SourceCredential(source, "private-key")))
        yield()
        assertNull(model.state.value.value.credential); assertEquals(1, model.state.value.value.sources.size)
        model.refresh(); assertNull(model.state.value.value.credential)
    }
    @Test fun rejectedRotationKeepsSourceAndNeverDisplaysAnOldKey() {
        val source = CountingSource(1, 2, null, "ACTIVE", Instant.EPOCH, null, null)
        var writes = 0
        val repo = object : CountingRepository {
            override suspend fun sources(busId: Long) = DomainResult.Success(listOf(source))
            override suspend fun occupancy(busId: Long) = DomainResult.Success(Occupancy(2, "UNAVAILABLE", null, null, null, null, null, null, Instant.EPOCH))
            override suspend fun register(busId: Long, label: String) = DomainResult.Success(SourceCredential(source, "key")).also { writes++ }
            override suspend fun rotate(busId: Long, sourceId: Long) = DomainResult.Error(Failure.fromStatus(403))
            override suspend fun revoke(busId: Long, sourceId: Long) = DomainResult.Error(Failure.fromStatus(403))
        }
        val model = CountingViewModel(repo, 2, Dispatchers.Unconfined)
        model.refresh(); model.register("source"); assertNotNull(model.state.value.value.credential)
        model.rotate(1)
        assertEquals(FailureKind.FORBIDDEN, model.state.value.failure?.kind)
        assertNull(model.state.value.value.credential); assertEquals(listOf(source), model.state.value.value.sources); assertEquals(1, writes)
        model.revoke(1); assertEquals(FailureKind.FORBIDDEN, model.state.value.failure?.kind)
        assertEquals("ACTIVE", model.state.value.value.sources.single().status); assertEquals(1, writes)
    }
    @Test fun requiresEmptyBusConfirmationAndShowsOnlyDurablePendingEventsOnFailure() {
        var stored = CounterState()
        var writes = 0
        val repo = object : CounterRepository {
            override suspend fun restore() = DomainResult.Success(stored)
            override suspend fun submit(command: CountCommand, key: String): DomainResult<CounterState> {
                stored = stored.copy(pending = command); writes++; return DomainResult.Error(Failure(FailureKind.NETWORK))
            }
            override suspend fun retry(key: String) = DomainResult.Error(Failure.fromStatus(403))
            override suspend fun heartbeat(busId: Long, key: String) = DomainResult.Error(Failure.fromStatus(403))
        }
        val model = CounterViewModel(repo, 2, 3, Dispatchers.Unconfined)
        model.send(CountAction.INITIALIZE, 1, "key", false)
        assertEquals(FailureKind.VALIDATION, model.state.value.failure?.kind); assertEquals(0, writes); assertNull(stored.pending)
        model.send(CountAction.INITIALIZE, 1, "key", true)
        assertEquals(FailureKind.NETWORK, model.state.value.failure?.kind); assertNotNull(model.state.value.value.counter.pending)
        assertNull(model.state.value.value.counter.receipt)
        model.heartbeat("key"); assertEquals(FailureKind.FORBIDDEN, model.state.value.failure?.kind)
        assertNull(model.state.value.value.heartbeat); assertEquals(1, writes)
    }
}
