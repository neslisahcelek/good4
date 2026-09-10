package com.good4.core.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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

// ODTU's interface uses the native platform typeface. Keeping the default
// Material typography gives Android Roboto and iOS San Francisco naturally.
private val Good4Typography = Typography()

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
