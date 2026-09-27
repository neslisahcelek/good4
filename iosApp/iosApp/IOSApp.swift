import SwiftUI
import FirebaseCore
import FirebaseFirestore
import FirebaseAuth
import GoogleSignIn
import ComposeApp
import VisionKit
import Vision
import AVFoundation
import AuthenticationServices
import CryptoKit
import Security

@main
struct IOSApp: App {
    @State private var isComposeReady = false

    init() {
        #if DEBUG
        FirebaseConfiguration.shared.setLoggerLevel(.debug)
        #endif
        FirebaseApp.configure()
        GoogleSignInBridge.shared.launcher = NativeGoogleSignInLauncher()
        let appleLauncher = NativeAppleSignInLauncher()
        AppleSignInBridge.shared.launcher = appleLauncher
        AppleTokenRevocationBridge.shared.launcher = appleLauncher
        EventScannerBridge.shared.launcher = NativeEventScannerLauncher()
        #if DEBUG
        Firestore.enableLogging(true)
        #endif
    }

    var body: some Scene {
        WindowGroup {
            ZStack {
                ComposeAppView(
                    onComposeReady: {
                        withAnimation(.easeOut(duration: 0.2)) {
                            isComposeReady = true
                        }
                    }
                )
                .ignoresSafeArea(.all, edges: .all)

                if !isComposeReady {
                    NativeLaunchPlaceholderView()
                        .transition(.opacity)
                }
            }
            .onOpenURL { url in GIDSignIn.sharedInstance.handle(url) }
        }
    }
}

private final class NativeGoogleSignInLauncher: NSObject, GoogleSignInLauncher {
    func launch(completion_: GoogleSignInCallback) {
        let completion = completion_
        guard let clientID = FirebaseApp.app()?.options.clientID,
              let scene = UIApplication.shared.connectedScenes.first(where: { $0.activationState == .foregroundActive }) as? UIWindowScene,
              var presenter = scene.windows.first(where: { $0.isKeyWindow })?.rootViewController else {
            completion.complete(token: nil, accessToken: nil, error: "Google ile giriş henüz yapılandırılmadı.")
            return
        }
        let reversedID = clientID.components(separatedBy: ".").reversed().joined(separator: ".")
        let urlTypes = Bundle.main.object(forInfoDictionaryKey: "CFBundleURLTypes") as? [[String: Any]] ?? []
        guard urlTypes.contains(where: { ($0["CFBundleURLSchemes"] as? [String])?.contains(reversedID) == true }) else {
            completion.complete(token: nil, accessToken: nil, error: "Google ile giriş henüz yapılandırılmadı.")
            return
        }
        while let presented = presenter.presentedViewController { presenter = presented }
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
        GIDSignIn.sharedInstance.signIn(withPresenting: presenter) { result, error in
            DispatchQueue.main.async {
                if let token = result?.user.idToken?.tokenString { completion.complete(token: token, accessToken: result?.user.accessToken.tokenString, error: nil) }
                else if let signInError = error as NSError?,
                        signInError.domain == kGIDSignInErrorDomain,
                        signInError.code == GIDSignInError.canceled.rawValue { completion.complete(token: nil, accessToken: nil, error: nil) }
                else { completion.complete(token: nil, accessToken: nil, error: "Google ile giriş tamamlanamadı. Tekrar deneyin.") }
            }
        }
    }
}

