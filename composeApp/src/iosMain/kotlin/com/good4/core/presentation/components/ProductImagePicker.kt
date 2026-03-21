package com.good4.core.presentation.components

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
import androidx.compose.ui.unit.dp
import com.good4.core.presentation.DeepGreen
import com.good4.core.presentation.TextPrimary
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.storage.Data
import dev.gitlive.firebase.storage.StorageReference
import dev.gitlive.firebase.storage.storage
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.error_image_prepare_failed
import good4.composeapp.generated.resources.error_image_upload_failed
import good4.composeapp.generated.resources.error_image_upload_permission_denied
import good4.composeapp.generated.resources.error_image_upload_timeout
import good4.composeapp.generated.resources.image_picker_camera
import good4.composeapp.generated.resources.image_picker_gallery
import good4.composeapp.generated.resources.image_uploaded
import good4.composeapp.generated.resources.image_uploading
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.jetbrains.compose.resources.stringResource
import platform.Foundation.NSData
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject
import kotlin.random.Random

@Composable
actual fun ProductImagePicker(
    modifier: Modifier,
    currentImageUrl: String,
    isUploading: Boolean,
    onImageUrlChange: (String) -> Unit,
    onUploadStateChange: (Boolean) -> Unit,
    onError: (String) -> Unit
) {
    var lastUploadedUrl by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val delegateHolder = remember { mutableStateOf<ImagePickerDelegate?>(null) }

    LaunchedEffect(currentImageUrl) {
        if (currentImageUrl.isBlank()) {
            lastUploadedUrl = ""
        } else if (lastUploadedUrl.isNotBlank() && currentImageUrl != lastUploadedUrl) {
            lastUploadedUrl = ""
        }
    }

    val galleryLabel = stringResource(Res.string.image_picker_gallery)
    val cameraLabel = stringResource(Res.string.image_picker_camera)
    val uploadingLabel = stringResource(Res.string.image_uploading)
    val uploadedLabel = stringResource(Res.string.image_uploaded)
    val uploadFailedMessage = stringResource(Res.string.error_image_upload_failed)
    val uploadPermissionDeniedMessage =
        stringResource(Res.string.error_image_upload_permission_denied)
    val prepareFailedMessage = stringResource(Res.string.error_image_prepare_failed)
    val uploadTimeoutMessage = stringResource(Res.string.error_image_upload_timeout)

    val displayUrl = lastUploadedUrl.ifBlank { currentImageUrl }

    fun uploadImageData(nsData: NSData) {
        scope.launch {
            onUploadStateChange(true)
            try {
                val storageRef = Firebase.storage.reference
                    .child("product_images/${Random.nextLong()}.jpg")
                val url = withTimeout(120_000L) {
                    storageRef.putData(Data(nsData))
                    // Let the upload session fully finalize before requesting the download URL (avoids HTTP 400
                    // "Upload has already been finalized" / cancelFetcher noise on iOS).
                    delay(150)
                    storageRef.getDownloadUrlWithRetry()
                }
                withContext(Dispatchers.Main) {
                    lastUploadedUrl = url
                    onImageUrlChange(url)
                }
            } catch (e: TimeoutCancellationException) {
                withContext(Dispatchers.Main) {
                    onError(uploadTimeoutMessage)
                }
            } catch (e: CancellationException) {
                val raw = e.message?.takeIf { it.isNotBlank() } ?: e.toString()
                val lower = raw.lowercase()
                val isPermissionIssue =
                    "permission denied" in lower ||
                        "unauthorized" in lower ||
                        "code=403" in lower ||
                        "\"code\": 403" in lower
                val isStorageFinalizeIssue =
                    "finalized" in lower ||
                        "httpstatus" in lower ||
                        "cancelfetcher" in lower ||
                        "code=400" in lower

                if (isPermissionIssue) {
                    withContext(Dispatchers.Main) {
                        onError(uploadPermissionDeniedMessage)
                    }
                } else if (isStorageFinalizeIssue) {
                    withContext(Dispatchers.Main) {
                        onError(uploadFailedMessage)
                    }
                } else {
                    throw e
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    val raw = e.message?.takeIf { it.isNotBlank() } ?: e.toString()
                    val lower = raw.lowercase()
                    val message = when {
                        "permission denied" in lower || "unauthorized" in lower || "code=403" in lower ||
                            "\"code\": 403" in lower -> uploadPermissionDeniedMessage
                        "finalized" in lower || "httpstatus" in lower -> uploadFailedMessage
                        raw.isBlank() -> uploadFailedMessage
                        else -> raw
                    }
                    onError(message)
                }
            } finally {
                onUploadStateChange(false)
            }
        }
    }

    fun openPicker(sourceType: UIImagePickerControllerSourceType) {
        val delegate = ImagePickerDelegate(
            onImageData = { nsData ->
                delegateHolder.value = null
                uploadImageData(nsData)
            },
            onEncodeFailed = {
                scope.launch {
                    withContext(Dispatchers.Main) {
                        onError(prepareFailedMessage)
                    }
                }
            },
            onCancel = {
                delegateHolder.value = null
            }
        )
        delegateHolder.value = delegate

        val picker = UIImagePickerController()
        picker.sourceType = sourceType
        picker.allowsEditing = false
        picker.delegate = delegate

        UIApplication.sharedApplication.keyWindow
            ?.rootViewController
            ?.presentViewController(picker, animated = true, completion = null)
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
                    openPicker(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary)
                },
                colors = ButtonDefaults.buttonColors(containerColor = DeepGreen)
            ) {
                Text(text = galleryLabel)
            }
            if (UIImagePickerController.isSourceTypeAvailable(
                    UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
                )
            ) {
                OutlinedButton(
                    onClick = {
                        openPicker(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)
                    }
                ) {
                    Text(text = cameraLabel)
                }
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

/**
 * iOS Firebase Storage: immediate getDownloadUrl after putData can hit HTTP 400
 * "Upload has already been finalized" (see cancelFetcher logs). Retry after a short delay.
 */
private suspend fun StorageReference.getDownloadUrlWithRetry(): String {
    return try {
        getDownloadUrl()
    } catch (first: Exception) {
        val m = first.message?.lowercase().orEmpty()
        if ("finalized" in m || "httpstatus" in m) {
            delay(500)
            getDownloadUrl()
        } else {
            throw first
        }
    }
}

private class ImagePickerDelegate(
    private val onImageData: (NSData) -> Unit,
    private val onEncodeFailed: () -> Unit,
    private val onCancel: () -> Unit
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>
    ) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        if (image == null) {
            onEncodeFailed()
            return
        }
        val nsData = UIImageJPEGRepresentation(image, 0.8)
        if (nsData == null) {
            onEncodeFailed()
            return
        }
        onImageData(nsData)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
        onCancel()
    }
}
