package com.dreamteam.safebus.safetycase.data

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class SafetyMappingTest {
    @Test fun mapsPrivateDecisionAndKeepsNullableCounts() {
        val item = JSONObject("""{"requestId":"00000000-0000-0000-0000-000000000001","message":"Help","status":"DECLINED","submittedAt":"2026-11-10T12:00:00Z","contributorCount":null,"groupReference":"G1","decision":{"result":"DECLINED","response":"Reviewed"}}""").toPassengerRequest()
        assertNull(item.contributorCount)
        assertEquals("Reviewed", item.response)
        assertEquals("DECLINED", item.decision)
    }
}
