package com.dreamteam.safebus.passenger.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.dreamteam.safebus.R
import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.shared.ui.*
import com.dreamteam.safebus.trip.ui.TrackingPanel

@Composable fun JourneyScreen(state: ScreenState<PassengerState>, tracking: TrackingState,
    start: (String) -> Unit, end: () -> Unit, request: () -> Unit, permission: () -> Unit) {
    var qr by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf(false) }
    CorePage(R.string.core_journey, state.busy, state.failure) {
        val journey = state.value.journey
        if (journey == null) {
            state.value.ended?.let { Text(stringResource(R.string.core_journey_ended)); DeliveryNotice(it) }
            if (tracking.status == TrackingStatus.AUTO_ENDED) Text(stringResource(R.string.core_tracking_ended))
            Text(stringResource(R.string.core_journey_intro))
            QrInput(qr, { qr = it }, !state.busy)
            CoreButton(R.string.core_start_journey, "startRealJourney", !state.busy && qr.isNotBlank()) { start(qr); qr = "" }
        } else {
            CoreCard(stringResource(R.string.core_journey_active)) {
                Text(journey.plate, Modifier.testTag("verifiedBus"), style = MaterialTheme.typography.headlineMedium)
                Text(journey.company); StateLabel(journey.validation)
                Text(journey.route); Text(stringResource(R.string.core_route, journey.origin, journey.destination))
                Text(stringResource(R.string.core_driver_name, journey.driver)); TimeLabel(journey.startedAt)
            }
            if (journey.busId == null) Text(stringResource(R.string.core_bus_id_missing))
            TrackingPanel(tracking, permission)
            CoreButton(R.string.core_request_new, "newRealRequest", !state.busy, action = request)
            CoreButton(R.string.core_end_journey, "endRealJourney", !state.busy) { confirmation = true }
        }
    }
    if (confirmation) AlertDialog(onDismissRequest = { confirmation = false }, title = { Text(stringResource(R.string.core_end_journey)) },
        text = { Text(stringResource(R.string.core_end_journey_detail)) }, confirmButton = { TextButton({ confirmation = false; end() }, Modifier.testTag("confirmRealJourneyEnd")) { Text(stringResource(R.string.core_confirm)) } },
        dismissButton = { TextButton({ confirmation = false }) { Text(stringResource(R.string.core_cancel)) } })
}
