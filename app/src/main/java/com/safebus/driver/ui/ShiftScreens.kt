package com.safebus.driver.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.safebus.driver.R
import com.safebus.driver.ui.theme.SafeBusColors

@Composable
fun ShiftScreen(state: ShiftState, onScan: () -> Unit, onRefresh: () -> Unit, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        when (state) {
            ShiftState.Assigned -> {
                Notice(stringResource(R.string.driver_role), icon = R.drawable.ic_badge)
                StatusChip(stringResource(R.string.assigned), SafeBusColors.Secondary, R.drawable.ic_check_circle)
                InfoCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Symbol(R.drawable.ic_directions_bus)
                        Column {
                            Text(stringResource(R.string.assigned_unit), style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(R.string.unit_route), style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    Text(stringResource(R.string.fleet_number), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    Endpoint(R.string.origin, "Terminal Naranjal", R.string.origin_detail)
                    Endpoint(R.string.destination, "Av. Javier Prado", R.string.destination_detail)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    Text(stringResource(R.string.schedule), style = MaterialTheme.typography.bodyMedium)
                    Text("06:00 - 14:00", style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.duration), style = MaterialTheme.typography.bodyMedium)
                }
                InfoCard {
                    Text(stringResource(R.string.route_map), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.route_distance), style = MaterialTheme.typography.bodyMedium)
                    RouteIllustration()
                    Text(stringResource(R.string.schematic_map), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Notice(stringResource(R.string.qr_notice))
                PrimaryAction(stringResource(R.string.scan_driver_qr), onScan, Modifier.testTag("scanQr"), R.drawable.ic_qr_code_scanner)
            }
            ShiftState.Active -> {
                StatusChip(stringResource(R.string.active_shift), SafeBusColors.Secondary, R.drawable.ic_schedule, Modifier.testTag("activeShift"))
                ServiceCard()
                Text(stringResource(R.string.location_status), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SecondaryAction(stringResource(R.string.close_shift), onClose, Modifier.testTag("closeShift"))
            }
            ShiftState.None -> NoShiftScreen(onRefresh)
        }
    }
}

@Composable
private fun Endpoint(label: Int, place: String, detail: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Symbol(R.drawable.ic_route, tint = SafeBusColors.Secondary)
        Column {
            Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
            Text(place, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(detail), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NoShiftScreen(onRefresh: () -> Unit) {
    var contact by rememberSaveable { mutableStateOf(false) }
    Notice(stringResource(R.string.operator) + "\n" + stringResource(R.string.company), icon = R.drawable.ic_badge)
    StatusChip(stringResource(R.string.inactive), SafeBusColors.Inactive, R.drawable.ic_schedule)
    InfoCard {
        Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
            Symbol(R.drawable.ic_calendar_month, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurface)
        }
        Text(stringResource(R.string.no_shift), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("noShift"))
        Text(stringResource(R.string.no_shift_message), style = MaterialTheme.typography.bodyLarge)
        Notice(stringResource(R.string.operations) + "\n" + stringResource(R.string.operations_message))
        PrimaryAction(stringResource(R.string.refresh_availability), onRefresh, Modifier.testTag("refreshAssignment"), R.drawable.ic_refresh)
    }
    InfoCard {
        Text(stringResource(R.string.traffic_base), style = MaterialTheme.typography.titleMedium)
        SecondaryAction(stringResource(R.string.call), { contact = true }, icon = R.drawable.ic_phone)
    }
    if (contact) AlertDialog(onDismissRequest = { contact = false }, title = { Text(stringResource(R.string.mock_contact)) },
        text = { Text(stringResource(R.string.contact_message)) },
        confirmButton = { DialogAction(onClick = { contact = false }) { Text(stringResource(R.string.ok)) } })
}

@Composable
fun ScannerScreen(onCancel: () -> Unit, onValid: () -> Unit, instructions: Int = R.string.qr_instructions, invalidText: Int = R.string.invalid_qr) {
    var invalid by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Color(0xFF0F1720)).verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.height(24.dp))
        Box(Modifier.fillMaxWidth().aspectRatio(1f).testTag("scannerFrame"), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().padding(16.dp)) {
                val line = 24.dp.toPx()
                val width = 3.dp.toPx()
                val color = Color(0xFF80CBC4)
                listOf(Offset.Zero, Offset(size.width, 0f), Offset(0f, size.height), Offset(size.width, size.height)).forEach { p ->
                    drawLine(color, p, Offset(p.x + if (p.x == 0f) line else -line, p.y), width)
                    drawLine(color, p, Offset(p.x, p.y + if (p.y == 0f) line else -line), width)
                }
            }
            Symbol(R.drawable.ic_qr_code_scanner, Modifier.size(48.dp), Color(0xFFA3ADB9))
        }
        Text(stringResource(instructions), color = Color(0xFFE6EAF0), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Text(stringResource(R.string.qr_simulation), color = Color(0xFFA3ADB9), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        if (invalid) Notice(stringResource(invalidText), Modifier.testTag("qrError"))
        PrimaryAction(stringResource(R.string.simulate_valid_qr), onValid, Modifier.testTag("validQr"), R.drawable.ic_check_circle)
        OutlinedButton(onClick = { invalid = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("invalidQr"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)) {
            Text(stringResource(R.string.simulate_invalid_qr))
        }
        TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = 48.dp), colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
            Text(stringResource(R.string.cancel))
        }
    }
}

@Composable
fun ValidatedScreen(onContinue: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Symbol(R.drawable.ic_check_circle, Modifier.size(64.dp), SafeBusColors.Secondary)
        Text(stringResource(R.string.operation_enabled), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(stringResource(R.string.shift_validated), style = MaterialTheme.typography.displaySmall, modifier = Modifier.testTag("validatedShift"))
        Text(stringResource(R.string.validation_subtitle), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Notice(stringResource(R.string.validated_by_qr) + " · 05:45\n" + stringResource(R.string.credential_active), icon = R.drawable.ic_qr_code_scanner)
        InfoCard {
            Text(stringResource(R.string.service_details), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.company), style = MaterialTheme.typography.bodyMedium)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            if (LocalDensity.current.fontScale < 1.5f) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { PlateDetails() }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { RouteDetails() }
                }
            } else {
                PlateDetails()
                RouteDetails()
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Text(stringResource(R.string.scheduled_hours), style = MaterialTheme.typography.bodyMedium)
            Text("06:00 - 14:00", style = MaterialTheme.typography.titleLarge)
        }
        Text(stringResource(R.string.protocols), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.weight(1f).heightIn(min = 24.dp))
        PrimaryAction(stringResource(R.string.go_to_shift), onContinue, Modifier.testTag("goToShift"), R.drawable.ic_arrow_forward)
    }
}

@Composable
private fun PlateDetails() {
    Text(stringResource(R.string.plate), style = MaterialTheme.typography.bodyMedium)
    Text("A1B-702", style = MaterialTheme.typography.titleLarge)
}

@Composable
private fun RouteDetails() {
    Text(stringResource(R.string.assigned_route), style = MaterialTheme.typography.bodyMedium)
    Text(stringResource(R.string.route_name), style = MaterialTheme.typography.titleLarge)
    Text(stringResource(R.string.route_description), style = MaterialTheme.typography.bodyMedium)
}
