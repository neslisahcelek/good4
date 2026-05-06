package com.good4.business.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.auth.data.repository.AuthRepository
import com.good4.business.data.dto.FirestoreBusinessRepository
import com.good4.code.data.repository.CodeRepository
import com.good4.code.data.repository.statusEnum
import com.good4.code.domain.CodeStatus
import com.good4.core.domain.Result
import com.good4.core.util.userFriendlyErrorMessage
import com.good4.order.data.repository.OrderRepository
import com.good4.order.domain.OrderStatus
import com.good4.order.domain.isActivePending
import com.good4.order.domain.isVisibleOnBusinessDashboard
import com.good4.product.data.repository.FirestoreProductRepository
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.business_name_fallback
import good4.composeapp.generated.resources.business_order_detail_cancel_failed
import good4.composeapp.generated.resources.business_order_detail_cancel_success
import good4.composeapp.generated.resources.error_business_not_found
import good4.composeapp.generated.resources.error_data_load_failed
import good4.composeapp.generated.resources.product_name_fallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

class BusinessDashboardViewModel(
    private val authRepository: AuthRepository,
    private val businessRepository: FirestoreBusinessRepository,
    private val codeRepository: CodeRepository,
    private val productRepository: FirestoreProductRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BusinessDashboardState())
    val state = _state.asStateFlow()

    private var cachedBusinessId: String? = null
    private var hasLoadedOnce: Boolean = false

    init {
        loadDashboard()
    }

    fun refreshDashboard(showLoading: Boolean = !hasLoadedOnce) {
        loadDashboard(showLoading = showLoading)
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun dismissOrderDetail() {
        _state.update {
            it.copy(
                orderDetailSheetVisible = false,
                orderDetailLoading = false,
                isCancellingOrderDetail = false,
                orderDetail = null
            )
        }
    }

    fun cancelOrderFromDetail() {
        val snapshot = _state.value
        val order = snapshot.orderDetail ?: return
        if (order.status != OrderStatus.PENDING || snapshot.isCancellingOrderDetail || snapshot.orderDetailLoading) {
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isCancellingOrderDetail = true, errorMessage = null) }

            when (val result = orderRepository.updateOrderStatus(order.id, OrderStatus.CANCELLED)) {
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isCancellingOrderDetail = false,
                            errorMessage = userFriendlyErrorMessage(
                                result.error.message,
                                getString(Res.string.business_order_detail_cancel_failed)
                            )
                        )
                    }
                    return@launch
                }

                is Result.Success -> Unit
            }

            order.items.forEach { item ->
                productRepository.incrementProductSuspendedCount(item.productId, item.quantity)
            }

            _state.update { current ->
                current.copy(
                    isCancellingOrderDetail = false,
                    orderDetail = order.copy(status = OrderStatus.CANCELLED),
                    supporterPendingCount = (current.supporterPendingCount - 1).coerceAtLeast(0),
                    recentOrders = current.recentOrders.map { recentOrder ->
                        if (recentOrder.id == order.id) {
                            recentOrder.copy(orderStatus = OrderStatus.CANCELLED)
                        } else {
                            recentOrder
                        }
                    },
                    errorMessage = getString(Res.string.business_order_detail_cancel_success)
                )
            }
        }
    }

    fun startOrderDetail(orderId: String) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    orderDetailSheetVisible = true,
                    orderDetailLoading = true,
                    isCancellingOrderDetail = false,
                    orderDetail = null
                )
            }
            val loadErrorFallback = getString(Res.string.error_data_load_failed)
            when (val result = orderRepository.getOrder(orderId)) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            orderDetail = result.data,
                            orderDetailLoading = false
                        )
                    }
                }

                is Result.Error -> {
                    _state.update {
                        it.copy(
                            orderDetailSheetVisible = false,
                            orderDetailLoading = false,
                            errorMessage = userFriendlyErrorMessage(
                                result.error.message,
                                loadErrorFallback
                            )
                        )
                    }
                }
            }
        }
    }

    fun openFirstPendingOrderDetail() {
        val businessId = cachedBusinessId ?: return
        viewModelScope.launch {
            orderRepository.checkAndExpireOrdersByBusiness(businessId)
            when (val result = orderRepository.getOrdersByBusinessAndStatus(businessId, OrderStatus.PENDING)) {
                is Result.Success -> {
                    val first = result.data.firstOrNull { order -> order.isActivePending() }
                    if (first != null) {
                        _state.update {
                            it.copy(
                                orderDetailSheetVisible = true,
                                orderDetailLoading = false,
                                orderDetail = first
                            )
                        }
                    }
                }

                is Result.Error -> {
                    val loadErrorFallback = getString(Res.string.error_data_load_failed)
                    _state.update {
                        it.copy(
                            errorMessage = userFriendlyErrorMessage(
                                result.error.message,
                                loadErrorFallback
                            )
                        )
                    }
                }
            }
        }
    }

    private fun loadDashboard(showLoading: Boolean = true) {
        val userId = authRepository.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                _state.update { current ->
                    current.copy(
                        isLoading = showLoading,
                        errorMessage = null,
                        orderDetailSheetVisible = false,
                        orderDetailLoading = false,
                        isCancellingOrderDetail = false,
                        orderDetail = null
                    )
                }
                val loadErrorFallback = getString(Res.string.error_data_load_failed)

                when (val ownedResult = businessRepository.getOwnedBusinessId(userId)) {
                    is Result.Error -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = userFriendlyErrorMessage(
                                    ownedResult.error.message,
                                    loadErrorFallback
                                )
                            )
                        }
                        return@launch
                    }

                    is Result.Success -> {
                        val businessId = ownedResult.data
                        if (businessId == null) {
                            cachedBusinessId = null
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = getString(Res.string.error_business_not_found)
                                )
                            }
                            return@launch
                        }

                        cachedBusinessId = businessId

                        val fallbackName = getString(Res.string.business_name_fallback)

                        codeRepository.checkAndExpireCodes()
                        orderRepository.checkAndExpireOrdersByBusiness(businessId)

                    when (val businessResult = businessRepository.getBusinessById(businessId)) {
                        is Result.Success -> {
                            _state.update {
                                it.copy(
                                    businessName = businessResult.data.name.ifBlank { fallbackName }
                                )
                            }
                        }

                        is Result.Error -> {
                            _state.update {
                                it.copy(businessName = fallbackName)
                            }
                        }
                    }

                    when (val countsResult = codeRepository.getCodeCountsByBusinessId(businessId)) {
                        is Result.Success -> {
                            _state.update {
                                it.copy(
                                    pendingCount = countsResult.data.pending,
                                    completedCount = countsResult.data.completed
                                )
                            }
                        }

                        is Result.Error -> {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = userFriendlyErrorMessage(
                                        countsResult.error.message,
                                        loadErrorFallback
                                    )
                                )
                            }
                            return@launch
                        }
                    }

                    val recentCodes = when (val recentResult = codeRepository.getRecentCodesByBusinessId(businessId, limit = 20)) {
                        is Result.Success -> {
                            val productFallback = getString(Res.string.product_name_fallback)
                            recentResult.data
                                .filter { code ->
                                    code.statusEnum != CodeStatus.CANCELLED &&
                                            code.statusEnum != CodeStatus.EXPIRED
                                }
                                .sortedByDescending { code -> code.usedAt ?: code.createdAt ?: 0L }
                                .map { code ->
                                    RecentCodeUiModel(
                                        id = code.id,
                                        codeValue = code.value,
                                        productName = code.productName ?: productFallback,
                                        status = code.status
                                    )
                                }
                                .take(10)
                        }

                        is Result.Error -> {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = userFriendlyErrorMessage(
                                        recentResult.error.message,
                                        loadErrorFallback
                                    )
                                )
                            }
                            return@launch
                        }
                    }

                    var supporterPending = 0
                    var supporterConfirmed = 0
                    var recentOrdersUi = emptyList<RecentOrderUiModel>()
                    val orderProductFallback = getString(Res.string.product_name_fallback)

                        when (val recentOrdersResult =
                            orderRepository.getRecentOrdersByBusiness(businessId, limit = 20)) {
                        is Result.Success -> {
                            recentOrdersUi = recentOrdersResult.data
                                .filter { order -> order.isVisibleOnBusinessDashboard() }
                                .take(5)
                                .map { order ->
                                    RecentOrderUiModel(
                                        id = order.id,
                                        productName = order.items.firstOrNull()?.productName?.ifBlank { orderProductFallback }
                                            ?: orderProductFallback,
                                        code = order.code,
                                        orderStatus = order.status
                                    )
                                }
                        }

                        is Result.Error -> Unit
                    }

                    when (val pendingOrders = orderRepository.getOrdersByBusinessAndStatus(businessId, OrderStatus.PENDING)) {
                        is Result.Success -> {
                            supporterPending =
                                pendingOrders.data.count { order -> order.isActivePending() }
                        }
                        is Result.Error -> Unit
                    }

                    when (val confirmedOrders = orderRepository.getOrdersByBusinessAndStatus(businessId, OrderStatus.CONFIRMED)) {
                        is Result.Success -> supporterConfirmed = confirmedOrders.data.size
                        is Result.Error -> Unit
                    }

                    when (val productsResult = productRepository.getProductsByBusinessId(businessId, includeOutOfStock = true)) {
                        is Result.Success -> {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    recentCodes = recentCodes,
                                    totalProducts = productsResult.data.size,
                                    supporterPendingCount = supporterPending,
                                    supporterConfirmedCount = supporterConfirmed,
                                    recentOrders = recentOrdersUi,
                                    errorMessage = null
                                )
                            }
                        }

                        is Result.Error -> {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    recentCodes = recentCodes,
                                    totalProducts = 0,
                                    supporterPendingCount = supporterPending,
                                    supporterConfirmedCount = supporterConfirmed,
                                    recentOrders = recentOrdersUi,
                                    errorMessage = userFriendlyErrorMessage(
                                        productsResult.error.message,
                                        loadErrorFallback
                                    )
                                )
                            }
                        }
                    }
                    }
                }
            } finally {
                hasLoadedOnce = true
            }
        }
    }
}
