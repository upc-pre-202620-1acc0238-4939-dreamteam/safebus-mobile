package com.dreamteam.safebus.iam.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.dreamteam.safebus.shared.ui.*
import com.dreamteam.safebus.shared.domain.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AccessScreenTest {
    @get:Rule val compose = createComposeRule()
    @Test fun loginRequiresCredentialsAndDoesNotAdvertiseDemoAccounts() {
        var submitted: Pair<String, String>? = null
        compose.setContent { CoreTheme { AccessScreen(ScreenState(AuthState()), { a, b -> submitted = a to b }, {}) } }
        compose.onNodeWithTag("welcomeLogin").performScrollTo().performClick()
        compose.onNodeWithTag("coreSignIn").assertIsNotEnabled()
        listOf("conductor.demo", "supervisor.demo", "Safebus2024!").forEach { compose.onNodeWithText(it).assertDoesNotExist() }
        compose.onNodeWithTag("coreLoginId").performTextInput("test-account")
        compose.onNodeWithTag("corePassword").performTextInput("test-password")
        compose.onNodeWithTag("coreSignIn").performScrollTo().performClick()
        assertEquals("test-account" to "test-password", submitted)
    }
    @Test fun passengerLoginRejectionShowsGuidanceWithoutNavigationOrRegistration() {
        val state = ScreenState(AuthState(), failure = Failure(FailureKind.FORBIDDEN, "ANDROID_ROLE_UNSUPPORTED"))
        compose.setContent { CoreTheme { AccessScreen(state, { _, _ -> }, {}) } }
        compose.onNodeWithTag("welcomeRegister").assertDoesNotExist()
        compose.onNodeWithTag("welcomeLogin").performScrollTo().performClick()
        compose.onNodeWithTag("coreError").assertExists()
        compose.onNodeWithText("This Android app is for supervisors and drivers. Use the team’s Flutter app for passenger access.").assertExists()
        compose.onNodeWithTag("openRegistration").assertDoesNotExist()
        compose.onNodeWithTag("nav:ACCOUNT").assertDoesNotExist()
        assertNull(state.value.session)
    }
    @Test fun welcomeExposesLanguageAndServerAccessWithoutDemoCredentials() {
        var language = ""
        compose.setContent { CoreTheme { AccessScreen(ScreenState(AuthState()), { _, _ -> }, {}, language = "en", onLanguage = { language = it }) } }
        compose.onNodeWithTag("language:es").performClick()
        assertEquals("es", language)
        compose.onNodeWithTag("welcomeLogin").assertExists()
        compose.onNodeWithTag("welcomeRegister").assertDoesNotExist()
        compose.onNodeWithTag("corePassword").assertDoesNotExist()
    }
}
