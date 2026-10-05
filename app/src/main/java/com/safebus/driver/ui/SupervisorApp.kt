package com.safebus.driver.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.safebus.driver.R
import com.safebus.driver.ui.theme.SafeBusColors

private enum class SupervisorPage { Fleet, BusDetails, Emergencies, CaseDetails, Reviews, ReviewGroup, Assignments, NewAssignment, Account }
enum class GroupDecision { Pending, Approved, Rejected }

@Composable
fun SupervisorApp(language: String, onLanguage: (String) -> Unit, onSignOut: () -> Unit) {
    var page by rememberSaveable { mutableStateOf(SupervisorPage.Fleet) }
    var selectedBus by rememberSaveable { mutableStateOf("A1B-702") }
    var selectedCase by rememberSaveable { mutableStateOf("driver") }
    var driverState by rememberSaveable { mutableStateOf(EmergencyState.Active) }
    var passengerState by rememberSaveable { mutableStateOf(EmergencyState.InProgress) }
    var groupState by rememberSaveable { mutableStateOf<EmergencyState?>(null) }
    var decision by rememberSaveable { mutableStateOf(GroupDecision.Pending) }
    var rejection by rememberSaveable { mutableStateOf("") }
    var outcomes by rememberSaveable { mutableStateOf(mapOf<String, String>()) }
    var capacities by rememberSaveable { mutableStateOf(mapOf<String, Int>()) }
    var assignment by rememberSaveable { mutableStateOf(listOf<String>()) }
    var mapAvailable by rememberSaveable { mutableStateOf(true) }
    val cases = listOfNotNull(
        CompanyCase("driver", "AL-302", "201", "14:22", true, driverState),
        CompanyCase("passenger", "EX-108", "107", "14:25", false, passengerState),
        groupState?.let { CompanyCase("group", "SM-420", "204", "14:35", false, it) },
    )
    fun setCaseState(id: String, state: EmergencyState) {
        when (id) { "driver" -> driverState = state; "passenger" -> passengerState = state; else -> groupState = state }
    }
    val section = when (page) {
        SupervisorPage.Fleet, SupervisorPage.BusDetails -> "Fleet"
        SupervisorPage.Emergencies, SupervisorPage.CaseDetails -> "Emergencies"
        SupervisorPage.Reviews, SupervisorPage.ReviewGroup -> "Reviews"
        SupervisorPage.Assignments, SupervisorPage.NewAssignment -> "Assignments"
        SupervisorPage.Account -> "Account"
    }
    val root = page.name == section
    val back = { page = if (root) SupervisorPage.Fleet else SupervisorPage.valueOf(section) }
    BackHandler(page != SupervisorPage.Fleet) { back() }
    val destinations = listOf(RoleDestination("Fleet", R.string.fleet, R.drawable.ic_map),
        RoleDestination("Emergencies", R.string.emergencies, R.drawable.ic_warning, badge = cases.count { it.state != EmergencyState.Closed }),
        RoleDestination("Reviews", R.string.reviews, R.drawable.ic_fact_check, badge = if (decision == GroupDecision.Pending) 1 else 0),
        RoleDestination("Assignments", R.string.assignments, R.drawable.ic_badge),
        RoleDestination("Account", R.string.account, R.drawable.ic_account_circle))
    val title = when (page) {
        SupervisorPage.Fleet -> R.string.fleet
        SupervisorPage.BusDetails -> R.string.bus_details
        SupervisorPage.Emergencies -> R.string.emergencies
        SupervisorPage.CaseDetails -> R.string.emergency_details
        SupervisorPage.Reviews -> R.string.reviews
        SupervisorPage.ReviewGroup -> R.string.review_group
        SupervisorPage.Assignments -> R.string.assignments
        SupervisorPage.NewAssignment -> R.string.new_assignment
        SupervisorPage.Account -> R.string.account
    }
    RoleScaffold(title, section, destinations, onNavigate = { page = SupervisorPage.valueOf(it) }, onBack = if (root) null else back,
        onAccount = if (page != SupervisorPage.Account) ({ page = SupervisorPage.Account }) else null) {
        when (page) {
            SupervisorPage.Fleet -> FleetScreen(mapAvailable, capacities, onBus = { selectedBus = it; page = SupervisorPage.BusDetails })
            SupervisorPage.BusDetails -> {
                val bus = MockFleet.buses.first { it.plate == selectedBus }
                BusDetailsScreen(bus, capacities[bus.plate] ?: bus.capacity, onCapacity = { capacities = capacities + (bus.plate to it) })
            }
            SupervisorPage.Emergencies -> CompanyEmergenciesScreen(cases, decision,
                onCase = { selectedCase = it; page = SupervisorPage.CaseDetails },
                onStart = { setCaseState(it, EmergencyState.InProgress); selectedCase = it; page = SupervisorPage.CaseDetails },
                onReview = { page = SupervisorPage.ReviewGroup })
            SupervisorPage.CaseDetails -> {
                val case = cases.first { it.id == selectedCase }
                CompanyCaseScreen(case, outcomes[case.id].orEmpty(), onStart = { setCaseState(case.id, EmergencyState.InProgress) },
                    onClose = { outcomes = outcomes + (case.id to it); setCaseState(case.id, EmergencyState.Closed) })
            }
            SupervisorPage.Reviews -> ReviewsScreen(decision, rejection, onReview = { page = SupervisorPage.ReviewGroup })
            SupervisorPage.ReviewGroup -> ReviewGroupScreen(decision, rejection, onApprove = {
                decision = GroupDecision.Approved; groupState = EmergencyState.Active; page = SupervisorPage.Emergencies
            }, onReject = { rejection = it; decision = GroupDecision.Rejected; page = SupervisorPage.Reviews })
            SupervisorPage.Assignments -> AssignmentsScreen(assignment, onNew = { page = SupervisorPage.NewAssignment })
            SupervisorPage.NewAssignment -> AssignmentForm(onCancel = { page = SupervisorPage.Assignments }, onCreate = { driver, bus, route, period ->
                assignment = listOf(driver, bus, route, period); page = SupervisorPage.Assignments
            })
            SupervisorPage.Account -> SegmentAccountScreen(UserRole.Supervisor, language, onLanguage, onSignOut) {
                InfoCard { ScenarioSwitch(stringResource(R.string.map_available), "", mapAvailable, { mapAvailable = it }, "mapAvailability") }
                SecondaryAction(stringResource(R.string.reset_cases), {
                    driverState = EmergencyState.Active; passengerState = EmergencyState.InProgress; groupState = null
                    decision = GroupDecision.Pending; rejection = ""; outcomes = emptyMap()
                }, Modifier.testTag("resetCases"))
            }
        }
    }
}

