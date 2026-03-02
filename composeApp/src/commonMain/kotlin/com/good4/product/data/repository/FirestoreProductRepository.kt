package com.good4.product.data.repository

import com.good4.business.data.dto.FirestoreBusinessRepository
import com.good4.business.domain.Business
import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.domain.Error
import com.good4.core.domain.Result
import com.good4.core.util.Logger
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
                    val productDto = documentWithId.data
                    val business = productDto.businessId?.let { businessCache[it] }
                    productDto.toProduct(documentWithId.id, business)
                }.let { items ->
                    if (includeOutOfStock) {
                        items
                    } else {
                        items.filter { product -> product.amount > 0 }
                    }
                }
                Logger.d("Admin", "Fetched ${products.size} products: $products")
                
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
                    val productDto = documentWithId.data
                    productDto.toProduct(documentWithId.id, business)
                }.let { items ->
                    if (includeOutOfStock) items else items.filter { it.amount > 0 }
                }

                Result.Success(products)
            }
            is Result.Error -> result
        }
    }
    
    suspend fun getProductById(id: String): Result<Product, Error> {
        return when (val result = firestoreRepository.getDocument("products", id, ProductDto::class)) {
            is Result.Success -> {
                val productDto = result.data
                val businessResult = productDto.businessId?.let { businessId ->
                    businessRepository.getBusinessById(businessId)
                }
                
                val business = when (businessResult) {
                    is Result.Success -> businessResult.data
                    else -> null
                }
                
                Result.Success(productDto.toProduct(id, business))
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

    suspend fun decrementProductCount(id: String): Result<Unit, Error> {
        return when (val result = firestoreRepository.getDocument("products", id, ProductDto::class)) {
            is Result.Success -> {
                val currentCount = result.data.count ?: 0
                val updatedCount = (currentCount - 1).coerceAtLeast(0)
                val updatedProduct = result.data.copy(count = updatedCount)
                updateProduct(id, updatedProduct)
            }
            is Result.Error -> result
        }
    }
    
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
        amount = count ?: 0,
        createdAt = createdAt
    )
}
