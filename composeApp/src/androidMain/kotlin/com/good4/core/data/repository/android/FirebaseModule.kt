package com.good4.core.data.repository.android

import com.good4.BuildConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import org.koin.dsl.module

val firebaseModule = module {
    single {
        FirebaseFirestore.setLoggingEnabled(BuildConfig.DEBUG)
        val firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .build()
        firestore.firestoreSettings = settings
        firestore
    }
}
