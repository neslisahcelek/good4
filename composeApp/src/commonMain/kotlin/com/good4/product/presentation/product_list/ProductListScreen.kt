package com.good4.product.presentation.product_list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.product.Product
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProductListScreenRoot(
    viewModel: ProductListViewModel = koinViewModel(),
    onProductClick: (Product) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProductListScreen(
        state = state,
        onAction = { action ->
            when(action) {
                is ProductListAction.OnProductClick -> onProductClick(action.product)
                is ProductListAction.OnSearchQueryChange -> TODO()
            }
            viewModel.onAction(action)
        }
    )
}

@Composable
fun ProductListScreen(
    modifier: Modifier = Modifier,
    state: ProductListState,
    onAction: (ProductListAction) -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            state.errorMessage != null -> {
                Text(
                    text = "Hata: ${state.errorMessage}",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error
                )
            }
            state.products.isEmpty() -> {
                Text(
                    text = "Ürünleri yüklemede sorun yaşıyoruz. Lütfen tekrar dene.",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(
                        horizontal = 12.dp,
                        vertical = 12.dp
                    )
                ) {
                    items(
                        items = state.products,
                        key = { it.documentId }
                    ) { product ->
                        ProductItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable { onAction(ProductListAction.OnProductClick(product)) },
                            product = product
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun ProductListScreenPreview() {
    MaterialTheme {
        val sample = listOf(
            Product(
                id = 1,
                documentId = "doc1",
                name = "Filtre Kahve",
                storeName = "Sokak Kahvecisi",
                address = "Yakut Çarşısı, Konyaaltı/Antalya",
                description = "Orta boy, sıcak servis",
                price = "80 TL",
                originalPrice = 100,
                discountPrice = 80,
                discountPercentage = 20,
                imageUrl = "image1.png",
                amount = 5
            ),
            Product(
                id = 2,
                documentId = "doc2",
                name = "Latte",
                storeName = "Kahve Durağı",
                address = "Atatürk Cd. No:12, Muratpaşa/Antalya",
                description = "Büyük boy, sütlü",
                price = "140 TL",
                originalPrice = 140,
                discountPrice = null,
                discountPercentage = null,
                imageUrl = "image2.png",
                amount = 3
            ),
            Product(
                id = 3,
                documentId = "doc3",
                name = "Çay",
                storeName = "Sokak Kahvecisi",
                address = "Yakut Çarşısı, Konyaaltı/Antalya",
                description = "İnce belli",
                price = "25 TL",
                originalPrice = 30,
                discountPrice = 25,
                discountPercentage = 17,
                imageUrl = "image3.png",
                amount = 20
            )
        )
        ProductListScreen(
            state = ProductListState(products = sample),
            onAction = {}
        )
    }
}