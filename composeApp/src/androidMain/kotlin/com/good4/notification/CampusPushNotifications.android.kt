package com.good4.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

internal const val CAMPUS_NOTIFICATION_CHANNEL = "campus_closet"

object AndroidCampusPush {
    private var context: Context? = null
    private var generation = 0
    var requestPermission: (() -> Unit)? = null

    fun initialize(value: Context) {
        context = value.applicationContext
        if (Build.VERSION.SDK_INT >= 26) {
            value.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CAMPUS_NOTIFICATION_CHANNEL, "Kampüs Dolabı", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }

    fun refresh() {
        val ctx = context ?: return
        if (com.good4.core.util.AppEnvironment.firebaseBackend != com.good4.core.util.FirebaseBackend.V2) {
            CampusPushNotifications.updateDevice(null, "unavailable")
            return
        }
        val channelEnabled = Build.VERSION.SDK_INT < 26 || ctx.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(CAMPUS_NOTIFICATION_CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE
        val granted = channelEnabled && NotificationManagerCompat.from(ctx).areNotificationsEnabled()
            && (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
        if (!granted) {
            CampusPushNotifications.updateDevice(null, "denied")
            return
        }
        FirebaseMessaging.getInstance().isAutoInitEnabled = true
        val currentGeneration = generation
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (generation != currentGeneration) return@addOnCompleteListener
            CampusPushNotifications.updateDevice(if (task.isSuccessful) task.result else null, "granted")
        }
    }

    fun invalidate() { generation += 1 }
}

actual fun nativePushPlatform() = "android"
actual fun refreshNativePushRegistration() = AndroidCampusPush.refresh()
actual fun requestNativePushPermission() { AndroidCampusPush.requestPermission?.invoke() }
actual suspend fun deleteNativePushToken() {
    AndroidCampusPush.invalidate()
    FirebaseMessaging.getInstance().isAutoInitEnabled = false
    runCatching { FirebaseMessaging.getInstance().deleteToken().await() }
    CampusPushNotifications.updateDevice(null, "unknown")
}
