package com.good4.product.presentation.product_list

import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.good4.product.Product

@Composable
fun ProductItem(
    product: Product,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(text = product.name, style = MaterialTheme.typography.h6)
        Text(text = product.description, style = MaterialTheme.typography.body2)
        Text(text = product.price, style = MaterialTheme.typography.subtitle1)
    }
}

@Preview
@Composable
fun ProductItemPreview(modifier: Modifier = Modifier) {
    MaterialTheme {
        ProductItem(
            product = Product(
                id = 1,
                name = "Kahve",
                description = "description",
                price = "price",
                imageUrl = "imageUrl"
            )
        )
    }
}