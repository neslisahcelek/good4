package com.good4.community

import good4.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.scale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
internal actual fun rememberCoverImagePicker(onPicked: (ByteArray) -> Unit, onError: (String) -> Unit): CoverImagePicker {
    val imagePreparationError = stringResource(Res.string.community_gorsel_hazirlanamadi_baska_bir_gorsel_deneyin)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pickedCallback by rememberUpdatedState(onPicked)
    val errorCallback by rememberUpdatedState(onError)
    var preparing by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        preparing = true
        scope.launch {
            try {
                pickedCallback(withContext(Dispatchers.IO) { encodeCover(context, uri) })
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                errorCallback(imagePreparationError)
            } finally {
                preparing = false
            }
        }
    }
    return CoverImagePicker(
        open = { if (!preparing) launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        preparing = preparing
    )
}

private fun encodeCover(context: Context, uri: Uri): ByteArray {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    require(bounds.outWidth > 0 && bounds.outHeight > 0)
    var sampleSize = 1
    while (max(bounds.outWidth, bounds.outHeight) / sampleSize > CoverImageSpec.MAX_EDGE_PX * 2) sampleSize *= 2
    val decoded = context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sampleSize })
    } ?: error("decode failed")
    val orientation = runCatching {
        context.contentResolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }
    }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
    val transform = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(270f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(270f)
        }
    }
    val bitmap = if (transform.isIdentity) decoded else
        Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, transform, true)
    val longest = max(bitmap.width, bitmap.height)
    val scaled = if (longest <= CoverImageSpec.MAX_EDGE_PX) bitmap else {
        val ratio = CoverImageSpec.MAX_EDGE_PX.toFloat() / longest
        bitmap.scale((bitmap.width * ratio).roundToInt().coerceAtLeast(1), (bitmap.height * ratio).roundToInt().coerceAtLeast(1))
    }
    return try {
        ByteArrayOutputStream().use { out ->
            check(scaled.compress(Bitmap.CompressFormat.JPEG, CoverImageSpec.JPEG_QUALITY_PERCENT, out))
            out.toByteArray()
        }
    } finally {
        if (scaled !== bitmap) scaled.recycle()
        if (bitmap !== decoded) bitmap.recycle()
        decoded.recycle()
    }
}
