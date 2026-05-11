package com.good4.product.data.repository

import com.good4.business.data.dto.FirestoreBusinessRepository
import com.good4.business.domain.Business
import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.domain.Error
import com.good4.core.domain.Result
import com.good4.product.Product
import com.good4.product.data.dto.ProductDto

class FirestoreProductRepository(
    private val firestoreRepository: FirestoreRepository,
    private val businessRepository: FirestoreBusinessRepository
) {
    suspend fun getProducts(includeOutOfStock: Boolean = false): Result<List<Product>, Error> {
        return when (val result = firestoreRepository.getCollectionWithIds("products", ProductDto::class)) {
            is Result.Success -> {
                val uniqueBusinessIds = result.data.mapNotNull { it.data.businessId }.distinct()
                
                val businessCache = mutableMapOf<String, Business?>()
                uniqueBusinessIds.forEach { businessId ->
                    when (val businessResult = businessRepository.getBusinessById(businessId)) {
                        is Result.Success -> {
                            businessCache[businessId] = businessResult.data
                        }
                        is Result.Error -> {
                            businessCache[businessId] = null
                        }
                    }
                }
                
                val products = result.data.map { documentWithId ->
                    val business = documentWithId.data.businessId?.let { businessCache[it] }
                    documentWithId.data.toProduct(documentWithId.id, business)
                }.let { items ->
                    if (includeOutOfStock) {
                        items
                    } else {
                        items.filter { product -> product.pendingCount > 0 }
                    }
                }
                
                Result.Success(products)
            }
            is Result.Error -> result
        }
    }

    suspend fun getProductsByBusinessId(
        businessId: String,
        includeOutOfStock: Boolean = true
    ): Result<List<Product>, Error> {
        return when (val result = firestoreRepository.queryCollectionWithIds(
            collectionPath = "products",
            field = "businessId",
            value = businessId,
            clazz = ProductDto::class
        )) {
            is Result.Success -> {
                val business = when (val businessResult = businessRepository.getBusinessById(businessId)) {
                    is Result.Success -> businessResult.data
                    is Result.Error -> null
                }

                val products = result.data.map { documentWithId ->
                    documentWithId.data.toProduct(documentWithId.id, business)
                }.let { items ->
                    if (includeOutOfStock) items else items.filter { it.pendingCount > 0 }
                }

                Result.Success(products)
            }
            is Result.Error -> result
        }
    }
    
    suspend fun getProductById(id: String): Result<Product, Error> {
        return when (val result = firestoreRepository.getDocument("products", id, ProductDto::class)) {
            is Result.Success -> {
                val businessResult = result.data.businessId?.let { businessId ->
                    businessRepository.getBusinessById(businessId)
                }
                
                val business = when (businessResult) {
                    is Result.Success -> businessResult.data
                    else -> null
                }
                
                Result.Success(result.data.toProduct(id, business))
            }
            is Result.Error -> result
        }
    }
    
    suspend fun addProduct(product: ProductDto): Result<String, Error> {
        return firestoreRepository.addDocument("products", product)
    }
    
    suspend fun updateProduct(id: String, product: ProductDto): Result<Unit, Error> {
        return firestoreRepository.updateDocument("products", id, product)
    }

    @Suppress("unused")
    suspend fun decrementProductPendingCount(id: String): Result<Unit, Error> {
        return when (val result = firestoreRepository.getDocument("products", id, ProductDto::class)) {
            is Result.Success -> {
                val currentCount = result.data.pendingCount ?: 0
                val updatedCount = (currentCount - 1).coerceAtLeast(0)
                val updatedProduct = result.data.copy(pendingCount = updatedCount)
                updateProduct(id, updatedProduct)
            }
            is Result.Error -> result
        }
    }

    suspend fun incrementProductPendingCount(id: String, amount: Int): Result<Unit, Error> {
        return when (val result = firestoreRepository.getDocument("products", id, ProductDto::class)) {
            is Result.Success -> {
                val current = result.data.pendingCount ?: 0
                val updated = result.data.copy(pendingCount = current + amount)
                updateProduct(id, updated)
            }
            is Result.Error -> result
        }
    }

    @Suppress("unused")
    suspend fun incrementProductDeliveredCount(id: String, amount: Int): Result<Unit, Error> {
        return when (val result = firestoreRepository.getDocument("products", id, ProductDto::class)) {
            is Result.Success -> {
                val current = result.data.totalDelivered ?: 0
                val updated = result.data.copy(totalDelivered = current + amount)
                updateProduct(id, updated)
            }

            is Result.Error -> result
        }
    }

    suspend fun recordProductDelivery(id: String): Result<Unit, Error> {
        return when (val result =
            firestoreRepository.getDocument("products", id, ProductDto::class)) {
            is Result.Success -> {
                val currentDelivered = result.data.totalDelivered ?: 0
                val updated = result.data.copy(
                    totalDelivered = currentDelivered + 1
                )
                updateProduct(id, updated)
            }
            is Result.Error -> result
        }
    }

    suspend fun incrementProductSuspendedCount(id: String, amount: Int): Result<Unit, Error> {
        return when (val result = firestoreRepository.getDocument("products", id, ProductDto::class)) {
            is Result.Success -> {
                val current = result.data.totalSuspended ?: 0
                val updated = result.data.copy(totalSuspended = current + amount)
                updateProduct(id, updated)
            }
            is Result.Error -> result
        }
    }

    @Suppress("unused")
    suspend fun deleteProduct(id: String): Result<Unit, Error> {
        return firestoreRepository.deleteDocument("products", id)
    }

}

private fun ProductDto.toProduct(documentId: String, business: Business?): Product {
    val originalPriceValue = originalPrice
    val discountPriceValue = discountPrice
    val discountPercentageValue = if (originalPriceValue != null && discountPriceValue != null && originalPriceValue > 0) {
        ((originalPriceValue - discountPriceValue).toDouble() / originalPriceValue * 100).toInt()
    } else null
    
    val displayPrice = discountPriceValue ?: originalPriceValue ?: 0
    
    return Product(
        id = documentId.hashCode().toLong(),
        documentId = documentId,
        name = name ?: "",
        description = description ?: "",
        storeName = business?.name ?: "",
        businessId = businessId ?: "",
        price = displayPrice,
        originalPrice = originalPriceValue,
        discountPrice = discountPriceValue,
        discountPercentage = discountPercentageValue,
        imageUrl = imageUrl ?: "",
        address = business?.address ?: "",
        addressUrl = business?.addressUrl ?: "",
        pendingCount = pendingCount ?: 0,
        dailyPendingLimit = dailyPendingLimit,
        isDonation = isDonation ?: false,
        totalDelivered = totalDelivered ?: 0,
        totalSuspended = totalSuspended ?: 0,
        createdAt = createdAt
    )
}
