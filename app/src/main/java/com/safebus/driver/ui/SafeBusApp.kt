package com.safebus.driver.ui

import android.content.res.Configuration
import android.app.Activity
import java.util.Locale
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.view.WindowCompat
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.safebus.driver.R
import com.safebus.driver.ui.theme.SafeBusColors
import com.safebus.driver.ui.theme.SafeBusTheme

@Composable
fun SafeBusApp() {
    var language by rememberSaveable { mutableStateOf("en") }
    val context = LocalContext.current
    val systemConfiguration = LocalConfiguration.current
    val configuration = remember(language, systemConfiguration) {
        Configuration(systemConfiguration).apply { setLocale(Locale.forLanguageTag(language)) }
    }
    val localizedContext = remember(context, configuration) { context.createConfigurationContext(configuration) }
    CompositionLocalProvider(LocalContext provides localizedContext, LocalConfiguration provides configuration) {
        SafeBusTheme { SafeBusPrototype(language, onLanguage = { language = it }) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SafeBusPrototype(language: String, onLanguage: (String) -> Unit) {
    var page by rememberSaveable { mutableStateOf(Page.Welcome) }
    var role by rememberSaveable { mutableStateOf<UserRole?>(null) }
    var registeredDni by rememberSaveable { mutableStateOf("") }
    var registeredPassword by rememberSaveable { mutableStateOf("") }
    var shift by rememberSaveable { mutableStateOf(ShiftState.Assigned) }
    var emergency by rememberSaveable { mutableStateOf<EmergencyState?>(null) }
    var connected by rememberSaveable { mutableStateOf(true) }
    var emptyHistory by rememberSaveable { mutableStateOf(false) }
    var showCloseShift by rememberSaveable { mutableStateOf(false) }
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val view = LocalView.current
    val darkTheme = isSystemInDarkTheme()
    SideEffect {
        (view.context as? Activity)?.let { activity ->
            WindowCompat.getInsetsController(activity.window, view).isAppearanceLightStatusBars = page == Page.Welcome && !darkTheme
        }
    }
    val signedIn = role == UserRole.Driver
    val selectedSection = when (page) {
        Page.Alerts, Page.Details, Page.HistoryDetails -> Page.Alerts
        Page.Account -> Page.Account
        else -> Page.Shift
    }
    fun goBack() {
        page = when (page) {
            Page.Login -> Page.Welcome
            Page.Scanner, Page.Validated, Page.Emergency -> Page.Shift
            Page.Details, Page.HistoryDetails -> Page.Alerts
            else -> Page.Shift
        }
    }
    fun reset() {
        role = null
        emergency = null
        shift = ShiftState.Assigned
        connected = true
        emptyHistory = false
        page = Page.Welcome
    }
    if (role == UserRole.Passenger) {
        PassengerApp(language, onLanguage, onSignOut = { reset() })
        return
    }
    if (role == UserRole.Supervisor) {
        SupervisorApp(language, onLanguage, onSignOut = { reset() })
        return
    }
    if (page == Page.Register) {
        RegistrationScreen(onCancel = { page = Page.Welcome }, onComplete = { dni, password ->
            registeredDni = dni
            registeredPassword = password
            page = Page.Login
        })
        return
    }
    BackHandler(enabled = page != Page.Welcome && page != Page.Shift) { goBack() }
    val title = when (page) {
        Page.Welcome -> R.string.welcome
        Page.Login -> R.string.sign_in
        Page.Register -> R.string.create_account
        Page.Shift -> R.string.my_shift
        Page.Scanner -> R.string.scan_driver_qr
        Page.Validated -> R.string.shift_validated
        Page.Alerts -> R.string.my_alerts
        Page.Account -> R.string.account
        Page.Emergency -> if (emergency == EmergencyState.Pending) R.string.status_pending else R.string.emergency_sent
        Page.Details, Page.HistoryDetails -> R.string.emergency_details
    }
    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (page != Page.Welcome) TopAppBar(
                expandedHeight = (64 * fontScale).dp,
                title = { Text(stringResource(title), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    if (page != Page.Shift && page != Page.Alerts && page != Page.Account) {
                        IconButton(onClick = { goBack() }) { Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back)) }
                    }
                },
                actions = {
                    if (signedIn && page != Page.Account) IconButton(onClick = { page = Page.Account }) {
                        Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_account_circle), stringResource(R.string.account))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SafeBusColors.Primary,
                    titleContentColor = Color.White, navigationIconContentColor = Color.White, actionIconContentColor = Color.White),
            )
        },
        bottomBar = {
            if (signedIn && page != Page.Scanner && page != Page.Validated) Column {
                if (shift == ShiftState.Active) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Button(
                            onClick = {
                                emergency = if (connected) EmergencyState.Active else EmergencyState.Pending
                                page = Page.Emergency
                            },
                            modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().heightIn(min = 96.dp).testTag("emergencyButton"),
                            colors = ButtonDefaults.buttonColors(containerColor = SafeBusColors.Critical, contentColor = Color.White),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        ) { Text(stringResource(R.string.emergency), style = MaterialTheme.typography.headlineSmall) }
                    }
                }
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, modifier = Modifier.heightIn(min = (80 * fontScale).dp)) {
                    listOf(Triple(Page.Shift, R.string.shift, R.drawable.ic_badge),
                        Triple(Page.Alerts, R.string.my_alerts, R.drawable.ic_history),
                        Triple(Page.Account, R.string.account, R.drawable.ic_account_circle)).forEach { (destination, label, icon) ->
                        NavigationBarItem(selected = selectedSection == destination, onClick = { page = destination },
                            icon = { Symbol(icon) }, label = { Text(stringResource(label), style = MaterialTheme.typography.labelLarge) },
                            modifier = Modifier.testTag("nav${destination.name}"),
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.White,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = SafeBusColors.Secondary))
                    }
                }
            }
        },
    ) { insets ->
        Box(Modifier.padding(insets).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.widthIn(max = 600.dp).fillMaxSize()) {
                when (page) {
                    Page.Welcome -> WelcomeScreen(language, onLanguage, onLogin = { page = Page.Login }, onRegister = { page = Page.Register })
                    Page.Login -> LoginScreen(onSuccess = { role = it; page = Page.Shift }, onRegister = { page = Page.Register }, registeredDni, registeredPassword)
                    Page.Register -> Unit
                    Page.Shift -> ShiftScreen(shift, onScan = { page = Page.Scanner },
                        onRefresh = { shift = ShiftState.Assigned }, onClose = { showCloseShift = true })
                    Page.Scanner -> ScannerScreen(onCancel = { page = Page.Shift }, onValid = { shift = ShiftState.Active; page = Page.Validated })
                    Page.Validated -> ValidatedScreen(onContinue = { page = Page.Shift })
                    Page.Alerts -> AlertsScreen(emergency, emptyHistory, onCurrent = { page = Page.Details }, onHistory = { page = Page.HistoryDetails })
                    Page.Emergency -> EmergencyScreen(emergency ?: EmergencyState.Active, onDetails = { page = Page.Details })
                    Page.Details -> EmergencyDetailsScreen(emergency ?: EmergencyState.Active)
                    Page.HistoryDetails -> EmergencyDetailsScreen(EmergencyState.Closed, historical = true)
                    Page.Account -> AccountScreen(language, onLanguage, connected,
                        onConnection = { connected = it; if (it && emergency == EmergencyState.Pending) emergency = EmergencyState.Active },
                        shift, onAssignment = { shift = if (it) ShiftState.Assigned else ShiftState.None },
                        emptyHistory, onEmptyHistory = { emptyHistory = it }, emergency,
                        onEmergencyState = { emergency = it }, onSignOut = { reset() }, onReset = { reset() })
                }
            }
        }
    }
    if (showCloseShift) AlertDialog(onDismissRequest = { showCloseShift = false },
        title = { Text(stringResource(R.string.close_shift)) }, text = { Text(stringResource(R.string.close_shift_message)) },
        confirmButton = { DialogAction(onClick = { shift = ShiftState.Assigned; page = Page.Shift; showCloseShift = false }, modifier = Modifier.testTag("confirmCloseShift")) { Text(stringResource(R.string.close_shift)) } },
        dismissButton = { DialogAction(onClick = { showCloseShift = false }, modifier = Modifier.testTag("cancelCloseShift")) { Text(stringResource(R.string.cancel)) } })
}

