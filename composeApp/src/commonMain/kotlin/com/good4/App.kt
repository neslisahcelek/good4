package com.good4

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.good4.product.presentation.product_list.ProductListScreenRoot
import com.good4.product.presentation.product_list.ProductListViewModel
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.ic_logo
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
@Preview
fun App() {
    MaterialTheme {
        var showContent by remember { mutableStateOf(false) }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = { showContent = !showContent }) {
                Text("Click me!")
            }
            AnimatedVisibility(showContent) {
                Image(
                    painter = painterResource(Res.drawable.ic_logo),
                    contentDescription = null
                )
            }

            val viewModel: ProductListViewModel = koinViewModel()
            ProductListScreenRoot(
                viewModel = viewModel,
                onProductClick = {}
            )
        }
    }
}