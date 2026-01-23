package com.good4.campaign.data.repository

import com.good4.campaign.data.dto.CampaignDto
import com.good4.campaign.domain.Campaign
import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.domain.Error
import com.good4.core.domain.Result

class CampaignRepository(
    private val firestoreRepository: FirestoreRepository
) {
    suspend fun getCampaigns(): Result<List<Campaign>, Error> {
        return when (val result = firestoreRepository.getCollectionWithIds("campaigns", CampaignDto::class)) {
            is Result.Success -> {
                val campaigns = result.data.mapNotNull { documentWithId ->
                    val dto = documentWithId.data
                    if (dto.image != null) {
                        Campaign(
                            id = documentWithId.id,
                            imageUrl = dto.image
                        )
                    } else null
                }
                Result.Success(campaigns)
            }
            is Result.Error -> result
        }
    }

    suspend fun addCampaign(campaignDto: CampaignDto): Result<String, Error> {
        return firestoreRepository.addDocument("campaigns", campaignDto)
    }

    suspend fun deleteCampaign(campaignId: String): Result<Unit, Error> {
        return firestoreRepository.deleteDocument("campaigns", campaignId)
    }
}

