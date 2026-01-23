package com.good4.core.data.repository

import com.good4.core.domain.Result
import com.good4.core.domain.Error
import com.good4.core.domain.NetworkError
import kotlin.reflect.KClass
import kotlinx.coroutines.delay
import kotlin.random.Random
import com.good4.core.util.Logger

class FirestoreRepositoryImpl : FirestoreRepository {
    override suspend fun <T : Any> addDocument(collectionPath: String, data: T): Result<String, Error> {
        delay(500)
        Logger.d("FirestoreRepositoryMock", "Added document to $collectionPath with data: $data")
        return Result.Success("mock_document_id_${Random.nextInt()}")
    }

    override suspend fun <T : Any> getDocument(collectionPath: String, documentId: String, clazz: KClass<T>): Result<T, Error> {
        delay(500)
        Logger.d("FirestoreRepositoryMock", "Getting document $documentId from $collectionPath")
        return Result.Error(NetworkError("Not implemented for mock"))
    }

    override suspend fun <T : Any> updateDocument(collectionPath: String, documentId: String, data: T): Result<Unit, Error> {
        delay(500)
        Logger.d("FirestoreRepositoryMock", "Updated document $documentId in $collectionPath with data: $data")
        return Result.Success(Unit)
    }

    override suspend fun deleteDocument(collectionPath: String, documentId: String): Result<Unit, Error> {
        delay(500)
        Logger.d("FirestoreRepositoryMock", "Deleted document $documentId from $collectionPath")
        return Result.Success(Unit)
    }

    override suspend fun <T : Any> getCollection(collectionPath: String, clazz: KClass<T>): Result<List<T>, Error> {
        delay(500)
        Logger.d("FirestoreRepositoryMock", "Getting collection $collectionPath")
        return Result.Error(NetworkError("Not implemented for mock"))
    }
    
    override suspend fun <T : Any> getCollectionWithIds(collectionPath: String, clazz: KClass<T>): Result<List<DocumentWithId<T>>, Error> {
        delay(500)
        return Result.Error(NetworkError("Not implemented for mock"))
    }
    
    override suspend fun <T : Any> queryCollectionWithIds(
        collectionPath: String,
        field: String,
        value: Any,
        clazz: KClass<T>
    ): Result<List<DocumentWithId<T>>, Error> {
        delay(500)
        return Result.Error(NetworkError("Not implemented for mock"))
    }
    
    override suspend fun <T : Any> queryCollectionWithMultipleConditions(
        collectionPath: String,
        conditions: Map<String, Any>,
        clazz: KClass<T>
    ): Result<List<DocumentWithId<T>>, Error> {
        delay(500)
        return Result.Error(NetworkError("Not implemented for mock"))
    }

    override suspend fun <T : Any> queryCollectionWithMultipleConditionsAndLimit(
        collectionPath: String,
        conditions: Map<String, Any>,
        orderByField: String?,
        descending: Boolean,
        limit: Long,
        clazz: KClass<T>
    ): Result<List<DocumentWithId<T>>, Error> {
        delay(500)
        return Result.Error(NetworkError("Not implemented for mock"))
    }
}
