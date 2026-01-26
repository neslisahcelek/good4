package com.good4.admin.presentation.products

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.ModalBottomSheet
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
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4Scaffold
import com.good4.core.presentation.components.Good4TopBar
import com.good4.core.presentation.components.ProductListCard
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.add_product
import good4.composeapp.generated.resources.admin_products_empty
import good4.composeapp.generated.resources.emoji_products_empty
import good4.composeapp.generated.resources.manage_products
import good4.composeapp.generated.resources.price_currency_suffix
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProductsScreen(
    modifier: Modifier = Modifier,
    viewModel: AdminProductsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val currencySuffix = stringResource(Res.string.price_currency_suffix)
    val addProductLabel = stringResource(Res.string.add_product)

    Good4Scaffold(
        modifier = modifier,
        topBar = { Good4TopBar(title = stringResource(Res.string.manage_products)) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = TextPrimary,
                contentColor = SurfaceDefault
            ) {
                Icon(Icons.Filled.Add, contentDescription = addProductLabel)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(paddingValues)
        ) {
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = TextPrimary)
                }
            } else if (state.products.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(Res.string.emoji_products_empty),
                            fontSize = 64.sp
                        )
                        Spacer(modifier = Modifier.padding(top = 16.dp))
                        Text(
                            text = stringResource(Res.string.admin_products_empty),
                            fontSize = 18.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = state.products,
                        key = { it.documentId }
                    ) { product ->
                        ProductListCard(
                            product = product,
                            currencySuffix = currencySuffix,
                            showStoreName = true,
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
            ModalBottomSheet(
                onDismissRequest = { showAddSheet = false },
                sheetState = addSheetState
            ) {
                AddProductSheet(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    viewModel = viewModel,
                    onDismiss = {
                        showAddSheet = false
                        viewModel.resetAddState()
                    }
                )
            }
        }

        if (showEditSheet) {
            LaunchedEffect(state.editSuccess) {
                if (state.editSuccess) {
                    showEditSheet = false
                    viewModel.resetEditState()
                }
            }

            EditProductSheet(
                modifier = Modifier.fillMaxSize(),
                sheetState = editSheetState,
                state = state,
                onDismiss = {
                    showEditSheet = false
                    viewModel.resetEditState()
                },
                onBusinessSelect = viewModel::onBusinessSelect,
                onProductNameChange = viewModel::onProductNameChange,
                onProductDescriptionChange = viewModel::onProductDescriptionChange,
                onOriginalPriceChange = viewModel::onOriginalPriceChange,
                onDiscountPriceChange = viewModel::onDiscountPriceChange,
                onCountChange = viewModel::onCountChange,
                onImageUrlChange = viewModel::onImageUrlChange,
                onImagePickerError = { message ->
                    scope.launch { snackbarHostState.showSnackbar(message) }
                },
                onUpdateProduct = viewModel::updateProduct
            )
        }
    }
}

@Preview
@Composable
fun AdminProductsScreenPreview() {
    MaterialTheme {
        AdminProductsScreen()
    }
}
