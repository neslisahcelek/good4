package com.good4.community

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
                errorCallback("Görsel hazırlanamadı. Başka bir görsel deneyin.")
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
    val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        ?: error("decode failed")
    val longest = max(bitmap.width, bitmap.height)
    val scaled = if (longest <= CoverImageSpec.MAX_EDGE_PX) bitmap else {
        val ratio = CoverImageSpec.MAX_EDGE_PX.toFloat() / longest
        bitmap.scale((bitmap.width * ratio).roundToInt().coerceAtLeast(1), (bitmap.height * ratio).roundToInt().coerceAtLeast(1))
    }
    return ByteArrayOutputStream().use { out ->
        scaled.compress(Bitmap.CompressFormat.JPEG, CoverImageSpec.JPEG_QUALITY_PERCENT, out)
        out.toByteArray()
    }
}
