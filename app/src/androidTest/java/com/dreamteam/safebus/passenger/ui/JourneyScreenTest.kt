package com.dreamteam.safebus.passenger.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.dreamteam.safebus.passenger.domain.Journey
import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.shared.ui.*
import java.time.Instant
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class JourneyScreenTest {
    @get:Rule val compose = createComposeRule()
    @Test fun confirmedJourneyShowsServerFieldsAndKeepsEndExplicit() {
        var ends = 0
        val journey = Journey(1, 1, "REAL-123", "Real Company", "VERIFIED", "Route", "A", "B", "Driver", Instant.EPOCH)
        compose.setContent { CoreTheme { JourneyScreen(ScreenState(PassengerState(journey)), TrackingState(), {}, { ends++ }, {}, {}) } }
        compose.onNodeWithTag("verifiedBus").assertTextEquals("REAL-123")
        compose.onNodeWithTag("endRealJourney").performScrollTo().performClick()
        assertEquals(0, ends)
        compose.onNodeWithTag("confirmRealJourneyEnd").performClick()
        assertEquals(1, ends)
    }
    @Test fun rejectedJourneyKeepsOnlyStartFormWithoutInvalidAssociation() {
        var associations = 0
        compose.setContent { CoreTheme { JourneyScreen(ScreenState(PassengerState(), failure = Failure.fromStatus(422)), TrackingState(), { associations++ }, {}, {}, {}) } }
        compose.onNodeWithTag("coreError").assertExists()
        compose.onNodeWithTag("verifiedBus").assertDoesNotExist()
        compose.onNodeWithTag("newRealRequest").assertDoesNotExist()
        assertEquals(0, associations)
    }
}
