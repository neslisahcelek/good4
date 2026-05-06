package com.good4.supporter.presentation.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.auth.data.repository.AuthRepository
import com.good4.config.data.repository.AppConfigRepository
import com.good4.core.domain.Result
import com.good4.core.presentation.UiText
import com.good4.order.data.dto.OrderDto
import com.good4.order.data.dto.OrderItemDto
import com.good4.order.data.repository.OrderRepository
import com.good4.order.domain.OrderStatus
import com.good4.order.domain.isActivePending
import com.good4.product.Product
import com.good4.supporter.data.local.SupporterCartStorage
import com.good4.supporter.data.local.SupporterCartStoredItem
import com.good4.supporter.data.local.toProduct
import com.good4.supporter.data.local.toStoredProduct
import com.good4.user.data.repository.UserRepository
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.supporter_cart_empty_error
import good4.composeapp.generated.resources.supporter_cart_no_session_error
import good4.composeapp.generated.resources.supporter_cart_order_cancel_failed
import good4.composeapp.generated.resources.supporter_cart_order_failed
import good4.composeapp.generated.resources.supporter_cart_single_business_error
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class SupporterCartViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val orderRepository: OrderRepository,
    private val cartStorage: SupporterCartStorage,
    private val configRepository: AppConfigRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SupporterCartState())
    val state = _state.asStateFlow()

    init {
        restoreCart()
        refreshActiveOrders()
    }

    fun onAction(action: SupporterCartAction) {
        when (action) {
            is SupporterCartAction.OnAddItem -> addItem(action.product)
            is SupporterCartAction.OnRemoveItem -> removeItem(action.productId)
            is SupporterCartAction.OnIncreaseQuantity -> changeQuantity(action.productId, +1)
            is SupporterCartAction.OnDecreaseQuantity -> changeQuantity(action.productId, -1)
            is SupporterCartAction.OnCancelActiveOrder -> cancelActiveOrder(action.orderId)
            is SupporterCartAction.OnCreateOrder -> startOrderReview()
            is SupporterCartAction.OnConfirmCreateOrder -> createOrder()
            is SupporterCartAction.OnCancelOrderReview -> cancelOrderReview()
            is SupporterCartAction.OnRefreshActiveOrders -> refreshActiveOrders()
            is SupporterCartAction.OnDismissError -> _state.update { it.copy(errorMessage = null) }
            is SupporterCartAction.OnOrderNavigated -> _state.update { it.copy(createdOrderId = null, isReviewingOrder = false) }
        }
    }

    private fun addItem(product: Product) {
        _state.update { current ->
            val existing = current.items.find { it.product.documentId == product.documentId }
            if (existing != null) {
                current.copy(items = current.items.map {
                    if (it.product.documentId == product.documentId) it.copy(quantity = it.quantity + 1)
                    else it
                })
            } else {
                current.copy(items = current.items + CartItem(product, 1))
            }
        }
        persistCart()
    }

    private fun removeItem(productId: String) {
        _state.update { it.copy(items = it.items.filter { item -> item.product.documentId != productId }) }
        persistCart()
    }

    private fun changeQuantity(productId: String, delta: Int) {
        _state.update { current ->
            val updated = current.items.mapNotNull { item ->
                if (item.product.documentId == productId) {
                    val newQty = item.quantity + delta
                    if (newQty <= 0) null else item.copy(quantity = newQty)
                } else {
                    item
                }
            }
            current.copy(items = updated)
        }
        persistCart()
    }

    private fun createOrder() {
        val currentUser = authRepository.currentUser ?: run {
            _state.update { it.copy(errorMessage = UiText.StringResourceId(Res.string.supporter_cart_no_session_error)) }
            return
        }
        val items = _state.value.items
        if (items.isEmpty()) {
            _state.update { it.copy(errorMessage = UiText.StringResourceId(Res.string.supporter_cart_empty_error)) }
            return
        }

        val distinctBusinessIds = items.map { it.product.businessId }.distinct()
        if (distinctBusinessIds.size > 1) {
            _state.update { it.copy(errorMessage = UiText.StringResourceId(Res.string.supporter_cart_single_business_error)) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isCreatingOrder = true, errorMessage = null) }

            val supporterName = when (val result = userRepository.getUser(currentUser.uid)) {
                is Result.Success -> result.data.fullName
                is Result.Error -> currentUser.email ?: ""
            }

            val firstItem = items.first()
            val businessId = firstItem.product.businessId
            val businessName = firstItem.product.storeName
            val nowSecs = Clock.System.now().epochSeconds
            val expiresAtSecs = nowSecs + configRepository
                .getSupporterOrderCodeExpirationDuration()
                .inWholeSeconds
            val code = generateUniqueOrderCode(businessId) ?: run {
                _state.update {
                    it.copy(
                        isCreatingOrder = false,
                        errorMessage = UiText.StringResourceId(Res.string.supporter_cart_order_failed)
                    )
                }
                return@launch
            }

            val orderItems = items.map { cartItem ->
                val unitPrice = (cartItem.product.discountPrice
                    ?: cartItem.product.originalPrice
                    ?: cartItem.product.price).toDouble()
                OrderItemDto(
                    businessId = cartItem.product.businessId,
                    businessName = cartItem.product.storeName,
                    productId = cartItem.product.documentId,
                    productName = cartItem.product.name,
                    quantity = cartItem.quantity,
                    unitPrice = unitPrice,
                    totalPrice = unitPrice * cartItem.quantity
                )
            }

            val totalAmount = orderItems.sumOf { it.totalPrice ?: 0.0 }

            val dto = OrderDto(
                businessId = businessId,
                businessName = businessName,
                code = code,
                createdAt = nowSecs,
                expiresAt = expiresAtSecs,
                totalAmount = totalAmount,
                grandTotal = totalAmount,
                platformDonation = 0.0,
                status = OrderStatus.PENDING.value,
                supporterId = currentUser.uid,
                supporterName = supporterName,
                items = orderItems
            )

            when (val result = orderRepository.createOrder(dto)) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            isReviewingOrder = false,
                            isCreatingOrder = false,
                            items = emptyList(),
                            createdOrderId = result.data
                        )
                    }
                    clearPersistedCart()
                    refreshActiveOrders()
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isCreatingOrder = false,
                            errorMessage = UiText.StringResourceId(Res.string.supporter_cart_order_failed)
                        )
                    }
                }
            }
        }
    }

    private fun startOrderReview() {
        if (_state.value.items.isEmpty()) {
            _state.update { it.copy(errorMessage = UiText.StringResourceId(Res.string.supporter_cart_empty_error)) }
            return
        }
        _state.update { it.copy(isReviewingOrder = true, errorMessage = null) }
    }

    private fun cancelOrderReview() {
        _state.update { it.copy(isReviewingOrder = false, errorMessage = null) }
    }

    private suspend fun generateUniqueOrderCode(
        businessId: String,
        maxAttempts: Int = 20
    ): String? {
        repeat(maxAttempts) {
            val candidate = (1000..9999).random().toString()
            when (val result = orderRepository.getOrderByCodeAndBusiness(candidate, businessId)) {
                is Result.Success -> if (result.data == null) return candidate
                is Result.Error -> return null
            }
        }
        return null
    }

    private fun restoreCart() {
        val userId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            val storedItems = cartStorage.loadItems(userId)
            if (storedItems.isEmpty()) return@launch

            _state.update { current ->
                current.copy(
                    items = storedItems.mapNotNull { stored ->
                        val quantity = stored.quantity
                        if (quantity <= 0) null
                        else CartItem(product = stored.product.toProduct(), quantity = quantity)
                    }
                )
            }
        }
    }

    private fun persistCart() {
        val userId = authRepository.currentUser?.uid ?: return
        val itemsToPersist = _state.value.items.map {
            SupporterCartStoredItem(
                product = it.product.toStoredProduct(),
                quantity = it.quantity
            )
        }

        viewModelScope.launch {
            if (itemsToPersist.isEmpty()) {
                cartStorage.clear(userId)
            } else {
                cartStorage.saveItems(userId, itemsToPersist)
            }
        }
    }

    private fun clearPersistedCart() {
        val userId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            cartStorage.clear(userId)
        }
    }

    private fun refreshActiveOrders() {
        val userId = authRepository.currentUser?.uid ?: run {
            _state.update { it.copy(activeOrders = emptyList()) }
            return
        }

        viewModelScope.launch {
            orderRepository.checkAndExpireOrdersBySupporter(userId)
            when (val result = orderRepository.getOrdersBySupporter(userId)) {
                is Result.Success -> {
                    val activeOrders = result.data
                        .asSequence()
                        .filter { order -> order.isActivePending() }
                        .sortedByDescending { it.createdAt?.epochSeconds ?: 0L }
                        .map { order ->
                            ActiveSupporterOrder(
                                id = order.id,
                                code = order.code,
                                productName = order.items.firstOrNull()?.productName?.ifBlank { order.businessName }
                                    ?: order.businessName,
                                businessName = order.businessName,
                                expiresAtEpochSeconds = order.expiresAt?.epochSeconds
                            )
                        }
                        .toList()

                    _state.update { it.copy(activeOrders = activeOrders) }
                }
                is Result.Error -> Unit
            }
        }
    }

    private fun cancelActiveOrder(orderId: String) {
        if (_state.value.cancellingOrderIds.contains(orderId)) return

        viewModelScope.launch {
            _state.update {
                it.copy(
                    cancellingOrderIds = it.cancellingOrderIds + orderId,
                    errorMessage = null
                )
            }

            when (orderRepository.updateOrderStatus(orderId, OrderStatus.CANCELLED)) {
                is Result.Success -> refreshActiveOrders()
                is Result.Error -> {
                    _state.update {
                        it.copy(errorMessage = UiText.StringResourceId(Res.string.supporter_cart_order_cancel_failed))
                    }
                }
            }

            _state.update {
                it.copy(cancellingOrderIds = it.cancellingOrderIds - orderId)
            }
        }
    }
}
