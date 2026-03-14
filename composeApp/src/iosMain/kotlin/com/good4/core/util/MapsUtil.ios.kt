package com.good4.core.util

import platform.Foundation.NSURL
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleCancel
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleActionSheet
import platform.UIKit.UIApplication

private fun String.encodeToUrlQuery(): String = buildString {
    for (c in this@encodeToUrlQuery) {
        when {
            c.isLetterOrDigit() || c in "-_.~" -> append(c)
            else -> c.toString().encodeToByteArray().forEach { b ->
                append('%')
                append((b.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0'))
            }
        }
    }
}

actual fun openMaps(address: String) {
    val encodedAddress = address.encodeToUrlQuery()

    val appleMapsUrl = NSURL.URLWithString("maps://?q=$encodedAddress")
    val googleMapsUrl = NSURL.URLWithString("comgooglemaps://?q=$encodedAddress")
    val browserUrl = NSURL.URLWithString("https://maps.google.com/?q=$encodedAddress")

    val hasAppleMaps = appleMapsUrl != null && UIApplication.sharedApplication.canOpenURL(appleMapsUrl)
    val hasGoogleMaps = googleMapsUrl != null && UIApplication.sharedApplication.canOpenURL(googleMapsUrl)

    val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        ?: return

    if (!hasAppleMaps && !hasGoogleMaps) {
        browserUrl?.let { UIApplication.sharedApplication.openURL(it) }
        return
    }

    if (hasAppleMaps && !hasGoogleMaps) {
        UIApplication.sharedApplication.openURL(appleMapsUrl!!)
        return
    }

    if (!hasAppleMaps && hasGoogleMaps) {
        UIApplication.sharedApplication.openURL(googleMapsUrl!!)
        return
    }

    val sheet = UIAlertController.alertControllerWithTitle(
        title = null,
        message = address,
        preferredStyle = UIAlertControllerStyleActionSheet
    )

    sheet.addAction(
        UIAlertAction.actionWithTitle("Apple Maps", style = UIAlertActionStyleDefault) {
            UIApplication.sharedApplication.openURL(appleMapsUrl!!)
        }
    )

    sheet.addAction(
        UIAlertAction.actionWithTitle("Google Maps", style = UIAlertActionStyleDefault) {
            UIApplication.sharedApplication.openURL(googleMapsUrl!!)
        }
    )

    sheet.addAction(
        UIAlertAction.actionWithTitle("İptal", style = UIAlertActionStyleCancel, handler = null)
    )

    rootViewController.presentViewController(sheet, animated = true, completion = null)
}
