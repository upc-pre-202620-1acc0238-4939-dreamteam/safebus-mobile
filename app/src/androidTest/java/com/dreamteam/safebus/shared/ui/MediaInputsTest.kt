package com.dreamteam.safebus.shared.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class MediaInputsTest {
    @get:Rule val compose = createComposeRule()
    @Test fun acceptsActualQrTextAndDoesNotSupplySampleEvidence() {
        var entered = ""
        compose.setContent { CoreTheme { Column {
            var qr by remember { mutableStateOf("") }
            QrInput(qr, { qr = it; entered = it })
            PhotoChooser(null, {}, enabled = false)
        } } }
        compose.onNodeWithTag("qrContent").performTextInput("actual-credential")
        assertEquals("actual-credential", entered)
        compose.onNodeWithTag("photoSelected").assertDoesNotExist()
        compose.onNodeWithTag("choosePhoto").assertIsNotEnabled()
    }
}
