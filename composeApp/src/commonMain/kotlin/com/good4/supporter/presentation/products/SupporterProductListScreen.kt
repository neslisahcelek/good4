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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.good4.core.presentation.components.Good4Scaffold
import com.good4.core.util.openMaps
import com.good4.core.util.toDisplayAddress
import com.good4.product.Product
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.add_to_cart
import good4.composeapp.generated.resources.ic_placeholder
import good4.composeapp.generated.resources.in_cart_label
import good4.composeapp.generated.resources.price_currency_suffix
import good4.composeapp.generated.resources.product_address_maps_hint
import good4.composeapp.generated.resources.product_image_description
import good4.composeapp.generated.resources.supporter_product_list_empty
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SupporterProductListScreenRoot(
    modifier: Modifier = Modifier,
    viewModel: SupporterProductListViewModel,
    cartItemCounts: Map<String, Int>,
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
        onAddToCart = onAddToCart,
        onAction = viewModel::onAction
    )
}

@Composable
fun SupporterProductListScreen(
    modifier: Modifier = Modifier,
    state: SupporterProductListState,
    cartItemCounts: Map<String, Int> = emptyMap(),
    onAddToCart: (Product) -> Unit = {},
    onAction: (SupporterProductListAction) -> Unit = {}
) {
    Good4Scaffold(modifier = modifier) { paddingValues ->
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
                state.products.isEmpty() -> {
                    Text(
                        text = stringResource(Res.string.supporter_product_list_empty),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items = state.products, key = { it.documentId }) { product ->
                            SupporterProductItem(
                                modifier = Modifier.fillMaxWidth(),
                                product = product,
                                cartCount = cartItemCounts[product.documentId] ?: 0,
                                onAddToCart = { onAddToCart(product) }
                            )
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
private fun SupporterProductItem(
    modifier: Modifier = Modifier,
    product: Product,
    cartCount: Int,
    onAddToCart: () -> Unit
) {
    val currencySuffix = stringResource(Res.string.price_currency_suffix)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceDefault,
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.5f)),
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ProductImageArea(
                imageUrl = product.imageUrl,
                discountPercentage = product.discountPercentage,
                cartCount = cartCount
            )
            ProductInfoSection(
                product = product,
                cartCount = cartCount,
                currencySuffix = currencySuffix,
                onAddToCart = onAddToCart
            )
        }
    }
}

@Composable
private fun ProductImageArea(
    modifier: Modifier = Modifier,
    imageUrl: String,
    discountPercentage: Int?,
    cartCount: Int
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(SurfaceMuted)
    ) {
        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = stringResource(Res.string.product_image_description),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(Res.drawable.ic_placeholder),
                    contentDescription = stringResource(Res.string.product_image_description),
                    modifier = Modifier.size(48.dp),
                    alpha = 0.3f
                )
            }
        }

        if (discountPercentage != null && discountPercentage > 0) {
            DiscountBadge(
                modifier = Modifier.align(Alignment.TopEnd),
                percentage = discountPercentage
            )
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
        shape = RoundedCornerShape(bottomStart = 12.dp)
    ) {
        Text(
            text = "-$percentage%",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun CartBadge(modifier: Modifier = Modifier, count: Int) {
    Surface(
        modifier = modifier,
        color = DeepGreen,
        shape = RoundedCornerShape(bottomEnd = 12.dp)
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
    product: Product,
    cartCount: Int,
    currencySuffix: String,
    onAddToCart: () -> Unit
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.5).sp
                ),
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = product.storeName,
                style = MaterialTheme.typography.bodyMedium,
                color = PrimaryGreen,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (product.description.isNotBlank()) {
            Text(
                text = product.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
        }

        PriceRow(
            originalPrice = product.originalPrice,
            discountPrice = product.discountPrice,
            displayPrice = product.price,
            currencySuffix = currencySuffix
        )

        if (product.address.isNotBlank()) {
            val displayAddress = toDisplayAddress(
                rawAddress = product.address,
                mapsFallbackLabel = stringResource(Res.string.product_address_maps_hint)
            )
            Row(
                modifier = Modifier.clickable { openMaps(product.address) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Place,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = displayAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textDecoration = TextDecoration.Underline,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Button(
            onClick = onAddToCart,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
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
}

@Composable
private fun PriceRow(
    originalPrice: Int?,
    discountPrice: Int?,
    displayPrice: Int,
    currencySuffix: String
) {
    if (originalPrice != null && discountPrice != null && discountPrice < originalPrice) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$originalPrice$currencySuffix",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textDecoration = TextDecoration.LineThrough
            )
            Text(
                text = "$discountPrice$currencySuffix",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PrimaryGreen
            )
        }
    } else {
        Text(
            text = "$displayPrice$currencySuffix",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
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
                        description = "Günlük hazırlanan taze tavuk döner.",
                        price = 120,
                        originalPrice = 150,
                        discountPrice = 120,
                        discountPercentage = 20,
                        imageUrl = "",
                        amount = 5
                    )
                )
            ),
            cartItemCounts = mapOf("p1" to 2),
            onAddToCart = {}
        )
    }
}
