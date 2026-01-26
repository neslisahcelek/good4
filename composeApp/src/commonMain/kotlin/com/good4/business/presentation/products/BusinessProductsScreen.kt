package com.good4.business.presentation.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4Scaffold
import com.good4.core.presentation.components.Good4TopBar
import com.good4.core.presentation.components.ProductListCard
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.business_products_add_content_desc
import good4.composeapp.generated.resources.business_products_added_message
import good4.composeapp.generated.resources.business_products_empty
import good4.composeapp.generated.resources.business_products_title
import good4.composeapp.generated.resources.emoji_products_empty
import good4.composeapp.generated.resources.price_currency_suffix
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
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

    Good4Scaffold(
        modifier = modifier,
        topBar = { Good4TopBar(title = stringResource(Res.string.business_products_title)) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = TextPrimary,
                contentColor = SurfaceDefault
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
                CircularProgressIndicator(color = TextPrimary)
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
                        color = TextSecondary
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
                    ProductListCard(
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

@Preview
@Composable
fun BusinessProductsScreenPreview() {
    MaterialTheme {
        BusinessProductsScreenRoot()
    }
}






