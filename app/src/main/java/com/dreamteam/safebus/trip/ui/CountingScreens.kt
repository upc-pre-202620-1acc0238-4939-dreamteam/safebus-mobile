package com.dreamteam.safebus.trip.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.dreamteam.safebus.R
import com.dreamteam.safebus.shared.ui.*
import com.dreamteam.safebus.trip.domain.CountAction
import kotlinx.coroutines.delay

@Composable fun CountingScreen(state: ScreenState<CountingState>, refresh: () -> Unit, register: (String) -> Unit,
    rotate: (Long) -> Unit, revoke: (Long) -> Unit, hideKey: () -> Unit, counter: (() -> Unit)?, back: () -> Unit) {
    var label by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf<Pair<Long, Boolean>?>(null) }
    CorePage(R.string.extra_sources, state.busy, state.failure, refresh) {
        CoreButton(R.string.core_back, "sourcesBack", action = back)
        state.value.occupancy?.let { occupancy -> CoreCard(stringResource(R.string.extra_occupancy)) {
            StateLabel(occupancy.status)
            occupancy.count?.let { Text(stringResource(R.string.core_count_value, it)) }
            occupancy.capacity?.let { Text(stringResource(R.string.core_capacity_value, it)) }
            occupancy.lastCountAt?.let { Text(stringResource(R.string.core_count_time)); TimeLabel(it) }
            occupancy.lastHeartbeatAt?.let { Text(stringResource(R.string.core_heartbeat_time)); TimeLabel(it) }
            TimeLabel(occupancy.generatedAt)
        } }
        if (state.value.loaded && state.value.sources.isEmpty()) Text(stringResource(R.string.extra_no_sources))
        state.value.sources.forEach { source -> CoreCard(stringResource(R.string.extra_source_ref, source.id)) {
            source.label?.let { Text(it) }
            Text(stringResource(if (source.status == "ACTIVE") R.string.core_enabled else R.string.extra_source_revoked))
            TimeLabel(source.createdAt); source.rotatedAt?.let { TimeLabel(it) }; source.revokedAt?.let { TimeLabel(it) }
            if (source.status == "ACTIVE") {
                CoreButton(R.string.extra_rotate_source, "rotate:${source.id}", !state.busy) { confirmation = source.id to true }
                CoreButton(R.string.extra_revoke_source, "revoke:${source.id}", !state.busy, danger = true) { confirmation = source.id to false }
            }
        } }
        if (state.value.sources.none { it.status == "ACTIVE" }) {
            CoreField(R.string.extra_source_label, label, { if (it.length <= 60) label = it }, "sourceLabel", enabled = !state.busy)
            CoreButton(R.string.extra_register_source, "registerSource", !state.busy && state.value.loaded) { register(label) }
        }
        state.value.credential?.let { credential -> CoreCard(stringResource(R.string.core_credential)) {
            Text(stringResource(R.string.extra_key_once))
            SelectionContainer { Text(credential.key, Modifier.testTag("sourceKey")) }
            CoreButton(R.string.extra_hide_key, "hideSourceKey", action = hideKey)
        } }
        if (counter != null) CoreButton(R.string.extra_counter, "openCounter", action = counter)
        else Text(stringResource(R.string.extra_counter_unavailable))
    }
    confirmation?.let { (id, rotation) -> AlertDialog(onDismissRequest = { confirmation = null },
        title = { Text(stringResource(if (rotation) R.string.extra_rotate_source else R.string.extra_revoke_source)) },
        text = { Text(stringResource(R.string.extra_source_confirm)) },
        confirmButton = { TextButton({ confirmation = null; if (rotation) rotate(id) else revoke(id) }, Modifier.testTag("confirmSource")) { Text(stringResource(R.string.core_confirm)) } },
        dismissButton = { TextButton({ confirmation = null }) { Text(stringResource(R.string.core_cancel)) } }) }
}

@Composable fun CounterScreen(state: ScreenState<CounterUiState>, foreground: Boolean,
    send: (CountAction, Long, String, Boolean) -> Unit, retry: (String) -> Unit, heartbeat: (String) -> Unit, back: () -> Unit) {
    var key by remember { mutableStateOf("") }
    var sequence by remember(state.value.counter.receipt) { mutableStateOf((state.value.counter.receipt?.nextSequence ?: 1).toString()) }
    var empty by remember { mutableStateOf(false) }
    var automatic by remember { mutableStateOf(false) }
    val pending = state.value.counter.pending
    val currentRetry by rememberUpdatedState(retry)
    val currentHeartbeat by rememberUpdatedState(heartbeat)
    val hasPending by rememberUpdatedState(pending != null)
    LaunchedEffect(automatic, key, foreground) {
        if (automatic && key.isNotBlank() && foreground) while (true) {
            if (hasPending) currentRetry(key) else currentHeartbeat(key)
            delay(45_000)
        }
    }
    CorePage(R.string.extra_counter_title, state.busy, state.failure) {
        CoreButton(R.string.core_back, "counterBack", action = back)
        Text(stringResource(R.string.extra_counter_help))
        CoreField(R.string.extra_device_key, key, { key = it }, "counterKey", password = true, enabled = !state.busy)
        state.failure?.expectedSequence?.let { Text(stringResource(R.string.extra_expected_sequence, it)) }
        state.value.counter.receipt?.let { receipt ->
            Text(stringResource(R.string.core_count_value, receipt.count))
            Text(stringResource(R.string.extra_next_sequence, receipt.nextSequence))
        }
        if (pending != null) {
            Text(stringResource(R.string.extra_counter_pending)); TimeLabel(pending.capturedAt)
            CoreButton(R.string.extra_retry_count, "retryCount", !state.busy && key.isNotBlank()) { retry(key) }
        } else {
            Row { Checkbox(empty, { empty = it }, enabled = !state.busy, modifier = Modifier.testTag("confirmEmptyBus")); Text(stringResource(R.string.extra_empty_confirm)) }
            CoreButton(R.string.extra_initialize, "initializeCount", !state.busy && empty && key.isNotBlank()) { send(CountAction.INITIALIZE, 1, key, empty); empty = false }
            CoreField(R.string.extra_sequence, sequence, { sequence = it }, "countSequence", enabled = !state.busy)
            val valid = sequence.toLongOrNull()?.let { it > 0 && it < Long.MAX_VALUE } == true && key.isNotBlank() && !state.busy
            CoreButton(R.string.extra_entry, "countEntry", valid) { send(CountAction.ENTRY, sequence.toLong(), key, false) }
            CoreButton(R.string.extra_exit, "countExit", valid) { send(CountAction.EXIT, sequence.toLong(), key, false) }
        }
        CoreButton(R.string.extra_heartbeat, "heartbeat", !state.busy && key.isNotBlank()) { heartbeat(key) }
        Row { Checkbox(automatic, { automatic = it }, modifier = Modifier.testTag("automaticHeartbeat")); Text(stringResource(R.string.extra_auto_heartbeat)) }
        state.value.heartbeat?.let { TimeLabel(it) }
    }
}
