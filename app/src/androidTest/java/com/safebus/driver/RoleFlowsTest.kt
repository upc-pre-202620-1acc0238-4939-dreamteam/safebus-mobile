package com.safebus.driver

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import java.io.File

class RoleFlowsTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun click(tag: String, scroll: Boolean = false) {
        val node = compose.onNodeWithTag(tag)
        if (scroll) node.performScrollTo()
        node.performClick()
    }
    private fun signIn(code: String, password: String = "SafeBus123", welcome: Boolean = true) {
        if (welcome) click("welcomeLogin", true)
        compose.onNodeWithTag("loginCode").performTextInput(code)
        compose.onNodeWithTag("loginPassword").performTextInput(password)
        click("submitLogin", true)
    }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        Thread.sleep(350)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        val dir = File(instrumentation.targetContext.filesDir, "screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun passengerAccountAndRegistrationAreUnavailableInThePrototype() {
        compose.onNodeWithTag("welcomeRegister").assertDoesNotExist()
        signIn("76543210")
        compose.onNodeWithTag("loginError").assertExists()
        compose.onNodeWithTag("loginRegister").assertDoesNotExist()
        compose.onNodeWithTag("navJourney").assertDoesNotExist()
        compose.onNodeWithTag("navFleet").assertDoesNotExist()
    }
    @Test fun loginDoesNotAdvertiseAnyDemoAccount() {
        click("welcomeLogin", true)
        listOf("conductor.demo", "76543210", "supervisor.demo", "SafeBus123").forEach {
            compose.onNodeWithText(it, substring = true).assertDoesNotExist()
        }
        screenshot("login-clean")
    }

    @Test fun supervisorFleetSearchFiltersMapAndCapacityWorkLocally() {
        signIn("supervisor.demo")
        compose.onNodeWithTag("navJourney").assertDoesNotExist()
        screenshot("supervisor-fleet-list")
        click("fleetFilterStale", true)
        compose.onNodeWithTag("fleetBusEX-108").assertExists()
        compose.onNodeWithTag("fleetBusA1B-702").assertDoesNotExist()
        click("fleetFilterAll", true)
        compose.onNodeWithTag("fleetSearch").performTextInput("missing")
        compose.onNodeWithTag("emptyFleet").assertExists()
        compose.onNodeWithTag("fleetSearch").performTextClearance()
        click("fleetViewMap", true)
        click("mapBusEX-108", true)
        screenshot("supervisor-fleet-map")
        click("busDetailsEX-108", true)
        screenshot("supervisor-bus-detail")
        click("editCapacity", true)
        compose.onNodeWithTag("capacityInput").performTextReplacement("0")
        compose.onNodeWithTag("saveCapacity").assertIsNotEnabled()
        compose.onNodeWithTag("capacityInput").performTextReplacement("45")
        click("saveCapacity")
        compose.onNodeWithTag("occupancyEX-108").assertTextEquals("Occupancy · 40 of 45")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("occupancyEX-108").assertTextEquals("Occupancy · 40 of 45")
        click("navAccount")
        click("mapAvailability", true)
        click("navFleet")
        click("fleetViewMap", true)
        compose.onNodeWithTag("mapFallback").assertExists()
        compose.onNodeWithTag("fleetMap").assertDoesNotExist()
        screenshot("supervisor-map-fallback")
    }

    @Test fun supervisorMustStartAttentionAndProvideOutcomeBeforeClosure() {
        signIn("supervisor.demo")
        click("navEmergencies")
        screenshot("supervisor-emergencies")
        click("opendriver", true)
        compose.onNodeWithTag("closeCase").assertDoesNotExist()
        click("startAttention", true)
        compose.onNodeWithTag("companyStatusInProgress").assertExists()
        compose.onNodeWithTag("closeCase").assertIsNotEnabled()
        compose.onNodeWithTag("caseOutcome").performTextInput("Operations contacted the driver and resolved the incident.")
        screenshot("supervisor-attention")
        click("closeCase", true)
        compose.onNodeWithTag("caseClosed").assertExists()
        screenshot("supervisor-case-closed")
        click("navEmergencies")
        compose.onNodeWithTag("casedriver").assertDoesNotExist()
        click("casesFilterHistory", true)
        compose.onNodeWithTag("casedriver").assertExists()
    }

    @Test fun supervisorApprovalCreatesHighEmergencyOnlyAfterReview() {
        signIn("supervisor.demo")
        click("navEmergencies")
        compose.onNodeWithTag("casegroup").assertDoesNotExist()
        click("navReviews")
        click("reviewGroup", true)
        screenshot("supervisor-review-group")
        click("groupEvidence0", true)
        screenshot("supervisor-review-evidence")
        click("closeEvidence")
        click("approveGroup", true)
        compose.onNodeWithTag("groupApproved").assertExists()
        click("opengroup", true)
        compose.onNodeWithText("HIGH").assertExists()
        compose.onNodeWithText("CRITICAL").assertDoesNotExist()
        compose.onNodeWithTag("companyStatusActive").assertExists()
        screenshot("supervisor-approved-high")
    }

    @Test fun supervisorRejectionRequiresReasonAndCreatesNoEmergency() {
        signIn("supervisor.demo")
        click("navReviews")
        click("reviewGroup", true)
        click("rejectGroup", true)
        compose.onNodeWithTag("confirmReject").assertIsNotEnabled()
        compose.onNodeWithTag("rejectionReason").performTextInput("The evidence describes unrelated incidents.")
        click("confirmReject")
        compose.onNodeWithTag("reviewDecision").assertExists()
        compose.onNodeWithText("The evidence describes unrelated incidents.").assertExists()
        screenshot("supervisor-review-rejected")
        click("navEmergencies")
        compose.onNodeWithTag("casegroup").assertDoesNotExist()
        compose.onNodeWithTag("reviewGroup").assertDoesNotExist()
    }

    @Test fun supervisorAssignmentConflictCanBeResolvedAndLanguageCanChange() {
        signIn("supervisor.demo")
        click("navAssignments")
        click("newAssignment", true)
        repeat(3) { click("assignmentNext", true) }
        compose.onNodeWithTag("assignmentConflict").assertExists()
        compose.onNodeWithTag("assignmentNext").assertIsNotEnabled()
        screenshot("supervisor-assignment-conflict")
        click("assignmentPeriod14:00 - 22:00", true)
        click("assignmentNext", true)
        screenshot("supervisor-assignment-review")
        click("confirmAssignment", true)
        compose.onNodeWithTag("createdAssignment").assertExists()
        screenshot("supervisor-assignments")
        click("navAccount")
        click("languageEs", true)
        compose.onNodeWithTag("navFleet").assertTextEquals("Flota")
        screenshot("supervisor-account-es")
        click("signOut", true)
        compose.onNodeWithTag("welcomeLogin").assertExists()
        compose.onNodeWithTag("navFleet").assertDoesNotExist()
    }
}
