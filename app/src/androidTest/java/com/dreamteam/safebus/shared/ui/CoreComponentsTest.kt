package com.dreamteam.safebus.shared.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.R
import org.junit.Rule
import org.junit.Test

class CoreComponentsTest {
    @get:Rule val compose = createComposeRule()
    @Test fun errorAndLoadingAreVisibleAndDisabledActionsCannotSubmit() {
        compose.setContent { CoreTheme { CorePage(R.string.app_name, busy = true, failure = Failure(FailureKind.NETWORK)) {
            CoreButton(R.string.core_confirm, "confirm", enabled = false) { error("Disabled action") }
        } } }
        compose.onNodeWithTag("coreError").assertExists()
        compose.onNodeWithTag("coreLoading").assertExists()
        compose.onNodeWithTag("confirm").assertIsNotEnabled()
    }
}
