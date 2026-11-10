package com.dreamteam.safebus.shared.domain

import org.junit.Assert.*
import org.junit.Test

class PhotoTest {
    @Test fun boundsPayloadAndProtectsItFromMutation() {
        assertThrows(IllegalArgumentException::class.java) { Photo(byteArrayOf(), "image/jpeg") }
        assertThrows(IllegalArgumentException::class.java) { Photo(ByteArray(5 * 1024 * 1024 + 1), "image/jpeg") }
        assertThrows(IllegalArgumentException::class.java) { Photo(byteArrayOf(1), "image/gif") }
        val bytes = byteArrayOf(1)
        val photo = Photo(bytes, "image/png")
        bytes[0] = 2; photo.bytes[0] = 3
        assertArrayEquals(byteArrayOf(1), photo.bytes)
        assertEquals("Photo(redacted)", photo.toString())
    }
}
