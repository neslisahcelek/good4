package com.good4.product.presentation.product_list

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.good4.core.presentation.UiText
import com.good4.product.Product
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.ic_placeholder
import good4.composeapp.generated.resources.product_image_description
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ProductItem(
    modifier: Modifier = Modifier,
    product: Product
) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
                    .height(180.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_placeholder),
                    contentDescription = stringResource(Res.string.product_image_description),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = product.storeName,
                    color = Color.Gray,
                    modifier = Modifier.fillMaxWidth()
                )
                AddressRow(
                    address = UiText.DynamicString(product.address)
                )
                Text(
                    text = product.description,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AmountCard(amount = product.amount)
                    ReserveButtonCard(
                        onClick = { /* TODO: handle reserve */ }
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
        ProductItem(
            product = Product(
                id = 1,
                name = "Filtre Kahve",
                storeName = "Sokak Kahvecisi",
                address = "Yakut Çarşısı Sokak Kahvecisi Konyaaltı/Antalya",
                description = "Orta Boy",
                price = "100 TL",
                imageUrl = "image.png",
                amount = 5,
                originalPrice = 100,
                discountPrice = 60,
                discountPercentage = 40
            )
        )
    }
}