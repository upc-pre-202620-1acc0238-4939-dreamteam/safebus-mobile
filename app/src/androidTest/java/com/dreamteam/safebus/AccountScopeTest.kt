package com.dreamteam.safebus

import com.dreamteam.safebus.iam.domain.*
import com.dreamteam.safebus.trip.domain.ActiveShift
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
        val session = Session("1", Role.DRIVER, "private", Instant.now().plusSeconds(100), 1)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val account = AccountScope(session, "https://example.com", false, store, HttpEngine { error("No implicit requests") }, { "private" }, {})
            account.setShift(ActiveShift(1, 1, 1, Instant.now()))
            assertNotNull(account.tracker)
            assertThrows(IllegalArgumentException::class.java) { account.countingModel(1) }
            assertThrows(IllegalArgumentException::class.java) { account.counterModel(1, 1, 1) }
            assertThrows(IllegalArgumentException::class.java) {
                AccountScope(session.copy(role = Role.PASSENGER, companyId = null), "https://example.com", false, store, HttpEngine { error("No implicit requests") }, { "private" }, {})
            }
            account.dispose()
            assertNull(account.tracker)
            assertEquals(TrackingStatus.STOPPED, account.tracking.value.status)
            assertTrue(store.data.isEmpty())
        }
    }
}
