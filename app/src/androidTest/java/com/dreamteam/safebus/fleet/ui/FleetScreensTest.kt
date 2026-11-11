package com.dreamteam.safebus.fleet.ui

import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.dreamteam.safebus.shared.ui.*
import com.dreamteam.safebus.fleet.domain.*
import com.dreamteam.safebus.trip.domain.Position
import java.time.Instant
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class FleetScreensTest {
    @get:Rule val compose = createComposeRule()
    @Test fun searchMatchesDriverAndRouteAndFiltersStaleLocations() {
        val bus = locatedBus().copy(route = "Lima Centro", driver = "Ana Perez", locationStatus = "STALE")
        compose.setContent { CoreTheme { FleetScreen(ScreenState(FleetState(FleetOverview(listOf(bus), Instant.EPOCH))), {}) } }
        compose.onNodeWithTag("fleetSearch").performTextInput("Ana")
        compose.onNodeWithText("ABC-123").assertExists()
        compose.onNodeWithTag("fleetSearch").performTextReplacement("Centro")
        compose.onNodeWithText("ABC-123").assertExists()
        compose.onNodeWithTag("fleetFilter:${com.dreamteam.safebus.R.string.ui_current}").performClick()
        compose.onNodeWithText("ABC-123").assertDoesNotExist()
        compose.onNodeWithTag("fleetFilter:${com.dreamteam.safebus.R.string.ui_stale}").performClick()
        compose.onNodeWithText("ABC-123").assertExists()
        compose.onNodeWithTag("fleetMap:true").performClick()
        compose.onNodeWithTag("fleetMapFallback").assertExists()
    }
    @Test fun assignmentWizardRequiresReviewBeforePersistingAndRetainsBackValues() {
        var saved: FleetChange? = null
        compose.setContent { CoreTheme { ProvisionScreen(ScreenState(FleetState()), { saved = it }, {}) } }
        listOf("driver" to "1", "bus" to "2", "route" to "3").forEach { (field, value) ->
            compose.onNodeWithTag("provision:$field").performScrollTo().performTextInput(value)
            compose.onNodeWithTag("assignmentNext").performScrollTo().performClick()
        }
        compose.onNodeWithTag("provision:start").performScrollTo().performTextInput("2026-11-12T08:00")
        compose.onNodeWithTag("provision:end").performScrollTo().performTextInput("2026-11-12T12:00")
        compose.onNodeWithTag("assignmentNext").performScrollTo().performClick()
        assertNull(saved)
        compose.onNodeWithTag("assignmentBack").performScrollTo().performClick()
        compose.onNodeWithTag("provision:start").assertTextContains("2026-11-12T08:00")
        compose.onNodeWithTag("assignmentNext").performScrollTo().performClick()
        compose.onNodeWithTag("saveProvision").performScrollTo().performClick()
        assertEquals(2L, (saved as FleetChange.Assignment).busId)
    }
    @Test fun mapActionPreservesCoordinatesAndViewIntent() {
        val bus = locatedBus()
        var opened: Intent? = null
        compose.setContent {
            val base = LocalContext.current
            val context = object : ContextWrapper(base) {
                override fun startActivity(intent: Intent) { opened = intent }
            }
            CompositionLocalProvider(LocalContext provides context) {
                CoreTheme { FleetScreen(ScreenState(FleetState(FleetOverview(listOf(bus), Instant.EPOCH))), {}) }
            }
        }
        compose.onNodeWithTag("fleetMap:true").performClick()
        compose.onNodeWithTag("map:1").performScrollTo().performClick()
        assertEquals(Intent.ACTION_VIEW, opened?.action)
        assertEquals("geo:-12.0464,-77.0428?q=-12.0464,-77.0428", opened?.data.toString())
    }
    @Test fun missingMapAppShowsFeedbackAndKeepsFleetData() {
        val bus = locatedBus()
        val state = ScreenState(FleetState(FleetOverview(listOf(bus), Instant.EPOCH)))
        compose.setContent {
            val base = LocalContext.current
            val context = object : ContextWrapper(base) {
                override fun startActivity(intent: Intent) { throw ActivityNotFoundException() }
            }
            CompositionLocalProvider(LocalContext provides context) {
                CoreTheme { FleetScreen(state, {}) }
            }
        }
        compose.onNodeWithTag("fleetMap:true").performClick()
        compose.onNodeWithTag("map:1").performScrollTo().performClick()
        compose.onNodeWithTag("mapUnavailable").assertExists()
        compose.onNodeWithText("ABC-123").assertExists()
        assertEquals(listOf(bus), state.value.overview?.buses)
    }
    private fun locatedBus() = FleetBus(1, "ABC-123", true, null, null, null, null, "AVAILABLE",
        Position(-12.0464, -77.0428, 5.0, Instant.EPOCH), "UNAVAILABLE", "NOT_INITIALIZED", null, null, 0, 0)
    @Test fun fleetUsesIndependentPluralsForPassengersEmergenciesAndReviews() {
        val bus = mutableStateOf(FleetBus(1, "ABC-123", true, 1, null, null, null, "UNAVAILABLE", null, "AVAILABLE", null, 1, null, 1, 2))
        compose.setContent { CoreTheme { FleetScreen(ScreenState(FleetState(FleetOverview(listOf(bus.value), Instant.EPOCH))), {}) } }
        compose.onNodeWithTag("busDetails:1").performScrollTo().performClick()
        compose.onNodeWithText("Capacity: 1 passenger").assertExists()
        compose.onNodeWithText("Occupancy: 1 passenger").assertExists()
        compose.onNodeWithText("1 emergency · 2 pending reviews").assertExists()
        compose.runOnIdle { bus.value = bus.value.copy(capacity = 40, count = 0, emergencyCount = 2, pendingGroups = 1) }
        compose.onNodeWithText("Capacity: 40 passengers").assertExists()
        compose.onNodeWithText("Occupancy: 0 passengers").assertExists()
        compose.onNodeWithText("2 emergencies · 1 pending review").assertExists()
    }
    @Test fun invalidAssignmentShowsFeedbackAndDoesNotSubmit() {
        var writes = 0
        compose.setContent { CoreTheme { ProvisionScreen(ScreenState(FleetState()), { writes++ }, {}) } }
        compose.onNodeWithTag("assignmentNext").performScrollTo().performClick()
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
