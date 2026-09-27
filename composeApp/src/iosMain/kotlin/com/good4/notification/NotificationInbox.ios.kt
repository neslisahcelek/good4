package com.good4.notification

import platform.Foundation.NSUserDefaults

private const val SEEN_NOTIFICATIONS_KEY = "seen_notification_ids"

actual fun loadSeenNotificationIds(): Set<String> =
    NSUserDefaults.standardUserDefaults.stringForKey(SEEN_NOTIFICATIONS_KEY)
        ?.split(',')
        ?.filter { it.isNotBlank() }
        ?.toSet()
        .orEmpty()

actual fun saveSeenNotificationIds(ids: Set<String>) {
    // Notification IDs are plain slugs without commas, so a joined string is enough.
    NSUserDefaults.standardUserDefaults.setObject(ids.joinToString(","), SEEN_NOTIFICATIONS_KEY)
}
