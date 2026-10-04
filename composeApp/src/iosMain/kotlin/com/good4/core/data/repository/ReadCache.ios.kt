package com.good4.core.data.repository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
actual fun firebaseCacheUserId(): String? = runCatching { Firebase.auth.currentUser?.uid }.getOrNull()
