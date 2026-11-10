package com.dreamteam.safebus.trip.domain

import java.time.Instant
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test

class CountingTest {
    @Test fun validatesCommandsAndNeverLogsDeviceKeys() {
        val source = CountingSource(1, 2, null, "ACTIVE", Instant.EPOCH, null, null)
        assertFalse(SourceCredential(source, "private-key").toString().contains("private-key"))
        assertThrows(IllegalArgumentException::class.java) { SourceCredential(source, "key\r\ninjected") }
        assertThrows(IllegalArgumentException::class.java) { CountCommand(UUID.randomUUID().toString(), 2, 1, CountAction.ENTRY, 0, Instant.EPOCH) }
        assertThrows(IllegalArgumentException::class.java) { CountReceipt(-1, 1, null) }
        assertNull(Occupancy(2, "UNAVAILABLE", "NOT_INITIALIZED", null, 40, null, null, null, Instant.EPOCH).count)
    }
}
