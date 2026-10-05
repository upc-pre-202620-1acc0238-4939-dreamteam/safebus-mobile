package com.safebus.driver.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.safebus.driver.R
import com.safebus.driver.ui.theme.SafeBusColors

enum class LocationStatus(val label: Int) { Fresh(R.string.fresh), Stale(R.string.stale), Unavailable(R.string.unavailable) }
enum class BusSafety(val label: Int) {
    Critical(R.string.critical), High(R.string.high), Pending(R.string.awaiting_approval), Normal(R.string.normal);
    val color get() = when (this) { Critical -> SafeBusColors.Critical; High -> SafeBusColors.High; Pending -> SafeBusColors.Pending; Normal -> SafeBusColors.Secondary }
}
data class FleetBus(val plate: String, val route: String, val driver: String, val location: LocationStatus, val time: String, val passengers: Int, val capacity: Int,
    val safety: BusSafety = BusSafety.Normal, val coordinates: String = "−12.0464, −77.0428")
object MockFleet {
    val buses = listOf(
        FleetBus("A1B-702", "23", "Carlos Mamani R.", LocationStatus.Fresh, "14:32", 32, 42),
        FleetBus("EX-108", "107", "Jorge Quispe", LocationStatus.Stale, "13:45", 40, 42, BusSafety.High, "−12.0718, −77.0341"),
        FleetBus("AL-302", "201", "Rosa Vargas", LocationStatus.Fresh, "14:32", 27, 40, BusSafety.Critical, "−12.0522, −77.0285"),
        FleetBus("SM-420", "204", "Elena Díaz", LocationStatus.Unavailable, "—", 28, 42, BusSafety.Pending, "—"),
        FleetBus("B8K-114", "23", "Luis Sánchez", LocationStatus.Fresh, "14:31", 44, 42),
    )
}

@Composable
fun FleetScreen(mapAvailable: Boolean, capacities: Map<String, Int>, onBus: (String) -> Unit) {
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    var map by rememberSaveable { mutableStateOf(false) }
    var selected by rememberSaveable { mutableStateOf("A1B-702") }
    val buses = MockFleet.buses.filter { bus ->
        (query.isBlank() || "${bus.plate} ${bus.route} ${bus.driver}".contains(query.trim(), ignoreCase = true)) &&
            (filter == "All" || filter == bus.location.name || (filter == "Over" && bus.passengers > (capacities[bus.plate] ?: bus.capacity)))
    }.sortedWith(compareBy<FleetBus> { it.safety.ordinal }.thenBy { it.plate })
    ScreenContent {
        SafeTextField(query, { query = it }, singleLine = true, label = { Text(stringResource(R.string.fleet_search)) },
            leadingIcon = { Symbol(R.drawable.ic_search) }, modifier = Modifier.fillMaxWidth().testTag("fleetSearch"))
        ChoiceChips(listOf("List" to stringResource(R.string.list), "Map" to stringResource(R.string.map)), if (map) "Map" else "List", { focus.clearFocus(); map = it == "Map" }, "fleetView")
        ChoiceChips(listOf("All" to stringResource(R.string.all), "Fresh" to stringResource(R.string.fresh), "Stale" to stringResource(R.string.stale),
            "Unavailable" to stringResource(R.string.unavailable), "Over" to stringResource(R.string.over_capacity)), filter, { filter = it }, "fleetFilter")
        if (map && !mapAvailable) Notice(stringResource(R.string.map_unavailable), Modifier.testTag("mapFallback"))
        if (buses.isEmpty()) Notice(stringResource(R.string.fleet_empty), Modifier.testTag("emptyFleet"))
        else if (map && mapAvailable) {
            FleetMap(buses.filter { it.location != LocationStatus.Unavailable }, selected, { selected = it })
            val bus = buses.firstOrNull { it.plate == selected } ?: buses.first()
            FleetCard(bus, capacities[bus.plate] ?: bus.capacity, { onBus(bus.plate) })
        } else buses.forEach { bus -> FleetCard(bus, capacities[bus.plate] ?: bus.capacity, { onBus(bus.plate) }) }
    }
}

