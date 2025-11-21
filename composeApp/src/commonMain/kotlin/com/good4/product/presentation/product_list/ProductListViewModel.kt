package com.good4.product.presentation.product_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.core.domain.Result
import com.good4.product.data.repository.FirestoreProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProductListViewModel(
    private val productRepository: FirestoreProductRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ProductListState())
    val state = _state.asStateFlow()

    init {
        loadProducts()
    }

    fun onAction(action: ProductListAction) {
        when (action) {
            is ProductListAction.OnSearchQueryChange -> {
                _state.update {
                    it.copy(
                        searchQuery = action.query
                    )
                }
            }

            is ProductListAction.OnProductClick -> {
                // TODO: Navigate to product detail
            }
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            when (val result = productRepository.getProducts()) {
                is Result.Success -> {
                    println("ProductListViewModel: Products loaded successfully: ${result.data.size}")
                    _state.update {
                        it.copy(
                            products = result.data,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                is Result.Error -> {
                    println("ProductListViewModel: Error loading products: ${result.error.message}")
                    _state.update {
                        it.copy(
                            products = emptyList(),
                            isLoading = false,
                            errorMessage = result.error.message
                        )
                    }
                }
            }
        }
    }
}