data class CompanyCase(val id: String, val plate: String, val route: String, val time: String, val driver: Boolean, val state: EmergencyState)

@Composable
private fun CaseStatus(case: CompanyCase) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusChip(stringResource(if (case.driver) R.string.critical else R.string.high), if (case.driver) SafeBusColors.Critical else SafeBusColors.High, R.drawable.ic_warning)
        StatusChip(stringResource(case.state.label), if (case.state == EmergencyState.Closed) SafeBusColors.Closed else if (case.driver) SafeBusColors.Critical else SafeBusColors.High,
            case.state.icon, Modifier.testTag("companyStatus${case.state.name}"))
    }
}

@Composable
private fun CompanyEmergenciesScreen(cases: List<CompanyCase>, decision: GroupDecision, onCase: (String) -> Unit, onStart: (String) -> Unit, onReview: () -> Unit) {
    var filter by rememberSaveable { mutableStateOf("All") }
    ScreenContent {
        ChoiceChips(listOf("All" to stringResource(R.string.all), "Driver" to stringResource(R.string.driver_tab),
            "Passenger" to stringResource(R.string.passenger_tab), "History" to stringResource(R.string.history)), filter, { filter = it }, "casesFilter")
        if (decision == GroupDecision.Approved && filter != "History") Notice(stringResource(R.string.group_approved), Modifier.testTag("groupApproved"), R.drawable.ic_check_circle)
        val visible = cases.filter { case -> if (filter == "History") case.state == EmergencyState.Closed else case.state != EmergencyState.Closed &&
            (filter == "All" || (filter == "Driver" && case.driver) || (filter == "Passenger" && !case.driver)) }
        visible.forEach { case ->
            Text(stringResource(if (case.driver) R.string.driver_emergencies else R.string.passenger_emergencies), style = MaterialTheme.typography.labelLarge)
            InfoCard(Modifier.testTag("case${case.id}")) {
                Text(case.plate + " · " + stringResource(R.string.route_number, case.route), style = MaterialTheme.typography.titleLarge)
                CaseStatus(case)
                Text(case.time, style = MaterialTheme.typography.bodyMedium)
                if (case.state == EmergencyState.Active) PrimaryAction(stringResource(R.string.start_attention), { onStart(case.id) }, Modifier.testTag("start${case.id}"), R.drawable.ic_schedule)
                else SecondaryAction(stringResource(if (case.state == EmergencyState.InProgress) R.string.close_emergency else R.string.view_details), { onCase(case.id) }, Modifier.testTag("open${case.id}"))
                if (case.state == EmergencyState.Active) SecondaryAction(stringResource(R.string.view_details), { onCase(case.id) }, Modifier.testTag("open${case.id}"))
            }
        }
        if (decision == GroupDecision.Pending && filter == "All") {
            Text(stringResource(R.string.passenger_reviews), style = MaterialTheme.typography.labelLarge)
            ReviewSummary(onReview)
        } else if (visible.isEmpty()) Notice(stringResource(R.string.no_cases))
    }
}

