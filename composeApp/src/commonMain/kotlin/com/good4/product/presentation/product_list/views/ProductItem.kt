package com.good4.product.presentation.product_list.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.DeepGreen
import com.good4.core.presentation.PrimaryGreen
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.SurfaceMuted
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.toDisplayAddressOrNull
import com.good4.core.util.openMaps
import com.good4.core.util.singleClick
import com.good4.product.Product
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.ic_placeholder
import good4.composeapp.generated.resources.product_free
import good4.composeapp.generated.resources.product_image_description
import good4.composeapp.generated.resources.reserve_button_label
import good4.composeapp.generated.resources.reserved
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

private val CardShape = RoundedCornerShape(12.dp)
private val ImageShape = RoundedCornerShape(8.dp)

@Composable
fun ProductItem(
    modifier: Modifier = Modifier,
    product: Product,
    onReserveClick: (() -> Unit)? = null,
    isReserving: Boolean = false,
    reservationSuccess: Boolean = false,
    isReserveEnabled: Boolean = true
) {
    val reserveClickHandler = remember(
        product.documentId,
        onReserveClick,
        isReserveEnabled,
        isReserving,
        reservationSuccess
    ) {
        singleClick {
            if (isReserveEnabled && !isReserving && !reservationSuccess) {
                onReserveClick?.invoke()
            }
        }
    }

    val priceLine = stringResource(Res.string.product_free)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
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
                        .clip(ImageShape)
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

                }

                Column(
                    modifier = Modifier.weight(1f),
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
                        Text(
                            text = priceLine,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = PrimaryGreen,
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
                            fontStyle = FontStyle.Italic,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            val displayAddress = toDisplayAddressOrNull(product.address)
            val mapsAddress = product.addressUrl
            if (displayAddress != null) {
                Row(
                    modifier = Modifier
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

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .height(40.dp),
                onClick = reserveClickHandler,
                enabled = isReserveEnabled && !reservationSuccess,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        reservationSuccess -> DeepGreen
                        else -> PrimaryGreen
                    },
                    contentColor = Color.White,
                    disabledContainerColor = SurfaceMuted,
                    disabledContentColor = TextSecondary.copy(alpha = 0.45f)
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp,
                    vertical = 0.dp
                )
            ) {
                if (isReserving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = when {
                            reservationSuccess -> stringResource(Res.string.reserved)
                            else -> stringResource(Res.string.reserve_button_label)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
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
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProductItem(
                product = Product(
                    id = 1,
                    documentId = "preview_doc1",
                    name = "Gluten Free Chocolate Cake",
                    storeName = "Healthy Bakery",
                    businessId = "preview_business",
                    address = "123 Green St, New York",
                    addressUrl = "",
                    description = "Delicious gluten-free cake made with organic ingredients.",
                    price = 120,
                    imageUrl = "",
                    amount = 3,
                    originalPrice = 150,
                    discountPrice = 120,
                    discountPercentage = 20
                ),
                modifier = modifier
            )

            ProductItem(
                product = Product(
                    id = 2,
                    documentId = "preview_doc2",
                    name = "Fresh Organic Apples",
                    storeName = "Farm Fresh",
                    businessId = "preview_business",
                    address = "456 Market Ave",
                    addressUrl = "",
                    description = "Crisp and sweet apples directly from the orchard.",
                    price = 0,
                    imageUrl = "",
                    amount = 10,
                    originalPrice = null,
                    discountPrice = null,
                    discountPercentage = null
                ),
                modifier = modifier
            )
        }
    }
}
