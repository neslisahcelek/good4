package com.good4.notification

import com.good4.auth.data.repository.AuthRepository
import com.good4.community.loadBlockedCommunityIds
import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.data.repository.NumericPageCursor
import com.good4.auth.domain.AuthUser
import kotlinx.coroutines.flow.Flow
import com.good4.core.domain.Result
import com.good4.core.network.callV2Function
import com.good4.core.util.AppEnvironment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.serialization.json.*

data class NotificationPage(val items: List<StudentNotification>, val nextCursor: NumericPageCursor?, val fetchedCount: Int)

const val NOTIFICATION_EXPIRY_SECONDS = 7 * 86_400L

interface NotificationDataSource {
    val currentUserId: String?
    val authStateFlow: Flow<AuthUser?>
    val supported: Boolean
    suspend fun inbox(cursor: NumericPageCursor? = null): NotificationPage
    suspend fun notification(id: String): StudentNotification?
    suspend fun preferences(): NotificationPreferencesDto
    suspend fun savePreferences(value: NotificationPreferencesDto)
    suspend fun enableAnnouncements()
    suspend fun markRead(ids: Set<String>)
    suspend fun delete(ids: Set<String>)
    suspend fun deleteAll()
    suspend fun syncDevice(): PushSnapshot
}

class NotificationRepository(private val store: FirestoreRepository, private val auth: AuthRepository) : NotificationDataSource {
    private var registeredPayload: JsonObject? = null
    private var registeredUid: String? = null
    private var registeredRevision = -1
    private var registeredAt = 0L
    override val currentUserId get() = auth.currentUser?.uid
    override val authStateFlow get() = auth.authStateFlow
    override val supported get() = AppEnvironment.firebaseProjectId == "good4tr-v2"

    override suspend fun inbox(cursor: NumericPageCursor?): NotificationPage {
        val uid = currentUserId ?: return NotificationPage(emptyList(), null, 0)
        if (!supported) return NotificationPage(emptyList(), null, 0)
        return when (val result = store.queryNumericPage(
            "users/$uid/notifications", "createdAt", NotificationDto::class, pageSize = 20, cursor = cursor
        )) {
            is Result.Success -> {
                val blocked = loadBlockedCommunityIds()
                val nowSeconds = Clock.System.now().epochSeconds
                val cutoff = nowSeconds - NOTIFICATION_EXPIRY_SECONDS
                val items = result.data.items.map { StudentNotification(it.id, it.data) }
                val validItems = items.filter { it.data.organizationId !in blocked && it.data.createdAt > cutoff }
                val expiredIds = items.filter { it.data.createdAt <= cutoff }.map { it.id }.toSet()
                if (expiredIds.isNotEmpty()) {
                    try { delete(expiredIds) } catch (_: Exception) {}
                }
                NotificationPage(
                    validItems,
                    if (items.any { it.data.createdAt <= cutoff }) null else result.data.nextCursor,
                    validItems.size
                )
            }
            is Result.Error -> error("notification_load_failed")
        }
    }
    override suspend fun notification(id: String): StudentNotification? {
        val uid = currentUserId ?: return null
        val nowSeconds = Clock.System.now().epochSeconds
        val cutoff = nowSeconds - NOTIFICATION_EXPIRY_SECONDS
        return when (val result = store.getDocument("users/$uid/notifications", id, NotificationDto::class)) {
            is Result.Success -> StudentNotification(id, result.data).takeIf {
                it.data.organizationId !in loadBlockedCommunityIds() && it.data.createdAt > cutoff
            }
            is Result.Error -> null
        }
    }
    override suspend fun preferences(): NotificationPreferencesDto {
        val uid = currentUserId ?: return NotificationPreferencesDto()
        if (!supported) return NotificationPreferencesDto()
        return when (val result = store.getDocument("users/$uid/notificationPreferences", "default", NotificationPreferencesDto::class)) {
            is Result.Success -> result.data
            is Result.Error -> if (result.error.message == "Document not found") NotificationPreferencesDto()
                else error("notification_preferences_load_failed")
        }
    }
    override suspend fun savePreferences(value: NotificationPreferencesDto) {
        callV2Function("updateNotificationPreferences", buildJsonObject {
            put("eventUpdates", value.eventUpdates); put("reminders", value.reminders); put("announcements", value.announcements)
        })
    }
    override suspend fun enableAnnouncements() {
        val uid = currentUserId ?: error("notification_auth_required")
        try {
            callV2Function("updateNotificationPreferences", buildJsonObject { put("announcements", true) })
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            // Earlier server releases require all three fields. Preserve the student's other
            // preferences, and retry only a definite schema rejection, never a transport failure.
            if (failure.message != "PREFERENCES_INVALID") throw failure
            val current = preferences()
            check(currentUserId == uid) { "notification_session_changed" }
            savePreferences(current.copy(announcements = true))
        }
    }
    override suspend fun markRead(ids: Set<String>) {
        if (!supported) return
        if (ids.isEmpty()) return
        callV2Function("markNotificationsRead", buildJsonObject {
            put("notificationIds", buildJsonArray { ids.forEach { add(it) } })
        })
    }
    override suspend fun delete(ids: Set<String>) {
        if (!supported || currentUserId == null) return
        if (ids.isEmpty()) return
        callV2Function("deleteNotifications", buildJsonObject {
            put("notificationIds", buildJsonArray { ids.forEach { add(it) } })
        })
    }
    override suspend fun deleteAll() {
        if (!supported || currentUserId == null) return
        callV2Function("deleteNotifications", buildJsonObject {
            put("all", true)
        })
    }
    override suspend fun syncDevice(): PushSnapshot = PushSession.mutex.withLock {
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
