package com.good4.product.presentation.product_list.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.DeepGreen
import com.good4.core.presentation.ErrorRed
import com.good4.core.presentation.PrimaryGreen
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.SurfaceMuted
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.util.singleClick
import com.good4.product.Product
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.ic_placeholder
import good4.composeapp.generated.resources.product_image_description
import good4.composeapp.generated.resources.reserve_button_label
import good4.composeapp.generated.resources.reserved
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ProductItem(
    modifier: Modifier = Modifier,
    product: Product,
    onReserveClick: (() -> Unit)? = null,
    isReserving: Boolean = false,
    reservationSuccess: Boolean = false
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceDefault,
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.5f)),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
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
                            modifier = Modifier.size(48.dp),
                            alpha = 0.3f
                        )
                    }
                }

                if (product.discountPercentage != null && product.discountPercentage > 0) {
                    Surface(
                        color = ErrorRed,
                        shape = RoundedCornerShape(bottomStart = 12.dp),
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = "-${product.discountPercentage}%",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
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

                Row(
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
                        text = product.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        overflow = TextOverflow.Ellipsis
                    )

                }

                Button(
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    onClick = {
                        singleClick {
                            if (!isReserving && !reservationSuccess) {
                                onReserveClick?.invoke()
                            }
                        }
                    },
                    enabled = !reservationSuccess,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (reservationSuccess) DeepGreen.copy(alpha = 0.8f) else PrimaryGreen,
                        contentColor = Color.White,
                        disabledContainerColor = DeepGreen.copy(alpha = 0.5f),
                        disabledContentColor = Color.White
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
                            text = if (reservationSuccess) stringResource(Res.string.reserved)
                            else stringResource(Res.string.reserve_button_label),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
