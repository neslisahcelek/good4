package com.good4.admin.presentation.products

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.components.ProductFormFields
import com.good4.core.presentation.components.SelectOption
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.admin_products_edit_title
import good4.composeapp.generated.resources.admin_products_update_button
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductSheet(
    modifier: Modifier = Modifier,
    sheetState: SheetState,
    state: AdminProductsState,
    onDismiss: () -> Unit,
    onBusinessSelect: (String) -> Unit,
    onProductNameChange: (String) -> Unit,
    onProductDescriptionChange: (String) -> Unit,
    onOriginalPriceChange: (String) -> Unit,
    onDiscountPriceChange: (String) -> Unit,
    onCountChange: (String) -> Unit,
    onImageUrlChange: (String) -> Unit,
    onImageUploadStateChange: (Boolean) -> Unit,
    onImagePickerError: (String) -> Unit,
    onUpdateProduct: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        val businessOptions = state.businesses.map { business ->
            SelectOption(id = business.id, label = business.name)
        }

        ProductFormFields(
            title = stringResource(Res.string.admin_products_edit_title),
            submitLabel = stringResource(Res.string.admin_products_update_button),
            onSubmit = onUpdateProduct,
            modifier = modifier,
            isSubmitting = state.isEditLoading,
            submitButtonColor = TextPrimary,
            submitButtonDisabledColor = TextPrimary.copy(alpha = 0.5f),
            errorMessage = state.errorMessage,
            showBusinessSelector = true,
            businessOptions = businessOptions,
            selectedBusinessId = state.selectedBusinessId,
            onBusinessSelect = onBusinessSelect,
            productName = state.productName,
            onProductNameChange = onProductNameChange,
            productDescription = state.productDescription,
            onProductDescriptionChange = onProductDescriptionChange,
            originalPrice = state.productOriginalPrice,
            onOriginalPriceChange = onOriginalPriceChange,
            discountPrice = state.productDiscountPrice,
            onDiscountPriceChange = onDiscountPriceChange,
            amount = state.productCount,
            onAmountChange = onCountChange,
            imageUrl = state.productImageUrl,
            onImageUrlChange = onImageUrlChange,
            isImageUploading = state.isProductImageUploading,
            onImageUploadStateChange = onImageUploadStateChange,
            onImagePickerError = onImagePickerError
        )
    }
}
