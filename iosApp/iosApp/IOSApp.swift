import SwiftUI
import FirebaseCore
import FirebaseFirestore
import FirebaseCrashlytics

@main
struct IOSApp: App {
    @State private var isComposeReady = false

    init() {
        #if DEBUG
        FirebaseConfiguration.shared.setLoggerLevel(.debug)
        #endif
        FirebaseApp.configure()
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
