package com.dreamteam.safebus.shared.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.dreamteam.safebus.R
import com.dreamteam.safebus.shared.domain.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable fun CoreTheme(content: @Composable () -> Unit) {
    val typography = Typography()
    val inter = FontFamily(Font(R.font.inter))
    MaterialTheme(colorScheme = darkColorScheme(primary = colorResource(R.color.core_primary), onPrimary = colorResource(R.color.core_background),
        secondary = colorResource(R.color.core_secondary), background = colorResource(R.color.core_background), surface = colorResource(R.color.core_surface),
        onSurface = colorResource(R.color.core_text), onBackground = colorResource(R.color.core_text), onSurfaceVariant = colorResource(R.color.core_muted),
        outline = colorResource(R.color.core_outline), error = colorResource(R.color.core_emergency)),
        typography = typography.copy(headlineMedium = typography.headlineMedium.copy(fontFamily = inter), titleLarge = typography.titleLarge.copy(fontFamily = inter),
            titleMedium = typography.titleMedium.copy(fontFamily = inter), bodyLarge = typography.bodyLarge.copy(fontFamily = inter),
            bodyMedium = typography.bodyMedium.copy(fontFamily = inter), labelLarge = typography.labelLarge.copy(fontFamily = inter)), content = content)
}

@Composable fun CorePage(@StringRes title: Int, busy: Boolean = false, failure: Failure? = null,
    refresh: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(dimensionResource(R.dimen.core_space)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.core_gap))) {
        Text(stringResource(title), style = MaterialTheme.typography.headlineMedium)
        if (refresh != null) TextButton(refresh, enabled = !busy, modifier = Modifier.testTag("coreRefresh")) { Text(stringResource(R.string.core_refresh)) }
        if (busy) { LinearProgressIndicator(Modifier.fillMaxWidth().testTag("coreLoading")); Text(stringResource(R.string.core_loading)) }
        failure?.let { FailureNotice(it) }
        content()
    }
}

@Composable fun CoreCard(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(dimensionResource(R.dimen.core_space)), verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.core_small))) {
            title?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
            content()
        }
    }
}

@Composable fun CoreButton(@StringRes label: Int, tag: String, enabled: Boolean = true, danger: Boolean = false, action: () -> Unit) {
    Button(action, Modifier.fillMaxWidth().heightIn(min = dimensionResource(if (danger) R.dimen.core_emergency_height else R.dimen.core_action_height)).testTag(tag), enabled = enabled,
        colors = if (danger) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.background) else ButtonDefaults.buttonColors()) {
        Text(stringResource(label))
    }
}

@Composable fun CoreField(@StringRes label: Int, value: String, onChange: (String) -> Unit, tag: String, password: Boolean = false, multiline: Boolean = false, enabled: Boolean = true) {
    OutlinedTextField(value, onChange, Modifier.fillMaxWidth().testTag(tag), enabled = enabled, label = { Text(stringResource(label)) },
        singleLine = !multiline, minLines = if (multiline) 3 else 1,
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None)
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
    Text(stringResource(failureText(failure)), color = MaterialTheme.colorScheme.error,
        modifier = Modifier.testTag("coreError").semantics { liveRegion = LiveRegionMode.Polite })
}
@Composable fun DeliveryNotice(delivery: Delivery) {
    Text(stringResource(if (delivery == Delivery.PENDING) R.string.core_pending else R.string.core_received), Modifier.testTag("deliveryNotice").semantics { liveRegion = LiveRegionMode.Polite })
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
    Text(stringResource(label), color = if (value == "CRITICAL") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
}
@Composable fun TimeLabel(at: Instant) { Text(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault()).format(at)) }
