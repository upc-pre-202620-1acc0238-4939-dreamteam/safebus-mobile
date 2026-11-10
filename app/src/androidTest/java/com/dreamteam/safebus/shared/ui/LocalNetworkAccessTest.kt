package com.dreamteam.safebus.shared.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class LocalNetworkAccessTest {
    @get:Rule val compose = createComposeRule()
    @Test fun denialKeepsNetworkContentBlockedAndDoesNotChangeConnection() {
        val denied = mutableStateOf(false)
        var requests = 0
        var connectionChanges = 0
        var networkContentShown = false
        compose.setContent { CoreTheme {
            LocalNetworkAccess(true, denied.value, { requests++; denied.value = true }, { connectionChanges++ }) {
                networkContentShown = true
                Text("Network content")
            }
        } }
        compose.onNodeWithTag("allowLocalNetwork").performClick()
        compose.onNodeWithTag("localNetworkDenied").assertIsDisplayed()
        compose.onNodeWithText("Network content").assertDoesNotExist()
        assertEquals(1, requests)
        assertFalse(networkContentShown)
        assertEquals(0, connectionChanges)
        compose.onNodeWithTag("localNetworkSettings").performClick()
        assertEquals(1, connectionChanges)
    }
    @Test fun grantedAccessOrPublicServerDisplaysContentWithoutAPrompt() {
        val blocked = mutableStateOf(true)
        compose.setContent { CoreTheme { LocalNetworkAccess(blocked.value, false, { blocked.value = false }, {}) { Text("Network content") } } }
        compose.onNodeWithTag("allowLocalNetwork").performClick()
        compose.onNodeWithText("Network content").assertIsDisplayed()
        compose.onNodeWithTag("allowLocalNetwork").assertDoesNotExist()
        compose.runOnIdle { blocked.value = true }
        compose.onNodeWithText("Network content").assertDoesNotExist()
    }
}
