package com.good4.supporter.presentation.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.auth.data.repository.AuthRepository
import com.good4.core.domain.Result
import com.good4.core.presentation.UiText
import com.good4.product.data.repository.FirestoreProductRepository
import com.good4.product.isVisibleToPublicUsers
import com.good4.user.data.repository.UserRepository
import kotlinx.coroutines.Job
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

    private var hasLoadedOnce = false
    private var loadJob: Job? = null

    fun loadProductsIfNeeded() {
        if (!hasLoadedOnce && !_state.value.isLoading) {
            loadSupporterInfo()
            loadProducts(showLoading = true)
        }
    }

    fun refresh(showLoading: Boolean = !hasLoadedOnce) {
        if (_state.value.isLoading) return
        loadSupporterInfo()
        loadProducts(showLoading = showLoading)
    }

    fun onAction(action: SupporterProductListAction) {
        when (action) {
            is SupporterProductListAction.OnDismissError ->
                _state.update { it.copy(errorMessage = null) }
            is SupporterProductListAction.OnRefresh -> refresh()
        }
    }

    private fun loadProducts(showLoading: Boolean) {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            if (showLoading) {
                _state.update { it.copy(isLoading = true, errorMessage = null) }
            } else {
                _state.update { it.copy(errorMessage = null) }
            }

            when (val result = productRepository.getProducts(includeOutOfStock = true)) {
                is Result.Success -> {
                    hasLoadedOnce = true
                    val availableProducts = result.data.filter { product ->
                        !product.isDonation &&
                                product.price > 0 &&
                                product.isVisibleToPublicUsers()
                    }
                    _state.update {
                        it.copy(products = availableProducts, isLoading = false)
                    }
                }
                is Result.Error -> {
                    hasLoadedOnce = true
                    _state.update {
                        it.copy(
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
