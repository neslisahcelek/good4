package com.good4.core.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class ImagePickerLabels(
    val gallery: String, val camera: String, val opening: String, val preparing: String,
    val uploading: String, val selected: String, val saved: String,
    val prepareError: String, val galleryError: String, val cameraError: String
)

@Composable
expect fun ProductImagePicker(
    modifier: Modifier = Modifier,
    currentRemoteImageUrl: String,
    pendingImageBytes: ByteArray?,
    isUploading: Boolean,
    onPendingImageChange: (ByteArray?) -> Unit,
    onError: (String) -> Unit,
    labels: ImagePickerLabels? = null
)
