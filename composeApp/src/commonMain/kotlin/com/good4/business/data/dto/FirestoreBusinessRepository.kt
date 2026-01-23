package com.good4.business.data.dto

import com.good4.business.domain.Business
import com.good4.core.data.repository.DocumentWithId
import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.domain.Error
import com.good4.core.domain.Result

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
    
    suspend fun getBusinessById(id: String): Result<Business, Error> {
        return when (val result = firestoreRepository.getDocument("businesses", id, BusinessDto::class)) {
            is Result.Success -> Result.Success(result.data.toBusiness(id))
            is Result.Error -> result
        }
    }
    
    suspend fun addBusiness(business: BusinessDto): Result<String, Error> {
        return firestoreRepository.addDocument("businesses", business)
    }
    
    suspend fun updateBusiness(id: String, business: BusinessDto): Result<Unit, Error> {
        return firestoreRepository.updateDocument("businesses", id, business)
    }
    
    suspend fun deleteBusiness(id: String): Result<Unit, Error> {
        return firestoreRepository.deleteDocument("businesses", id)
    }
}
