package com.dreamteam.safebus.shared.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.dreamteam.safebus.R
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable fun dateTimeLabel(value: String): String {
    val locale = LocalConfiguration.current.locales[0]
    return value.takeIf(String::isNotBlank)?.let {
        LocalDateTime.parse(it).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale))
    } ?: stringResource(R.string.ui_choose_date_time)
}

@Composable fun DateTimeField(@StringRes label: Int, value: String, onChange: (String) -> Unit, tag: String, enabled: Boolean = true) {
    var open by rememberSaveable { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, enabled = enabled, shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth().heightIn(min = dimensionResource(R.dimen.core_action_height)).testTag(tag)) {
        Column(Modifier.fillMaxWidth().padding(vertical = dimensionResource(R.dimen.core_small))) {
            Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
            Text(dateTimeLabel(value), style = MaterialTheme.typography.bodyLarge)
        }
    }
    if (open) DateTimeSelection(label, value, { open = false }) { onChange(it); open = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun DateTimeSelection(@StringRes label: Int, value: String, dismiss: () -> Unit, select: (String) -> Unit) {
    val initial = remember { value.takeIf(String::isNotBlank)?.let(LocalDateTime::parse) ?: LocalDateTime.now().withSecond(0).withNano(0) }
    var choosingTime by rememberSaveable { mutableStateOf(false) }
    val date = rememberDatePickerState(initialSelectedDateMillis = initial.toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
    val time = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    if (!choosingTime) DatePickerDialog(onDismissRequest = dismiss,
        confirmButton = { TextButton({ choosingTime = true }, enabled = date.selectedDateMillis != null, modifier = Modifier.testTag("dateConfirm")) { Text(stringResource(R.string.ui_continue)) } },
        dismissButton = { TextButton(dismiss, Modifier.testTag("dateCancel")) { Text(stringResource(R.string.core_cancel)) } }) {
        DatePicker(date, showModeToggle = false, title = { Text(stringResource(label), Modifier.padding(dimensionResource(R.dimen.core_space))) })
    } else AlertDialog(onDismissRequest = dismiss, modifier = Modifier.testTag("timeDialog"),
        title = { Text(stringResource(R.string.ui_choose_time)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { TimePicker(time) } },
        confirmButton = { TextButton({
            val day = Instant.ofEpochMilli(requireNotNull(date.selectedDateMillis)).atZone(ZoneOffset.UTC).toLocalDate()
            select(day.atTime(time.hour, time.minute).toString())
        }, Modifier.testTag("timeConfirm")) { Text(stringResource(R.string.core_confirm)) } },
        dismissButton = {
            Row {
                TextButton({ choosingTime = false }, Modifier.testTag("timeBack")) { Text(stringResource(R.string.core_back)) }
                TextButton(dismiss, Modifier.testTag("timeCancel")) { Text(stringResource(R.string.core_cancel)) }
            }
        })
}
