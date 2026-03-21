package com.good4.core.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.datetime.Clock

/**
 * Creates a click handler that prevents multiple rapid clicks.
 * Use this with `remember` in Composable functions to prevent double-click issues.
 *
 * @param onClick The action to perform when clicked
 * @param delayMillis Minimum time in milliseconds between clicks (default: 500ms)
 * @return A click handler that prevents rapid successive clicks
 */
fun singleClick(
    delayMillis: Long = 500L,
    onClick: () -> Unit
): () -> Unit {
    var lastClickTime = 0L
    return {
        val currentTime = Clock.System.now().toEpochMilliseconds()
        if (currentTime - lastClickTime >= delayMillis) {
            lastClickTime = currentTime
            onClick()
        }
    }
}

/**
 * Platform-specific modifier for system bars padding (status bar and navigation bar).
 * On iOS, this applies safe area insets.
 * On Android, this applies system bars insets.
 */
@Composable
expect fun Modifier.systemBarsPadding(): Modifier

/**
 * Platform-specific modifier for top safe area padding only.
 * Pushes content below Dynamic Island on iOS and status bar on Android.
 */
@Composable
expect fun Modifier.topSafeAreaPadding(): Modifier
