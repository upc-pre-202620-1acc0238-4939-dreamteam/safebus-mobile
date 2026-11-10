package com.dreamteam.safebus.trip.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.dreamteam.safebus.shared.ui.*
import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.trip.domain.*
import java.time.Instant
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TripScreenTest {
    @get:Rule val compose = createComposeRule()
    @Test fun confirmedClosureRequiresExplicitInteraction() {
        var closed = 0
        compose.setContent { CoreTheme { TripScreen(ScreenState(TripState(shift = ActiveShift(1, 1, 1, Instant.EPOCH))), TrackingState(), {}, {}, { closed++ }, {}) } }
        compose.onNodeWithTag("closeShift").performScrollTo().performClick()
        assertEquals(0, closed)
        compose.onNodeWithTag("confirmShiftClose").performClick()
        assertEquals(1, closed)
    }
    @Test fun failedActivationShowsErrorWithoutActiveControlsOrPersistence() {
        var writes = 0
        compose.setContent { CoreTheme { TripScreen(ScreenState(TripState(loaded = true), failure = Failure.fromStatus(422)), TrackingState(), {}, { writes++ }, {}, {}) } }
        compose.onNodeWithTag("coreError").assertExists()
        compose.onNodeWithTag("closeShift").assertDoesNotExist()
        compose.onNodeWithTag("noAssignment").assertExists()
        assertEquals(0, writes)
    }
}
