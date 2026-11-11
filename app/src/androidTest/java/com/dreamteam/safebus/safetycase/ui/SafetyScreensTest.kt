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
    @Test fun reviewKeepsEvidenceVisibleAndSendsOnlyOnExplicitConfirmation() {
        var sends = 0
        var edits = 0
        val photo = Photo(byteArrayOf(1), "image/jpeg")
        compose.setContent { CoreTheme { CorePage(com.dreamteam.safebus.R.string.ui_review_request) {
            RequestReview("Recorded incident", photo, false, { sends++ }, { edits++ })
        } } }
        compose.onNodeWithText("Recorded incident").assertExists()
        assertEquals(0, sends)
        compose.onNodeWithTag("editRealRequest").performScrollTo().performClick()
        assertEquals(1, edits)
        assertEquals(0, sends)
        compose.onNodeWithTag("sendRealRequest").performScrollTo().performClick()
        assertEquals(1, sends)
    }
    @Test fun completedTabSeparatesClosedRequests() {
        val active = PassengerRequest("00000000-0000-0000-0000-000000000001", "Active incident", "COLLECTING", Instant.EPOCH, 2, null, null, null)
        val closed = active.copy(id = "00000000-0000-0000-0000-000000000002", message = "Closed incident", status = "EXPIRED")
        compose.setContent { CoreTheme { SafetyListScreen(ScreenState(SafetyState(requests = listOf(active, closed))), SafetyTab.REQUESTS, {}, {}, {}) } }
        compose.onNodeWithText("Active incident").assertExists()
        compose.onNodeWithText("Closed incident").assertDoesNotExist()
        compose.onNodeWithTag("cases:true").performClick()
        compose.onNodeWithText("Closed incident").assertExists()
        compose.onNodeWithText("Active incident").assertDoesNotExist()
    }
    @Test fun pendingRequestDoesNotExposeRemoteEvidenceBeforeAcknowledgement() {
        val id = "00000000-0000-0000-0000-000000000001"
        val request = PassengerRequest(id, "Pending report", "PENDING", Instant.EPOCH, null, null, null, null)
        compose.setContent { CoreTheme { SafetyListScreen(ScreenState(SafetyState(requests = listOf(request))), SafetyTab.REQUESTS, {}, {}, {}) } }
        compose.onNodeWithTag("deliveryNotice").assertExists()
        compose.onNodeWithTag("evidence:$id").assertIsNotEnabled()
    }
    @Test fun requestCannotSubmitWithoutAnIncidentPhotoEvenAfterAnError() {
        var submitted = 0
        compose.setContent { CoreTheme { RequestFormScreen(ScreenState(SafetyState(), failure = Failure.fromStatus(422)), { _, _ -> submitted++ }, {}) } }
        compose.onNodeWithTag("realIncidentMessage").performTextInput("Incident at the rear door")
        compose.onNodeWithTag("reviewRealRequest").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("coreError").assertExists()
        assertEquals(0, submitted)
    }
    @Test fun rejectionRequiresAReasonAndDoesNotExposeApprovalInPassengerHistory() {
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
