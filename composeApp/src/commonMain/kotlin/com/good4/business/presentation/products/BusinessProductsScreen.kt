package com.good4.business.presentation.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.domain.CurrencyConstants
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4NestedScaffold
import com.good4.core.presentation.components.Good4TopBar
import com.good4.core.presentation.components.ProductListCard
import com.good4.core.presentation.components.ProfileTopBarAction
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.business_products_add_content_desc
import good4.composeapp.generated.resources.business_products_added_message
import good4.composeapp.generated.resources.business_products_daily_stock_prefix
import good4.composeapp.generated.resources.business_products_deleted_message
import good4.composeapp.generated.resources.business_products_donation_section_title
import good4.composeapp.generated.resources.business_products_empty
import good4.composeapp.generated.resources.business_products_title
import good4.composeapp.generated.resources.business_products_updated_message
import good4.composeapp.generated.resources.emoji_products_empty
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessProductsScreenRoot(
    modifier: Modifier = Modifier,
    viewModel: BusinessProductsViewModel = koinViewModel(),
    onProfileClick: (() -> Unit)? = null
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val productAddedMessage = stringResource(Res.string.business_products_added_message)
    val productUpdatedMessage = stringResource(Res.string.business_products_updated_message)
    val productDeletedMessage = stringResource(Res.string.business_products_deleted_message)
    val currencySuffix = CurrencyConstants.TURKISH_LIRA_SYMBOL
    val dailyStockPrefix = stringResource(Res.string.business_products_daily_stock_prefix)
    val showAddSheet = remember { mutableStateOf(false) }
    val showEditSheet = remember { mutableStateOf(false) }
    val addSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden }
    )
    val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(state.addSuccess) {
        if (state.addSuccess) {
            showAddSheet.value = false
            viewModel.resetAddState()
            snackbarHostState.showSnackbar(productAddedMessage)
        }
    }

    LaunchedEffect(state.editSuccess) {
        if (state.editSuccess) {
            snackbarHostState.showSnackbar(productUpdatedMessage)
            showEditSheet.value = false
            viewModel.resetEditState()
        }
    }

    LaunchedEffect(state.deleteSuccess) {
        if (state.deleteSuccess) {
            showEditSheet.value = false
            viewModel.resetEditState()
            snackbarHostState.showSnackbar(productDeletedMessage)
        }
    }

    Good4NestedScaffold(
        modifier = modifier,
        topBar = {
            Good4TopBar(
                title = stringResource(Res.string.business_products_title),
                actions = {
                    if (onProfileClick != null) {
                        ProfileTopBarAction(onClick = onProfileClick)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet.value = true },
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
            val donationProducts = state.products.filter { it.isDonation }
            val regularProducts = state.products.filterNot { it.isDonation }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(regularProducts, key = { it.documentId }) { product ->
                    ProductListCard(
                        product = product,
                        currencySuffix = currencySuffix,
                        showStockInfo = false,
                        onClick = {
                            viewModel.selectProductForEdit(product)
                            showEditSheet.value = true
                        }
                    )
                }

                if (donationProducts.isNotEmpty()) {
                    item(key = "donation_section_title") {
                        Spacer(Modifier.size(12.dp))
                        Text(
                            text = stringResource(Res.string.business_products_donation_section_title),
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                    }

                    items(donationProducts, key = { it.documentId }) { product ->
                        ProductListCard(
                            product = product,
                            currencySuffix = currencySuffix,
                            stockPrefix = dailyStockPrefix,
                            stockValue = product.dailyPendingLimit ?: product.pendingCount,
                            onClick = {
                                viewModel.selectProductForEdit(product)
                                showEditSheet.value = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddSheet.value) {
        AddProductBottomSheet(
            sheetState = addSheetState,
            state = state,
            onDismiss = {
                showAddSheet.value = false
                viewModel.resetAddState()
                viewModel.dismissError()
            },
            onProductNameChange = viewModel::onProductNameChange,
            onProductDescriptionChange = viewModel::onProductDescriptionChange,
            onDonationProductChange = viewModel::onDonationProductChange,
            onOriginalPriceChange = viewModel::onOriginalPriceChange,
            onDiscountPriceChange = viewModel::onDiscountPriceChange,
            onDailyPendingLimitChange = viewModel::onDailyPendingLimitChange,
            onPendingProductImageChange = viewModel::onPendingProductImageChange,
            onImagePickerError = { message ->
                scope.launch { snackbarHostState.showSnackbar(message) }
            },
            onAddProduct = viewModel::addProduct
        )
    }

    if (showEditSheet.value) {
        EditProductBottomSheet(
            sheetState = editSheetState,
            state = state,
            onDismiss = {
                showEditSheet.value = false
                viewModel.resetEditState()
                viewModel.dismissError()
            },
            onProductNameChange = viewModel::onProductNameChange,
            onProductDescriptionChange = viewModel::onProductDescriptionChange,
            onDonationProductChange = viewModel::onDonationProductChange,
            onOriginalPriceChange = viewModel::onOriginalPriceChange,
            onDiscountPriceChange = viewModel::onDiscountPriceChange,
            onDailyPendingLimitChange = viewModel::onDailyPendingLimitChange,
            onPendingProductImageChange = viewModel::onPendingProductImageChange,
            onImagePickerError = { message ->
                scope.launch { snackbarHostState.showSnackbar(message) }
            },
            onUpdateProduct = viewModel::updateProduct,
            onDeleteProduct = viewModel::deleteProduct
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
