package com.good4.business.data.dto

import com.good4.business.domain.Business
import com.good4.business.domain.BusinessApprovalStatus
import com.good4.core.data.repository.DocumentWithId
import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.domain.Error
import com.good4.core.domain.Result
import com.good4.core.domain.ValidationError

class FirestoreBusinessRepository(
    private val firestoreRepository: FirestoreRepository
) {
    suspend fun getBusinesses(): Result<List<Business>, Error> {
        return when (val result = firestoreRepository.getCollectionWithIds("businesses", BusinessDto::class)) {
            is Result.Success -> {
                Result.Success(result.data.map { it.data.toBusiness(it.id) })
            }
            is Result.Error -> result
        }
    }
    
    suspend fun getBusinessesWithIds(): Result<List<DocumentWithId<Business>>, Error> {
        return when (val result = firestoreRepository.getCollectionWithIds("businesses", BusinessDto::class)) {
            is Result.Success -> {
                Result.Success(result.data.map { DocumentWithId(it.id, it.data.toBusiness(it.id)) })
            }
            is Result.Error -> result
        }
    }

    suspend fun getPendingBusinesses(): Result<List<DocumentWithId<Business>>, Error> {
        return when (val result = getBusinessesWithIds()) {
            is Result.Success -> {
                Result.Success(
                    result.data.filter { it.data.approvalStatus == BusinessApprovalStatus.PENDING }
                )
            }
            is Result.Error -> result
        }
    }

    /**
     * Giriş yapan kullanıcının sahibi olduğu işletmenin Firestore belge kimliği.
     * Kullanıcıya ait işletme yoksa [Result.Success] içinde `null` döner.
     */
    suspend fun getOwnedBusinessId(userId: String): Result<String?, Error> {
        return when (val result = getBusinessesWithIds()) {
            is Result.Success -> {
                val id = result.data.find { it.data.ownerId == userId }?.id
                Result.Success(id)
            }
            is Result.Error -> result
        }
    }
    
    suspend fun getBusinessById(id: String): Result<Business, Error> {
        return when (val result = firestoreRepository.getDocument("businesses", id, BusinessDto::class)) {
            is Result.Success -> Result.Success(result.data.toBusiness(id))
            is Result.Error -> result
        }
    }

    suspend fun getOwnedBusiness(ownerId: String): Result<DocumentWithId<Business>?, Error> {
        return when (val result = getBusinessesWithIds()) {
            is Result.Success -> {
                Result.Success(result.data.firstOrNull { it.data.ownerId == ownerId })
            }
            is Result.Error -> result
        }
    }
    
    suspend fun addBusiness(business: BusinessDto): Result<String, Error> {
        return firestoreRepository.addDocument("businesses", business)
    }
    
    suspend fun updateBusiness(id: String, business: BusinessDto): Result<Unit, Error> {
        return firestoreRepository.updateDocument("businesses", id, business)
    }

    suspend fun approveBusiness(
        businessId: String,
        adminUserId: String,
        approvedAt: Long
    ): Result<Unit, Error> {
        return when (val current = getBusinessById(businessId)) {
            is Result.Success -> {
                val updated = current.data.copy(
                    approvalStatus = BusinessApprovalStatus.APPROVED,
                    approvedAt = approvedAt,
                    approvedBy = adminUserId
                )
                updateBusiness(businessId, updated.toDto())
            }
            is Result.Error -> current
        }
    }

    suspend fun updateOwnedBusinessProfile(
        ownerId: String,
        businessName: String,
        phone: String?
    ): Result<Unit, Error> {
        return when (val businessIdResult = getOwnedBusinessId(ownerId)) {
            is Result.Success -> {
                val businessId = businessIdResult.data
                    ?: return Result.Error(ValidationError("Business not found"))

                when (val businessResult = getBusinessById(businessId)) {
                    is Result.Success -> {
                        val normalizedPhone = phone?.trim().orEmpty()
                        val updatedDto = businessResult.data.copy(
                            name = businessName.trim(),
                            phone = normalizedPhone.ifBlank {
                                businessResult.data.phone
                            }
                        ).toDto()
                        updateBusiness(businessId, updatedDto)
                    }

                    is Result.Error -> businessResult
                }
            }

            is Result.Error -> businessIdResult
        }
    }
    
    suspend fun deleteBusiness(id: String): Result<Unit, Error> {
        return firestoreRepository.deleteDocument("businesses", id)
    }
}
