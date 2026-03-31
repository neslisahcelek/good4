package com.good4.core.data.local

import com.good4.core.util.AppEnvironment
import com.good4.user.domain.UserRole
import kotlinx.datetime.Clock

data class StartupSession(
    val uid: String,
    val role: UserRole,
    val isUserVerified: Boolean,
    val isAuthEmailVerified: Boolean,
    val updatedAtEpochMillis: Long
) {
    fun shouldOpenEmailVerification(): Boolean {
        val requiresVerification = shouldCheckEmailVerificationFor(role)
        return requiresVerification && (!isUserVerified || !isAuthEmailVerified)
    }
}

interface StartupSessionCache {
    fun read(uid: String): StartupSession?
    fun write(
        uid: String,
        role: UserRole,
        isUserVerified: Boolean,
        isAuthEmailVerified: Boolean,
        updatedAtEpochMillis: Long
    )

    fun clear(uid: String? = null)
}

fun shouldCheckEmailVerificationFor(role: UserRole): Boolean {
    return AppEnvironment.isEmailVerificationRequired && role.requiresEmailVerification()
}

fun UserRole.requiresEmailVerification(): Boolean {
    return this == UserRole.STUDENT || this == UserRole.SUPPORTER
}

fun StartupSessionCache.cacheStartupSession(
    uid: String,
    role: UserRole,
    isUserVerified: Boolean,
    isAuthEmailVerified: Boolean,
    updatedAtEpochMillis: Long = Clock.System.now().toEpochMilliseconds()
) {
    write(
        uid = uid,
        role = role,
        isUserVerified = isUserVerified,
        isAuthEmailVerified = isAuthEmailVerified,
        updatedAtEpochMillis = updatedAtEpochMillis
    )
}
