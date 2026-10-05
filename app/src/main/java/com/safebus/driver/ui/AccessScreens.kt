package com.safebus.driver.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.safebus.driver.R
import com.safebus.driver.ui.theme.SafeBusColors

@Composable
fun LanguageSelector(language: String, onLanguage: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = language == "en", onClick = { onLanguage("en") }, label = { Text("English") },
            modifier = Modifier.heightIn(min = 48.dp).testTag("languageEn"))
        FilterChip(selected = language == "es", onClick = { onLanguage("es") }, label = { Text("Español") },
            modifier = Modifier.heightIn(min = 48.dp).testTag("languageEs"))
    }
}

@Composable
fun WelcomeScreen(language: String, onLanguage: (String) -> Unit, onLogin: () -> Unit, onRegister: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) { LanguageSelector(language, onLanguage) }
        Spacer(Modifier.weight(1f).heightIn(min = 80.dp))
        Image(painterResource(if (androidx.compose.foundation.isSystemInDarkTheme()) R.drawable.ic_safebus_dark else R.drawable.ic_safebus), contentDescription = null, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(8.dp))
        Text(buildAnnotatedString {
            withStyle(SpanStyle(color = if (androidx.compose.foundation.isSystemInDarkTheme()) Color.White else SafeBusColors.Primary)) { append("Safe") }
            withStyle(SpanStyle(color = if (androidx.compose.foundation.isSystemInDarkTheme()) Color.White else SafeBusColors.Secondary)) { append("Bus") }
        }, style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.weight(1f).heightIn(min = 96.dp))
        PrimaryAction(stringResource(R.string.sign_in), onLogin, Modifier.testTag("welcomeLogin"))
        Spacer(Modifier.height(8.dp))
        SecondaryAction(stringResource(R.string.create_account), onRegister, Modifier.testTag("welcomeRegister"))
        Spacer(Modifier.height(48.dp))
    }
}

@Composable
fun LoginScreen(onSuccess: (UserRole) -> Unit, onRegister: () -> Unit, registeredDni: String = "", registeredPassword: String = "") {
    var code by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var visible by rememberSaveable { mutableStateOf(false) }
    var invalid by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SafeTextField(value = code, onValueChange = { code = it; invalid = false },
            label = { Text(stringResource(R.string.identifier)) }, leadingIcon = { Symbol(R.drawable.ic_badge) },
            singleLine = true, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("loginCode"),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
        SafeTextField(value = password, onValueChange = { password = it; invalid = false },
            label = { Text(stringResource(R.string.password)) }, placeholder = { Text(stringResource(R.string.password_hint)) }, leadingIcon = { Symbol(R.drawable.ic_lock) },
            trailingIcon = { IconButton(onClick = { visible = !visible }) {
                Icon(painterResource(if (visible) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
                    stringResource(if (visible) R.string.hide_password else R.string.show_password))
            } }, singleLine = true,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("loginPassword"), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
        if (invalid) Notice(stringResource(R.string.invalid_credentials), Modifier.testTag("loginError"), R.drawable.ic_info)
        PrimaryAction(stringResource(R.string.sign_in), {
            val role = MockAccounts.roleFor(code, password)
                ?: if (registeredDni.isNotBlank() && code.trim() == registeredDni && password == registeredPassword) UserRole.Passenger else null
            if (role != null) onSuccess(role) else invalid = true
        }, Modifier.testTag("submitLogin"), R.drawable.ic_arrow_forward)
        SecondaryAction(stringResource(R.string.create_account), onRegister, Modifier.testTag("loginRegister"))
        Notice(stringResource(R.string.company_access), icon = R.drawable.ic_shield)
    }
}
