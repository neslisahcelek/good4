package com.good4.core.data.repository

import com.good4.core.domain.Error
import com.good4.core.domain.NetworkError
import com.good4.core.domain.Result
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.Query
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.where
import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass

class FirestoreRepositoryIOSImpl : FirestoreRepository {
    private val firestore = Firebase.firestore

    override suspend fun <T : Any> addDocument(collectionPath: String, data: T): Result<String, Error> {
        return try {
            val serializer = serializerForData(data)
            val documentReference = firestore.collection(collectionPath).add(serializer, data)
            Result.Success(documentReference.id)
        } catch (e: Exception) {
            Result.Error(NetworkError(e.message ?: "Unknown error"))
        }
    }

    override suspend fun <T : Any> getDocument(
        collectionPath: String,
        documentId: String,
        clazz: KClass<T>
    ): Result<T, Error> {
        return try {
            val documentSnapshot = firestore.collection(collectionPath)
                .document(documentId)
                .get()

            if (documentSnapshot.exists) {
                val serializer = serializerFor(clazz)
                val result = documentSnapshot.data(serializer)
                Result.Success(result)
            } else {
                Result.Error(NetworkError("Document not found"))
            }
        } catch (e: Exception) {
            Result.Error(NetworkError(e.message ?: "Unknown error"))
        }
    }

    override suspend fun <T : Any> updateDocument(
        collectionPath: String,
        documentId: String,
        data: T
    ): Result<Unit, Error> {
        return try {
            val serializer = serializerForData(data)
            firestore.collection(collectionPath)
                .document(documentId)
                .set(serializer, data)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(NetworkError(e.message ?: "Unknown error"))
        }
    }

    override suspend fun deleteDocument(collectionPath: String, documentId: String): Result<Unit, Error> {
        return try {
            firestore.collection(collectionPath)
                .document(documentId)
                .delete()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(NetworkError(e.message ?: "Unknown error"))
        }
    }

    override suspend fun <T : Any> getCollection(
        collectionPath: String,
        clazz: KClass<T>
    ): Result<List<T>, Error> {
        return try {
            val querySnapshot = firestore.collection(collectionPath).get()

            val serializer = serializerFor(clazz)
            val results = querySnapshot.documents.mapNotNull { document ->
                try {
                    document.data(serializer)
                } catch (e: Exception) {
                    println("FirestoreRepositoryIOSImpl: Error decoding document: ${e.message}")
                    null
                }
            }

            Result.Success(results)
        } catch (e: Exception) {
            Result.Error(NetworkError(e.message ?: "Unknown error"))
        }
    }

    override suspend fun <T : Any> getCollectionWithIds(
        collectionPath: String,
        clazz: KClass<T>
    ): Result<List<DocumentWithId<T>>, Error> {
        return try {
            val querySnapshot = firestore.collection(collectionPath).get()

            val serializer = serializerFor(clazz)
            val results = querySnapshot.documents.mapNotNull { document ->
                try {
                    val decoded = document.data(serializer)
                    DocumentWithId(id = document.id, data = decoded)
                } catch (e: Exception) {
                    println("FirestoreRepositoryIOSImpl: Error decoding document: ${e.message}")
                    null
                }
            }

            Result.Success(results)
        } catch (e: Exception) {
            Result.Error(NetworkError(e.message ?: "Unknown error"))
        }
    }

    override suspend fun <T : Any> queryCollectionWithIds(
        collectionPath: String,
        field: String,
        value: Any,
        clazz: KClass<T>
    ): Result<List<DocumentWithId<T>>, Error> {
        return try {
            val querySnapshot = firestore.collection(collectionPath)
                .where(field, equalTo = value)
                .get()

            val serializer = serializerFor(clazz)
            val results = querySnapshot.documents.mapNotNull { document ->
                try {
                    val decoded = document.data(serializer)
                    DocumentWithId(id = document.id, data = decoded)
                } catch (e: Exception) {
                    println("FirestoreRepositoryIOSImpl: Error decoding document: ${e.message}")
                    null
                }
            }

            Result.Success(results)
        } catch (e: Exception) {
            Result.Error(NetworkError(e.message ?: "Unknown error"))
        }
    }

