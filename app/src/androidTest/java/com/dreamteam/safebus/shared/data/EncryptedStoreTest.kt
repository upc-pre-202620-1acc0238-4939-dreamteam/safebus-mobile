package com.dreamteam.safebus.shared.data

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class EncryptedStoreTest {
    @Test fun roundTripsAtomicRecordsAndDeletesCredentials() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = EncryptedStore(context)
        val name = "test-${UUID.randomUUID()}"
        try {
            assertNull(store.read(name))
            store.write(name, byteArrayOf(1, 2))
            assertArrayEquals(byteArrayOf(1, 2), EncryptedStore(context).read(name))
            store.write(name, byteArrayOf(3))
            assertArrayEquals(byteArrayOf(3), store.read(name))
            assertNull(store.read("$name-other-account"))
        } finally { store.remove(name) }
        assertNull(store.read(name))
    }
}
