package com.dreamteam.safebus.shared.data

import android.graphics.Bitmap
import org.junit.Assert.*
import org.junit.Test

class PhotoLoaderDeviceTest {
    @Test fun decodesActualImagesAndRejectsInvalidBytes() {
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        val output = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output); bitmap.recycle()
        assertEquals("image/png", PhotoLoader.decode(output.toByteArray()).mime)
        assertThrows(IllegalArgumentException::class.java) { PhotoLoader.decode("invalid photo".toByteArray()) }
    }
}
