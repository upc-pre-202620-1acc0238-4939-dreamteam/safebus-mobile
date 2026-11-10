package com.dreamteam.safebus.shared.data

import com.dreamteam.safebus.shared.domain.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.util.UUID

class DurableContactRepositoryTest {
    @Test fun persistsOriginalSubmissionForRetryAndNeverStoresRejectedInput() = runBlocking {
        val records = mutableMapOf<String, ByteArray>()
        val store = object : SecureStore {
            override fun read(key: String) = records[key]
            override fun write(key: String, value: ByteArray) { records[key] = value }
            override fun remove(key: String) { records.remove(key) }
        }
        val sent = mutableListOf<ContactSubmission>()
        var failure: Failure? = Failure(FailureKind.NETWORK)
        val service = object : ServiceContact {
            override suspend fun health() = DomainResult.Success(true)
            override suspend fun contact(input: ContactSubmission): DomainResult<ContactReceipt> {
                sent.add(input); return failure?.let { DomainResult.Error(it) } ?: DomainResult.Success(ContactReceipt("receipt", Instant.EPOCH))
            }
        }
        fun repo() = DurableContactRepository(service, store, "endpoint", Dispatchers.Unconfined)
        val input = ContactSubmission(UUID.randomUUID().toString(), "Company", "Name", "name@example.com", true)
        assertTrue(repo().submit(input.copy(consent = false)) is DomainResult.Error)
        assertTrue(records.isEmpty()); assertTrue(sent.isEmpty())
        assertTrue(repo().submit(input) is DomainResult.Error)
        assertEquals(input, (repo().restore() as DomainResult.Success).value.pending)
        assertTrue(repo().clear() is DomainResult.Error); assertEquals(1, sent.size)
        failure = null
        assertEquals("receipt", (repo().submit(input) as DomainResult.Success).value.receipt?.reference)
        assertEquals(listOf(input, input), sent)
        repo().clear(); failure = Failure.fromStatus(422)
        assertTrue(repo().submit(input) is DomainResult.Error); assertTrue(records.isEmpty())
    }
}
