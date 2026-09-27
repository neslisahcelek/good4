package com.good4.core.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color

private val Good4LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color.White,
    primaryContainer = LightGood4Colors.pistachioGreen,
    onPrimaryContainer = LightGood4Colors.textPrimary,
    secondary = Color(0xFFE6F4C2),
    onSecondary = Color(0xFF1D1D1B),
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = LightGood4Colors.textPrimary,
    tertiary = AccentYellow,
    onTertiary = Color(0xFF1D1D1B),
    tertiaryContainer = Color(0xFFFFF4B0),
    onTertiaryContainer = Color(0xFF343000),
    background = Color.White,
    onBackground = Color(0xFF1D1D1B),
    surface = Color.White,
    onSurface = Color(0xFF1D1D1B),
    surfaceContainerLowest = LightGood4Colors.appBackground,
    surfaceContainerLow = LightGood4Colors.surfaceDefault,
    surfaceContainer = LightGood4Colors.surfaceDefault,
    surfaceContainerHigh = LightGood4Colors.surfaceCanvasWarm,
    surfaceContainerHighest = LightGood4Colors.surfaceMuted,
    surfaceDim = LightGood4Colors.surfaceMuted,
    surfaceBright = LightGood4Colors.surfaceDefault,
    surfaceVariant = LightGood4Colors.surfaceMuted,
    onSurfaceVariant = LightGood4Colors.textSecondary,
    // Match the base surface so elevation cannot add a coloured tint.
    surfaceTint = LightGood4Colors.surfaceDefault,
    inverseSurface = DarkGood4Colors.surfaceDefault,
    inverseOnSurface = DarkGood4Colors.textPrimary,
    inversePrimary = PrimaryGreenDark,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD5),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF9D9A90),
    outlineVariant = LightGood4Colors.borderMuted,
    scrim = Color.Black
)

private val Good4DarkColorScheme = darkColorScheme(
    primary = PrimaryGreenDark,
    onPrimary = Color(0xFF062C20),
    primaryContainer = Color(0xFF123D2E),
    onPrimaryContainer = Color(0xFFB7F1D7),
    secondary = Color(0xFFBFD79A),
    onSecondary = Color(0xFF18220F),
    secondaryContainer = DarkGood4Colors.pistachioGreen,
    onSecondaryContainer = Color(0xFFDCECC0),
    tertiary = Color(0xFFFFE85A),
    onTertiary = Color(0xFF262200),
    tertiaryContainer = Color(0xFF4B4600),
    onTertiaryContainer = Color(0xFFFFF4B0),
    background = Color(0xFF171514),
    onBackground = Color(0xFFF6F4EF),
    surface = Color(0xFF252120),
    onSurface = Color(0xFFF6F4EF),
    surfaceContainerLowest = DarkGood4Colors.appBackground,
    surfaceContainerLow = Color(0xFF211D1B),
    surfaceContainer = DarkGood4Colors.surfaceDefault,
    surfaceContainerHigh = DarkGood4Colors.surfaceMuted,
    surfaceContainerHighest = Color(0xFF3B3532),
    surfaceDim = DarkGood4Colors.surfaceDefault,
    surfaceBright = Color(0xFF3B3532),
    surfaceVariant = DarkGood4Colors.surfaceMuted,
    onSurfaceVariant = DarkGood4Colors.textSecondary,
    surfaceTint = DarkGood4Colors.surfaceDefault,
    inverseSurface = LightGood4Colors.surfaceDefault,
    inverseOnSurface = LightGood4Colors.textPrimary,
    inversePrimary = PrimaryGreen,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD5),
    outline = Color(0xFF8D8982),
    outlineVariant = DarkGood4Colors.borderMuted,
    scrim = Color.Black
)

// ODTU's interface uses the native platform typeface. Keeping the default
// Material typography gives Android Roboto and iOS San Francisco naturally.
private val Good4Typography = Typography()

@Immutable
data class ThemeController(
    val isDark: Boolean,
    val setDark: (Boolean) -> Unit
)

val LocalThemeController = staticCompositionLocalOf {
    ThemeController(isDark = false, setDark = {})
}

@Composable
fun Good4Theme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalGood4Colors provides if (darkTheme) DarkGood4Colors else LightGood4Colors
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) Good4DarkColorScheme else Good4LightColorScheme,
            typography = Good4Typography,
            content = content
        )
    }
}
