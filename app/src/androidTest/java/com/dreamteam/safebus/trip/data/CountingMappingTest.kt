package com.dreamteam.safebus.trip.data

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class CountingMappingTest {
    @Test fun unavailableCountIsNotZeroAndSourceKeepsRevocationTime() {
        val occupancy = JSONObject("""{"busId":2,"status":"UNAVAILABLE","reason":"NOT_INITIALIZED","generatedAt":"2026-01-01T00:00:00Z"}""").toOccupancy()
        assertNull(occupancy.count); assertNull(occupancy.lastCountAt)
        val source = JSONObject("""{"sourceId":1,"busId":2,"status":"REVOKED","createdAt":"2026-01-01T00:00:00Z","revokedAt":"2026-01-02T00:00:00Z"}""").toCountingSource()
        assertNotNull(source.revokedAt); assertNull(source.label)
    }
}
