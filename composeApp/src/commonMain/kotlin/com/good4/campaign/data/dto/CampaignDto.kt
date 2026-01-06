package com.good4.campaign.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

@Serializable
data class CampaignDto(
    @SerialName("image")
    val image: String? = null,
)


