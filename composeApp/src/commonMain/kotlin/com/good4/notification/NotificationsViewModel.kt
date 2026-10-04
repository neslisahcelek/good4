package com.good4.notification

import com.good4.core.data.repository.NumericPageCursor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.core.presentation.UiText
import good4.composeapp.generated.resources.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsState(
    val notifications: List<StudentNotification> = emptyList(),
    val preferences: NotificationPreferencesDto = NotificationPreferencesDto(),
    val loading: Boolean = false, val saving: Boolean = false, val permission: Boolean = false,
    val supported: Boolean = true, val error: UiText? = null,
    val loadingMore: Boolean = false, val nextCursor: NumericPageCursor? = null,
    val pageError: UiText? = null,
    val selected: StudentNotification? = null
)
class NotificationsViewModel(private val repository: NotificationDataSource) : ViewModel() {
    private val mutable = MutableStateFlow(NotificationsState(supported = repository.supported))
    val state = mutable.asStateFlow()
    private var foreground = false
    private var refreshJob: Job? = null
    private var sessionUid: String? = null
    private var pageJob: Job? = null
    private var generation = 0
    private var fetchedCount = 0
    init {
        viewModelScope.launch {
            repository.authStateFlow.collect { user ->
                if (sessionUid != user?.uid) {
                    sessionUid = user?.uid
                    generation++
                    fetchedCount = 0
                    refreshJob?.cancel()
                    pageJob?.cancel()
                    PushSession.detaching = false
                    mutable.update { NotificationsState(supported = repository.supported) }
                    NotificationInbox.update(false)
                    if (foreground) refresh()
                }
            }
        }
        viewModelScope.launch { PushSignals.revision.collect { if (foreground) refresh() } }
        viewModelScope.launch { PushSignals.received.collect { if (foreground) receive(it) } }
    }
    fun setForeground(value: Boolean) { foreground = value; if (value) refresh() }
    fun refresh() {
        if (!repository.supported || repository.currentUserId == null || PushSession.detaching) return
        if (refreshJob?.isActive == true) return
        val uid = repository.currentUserId
        val requestGeneration = ++generation
        pageJob?.cancel()
        refreshJob = viewModelScope.launch {
            mutable.update { it.copy(loading = true, loadingMore = false, pageError = null, error = null) }
            try {
                val page = repository.inbox()
                val notifications = page.items
                val preferences = repository.preferences()
                // The inbox remains usable even if APNs/FCM registration is temporarily unavailable.
                val permission = try { repository.syncDevice().permission } catch (e: CancellationException) { throw e } catch (_: Exception) { mutable.value.permission }
                if (repository.currentUserId == uid && generation == requestGeneration) {
                    fetchedCount = page.fetchedCount
                    mutable.update { it.copy(notifications = preserveRead(notifications, it.notifications), preferences = preferences,
                        permission = permission, loading = false, nextCursor = page.nextCursor) }
                    NotificationInbox.update(mutable.value.notifications.any { it.data.readAt == 0L })
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                if (repository.currentUserId == uid && generation == requestGeneration) mutable.update { it.copy(loading = false, error = UiText.StringResourceId(Res.string.notification_load_failed)) }
            }
        }
    }
    fun loadMore() {
        val cursor = mutable.value.nextCursor ?: return
        if (mutable.value.loading || pageJob?.isActive == true || fetchedCount >= 100 || PushSession.detaching) return
        val uid = repository.currentUserId ?: return
        val requestGeneration = generation
        pageJob = viewModelScope.launch {
            mutable.update { it.copy(loadingMore = true, pageError = null) }
            try {
                val page = repository.inbox(cursor)
                if (repository.currentUserId == uid && generation == requestGeneration) {
                    fetchedCount += page.fetchedCount
                    mutable.update { it.copy(notifications = merge(it.notifications, page.items), loadingMore = false,
                        nextCursor = page.nextCursor.takeIf { fetchedCount < 100 }) }
                    NotificationInbox.update(mutable.value.notifications.any { it.data.readAt == 0L })
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                if (repository.currentUserId == uid && generation == requestGeneration) mutable.update {
                    it.copy(loadingMore = false, pageError = UiText.StringResourceId(Res.string.notification_load_failed))
                }
            }
        }
    }
    private suspend fun receive(intent: NotificationOpen) {
        if (intent.recipientUid != repository.currentUserId || PushSession.detaching) return
        // Finish the first page before inserting a push, so it cannot overwrite the new item.
        refreshJob?.join()
        val requestGeneration = generation
        try {
            val notification = repository.notification(intent.notificationId) ?: return
            if (intent.recipientUid == repository.currentUserId && generation == requestGeneration) {
                mutable.update { it.copy(notifications = merge(it.notifications, listOf(notification))) }
                NotificationInbox.update(mutable.value.notifications.any { it.data.readAt == 0L })
            }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) {
            if (intent.recipientUid == repository.currentUserId) mutable.update {
                it.copy(error = UiText.StringResourceId(Res.string.notification_load_failed))
            }
        }
    }
    private fun preserveRead(items: List<StudentNotification>, previous: List<StudentNotification>): List<StudentNotification> {
        val read = previous.filter { it.data.readAt != 0L }.associateBy { it.id }
        return items.map { item -> read[item.id]?.let { item.copy(data = item.data.copy(readAt = it.data.readAt)) } ?: item }
    }
    private fun merge(previous: List<StudentNotification>, incoming: List<StudentNotification>): List<StudentNotification> =
        (preserveRead(incoming, previous) + previous).distinctBy { it.id }
            .sortedWith(compareByDescending<StudentNotification> { it.data.createdAt }.thenByDescending { it.id }).take(100)

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
        val capturedIds = if (id == null) mutable.value.notifications.filter { it.data.readAt == 0L }.map { it.id }.toSet() else setOf(id)
        try {
            repository.markRead(capturedIds)
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
