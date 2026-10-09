package com.good4.update

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
internal actual fun DisableBackHandler() {
    BackHandler(enabled = true) {
        // Un-dismissible: intentionally ignore system back press/gesture
    }
}
