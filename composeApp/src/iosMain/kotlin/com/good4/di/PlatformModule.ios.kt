package com.good4.di

import com.good4.auth.data.repository.AuthRepository
import com.good4.auth.data.repository.FirebaseAuthRepositoryIOS
import com.good4.core.data.local.StartupSessionCache
import com.good4.core.data.local.StartupSessionCacheIOS
import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.data.repository.FirestoreRepositoryIOSImpl
import com.good4.core.data.repository.ProductImageUploadRepository
import com.good4.core.data.repository.ProductImageUploadRepositoryIOS
import com.good4.supporter.data.local.SupporterCartStorage
import com.good4.supporter.data.local.SupporterCartStorageIOS
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<AuthRepository> { FirebaseAuthRepositoryIOS() }
    single<FirestoreRepository> { FirestoreRepositoryIOSImpl() }
    single<ProductImageUploadRepository> { ProductImageUploadRepositoryIOS() }
    single<SupporterCartStorage> { SupporterCartStorageIOS() }
    single<StartupSessionCache> { StartupSessionCacheIOS() }
}
