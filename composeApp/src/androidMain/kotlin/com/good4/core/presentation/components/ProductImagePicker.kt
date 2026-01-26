package com.good4.core.presentation.components

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.good4.core.presentation.DeepGreen
import com.good4.core.presentation.TextPrimary
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.error_image_upload_failed
import good4.composeapp.generated.resources.image_picker_camera
import good4.composeapp.generated.resources.image_picker_gallery
import good4.composeapp.generated.resources.image_uploaded
import good4.composeapp.generated.resources.image_uploading
import good4.composeapp.generated.resources.product_image_preview_desc
import org.jetbrains.compose.resources.stringResource
import java.io.File
import java.util.UUID

@Composable
actual fun ProductImagePicker(
    modifier: Modifier,
    currentImageUrl: String,
    onImageUrlChange: (String) -> Unit,
    onUploadStateChange: (Boolean) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    var isUploading by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val uploadFailedMessage = stringResource(Res.string.error_image_upload_failed)
    val galleryLabel = stringResource(Res.string.image_picker_gallery)
    val cameraLabel = stringResource(Res.string.image_picker_camera)
    val uploadingLabel = stringResource(Res.string.image_uploading)
    val uploadedLabel = stringResource(Res.string.image_uploaded)
    val previewDescription = stringResource(Res.string.product_image_preview_desc)

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            uploadImage(
                uri = uri,
                onStart = {
                    isUploading = true
                    onUploadStateChange(true)
                },
                onSuccess = { url ->
                    isUploading = false
                    onUploadStateChange(false)
                    onImageUrlChange(url)
                },
                onFailure = { error ->
                    isUploading = false
                    onUploadStateChange(false)
                    onError(error ?: uploadFailedMessage)
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
                onStart = {
                    isUploading = true
                    onUploadStateChange(true)
                },
                onSuccess = { url ->
                    isUploading = false
                    onUploadStateChange(false)
                    onImageUrlChange(url)
                },
                onFailure = { error ->
                    isUploading = false
                    onUploadStateChange(false)
                    onError(error ?: uploadFailedMessage)
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

        if (!isUploading && currentImageUrl.isNotBlank()) {
            Text(text = uploadedLabel)
        }

        if (currentImageUrl.isNotBlank()) {
            AsyncImage(
                model = currentImageUrl,
                contentDescription = previewDescription,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(Color.LightGray, RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
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
    onStart: () -> Unit,
    onSuccess: (String) -> Unit,
    onFailure: (String?) -> Unit
) {
    onStart()
    Log.d("ProductImagePicker", "Upload started for uri=$uri")
    val storage = Firebase.storage
    val imageRef = storage.reference.child("product_images/${UUID.randomUUID()}.jpg")
    imageRef.putFile(uri)
        .addOnSuccessListener {
            imageRef.downloadUrl
                .addOnSuccessListener { url ->
                    Log.d("ProductImagePicker", "Upload success, url=$url")
                    onSuccess(url.toString())
                }
                .addOnFailureListener { error ->
                    Log.e("ProductImagePicker", "Download URL failed", error)
                    onFailure(error.message)
                }
        }
        .addOnFailureListener { error ->
            Log.e("ProductImagePicker", "Upload failed", error)
            onFailure(error.message)
        }
}
