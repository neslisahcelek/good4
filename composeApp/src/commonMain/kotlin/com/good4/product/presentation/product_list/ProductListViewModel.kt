package com.good4.product.presentation.product_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.product.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProductListViewModel(
    //private val productRepository: ProductRepository
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

            val sampleProducts = listOf(
                        Product(
                            id = 1,
                            name = "Filtre Kahve",
                            description = "Orta boy, sıcak servis",
                            storeName = "Sokak Kahvecisi",
                            price = "100 TL",
                            imageUrl = "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=1200",
                            address = "Yakut Çarşısı, Konyaaltı/Antalya",
                            amount = 5
                        ),
                        Product(
                            id = 2,
                            name = "Latte",
                            description = "Büyük boy, sütlü",
                            storeName = "Kahve Durağı",
                            price = "140 TL",
                            imageUrl = "https://images.unsplash.com/photo-1511920170033-f8396924c348?w=1200",
                            address = "Atatürk Cd. No:12, Muratpaşa/Antalya",
                            amount = 3
                        ),
                        Product(
                            id = 3,
                            name = "Çay",
                            description = "İnce belli",
                            storeName = "Sokak Kahvecisi",
                            price = "30 TL",
                            imageUrl = "https://images.unsplash.com/photo-1518977676601-b53f82aba655?w=1200",
                            address = "Yakut Çarşısı, Konyaaltı/Antalya",
                            amount = 20
                        )
                    )
                    _state.update {
                        it.copy(
                            products = sampleProducts, //emptyList(),
                            isLoading = false,
                        )
                    }

//            when (val result = productRepository.getProducts()) {
//                is Result.Success -> {
//                    _state.update {
//                        it.copy(
//                            products = result.data,
//                            isLoading = false,
//                            errorMessage = null
//                        )
//                    }
//                }
//                is Result.Error -> {
//                    val sampleProducts = listOf(
//                        Product(
//                            id = 1,
//                            name = "Filtre Kahve",
//                            description = "Orta boy, sıcak servis",
//                            storeName = "Sokak Kahvecisi",
//                            price = "100 TL",
//                            imageUrl = "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=1200",
//                            address = "Yakut Çarşısı, Konyaaltı/Antalya",
//                            amount = 5
//                        ),
//                        Product(
//                            id = 2,
//                            name = "Latte",
//                            description = "Büyük boy, sütlü",
//                            storeName = "Kahve Durağı",
//                            price = "140 TL",
//                            imageUrl = "https://images.unsplash.com/photo-1511920170033-f8396924c348?w=1200",
//                            address = "Atatürk Cd. No:12, Muratpaşa/Antalya",
//                            amount = 3
//                        ),
//                        Product(
//                            id = 3,
//                            name = "Çay",
//                            description = "İnce belli",
//                            storeName = "Sokak Kahvecisi",
//                            price = "30 TL",
//                            imageUrl = "https://images.unsplash.com/photo-1518977676601-b53f82aba655?w=1200",
//                            address = "Yakut Çarşısı, Konyaaltı/Antalya",
//                            amount = 20
//                        )
//                    )
//                    _state.update {
//                        it.copy(
//                            products = sampleProducts, //emptyList(),
//                            isLoading = false,
//                            errorMessage = result.error
//                        )
//                    }
//                }
//            }
        }
    }
}

