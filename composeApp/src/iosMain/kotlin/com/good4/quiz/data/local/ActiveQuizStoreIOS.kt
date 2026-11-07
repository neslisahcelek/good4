package com.good4.quiz.data.local

import platform.Foundation.NSUserDefaults

class ActiveQuizStoreIOS : ActiveQuizStore {

    private val defaults = NSUserDefaults.standardUserDefaults

    override fun saveActiveSession(
        sessionId: String,
        role: String,
        title: String,
        savedAtEpochMillis: Long
    ) {
        defaults.setObject(sessionId, KEY_SESSION_ID)
        defaults.setObject(role, KEY_ROLE)
        defaults.setObject(title, KEY_TITLE)
        defaults.setObject(savedAtEpochMillis.toString(), KEY_SAVED_AT)
    }

    override fun getActiveSession(): ActiveQuizSession? {
        val sessionId = defaults.stringForKey(KEY_SESSION_ID) ?: return null
        val role = defaults.stringForKey(KEY_ROLE) ?: "player"
        val title = defaults.stringForKey(KEY_TITLE) ?: ""
        val savedAt = defaults.stringForKey(KEY_SAVED_AT)?.toLongOrNull() ?: 0L
        return ActiveQuizSession(
            sessionId = sessionId,
            role = role,
            title = title,
            savedAtEpochMillis = savedAt
        )
    }

    override fun clearActiveSession() {
        defaults.removeObjectForKey(KEY_SESSION_ID)
        defaults.removeObjectForKey(KEY_ROLE)
        defaults.removeObjectForKey(KEY_TITLE)
        defaults.removeObjectForKey(KEY_SAVED_AT)
    }

    private companion object {
        const val KEY_SESSION_ID = "active_quiz_session_id"
        const val KEY_ROLE = "active_quiz_role"
        const val KEY_TITLE = "active_quiz_title"
        const val KEY_SAVED_AT = "active_quiz_saved_at"
    }
}
