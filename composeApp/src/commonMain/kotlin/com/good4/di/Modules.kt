package com.good4.di

import com.good4.admin.presentation.campaigns.AdminCampaignsViewModel
import com.good4.admin.presentation.dashboard.AdminDashboardViewModel
import com.good4.admin.presentation.editstudentcredit.EditStudentCreditViewModel
import com.good4.admin.presentation.products.AdminProductsViewModel
import com.good4.admin.presentation.profile.AdminProfileViewModel
import com.good4.auth.data.repository.AuthRepository
import com.good4.auth.presentation.login.LoginViewModel
import com.good4.auth.presentation.register.business.BusinessRegisterViewModel
import com.good4.auth.presentation.register.student.StudentRegisterViewModel
import com.good4.auth.presentation.register.supporter.SupporterRegisterViewModel
import com.good4.auth.presentation.verify_email.EmailVerificationViewModel
import com.good4.business.data.dto.FirestoreBusinessRepository
import com.good4.business.presentation.dashboard.BusinessDashboardViewModel
import com.good4.business.presentation.products.BusinessProductsViewModel
import com.good4.business.presentation.profile.BusinessProfileViewModel
import com.good4.business.presentation.verify.VerifyCodeViewModel
import com.good4.campaign.data.repository.CampaignRepository
import com.good4.code.data.repository.CodeRepository
import com.good4.config.data.repository.AppConfigRepository
import com.good4.core.data.local.StartupSessionCache
import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.data.repository.FirestoreRepositoryImpl
import com.good4.core.data.repository.ProductImageUploadRepository
import com.good4.core.presentation.sessionrestore.SessionRestoreViewModel
import com.good4.core.presentation.splash.SplashViewModel
import com.good4.order.data.repository.OrderRepository
import com.good4.product.data.repository.FirestoreProductRepository
import com.good4.product.presentation.product_list.ProductListViewModel
import com.good4.student.presentation.profile.StudentProfileViewModel
import com.good4.student.presentation.reservations.StudentReservationsViewModel
import com.good4.supportactivity.data.repository.SupportActivityRepository
import com.good4.supporter.data.local.SupporterCartStorage
import com.good4.supporter.presentation.cart.SupporterCartViewModel
import com.good4.supporter.presentation.ordercode.SupporterOrderCodeViewModel
import com.good4.supporter.presentation.products.SupporterProductListViewModel
import com.good4.supporter.presentation.profile.SupporterProfileViewModel
import com.good4.user.data.repository.UserRepository
import com.good4.user.presentation.accountsettings.AccountSettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

expect val platformModule: org.koin.core.module.Module

val commonModule = module {
    single<FirestoreRepository> { FirestoreRepositoryImpl() }

    single { AppConfigRepository(get<FirestoreRepository>()) }

    single { FirestoreBusinessRepository(get<FirestoreRepository>()) }
    single { UserRepository(get<FirestoreRepository>(), get<AppConfigRepository>()) }
    single { FirestoreProductRepository(get<FirestoreRepository>(), get<FirestoreBusinessRepository>()) }
    single { CampaignRepository(get<FirestoreRepository>()) }
    single { CodeRepository(get<FirestoreRepository>(), get<FirestoreBusinessRepository>(), get<FirestoreProductRepository>(), get<AppConfigRepository>()) }
    single { SupportActivityRepository(get<FirestoreRepository>()) }
    single { OrderRepository(get<FirestoreRepository>()) }

    viewModel { LoginViewModel(get<AuthRepository>(), get<UserRepository>(), get<StartupSessionCache>()) }
    viewModel {
        StudentRegisterViewModel(
            get<AuthRepository>(),
            get<UserRepository>(),
            get<AppConfigRepository>(),
            get<StartupSessionCache>()
        )
    }
    viewModel {
        BusinessRegisterViewModel(
            get<AuthRepository>(),
            get<UserRepository>(),
            get<FirestoreBusinessRepository>(),
            get<StartupSessionCache>()
        )
    }
    viewModel { SupporterRegisterViewModel(get<AuthRepository>(), get<UserRepository>(), get<StartupSessionCache>()) }
    viewModel {
        EmailVerificationViewModel(
            get<AuthRepository>(),
            get<UserRepository>(),
            get<StartupSessionCache>()
        )
    }
    viewModel { ProductListViewModel(get<FirestoreProductRepository>(), get<CodeRepository>(), get<AuthRepository>(), get<AppConfigRepository>(), get<UserRepository>()) }
    viewModel { StudentProfileViewModel(get<AuthRepository>(), get<UserRepository>()) }
    viewModel {
        StudentReservationsViewModel(
            get<AuthRepository>(),
            get<CodeRepository>(),
            get<UserRepository>(),
            get<FirestoreProductRepository>(),
            get<AppConfigRepository>()
        )
    }
    viewModel { BusinessProfileViewModel(get<AuthRepository>(), get<UserRepository>(), get<FirestoreBusinessRepository>()) }
    viewModel {
        BusinessDashboardViewModel(
            get<AuthRepository>(),
            get<FirestoreBusinessRepository>(),
            get<CodeRepository>(),
            get<FirestoreProductRepository>(),
            get<OrderRepository>()
        )
    }
    viewModel { VerifyCodeViewModel(get<AuthRepository>(), get<FirestoreBusinessRepository>(), get<CodeRepository>(), get<FirestoreProductRepository>(), get<OrderRepository>(), get<UserRepository>()) }
    viewModel {
        BusinessProductsViewModel(
            get<AuthRepository>(),
            get<FirestoreBusinessRepository>(),
            get<FirestoreProductRepository>(),
            get<ProductImageUploadRepository>()
        )
    }
    viewModel {
        AdminDashboardViewModel(
            get<FirestoreProductRepository>(),
            get<FirestoreBusinessRepository>(),
            get<CampaignRepository>(),
            get<UserRepository>()
        )
    }
    viewModel { AdminCampaignsViewModel(get<CampaignRepository>()) }
    viewModel {
        AdminProductsViewModel(
            get<FirestoreProductRepository>(),
            get<FirestoreBusinessRepository>(),
            get<ProductImageUploadRepository>()
        )
    }
    viewModel { EditStudentCreditViewModel(get<UserRepository>()) }
    viewModel { AdminProfileViewModel(get<AuthRepository>(), get<UserRepository>()) }
    viewModel {
        SplashViewModel(
            get<AuthRepository>(),
            get<UserRepository>(),
            get<AppConfigRepository>(),
            get<StartupSessionCache>()
        )
    }
    viewModel { SessionRestoreViewModel(get<AuthRepository>(), get<UserRepository>(), get<StartupSessionCache>()) }
    viewModel { SupporterProductListViewModel(get<FirestoreProductRepository>(), get<AuthRepository>(), get<UserRepository>()) }
    viewModel {
        SupporterCartViewModel(
            get<AuthRepository>(),
            get<UserRepository>(),
            get<OrderRepository>(),
            get<SupporterCartStorage>(),
            get<AppConfigRepository>()
        )
    }
    viewModel { SupporterOrderCodeViewModel(get<OrderRepository>(), get<FirestoreBusinessRepository>()) }
    viewModel { SupporterProfileViewModel(get<AuthRepository>(), get<UserRepository>()) }
    viewModel {
        AccountSettingsViewModel(
            get<AuthRepository>(),
            get<UserRepository>(),
            get<FirestoreBusinessRepository>(),
            get<AppConfigRepository>()
        )
    }
}