@Composable
private fun FleetMap(buses: List<FleetBus>, selected: String, onSelect: (String) -> Unit) {
    val street = MaterialTheme.colorScheme.outline
    val route = SafeBusColors.Secondary
    val scale = androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceAtLeast(1f)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.map_schematic), style = MaterialTheme.typography.bodyMedium)
        Box(Modifier.fillMaxWidth().height((300 * scale).dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).testTag("fleetMap")) {
            Canvas(Modifier.matchParentSize()) {
                repeat(7) { i -> val x = size.width * i / 6; drawLine(street, Offset(x, 0f), Offset(x + 65.dp.toPx(), size.height), 2.dp.toPx()) }
                repeat(6) { i -> val y = size.height * i / 5; drawLine(street, Offset(0f, y), Offset(size.width, y - 40.dp.toPx()), 2.dp.toPx()) }
                drawLine(route, Offset(size.width * .15f, size.height * .15f), Offset(size.width * .8f, size.height * .85f), 5.dp.toPx())
            }
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            buses.forEachIndexed { index, bus ->
                FilledTonalButton(onClick = { onSelect(bus.plate) }, modifier = Modifier.align(if (index % 2 == 0) Alignment.Start else Alignment.End).padding(horizontal = 8.dp).heightIn(min = 48.dp).alpha(if (bus.location == LocationStatus.Stale) .75f else 1f).testTag("mapBus${bus.plate}"),
                    border = if (selected == bus.plate) androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null,
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = bus.safety.color,
                        contentColor = if (bus.safety == BusSafety.Pending) androidx.compose.ui.graphics.Color(0xFF1B1F24) else androidx.compose.ui.graphics.Color.White)) {
                    Symbol(R.drawable.ic_directions_bus); Spacer(Modifier.width(4.dp)); Text(bus.plate)
                }
            }
            }
        }
    }
}

@Composable
private fun BusStatus(bus: FleetBus, capacity: Int) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusChip(stringResource(bus.safety.label), bus.safety.color, if (bus.safety == BusSafety.Normal) R.drawable.ic_check_circle else R.drawable.ic_warning)
        StatusChip(stringResource(bus.location.label), when (bus.location) { LocationStatus.Fresh -> SafeBusColors.Secondary; LocationStatus.Stale -> SafeBusColors.Pending; LocationStatus.Unavailable -> SafeBusColors.Inactive }, R.drawable.ic_location_on)
        if (bus.passengers > capacity) StatusChip(stringResource(R.string.over_capacity), SafeBusColors.High, R.drawable.ic_groups)
    }
    Text(stringResource(if (bus.location == LocationStatus.Stale) R.string.last_location else R.string.updated_at, bus.time), style = MaterialTheme.typography.bodyMedium)
    Text(bus.coordinates, style = MaterialTheme.typography.bodySmall)
    Text(stringResource(R.string.occupancy, bus.passengers, capacity), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("occupancy${bus.plate}"))
    LinearProgressIndicator(progress = { (bus.passengers.toFloat() / capacity).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(),
        color = if (bus.passengers > capacity) SafeBusColors.High else SafeBusColors.Secondary)
}

@Composable
private fun FleetCard(bus: FleetBus, capacity: Int, onDetails: () -> Unit) {
    InfoCard(Modifier.testTag("fleetBus${bus.plate}")) {
        Text(bus.plate + " · " + stringResource(R.string.route_number, bus.route), style = MaterialTheme.typography.titleLarge)
        Text(bus.driver, style = MaterialTheme.typography.bodyMedium)
        BusStatus(bus, capacity)
        SecondaryAction(stringResource(R.string.view_details), onDetails, Modifier.testTag("busDetails${bus.plate}"), R.drawable.ic_directions_bus)
    }
}

@Composable
fun BusDetailsScreen(bus: FleetBus, capacity: Int, onCapacity: (Int) -> Unit) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable(capacity) { mutableStateOf(capacity.toString()) }
    ScreenContent {
        InfoCard {
            Symbol(R.drawable.ic_directions_bus, Modifier.size(40.dp))
            Text(bus.plate, style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.route_number, bus.route), style = MaterialTheme.typography.titleLarge)
            Text(bus.driver); Text(stringResource(R.string.service_company), style = MaterialTheme.typography.bodyMedium)
            HorizontalDivider(); BusStatus(bus, capacity)
        }
        if (bus.location != LocationStatus.Unavailable) RouteIllustration()
        SecondaryAction(stringResource(R.string.edit_capacity), { draft = capacity.toString(); editing = true }, Modifier.testTag("editCapacity"), R.drawable.ic_groups)
        Notice(stringResource(R.string.prototype_notice))
    }
    if (editing) AlertDialog(onDismissRequest = { editing = false }, title = { Text(stringResource(R.string.edit_capacity)) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.capacity_help))
            SafeTextField(draft, { draft = it.filter(Char::isDigit).take(3) }, label = { Text(stringResource(R.string.capacity)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.testTag("capacityInput"))
        }
    }, confirmButton = { DialogAction(onClick = { draft.toIntOrNull()?.takeIf { it > 0 }?.let(onCapacity); editing = false },
        enabled = (draft.toIntOrNull() ?: 0) > 0, modifier = Modifier.testTag("saveCapacity")) { Text(stringResource(R.string.save)) } },
        dismissButton = { DialogAction(onClick = { editing = false }) { Text(stringResource(R.string.cancel)) } })
}
