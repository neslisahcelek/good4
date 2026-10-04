package com.good4.review

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.good4.BuildConfig
import com.google.android.play.core.review.ReviewManagerFactory
import config.StoreLinks
import org.koin.core.context.GlobalContext

private fun context() = GlobalContext.get().get<Context>()
private fun preferences() = context().getSharedPreferences("good4_preferences", Context.MODE_PRIVATE)
actual fun loadReviewHistory(): String? = preferences().getString("store_review_history", null)
actual fun saveReviewHistory(value: String) {
    check(preferences().edit().putString("store_review_history", value).commit())
}
actual fun reviewAppVersion(): String = BuildConfig.VERSION_NAME
actual fun automaticReviewEnabled(): Boolean = BuildConfig.DEBUG || BuildConfig.APPLICATION_ID == "com.good4"

actual suspend fun openReviewStore(): StoreOpenResult {
    val url = StoreLinks.GOOGLE_PLAY_URL.trim()
    if (url.isBlank()) return StoreOpenResult.NOT_CONFIGURED
    val uri = Uri.parse(url)
    if (uri.scheme != "https" || uri.host != "play.google.com" || uri.getQueryParameter("id").isNullOrBlank()) {
        return StoreOpenResult.FAILED
    }
    val appContext = context()
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${Uri.encode(uri.getQueryParameter("id"))}"))
        .setPackage("com.android.vending").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return runCatching { appContext.startActivity(market); StoreOpenResult.OPENED }.getOrElse {
        runCatching {
            appContext.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            StoreOpenResult.OPENED
        }.getOrDefault(StoreOpenResult.FAILED)
    }
}

private fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

@Composable
actual fun rememberStoreReviewLauncher(): StoreReviewLauncher {
    val context = LocalContext.current
    return remember(context) {
        object : StoreReviewLauncher {
            override fun requestReview(approve: () -> Boolean) {
                val activity = context.activity() ?: return
                val manager = ReviewManagerFactory.create(activity)
                manager.requestReviewFlow().addOnCompleteListener { task ->
                    if (!task.isSuccessful || activity.isFinishing || activity.isDestroyed) return@addOnCompleteListener
                    val lifecycle = (activity as? LifecycleOwner)?.lifecycle ?: return@addOnCompleteListener
                    if (lifecycle.currentState != Lifecycle.State.RESUMED || !approve()) return@addOnCompleteListener
                    // Completion does not tell us whether a dialog appeared or a rating was submitted.
                    manager.launchReviewFlow(activity, task.result)
                }
            }
        }
    }
}
