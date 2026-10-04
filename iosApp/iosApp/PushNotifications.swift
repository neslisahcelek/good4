import UIKit
import UserNotifications
import FirebaseCore
import FirebaseAuth
import FirebaseMessaging
import ComposeApp

final class Good4PushAppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {
    private let launcher = Good4NativePushLauncher()
    private var supported: Bool { Bundle.main.bundleIdentifier == "com.good4.iosApp" }

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        guard supported else { return true }
        Good4AppCheck.configureFirebaseIfNeeded()
        NativePushBridge.shared.launcher = launcher
        UNUserNotificationCenter.current().delegate = self
        Messaging.messaging().delegate = self
        Messaging.messaging().isAutoInitEnabled = true
        application.registerForRemoteNotifications()
        return true
    }
    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        // Explicit mapping is required for SwiftUI's delegate adaptor.
        Messaging.messaging().apnsToken = deviceToken
        NativeCampusPush.shared.fetchToken()
        PushSignals.shared.refresh()
    }
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        NativeCampusPush.shared.receiveToken(fcmToken)
        PushSignals.shared.refresh()
    }
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification,
                                withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        let data = notification.request.content.userInfo
        guard data["recipientUid"] as? String == Auth.auth().currentUser?.uid else { completionHandler([]); return }
        if let id = data["notificationId"] as? String, let uid = data["recipientUid"] as? String {
            PushSignals.shared.received(notificationId: id, recipientUid: uid)
        }
        completionHandler([.banner, .sound, .list])
    }
    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse,
                                withCompletionHandler completionHandler: @escaping () -> Void) {
        let data = response.notification.request.content.userInfo
        if let id = data["notificationId"] as? String, let uid = data["recipientUid"] as? String {
            // Kotlin defers opening until the authenticated navigation graph is ready.
            PushSignals.shared.opened(notificationId: id, recipientUid: uid)
        } else {
            CampusPushNotifications.shared.receive(
                recipientUid: data["recipientUid"] as? String,
                type: data["type"] as? String,
                targetId: (data["conversationId"] ?? data["listingId"]) as? String
            )
        }
        completionHandler()
    }
}

private final class Good4NativePushLauncher: NSObject, NativePushLauncher {
    func snapshot(callback: NativePushCallback) {
        Messaging.messaging().isAutoInitEnabled = true
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            let allowed = [.authorized, .provisional, .ephemeral].contains(settings.authorizationStatus)
            Messaging.messaging().token { token, error in
                DispatchQueue.main.async { callback.complete(token: token, permission: allowed, error: error == nil ? nil : "registration_failed") }
            }
        }
    }
    func requestPermission(callback: NativePushCallback) {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { _, _ in
            DispatchQueue.main.async {
                UIApplication.shared.registerForRemoteNotifications()
                self.snapshot(callback: callback)
            }
        }
    }
    func clearRegistration(callback: NativePushCallback) {
        Messaging.messaging().isAutoInitEnabled = false
        UNUserNotificationCenter.current().removeAllDeliveredNotifications()
        Messaging.messaging().deleteToken { error in
            DispatchQueue.main.async { callback.complete(token: nil, permission: false, error: error == nil ? nil : "unregister_failed") }
        }
    }
    func openSettings() {
        guard let url = URL(string: UIApplication.openNotificationSettingsURLString) else { return }
        UIApplication.shared.open(url)
    }
}
