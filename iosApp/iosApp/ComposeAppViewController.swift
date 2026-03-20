import SwiftUI
import ComposeApp

struct ComposeAppView: UIViewControllerRepresentable {
    var onComposeReady: (() -> Void)? = nil

    func makeCoordinator() -> Coordinator {
        Coordinator(onComposeReady: onComposeReady)
    }

    func makeUIViewController(context: Context) -> UIViewController {
        let controller = MainViewControllerKt.MainViewController()
        controller.view.insetsLayoutMarginsFromSafeArea = false

        DispatchQueue.main.async {
            context.coordinator.notifyReady()
        }

        return controller
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }

    final class Coordinator {
        private var didNotifyReady = false
        private let onComposeReady: (() -> Void)?

        init(onComposeReady: (() -> Void)?) {
            self.onComposeReady = onComposeReady
        }

        func notifyReady() {
            guard !didNotifyReady else {
                return
            }
            didNotifyReady = true
            onComposeReady?()
        }
    }
}
