package com.dreamteam.safebus.shared.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.dreamteam.safebus.shared.domain.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ContactScreenTest {
    @get:Rule val compose = createComposeRule()
    @Test fun requiresConsentAndDoesNotShowAReceiptAfterRejectedSubmission() {
        var sent: ContactSubmission? = null
        compose.setContent { CoreTheme { ContactScreen(ScreenState(ServiceState(), failure = Failure.fromStatus(422)), { sent = it }, {}, {}, {}) } }
        compose.onNodeWithTag("contactCompany").performTextInput("Company")
        compose.onNodeWithTag("contactName").performTextInput("Contact")
        compose.onNodeWithTag("contactEmail").performTextInput("contact@example.com")
        compose.onNodeWithTag("sendContact").assertIsNotEnabled(); assertNull(sent)
        compose.onNodeWithTag("contactReceipt").assertDoesNotExist()
        compose.onNodeWithTag("contactConsent").performScrollTo().performClick()
        compose.onNodeWithTag("sendContact").performScrollTo().performClick()
        assertTrue(sent!!.valid())
    }
}
