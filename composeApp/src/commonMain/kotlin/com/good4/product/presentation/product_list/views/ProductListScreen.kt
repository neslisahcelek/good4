package com.good4.product.presentation.product_list.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.DarkBlue
import com.good4.core.presentation.ErrorSnackbar
import com.good4.product.Product
import com.good4.product.presentation.product_list.ProductListAction
import com.good4.product.presentation.product_list.ProductListState
import com.good4.product.presentation.product_list.ProductListViewModel
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.*
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.seconds

@Composable
fun ProductListScreenRoot(
    modifier: Modifier = Modifier,
    viewModel: ProductListViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.loadProductsIfNeeded()
        viewModel.loadActiveReservation()
    }
    
    ProductListScreen(
        modifier = modifier,
        state = state,
        onAction = { action ->
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
    val listState = rememberLazyListState()
    
    // Rezervasyon başarılı olunca en üste scroll et
    LaunchedEffect(state.activeReservation) {
        if (state.activeReservation != null) {
            listState.animateScrollToItem(0)
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            state.products.isEmpty() -> {
                Text(
                    text = stringResource(Res.string.products_load_error),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    // Rezervasyon kartı varsa en üstte göster
                    state.activeReservation?.let { reservation ->
                        item {
                            ReservationDetailsCard(
                                reservationCode = reservation.code,
                                product = reservation.product,
                                expiryTime = reservation.expiryTime,
                                codeId = reservation.codeId,
                                onExpired = { codeId ->
                                    onAction(ProductListAction.OnReservationExpired(codeId))
                                }
                            )
                        }
                    }
                    
                    // Ürün listesi
                    items(
                        items = state.products,
                        key = { it.documentId }
                    ) { product ->
                        ProductItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            product = product,
                            onReserveClick = { onAction(ProductListAction.OnReserveProduct(product)) },
                            isReserving = state.isReserving && state.activeReservation?.product?.documentId == product.documentId,
                            reservationSuccess = state.activeReservation?.product?.documentId == product.documentId
                        )
                    }
                }
            }
        }
        
        // Error Snackbar - üstte overlay olarak göster
        ErrorSnackbar(
            modifier = Modifier.align(Alignment.TopCenter),
            errorMessage = state.errorMessage,
            onDismiss = { onAction(ProductListAction.OnDismissError) }
        )
    }
}

@Composable
private fun ReservationDetailsCard(
    modifier: Modifier = Modifier,
    reservationCode: String,
    product: Product,
    expiryTime: Instant?,
    codeId: String,
    onExpired: (String) -> Unit
) {
    var remainingTime by remember { mutableStateOf("") }
    var isExpired by remember { mutableStateOf(false) }

    LaunchedEffect(expiryTime) {
        while (expiryTime != null && !isExpired) {
            val now = Clock.System.now()
            val diff = expiryTime - now
            
            if (diff.inWholeSeconds <= 0) {
                isExpired = true
                onExpired(codeId)
                break
            }
            
            val minutes = diff.inWholeMinutes
            val seconds = diff.inWholeSeconds % 60
            remainingTime = String.format("%02d:%02d", minutes, seconds)
            
            delay(1.seconds)
        }
    }

    Column(modifier = modifier.padding(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFF3CD)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = Color(0xFF856404),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.active_reservation_warning),
                    fontSize = 12.sp,
                    color = Color(0xFF856404),
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Rezervasyon detay kartı
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Ürün adı
                Text(
                    text = stringResource(Res.string.product_label).replace("%s", product.name),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBlue
                )

                Spacer(modifier = Modifier.height(6.dp))

                // İşletme adı
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Store,
                        contentDescription = null,
                        tint = DarkBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(Res.string.business_label).replace("%s", product.storeName),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkBlue
                    )
                }

                // Adres
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = product.address,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                // Kod
                Text(
                    text = stringResource(Res.string.reservation_code_label).replace("%s", reservationCode),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBlue
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Kalan süre
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccessTime,
                        contentDescription = null,
                        tint = if (remainingTime.startsWith("00:0")) Color.Red else DarkBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(Res.string.remaining_time).replace("%s", remainingTime),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (remainingTime.startsWith("00:0")) Color.Red else DarkBlue
                    )
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
                businessId = "business1",
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
                businessId = "business2",
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
                businessId = "business1",
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