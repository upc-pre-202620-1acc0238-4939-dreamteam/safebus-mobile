package com.safebus.driver.ui

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.safebus.driver.R
import com.safebus.driver.ui.theme.SafeBusColors

data class RoleDestination(val id: String, val label: Int, val icon: Int, val enabled: Boolean = true, val badge: Int = 0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleScaffold(title: Int, selected: String = "", destinations: List<RoleDestination> = emptyList(),
    onNavigate: (String) -> Unit = {}, onBack: (() -> Unit)? = null, onAccount: (() -> Unit)? = null,
    content: @Composable () -> Unit) {
    val scale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val view = LocalView.current
    SideEffect {
        (view.context as? Activity)?.let { WindowCompat.getInsetsController(it.window, view).isAppearanceLightStatusBars = false }
    }
    Scaffold(modifier = Modifier.fillMaxSize().imePadding(), containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(expandedHeight = (64 * scale).dp,
            title = { Text(stringResource(title), style = MaterialTheme.typography.titleLarge) },
            navigationIcon = { if (onBack != null) IconButton(onClick = onBack, modifier = Modifier.testTag("screenBack")) {
                Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back))
            } },
            actions = { if (onAccount != null) IconButton(onClick = onAccount) {
                Icon(painterResource(R.drawable.ic_account_circle), stringResource(R.string.account))
            } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = SafeBusColors.Primary, titleContentColor = Color.White,
                navigationIconContentColor = Color.White, actionIconContentColor = Color.White)) },
        bottomBar = { if (destinations.isNotEmpty()) NavigationBar(containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.heightIn(min = (80 * scale).dp)) {
            destinations.forEach { destination ->
                val label = stringResource(destination.label)
                // Give long destination names room without reducing the report's 14 sp labels.
                Row(Modifier.weight(if (destinations.size == 5) label.length.coerceAtLeast(6).toFloat() else 1f)) {
                NavigationBarItem(selected = selected == destination.id, enabled = destination.enabled,
                    onClick = { onNavigate(destination.id) }, modifier = Modifier.testTag("nav${destination.id}"),
                    icon = { BadgedBox(badge = { if (destination.badge > 0) Badge(containerColor = if (destination.id == "Reviews") SafeBusColors.Pending else SafeBusColors.Critical,
                        contentColor = if (destination.id == "Reviews") Color(0xFF1B1F24) else Color.White) { Text(destination.badge.toString()) } }) { Symbol(destination.icon) } },
                    label = { Text(label, style = MaterialTheme.typography.labelLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = SafeBusColors.Secondary, selectedIconColor = Color.White,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface))
                }
            }
        } },
    ) { insets -> Box(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = 600.dp).fillMaxSize()) { content() }
    } }
}

@Composable
fun ScreenContent(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
}

@Composable
fun ChoiceChips(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit, tagPrefix: String = "choice") {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (id, label) -> FilterChip(selected = id == selected, onClick = { onSelect(id) },
            label = { Text(label) }, modifier = Modifier.heightIn(min = 48.dp).testTag("$tagPrefix$id")) }
    }
}

@Composable
fun EvidencePreview(compact: Boolean = false) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Symbol(R.drawable.ic_photo_camera, Modifier.size(if (compact) 32.dp else 64.dp))
            Text(stringResource(R.string.sample_evidence), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun SegmentAccountScreen(role: UserRole, language: String, onLanguage: (String) -> Unit, onSignOut: () -> Unit,
    scenarios: @Composable ColumnScope.() -> Unit) {
    var terms by rememberSaveable { mutableStateOf(false) }
    ScreenContent {
        InfoCard {
            Symbol(R.drawable.ic_account_circle, Modifier.size(32.dp))
            Text(stringResource(if (role == UserRole.Passenger) R.string.passenger_name else R.string.supervisor_name), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(if (role == UserRole.Passenger) R.string.passenger_role else R.string.supervisor_role), style = MaterialTheme.typography.bodyLarge)
            if (role == UserRole.Supervisor) Text(stringResource(R.string.service_company), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.profile_mock), style = MaterialTheme.typography.bodySmall)
        }
        Text(stringResource(R.string.language), style = MaterialTheme.typography.titleLarge)
        LanguageSelector(language, onLanguage)
        SecondaryAction(stringResource(R.string.terms), { terms = true })
        SecondaryAction(stringResource(R.string.sign_out), onSignOut, Modifier.testTag("signOut"), R.drawable.ic_logout)
        HorizontalDivider()
        Text(stringResource(R.string.demo_scenarios), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.demo_scenarios_help), style = MaterialTheme.typography.bodyMedium)
        scenarios()
        Notice(stringResource(R.string.prototype_notice))
    }
    if (terms) AlertDialog(onDismissRequest = { terms = false }, title = { Text(stringResource(R.string.terms)) },
        text = { Text(stringResource(R.string.terms_message)) }, confirmButton = { DialogAction(onClick = { terms = false }) { Text(stringResource(R.string.ok)) } })
}
