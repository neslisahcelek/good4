package com.good4.core.util

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard

@OptIn(ExperimentalComposeUiApi::class)
actual suspend fun setPlainTextToClipboard(clipboard: Clipboard, text: String) {
    clipboard.setClipEntry(ClipEntry.withPlainText(text))
}
