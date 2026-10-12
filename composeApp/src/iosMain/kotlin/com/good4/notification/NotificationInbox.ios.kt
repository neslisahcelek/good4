package com.good4.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUUID
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

actual fun pushInstallation(): PushInstallation {
    val prefs = NSUserDefaults.standardUserDefaults
    val id = prefs.stringForKey("push_installation_id") ?: NSUUID().UUIDString.also { prefs.setObject(it, "push_installation_id") }
    val secret = prefs.stringForKey("push_installation_secret") ?: (NSUUID().UUIDString + NSUUID().UUIDString).also { prefs.setObject(it, "push_installation_secret") }
    return PushInstallation(id, secret, "ios")
}
actual fun notificationEducationShown() = NSUserDefaults.standardUserDefaults.boolForKey("push_education_shown")
actual fun saveNotificationEducationShown() { NSUserDefaults.standardUserDefaults.setBool(true, "push_education_shown") }
actual fun loadHomePermissionPromptHistory(): String? = NSUserDefaults.standardUserDefaults.stringForKey("home_permission_prompts_v1")
actual fun saveHomePermissionPromptHistory(value: String) {
    NSUserDefaults.standardUserDefaults.setObject(value, "home_permission_prompts_v1")
}
actual suspend fun notificationPermissionGranted(): Boolean = suspendCancellableCoroutine { continuation ->
    UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { settings ->
        val allowed = settings?.authorizationStatus in setOf(
            UNAuthorizationStatusAuthorized, UNAuthorizationStatusProvisional, UNAuthorizationStatusEphemeral
        )
        if (continuation.isActive) continuation.resume(allowed)
    }
}
actual suspend fun pushSnapshot(): PushSnapshot = suspendCancellableCoroutine { continuation ->
    val launcher = NativePushBridge.launcher
    if (launcher == null) { continuation.resume(PushSnapshot()); return@suspendCancellableCoroutine }
    launcher.snapshot(object : NativePushCallback {
        override fun complete(token: String?, permission: Boolean, error: String?) {
            if (continuation.isActive) continuation.resume(PushSnapshot(token.orEmpty(), permission))
        }
    })
}
actual suspend fun clearPushRegistration(): Boolean = suspendCancellableCoroutine { continuation ->
    val launcher = NativePushBridge.launcher
    if (launcher == null) { continuation.resume(false); return@suspendCancellableCoroutine }
    launcher.clearRegistration(object : NativePushCallback {
        override fun complete(token: String?, permission: Boolean, error: String?) {
            if (continuation.isActive) continuation.resume(error == null)
        }
    })
}
@Composable
actual fun rememberNotificationPermissionLauncher(): NotificationPermissionLauncher = remember {
    object : NotificationPermissionLauncher {
        override fun request(onComplete: () -> Unit) {
            NativePushBridge.launcher?.requestPermission(object : NativePushCallback {
                override fun complete(token: String?, permission: Boolean, error: String?) { PushSignals.refresh(); onComplete() }
            }) ?: onComplete()
        }
        override fun openSettings() { NativePushBridge.launcher?.openSettings() }
    }
}
