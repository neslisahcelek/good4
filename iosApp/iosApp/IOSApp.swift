import SwiftUI
import FirebaseCore
import FirebaseFirestore
import FirebaseCrashlytics
import GoogleSignIn
import ComposeApp

@main
struct IOSApp: App {
    @State private var isComposeReady = false

    init() {
        #if DEBUG
        FirebaseConfiguration.shared.setLoggerLevel(.debug)
        #endif
        FirebaseApp.configure()
        GoogleSignInBridge.shared.launcher = NativeGoogleSignInLauncher()
        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(true)
        Crashlytics.crashlytics().setCustomValue("ios", forKey: "platform")
        Crashlytics.crashlytics().log("IOSApp initialized")
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
    func launch(completion: GoogleSignInCallback) {
        guard let clientID = FirebaseApp.app()?.options.clientID,
              let scene = UIApplication.shared.connectedScenes.first(where: { $0.activationState == .foregroundActive }) as? UIWindowScene,
              var presenter = scene.windows.first(where: { $0.isKeyWindow })?.rootViewController else {
            completion.complete(token: nil, error: "Google ile giriş henüz yapılandırılmadı.")
            return
        }
        let reversedID = clientID.components(separatedBy: ".").reversed().joined(separator: ".")
        let urlTypes = Bundle.main.object(forInfoDictionaryKey: "CFBundleURLTypes") as? [[String: Any]] ?? []
        guard urlTypes.contains(where: { ($0["CFBundleURLSchemes"] as? [String])?.contains(reversedID) == true }) else {
            completion.complete(token: nil, error: "Google ile giriş henüz yapılandırılmadı.")
            return
        }
        while let presented = presenter.presentedViewController { presenter = presented }
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
        GIDSignIn.sharedInstance.signIn(withPresenting: presenter) { result, error in
            DispatchQueue.main.async {
                if let token = result?.user.idToken?.tokenString { completion.complete(token: token, error: nil) }
                else if (error as NSError?)?.code == GIDSignInErrorCode.canceled.rawValue { completion.complete(token: nil, error: nil) }
                else { completion.complete(token: nil, error: "Google ile giriş tamamlanamadı. Tekrar deneyin.") }
            }
        }
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
