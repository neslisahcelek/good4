@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.good4.core.domain.ProductImageConstants
import com.good4.core.presentation.DeepGreen
import com.good4.core.presentation.TextPrimary
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.error_image_prepare_failed
import good4.composeapp.generated.resources.image_picker_camera
import good4.composeapp.generated.resources.image_picker_gallery
import good4.composeapp.generated.resources.image_uploading
import good4.composeapp.generated.resources.product_image_saved_remote
import good4.composeapp.generated.resources.product_image_selected_pending
import kotlinx.cinterop.Pinned
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.darwin.NSObject
import platform.posix.memcpy
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
    val scope = rememberCoroutineScope()
    val delegateHolder = remember { mutableStateOf<ImagePickerDelegate?>(null) }
    val hadUploadingPhase = remember { mutableStateOf(false) }
    val showSavedRemoteStatus = remember { mutableStateOf(false) }

    val galleryLabel = stringResource(Res.string.image_picker_gallery)
    val cameraLabel = stringResource(Res.string.image_picker_camera)
    val uploadingLabel = stringResource(Res.string.image_uploading)
    val selectedPendingLabel = stringResource(Res.string.product_image_selected_pending)
    val savedRemoteLabel = stringResource(Res.string.product_image_saved_remote)
    val prepareFailedMessage = stringResource(Res.string.error_image_prepare_failed)

    LaunchedEffect(isUploading, pendingImageBytes, currentRemoteImageUrl) {
        if (isUploading) {
            hadUploadingPhase.value = true
        }
        if (pendingImageBytes != null) {
            showSavedRemoteStatus.value = false
        }
        if (!isUploading && hadUploadingPhase.value) {
            showSavedRemoteStatus.value =
                pendingImageBytes == null && currentRemoteImageUrl.isNotBlank()
            hadUploadingPhase.value = false
        }
    }

    fun processPickedImage(image: UIImage) {
        scope.launch {
            try {
                val bytes = withContext(Dispatchers.Default) {
                    val scaled = image.scaleToMaxEdge(ProductImageConstants.MAX_EDGE_PX.toDouble())
                    val nsData = UIImageJPEGRepresentation(
                        scaled,
                        ProductImageConstants.JPEG_QUALITY_PERCENT / 100.0
                    ) ?: throw IllegalArgumentException("jpeg failed")
                    nsData.toByteArrayFromNSData()
                }
                withContext(Dispatchers.Main) {
                    onPendingImageChange(bytes)
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    onError(prepareFailedMessage)
                }
            }
        }
    }

    fun openPicker(sourceType: UIImagePickerControllerSourceType) {
        val delegate = ImagePickerDelegate(
            onImagePicked = { image ->
                delegateHolder.value = null
                if (image == null) {
                    scope.launch {
                        withContext(Dispatchers.Main) {
                            onError(prepareFailedMessage)
                        }
                    }
                } else {
                    processPickedImage(image)
                }
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

        if (!isUploading && showSavedRemoteStatus.value) {
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

private fun UIImage.scaleToMaxEdge(maxEdge: Double): UIImage {
    val w = size.useContents { width.toDouble() }
    val h = size.useContents { height.toDouble() }
    val maxDim = max(w, h)
    if (maxDim <= maxEdge) return this
    val scale = maxEdge / maxDim
    val newW = (w * scale).roundToInt().toDouble()
    val newH = (h * scale).roundToInt().toDouble()
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(newW, newH), false, 1.0)
    this.drawInRect(CGRectMake(0.0, 0.0, newW, newH))
    val out = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()
    return out ?: this
}

private fun NSData.toByteArrayFromNSData(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    val result = ByteArray(size)
    result.usePinned { pinned: Pinned<ByteArray> ->
        memcpy(pinned.addressOf(0), this.bytes, length)
    }
    return result
}

private class ImagePickerDelegate(
    private val onImagePicked: (UIImage?) -> Unit,
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
        onImagePicked(image)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
        onCancel()
    }
}
