package com.good4.supporter.presentation.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.good4.core.presentation.ErrorRed
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
import good4.composeapp.generated.resources.supporter_cart
import good4.composeapp.generated.resources.supporter_cart_create_order
import good4.composeapp.generated.resources.supporter_cart_creating_order
import good4.composeapp.generated.resources.supporter_cart_empty
import good4.composeapp.generated.resources.supporter_cart_empty_subtitle
import good4.composeapp.generated.resources.supporter_cart_item_remove
import good4.composeapp.generated.resources.supporter_cart_total
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SupporterCartScreen(
    modifier: Modifier = Modifier,
    state: SupporterCartState,
    onAction: (SupporterCartAction) -> Unit
) {
    Good4NestedScaffold(
        modifier = modifier,
        topBar = {
            Good4TopBar(
                titleContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(Res.string.supporter_cart),
                            fontWeight = FontWeight.SemiBold
                        )
                        if (state.totalItemCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .background(DeepGreen, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = state.totalItemCount.toString(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.items.isEmpty()) {
                CartEmptyContent()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppBackground)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(12.dp)) }
                        items(state.items, key = { it.product.documentId }) { cartItem ->
                            CartItemCard(
                                cartItem = cartItem,
                                currencySuffix = stringResource(Res.string.price_currency_suffix),
                                onIncrease = { onAction(SupporterCartAction.OnIncreaseQuantity(cartItem.product.documentId)) },
                                onDecrease = { onAction(SupporterCartAction.OnDecreaseQuantity(cartItem.product.documentId)) },
                                onRemove = { onAction(SupporterCartAction.OnRemoveItem(cartItem.product.documentId)) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }

                    CartBottomBar(
                        state = state,
                        currencySuffix = stringResource(Res.string.price_currency_suffix),
                        onCreateOrder = { onAction(SupporterCartAction.OnCreateOrder) }
                    )
                }
            }

            ErrorSnackbar(
                modifier = Modifier.align(Alignment.TopCenter),
                errorMessage = state.errorMessage,
                onDismiss = { onAction(SupporterCartAction.OnDismissError) }
            )
        }
    }
}

@Composable
private fun CartEmptyContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.ShoppingCart,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = BorderMuted
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(Res.string.supporter_cart_empty),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.supporter_cart_empty_subtitle),
            fontSize = 13.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun CartItemCard(
    modifier: Modifier = Modifier,
    cartItem: CartItem,
    currencySuffix: String,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    val unitPrice = (cartItem.product.discountPrice
        ?: cartItem.product.originalPrice
        ?: cartItem.product.price).toDouble()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDefault, RoundedCornerShape(12.dp))
            .border(1.dp, BorderMuted, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cartItem.product.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = cartItem.product.storeName,
                fontSize = 12.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${unitPrice.toInt()}$currencySuffix",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryGreen
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onDecrease,
                modifier = Modifier
                    .size(32.dp)
                    .background(SurfaceMuted, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(Res.string.supporter_cart_item_remove),
                    modifier = Modifier.size(14.dp),
                    tint = TextPrimary
                )
            }
            Text(
                text = cartItem.quantity.toString(),
                modifier = Modifier.padding(horizontal = 12.dp),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            IconButton(
                onClick = onIncrease,
                modifier = Modifier
                    .size(32.dp)
                    .background(DeepGreen, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = TextPrimary
                )
            }
        }
        Spacer(modifier = Modifier.width(4.dp))
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = stringResource(Res.string.supporter_cart_item_remove),
                modifier = Modifier.size(18.dp),
                tint = ErrorRed
            )
        }
    }
}

@Composable
private fun CartBottomBar(
    modifier: Modifier = Modifier,
    state: SupporterCartState,
    currencySuffix: String,
    onCreateOrder: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDefault)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.supporter_cart_total),
                fontSize = 15.sp,
                color = TextSecondary
            )
            Text(
                text = "${state.totalPrice.toInt()}$currencySuffix",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = onCreateOrder,
            enabled = !state.isCreatingOrder,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryGreen,
                contentColor = SurfaceDefault
            )
        ) {
            if (state.isCreatingOrder) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = SurfaceDefault,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.supporter_cart_creating_order),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Text(
                    text = stringResource(Res.string.supporter_cart_create_order),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Preview
@Composable
fun SupporterCartScreenPreview() {
    MaterialTheme {
        SupporterCartScreen(
            state = SupporterCartState(),
            onAction = {}
        )
    }
}
