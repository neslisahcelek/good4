package com.good4.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
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
actual fun loadHomePermissionPromptHistory(): String? = preferences().getString("home_permission_prompts_v1", null)
actual fun saveHomePermissionPromptHistory(value: String) {
    check(preferences().edit().putString("home_permission_prompts_v1", value).commit())
}
actual suspend fun notificationPermissionGranted(): Boolean = androidNotificationPermissionGranted(context())
actual suspend fun pushSnapshot(): PushSnapshot {
    createNotificationChannels(context())
    val messaging = FirebaseMessaging.getInstance()
    messaging.isAutoInitEnabled = true
    return PushSnapshot(runCatching { messaging.token.await() }.getOrDefault(""), androidNotificationPermissionGranted(context()))
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
    var pendingComplete by remember { mutableStateOf<(() -> Unit)?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        PushSignals.refresh()
        val complete = pendingComplete
        pendingComplete = null
        complete?.invoke()
    }
    return remember(context, launcher) { object : NotificationPermissionLauncher {
        override fun request(onComplete: () -> Unit) {
            if (androidNotificationPermissionGranted(context)) { PushSignals.refresh(); onComplete(); return }
            pendingComplete = onComplete
            try {
                if (!requestAndroidNotificationPermission(context) { launcher.launch(it) }) {
                    pendingComplete = null
                    onComplete()
                }
            } catch (failure: Exception) {
                pendingComplete = null
                throw failure
            }
        }
        override fun openSettings() {
            openAndroidNotificationSettings(context)
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
