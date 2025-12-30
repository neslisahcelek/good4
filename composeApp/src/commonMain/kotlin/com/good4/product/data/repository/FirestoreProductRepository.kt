package com.good4.product.data.repository

import com.good4.business.data.dto.BusinessDto
import com.good4.business.data.dto.FirestoreBusinessRepository
import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.domain.Error
import com.good4.core.domain.Result
import com.good4.product.Product
import com.good4.product.data.dto.ProductDto

class FirestoreProductRepository(
    private val firestoreRepository: FirestoreRepository,
    private val businessRepository: FirestoreBusinessRepository
) {
    suspend fun getProducts(): Result<List<Product>, Error> {
        return when (val result = firestoreRepository.getCollectionWithIds("products", ProductDto::class)) {
            is Result.Success -> {
                val products = result.data.map { documentWithId ->
                    val productDto = documentWithId.data
                    val businessResult = productDto.businessId?.let { businessId ->
                        businessRepository.getBusinessById(businessId)
                    }
                    
                    when (businessResult) {
                        is Result.Success -> {
                            productDto.toProduct(documentWithId.id, businessResult.data)
                        }
                        else -> {
                            productDto.toProduct(documentWithId.id, null)
                        }
                    }
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
    
    suspend fun deleteProduct(id: String): Result<Unit, Error> {
        return firestoreRepository.deleteDocument("products", id)
    }
}

private fun ProductDto.toProduct(documentId: String, business: BusinessDto?): Product {
    val originalPriceValue = originalPrice
    val discountPriceValue = discountPrice
    val discountPercentageValue = if (originalPriceValue != null && discountPriceValue != null && originalPriceValue > 0) {
        ((originalPriceValue - discountPriceValue).toDouble() / originalPriceValue * 100).toInt()
    } else null
    
    val displayPrice = discountPriceValue ?: originalPriceValue ?: 0
    
    return Product(
        id = documentId.hashCode().toLong(),
        name = name ?: "",
        description = description ?: "",
        storeName = business?.name ?: "",
        price = "$displayPrice TL",
        originalPrice = originalPriceValue,
        discountPrice = discountPriceValue,
        discountPercentage = discountPercentageValue,
        imageUrl = imageUrl ?: "",
        address = business?.address ?: "",
        amount = count ?: 0
    )
}
