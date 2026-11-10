package com.dreamteam.safebus

import android.content.res.Configuration
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class SpanishResourcesTest {
    @Test fun spanishContextExplainsCredentialAndJourneyFailures() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag("es-PE")) }
        val localized = context.createConfigurationContext(configuration)
        assertEquals("La credencial QR venció. Solicita una nueva a tu empresa.", localized.getString(R.string.core_credential_expired))
        assertTrue(localized.getString(R.string.core_existing_journey).contains("viaje activo"))
    }
}
