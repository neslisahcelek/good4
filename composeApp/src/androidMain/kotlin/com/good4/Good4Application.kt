package com.good4

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
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
        
        FirebaseApp.initializeApp(this)
        FirebaseAppCheck.getInstance().apply {
            if (BuildConfig.DEBUG) {
                installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
            } else {
                installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
            }
        }
        
        startKoin {
            androidContext(this@Good4Application)
            modules(commonModule, platformModule, firebaseModule, firestoreModule)
        }
    }
}
