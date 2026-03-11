package com.good4.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.koin.core.context.GlobalContext

actual fun openMaps(address: String) {
    val context: Context = GlobalContext.get().get()
    val encodedAddress = Uri.encode(address)
    val gmmIntentUri = Uri.parse("geo:0,0?q=$encodedAddress")
    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    if (mapIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(mapIntent)
    } else {
        val browserIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://maps.google.com/?q=$encodedAddress")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(browserIntent)
    }
}
