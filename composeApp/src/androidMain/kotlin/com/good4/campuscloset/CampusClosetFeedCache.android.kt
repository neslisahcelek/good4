package com.good4.campuscloset

import android.content.Context
import org.koin.core.context.GlobalContext

private const val FEED_KEY = "feed"

private fun feedPreferences() = GlobalContext.get().get<Context>()
    .getSharedPreferences("good4_campus_closet", Context.MODE_PRIVATE)

actual fun loadCampusClosetFeedCache(): String? = runCatching { feedPreferences().getString(FEED_KEY, null) }.getOrNull()

actual fun saveCampusClosetFeedCache(value: String?) {
    runCatching {
        feedPreferences().edit().apply { if (value == null) remove(FEED_KEY) else putString(FEED_KEY, value) }.apply()
    }
}
