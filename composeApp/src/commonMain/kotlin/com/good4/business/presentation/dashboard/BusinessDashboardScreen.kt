package com.good4.business.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.code.domain.CodeStatus
import com.good4.order.domain.OrderStatus
import com.good4.core.presentation.AccentYellow
import com.good4.core.presentation.DeepGreen
import com.good4.core.presentation.PistachioGreen
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4NestedScaffold
import com.good4.core.presentation.components.Good4TopBar
import com.good4.core.presentation.components.StatCard
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.dashboard_code_prefix
import good4.composeapp.generated.resources.dashboard_completed_codes_label
import good4.composeapp.generated.resources.dashboard_no_actions
import good4.composeapp.generated.resources.dashboard_no_orders
import good4.composeapp.generated.resources.dashboard_order_code_prefix
import good4.composeapp.generated.resources.dashboard_order_status_cancelled
import good4.composeapp.generated.resources.dashboard_order_status_confirmed
import good4.composeapp.generated.resources.dashboard_order_status_pending
import good4.composeapp.generated.resources.dashboard_pending_codes_label
import good4.composeapp.generated.resources.dashboard_recent_actions
import good4.composeapp.generated.resources.dashboard_recent_orders
import good4.composeapp.generated.resources.dashboard_section_products
import good4.composeapp.generated.resources.dashboard_section_student_codes
import good4.composeapp.generated.resources.dashboard_section_supporter_orders
import good4.composeapp.generated.resources.dashboard_status_pending
import good4.composeapp.generated.resources.dashboard_status_used
import good4.composeapp.generated.resources.dashboard_supporter_confirmed_orders
import good4.composeapp.generated.resources.dashboard_supporter_pending_orders
import good4.composeapp.generated.resources.dashboard_total_products
import good4.composeapp.generated.resources.dashboard_pending_orders_open_detail
import good4.composeapp.generated.resources.dashboard_verify_tab_hint
import good4.composeapp.generated.resources.dashboard_welcome_prefix
import good4.composeapp.generated.resources.nav_products
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessDashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: BusinessDashboardViewModel = koinViewModel(),
    onOpenProductsTab: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.dismissError()
        }
    }

    Good4NestedScaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Good4TopBar(
                titleContent = {
                    Text(
                        text = stringResource(Res.string.dashboard_welcome_prefix) + state.businessName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = TextPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    DashboardSectionTitle(
                        text = stringResource(Res.string.dashboard_section_products)
                    )
                }

                item {
                    StatCard(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(Res.string.dashboard_total_products),
                        value = state.totalProducts.toString(),
                        icon = Icons.Filled.Star,
                        color = TextPrimary,
                        onClick = onOpenProductsTab,
                        clickLabel = stringResource(Res.string.nav_products)
                    )
                }

                item {
                    DashboardSectionTitle(
                        text = stringResource(Res.string.dashboard_section_student_codes)
                    )
                }

                item {
                    StatCard(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(Res.string.dashboard_pending_codes_label),
                        value = state.pendingCount.toString(),
                        icon = Icons.Filled.DateRange,
                        color = AccentYellow
                    )
                }

                item {
                    StatCard(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(Res.string.dashboard_completed_codes_label),
                        value = state.completedCount.toString(),
                        icon = Icons.Filled.Check,
                        color = DeepGreen
                    )
                }

                item {
                    Text(
                        text = stringResource(Res.string.dashboard_recent_actions),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                if (state.recentCodes.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(Res.string.dashboard_no_actions),
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    items(state.recentCodes, key = { it.id }) { code ->
                        RecentCodeCard(code = code)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    DashboardSectionTitle(
                        text = stringResource(Res.string.dashboard_section_supporter_orders)
                    )
                }

                item {
                    StatCard(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(Res.string.dashboard_supporter_pending_orders),
                        value = state.supporterPendingCount.toString(),
                        icon = Icons.Filled.ShoppingCart,
                        color = AccentYellow,
                        onClick = if (state.supporterPendingCount > 0) {
                            { viewModel.openFirstPendingOrderDetail() }
                        } else {
                            null
                        },
                        clickLabel = stringResource(Res.string.dashboard_pending_orders_open_detail)
                    )
                }

                item {
                    StatCard(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(Res.string.dashboard_supporter_confirmed_orders),
                        value = state.supporterConfirmedCount.toString(),
                        icon = Icons.Filled.Check,
                        color = DeepGreen
                    )
                }

                item {
                    Text(
                        text = stringResource(Res.string.dashboard_recent_orders),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                if (state.recentOrders.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(Res.string.dashboard_no_orders),
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    items(state.recentOrders, key = { it.id }) { order ->
                        RecentOrderCard(
                            order = order,
                            onPendingOrderClick = viewModel::startOrderDetail
                        )
                    }
                }

                item {
                    Text(
                        modifier = Modifier.padding(vertical = 8.dp),
                        text = stringResource(Res.string.dashboard_verify_tab_hint),
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }

    BusinessOrderDetailBottomSheet(
        visible = state.orderDetailSheetVisible,
        isLoading = state.orderDetailLoading,
        isCancellingOrder = state.isCancellingOrderDetail,
        order = state.orderDetail,
        onDismiss = viewModel::dismissOrderDetail,
        onCancelOrder = viewModel::cancelOrderFromDetail
    )
}

@Composable
private fun DashboardSectionTitle(
    modifier: Modifier = Modifier,
    text: String
) {
    Text(
        modifier = modifier.fillMaxWidth(),
        text = text,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary
    )
}

@Composable
private fun RecentOrderCard(
    modifier: Modifier = Modifier,
    order: RecentOrderUiModel,
    onPendingOrderClick: (String) -> Unit
) {
    val isPending = order.orderStatus == OrderStatus.PENDING
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isPending) {
                    Modifier.clickable(
                        onClick = { onPendingOrderClick(order.id) },
                        onClickLabel = stringResource(Res.string.dashboard_pending_orders_open_detail)
                    )
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceDefault
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = order.productName,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Text(
                    text = stringResource(Res.string.dashboard_order_code_prefix) + order.code,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        when (order.orderStatus) {
                            OrderStatus.CONFIRMED -> DeepGreen.copy(alpha = 0.1f)
                            OrderStatus.CANCELLED -> TextSecondary.copy(alpha = 0.15f)
                            else -> PistachioGreen.copy(alpha = 0.3f)
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = OrderStatusLabel(order.orderStatus),
                    fontSize = 12.sp,
                    color = when (order.orderStatus) {
                        OrderStatus.CONFIRMED -> DeepGreen
                        OrderStatus.CANCELLED -> TextSecondary
                        else -> TextPrimary
                    }
                )
            }
        }
    }
}

@Composable
private fun OrderStatusLabel(status: OrderStatus): String {
    return when (status) {
        OrderStatus.PENDING -> stringResource(Res.string.dashboard_order_status_pending)
        OrderStatus.CONFIRMED -> stringResource(Res.string.dashboard_order_status_confirmed)
        OrderStatus.CANCELLED -> stringResource(Res.string.dashboard_order_status_cancelled)
        OrderStatus.COMPLETED -> stringResource(Res.string.dashboard_order_status_confirmed)
    }
}

@Composable
private fun RecentCodeCard(
    modifier: Modifier = Modifier,
    code: RecentCodeUiModel
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceDefault
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = code.productName,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Text(
                    text = stringResource(Res.string.dashboard_code_prefix) + code.codeValue,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (code.statusEnum == CodeStatus.USED) DeepGreen.copy(alpha = 0.1f)
                        else PistachioGreen.copy(alpha = 0.3f)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (code.statusEnum == CodeStatus.USED) {
                        stringResource(Res.string.dashboard_status_used)
                    } else {
                        stringResource(Res.string.dashboard_status_pending)
                    },
                    fontSize = 12.sp,
                    color = if (code.statusEnum == CodeStatus.USED) DeepGreen else TextPrimary
                )
            }
        }
    }
}

@Preview
@Composable
fun BusinessDashboardScreenPreview() {
    MaterialTheme {
        BusinessDashboardScreen()
    }
}
