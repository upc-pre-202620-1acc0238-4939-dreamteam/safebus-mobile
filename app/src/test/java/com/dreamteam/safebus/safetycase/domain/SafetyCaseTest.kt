package com.dreamteam.safebus.safetycase.domain

import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class SafetyCaseTest {
    @Test fun validatesIdentifiersAndKeepsPublicHistoryRedactedByConstruction() {
        assertThrows(IllegalArgumentException::class.java) { Emergency("bad", "ACTIVE", "CRITICAL", 1, Instant.EPOCH) }
        assertThrows(IllegalArgumentException::class.java) { ReviewGroup(1, "G1", 1, -1, emptyList()) }
        val entry = SafetyEntry("G1", "PASSENGER", "COLLECTING", Instant.EPOCH, 2)
        assertEquals(2, entry.count)
        val fields = SafetyEntry::class.java.declaredFields.map { it.name }
        assertFalse(fields.any { it in setOf("message", "photo", "dni", "passengerId") })
    }
}
