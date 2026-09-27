package com.good4.student.home

import android.content.Context
import org.koin.core.context.GlobalContext

private fun preferences() = GlobalContext.get().get<Context>()
    .getSharedPreferences("good4_preferences", Context.MODE_PRIVATE)

actual fun loadHomeLayout(uid: String): String? = preferences().getString("home_layout_$uid", null)

actual fun saveHomeLayout(uid: String, value: String) {
    preferences().edit().putString("home_layout_$uid", value).apply()
}
