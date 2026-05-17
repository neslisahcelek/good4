package com.good4.core.util

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.ui.platform.Clipboard

actual suspend fun setPlainTextToClipboard(clipboard: Clipboard, text: String) {
    val nativeClipboard = clipboard.nativeClipboard
    nativeClipboard.setPrimaryClip(ClipData.newPlainText(null, text))
}
