package com.good4.admin.presentation.products

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.domain.CurrencyConstants
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4NestedScaffold
import com.good4.core.presentation.components.Good4TopBar
import com.good4.core.presentation.components.ProductFormDiscardConfirmDialog
import com.good4.core.presentation.components.ProductListCard
import com.good4.core.presentation.components.ProfileTopBarAction
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.add_product
import good4.composeapp.generated.resources.admin_products_empty
import good4.composeapp.generated.resources.business_products_deleted_message
import good4.composeapp.generated.resources.emoji_products_empty
import good4.composeapp.generated.resources.manage_products
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProductsScreen(
    modifier: Modifier = Modifier,
    viewModel: AdminProductsViewModel = koinViewModel(),
    onMenuClick: (() -> Unit)? = null,
    onProfileClick: (() -> Unit)? = null
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val showAddSheet = remember { mutableStateOf(false) }
    val showEditSheet = remember { mutableStateOf(false) }
    val showAddDiscardConfirmDialog = remember { mutableStateOf(false) }
    val addSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden }
    )
    val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val currencySuffix = CurrencyConstants.TURKISH_LIRA_SYMBOL
    val addProductLabel = stringResource(Res.string.add_product)
    val productDeletedMessage = stringResource(Res.string.business_products_deleted_message)

    Good4NestedScaffold(
        modifier = modifier,
        topBar = {
            Good4TopBar(
                title = stringResource(Res.string.manage_products),
                navigationIcon = {
                    if (onMenuClick != null) {
                        IconButton(onClick = onMenuClick) {
                            Icon(Icons.Filled.Menu, contentDescription = null)
                        }
                    }
                },
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
                                showEditSheet.value = true
                            }
                        )
                    }
                }
            }
        }

        if (showAddSheet.value) {
            ModalBottomSheet(
                onDismissRequest = {
                    focusManager.clearFocus(force = true)
                    showAddDiscardConfirmDialog.value = true
                },
                sheetState = addSheetState,
                containerColor = SurfaceDefault
            ) {
                AddProductSheet(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { focusManager.clearFocus(force = true) }
                            )
                        },
                    state = state,
                    viewModel = viewModel,
                    onDismiss = {
                        showAddDiscardConfirmDialog.value = true
                    },
                    onAddSuccess = {
                        showAddSheet.value = false
                        showAddDiscardConfirmDialog.value = false
                        viewModel.resetAddState()
                    },
                    onImagePickerError = { message ->
                        scope.launch { snackbarHostState.showSnackbar(message) }
                    },
                )
            }
        }

        if (showAddDiscardConfirmDialog.value) {
            ProductFormDiscardConfirmDialog(
                onConfirm = {
                    showAddDiscardConfirmDialog.value = false
                    showAddSheet.value = false
                    viewModel.resetAddState()
                },
                onDismiss = {
                    showAddDiscardConfirmDialog.value = false
                    scope.launch { addSheetState.show() }
                }
            )
        }

        if (showEditSheet.value) {
            LaunchedEffect(state.editSuccess) {
                if (state.editSuccess) {
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

            EditProductSheet(
                modifier = Modifier.fillMaxSize(),
                sheetState = editSheetState,
                state = state,
                onDismiss = {
                    showEditSheet.value = false
                    viewModel.resetEditState()
                },
                onBusinessSelect = viewModel::onBusinessSelect,
                onDonationProductChange = viewModel::onDonationProductChange,
                onProductNameChange = viewModel::onProductNameChange,
                onProductDescriptionChange = viewModel::onProductDescriptionChange,
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
}

@Preview
@Composable
fun AdminProductsScreenPreview() {
    MaterialTheme {
        AdminProductsScreen()
    }
}
