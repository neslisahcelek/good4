package com.good4.code.data.repository

import com.good4.business.data.dto.FirestoreBusinessRepository
import com.good4.code.data.dto.CodeDto
import com.good4.code.data.dto.isExpired
import com.good4.code.data.dto.statusEnum
import com.good4.code.domain.CodeStatus
import com.good4.config.data.repository.AppConfigRepository
import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.domain.Error
import com.good4.core.domain.NetworkError
import com.good4.core.domain.Result
import com.good4.product.data.repository.FirestoreProductRepository

data class CodeWithDetails(
    val id: String,
    val value: String,
    val businessId: String,
    val productId: String,
    val userId: String,
    val status: String,
    val createdAt: String?,
    val usedAt: String?,
    val productName: String?,
    val businessName: String?
)

class CodeRepository(
    private val firestoreRepository: FirestoreRepository,
    private val businessRepository: FirestoreBusinessRepository,
    private val productRepository: FirestoreProductRepository,
    private val configRepository: AppConfigRepository
) {
    suspend fun getPendingCodeByUserId(userId: String): Result<CodeDto?, Error> {
        return when (val result = firestoreRepository.queryCollectionWithMultipleConditions(
            collectionPath = "codes",
            conditions = mapOf(
                "userId" to userId,
                "status" to CodeStatus.PENDING.value
            ),
            clazz = CodeDto::class
        )) {
            is Result.Success -> {
                val pendingCode = result.data
                    .map { it.data }
                    .firstOrNull { code ->
                        code.statusEnum == CodeStatus.PENDING && !code.isExpired()
                    }
                Result.Success(pendingCode)
            }
            is Result.Error -> result
        }
    }
    
    suspend fun getCodesByUserId(userId: String): Result<List<CodeWithDetails>, Error> {
        return when (val result = firestoreRepository.queryCollectionWithIds("codes", "userId", userId, CodeDto::class)) {
            is Result.Success -> {
                val userCodes = result.data

                val codesWithDetails = userCodes.map { documentWithId ->
                    val code = documentWithId.data
                    
                    // Get product and business names
                    val productName = code.productId?.let { productId ->
                        when (val productResult = productRepository.getProductById(productId)) {
                            is Result.Success -> productResult.data.name
                            is Result.Error -> null
                        }
                    }

                    val businessName = code.businessId?.let { businessId ->
                        when (val businessResult = businessRepository.getBusinessById(businessId)) {
                            is Result.Success -> businessResult.data.name
                            is Result.Error -> null
                        }
                    }

                    CodeWithDetails(
                        id = documentWithId.id,
                        value = code.value ?: "",
                        businessId = code.businessId ?: "",
                        productId = code.productId ?: "",
                        userId = code.userId ?: "",
                        status = code.status ?: CodeStatus.PENDING.value,
                        createdAt = code.createdAt?.toString(),
                        usedAt = code.usedAt?.toString(),
                        productName = productName,
                        businessName = businessName
                    )
                }

                Result.Success(codesWithDetails)
            }
            is Result.Error -> result
        }
    }

    suspend fun getCodesByBusinessId(businessId: String): Result<List<CodeWithDetails>, Error> {
        return when (val result = firestoreRepository.queryCollectionWithIds("codes", "businessId", businessId, CodeDto::class)) {
            is Result.Success -> {
                val businessCodes = result.data

                val codesWithDetails = businessCodes.map { documentWithId ->
                    val code = documentWithId.data

                    val productName = code.productId?.let { productId ->
                        when (val productResult = productRepository.getProductById(productId)) {
                            is Result.Success -> productResult.data.name
                            is Result.Error -> null
                        }
                    }

                    CodeWithDetails(
                        id = documentWithId.id,
                        value = code.value ?: "",
                        businessId = code.businessId ?: "",
                        productId = code.productId ?: "",
                        userId = code.userId ?: "",
                        status = code.status ?: CodeStatus.PENDING.value,
                        createdAt = code.createdAt?.toString(),
                        usedAt = code.usedAt?.toString(),
                        productName = productName,
                        businessName = null
                    )
                }

                Result.Success(codesWithDetails)
            }
            is Result.Error -> result
        }
    }

    suspend fun verifyCode(codeValue: String, businessId: String): Result<CodeWithDetails, Error> {
        return when (val result = firestoreRepository.getCollectionWithIds("codes", CodeDto::class)) {
            is Result.Success -> {
                val matchingCode = result.data.find { 
                    it.data.value == codeValue && 
                    it.data.businessId == businessId &&
                    it.data.statusEnum == CodeStatus.PENDING
                }

                if (matchingCode != null) {
                    val code = matchingCode.data
                    Result.Success(
                        CodeWithDetails(
                            id = matchingCode.id,
                            value = code.value ?: "",
                            businessId = code.businessId ?: "",
                            productId = code.productId ?: "",
                            userId = code.userId ?: "",
                            status = code.status ?: CodeStatus.PENDING.value,
                            createdAt = code.createdAt?.toString(),
                            usedAt = code.usedAt?.toString(),
                            productName = null,
                            businessName = null
                        )
                    )
                } else {
                    Result.Error(com.good4.core.domain.NetworkError("Kod bulunamadı veya zaten kullanılmış"))
                }
            }
            is Result.Error -> result
        }
    }

    suspend fun markCodeAsUsed(codeId: String): Result<Unit, Error> {
        val updatedCode = CodeDto(
            status = CodeStatus.USED.value,
            usedAt = kotlinx.datetime.Clock.System.now()
        )
        return firestoreRepository.updateDocument("codes", codeId, updatedCode)
    }

    suspend fun createCode(code: CodeDto): Result<String, Error> {
        return firestoreRepository.addDocument("codes", code)
    }

    suspend fun getCodeIdByValue(codeValue: String): Result<String, Error> {
        return when (val result = firestoreRepository.queryCollectionWithIds("codes", "value", codeValue, CodeDto::class)) {
            is Result.Success -> {
                val codeId = result.data.firstOrNull()?.id
                if (codeId != null) {
                    Result.Success(codeId)
                } else {
                    Result.Error(NetworkError("Code not found"))
                }
            }
            is Result.Error -> result
        }
    }
    
    suspend fun markCodeAsExpired(codeId: String): Result<Unit, Error> {
        val updatedCode = CodeDto(
            status = CodeStatus.EXPIRED.value
        )
        return firestoreRepository.updateDocument("codes", codeId, updatedCode)
    }

    suspend fun checkAndExpireCodes(): Result<Unit, Error> {
        return when (val result = firestoreRepository.getCollectionWithIds("codes", CodeDto::class)) {
            is Result.Success -> {
                val now = kotlinx.datetime.Clock.System.now()
                val expirationTime = kotlin.time.Duration.parse("PT45M")

                result.data.forEach { documentWithId ->
                    val code = documentWithId.data
                    if (code.statusEnum == CodeStatus.PENDING && code.createdAt != null) {
                        val elapsed = now - code.createdAt
                        if (elapsed >= expirationTime) {
                            markCodeAsExpired(documentWithId.id)
                        }
                    }
                }
                Result.Success(Unit)
            }
            is Result.Error -> result
        }
    }
}

