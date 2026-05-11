package com.good4.business.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.good4.core.domain.CurrencyConstants
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.ErrorRed
import com.good4.core.presentation.PrimaryGreen
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.StandardButtonHeight
import com.good4.core.presentation.components.StandardButtonLoadingIndicatorSize
import com.good4.core.util.toInitials
import com.good4.order.domain.Order
import com.good4.order.domain.OrderItem
import com.good4.order.domain.OrderStatus
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.business_order_detail_title
import good4.composeapp.generated.resources.order_code_piece_suffix
import good4.composeapp.generated.resources.verify_code_order_cancel
import good4.composeapp.generated.resources.verify_code_order_canceling
import good4.composeapp.generated.resources.verify_code_order_items_label
import good4.composeapp.generated.resources.verify_code_order_supporter_label
import good4.composeapp.generated.resources.verify_code_order_title
import good4.composeapp.generated.resources.verify_code_order_total_label
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessOrderDetailBottomSheet(
    modifier: Modifier = Modifier,
    visible: Boolean,
    isLoading: Boolean,
    isCancellingOrder: Boolean,
    order: Order?,
    onDismiss: () -> Unit,
    onCancelOrder: () -> Unit
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        when {
            isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = TextPrimary)
                }
            }

            order != null -> {
                BusinessOrderDetailContent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    order = order,
                    isCancellingOrder = isCancellingOrder,
                    onCancelOrder = onCancelOrder
                )
            }
        }
    }
}

@Composable
private fun BusinessOrderDetailContent(
    modifier: Modifier = Modifier,
    order: Order,
    isCancellingOrder: Boolean,
    onCancelOrder: () -> Unit
) {
    val currencySuffix = CurrencyConstants.TURKISH_LIRA_SYMBOL
    val pieceSuffix = stringResource(Res.string.order_code_piece_suffix)
    val isPendingOrder = order.status == OrderStatus.PENDING

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(Res.string.business_order_detail_title),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Text(
            text = stringResource(Res.string.verify_code_order_title),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )

        HorizontalDivider(color = BorderMuted)

        OrderDetailRow(
            label = stringResource(Res.string.verify_code_order_supporter_label),
            value = order.supporterName.toInitials()
        )

        HorizontalDivider(color = BorderMuted)

        Text(
            text = stringResource(Res.string.verify_code_order_items_label),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )

        order.items.forEach { item ->
            OrderDetailItemRow(
                item = item,
                currencySuffix = currencySuffix,
                pieceSuffix = pieceSuffix
            )
        }

        HorizontalDivider(color = BorderMuted)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.verify_code_order_total_label),
                fontSize = 14.sp,
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

        if (isPendingOrder) {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(StandardButtonHeight),
                onClick = onCancelOrder,
                enabled = !isCancellingOrder,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ErrorRed,
                    disabledContainerColor = ErrorRed.copy(alpha = 0.5f)
                )
            ) {
                if (isCancellingOrder) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(StandardButtonLoadingIndicatorSize),
                        color = SurfaceDefault,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(text = stringResource(Res.string.verify_code_order_cancel))
                }
            }
        }

        if (isCancellingOrder) {
            Text(
                text = stringResource(Res.string.verify_code_order_canceling),
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun OrderDetailRow(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = TextSecondary)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
    }
}

@Composable
private fun OrderDetailItemRow(
    modifier: Modifier = Modifier,
    item: OrderItem,
    currencySuffix: String,
    pieceSuffix: String
) {
    Row(
        modifier = modifier.fillMaxWidth(),
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
