package com.good4

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.good4.navigation.Route

class MainActivity : ComponentActivity() {
    companion object {
        private const val SPLASH_HARD_CAP_MS = 2_000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        var isSplashReady = false
        val splashShownAt = SystemClock.elapsedRealtime()
        splashScreen.setKeepOnScreenCondition {
            val elapsed = SystemClock.elapsedRealtime() - splashShownAt
            !isSplashReady && elapsed < SPLASH_HARD_CAP_MS
        }

        enableEdgeToEdge()

        setContent {
            App(
                startDestination = Route.Splash,
                onSplashReady = { isSplashReady = true }
            )
        }
    }
}
