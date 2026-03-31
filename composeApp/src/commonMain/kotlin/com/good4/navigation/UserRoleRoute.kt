package com.good4.navigation

import com.good4.user.domain.UserRole

fun UserRole.toHomeRoute(): Route {
    return when (this) {
        UserRole.ADMIN -> Route.AdminHome
        UserRole.BUSINESS -> Route.BusinessHome
        UserRole.STUDENT -> Route.StudentHome
        UserRole.SUPPORTER -> Route.SupporterHome
    }
}
