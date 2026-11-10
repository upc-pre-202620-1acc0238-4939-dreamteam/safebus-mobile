package com.dreamteam.safebus.iam.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.dreamteam.safebus.R
import com.dreamteam.safebus.passenger.domain.Registration
import com.dreamteam.safebus.shared.domain.Photo
import com.dreamteam.safebus.shared.ui.*

@Composable fun AccessScreen(state: ScreenState<AuthState>, signIn: (String, String) -> Unit,
    register: (Registration) -> Unit, connection: () -> Unit) {
    var registration by rememberSaveable { mutableStateOf(false) }
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf<Photo?>(null) }
    var consent by remember { mutableStateOf(false) }
    CorePage(if (registration) R.string.core_register else R.string.core_login_title, state.busy, state.failure) {
        Text(stringResource(R.string.app_name), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleLarge)
        if (state.value.registered) Text(stringResource(R.string.core_registered), Modifier.testTag("registeredNotice"))
        Text(stringResource(if (registration) R.string.core_registration_privacy else R.string.core_login_description))
        CoreField(if (registration) R.string.core_dni else R.string.core_login_id, login, { login = it }, "coreLoginId", enabled = !state.busy)
        CoreField(R.string.core_password, password, { password = it }, "corePassword", password = true, enabled = !state.busy)
        if (registration) {
            Text(stringResource(R.string.core_password_rule))
            Text(stringResource(R.string.core_face_photo), style = MaterialTheme.typography.titleMedium)
            PhotoChooser(photo, { photo = it }, !state.busy)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(consent, { consent = it }, enabled = !state.busy, modifier = Modifier.testTag("acceptTerms"))
                Text(stringResource(R.string.core_terms), Modifier.weight(1f))
            }
            CoreButton(R.string.core_register, "coreRegister", !state.busy && login.matches(Regex("[0-9]{8}")) && password.length >= 8 && photo != null && consent) {
                photo?.let { register(Registration(login, password, consent, it)) }
                password = ""; photo = null
            }
            TextButton({ registration = false; password = ""; photo = null }, enabled = !state.busy) { Text(stringResource(R.string.core_back)) }
        } else {
            CoreButton(R.string.core_login, "coreSignIn", !state.busy && login.isNotBlank() && password.isNotBlank()) {
                signIn(login, password); password = ""
            }
            TextButton({ registration = true; login = ""; password = "" }, enabled = !state.busy, modifier = Modifier.testTag("openRegistration")) { Text(stringResource(R.string.core_register)) }
            TextButton(connection, enabled = !state.busy) { Text(stringResource(R.string.core_server_settings)) }
        }
    }
}
