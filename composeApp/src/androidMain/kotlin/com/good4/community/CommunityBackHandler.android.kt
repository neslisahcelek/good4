package com.good4.community

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
internal actual fun CommunityBackHandler(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
}
