package com.dreamteam.safebus.trip.data

import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.trip.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.*
import java.time.Instant

interface CounterService {
    suspend fun send(command: CountCommand, key: String): DomainResult<CountReceipt>
    suspend fun heartbeat(busId: Long, key: String): DomainResult<Instant>
}
class DurableCounterRepository(private val service: CounterService, private val store: SecureStore, owner: String,
    private val busId: Long, private val shiftId: Long, private val dispatcher: CoroutineDispatcher = Dispatchers.IO) : CounterRepository {
    private val storageKey = "counter:$owner:$busId:$shiftId"
    private val mutex = Mutex()
    private fun read(): CounterState {
        val bytes = store.read(storageKey) ?: return CounterState()
        return DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            require(input.readInt() == 1)
            val pending = if (input.readBoolean()) CountCommand(input.readUTF(), busId, shiftId, CountAction.valueOf(input.readUTF()), input.readLong(), Instant.parse(input.readUTF())) else null
            val receipt = if (input.readBoolean()) CountReceipt(input.readInt(), input.readLong(), input.readUTF().takeIf(String::isNotEmpty)?.let(Instant::parse)) else null
            require(input.available() == 0)
            CounterState(pending, receipt)
        }
    }
    private fun write(value: CounterState) {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { output ->
            output.writeInt(1); output.writeBoolean(value.pending != null)
            value.pending?.let { output.writeUTF(it.id); output.writeUTF(it.action.name); output.writeLong(it.sequence); output.writeUTF(it.capturedAt.toString()) }
            output.writeBoolean(value.receipt != null)
            value.receipt?.let { output.writeInt(it.count); output.writeLong(it.nextSequence); output.writeUTF(it.receivedAt?.toString().orEmpty()) }
        }
        store.write(storageKey, bytes.toByteArray())
    }
    private suspend fun <T> locked(action: suspend () -> DomainResult<T>): DomainResult<T> = withContext(dispatcher) {
        mutex.withLock {
            try { action() } catch (e: CancellationException) { throw e }
            catch (_: Exception) { DomainResult.Error(Failure(FailureKind.STORAGE)) }
        }
    }
    override suspend fun restore() = locked { DomainResult.Success(read()) }
    override suspend fun submit(command: CountCommand, key: String): DomainResult<CounterState> = locked {
        if (command.busId != busId || command.shiftId != shiftId || key.isBlank()) return@locked invalid("COUNTING_FIELDS_REQUIRED")
        val state = read()
        if (state.pending != null) return@locked DomainResult.Error(Failure(FailureKind.CONFLICT, "COUNT_PENDING"))
        val pending = state.copy(pending = command)
        write(pending)
        deliver(pending, key)
    }
    override suspend fun retry(key: String): DomainResult<CounterState> = locked {
        if (key.isBlank()) invalid("COUNTING_FIELDS_REQUIRED") else deliver(read(), key)
    }
    private suspend fun deliver(state: CounterState, key: String): DomainResult<CounterState> {
        val command = state.pending ?: return DomainResult.Success(state)
        return when (val result = service.send(command, key)) {
            is DomainResult.Success -> DomainResult.Success(CounterState(receipt = result.value).also(::write))
            is DomainResult.Error -> {
                if (!result.failure.retryable && result.failure.kind !in setOf(FailureKind.UNAUTHORIZED, FailureKind.FORBIDDEN)) write(state.copy(pending = null))
                result
            }
        }
    }
    override suspend fun heartbeat(busId: Long, key: String): DomainResult<Instant> =
        if (busId != this.busId || key.isBlank()) invalid("COUNTING_FIELDS_REQUIRED") else service.heartbeat(busId, key)
}
