package com.good4.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Route {
    @Serializable
    data object Splash : Route()
    
    @Serializable
    data object ForceUpdate : Route()
    
    @Serializable
    data object Login : Route()

    @Serializable
    data object RegisterOptions : Route()
    
    @Serializable
    data object StudentRegister : Route()
    
    @Serializable
    data object BusinessRegister : Route()

    @Serializable
    data object EmailVerification : Route()

    @Serializable
    data object SessionRestore : Route()

    @Serializable
    data object WebPanelNotice : Route()
    
    @Serializable
    data object StudentHome : Route()

    @Serializable
    data class EditHome(val communityManager: Boolean = false) : Route()

    @Serializable
    data object AcademicCalendar : Route()

    @Serializable
    data object ClassSchedule : Route()

    @Serializable
    data object StudentProfile : Route()

    @Serializable
    data class StudentAccountSettings(val academicSelectionPrompt: Boolean = false) : Route()

    @Serializable
    data object Notifications : Route()

    @Serializable
    data object CampusCloset : Route()

    @Serializable
    data class CampusClosetListing(val listingId: String) : Route()

    @Serializable
    data object CampusClosetNewListing : Route()

    @Serializable
    data object CampusClosetMyListings : Route()

    @Serializable
    data object CampusClosetInbox : Route()

    @Serializable
    data object CampusClosetFavorites : Route()

    @Serializable
    data object CampusClosetBlocked : Route()

    @Serializable
    data class CampusClosetChat(val conversationId: String) : Route()

    @Serializable
    data object Social : Route()

    @Serializable
    data object SocialCreate : Route()

    @Serializable
    data class SocialActivity(val activityId: String) : Route()

    @Serializable
    data class SocialRequests(val activityId: String) : Route()

    @Serializable
    data object SocialInbox : Route()

    @Serializable
    data class SocialChat(val conversationId: String) : Route()

    @Serializable
    data class NotificationEvent(val organizationId: String, val eventId: String, val showTicket: Boolean = false) : Route()
    
    @Serializable
    data class ProductDetail(val productId: String) : Route()
    
    @Serializable
    data object BusinessHome : Route()

    @Serializable
    data object BusinessProfile : Route()

    @Serializable
    data object BusinessAccountSettings : Route()
    
    @Serializable
    data object AdminHome : Route()

    @Serializable
    data object AdminProfile : Route()

    @Serializable
    data object AdminAccountSettings : Route()
}
