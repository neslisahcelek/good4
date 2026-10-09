package com.good4.config.domain

data class UpdateNotice(
    val title: String = "",
    val message: String = "",
    val enabled: Boolean = false
)
