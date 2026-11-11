package com.dreamteam.safebus.trip.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.dreamteam.safebus.R
import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.shared.ui.*

@Composable fun TripScreen(state: ScreenState<TripState>, tracking: TrackingState, refresh: () -> Unit,
    activate: (String) -> Unit, close: () -> Unit, permission: () -> Unit) {
    var qr by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf(false) }
    CorePage(R.string.core_shift, state.busy, state.failure, refresh) {
        state.value.assignment?.let { assignment ->
            CoreCard(stringResource(R.string.ui_assigned_unit)) {
                CoreIcon(R.drawable.ic_directions_bus)
                StateLabel(assignment.status)
                Text(assignment.plate, style = MaterialTheme.typography.titleLarge)
                Text(assignment.route)
                Text(stringResource(R.string.core_route, assignment.origin, assignment.destination))
                TimeLabel(assignment.startsAt); TimeLabel(assignment.endsAt)
            }
        }
        val shift = state.value.shift
        if (shift != null) {
            CoreCard(stringResource(R.string.core_shift)) { StateLabel("ACTIVE"); TimeLabel(shift.startedAt) }
            TrackingPanel(tracking, permission)
            CoreButton(R.string.core_close_shift, "closeShift", !state.busy, secondary = true) { confirmation = true }
        } else if (state.value.assignment?.status == "ACTIVE") {
            CoreNotice(stringResource(R.string.core_restore_shift_missing), modifier = Modifier.testTag("shiftRecoveryUnavailable"))
        } else if (state.value.assignment != null) {
            QrInput(qr, { qr = it }, !state.busy)
            CoreButton(R.string.core_activate_shift, "activateShift", !state.busy && qr.isNotBlank()) { activate(qr); qr = "" }
        } else if (state.value.loaded) CoreEmpty(R.string.core_no_assignment, R.drawable.ic_assignment, Modifier.testTag("noAssignment"))
    }
    if (confirmation) AlertDialog(onDismissRequest = { confirmation = false }, title = { Text(stringResource(R.string.core_close_shift)) },
        text = { Text(stringResource(R.string.core_close_shift_detail)) }, confirmButton = { TextButton({ confirmation = false; close() }, Modifier.testTag("confirmShiftClose")) { Text(stringResource(R.string.core_confirm)) } },
        dismissButton = { TextButton({ confirmation = false }) { Text(stringResource(R.string.core_cancel)) } })
}

@Composable fun TrackingPanel(state: TrackingState, permission: () -> Unit) {
    CoreCard(stringResource(R.string.core_location)) {
        CoreNotice(stringResource(when (state.status) {
            TrackingStatus.STOPPED -> R.string.core_tracking_stopped
            TrackingStatus.WAITING -> R.string.core_tracking_waiting
            TrackingStatus.TRACKING -> R.string.core_tracking_active
            TrackingStatus.PENDING -> R.string.core_tracking_pending
            TrackingStatus.MANUAL_ONLY -> R.string.core_tracking_manual
            TrackingStatus.FAILED -> R.string.core_tracking_failed
            TrackingStatus.AUTO_ENDED -> R.string.core_tracking_ended
        }))
        state.failure?.let { FailureNotice(it) }
        state.lastSample?.let { TimeLabel(it) }
        CoreButton(R.string.core_enable_location, "enableLocation", secondary = true, icon = R.drawable.ic_location_on, action = permission)
    }
}
