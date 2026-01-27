package com.good4.core.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.Typography

private val Good4ColorScheme = lightColorScheme(
    primary = DeepGreen,
    onPrimary = SurfaceDefault,
    secondary = PistachioGreen,
    onSecondary = TextPrimary,
    tertiary = AccentYellow,
    onTertiary = TextPrimary,
    background = AppBackground,
    onBackground = TextPrimary,
    surface = SurfaceDefault,
    onSurface = TextPrimary,
    error = ErrorRed,
    onError = SurfaceDefault,
    outline = BorderMuted
)

private val Good4Typography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = FontFamily.SansSerif),
        displayMedium = base.displayMedium.copy(fontFamily = FontFamily.SansSerif),
        displaySmall = base.displaySmall.copy(fontFamily = FontFamily.SansSerif),
        headlineLarge = base.headlineLarge.copy(fontFamily = FontFamily.SansSerif),
        headlineMedium = base.headlineMedium.copy(fontFamily = FontFamily.SansSerif),
        headlineSmall = base.headlineSmall.copy(fontFamily = FontFamily.SansSerif),
        titleLarge = base.titleLarge.copy(fontFamily = FontFamily.SansSerif),
        titleMedium = base.titleMedium.copy(fontFamily = FontFamily.SansSerif),
        titleSmall = base.titleSmall.copy(fontFamily = FontFamily.SansSerif),
        bodyLarge = base.bodyLarge.copy(fontFamily = FontFamily.SansSerif),
        bodyMedium = base.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
        bodySmall = base.bodySmall.copy(fontFamily = FontFamily.SansSerif),
        labelLarge = base.labelLarge.copy(fontFamily = FontFamily.SansSerif),
        labelMedium = base.labelMedium.copy(fontFamily = FontFamily.SansSerif),
        labelSmall = base.labelSmall.copy(fontFamily = FontFamily.SansSerif)
    )
}

@Composable
fun Good4Theme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = Good4ColorScheme,
        typography = Good4Typography,
        content = content
    )
}
