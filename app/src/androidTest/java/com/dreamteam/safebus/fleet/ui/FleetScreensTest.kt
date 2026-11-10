package com.dreamteam.safebus.fleet.ui

import androidx.compose.runtime.mutableStateOf
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
    @Test fun fleetUsesIndependentPluralsForPassengersEmergenciesAndReviews() {
        val bus = mutableStateOf(FleetBus(1, "ABC-123", true, 1, null, null, null, "UNAVAILABLE", null, "AVAILABLE", null, 1, null, 1, 2))
        compose.setContent { CoreTheme { FleetScreen(ScreenState(FleetState(FleetOverview(listOf(bus.value), Instant.EPOCH))), {}) } }
        compose.onNodeWithText("Capacidad: 1 pasajero").assertExists()
        compose.onNodeWithText("Ocupación: 1 pasajero").assertExists()
        compose.onNodeWithText("1 emergencia · 2 revisiones pendientes").assertExists()
        compose.runOnIdle { bus.value = bus.value.copy(capacity = 40, count = 0, emergencyCount = 2, pendingGroups = 1) }
        compose.onNodeWithText("Capacidad: 40 pasajeros").assertExists()
        compose.onNodeWithText("Ocupación: 0 pasajeros").assertExists()
        compose.onNodeWithText("2 emergencias · 1 revisión pendiente").assertExists()
    }
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
