package com.good4.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary

@Composable
fun Good4Scaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Good4TopBar(
    title: String = "",
    modifier: Modifier = Modifier,
    titleContent: (@Composable () -> Unit)? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        modifier = modifier,
        title = {
            if (titleContent != null) {
                titleContent()
            } else {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        navigationIcon = navigationIcon,
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AppBackground,
            titleContentColor = TextPrimary,
            navigationIconContentColor = TextPrimary,
            actionIconContentColor = TextPrimary
        )
    )
}

@Composable
fun Good4NavigationBar(
    modifier: Modifier = Modifier,
    containerColor: Color = SurfaceDefault.copy(alpha = 0.95f),
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = 0.dp,
    content: @Composable RowScope.() -> Unit
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(BorderMuted)
                .align(Alignment.TopCenter)
        )
        
        NavigationBar(
            modifier = Modifier.fillMaxWidth(),
            containerColor = containerColor,
            contentColor = contentColor,
            tonalElevation = tonalElevation,
            windowInsets = WindowInsets(0, 0, 0, 0),
            content = content
        )
    }
}
