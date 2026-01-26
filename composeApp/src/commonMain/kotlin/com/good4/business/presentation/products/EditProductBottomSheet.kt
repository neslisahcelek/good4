package com.good4.business.presentation.products

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.components.ProductFormFields
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.business_products_edit_title
import good4.composeapp.generated.resources.business_products_update_button
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductBottomSheet(
    modifier: Modifier = Modifier,
    sheetState: SheetState,
    state: BusinessProductsState,
    onDismiss: () -> Unit,
    onProductNameChange: (String) -> Unit,
    onProductDescriptionChange: (String) -> Unit,
    onOriginalPriceChange: (String) -> Unit,
    onDiscountPriceChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onImageUrlChange: (String) -> Unit,
    onImagePickerError: (String) -> Unit,
    onUpdateProduct: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        ProductFormFields(
            title = stringResource(Res.string.business_products_edit_title),
            submitLabel = stringResource(Res.string.business_products_update_button),
            onSubmit = onUpdateProduct,
            modifier = modifier.fillMaxSize(),
            isSubmitting = state.isEditLoading,
            submitButtonColor = TextPrimary,
            submitButtonDisabledColor = TextPrimary.copy(alpha = 0.5f),
            productName = state.productName,
            onProductNameChange = onProductNameChange,
            productDescription = state.productDescription,
            onProductDescriptionChange = onProductDescriptionChange,
            originalPrice = state.productOriginalPrice,
            onOriginalPriceChange = onOriginalPriceChange,
            discountPrice = state.productDiscountPrice,
            onDiscountPriceChange = onDiscountPriceChange,
            amount = state.productAmount,
            onAmountChange = onAmountChange,
            imageUrl = state.productImageUrl,
            onImageUrlChange = onImageUrlChange,
            onImagePickerError = onImagePickerError
        )
    }
}
