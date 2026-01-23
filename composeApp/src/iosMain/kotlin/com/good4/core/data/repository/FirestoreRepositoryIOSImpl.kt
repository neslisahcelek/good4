package com.good4.core.data.repository

import com.good4.core.domain.Error
import com.good4.core.domain.NetworkError
import com.good4.core.domain.Result
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.firestore
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.reflect.KClass

class FirestoreRepositoryIOSImpl : FirestoreRepository {
    private val firestore = Firebase.firestore

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    override suspend fun <T : Any> addDocument(collectionPath: String, data: T): Result<String, Error> {
        return try {
            val documentReference = firestore.collection(collectionPath).add(data)
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
                val data: Map<String, Any?> = documentSnapshot.data() ?: emptyMap()
                val jsonString = convertMapToJsonString(data)
                val result = decodeFromJsonString(jsonString, clazz)
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
            firestore.collection(collectionPath)
                .document(documentId)
                .set(data)
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

            val results = querySnapshot.documents.mapNotNull { document ->
                try {
                    val data: Map<String, Any?> = document.data() ?: emptyMap()
                    val jsonString = convertMapToJsonString(data)
                    decodeFromJsonString(jsonString, clazz)
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

            val results = querySnapshot.documents.mapNotNull { document ->
                try {
                    val data: Map<String, Any?> = document.data() ?: emptyMap()
                    val jsonString = convertMapToJsonString(data)
                    val decoded = decodeFromJsonString(jsonString, clazz)
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
                .whereEqualTo(field, value)
                .get()

            val results = querySnapshot.documents.mapNotNull { document ->
                try {
                    val data: Map<String, Any?> = document.data() ?: emptyMap()
                    val jsonString = convertMapToJsonString(data)
                    val decoded = decodeFromJsonString(jsonString, clazz)
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
            var query = firestore.collection(collectionPath)

            conditions.forEach { (field, value) ->
                query = query.whereEqualTo(field, value)
            }

            val querySnapshot = query.get()

            val results = querySnapshot.documents.mapNotNull { document ->
                try {
                    val data: Map<String, Any?> = document.data() ?: emptyMap()
                    val jsonString = convertMapToJsonString(data)
                    val decoded = decodeFromJsonString(jsonString, clazz)
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
            var query = firestore.collection(collectionPath)

            conditions.forEach { (field, value) ->
                query = query.whereEqualTo(field, value)
            }

            if (orderByField != null) {
                val direction = if (descending) Direction.DESCENDING else Direction.ASCENDING
                query = query.orderBy(orderByField, direction)
            }

            val querySnapshot = query.limit(limit).get()

            val results = querySnapshot.documents.mapNotNull { document ->
                try {
                    val data: Map<String, Any?> = document.data() ?: emptyMap()
                    val jsonString = convertMapToJsonString(data)
                    val decoded = decodeFromJsonString(jsonString, clazz)
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
    private fun <T : Any> decodeFromJsonString(jsonString: String, clazz: KClass<T>): T {
        return when (clazz.simpleName) {
            "ProductDto" -> json.decodeFromString<com.good4.product.data.dto.ProductDto>(jsonString) as T
            "BusinessDto" -> json.decodeFromString<com.good4.business.data.dto.BusinessDto>(jsonString) as T
            "CampaignDto" -> json.decodeFromString<com.good4.campaign.data.dto.CampaignDto>(jsonString) as T
            "CodeDto" -> json.decodeFromString<com.good4.code.data.dto.CodeDto>(jsonString) as T
            "UserDto" -> json.decodeFromString<com.good4.user.data.dto.UserDto>(jsonString) as T
            "ReservationDto" -> json.decodeFromString<com.good4.reservation.data.dto.ReservationDto>(jsonString) as T
            else -> throw IllegalArgumentException("No serializer found for ${clazz.simpleName}")
        }
    }

    private fun convertMapToJsonString(map: Map<String, Any?>): String {
        val jsonObject = buildJsonObject {
            map.forEach { (key, value) ->
                put(key, convertValue(value))
            }
        }
        return jsonObject.toString()
    }

    private fun convertValue(value: Any?): kotlinx.serialization.json.JsonElement {
        return when (value) {
            is String -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                val map = value as Map<String, Any?>
                buildJsonObject {
                    map.forEach { (k, v) ->
                        put(k, convertValue(v))
                    }
                }
            }
            is List<*> -> {
                JsonArray(value.map { convertValue(it) })
            }
            null -> JsonNull
            else -> {
                JsonPrimitive(value.toString())
            }
        }
    }
}
