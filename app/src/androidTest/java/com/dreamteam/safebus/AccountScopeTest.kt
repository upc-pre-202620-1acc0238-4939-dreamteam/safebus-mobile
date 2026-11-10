package com.dreamteam.safebus

import com.dreamteam.safebus.iam.domain.*
import com.dreamteam.safebus.passenger.domain.Journey
import com.dreamteam.safebus.shared.data.*
import com.dreamteam.safebus.shared.domain.*
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class AccountScopeTest {
    @Test fun disconnectingAccountStopsTrackingAndKeepsPrivateStateIsolated() {
        val store = object : SecureStore {
            val data = mutableMapOf<String, ByteArray>()
            override fun read(key: String) = data[key]
            override fun write(key: String, value: ByteArray) { data[key] = value }
            override fun remove(key: String) { data.remove(key) }
        }
        val session = Session("1", Role.PASSENGER, "private", Instant.now().plusSeconds(100), null)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val account = AccountScope(session, "https://example.com", false, store, HttpEngine { error("No implicit requests") }, { "private" }, {})
            account.setJourney(Journey(1, null, "ABC", "Company", "VERIFIED", "R", "A", "B", "Driver", Instant.now()))
            assertNotNull(account.tracker)
            assertThrows(IllegalArgumentException::class.java) { account.countingModel(1) }
            assertThrows(IllegalArgumentException::class.java) { account.counterModel(1, 1, 1) }
            account.dispose()
            assertNull(account.tracker)
            assertEquals(TrackingStatus.STOPPED, account.tracking.value.status)
            assertTrue(store.data.isEmpty())
        }
    }
}
