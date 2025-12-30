package com.good4

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.good4.di.commonModule
import org.koin.core.context.startKoin

fun main() = application {
    startKoin {
        modules(commonModule)
    }
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "Good4",
    ) {
        App()
    }
}

