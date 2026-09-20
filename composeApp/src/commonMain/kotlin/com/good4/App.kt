package com.good4

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.Good4Theme
import com.good4.core.presentation.LocalThemeController
import com.good4.core.presentation.ThemeController
import com.good4.core.presentation.loadDarkModePreference
import com.good4.core.presentation.saveDarkModePreference
import com.good4.navigation.Good4NavGraph
import com.good4.navigation.Route

@Composable
fun App(
    startDestination: Route = Route.Splash,
    onSplashReady: (() -> Unit)? = null
) {
    var isDarkMode by remember { mutableStateOf(loadDarkModePreference()) }
    Good4Theme(darkTheme = isDarkMode) {
        CompositionLocalProvider(
            LocalThemeController provides ThemeController(
                isDark = isDarkMode,
                setDark = { enabled ->
                    isDarkMode = enabled
                    saveDarkModePreference(enabled)
                }
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppBackground)
            ) {
                Good4NavGraph(
                    startDestination = startDestination,
                    onSplashReady = onSplashReady
                )
            }
        }
    }
}
