package com.good4.community

import androidx.compose.runtime.Composable

/** iOS has no system back button; the top bar's back arrow closes the page. */
@Composable
internal actual fun CommunityBackHandler(onBack: () -> Unit) = Unit
