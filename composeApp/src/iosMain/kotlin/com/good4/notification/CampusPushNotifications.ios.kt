package com.good4.notification

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual fun nativePushPlatform() = "ios"
actual fun refreshNativePushRegistration() { NativePushBridge.launcher?.refresh() }
actual fun requestNativePushPermission() { NativePushBridge.launcher?.requestPermission() }
actual suspend fun deleteNativePushToken() = suspendCancellableCoroutine<Unit> { continuation ->
    val launcher = NativePushBridge.launcher
    if (launcher == null) continuation.resume(Unit)
    else launcher.deleteToken(object : PushTokenDeletedCallback {
        override fun complete() { if (continuation.isActive) continuation.resume(Unit) }
    })
}
