package com.dreamteam.safebus.fleet.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.dreamteam.safebus.R
import com.dreamteam.safebus.fleet.domain.*
import com.dreamteam.safebus.passenger.domain.BusQr
import com.dreamteam.safebus.shared.domain.*
import com.dreamteam.safebus.shared.ui.*
import java.time.LocalDateTime
import java.time.ZoneId

@Composable fun FleetScreen(state: ScreenState<FleetState>, refresh: () -> Unit) {
    val context = LocalContext.current
    var search by remember { mutableStateOf("") }
    var mapMissing by remember { mutableStateOf(false) }
    CorePage(R.string.core_fleet, state.busy, state.failure, refresh) {
        CoreField(R.string.core_search_bus, search, { search = it }, "fleetSearch")
        if (mapMissing) Text(stringResource(R.string.core_map_unavailable))
        state.value.overview?.let { overview ->
            TimeLabel(overview.updatedAt)
            val buses = overview.buses.filter { it.plate.contains(search, ignoreCase = true) }
            if (buses.isEmpty()) Text(stringResource(R.string.core_empty))
            buses.forEach { bus -> CoreCard(bus.plate) {
                Text(stringResource(R.string.core_bus_reference, bus.id))
                Text(stringResource(if (bus.enabled) R.string.core_enabled else R.string.core_disabled))
                bus.route?.let { Text(it) }; bus.driver?.let { Text(stringResource(R.string.core_driver_name, it)) }
                bus.capacity?.let { Text(stringResource(R.string.core_capacity_value, it)) }
                StateLabel(bus.occupancyStatus)
                bus.count?.let { Text(stringResource(R.string.core_count_value, it)) }
                bus.lastCountAt?.let { Text(stringResource(R.string.core_count_time)); TimeLabel(it) }
                bus.lastHeartbeatAt?.let { Text(stringResource(R.string.core_heartbeat_time)); TimeLabel(it) }
                if (bus.load == "OVER_CAPACITY") Text(stringResource(R.string.core_over_capacity), color = MaterialTheme.colorScheme.error)
                Text(stringResource(R.string.core_open_cases, bus.emergencyCount, bus.pendingGroups))
                bus.groups.forEach { group ->
                    StateLabel(group.status)
                    Text(stringResource(R.string.core_contributors, group.contributors))
                    if (group.attached > 0) Text(stringResource(R.string.core_attached_count, group.attached))
                }
                StateLabel(bus.locationStatus)
                bus.position?.let { position ->
                    TimeLabel(position.capturedAt)
                    Text(stringResource(R.string.core_coordinates, position.latitude, position.longitude))
                    CoreButton(R.string.core_open_map, "map:${bus.id}") {
                        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:${position.latitude},${position.longitude}?q=${position.latitude},${position.longitude}"))) }
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
    var invalid by remember(form) { mutableStateOf(false) }
    val zone = ZoneId.systemDefault()
    CorePage(R.string.core_assignments, state.busy, state.failure) {
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            ProvisionForm.entries.forEach { option -> TextButton({ form = option; clear() }, enabled = !state.busy) { Text(stringResource(option.title)) } }
        }
        Text(stringResource(form.title), style = MaterialTheme.typography.titleLarge)
        if (form == ProvisionForm.ASSIGNMENT) Text(stringResource(R.string.core_catalog_note))
        if (invalid) FailureNotice(Failure(FailureKind.VALIDATION))
        val labels = when (form) {
            ProvisionForm.BUS -> listOf("plate" to R.string.core_plate)
            ProvisionForm.DRIVER -> listOf("name" to R.string.core_name, "login" to R.string.core_login_id, "password" to R.string.core_password)
            ProvisionForm.ROUTE -> listOf("name" to R.string.core_name, "origin" to R.string.core_origin, "destination" to R.string.core_destination)
            ProvisionForm.ASSIGNMENT -> listOf("driver" to R.string.core_driver_id, "bus" to R.string.core_bus_id, "route" to R.string.core_route_id, "start" to R.string.core_start_time, "end" to R.string.core_end_time)
            ProvisionForm.CAPACITY -> listOf("bus" to R.string.core_bus_id, "capacity" to R.string.core_capacity, "reference" to R.string.core_technical_record)
        }
        labels.forEach { (key, label) -> CoreField(label, fields[key].orEmpty(), { fields = fields + (key to it) }, "provision:$key", password = key == "password", enabled = !state.busy) }
        if (form == ProvisionForm.ASSIGNMENT) Text(stringResource(R.string.core_time_zone, zone.id))
        CoreButton(R.string.core_save, "saveProvision", !state.busy) {
            fun value(key: String) = fields[key].orEmpty()
            val change = runCatching { when (form) {
                ProvisionForm.BUS -> FleetChange.Bus(value("plate"))
                ProvisionForm.DRIVER -> FleetChange.Driver(value("name"), value("login"), value("password"))
                ProvisionForm.ROUTE -> FleetChange.Route(value("name"), value("origin"), value("destination"))
                ProvisionForm.ASSIGNMENT -> FleetChange.Assignment(value("driver").toLong(), value("bus").toLong(), value("route").toLong(),
                    LocalDateTime.parse(value("start")).atZone(zone).toInstant(), LocalDateTime.parse(value("end")).atZone(zone).toInstant())
                ProvisionForm.CAPACITY -> FleetChange.Capacity(value("bus").toLong(), value("capacity").toInt(), value("reference"))
            } }.getOrNull()
            invalid = change == null || !change.valid()
            if (change != null && !invalid) { clear(); save(change); fields = fields - "password" }
        }
        state.value.receipt?.let { receipt -> CoreCard(stringResource(R.string.core_saved_id, receipt.id)) {
            receipt.credential?.let { credential ->
                Text(stringResource(R.string.core_credential))
                SelectionContainer { Text(if (state.value.savedChange is FleetChange.Bus) BusQr(credential, receipt.id).encoded() else credential) }
            }
            receipt.expiresAt?.let { TimeLabel(it) }
        } }
    }
}
