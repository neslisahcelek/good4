package com.good4.core.network

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

actual suspend fun currentFirebaseIdToken(): String =
    Firebase.auth.currentUser?.getIdToken(false) ?: error("Giriş oturumu bulunamadı.")

/** Implemented by the native Firebase SDK so Firestore and callable requests share tokens. */
interface NativeAppCheckTokenProvider {
    fun fetchToken(callback: NativeAppCheckTokenCallback)
}

interface NativeAppCheckTokenCallback {
    fun complete(token: String?)
}

object NativeAppCheckBridge {
    var provider: NativeAppCheckTokenProvider? = null
}

actual suspend fun currentAppCheckToken(): String? {
    val provider = NativeAppCheckBridge.provider ?: return null
    // A failed attestation must not hold up a callable request indefinitely.
    return withTimeoutOrNull(10_000L) {
        suspendCancellableCoroutine { continuation ->
            provider.fetchToken(object : NativeAppCheckTokenCallback {
                override fun complete(token: String?) {
                    if (continuation.isActive) continuation.resume(token?.takeIf { it.isNotBlank() })
                }
            })
        }
    }
}
