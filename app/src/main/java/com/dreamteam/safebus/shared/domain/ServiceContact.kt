package com.dreamteam.safebus.shared.domain

import java.time.Instant
import java.util.UUID

data class ContactSubmission(val id: String, val company: String, val name: String, val email: String, val consent: Boolean) {
    fun valid(): Boolean = runCatching { UUID.fromString(id).toString() == id }.getOrDefault(false) &&
        company.trim().length in 1..100 && name.trim().length in 1..100 && email.trim().length <= 254 &&
        email.trim().matches(Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) && consent
    override fun toString() = "ContactSubmission(redacted)"
}
data class ContactReceipt(val reference: String, val receivedAt: Instant) { init { require(reference.isNotBlank()) } }
data class ContactState(val pending: ContactSubmission? = null, val receipt: ContactReceipt? = null)
interface ServiceContact {
    suspend fun health(): DomainResult<Boolean>
    suspend fun contact(input: ContactSubmission): DomainResult<ContactReceipt>
}
interface ContactRepository {
    suspend fun restore(): DomainResult<ContactState>
    suspend fun submit(input: ContactSubmission): DomainResult<ContactState>
    suspend fun clear(): DomainResult<ContactState>
}
