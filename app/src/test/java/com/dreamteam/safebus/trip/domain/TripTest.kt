package com.dreamteam.safebus.trip.domain

import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class TripTest {
    private val now = Instant.parse("2026-11-10T12:00:00Z")
    @Test fun rejectsImpossibleAssignmentsAndShiftIdentifiers() {
        assertThrows(IllegalArgumentException::class.java) { Assignment(1, "ASSIGNED", "ABC", "route", "A", "B", now, now) }
        assertThrows(IllegalArgumentException::class.java) { ActiveShift(0, 1, 1, now) }
        assertEquals(1L, ActiveShift(1, 2, 3, now).id)
    }
    @Test fun locationReliabilityHonorsBoundariesAndDistance() {
        val sample = Position(-12.0, -77.0, 30.0, now)
        assertTrue(sample.reliableAt(now.plusSeconds(60)))
        assertFalse(sample.reliableAt(now.plusSeconds(61)))
        assertFalse(sample.reliableAt(now.minusSeconds(1)))
        assertFalse(sample.copy(accuracy = 31.0).reliableAt(now))
        assertEquals(111.19, sample.distanceTo(sample.copy(latitude = -11.999)), 0.1)
        assertThrows(IllegalArgumentException::class.java) { sample.copy(latitude = Double.NaN) }
        assertThrows(IllegalArgumentException::class.java) { sample.copy(longitude = 181.0) }
    }
}
