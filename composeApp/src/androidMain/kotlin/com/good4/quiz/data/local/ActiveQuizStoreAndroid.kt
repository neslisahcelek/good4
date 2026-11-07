package com.good4.quiz.data.local

import android.content.Context

class ActiveQuizStoreAndroid(
    context: Context
) : ActiveQuizStore {

    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    override fun saveActiveSession(
        sessionId: String,
        role: String,
        title: String,
        savedAtEpochMillis: Long
    ) {
        prefs.edit()
            .putString(KEY_SESSION_ID, sessionId)
            .putString(KEY_ROLE, role)
            .putString(KEY_TITLE, title)
            .putLong(KEY_SAVED_AT, savedAtEpochMillis)
            .apply()
    }

    override fun getActiveSession(): ActiveQuizSession? {
        val sessionId = prefs.getString(KEY_SESSION_ID, null) ?: return null
        val role = prefs.getString(KEY_ROLE, "player") ?: "player"
        val title = prefs.getString(KEY_TITLE, "") ?: ""
        val savedAt = prefs.getLong(KEY_SAVED_AT, 0L)
        return ActiveQuizSession(
            sessionId = sessionId,
            role = role,
            title = title,
            savedAtEpochMillis = savedAt
        )
    }

    override fun clearActiveSession() {
        prefs.edit()
            .remove(KEY_SESSION_ID)
            .remove(KEY_ROLE)
            .remove(KEY_TITLE)
            .remove(KEY_SAVED_AT)
            .apply()
    }

    private companion object {
        const val PREF_NAME = "good4_active_quiz"
        const val KEY_SESSION_ID = "active_session_id"
        const val KEY_ROLE = "active_role"
        const val KEY_TITLE = "active_title"
        const val KEY_SAVED_AT = "active_saved_at"
    }
}
