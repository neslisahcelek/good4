package com.good4.product.presentation.product_list

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ProductListViewModel: ViewModel() {
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

            is ProductListAction.OnProductClick -> TODO()
        }
    }
}

