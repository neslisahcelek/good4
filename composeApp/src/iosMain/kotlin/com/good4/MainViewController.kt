package com.good4

import androidx.compose.ui.window.ComposeUIViewController
import com.good4.di.commonModule
import com.good4.di.platformModule
import org.koin.core.context.startKoin
import platform.UIKit.UIViewController
import platform.UIKit.UIRectEdgeAll

private var koinInitialized = false

fun MainViewController(): UIViewController {
    val controller = ComposeUIViewController(
        configure = { 
            enforceStrictPlistSanityCheck = false
        }
    ) {
        if (!koinInitialized) {
            startKoin {
                modules(commonModule, platformModule)
            }
            koinInitialized = true
        }
        
        App() 
    }
    
    controller.edgesForExtendedLayout = UIRectEdgeAll
    controller.extendedLayoutIncludesOpaqueBars = true
    
    return controller
}

