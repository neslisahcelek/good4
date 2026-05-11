package com.good4.supporter.presentation.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.good4.core.domain.CurrencyConstants
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
import com.good4.core.presentation.components.ProfileTopBarAction
import com.good4.core.presentation.components.toDisplayAddressOrNull
import com.good4.core.util.openMaps
import com.good4.product.Product
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.add_to_cart
import good4.composeapp.generated.resources.ic_placeholder
import good4.composeapp.generated.resources.in_cart_label
import good4.composeapp.generated.resources.product_image_description
import good4.composeapp.generated.resources.supporter_product_list_empty
import good4.composeapp.generated.resources.supporter_products_greeting_prefix
import good4.composeapp.generated.resources.supporter_products_greeting_suffix
import good4.composeapp.generated.resources.supporter_products_name_fallback
import good4.composeapp.generated.resources.supporter_products_prompt
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SupporterProductListScreenRoot(
    modifier: Modifier = Modifier,
    viewModel: SupporterProductListViewModel,
    cartItemCounts: Map<String, Int>,
    onProfileClick: (() -> Unit)? = null,
    onAddToCart: (Product) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadProductsIfNeeded()
    }

    SupporterProductListScreen(
        modifier = modifier,
        state = state,
        cartItemCounts = cartItemCounts,
        onProfileClick = onProfileClick,
        onAddToCart = onAddToCart,
        onAction = viewModel::onAction
    )
}

@Composable
fun SupporterProductListScreen(
    modifier: Modifier = Modifier,
    state: SupporterProductListState,
    cartItemCounts: Map<String, Int> = emptyMap(),
    onProfileClick: (() -> Unit)? = null,
    onAddToCart: (Product) -> Unit = {},
    onAction: (SupporterProductListAction) -> Unit = {}
) {
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
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        item {
                            SupporterWelcomeCard(
                                supporterName = state.supporterName,
                                onProfileClick = onProfileClick
                            )
                        }
                        if (state.products.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(Res.string.supporter_product_list_empty),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        } else {
                            items(items = state.products, key = { it.documentId }) { product ->
                                SupporterProductItem(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    product = product,
                                    cartCount = cartItemCounts[product.documentId] ?: 0,
                                    onAddToCart = { onAddToCart(product) }
                                )
                            }
                        }
                    }
                }
            }

            ErrorSnackbar(
                modifier = Modifier.align(Alignment.TopCenter),
                errorMessage = state.errorMessage,
                onDismiss = { onAction(SupporterProductListAction.OnDismissError) }
            )
        }
    }
}

@Composable
private fun SupporterWelcomeCard(
    modifier: Modifier = Modifier,
    supporterName: String,
    onProfileClick: (() -> Unit)? = null
) {
    val displayName = supporterName.ifBlank {
        stringResource(Res.string.supporter_products_name_fallback)
    }
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.supporter_products_greeting_prefix) +
                        displayName +
                        stringResource(Res.string.supporter_products_greeting_suffix),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = stringResource(Res.string.supporter_products_prompt),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        if (onProfileClick != null) {
            ProfileTopBarAction(onClick = onProfileClick)
        }
    }
}

@Composable
private fun SupporterProductItem(
    modifier: Modifier = Modifier,
    product: Product,
    cartCount: Int,
    onAddToCart: () -> Unit
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = SurfaceDefault,
            border = BorderStroke(1.dp, BorderMuted),
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceMuted)
                    ) {
                        if (product.imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = product.imageUrl,
                                contentDescription = stringResource(Res.string.product_image_description),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(Res.drawable.ic_placeholder),
                                    contentDescription = stringResource(Res.string.product_image_description),
                                    modifier = Modifier.size(40.dp),
                                    alpha = 0.35f
                                )
                            }
                        }

                        if (product.discountPercentage != null && product.discountPercentage > 0) {
                            DiscountBadge(
                                modifier = Modifier.align(Alignment.TopEnd),
                                percentage = product.discountPercentage
                            )
                        }
                    }

                    ProductInfoSection(
                        modifier = Modifier.weight(1f),
                        product = product
                    )
                }

                ProductAddressRow(product = product)

                AddToCartButton(
                    cartCount = cartCount,
                    onAddToCart = onAddToCart
                )
            }
        }

        if (cartCount > 0) {
            CartBadge(
                modifier = Modifier.align(Alignment.TopStart),
                count = cartCount
            )
        }
    }
}

