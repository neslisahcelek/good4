package com.good4.product.presentation.product_list

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ProductListViewModel {
    private val _state = MutableStateFlow(ProductListState())
    val state = _state.asStateFlow()

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
                // Handle product click in platform-agnostic way later
            }
        }
    }
}