private final class NativeAppleSignInLauncher: NSObject, AppleSignInLauncher, AppleTokenRevocationLauncher,
    ASAuthorizationControllerDelegate, ASAuthorizationControllerPresentationContextProviding {
    private var completion: AppleSignInCallback?
    private var revocationCompletion: AppleTokenRevocationCallback?
    private var rawNonce: String?

    func launch(completion: AppleSignInCallback) {
        guard self.completion == nil && revocationCompletion == nil else {
            completion.complete(idToken: nil, rawNonce: nil, error: "Apple işlemi zaten sürüyor.")
            return
        }
        guard let nonce = Self.makeNonce() else {
            completion.complete(idToken: nil, rawNonce: nil, error: "Apple ile giriş başlatılamadı. Tekrar deneyin.")
            return
        }

        self.completion = completion
        rawNonce = nonce

        performAuthorization(nonce: nonce)
    }

    func revokeIfNeeded(completion: AppleTokenRevocationCallback) {
        guard let user = Auth.auth().currentUser else {
            completion.complete(error: "Oturum sona erdi. Lütfen yeniden giriş yapın.")
            return
        }
        guard user.providerData.contains(where: { $0.providerID == "apple.com" }) else {
            completion.complete(error: nil)
            return
        }
        guard self.completion == nil && revocationCompletion == nil else {
            completion.complete(error: "Apple işlemi zaten sürüyor.")
            return
        }
        guard let nonce = Self.makeNonce() else {
            completion.complete(error: "Apple hesap silme doğrulaması başlatılamadı.")
            return
        }

        revocationCompletion = completion
        rawNonce = nonce
        performAuthorization(nonce: nonce)
    }

    private func performAuthorization(nonce: String) {
        let request = ASAuthorizationAppleIDProvider().createRequest()
        request.requestedScopes = [.fullName, .email]
        request.nonce = Self.sha256(nonce)

        let controller = ASAuthorizationController(authorizationRequests: [request])
        controller.delegate = self
        controller.presentationContextProvider = self
        controller.performRequests()
    }

    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        let scene = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }
        return scene?.windows.first(where: { $0.isKeyWindow }) ?? ASPresentationAnchor()
    }

    func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization authorization: ASAuthorization
    ) {
        if revocationCompletion != nil {
            guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
                  let codeData = credential.authorizationCode,
                  let code = String(data: codeData, encoding: .utf8) else {
                finishRevocation(error: "Apple yetki kodu alınamadı. Hesap silinmedi.")
                return
            }
            Task {
                do {
                    try await Auth.auth().revokeToken(withAuthorizationCode: code)
                    finishRevocation(error: nil)
                } catch {
                    finishRevocation(error: "Apple bağlantısı kaldırılamadı. Hesap silinmedi; lütfen yeniden deneyin.")
                }
            }
            return
        }
        guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
              let tokenData = credential.identityToken,
              let idToken = String(data: tokenData, encoding: .utf8),
              let nonce = rawNonce else {
            finish(idToken: nil, rawNonce: nil, error: "Apple kimliği doğrulanamadı. Tekrar deneyin.")
            return
        }
        finish(idToken: idToken, rawNonce: nonce, error: nil)
    }

    func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError error: any Swift.Error
    ) {
        let nsError = error as NSError
        if revocationCompletion != nil {
            let message = nsError.domain == ASAuthorizationError.errorDomain &&
                nsError.code == ASAuthorizationError.canceled.rawValue
                ? "Apple doğrulaması iptal edildi. Hesap silinmedi."
                : "Apple doğrulaması tamamlanamadı. Hesap silinmedi."
            finishRevocation(error: message)
            return
        }
        if nsError.domain == ASAuthorizationError.errorDomain,
           nsError.code == ASAuthorizationError.canceled.rawValue {
            finish(idToken: nil, rawNonce: nil, error: nil)
        } else {
            finish(idToken: nil, rawNonce: nil, error: "Apple ile giriş tamamlanamadı. Tekrar deneyin.")
        }
    }

    private func finish(idToken: String?, rawNonce: String?, error: String?) {
        let callback = completion
        completion = nil
        self.rawNonce = nil
        callback?.complete(idToken: idToken, rawNonce: rawNonce, error: error)
    }

    private func finishRevocation(error: String?) {
        let callback = revocationCompletion
        revocationCompletion = nil
        rawNonce = nil
        callback?.complete(error: error)
    }

    private static func sha256(_ value: String) -> String {
        SHA256.hash(data: Data(value.utf8)).map { String(format: "%02x", $0) }.joined()
    }

    private static func makeNonce(length: Int = 32) -> String? {
        precondition(length > 0)
        let characters = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._")
        var result = ""
        var bytes = [UInt8](repeating: 0, count: 16)

        while result.count < length {
            let byteCount = bytes.count
            let status = bytes.withUnsafeMutableBytes {
                SecRandomCopyBytes(kSecRandomDefault, byteCount, $0.baseAddress!)
            }
            guard status == errSecSuccess else { return nil }
            for byte in bytes where result.count < length && byte < characters.count {
                result.append(characters[Int(byte)])
            }
        }
        return result
    }
}

