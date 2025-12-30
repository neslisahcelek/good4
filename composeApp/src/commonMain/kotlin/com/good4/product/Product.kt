package com.good4.product

data class Product(
    val id: Long,
    val name: String,
    val description: String,
    val storeName: String,
    val price: String,
    val originalPrice: Int?,
    val discountPrice: Int?,
    val discountPercentage: Int?,
    val imageUrl: String,
    val address: String,
    val amount: Int
)

