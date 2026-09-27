package com.good4.core.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import org.koin.core.context.GlobalContext

actual fun openWalkingDirections(latitude: Double, longitude: Double) {
    val context: Context = GlobalContext.get().get()
    // Google Maps handles this link in its app when installed and in the browser otherwise.
    val intent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude&travelmode=walking")
    ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Logger.e("Maps", "No application is available to open directions", null)
    }
}
