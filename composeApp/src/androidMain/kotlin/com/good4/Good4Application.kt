package com.good4

import android.app.Application
import android.content.pm.ApplicationInfo
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.good4.core.data.repository.android.firebaseModule
import com.good4.core.data.repository.android.firestoreModule
import com.good4.di.commonModule
import com.good4.di.platformModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class Good4Application : Application() {
    override fun onCreate() {
        super.onCreate()

        val isDebuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (shouldUseFirebaseEmulators(isDebuggable, BuildConfig.USE_FIREBASE_EMULATORS)) {
            // Replace the SDK's auto-created default app before Auth/Firestore are initialized.
            FirebaseApp.getApps(this).forEach { it.delete() }
            val options = com.google.firebase.FirebaseOptions.Builder().setProjectId("demo-good4-v2")
                .setApplicationId("1:123456789:android:demo").setApiKey("demo-api-key").setStorageBucket("demo-good4-v2.appspot.com").build()
            FirebaseApp.initializeApp(this, options)
        } else FirebaseApp.initializeApp(this)
        FirebaseAppCheck.getInstance().apply {
            if (BuildConfig.DEBUG) {
                val debugFactory = runCatching {
                    val clazz = Class.forName("com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory")
                    val getInstance = clazz.getMethod("getInstance")
                    getInstance.invoke(null) as AppCheckProviderFactory
                }.getOrNull()
                installAppCheckProviderFactory(
                    debugFactory ?: PlayIntegrityAppCheckProviderFactory.getInstance()
                )
            } else {
                installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
            }
        }
        if (shouldUseFirebaseEmulators(isDebuggable, BuildConfig.USE_FIREBASE_EMULATORS)) {
            val host = BuildConfig.FIREBASE_EMULATOR_HOST
            com.google.firebase.auth.FirebaseAuth.getInstance().useEmulator(host, 9199)
            com.google.firebase.firestore.FirebaseFirestore.getInstance().useEmulator(host, 8285)
            com.google.firebase.storage.FirebaseStorage.getInstance().useEmulator(host, 9295)
            com.google.firebase.messaging.FirebaseMessaging.getInstance().isAutoInitEnabled = false
        }
        com.good4.notification.AndroidCampusPush.initialize(this)

        startKoin {
            androidContext(this@Good4Application)
            modules(commonModule, platformModule, firebaseModule, firestoreModule)
        }
    }

    private fun shouldUseFirebaseEmulators(
        isDebuggable: Boolean,
        emulatorsEnabled: Boolean
    ): Boolean = isDebuggable && emulatorsEnabled
}
