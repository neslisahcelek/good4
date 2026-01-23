package com.good4.product.presentation.product_list.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.good4.core.presentation.BrickRed
import com.good4.core.presentation.LimeGreen
import com.good4.core.presentation.SlateGray
import com.good4.core.presentation.UiText
import com.good4.core.presentation.Surface
import com.good4.product.Product
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.ic_placeholder
import good4.composeapp.generated.resources.product_image_description
import good4.composeapp.generated.resources.preview_address
import good4.composeapp.generated.resources.preview_business_name
import good4.composeapp.generated.resources.preview_description
import good4.composeapp.generated.resources.preview_price
import good4.composeapp.generated.resources.preview_product_name
import good4.composeapp.generated.resources.reserved
import good4.composeapp.generated.resources.reserve_button_label
import good4.composeapp.generated.resources.price_currency_suffix
import good4.composeapp.generated.resources.discount_badge_prefix
import good4.composeapp.generated.resources.discount_badge_suffix
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProductItem(
    modifier: Modifier = Modifier,
    product: Product,
    onReserveClick: (() -> Unit)? = null,
    isReserving: Boolean = false,
    reservationSuccess: Boolean = false
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = Surface
        )
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                if (product.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = stringResource(Res.string.product_image_description),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(Res.drawable.ic_placeholder),
                        contentDescription = stringResource(Res.string.product_image_description),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.titleLarge.fontSize,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = product.storeName,
                    fontSize = MaterialTheme.typography.titleMedium.fontSize,
                    color = SlateGray,
                    modifier = Modifier.fillMaxWidth()
                )
                AddressRow(
                    address = UiText.DynamicString(product.address),
                    modifier = Modifier.padding(top = 4.dp)
                )
                PriceRow(product = product)
                Text(
                    text = product.description,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AmountCard(amount = product.amount)
                    ReserveButtonCard(
                        onClick = if (reservationSuccess) null else onReserveClick,
                        isLoading = isReserving,
                        label = if (reservationSuccess) UiText.StringResourceId(id = Res.string.reserved) else UiText.StringResourceId(
                            id = Res.string.reserve_button_label
                        )
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun ProductItemPreview(modifier: Modifier = Modifier) {
    MaterialTheme {
        val productName = stringResource(Res.string.preview_product_name)
        val businessName = stringResource(Res.string.preview_business_name)
        val address = stringResource(Res.string.preview_address)
        val description = stringResource(Res.string.preview_description)
        val price = stringResource(Res.string.preview_price)
        ProductItem(
            product = Product(
                id = 1,
                documentId = "preview_doc1",
                name = productName,
                storeName = businessName,
                businessId = "preview_business",
                address = address,
                description = description,
                price = price,
                imageUrl = "",
                amount = 5,
                originalPrice = 100,
                discountPrice = 60,
                discountPercentage = 40
            ),
            modifier = modifier
        )
    }
}

@Composable
private fun PriceRow(
    modifier: Modifier = Modifier,
    product: Product
) {
    val currencySuffix = stringResource(Res.string.price_currency_suffix)
    val discountSuffix = stringResource(Res.string.discount_badge_suffix)
    val discountPrefix = stringResource(Res.string.discount_badge_prefix)
    val hasDiscount = product.discountPrice != null && product.originalPrice != null
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (product.discountPrice != null) {
            Text(
                text = "${product.discountPrice} $currencySuffix",
                fontWeight = FontWeight.Bold,
                color = LimeGreen
            )
        } else {
            Text(
                text = "${product.price} $currencySuffix",
                fontWeight = FontWeight.Bold,
                color = LimeGreen
            )
        }
        if (hasDiscount) {
            Text(
                text = "${product.originalPrice} $currencySuffix",
                color = SlateGray,
                fontSize = 12.sp,
                textDecoration = TextDecoration.LineThrough
            )
            product.discountPercentage?.let {
                if (it > 0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BrickRed.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "$discountPrefix${product.discountPercentage}$discountSuffix",
                            color = BrickRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
