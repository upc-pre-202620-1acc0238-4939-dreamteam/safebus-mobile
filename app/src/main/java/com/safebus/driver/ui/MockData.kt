package com.safebus.driver.ui

import com.safebus.driver.R
import com.safebus.driver.ui.theme.SafeBusColors

enum class Page { Welcome, Login, Register, Shift, Scanner, Validated, Alerts, Account, Emergency, Details, HistoryDetails }
enum class UserRole { Driver, Passenger, Supervisor }

/** Local fixtures only. Credentials are documented in README, never advertised in the UI. */
object MockAccounts {
    const val PassengerCode = "76543210"
    fun roleFor(code: String, password: String): UserRole? {
        if (password != MockDriver.Password) return null
        return when (code.trim()) {
            MockDriver.Code -> UserRole.Driver
            PassengerCode -> UserRole.Passenger
            "supervisor.demo" -> UserRole.Supervisor
            else -> null
        }
    }
}
enum class ShiftState { Assigned, Active, None }
enum class EmergencyState(val label: Int, val message: Int, val icon: Int) {
    Pending(R.string.status_pending, R.string.pending_message, R.drawable.ic_cloud_off),
    Active(R.string.status_active, R.string.sent_message, R.drawable.ic_warning),
    InProgress(R.string.status_progress, R.string.progress_message, R.drawable.ic_schedule),
    Closed(R.string.status_closed, R.string.closed_message, R.drawable.ic_check_circle);

    val color get() = when (this) {
        Pending -> SafeBusColors.Information
        Active, InProgress -> SafeBusColors.Critical
        Closed -> SafeBusColors.Closed
    }
}

object MockDriver {
    const val Code = "conductor.demo"
    const val Password = "SafeBus123"
}
