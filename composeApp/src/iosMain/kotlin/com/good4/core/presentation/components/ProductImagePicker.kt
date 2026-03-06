package com.good4.core.presentation.components

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.good4.core.presentation.DeepGreen
import com.good4.core.presentation.TextPrimary
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.storage.Data
import dev.gitlive.firebase.storage.storage
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.error_image_upload_failed
import good4.composeapp.generated.resources.image_picker_camera
import good4.composeapp.generated.resources.image_picker_gallery
import good4.composeapp.generated.resources.image_uploaded
import good4.composeapp.generated.resources.image_uploading
import good4.composeapp.generated.resources.product_image_preview_desc
import kotlinx.coroutines.launch
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
    onImageUrlChange: (String) -> Unit,
    onUploadStateChange: (Boolean) -> Unit,
    onError: (String) -> Unit
) {
    var isUploading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val delegateHolder = remember { mutableStateOf<ImagePickerDelegate?>(null) }

    val galleryLabel = stringResource(Res.string.image_picker_gallery)
    val cameraLabel = stringResource(Res.string.image_picker_camera)
    val uploadingLabel = stringResource(Res.string.image_uploading)
    val uploadedLabel = stringResource(Res.string.image_uploaded)
    val uploadFailedMessage = stringResource(Res.string.error_image_upload_failed)
    val previewDescription = stringResource(Res.string.product_image_preview_desc)

    fun uploadImageData(nsData: NSData) {
        scope.launch {
            isUploading = true
            onUploadStateChange(true)
            try {
                val storageRef = Firebase.storage.reference
                    .child("product_images/${Random.nextLong()}.jpg")
                storageRef.putData(Data(nsData))
                val url = storageRef.getDownloadUrl()
                isUploading = false
                onUploadStateChange(false)
                onImageUrlChange(url)
            } catch (e: Exception) {
                isUploading = false
                onUploadStateChange(false)
                onError(e.message ?: uploadFailedMessage)
            }
        }
    }

    fun openPicker(sourceType: UIImagePickerControllerSourceType) {
        val delegate = ImagePickerDelegate(
            onImagePicked = { nsData ->
                delegateHolder.value = null
                nsData?.let { uploadImageData(it) }
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

private class ImagePickerDelegate(
    private val onImagePicked: (NSData?) -> Unit
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>
    ) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        val nsData = image?.let { UIImageJPEGRepresentation(it, 0.8) }
        onImagePicked(nsData)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
        onImagePicked(null)
    }
}
