package com.good4.notification

import com.good4.auth.data.repository.AuthRepository
import com.good4.community.loadBlockedCommunityIds
import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.domain.Result
import com.good4.core.network.callV2Function
import com.good4.core.util.AppEnvironment
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.serialization.json.*

class NotificationRepository(private val store: FirestoreRepository, private val auth: AuthRepository) {
    private var registeredPayload: JsonObject? = null
    private var registeredUid: String? = null
    private var registeredRevision = -1
    private var registeredAt = 0L
    val currentUserId get() = auth.currentUser?.uid
    val authStateFlow get() = auth.authStateFlow
    val supported get() = AppEnvironment.firebaseProjectId == "good4tr-v2"

    suspend fun inbox(): List<StudentNotification> {
        val uid = currentUserId ?: return emptyList()
        if (!supported) return emptyList()
        return when (val result = store.queryCollectionWithMultipleConditionsAndLimit(
            "users/$uid/notifications", emptyMap(), "createdAt", true, 100, NotificationDto::class
        )) {
            is Result.Success -> result.data.map { StudentNotification(it.id, it.data) }
                .filter { it.data.organizationId !in loadBlockedCommunityIds() }
            is Result.Error -> error("notification_load_failed")
        }
    }
    suspend fun notification(id: String): StudentNotification? {
        val uid = currentUserId ?: return null
        return when (val result = store.getDocument("users/$uid/notifications", id, NotificationDto::class)) {
            is Result.Success -> StudentNotification(id, result.data).takeIf { it.data.organizationId !in loadBlockedCommunityIds() }
            is Result.Error -> null
        }
    }
    suspend fun preferences(): NotificationPreferencesDto {
        val uid = currentUserId ?: return NotificationPreferencesDto()
        if (!supported) return NotificationPreferencesDto()
        return when (val result = store.getDocument("users/$uid/notificationPreferences", "default", NotificationPreferencesDto::class)) {
            is Result.Success -> result.data
            is Result.Error -> NotificationPreferencesDto()
        }
    }
    suspend fun savePreferences(value: NotificationPreferencesDto) {
        callV2Function("updateNotificationPreferences", buildJsonObject {
            put("eventUpdates", value.eventUpdates); put("reminders", value.reminders); put("announcements", value.announcements)
        })
    }
    suspend fun markRead(id: String? = null) {
        if (!supported) return
        callV2Function("markNotificationsRead", buildJsonObject { if (id == null) put("all", true) else put("notificationId", id) })
    }
    suspend fun syncDevice(): PushSnapshot = PushSession.mutex.withLock {
        if (!supported || currentUserId == null || PushSession.detaching) return@withLock PushSnapshot()
        val uid = currentUserId
        val installation = pushInstallation()
        val snapshot = pushSnapshot()
        if (currentUserId != uid || PushSession.detaching) return@withLock snapshot
        val payload = buildJsonObject {
            put("installationId", installation.id); put("secret", installation.secret)
            put("platform", installation.platform); put("token", snapshot.token); put("permission", snapshot.permission)
            put("blockedCommunityIds", buildJsonArray { loadBlockedCommunityIds().sorted().forEach { add(it) } })
        }
        val now = Clock.System.now().epochSeconds
        val revision = PushSession.registrationRevision
        // Refresh unchanged endpoints daily; token, account, permission and block changes sync immediately.
        if (payload != registeredPayload || uid != registeredUid || registeredRevision != revision || now - registeredAt !in 0L until 86_400L) {
            callV2Function("registerPushDevice", payload)
            registeredPayload = payload
            registeredUid = uid
            registeredRevision = revision
            registeredAt = now
        }
        snapshot
    }
}