@Composable
private fun CompanyCaseScreen(case: CompanyCase, outcome: String, onStart: () -> Unit, onClose: (String) -> Unit) {
    var draft by rememberSaveable(case.id) { mutableStateOf("") }
    ScreenContent {
        CaseStatus(case)
        InfoCard {
            Text(case.plate + " · " + stringResource(R.string.route_number, case.route), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(if (case.driver) R.string.driver_emergencies else R.string.passenger_emergencies))
            Text(case.time + " · " + stringResource(R.string.service_company), style = MaterialTheme.typography.bodyMedium)
        }
        when (case.state) {
            EmergencyState.Active -> PrimaryAction(stringResource(R.string.start_attention), onStart, Modifier.testTag("startAttention"), R.drawable.ic_schedule)
            EmergencyState.InProgress -> {
                Text(stringResource(R.string.close_case_help), style = MaterialTheme.typography.bodyLarge)
                SafeTextField(draft, { draft = it.take(500) }, label = { Text(stringResource(R.string.outcome_label)) }, minLines = 3,
                    supportingText = { Text("${draft.length}/500") }, modifier = Modifier.fillMaxWidth().testTag("caseOutcome"))
                PrimaryAction(stringResource(R.string.close_emergency), { onClose(draft.trim()) }, Modifier.testTag("closeCase"), enabled = draft.isNotBlank())
            }
            EmergencyState.Closed -> {
                Text(stringResource(R.string.case_closed), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("caseClosed"))
                InfoCard { Text(stringResource(R.string.result), style = MaterialTheme.typography.titleMedium); Text(outcome) }
            }
            EmergencyState.Pending -> Unit
        }
        Notice(stringResource(R.string.prototype_notice))
    }
}

@Composable
private fun ReviewSummary(onReview: () -> Unit) {
    InfoCard {
        Text("SM-420 · " + stringResource(R.string.route_number, "204"), style = MaterialTheme.typography.titleLarge)
        StatusChip(stringResource(R.string.awaiting_approval), SafeBusColors.Pending, R.drawable.ic_schedule)
        Text(stringResource(R.string.threshold_three))
        Text("14:30", style = MaterialTheme.typography.bodySmall)
        PrimaryAction(stringResource(R.string.review_incident), onReview, Modifier.testTag("reviewGroup"), R.drawable.ic_fact_check)
    }
}

