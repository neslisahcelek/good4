package com.good4

import androidx.compose.ui.window.ComposeUIViewController
import com.good4.di.commonModule
import com.good4.di.platformModule
import org.koin.core.context.startKoin
import platform.UIKit.UIRectEdgeAll
import platform.UIKit.UIViewController

private var koinInitialized = false

fun MainViewController(): UIViewController {
    if (!koinInitialized) {
        startKoin {
            modules(commonModule, platformModule)
        }
        koinInitialized = true
    }

    val controller = ComposeUIViewController(
        configure = {
            enforceStrictPlistSanityCheck = false
        }
    ) {
        App()
    }

    controller.edgesForExtendedLayout = UIRectEdgeAll
    controller.extendedLayoutIncludesOpaqueBars = true

    return controller
}

