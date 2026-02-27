package com.good4.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.good4.core.presentation.DeepGreen
import com.good4.core.presentation.ErrorRed
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.product.Product
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.business_products_price_unavailable
import good4.composeapp.generated.resources.business_products_stock_prefix
import good4.composeapp.generated.resources.emoji_product_placeholder
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProductListCard(
    product: Product,
    currencySuffix: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    showStoreName: Boolean = false
) {
    val cardModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    } else {
        modifier.fillMaxWidth()
    }

    Card(
        modifier = cardModifier,
        colors = CardDefaults.cardColors(containerColor = SurfaceDefault),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ImagePreviewBox(
                imageUrl = product.imageUrl,
                placeholderText = stringResource(Res.string.emoji_product_placeholder),
                modifier = Modifier.size(80.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (showStoreName && product.storeName.isNotBlank()) {
                    Spacer(modifier = Modifier.padding(top = 2.dp))
                    Text(
                        text = product.storeName,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (product.description.isNotBlank()) {
                    Spacer(modifier = Modifier.padding(top = 4.dp))
                    Text(
                        text = product.description,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.padding(top = 8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when {
                        product.discountPrice != null && product.originalPrice != null -> {
                            Text(
                                text = "${product.discountPrice} $currencySuffix",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepGreen
                            )
                            Text(
                                text = "${product.originalPrice} $currencySuffix",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                style = androidx.compose.ui.text.TextStyle(
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                )
                            )
                        }
                        product.discountPrice != null -> {
                            Text(
                                text = "${product.discountPrice} $currencySuffix",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        product.originalPrice != null -> {
                            Text(
                                text = "${product.originalPrice} $currencySuffix",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        else -> {
                            Text(
                                text = "${product.price} $currencySuffix",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .background(
                        if (product.amount > 0) DeepGreen.copy(alpha = 0.1f)
                        else ErrorRed.copy(alpha = 0.1f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(Res.string.business_products_stock_prefix) + product.amount,
                    fontSize = 12.sp,
                    color = if (product.amount > 0) DeepGreen else ErrorRed,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
