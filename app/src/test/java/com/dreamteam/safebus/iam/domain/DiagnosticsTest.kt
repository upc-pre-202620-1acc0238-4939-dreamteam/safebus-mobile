package com.dreamteam.safebus.iam.domain

import org.junit.Assert.*
import org.junit.Test

class DiagnosticsTest {
    @Test fun formatsAndSanitizesEndpointsCorrectly() {
        val diagnostics = SystemDiagnostics(
            endpoint = "http://10.0.2.2:8080/  ",
            role = Role.SUPERVISOR,
            userId = "101"
        )

        assertEquals("http://10.0.2.2:8080", diagnostics.sanitizedEndpoint())
        assertTrue(diagnostics.isLocalHost)
        assertFalse(diagnostics.hasPendingWork)
        assertEquals("0.1.0", diagnostics.appVersion)
        assertEquals(1, diagnostics.versionCode)
    }

    @Test fun recognizesRemoteEndpointsAndPendingTasks() {
        val diagnostics = SystemDiagnostics(
            endpoint = "https://safebus-api-production.up.railway.app",
            role = Role.DRIVER,
            userId = "202",
            pendingSyncs = 3
        )

        assertEquals("https://safebus-api-production.up.railway.app", diagnostics.sanitizedEndpoint())
        assertFalse(diagnostics.isLocalHost)
        assertTrue(diagnostics.hasPendingWork)
        assertEquals(3, diagnostics.pendingSyncs)
    }
}
