package com.good4.supporter.presentation.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.auth.data.repository.AuthRepository
import com.good4.core.domain.Result
import com.good4.core.presentation.UiText
import com.good4.product.data.repository.FirestoreProductRepository
import com.good4.user.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SupporterProductListViewModel(
    private val productRepository: FirestoreProductRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SupporterProductListState())
    val state = _state.asStateFlow()

    private var isLoaded = false

    fun loadProductsIfNeeded() {
        if (!isLoaded && !_state.value.isLoading) {
            loadSupporterInfo()
            loadProducts()
        }
    }

    fun refresh() {
        isLoaded = false
        loadSupporterInfo()
        loadProducts()
    }

    fun onAction(action: SupporterProductListAction) {
        when (action) {
            is SupporterProductListAction.OnDismissError ->
                _state.update { it.copy(errorMessage = null) }
            is SupporterProductListAction.OnRefresh -> refresh()
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = productRepository.getProducts(includeOutOfStock = true)) {
                is Result.Success -> {
                    isLoaded = true
                    val availableProducts = result.data.filter { it.price > 0 }
                    _state.update {
                        it.copy(products = availableProducts, isLoading = false)
                    }
                }
                is Result.Error -> {
                    isLoaded = true
                    _state.update {
                        it.copy(
                            products = emptyList(),
                            isLoading = false,
                            errorMessage = UiText.DynamicString(result.error.message ?: "")
                        )
                    }
                }
            }
        }
    }

    private fun loadSupporterInfo() {
        val userId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            when (val result = userRepository.getUser(userId)) {
                is Result.Success -> {
                    val name = result.data.fullName
                        .trim()
                        .split(" ")
                        .firstOrNull()
                        .orEmpty()
                    _state.update { it.copy(supporterName = name) }
                }
                is Result.Error -> Unit
            }
        }
    }
}
