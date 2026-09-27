package com.good4.core.util

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openWalkingDirections(latitude: Double, longitude: Double) {
    val app = UIApplication.sharedApplication
    val googleMapsApp = NSURL.URLWithString("comgooglemaps://?daddr=$latitude,$longitude&directionsmode=walking")
    // The web page avoids Google's app redirect, which fails when the Google Maps app is missing.
    val googleMapsWeb = NSURL.URLWithString("https://maps.google.com/?daddr=$latitude,$longitude&dirflg=w")
    fun openWeb() {
        googleMapsWeb?.let { app.openURL(it, options = emptyMap<Any?, Any>(), completionHandler = null) }
    }
    if (googleMapsApp == null) return openWeb()
    // openURL reports failure when the Google Maps app is not installed; no scheme query permission is needed.
    app.openURL(googleMapsApp, options = emptyMap<Any?, Any>()) { opened -> if (!opened) openWeb() }
}
