package com.good4.di

import com.good4.auth.data.repository.AuthRepository
import com.good4.auth.presentation.login.LoginViewModel
import com.good4.auth.presentation.register.business.BusinessRegisterViewModel
import com.good4.auth.presentation.register.student.StudentRegisterViewModel
import com.good4.business.data.dto.FirestoreBusinessRepository
import com.good4.business.presentation.dashboard.BusinessDashboardViewModel
import com.good4.business.presentation.products.BusinessProductsViewModel
import com.good4.business.presentation.profile.BusinessProfileViewModel
import com.good4.business.presentation.verify.VerifyCodeViewModel
import com.good4.code.data.repository.CodeRepository
import com.good4.config.data.repository.AppConfigRepository
import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.data.repository.FirestoreRepositoryImpl
import com.good4.product.data.repository.FirestoreProductRepository
import com.good4.product.presentation.product_list.ProductListViewModel
import com.good4.student.presentation.profile.StudentProfileViewModel
import com.good4.student.presentation.reservations.StudentReservationsViewModel
import com.good4.user.data.repository.UserRepository
import org.koin.core.module.dsl.*
import org.koin.dsl.module

expect val platformModule: org.koin.core.module.Module

val commonModule = module {
    single<FirestoreRepository> { FirestoreRepositoryImpl() }

    single { AppConfigRepository(get<FirestoreRepository>()) }

    single { FirestoreBusinessRepository(get<FirestoreRepository>()) }
    single { UserRepository(get<FirestoreRepository>(), get<AppConfigRepository>()) }
    single { FirestoreProductRepository(get<FirestoreRepository>(), get<FirestoreBusinessRepository>()) }
    single { CodeRepository(get<FirestoreRepository>(), get<FirestoreBusinessRepository>(), get<FirestoreProductRepository>(), get<AppConfigRepository>()) }

    viewModel { LoginViewModel(get<AuthRepository>(), get<UserRepository>()) }
    viewModel { StudentRegisterViewModel(get<AuthRepository>(), get<UserRepository>(), get<AppConfigRepository>()) }
    viewModel { BusinessRegisterViewModel(get<AuthRepository>(), get<UserRepository>(), get<FirestoreBusinessRepository>()) }
    viewModel { ProductListViewModel(get<FirestoreProductRepository>(), get<CodeRepository>(), get<AuthRepository>(), get<AppConfigRepository>(), get<UserRepository>()) }
    viewModel { StudentProfileViewModel(get<AuthRepository>(), get<UserRepository>()) }
    viewModel { StudentReservationsViewModel(get<AuthRepository>(), get<CodeRepository>(), get<UserRepository>(), get<AppConfigRepository>()) }
    viewModel { BusinessProfileViewModel(get<AuthRepository>(), get<UserRepository>(), get<FirestoreBusinessRepository>()) }
    viewModel {
        BusinessDashboardViewModel(
            get<AuthRepository>(),
            get<UserRepository>(),
            get<FirestoreBusinessRepository>(),
            get<CodeRepository>(),
            get<FirestoreProductRepository>()
        )
    }
    viewModel { VerifyCodeViewModel(get<AuthRepository>(), get<FirestoreBusinessRepository>(), get<CodeRepository>(), get<FirestoreProductRepository>(), get<UserRepository>()) }
    viewModel { BusinessProductsViewModel(get<AuthRepository>(), get<FirestoreBusinessRepository>(), get<FirestoreProductRepository>()) }
}
