package com.good4.community

import platform.Foundation.NSUserDefaults

private const val BLOCKED_COMMUNITIES_KEY = "blocked_community_ids"

actual fun loadBlockedCommunityIds(): Set<String> =
    NSUserDefaults.standardUserDefaults.stringForKey(BLOCKED_COMMUNITIES_KEY)
        ?.split(',')
        ?.filter { it.isNotBlank() }
        ?.toSet()
        .orEmpty()

actual fun saveBlockedCommunityIds(ids: Set<String>) {
    // Firestore document IDs never contain commas, so a joined string is enough.
    NSUserDefaults.standardUserDefaults.setObject(ids.joinToString(","), BLOCKED_COMMUNITIES_KEY)
}
