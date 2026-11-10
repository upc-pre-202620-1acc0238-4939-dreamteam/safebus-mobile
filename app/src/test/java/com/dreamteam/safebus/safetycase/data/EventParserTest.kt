package com.dreamteam.safebus.safetycase.data

import org.junit.Assert.*
import org.junit.Test

class EventParserTest {
    @Test fun reconcilesOnlyKnownCompleteEventsAndIgnoresPrivatePayloads() {
        val parser = EventParser()
        assertFalse(parser.accept(": heartbeat")); assertFalse(parser.accept(""))
        assertFalse(parser.accept("event: EmergencyActivated"))
        assertFalse(parser.accept("data: {\"id\":\"case-1\"}"))
        assertTrue(parser.accept("")); assertFalse(parser.accept(""))
        parser.accept("event: PassengerReviewRequired"); assertTrue(parser.accept(""))
        parser.accept("event: Unknown"); assertFalse(parser.accept(""))
        assertThrows(IllegalArgumentException::class.java) { parser.accept("a".repeat(8193)) }
    }
}
