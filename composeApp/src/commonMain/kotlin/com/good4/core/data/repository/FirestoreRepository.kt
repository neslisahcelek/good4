package com.good4.core.data.repository

import com.good4.core.domain.Result
import com.good4.core.domain.Error
import kotlin.reflect.KClass

data class DocumentWithId<T>(
    val id: String,
    val data: T
)

interface FirestoreRepository {
    suspend fun <T : Any> addDocument(collectionPath: String, data: T): Result<String, Error>
    suspend fun <T : Any> getDocument(collectionPath: String, documentId: String, clazz: KClass<T>): Result<T, Error>
    suspend fun <T : Any> updateDocument(collectionPath: String, documentId: String, data: T): Result<Unit, Error>
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
}
