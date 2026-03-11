package com.good4.supporter.presentation.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.auth.data.repository.AuthRepository
import com.good4.core.domain.Result
import com.good4.core.presentation.UiText
import com.good4.order.data.dto.OrderDto
import com.good4.order.data.dto.OrderItemDto
import com.good4.order.data.repository.OrderRepository
import com.good4.order.domain.OrderStatus
import com.good4.product.Product
import com.good4.user.data.repository.UserRepository
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.supporter_cart_empty_error
import good4.composeapp.generated.resources.supporter_cart_no_session_error
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
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SupporterCartState())
    val state = _state.asStateFlow()

    fun onAction(action: SupporterCartAction) {
        when (action) {
            is SupporterCartAction.OnAddItem -> addItem(action.product)
            is SupporterCartAction.OnRemoveItem -> removeItem(action.productId)
            is SupporterCartAction.OnIncreaseQuantity -> changeQuantity(action.productId, +1)
            is SupporterCartAction.OnDecreaseQuantity -> changeQuantity(action.productId, -1)
            is SupporterCartAction.OnCreateOrder -> createOrder()
            is SupporterCartAction.OnDismissError -> _state.update { it.copy(errorMessage = null) }
            is SupporterCartAction.OnOrderNavigated -> _state.update { it.copy(createdOrderId = null) }
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
    }

    private fun removeItem(productId: String) {
        _state.update { it.copy(items = it.items.filter { item -> item.product.documentId != productId }) }
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
            val expiresAtSecs = nowSecs + 24 * 60 * 60
            val code = (100000..999999).random().toString()

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
                            isCreatingOrder = false,
                            items = emptyList(),
                            createdOrderId = result.data
                        )
                    }
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
}
