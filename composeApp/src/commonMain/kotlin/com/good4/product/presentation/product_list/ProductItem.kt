package com.good4.product.presentation.product_list

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.good4.product.Product
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ProductItem(
    product: Product,
    modifier: Modifier = Modifier
) {
    Column(

    ) {
        Icon(
            painter = painterResource(DrawableResource(id = product.imageUrl)),
        )
    }
}

@Preview
@Composable
fun ProductItemPreview(modifier: Modifier = Modifier) {
    MaterialTheme{
        ProductItem(product = Product(
            id = 1,
            name = "Kahve",
            description = "description",
            price = "price",
            imageUrl = "imageUrl"
        ))
    }
}