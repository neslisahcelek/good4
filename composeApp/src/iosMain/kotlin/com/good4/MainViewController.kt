package com.good4

import androidx.compose.ui.window.ComposeUIViewController
import com.good4.di.commonModule
import org.koin.core.context.startKoin

fun MainViewController() = ComposeUIViewController { 

    startKoin {
        modules(commonModule)
    }
    
    App() 
}

