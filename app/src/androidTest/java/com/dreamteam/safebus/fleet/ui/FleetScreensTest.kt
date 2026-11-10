package com.dreamteam.safebus.fleet.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.dreamteam.safebus.shared.ui.*
import com.dreamteam.safebus.fleet.domain.*
import java.time.Instant
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class FleetScreensTest {
    @get:Rule val compose = createComposeRule()
    @Test fun invalidAssignmentShowsFeedbackAndDoesNotSubmit() {
        var writes = 0
        compose.setContent { CoreTheme { ProvisionScreen(ScreenState(FleetState()), { writes++ }, {}) } }
        compose.onNodeWithTag("saveProvision").performScrollTo().performClick()
        compose.onNodeWithTag("coreError").assertExists()
        assertEquals(0, writes)
    }
    @Test fun unavailableLocationHasNoMapActionAndFleetCanBeSearched() {
        val bus = FleetBus(1, "ABC-123", true, null, null, null, null, "UNAVAILABLE", null, "UNAVAILABLE", "NOT_INITIALIZED", null, null, 0, 0)
        compose.setContent { CoreTheme { FleetScreen(ScreenState(FleetState(FleetOverview(listOf(bus), Instant.EPOCH))), {}) } }
        compose.onNodeWithText("ABC-123").assertExists()
        compose.onNodeWithTag("map:1").assertDoesNotExist()
        compose.onNodeWithTag("fleetSearch").performTextInput("OTHER")
        compose.onNodeWithText("ABC-123").assertDoesNotExist()
    }
}
