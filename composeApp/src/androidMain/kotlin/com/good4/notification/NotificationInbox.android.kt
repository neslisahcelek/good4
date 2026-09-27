package com.good4.notification

import android.content.Context
import org.koin.core.context.GlobalContext

private const val PREFERENCES_NAME = "good4_preferences"
private const val SEEN_NOTIFICATIONS_KEY = "seen_notification_ids"

actual fun loadSeenNotificationIds(): Set<String> = runCatching {
    GlobalContext.get().get<Context>()
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        .getStringSet(SEEN_NOTIFICATIONS_KEY, emptySet())
        .orEmpty()
        .toSet()
}.getOrDefault(emptySet())

actual fun saveSeenNotificationIds(ids: Set<String>) {
    runCatching {
        GlobalContext.get().get<Context>()
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(SEEN_NOTIFICATIONS_KEY, ids)
            .apply()
    }
}
