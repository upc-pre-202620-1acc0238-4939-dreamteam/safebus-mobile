package com.safebus.driver.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import com.safebus.driver.R
import com.safebus.driver.ui.theme.SafeBusColors

private enum class PassengerPage { Journey, Scanner, Verified, BusAlerts, RequestForm, RequestReview, Requests, RequestDetails, Account, Ended }
enum class RequestState(val label: Int) {
    Pending(R.string.status_pending), Collecting(R.string.collecting), AwaitingApproval(R.string.awaiting_approval),
    Expired(R.string.expired), NotApproved(R.string.not_approved), Late(R.string.late),
    Active(R.string.status_active), InProgress(R.string.status_progress), Closed(R.string.status_closed);
    val color get() = when (this) {
        Pending, Collecting -> SafeBusColors.Information
        AwaitingApproval -> SafeBusColors.Pending
        Active, InProgress -> SafeBusColors.High
        Closed -> SafeBusColors.Closed
        else -> SafeBusColors.Inactive
    }
    val finished get() = this in listOf(Expired, NotApproved, Late, Closed)
}

@Composable
fun PassengerApp(language: String, onLanguage: (String) -> Unit, onSignOut: () -> Unit) {
    var page by rememberSaveable { mutableStateOf(PassengerPage.Journey) }
    var active by rememberSaveable { mutableStateOf(false) }
    var location by rememberSaveable { mutableStateOf(false) }
    var connected by rememberSaveable { mutableStateOf(true) }
    var emptyReports by rememberSaveable { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf("") }
    var photo by rememberSaveable { mutableStateOf(false) }
    var submittedMessage by rememberSaveable { mutableStateOf("") }
    var request by rememberSaveable { mutableStateOf<RequestState?>(null) }
    var ending by rememberSaveable { mutableStateOf(false) }
    var signingOut by rememberSaveable { mutableStateOf(false) }
    var endedAutomatically by rememberSaveable { mutableStateOf(false) }
    val section = when (page) {
        PassengerPage.BusAlerts -> "BusAlerts"
        PassengerPage.Requests, PassengerPage.RequestDetails -> "Requests"
        PassengerPage.Account -> "Account"
        else -> "Journey"
    }
    val isRoot = page in listOf(PassengerPage.Journey, PassengerPage.BusAlerts, PassengerPage.Requests, PassengerPage.Account)
    val back = { page = if (page == PassengerPage.RequestReview) PassengerPage.RequestForm else if (page == PassengerPage.RequestDetails) PassengerPage.Requests else PassengerPage.Journey }
    BackHandler(page != PassengerPage.Journey) { back() }
    val title = when (page) {
        PassengerPage.Scanner -> R.string.scan_bus_qr
        PassengerPage.Verified -> R.string.bus_verified
        PassengerPage.BusAlerts -> R.string.bus_alerts
        PassengerPage.RequestForm -> R.string.request_title
        PassengerPage.RequestReview -> R.string.review_request
        PassengerPage.Requests -> R.string.my_requests
        PassengerPage.RequestDetails -> R.string.request_details
        PassengerPage.Account -> R.string.account
        PassengerPage.Ended -> R.string.journey_ended
        else -> R.string.my_journey
    }
    val destinations = listOf(RoleDestination("Journey", R.string.journey, R.drawable.ic_route),
        RoleDestination("BusAlerts", R.string.bus_alerts, R.drawable.ic_warning, enabled = active),
        RoleDestination("Requests", R.string.my_requests, R.drawable.ic_assignment),
        RoleDestination("Account", R.string.account, R.drawable.ic_account_circle))
    RoleScaffold(title, section, if (page == PassengerPage.Scanner) emptyList() else destinations,
        onNavigate = { page = PassengerPage.valueOf(it) }, onBack = if (isRoot) null else back,
        onAccount = if (page != PassengerPage.Account && page != PassengerPage.Scanner) ({ page = PassengerPage.Account }) else null) {
        when (page) {
            PassengerPage.Journey -> ScreenContent {
                if (!active) {
                    Spacer(Modifier.weight(1f).heightIn(min = 32.dp))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Symbol(R.drawable.ic_qr_code_scanner, Modifier.size(96.dp)) }
                    Text(stringResource(R.string.scan_bus_help), style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.weight(1f).heightIn(min = 32.dp))
                    PrimaryAction(stringResource(R.string.scan_bus_qr), { page = PassengerPage.Scanner }, Modifier.testTag("scanBusQr"), R.drawable.ic_qr_code_scanner)
                } else {
                    StatusChip(stringResource(R.string.journey_active), SafeBusColors.Secondary, R.drawable.ic_route, Modifier.testTag("activeJourney"))
                    PassengerBusCard()
                    if (!connected) Notice(stringResource(R.string.offline), icon = R.drawable.ic_cloud_off)
                    Notice(stringResource(if (location) R.string.automatic_enabled else R.string.automatic_unavailable), icon = R.drawable.ic_location_on)
                    if (!location) SecondaryAction(stringResource(R.string.enable_location), { location = true }, Modifier.testTag("enableLocation"))
                    Spacer(Modifier.weight(1f).heightIn(min = 24.dp))
                    PrimaryAction(stringResource(R.string.send_request), { message = ""; photo = false; page = PassengerPage.RequestForm }, Modifier.testTag("newRequest"), R.drawable.ic_shield)
                    SecondaryAction(stringResource(R.string.end_journey), { ending = true }, Modifier.testTag("endJourney"))
                }
            }
            PassengerPage.Scanner -> ScannerScreen(onCancel = { page = PassengerPage.Journey }, onValid = { page = PassengerPage.Verified },
                instructions = R.string.scan_bus_help, invalidText = R.string.invalid_bus_qr)
            PassengerPage.Verified -> ScreenContent {
                PassengerBusCard()
                InfoCard {
                    Text(stringResource(if (location) R.string.location_enabled else R.string.location_required), style = MaterialTheme.typography.titleMedium)
                    if (!location) {
                        SecondaryAction(stringResource(R.string.enable_location), { location = true }, Modifier.testTag("enableLocation"), R.drawable.ic_location_on)
                        Text(stringResource(R.string.automatic_unavailable), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.weight(1f).heightIn(min = 32.dp))
                PrimaryAction(stringResource(R.string.start_journey), { active = true; page = PassengerPage.Journey }, Modifier.testTag("startJourney"), R.drawable.ic_route)
            }
            PassengerPage.BusAlerts -> BusAlertsScreen(emptyReports)
            PassengerPage.RequestForm -> RequestFormScreen(message, { message = it }, photo, { photo = it }, onReview = { page = PassengerPage.RequestReview })
            PassengerPage.RequestReview -> ScreenContent {
                Notice(stringResource(R.string.request_notice))
                PassengerBusCard()
                InfoCard { Text(stringResource(R.string.incident_message), style = MaterialTheme.typography.titleMedium); Text(message) }
                EvidencePreview()
                Text(stringResource(R.string.evidence_privacy), style = MaterialTheme.typography.bodyMedium)
                PrimaryAction(stringResource(R.string.send_confirm), {
                    submittedMessage = message.trim()
                    request = if (connected) RequestState.Collecting else RequestState.Pending
                    page = PassengerPage.Requests
                }, Modifier.testTag("sendRequest"), enabled = active && message.isNotBlank() && photo)
                SecondaryAction(stringResource(R.string.edit_request), { page = PassengerPage.RequestForm }, Modifier.testTag("editRequest"))
            }
            PassengerPage.Requests -> RequestsScreen(request, onDetails = { page = PassengerPage.RequestDetails })
            PassengerPage.RequestDetails -> ScreenContent {
                val state = request ?: RequestState.Collecting
                RequestStatus(state)
                Text(stringResource(R.string.unit_route), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.request_received), style = MaterialTheme.typography.bodyMedium)
                RequestProgress(state)
                InfoCard { Text(stringResource(R.string.incident_message), style = MaterialTheme.typography.titleMedium); Text(submittedMessage) }
                EvidencePreview()
                Text(stringResource(R.string.evidence_privacy), style = MaterialTheme.typography.bodyMedium)
            }
            PassengerPage.Ended -> ScreenContent {
                Symbol(R.drawable.ic_check_circle, Modifier.size(64.dp), SafeBusColors.Closed)
                Text(stringResource(R.string.journey_ended), style = MaterialTheme.typography.displaySmall, modifier = Modifier.testTag("journeyEnded"))
                Text(stringResource(if (endedAutomatically) R.string.away_help else R.string.journey_ended_help), style = MaterialTheme.typography.bodyLarge)
                PrimaryAction(stringResource(R.string.my_requests), { page = PassengerPage.Requests }, Modifier.testTag("endedRequests"))
                SecondaryAction(stringResource(R.string.my_journey), { page = PassengerPage.Journey })
            }
            PassengerPage.Account -> SegmentAccountScreen(UserRole.Passenger, language, onLanguage,
                onSignOut = { if (active) signingOut = true else onSignOut() }) {
                InfoCard {
                    ScenarioSwitch(stringResource(R.string.connection), stringResource(if (connected) R.string.online else R.string.offline), connected, {
                        connected = it
                        if (it && request == RequestState.Pending) request = RequestState.Collecting
                    }, "passengerConnection")
                    ScenarioSwitch(stringResource(R.string.location_simulation), "", location, { location = it }, "passengerLocation")
                    ScenarioSwitch(stringResource(R.string.empty_bus_reports), "", emptyReports, { emptyReports = it }, "emptyBusReports")
                }
                if (request != null) {
                    Text(stringResource(R.string.simulate_request_state), style = MaterialTheme.typography.titleMedium)
                    ChoiceChips(RequestState.entries.map { it.name to stringResource(it.label) }, request!!.name,
                        { request = RequestState.valueOf(it) }, "requestState")
                }
                if (active && location) SecondaryAction(stringResource(R.string.simulate_away), {
                    active = false; endedAutomatically = true; page = PassengerPage.Ended
                }, Modifier.testTag("simulateAway"))
            }
        }
    }
    if (ending || signingOut) AlertDialog(onDismissRequest = { ending = false; signingOut = false },
        title = { Text(stringResource(if (signingOut) R.string.sign_out else R.string.end_journey)) },
        text = { Text(stringResource(if (signingOut) R.string.sign_out_journey else R.string.end_journey_confirm)) },
        confirmButton = { DialogAction(onClick = {
            active = false
            if (signingOut) onSignOut() else { endedAutomatically = false; page = PassengerPage.Ended }
            ending = false; signingOut = false
        }, Modifier.testTag("confirmEndJourney")) { Text(stringResource(if (signingOut) R.string.sign_out else R.string.end_journey)) } },
        dismissButton = { DialogAction(onClick = { ending = false; signingOut = false }, Modifier.testTag("cancelEndJourney")) { Text(stringResource(R.string.cancel)) } })
}

