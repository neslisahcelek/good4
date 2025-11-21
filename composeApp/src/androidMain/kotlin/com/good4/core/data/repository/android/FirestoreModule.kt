package com.good4.core.data.repository.android

import com.good4.business.data.dto.FirestoreBusinessRepository
import com.good4.core.data.repository.FirestoreRepository
import com.good4.product.data.repository.FirestoreProductRepository
import com.good4.product.presentation.product_list.ProductListViewModel
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module

val firestoreModule = module {
    single<FirestoreRepository> { FirestoreRepositoryAndroidImpl(get()) }
    single { FirestoreBusinessRepository(get<FirestoreRepository>()) }
    single { FirestoreProductRepository(get<FirestoreRepository>()) }
    viewModel { ProductListViewModel(get()) }
}
