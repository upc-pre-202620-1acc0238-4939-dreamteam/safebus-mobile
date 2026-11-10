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
        compose.setContent { CoreTheme { AccessScreen(ScreenState(AuthState()), { a, b -> submitted = a to b }, {}, {}) } }
        compose.onNodeWithTag("coreSignIn").assertIsNotEnabled()
        listOf("conductor.demo", "supervisor.demo", "Safebus2024!").forEach { compose.onNodeWithText(it).assertDoesNotExist() }
        compose.onNodeWithTag("coreLoginId").performTextInput("test-account")
        compose.onNodeWithTag("corePassword").performTextInput("test-password")
        compose.onNodeWithTag("coreSignIn").performScrollTo().performClick()
        assertEquals("test-account" to "test-password", submitted)
    }
    @Test fun registrationHasNoSamplePhotoAndAuthenticationErrorDoesNotNavigate() {
        var registrations = 0
        compose.setContent { CoreTheme { AccessScreen(ScreenState(AuthState(), failure = Failure.fromStatus(401)), { _, _ -> }, { registrations++ }, {}) } }
        compose.onNodeWithTag("coreError").assertExists()
        compose.onNodeWithTag("openRegistration").performScrollTo().performClick()
        compose.onNodeWithTag("coreRegister").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("photoSelected").assertDoesNotExist()
        assertEquals(0, registrations)
    }
}
