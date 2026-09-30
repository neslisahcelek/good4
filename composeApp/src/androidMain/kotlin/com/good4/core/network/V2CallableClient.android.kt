package com.good4.core.network

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

actual suspend fun currentFirebaseIdToken(): String =
    FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
        ?: error("Giriş oturumu bulunamadı.")

actual suspend fun currentAppCheckToken(): String? = runCatching {
    FirebaseAppCheck.getInstance().getAppCheckToken(false).await()?.token
}.getOrNull()

