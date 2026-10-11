package com.good4

import android.os.Bundle
import android.os.SystemClock
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.good4.notification.AndroidCampusPush
import com.good4.notification.CampusPushNotifications
import com.good4.notification.requestAndroidNotificationPermission
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.good4.navigation.Route

class MainActivity : ComponentActivity() {
    private val pushPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { AndroidCampusPush.refresh() }
    private val requestPushPermission: () -> Unit = {
        requestAndroidNotificationPermission(this) { pushPermission.launch(it) }
    }
    companion object {
        private const val SPLASH_HARD_CAP_MS = 2_000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        AndroidCampusPush.requestPermission = requestPushPermission
        receivePush(intent)
        intent?.dataString?.let(com.good4.campuscloset.CampusEmailVerificationLinks::receive)
        com.good4.notification.handleNotificationIntent(intent)

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        com.good4.notification.handleNotificationIntent(intent)
        intent.dataString?.let(com.good4.campuscloset.CampusEmailVerificationLinks::receive)
        receivePush(intent)
    }

    override fun onResume() { super.onResume(); AndroidCampusPush.refresh() }
    override fun onDestroy() {
        if (AndroidCampusPush.requestPermission === requestPushPermission) AndroidCampusPush.requestPermission = null
        super.onDestroy()
    }
    private fun receivePush(intent: Intent?) {
        CampusPushNotifications.receive(intent?.getStringExtra("recipientUid"), intent?.getStringExtra("type"),
            intent?.getStringExtra("conversationId") ?: intent?.getStringExtra("listingId") ?: intent?.getStringExtra("activityId"))
    }
}
