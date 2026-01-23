package com.good4.business.presentation.products

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.good4.core.presentation.SoftGray
import com.good4.core.presentation.InkBlack
import com.good4.core.presentation.Background
import com.good4.core.presentation.LimeGreen
import com.good4.core.presentation.Surface
import com.good4.core.presentation.SlateGray
import com.good4.product.Product
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.business_products_add_content_desc
import good4.composeapp.generated.resources.business_products_added_message
import good4.composeapp.generated.resources.business_products_empty
import good4.composeapp.generated.resources.business_products_price_unavailable
import good4.composeapp.generated.resources.business_products_stock_prefix
import good4.composeapp.generated.resources.business_products_title
import good4.composeapp.generated.resources.emoji_product_placeholder
import good4.composeapp.generated.resources.emoji_products_empty
import good4.composeapp.generated.resources.price_currency_suffix
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessProductsScreenRoot(
    modifier: Modifier = Modifier,
    viewModel: BusinessProductsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val productAddedMessage = stringResource(Res.string.business_products_added_message)
    val currencySuffix = stringResource(Res.string.price_currency_suffix)
    var showAddSheet by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    LaunchedEffect(state.addSuccess) {
        if (state.addSuccess) {
            snackbarHostState.showSnackbar(productAddedMessage)
            showAddSheet = false
            viewModel.resetAddState()
        }
    }

    LaunchedEffect(state.editSuccess) {
        if (state.editSuccess) {
            showEditSheet = false
            viewModel.resetEditState()
        }
    }

    Scaffold(
        modifier = modifier.background(Background),
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.business_products_title),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = InkBlack,
                contentColor = Color.White
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(Res.string.business_products_add_content_desc)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = InkBlack)
            }
        } else if (state.products.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.emoji_products_empty),
                        fontSize = 64.sp
                    )
                    Text(
                        text = stringResource(Res.string.business_products_empty),
                        fontSize = 16.sp,
                        color = SlateGray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.products, key = { it.documentId }) { product ->
                    ProductCard(
                        product = product,
                        currencySuffix = currencySuffix,
                        onClick = {
                            viewModel.selectProductForEdit(product)
                            showEditSheet = true
                        }
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        AddProductBottomSheet(
            sheetState = addSheetState,
            state = state,
            onDismiss = {
                showAddSheet = false
                viewModel.resetAddState()
                viewModel.refreshProducts()
            },
            onProductNameChange = viewModel::onProductNameChange,
            onProductDescriptionChange = viewModel::onProductDescriptionChange,
            onOriginalPriceChange = viewModel::onOriginalPriceChange,
            onDiscountPriceChange = viewModel::onDiscountPriceChange,
            onAmountChange = viewModel::onAmountChange,
            onImageUrlChange = viewModel::onImageUrlChange,
            onImagePickerError = { message ->
                scope.launch { snackbarHostState.showSnackbar(message) }
            },
            onAddProduct = viewModel::addProduct
        )
    }

    if (showEditSheet) {
        EditProductBottomSheet(
            sheetState = editSheetState,
            state = state,
            onDismiss = {
                showEditSheet = false
                viewModel.resetEditState()
                viewModel.refreshProducts()
            },
            onProductNameChange = viewModel::onProductNameChange,
            onProductDescriptionChange = viewModel::onProductDescriptionChange,
            onOriginalPriceChange = viewModel::onOriginalPriceChange,
            onDiscountPriceChange = viewModel::onDiscountPriceChange,
            onAmountChange = viewModel::onAmountChange,
            onImageUrlChange = viewModel::onImageUrlChange,
            onImagePickerError = { message ->
                scope.launch { snackbarHostState.showSnackbar(message) }
            },
            onUpdateProduct = viewModel::updateProduct
        )
    }
}

@Composable
private fun ProductCard(
    modifier: Modifier = Modifier,
    product: Product,
    currencySuffix: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (product.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.name,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SoftGray),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SoftGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(Res.string.emoji_product_placeholder),
                        fontSize = 32.sp
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = InkBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (product.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = product.description,
                        fontSize = 12.sp,
                        color = SlateGray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

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
                                color = LimeGreen
                            )
                            Text(
                                text = "${product.originalPrice} $currencySuffix",
                                fontSize = 12.sp,
                                color = SlateGray,
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
                                color = InkBlack
                            )
                        }
                        product.originalPrice != null -> {
                            Text(
                                text = "${product.originalPrice} $currencySuffix",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = InkBlack
                            )
                        }
                        else -> {
                            Text(
                                text = stringResource(Res.string.business_products_price_unavailable),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = SlateGray
                            )
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (product.amount > 0) LimeGreen.copy(alpha = 0.1f)
                            else Color.Red.copy(alpha = 0.1f)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.business_products_stock_prefix) + product.amount,
                        fontSize = 12.sp,
                        color = if (product.amount > 0) LimeGreen else Color.Red,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun BusinessProductsScreenPreview() {
    MaterialTheme {
        BusinessProductsScreenRoot()
    }
}
