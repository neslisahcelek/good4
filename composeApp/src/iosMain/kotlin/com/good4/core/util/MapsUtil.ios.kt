package com.good4.core.util

import platform.Foundation.NSCharacterSet
import platform.Foundation.NSURL
import platform.Foundation.URLQueryAllowedCharacterSet
import platform.Foundation.addingPercentEncodingWithAllowedCharacters
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleCancel
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleActionSheet
import platform.UIKit.UIApplication

actual fun openMaps(address: String) {
    val encodedAddress = address.addingPercentEncodingWithAllowedCharacters(
        NSCharacterSet.URLQueryAllowedCharacterSet
    ) ?: address

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
