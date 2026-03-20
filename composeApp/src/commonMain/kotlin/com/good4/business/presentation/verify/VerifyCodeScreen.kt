package com.good4.business.presentation.verify

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.DeepGreen
import com.good4.core.presentation.ErrorRed
import com.good4.core.presentation.PrimaryGreen
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4NestedScaffold
import com.good4.core.presentation.components.Good4TopBar
import com.good4.core.util.singleClick
import com.good4.order.domain.Order
import com.good4.order.domain.OrderItem
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.emoji_ticket
import good4.composeapp.generated.resources.enter_code
import good4.composeapp.generated.resources.order_code_piece_suffix
import good4.composeapp.generated.resources.price_currency_suffix
import good4.composeapp.generated.resources.verify_code
import good4.composeapp.generated.resources.verify_code_button
import good4.composeapp.generated.resources.verify_code_input_label
import good4.composeapp.generated.resources.verify_code_new
import good4.composeapp.generated.resources.verify_code_order_cancel
import good4.composeapp.generated.resources.verify_code_order_canceling
import good4.composeapp.generated.resources.verify_code_order_cancelled
import good4.composeapp.generated.resources.verify_code_order_confirm
import good4.composeapp.generated.resources.verify_code_order_confirmed
import good4.composeapp.generated.resources.verify_code_order_items_label
import good4.composeapp.generated.resources.verify_code_order_supporter_label
import good4.composeapp.generated.resources.verify_code_order_title
import good4.composeapp.generated.resources.verify_code_order_total_label
import good4.composeapp.generated.resources.verify_code_product_prefix
import good4.composeapp.generated.resources.verify_code_success
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun VerifyCodeScreen(
    modifier: Modifier = Modifier,
    viewModel: VerifyCodeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val onVerifyClick = remember {
        singleClick {
            focusManager.clearFocus()
            viewModel.verifyCode()
        }
    }

    Good4NestedScaffold(
        modifier = modifier,
        topBar = { Good4TopBar(title = stringResource(Res.string.verify_code)) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(text = stringResource(Res.string.emoji_ticket), fontSize = 64.sp)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                text = stringResource(Res.string.enter_code),
                fontSize = 22.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = state.codeInput,
                onValueChange = { viewModel.onCodeInputChange(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.verify_code_input_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        viewModel.verifyCode()
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TextPrimary,
                    focusedLabelColor = TextPrimary,
                    cursorColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onVerifyClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !state.isLoading && state.codeInput.length == 6,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TextPrimary,
                    disabledContainerColor = TextPrimary.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = SurfaceDefault,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.verify_code_button),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            when {
                state.verificationSuccess -> {
                    VerificationResultCard(
                        isSuccess = true,
                        productName = state.verifiedProductName,
                        message = stringResource(Res.string.verify_code_success)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.resetState() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.verify_code_new),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                state.orderConfirmedSuccess -> {
                    VerificationResultCard(
                        isSuccess = true,
                        productName = null,
                        message = stringResource(Res.string.verify_code_order_confirmed)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.resetState() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.verify_code_new),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                state.orderCancelledSuccess -> {
                    VerificationResultCard(
                        isSuccess = true,
                        productName = null,
                        message = stringResource(Res.string.verify_code_order_cancelled)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.resetState() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.verify_code_new),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                state.pendingOrder != null -> {
                    state.pendingOrder?.let { order ->
                        OrderConfirmCard(
                            order = order,
                            isConfirming = state.isConfirmingOrder,
                            isCancelling = state.isCancellingOrder,
                            onConfirm = { viewModel.confirmOrder() },
                            onCancel = { viewModel.cancelOrder() }
                        )
                    }
                }

                state.errorMessage != null -> {
                    VerificationResultCard(
                        isSuccess = false,
                        productName = null,
                        message = state.errorMessage.orEmpty()
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderConfirmCard(
    modifier: Modifier = Modifier,
    order: Order,
    isConfirming: Boolean,
    isCancelling: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val currencySuffix = stringResource(Res.string.price_currency_suffix)
    val pieceSuffix = stringResource(Res.string.order_code_piece_suffix)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDefault),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(Res.string.verify_code_order_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            HorizontalDivider(color = BorderMuted)

            OrderConfirmRow(
                label = stringResource(Res.string.verify_code_order_supporter_label),
                value = order.supporterName
            )

            HorizontalDivider(color = BorderMuted)

            Text(
                text = stringResource(Res.string.verify_code_order_items_label),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )

            order.items.forEach { item ->
                OrderConfirmItemRow(
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onCancel,
                    enabled = !isConfirming && !isCancelling,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRed,
                        disabledContainerColor = ErrorRed.copy(alpha = 0.5f)
                    )
                ) {
                    if (isCancelling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = SurfaceDefault,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(Res.string.verify_code_order_cancel),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Button(
                    onClick = onConfirm,
                    enabled = !isConfirming && !isCancelling,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DeepGreen,
                        disabledContainerColor = DeepGreen.copy(alpha = 0.5f)
                    )
                ) {
                    if (isConfirming) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = SurfaceDefault,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(Res.string.verify_code_order_confirm),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (isCancelling) {
                Text(
                    text = stringResource(Res.string.verify_code_order_canceling),
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun OrderConfirmRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = TextSecondary)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
    }
}

@Composable
private fun OrderConfirmItemRow(
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

@Composable
private fun VerificationResultCard(
    modifier: Modifier = Modifier,
    isSuccess: Boolean,
    productName: String?,
    message: String
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSuccess) DeepGreen.copy(alpha = 0.1f) else ErrorRed.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isSuccess) Icons.Filled.CheckCircle else Icons.Filled.Close,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (isSuccess) DeepGreen else ErrorRed
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = message,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSuccess) DeepGreen else ErrorRed,
                textAlign = TextAlign.Center
            )

            if (productName != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.verify_code_product_prefix) + productName,
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Preview
@Composable
fun VerifyCodeScreenPreview() {
    MaterialTheme {
        VerifyCodeScreen()
    }
}