@Composable
private fun ReviewsScreen(decision: GroupDecision, reason: String, onReview: () -> Unit) {
    var reviewed by rememberSaveable(decision) { mutableStateOf(decision != GroupDecision.Pending) }
    ScreenContent {
        ChoiceChips(listOf("Pending" to stringResource(R.string.pending_reviews), "Reviewed" to stringResource(R.string.reviewed)),
            if (reviewed) "Reviewed" else "Pending", { reviewed = it == "Reviewed" }, "reviewsFilter")
        if (!reviewed && decision == GroupDecision.Pending) ReviewSummary(onReview)
        else if (reviewed && decision != GroupDecision.Pending) InfoCard {
            Text("SM-420 · " + stringResource(R.string.route_number, "204"), style = MaterialTheme.typography.titleLarge)
            StatusChip(stringResource(if (decision == GroupDecision.Approved) R.string.group_approved else R.string.group_rejected),
                if (decision == GroupDecision.Approved) SafeBusColors.High else SafeBusColors.Inactive, R.drawable.ic_fact_check,
                Modifier.testTag("reviewDecision"))
            Text(stringResource(R.string.review_decision))
            if (reason.isNotBlank()) Text(reason)
            SecondaryAction(stringResource(R.string.view_details), onReview, Modifier.testTag("reviewedGroup"))
        } else Notice(stringResource(R.string.no_cases))
    }
}

@Composable
private fun ReviewGroupScreen(decision: GroupDecision, reason: String, onApprove: () -> Unit, onReject: (String) -> Unit) {
    var rejecting by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf("") }
    var evidence by rememberSaveable { mutableIntStateOf(-1) }
    val messages = listOf(R.string.evidence_one, R.string.evidence_two, R.string.evidence_three)
    ScreenContent {
        Text("SM-420 · " + stringResource(R.string.route_number, "204"), style = MaterialTheme.typography.titleLarge)
        StatusChip(stringResource(when (decision) { GroupDecision.Pending -> R.string.awaiting_approval; GroupDecision.Approved -> R.string.group_approved; GroupDecision.Rejected -> R.string.group_rejected }),
            when (decision) { GroupDecision.Pending -> SafeBusColors.Pending; GroupDecision.Approved -> SafeBusColors.High; GroupDecision.Rejected -> SafeBusColors.Inactive }, R.drawable.ic_groups)
        Text(stringResource(R.string.threshold_three), style = MaterialTheme.typography.titleMedium)
        messages.forEachIndexed { index, message -> InfoCard {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Symbol(R.drawable.ic_photo_camera, Modifier.size(48.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.passenger_n, index + 1), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(message), style = MaterialTheme.typography.bodyMedium)
                }
            }
            SecondaryAction(stringResource(R.string.incident_photo), { evidence = index }, Modifier.testTag("groupEvidence$index"))
        } }
        Text(stringResource(R.string.authorized_evidence), style = MaterialTheme.typography.bodyMedium)
        if (decision == GroupDecision.Pending) {
            PrimaryAction(stringResource(R.string.approve), onApprove, Modifier.testTag("approveGroup"), R.drawable.ic_check_circle)
            SecondaryAction(stringResource(R.string.reject), { rejecting = true }, Modifier.testTag("rejectGroup"))
        } else if (reason.isNotBlank()) Notice(reason)
    }
    if (evidence >= 0) AlertDialog(onDismissRequest = { evidence = -1 }, title = { Text(stringResource(R.string.incident_photo)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { EvidencePreview(); Text(stringResource(messages[evidence])) } },
        confirmButton = { DialogAction(onClick = { evidence = -1 }, modifier = Modifier.testTag("closeEvidence")) { Text(stringResource(R.string.ok)) } })
    if (rejecting) AlertDialog(onDismissRequest = { rejecting = false }, title = { Text(stringResource(R.string.reject_reason)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.rejection_required))
            SafeTextField(draft, { draft = it.take(500) }, minLines = 2, label = { Text(stringResource(R.string.reject_reason)) }, modifier = Modifier.testTag("rejectionReason"))
        } }, confirmButton = { DialogAction(onClick = { rejecting = false; onReject(draft.trim()) }, enabled = draft.isNotBlank(), modifier = Modifier.testTag("confirmReject")) { Text(stringResource(R.string.reject)) } },
        dismissButton = { DialogAction(onClick = { rejecting = false }) { Text(stringResource(R.string.cancel)) } })
}
