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
        return ComposeContainerViewController(
            childController: controller,
            onFirstAppear: {
                context.coordinator.notifyReady()
            }
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}

    final class Coordinator {
        private var didNotifyReady = false
        private let onComposeReady: (() -> Void)?

        init(onComposeReady: (() -> Void)?) {
            self.onComposeReady = onComposeReady
        }

        func notifyReady() {
            guard !didNotifyReady else { return }
            didNotifyReady = true
            onComposeReady?()
        }
    }
}

final class ComposeContainerViewController: UIViewController {
    private let childController: UIViewController
    private let onFirstAppear: () -> Void
    private var didCallReady = false

    init(
        childController: UIViewController,
        onFirstAppear: @escaping () -> Void
    ) {
        self.childController = childController
        self.onFirstAppear = onFirstAppear
        super.init(nibName: nil, bundle: nil)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        addChild(childController)
        view.addSubview(childController.view)
        childController.view.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            childController.view.topAnchor.constraint(equalTo: view.topAnchor),
            childController.view.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            childController.view.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            childController.view.trailingAnchor.constraint(equalTo: view.trailingAnchor)
        ])
        childController.didMove(toParent: self)
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        guard !didCallReady else { return }
        didCallReady = true
        onFirstAppear()
    }
}
