package com.dreamteam.safebus

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.dreamteam.safebus.fleet.domain.FleetChange
import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.trip.data.RemoteCountingRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.util.UUID

class RemainingRoleFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun await(tag: String) = compose.waitUntil(20000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    private fun click(tag: String, scroll: Boolean = true) {
        await(tag)
        val node = compose.onNodeWithTag(tag)
        if (scroll) node.performScrollTo()
        node.performClick()
    }
    private fun <T> value(result: DomainResult<T>): T = when (result) {
        is DomainResult.Success -> result.value
        is DomainResult.Error -> throw AssertionError("Backend failure: ${result.failure.kind}/${result.failure.code}")
    }
    @Test fun supervisorManagesSourceAndRecordsRealCountsThroughNativeScreens() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("backend") == "true")
        val supervisor = BackendIntegrationTest.Client("http://10.0.2.2:8080")
        val suffix = UUID.randomUUID().toString().take(8)
        val bus = runBlocking {
            value(supervisor.auth.signIn("sup-001", "Safebus2024!"))
            value(supervisor.fleet.save(FleetChange.Bus("UC-$suffix")))
        }
        val driver = runBlocking { value(supervisor.fleet.save(FleetChange.Driver("UI count driver", "uc-$suffix", "Integration2026!"))) }
        val route = runBlocking { value(supervisor.fleet.save(FleetChange.Route("UI count route", "A", "B"))) }
        val assignment = runBlocking { value(supervisor.fleet.save(FleetChange.Assignment(driver.id, bus.id, route.id, Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600)))) }
        val conductor = BackendIntegrationTest.Client("http://10.0.2.2:8080")
        val shift = runBlocking {
            value(conductor.auth.signIn("uc-$suffix", "Integration2026!"))
            value(conductor.trip.activate(assignment.id, driver.credential!!))
        }
        await("coreSignIn")
        compose.onNodeWithTag("coreLoginId").performTextInput("sup-001")
        compose.onNodeWithTag("corePassword").performTextInput("Safebus2024!")
        click("coreSignIn")
        await("fleetSearch")
        compose.onNodeWithTag("fleetSearch").performTextInput("UC-$suffix")
        click("sources:${bus.id}")
        compose.waitUntil(20000) { compose.onAllNodes(hasTestTag("registerSource") and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("sourceLabel").performTextInput("Native counter")
        click("registerSource"); await("sourceKey")
        val key = compose.onNodeWithTag("sourceKey").fetchSemanticsNode().config[SemanticsProperties.Text].joinToString("") { it.text }
        assertTrue(key.isNotBlank())
        val sources = RemoteCountingRepository(supervisor.api)
        val source = runBlocking { value(sources.sources(bus.id)).single() }
        click("hideSourceKey"); compose.onNodeWithTag("sourceKey").assertDoesNotExist()
        click("openCounter")
        compose.onNodeWithTag("counterKey").performTextInput(key)
        compose.onNodeWithTag("initializeCount").assertIsNotEnabled()
        click("confirmEmptyBus"); click("initializeCount")
        compose.waitUntil(20000) { runBlocking { (sources.occupancy(bus.id) as? DomainResult.Success)?.value?.count == 0 } }
        click("countEntry")
        compose.waitUntil(20000) { runBlocking { (sources.occupancy(bus.id) as? DomainResult.Success)?.value?.count == 1 } }
        click("heartbeat")
        click("counterBack")
        click("rotate:${source.id}"); click("confirmSource", false); await("sourceKey")
        click("hideSourceKey")
        click("revoke:${source.id}"); click("confirmSource", false)
        compose.waitUntil(20000) { compose.onAllNodesWithTag("revoke:${source.id}").fetchSemanticsNodes().isEmpty() }
        runBlocking { assertEquals("REVOKED", value(sources.sources(bus.id)).single().status) }
        click("nav:ACCOUNT", false); click("realSignOut"); click("confirmRealSignOut", false); await("coreSignIn")
        runBlocking { value(conductor.trip.close(shift)); value(conductor.auth.signOut()); value(supervisor.auth.signOut()) }
    }
}
