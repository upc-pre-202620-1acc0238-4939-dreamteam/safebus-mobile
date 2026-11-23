package com.dreamteam.safebus.fleet.domain

import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class FleetMetricsTest {
    @Test fun emptyFleetProducesZeroedMetrics() {
        val overview = FleetOverview(emptyList(), Instant.EPOCH)
        val metrics = overview.metrics()

        assertEquals(0, metrics.totalBuses)
        assertEquals(0, metrics.activeBuses)
        assertEquals(0, metrics.overCapacityCount)
        assertEquals(0, metrics.attentionNeededCount)
        assertEquals(0f, metrics.inServiceRate, 0.001f)
    }

    @Test fun calculatesActiveAndOverCapacityBusesCorrectly() {
        val bus1 = FleetBus(
            id = 1, plate = "ABC-123", enabled = true, capacity = 40, shiftId = 10,
            route = "Route A", driver = "Driver 1", locationStatus = "CURRENT",
            position = null, occupancyStatus = "NORMAL", occupancyReason = null,
            count = 20, load = "NORMAL", emergencyCount = 0, pendingGroups = 0
        )
        val bus2 = FleetBus(
            id = 2, plate = "DEF-456", enabled = true, capacity = 30, shiftId = 11,
            route = "Route B", driver = "Driver 2", locationStatus = "AVAILABLE",
            position = null, occupancyStatus = "FULL", occupancyReason = null,
            count = 35, load = "OVER_CAPACITY", emergencyCount = 0, pendingGroups = 0
        )
        val bus3 = FleetBus(
            id = 3, plate = "GHI-789", enabled = false, capacity = null, shiftId = null,
            route = null, driver = null, locationStatus = "UNAVAILABLE",
            position = null, occupancyStatus = "UNKNOWN", occupancyReason = null,
            count = null, load = null, emergencyCount = 0, pendingGroups = 0
        )

        val overview = FleetOverview(listOf(bus1, bus2, bus3), Instant.now())
        val metrics = overview.metrics()

        assertEquals(3, metrics.totalBuses)
        assertEquals(2, metrics.activeBuses)
        assertEquals(1, metrics.overCapacityCount)
        assertEquals(1, metrics.attentionNeededCount)
        assertEquals(2f / 3f, metrics.inServiceRate, 0.001f)
    }

    @Test fun countsBusesNeedingAttentionWhenEmergenciesOrPendingReviewsOccur() {
        val busWithEmergency = FleetBus(
            id = 10, plate = "EMG-001", enabled = true, capacity = 50, shiftId = 20,
            route = "Line 1", driver = "Driver A", locationStatus = "CURRENT",
            position = null, occupancyStatus = "NORMAL", occupancyReason = null,
            count = 25, load = "NORMAL", emergencyCount = 2, pendingGroups = 0
        )
        val busWithReview = FleetBus(
            id = 11, plate = "REV-002", enabled = true, capacity = 40, shiftId = 21,
            route = "Line 2", driver = "Driver B", locationStatus = "CURRENT",
            position = null, occupancyStatus = "NORMAL", occupancyReason = null,
            count = 15, load = "NORMAL", emergencyCount = 0, pendingGroups = 1
        )
        val normalBus = FleetBus(
            id = 12, plate = "OK-003", enabled = true, capacity = 30, shiftId = 22,
            route = "Line 3", driver = "Driver C", locationStatus = "AVAILABLE",
            position = null, occupancyStatus = "NORMAL", occupancyReason = null,
            count = 10, load = "NORMAL", emergencyCount = 0, pendingGroups = 0
        )

        val overview = FleetOverview(listOf(busWithEmergency, busWithReview, normalBus), Instant.now())
        val metrics = overview.metrics()

        assertEquals(3, metrics.totalBuses)
        assertEquals(3, metrics.activeBuses)
        assertEquals(0, metrics.overCapacityCount)
        assertEquals(2, metrics.attentionNeededCount)
        assertEquals(1.0f, metrics.inServiceRate, 0.001f)
    }
}
