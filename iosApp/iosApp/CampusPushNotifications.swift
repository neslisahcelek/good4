import UIKit
import UserNotifications
import FirebaseAuth
import FirebaseMessaging
import ComposeApp

final class CampusPushAppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        Messaging.messaging().delegate = self
        return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        // Swizzling is disabled: explicitly map Apple's address to the FCM token.
        Messaging.messaging().apnsToken = deviceToken
        NativeCampusPush.shared.fetchToken()
    }

    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Swift.Error) {
        CampusPushNotifications.shared.updateDevice(token: nil, permission: "granted")
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        NativeCampusPush.shared.receiveToken(fcmToken)
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        let data = notification.request.content.userInfo
        let allowed = data["recipientUid"] as? String == Auth.auth().currentUser?.uid
        completionHandler(allowed ? [.banner, .sound] : [])
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse, withCompletionHandler completionHandler: @escaping () -> Void) {
        let data = response.notification.request.content.userInfo
        CampusPushNotifications.shared.receive(
            recipientUid: data["recipientUid"] as? String,
            type: data["type"] as? String,
            targetId: (data["conversationId"] ?? data["listingId"]) as? String
        )
        completionHandler()
    }
}

final class NativeCampusPush: NSObject, NativePushLauncher {
    static let shared = NativeCampusPush()
    private var generation = 0

    func refresh() {
        #if targetEnvironment(simulator)
        CampusPushNotifications.shared.updateDevice(token: nil, permission: "unavailable")
        #else
        let currentGeneration = generation
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            DispatchQueue.main.async {
                guard currentGeneration == self.generation else { return }
                switch settings.authorizationStatus {
                case .authorized, .provisional, .ephemeral:
                    Messaging.messaging().isAutoInitEnabled = true
                    UIApplication.shared.registerForRemoteNotifications()
                    if Messaging.messaging().apnsToken != nil { self.fetchToken() }
                    else { CampusPushNotifications.shared.updateDevice(token: nil, permission: "granted") }
                case .denied:
                    CampusPushNotifications.shared.updateDevice(token: nil, permission: "denied")
                default:
                    CampusPushNotifications.shared.updateDevice(token: nil, permission: "notRequested")
                }
            }
        }
        #endif
    }

    func requestPermission() {
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            if settings.authorizationStatus == .denied {
                DispatchQueue.main.async {
                    if let url = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(url) }
                }
            } else {
                UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound]) { _, _ in
                    DispatchQueue.main.async { self.refresh() }
                }
            }
        }
    }

    func fetchToken() {
        let currentGeneration = generation
        Messaging.messaging().token { token, _ in
            DispatchQueue.main.async {
                guard currentGeneration == self.generation else { return }
                self.receiveToken(token)
            }
        }
    }

    func receiveToken(_ token: String?) {
        guard Messaging.messaging().apnsToken != nil else { return }
        let currentGeneration = generation
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            DispatchQueue.main.async {
                guard currentGeneration == self.generation else { return }
                let granted = settings.authorizationStatus == .authorized || settings.authorizationStatus == .provisional || settings.authorizationStatus == .ephemeral
                CampusPushNotifications.shared.updateDevice(token: granted ? token : nil, permission: granted ? "granted" : "denied")
            }
        }
    }

    func deleteToken(completion: PushTokenDeletedCallback) {
        generation += 1
        Messaging.messaging().isAutoInitEnabled = false
        UIApplication.shared.unregisterForRemoteNotifications()
        Messaging.messaging().deleteToken { _ in
            DispatchQueue.main.async {
                CampusPushNotifications.shared.updateDevice(token: nil, permission: "unknown")
                completion.complete()
            }
        }
    }
}
