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
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.error_code_not_found
import good4.composeapp.generated.resources.error_code_not_found_or_used
import org.jetbrains.compose.resources.getString

data class CodeWithDetails(
    val id: String,
    val value: String,
    val businessId: String,
    val productId: String,
    val userId: String,
    val status: String,
    val createdAt: Long?,
    val usedAt: Long?,
    val productName: String?,
    val businessName: String?,
    val businessAddress: String?,
    val businessAddressUrl: String?
)

data class CodeCounts(
    val pending: Int,
    val completed: Int
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

    private suspend fun buildCodesWithDetails(
        userCodes: List<com.good4.core.data.repository.DocumentWithId<CodeDto>>
    ): List<CodeWithDetails> {
        return userCodes.map { documentWithId ->
            val code = documentWithId.data

            val productName = code.productId?.let { productId ->
                when (val productResult = productRepository.getProductById(productId)) {
                    is Result.Success -> productResult.data.name
                    is Result.Error -> null
                }
            }

            val business = code.businessId?.let { businessId ->
                when (val businessResult = businessRepository.getBusinessById(businessId)) {
                    is Result.Success -> businessResult.data
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
                createdAt = code.createdAt,
                usedAt = code.usedAt,
                productName = productName,
                businessName = business?.name,
                businessAddress = business?.address,
                businessAddressUrl = business?.addressUrl
            )
        }
    }

    suspend fun getCodesByUserId(userId: String): Result<List<CodeWithDetails>, Error> {
        return when (val result = firestoreRepository.queryCollectionWithIds("codes", "userId", userId, CodeDto::class)) {
            is Result.Success -> {
                Result.Success(buildCodesWithDetails(result.data))
            }
            is Result.Error -> result
        }
    }

    suspend fun getRecentCodesByUserId(
        userId: String,
        limit: Long
    ): Result<List<CodeWithDetails>, Error> {
        return when (val result = firestoreRepository.queryCollectionWithMultipleConditionsAndLimit(
            collectionPath = "codes",
            conditions = mapOf("userId" to userId),
            orderByField = "createdAt",
            descending = true,
            limit = limit,
            clazz = CodeDto::class
        )) {
            is Result.Success -> Result.Success(buildCodesWithDetails(result.data))
            is Result.Error -> result
        }
    }

    suspend fun getCodesByUserIdAndStatus(
        userId: String,
        status: CodeStatus,
        limit: Long? = null,
        orderByField: String? = null,
        descending: Boolean = false
    ): Result<List<CodeWithDetails>, Error> {
        val conditions = mapOf(
            "userId" to userId,
            "status" to status.value
        )

        val result = if (limit != null) {
            firestoreRepository.queryCollectionWithMultipleConditionsAndLimit(
                collectionPath = "codes",
                conditions = conditions,
                orderByField = orderByField,
                descending = descending,
                limit = limit,
                clazz = CodeDto::class
            )
        } else {
            firestoreRepository.queryCollectionWithMultipleConditions(
                collectionPath = "codes",
                conditions = conditions,
                clazz = CodeDto::class
            )
        }

        return when (result) {
            is Result.Success -> Result.Success(buildCodesWithDetails(result.data))
            is Result.Error -> result
        }
    }

    @Suppress("unused")
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
                        createdAt = code.createdAt,
                        usedAt = code.usedAt,
                        productName = productName,
                        businessName = null,
                        businessAddress = null,
                        businessAddressUrl = null
                    )
                }

                Result.Success(codesWithDetails)
            }
            is Result.Error -> result
        }
    }

    suspend fun getCodeCountsByBusinessId(businessId: String): Result<CodeCounts, Error> {
        return when (val result = firestoreRepository.queryCollectionWithIds("codes", "businessId", businessId, CodeDto::class)) {
            is Result.Success -> {
                val pending = result.data.count { it.data.statusEnum == CodeStatus.PENDING }
                val completed = result.data.count { it.data.statusEnum == CodeStatus.USED }
                Result.Success(CodeCounts(pending = pending, completed = completed))
            }
            is Result.Error -> result
        }
    }

    suspend fun getRecentCodesByBusinessId(
        businessId: String,
        limit: Long
    ): Result<List<CodeWithDetails>, Error> {
        return when (val result = firestoreRepository.queryCollectionWithMultipleConditionsAndLimit(
            collectionPath = "codes",
            conditions = mapOf("businessId" to businessId),
            orderByField = "createdAt",
            descending = true,
            limit = limit,
            clazz = CodeDto::class
        )) {
            is Result.Success -> Result.Success(buildCodesWithDetails(result.data))
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
                            createdAt = code.createdAt,
                            usedAt = code.usedAt,
                            productName = null,
                            businessName = null,
                            businessAddress = null,
                            businessAddressUrl = null
                        )
                    )
                } else {
                    Result.Error(NetworkError(getString(Res.string.error_code_not_found_or_used)))
                }
            }
            is Result.Error -> result
        }
    }

    suspend fun markCodeAsUsed(codeId: String): Result<Unit, Error> {
        return firestoreRepository.updateFields(
            collectionPath = "codes",
            documentId = codeId,
            fields = mapOf(
                "status" to CodeStatus.USED.value,
                "usedAt" to kotlinx.datetime.Clock.System.now().epochSeconds
            )
        )
    }

    @Suppress("unused")
    suspend fun createCode(code: CodeDto): Result<String, Error> {
        return firestoreRepository.addDocument("codes", code)
    }

    suspend fun reserveProductAndCreateCode(
        productId: String,
        code: CodeDto
    ): Result<String, Error> {
        return firestoreRepository.reserveProductAndCreateCode(productId, code)
    }

    suspend fun getCodeIdByValue(codeValue: String): Result<String, Error> {
        return when (val result = firestoreRepository.queryCollectionWithIds("codes", "value", codeValue, CodeDto::class)) {
            is Result.Success -> {
                val codeId = result.data.firstOrNull()?.id
                if (codeId != null) {
                    Result.Success(codeId)
                } else {
                    Result.Error(NetworkError(getString(Res.string.error_code_not_found)))
                }
            }
            is Result.Error -> result
        }
    }
    
    suspend fun markCodeAsExpired(codeId: String): Result<Unit, Error> {
        return firestoreRepository.updateFields(
            collectionPath = "codes",
            documentId = codeId,
            fields = mapOf("status" to CodeStatus.EXPIRED.value)
        )
    }

    suspend fun markCodeAsCancelled(codeId: String): Result<Unit, Error> {
        return firestoreRepository.updateFields(
            collectionPath = "codes",
            documentId = codeId,
            fields = mapOf("status" to CodeStatus.CANCELLED.value)
        )
    }

    suspend fun checkAndExpireCodes(): Result<Unit, Error> {
        return when (val result = firestoreRepository.getCollectionWithIds("codes", CodeDto::class)) {
            is Result.Success -> {
                val nowSecs = kotlinx.datetime.Clock.System.now().epochSeconds
                val expirationSecs = configRepository.getExpirationDuration().inWholeSeconds

                result.data.forEach { documentWithId ->
                    val code = documentWithId.data
                    if (code.statusEnum == CodeStatus.PENDING) {
                        val hasExpired = code.expiresAt?.let { expiresAt ->
                            nowSecs >= expiresAt
                        } ?: code.createdAt?.let { createdAt ->
                            nowSecs - createdAt >= expirationSecs
                        } ?: false

                        if (hasExpired) {
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
