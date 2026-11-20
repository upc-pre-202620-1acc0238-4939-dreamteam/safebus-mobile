package com.dreamteam.safebus.safetycase.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.*
import com.dreamteam.safebus.R
import com.dreamteam.safebus.shared.ui.*

enum class SafetyTab { DRIVER_ALERTS, EMERGENCIES, REVIEWS }
private data class Decision(val title: Int, val id: String, val needsText: Boolean, val action: (String, String) -> Unit)

@Composable private fun ContributionProgress(count: Int) {
    Text(stringResource(R.string.core_contributors, count))
    LinearProgressIndicator(progress = { (count / 3f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), trackColor = MaterialTheme.colorScheme.outlineVariant,
        color = MaterialTheme.colorScheme.secondary)
}

@Composable private fun CaseTabs(completed: Boolean, select: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.core_small))) {
        listOf(false to R.string.ui_active_cases, true to R.string.ui_finished_cases).forEach { (value, label) ->
            FilterChip(completed == value, { select(value) }, { Text(stringResource(label)) },
                Modifier.weight(1f).heightIn(min = dimensionResource(R.dimen.core_secondary_height)).testTag("cases:$value"))
        }
    }
}

@Composable fun SafetyListScreen(state: ScreenState<SafetyState>, tab: SafetyTab, refresh: () -> Unit,
    evidence: (String) -> Unit, clearEvidence: () -> Unit, approve: (Long) -> Unit = {}, decline: (Long, String, String) -> Unit = { _, _, _ -> },
    attend: (String) -> Unit = {}, close: (String, String, String) -> Unit = { _, _, _ -> }) {
    var selectedGroup by remember(tab) { mutableStateOf<Long?>(null) }
    var selectedAlert by remember(tab) { mutableStateOf<String?>(null) }
    BackHandler(selectedGroup != null || selectedAlert != null) { selectedGroup = null; selectedAlert = null }
    var completed by remember(tab) { mutableStateOf(false) }
    var decision by remember(tab) { mutableStateOf<Decision?>(null) }
    val title = when (tab) {
        SafetyTab.DRIVER_ALERTS -> R.string.core_my_alerts
        SafetyTab.EMERGENCIES -> R.string.core_emergencies
        SafetyTab.REVIEWS -> R.string.core_reviews
    }
    CorePage(if (selectedGroup != null) R.string.ui_review_group else if (selectedAlert != null) R.string.core_emergency else title, state.busy, state.failure, refresh,
        back = if (selectedGroup != null || selectedAlert != null) ({ selectedGroup = null; selectedAlert = null }) else null) {
        if (state.value.decisionSaved) CoreNotice(stringResource(R.string.core_decision_saved), icon = R.drawable.ic_check_circle)
        when (tab) {
            SafetyTab.DRIVER_ALERTS, SafetyTab.EMERGENCIES -> {
                if (tab == SafetyTab.DRIVER_ALERTS) CaseTabs(completed) { completed = it }
                val alerts = (if (tab == SafetyTab.DRIVER_ALERTS) state.value.driverAlerts.filter { (it.status == "CLOSED") == completed }
                    else state.value.queue?.emergencies.orEmpty()).sortedBy { if (it.priority == "CRITICAL") 0 else 1 }
                if (alerts.isEmpty() && state.value.loaded) CoreEmpty(R.string.ui_no_emergencies, icon = R.drawable.ic_shield)
                alerts.filter { selectedAlert == null || it.id == selectedAlert }.forEachIndexed { index, alert ->
                    if (tab == SafetyTab.EMERGENCIES && selectedAlert == null && (index == 0 || alerts[index - 1].priority != alert.priority))
                        Text(stringResource(if (alert.priority == "CRITICAL") R.string.ui_driver_emergencies else R.string.ui_passenger_emergencies), style = MaterialTheme.typography.titleLarge)
                    CoreCard(accent = colorResource(stateColorResource(alert.priority))) {
                    StateLabel(alert.priority); StateLabel(alert.status); TimeLabel(alert.activatedAt)
                    Text(stringResource(R.string.core_case_reference, alert.id), style = MaterialTheme.typography.bodySmall)
                    alert.busId?.let { Text(stringResource(R.string.core_bus_reference, it)) }
                    alert.response?.let { Text(it) }
                    if (tab == SafetyTab.EMERGENCIES && selectedAlert == null) CoreButton(R.string.ui_view_details, "emergencyDetail:${alert.id}", secondary = true) { selectedAlert = alert.id }
                    if (tab == SafetyTab.EMERGENCIES && selectedAlert == alert.id) {
                        if (alert.status == "ACTIVE") CoreButton(R.string.core_attend, "attend:${alert.id}", !state.busy) {
                            decision = Decision(R.string.core_attend, alert.id, false) { _, _ -> attend(alert.id) }
                        }
                        if (alert.status == "IN_PROGRESS") CoreButton(R.string.core_close_emergency, "close:${alert.id}", !state.busy) {
                            decision = Decision(R.string.core_close_emergency, alert.id, true) { outcome, response -> close(alert.id, outcome, response) }
                        }
                    }
                } }
            }
            SafetyTab.REVIEWS -> {
                val groups = state.value.queue?.groups.orEmpty()
                if (groups.isEmpty() && state.value.loaded) CoreEmpty(R.string.ui_no_reviews, icon = R.drawable.ic_fact_check)
                groups.filter { selectedGroup == null || it.id == selectedGroup }.forEach { group -> CoreCard(group.reference, accent = colorResource(R.color.core_pending)) {
                    StateLabel("AWAITING_APPROVAL")
                    Text(stringResource(R.string.core_bus_reference, group.busId)); ContributionProgress(group.contributors)
                    if (selectedGroup == null) CoreButton(R.string.ui_review_group, "reviewGroup:${group.id}") { selectedGroup = group.id }
                    if (selectedGroup == group.id) {
                    group.requests.forEach { request ->
                        HorizontalDivider()
                        Text(request.message); TimeLabel(request.submittedAt)
                        CoreButton(R.string.core_view_photo, "evidence:${request.id}", !state.busy, secondary = true, icon = R.drawable.ic_photo_camera) { evidence(request.id) }
                    }
                    CoreButton(R.string.core_approve, "approve:${group.id}", !state.busy) {
                        decision = Decision(R.string.core_approve, group.reference, false) { _, _ -> approve(group.id) }
                    }
                    CoreButton(R.string.core_decline, "decline:${group.id}", !state.busy, secondary = true) {
                        decision = Decision(R.string.core_decline, group.reference, true) { reason, response -> decline(group.id, reason, response) }
                    }
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
