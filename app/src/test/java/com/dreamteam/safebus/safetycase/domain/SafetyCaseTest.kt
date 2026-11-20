package com.dreamteam.safebus.safetycase.domain

import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class SafetyCaseTest {
    @Test fun validatesEmergencyIdentifiersAndReviewContributors() {
        assertThrows(IllegalArgumentException::class.java) { Emergency("bad", "ACTIVE", "CRITICAL", 1, Instant.EPOCH) }
        assertThrows(IllegalArgumentException::class.java) { ReviewGroup(1, "G1", 1, -1, emptyList()) }
        val request = PassengerRequest("00000000-0000-0000-0000-000000000001", "Flutter report", "AWAITING_APPROVAL", Instant.EPOCH, 3, "G1", null, null)
        assertEquals(request, ReviewGroup(1, "G1", 1, 3, listOf(request)).requests.single())
    }
}
