package com.good4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.google.firebase.FirebaseApp
import com.good4.core.data.repository.android.firebaseModule
import com.good4.core.data.repository.android.firestoreModule
import com.good4.di.commonModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Firestore
        try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            firestore.firestoreSettings = settings
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Initialize Koin
        startKoin {
            androidContext(this@MainActivity)
            modules(commonModule, firebaseModule, firestoreModule)
        }
        
        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}

