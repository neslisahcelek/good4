package com.good4.supporter.presentation.ordercode

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.DeepGreen
import com.good4.core.presentation.ErrorSnackbar
import com.good4.core.presentation.PrimaryGreen
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4Scaffold
import com.good4.order.domain.Order
import com.good4.order.domain.OrderItem
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.order_code_back_to_home
import good4.composeapp.generated.resources.order_code_expires
import good4.composeapp.generated.resources.order_code_items_title
import good4.composeapp.generated.resources.order_code_piece_suffix
import good4.composeapp.generated.resources.order_code_status_pending
import good4.composeapp.generated.resources.order_code_store
import good4.composeapp.generated.resources.order_code_subtitle
import good4.composeapp.generated.resources.order_code_title
import good4.composeapp.generated.resources.order_code_total
import good4.composeapp.generated.resources.price_currency_suffix
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SupporterOrderCodeScreenRoot(
    modifier: Modifier = Modifier,
    orderId: String,
    viewModel: SupporterOrderCodeViewModel,
    onBackToHome: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(orderId) {
        viewModel.loadOrder(orderId)
    }

    SupporterOrderCodeScreen(
        modifier = modifier,
        state = state,
        onDismissError = viewModel::dismissError,
        onBackToHome = onBackToHome
    )
}

@Composable
fun SupporterOrderCodeScreen(
    modifier: Modifier = Modifier,
    state: SupporterOrderCodeState,
    onDismissError: () -> Unit = {},
    onBackToHome: () -> Unit = {}
) {
    Good4Scaffold(modifier = modifier) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AppBackground)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = PrimaryGreen
                    )
                }
                state.order != null -> {
                    OrderCodeContent(
                        order = state.order,
                        currencySuffix = stringResource(Res.string.price_currency_suffix),
                        pieceSuffix = stringResource(Res.string.order_code_piece_suffix),
                        onBackToHome = onBackToHome
                    )
                }
                else -> {
                    Button(
                        onClick = onBackToHome,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TextPrimary)
                    ) {
                        Text(
                            text = stringResource(Res.string.order_code_back_to_home),
                            color = SurfaceDefault,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            ErrorSnackbar(
                modifier = Modifier.align(Alignment.TopCenter),
                errorMessage = state.errorMessage,
                onDismiss = onDismissError
            )
        }
    }
}

@Composable
private fun OrderCodeContent(
    modifier: Modifier = Modifier,
    order: Order,
    currencySuffix: String,
    pieceSuffix: String,
    onBackToHome: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OrderCodeHeader()

        Spacer(modifier = Modifier.height(24.dp))

        QrCodeSection(code = order.code)

        Spacer(modifier = Modifier.height(24.dp))

        OrderDetailCard(
            order = order,
            currencySuffix = currencySuffix,
            pieceSuffix = pieceSuffix
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onBackToHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TextPrimary,
                contentColor = SurfaceDefault
            )
        ) {
            Text(
                text = stringResource(Res.string.order_code_back_to_home),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun OrderCodeHeader(modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(Res.string.order_code_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.order_code_subtitle),
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun QrCodeSection(modifier: Modifier = Modifier, code: String) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .background(SurfaceDefault, RoundedCornerShape(20.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = rememberQrCodePainter(code),
                contentDescription = code,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .background(SurfaceDefault, RoundedCornerShape(16.dp))
                .padding(horizontal = 32.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = code.chunked(3).joinToString(" "),
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = TextPrimary,
                letterSpacing = 4.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OrderStatusBadge()
    }
}

@Composable
private fun OrderStatusBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(DeepGreen.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = PrimaryGreen,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = stringResource(Res.string.order_code_status_pending),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = PrimaryGreen
        )
    }
}

@Composable
private fun OrderDetailCard(
    modifier: Modifier = Modifier,
    order: Order,
    currencySuffix: String,
    pieceSuffix: String
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDefault, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DetailRow(label = stringResource(Res.string.order_code_store), value = order.businessName)

        HorizontalDivider(color = BorderMuted)

        Text(
            text = stringResource(Res.string.order_code_items_title),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )

        order.items.forEach { item ->
            OrderItemRow(item = item, currencySuffix = currencySuffix, pieceSuffix = pieceSuffix)
        }

        HorizontalDivider(color = BorderMuted)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.order_code_total),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = "${order.grandTotal.toInt()}$currencySuffix",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryGreen
            )
        }

        order.expiresAt?.let { expiresAt ->
            HorizontalDivider(color = BorderMuted)
            val local = expiresAt.toLocalDateTime(TimeZone.currentSystemDefault())
            val formatted = "${local.dayOfMonth.toString().padStart(2, '0')}/" +
                "${local.monthNumber.toString().padStart(2, '0')}/${local.year} " +
                "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
            DetailRow(
                label = stringResource(Res.string.order_code_expires),
                value = formatted,
                valueColor = TextSecondary
            )
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = TextPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = TextSecondary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@Composable
private fun OrderItemRow(
    item: OrderItem,
    currencySuffix: String,
    pieceSuffix: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.productName,
                fontSize = 13.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${item.quantity}$pieceSuffix × ${item.unitPrice.toInt()}$currencySuffix",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
        Text(
            text = "${item.totalPrice.toInt()}$currencySuffix",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}

@Preview
@Composable
fun SupporterOrderCodeScreenPreview() {
    MaterialTheme {
        SupporterOrderCodeScreen(
            state = SupporterOrderCodeState(isLoading = true),
            onDismissError = {},
            onBackToHome = {}
        )
    }
}
