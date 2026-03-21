package com.good4.core.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun ProductImagePicker(
    modifier: Modifier = Modifier,
    currentImageUrl: String,
    isUploading: Boolean,
    onImageUrlChange: (String) -> Unit,
    onUploadStateChange: (Boolean) -> Unit = {},
    onError: (String) -> Unit = {}
)
