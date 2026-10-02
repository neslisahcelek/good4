package com.good4.review

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

/** Screen signals stay in Compose; exposure and quiet-period decisions stay in the ViewModel. */
@Composable
fun ReviewFeaturePrompt(feature: ReviewFeature, contentReady: Boolean, isScrolling: Boolean, canPrompt: Boolean = true) {
    val viewModel: StoreReviewViewModel = koinViewModel(key = "store_review_feature")
    val launcher = rememberStoreReviewLauncher()
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    SideEffect {
        viewModel.updateFeature(
            feature = feature,
            studentResumed = lifecycleState == Lifecycle.State.RESUMED,
            contentReady = contentReady && canPrompt,
            isIdle = !isScrolling,
            reviewLauncher = launcher
        )
    }
    DisposableEffect(viewModel, feature) {
        onDispose { viewModel.leaveScreen() }
    }
}
