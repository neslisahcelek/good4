package com.good4.di

import com.good4.business.data.dto.FirestoreBusinessRepository
import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.data.repository.FirestoreRepositoryImpl
import com.good4.product.data.repository.FirestoreProductRepository
import com.good4.product.presentation.product_list.ProductListViewModel
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module

val commonModule = module {
    single<FirestoreRepository> { FirestoreRepositoryImpl() }
    
    single { FirestoreBusinessRepository(get<FirestoreRepository>()) }
    single { FirestoreProductRepository(get<FirestoreRepository>(), get<FirestoreBusinessRepository>()) }
    
    viewModel { ProductListViewModel(get()) }
}