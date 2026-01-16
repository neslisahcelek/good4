package com.good4.di

import com.good4.auth.data.repository.AuthRepository
import com.good4.auth.presentation.login.LoginViewModel
import com.good4.auth.presentation.register.business.BusinessRegisterViewModel
import com.good4.auth.presentation.register.student.StudentRegisterViewModel
import com.good4.business.data.dto.FirestoreBusinessRepository
import com.good4.config.data.repository.AppConfigRepository
import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.data.repository.FirestoreRepositoryImpl
import com.good4.user.data.repository.UserRepository
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module

// Platform-specific auth module will be provided
expect val platformModule: org.koin.core.module.Module

val commonModule = module {
    // Core repository for auth-related operations
    single<FirestoreRepository> { FirestoreRepositoryImpl() }

    // Config repository
    single { AppConfigRepository(get<FirestoreRepository>()) }

    // Repositories required for auth
    single { FirestoreBusinessRepository(get<FirestoreRepository>()) }
    single { UserRepository(get<FirestoreRepository>()) }

    // Auth ViewModels
    viewModel { LoginViewModel(get<AuthRepository>(), get<UserRepository>()) }
    viewModel { StudentRegisterViewModel(get<AuthRepository>(), get<UserRepository>()) }
    viewModel { BusinessRegisterViewModel(get<AuthRepository>(), get<UserRepository>(), get<FirestoreBusinessRepository>()) }
}