package com.good4.core.presentation.components

import android.content.Context
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
import com.good4.core.presentation.DeepGreen
import com.good4.core.presentation.TextPrimary
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.error_image_upload_failed
import good4.composeapp.generated.resources.error_image_upload_permission_denied
import good4.composeapp.generated.resources.image_picker_camera
import good4.composeapp.generated.resources.image_picker_gallery
import good4.composeapp.generated.resources.image_uploaded
import good4.composeapp.generated.resources.image_uploading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import java.io.File
import java.util.UUID

@Composable
actual fun ProductImagePicker(
    modifier: Modifier,
    currentImageUrl: String,
    isUploading: Boolean,
    onImageUrlChange: (String) -> Unit,
    onUploadStateChange: (Boolean) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var lastUploadedUrl by remember { mutableStateOf("") }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(currentImageUrl) {
        if (currentImageUrl.isBlank()) {
            lastUploadedUrl = ""
        } else if (lastUploadedUrl.isNotBlank() && currentImageUrl != lastUploadedUrl) {
            lastUploadedUrl = ""
        }
    }
    val uploadFailedMessage = stringResource(Res.string.error_image_upload_failed)
    val uploadPermissionDeniedMessage =
        stringResource(Res.string.error_image_upload_permission_denied)
    val galleryLabel = stringResource(Res.string.image_picker_gallery)
    val cameraLabel = stringResource(Res.string.image_picker_camera)
    val uploadingLabel = stringResource(Res.string.image_uploading)
    val uploadedLabel = stringResource(Res.string.image_uploaded)

    val displayUrl = lastUploadedUrl.ifBlank { currentImageUrl }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            uploadImage(
                uri = uri,
                scope = scope,
                onStart = {
                    onUploadStateChange(true)
                },
                onSuccess = { url ->
                    lastUploadedUrl = url
                    onUploadStateChange(false)
                    onImageUrlChange(url)
                },
                onFailure = { error ->
                    onUploadStateChange(false)
                    onError(
                        mapUploadErrorMessage(
                            raw = error,
                            fallback = uploadFailedMessage,
                            permissionDenied = uploadPermissionDeniedMessage
                        )
                    )
                }
            )
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            uploadImage(
                uri = uri,
                scope = scope,
                onStart = {
                    onUploadStateChange(true)
                },
                onSuccess = { url ->
                    lastUploadedUrl = url
                    onUploadStateChange(false)
                    onImageUrlChange(url)
                },
                onFailure = { error ->
                    onUploadStateChange(false)
                    onError(
                        mapUploadErrorMessage(
                            raw = error,
                            fallback = uploadFailedMessage,
                            permissionDenied = uploadPermissionDeniedMessage
                        )
                    )
                }
            )
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
                    galleryLauncher.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = DeepGreen)
            ) {
                Text(text = galleryLabel)
            }
            OutlinedButton(
                onClick = {
                    val uri = createTempImageUri(context)
                    pendingCameraUri = uri
                    cameraLauncher.launch(uri)
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

        if (!isUploading && displayUrl.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = uploadedLabel,
                    tint = DeepGreen,
                    modifier = Modifier.size(22.dp)
                )
                Text(text = uploadedLabel)
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

private fun uploadImage(
    uri: Uri,
    scope: CoroutineScope,
    onStart: () -> Unit,
    onSuccess: (String) -> Unit,
    onFailure: (String?) -> Unit
) {
    onStart()
    val storage = Firebase.storage
    val imageRef = storage.reference.child("product_images/${UUID.randomUUID()}.jpg")
    imageRef.putFile(uri)
        .addOnSuccessListener {
            imageRef.downloadUrl
                .addOnSuccessListener { url ->
                    scope.launch(Dispatchers.Main.immediate) {
                        onSuccess(url.toString())
                    }
                }
                .addOnFailureListener { error ->
                    scope.launch(Dispatchers.Main.immediate) {
                        onFailure(error.message)
                    }
                }
        }
        .addOnFailureListener { error ->
            scope.launch(Dispatchers.Main.immediate) {
                onFailure(error.message)
            }
        }
}

private fun mapUploadErrorMessage(
    raw: String?,
    fallback: String,
    permissionDenied: String
): String {
    val lower = raw?.lowercase().orEmpty()
    if (
        "permission denied" in lower ||
        "unauthorized" in lower ||
        "not authorized" in lower ||
        "403" in lower
    ) {
        return permissionDenied
    }
    return raw?.takeIf { it.isNotBlank() } ?: fallback
}
