package com.good4.core.data.repository.android

import com.good4.core.data.repository.DocumentWithId
import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.domain.Error
import com.good4.core.domain.NetworkError
import com.good4.core.domain.Result
import com.good4.core.util.FirebaseDebugLogger
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.reflect.KClass

class FirestoreRepositoryAndroidImpl(
    private val firestore: FirebaseFirestore
) : FirestoreRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    override suspend fun <T : Any> addDocument(
        collectionPath: String,
        data: T
    ): Result<String, Error> {
        FirebaseDebugLogger.request(
            operation = "addDocument",
            path = collectionPath,
            detail = "dataType=${data::class.simpleName}"
        )
        return try {
            val convertedData = convertInstantToTimestamp(data)
            val documentReference = firestore.collection(collectionPath).add(convertedData).await()
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

    private fun convertInstantToTimestamp(obj: Any): Any {
        return when (obj) {
            is kotlinx.datetime.Instant -> {
                com.google.firebase.Timestamp(obj.epochSeconds, obj.nanosecondsOfSecond)
            }

            is Map<*, *> -> {
                obj.mapValues { (_, value) ->
                    if (value != null) convertInstantToTimestamp(value) else null
                }
            }

            is List<*> -> {
                obj.map { if (it != null) convertInstantToTimestamp(it) else null }
            }

            else -> obj
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
                .await()

            if (documentSnapshot.exists()) {
                val data: Map<String, Any?> = documentSnapshot.data ?: emptyMap()
                val jsonString = convertMapToJsonString(data)
                val result = decodeFromJsonString<T>(jsonString, clazz)
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
            val convertedData = convertInstantToTimestamp(data)
            firestore.collection(collectionPath)
                .document(documentId)
                .set(convertedData)
                .await()
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

    override suspend fun deleteDocument(
        collectionPath: String,
        documentId: String
    ): Result<Unit, Error> {
        FirebaseDebugLogger.request(
            operation = "deleteDocument",
            path = collectionPath,
            detail = "documentId=$documentId"
        )
        return try {
            firestore.collection(collectionPath)
                .document(documentId)
                .delete()
                .await()
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
            val querySnapshot = firestore.collection(collectionPath).get().await()

            val results = querySnapshot.documents.mapNotNull { document ->
                try {
                    val data: Map<String, Any?> = document.data ?: emptyMap()
                    val jsonString = convertMapToJsonString(data)
                    decodeFromJsonString(jsonString, clazz)
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
            val querySnapshot = firestore.collection(collectionPath).get().await()

            val results = querySnapshot.documents.mapNotNull { document ->
                try {
                    val data: Map<String, Any?> = document.data ?: emptyMap()
                    val jsonString = convertMapToJsonString(data)
                    val decoded = decodeFromJsonString(jsonString, clazz)
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
                .whereEqualTo(field, value)
                .get()
                .await()

            val results = querySnapshot.documents.mapNotNull { document ->
                try {
                    val data: Map<String, Any?> = document.data ?: emptyMap()
                    val jsonString = convertMapToJsonString(data)
                    val decoded = decodeFromJsonString(jsonString, clazz)
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
            var query = firestore.collection(collectionPath) as com.google.firebase.firestore.Query

            conditions.forEach { (field, value) ->
                query = query.whereEqualTo(field, value)
            }

            val querySnapshot = query.get().await()

            val results = querySnapshot.documents.mapNotNull { document ->
                try {
                    val data: Map<String, Any?> = document.data ?: emptyMap()
                    val jsonString = convertMapToJsonString(data)
                    val decoded = decodeFromJsonString(jsonString, clazz)
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
            var query = firestore.collection(collectionPath) as Query

            conditions.forEach { (field, value) ->
                query = query.whereEqualTo(field, value)
            }

            if (orderByField != null) {
                val direction = if (descending) {
                    Query.Direction.DESCENDING
                } else {
                    Query.Direction.ASCENDING
                }
                query = query.orderBy(orderByField, direction)
            }

            val querySnapshot = query.limit(limit).get().await()

            val results = querySnapshot.documents.mapNotNull { document ->
                try {
                    val data: Map<String, Any?> = document.data ?: emptyMap()
                    val jsonString = convertMapToJsonString(data)
                    val decoded = decodeFromJsonString(jsonString, clazz)
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
    private inline fun <reified T : Any> decodeFromJsonString(jsonString: String): T {
        return json.decodeFromString<T>(jsonString)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> decodeFromJsonString(jsonString: String, clazz: KClass<T>): T {
        return when (clazz.simpleName) {
            "ProductDto" -> json.decodeFromString<com.good4.product.data.dto.ProductDto>(jsonString) as T
            "BusinessDto" -> json.decodeFromString<com.good4.business.data.dto.BusinessDto>(
                jsonString
            ) as T

            "CampaignDto" -> json.decodeFromString<com.good4.campaign.data.dto.CampaignDto>(
                jsonString
            ) as T

            "CodeDto" -> json.decodeFromString<com.good4.code.data.dto.CodeDto>(jsonString) as T
            "UserDto" -> json.decodeFromString<com.good4.user.data.dto.UserDto>(jsonString) as T
            "AppConfigDto" -> json.decodeFromString<com.good4.config.data.dto.AppConfigDto>(jsonString) as T
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
            is com.google.firebase.Timestamp -> {
                val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(value.toDate().time)
                JsonPrimitive(instant.toString())
            }

            is java.util.Date -> {
                val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(value.time)
                JsonPrimitive(instant.toString())
            }

            is Map<*, *> -> {
                if (value.containsKey("epochSeconds") || value.containsKey("value\$kotlinx_datetime")) {
                    try {
                        val epochSeconds = (value["epochSeconds"] as? Number)?.toLong()
                        val nanos = (value["nanosecondsOfSecond"] as? Number)?.toInt() ?: 0

                        if (epochSeconds != null) {
                            val instant = kotlinx.datetime.Instant.fromEpochSeconds(epochSeconds, nanos)
                            return JsonPrimitive(instant.toString())
                        }
                    } catch (e: Exception) { }
                }

                buildJsonObject {
                    value.forEach { (k, v) ->
                        put(k as String, convertValue(v))
                    }
                }
            }

            is List<*> -> {
                JsonArray(value.map { convertValue(it) })
            }

            null -> JsonNull
            else -> JsonPrimitive(value.toString())
        }
    }

    private fun toFirestoreMap(jsonObject: JsonObject): Map<String, Any?> {
        return jsonObject.entries.associate { (key, value) ->
            key to when (value) {
                is JsonObject -> toFirestoreMap(value)
                is JsonPrimitive -> {
                    when {
                        value.isString -> value.content
                        else -> {
                            value.content.toLongOrNull()
                                ?: value.content.toDoubleOrNull()
                                ?: value.content.toBooleanStrictOrNull()
                                ?: value.content
                        }
                    }
                }

                else -> value.toString()
            }
        }
    }
}
