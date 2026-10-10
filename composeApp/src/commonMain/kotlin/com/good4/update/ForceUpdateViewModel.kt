package com.good4.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.config.data.repository.AppConfigRepository
import com.good4.core.presentation.UiText
import com.good4.core.util.PlatformType
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.force_update_title
import good4.composeapp.generated.resources.force_update_body
import com.good4.core.util.AppVersionInfo
import com.good4.core.util.getAppVersionInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class ForceUpdateState(
    val title: UiText = UiText.StringResourceId(Res.string.force_update_title),
    val message: UiText = UiText.StringResourceId(Res.string.force_update_body),
    val minimumVersion: String? = null,
    val currentVersion: AppVersionInfo = getAppVersionInfo(),
    val isUpdateRequired: Boolean = true
)

class ForceUpdateViewModel(
    private val configRepository: AppConfigRepository
) : ViewModel() {
    private companion object {
        const val REFRESH_TIMEOUT_MS = 3_000L
    }

    private val _state = MutableStateFlow(ForceUpdateState())
    val state = _state.asStateFlow()

    private var refreshJob: Job? = null

    fun onResume() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val notice = try {
                withTimeoutOrNull(REFRESH_TIMEOUT_MS) {
                    configRepository.getUpdateNotice(forceRefresh = true)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                null
            }
            coroutineContext.ensureActive()
            if (notice != null) {
                _state.update { current ->
                    current.copy(
                        title = notice.title.trim().takeIf { it.isNotBlank() }
                            ?.let { UiText.DynamicString(it) }
                            ?: UiText.StringResourceId(Res.string.force_update_title),
                        message = notice.message.trim().takeIf { it.isNotBlank() }
                            ?.let { UiText.DynamicString(it) }
                            ?: UiText.StringResourceId(Res.string.force_update_body),
                        minimumVersion = when (current.currentVersion.platform) {
                            PlatformType.ANDROID -> notice.minVersionCodeAndroid?.let { "v$it" }
                            PlatformType.IOS -> notice.minVersionIos
                        },
                        isUpdateRequired = notice.isForceUpdateRequired(current.currentVersion)
                    )
                }
            }
        }
    }

    fun onPause() {
        refreshJob?.cancel()
    }

    fun onUpdateClicked() {
        openStoreAppPage()
    }
}
