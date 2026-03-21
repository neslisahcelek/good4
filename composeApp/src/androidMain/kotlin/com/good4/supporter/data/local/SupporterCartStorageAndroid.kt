package com.good4.supporter.data.local

import android.content.Context
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class SupporterCartStorageAndroid(
    context: Context
) : SupporterCartStorage {

    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun loadItems(userId: String): List<SupporterCartStoredItem> {
        val raw = prefs.getString(key(userId), null) ?: return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(SupporterCartStoredItem.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    override suspend fun saveItems(userId: String, items: List<SupporterCartStoredItem>) {
        val encoded = json.encodeToString(ListSerializer(SupporterCartStoredItem.serializer()), items)
        prefs.edit().putString(key(userId), encoded).apply()
    }

    override suspend fun clear(userId: String) {
        prefs.edit().remove(key(userId)).apply()
    }

    private fun key(userId: String): String = "$KEY_PREFIX$userId"

    private companion object {
        private const val PREF_NAME = "good4_supporter_cart"
        private const val KEY_PREFIX = "supporter_cart_"
    }
}
