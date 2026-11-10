package com.dreamteam.safebus.trip.data

import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.trip.domain.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.util.UUID

class DurableCounterRepositoryTest {
    @Test fun retriesOriginalEventAfterRestartAndPreservesConfirmedCountOnRejection() = runBlocking {
        val records = mutableMapOf<String, ByteArray>()
        val store = object : SecureStore {
            override fun read(key: String) = records[key]
            override fun write(key: String, value: ByteArray) { records[key] = value }
            override fun remove(key: String) { records.remove(key) }
        }
        val sent = mutableListOf<CountCommand>()
        var failure: Failure? = Failure(FailureKind.NETWORK)
        val service = object : CounterService {
            override suspend fun send(command: CountCommand, key: String): DomainResult<CountReceipt> {
                sent.add(command)
                return failure?.let { DomainResult.Error(it) } ?: DomainResult.Success(CountReceipt(1, 2, null))
            }
            override suspend fun heartbeat(busId: Long, key: String) = DomainResult.Success(Instant.EPOCH)
        }
        fun repo(owner: String = "one") = DurableCounterRepository(service, store, owner, 1, 2, Dispatchers.Unconfined)
        fun state() = (runBlocking { repo().restore() } as DomainResult.Success).value
        val command = CountCommand(UUID.randomUUID().toString(), 1, 2, CountAction.ENTRY, 1, Instant.EPOCH)
        assertTrue(repo().submit(command, "private-key") is DomainResult.Error)
        assertEquals(command, state().pending); assertNull(state().receipt)
        assertNull((repo("other").restore() as DomainResult.Success).value.pending)
        assertNull((repo("one:replacement-source").restore() as DomainResult.Success).value.pending)
        assertFalse(records.values.any { it.toString(Charsets.UTF_8).contains("private-key") })
        failure = null
        assertTrue(repo().retry("private-key") is DomainResult.Success)
        assertEquals(listOf(command, command), sent); assertNull(state().pending); assertEquals(1, state().receipt?.count)
        failure = Failure.fromStatus(422, "NEGATIVE_COUNT")
        assertTrue(repo().submit(command.copy(id = UUID.randomUUID().toString(), sequence = 2), "private-key") is DomainResult.Error)
        assertNull(state().pending); assertEquals(1, state().receipt?.count)
    }
}
