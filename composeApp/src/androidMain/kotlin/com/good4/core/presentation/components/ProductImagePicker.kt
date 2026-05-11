package com.good4.core.presentation.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import com.good4.core.domain.ProductImageConstants
import com.good4.core.presentation.DeepGreen
import com.good4.core.presentation.TextPrimary
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.error_camera_open_failed
import good4.composeapp.generated.resources.error_image_picker_open_failed
import good4.composeapp.generated.resources.error_image_prepare_failed
import good4.composeapp.generated.resources.image_picker_camera
import good4.composeapp.generated.resources.image_picker_gallery
import good4.composeapp.generated.resources.image_uploading
import good4.composeapp.generated.resources.product_image_saved_remote
import good4.composeapp.generated.resources.product_image_selected_pending
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
actual fun ProductImagePicker(
    modifier: Modifier,
    currentRemoteImageUrl: String,
    pendingImageBytes: ByteArray?,
    isUploading: Boolean,
    onPendingImageChange: (ByteArray?) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val hadUploadingPhase = remember { mutableStateOf(false) }
    var showSavedRemoteStatus by remember { mutableStateOf(false) }

    val galleryLabel = stringResource(Res.string.image_picker_gallery)
    val cameraLabel = stringResource(Res.string.image_picker_camera)
    val uploadingLabel = stringResource(Res.string.image_uploading)
    val selectedPendingLabel = stringResource(Res.string.product_image_selected_pending)
    val savedRemoteLabel = stringResource(Res.string.product_image_saved_remote)
    val prepareFailedMessage = stringResource(Res.string.error_image_prepare_failed)
    val pickerOpenFailedMessage = stringResource(Res.string.error_image_picker_open_failed)
    val cameraOpenFailedMessage = stringResource(Res.string.error_camera_open_failed)

    LaunchedEffect(isUploading, pendingImageBytes, currentRemoteImageUrl) {
        if (isUploading) {
            hadUploadingPhase.value = true
        }
        if (pendingImageBytes != null) {
            showSavedRemoteStatus = false
        }
        if (!isUploading && hadUploadingPhase.value) {
            showSavedRemoteStatus =
                pendingImageBytes == null && currentRemoteImageUrl.isNotBlank()
            hadUploadingPhase.value = false
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val bytes = withContext(Dispatchers.Default) {
                        compressJpegFromUri(context, uri)
                    }
                    onPendingImageChange(bytes)
                } catch (_: Exception) {
                    onError(prepareFailedMessage)
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            scope.launch {
                try {
                    val bytes = withContext(Dispatchers.Default) {
                        compressJpegFromUri(context, uri)
                    }
                    onPendingImageChange(bytes)
                } catch (_: Exception) {
                    onError(prepareFailedMessage)
                }
            }
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    try {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    } catch (_: Exception) {
                        onError(pickerOpenFailedMessage)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DeepGreen)
            ) {
                Text(text = galleryLabel)
            }
            OutlinedButton(
                onClick = {
                    try {
                        val uri = createTempImageUri(context)
                        pendingCameraUri = uri
                        cameraLauncher.launch(uri)
                    } catch (_: Exception) {
                        onError(cameraOpenFailedMessage)
                    }
                }
            ) {
                Text(text = cameraLabel)
            }
        }

        if (isUploading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = TextPrimary,
                    strokeWidth = 2.dp
                )
                Text(text = uploadingLabel)
            }
        }

        if (!isUploading && pendingImageBytes != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = selectedPendingLabel,
                    tint = DeepGreen,
                    modifier = Modifier.size(22.dp)
                )
                Text(text = selectedPendingLabel)
            }
        }

        if (!isUploading && showSavedRemoteStatus) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = savedRemoteLabel,
                    tint = DeepGreen,
                    modifier = Modifier.size(22.dp)
                )
                Text(text = savedRemoteLabel)
            }
        }
    }
}

private fun createTempImageUri(context: Context): Uri {
    val imageFile = File(context.cacheDir, "product_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}

private fun compressJpegFromUri(context: Context, uri: Uri): ByteArray {
    val input = context.contentResolver.openInputStream(uri)
        ?: throw IllegalArgumentException("openInputStream failed")
    input.use { stream ->
        var bitmap = BitmapFactory.decodeStream(stream)
            ?: throw IllegalArgumentException("decode failed")
        val maxEdge = ProductImageConstants.MAX_EDGE_PX
        val w = bitmap.width
        val h = bitmap.height
        val maxDim = max(w, h)
        if (maxDim > maxEdge) {
            val scale = maxEdge.toFloat() / maxDim
            val newW = (w * scale).roundToInt().coerceAtLeast(1)
            val newH = (h * scale).roundToInt().coerceAtLeast(1)
            bitmap = bitmap.scale(newW, newH)
        }
        val out = ByteArrayOutputStream()
        bitmap.compress(
            Bitmap.CompressFormat.JPEG,
            ProductImageConstants.JPEG_QUALITY_PERCENT,
            out
        )
        return out.toByteArray()
    }
}
