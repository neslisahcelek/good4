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
    primary = DeepGreen,
    onPrimary = Color(0xFF172000),
    secondary = Color(0xFFE6F4C2),
    onSecondary = Color(0xFF1D1D1B),
    tertiary = AccentYellow,
    onTertiary = Color(0xFF1D1D1B),
    background = Color.White,
    onBackground = Color(0xFF1D1D1B),
    surface = Color.White,
    onSurface = Color(0xFF1D1D1B),
    error = ErrorRed,
    onError = Color.White,
    outline = Color(0xFF9D9A90)
)

private val Good4DarkColorScheme = darkColorScheme(
    primary = Color(0xFFB6E53B),
    onPrimary = Color(0xFF172000),
    secondary = Color(0xFFBFD79A),
    onSecondary = Color(0xFF18220F),
    tertiary = Color(0xFFFFE85A),
    onTertiary = Color(0xFF262200),
    background = Color(0xFF171514),
    onBackground = Color(0xFFF6F4EF),
    surface = Color(0xFF252120),
    onSurface = Color(0xFFF6F4EF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    outline = Color(0xFF8D8982)
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
