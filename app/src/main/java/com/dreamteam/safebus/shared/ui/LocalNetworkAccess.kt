package com.dreamteam.safebus.shared.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.dreamteam.safebus.R

@Composable fun LocalNetworkAccess(blocked: Boolean, denied: Boolean, request: () -> Unit,
    settings: () -> Unit, content: @Composable () -> Unit) {
    if (!blocked) content()
    else Scaffold { padding -> Box(Modifier.padding(padding)) { CorePage(R.string.core_local_network_title) {
        Text(stringResource(R.string.core_local_network_help))
        if (denied) Text(stringResource(R.string.core_local_network_denied), Modifier.testTag("localNetworkDenied"))
        CoreButton(R.string.core_local_network_allow, "allowLocalNetwork", action = request)
        CoreButton(R.string.core_server_settings, "localNetworkSettings", action = settings)
    } } }
}
