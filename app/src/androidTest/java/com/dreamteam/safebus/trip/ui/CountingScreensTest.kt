package com.dreamteam.safebus.trip.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.shared.ui.*
import com.dreamteam.safebus.trip.domain.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class CountingScreensTest {
    @get:Rule val compose = createComposeRule()
    @Test fun occupancyUsesPassengerSingularAndPlural() {
        val occupancy = mutableStateOf(Occupancy(1, "AVAILABLE", null, 1, 1, null, null, null, Instant.EPOCH))
        compose.setContent { CoreTheme { CountingScreen(ScreenState(CountingState(occupancy = occupancy.value)), {}, {}, {}, {}, {}, null, {}) } }
        compose.onNodeWithText("Occupancy: 1 passenger").assertExists()
        compose.onNodeWithText("Capacity: 1 passenger").assertExists()
        compose.runOnIdle { occupancy.value = occupancy.value.copy(count = 0, capacity = 40) }
        compose.onNodeWithText("Occupancy: 0 passengers").assertExists()
        compose.onNodeWithText("Capacity: 40 passengers").assertExists()
    }
    @Test fun confirmedCountUsesPassengerSingularAndPlural() {
        val receipt = mutableStateOf(CountReceipt(1, 2, Instant.EPOCH))
        compose.setContent { CoreTheme { CounterScreen(ScreenState(CounterUiState(CounterState(receipt = receipt.value))), false, { _, _, _, _ -> }, {}, {}, {}) } }
        compose.onNodeWithText("Occupancy: 1 passenger").assertExists()
        compose.runOnIdle { receipt.value = CountReceipt(2, 3, Instant.EPOCH) }
        compose.onNodeWithText("Occupancy: 2 passengers").assertExists()
    }
    @Test fun revocationRequiresConfirmationAndRejectionDoesNotChangeSource() {
        var calls = 0
        val state = ScreenState(CountingState(sources = listOf(CountingSource(1, 2, null, "ACTIVE", Instant.EPOCH, null, null)), loaded = true), failure = Failure.fromStatus(403))
        compose.setContent { CoreTheme { CountingScreen(state, {}, {}, {}, { calls++ }, {}, null, {}) } }
        compose.onNodeWithTag("revoke:1").performScrollTo().performClick()
        assertEquals(0, calls)
        compose.onNodeWithTag("confirmSource").performClick(); assertEquals(1, calls)
        compose.onNodeWithTag("revoke:1").assertExists()
    }
    @Test fun cannotInitializeWithoutEmptyConfirmationOrSendWithoutAKey() {
        var writes = 0
        compose.setContent { CoreTheme { CounterScreen(ScreenState(CounterUiState()), false, { _, _, _, _ -> writes++ }, {}, {}, {}) } }
        compose.onNodeWithTag("initializeCount").assertIsNotEnabled()
        compose.onNodeWithTag("countEntry").assertIsNotEnabled()
        compose.onNodeWithTag("counterKey").performTextInput("private-key")
        compose.onNodeWithTag("initializeCount").assertIsNotEnabled()
        compose.onNodeWithTag("confirmEmptyBus").performClick()
        compose.onNodeWithTag("initializeCount").performScrollTo().performClick()
        assertEquals(1, writes)
    }
}
