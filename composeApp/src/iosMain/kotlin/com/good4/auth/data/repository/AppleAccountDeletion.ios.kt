package com.good4.auth.data.repository

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual suspend fun revokeAppleTokenIfNeeded() {
    val launcher = AppleTokenRevocationBridge.launcher
        ?: error("Apple hesap silme doğrulaması başlatılamadı.")
    suspendCancellableCoroutine<Unit> { continuation ->
        launcher.revokeIfNeeded(object : AppleTokenRevocationCallback {
            override fun complete(error: String?) {
                if (!continuation.isActive) return
                if (error == null) continuation.resume(Unit)
                else continuation.resumeWithException(IllegalStateException(error))
            }
        })
    }
}
