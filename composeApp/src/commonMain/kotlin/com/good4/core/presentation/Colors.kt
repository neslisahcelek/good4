package com.good4.core.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class Good4Colors(
    val textPrimary: Color,
    val textSecondary: Color,
    val borderMuted: Color,
    val pistachioGreen: Color,
    val surfaceDefault: Color,
    val surfaceMuted: Color,
    val surfaceCanvasWarm: Color,
    val appBackground: Color
)

internal val LightGood4Colors = Good4Colors(
    textPrimary = Color(0xFF1D1D1B),
    textSecondary = Color(0xFF6E6E6B),
    borderMuted = Color(0x4D9D9A90),
    pistachioGreen = Color(0xFFE6F4C2),
    surfaceDefault = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFF2F1ED),
    surfaceCanvasWarm = Color(0xFFFAF9F5),
    appBackground = Color.White
)

internal val DarkGood4Colors = Good4Colors(
    textPrimary = Color(0xFFF6F4EF),
    textSecondary = Color(0xFFB8B5AE),
    borderMuted = Color(0x667D7972),
    pistachioGreen = Color(0xFF26371D),
    surfaceDefault = Color(0xFF252120),
    surfaceMuted = Color(0xFF302B29),
    surfaceCanvasWarm = Color(0xFF171514),
    appBackground = Color(0xFF171514)
)

internal val LocalGood4Colors = staticCompositionLocalOf { LightGood4Colors }

val TextPrimary: Color @Composable get() = LocalGood4Colors.current.textPrimary
val TextSecondary: Color @Composable get() = LocalGood4Colors.current.textSecondary
val AccentYellow = Color(0xFFFFE600)
val BorderMuted: Color @Composable get() = LocalGood4Colors.current.borderMuted
val PistachioGreen: Color @Composable get() = LocalGood4Colors.current.pistachioGreen
val PrimaryGreen = Color(0xFF008556)
val ErrorRed = Color(0xFFD6483B)
val SurfaceDefault: Color @Composable get() = LocalGood4Colors.current.surfaceDefault
val SurfaceMuted: Color @Composable get() = LocalGood4Colors.current.surfaceMuted
val SurfaceCanvasWarm: Color @Composable get() = LocalGood4Colors.current.surfaceCanvasWarm
val AppBackground: Color @Composable get() = LocalGood4Colors.current.appBackground
val SecondaryContainer = Color(0xFFD8E5B4)
val DeepGreen = Color(0xFFA7D80A)
val TertiaryOlive = Color(0xFF4B6400)
