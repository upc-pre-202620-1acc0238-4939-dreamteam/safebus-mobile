package com.dreamteam.safebus.iam.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.*
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.dreamteam.safebus.R
import com.dreamteam.safebus.passenger.domain.Registration
import com.dreamteam.safebus.shared.domain.Photo
import com.dreamteam.safebus.shared.ui.*

private enum class AccessPage { WELCOME, LOGIN, REGISTER }

@Composable fun AccessScreen(state: ScreenState<AuthState>, signIn: (String, String) -> Unit,
    register: (Registration) -> Unit, connection: () -> Unit, contact: () -> Unit = {},
    language: String = "en", onLanguage: (String) -> Unit = {}) {
    var page by rememberSaveable { mutableStateOf(AccessPage.WELCOME) }
    var step by rememberSaveable { mutableIntStateOf(0) }
    var login by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf<Photo?>(null) }
    var consent by rememberSaveable { mutableStateOf(false) }
    fun back() {
        if (page == AccessPage.REGISTER && step > 0) step--
        else { page = AccessPage.WELCOME; password = ""; photo = null }
    }
    fun registration() { page = AccessPage.REGISTER; step = 0; login = ""; password = ""; photo = null; consent = false }
    BackHandler(page != AccessPage.WELCOME && !state.busy, ::back)
    LaunchedEffect(state.value.registered) { if (state.value.registered) { page = AccessPage.LOGIN; password = ""; photo = null } }
    if (page == AccessPage.WELCOME) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val breathingRoom = maxHeight / 6
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(dimensionResource(R.dimen.core_space)),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.core_small))) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { LanguageOptions(language, onLanguage) }
                Spacer(Modifier.height(breathingRoom))
                Image(painterResource(if (isSystemInDarkTheme()) R.drawable.ic_safebus_dark else R.drawable.ic_safebus), null, Modifier.size(dimensionResource(R.dimen.core_thumbnail)))
                val brandColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
                val routeColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.secondary
                Text(buildAnnotatedString {
                    withStyle(SpanStyle(color = brandColor)) { append(stringResource(R.string.ui_brand_safe)) }
                    withStyle(SpanStyle(color = routeColor)) { append(stringResource(R.string.ui_brand_bus)) }
                }, style = MaterialTheme.typography.displaySmall)
                Text(stringResource(R.string.ui_slogan), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(breathingRoom))
                CoreButton(R.string.core_login, "welcomeLogin") { page = AccessPage.LOGIN }
                CoreButton(R.string.core_register, "welcomeRegister", secondary = true) { registration() }
                TextButton(connection, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) { Text(stringResource(R.string.core_server_settings)) }
                TextButton(contact, Modifier.testTag("openContact"), colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) { Text(stringResource(R.string.extra_contact)) }
            }
        }
    } else {
        CorePage(if (page == AccessPage.LOGIN) R.string.core_login else if (step == 1) R.string.core_face_photo else if (step == 2) R.string.ui_registration_password else R.string.core_register,
            state.busy, state.failure, back = { if (!state.busy) back() }) {
            if (state.value.registered) CoreNotice(stringResource(R.string.core_registered), modifier = Modifier.testTag("registeredNotice"))
            if (page == AccessPage.LOGIN) {
                CoreField(R.string.core_login_id, login, { login = it }, "coreLoginId", enabled = !state.busy)
                CoreField(R.string.core_password, password, { password = it }, "corePassword", password = true, enabled = !state.busy)
                CoreButton(R.string.core_login, "coreSignIn", !state.busy && login.isNotBlank() && password.isNotBlank(), icon = R.drawable.ic_arrow_forward) {
                    signIn(login, password); password = ""
                }
                CoreButton(R.string.core_register, "openRegistration", !state.busy, secondary = true) { registration() }
                CoreNotice(stringResource(R.string.company_access), R.drawable.ic_shield)
                TextButton(connection, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) { Text(stringResource(R.string.core_server_settings)) }
            } else {
                StepIndicator(step, 3)
                when (step) {
                    0 -> {
                        CoreField(R.string.core_dni, login, { if (it.length <= 8) login = it }, "coreLoginId", enabled = !state.busy)
                        Text(stringResource(R.string.ui_dni_count, login.length))
                        CoreButton(R.string.ui_continue, "registrationNext", login.matches(Regex("[0-9]{8}"))) { step = 1 }
                    }
                    1 -> {
                        CoreNotice(stringResource(R.string.ui_registration_photo), R.drawable.ic_shield)
                        CoreCard { if (photo == null) CoreEmpty(R.string.core_face_photo, R.drawable.ic_account_circle) else PrivatePhoto(photo!!) }
                        PhotoChooser(photo, { photo = it }, !state.busy)
                        CoreButton(R.string.ui_continue, "registrationNext", photo != null) { step = 2 }
                    }
                    else -> {
                        CoreField(R.string.core_password, password, { password = it }, "corePassword", password = true, enabled = !state.busy)
                        Text(stringResource(R.string.core_password_rule))
                        CoreNotice(stringResource(R.string.core_registration_privacy), R.drawable.ic_lock)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(consent, { consent = it }, enabled = !state.busy, modifier = Modifier.testTag("acceptTerms"))
                            Text(stringResource(R.string.core_terms), Modifier.weight(1f))
                        }
                        CoreButton(R.string.core_register, "coreRegister", !state.busy && login.matches(Regex("[0-9]{8}")) && password.length >= 8 && photo != null && consent) {
                            photo?.let { register(Registration(login, password, consent, it)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable fun LanguageOptions(language: String, onLanguage: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.core_small))) {
        listOf("en" to R.string.ui_language_en, "es" to R.string.ui_language_es).forEach { (code, label) ->
            FilterChip(language == code, { onLanguage(code) }, label = { Text(stringResource(label)) },
                modifier = Modifier.heightIn(min = dimensionResource(R.dimen.core_secondary_height)).testTag("language:$code"),
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer, selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer))
        }
    }
}

@Composable fun StepIndicator(step: Int, count: Int) {
    Text(stringResource(R.string.ui_step, step + 1, count), Modifier.testTag("stepIndicator"), style = MaterialTheme.typography.labelLarge)
    LinearProgressIndicator(progress = { (step + 1).toFloat() / count }, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.secondary,
        trackColor = MaterialTheme.colorScheme.primaryContainer)
}
