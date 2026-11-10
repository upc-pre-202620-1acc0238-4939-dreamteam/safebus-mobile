package com.dreamteam.safebus.shared.ui

import com.dreamteam.safebus.shared.domain.*
import org.junit.Assert.*
import org.junit.Test

class FailureTextTest {
    @Test fun everyFailureHasAnIsolatedUserMessage() {
        val messages = FailureKind.entries.map { failureText(Failure(it)) }
        assertEquals(FailureKind.entries.size, messages.distinct().size)
        assertTrue(messages.all { it != 0 })
        assertNotEquals(failureText(Failure.fromStatus(409)), failureText(Failure.fromStatus(409, "DUPLICATE_CONTRIBUTION")))
    }
}
