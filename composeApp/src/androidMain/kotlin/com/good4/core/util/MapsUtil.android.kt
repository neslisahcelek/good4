package com.good4.core.util

import android.content.Context
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import org.koin.core.context.GlobalContext

actual fun openMaps(address: String) {
    val context: Context = GlobalContext.get().get()
    launchMap(address, context::startActivity)
}

internal fun launchMap(address: String, startActivity: (Intent) -> Unit) {
    val normalized = address.trim()
    if (normalized.isEmpty()) return

    val mapIntent = if (isDirectMapUrl(normalized)) {
        Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    } else {
        val encodedAddress = Uri.encode(normalized)
        val gmmIntentUri = Uri.parse("geo:0,0?q=$encodedAddress")
        Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    // Launch directly: Android package visibility can hide an installed handler
    // from resolveActivity, even though startActivity can open it.
    try {
        startActivity(mapIntent)
        return
    } catch (_: ActivityNotFoundException) {
        // Fall back to a browser when no app handles the map scheme.
    }
    if (mapIntent.data?.scheme.equals("https", ignoreCase = true) ||
        mapIntent.data?.scheme.equals("http", ignoreCase = true)) return

    val encodedAddress = Uri.encode(normalized)
    val browserIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://maps.google.com/?q=$encodedAddress")
    ).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
        startActivity(browserIntent)
    } catch (_: ActivityNotFoundException) {
        Logger.e("Maps", "No application is available to open maps", null)
    }
}
