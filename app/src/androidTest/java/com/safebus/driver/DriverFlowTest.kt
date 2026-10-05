package com.safebus.driver

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Tests user-visible paths from report 3.1.4, rather than mocked backend behavior. */
class DriverFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun click(tag: String, scroll: Boolean = false) {
        val node = compose.onNodeWithTag(tag)
        if (scroll) node.performScrollTo()
        node.performClick()
    }

    private fun signIn() {
        click("welcomeLogin")
        compose.onNodeWithTag("loginCode").performTextInput("conductor.demo")
        compose.onNodeWithTag("loginPassword").performTextInput("SafeBus123")
        click("submitLogin", true)
        compose.onNodeWithTag("scanQr").assertExists()
    }

    private fun activateShift() {
        signIn()
        click("scanQr", true)
        click("validQr", true)
        compose.onNodeWithTag("validatedShift").assertExists()
        click("goToShift", true)
        compose.onNodeWithTag("activeShift").assertIsDisplayed()
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        // Compose semantics can settle before the emulator's GPU presents its frame.
        instrumentation.waitForIdleSync()
        Thread.sleep(350)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        val dir = File(instrumentation.targetContext.filesDir, "screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun invalidCredentialsDoNotCreateSession() {
        click("welcomeLogin")
        compose.onNodeWithTag("loginCode").performTextInput("wrong")
        compose.onNodeWithTag("loginPassword").performTextInput("wrong")
        click("submitLogin", true)
        compose.onNodeWithTag("loginError").assertExists()
        compose.onNodeWithTag("navShift").assertDoesNotExist()
        screenshot("login-error")
    }

    @Test fun qrValidationAndSingleTapEmergencyFollowTheDriverFlow() {
        screenshot("welcome")
        signIn()
        screenshot("assigned-shift")
        click("scanQr", true)
        screenshot("scanner")
        click("invalidQr", true)
        compose.onNodeWithTag("qrError").assertExists()
        compose.onNodeWithTag("emergencyButton").assertDoesNotExist()
        click("validQr", true)
        screenshot("validated-shift")
        click("goToShift", true)
        screenshot("active-shift")
        click("emergencyButton")
        compose.onNodeWithTag("emergencyMessage").assertTextEquals("Emergency sent to your company at 22:41.")
        screenshot("emergency-sent")
        click("viewDetails", true)
        compose.onNodeWithTag("statusActive").assertExists()
    }

    @Test fun activeShiftKeepsEmergencyAvailableAcrossAllDriverTabs() {
        activateShift()
        click("navAlerts")
        compose.onNodeWithTag("emergencyButton").assertIsDisplayed()
        click("navAccount")
        compose.onNodeWithTag("emergencyButton").assertIsDisplayed()
        click("emergencyButton")
        compose.onNodeWithTag("statusActive").assertIsDisplayed()
    }

    @Test fun offlineEmergencyRecoversAndShowsCompanyOutcome() {
        activateShift()
        click("navAccount")
        click("connectionSwitch", true)
        click("emergencyButton")
        compose.onNodeWithTag("statusPending").assertIsDisplayed()
        compose.onNodeWithTag("emergencyMessage").assertTextEquals("No connection. Your emergency is saved and will be sent automatically.")
        screenshot("pending-transmission")
        click("navAccount")
        click("connectionSwitch", true)
        click("demoInProgress", true)
        click("navAlerts")
        click("currentAlertDetails", true)
        compose.onNodeWithTag("statusInProgress").assertExists()
        screenshot("in-progress")
        click("navAccount")
        click("demoClosed", true)
        click("navAlerts")
        click("currentAlertDetails", true)
        compose.onNodeWithTag("statusClosed").assertExists()
        compose.onNodeWithText("Company outcome").assertExists()
        screenshot("closed-emergency")
    }

    @Test fun missingAssignmentAndEmptyHistoryHaveRecoveryStates() {
        signIn()
        click("navAccount")
        click("assignmentSwitch", true)
        click("emptyHistorySwitch", true)
        click("navShift")
        compose.onNodeWithTag("noShift").assertExists()
        compose.onNodeWithTag("emergencyButton").assertDoesNotExist()
        screenshot("no-assignment")
        click("navAlerts")
        compose.onNodeWithTag("emptyAlerts").assertExists()
        screenshot("empty-alerts")
        click("navShift")
        click("refreshAssignment", true)
        compose.onNodeWithTag("scanQr").assertExists()
    }

    @Test fun languageAndLocalStateSurviveActivityRecreation() {
        click("languageEs")
        compose.onNodeWithTag("welcomeLogin").assertTextEquals("Iniciar sesión")
        activateShift()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("activeShift").assertExists()
        compose.onNodeWithTag("emergencyButton").assertTextEquals("Emergencia")
        click("navAccount")
        click("signOut", true)
        compose.onNodeWithTag("welcomeLogin").assertExists()
        compose.onNodeWithTag("navShift").assertDoesNotExist()
    }

    @Test fun cancelScannerAndConfirmShiftClosureRespectTheFlow() {
        signIn()
        click("scanQr", true)
        compose.onNodeWithText("Cancel").performScrollTo().performClick()
        compose.onNodeWithTag("emergencyButton").assertDoesNotExist()
        click("scanQr", true)
        click("validQr", true)
        click("goToShift", true)
        click("closeShift", true)
        click("cancelCloseShift")
        compose.onNodeWithTag("activeShift").assertExists()
        click("closeShift", true)
        click("confirmCloseShift")
        compose.onNodeWithTag("scanQr").assertExists()
        compose.onNodeWithTag("emergencyButton").assertDoesNotExist()
    }
}
