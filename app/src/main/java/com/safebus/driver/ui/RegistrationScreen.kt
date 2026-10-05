package com.safebus.driver.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.safebus.driver.R
import com.safebus.driver.ui.theme.SafeBusColors

@Composable
fun RegistrationScreen(onCancel: () -> Unit, onComplete: (String, String) -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var dni by rememberSaveable { mutableStateOf("") }
    var photo by rememberSaveable { mutableStateOf(false) }
    var password by rememberSaveable { mutableStateOf("") }
    var accepted by rememberSaveable { mutableStateOf(false) }
    var duplicate by rememberSaveable { mutableStateOf(false) }
    var terms by rememberSaveable { mutableStateOf(false) }
    val back: () -> Unit = { if (step in 1..2) step-- else onCancel() }
    BackHandler { back() }
    RoleScaffold(title = when (step) { 1 -> R.string.face_photo; 2 -> R.string.password_terms; 3 -> R.string.account_created; else -> R.string.create_account }, onBack = back) {
        ScreenContent {
            if (step < 3) {
                Text(stringResource(R.string.step_of, step + 1, 3), style = MaterialTheme.typography.labelLarge)
                LinearProgressIndicator(progress = { (step + 1) / 3f }, modifier = Modifier.fillMaxWidth(), color = SafeBusColors.Primary)
            }
            when (step) {
                0 -> {
                    SafeTextField(dni, { dni = it.filter { c -> c in '0'..'9' }.take(8); duplicate = false },
                        label = { Text(stringResource(R.string.dni)) }, supportingText = { Text(stringResource(R.string.dni_hint) + " · ${dni.length}/8") },
                        leadingIcon = { Symbol(R.drawable.ic_badge) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("registrationDni"))
                    if (duplicate) Notice(stringResource(R.string.dni_duplicate), Modifier.testTag("duplicateDni"))
                    PrimaryAction(stringResource(R.string.continue_action), { if (dni == MockAccounts.PassengerCode) duplicate = true else step = 1 },
                        Modifier.testTag("registrationNext"), enabled = dni.length == 8)
                }
                1 -> {
                    Text(stringResource(R.string.face_help), style = MaterialTheme.typography.bodyLarge)
                    InfoCard {
                        Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                            Symbol(if (photo) R.drawable.ic_check_circle else R.drawable.ic_account_circle, Modifier.size(128.dp), SafeBusColors.Secondary)
                        }
                        if (photo) Text(stringResource(R.string.photo_captured))
                    }
                    PrimaryAction(stringResource(if (photo) R.string.retake_photo else R.string.take_sample_photo), { photo = true }, Modifier.testTag("registrationPhoto"), R.drawable.ic_photo_camera)
                    if (photo) PrimaryAction(stringResource(R.string.continue_action), { step = 2 }, Modifier.testTag("registrationNext"))
                }
                2 -> {
                    SafeTextField(password, { password = it }, label = { Text(stringResource(R.string.password)) },
                        supportingText = { Text(stringResource(R.string.password_minimum)) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("registrationPassword"))
                    InfoCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(accepted, { accepted = it }, Modifier.testTag("acceptTerms"))
                            Text(stringResource(R.string.accept_terms), style = MaterialTheme.typography.bodyMedium)
                        }
                        SecondaryAction(stringResource(R.string.terms), { terms = true })
                    }
                    Spacer(Modifier.weight(1f).heightIn(min = 32.dp))
                    PrimaryAction(stringResource(R.string.create_account), { step = 3 }, Modifier.testTag("createPassenger"), enabled = password.length >= 8 && accepted)
                }
                else -> {
                    Symbol(R.drawable.ic_check_circle, Modifier.size(64.dp), SafeBusColors.Secondary)
                    Text(stringResource(R.string.account_created), style = MaterialTheme.typography.displaySmall, modifier = Modifier.testTag("accountCreated"))
                    Text(stringResource(R.string.account_created_help), style = MaterialTheme.typography.bodyLarge)
                    PrimaryAction(stringResource(R.string.sign_in), { onComplete(dni, password) }, Modifier.testTag("registrationLogin"))
                }
            }
        }
    }
    if (terms) AlertDialog(onDismissRequest = { terms = false }, title = { Text(stringResource(R.string.terms)) }, text = { Text(stringResource(R.string.terms_message)) },
        confirmButton = { DialogAction(onClick = { terms = false }) { Text(stringResource(R.string.ok)) } })
}
