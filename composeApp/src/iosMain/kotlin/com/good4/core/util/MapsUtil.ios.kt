package com.good4.core.util

import platform.Foundation.NSURL
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleCancel
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleActionSheet
import platform.UIKit.UIApplication

private fun openUrl(url: NSURL) {
    UIApplication.sharedApplication.openURL(
        url = url,
        options = emptyMap<Any?, Any>(),
        completionHandler = null
    )
}

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
    val normalized = address.trim()

    if (isDirectMapUrl(normalized)) {
        NSURL.URLWithString(normalized)?.let { directUrl ->
            if (UIApplication.sharedApplication.canOpenURL(directUrl)) {
                openUrl(directUrl)
                return
            }
        }
    }

    val encodedAddress = normalized.encodeToUrlQuery()

    val appleMapsUrl = NSURL.URLWithString("maps://?q=$encodedAddress")
    val googleMapsUrl = NSURL.URLWithString("comgooglemaps://?q=$encodedAddress")
    val browserUrl = NSURL.URLWithString("https://maps.google.com/?q=$encodedAddress")

    val appleMapsOpenUrl = appleMapsUrl?.takeIf { UIApplication.sharedApplication.canOpenURL(it) }
    val googleMapsOpenUrl = googleMapsUrl?.takeIf { UIApplication.sharedApplication.canOpenURL(it) }

    val hasAppleMaps = appleMapsOpenUrl != null
    val hasGoogleMaps = googleMapsOpenUrl != null

    val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        ?: return

    if (!hasAppleMaps && !hasGoogleMaps) {
        browserUrl?.let { openUrl(it) }
        return
    }

    if (hasAppleMaps && !hasGoogleMaps) {
        appleMapsOpenUrl?.let { openUrl(it) }
        return
    }

    if (!hasAppleMaps && hasGoogleMaps) {
        googleMapsOpenUrl?.let { openUrl(it) }
        return
    }

    val sheet = UIAlertController.alertControllerWithTitle(
        title = null,
        message = normalized,
        preferredStyle = UIAlertControllerStyleActionSheet
    )

    sheet.addAction(
        UIAlertAction.actionWithTitle("Apple Maps", style = UIAlertActionStyleDefault) {
            appleMapsOpenUrl?.let { openUrl(it) }
        }
    )

    sheet.addAction(
        UIAlertAction.actionWithTitle("Google Maps", style = UIAlertActionStyleDefault) {
            googleMapsOpenUrl?.let { openUrl(it) }
        }
    )

    sheet.addAction(
        UIAlertAction.actionWithTitle("İptal", style = UIAlertActionStyleCancel, handler = null)
    )

    rootViewController.presentViewController(sheet, animated = true, completion = null)
}
