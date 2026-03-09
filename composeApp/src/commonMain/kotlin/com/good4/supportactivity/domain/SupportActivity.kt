package com.good4.supportactivity.domain

import kotlinx.datetime.Instant

data class SupportActivity(
    val id: String,
    val createdAt: Instant?,
    val creatorId: String,
    val currentCount: Int,
    val description: String,
    val endDate: Instant?,
    val shareId: String,
    val shareLink: String,
    val startDate: Instant?,
    val status: SupportActivityStatus,
    val targetBusinessId: String,
    val targetCount: Int,
    val title: String,
    val type: String
)
