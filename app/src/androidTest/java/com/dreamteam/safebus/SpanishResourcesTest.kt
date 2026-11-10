package com.dreamteam.safebus

import android.content.res.Configuration
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class SpanishResourcesTest {
    @Test fun defaultAndSpanishResourcesKeepRecoveryGuidanceAndVersionMeaning() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        for (language in listOf("en-US", "es-PE")) {
            val configuration = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) }
            val localized = context.createConfigurationContext(configuration)
            assertEquals("Demasiados intentos. Espera un instante antes de volver a intentar.", localized.getString(R.string.core_rate_limit))
            assertEquals("Actualmente no tienes un turno asignado.", localized.getString(R.string.core_no_assignment))
            assertEquals("Actualmente este bus no tiene un turno activo.", localized.getString(R.string.core_bus_not_serving))
            assertEquals("Turno activo: 42", localized.getString(R.string.core_active_shift, 42L))
            assertTrue(localized.getString(R.string.core_terms).endsWith("versión de octubre de 2026."))
        }
    }
    @Test fun spanishContextExplainsCredentialAndJourneyFailures() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag("es-PE")) }
        val localized = context.createConfigurationContext(configuration)
        assertEquals("La credencial QR venció. Solicita una nueva a tu empresa.", localized.getString(R.string.core_credential_expired))
        assertTrue(localized.getString(R.string.core_existing_journey).contains("viaje activo"))
    }
}
