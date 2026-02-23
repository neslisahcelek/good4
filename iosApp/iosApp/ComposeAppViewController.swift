//
//  ComposeAppViewController.swift
//  iosApp
//
//  Created by Neslişah Çelek on 2.02.2026.
//
import SwiftUI
import ComposeApp

struct ComposeAppView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let controller = MainViewControllerKt.MainViewController()
        controller.view.insetsLayoutMarginsFromSafeArea = false
        return controller
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
