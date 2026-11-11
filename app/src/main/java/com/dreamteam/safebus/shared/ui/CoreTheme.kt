package com.dreamteam.safebus.shared.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.sp
import com.dreamteam.safebus.R

@OptIn(ExperimentalTextApi::class)
private val inter = FontFamily(
    Font(R.font.inter, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.inter, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.inter, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.inter, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)
private fun reportType(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = inter, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight, fontFeatureSettings = "tnum")

@Composable fun CoreTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val base = if (darkTheme) darkColorScheme() else lightColorScheme()
    val colors = base.copy(
        primary = colorResource(R.color.core_primary), onPrimary = colorResource(R.color.core_white),
        secondary = colorResource(R.color.core_secondary), onSecondary = colorResource(R.color.core_white),
        primaryContainer = colorResource(R.color.core_container), onPrimaryContainer = colorResource(R.color.core_primary),
        secondaryContainer = colorResource(R.color.core_secondary), onSecondaryContainer = colorResource(R.color.core_white),
        background = colorResource(if (darkTheme) R.color.core_dark_background else R.color.core_background),
        surface = colorResource(if (darkTheme) R.color.core_dark_surface else R.color.core_surface),
        surfaceContainer = colorResource(if (darkTheme) R.color.core_dark_surface else R.color.core_surface),
        surfaceContainerHigh = colorResource(if (darkTheme) R.color.core_dark_surface else R.color.core_surface),
        surfaceContainerHighest = colorResource(if (darkTheme) R.color.core_dark_surface else R.color.core_surface),
        onBackground = colorResource(if (darkTheme) R.color.core_dark_text else R.color.core_text),
        onSurface = colorResource(if (darkTheme) R.color.core_dark_text else R.color.core_text),
        onSurfaceVariant = colorResource(if (darkTheme) R.color.core_dark_muted else R.color.core_muted),
        outline = colorResource(if (darkTheme) R.color.core_dark_outline else R.color.core_outline),
        outlineVariant = colorResource(if (darkTheme) R.color.core_dark_outline else R.color.core_outline),
        error = colorResource(R.color.core_emergency), onError = colorResource(R.color.core_white),
    )
    val typography = Typography(
        displayLarge = reportType(32, 40, FontWeight.Bold), displayMedium = reportType(32, 40, FontWeight.Bold), displaySmall = reportType(32, 40, FontWeight.Bold),
        headlineLarge = reportType(24, 32, FontWeight.SemiBold), headlineMedium = reportType(24, 32, FontWeight.SemiBold), headlineSmall = reportType(24, 32, FontWeight.SemiBold),
        titleLarge = reportType(20, 28, FontWeight.SemiBold), titleMedium = reportType(20, 28, FontWeight.SemiBold), titleSmall = reportType(16, 24, FontWeight.SemiBold),
        bodyLarge = reportType(16, 24), bodyMedium = reportType(14, 20), bodySmall = reportType(12, 16),
        labelLarge = reportType(14, 20, FontWeight.Medium), labelMedium = reportType(14, 20, FontWeight.Medium), labelSmall = reportType(12, 16),
    )
    MaterialTheme(colors, Shapes(small = RoundedCornerShape(dimensionResource(R.dimen.core_control_radius)),
        medium = RoundedCornerShape(dimensionResource(R.dimen.core_card_radius))), typography, content)
}
