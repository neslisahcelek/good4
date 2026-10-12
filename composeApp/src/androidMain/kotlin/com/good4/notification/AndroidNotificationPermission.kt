package com.good4.notification

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

internal fun androidNotificationPermissionGranted(context: Context): Boolean =
    NotificationManagerCompat.from(context).areNotificationsEnabled() &&
        (Build.VERSION.SDK_INT < 33 || hasRuntimeNotificationPermission(context))

private fun hasRuntimeNotificationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

/** Returns true only when completion will arrive through the permission result launcher. */
internal fun requestAndroidNotificationPermission(context: Context, launchPermission: (String) -> Unit): Boolean {
    val activity = context.findActivity()
    val preferences = context.getSharedPreferences("campus_push", Context.MODE_PRIVATE)
    if (Build.VERSION.SDK_INT >= 33 && !hasRuntimeNotificationPermission(context) && activity != null &&
        (!preferences.getBoolean("permission_asked", false) ||
            activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS))
    ) {
        launchPermission(Manifest.permission.POST_NOTIFICATIONS)
        preferences.edit().putBoolean("permission_asked", true).apply()
        return true
    }
    openAndroidNotificationSettings(context)
    return false
}

internal fun openAndroidNotificationSettings(context: Context) {
    fun launch(intent: Intent) {
        if (context.findActivity() == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
    if (Build.VERSION.SDK_INT >= 26) {
        try {
            launch(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
            return
        } catch (_: ActivityNotFoundException) {
            // Some devices do not expose the dedicated notification settings activity.
        } catch (_: SecurityException) {
            // Fall back to the public application details page.
        }
    }
    launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
