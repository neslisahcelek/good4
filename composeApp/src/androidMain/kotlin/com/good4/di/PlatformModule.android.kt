package com.good4.di

import com.good4.auth.data.repository.AuthRepository
import com.good4.auth.data.repository.FirebaseAuthRepository
import com.good4.core.data.local.StartupSessionCache
import com.good4.core.data.local.StartupSessionCacheAndroid
import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.data.repository.ProductImageUploadRepository
import com.good4.core.data.repository.ProductImageUploadRepositoryAndroid
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
    single<ProductImageUploadRepository> { ProductImageUploadRepositoryAndroid() }
    single<SupporterCartStorage> { SupporterCartStorageAndroid(get()) }
    single<StartupSessionCache> { StartupSessionCacheAndroid(get()) }
}
