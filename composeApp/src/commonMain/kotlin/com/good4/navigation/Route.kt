package com.good4.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Route {
    @Serializable
    data object Splash : Route()
    
    @Serializable
    data object Login : Route()
    
    @Serializable
    data object StudentRegister : Route()
    
    @Serializable
    data object BusinessRegister : Route()

    @Serializable
    data object EmailVerification : Route()

    @Serializable
    data object SessionRestore : Route()
    
    @Serializable
    data object StudentHome : Route()
    
    @Serializable
    data class ProductDetail(val productId: String) : Route()
    
    @Serializable
    data object BusinessHome : Route()
    
    @Serializable
    data object AdminHome : Route()

    @Serializable
    data object SupporterRegister : Route()

    @Serializable
    data object SupporterHome : Route()

    @Serializable
    data class SupporterOrderCode(val orderId: String) : Route()
}
