package com.dreamteam.safebus.safetycase.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.dreamteam.safebus.R
import com.dreamteam.safebus.shared.domain.Photo
import com.dreamteam.safebus.shared.domain.Delivery
import com.dreamteam.safebus.shared.ui.*

enum class SafetyTab { DRIVER_ALERTS, REQUESTS, HISTORY, EMERGENCIES, REVIEWS }
private data class Decision(val title: Int, val id: String, val needsText: Boolean, val action: (String, String) -> Unit)

@Composable fun RequestFormScreen(state: ScreenState<SafetyState>, submit: (String, Photo?) -> Unit, back: () -> Unit) {
    var message by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf<Photo?>(null) }
    LaunchedEffect(state.value.receipt) { if (state.value.receipt != null) { message = ""; photo = null } }
    CorePage(R.string.core_request_new, state.busy, state.failure) {
        Text(stringResource(R.string.core_request_explanation))
        val receipt = state.value.receipt
        if (receipt != null) {
            DeliveryNotice(receipt.delivery)
            Text(stringResource(R.string.core_case_reference, receipt.id))
        } else {
            CoreField(R.string.core_request_message, message, { if (it.length <= 500) message = it }, "realIncidentMessage", multiline = true, enabled = !state.busy)
            PhotoChooser(photo, { photo = it }, !state.busy)
            CoreButton(R.string.core_send_request, "sendRealRequest", !state.busy && message.trim().length in 1..500 && photo != null) { submit(message, photo) }
        }
        CoreButton(R.string.core_back, "requestBack", !state.busy, action = back)
    }
}

