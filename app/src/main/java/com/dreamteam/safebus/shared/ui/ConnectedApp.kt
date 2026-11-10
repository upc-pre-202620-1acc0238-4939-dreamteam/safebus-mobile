package com.dreamteam.safebus.shared.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.dreamteam.safebus.*
import com.dreamteam.safebus.R
import com.dreamteam.safebus.fleet.ui.*
import com.dreamteam.safebus.iam.domain.Role
import com.dreamteam.safebus.iam.ui.AccessScreen
import com.dreamteam.safebus.passenger.ui.JourneyScreen
import com.dreamteam.safebus.safetycase.data.LiveEvent
import com.dreamteam.safebus.safetycase.ui.*
import com.dreamteam.safebus.shared.data.LocationTrackingService
import com.dreamteam.safebus.trip.ui.TripScreen
import kotlinx.coroutines.delay

enum class Destination(val label: Int, val icon: Int) {
    SHIFT(R.string.core_shift, R.drawable.ic_directions_bus), DRIVER_ALERTS(R.string.core_my_alerts, R.drawable.ic_warning),
    JOURNEY(R.string.core_journey, R.drawable.ic_directions_bus), HISTORY(R.string.core_bus_alerts, R.drawable.ic_shield),
    REQUESTS(R.string.core_my_requests, R.drawable.ic_history), FLEET(R.string.core_fleet, R.drawable.ic_map),
    EMERGENCIES(R.string.core_emergencies, R.drawable.ic_warning), REVIEWS(R.string.core_reviews, R.drawable.ic_fact_check),
    ASSIGNMENTS(R.string.core_assignments, R.drawable.ic_assignment), ACCOUNT(R.string.core_account, R.drawable.ic_account_circle)
}
fun destinations(role: Role): List<Destination> = when (role) {
    Role.DRIVER -> listOf(Destination.SHIFT, Destination.DRIVER_ALERTS, Destination.ACCOUNT)
    Role.PASSENGER -> listOf(Destination.JOURNEY, Destination.HISTORY, Destination.REQUESTS, Destination.ACCOUNT)
    Role.SUPERVISOR -> listOf(Destination.FLEET, Destination.EMERGENCIES, Destination.REVIEWS, Destination.ASSIGNMENTS, Destination.ACCOUNT)
}
fun roleLabel(role: Role) = when (role) { Role.DRIVER -> R.string.core_role_driver; Role.PASSENGER -> R.string.core_role_passenger; Role.SUPERVISOR -> R.string.core_role_supervisor }

@Composable fun ConnectedApp(application: SafeBusApplication, lifecycle: Lifecycle) {
    val container by application.container.collectAsState()
    var settings by rememberSaveable { mutableStateOf(false) }
    CoreTheme {
        val current = container
        if (current == null || settings) {
            Scaffold { padding -> Box(Modifier.padding(padding)) { ConnectionScreen(application.endpoint, { if (application.configure(it)) { settings = false; true } else false }, { settings = false }) } }
        } else {
            val auth by current.authModel.state.collectAsState()
            val session = auth.value.session
            if (session == null) Scaffold { padding -> Box(Modifier.padding(padding)) { AccessScreen(auth, current.authModel::signIn, current.authModel::register, { settings = true }) } }
            else key(session.accessToken) {
                val account = remember { current.bind(session) }
                AccountWorkspace(application, account, lifecycle) {
                    application.stopService(Intent(application, LocationTrackingService::class.java))
                    current.authModel.signOut()
                }
            }
        }
    }
}

@Composable private fun ConnectionScreen(initial: String, save: (String) -> Boolean, back: () -> Unit) {
    var value by remember { mutableStateOf(initial) }
    var invalid by remember { mutableStateOf(false) }
    CorePage(R.string.core_server_settings) {
        Text(stringResource(R.string.core_api_help))
        CoreField(R.string.core_api_url, value, { value = it }, "apiEndpoint")
        if (invalid) Text(stringResource(R.string.core_api_invalid), color = MaterialTheme.colorScheme.error)
        CoreButton(R.string.core_api_save, "saveEndpoint") { invalid = !save(value) }
        CoreButton(R.string.core_back, "connectionBack", action = back)
    }
}

