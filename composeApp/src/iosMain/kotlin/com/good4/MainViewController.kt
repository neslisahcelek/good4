package com.good4

import androidx.compose.ui.window.ComposeUIViewController
import com.good4.core.presentation.Good4Theme
import com.good4.core.util.AppEnvironment
import com.good4.quiz.presentation.QuizUiPreview
import platform.Foundation.NSProcessInfo
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

    // Debug-only design preview: `-quizPreview <stage>` shows the live quiz with sample data.
    val arguments = NSProcessInfo.processInfo.arguments.map { it.toString() }
    val quizPreviewStage = arguments.indexOf("-quizPreview")
        .takeIf { it >= 0 && AppEnvironment.isDebug }
        ?.let { arguments.getOrNull(it + 1) ?: "question4" }
    // Debug-only: `-campusEventsShowcase <full|empty>` shows the events box with sample data.
    val campusEventsShowcase = arguments.indexOf("-campusEventsShowcase")
        .takeIf { it >= 0 && AppEnvironment.isDebug }
        ?.let { arguments.getOrNull(it + 1) ?: "full" }

    val controller = ComposeUIViewController(
        configure = {
            enforceStrictPlistSanityCheck = false
        }
    ) {
        if (quizPreviewStage != null) {
            Good4Theme { QuizUiPreview(quizPreviewStage) }
        } else if (campusEventsShowcase != null) {
            Good4Theme { com.good4.campusevents.CampusEventsShowcase(campusEventsShowcase) }
        } else {
            App()
        }
    }

    controller.edgesForExtendedLayout = UIRectEdgeAll
    controller.extendedLayoutIncludesOpaqueBars = true

    return controller
}

