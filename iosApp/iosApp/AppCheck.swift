import Foundation
import FirebaseCore
import FirebaseAppCheck
import FirebaseAuth
import FirebaseFirestore
import FirebaseStorage
import ComposeApp

enum Good4AppCheck {
    static func configureFirebaseIfNeeded() {
        if FirebaseApp.app() == nil {
            // The factory must be installed before any Firebase service is initialized.
            #if DEBUG || targetEnvironment(simulator)
            AppCheck.setAppCheckProviderFactory(AppCheckDebugProviderFactory())
            #else
            AppCheck.setAppCheckProviderFactory(Good4AppAttestProviderFactory())
            #endif
            #if DEBUG
            if Bundle.main.bundleIdentifier == "com.good4.iosApp.test" {
                let options = FirebaseOptions(googleAppID: "1:123456789:ios:demo", gcmSenderID: "123456789")
                options.apiKey = "demo-api-key"
                options.projectID = "demo-good4-v2"
                options.storageBucket = "demo-good4-v2.appspot.com"
                FirebaseApp.configure(options: options)
                Auth.auth().useEmulator(withHost: "127.0.0.1", port: 9199)
                Firestore.firestore().useEmulator(withHost: "127.0.0.1", port: 8285)
                Storage.storage().useEmulator(withHost: "127.0.0.1", port: 9295)
            } else { FirebaseApp.configure() }
            #else
            FirebaseApp.configure()
            #endif
            AppCheck.appCheck().isTokenAutoRefreshEnabled = true
        }
        NativeAppCheckBridge.shared.provider = Good4AppCheckTokenProvider()
    }
}

private final class Good4AppAttestProviderFactory: NSObject, AppCheckProviderFactory {
    nonisolated func createProvider(with app: FirebaseApp) -> AppCheckProvider? {
        AppAttestProvider(app: app)
    }
}

private final class Good4AppCheckTokenProvider: NSObject, NativeAppCheckTokenProvider {
    func fetchToken(callback: NativeAppCheckTokenCallback) {
        // Firebase reuses a cached token and refreshes it when necessary.
        AppCheck.appCheck().token(forcingRefresh: false) { token, error in
            if let error = error as NSError? {
                // Report configuration/attestation failures without logging token contents.
                NSLog("[App Check] Token request failed (%@, %ld).", error.domain, error.code)
            }
            DispatchQueue.main.async {
                callback.complete(token: error == nil ? token?.token : nil)
            }
        }
    }
}
