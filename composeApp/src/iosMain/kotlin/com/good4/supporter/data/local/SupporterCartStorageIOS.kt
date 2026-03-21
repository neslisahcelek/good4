package com.good4.supporter.data.local

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

class SupporterCartStorageIOS : SupporterCartStorage {

    private val defaults = NSUserDefaults.standardUserDefaults

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun loadItems(userId: String): List<SupporterCartStoredItem> {
        val raw = defaults.stringForKey(key(userId)) ?: return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(SupporterCartStoredItem.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    override suspend fun saveItems(userId: String, items: List<SupporterCartStoredItem>) {
        val encoded = json.encodeToString(ListSerializer(SupporterCartStoredItem.serializer()), items)
        defaults.setObject(encoded, key(userId))
    }

    override suspend fun clear(userId: String) {
        defaults.removeObjectForKey(key(userId))
    }

    private fun key(userId: String): String = "$KEY_PREFIX$userId"

    private companion object {
        private const val KEY_PREFIX = "supporter_cart_"
    }
}
