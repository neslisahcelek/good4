package com.good4.core.data.local

import android.content.Context
import com.good4.user.domain.UserRole

class StartupSessionCacheAndroid(
    context: Context
) : StartupSessionCache {

    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    override fun read(uid: String): StartupSession? {
        val roleValue = prefs.getString(key(uid, KEY_ROLE), null) ?: return null
        val role = UserRole.fromValue(roleValue)
        val isUserVerified = prefs.getBoolean(key(uid, KEY_USER_VERIFIED), false)
        val isAuthEmailVerified = prefs.getBoolean(key(uid, KEY_AUTH_EMAIL_VERIFIED), false)
        val updatedAt = prefs.getLong(key(uid, KEY_UPDATED_AT), 0L)
        return StartupSession(
            uid = uid,
            role = role,
            isUserVerified = isUserVerified,
            isAuthEmailVerified = isAuthEmailVerified,
            updatedAtEpochMillis = updatedAt
        )
    }

    override fun write(
        uid: String,
        role: UserRole,
        isUserVerified: Boolean,
        isAuthEmailVerified: Boolean,
        updatedAtEpochMillis: Long
    ) {
        prefs.edit()
            .putString(key(uid, KEY_ROLE), role.value)
            .putBoolean(key(uid, KEY_USER_VERIFIED), isUserVerified)
            .putBoolean(key(uid, KEY_AUTH_EMAIL_VERIFIED), isAuthEmailVerified)
            .putLong(key(uid, KEY_UPDATED_AT), updatedAtEpochMillis)
            .apply()
    }

    override fun clear(uid: String?) {
        if (uid != null) {
            prefs.edit()
                .remove(key(uid, KEY_ROLE))
                .remove(key(uid, KEY_USER_VERIFIED))
                .remove(key(uid, KEY_AUTH_EMAIL_VERIFIED))
                .remove(key(uid, KEY_UPDATED_AT))
                .apply()
            return
        }

        val keysToRemove = prefs.all.keys.filter { it.startsWith(KEY_PREFIX) }
        if (keysToRemove.isEmpty()) return
        prefs.edit().apply {
            keysToRemove.forEach(::remove)
            apply()
        }
    }

    private fun key(uid: String, suffix: String): String = "$KEY_PREFIX$uid.$suffix"

    private companion object {
        private const val PREF_NAME = "good4_startup_session_cache"
        private const val KEY_PREFIX = "startup_session."
        private const val KEY_ROLE = "role"
        private const val KEY_USER_VERIFIED = "user_verified"
        private const val KEY_AUTH_EMAIL_VERIFIED = "auth_email_verified"
        private const val KEY_UPDATED_AT = "updated_at"
    }
}
