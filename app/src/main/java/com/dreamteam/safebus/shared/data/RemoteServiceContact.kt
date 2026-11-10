package com.dreamteam.safebus.shared.data

import com.dreamteam.safebus.shared.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.*
import java.time.Instant

class HttpServiceContact(private val api: ApiClient) : ServiceContact {
    override suspend fun health() = api.call("GET", "/api/v1/health", authenticated = false) {
        require(it.json().getString("status") == "UP"); true
    }
    override suspend fun contact(input: ContactSubmission): DomainResult<ContactReceipt> = if (!input.valid()) invalid("CONTACT_FIELDS_REQUIRED") else
        api.call("POST", "/api/v1/contact-requests", jsonBody("submissionId" to input.id, "companyName" to input.company.trim(),
            "contactName" to input.name.trim(), "email" to input.email.trim(), "consent" to input.consent), "application/json", authenticated = false) {
            val dto = it.json(); ContactReceipt(dto.getString("receiptReference"), Instant.parse(dto.getString("receivedAt")))
        }
}
class DurableContactRepository(private val service: ServiceContact, private val store: SecureStore, endpoint: String,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO) : ContactRepository {
    private val key = "company-contact:$endpoint"
    private val mutex = Mutex()
    private fun read(): ContactState {
        val bytes = store.read(key) ?: return ContactState()
        return DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            require(input.readInt() == 1)
            val pending = if (input.readBoolean()) ContactSubmission(input.readUTF(), input.readUTF(), input.readUTF(), input.readUTF(), input.readBoolean()).also { require(it.valid()) } else null
            val receipt = if (input.readBoolean()) ContactReceipt(input.readUTF(), Instant.parse(input.readUTF())) else null
            require(input.available() == 0); ContactState(pending, receipt)
        }
    }
    private fun write(state: ContactState) {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeInt(1); out.writeBoolean(state.pending != null)
            state.pending?.let { out.writeUTF(it.id); out.writeUTF(it.company); out.writeUTF(it.name); out.writeUTF(it.email); out.writeBoolean(it.consent) }
            out.writeBoolean(state.receipt != null)
            state.receipt?.let { out.writeUTF(it.reference); out.writeUTF(it.receivedAt.toString()) }
        }
        store.write(key, bytes.toByteArray())
    }
    private suspend fun <T> locked(action: suspend () -> DomainResult<T>): DomainResult<T> = withContext(dispatcher) {
        mutex.withLock {
            try { action() } catch (e: CancellationException) { throw e }
            catch (_: Exception) { DomainResult.Error(Failure(FailureKind.STORAGE)) }
        }
    }
    override suspend fun restore() = locked { DomainResult.Success(read()) }
    override suspend fun submit(input: ContactSubmission): DomainResult<ContactState> = locked {
        if (!input.valid()) return@locked invalid("CONTACT_FIELDS_REQUIRED")
        val old = read()
        if (old.receipt != null) return@locked DomainResult.Success(old)
        if (old.pending != null && old.pending != input) return@locked DomainResult.Error(Failure(FailureKind.CONFLICT, "CONTACT_PENDING"))
        write(ContactState(pending = input))
        when (val result = service.contact(input)) {
            is DomainResult.Success -> DomainResult.Success(ContactState(receipt = result.value).also(::write))
            is DomainResult.Error -> {
                if (!result.failure.retryable) store.remove(key)
                result
            }
        }
    }
    override suspend fun clear() = locked {
        if (read().pending != null) DomainResult.Error(Failure(FailureKind.CONFLICT, "CONTACT_PENDING"))
        else { store.remove(key); DomainResult.Success(ContactState()) }
    }
}
