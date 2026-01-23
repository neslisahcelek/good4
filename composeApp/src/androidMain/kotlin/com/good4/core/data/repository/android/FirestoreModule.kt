package com.good4.core.data.repository.android

import com.good4.auth.data.repository.AuthRepository
import com.good4.business.data.dto.FirestoreBusinessRepository
import com.good4.code.data.repository.CodeRepository
import com.good4.config.data.repository.AppConfigRepository
import com.good4.core.data.repository.FirestoreRepository
import com.good4.product.data.repository.FirestoreProductRepository
import com.good4.product.presentation.product_list.ProductListViewModel
import com.good4.user.data.repository.UserRepository
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val firestoreModule = module {
    single<FirestoreRepository> { FirestoreRepositoryAndroidImpl(get()) }
    single { FirestoreBusinessRepository(get<FirestoreRepository>()) }
    single {
        FirestoreProductRepository(
            get<FirestoreRepository>(),
            get<FirestoreBusinessRepository>()
        )
    }
    single {
        CodeRepository(
            get<FirestoreRepository>(),
            get<FirestoreBusinessRepository>(),
            get<FirestoreProductRepository>(),
            get()
        )
    }
    viewModel {
        ProductListViewModel(
            get<FirestoreProductRepository>(),
            get<CodeRepository>(),
            get<AuthRepository>(),
            get<AppConfigRepository>(),
            get<UserRepository>()
        )
    }
}
