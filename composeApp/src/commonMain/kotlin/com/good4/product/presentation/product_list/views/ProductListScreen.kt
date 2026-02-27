package com.good4.product.presentation.product_list.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.code.domain.CodeStatus
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.ErrorSnackbar
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4Scaffold
import com.good4.core.presentation.components.ReservationCard
import com.good4.product.Product
import com.good4.product.presentation.product_list.ProductListAction
import com.good4.product.presentation.product_list.ProductListState
import com.good4.product.presentation.product_list.ProductListViewModel
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.active_reservation_title
import good4.composeapp.generated.resources.preview_address
import good4.composeapp.generated.resources.preview_business_name
import good4.composeapp.generated.resources.preview_description
import good4.composeapp.generated.resources.preview_price
import good4.composeapp.generated.resources.preview_product_name
import good4.composeapp.generated.resources.products_load_error
import good4.composeapp.generated.resources.time_minute_suffix
import good4.composeapp.generated.resources.time_second_suffix
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
        viewModel.loadStudentInfo()
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

    Good4Scaffold(
        modifier = modifier,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
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
                        if (state.userName != null && state.remainingCredits != null && state.deliveryTimeMinutes != null) {
                            item {
                                StudentStatusCard(
                                    userName = state.userName,
                                    remainingCredits = state.remainingCredits,
                                    renewalDuration = state.creditRenewalDuration,
                                    deliveryTimeMinutes = state.deliveryTimeMinutes
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        state.activeReservation?.let { reservation ->
                            item {
                                ReservationDetailsCard(
                                    reservationCode = reservation.code,
                                    product = reservation.product,
                                    expiryTime = reservation.expiryTime,
                                    codeId = reservation.codeId,
                                    expirationMinutes = state.reservationExpirationMinutes,
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

        val sample = listOf(
            Product(
                id = 1,
                documentId = "preview_doc1",
                name = productName,
                storeName = businessName,
                businessId = "preview_business",
                address = address,
                description = description,
                price = 120,
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
                price = 0,
                originalPrice = null,
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
    expirationMinutes: Long?,
    onExpired: (String) -> Unit
) {
    var remainingTime by remember { mutableStateOf("") }
    var isExpired by remember { mutableStateOf(false) }
    val minuteSuffix = stringResource(Res.string.time_minute_suffix)
    val secondSuffix = stringResource(Res.string.time_second_suffix)

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
            remainingTime = "${minutes}${minuteSuffix} ${seconds}${secondSuffix}"
            
            delay(1.seconds)
        }
    }

    Column(modifier = modifier.padding(12.dp)) {
        ReservationCard(
            title = stringResource(Res.string.active_reservation_title),
            productName = product.name,
            businessName = product.storeName,
            status = CodeStatus.PENDING,
            code = reservationCode,
            remainingTime = remainingTime.ifBlank { null }
        )
    }
}


