package com.good4.admin.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.business.data.dto.FirestoreBusinessRepository
import com.good4.campaign.data.repository.CampaignRepository
import com.good4.core.domain.Result
import com.good4.product.data.repository.FirestoreProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AdminDashboardViewModel(
    private val productRepository: FirestoreProductRepository,
    private val businessRepository: FirestoreBusinessRepository,
    private val campaignRepository: CampaignRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AdminDashboardState())
    val state = _state.asStateFlow()

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            // Get products count
            when (val result = productRepository.getProducts()) {
                is Result.Success -> {
                    _state.update { it.copy(totalProducts = result.data.size) }
                }
                is Result.Error -> {}
            }

            // Get businesses count
            when (val result = businessRepository.getBusinesses()) {
                is Result.Success -> {
                    _state.update { it.copy(totalBusinesses = result.data.size) }
                }
                is Result.Error -> {}
            }

            // Get campaigns count
            when (val result = campaignRepository.getCampaigns()) {
                is Result.Success -> {
                    _state.update { it.copy(totalCampaigns = result.data.size) }
                }
                is Result.Error -> {}
            }

            _state.update { it.copy(isLoading = false) }
        }
    }
}

