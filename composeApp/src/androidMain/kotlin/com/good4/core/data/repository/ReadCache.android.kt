package com.good4.core.data.repository
actual fun firebaseCacheUserId(): String? = runCatching { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid }.getOrNull()
