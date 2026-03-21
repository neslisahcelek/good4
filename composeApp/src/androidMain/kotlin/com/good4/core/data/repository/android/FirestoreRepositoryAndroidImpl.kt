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
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.serializer
import kotlinx.datetime.Instant
import kotlin.reflect.KClass

class FirestoreRepositoryAndroidImpl(
    private val firestore: FirebaseFirestore
) : FirestoreRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val timestampFieldNames = setOf(
        "createdAt", "expiresAt", "usedAt", "lastCreditResetAt", "registrationDate",
        "startDate", "endDate"
    )

    // --- Write helpers ---

    private fun encodeToFirestoreMap(data: Any): Map<String, Any?> {
        val jsonString = encodeToJsonString(data)
        val jsonObject = json.parseToJsonElement(jsonString) as JsonObject
        return jsonObject.entries.associate { (key, element) ->
            key to encodeElementToFirestoreValue(key, element)
        }
    }

    private fun encodeElementToFirestoreValue(fieldName: String, element: JsonElement): Any? {
        return when {
            element is JsonNull -> null
            fieldName in timestampFieldNames && element is JsonPrimitive && !element.isString -> {
                val epochSecs = element.content.toLongOrNull()
                if (epochSecs != null) {
                    com.google.firebase.Timestamp(epochSecs, 0)
                } else null
            }
            element is JsonPrimitive -> when {
                element.isString -> element.content
                element.content == "true" || element.content == "false" ->
                    element.content.toBooleanStrict()
                else ->
                    element.content.toLongOrNull()
                        ?: element.content.toDoubleOrNull()
                        ?: element.content
            }
            element is JsonObject -> element.entries.associate { (k, v) ->
                k to encodeElementToFirestoreValue(k, v)
            }
            element is JsonArray -> element.map { encodeElementToFirestoreValue(fieldName, it) }
            else -> null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun encodeToJsonString(data: Any): String {
        val ser = serializer(data::class.java) as KSerializer<Any>
        return json.encodeToString(ser, data)
    }

    // --- Read helpers ---

    private fun convertMapToJsonString(map: Map<String, Any?>): String {
        val jsonObject = buildJsonObject {
            map.forEach { (key, value) ->
                put(key, convertValue(key, value))
            }
        }
        return jsonObject.toString()
    }

    private fun convertValue(fieldName: String, value: Any?): JsonElement {
        return when (value) {
            is String -> {
                if (fieldName in timestampFieldNames) {
                    val epochSeconds = value.toLongOrNull() ?: value.toEpochSecondsOrNull()
                    if (epochSeconds != null) JsonPrimitive(epochSeconds) else JsonPrimitive(value)
                } else {
                    JsonPrimitive(value)
                }
            }
            is Number -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is com.google.firebase.Timestamp -> {
                JsonPrimitive(value.seconds)
            }
            is java.util.Date -> {
                JsonPrimitive(value.time / 1000L)
            }
            is Map<*, *> -> {
                // Handle old Instant-as-map format stored by previous Android versions
                if (value.containsKey("epochSeconds") || value.containsKey("value\$kotlinx_datetime")) {
                    try {
                        val epochSeconds = (value["epochSeconds"] as? Number)?.toLong()
                            ?: ((value["value\$kotlinx_datetime"] as? Map<*, *>)
                                ?.get("epochSecond") as? Number)
                                ?.toLong()
                        if (epochSeconds != null) {
                            return JsonPrimitive(epochSeconds)
                        }
                    } catch (e: Exception) { }
                }

                buildJsonObject {
                    value.forEach { (k, v) ->
                        put(k as String, convertValue(k, v))
                    }
                }
            }
            is List<*> -> {
                JsonArray(value.map { convertValue(fieldName, it) })
            }
            null -> JsonNull
            else -> JsonPrimitive(value.toString())
        }
    }

    private fun String.toEpochSecondsOrNull(): Long? {
        return try {
            Instant.parse(this).epochSeconds
        } catch (_: Exception) {
            null
        }
    }

    // --- Decode helpers ---

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> decodeFromJsonString(jsonString: String, clazz: KClass<T>): T {
        val ser = serializer(clazz.java) as KSerializer<T>
        return json.decodeFromString(ser, jsonString)
    }

    // --- FirestoreRepository implementation ---

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
            val firestoreMap = encodeToFirestoreMap(data)
            val documentReference = firestore.collection(collectionPath).add(firestoreMap).await()
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
        if (documentId.isBlank()) {
            FirebaseDebugLogger.error(
                operation = "getDocument",
                path = collectionPath,
                detail = "documentId is empty"
            )
            return Result.Error(NetworkError("Document path cannot be empty"))
        }
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
        if (documentId.isBlank()) {
            FirebaseDebugLogger.error(
                operation = "updateDocument",
                path = collectionPath,
                detail = "documentId is empty"
            )
            return Result.Error(NetworkError("Document path cannot be empty"))
        }
        FirebaseDebugLogger.request(
            operation = "updateDocument",
            path = collectionPath,
            detail = "documentId=$documentId, dataType=${data::class.simpleName}"
        )
        return try {
            val firestoreMap = encodeToFirestoreMap(data)
            firestore.collection(collectionPath)
                .document(documentId)
                .set(firestoreMap)
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
        if (documentId.isBlank()) {
            FirebaseDebugLogger.error(
                operation = "deleteDocument",
                path = collectionPath,
                detail = "documentId is empty"
            )
            return Result.Error(NetworkError("Document path cannot be empty"))
        }
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
            var query = firestore.collection(collectionPath) as Query

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
}
