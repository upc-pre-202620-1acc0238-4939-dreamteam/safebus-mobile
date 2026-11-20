package com.dreamteam.safebus.fleet.domain

import org.junit.Assert.*
import org.junit.Test

class BusQrTest {
    @Test fun supervisorBusCredentialPreservesTheOtherAppsQrContract() {
        val qr = BusQr("code+&?private", 9)
        assertEquals("safebus://bus/9?code=code%2B%26%3Fprivate", qr.encoded())
        assertEquals(qr, BusQr.parse(qr.encoded()))
        assertThrows(IllegalArgumentException::class.java) { BusQr("", 9) }
    }
}
