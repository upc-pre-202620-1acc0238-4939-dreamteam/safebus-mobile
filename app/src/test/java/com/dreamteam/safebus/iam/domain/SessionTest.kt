package com.dreamteam.safebus.iam.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class SessionTest {
    @Test fun expiresAtTheBoundaryAndNeverLogsCredentials() {
        val expiry = Instant.parse("2026-11-10T17:00:00Z")
        for (role in Role.entries) {
            val session = Session("18", role, "private-token", expiry, if (role == Role.PASSENGER) null else 4)
            assertTrue(session.validAt(expiry.minusSeconds(1)))
            assertFalse(session.validAt(expiry))
            assertFalse(session.toString().contains("private-token"))
            assertFalse(session.toString().contains("18"))
        }
    }
    @Test fun refusesMalformedIdentityAndMissingCompanyScope() {
        assertThrows(IllegalArgumentException::class.java) { Session("0", Role.PASSENGER, "token", Instant.MAX, null) }
        assertThrows(IllegalArgumentException::class.java) { Session("1", Role.DRIVER, "token", Instant.MAX, null) }
        assertThrows(IllegalArgumentException::class.java) { Session("1", Role.PASSENGER, "", Instant.MAX, null) }
    }
}