@Composable
private fun DiscountBadge(modifier: Modifier = Modifier, percentage: Int) {
    Surface(
        modifier = modifier,
        color = ErrorRed,
        shape = RoundedCornerShape(bottomStart = 8.dp)
    ) {
        Text(
            text = "-$percentage%",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun CartBadge(modifier: Modifier = Modifier, count: Int) {
    Surface(
        modifier = modifier,
        color = PrimaryGreen,
        shape = RoundedCornerShape(topStart = 12.dp, bottomEnd = 12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ShoppingCart,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = "x$count",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ProductInfoSection(
    modifier: Modifier = Modifier,
    product: Product
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            PriceBlock(
                product = product,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Text(
            text = product.storeName,
            style = MaterialTheme.typography.labelMedium,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (product.description.isNotBlank()) {
            Text(
                text = product.description,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = TextSecondary,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PriceBlock(
    modifier: Modifier = Modifier,
    product: Product
) {
    val currencySuffix = CurrencyConstants.TURKISH_LIRA_SYMBOL

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = product.currentSupporterPrice(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = PrimaryGreen
        )
        if (
            product.originalPrice != null &&
            product.discountPrice != null &&
            product.discountPrice < product.originalPrice
        ) {
            Text(
                text = "${product.originalPrice}$currencySuffix",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                color = TextSecondary,
                textDecoration = TextDecoration.LineThrough
            )
        }
    }
}

@Composable
private fun ProductAddressRow(
    modifier: Modifier = Modifier,
    product: Product
) {
    val displayAddress = toDisplayAddressOrNull(product.address)
    val mapsAddress = product.addressUrl
    if (displayAddress != null) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 16.dp)
                .then(
                    if (mapsAddress.isNotBlank()) {
                        Modifier.clickable { openMaps(mapsAddress) }
                    } else {
                        Modifier
                    }
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Place,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = displayAddress,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                textDecoration = if (mapsAddress.isNotBlank()) TextDecoration.Underline else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AddToCartButton(
    modifier: Modifier = Modifier,
    cartCount: Int,
    onAddToCart: () -> Unit
) {
    Button(
        onClick = onAddToCart,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .height(40.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (cartCount > 0) DeepGreen else PrimaryGreen,
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.ShoppingCart,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = if (cartCount > 0) {
                stringResource(Res.string.in_cart_label) + " (x$cartCount)"
            } else {
                stringResource(Res.string.add_to_cart)
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

private fun Product.currentSupporterPrice(): String {
    val priceToShow = discountPrice?.takeIf { discount ->
        originalPrice != null && discount < originalPrice
    } ?: price
    return "$priceToShow${CurrencyConstants.TURKISH_LIRA_SYMBOL}"
}

@Preview
@Composable
fun SupporterProductListScreenPreview() {
    MaterialTheme {
        SupporterProductListScreen(
            state = SupporterProductListState(
                products = listOf(
                    Product(
                        id = 1,
                        documentId = "p1",
                        name = "Tavuk Döner",
                        storeName = "Yakut Lokantası",
                        businessId = "b1",
                        address = "Atatürk Mah. No:12",
                        addressUrl = "",
                        description = "Günlük hazırlanan taze tavuk döner.",
                        price = 120,
                        originalPrice = 150,
                        discountPrice = 120,
                        discountPercentage = 20,
                        imageUrl = "",
                        pendingCount = 5
                    )
                )
            ),
            cartItemCounts = mapOf("p1" to 2),
            onAddToCart = {}
        )
    }
}
