package com.good4.core.data.local

import com.good4.user.domain.UserRole
import platform.Foundation.NSUserDefaults

class StartupSessionCacheIOS : StartupSessionCache {

    private val defaults = NSUserDefaults.standardUserDefaults

    override fun read(uid: String): StartupSession? {
        val roleValue = defaults.stringForKey(key(uid, KEY_ROLE)) ?: return null
        val role = UserRole.fromValue(roleValue)
        val isUserVerified = defaults.boolForKey(key(uid, KEY_USER_VERIFIED))
        val isAuthEmailVerified = defaults.boolForKey(key(uid, KEY_AUTH_EMAIL_VERIFIED))
        val updatedAt = defaults.stringForKey(key(uid, KEY_UPDATED_AT))?.toLongOrNull() ?: 0L
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
        defaults.setObject(role.value, key(uid, KEY_ROLE))
        defaults.setBool(isUserVerified, key(uid, KEY_USER_VERIFIED))
        defaults.setBool(isAuthEmailVerified, key(uid, KEY_AUTH_EMAIL_VERIFIED))
        defaults.setObject(updatedAtEpochMillis.toString(), key(uid, KEY_UPDATED_AT))
    }

    override fun clear(uid: String?) {
        if (uid != null) {
            defaults.removeObjectForKey(key(uid, KEY_ROLE))
            defaults.removeObjectForKey(key(uid, KEY_USER_VERIFIED))
            defaults.removeObjectForKey(key(uid, KEY_AUTH_EMAIL_VERIFIED))
            defaults.removeObjectForKey(key(uid, KEY_UPDATED_AT))
            return
        }

        val dictionary = defaults.dictionaryRepresentation()
        val allKeys = dictionary.keys.mapNotNull { it as? String }
        allKeys
            .filter { it.startsWith(KEY_PREFIX) }
            .forEach { defaults.removeObjectForKey(it) }
    }

    private fun key(uid: String, suffix: String): String = "$KEY_PREFIX$uid.$suffix"

    private companion object {
        private const val KEY_PREFIX = "startup_session."
        private const val KEY_ROLE = "role"
        private const val KEY_USER_VERIFIED = "user_verified"
        private const val KEY_AUTH_EMAIL_VERIFIED = "auth_email_verified"
        private const val KEY_UPDATED_AT = "updated_at"
    }
}
