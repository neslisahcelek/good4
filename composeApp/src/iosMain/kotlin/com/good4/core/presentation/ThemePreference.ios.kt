package com.good4.core.presentation

import platform.Foundation.NSUserDefaults

private const val DARK_MODE_KEY = "dark_mode_enabled"

actual fun loadDarkModePreference(): Boolean =
    NSUserDefaults.standardUserDefaults.boolForKey(DARK_MODE_KEY)

actual fun saveDarkModePreference(enabled: Boolean) {
    NSUserDefaults.standardUserDefaults.setBool(enabled, DARK_MODE_KEY)
}
