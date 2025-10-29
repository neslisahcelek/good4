package com.good4.core.data.repository

import com.good4.core.domain.Result
import com.good4.core.domain.Error

interface FirestoreRepository {
    suspend fun <T : Any> addDocument(collectionPath: String, data: T): Result<String, Error>
    suspend fun <T : Any> getDocument(collectionPath: String, documentId: String, clazz: Class<T>): Result<T, Error>
    suspend fun <T : Any> updateDocument(collectionPath: String, documentId: String, data: T): Result<Unit, Error>
    suspend fun deleteDocument(collectionPath: String, documentId: String): Result<Unit, Error>
    suspend fun <T : Any> getCollection(collectionPath: String, clazz: Class<T>): Result<List<T>, Error>
}
