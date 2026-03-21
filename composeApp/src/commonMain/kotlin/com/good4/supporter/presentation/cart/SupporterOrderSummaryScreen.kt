package com.good4.supporter.presentation.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.DeepGreen
import com.good4.core.presentation.ErrorSnackbar
import com.good4.core.presentation.PrimaryGreen
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.SurfaceMuted
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4NestedScaffold
import com.good4.core.presentation.components.Good4TopBar
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.price_currency_suffix
import good4.composeapp.generated.resources.supporter_cart_total
import good4.composeapp.generated.resources.supporter_order_summary_cancel
import good4.composeapp.generated.resources.supporter_order_summary_card_title
import good4.composeapp.generated.resources.supporter_order_summary_confirm
import good4.composeapp.generated.resources.supporter_order_summary_info_text
import good4.composeapp.generated.resources.supporter_order_summary_info_title
import good4.composeapp.generated.resources.supporter_order_summary_subtotal
import good4.composeapp.generated.resources.supporter_order_summary_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun SupporterOrderSummaryScreen(
    modifier: Modifier = Modifier,
    state: SupporterCartState,
    onAction: (SupporterCartAction) -> Unit
) {
    val currencySuffix = stringResource(Res.string.price_currency_suffix)

    Good4NestedScaffold(
        modifier = modifier,
        topBar = {
            Good4TopBar(
                title = stringResource(Res.string.supporter_order_summary_title)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AppBackground)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (state.items.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDefault, RoundedCornerShape(16.dp))
                        .border(1.dp, BorderMuted, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.supporter_order_summary_card_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceMuted, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Store,
                            contentDescription = null,
                            tint = DeepGreen
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = state.items.first().product.storeName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    state.items.forEach { cartItem ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${cartItem.quantity}x ${cartItem.product.name}",
                                fontSize = 14.sp,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            val unitPrice = (
                                    cartItem.product.discountPrice
                                        ?: cartItem.product.originalPrice
                                        ?: cartItem.product.price
                                    ).toDouble()
                            Text(
                                text = "${(unitPrice * cartItem.quantity).toInt()}$currencySuffix",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }

                    HorizontalDivider(color = BorderMuted)

                    SummaryRow(
                        label = stringResource(Res.string.supporter_order_summary_subtotal),
                        value = "${state.totalPrice.toInt()}$currencySuffix",
                        isEmphasized = false
                    )
                    SummaryRow(
                        label = stringResource(Res.string.supporter_cart_total),
                        value = "${state.totalPrice.toInt()}$currencySuffix",
                        isEmphasized = true
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDefault, RoundedCornerShape(14.dp))
                        .border(1.dp, DeepGreen.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.supporter_order_summary_info_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(Res.string.supporter_order_summary_info_text),
                        fontSize = 15.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { onAction(SupporterCartAction.OnConfirmCreateOrder) },
                enabled = state.items.isNotEmpty() && !state.isCreatingOrder,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen,
                    contentColor = SurfaceDefault
                )
            ) {
                if (state.isCreatingOrder) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        color = SurfaceDefault,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "${stringResource(Res.string.supporter_order_summary_confirm)} - ${state.totalPrice.toInt()}$currencySuffix",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = { onAction(SupporterCartAction.OnCancelOrderReview) },
                enabled = !state.isCreatingOrder,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
            ) {
                Text(
                    text = stringResource(Res.string.supporter_order_summary_cancel),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            ErrorSnackbar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                errorMessage = state.errorMessage,
                onDismiss = { onAction(SupporterCartAction.OnDismissError) },
                addTopSafeArea = false
            )
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    isEmphasized: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = if (isEmphasized) 16.sp else 14.sp,
            color = if (isEmphasized) TextPrimary else TextSecondary,
            fontWeight = if (isEmphasized) FontWeight.Bold else FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = if (isEmphasized) 20.sp else 14.sp,
            color = TextPrimary,
            fontWeight = if (isEmphasized) FontWeight.Bold else FontWeight.Medium
        )
    }
}
