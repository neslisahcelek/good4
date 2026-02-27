//
//  iosAppApp.swift
//  iosApp
//
//  Created by Neslişah Çelek on 2.02.2026.
//

import SwiftUI
import FirebaseCore
import FirebaseFirestore

@main
struct IOSApp: App {
    init() {
#if DEBUG
        FirebaseConfiguration.shared.setLoggerLevel(.debug)
#endif
        FirebaseApp.configure()
#if DEBUG
        Firestore.enableLogging(true)
#endif
    }
    
    var body: some Scene {
        WindowGroup {
            ComposeAppView()
                .ignoresSafeArea(.all, edges: .all)
        }
    }
}