    override suspend fun <T : Any> queryCollectionWithMultipleConditions(
        collectionPath: String,
        conditions: Map<String, Any>,
        clazz: KClass<T>
    ): Result<List<DocumentWithId<T>>, Error> {
        return try {
            var query: Query = firestore.collection(collectionPath)

            conditions.forEach { (field, value) ->
                query = query.where(field, equalTo = value)
            }

            val querySnapshot = query.get()

            val serializer = serializerFor(clazz)
            val results = querySnapshot.documents.mapNotNull { document ->
                try {
                    val decoded = document.data(serializer)
                    DocumentWithId(id = document.id, data = decoded)
                } catch (e: Exception) {
                    println("FirestoreRepositoryIOSImpl: Error decoding document: ${e.message}")
                    null
                }
            }

            Result.Success(results)
        } catch (e: Exception) {
            Result.Error(NetworkError(e.message ?: "Unknown error"))
        }
    }

    override suspend fun <T : Any> queryCollectionWithMultipleConditionsAndLimit(
        collectionPath: String,
        conditions: Map<String, Any>,
        orderByField: String?,
        descending: Boolean,
        limit: Long,
        clazz: KClass<T>
    ): Result<List<DocumentWithId<T>>, Error> {
        return try {
            var query: Query = firestore.collection(collectionPath)

            conditions.forEach { (field, value) ->
                query = query.where(field, equalTo = value)
            }

            if (orderByField != null) {
                val direction = if (descending) Direction.DESCENDING else Direction.ASCENDING
                query = query.orderBy(orderByField, direction)
            }

            val querySnapshot = query.limit(limit).get()

            val serializer = serializerFor(clazz)
            val results = querySnapshot.documents.mapNotNull { document ->
                try {
                    val decoded = document.data(serializer)
                    DocumentWithId(id = document.id, data = decoded)
                } catch (e: Exception) {
                    println("FirestoreRepositoryIOSImpl: Error decoding document: ${e.message}")
                    null
                }
            }

            Result.Success(results)
        } catch (e: Exception) {
            Result.Error(NetworkError(e.message ?: "Unknown error"))
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> serializerFor(clazz: KClass<T>): KSerializer<T> {
        return when (clazz.simpleName) {
            "ProductDto" -> com.good4.product.data.dto.ProductDto.serializer() as KSerializer<T>
            "BusinessDto" -> com.good4.business.data.dto.BusinessDto.serializer() as KSerializer<T>
            "CampaignDto" -> com.good4.campaign.data.dto.CampaignDto.serializer() as KSerializer<T>
            "CodeDto" -> com.good4.code.data.dto.CodeDto.serializer() as KSerializer<T>
            "UserDto" -> com.good4.user.data.dto.UserDto.serializer() as KSerializer<T>
            else -> throw IllegalArgumentException("No serializer found for ${clazz.simpleName}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> serializerForData(data: T): KSerializer<T> {
        return when (data) {
            is com.good4.product.data.dto.ProductDto -> com.good4.product.data.dto.ProductDto.serializer() as KSerializer<T>
            is com.good4.business.data.dto.BusinessDto -> com.good4.business.data.dto.BusinessDto.serializer() as KSerializer<T>
            is com.good4.campaign.data.dto.CampaignDto -> com.good4.campaign.data.dto.CampaignDto.serializer() as KSerializer<T>
            is com.good4.code.data.dto.CodeDto -> com.good4.code.data.dto.CodeDto.serializer() as KSerializer<T>
            is com.good4.user.data.dto.UserDto -> com.good4.user.data.dto.UserDto.serializer() as KSerializer<T>
            else -> throw IllegalArgumentException("No serializer found for ${data::class.simpleName}")
        }
    }

}
