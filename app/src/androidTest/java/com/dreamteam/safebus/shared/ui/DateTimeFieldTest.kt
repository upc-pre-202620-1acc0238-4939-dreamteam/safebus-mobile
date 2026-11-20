package com.dreamteam.safebus.shared.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import com.dreamteam.safebus.R
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DateTimeFieldTest {
    @get:Rule val compose = createComposeRule()
    @Test fun calendarAndClockSelectWithoutExposingAnEditableIsoField() {
        val value = mutableStateOf("2028-04-15T09:30")
        compose.setContent { CoreTheme { DateTimeField(R.string.core_start_time, value.value, { value.value = it }, "date") } }
        compose.onNodeWithTag("date").assertHasClickAction().performClick()
        compose.onNodeWithText("Sunday, April 16, 2028").performClick()
        compose.onNodeWithTag("dateConfirm").performClick()
        compose.onNodeWithContentDescription("10 hours").performClick()
        compose.onNodeWithContentDescription("Select minutes").performClick()
        compose.onNodeWithContentDescription("45 minutes").performClick()
        compose.onNodeWithTag("timeConfirm").performClick()
        assertEquals("2028-04-16T10:45", value.value)
        compose.onNode(hasSetTextAction()).assertDoesNotExist()
        compose.onNodeWithText("2028-04-16T10:45").assertDoesNotExist()
    }
    @Test fun cancellingEitherDialogKeepsOriginalValueAndDoesNotWrite() {
        var writes = 0
        compose.setContent { CoreTheme { DateTimeField(R.string.core_start_time, "2028-04-15T09:30", { writes++ }, "date") } }
        compose.onNodeWithTag("date").performClick()
        compose.onNodeWithTag("dateCancel").performClick()
        compose.onNodeWithTag("date").performClick()
        compose.onNodeWithTag("dateConfirm").performClick()
        compose.onNodeWithContentDescription("10 hours").performClick()
        compose.onNodeWithTag("timeCancel").performClick()
        assertEquals(0, writes)
        compose.onNodeWithTag("date").assertTextContains("Apr 15, 2028, 9:30", substring = true)
    }
    @Test fun dialogRestorationKeepsClockSelectionAndCalendarBackNavigation() {
        val restoration = StateRestorationTester(compose)
        var result = ""
        restoration.setContent { CoreTheme { DateTimeField(R.string.core_start_time, "2028-04-15T09:30", { result = it }, "date") } }
        compose.onNodeWithTag("date").performClick()
        compose.onNodeWithTag("dateConfirm").performClick()
        compose.onNodeWithContentDescription("10 hours").performClick()
        compose.onNodeWithContentDescription("Select minutes").performClick()
        compose.onNodeWithContentDescription("45 minutes").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("timeBack").performClick()
        compose.onNodeWithTag("dateConfirm").performClick()
        compose.onNodeWithTag("timeConfirm").performClick()
        assertEquals("2028-04-15T10:45", result)
    }
    @Test fun disabledFieldDoesNotOpenCalendar() {
        compose.setContent { CoreTheme { DateTimeField(R.string.core_start_time, "", {}, "date", enabled = false) } }
        compose.onNodeWithTag("date").assertIsNotEnabled().performClick()
        compose.onNodeWithTag("dateConfirm").assertDoesNotExist()
    }
}
