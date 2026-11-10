package com.dreamteam.safebus.passenger.domain

import com.dreamteam.safebus.shared.domain.Photo
import com.dreamteam.safebus.trip.domain.Position
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class PassengerTest {
    private val now = Instant.parse("2026-11-10T12:00:00Z")
    @Test fun endTimeNeverPrecedesAcknowledgedStartOrMeasuredSeparation() {
        assertEquals(now, journeyEndTime(now, now.minusSeconds(2), null))
        assertEquals(now.plusSeconds(60), journeyEndTime(now, now.plusSeconds(58), Separation(200, 60)))
        assertEquals(now.plusSeconds(90), journeyEndTime(now, now.plusSeconds(90), Separation(200, 60)))
    }
    @Test fun qrPreservesRawCredentialsAndOptionalBusIdentifier() {
        assertEquals(BusQr("raw-code"), BusQr.parse(" raw-code "))
        val qr = BusQr("code+&?private", 9)
        assertEquals(qr, BusQr.parse(qr.encoded()))
        assertThrows(IllegalArgumentException::class.java) { BusQr.parse("safebus://bus/0?code=qr") }
        assertThrows(IllegalArgumentException::class.java) { BusQr.parse("") }
    }
    @Test fun registrationRequiresIdentityPasswordAndConsent() {
        val input = Registration("12345678", "password", true, Photo(byteArrayOf(1), "image/jpeg"))
        assertTrue(input.valid())
        assertFalse(input.copy(dni = "1234").valid())
        assertFalse(input.copy(password = "short").valid())
        assertFalse(input.copy(acceptedTerms = false).valid())
        assertFalse(input.toString().contains(input.password))
    }
    @Test fun autoExitRequiresContinuousReliableSeparationAndResetsOnAnyGap() {
        val detector = SeparationDetector()
        fun sample(seconds: Long, accurate: Boolean = true) = detector.compare(
            Position(-12.0, -77.0, if (accurate) 10.0 else 31.0, now.plusSeconds(seconds)),
            Position(-12.002, -77.0, 10.0, now.plusSeconds(seconds)), now.plusSeconds(seconds))
        assertNull(sample(0)); assertNull(sample(30)); assertEquals(60, sample(60)?.seconds)
        assertNull(sample(90, false)); assertNull(sample(120)); assertNull(sample(151)); assertNull(sample(181))
        assertEquals(60, sample(211)?.seconds)
        assertNull(detector.compare(null, null, now.plusSeconds(212)))
        assertNull(sample(213))
        assertNull(detector.compare(Position(0.0, 0.0, 10.0, now), Position(1.0, 1.0, 10.0, now), now.plusSeconds(500)))
    }
}
