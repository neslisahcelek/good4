package com.good4.di

import com.good4.auth.data.repository.AuthRepository
import com.good4.auth.data.repository.FirebaseAuthRepository
import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.data.repository.android.FirestoreRepositoryAndroidImpl
import com.good4.supporter.data.local.SupporterCartStorage
import com.good4.supporter.data.local.SupporterCartStorageAndroid
import com.google.firebase.firestore.FirebaseFirestore
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<AuthRepository> { FirebaseAuthRepository() }
    single { FirebaseFirestore.getInstance() }
    single<FirestoreRepository> { FirestoreRepositoryAndroidImpl(get()) }
    single<SupporterCartStorage> { SupporterCartStorageAndroid(get()) }
}
