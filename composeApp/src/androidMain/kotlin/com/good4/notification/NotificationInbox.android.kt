package com.good4.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.good4.R
import kotlinx.coroutines.tasks.await
import org.koin.core.context.GlobalContext
import java.util.UUID

private fun context() = GlobalContext.get().get<Context>()
private fun preferences() = context().getSharedPreferences("good4_push", Context.MODE_PRIVATE)
actual fun pushInstallation(): PushInstallation {
    val prefs = preferences()
    val id = prefs.getString("installation_id", null) ?: UUID.randomUUID().toString().also { prefs.edit().putString("installation_id", it).commit() }
    val secret = prefs.getString("installation_secret", null) ?: (UUID.randomUUID().toString() + UUID.randomUUID()).also { prefs.edit().putString("installation_secret", it).commit() }
    return PushInstallation(id, secret, "android")
}
actual fun notificationEducationShown() = preferences().getBoolean("education_shown", false)
actual fun saveNotificationEducationShown() { preferences().edit().putBoolean("education_shown", true).apply() }
private fun permitted(context: Context) = NotificationManagerCompat.from(context).areNotificationsEnabled() &&
    (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
actual suspend fun pushSnapshot(): PushSnapshot {
    createNotificationChannels(context())
    val messaging = FirebaseMessaging.getInstance()
    messaging.isAutoInitEnabled = true
    return PushSnapshot(runCatching { messaging.token.await() }.getOrDefault(""), permitted(context()))
}
actual suspend fun clearPushRegistration(): Boolean = runCatching {
    val messaging = FirebaseMessaging.getInstance()
    messaging.isAutoInitEnabled = false
    messaging.deleteToken().await()
    NotificationManagerCompat.from(context()).cancelAll()
    true
}.getOrDefault(false)
@Composable
actual fun rememberNotificationPermissionLauncher(): NotificationPermissionLauncher {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { PushSignals.refresh() }
    return remember(context, launcher) { object : NotificationPermissionLauncher {
        override fun request(onComplete: () -> Unit) {
            if (Build.VERSION.SDK_INT >= 33 && !permitted(context)) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else PushSignals.refresh()
            onComplete()
        }
        override fun openSettings() {
            context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
        }
    } }
}
internal fun createNotificationChannels(context: Context) {
    if (Build.VERSION.SDK_INT < 26) return
    val manager = context.getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(NotificationChannel("events", context.getString(R.string.push_channel_events), NotificationManager.IMPORTANCE_DEFAULT))
    manager.createNotificationChannel(NotificationChannel("announcements", context.getString(R.string.push_channel_announcements), NotificationManager.IMPORTANCE_DEFAULT))
}
fun handleNotificationIntent(intent: Intent?) {
    val id = intent?.getStringExtra("notificationId") ?: return
    val uid = intent.getStringExtra("recipientUid") ?: return
    PushSignals.opened(id, uid)
    intent.removeExtra("notificationId")
}
