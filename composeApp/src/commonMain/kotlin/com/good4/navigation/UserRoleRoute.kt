package com.good4.navigation

import com.good4.user.domain.UserRole
import config.ReleaseFeatures

fun UserRole.toHomeRoute(): Route {
    return when (this) {
        UserRole.ADMIN -> if (ReleaseFeatures.inAppStaffPanels) Route.AdminHome else Route.WebPanelNotice
        UserRole.BUSINESS -> if (ReleaseFeatures.inAppStaffPanels) Route.BusinessHome else Route.WebPanelNotice
        UserRole.STUDENT -> Route.StudentHome
        // Keep the legacy role readable, but send old supporter accounts back to login.
        UserRole.SUPPORTER -> Route.Login
    }
}
