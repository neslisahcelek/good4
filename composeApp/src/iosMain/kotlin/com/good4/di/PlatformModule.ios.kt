package com.good4.di

import com.good4.auth.data.repository.AuthRepository
import com.good4.auth.data.repository.FirebaseAuthRepositoryIOS
import com.good4.core.data.local.StartupSessionCache
import com.good4.core.data.local.StartupSessionCacheIOS
import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.data.repository.FirestoreRepositoryIOSImpl
import com.good4.core.data.repository.ProductImageUploadRepository
import com.good4.core.data.repository.ProductImageUploadRepositoryIOS
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<com.good4.analytics.AnalyticsTracker>(createdAtStart = true) {
        com.good4.analytics.DeviceAnalyticsTracker(
            com.good4.analytics.IOSAnalyticsConsentStorage(),
            com.good4.analytics.IOSAnalyticsTransport(),
            com.good4.analytics.analyticsCollectionAllowed()
        )
    }
    single<AuthRepository> { FirebaseAuthRepositoryIOS() }
    single<FirestoreRepository> { FirestoreRepositoryIOSImpl() }
    single<ProductImageUploadRepository> { ProductImageUploadRepositoryIOS() }
    single<StartupSessionCache> { StartupSessionCacheIOS() }
}
