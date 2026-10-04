package com.good4.dining.data.repository

import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.domain.Result
import com.good4.dining.data.dto.KykMenuDayDto
import com.good4.dining.domain.KykMenuDay
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

class KykMenuRepository(
    private val firestoreRepository: FirestoreRepository
) {
    /** The menu for [date] (yyyy-MM-dd), or null when that day was not published. */
    private val caches = mutableMapOf<String, com.good4.core.data.repository.ReadCache<KykMenuDay?>>()
    suspend fun getDay(date: String): KykMenuDay? {
        if (caches.size > 32) caches.clear()
        return caches.getOrPut(date) { com.good4.core.data.repository.ReadCache(3600) }.load { loadDay(date) }
    }
    private suspend fun loadDay(date: String): KykMenuDay? {
        val result = firestoreRepository.getDocument(
            collectionPath = "kyk_menu_days",
            documentId = date,
            clazz = KykMenuDayDto::class
        )
        val dto = (result as? Result.Success)?.data ?: return null
        val breakfast = dto.breakfast.filter { it.isNotBlank() }
        val dinner = dto.dinner.filter { it.isNotBlank() }
        if (breakfast.isEmpty() && dinner.isEmpty()) return null
        return KykMenuDay(date = date, breakfast = breakfast, dinner = dinner)
    }

    /**
     * Today's menu, or the next published day within [lookAheadDays] (e.g. before a new month's list
     * starts). Days are read one by one because clients may not list the collection.
     */
    suspend fun getDayOrNext(date: String, lookAheadDays: Int = 3): KykMenuDay? {
        val start = runCatching { LocalDate.parse(date) }.getOrNull() ?: return getDay(date)
        for (offset in 0..lookAheadDays) {
            getDay(start.plus(DatePeriod(days = offset)).toString())?.let { return it }
        }
        return null
    }
}