@Composable private fun AccountWorkspace(application: SafeBusApplication, account: AccountScope, lifecycle: Lifecycle, signOut: () -> Unit) {
    val tabs = destinations(account.session.role)
    var tab by rememberSaveable { mutableStateOf(tabs.first()) }
    var requestForm by rememberSaveable { mutableStateOf(false) }
    var foreground by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    var connected by remember { mutableStateOf(false) }
    val trip by account.tripModel.state.collectAsState()
    val passenger by account.passengerModel.state.collectAsState()
    val safety by account.safetyModel.state.collectAsState()
    val history by account.historyModel.state.collectAsState()
    val fleet by account.fleetModel.state.collectAsState()
    val panic by account.panicModel.state.collectAsState()
    val sync by account.syncModel.state.collectAsState()
    val tracking by account.tracking.collectAsState()
    val shift by account.shift.collectAsState()
    val journey by account.journey.collectAsState()

    fun startLocation() {
        val permitted = application.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            application.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!permitted) { account.unavailable(); return }
        try { application.startForegroundService(Intent(application, LocationTrackingService::class.java)) }
        catch (_: SecurityException) { account.unavailable() }
        catch (_: IllegalStateException) { account.unavailable() }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { startLocation() }
    val requestPermission = {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= 33) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        permission.launch(permissions.toTypedArray())
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ -> foreground = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(account) {
        account.syncModel.count()
        when (account.session.role) {
            Role.DRIVER -> account.tripModel.refresh()
            Role.PASSENGER -> account.passengerModel.restore()
            Role.SUPERVISOR -> { account.fleetModel.refresh(); account.safetyModel.review() }
        }
    }
    LaunchedEffect(shift?.id, journey?.id, foreground) {
        if (foreground && (shift != null || journey != null)) startLocation()
        if (shift == null && journey == null) application.stopService(Intent(application, LocationTrackingService::class.java))
        if (journey == null && tab == Destination.HISTORY) tab = Destination.JOURNEY
    }
    fun refresh() {
        when (tab) {
            Destination.SHIFT -> account.tripModel.refresh()
            Destination.DRIVER_ALERTS -> account.safetyModel.alerts()
            Destination.REQUESTS -> account.safetyModel.requests()
            Destination.HISTORY -> account.historyModel.history(journey?.busId)
            Destination.FLEET -> account.fleetModel.refresh()
            Destination.EMERGENCIES, Destination.REVIEWS -> account.safetyModel.review()
            else -> Unit
        }
    }
    LaunchedEffect(tab, foreground) { if (foreground) refresh() }
    LaunchedEffect(foreground, account) {
        if (foreground && account.session.role == Role.SUPERVISOR) account.events.observe().collect { event ->
            connected = event !is LiveEvent.Disconnected
            if (connected) { account.safetyModel.review(); account.fleetModel.refresh() }
        }
    }
    LaunchedEffect(foreground, tab) {
        if (foreground) while (true) {
            account.syncModel.retry()
            delay(15_000)
            if (!requestForm && tab != Destination.SHIFT) refresh()
        }
    }
    BackHandler(requestForm || tab != tabs.first()) { if (requestForm) requestForm = false else tab = tabs.first() }
    Scaffold(bottomBar = {
        Column {
            if (account.session.role == Role.DRIVER && shift != null) {
                Column(Modifier.padding(horizontal = dimensionResource(R.dimen.core_space))) {
                    panic.failure?.let { FailureNotice(it) }
                    panic.value?.let { DeliveryNotice(it.delivery) }
                    CoreButton(R.string.core_emergency, "realPanic", !panic.busy, danger = true) { account.panicModel.panic(shift?.id) }
                }
            }
            NavigationBar { tabs.forEach { destination ->
                NavigationBarItem(selected = tab == destination, onClick = { tab = destination; requestForm = false; account.safetyModel.clearEvidence() },
                    enabled = destination != Destination.HISTORY || journey != null, modifier = Modifier.testTag("nav:${destination.name}"),
                    icon = { Icon(painterResource(destination.icon), contentDescription = null) }, label = { Text(stringResource(destination.label), maxLines = 2) })
            } }
        }
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (account.session.role == Role.SUPERVISOR) Text(stringResource(if (connected) R.string.core_live_connected else R.string.core_live_reconnecting), Modifier.padding(horizontal = dimensionResource(R.dimen.core_space)), style = MaterialTheme.typography.bodySmall)
            if ((sync.value ?: 0) > 0) TextButton(account.syncModel::retry, enabled = !sync.busy) { Text(stringResource(R.string.core_sync_count, sync.value ?: 0)) }
            sync.failure?.let { FailureNotice(it) }
            Box(Modifier.weight(1f)) {
                when {
                    requestForm && journey != null -> RequestFormScreen(safety, { message, photo -> account.safetyModel.submit(journey?.id, message, photo) }, { requestForm = false; tab = Destination.REQUESTS })
                    tab == Destination.SHIFT -> TripScreen(trip, tracking, account.tripModel::refresh, account.tripModel::activate, account.tripModel::close, requestPermission)
                    tab == Destination.JOURNEY -> JourneyScreen(passenger, tracking, account.passengerModel::start, account.passengerModel::end, { account.safetyModel.clearReceipt(); requestForm = true }, requestPermission)
                    tab == Destination.FLEET -> FleetScreen(fleet, account.fleetModel::refresh)
                    tab == Destination.ASSIGNMENTS -> ProvisionScreen(fleet, account.fleetModel::save, account.fleetModel::clearReceipt)
                    tab == Destination.ACCOUNT -> AccountScreen(account.session.role, account.session.userId, sync, account.syncModel::retry, signOut)
                    tab == Destination.HISTORY && journey == null -> CorePage(R.string.core_bus_alerts) { Text(stringResource(R.string.core_history_requires_journey)) }
                    else -> {
                        val mode = when (tab) {
                            Destination.DRIVER_ALERTS -> SafetyTab.DRIVER_ALERTS
                            Destination.REQUESTS -> SafetyTab.REQUESTS
                            Destination.HISTORY -> SafetyTab.HISTORY
                            Destination.EMERGENCIES -> SafetyTab.EMERGENCIES
                            else -> SafetyTab.REVIEWS
                        }
                        SafetyListScreen(if (tab == Destination.HISTORY) history else safety, mode, ::refresh,
                            { account.safetyModel.evidence(it, account.session.role == Role.SUPERVISOR) }, account.safetyModel::clearEvidence,
                            account.safetyModel::approve, account.safetyModel::decline, account.safetyModel::attend, account.safetyModel::close)
                    }
                }
            }
        }
    }
}

@Composable private fun AccountScreen(role: Role, user: String, sync: ScreenState<Int?>, retry: () -> Unit, signOut: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    CorePage(R.string.core_account) {
        Text(stringResource(roleLabel(role))); Text(stringResource(R.string.core_user_id, user))
        Text(stringResource(R.string.core_sync_count, sync.value ?: 0))
        CoreButton(R.string.core_sync_retry, "retryPending", !sync.busy, action = retry)
        CoreButton(R.string.core_sign_out, "realSignOut") { confirm = true }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text(stringResource(R.string.core_sign_out)) },
        text = { Text(stringResource(R.string.core_sign_out_detail)) }, confirmButton = { TextButton({ confirm = false; signOut() }, Modifier.testTag("confirmRealSignOut")) { Text(stringResource(R.string.core_confirm)) } },
        dismissButton = { TextButton({ confirm = false }) { Text(stringResource(R.string.core_cancel)) } })
}