@Composable fun SafetyListScreen(state: ScreenState<SafetyState>, tab: SafetyTab, refresh: () -> Unit,
    evidence: (String) -> Unit, clearEvidence: () -> Unit, approve: (Long) -> Unit = {}, decline: (Long, String, String) -> Unit = { _, _, _ -> },
    attend: (String) -> Unit = {}, close: (String, String, String) -> Unit = { _, _, _ -> }, detail: ((String) -> Unit)? = null) {
    var decision by remember(tab) { mutableStateOf<Decision?>(null) }
    val title = when (tab) {
        SafetyTab.DRIVER_ALERTS -> R.string.core_my_alerts
        SafetyTab.REQUESTS -> R.string.core_my_requests
        SafetyTab.HISTORY -> R.string.core_bus_alerts
        SafetyTab.EMERGENCIES -> R.string.core_emergencies
        SafetyTab.REVIEWS -> R.string.core_reviews
    }
    CorePage(title, state.busy, state.failure, refresh) {
        if (state.value.decisionSaved) Text(stringResource(R.string.core_decision_saved))
        when (tab) {
            SafetyTab.DRIVER_ALERTS, SafetyTab.EMERGENCIES -> {
                val alerts = if (tab == SafetyTab.DRIVER_ALERTS) state.value.driverAlerts else state.value.queue?.emergencies.orEmpty()
                if (alerts.isEmpty() && state.value.loaded) Text(stringResource(R.string.core_empty))
                alerts.forEach { alert -> CoreCard(stringResource(R.string.core_case_reference, alert.id)) {
                    StateLabel(alert.priority); StateLabel(alert.status); TimeLabel(alert.activatedAt)
                    alert.busId?.let { Text(stringResource(R.string.core_bus_reference, it)) }
                    alert.response?.let { Text(it) }
                    if (tab == SafetyTab.EMERGENCIES) {
                        if (alert.status == "ACTIVE") CoreButton(R.string.core_attend, "attend:${alert.id}", !state.busy) {
                            decision = Decision(R.string.core_attend, alert.id, false) { _, _ -> attend(alert.id) }
                        }
                        if (alert.status == "IN_PROGRESS") CoreButton(R.string.core_close_emergency, "close:${alert.id}", !state.busy) {
                            decision = Decision(R.string.core_close_emergency, alert.id, true) { outcome, response -> close(alert.id, outcome, response) }
                        }
                    }
                } }
            }
            SafetyTab.REQUESTS -> {
                if (state.value.requests.isEmpty() && state.value.loaded) Text(stringResource(R.string.core_empty))
                state.value.requests.forEach { request -> CoreCard(stringResource(R.string.core_case_reference, request.id)) {
                    Text(request.message); StateLabel(request.status); TimeLabel(request.submittedAt)
                    request.contributorCount?.let { Text(stringResource(R.string.core_contributors, it)) }
                    request.decision?.let { StateLabel(it) }; request.decidedAt?.let { TimeLabel(it) }; request.response?.let { Text(it) }
                    if (request.status == "PENDING") DeliveryNotice(Delivery.PENDING)
                    if (detail != null) CoreButton(R.string.extra_request_detail, "requestDetail:${request.id}", !state.busy && request.status != "PENDING") { detail(request.id) }
                    CoreButton(R.string.core_view_photo, "evidence:${request.id}", !state.busy && request.status != "PENDING") { evidence(request.id) }
                } }
            }
            SafetyTab.HISTORY -> {
                if (state.value.history.isEmpty() && state.value.loaded) Text(stringResource(R.string.core_empty))
                state.value.history.forEach { item -> CoreCard(item.reference) {
                    Text(stringResource(if (item.source == "DRIVER") R.string.core_role_driver else R.string.core_role_passenger))
                    StateLabel(item.state); TimeLabel(item.recordedAt)
                    item.count?.let { Text(stringResource(R.string.core_contributors, it)) }
                } }
            }
            SafetyTab.REVIEWS -> {
                val groups = state.value.queue?.groups.orEmpty()
                if (groups.isEmpty() && state.value.loaded) Text(stringResource(R.string.core_empty))
                groups.forEach { group -> CoreCard(group.reference) {
                    Text(stringResource(R.string.core_bus_reference, group.busId)); Text(stringResource(R.string.core_contributors, group.contributors))
                    group.requests.forEach { request ->
                        Text(request.message); TimeLabel(request.submittedAt)
                        CoreButton(R.string.core_view_photo, "evidence:${request.id}", !state.busy) { evidence(request.id) }
                    }
                    CoreButton(R.string.core_approve, "approve:${group.id}", !state.busy) {
                        decision = Decision(R.string.core_approve, group.reference, false) { _, _ -> approve(group.id) }
                    }
                    CoreButton(R.string.core_decline, "decline:${group.id}", !state.busy) {
                        decision = Decision(R.string.core_decline, group.reference, true) { reason, response -> decline(group.id, reason, response) }
                    }
                } }
            }
        }
    }
    state.value.evidence?.let { photo -> AlertDialog(onDismissRequest = clearEvidence, title = { Text(stringResource(R.string.core_view_photo)) },
        text = { PrivatePhoto(photo) }, confirmButton = { TextButton(clearEvidence) { Text(stringResource(R.string.core_back)) } }) }
    decision?.let { choice ->
        var reason by remember(choice) { mutableStateOf("") }
        var response by remember(choice) { mutableStateOf("") }
        AlertDialog(onDismissRequest = { decision = null }, title = { Text(stringResource(choice.title)) }, text = {
            Column {
                Text(choice.id)
                if (choice.needsText) {
                    CoreField(R.string.core_decision_reason, reason, { if (it.length <= 500) reason = it }, "decisionReason", multiline = true)
                    CoreField(R.string.core_decision_response, response, { if (it.length <= 500) response = it }, "decisionResponse", multiline = true)
                } else Text(stringResource(if (choice.title == R.string.core_approve) R.string.core_approve_detail else R.string.core_attend_detail))
            }
        }, confirmButton = { TextButton({ decision = null; choice.action(reason, response) }, enabled = !choice.needsText || reason.isNotBlank(), modifier = Modifier.testTag("confirmDecision")) { Text(stringResource(R.string.core_confirm)) } },
            dismissButton = { TextButton({ decision = null }) { Text(stringResource(R.string.core_cancel)) } })
    }
}
