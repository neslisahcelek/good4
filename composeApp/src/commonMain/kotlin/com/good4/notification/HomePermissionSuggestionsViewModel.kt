package com.good4.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.analytics.AnalyticsConsent
import com.good4.analytics.AnalyticsTracker
import com.good4.core.presentation.UiText
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.analytics_preference_error
import good4.composeapp.generated.resources.notification_save_failed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomePermissionSuggestionsState(
    val notificationCardVisible: Boolean = false,
    val saving: Boolean = false,
    val analyticsDialogOpen: Boolean = false,
    val error: UiText? = null
)

class HomePermissionSuggestionsViewModel(
    private val store: HomePermissionPromptStore,
    private val analytics: AnalyticsTracker,
    private val notifications: NotificationDataSource
) : ViewModel() {
    private val mutable = MutableStateFlow(HomePermissionSuggestionsState())
    val state = mutable.asStateFlow()
    private var notificationSuggested = false
    private var permissionChecked = false
    private var checkJob: Job? = null
    private var enableJob: Job? = null
    private var generation = 0
    private var homeVisible = false

    init {
        viewModelScope.launch {
            combine(store.history, analytics.consent) { _, _ -> Unit }.collect { updateSuggestion() }
        }
        viewModelScope.launch {
            notifications.authStateFlow.map { it?.uid }.distinctUntilChanged().collect {
                generation++
                checkJob?.cancel()
                enableJob?.cancel()
                permissionChecked = false
                notificationSuggested = false
                mutable.update { HomePermissionSuggestionsState() }
                if (homeVisible) onHomeEntered()
            }
        }
    }

    fun onHomeEntered() {
        homeVisible = true
        if (notifications.currentUserId != null) store.onStudentHomeEntered()
        checkJob?.cancel()
        val requestGeneration = generation
        checkJob = viewModelScope.launch {
            try {
                val suggested = notifications.supported && !notificationEducationShown() &&
                    !notificationPermissionGranted()
                if (generation != requestGeneration) return@launch
                notificationSuggested = suggested
                permissionChecked = true
                // A recreated Android Activity may no longer hold the native completion callback.
                if (enableJob?.isActive != true) mutable.update { it.copy(saving = false) }
                updateSuggestion()
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                // An unknown OS permission state must not produce an unnecessary request.
                permissionChecked = false
                updateSuggestion()
            }
        }
    }

    fun onHomeLeft() { homeVisible = false; checkJob?.cancel() }

    private fun updateSuggestion() {
        val history = store.history.value
        val ready = permissionChecked && notifications.currentUserId != null
        val analyticsEligible = ready && history.sessions >= 3 && !history.analyticsDismissed &&
            analytics.consent.value == AnalyticsConsent.UNKNOWN
        var dialogOpen = mutable.value.analyticsDialogOpen && analyticsEligible
        if (homeVisible && analyticsEligible && !mutable.value.saving && !store.analyticsPromptShownThisSession) {
            store.markAnalyticsPromptShown()
            dialogOpen = true
        }
        mutable.update { it.copy(notificationCardVisible = ready && notificationSuggested,
            analyticsDialogOpen = dialogOpen) }
    }

    fun dismiss() {
        if (mutable.value.saving) return
        val saved = runCatching {
            saveNotificationEducationShown()
            notificationSuggested = false
        }.isSuccess
        mutable.update { it.copy(error = if (saved) null else UiText.StringResourceId(Res.string.notification_save_failed)) }
        updateSuggestion()
    }

    fun dismissAnalyticsDialog() {
        val saved = store.dismissAnalytics()
        mutable.update { it.copy(analyticsDialogOpen = false,
            error = if (saved) null else UiText.StringResourceId(Res.string.analytics_preference_error)) }
        updateSuggestion()
    }

    fun enableNotifications(requestPermission: (() -> Unit) -> Unit) {
        if (mutable.value.saving) return
        val uid = notifications.currentUserId ?: return
        val requestGeneration = generation
        mutable.update { it.copy(saving = true, error = null) }
        enableJob = viewModelScope.launch {
            try {
                notifications.enableAnnouncements()
                if (notifications.currentUserId != uid || generation != requestGeneration) return@launch
                if (!homeVisible) { mutable.update { it.copy(saving = false) }; return@launch }
                requestPermission {
                    if (generation == requestGeneration) mutable.update { it.copy(saving = false) }
                    PushSignals.refresh()
                    if (generation == requestGeneration) updateSuggestion()
                }
                // A failed native launch must remain retryable, including after returning home.
                saveNotificationEducationShown()
                notificationSuggested = false
                updateSuggestion()
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                if (generation == requestGeneration) mutable.update {
                    it.copy(saving = false, error = UiText.StringResourceId(Res.string.notification_save_failed))
                }
            }
        }
    }

}