@Composable
fun PassengerBusCard() {
    InfoCard {
        Text(stringResource(R.string.in_service), style = MaterialTheme.typography.labelLarge)
        Text(stringResource(R.string.unit_route), style = MaterialTheme.typography.titleLarge)
        HorizontalDivider()
        Text(stringResource(R.string.service_company), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.route_name), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.registered_driver), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.bus_driver), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun RequestFormScreen(message: String, onMessage: (String) -> Unit, photo: Boolean, onPhoto: (Boolean) -> Unit, onReview: () -> Unit) {
    var picker by rememberSaveable { mutableStateOf(false) }
    var photoError by rememberSaveable { mutableStateOf(false) }
    ScreenContent {
        Notice(stringResource(R.string.request_notice))
        SafeTextField(message, { onMessage(it.take(500)) }, label = { Text(stringResource(R.string.incident_message)) },
            placeholder = { Text(stringResource(R.string.incident_hint)) }, supportingText = { Text("${message.length}/500") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp).testTag("incidentMessage"), minLines = 4)
        Text(stringResource(R.string.incident_photo), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.photo_requirements), style = MaterialTheme.typography.bodyMedium)
        if (photo) { EvidencePreview(); SecondaryAction(stringResource(R.string.remove_photo), { onPhoto(false) }, Modifier.testTag("removeEvidence")) }
        else SecondaryAction(stringResource(R.string.choose_photo), { picker = true }, Modifier.testTag("chooseEvidence"), R.drawable.ic_photo_camera)
        if (photoError) Notice(stringResource(R.string.invalid_photo), Modifier.testTag("invalidEvidence"))
        PrimaryAction(stringResource(R.string.review_request), onReview, Modifier.testTag("reviewRequest"), R.drawable.ic_shield, enabled = message.isNotBlank() && photo)
    }
    if (picker) AlertDialog(onDismissRequest = { picker = false }, title = { Text(stringResource(R.string.incident_photo)) },
        text = { Text(stringResource(R.string.photo_requirements)) },
        confirmButton = { DialogAction(onClick = { onPhoto(true); photoError = false; picker = false }, Modifier.testTag("useSampleEvidence")) { Text(stringResource(R.string.take_sample_photo)) } },
        dismissButton = { DialogAction(onClick = { onPhoto(false); photoError = true; picker = false }, Modifier.testTag("useInvalidEvidence")) { Text(stringResource(R.string.invalid_photo_action)) } })
}

