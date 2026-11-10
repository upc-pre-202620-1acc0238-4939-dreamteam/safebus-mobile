package com.dreamteam.safebus

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class ConnectedLaunchTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun productionLauncherOpensConnectedAccessWithoutPrototypeControls() {
        compose.waitUntil(10000) { compose.onAllNodesWithTag("coreSignIn").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("coreSignIn").assertIsNotEnabled()
        compose.onNodeWithTag("validQr").assertDoesNotExist()
        compose.onNodeWithTag("useSampleEvidence").assertDoesNotExist()
        compose.onNodeWithTag("nav:FLEET").assertDoesNotExist()
    }
}
