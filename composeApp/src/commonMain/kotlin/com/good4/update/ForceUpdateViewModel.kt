package com.good4.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.config.data.repository.AppConfigRepository
import com.good4.core.util.AppVersionInfo
import com.good4.core.util.getAppVersionInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class ForceUpdateState(
    val title: String? = null,
    val message: String? = null,
    val minVersionCodeAndroid: Int? = null,
    val minVersionIos: String? = null,
    val currentVersion: AppVersionInfo = getAppVersionInfo(),
    val isUpdateRequired: Boolean = true
)

class ForceUpdateViewModel(
    private val configRepository: AppConfigRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ForceUpdateState())
    val state = _state.asStateFlow()

    private var refreshJob: Job? = null

    fun onResume() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val notice = try {
                withTimeoutOrNull(3_000L) {
                    configRepository.getUpdateNotice(forceRefresh = true)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                null
            }
            coroutineContext.ensureActive()
            if (notice != null) {
                _state.value = _state.value.copy(
                    title = notice.title.trim().takeIf { it.isNotBlank() },
                    message = notice.message.trim().takeIf { it.isNotBlank() },
                    minVersionCodeAndroid = notice.minVersionCodeAndroid,
                    minVersionIos = notice.minVersionIos,
                    isUpdateRequired = notice.isForceUpdateRequired(_state.value.currentVersion)
                )
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
