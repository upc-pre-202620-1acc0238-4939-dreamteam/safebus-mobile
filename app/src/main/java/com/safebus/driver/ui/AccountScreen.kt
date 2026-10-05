package com.safebus.driver.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.safebus.driver.R

@Composable
fun AccountScreen(
    language: String, onLanguage: (String) -> Unit,
    connected: Boolean, onConnection: (Boolean) -> Unit,
    shift: ShiftState, onAssignment: (Boolean) -> Unit,
    emptyHistory: Boolean, onEmptyHistory: (Boolean) -> Unit,
    emergency: EmergencyState?, onEmergencyState: (EmergencyState) -> Unit,
    onSignOut: () -> Unit, onReset: () -> Unit,
) {
    var showTerms by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        InfoCard {
            Symbol(R.drawable.ic_account_circle, Modifier.size(32.dp))
            Text(stringResource(R.string.profile_name), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.driver_role), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.company), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.driver_code), style = MaterialTheme.typography.bodyMedium)
        }
        Text(stringResource(R.string.language), style = MaterialTheme.typography.titleLarge)
        LanguageSelector(language, onLanguage)
        SecondaryAction(stringResource(R.string.terms), { showTerms = true })
        SecondaryAction(stringResource(R.string.sign_out), onSignOut, Modifier.testTag("signOut"), R.drawable.ic_logout)
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Text(stringResource(R.string.demo_scenarios), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.demo_scenarios_help), style = MaterialTheme.typography.bodyMedium)
        InfoCard {
            ScenarioSwitch(stringResource(R.string.connection), stringResource(if (connected) R.string.online else R.string.offline), connected, onConnection, "connectionSwitch")
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            ScenarioSwitch(stringResource(R.string.assignment), stringResource(if (shift == ShiftState.None) R.string.no_shift else R.string.assigned), shift != ShiftState.None, onAssignment, "assignmentSwitch")
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            ScenarioSwitch(stringResource(R.string.empty_history), "", emptyHistory, onEmptyHistory, "emptyHistorySwitch")
        }
        if (emergency != null && emergency != EmergencyState.Pending) {
            Text(stringResource(R.string.mock_emergency_status), style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(EmergencyState.Active, EmergencyState.InProgress, EmergencyState.Closed).forEach { state ->
                    FilterChip(selected = emergency == state, onClick = { onEmergencyState(state) }, label = { Text(stringResource(state.label)) },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("demo${state.name}"))
                }
            }
        }
        SecondaryAction(stringResource(R.string.reset_demo), onReset, Modifier.testTag("resetDemo"))
        Notice(stringResource(R.string.demo_status_notice))
    }
    if (showTerms) AlertDialog(onDismissRequest = { showTerms = false }, title = { Text(stringResource(R.string.terms)) },
        text = { Text(stringResource(R.string.terms_message)) },
        confirmButton = { DialogAction(onClick = { showTerms = false }) { Text(stringResource(R.string.ok)) } })
}

@Composable
fun ScenarioSwitch(label: String, detail: String, checked: Boolean, onChecked: (Boolean) -> Unit, tag: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            if (detail.isNotEmpty()) Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked, onChecked, Modifier.testTag(tag).semantics { contentDescription = label })
    }
}