private final class NativeEventScannerLauncher: NSObject, EventScannerLauncher {
    func launch(completion__: EventScannerCallback) {
        let completion = completion__
        guard DataScannerViewController.isSupported else {
            completion.complete(value: nil, error: "Bu cihazda kamera taraması desteklenmiyor. Simülatörde manuel giriş kullanabilirsiniz.")
            return
        }
        AVCaptureDevice.requestAccess(for: .video) { allowed in
            DispatchQueue.main.async {
                guard allowed, DataScannerViewController.isAvailable else {
                    completion.complete(value: nil, error: "Kamera izni gerekli. Ayarlar’dan kamera iznini açabilir veya manuel giriş kullanabilirsiniz.")
                    return
                }
                guard let scene = UIApplication.shared.connectedScenes.first(where: { $0.activationState == .foregroundActive }) as? UIWindowScene,
                      var presenter = scene.windows.first(where: { $0.isKeyWindow })?.rootViewController else {
                    completion.complete(value: nil, error: "Kamera açılamadı.")
                    return
                }
                while let presented = presenter.presentedViewController { presenter = presented }
                let controller = EventScannerController(completion: completion)
                let navigation = UINavigationController(rootViewController: controller)
                navigation.modalPresentationStyle = .fullScreen
                presenter.present(navigation, animated: true)
            }
        }
    }
}

private final class EventScannerController: UIViewController, DataScannerViewControllerDelegate {
    private let scanner = DataScannerViewController(recognizedDataTypes: [.barcode(symbologies: [.qr])],
        qualityLevel: .balanced, recognizesMultipleItems: false, isGuidanceEnabled: true, isHighlightingEnabled: true)
    private var completion: EventScannerCallback?
    init(completion: EventScannerCallback) { self.completion = completion; super.init(nibName: nil, bundle: nil) }
    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }
    override func viewDidLoad() {
        super.viewDidLoad()
        title = "Öğrencinin QR biletini okut"
        navigationItem.leftBarButtonItem = UIBarButtonItem(title: "Kapat", style: .plain, target: self, action: #selector(cancel))
        addChild(scanner)
        view.addSubview(scanner.view)
        scanner.view.frame = view.bounds
        scanner.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        scanner.didMove(toParent: self)
        scanner.delegate = self
    }
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        do { try scanner.startScanning() }
        catch { finish(value: nil, error: "Kamera başlatılamadı. Manuel giriş kullanabilirsiniz.") }
    }
    @objc private func cancel() { finish(value: nil, error: nil) }
    private func finish(value: String?, error: String?) {
        guard let callback = completion else { return }
        completion = nil
        scanner.stopScanning()
        dismiss(animated: true) { callback.complete(value: value, error: error) }
    }
    func dataScanner(_ dataScanner: DataScannerViewController, didAdd addedItems: [RecognizedItem], allItems: [RecognizedItem]) {
        for item in addedItems {
            if case .barcode(let barcode) = item, let value = barcode.payloadStringValue {
                finish(value: value, error: nil)
                break
            }
        }
    }
    func dataScanner(_ dataScanner: DataScannerViewController, becameUnavailableWithError error: DataScannerViewController.ScanningUnavailable) {
        finish(value: nil, error: "Kamera kullanılamıyor. Manuel giriş kullanabilirsiniz.")
    }
}

private struct NativeLaunchPlaceholderView: View {
    var body: some View {
        ZStack {
            Color(red: 248.0 / 255.0, green: 247.0 / 255.0, blue: 244.0 / 255.0)
                .ignoresSafeArea()

            VStack(spacing: 16) {
                Image("SplashLogo")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 120, height: 120)
                ProgressView()
                    .progressViewStyle(.circular)
                    .tint(Color.black)
            }
        }
    }
}
