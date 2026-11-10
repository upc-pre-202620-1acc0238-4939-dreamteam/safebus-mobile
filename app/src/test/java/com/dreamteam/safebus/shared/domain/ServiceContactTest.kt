package com.dreamteam.safebus.shared.domain

import java.util.UUID
import org.junit.Assert.*
import org.junit.Test

class ServiceContactTest {
    @Test fun requiresValidContactAndConsentWithoutLoggingPersonalData() {
        val input = ContactSubmission(UUID.randomUUID().toString(), "Bus company", "Name", "name@example.com", true)
        assertTrue(input.valid()); assertFalse(input.toString().contains(input.email))
        assertFalse(input.copy(consent = false).valid()); assertFalse(input.copy(email = "invalid").valid())
        assertFalse(input.copy(company = " ").valid()); assertFalse(input.copy(name = "x".repeat(101)).valid())
        assertFalse(input.copy(id = "invalid").valid())
    }
}
