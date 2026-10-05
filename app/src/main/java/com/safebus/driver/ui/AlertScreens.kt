package com.safebus.driver.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.safebus.driver.R
import com.safebus.driver.ui.theme.SafeBusColors

@Composable
private fun EmergencyStatus(state: EmergencyState) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusChip(stringResource(R.string.critical), SafeBusColors.Critical, R.drawable.ic_warning)
        StatusChip(stringResource(state.label), state.color, state.icon, Modifier.testTag("status${state.name}"))
    }
}

@Composable
fun EmergencyScreen(state: EmergencyState, onDetails: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        EmergencyStatus(state)
        val headline = when (state) {
            EmergencyState.Pending -> R.string.status_pending
            EmergencyState.Active -> R.string.emergency_sent
            EmergencyState.InProgress -> R.string.status_progress
            EmergencyState.Closed -> R.string.status_closed
        }
        Text(stringResource(headline), style = MaterialTheme.typography.displaySmall, modifier = Modifier.testTag("emergencyHeadline"))
        Text(stringResource(state.message), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("emergencyMessage"))
        Text(stringResource(R.string.demo_status_notice), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f).heightIn(min = 32.dp))
        PrimaryAction(stringResource(R.string.view_details), onDetails, Modifier.testTag("viewDetails"), R.drawable.ic_arrow_forward)
    }
}

@Composable
fun EmergencyDetailsScreen(state: EmergencyState, historical: Boolean = false) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        EmergencyStatus(state)
        InfoCard {
            Text(stringResource(if (historical) R.string.historical_alert else R.string.case_reference), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.unit_route), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(if (historical) R.string.historical_time else R.string.registered_at), style = MaterialTheme.typography.bodyMedium)
        }
        Text(stringResource(R.string.timeline), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(state.message), style = MaterialTheme.typography.bodyLarge)
        if (state == EmergencyState.Closed) InfoCard {
            Text(stringResource(R.string.result), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.outcome), style = MaterialTheme.typography.bodyLarge)
        }
        Notice(stringResource(R.string.demo_status_notice))
    }
}

@Composable
fun AlertsScreen(current: EmergencyState?, emptyHistory: Boolean, onCurrent: () -> Unit, onHistory: () -> Unit) {
    var filter by rememberSaveable { mutableStateOf("All") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.own_alerts), style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = filter == "All", onClick = { filter = "All" }, label = { Text(stringResource(R.string.all)) }, modifier = Modifier.heightIn(min = 48.dp))
            EmergencyState.entries.forEach { state ->
                FilterChip(selected = filter == state.name, onClick = { filter = state.name }, label = { Text(stringResource(state.label)) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("filter${state.name}"))
            }
        }
        val showCurrent = current != null && (filter == "All" || filter == current.name)
        val showHistory = !emptyHistory && (filter == "All" || filter == EmergencyState.Closed.name)
        if (showCurrent) InfoCard {
            Text(stringResource(R.string.current_alert), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.unit_route), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.registered_at), style = MaterialTheme.typography.bodyMedium)
            EmergencyStatus(current!!)
            SecondaryAction(stringResource(R.string.view_details), onCurrent, Modifier.testTag("currentAlertDetails"))
        }
        if (showHistory) InfoCard {
            Text(stringResource(R.string.historical_alert), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.historical_time), style = MaterialTheme.typography.bodyMedium)
            StatusChip(stringResource(R.string.status_closed), SafeBusColors.Closed, R.drawable.ic_check_circle)
            SecondaryAction(stringResource(R.string.view_details), onHistory, Modifier.testTag("historyAlertDetails"))
        }
        if (!showCurrent && !showHistory) InfoCard {
            Symbol(R.drawable.ic_history, Modifier.size(32.dp))
            Text(stringResource(R.string.no_alerts), style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("emptyAlerts"))
            Text(stringResource(R.string.no_alerts_message), style = MaterialTheme.typography.bodyLarge)
        }
    }
}
