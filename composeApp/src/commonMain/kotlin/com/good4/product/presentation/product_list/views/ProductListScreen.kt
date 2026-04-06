package com.good4.product.presentation.product_list.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.ErrorSnackbar
import com.good4.core.presentation.PistachioGreen
import com.good4.core.presentation.PrimaryGreen
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.SurfaceMuted
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4NestedScaffold
import com.good4.core.presentation.components.ProfileTopBarAction
import com.good4.core.presentation.components.toDisplayAddressOrNull
import com.good4.core.util.ReservationTimeCalculator
import com.good4.core.util.openMaps
import com.good4.product.Product
import com.good4.product.presentation.product_list.ProductListAction
import com.good4.product.presentation.product_list.ProductListState
import com.good4.product.presentation.product_list.ProductListViewModel
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.preview_address
import good4.composeapp.generated.resources.preview_business_name
import good4.composeapp.generated.resources.preview_description
import good4.composeapp.generated.resources.preview_product_name
import good4.composeapp.generated.resources.product_list_active_reservation_title
import good4.composeapp.generated.resources.product_list_countdown_prefix
import good4.composeapp.generated.resources.product_list_greeting_prefix
import good4.composeapp.generated.resources.product_list_greeting_suffix
import good4.composeapp.generated.resources.product_list_order_code_label
import good4.composeapp.generated.resources.products_load_error
import good4.composeapp.generated.resources.reservation_status_pending
import good4.composeapp.generated.resources.time_minute_suffix
import good4.composeapp.generated.resources.time_second_suffix
import kotlinx.coroutines.delay
import kotlinx.datetime.Instant
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.seconds

@Composable
fun ProductListScreenRoot(
    modifier: Modifier = Modifier,
    viewModel: ProductListViewModel = koinViewModel(),
    onProfileClick: (() -> Unit)? = null,
    onReservationCardClick: () -> Unit = {}
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
        onProfileClick = onProfileClick,
        onReservationCardClick = onReservationCardClick,
        onAction = { action ->
            viewModel.onAction(action)
        }
    )
}

