package com.dreamteam.safebus.shared.data

import com.dreamteam.safebus.shared.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.*

data class PendingRequest(val id: String, val path: String, val body: ByteArray, val contentType: String) {
    init {
        require(id.isNotBlank() && body.size <= 6 * 1024 * 1024)
        require(path in setOf("/api/v1/location-events", "/api/v1/driver-emergencies", "/api/v1/passenger-requests") ||
            path.matches(Regex("/api/v1/journeys/[0-9]+/end")))
    }
    override fun toString() = "PendingRequest(redacted)"
}

fun interface PendingTransport { suspend fun send(request: PendingRequest): DomainResult<Unit> }

class Outbox(private val store: SecureStore, owner: String, private val transport: PendingTransport,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO) {
    private val key = "outbox:$owner"
    private val lock = Mutex()
    private fun read(): List<PendingRequest> = store.read(key)?.let { bytes ->
        DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            require(input.readInt() == 1)
            val count = input.readInt().also { require(it in 0..100) }
            List(count) {
                val id = input.readUTF(); val path = input.readUTF(); val type = input.readUTF()
                val size = input.readInt().also { require(it in 0..6 * 1024 * 1024) }
                PendingRequest(id, path, ByteArray(size).also(input::readFully), type)
            }.also { require(input.available() == 0) }
        }
    } ?: emptyList()
    private fun save(items: List<PendingRequest>) {
        if (items.isEmpty()) { store.remove(key); return }
        require(items.size <= 100 && items.sumOf { it.body.size.toLong() } <= 20 * 1024 * 1024)
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { output ->
            output.writeInt(1); output.writeInt(items.size)
            items.forEach { item ->
                output.writeUTF(item.id); output.writeUTF(item.path); output.writeUTF(item.contentType)
                output.writeInt(item.body.size); output.write(item.body)
            }
        }
        store.write(key, bytes.toByteArray())
    }
    suspend fun count(): DomainResult<Int> = guarded { DomainResult.Success(read().size) }
    suspend fun submit(request: PendingRequest, preserveExistingEnd: Boolean = false): DomainResult<Delivery> = guarded {
        val items = read()
        val existing = items.find { it.id == request.id }
        if (preserveExistingEnd && !request.path.matches(Regex("/api/v1/journeys/[0-9]+/end"))) return@guarded invalid("INVALID_END_RETRY")
        if (preserveExistingEnd && existing?.path == request.path) return@guarded deliver(existing)
        if (existing != null && (existing.path != request.path || existing.contentType != request.contentType || !existing.body.contentEquals(request.body))) {
            return@guarded invalid("REQUEST_ID_REUSED")
        }
        if (existing == null) save(items + request)
        deliver(existing ?: request)
    }
    suspend fun flush(): DomainResult<Int> = guarded {
        for (request in read()) {
            when (val result = deliver(request)) {
                is DomainResult.Error -> return@guarded result
                is DomainResult.Success -> if (result.value == Delivery.PENDING) break
            }
        }
        DomainResult.Success(read().size)
    }
    private suspend fun deliver(request: PendingRequest): DomainResult<Delivery> {
        return when (val result = transport.send(request)) {
            is DomainResult.Success -> {
                save(read().filterNot { it.id == request.id })
                DomainResult.Success(Delivery.ACKNOWLEDGED)
            }
            is DomainResult.Error -> when {
                result.failure.retryable -> DomainResult.Success(Delivery.PENDING)
                result.failure.kind == FailureKind.UNAUTHORIZED -> result
                else -> { save(read().filterNot { it.id == request.id }); result }
            }
        }
    }
    private suspend fun <T> guarded(block: suspend () -> DomainResult<T>): DomainResult<T> = withContext(dispatcher) {
        lock.withLock {
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { DomainResult.Error(Failure(FailureKind.STORAGE)) }
        }
    }
}
