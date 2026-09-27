package com.good4.config.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HomeBannerDto(
    @SerialName("imageUrl") val imageUrl: String? = null,
    @SerialName("advertiserName") val advertiserName: String? = null,
    @SerialName("targetUrl") val targetUrl: String? = null,
    @SerialName("startsOn") val startsOn: String? = null,
    @SerialName("endsOn") val endsOn: String? = null,
    @SerialName("active") val active: Boolean? = null
)