@Composable
fun ProductListScreen(
    modifier: Modifier = Modifier,
    state: ProductListState,
    onProfileClick: (() -> Unit)? = null,
    onReservationCardClick: () -> Unit = {},
    onAction: (ProductListAction) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.activeReservation) {
        if (state.activeReservation != null) {
            listState.animateScrollToItem(0)
        }
    }

    Good4NestedScaffold(
        modifier = modifier
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
                        modifier = Modifier.align(Alignment.Center),
                        color = PrimaryGreen
                    )
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = if (onProfileClick == null) {
                                WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
                            } else {
                                0.dp
                            },
                            bottom = 16.dp
                        )
                    ) {
                        if (state.userName != null) {
                            item {
                                ProductListGreetingHeader(
                                    userName = state.userName,
                                    onProfileClick = onProfileClick
                                )
                            }
                        }

                        if (state.remainingCredits != null && state.deliveryTimeMinutes != null) {
                            item {
                                StudentStatusCard(
                                    remainingCredits = state.remainingCredits,
                                    deliveryTimeMinutes = state.deliveryTimeMinutes
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        state.activeReservation?.let { reservation ->
                            item {
                                ProductListActiveReservationCard(
                                    reservationCode = reservation.code,
                                    product = reservation.product,
                                    expiryTime = reservation.expiryTime,
                                    codeId = reservation.codeId,
                                    onClick = onReservationCardClick,
                                    onExpired = { codeId ->
                                        onAction(ProductListAction.OnReservationExpired(codeId))
                                    }
                                )
                            }
                        }

                        if (state.products.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(Res.string.products_load_error),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp).padding(top = 60.dp),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        } else {
                            items(
                                items = state.products,
                                key = { it.documentId }
                            ) { product ->
                                val isReservedProduct =
                                    state.activeReservation?.product?.documentId == product.documentId
                                val canReserve =
                                    state.activeReservation == null && !state.isReserving

                                ProductItem(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    product = product,
                                    onReserveClick = {
                                        onAction(
                                            ProductListAction.OnReserveProduct(
                                                product
                                            )
                                        )
                                    },
                                    isReserving = state.isReserving &&
                                            state.reservingProductId == product.documentId,
                                    reservationSuccess = isReservedProduct,
                                    isReserveEnabled = canReserve
                                )
                            }
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

@Composable
private fun ProductListGreetingHeader(
    modifier: Modifier = Modifier,
    userName: String,
    onProfileClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding() + 8.dp,
                bottom = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(Res.string.product_list_greeting_prefix) +
                    userName +
                    stringResource(Res.string.product_list_greeting_suffix),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        if (onProfileClick != null) {
            ProfileTopBarAction(onClick = onProfileClick)
        }
    }
}

@Composable
private fun ProductListSectionTitle(
    modifier: Modifier = Modifier,
    title: String
) {
    Text(
        text = title,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
    )
}

@Composable
private fun ProductListActiveReservationCard(
    modifier: Modifier = Modifier,
    reservationCode: String,
    product: Product,
    expiryTime: Instant?,
    codeId: String,
    onClick: () -> Unit = {},
    onExpired: (String) -> Unit
) {
    var remainingTime by remember { mutableStateOf("") }
    var isExpired by remember { mutableStateOf(false) }
    val minuteSuffix = stringResource(Res.string.time_minute_suffix)
    val secondSuffix = stringResource(Res.string.time_second_suffix)

    LaunchedEffect(expiryTime) {
        while (expiryTime != null && !isExpired) {
            val remainingSeconds = ReservationTimeCalculator.remainingSecondsUntilExpiry(
                expiresAtEpochSeconds = expiryTime.epochSeconds
            ) ?: break
            if (remainingSeconds <= 0) {
                isExpired = true
                onExpired(codeId)
                break
            }

            remainingTime = ReservationTimeCalculator.formatRemainingTimeFromExpiry(
                expiresAtEpochSeconds = expiryTime.epochSeconds,
                minuteSuffix = minuteSuffix,
                secondSuffix = secondSuffix,
                expiredLabel = ""
            ).orEmpty()

            delay(1.seconds)
        }
    }

    val displayAddress = toDisplayAddressOrNull(product.address)
    val mapsAddress = product.addressUrl.ifBlank { null }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = PistachioGreen,
        border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.3f)),
        shadowElevation = 3.dp
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            shape = RoundedCornerShape(14.dp),
            color = SurfaceDefault,
            border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.product_list_active_reservation_title),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = PrimaryGreen
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PistachioGreen
                    ) {
                        Text(
                            text = stringResource(Res.string.reservation_status_pending),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                HorizontalDivider(color = BorderMuted.copy(alpha = 0.4f))

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceMuted),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Store,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = product.storeName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (displayAddress != null) {
                            Text(
                                text = displayAddress,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = if (mapsAddress != null) {
                                    Modifier.clickable { openMaps(mapsAddress) }
                                } else {
                                    Modifier
                                },
                                textDecoration = if (mapsAddress != null) TextDecoration.Underline else null
                            )
                        }
                    }
                }

                if (reservationCode.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(Res.string.product_list_order_code_label),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary.copy(alpha = 0.65f)
                            )
                            Text(
                                text = reservationCode,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = PrimaryGreen,
                                letterSpacing = 0.5.sp
                            )
                        }
                        if (remainingTime.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(Res.string.product_list_countdown_prefix) +
                                            remainingTime,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen
                                )
                            }
                        }
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
                addressUrl = "",
                description = description,
                price = 120,
                originalPrice = 100,
                discountPrice = 80,
                discountPercentage = 20,
                imageUrl = "",
                pendingCount = 5
            ),
            Product(
                id = 2,
                documentId = "preview_doc2",
                name = productName,
                storeName = businessName,
                businessId = "preview_business",
                address = address,
                addressUrl = "",
                description = description,
                price = 0,
                originalPrice = null,
                discountPrice = null,
                discountPercentage = null,
                imageUrl = "",
                pendingCount = 3
            )
        )
        ProductListScreen(
            state = ProductListState(products = sample),
            onAction = {}
        )
    }
}
