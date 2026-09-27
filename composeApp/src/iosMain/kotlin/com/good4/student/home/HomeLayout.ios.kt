package com.good4.student.home

import platform.Foundation.NSUserDefaults

actual fun loadHomeLayout(uid: String): String? =
    NSUserDefaults.standardUserDefaults.stringForKey("home_layout_$uid")

actual fun saveHomeLayout(uid: String, value: String) {
    NSUserDefaults.standardUserDefaults.setObject(value, "home_layout_$uid")
}
