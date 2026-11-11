package com.dreamteam.safebus.shared.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.dreamteam.safebus.R
import com.dreamteam.safebus.shared.domain.ContactSubmission
import java.util.UUID

@Composable fun ContactScreen(state: ScreenState<ServiceState>, submit: (ContactSubmission) -> Unit, clear: () -> Unit, health: () -> Unit, back: () -> Unit) {
    val pending = state.value.contact.pending
    val receipt = state.value.contact.receipt
    var company by remember(pending) { mutableStateOf(pending?.company.orEmpty()) }
    var name by remember(pending) { mutableStateOf(pending?.name.orEmpty()) }
    var email by remember(pending) { mutableStateOf(pending?.email.orEmpty()) }
    var consent by remember(pending) { mutableStateOf(pending?.consent ?: false) }
    val id = remember(pending, receipt) { pending?.id ?: UUID.randomUUID().toString() }
    CorePage(R.string.extra_contact, state.busy, state.failure, back = back) {
        CoreButton(R.string.core_back, "contactBack", secondary = true, action = back)
        CoreNotice(stringResource(R.string.extra_contact_help), icon = R.drawable.ic_phone)
        CoreButton(R.string.extra_health, "checkService", !state.busy, secondary = true, action = health)
        if (state.value.available) Text(stringResource(R.string.extra_health_up), Modifier.testTag("serviceAvailable"))
        if (receipt != null) {
            Text(stringResource(R.string.extra_contact_receipt, receipt.reference), Modifier.testTag("contactReceipt"))
            TimeLabel(receipt.receivedAt)
            CoreButton(R.string.extra_clear_contact, "newContact", !state.busy, action = clear)
        } else {
            if (pending != null) Text(stringResource(R.string.extra_connection_pending))
            CoreField(R.string.extra_company, company, { if (it.length <= 100) company = it }, "contactCompany", enabled = !state.busy && pending == null)
            CoreField(R.string.extra_contact_name, name, { if (it.length <= 100) name = it }, "contactName", enabled = !state.busy && pending == null)
            CoreField(R.string.extra_email, email, { if (it.length <= 254) email = it }, "contactEmail", enabled = !state.busy && pending == null)
            Row { Checkbox(consent, { consent = it }, enabled = !state.busy && pending == null, modifier = Modifier.testTag("contactConsent")); Text(stringResource(R.string.extra_contact_consent)) }
            val input = pending ?: ContactSubmission(id, company, name, email, consent)
            CoreButton(R.string.extra_contact_send, "sendContact", !state.busy && input.valid()) { submit(input) }
        }
    }
}
