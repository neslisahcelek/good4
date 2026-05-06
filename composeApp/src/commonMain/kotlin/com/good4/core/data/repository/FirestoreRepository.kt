package com.good4.core.data.repository

import com.good4.core.domain.Error
import com.good4.core.domain.Result
import kotlin.reflect.KClass

data class DocumentWithId<T>(
    val id: String,
    val data: T
)

/**
 * Çoklu `where` eşitliği veya `where` + `orderBy` + `limit` bileşik Firestore indeksi ister.
 * Tanımlar `firestore.indexes.json` içinde; Firebase projesinde deploy edilmiş olmalıdır.
 *
 * Özet (kod referansı):
 * - **codes** `businessId` + `createdAt` (desc) + limit — [com.good4.code.data.repository.CodeRepository.getRecentCodesByBusinessId]
 * - **codes** `userId` + `status` — [com.good4.code.data.repository.CodeRepository.getPendingCodeByUserId],
 *   [com.good4.code.data.repository.CodeRepository.getCodesByUserIdAndStatus] (limit + null olmayan orderBy eklenirse ek indeks gerekir)
 * - **orders** `businessId` + `createdAt` (desc) + limit — [com.good4.order.data.repository.OrderRepository.getRecentOrdersByBusiness]
 * - **orders** `businessId` + `status` — [com.good4.order.data.repository.OrderRepository.getOrdersByBusinessAndStatus]
 * - **orders** `code` + `businessId` + `status` — [com.good4.order.data.repository.OrderRepository.getOrderByCodeAndBusiness]
 */
interface FirestoreRepository {
    suspend fun <T : Any> addDocument(collectionPath: String, data: T): Result<String, Error>
    suspend fun <T : Any> getDocument(collectionPath: String, documentId: String, clazz: KClass<T>): Result<T, Error>
    suspend fun <T : Any> updateDocument(collectionPath: String, documentId: String, data: T): Result<Unit, Error>
    suspend fun updateFields(
        collectionPath: String,
        documentId: String,
        fields: Map<String, Any?>
    ): Result<Unit, Error>
    suspend fun deleteDocument(collectionPath: String, documentId: String): Result<Unit, Error>
    suspend fun <T : Any> getCollection(collectionPath: String, clazz: KClass<T>): Result<List<T>, Error>
    suspend fun <T : Any> getCollectionWithIds(collectionPath: String, clazz: KClass<T>): Result<List<DocumentWithId<T>>, Error>
    suspend fun <T : Any> queryCollectionWithIds(
        collectionPath: String,
        field: String,
        value: Any,
        clazz: KClass<T>
    ): Result<List<DocumentWithId<T>>, Error>
    
    suspend fun <T : Any> queryCollectionWithMultipleConditions(
        collectionPath: String,
        conditions: Map<String, Any>,
        clazz: KClass<T>
    ): Result<List<DocumentWithId<T>>, Error>

    suspend fun <T : Any> queryCollectionWithMultipleConditionsAndLimit(
        collectionPath: String,
        conditions: Map<String, Any>,
        orderByField: String?,
        descending: Boolean,
        limit: Long,
        clazz: KClass<T>
    ): Result<List<DocumentWithId<T>>, Error>
}
