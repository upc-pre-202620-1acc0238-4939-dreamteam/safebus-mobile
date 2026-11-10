package com.dreamteam.safebus

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.dreamteam.safebus.fleet.domain.FleetChange
import com.dreamteam.safebus.passenger.domain.*
import com.dreamteam.safebus.shared.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.util.UUID
import kotlin.random.Random

class ConnectedRoleFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun await(tag: String) = compose.waitUntil(15000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    private fun click(tag: String, scroll: Boolean = false) {
        await(tag)
        val node = compose.onNodeWithTag(tag)
        if (scroll) node.performScrollTo()
        node.performClick()
    }
    private fun login(id: String, password: String) {
        await("coreSignIn")
        compose.onNodeWithTag("coreLoginId").performTextClearance()
        compose.onNodeWithTag("coreLoginId").performTextInput(id)
        compose.onNodeWithTag("corePassword").performTextInput(password)
        click("coreSignIn", true)
        await("nav:ACCOUNT")
    }
    private fun logout() { click("nav:ACCOUNT"); click("realSignOut", true); click("confirmRealSignOut"); await("coreSignIn") }
    private fun <T> value(result: DomainResult<T>): T = when (result) {
        is DomainResult.Success -> result.value
        is DomainResult.Error -> throw AssertionError("Backend failure: ${result.failure.kind}/${result.failure.code}")
    }
    @Test fun allRolesNavigateRealOperationsAndPreserveIsolationAcrossLogout() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("backend") == "true")
        val supervisor = BackendIntegrationTest.Client("http://10.0.2.2:8080")
        val suffix = UUID.randomUUID().toString().take(8)
        val bus = runBlocking {
            value(supervisor.auth.signIn("sup-001", "Safebus2024!"))
            value(supervisor.fleet.save(FleetChange.Bus("UI-$suffix")))
        }
        val driver = runBlocking { value(supervisor.fleet.save(FleetChange.Driver("UI driver", "ui-$suffix", "Integration2026!"))) }
        val route = runBlocking { value(supervisor.fleet.save(FleetChange.Route("UI route", "A", "B"))) }
        runBlocking { value(supervisor.fleet.save(FleetChange.Assignment(driver.id, bus.id, route.id, Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600)))) }
        login("ui-$suffix", "Integration2026!")
        await("qrContent")
        compose.onNodeWithTag("nav:FLEET").assertDoesNotExist()
        compose.onNodeWithTag("qrContent").performTextInput("invalid")
        click("activateShift", true)
        await("coreError")
        compose.onNodeWithTag("realPanic").assertDoesNotExist()
        val account = requireNotNull((compose.activity.application as SafeBusApplication).container.value?.account)
        runBlocking { assertNull(value(account.trip.restore())) }
        compose.onNodeWithTag("qrContent").performTextInput(requireNotNull(driver.credential))
        click("activateShift", true)
        await("realPanic")
        click("nav:ACCOUNT")
        compose.onNodeWithTag("realPanic").assertIsDisplayed()
        click("realPanic")
        await("deliveryNotice")
        val emergency = runBlocking { value(supervisor.safety.queue()).emergencies.single { it.busId == bus.id } }
        assertEquals("CRITICAL", emergency.priority)
        logout()

        login("sup-001", "Safebus2024!")
        compose.onNodeWithTag("nav:SHIFT").assertDoesNotExist()
        compose.onNodeWithTag("realPanic").assertDoesNotExist()
        click("nav:EMERGENCIES")
        click("attend:${emergency.id}", true)
        click("confirmDecision")
        click("close:${emergency.id}", true)
        compose.onNodeWithTag("confirmDecision").assertIsNotEnabled()
        compose.onNodeWithTag("decisionReason").performTextInput("Situation resolved")
        compose.onNodeWithTag("decisionResponse").performTextInput("Company attended")
        click("confirmDecision")
        compose.waitUntil(15000) { compose.onAllNodesWithTag("close:${emergency.id}").fetchSemanticsNodes().isEmpty() }
        runBlocking { assertTrue(value(supervisor.safety.queue()).emergencies.none { it.id == emergency.id }) }
        logout()

        val passenger = BackendIntegrationTest.Client("http://10.0.2.2:8080")
        val dni = Random.nextInt(10_000_000, 99_999_999).toString()
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        val output = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output); bitmap.recycle()
        runBlocking { value(passenger.passenger.register(Registration(dni, "Integration2026!", true, Photo(output.toByteArray(), "image/png")))) }
        login(dni, "Integration2026!")
        compose.onNodeWithTag("nav:HISTORY").assertIsNotEnabled()
        compose.onNodeWithTag("nav:FLEET").assertDoesNotExist()
        compose.onNodeWithTag("qrContent").performTextInput(BusQr(requireNotNull(bus.credential), bus.id).encoded())
        click("startRealJourney", true)
        await("verifiedBus")
        compose.onNodeWithTag("nav:HISTORY").assertIsEnabled()
        click("newRealRequest", true)
        compose.onNodeWithTag("realIncidentMessage").performTextInput("Actual report text")
        compose.onNodeWithTag("sendRealRequest").performScrollTo().assertIsNotEnabled()
        click("requestBack", true)
        click("nav:JOURNEY")
        click("endRealJourney", true)
        click("confirmRealJourneyEnd")
        await("startRealJourney")
        compose.onNodeWithTag("nav:HISTORY").assertIsNotEnabled()
        logout()

        login("ui-$suffix", "Integration2026!")
        await("realPanic")
        click("closeShift", true)
        click("confirmShiftClose")
        compose.waitUntil(15000) { compose.onAllNodesWithTag("realPanic").fetchSemanticsNodes().isEmpty() }
        runBlocking { assertNull(value(supervisor.fleet.overview()).buses.single { it.id == bus.id }.shiftId) }
        logout()
        compose.onNodeWithTag("verifiedBus").assertDoesNotExist()
        compose.onNodeWithTag("nav:ACCOUNT").assertDoesNotExist()
    }
}
