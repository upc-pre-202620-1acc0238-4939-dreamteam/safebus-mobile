package com.dreamteam.safebus.shared.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.dreamteam.safebus.R
import com.dreamteam.safebus.shared.domain.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable fun CoreIcon(icon: Int, modifier: Modifier = Modifier) {
    Icon(painterResource(icon), contentDescription = null, modifier.size(dimensionResource(R.dimen.core_icon)))
}

@Composable fun CorePage(@StringRes title: Int, busy: Boolean = false, failure: Failure? = null,
    refresh: (() -> Unit)? = null, back: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
            Row(Modifier.fillMaxWidth().heightIn(min = dimensionResource(R.dimen.core_appbar_height)).padding(horizontal = dimensionResource(R.dimen.core_small)),
                verticalAlignment = Alignment.CenterVertically) {
                if (back != null) IconButton(back, Modifier.testTag("pageBack")) {
                    Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.core_back))
                } else Spacer(Modifier.width(dimensionResource(R.dimen.core_small)))
                Text(stringResource(title), Modifier.weight(1f).padding(vertical = dimensionResource(R.dimen.core_small)), style = MaterialTheme.typography.headlineSmall)
                if (refresh != null) IconButton(refresh, enabled = !busy, modifier = Modifier.testTag("coreRefresh")) {
                    Icon(painterResource(R.drawable.ic_refresh), stringResource(R.string.core_refresh))
                }
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(dimensionResource(R.dimen.core_space)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.core_gap))) {
            if (busy) { LinearProgressIndicator(Modifier.fillMaxWidth().testTag("coreLoading")); Text(stringResource(R.string.core_loading)) }
            failure?.let { FailureNotice(it) }
            content()
        }
    }
}

@Composable fun CoreCard(title: String? = null, accent: Color? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
        border = BorderStroke(dimensionResource(R.dimen.core_border), accent ?: MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(dimensionResource(R.dimen.core_space)), verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.core_small))) {
            title?.let { Text(it, style = MaterialTheme.typography.titleLarge) }
            content()
        }
    }
}

