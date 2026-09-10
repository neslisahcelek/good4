package com.good4.dining.data.repository

import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.domain.Error
import com.good4.core.domain.Result
import com.good4.dining.data.dto.AkdenizDiningMenuDto
import com.good4.dining.domain.AkdenizDiningMenu
import com.good4.dining.domain.AkdenizDiningMenuDay

class AkdenizDiningMenuRepository(
    private val firestoreRepository: FirestoreRepository
) {
    suspend fun getCurrentMenu(): Result<AkdenizDiningMenu, Error> {
        return when (
            val result = firestoreRepository.getDocument(
                collectionPath = COLLECTION_PATH,
                documentId = DOCUMENT_ID,
                clazz = AkdenizDiningMenuDto::class
            )
        ) {
            is Result.Success -> Result.Success(result.data.toDomain())
            is Result.Error -> Result.Error(result.error)
        }
    }

    private fun AkdenizDiningMenuDto.toDomain(): AkdenizDiningMenu {
        return AkdenizDiningMenu(
            weekLabel = weekLabel.orEmpty(),
            weekStart = weekStart.orEmpty(),
            weekEnd = weekEnd.orEmpty(),
            days = days.mapNotNull { day ->
                val date = day.date.orEmpty()
                val dayName = day.dayName.orEmpty()
                if (date.isBlank() || dayName.isBlank() || day.meals.isEmpty()) {
                    null
                } else {
                    AkdenizDiningMenuDay(
                        date = date,
                        dayName = dayName,
                        meals = day.meals.filter { it.isNotBlank() },
                        calories = day.calories
                    )
                }
            }
        )
    }

    private companion object {
        const val COLLECTION_PATH = "app_config"
        const val DOCUMENT_ID = "akdeniz_dining_menu"
    }
}
