package com.dreamteam.safebus.fleet.data

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class FleetMappingTest {
    @Test fun missingOccupancyAndPositionAreNeverDisplayedAsZero() {
        val bus = JSONObject("""{"busId":1,"plate":"ABC-123","enabled":true,"capacity":null,"shift":null,"route":null,"driver":null,"location":{"status":"UNAVAILABLE"},"occupancy":{"status":"UNAVAILABLE","reason":"NOT_INITIALIZED","count":null},"emergencies":[],"passengerGroups":[]}""").toFleetBus()
        assertNull(bus.capacity); assertNull(bus.count); assertNull(bus.position)
        assertEquals("UNAVAILABLE", bus.occupancyStatus)
        assertEquals("NOT_INITIALIZED", bus.occupancyReason)
        assertEquals(0, bus.emergencyCount)
    }
}
