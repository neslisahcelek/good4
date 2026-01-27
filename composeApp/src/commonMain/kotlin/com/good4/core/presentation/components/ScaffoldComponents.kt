package com.good4.core.presentation.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.good4.core.presentation.AppBackground
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
