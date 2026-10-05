package com.safebus.driver.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.sp
import com.safebus.driver.R

object SafeBusColors {
    val Primary = Color(0xFF0D2C54)
    val Secondary = Color(0xFF00796B)
    val PrimaryContainer = Color(0xFFDCE7F5)
    val Critical = Color(0xFFC62828)
    val High = Color(0xFFBF360C)
    val Pending = Color(0xFFF9A825)
    val Information = Color(0xFF1565C0)
    val Closed = Color(0xFF2E7D32)
    val Inactive = Color(0xFF6B7480)
}

private val Light = lightColorScheme(
    primary = SafeBusColors.Primary, onPrimary = Color.White,
    secondary = SafeBusColors.Secondary, onSecondary = Color.White,
    primaryContainer = SafeBusColors.PrimaryContainer, onPrimaryContainer = SafeBusColors.Primary,
    secondaryContainer = SafeBusColors.Secondary, onSecondaryContainer = Color.White,
    background = Color(0xFFF5F7FA), surface = Color.White,
    onBackground = Color(0xFF1B1F24), onSurface = Color(0xFF1B1F24),
    onSurfaceVariant = Color(0xFF5F6B7A), outline = Color(0xFFD0D7DE),
    surfaceVariant = SafeBusColors.PrimaryContainer,
    error = SafeBusColors.Critical,
)
private val Dark = darkColorScheme(
    primary = SafeBusColors.Primary, onPrimary = Color.White,
    secondary = SafeBusColors.Secondary, onSecondary = Color.White,
    primaryContainer = SafeBusColors.PrimaryContainer, onPrimaryContainer = SafeBusColors.Primary,
    secondaryContainer = SafeBusColors.Secondary, onSecondaryContainer = Color.White,
    background = Color(0xFF0F1720), surface = Color(0xFF18222E),
    onBackground = Color(0xFFE6EAF0), onSurface = Color(0xFFE6EAF0),
    onSurfaceVariant = Color(0xFFA3ADB9), outline = Color(0xFF2C3846),
    surfaceVariant = Color(0xFF18222E), error = SafeBusColors.Critical,
)

@OptIn(ExperimentalTextApi::class)
private val Inter = FontFamily(
    Font(R.font.inter, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.inter, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.inter, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.inter, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)
private fun style(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = Inter, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight, fontFeatureSettings = "tnum")
private val Type = Typography(
    displaySmall = style(32, 40, FontWeight.Bold),
    headlineSmall = style(24, 32, FontWeight.SemiBold),
    titleLarge = style(20, 28, FontWeight.SemiBold),
    titleMedium = style(16, 24, FontWeight.SemiBold),
    bodyLarge = style(16, 24), bodyMedium = style(14, 20), bodySmall = style(12, 16),
    labelLarge = style(14, 20, FontWeight.Medium),
    labelMedium = style(14, 20, FontWeight.Medium), labelSmall = style(12, 16),
)

@Composable
fun SafeBusTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) Dark else Light, typography = Type, content = content)
}
