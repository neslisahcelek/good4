package com.good4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.good4.navigation.Route

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        var isSplashReady = false
        splashScreen.setKeepOnScreenCondition { !isSplashReady }

        enableEdgeToEdge()

        setContent {
            App(
                startDestination = Route.Splash,
                onSplashReady = { isSplashReady = true }
            )
        }
    }
}