@Composable
fun RequestStatus(state: RequestState) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state in listOf(RequestState.Active, RequestState.InProgress, RequestState.Closed)) StatusChip(stringResource(R.string.high), SafeBusColors.High, R.drawable.ic_warning)
        StatusChip(stringResource(state.label), state.color, if (state == RequestState.Pending) R.drawable.ic_cloud_off else R.drawable.ic_info, Modifier.testTag("passengerStatus${state.name}"))
    }
}

@Composable
private fun RequestProgress(state: RequestState) {
    when (state) {
        RequestState.Collecting, RequestState.AwaitingApproval -> {
            LinearProgressIndicator(progress = { if (state == RequestState.Collecting) 2f / 3 else 1f }, modifier = Modifier.fillMaxWidth(), color = SafeBusColors.Secondary)
            Text(stringResource(if (state == RequestState.Collecting) R.string.threshold_two else R.string.threshold_three))
            Text(stringResource(R.string.threshold_help), style = MaterialTheme.typography.bodyMedium)
        }
        else -> Notice(stringResource(when (state) {
            RequestState.Pending -> R.string.pending_request_help
            RequestState.NotApproved -> R.string.request_rejected_reason
            RequestState.Expired -> R.string.expired_help
            RequestState.Late -> R.string.late_help
            RequestState.Active -> R.string.approved_help
            RequestState.InProgress -> R.string.passenger_progress
            else -> R.string.outcome
        }))
    }
}

