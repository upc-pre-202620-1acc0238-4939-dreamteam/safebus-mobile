package com.dreamteam.safebus.shared.data

import org.junit.Assert.*
import org.junit.Test

class PhotoLoaderTest {
    @Test fun rejectsInvalidDimensionsAndDecompressionBombs() {
        assertTrue(PhotoLoader.acceptableDimensions(5000, 5000))
        assertFalse(PhotoLoader.acceptableDimensions(5001, 5000))
        assertFalse(PhotoLoader.acceptableDimensions(Int.MAX_VALUE, Int.MAX_VALUE))
        assertFalse(PhotoLoader.acceptableDimensions(0, 20))
        assertFalse(PhotoLoader.acceptableDimensions(-1, -1))
    }
}
