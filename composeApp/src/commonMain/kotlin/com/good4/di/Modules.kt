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
import com.good4.auth.presentation.verify_email.EmailVerificationViewModel
import com.good4.business.data.dto.FirestoreBusinessRepository
import com.good4.business.presentation.dashboard.BusinessDashboardViewModel
import com.good4.business.presentation.products.BusinessProductsViewModel
import com.good4.business.presentation.profile.BusinessProfileViewModel
import com.good4.business.presentation.verify.VerifyCodeViewModel
import com.good4.calendar.AcademicCalendarRepository
import com.good4.calendar.AcademicCalendarViewModel
import com.good4.campaign.data.repository.CampaignRepository
import com.good4.code.data.repository.CodeRepository
import com.good4.config.data.repository.AppConfigRepository
import com.good4.community.CommunityRepository
import com.good4.core.data.local.StartupSessionCache
import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.data.repository.FirestoreRepositoryImpl
import com.good4.core.data.repository.ProductImageUploadRepository
import com.good4.core.presentation.sessionrestore.SessionRestoreViewModel
import com.good4.core.presentation.splash.SplashViewModel
import com.good4.dining.data.repository.AkdenizDiningMenuRepository
import com.good4.dining.data.repository.KykMenuRepository
import com.good4.dining.presentation.AkdenizDiningMenuViewModel
import com.good4.feedback.FeedbackRepository
import com.good4.feedback.FeedbackViewModel
import com.good4.product.data.repository.FirestoreProductRepository
import com.good4.product.presentation.product_list.ProductListViewModel
import com.good4.schedule.presentation.ClassScheduleViewModel
import com.good4.student.presentation.profile.StudentProfileViewModel
import com.good4.student.presentation.reservations.StudentReservationsViewModel
import com.good4.user.data.repository.UserRepository
import com.good4.user.presentation.accountsettings.AccountSettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

expect val platformModule: org.koin.core.module.Module

val commonModule = module {
    single { com.good4.student.home.HomeLayoutStore(com.good4.student.home.DeviceHomeLayoutStorage()) }
    viewModel { com.good4.student.home.HomeLayoutViewModel(get(), get()) }
    single { com.good4.community.CommunityRepository(get(), get(), get()) }
    viewModel { com.good4.community.CommunityViewModel(get()) }
    single<FirestoreRepository> { FirestoreRepositoryImpl() }

    single { AppConfigRepository(get<FirestoreRepository>()) }

    single { FirestoreBusinessRepository(get<FirestoreRepository>()) }
    single { UserRepository(get<FirestoreRepository>(), get<AppConfigRepository>()) }
    single { FirestoreProductRepository(get<FirestoreRepository>(), get<FirestoreBusinessRepository>()) }
    single { CampaignRepository(get<FirestoreRepository>()) }
    single { CodeRepository(get<FirestoreRepository>(), get<FirestoreBusinessRepository>(), get<FirestoreProductRepository>(), get<AppConfigRepository>()) }
    single { AkdenizDiningMenuRepository(get<FirestoreRepository>()) }
    single { KykMenuRepository(get<FirestoreRepository>()) }
    single { com.good4.suspendedmeal.SuspendedMealRepository(get<FirestoreRepository>()) }
    single { com.good4.weather.CampusWeatherRepository(get<FirestoreRepository>()) }
    single { AcademicCalendarRepository(get<FirestoreRepository>()) }
    single { FeedbackRepository(get<FirestoreRepository>(), get<AuthRepository>()) }
    single { com.good4.eduverification.EduVerificationRepository(get<FirestoreRepository>(), get<AuthRepository>()) }

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
    viewModel {
        EmailVerificationViewModel(
            get<AuthRepository>(),
            get<UserRepository>(),
            get<StartupSessionCache>()
        )
    }
    viewModel { ProductListViewModel(get<FirestoreProductRepository>(), get<CodeRepository>(), get<AuthRepository>(), get<AppConfigRepository>(), get<UserRepository>()) }
    viewModel {
        StudentProfileViewModel(
            get<AuthRepository>(),
            get<UserRepository>(),
            get<CommunityRepository>()
        )
    }
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
            get<FirestoreProductRepository>()
        )
    }
    viewModel { VerifyCodeViewModel(get<AuthRepository>(), get<FirestoreBusinessRepository>(), get<CodeRepository>(), get<FirestoreProductRepository>(), get()) }
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
    viewModel { AkdenizDiningMenuViewModel(get<AkdenizDiningMenuRepository>(), get<KykMenuRepository>()) }
    viewModel { AcademicCalendarViewModel(get<AcademicCalendarRepository>()) }
    viewModel { ClassScheduleViewModel(get<AuthRepository>(), get<UserRepository>()) }
    viewModel { FeedbackViewModel(get<FeedbackRepository>()) }
    viewModel { com.good4.eduverification.EduVerificationViewModel(get()) }
    viewModel { com.good4.suspendedmeal.SuspendedMealsViewModel(get()) }
    viewModel {
        AccountSettingsViewModel(
            get<AuthRepository>(),
            get<UserRepository>(),
            get<FirestoreBusinessRepository>(),
            get<AppConfigRepository>(),
            get<CommunityRepository>()
        )
    }
}
