package com.good4.core.util

import androidx.compose.ui.platform.Clipboard

expect suspend fun setPlainTextToClipboard(clipboard: Clipboard, text: String)
