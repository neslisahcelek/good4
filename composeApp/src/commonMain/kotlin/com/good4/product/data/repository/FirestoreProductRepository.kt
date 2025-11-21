package com.good4.product.data.repository

import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.domain.Error
import com.good4.core.domain.Result
import com.good4.product.Product
import com.good4.product.data.dto.ProductDto

class FirestoreProductRepository(
    private val firestoreRepository: FirestoreRepository
) {
    suspend fun getProducts(): Result<List<Product>, Error> {
        return when (val result = firestoreRepository.getCollectionWithIds("products", ProductDto::class)) {
            is Result.Success -> {
                val products = result.data.map { it.data.toProduct(it.id) }
                Result.Success(products)
            }
            is Result.Error -> result
        }
    }
    
    suspend fun getProductById(id: String): Result<Product, Error> {
        return when (val result = firestoreRepository.getDocument("products", id, ProductDto::class)) {
            is Result.Success -> {
                Result.Success(result.data.toProduct(id))
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

private fun ProductDto.toProduct(documentId: String): Product {
    return Product(
        id = documentId.hashCode().toLong(),
        documentId = documentId,
        name = name ?: "",
        description = description ?: "",
        storeName = "", // Business'tan çekilebilir
        price = "${discountPrice ?: originalPrice ?: 0} TL",
        imageUrl = imageUrl ?: "",
        address = "", // Business'tan çekilebilir
        amount = count ?: 0
    )
}
