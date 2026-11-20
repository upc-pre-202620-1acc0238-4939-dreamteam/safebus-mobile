package com.dreamteam.safebus.safetycase.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.dreamteam.safebus.shared.ui.*
import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.safetycase.domain.*
import java.time.Instant
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class SafetyScreensTest {
    @get:Rule val compose = createComposeRule()
    @Test fun supervisorRejectionRequiresAReason() {
        var declined: String? = null
        val queue = ReviewQueue(emptyList(), listOf(ReviewGroup(1, "G1", 1, 3, emptyList())), Instant.EPOCH)
        compose.setContent { CoreTheme { SafetyListScreen(ScreenState(SafetyState(queue = queue)), SafetyTab.REVIEWS, {}, {}, {}, decline = { _, reason, _ -> declined = reason }) } }
        compose.onNodeWithTag("decline:1").assertDoesNotExist()
        compose.onNodeWithTag("reviewGroup:1").performScrollTo().performClick()
        compose.onNodeWithTag("decline:1").performScrollTo().performClick()
        compose.onNodeWithTag("confirmDecision").assertIsNotEnabled()
        assertNull(declined)
        compose.onNodeWithTag("decisionReason").performTextInput("Reviewed evidence")
        compose.onNodeWithTag("confirmDecision").performClick()
        assertEquals("Reviewed evidence", declined)
    }
}
