package com.good4.core.presentation

import android.content.Context
import org.koin.core.context.GlobalContext

private const val PREFERENCES_NAME = "good4_preferences"
private const val DARK_MODE_KEY = "dark_mode_enabled"

actual fun loadDarkModePreference(): Boolean = runCatching {
    GlobalContext.get().get<Context>()
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        .getBoolean(DARK_MODE_KEY, false)
}.getOrDefault(false)

actual fun saveDarkModePreference(enabled: Boolean) {
    runCatching {
        GlobalContext.get().get<Context>()
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(DARK_MODE_KEY, enabled)
            .apply()
    }
}
