package com.good4.business.presentation.products

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import com.good4.core.presentation.DeepGreen
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.components.ProductFormDiscardConfirmDialog
import com.good4.core.presentation.components.ProductFormFields
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.business_products_add_button
import good4.composeapp.generated.resources.business_products_add_title
import good4.composeapp.generated.resources.delete_account_cancel_button
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductBottomSheet(
    modifier: Modifier = Modifier,
    sheetState: SheetState,
    state: BusinessProductsState,
    onDismiss: () -> Unit,
    onProductNameChange: (String) -> Unit,
    onProductDescriptionChange: (String) -> Unit,
    onDonationProductChange: (Boolean) -> Unit,
    onOriginalPriceChange: (String) -> Unit,
    onDiscountPriceChange: (String) -> Unit,
    onDailyPendingLimitChange: (String) -> Unit,
    onPendingProductImageChange: (ByteArray?) -> Unit,
    onImagePickerError: (String) -> Unit,
    onAddProduct: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val showDiscardConfirmDialog = remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = {
            focusManager.clearFocus(force = true)
            showDiscardConfirmDialog.value = true
        },
        sheetState = sheetState,
        containerColor = SurfaceDefault
    ) {
        ProductFormFields(
            title = stringResource(Res.string.business_products_add_title),
            submitLabel = stringResource(Res.string.business_products_add_button),
            onSubmit = onAddProduct,
            modifier = modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus(force = true) })
                },
            isSubmitting = state.isAddLoading,
            dismissLabel = stringResource(Res.string.delete_account_cancel_button),
            onDismiss = { showDiscardConfirmDialog.value = true },
            submitButtonColor = DeepGreen,
            submitButtonDisabledColor = DeepGreen.copy(alpha = 0.5f),
            errorMessage = state.errorMessage,
            showDonationOption = true,
            isDonationProduct = state.isDonationProduct,
            onDonationProductChange = onDonationProductChange,
            productName = state.productName,
            onProductNameChange = onProductNameChange,
            productDescription = state.productDescription,
            onProductDescriptionChange = onProductDescriptionChange,
            originalPrice = state.productOriginalPrice,
            onOriginalPriceChange = onOriginalPriceChange,
            discountPrice = state.productDiscountPrice,
            onDiscountPriceChange = onDiscountPriceChange,
            dailyPendingLimit = state.productDailyPendingLimit,
            onDailyPendingLimitChange = onDailyPendingLimitChange,
            currentRemoteImageUrl = state.productImageUrl,
            pendingProductImageBytes = state.pendingProductImageBytes,
            onPendingProductImageChange = onPendingProductImageChange,
            isImageUploading = state.isProductImageUploading,
            onImagePickerError = onImagePickerError
        )
    }

    if (showDiscardConfirmDialog.value) {
        ProductFormDiscardConfirmDialog(
            onConfirm = {
                showDiscardConfirmDialog.value = false
                onDismiss()
            },
            onDismiss = {
                showDiscardConfirmDialog.value = false
                scope.launch { sheetState.show() }
            }
        )
    }
}
