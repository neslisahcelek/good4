package com.good4.admin.presentation.campaigns

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.campaign.data.dto.CampaignDto
import com.good4.campaign.data.repository.CampaignRepository
import com.good4.core.domain.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminCampaignsViewModel(
    private val campaignRepository: CampaignRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AdminCampaignsState())
    val state = _state.asStateFlow()

    init {
        loadCampaigns()
    }

    private fun loadCampaigns() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = campaignRepository.getCampaigns()) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            campaigns = result.data
                        )
                    }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    fun onImageUrlChange(url: String) {
        _state.update { it.copy(campaignImageUrl = url, errorMessage = null) }
    }

    fun addCampaign() {
        val imageUrl = _state.value.campaignImageUrl

        if (imageUrl.isBlank()) {
            _state.update { it.copy(errorMessage = "Resim URL'i gerekli") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isAddLoading = true, errorMessage = null) }

            val campaignDto = CampaignDto(image = imageUrl)

            when (val result = campaignRepository.addCampaign(campaignDto)) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            isAddLoading = false,
                            addSuccess = true
                        )
                    }
                    loadCampaigns()
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isAddLoading = false,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }

    fun resetAddState() {
        _state.update {
            it.copy(
                campaignImageUrl = "",
                addSuccess = false,
                errorMessage = null
            )
        }
    }
}

