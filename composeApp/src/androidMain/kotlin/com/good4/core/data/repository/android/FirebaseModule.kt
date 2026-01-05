package com.good4.core.data.repository.android

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import org.koin.dsl.module

val firebaseModule = module {
    single { 
        val firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .build()
        firestore.firestoreSettings = settings
        firestore
    }
}
