package com.good4.core.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun ProductImagePicker(
    modifier: Modifier = Modifier,
    currentRemoteImageUrl: String,
    pendingImageBytes: ByteArray?,
    isUploading: Boolean,
    onPendingImageChange: (ByteArray?) -> Unit,
    onError: (String) -> Unit
)
