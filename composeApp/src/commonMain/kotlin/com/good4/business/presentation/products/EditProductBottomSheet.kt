package com.good4.business.presentation.products

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.good4.core.presentation.InkBlack
import com.good4.core.presentation.components.ProductImagePicker
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.amount
import good4.composeapp.generated.resources.business_products_edit_title
import good4.composeapp.generated.resources.business_products_update_button
import good4.composeapp.generated.resources.description
import good4.composeapp.generated.resources.discounted_price
import good4.composeapp.generated.resources.original_price
import good4.composeapp.generated.resources.product_name
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
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(Res.string.business_products_edit_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = InkBlack
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = state.productName,
                onValueChange = onProductNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.product_name)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = InkBlack,
                    focusedLabelColor = InkBlack,
                    cursorColor = InkBlack
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.productDescription,
                onValueChange = onProductDescriptionChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.description)) },
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = InkBlack,
                    focusedLabelColor = InkBlack,
                    cursorColor = InkBlack
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.productOriginalPrice,
                onValueChange = onOriginalPriceChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.original_price)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = InkBlack,
                    focusedLabelColor = InkBlack,
                    cursorColor = InkBlack
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.productDiscountPrice,
                onValueChange = onDiscountPriceChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.discounted_price)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = InkBlack,
                    focusedLabelColor = InkBlack,
                    cursorColor = InkBlack
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.productAmount,
                onValueChange = onAmountChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.amount)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = InkBlack,
                    focusedLabelColor = InkBlack,
                    cursorColor = InkBlack
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            ProductImagePicker(
                currentImageUrl = state.productImageUrl,
                onImageUrlChange = onImageUrlChange,
                onError = onImagePickerError,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onUpdateProduct,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !state.isEditLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = InkBlack,
                    disabledContainerColor = InkBlack.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (state.isEditLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(4.dp),
                        color = Color.White
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.business_products_update_button),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
