package com.dreamteam.safebus.trip.data

import com.dreamteam.safebus.shared.data.*
import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.trip.domain.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.util.UUID

class HttpCounterServiceTest {
    @Test fun usesDeviceContractAndReportsExpectedSequenceWithoutChangingSession() = runBlocking {
        val sent = mutableListOf<ApiRequest>()
        val service = HttpCounterService("https://example.com", false, { true }, HttpEngine {
            sent.add(it); ApiResponse(409, """{"code":"SEQUENCE_GAP","expectedSequence":4}""".toByteArray())
        })
        val command = CountCommand(UUID.randomUUID().toString(), 1, 2, CountAction.ENTRY, 5, Instant.EPOCH)
        val result = service.send(command, "device-key") as DomainResult.Error
        assertEquals(4L, result.failure.expectedSequence); assertEquals(FailureKind.CONFLICT, result.failure.kind)
        assertEquals("device-key", sent.single().countingKey); assertNull(sent.single().token)
        assertEquals(5L, JSONObject(sent.single().body!!.toString(Charsets.UTF_8)).getLong("sequence"))
        val denied = HttpCounterService("https://example.com", false, { false }, HttpEngine { fail("Must not send after logout"); ApiResponse(200, byteArrayOf()) })
        assertEquals(FailureKind.UNAUTHORIZED, (denied.heartbeat(1, "device-key") as DomainResult.Error).failure.kind)
        assertEquals(1, sent.size)
    }
}
