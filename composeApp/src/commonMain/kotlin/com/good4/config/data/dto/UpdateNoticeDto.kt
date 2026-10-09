package com.good4.config.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateNoticeDto(
    val title: String? = null,
    val message: String? = null,
    val enabled: Boolean? = null
)
