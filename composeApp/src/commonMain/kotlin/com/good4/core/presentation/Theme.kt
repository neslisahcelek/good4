package com.good4.core.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.Typography
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.inter_bold
import good4.composeapp.generated.resources.inter_medium
import good4.composeapp.generated.resources.inter_regular
import good4.composeapp.generated.resources.inter_semibold
import org.jetbrains.compose.resources.Font

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

@Composable
private fun good4Typography(): Typography {
    val interFontFamily = FontFamily(
        Font(Res.font.inter_regular, FontWeight.Normal),
        Font(Res.font.inter_medium, FontWeight.Medium),
        Font(Res.font.inter_semibold, FontWeight.SemiBold),
        Font(Res.font.inter_bold, FontWeight.Bold)
    )

    return remember(interFontFamily) {
        Typography().let { base ->
            base.copy(
                displayLarge = base.displayLarge.copy(fontFamily = interFontFamily),
                displayMedium = base.displayMedium.copy(fontFamily = interFontFamily),
                displaySmall = base.displaySmall.copy(fontFamily = interFontFamily),
                headlineLarge = base.headlineLarge.copy(fontFamily = interFontFamily),
                headlineMedium = base.headlineMedium.copy(fontFamily = interFontFamily),
                headlineSmall = base.headlineSmall.copy(fontFamily = interFontFamily),
                titleLarge = base.titleLarge.copy(fontFamily = interFontFamily),
                titleMedium = base.titleMedium.copy(fontFamily = interFontFamily),
                titleSmall = base.titleSmall.copy(fontFamily = interFontFamily),
                bodyLarge = base.bodyLarge.copy(fontFamily = interFontFamily),
                bodyMedium = base.bodyMedium.copy(fontFamily = interFontFamily),
                bodySmall = base.bodySmall.copy(fontFamily = interFontFamily),
                labelLarge = base.labelLarge.copy(
                    fontFamily = interFontFamily,
                    fontSize = (base.labelLarge.fontSize.value - 2f).sp
                ),
                labelMedium = base.labelMedium.copy(
                    fontFamily = interFontFamily,
                    fontSize = (base.labelMedium.fontSize.value - 2f).sp
                ),
                labelSmall = base.labelSmall.copy(
                    fontFamily = interFontFamily,
                    fontSize = (base.labelSmall.fontSize.value - 2f).sp
                )
            )
        }
    }
}

@Composable
fun Good4Theme(
    content: @Composable () -> Unit
) {
    val typography = good4Typography()

    MaterialTheme(
        colorScheme = Good4ColorScheme,
        typography = typography,
        content = content
    )
}
