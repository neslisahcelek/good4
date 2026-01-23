package com.good4.product.presentation.product_list.views

import androidx.compose.foundation.background
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
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import com.good4.core.presentation.InkBlack
import com.good4.core.presentation.Background
import com.good4.core.presentation.ErrorSnackbar
import com.good4.core.presentation.Surface
import com.good4.core.presentation.WarningSand
import com.good4.core.presentation.WarningBrown
import com.good4.core.presentation.SlateGray
import com.good4.product.Product
import com.good4.product.presentation.product_list.ProductListAction
import com.good4.product.presentation.product_list.ProductListState
import com.good4.product.presentation.product_list.ProductListViewModel
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.active_reservation_info
import good4.composeapp.generated.resources.active_reservation_warning
import good4.composeapp.generated.resources.business_label_prefix
import good4.composeapp.generated.resources.preview_address
import good4.composeapp.generated.resources.preview_business_name
import good4.composeapp.generated.resources.preview_description
import good4.composeapp.generated.resources.preview_price
import good4.composeapp.generated.resources.preview_product_name
import good4.composeapp.generated.resources.product_label_prefix
import good4.composeapp.generated.resources.products_load_error
import good4.composeapp.generated.resources.remaining_time_prefix
import good4.composeapp.generated.resources.reservation_code_label_prefix
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
    
    LaunchedEffect(state.activeReservation) {
        if (state.activeReservation != null) {
            listState.animateScrollToItem(0)
        }
    }
    
    Scaffold(
        modifier = modifier,
        containerColor = Background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(paddingValues)
        ) {
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

            ErrorSnackbar(
                modifier = Modifier.align(Alignment.TopCenter),
                errorMessage = state.errorMessage,
                onDismiss = { onAction(ProductListAction.OnDismissError) }
            )
        }
    }
}

@Preview
@Composable
fun ProductListScreenPreview() {
    MaterialTheme {
        val productName = stringResource(Res.string.preview_product_name)
        val businessName = stringResource(Res.string.preview_business_name)
        val address = stringResource(Res.string.preview_address)
        val description = stringResource(Res.string.preview_description)
        val price = stringResource(Res.string.preview_price)
        val sample = listOf(
            Product(
                id = 1,
                documentId = "preview_doc1",
                name = productName,
                storeName = businessName,
                businessId = "preview_business",
                address = address,
                description = description,
                price = price,
                originalPrice = 100,
                discountPrice = 80,
                discountPercentage = 20,
                imageUrl = "",
                amount = 5
            ),
            Product(
                id = 2,
                documentId = "preview_doc2",
                name = productName,
                storeName = businessName,
                businessId = "preview_business",
                address = address,
                description = description,
                price = price,
                originalPrice = 140,
                discountPrice = null,
                discountPercentage = null,
                imageUrl = "",
                amount = 3
            )
        )
        ProductListScreen(
            state = ProductListState(products = sample),
            onAction = {}
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
                containerColor = WarningSand
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, WarningBrown.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .border(1.dp, WarningBrown.copy(alpha = 0.4f), RoundedCornerShape(999.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = WarningBrown,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.active_reservation_warning),
                        fontSize = 13.sp,
                        color = WarningBrown,
                        lineHeight = 18.sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = WarningBrown.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(Res.string.active_reservation_info),
                        fontSize = 12.sp,
                        color = WarningBrown.copy(alpha = 0.85f),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, SlateGray.copy(alpha = 0.12f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = stringResource(Res.string.product_label_prefix) + product.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkBlack
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Store,
                        contentDescription = null,
                        tint = InkBlack,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(Res.string.business_label_prefix) + product.storeName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = InkBlack
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = SlateGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = product.address,
                        fontSize = 12.sp,
                        color = SlateGray,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = SlateGray.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(Res.string.reservation_code_label_prefix) + reservationCode,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = InkBlack
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccessTime,
                        contentDescription = null,
                        tint = if (remainingTime.startsWith("00:0")) Color.Red else InkBlack,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(Res.string.remaining_time_prefix) + remainingTime,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (remainingTime.startsWith("00:0")) Color.Red else InkBlack
                    )
                }
            }
        }
    }
}
