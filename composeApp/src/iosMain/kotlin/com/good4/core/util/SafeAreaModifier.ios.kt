package com.good4.core.util

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun Modifier.systemBarsPadding(): Modifier {
    return this.windowInsetsPadding(WindowInsets.safeDrawing)
}

@Composable
actual fun Modifier.topSafeAreaPadding(): Modifier {
    return this.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
}
