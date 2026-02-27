package com.good4.core.data.repository

import com.good4.core.domain.Error
import com.good4.core.domain.NetworkError
import com.good4.core.domain.Result
import com.good4.core.util.FirebaseDebugLogger
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
        FirebaseDebugLogger.request(
            operation = "addDocument",
            path = collectionPath,
            detail = "dataType=${data::class.simpleName}"
        )
        return try {
            val serializer = serializerForData(data)
            val documentReference = firestore.collection(collectionPath).add(serializer, data)
            FirebaseDebugLogger.success(
                operation = "addDocument",
                path = collectionPath,
                detail = "documentId=${documentReference.id}"
            )
            Result.Success(documentReference.id)
        } catch (e: Exception) {
            FirebaseDebugLogger.error(operation = "addDocument", path = collectionPath, throwable = e)
            Result.Error(NetworkError(e.message ?: "Unknown error"))
        }
    }

    override suspend fun <T : Any> getDocument(
        collectionPath: String,
        documentId: String,
        clazz: KClass<T>
    ): Result<T, Error> {
        FirebaseDebugLogger.request(
            operation = "getDocument",
            path = collectionPath,
            detail = "documentId=$documentId, type=${clazz.simpleName}"
        )
        return try {
            val documentSnapshot = firestore.collection(collectionPath)
                .document(documentId)
                .get()

            if (documentSnapshot.exists) {
                val serializer = serializerFor(clazz)
                val result = documentSnapshot.data(serializer)
                FirebaseDebugLogger.success(
                    operation = "getDocument",
                    path = collectionPath,
                    detail = "documentId=$documentId, data=$result"
                )
                Result.Success(result)
            } else {
                FirebaseDebugLogger.error(
                    operation = "getDocument",
                    path = collectionPath,
                    detail = "documentId=$documentId not found"
                )
                Result.Error(NetworkError("Document not found"))
            }
        } catch (e: Exception) {
            FirebaseDebugLogger.error(
                operation = "getDocument",
                path = collectionPath,
                throwable = e,
                detail = "documentId=$documentId"
            )
            Result.Error(NetworkError(e.message ?: "Unknown error"))
        }
    }

    override suspend fun <T : Any> updateDocument(
        collectionPath: String,
        documentId: String,
        data: T
    ): Result<Unit, Error> {
        FirebaseDebugLogger.request(
            operation = "updateDocument",
            path = collectionPath,
            detail = "documentId=$documentId, dataType=${data::class.simpleName}"
        )
        return try {
            val serializer = serializerForData(data)
            firestore.collection(collectionPath)
                .document(documentId)
                .set(serializer, data)
            FirebaseDebugLogger.success(
                operation = "updateDocument",
                path = collectionPath,
                detail = "documentId=$documentId"
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            FirebaseDebugLogger.error(
                operation = "updateDocument",
                path = collectionPath,
                throwable = e,
                detail = "documentId=$documentId"
            )
            Result.Error(NetworkError(e.message ?: "Unknown error"))
        }
    }

    override suspend fun deleteDocument(collectionPath: String, documentId: String): Result<Unit, Error> {
        FirebaseDebugLogger.request(
            operation = "deleteDocument",
            path = collectionPath,
            detail = "documentId=$documentId"
        )
        return try {
            firestore.collection(collectionPath)
                .document(documentId)
                .delete()
            FirebaseDebugLogger.success(
                operation = "deleteDocument",
                path = collectionPath,
                detail = "documentId=$documentId"
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            FirebaseDebugLogger.error(
                operation = "deleteDocument",
                path = collectionPath,
                throwable = e,
                detail = "documentId=$documentId"
            )
            Result.Error(NetworkError(e.message ?: "Unknown error"))
        }
    }

    override suspend fun <T : Any> getCollection(
        collectionPath: String,
        clazz: KClass<T>
    ): Result<List<T>, Error> {
        FirebaseDebugLogger.request(
            operation = "getCollection",
            path = collectionPath,
            detail = "type=${clazz.simpleName}"
        )
        return try {
            val querySnapshot = firestore.collection(collectionPath).get()

            val serializer = serializerFor(clazz)
            val results = querySnapshot.documents.mapNotNull { document ->
                try {
                    document.data(serializer)
                } catch (e: Exception) {
                    FirebaseDebugLogger.error(
                        operation = "getCollectionDecode",
                        path = collectionPath,
                        throwable = e,
                        detail = "documentId=${document.id}, type=${clazz.simpleName}"
                    )
                    null
                }
            }
            val idsPreview = querySnapshot.documents.take(5).joinToString(",") { it.id }

            FirebaseDebugLogger.success(
                operation = "getCollection",
                path = collectionPath,
                detail = "count=${results.size}, ids=$idsPreview"
            )
            Result.Success(results)
        } catch (e: Exception) {
            FirebaseDebugLogger.error(
                operation = "getCollection",
                path = collectionPath,
                throwable = e,
                detail = "type=${clazz.simpleName}"
            )
            Result.Error(NetworkError(e.message ?: "Unknown error"))
        }
    }

    override suspend fun <T : Any> getCollectionWithIds(
        collectionPath: String,
        clazz: KClass<T>
    ): Result<List<DocumentWithId<T>>, Error> {
        FirebaseDebugLogger.request(
            operation = "getCollectionWithIds",
            path = collectionPath,
            detail = "type=${clazz.simpleName}"
        )
        return try {
            val querySnapshot = firestore.collection(collectionPath).get()

            val serializer = serializerFor(clazz)
            val results = querySnapshot.documents.mapNotNull { document ->
                try {
                    val decoded = document.data(serializer)
                    DocumentWithId(id = document.id, data = decoded)
                } catch (e: Exception) {
                    FirebaseDebugLogger.error(
                        operation = "getCollectionWithIdsDecode",
                        path = collectionPath,
                        throwable = e,
                        detail = "documentId=${document.id}, type=${clazz.simpleName}"
                    )
                    null
                }
            }
            val idsPreview = querySnapshot.documents.take(5).joinToString(",") { it.id }

            FirebaseDebugLogger.success(
                operation = "getCollectionWithIds",
                path = collectionPath,
                detail = "count=${results.size}, ids=$idsPreview"
            )
            Result.Success(results)
        } catch (e: Exception) {
            FirebaseDebugLogger.error(
                operation = "getCollectionWithIds",
                path = collectionPath,
                throwable = e,
                detail = "type=${clazz.simpleName}"
            )
            Result.Error(NetworkError(e.message ?: "Unknown error"))
        }
    }

    override suspend fun <T : Any> queryCollectionWithIds(
        collectionPath: String,
        field: String,
        value: Any,
        clazz: KClass<T>
    ): Result<List<DocumentWithId<T>>, Error> {
        FirebaseDebugLogger.request(
            operation = "queryCollectionWithIds",
            path = collectionPath,
            detail = "field=$field, value=$value, type=${clazz.simpleName}"
        )
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
                    FirebaseDebugLogger.error(
                        operation = "queryCollectionWithIdsDecode",
                        path = collectionPath,
                        throwable = e,
                        detail = "documentId=${document.id}, type=${clazz.simpleName}"
                    )
                    null
                }
            }
            val idsPreview = querySnapshot.documents.take(5).joinToString(",") { it.id }

            FirebaseDebugLogger.success(
                operation = "queryCollectionWithIds",
                path = collectionPath,
                detail = "count=${results.size}, ids=$idsPreview"
            )
            Result.Success(results)
        } catch (e: Exception) {
            FirebaseDebugLogger.error(
                operation = "queryCollectionWithIds",
                path = collectionPath,
                throwable = e,
                detail = "field=$field, value=$value, type=${clazz.simpleName}"
            )
            Result.Error(NetworkError(e.message ?: "Unknown error"))
        }
    }

    override suspend fun <T : Any> queryCollectionWithMultipleConditions(
        collectionPath: String,
        conditions: Map<String, Any>,
        clazz: KClass<T>
    ): Result<List<DocumentWithId<T>>, Error> {
        FirebaseDebugLogger.request(
            operation = "queryCollectionWithMultipleConditions",
            path = collectionPath,
            detail = "conditions=$conditions, type=${clazz.simpleName}"
        )
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
                    FirebaseDebugLogger.error(
                        operation = "queryCollectionWithMultipleConditionsDecode",
                        path = collectionPath,
                        throwable = e,
                        detail = "documentId=${document.id}, type=${clazz.simpleName}"
                    )
                    null
                }
            }
            val idsPreview = querySnapshot.documents.take(5).joinToString(",") { it.id }

            FirebaseDebugLogger.success(
                operation = "queryCollectionWithMultipleConditions",
                path = collectionPath,
                detail = "count=${results.size}, ids=$idsPreview"
            )
            Result.Success(results)
        } catch (e: Exception) {
            FirebaseDebugLogger.error(
                operation = "queryCollectionWithMultipleConditions",
                path = collectionPath,
                throwable = e,
                detail = "conditions=$conditions, type=${clazz.simpleName}"
            )
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
        FirebaseDebugLogger.request(
            operation = "queryCollectionWithMultipleConditionsAndLimit",
            path = collectionPath,
            detail = "conditions=$conditions, orderBy=$orderByField, desc=$descending, limit=$limit, type=${clazz.simpleName}"
        )
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
                    FirebaseDebugLogger.error(
                        operation = "queryCollectionWithMultipleConditionsAndLimitDecode",
                        path = collectionPath,
                        throwable = e,
                        detail = "documentId=${document.id}, type=${clazz.simpleName}"
                    )
                    null
                }
            }
            val idsPreview = querySnapshot.documents.take(5).joinToString(",") { it.id }

            FirebaseDebugLogger.success(
                operation = "queryCollectionWithMultipleConditionsAndLimit",
                path = collectionPath,
                detail = "count=${results.size}, ids=$idsPreview"
            )
            Result.Success(results)
        } catch (e: Exception) {
            FirebaseDebugLogger.error(
                operation = "queryCollectionWithMultipleConditionsAndLimit",
                path = collectionPath,
                throwable = e,
                detail = "conditions=$conditions, orderBy=$orderByField, desc=$descending, limit=$limit, type=${clazz.simpleName}"
            )
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
