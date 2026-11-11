package com.dreamteam.safebus.shared.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.dreamteam.safebus.iam.ui.*
import com.dreamteam.safebus.fleet.ui.*
import com.dreamteam.safebus.fleet.domain.*
import com.dreamteam.safebus.passenger.ui.*
import com.dreamteam.safebus.passenger.domain.Journey
import com.dreamteam.safebus.safetycase.ui.*
import com.dreamteam.safebus.safetycase.domain.*
import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.trip.ui.*
import com.dreamteam.safebus.trip.domain.*
import java.io.File
import java.time.Instant
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ReportLayoutsTest {
    @get:Rule val compose = createComposeRule()
    private fun show(dark: Boolean = false, content: @Composable () -> Unit) {
        compose.setContent { CoreTheme(dark) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background, content = content)
        } }
    }
    private fun capture(name: String) {
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(target.getExternalFilesDir(null), "report-$name.png")
        file.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(file.length() > 0)
    }
    @Test fun welcomeAndLoginMatchReportStructure() {
        show { AccessScreen(ScreenState(AuthState()), { _, _ -> }, {}, {}) }
        compose.onNodeWithTag("language:en").assertIsSelected()
        compose.onNodeWithTag("welcomeLogin").assertIsDisplayed()
        capture("welcome")
        compose.onNodeWithTag("welcomeLogin").performClick()
        compose.onNodeWithTag("corePassword").assertIsDisplayed()
        capture("login")
    }
    @Test fun activeShiftShowsAssignedUnitAndLocationPanel() {
        val assignment = Assignment(1, "ACTIVE", "ABC-123", "Route 301", "San Juan", "Centro", Instant.EPOCH, Instant.EPOCH.plusSeconds(3600))
        show { TripScreen(ScreenState(TripState(assignment = assignment, shift = ActiveShift(1, 1, 1, Instant.EPOCH))), TrackingState(), {}, {}, {}, {}) }
        compose.onNodeWithText("ABC-123").assertExists()
        compose.onNodeWithTag("closeShift").assertExists()
        capture("shift")
    }
    @Test fun passengerUnitKeepsServerIdentityInDarkTheme() {
        val journey = Journey(1, 1, "ABC-123", "Transport Company", "VERIFIED", "Route 301", "San Juan", "Centro", "Ana Perez", Instant.EPOCH)
        show(true) { JourneyScreen(ScreenState(PassengerState(journey)), TrackingState(), {}, {}, {}, {}) }
        compose.onNodeWithTag("verifiedBus").assertTextEquals("ABC-123")
        compose.onNodeWithTag("newRealRequest").assertExists()
        capture("journey-dark")
    }
    @Test fun fleetKeepsOccupancyAndLocationIndependent() {
        val bus = FleetBus(1, "ABC-123", true, 40, 1, "Route 301", "Ana Perez", "STALE", Position(-12.04, -77.03, 5.0, Instant.EPOCH), "AVAILABLE", null, 28, null, 1, 1)
        show { FleetScreen(ScreenState(FleetState(FleetOverview(listOf(bus), Instant.EPOCH))), {}) }
        compose.onNodeWithText("28 of 40 passengers").assertExists()
        compose.onNodeWithText("Last known data").assertExists()
        compose.onNodeWithTag("busDetails:1").assertExists()
        capture("fleet")
    }
    @Test fun requestFormShowsCountAndPreventsPrematureSubmission() {
        var writes = 0
        show { RequestFormScreen(ScreenState(SafetyState()), { _, _ -> writes++ }, {}) }
        compose.onNodeWithText("0/500").assertExists()
        compose.onNodeWithTag("reviewRealRequest").assertIsNotEnabled()
        compose.onNodeWithTag("sendRealRequest").assertDoesNotExist()
        assertEquals(0, writes)
        capture("request")
    }
    @Test fun emergenciesPrioritizeDriversAndOpenAnIndependentDetail() {
        val critical = Emergency("00000000-0000-0000-0000-000000000001", "ACTIVE", "CRITICAL", 1, Instant.EPOCH)
        val high = critical.copy(id = "00000000-0000-0000-0000-000000000002", priority = "HIGH")
        val state = ScreenState(SafetyState(queue = ReviewQueue(listOf(high, critical), emptyList(), Instant.EPOCH)))
        var attended: String? = null
        show { SafetyListScreen(state, SafetyTab.EMERGENCIES, {}, {}, {}, attend = { attended = it }) }
        assertTrue(compose.onNodeWithText("Critical · Driver").fetchSemanticsNode().positionInRoot.y <
            compose.onNodeWithText("High · Passengers").fetchSemanticsNode().positionInRoot.y)
        capture("emergencies")
        compose.onNodeWithTag("emergencyDetail:${critical.id}").performScrollTo().performClick()
        compose.onNodeWithText("High · Passengers").assertDoesNotExist()
        compose.onNodeWithTag("attend:${critical.id}").performScrollTo().performClick()
        assertNull(attended)
        compose.onNodeWithTag("confirmDecision").performClick()
        assertEquals(critical.id, attended)
    }
    @Test fun groupReviewShowsPrivateContributionsBeforeDecision() {
        val request = PassengerRequest("00000000-0000-0000-0000-000000000001", "Incident near the rear door", "AWAITING_APPROVAL", Instant.EPOCH, 3, "G-001", null, null)
        val group = ReviewGroup(1, "G-001", 1, 3, listOf(request))
        show { SafetyListScreen(ScreenState(SafetyState(queue = ReviewQueue(emptyList(), listOf(group), Instant.EPOCH))), SafetyTab.REVIEWS, {}, {}, {}) }
        compose.onNodeWithTag("approve:1").assertDoesNotExist()
        compose.onNodeWithTag("reviewGroup:1").performScrollTo().performClick()
        compose.onNodeWithText(request.message).assertExists()
        compose.onNodeWithTag("evidence:${request.id}").assertExists()
        capture("group-review")
        compose.onNodeWithTag("pageBack").performClick()
        compose.onNodeWithTag("approve:1").assertDoesNotExist()
    }
    @Test fun largeTextKeepsLoginActionsReachable() {
        var login: String? = null
        show {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                AccessScreen(ScreenState(AuthState()), { id, _ -> login = id }, {}, {})
            }
        }
        compose.onNodeWithTag("welcomeLogin").performScrollTo().performClick()
        compose.onNodeWithTag("coreLoginId").performScrollTo().performTextInput("12345678")
        compose.onNodeWithTag("corePassword").performScrollTo().performTextInput("Password123")
        compose.onNodeWithTag("coreSignIn").performScrollTo().assertIsDisplayed().performClick()
        assertEquals("12345678", login)
        capture("large-text")
    }
}
