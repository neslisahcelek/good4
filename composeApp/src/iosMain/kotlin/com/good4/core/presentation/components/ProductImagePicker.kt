package com.good4.core.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.image_picker_ios_placeholder
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun ProductImagePicker(
    modifier: Modifier,
    currentImageUrl: String,
    onImageUrlChange: (String) -> Unit,
    onUploadStateChange: (Boolean) -> Unit,
    onError: (String) -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(Res.string.image_picker_ios_placeholder),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
