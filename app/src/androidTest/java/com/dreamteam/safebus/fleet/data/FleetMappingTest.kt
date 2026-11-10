package com.dreamteam.safebus.fleet.data

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class FleetMappingTest {
    @Test fun mapsStaleOccupancyTimesAndSeparatesQualifyingFromAttachedEvidence() {
        val bus = JSONObject("""{"busId":1,"plate":"ABC-123","enabled":true,"capacity":20,"location":{"status":"UNAVAILABLE"},"occupancy":{"status":"STALE","count":5,"lastCountAt":"2026-11-10T12:00:00Z","lastHeartbeatAt":"2026-11-10T12:01:00Z"},"emergencies":[],"passengerGroups":[{"status":"AWAITING_APPROVAL","requestCount":3,"attachedCount":2}]}""").toFleetBus()
        assertEquals("STALE", bus.occupancyStatus)
        assertNotNull(bus.lastCountAt); assertNotNull(bus.lastHeartbeatAt)
        assertEquals(3, bus.groups.single().contributors); assertEquals(2, bus.groups.single().attached)
        assertEquals(1, bus.pendingGroups)
    }
    @Test fun missingOccupancyAndPositionAreNeverDisplayedAsZero() {
        val bus = JSONObject("""{"busId":1,"plate":"ABC-123","enabled":true,"capacity":null,"shift":null,"route":null,"driver":null,"location":{"status":"UNAVAILABLE"},"occupancy":{"status":"UNAVAILABLE","reason":"NOT_INITIALIZED","count":null},"emergencies":[],"passengerGroups":[]}""").toFleetBus()
        assertNull(bus.capacity); assertNull(bus.count); assertNull(bus.position)
        assertEquals("UNAVAILABLE", bus.occupancyStatus)
        assertEquals("NOT_INITIALIZED", bus.occupancyReason)
        assertEquals(0, bus.emergencyCount)
    }
}
