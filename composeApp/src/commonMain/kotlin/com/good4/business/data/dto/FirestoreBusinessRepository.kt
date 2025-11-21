package com.good4.business.data.dto

import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.domain.Error
import com.good4.core.domain.Result

class FirestoreBusinessRepository(
    private val firestoreRepository: FirestoreRepository
) {
    suspend fun getBusinesses(): Result<List<BusinessDto>, Error> {
        return firestoreRepository.getCollection("businesses", BusinessDto::class)
    }
    
    suspend fun getBusinessById(id: String): Result<BusinessDto, Error> {
        return firestoreRepository.getDocument("businesses", id, BusinessDto::class)
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