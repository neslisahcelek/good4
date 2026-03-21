package com.good4.core.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.amount
import good4.composeapp.generated.resources.business_name
import good4.composeapp.generated.resources.description
import good4.composeapp.generated.resources.discounted_price
import good4.composeapp.generated.resources.original_price
import good4.composeapp.generated.resources.product_name
import org.jetbrains.compose.resources.stringResource

data class SelectOption(
    val id: String,
    val label: String
)

@Composable
fun ProductFormFields(
    title: String,
    submitLabel: String,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    isSubmitting: Boolean = false,
    submitButtonColor: Color,
    submitButtonDisabledColor: Color = submitButtonColor.copy(alpha = 0.5f),
    errorMessage: String? = null,
    showBusinessSelector: Boolean = false,
    businessOptions: List<SelectOption> = emptyList(),
    selectedBusinessId: String? = null,
    onBusinessSelect: (String) -> Unit = {},
    productName: String,
    onProductNameChange: (String) -> Unit,
    productDescription: String,
    onProductDescriptionChange: (String) -> Unit,
    originalPrice: String,
    onOriginalPriceChange: (String) -> Unit,
    discountPrice: String,
    onDiscountPriceChange: (String) -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit,
    imageUrl: String,
    onImageUrlChange: (String) -> Unit,
    isImageUploading: Boolean = false,
    onImageUploadStateChange: (Boolean) -> Unit = {},
    onImagePickerError: (String) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (showBusinessSelector) {
            Text(
                text = stringResource(Res.string.business_name),
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Box {
                OutlinedTextField(
                    value = businessOptions.find { it.id == selectedBusinessId }?.label.orEmpty(),
                    onValueChange = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = true },
                    enabled = false,
                    placeholder = { Text(stringResource(Res.string.business_name)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = TextPrimary,
                        disabledBorderColor = TextSecondary,
                        disabledPlaceholderColor = TextSecondary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    businessOptions.forEach { business ->
                        DropdownMenuItem(
                            text = { Text(business.label) },
                            onClick = {
                                onBusinessSelect(business.id)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = productName,
            onValueChange = onProductNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.product_name)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TextPrimary,
                focusedLabelColor = TextPrimary,
                cursorColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = productDescription,
            onValueChange = onProductDescriptionChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.description)) },
            minLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TextPrimary,
                focusedLabelColor = TextPrimary,
                cursorColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = originalPrice,
            onValueChange = onOriginalPriceChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.original_price)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TextPrimary,
                focusedLabelColor = TextPrimary,
                cursorColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = discountPrice,
            onValueChange = onDiscountPriceChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.discounted_price)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TextPrimary,
                focusedLabelColor = TextPrimary,
                cursorColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = amount,
            onValueChange = onAmountChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.amount)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TextPrimary,
                focusedLabelColor = TextPrimary,
                cursorColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        ProductImagePicker(
            modifier = Modifier.fillMaxWidth(),
            currentImageUrl = imageUrl,
            isUploading = isImageUploading,
            onImageUrlChange = onImageUrlChange,
            onUploadStateChange = onImageUploadStateChange,
            onError = onImagePickerError
        )

        Spacer(modifier = Modifier.height(16.dp))

        errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isSubmitting,
            colors = ButtonDefaults.buttonColors(
                containerColor = submitButtonColor,
                disabledContainerColor = submitButtonDisabledColor
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(4.dp),
                    color = SurfaceDefault,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = submitLabel,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
