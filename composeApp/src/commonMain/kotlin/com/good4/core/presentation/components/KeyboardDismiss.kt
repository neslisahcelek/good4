package com.good4.core.presentation.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/**
 * A tap that no control consumes closes the keyboard. Buttons, text fields and scrolling
 * consume their own touches, so only taps on empty space reach this.
 */
@Composable
fun Modifier.dismissKeyboardOnTap(): Modifier {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    return pointerInput(focusManager, keyboard) {
        detectTapGestures(onTap = {
            focusManager.clearFocus()
            keyboard?.hide()
        })
    }
}
