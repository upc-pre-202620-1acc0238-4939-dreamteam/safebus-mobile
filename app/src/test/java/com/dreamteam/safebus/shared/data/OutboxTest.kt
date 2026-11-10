package com.dreamteam.safebus.shared.data

import com.dreamteam.safebus.iam.data.MemoryStore
import com.dreamteam.safebus.shared.domain.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class OutboxTest {
    private val store = MemoryStore()
    private val request = PendingRequest("event-1", "/api/v1/driver-emergencies", "original timestamp".toByteArray(), "application/json")
    private var answer: DomainResult<Unit> = DomainResult.Error(Failure(FailureKind.NETWORK))
    private val sent = mutableListOf<PendingRequest>()
    private fun box(owner: String = "account-1") = Outbox(store, owner, PendingTransport { sent.add(it); answer }, Dispatchers.Unconfined)
    @Test fun schedulesBackgroundDeliveryOnlyAfterDurableWrite() = runBlocking {
        var scheduled = 0
        val box = Outbox(store, "owner", PendingTransport { answer }, Dispatchers.Unconfined, { assertTrue(store.values.isNotEmpty()); scheduled++ })
        store.failWrites = true
        assertTrue(box.submit(request) is DomainResult.Error)
        assertEquals(0, scheduled); assertTrue(store.values.isEmpty())
        store.failWrites = false; box.submit(request)
        assertEquals(1, scheduled)
    }
    @Test fun repeatedJourneyClosurePreservesOriginalTimestampAndReason() = runBlocking {
        val end = request.copy(id = "end:1", path = "/api/v1/journeys/1/end")
        box().submit(end, preserveExistingEnd = true)
        answer = DomainResult.Success(Unit)
        assertEquals(Delivery.ACKNOWLEDGED, (box().submit(end.copy(body = "new timestamp".toByteArray()), preserveExistingEnd = true) as DomainResult.Success).value)
        assertArrayEquals(end.body, sent.last().body)
        assertTrue(store.values.isEmpty())
    }
    @Test fun retriesExactOriginalPayloadAcrossRestartAndIsolatesAccounts() = runBlocking {
        assertEquals(Delivery.PENDING, (box().submit(request) as DomainResult.Success).value)
        assertEquals(0, (box("account-2").count() as DomainResult.Success).value)
        answer = DomainResult.Success(Unit)
        assertEquals(0, (box().flush() as DomainResult.Success).value)
        assertEquals(2, sent.size)
        assertArrayEquals(sent[0].body, sent[1].body)
        assertEquals(sent[0].id, sent[1].id)
        assertTrue(store.values.isEmpty())
    }
    @Test fun rejectsInvalidDataAndNeverQueuesNonIdempotentOperations() = runBlocking {
        for (status in listOf(400, 403, 404, 409, 422)) {
            answer = DomainResult.Error(Failure.fromStatus(status))
            assertEquals(answer, box().submit(request))
            assertTrue(store.values.isEmpty())
        }
        assertThrows(IllegalArgumentException::class.java) { request.copy(path = "/api/v1/shifts/activate") }
        Unit
    }
    @Test fun storageFailureDoesNotSendAndIdReuseDoesNotOverwriteOriginal() = runBlocking {
        store.failWrites = true
        assertEquals(FailureKind.STORAGE, (box().submit(request) as DomainResult.Error).failure.kind)
        assertTrue(sent.isEmpty()); assertTrue(store.values.isEmpty())
        store.failWrites = false
        box().submit(request)
        assertTrue(box().submit(request.copy(body = byteArrayOf(1))) is DomainResult.Error)
        assertEquals(1, (box().count() as DomainResult.Success).value)
        answer = DomainResult.Success(Unit)
        box().flush()
        assertArrayEquals(request.body, sent.last().body)
    }
}
