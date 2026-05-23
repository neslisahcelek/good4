package com.good4.admin.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.auth.data.repository.AuthRepository
import com.good4.business.data.dto.FirestoreBusinessRepository
import com.good4.campaign.data.repository.CampaignRepository
import com.good4.core.domain.Result
import com.good4.notification.data.repository.NotificationEventRepository
import com.good4.product.data.repository.FirestoreProductRepository
import com.good4.user.data.repository.UserRepository
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.admin_dashboard_business_approved
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.getString

class AdminDashboardViewModel(
    private val authRepository: AuthRepository,
    private val productRepository: FirestoreProductRepository,
    private val businessRepository: FirestoreBusinessRepository,
    private val campaignRepository: CampaignRepository,
    private val userRepository: UserRepository,
    private val notificationEventRepository: NotificationEventRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AdminDashboardState())
    val state = _state.asStateFlow()

    init {
        refreshDashboard()
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null, actionMessage = null) }

            // Get products count
            when (val result = productRepository.getProducts(includeOutOfStock = true)) {
                is Result.Success -> {
                    val activeProducts = result.data
                        .filter { product -> product.pendingCount > 0 }
                        .map { product ->
                            ActiveProductStock(
                                id = product.documentId,
                                name = product.name,
                                stock = product.pendingCount
                            )
                        }
                    _state.update {
                        it.copy(
                            totalProducts = result.data.size,
                            activeProducts = activeProducts
                        )
                    }
                }
                is Result.Error -> {
                    _state.update { it.copy(errorMessage = "Urunler alinamadi: ${result.error.message}") }
                }
            }

            // Get businesses count + pending list
            when (val result = businessRepository.getBusinesses()) {
                is Result.Success -> {
                    _state.update { it.copy(totalBusinesses = result.data.size) }
                }
                is Result.Error -> {
                    _state.update { it.copy(errorMessage = "Isletmeler alinamadi: ${result.error.message}") }
                }
            }

            when (val pendingResult = businessRepository.getPendingBusinesses()) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            pendingBusinesses = pendingResult.data.map { doc -> doc.data.copy(id = doc.id) },
                            pendingBusinessesCount = pendingResult.data.size
                        )
                    }
                }
                is Result.Error -> {
                    _state.update { it.copy(errorMessage = "Onay bekleyen isletmeler alinamadi: ${pendingResult.error.message}") }
                }
            }

            // Get campaigns count
            when (val result = campaignRepository.getCampaigns()) {
                is Result.Success -> {
                    _state.update { it.copy(totalCampaigns = result.data.size) }
                }
                is Result.Error -> {
                    _state.update { it.copy(errorMessage = "Kampanyalar alinamadi: ${result.error.message}") }
                }
            }

            // Get users count
            when (val result = userRepository.getAllUsers()) {
                is Result.Success -> {
                    _state.update { it.copy(totalUsers = result.data.size) }
                }
                is Result.Error -> {
                    _state.update { it.copy(errorMessage = "Kullanicilar alinamadi: ${result.error.message}") }
                }
            }

            _state.update { it.copy(isLoading = false) }
        }
    }

    fun approveBusiness(businessId: String) {
        val adminId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            val approvedAt = Clock.System.now().epochSeconds
            when (val result = businessRepository.approveBusiness(businessId, adminId, approvedAt)) {
                is Result.Success -> {
                    val approvedBusiness = _state.value.pendingBusinesses.firstOrNull { it.id == businessId }
                    if (approvedBusiness != null) {
                        notificationEventRepository.publishBusinessApprovedEvent(
                            businessId = approvedBusiness.id,
                            ownerUserId = approvedBusiness.ownerId,
                            approvedBy = adminId,
                            approvedAt = approvedAt
                        )
                    }
                    _state.update { it.copy(actionMessage = getString(Res.string.admin_dashboard_business_approved)) }
                    refreshDashboard()
                }
                is Result.Error -> {
                    _state.update { it.copy(errorMessage = "İşletme onaylanamadı: ${result.error.message}") }
                }
            }
        }
    }
}
