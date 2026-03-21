package com.good4.product

data class Product(
    val id: Long,
    val documentId: String,
    val name: String,
    val description: String,
    val storeName: String,
    val businessId: String,
    val price: Int,
    val originalPrice: Int?,
    val discountPrice: Int?,
    val discountPercentage: Int?,
    val imageUrl: String,
    val address: String,
    val addressUrl: String = "",
    val amount: Int,
    val pendingCount: Int = 0,
    val totalDelivered: Int = 0,
    val totalSuspended: Int = 0,
    val createdAt: Long? = null
)
