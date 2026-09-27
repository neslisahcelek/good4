package com.good4.auth.presentation.register

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.good4.auth.presentation.components.AuthBackdrop
import com.good4.core.util.keyboardPadding

@Composable
internal fun RegisterScreenContentContainer(
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val focusManager = LocalFocusManager.current

    AuthBackdrop(
        modifier = modifier
            .padding(paddingValues)
            .consumeWindowInsets(paddingValues)
            .keyboardPadding()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus(force = true) })
            },
        belowTopBar = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}
