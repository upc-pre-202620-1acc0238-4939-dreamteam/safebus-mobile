package com.safebus.driver.ui

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.safebus.driver.ui.theme.SafeBusTheme

@Preview(name = "Assigned shift · Light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AssignedShiftPreview() { SafeBusTheme(false) { Surface { ShiftScreen(ShiftState.Assigned, {}, {}, {}) } } }

@Preview(name = "Active shift · Dark", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ActiveShiftPreview() { SafeBusTheme(true) { Surface { ShiftScreen(ShiftState.Active, {}, {}, {}) } } }

@Preview(name = "Emergency · Dark", showBackground = true, widthDp = 390, heightDp = 844)
@Preview(name = "Emergency · Large text", showBackground = true, widthDp = 390, heightDp = 844, fontScale = 2f)
@Composable
private fun EmergencyPreview() { SafeBusTheme(true) { Surface { EmergencyScreen(EmergencyState.Active, {}) } } }

@Preview(name = "Pending transmission · Light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PendingPreview() { SafeBusTheme(false) { Surface { EmergencyScreen(EmergencyState.Pending, {}) } } }

@Preview(name = "Driver app", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun AppPreview() { SafeBusApp() }

@Preview(name = "Passenger · Light", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PassengerPreview() { SafeBusTheme(false) { PassengerApp("en", {}, {}) } }

@Preview(name = "Supervisor · Dark", showBackground = true, widthDp = 390, heightDp = 844)
@Preview(name = "Supervisor · Large text", showBackground = true, widthDp = 390, heightDp = 844, fontScale = 2f)
@Composable
private fun SupervisorPreview() { SafeBusTheme(true) { SupervisorApp("en", {}, {}) } }
