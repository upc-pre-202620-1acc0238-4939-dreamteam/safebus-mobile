package com.dreamteam.safebus.shared.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dreamteam.safebus.R
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ReportStyleTest {
    @get:Rule val compose = createComposeRule()

    @Test fun reportPaletteAndTypographyFollowBothThemes() {
        val dark = mutableStateOf(false)
        compose.setContent { CoreTheme(dark.value) {
            val colors = MaterialTheme.colorScheme
            assertEquals(Color(0xFF0D2C54), colors.primary)
            assertEquals(Color(0xFF00796B), colors.secondary)
            assertEquals(Color.White, colors.onPrimary)
            assertEquals(if (dark.value) Color(0xFF0F1720) else Color(0xFFF5F7FA), colors.background)
            assertEquals(if (dark.value) Color(0xFF18222E) else Color.White, colors.surface)
            assertEquals(24.sp, MaterialTheme.typography.headlineSmall.fontSize)
            assertEquals(32.sp, MaterialTheme.typography.headlineSmall.lineHeight)
            assertNotNull(MaterialTheme.typography.bodyLarge.fontFamily)
        } }
        compose.runOnIdle { dark.value = true }
        compose.waitForIdle()
    }

    @Test fun safetyPrioritiesAndPendingGroupsHaveDistinctSemanticColors() {
        assertEquals(R.color.core_emergency, stateColorResource("CRITICAL"))
        assertEquals(R.color.core_high, stateColorResource("HIGH"))
        assertEquals(R.color.core_pending, stateColorResource("AWAITING_APPROVAL"))
        assertEquals(R.color.core_information, stateColorResource("PENDING"))
        assertEquals(R.color.core_closed, stateColorResource("CLOSED"))
        assertEquals(R.color.core_inactive, stateColorResource("EXPIRED"))
    }

    @Test fun emergencyMeetsReportSizeAndActivatesWithOneTap() {
        var sent = 0
        compose.setContent { CoreTheme { CoreButton(R.string.core_emergency, "panic", danger = true) { sent++ } } }
        compose.onNodeWithTag("panic").assertHeightIsAtLeast(96.dp).performClick()
        compose.runOnIdle { assertEquals(1, sent) }
    }
}
