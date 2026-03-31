package com.good4.supporter.data.local

import com.good4.product.Product
import kotlinx.serialization.Serializable

interface SupporterCartStorage {
    suspend fun loadItems(userId: String): List<SupporterCartStoredItem>
    suspend fun saveItems(userId: String, items: List<SupporterCartStoredItem>)
    suspend fun clear(userId: String)
}

@Serializable
data class SupporterCartStoredItem(
    val product: SupporterCartStoredProduct,
    val quantity: Int
)

@Serializable
data class SupporterCartStoredProduct(
    val id: Long,
    val documentId: String,
    val name: String,
    val description: String,
    val storeName: String,
    val businessId: String,
    val price: Int,
    val originalPrice: Int? = null,
    val discountPrice: Int? = null,
    val discountPercentage: Int? = null,
    val imageUrl: String,
    val address: String,
    val addressUrl: String = "",
    val pendingCount: Int = 0,
    val totalDelivered: Int = 0,
    val totalSuspended: Int = 0,
    val createdAt: Long? = null
)

fun SupporterCartStoredProduct.toProduct(): Product {
    return Product(
        id = id,
        documentId = documentId,
        name = name,
        description = description,
        storeName = storeName,
        businessId = businessId,
        price = price,
        originalPrice = originalPrice,
        discountPrice = discountPrice,
        discountPercentage = discountPercentage,
        imageUrl = imageUrl,
        address = address,
        addressUrl = addressUrl,
        pendingCount = pendingCount,
        totalDelivered = totalDelivered,
        totalSuspended = totalSuspended,
        createdAt = createdAt
    )
}

fun Product.toStoredProduct(): SupporterCartStoredProduct {
    return SupporterCartStoredProduct(
        id = id,
        documentId = documentId,
        name = name,
        description = description,
        storeName = storeName,
        businessId = businessId,
        price = price,
        originalPrice = originalPrice,
        discountPrice = discountPrice,
        discountPercentage = discountPercentage,
        imageUrl = imageUrl,
        address = address,
        addressUrl = addressUrl,
        pendingCount = pendingCount,
        totalDelivered = totalDelivered,
        totalSuspended = totalSuspended,
        createdAt = createdAt
    )
}
