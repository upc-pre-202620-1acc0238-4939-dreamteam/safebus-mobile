package com.safebus.driver.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.safebus.driver.R

@Composable
fun AssignmentsScreen(assignment: List<String>, onNew: () -> Unit) {
    ScreenContent {
        PrimaryAction(stringResource(R.string.new_assignment), onNew, Modifier.testTag("newAssignment"), R.drawable.ic_assignment)
        if (assignment.isNotEmpty()) InfoCard(Modifier.testTag("createdAssignment")) {
            Text(stringResource(R.string.assignment_created), style = MaterialTheme.typography.titleLarge)
            Text(assignment[0]); Text(assignment[1] + " · " + stringResource(R.string.route_number, assignment[2])); Text(assignment[3])
        }
        InfoCard {
            Text(stringResource(R.string.morning_assigned), style = MaterialTheme.typography.titleMedium)
            Text("Carlos Mamani R.", style = MaterialTheme.typography.titleLarge)
            Text("A1B-702 · " + stringResource(R.string.route_number, "23"))
            Text(stringResource(R.string.service_company), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun AssignmentForm(onCancel: () -> Unit, onCreate: (String, String, String, String) -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var driver by rememberSaveable { mutableStateOf("Carlos Mamani R.") }
    var bus by rememberSaveable { mutableStateOf("A1B-702") }
    var route by rememberSaveable { mutableStateOf("23") }
    var period by rememberSaveable { mutableStateOf("06:00 - 14:00") }
    val conflict = period == "06:00 - 14:00" && (driver == "Carlos Mamani R." || bus == "A1B-702")
    BackHandler { if (step > 0) step-- else onCancel() }
    ScreenContent {
        Text(stringResource(R.string.step_of, step + 1, 5), style = MaterialTheme.typography.bodyMedium)
        LinearProgressIndicator(progress = { (step + 1) / 5f }, modifier = Modifier.fillMaxWidth())
        Text(stringResource(listOf(R.string.select_driver, R.string.select_bus, R.string.select_route, R.string.select_period, R.string.confirm_assignment)[step]), style = MaterialTheme.typography.headlineSmall)
        when (step) {
            0 -> ChoiceChips(MockFleet.buses.map { it.driver to it.driver }, driver, { driver = it }, "assignmentDriver")
            1 -> ChoiceChips(MockFleet.buses.map { it.plate to it.plate }, bus, { bus = it }, "assignmentBus")
            2 -> ChoiceChips(listOf("23", "107", "201", "204").map { it to stringResource(R.string.route_number, it) }, route, { route = it }, "assignmentRoute")
            3 -> {
                ChoiceChips(listOf("06:00 - 14:00" to stringResource(R.string.morning_shift), "14:00 - 22:00" to stringResource(R.string.afternoon_shift)), period, { period = it }, "assignmentPeriod")
                if (conflict) Notice(stringResource(R.string.assignment_conflict), Modifier.testTag("assignmentConflict"))
            }
            4 -> InfoCard {
                Text(stringResource(R.string.assignment_summary), style = MaterialTheme.typography.titleMedium)
                Text(driver); Text(bus); Text(stringResource(R.string.route_number, route)); Text(period)
            }
        }
        if (step < 4) PrimaryAction(stringResource(R.string.continue_action), { step++ }, Modifier.testTag("assignmentNext"), enabled = step != 3 || !conflict)
        else PrimaryAction(stringResource(R.string.confirm_assignment), { onCreate(driver, bus, route, period) }, Modifier.testTag("confirmAssignment"))
        SecondaryAction(stringResource(if (step == 0) R.string.cancel else R.string.back), { if (step > 0) step-- else onCancel() }, Modifier.testTag("assignmentBack"))
    }
}
