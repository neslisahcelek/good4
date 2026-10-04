package com.good4.community

import androidx.compose.runtime.Composable

/** System back (Android gesture or button) closes the student event page instead of the whole screen. */
@Composable
internal expect fun CommunityBackHandler(onBack: () -> Unit)
