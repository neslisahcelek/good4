package com.good4

import androidx.compose.runtime.Composable
import com.good4.core.presentation.Good4Theme
import com.good4.navigation.Good4NavGraph
import com.good4.navigation.Route

@Composable
fun App(
    startDestination: Route = Route.Splash,
    onSplashReady: (() -> Unit)? = null
) {
    Good4Theme {
        Good4NavGraph(
            startDestination = startDestination,
            onSplashReady = onSplashReady
        )
    }
}
