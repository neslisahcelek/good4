package com.good4.notification

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import com.good4.core.network.callV2Function
import com.good4.core.util.AppEnvironment
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class NotificationDto(
    val kind: String = "", val title: String = "", val body: String = "",
    val organizationId: String = "", val eventId: String = "",
    val createdAt: Long = 0, val readAt: Long = 0
)
data class StudentNotification(val id: String, val data: NotificationDto)
@Serializable
data class NotificationPreferencesDto(
    val eventUpdates: Boolean = true, val reminders: Boolean = true, val announcements: Boolean = false
)
data class PushInstallation(val id: String, val secret: String, val platform: String)
data class PushSnapshot(val token: String = "", val permission: Boolean = false)
data class NotificationOpen(val notificationId: String, val recipientUid: String)

expect fun pushInstallation(): PushInstallation
expect suspend fun pushSnapshot(): PushSnapshot
expect suspend fun clearPushRegistration(): Boolean
expect fun notificationEducationShown(): Boolean
expect fun saveNotificationEducationShown()
interface NotificationPermissionLauncher {
    fun request(onComplete: () -> Unit)
    fun openSettings()
}
@Composable expect fun rememberNotificationPermissionLauncher(): NotificationPermissionLauncher

interface NativePushCallback { fun complete(token: String?, permission: Boolean, error: String?) }
interface NativePushLauncher {
    fun snapshot(callback: NativePushCallback)
    fun requestPermission(callback: NativePushCallback)
    fun clearRegistration(callback: NativePushCallback)
    fun openSettings()
}
object NativePushBridge { var launcher: NativePushLauncher? = null }

/** Native callbacks only signal refresh/open intents; all UI state belongs to the ViewModel. */
object PushSignals {
    private val _revision = MutableStateFlow(0)
    val revision = _revision.asStateFlow()
    private val _pendingOpen = MutableStateFlow<NotificationOpen?>(null)
    val pendingOpen = _pendingOpen.asStateFlow()
    private val _education = MutableStateFlow(false)
    val education = _education.asStateFlow()
    fun refresh() { _revision.update { it + 1 } }
    fun opened(notificationId: String, recipientUid: String) {
        if (notificationId.matches(Regex("[A-Za-z0-9_-]{1,256}")) && recipientUid.isNotBlank()) {
            _pendingOpen.update { NotificationOpen(notificationId, recipientUid) }
        }
    }
    fun consumeOpen() { _pendingOpen.update { null } }
    fun suggestPermission() {
        if (AppEnvironment.firebaseProjectId == "good4tr-v2" && !notificationEducationShown()) _education.update { true }
    }
    fun dismissEducation() { saveNotificationEducationShown(); _education.update { false } }
}
object NotificationInbox {
    private val _hasUnseen = MutableStateFlow(false)
    val hasUnseen = _hasUnseen.asStateFlow()
    internal fun update(unread: Boolean) { _hasUnseen.update { unread } }
}

/** Serializes device registration with logout so an in-flight refresh cannot reattach a signed-out account. */
object PushSession {
    val mutex = Mutex()
    var detaching = false
    var registrationRevision = 0
        private set
    suspend fun detach() {
        if (AppEnvironment.firebaseProjectId != "good4tr-v2") return
        detaching = true
        registrationRevision += 1
        try {
            mutex.withLock {
                val installation = pushInstallation()
                val removed = runCatching { callV2Function("unregisterPushDevice", buildJsonObject {
                    put("installationId", installation.id); put("secret", installation.secret)
                }) }.isSuccess
                val invalidated = runCatching { clearPushRegistration() }.getOrDefault(false)
                // If both operations fail, preserve the session rather than leak notifications to a shared device.
                check(removed || invalidated)
                NotificationInbox.update(false)
                PushSignals.consumeOpen()
            }
        } catch (error: Exception) {
            detaching = false
            throw error
        }
    }
}
