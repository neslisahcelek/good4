package com.good4.community

import android.content.Context
import org.koin.core.context.GlobalContext

private const val PREFERENCES_NAME = "good4_preferences"
private const val BLOCKED_COMMUNITIES_KEY = "blocked_community_ids"

actual fun loadBlockedCommunityIds(): Set<String> = runCatching {
    GlobalContext.get().get<Context>()
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        .getStringSet(BLOCKED_COMMUNITIES_KEY, emptySet())
        .orEmpty()
        .toSet()
}.getOrDefault(emptySet())

actual fun saveBlockedCommunityIds(ids: Set<String>) {
    runCatching {
        GlobalContext.get().get<Context>()
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(BLOCKED_COMMUNITIES_KEY, ids)
            .apply()
    }
}
