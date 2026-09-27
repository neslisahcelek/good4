package com.good4.core.network

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth

actual suspend fun currentFirebaseIdToken(): String =
    Firebase.auth.currentUser?.getIdToken(false) ?: error("Giriş oturumu bulunamadı.")
