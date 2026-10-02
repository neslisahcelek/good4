package com.good4.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.core.presentation.UiText
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.store_review_not_configured
import good4.composeapp.generated.resources.store_review_open_failed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StoreReviewState(val isOpening: Boolean = false, val error: UiText? = null)

private data class ReviewExposure(
    val resumed: Boolean = false,
    val feature: ReviewFeature? = null,
    val contentReady: Boolean = false
)

class StoreReviewViewModel(
    private val coordinator: StoreReviewCoordinator,
    private val version: String = reviewAppVersion(),
    private val enabled: Boolean = automaticReviewEnabled(),
    private val openStore: suspend () -> StoreOpenResult = { openReviewStore() },
    private val contentWaitMs: Long = REVIEW_CONTENT_WAIT_MS,
    private val idleWaitMs: Long = REVIEW_IDLE_WAIT_MS
) : ViewModel() {
    private val exposure = MutableStateFlow(ReviewExposure())
    private val idle = MutableStateFlow(false)
    private val _state = MutableStateFlow(StoreReviewState())
    val state = _state.asStateFlow()
    private var launcher: StoreReviewLauncher? = null
    private var requestGeneration = 0

    init {
        viewModelScope.launch {
            combine(exposure, coordinator.foreground, coordinator.modalCount) { screen, foreground, modals ->
                if (screen.resumed && foreground) coordinator.onStudentHomeEntered()
                screen.takeIf { it.resumed && it.feature != null && it.contentReady && foreground && modals == 0 }
            }.distinctUntilChanged().collectLatest { available ->
                ++requestGeneration
                if (available == null || !enabled || !coordinator.isEligible(version)) return@collectLatest
                // Reading time survives scrolling; only the final quiet period restarts on scroll.
                delay(contentWaitMs)
                idle.collectLatest { isIdle ->
                    val generation = ++requestGeneration
                    if (isIdle) {
                        delay(idleWaitMs)
                        if (coordinator.isEligible(version)) {
                            runCatching {
                                launcher?.requestReview {
                                    generation == requestGeneration && exposure.value == available && idle.value && enabled &&
                                        coordinator.recordAttempt(version)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun updateHome(studentResumed: Boolean) {
        exposure.update { ReviewExposure(resumed = studentResumed) }
    }

    fun updateFeature(
        feature: ReviewFeature,
        studentResumed: Boolean,
        contentReady: Boolean,
        isIdle: Boolean,
        reviewLauncher: StoreReviewLauncher
    ) {
        launcher = reviewLauncher
        exposure.update { ReviewExposure(studentResumed, feature, contentReady) }
        idle.update { isIdle }
    }

    fun leaveScreen() {
        exposure.update { ReviewExposure() }
        idle.update { false }
        launcher = null
    }

    fun openStore() {
        if (_state.value.isOpening) return
        _state.update { it.copy(isOpening = true, error = null) }
        viewModelScope.launch {
            val result = try { openStore.invoke() }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) { StoreOpenResult.FAILED }
            if (result == StoreOpenResult.OPENED) coordinator.onManualStoreOpened(version)
            _state.update {
                it.copy(isOpening = false, error = when (result) {
                    StoreOpenResult.OPENED -> null
                    StoreOpenResult.NOT_CONFIGURED -> UiText.StringResourceId(Res.string.store_review_not_configured)
                    StoreOpenResult.FAILED -> UiText.StringResourceId(Res.string.store_review_open_failed)
                })
            }
        }
    }
}
