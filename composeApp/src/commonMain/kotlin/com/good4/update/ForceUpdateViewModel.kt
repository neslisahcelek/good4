package com.good4.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.config.data.repository.AppConfigRepository
import com.good4.core.util.AppVersionInfo
import com.good4.core.util.getAppVersionInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ForceUpdateState(
    val title: String? = null,
    val message: String? = null,
    val minVersionCodeAndroid: Int? = null,
    val minVersionIos: String? = null,
    val currentVersion: AppVersionInfo = getAppVersionInfo()
)

class ForceUpdateViewModel(
    private val configRepository: AppConfigRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ForceUpdateState())
    val state = _state.asStateFlow()

    init {
        loadNotice()
    }

    private fun loadNotice() {
        viewModelScope.launch {
            val notice = runCatching { configRepository.getUpdateNotice() }.getOrNull()
            if (notice != null) {
                _state.value = _state.value.copy(
                    title = notice.title.trim().takeIf { it.isNotBlank() },
                    message = notice.message.trim().takeIf { it.isNotBlank() },
                    minVersionCodeAndroid = notice.minVersionCodeAndroid,
                    minVersionIos = notice.minVersionIos
                )
            }
        }
    }

    fun onUpdateClicked() {
        openStoreAppPage()
    }
}
