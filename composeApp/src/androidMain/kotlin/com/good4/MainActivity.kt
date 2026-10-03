package com.good4

import android.os.Bundle
import android.os.SystemClock
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import com.good4.notification.AndroidCampusPush
import com.good4.notification.CampusPushNotifications
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.good4.navigation.Route

class MainActivity : ComponentActivity() {
    private val pushPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { AndroidCampusPush.refresh() }
    private val requestPushPermission: () -> Unit = {
        val preferences = getSharedPreferences("campus_push", MODE_PRIVATE)
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            && (!preferences.getBoolean("permission_asked", false) || shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS))) {
            preferences.edit().putBoolean("permission_asked", true).apply()
            pushPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
        }
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
            intent?.getStringExtra("conversationId") ?: intent?.getStringExtra("listingId"))
    }
}
