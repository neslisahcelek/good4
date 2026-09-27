package com.good4.core.network

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

actual suspend fun currentFirebaseIdToken(): String =
    FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
        ?: error("Giriş oturumu bulunamadı.")