@Composable
private fun RequestsScreen(state: RequestState?, onDetails: () -> Unit) {
    var filter by rememberSaveable { mutableStateOf("Ongoing") }
    ScreenContent {
        ChoiceChips(listOf("Ongoing" to stringResource(R.string.ongoing), "Finished" to stringResource(R.string.finished)), filter, { filter = it }, "requestsFilter")
        if (state == null || state.finished != (filter == "Finished")) InfoCard {
            Symbol(R.drawable.ic_assignment, Modifier.size(32.dp))
            Text(stringResource(R.string.no_requests), style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("emptyRequests"))
            Text(stringResource(R.string.no_requests_help))
        } else InfoCard {
            Text(stringResource(R.string.unit_route), style = MaterialTheme.typography.titleLarge)
            RequestStatus(state)
            Text(stringResource(R.string.request_received), style = MaterialTheme.typography.bodyMedium)
            RequestProgress(state)
            EvidencePreview(compact = true)
            Text(stringResource(R.string.evidence_privacy), style = MaterialTheme.typography.bodyMedium)
            SecondaryAction(stringResource(R.string.view_details), onDetails, Modifier.testTag("requestDetails"))
        }
    }
}

@Composable
private fun BusAlertsScreen(empty: Boolean) {
    var filter by rememberSaveable { mutableStateOf("All") }
    ScreenContent {
        ChoiceChips(listOf("All" to stringResource(R.string.all), "Ongoing" to stringResource(R.string.ongoing), "Finished" to stringResource(R.string.finished)), filter, { filter = it }, "busFilter")
        if (empty || filter == "Finished") {
            Spacer(Modifier.weight(1f).heightIn(min = 32.dp))
            Symbol(R.drawable.ic_history, Modifier.size(48.dp))
            Text(stringResource(R.string.no_bus_reports), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("emptyBusAlerts"))
            Spacer(Modifier.weight(1f))
        } else {
            listOf(RequestState.Collecting, RequestState.AwaitingApproval).forEachIndexed { index, state -> InfoCard {
                Text(stringResource(if (index == 0) R.string.report_one else R.string.report_two), style = MaterialTheme.typography.titleMedium)
                Text(pluralStringResource(R.plurals.request_count, if (index == 0) 2 else 3, if (index == 0) 2 else 3))
                RequestStatus(state)
                Text(stringResource(R.string.passenger_origin), style = MaterialTheme.typography.bodyMedium)
            } }
        }
        Notice(stringResource(R.string.shared_privacy), icon = R.drawable.ic_lock)
    }
}
