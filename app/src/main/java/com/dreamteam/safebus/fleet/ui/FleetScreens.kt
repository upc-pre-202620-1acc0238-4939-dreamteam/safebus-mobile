package com.dreamteam.safebus.fleet.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.dreamteam.safebus.R
import com.dreamteam.safebus.iam.ui.StepIndicator
import com.dreamteam.safebus.fleet.domain.*
import com.dreamteam.safebus.passenger.domain.BusQr
import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.shared.ui.*
import java.time.LocalDateTime
import java.time.ZoneId

@Composable fun FleetScreen(state: ScreenState<FleetState>, refresh: () -> Unit, sources: ((FleetBus) -> Unit)? = null) {
    val context = LocalContext.current
    var search by remember { mutableStateOf("") }
    var mapMissing by remember { mutableStateOf(false) }
    var map by remember { mutableStateOf(false) }
    var filter by remember { mutableIntStateOf(R.string.ui_all) }
    var selected by remember { mutableStateOf<Long?>(null) }
    CorePage(R.string.core_fleet, state.busy, state.failure, refresh) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.core_small))) {
            listOf(false to R.string.ui_list, true to R.string.ui_map).forEach { (value, label) ->
                FilterChip(map == value, { map = value }, { Text(stringResource(label)) },
                    Modifier.weight(1f).heightIn(min = dimensionResource(R.dimen.core_secondary_height)).testTag("fleetMap:$value"))
            }
        }
        CoreField(R.string.ui_fleet_search, search, { search = it }, "fleetSearch")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.core_small))) {
            listOf(R.string.ui_all, R.string.ui_current, R.string.ui_stale, R.string.ui_unavailable, R.string.ui_over_capacity).forEach { option ->
                FilterChip(filter == option, { filter = option }, { Text(stringResource(option)) },
                    Modifier.heightIn(min = dimensionResource(R.dimen.core_secondary_height)).testTag("fleetFilter:$option"))
            }
        }
        if (map) CoreNotice(stringResource(R.string.ui_map_fallback), icon = R.drawable.ic_map, modifier = Modifier.testTag("fleetMapFallback"))
        if (mapMissing) CoreNotice(stringResource(R.string.core_map_unavailable), modifier = Modifier.testTag("mapUnavailable"))
        state.value.overview?.let { overview ->
            TimeLabel(overview.updatedAt)
            val buses = overview.buses.filter { bus ->
                listOfNotNull(bus.plate, bus.route, bus.driver).any { it.contains(search, ignoreCase = true) } && when (filter) {
                    R.string.ui_current -> bus.locationStatus in setOf("CURRENT", "AVAILABLE")
                    R.string.ui_stale -> bus.locationStatus == "STALE"
                    R.string.ui_unavailable -> bus.position == null || bus.locationStatus == "UNAVAILABLE"
                    R.string.ui_over_capacity -> bus.load == "OVER_CAPACITY"
                    else -> true
                }
            }
            if (buses.isEmpty()) CoreEmpty(R.string.core_empty, icon = R.drawable.ic_directions_bus)
            buses.forEach { bus -> CoreCard {
                Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.core_small))) {
                    CoreIcon(R.drawable.ic_directions_bus)
                    Text(bus.plate, style = MaterialTheme.typography.titleLarge)
                }
                bus.route?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                bus.driver?.let { Text(stringResource(R.string.core_driver_name, it)) }
                StateLabel(bus.locationStatus)
                bus.position?.let { TimeLabel(it.capturedAt) }
                if (bus.count != null && bus.capacity != null) {
                    Text(pluralStringResource(R.plurals.ui_occupancy_ratio, bus.capacity, bus.count, bus.capacity))
                    LinearProgressIndicator(progress = { (bus.count.toFloat() / bus.capacity).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), trackColor = MaterialTheme.colorScheme.outlineVariant,
                        color = if (bus.load == "OVER_CAPACITY") colorResource(R.color.core_pending) else MaterialTheme.colorScheme.secondary)
                } else StateLabel(bus.occupancyStatus)
                if (bus.load == "OVER_CAPACITY") CoreNotice(stringResource(R.string.core_over_capacity), icon = R.drawable.ic_groups)
                Text(stringResource(R.string.core_open_cases,
                    pluralStringResource(R.plurals.core_emergency_count, bus.emergencyCount, bus.emergencyCount),
                    pluralStringResource(R.plurals.core_pending_reviews, bus.pendingGroups, bus.pendingGroups)), style = MaterialTheme.typography.bodyMedium)
                CoreButton(if (selected == bus.id) R.string.core_back else R.string.ui_view_details, "busDetails:${bus.id}", secondary = true) {
                    selected = if (selected == bus.id) null else bus.id
                }
                if (selected == bus.id) {
                    HorizontalDivider()
                    Text(stringResource(R.string.ui_bus_details), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.core_bus_reference, bus.id))
                    Text(stringResource(if (bus.enabled) R.string.core_enabled else R.string.core_disabled))
                    bus.capacity?.let { Text(pluralStringResource(R.plurals.core_capacity_value, it, it)) }
                    StateLabel(bus.occupancyStatus)
                    bus.count?.let { Text(pluralStringResource(R.plurals.core_count_value, it, it)) }
                    bus.lastCountAt?.let { Text(stringResource(R.string.core_count_time)); TimeLabel(it) }
                    bus.lastHeartbeatAt?.let { Text(stringResource(R.string.core_heartbeat_time)); TimeLabel(it) }
                    bus.groups.forEach { group ->
                        StateLabel(group.status); Text(stringResource(R.string.core_contributors, group.contributors))
                        if (group.attached > 0) Text(stringResource(R.string.core_attached_count, group.attached))
                    }
                    if (sources != null) CoreButton(R.string.extra_sources, "sources:${bus.id}", secondary = true) { sources(bus) }
                }
                if (map || selected == bus.id) bus.position?.let { position ->
                    Text(stringResource(R.string.core_coordinates, position.latitude, position.longitude), style = MaterialTheme.typography.bodySmall)
                    CoreButton(R.string.core_open_map, "map:${bus.id}", secondary = true, icon = R.drawable.ic_map) {
                        try { context.startActivity(Intent(Intent.ACTION_VIEW, "geo:${position.latitude},${position.longitude}?q=${position.latitude},${position.longitude}".toUri())) }
                        catch (_: ActivityNotFoundException) { mapMissing = true }
                    }
                }
            } }
        }
    }
}

