//
//  iosAppApp.swift
//  iosApp
//
//  Created by Neslişah Çelek on 2.02.2026.
//

import SwiftUI
import FirebaseCore

@main
struct IOSApp: App {
    init() {
        FirebaseApp.configure()
    }
    
    var body: some Scene {
        WindowGroup {
            ComposeAppView()
                .ignoresSafeArea(.all, edges: .all)
        }
    }
}
