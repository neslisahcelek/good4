package com.good4.notification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Device-local record of which notification IDs the student has already seen. */
expect fun loadSeenNotificationIds(): Set<String>

expect fun saveSeenNotificationIds(ids: Set<String>)

data class StudentNotification(
    /** Stable ID; changing it makes the notification unread again on every device. */
    val id: String,
    val title: String,
    val source: String,
    val date: String,
    val body: String
)

val studentNotifications = listOf(
    StudentNotification(
        id = "2026-09-11-community-days",
        title = "Topluluk günleri yaklaşıyor!",
        source = "Good4 Topluluklar",
        date = "11 Eyl 2026, 12:00",
        body = "Kampüsteki topluluklarla tanışmaya hazır mısın? Topluluk günleri çok yakında. Etkinlikleri keşfetmek ve favori topluluklarını takip etmek için Topluluklar sayfasına göz at."
    )
)

/** Drives the unread dot on the home bell; opening the notifications screen marks everything seen. */
object NotificationInbox {
    private val seenIds = MutableStateFlow(loadSeenNotificationIds())
    private val _hasUnseen = MutableStateFlow(unseenIds().isNotEmpty())
    val hasUnseen: StateFlow<Boolean> = _hasUnseen.asStateFlow()

    fun unseenIds(): Set<String> = studentNotifications.map { it.id }.toSet() - seenIds.value

    fun markAllSeen() {
        val all = seenIds.value + studentNotifications.map { it.id }
        if (all == seenIds.value) return
        seenIds.value = all
        saveSeenNotificationIds(all)
        _hasUnseen.value = false
    }
}
