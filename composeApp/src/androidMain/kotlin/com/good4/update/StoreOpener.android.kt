package com.good4.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.good4.BuildConfig
import config.StoreLinks
import org.koin.core.context.GlobalContext

actual fun openStoreAppPage() {
    val context = runCatching { GlobalContext.get().get<Context>() }.getOrNull() ?: return
    val packageName = BuildConfig.APPLICATION_ID
    val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
        .setPackage("com.android.vending")
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    runCatching {
        context.startActivity(marketIntent)
    }.onFailure {
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(StoreLinks.GOOGLE_PLAY_URL))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching {
            context.startActivity(webIntent)
        }
    }
}