@Composable fun CoreNotice(text: String, icon: Int = R.drawable.ic_info, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer, shape = MaterialTheme.shapes.medium) {
        Row(Modifier.padding(dimensionResource(R.dimen.core_space)), horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.core_small))) {
            CoreIcon(icon); Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable fun CoreEmpty(@StringRes message: Int, icon: Int = R.drawable.ic_info, modifier: Modifier = Modifier) {
    CoreCard {
        Column(modifier.fillMaxWidth().padding(vertical = dimensionResource(R.dimen.core_section)), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.core_space))) {
            Icon(painterResource(icon), null, Modifier.size(dimensionResource(R.dimen.core_status_icon)), tint = MaterialTheme.colorScheme.secondary)
            Text(stringResource(message), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable fun CoreButton(@StringRes label: Int, tag: String, enabled: Boolean = true, danger: Boolean = false,
    secondary: Boolean = false, icon: Int? = null, action: () -> Unit) {
    val modifier = Modifier.fillMaxWidth().heightIn(min = dimensionResource(when { danger -> R.dimen.core_emergency_height; secondary -> R.dimen.core_secondary_height; else -> R.dimen.core_action_height })).testTag(tag)
    val body: @Composable RowScope.() -> Unit = {
        icon?.let { CoreIcon(it); Spacer(Modifier.width(dimensionResource(R.dimen.core_small))) }
        Text(stringResource(label), style = if (danger) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.labelLarge)
    }
    if (secondary) OutlinedButton(action, modifier, enabled, shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        border = BorderStroke(dimensionResource(R.dimen.core_border), MaterialTheme.colorScheme.outline), content = body)
    else Button(action, modifier, enabled, shape = if (danger) MaterialTheme.shapes.medium else MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(containerColor = if (danger) colorResource(R.color.core_emergency) else MaterialTheme.colorScheme.primary,
            contentColor = colorResource(R.color.core_white)), content = body)
}

@Composable fun CoreField(@StringRes label: Int, value: String, onChange: (String) -> Unit, tag: String, password: Boolean = false, multiline: Boolean = false, enabled: Boolean = true) {
    var visible by rememberSaveable { mutableStateOf(false) }
    val leading = when { password -> R.drawable.ic_lock; tag == "fleetSearch" -> R.drawable.ic_search; tag == "qrContent" -> R.drawable.ic_qr_code_scanner; tag == "coreLoginId" -> R.drawable.ic_badge; else -> null }
    OutlinedTextField(value, onChange, Modifier.fillMaxWidth().heightIn(min = dimensionResource(if (multiline) R.dimen.core_message_height else R.dimen.core_action_height)).testTag(tag), enabled = enabled,
        label = { Text(stringResource(label)) }, singleLine = !multiline, minLines = if (multiline) 4 else 1, shape = MaterialTheme.shapes.small,
        leadingIcon = leading?.let { { CoreIcon(it) } }, trailingIcon = if (password) { {
            IconButton({ visible = !visible }) { Icon(painterResource(if (visible) R.drawable.ic_visibility_off else R.drawable.ic_visibility), stringResource(if (visible) R.string.hide_password else R.string.show_password)) }
        } } else null,
        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedTextColor = MaterialTheme.colorScheme.onSurface, focusedBorderColor = MaterialTheme.colorScheme.onSurface,
            focusedLabelColor = MaterialTheme.colorScheme.onSurface, cursorColor = MaterialTheme.colorScheme.onSurface),
        visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None)
}

fun failureText(failure: Failure): Int = when (failure.code) {
    "ACTIVE_SOURCE_EXISTS", "SOURCE_REVOKED", "SOURCE_NOT_FOUND", "LABEL_TOO_LONG", "OCCUPANCY_SOURCE_NOT_AUTHORIZED", "COUNTING_FIELDS_REQUIRED" -> R.string.extra_source_error
    "SEQUENCE_GAP", "SEQUENCE_ALREADY_APPLIED", "EVENT_ID_REUSED", "NEGATIVE_COUNT", "OCCUPANCY_NOT_INITIALIZED", "OCCUPANCY_INCONSISTENT", "ALREADY_INITIALIZED" -> R.string.extra_count_error
    "COUNT_PENDING" -> R.string.extra_counter_pending
    "CONTACT_PENDING" -> R.string.extra_connection_pending
    "CREDENTIAL_EXPIRED" -> R.string.core_credential_expired
    "CREDENTIAL_DISABLED" -> R.string.core_credential_disabled
    "CREDENTIAL_NOT_OWNED" -> R.string.core_credential_foreign
    "CREDENTIAL_INVALID" -> R.string.core_credential_invalid
    "ASSIGNMENT_NOT_AVAILABLE" -> R.string.core_assignment_unavailable
    "ASSIGNMENT_NOT_FOUND" -> R.string.core_no_assignment
    "ACTIVE_JOURNEY_EXISTS" -> R.string.core_existing_journey
    "BUS_NOT_IN_SERVICE" -> R.string.core_bus_not_serving
    "BUS_QR_INVALID" -> R.string.core_bus_qr_invalid
    "ALERT_NOT_DELIVERED" -> R.string.core_alert_not_delivered
    "INVALID_END_TIME", "INVALID_ACTIVATION_TIME", "INVALID_CAPTURE_TIME" -> R.string.core_clock_error
    "PHOTO_INVALID", "FACE_PHOTO_INVALID", "FACE_PHOTO_TOO_LARGE", "EVIDENCE_PHOTO_INVALID", "EVIDENCE_PHOTO_TOO_LARGE" -> R.string.core_photo_invalid
    "DUPLICATE_CONTRIBUTION" -> R.string.core_duplicate_request
    "DNI_ALREADY_REGISTERED" -> R.string.core_duplicate_dni
    "BUS_ID_UNAVAILABLE" -> R.string.core_bus_id_missing
    else -> when (failure.kind) {
        FailureKind.NETWORK -> R.string.core_network_error
        FailureKind.UNAUTHORIZED -> R.string.core_auth_error
        FailureKind.FORBIDDEN -> R.string.core_forbidden
        FailureKind.NOT_FOUND -> R.string.core_missing
        FailureKind.CONFLICT -> R.string.core_conflict
        FailureKind.VALIDATION -> R.string.core_validation
        FailureKind.RATE_LIMITED -> R.string.core_rate_limit
        FailureKind.SERVER -> R.string.core_server_error
        FailureKind.INVALID_RESPONSE -> R.string.core_response_error
        FailureKind.STORAGE -> R.string.core_storage_error
    }
}

@Composable fun FailureNotice(failure: Failure) {
    CoreNotice(stringResource(failureText(failure)), R.drawable.ic_info,
        Modifier.testTag("coreError").semantics { liveRegion = LiveRegionMode.Polite })
}
@Composable fun DeliveryNotice(delivery: Delivery) {
    CoreNotice(stringResource(if (delivery == Delivery.PENDING) R.string.core_pending else R.string.core_received),
        if (delivery == Delivery.PENDING) R.drawable.ic_cloud_off else R.drawable.ic_check_circle,
        Modifier.testTag("deliveryNotice").semantics { liveRegion = LiveRegionMode.Polite })
}
fun stateColorResource(value: String): Int = when (value) {
    "CRITICAL" -> R.color.core_emergency
    "HIGH" -> R.color.core_high
    "AWAITING_APPROVAL", "STALE" -> R.color.core_pending
    "CLOSED", "ENDED" -> R.color.core_closed
    "PENDING", "COLLECTING", "COUNTED", "ATTACHED" -> R.color.core_information
    "ACTIVE", "ASSIGNED", "CURRENT", "VALIDATED", "VERIFIED", "APPROVED" -> R.color.core_secondary
    else -> R.color.core_inactive
}
@Composable fun StateLabel(value: String) {
    val label = when (value) {
        "ACTIVE", "ASSIGNED" -> R.string.core_status_active
        "VERIFIED", "VALIDATED" -> R.string.core_company_verified
        "NOT_VALIDATED" -> R.string.core_company_not_verified
        "IN_PROGRESS" -> R.string.core_status_attention
        "CLOSED", "ENDED" -> R.string.core_status_closed
        "PENDING" -> R.string.core_status_pending
        "COLLECTING", "COUNTED" -> R.string.core_status_collecting
        "AWAITING_APPROVAL" -> R.string.core_status_review
        "APPROVED" -> R.string.core_status_approved
        "DECLINED", "NOT_APPROVED" -> R.string.core_status_declined
        "EXPIRED" -> R.string.core_status_expired
        "ATTACHED" -> R.string.core_status_attached
        "LATE" -> R.string.core_status_late
        "CURRENT" -> R.string.core_status_current
        "STALE" -> R.string.core_status_stale
        "CRITICAL" -> R.string.core_status_critical
        "HIGH" -> R.string.core_status_high
        else -> R.string.core_status_unknown
    }
    val icon = when (value) {
        "CRITICAL", "HIGH" -> R.drawable.ic_warning
        "AWAITING_APPROVAL", "STALE", "PENDING" -> R.drawable.ic_schedule
        "COLLECTING", "COUNTED", "ATTACHED" -> R.drawable.ic_groups
        "CLOSED", "ENDED", "VERIFIED", "VALIDATED" -> R.drawable.ic_check_circle
        else -> R.drawable.ic_info
    }
    Surface(color = colorResource(stateColorResource(value)), contentColor = colorResource(if (value in setOf("STALE", "AWAITING_APPROVAL")) R.color.core_text else R.color.core_white), shape = RoundedCornerShape(50)) {
        Row(Modifier.heightIn(min = dimensionResource(R.dimen.core_chip_height)).padding(horizontal = dimensionResource(R.dimen.core_small), vertical = dimensionResource(R.dimen.core_tiny)),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.core_tiny))) {
            Icon(painterResource(icon), null, Modifier.size(dimensionResource(R.dimen.core_chip_icon)))
            Text(stringResource(label), style = MaterialTheme.typography.labelMedium)
        }
    }
}
@Composable fun TimeLabel(at: Instant) {
    Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.core_tiny)), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(R.drawable.ic_schedule), null, Modifier.size(dimensionResource(R.dimen.core_chip_icon)))
        Text(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault()).format(at), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
