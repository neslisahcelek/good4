package com.good4.community

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerConfigurationAssetRepresentationModeCurrent
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.posix.memcpy
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
internal actual fun rememberCoverImagePicker(onPicked: (ByteArray) -> Unit, onError: (String) -> Unit): CoverImagePicker {
    val scope = rememberCoroutineScope()
    val pickedCallback by rememberUpdatedState(onPicked)
    val errorCallback by rememberUpdatedState(onError)
    var preparing by remember { mutableStateOf(false) }
    // PHPickerViewController keeps its delegate weakly, so it is retained here until the pick ends.
    val delegateHolder = remember { mutableStateOf<NSObject?>(null) }

    fun open() {
        if (preparing || delegateHolder.value != null) return
        val presenter = topPresenter()
        if (presenter == null) {
            errorCallback("Galeri açılamadı. Tekrar deneyin.")
            return
        }
        val picker = PHPickerViewController(configuration = PHPickerConfiguration().apply {
            filter = PHPickerFilter.imagesFilter
            selectionLimit = 1
            preferredAssetRepresentationMode = PHPickerConfigurationAssetRepresentationModeCurrent
        })
        val delegate = CoverPickerDelegate(
            onSelection = { preparing = true },
            onData = { data ->
                scope.launch {
                    delegateHolder.value = null
                    try {
                        val bytes = withContext(Dispatchers.Default) { encodeCover(data) }
                        pickedCallback(bytes)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        errorCallback("Görsel hazırlanamadı. Başka bir görsel deneyin.")
                    } finally {
                        preparing = false
                    }
                }
            },
            onCancel = { scope.launch { delegateHolder.value = null } }
        )
        delegateHolder.value = delegate
        picker.delegate = delegate
        presenter.presentViewController(picker, animated = true, completion = null)
    }

    return CoverImagePicker(::open, preparing)
}

private class CoverPickerDelegate(
    private val onSelection: () -> Unit,
    private val onData: (NSData?) -> Unit,
    private val onCancel: () -> Unit
) : NSObject(), PHPickerViewControllerDelegateProtocol {
    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val result = didFinishPicking.firstOrNull() as? PHPickerResult
        if (result == null) {
            onCancel()
            return
        }
        onSelection()
        result.itemProvider.loadDataRepresentationForTypeIdentifier("public.image") { data, _ -> onData(data) }
    }
}

private fun topPresenter(): UIViewController? {
    var presenter = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return null
    while (true) {
        val presented = presenter.presentedViewController ?: return presenter
        if (presented.isBeingDismissed()) return presenter
        presenter = presented
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun encodeCover(data: NSData?): ByteArray {
    val image = data?.let { UIImage.imageWithData(it) } ?: error("decode failed")
    val width = image.size.useContents { width }
    val height = image.size.useContents { height }
    val longest = max(width, height)
    val scaled = if (longest <= CoverImageSpec.MAX_EDGE_PX) image else {
        val scale = CoverImageSpec.MAX_EDGE_PX / longest
        val newWidth = (width * scale).roundToInt().toDouble()
        val newHeight = (height * scale).roundToInt().toDouble()
        UIGraphicsBeginImageContextWithOptions(CGSizeMake(newWidth, newHeight), false, 1.0)
        image.drawInRect(CGRectMake(0.0, 0.0, newWidth, newHeight))
        val output = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        output ?: image
    }
    val jpeg = UIImageJPEGRepresentation(scaled, CoverImageSpec.JPEG_QUALITY_PERCENT / 100.0) ?: error("jpeg failed")
    val size = jpeg.length.toInt()
    val bytes = ByteArray(size)
    if (size > 0) bytes.usePinned { memcpy(it.addressOf(0), jpeg.bytes, jpeg.length) }
    return bytes
}