enum class ProvisionForm(val title: Int) {
    BUS(R.string.core_create_bus), DRIVER(R.string.core_create_driver), ROUTE(R.string.core_create_route),
    ASSIGNMENT(R.string.core_create_assignment), CAPACITY(R.string.core_update_capacity)
}

@Composable fun ProvisionScreen(state: ScreenState<FleetState>, save: (FleetChange) -> Unit, clear: () -> Unit) {
    var form by remember { mutableStateOf(ProvisionForm.ASSIGNMENT) }
    var fields by remember(form) { mutableStateOf<Map<String, String>>(emptyMap()) }
    var step by remember(form) { mutableIntStateOf(0) }
    var invalid by remember(form) { mutableStateOf(false) }
    var submitting by remember(form) { mutableStateOf(false) }
    var saved by remember(form) { mutableStateOf(false) }
    var outcome by remember(form) { mutableStateOf<DomainResult<ProvisionReceipt>?>(null) }
    LaunchedEffect(submitting, state.value.saveResult, state.busy, state.failure) {
        if (submitting && outcome == null) {
            outcome = state.value.saveResult ?: if (!state.busy) DomainResult.Error(state.failure ?: Failure(FailureKind.CONFLICT, "SAVE_NOT_STARTED")) else null
            if (outcome is DomainResult.Success) saved = true
        }
    }
    val zone = ZoneId.systemDefault()
    CorePage(R.string.core_assignments, state.busy, state.failure) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.core_small))) {
            ProvisionForm.entries.forEach { option -> FilterChip(form == option, { form = option; clear() },
                { Text(stringResource(option.title)) }, Modifier.heightIn(min = dimensionResource(R.dimen.core_secondary_height)), enabled = !state.busy) }
        }
        Text(stringResource(form.title), style = MaterialTheme.typography.titleLarge)
        if (form == ProvisionForm.ASSIGNMENT) { StepIndicator(step, 5); CoreNotice(stringResource(R.string.core_catalog_note)) }
        if (invalid) FailureNotice(Failure(FailureKind.VALIDATION))
        val labels = when (form) {
            ProvisionForm.BUS -> listOf("plate" to R.string.core_plate)
            ProvisionForm.DRIVER -> listOf("name" to R.string.core_name, "login" to R.string.core_login_id, "password" to R.string.core_password)
            ProvisionForm.ROUTE -> listOf("name" to R.string.core_name, "origin" to R.string.core_origin, "destination" to R.string.core_destination)
            ProvisionForm.ASSIGNMENT -> listOf("driver" to R.string.core_driver_id, "bus" to R.string.core_bus_id, "route" to R.string.core_route_id, "start" to R.string.core_start_time, "end" to R.string.core_end_time)
            ProvisionForm.CAPACITY -> listOf("bus" to R.string.core_bus_id, "capacity" to R.string.core_capacity, "reference" to R.string.core_technical_record)
        }
        val visible = if (form != ProvisionForm.ASSIGNMENT) labels else when (step) {
            0 -> labels.take(1)
            1 -> labels.subList(1, 2)
            2 -> labels.subList(2, 3)
            3 -> labels.drop(3)
            else -> emptyList()
        }
        CoreCard(stringResource(if (form == ProvisionForm.ASSIGNMENT && step == 4) R.string.ui_assignment_review else form.title)) {
            visible.forEach { (key, label) -> CoreField(label, fields[key].orEmpty(), { fields = fields + (key to it); invalid = false; saved = false }, "provision:$key", password = key == "password", enabled = !state.busy) }
            if (form == ProvisionForm.ASSIGNMENT && step == 4) labels.forEach { (key, label) ->
                Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
                Text(fields[key].orEmpty())
            }
        }
        if (form == ProvisionForm.ASSIGNMENT) Text(stringResource(R.string.core_time_zone, zone.id))
        if (form == ProvisionForm.ASSIGNMENT && step < 4) {
            CoreButton(R.string.ui_continue, "assignmentNext", !state.busy) {
                invalid = if (step < 3) fields[visible.single().first]?.toLongOrNull()?.let { it > 0 } != true else runCatching {
                    LocalDateTime.parse(fields["end"]?.trim()) > LocalDateTime.parse(fields["start"]?.trim())
                }.getOrDefault(false).not()
                if (!invalid) step++
            }
        } else CoreButton(R.string.core_save, "saveProvision", !state.busy && !submitting && !saved) {
            fun value(key: String) = fields[key].orEmpty()
            val change = runCatching { when (form) {
                ProvisionForm.BUS -> FleetChange.Bus(value("plate"))
                ProvisionForm.DRIVER -> FleetChange.Driver(value("name"), value("login"), value("password"))
                ProvisionForm.ROUTE -> FleetChange.Route(value("name"), value("origin"), value("destination"))
                ProvisionForm.ASSIGNMENT -> FleetChange.Assignment(value("driver").toLong(), value("bus").toLong(), value("route").toLong(),
                    LocalDateTime.parse(value("start").trim()).atZone(zone).toInstant(), LocalDateTime.parse(value("end").trim()).atZone(zone).toInstant())
                ProvisionForm.CAPACITY -> FleetChange.Capacity(value("bus").toLong(), value("capacity").toInt(), value("reference"))
            } }.getOrNull()
            invalid = change == null || !change.valid()
            if (change != null && !invalid) {
                clear(); outcome = null; submitting = true; save(change); fields = fields - "password"
            }
        }
        if (form == ProvisionForm.ASSIGNMENT && step > 0) CoreButton(R.string.core_back, "assignmentBack", !state.busy, secondary = true) { step--; invalid = false }
        state.value.receipt?.let { receipt -> CoreCard(stringResource(R.string.core_saved_id, receipt.id)) {
            receipt.credential?.let { credential ->
                Text(stringResource(R.string.core_credential))
                SelectionContainer { Text(if (state.value.savedChange is FleetChange.Bus) BusQr(credential, receipt.id).encoded() else credential) }
            }
            receipt.expiresAt?.let { TimeLabel(it) }
        } }
    }
    if (submitting) {
        val result = outcome
        val finished = result != null
        val dismiss = { if (finished) { submitting = false; outcome = null } }
        AlertDialog(onDismissRequest = dismiss, modifier = Modifier.testTag("provisionFeedback"),
            title = { Text(stringResource(when (result) {
                is DomainResult.Success -> R.string.provision_saved_title
                is DomainResult.Error -> R.string.provision_failed_title
                null -> R.string.provision_saving_title
            })) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.core_small))) {
                    when (result) {
                        null -> { LinearProgressIndicator(Modifier.fillMaxWidth()); Text(stringResource(R.string.core_loading)) }
                        is DomainResult.Error -> Text(stringResource(failureText(result.failure)), Modifier.testTag("provisionSaveError"))
                        is DomainResult.Success -> {
                            Text(stringResource(R.string.core_saved_id, result.value.id), Modifier.testTag("provisionSavedId"))
                            result.value.credential?.let { credential ->
                                Text(stringResource(R.string.core_credential))
                                SelectionContainer { Text(if (form == ProvisionForm.BUS) BusQr(credential, result.value.id).encoded() else credential) }
                            }
                            result.value.expiresAt?.let { TimeLabel(it) }
                        }
                    }
                }
            },
            confirmButton = { if (finished) TextButton(dismiss, Modifier.testTag("acknowledgeProvision")) { Text(stringResource(R.string.provision_acknowledge)) } })
    }

}
