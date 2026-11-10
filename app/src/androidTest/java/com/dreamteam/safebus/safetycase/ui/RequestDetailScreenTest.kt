package com.dreamteam.safebus.safetycase.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.shared.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class RequestDetailScreenTest {
    @get:Rule val compose = createComposeRule()
    @Test fun forbiddenDetailShowsFeedbackWithoutPrivateContentAndAllowsReturn() {
        var returned = false
        compose.setContent { CoreTheme { RequestDetailScreen(ScreenState(null, failure = Failure.fromStatus(403)), {}, { returned = true }) } }
        compose.onNodeWithTag("coreError").assertExists()
        compose.onNodeWithTag("requestDetailMessage").assertDoesNotExist()
        compose.onNodeWithTag("detailBack").performClick(); assertTrue(returned)
    }
}
