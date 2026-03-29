package com.good4.admin.presentation.products

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.good4.core.presentation.DeepGreen
import com.good4.core.presentation.components.ProductFormFields
import com.good4.core.presentation.components.SelectOption
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.add_product
import org.jetbrains.compose.resources.stringResource

@Composable
fun AddProductSheet(
    modifier: Modifier = Modifier,
    state: AdminProductsState,
    viewModel: AdminProductsViewModel,
    onDismiss: () -> Unit,
    onImagePickerError: (String) -> Unit
) {
    LaunchedEffect(state.addSuccess) {
        if (state.addSuccess) {
            onDismiss()
        }
    }

    val businessOptions = state.businesses.map { business ->
        SelectOption(id = business.id, label = business.name)
    }

    ProductFormFields(
        title = stringResource(Res.string.add_product),
        submitLabel = stringResource(Res.string.add_product),
        onSubmit = viewModel::addProduct,
        modifier = modifier,
        isSubmitting = state.isAddLoading,
        submitButtonColor = DeepGreen,
        errorMessage = state.errorMessage,
        showBusinessSelector = true,
        businessOptions = businessOptions,
        selectedBusinessId = state.selectedBusinessId,
        onBusinessSelect = viewModel::onBusinessSelect,
        productName = state.productName,
        onProductNameChange = viewModel::onProductNameChange,
        productDescription = state.productDescription,
        onProductDescriptionChange = viewModel::onProductDescriptionChange,
        originalPrice = state.productOriginalPrice,
        onOriginalPriceChange = viewModel::onOriginalPriceChange,
        discountPrice = state.productDiscountPrice,
        onDiscountPriceChange = viewModel::onDiscountPriceChange,
        amount = state.productCount,
        onAmountChange = viewModel::onCountChange,
        currentRemoteImageUrl = state.productImageUrl,
        pendingProductImageBytes = state.pendingProductImageBytes,
        onPendingProductImageChange = viewModel::onPendingProductImageChange,
        isImageUploading = state.isProductImageUploading,
        onImagePickerError = onImagePickerError
    )
}
