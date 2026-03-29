package com.good4

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.Good4Theme
import com.good4.navigation.Good4NavGraph
import com.good4.navigation.Route

@Composable
fun App(
    startDestination: Route = Route.Splash,
    onSplashReady: (() -> Unit)? = null
) {
    Good4Theme {
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
