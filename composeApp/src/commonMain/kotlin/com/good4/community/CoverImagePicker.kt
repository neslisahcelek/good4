package com.good4.community

import androidx.compose.runtime.Composable

/** Community covers are Instagram-style 4:5 posters, so they keep more detail than product photos. */
internal object CoverImageSpec {
    const val MAX_EDGE_PX = 1350
    const val JPEG_QUALITY_PERCENT = 80
    const val ASPECT_RATIO = 4f / 5f
}

internal class CoverImagePicker(val open: () -> Unit, val preparing: Boolean)

/** Opens the photo library straight away (no intermediate sheet) and returns a scaled JPEG. */
@Composable
internal expect fun rememberCoverImagePicker(onPicked: (ByteArray) -> Unit, onError: (String) -> Unit): CoverImagePicker
