package com.dreamteam.safebus

import android.os.Bundle
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.dreamteam.safebus.fleet.domain.FleetChange
import com.dreamteam.safebus.fleet.domain.ProvisionReceipt
import com.dreamteam.safebus.iam.domain.Role
import com.dreamteam.safebus.shared.domain.DomainResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import java.util.UUID

class DeployedStaffFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun await(tag: String) = compose.waitUntil(30_000) {
        compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }

    private fun click(tag: String, scroll: Boolean = false) {
        await(tag)
        compose.waitUntil(30_000) {
            compose.onAllNodes(hasTestTag(tag) and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        val node = compose.onNodeWithTag(tag)
        if (scroll) node.performScrollTo()
        node.performClick()
    }

    private fun field(name: String, value: String) {
        compose.onNodeWithTag("provision:$name").performScrollTo().performTextReplacement(value)
    }

    private fun selectForm(title: String) {
        compose.onNode(hasText(title) and hasClickAction()).performScrollTo().performClick()
    }

    private fun save(account: AccountScope): ProvisionReceipt {
        click("saveProvision", true)
        compose.waitUntil(30_000) {
            compose.onAllNodesWithTag("provisionSavedId").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithTag("provisionSaveError").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("provisionSaveError").assertDoesNotExist()
        compose.onNodeWithTag("provisionSavedId").assertIsDisplayed()
        val receipt = requireNotNull(account.fleetModel.state.value.value.receipt)
        click("acknowledgeProvision")
        compose.onNodeWithTag("saveProvision").performScrollTo().assertIsNotEnabled()
        return receipt
    }

    private fun chooseTime(field: String, hour: Int, minute: Int) {
        click("provision:$field", true)
        click("dateConfirm")
        compose.onNodeWithContentDescription("$hour hours").performClick()
        compose.onNodeWithContentDescription("Select minutes").performClick()
        compose.onNodeWithContentDescription("$minute minutes").performClick()
        click("timeConfirm")
    }

    private fun logout() {
        click("nav:ACCOUNT")
        click("realSignOut", true)
        click("confirmRealSignOut")
        await("welcomeLogin")
    }

    private fun <T> value(result: DomainResult<T>): T = when (result) {
        is DomainResult.Success -> result.value
        is DomainResult.Error -> throw AssertionError("Backend failure: ${result.failure.kind}/${result.failure.code}")
    }

    private fun report(message: String) {
        InstrumentationRegistry.getInstrumentation().sendStatus(2, Bundle().apply { putString("stream", "\n$message\n") })
    }

    @Test fun manualSupervisorSessionCreatesCalendarAssignmentThenDriverCompletesShift() {
        val args = InstrumentationRegistry.getArguments()
        val local = args.getString("backend") == "true"
        assumeTrue("Requires local integration or an explicitly enabled deployed run with a manually signed-in supervisor", local || args.getString("deployedStaff") == "true")
        assumeTrue("Run before 23:45 in the emulator time zone", LocalTime.now() < LocalTime.of(23, 45))
        val application = compose.activity.application as SafeBusApplication
        assertEquals(if (local) "http://10.0.2.2:8080" else "https://safebus-api-production-cb4d.up.railway.app", application.endpoint.trimEnd('/'))
        if (local) {
            click("welcomeLogin", true)
            compose.onNodeWithTag("coreLoginId").performTextInput("sup-001")
            compose.onNodeWithTag("corePassword").performTextInput("Safebus2024!")
            click("coreSignIn", true)
        }
        await("nav:FLEET")
        val container = requireNotNull(application.container.value)
        val supervisor = requireNotNull(container.account)
        assertEquals(Role.SUPERVISOR, supervisor.session.role)
        val originalLanguage = application.getSharedPreferences("presentation", 0).getString("language", "en") ?: "en"
        click("nav:ACCOUNT")
        click("language:en", true)
        compose.onNodeWithTag("nav:SHIFT").assertDoesNotExist()
        compose.onNodeWithTag("welcomeRegister").assertDoesNotExist()
        runBlocking {
            value(supervisor.fleet.overview())
            value(supervisor.safety.queue())
            assertEquals(0, value(supervisor.outbox.count()))
        }
        for (tab in listOf("EMERGENCIES", "REVIEWS", "FLEET")) {
            click("nav:$tab")
            compose.waitUntil(30_000) { !supervisor.fleetModel.state.value.busy && !supervisor.safetyModel.state.value.busy }
            compose.onNodeWithTag("coreError").assertDoesNotExist()
        }
        click("nav:ASSIGNMENTS")
        val suffix = UUID.randomUUID().toString().take(8)
        val plate = "QA-${suffix.uppercase(Locale.ROOT)}"
        val login = "qa-$suffix"
        val password = UUID.randomUUID().toString()
        selectForm("Register bus")
        field("plate", plate)
        val bus = save(supervisor)
        report("Staff QA bus=${bus.id}, plate=$plate")
        selectForm("Register driver")
        field("name", "QA calendar $suffix")
        field("login", login)
        field("password", password)
        val driver = save(supervisor)
        report("Staff QA driver=${driver.id}")
        selectForm("Register route")
        field("name", "QA route $suffix")
        field("origin", "QA San Miguel")
        field("destination", "QA Comas")
        val route = save(supervisor)
        report("Staff QA route=${route.id}")
        selectForm("Register capacity")
        field("bus", bus.id.toString())
        field("capacity", "40")
        field("reference", "QA-$suffix")
        save(supervisor)
        runBlocking { assertEquals(40, value(supervisor.fleet.overview()).buses.single { it.id == bus.id }.capacity) }
        selectForm("Assign shift")
        listOf("driver" to driver.id, "bus" to bus.id, "route" to route.id).forEach { (name, id) ->
            field(name, id.toString())
            click("assignmentNext", true)
        }
        click("assignmentNext", true)
        compose.onNodeWithTag("assignmentDateError").assertExists()
        compose.onNodeWithTag("saveProvision").assertDoesNotExist()
        assertNull(supervisor.fleetModel.state.value.value.receipt)
        chooseTime("start", 0, 0)
        chooseTime("end", 0, 0)
        click("assignmentNext", true)
        compose.onNodeWithTag("assignmentDateError").assertExists()
        compose.onNodeWithTag("saveProvision").assertDoesNotExist()
        assertNull(supervisor.fleetModel.state.value.value.receipt)
        runBlocking { assertEquals(0, value(supervisor.outbox.count())) }
        chooseTime("end", 23, 55)
        click("provision:end", true)
        click("dateCancel")
        click("assignmentNext", true)
        val assignment = save(supervisor)
        val expected = supervisor.fleetModel.state.value.value.savedChange as FleetChange.Assignment
        assertEquals(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant(), expected.startsAt)
        assertEquals(LocalDate.now().atTime(23, 55).atZone(ZoneId.systemDefault()).toInstant(), expected.endsAt)
        report("Staff QA assignment=${assignment.id}; calendar start/end saved")
        logout()
        assertNull(container.auth.session.value)
        runBlocking { assertNull(value(container.auth.restore())) }
        compose.onNodeWithTag("welcomeRegister").assertDoesNotExist()
        click("welcomeLogin", true)
        compose.onNodeWithTag("coreLoginId").performTextInput(login)
        compose.onNodeWithTag("corePassword").performTextInput("invalid-password")
        click("coreSignIn", true)
        await("coreError")
        compose.onNodeWithTag("nav:SHIFT").assertDoesNotExist()
        assertNull(container.auth.session.value)
        runBlocking { assertNull(value(container.auth.restore())) }
        compose.onNodeWithTag("corePassword").performTextInput(password)
        click("coreSignIn", true)
        await("nav:SHIFT")
        val conductor = requireNotNull(container.account)
        assertEquals(Role.DRIVER, conductor.session.role)
        try {
            compose.onNodeWithTag("nav:FLEET").assertDoesNotExist()
            compose.onNodeWithTag("nav:ASSIGNMENTS").assertDoesNotExist()
            click("openShiftScan", true)
            val persisted = runBlocking { value(conductor.trip.assignment()) }
            assertEquals(assignment.id, persisted.id)
            assertEquals(expected.startsAt, persisted.startsAt)
            assertEquals(expected.endsAt, persisted.endsAt)
            assertEquals(plate, persisted.plate)
            compose.onNodeWithTag("qrContent").performTextInput("invalid-qr")
            click("activateShift", true)
            await("coreError")
            compose.onNodeWithTag("realPanic").assertDoesNotExist()
            runBlocking { assertNull(value(conductor.trip.restore())); assertEquals(0, value(conductor.outbox.count())) }
            compose.onNodeWithTag("qrContent").performTextInput(requireNotNull(driver.credential))
            click("activateShift", true)
            await("realPanic")
            val shift = runBlocking { requireNotNull(value(conductor.trip.restore())) }
            assertEquals(assignment.id, shift.assignmentId)
            assertEquals(bus.id, shift.busId)
            report("Staff QA shift=${shift.id} activated")
            click("nav:DRIVER_ALERTS")
            compose.waitUntil(30_000) { !conductor.safetyModel.state.value.busy }
            compose.onNodeWithTag("coreError").assertDoesNotExist()
            runBlocking { assertTrue(value(conductor.safety.driverAlerts()).isEmpty()) }
            click("nav:SHIFT")
            click("closeShift", true)
            click("confirmShiftClose")
            compose.waitUntil(30_000) { compose.onAllNodesWithTag("realPanic").fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithTag("coreError").assertDoesNotExist()
            runBlocking {
                assertNull(value(conductor.trip.restore()))
                assertEquals(0, value(conductor.outbox.count()))
            }
            report("Staff QA shift=${shift.id} closed; no local active shift")
            click("nav:ACCOUNT")
            click("language:$originalLanguage", true)
            logout()
            runBlocking { assertNull(value(container.auth.restore())) }
        } finally {
            if (container.auth.session.value?.role == Role.DRIVER) runBlocking {
                value(conductor.trip.restore())?.let { value(conductor.trip.close(it)) }
            }
        }
    }
}
