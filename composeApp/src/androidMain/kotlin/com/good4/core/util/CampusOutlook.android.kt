package com.good4.core.util

import android.content.Context
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import org.koin.core.context.GlobalContext

actual fun openCampusOutlook() {
    val context: Context = GlobalContext.get().get()
    launchCampusOutlook(context::startActivity)
}

internal fun launchCampusOutlook(startActivity: (Intent) -> Unit) {
    fun intent(uri: String) = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
    try { startActivity(intent("ms-outlook://")) }
    catch (_: ActivityNotFoundException) { startActivity(intent("https://outlook.office.com/mail")) }
}
