package com.good4.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.core.presentation.UiText
import good4.composeapp.generated.resources.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsState(
    val notifications: List<StudentNotification> = emptyList(),
    val preferences: NotificationPreferencesDto = NotificationPreferencesDto(),
    val loading: Boolean = false, val saving: Boolean = false, val permission: Boolean = false,
    val supported: Boolean = true, val error: UiText? = null,
    val selected: StudentNotification? = null
)
class NotificationsViewModel(private val repository: NotificationRepository) : ViewModel() {
    private val mutable = MutableStateFlow(NotificationsState(supported = repository.supported))
    val state = mutable.asStateFlow()
    private var foreground = false
    private var refreshJob: Job? = null
    private var sessionUid: String? = null
    init {
        viewModelScope.launch {
            repository.authStateFlow.collect { user ->
                if (sessionUid != user?.uid) {
                    sessionUid = user?.uid
                    refreshJob?.cancel()
                    PushSession.detaching = false
                    mutable.update { NotificationsState(supported = repository.supported) }
                    NotificationInbox.update(false)
                    if (foreground) refresh()
                }
            }
        }
        viewModelScope.launch { PushSignals.revision.collect { if (foreground) refresh() } }
        viewModelScope.launch {
            while (true) { delay(30_000); if (foreground) refresh() }
        }
    }
    fun setForeground(value: Boolean) { foreground = value; if (value) refresh() }
    fun refresh() {
        if (!repository.supported || repository.currentUserId == null || PushSession.detaching) return
        if (refreshJob?.isActive == true) return
        val uid = repository.currentUserId
        refreshJob = viewModelScope.launch {
            mutable.update { it.copy(loading = it.notifications.isEmpty(), error = null) }
            try {
                val notifications = repository.inbox()
                val preferences = repository.preferences()
                // The inbox remains usable even if APNs/FCM registration is temporarily unavailable.
                val permission = try { repository.syncDevice().permission } catch (e: CancellationException) { throw e } catch (_: Exception) { mutable.value.permission }
                if (repository.currentUserId == uid) {
                    mutable.update { it.copy(notifications = notifications, preferences = preferences, permission = permission, loading = false) }
                    NotificationInbox.update(notifications.any { it.data.readAt == 0L })
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                if (repository.currentUserId == uid) mutable.update { it.copy(loading = false, error = UiText.StringResourceId(Res.string.notification_load_failed)) }
            }
        }
    }
    fun select(notification: StudentNotification) {
        mutable.update { it.copy(selected = notification) }
        read(notification.id)
    }
    fun closeDetail() { mutable.update { it.copy(selected = null) } }
    fun openPending(intent: NotificationOpen, onReady: (StudentNotification) -> Unit) {
        if (intent.recipientUid != repository.currentUserId) { PushSignals.consumeOpen(); return }
        viewModelScope.launch {
            try {
                val notification = repository.notification(intent.notificationId)
                if (intent.recipientUid != repository.currentUserId) return@launch
                notification?.let { select(it); onReady(it) }
                    ?: run { mutable.update { it.copy(error = UiText.StringResourceId(Res.string.notification_unavailable)) }; onReady(StudentNotification(intent.notificationId, NotificationDto())) }
                PushSignals.consumeOpen()
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutable.update { it.copy(error = UiText.StringResourceId(Res.string.notification_load_failed)) } }
        }
    }
    fun read(id: String? = null) = viewModelScope.launch {
        val uid = repository.currentUserId
        val capturedIds = if (id == null) mutable.value.notifications.map { it.id }.toSet() else setOf(id)
        try {
            repository.markRead(id)
            if (repository.currentUserId == uid) {
                mutable.update { it.copy(notifications = it.notifications.map { item ->
                    if (item.id in capturedIds) item.copy(data = item.data.copy(readAt = 1)) else item
                }) }
                NotificationInbox.update(mutable.value.notifications.any { it.data.readAt == 0L })
            }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { if (repository.currentUserId == uid) mutable.update { it.copy(error = UiText.StringResourceId(Res.string.notification_save_failed)) } }
    }
    fun savePreferences(value: NotificationPreferencesDto) {
        if (mutable.value.saving) return
        val uid = repository.currentUserId
        viewModelScope.launch {
            mutable.update { it.copy(saving = true, error = null) }
            try {
                repository.savePreferences(value)
                if (repository.currentUserId == uid) mutable.update { it.copy(preferences = value, saving = false) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (repository.currentUserId == uid) mutable.update { it.copy(saving = false, error = UiText.StringResourceId(Res.string.notification_save_failed)) } }
        }
    }
}
