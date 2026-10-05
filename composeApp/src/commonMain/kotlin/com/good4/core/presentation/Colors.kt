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
val PrimaryGreenDark = Color(0xFF5CD6A9)
val ErrorRed = Color(0xFFD6483B)
val SurfaceDefault: Color @Composable get() = LocalGood4Colors.current.surfaceDefault
val SurfaceMuted: Color @Composable get() = LocalGood4Colors.current.surfaceMuted
val SurfaceCanvasWarm: Color @Composable get() = LocalGood4Colors.current.surfaceCanvasWarm
val AppBackground: Color @Composable get() = LocalGood4Colors.current.appBackground
val SecondaryContainer = Color(0xFFD8E5B4)
val DeepGreen = Color(0xFFA7D80A)
val TertiaryOlive = Color(0xFF4B6400)

// Campus categories share these accents with cards, filters and offer states.
val ClosetOfferAccent = Color(0xFFE08A1E)
val ClosetClothingAccent = Color(0xFFE59AC0)
val ClosetAccessoriesAccent = Color(0xFFF2A66F)
val ClosetElectronicsAccent = Color(0xFF4B9FD1)
val ClosetSportsAccent = Color(0xFF75D9BE)
val ClosetBooksAccent = Color(0xFFA58DEB)
val ClosetDormAccent = Color(0xFF8CB7ED)
val ClosetHobbyAccent = Color(0xFF68CCDC)
val ClosetOtherAccent = Color(0xFFB8B08D)

// Community screens share accents across student and manager views.
val CommunityAccent = Color(0xFF75D9BE)
val DraftAccent = Color(0xFFE08A1E)
val CommunityAttendanceAccent = Color(0xFFA58DEB)
