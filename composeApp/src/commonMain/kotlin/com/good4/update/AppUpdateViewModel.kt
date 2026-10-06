package com.good4.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.config.data.repository.AppConfigRepository
import com.good4.core.presentation.UiText
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.app_update_title
import good4.composeapp.generated.resources.app_update_body
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

private const val REMINDER_GAP_MS = 7 * 24 * 60 * 60 * 1000L

data class AppUpdateState(
    val status: UpdateStatus = UpdateStatus.NONE,
    val snoozed: Boolean = false,
    val busy: Boolean = false,
    val error: Boolean = false,
    val title: UiText = UiText.StringResourceId(Res.string.app_update_title),
    val message: UiText = UiText.StringResourceId(Res.string.app_update_body)
)

class AppUpdateViewModel(private val configRepository: AppConfigRepository) : ViewModel() {
    private val _state = MutableStateFlow(AppUpdateState())
    val state = _state.asStateFlow()
    private var service: AppUpdateService? = null
    private var observation: Job? = null
    private var declineObservation: Job? = null
    private var checkJob: Job? = null
    private var actionJob: Job? = null
    private var lastCheckedAt = 0L

    fun attach(value: AppUpdateService) {
        if (service === value) return
        detach()
        service = value
        lastCheckedAt = 0L
        observation = viewModelScope.launch {
            value.status.collect { status ->
                _state.update { it.copy(status = status, error = status == UpdateStatus.FAILED) }
            }
        }
        declineObservation = viewModelScope.launch {
            value.declinedUpdates.collect { later() }
        }
    }

    fun detach() {
        observation?.cancel()
        declineObservation?.cancel()
        checkJob?.cancel()
        actionJob?.cancel()
        service = null
        _state.update { it.copy(busy = false) }
    }

    fun refresh() {
        val current = service ?: return
        if (checkJob?.isActive == true || actionJob?.isActive == true) return
        val reminderAt = loadUpdateReminderAt()
        val now = Clock.System.now().toEpochMilliseconds()
        _state.update { it.copy(snoozed = now < reminderAt) }
        // Opening/closing menus or returning between tabs should not hammer the store.
        if (now - lastCheckedAt in 0 until 60_000L) return
        lastCheckedAt = now
        checkJob = viewModelScope.launch {
            try {
                current.check()
                if (current.status.value == UpdateStatus.AVAILABLE ||
                    current.status.value == UpdateStatus.DOWNLOADING ||
                    current.status.value == UpdateStatus.READY) {
                    val notice = configRepository.getUpdateNotice()
                    // Each optional field falls back independently; remote copy cannot inflate the card indefinitely.
                    val title = notice?.title?.trim()?.takeIf { it.isNotBlank() && it.length <= 120 }
                    val message = notice?.message?.trim()?.takeIf { it.isNotBlank() && it.length <= 400 }
                    _state.update {
                        it.copy(
                            title = title?.let { text -> UiText.DynamicString(text) }
                                ?: UiText.StringResourceId(Res.string.app_update_title),
                            message = message?.let { text -> UiText.DynamicString(text) }
                                ?: UiText.StringResourceId(Res.string.app_update_body)
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // A background store lookup must never interrupt normal app use.
            }
        }
    }

    fun later() {
        saveUpdateReminderAt(Clock.System.now().toEpochMilliseconds() + REMINDER_GAP_MS)
        _state.update { it.copy(snoozed = true, error = false) }
    }

    fun update() {
        val current = service ?: return
        if (actionJob?.isActive == true) return
        checkJob?.cancel()
        val ready = _state.value.status == UpdateStatus.READY
        _state.update { it.copy(busy = true, error = false) }
        actionJob = viewModelScope.launch {
            try {
                if (ready) current.complete()
                else if (!current.start() || current.snoozeAfterStart) later()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(error = true) }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }
}
