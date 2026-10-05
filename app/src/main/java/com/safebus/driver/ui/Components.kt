package com.safebus.driver.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.VisualTransformation
import com.safebus.driver.R
import com.safebus.driver.ui.theme.SafeBusColors

@Composable
fun Symbol(@DrawableRes icon: Int, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    Icon(painterResource(icon), contentDescription = null, modifier = modifier.size(24.dp), tint = tint)
}

@Composable
fun PrimaryAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: Int? = null, enabled: Boolean = true) {
    Button(onClick, modifier.fillMaxWidth().heightIn(min = 56.dp), enabled = enabled, shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(16.dp)) {
        if (icon != null) { Symbol(icon); Spacer(Modifier.width(8.dp)) }
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: Int? = null) {
    OutlinedButton(onClick, modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        if (icon != null) { Symbol(icon); Spacer(Modifier.width(8.dp)) }
        Text(label)
    }
}

/** Brand navy stays on filled actions; text controls use the readable foreground in both themes. */
@Composable
fun DialogAction(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    TextButton(onClick, modifier.heightIn(min = 48.dp), enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface), content = content)
}

@Composable
fun SafeTextField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null, placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null, trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null, singleLine: Boolean = false, minLines: Int = 1,
    shape: Shape = RoundedCornerShape(8.dp), keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None) {
    OutlinedTextField(value, onValueChange, modifier, label = label, placeholder = placeholder,
        leadingIcon = leadingIcon, trailingIcon = trailingIcon, supportingText = supportingText,
        singleLine = singleLine, minLines = minLines, shape = shape, keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation, colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.onSurface, focusedLabelColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = MaterialTheme.colorScheme.onSurface))
}

@Composable
fun InfoCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
fun Notice(text: String, modifier: Modifier = Modifier, icon: Int = R.drawable.ic_info) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Symbol(icon)
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun StatusChip(label: String, color: Color, icon: Int, modifier: Modifier = Modifier) {
    Surface(modifier.heightIn(min = 32.dp), color = color,
        contentColor = if (color == SafeBusColors.Pending) Color(0xFF1B1F24) else Color.White,
        shape = RoundedCornerShape(50)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Symbol(icon, Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun ServiceCard() {
    InfoCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Symbol(R.drawable.ic_directions_bus, tint = MaterialTheme.colorScheme.onSurface)
            Column {
                Text(stringResource(R.string.assigned_unit), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.unit_route), style = MaterialTheme.typography.titleLarge)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Text(stringResource(R.string.route_endpoints), style = MaterialTheme.typography.bodyMedium)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.schedule), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("06:00 - 14:00", style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** A local schematic, without map tiles, location permission or remote services. */
@Composable
fun RouteIllustration() {
    val street = MaterialTheme.colorScheme.outline
    val route = SafeBusColors.Secondary
    Canvas(Modifier.fillMaxWidth().height(128.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
        repeat(6) { i ->
            val x = size.width * i / 5
            drawLine(street, Offset(x, 0f), Offset(x + size.width * .18f, size.height), 2.dp.toPx())
        }
        repeat(4) { i ->
            val y = size.height * i / 3
            drawLine(street, Offset(0f, y), Offset(size.width, y - size.height * .15f), 2.dp.toPx())
        }
        val points = listOf(Offset(size.width * .18f, size.height * .2f), Offset(size.width * .38f, size.height * .2f),
            Offset(size.width * .52f, size.height * .7f), Offset(size.width * .82f, size.height * .7f))
        points.zipWithNext().forEach { (a, b) -> drawLine(route, a, b, 5.dp.toPx()) }
        drawCircle(route, 7.dp.toPx(), points.first())
        drawCircle(route, 7.dp.toPx(), points.last())
    }
}
