package com.good4.core.data.repository.android

import com.good4.core.data.repository.DocumentWithId
import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.domain.Error
import com.good4.core.domain.NetworkError
import com.good4.core.domain.Result
import com.google.firebase.firestore.FirebaseFirestore
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
    
    override suspend fun <T : Any> addDocument(collectionPath: String, data: T): Result<String, Error> {
        return try {
            val documentReference = firestore.collection(collectionPath).add(data).await()
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
                .await()
            
            if (documentSnapshot.exists()) {
                val data: Map<String, Any?> = documentSnapshot.data ?: emptyMap()
                val jsonString = convertMapToJsonString(data)
                val result = decodeFromJsonString<T>(jsonString, clazz)
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
                .await()
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
                .await()
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
            val querySnapshot = firestore.collection(collectionPath)
                .get()
                .await()

            val results = querySnapshot.documents.mapNotNull { document ->
                try {
                    val data: Map<String, Any?> = document.data ?: emptyMap()
                    println("FirestoreRepositoryAndroidImpl: Document data: $data")
                    val jsonString = convertMapToJsonString(data)
                    println("FirestoreRepositoryAndroidImpl: JSON string: $jsonString")
                    val decoded = decodeFromJsonString(jsonString, clazz)
                    println("FirestoreRepositoryAndroidImpl: Decoded successfully")
                    decoded
                } catch (e: Exception) {
                    println("FirestoreRepositoryAndroidImpl: Error decoding document: ${e.message}")
                    e.printStackTrace()
                    null
                }
            }
            
            println("FirestoreRepositoryAndroidImpl: Successfully decoded ${results.size} items")
            Result.Success(results)
        } catch (e: Exception) {
            println("FirestoreRepositoryAndroidImpl: Error getting collection: ${e.message}")
            e.printStackTrace()
            Result.Error(NetworkError(e.message ?: "Unknown error"))
        }
    }
    
    override suspend fun <T : Any> getCollectionWithIds(
        collectionPath: String, 
        clazz: KClass<T>
    ): Result<List<DocumentWithId<T>>, Error> {
        return try {
            val querySnapshot = firestore.collection(collectionPath)
                .get()
                .await()

            val results = querySnapshot.documents.mapNotNull { document ->
                try {
                    val data: Map<String, Any?> = document.data ?: emptyMap()
                    val jsonString = convertMapToJsonString(data)
                    val decoded = decodeFromJsonString(jsonString, clazz)
                    DocumentWithId(id = document.id, data = decoded)
                } catch (e: Exception) {
                    println("FirestoreRepositoryAndroidImpl: Error decoding document: ${e.message}")
                    e.printStackTrace()
                    null
                }
            }
            
            println("FirestoreRepositoryAndroidImpl: Successfully decoded ${results.size} items with IDs")
            Result.Success(results)
        } catch (e: Exception) {
            println("FirestoreRepositoryAndroidImpl: Error getting collection with IDs: ${e.message}")
            e.printStackTrace()
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
            "BusinessDto" -> json.decodeFromString<com.good4.business.data.dto.BusinessDto>(jsonString) as T
            "CampaignDto" -> json.decodeFromString<com.good4.campaign.data.dto.CampaignDto>(jsonString) as T
            "CodeDto" -> json.decodeFromString<com.good4.code.data.dto.CodeDto>(jsonString) as T
            "UserDto" -> json.decodeFromString<com.good4.user.data.dto.UserDto>(jsonString) as T
            else -> throw IllegalArgumentException("No serializer found for ${clazz.simpleName}. Add it to the when statement or use @Serializable annotation")
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
                // Convert Firestore Timestamp to ISO string for Instant
                val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(value.toDate().time)
                JsonPrimitive(instant.toString())
            }
            is java.util.Date -> {
                // Convert Date to ISO string for Instant
                val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(value.time)
                JsonPrimitive(instant.toString())
            }
            is Map<*, *> -> {
                val map = value as Map<String, Any?>
                buildJsonObject {
                    map.forEach { (k, v) ->
                        put(k, convertValue(v))
                    }
                }
            }
            is List<*> -> {
                // Convert list to JSON array
                JsonArray(
                    value.map { convertValue(it) }
                )
            }
            null -> JsonNull
            else -> {
                println("FirestoreRepositoryAndroidImpl: Unknown value type: ${value::class.simpleName}, value: $value")
                JsonPrimitive(value.toString())
            }
        }
    }
    
    private fun JsonObject.toFirestoreMap(): Map<String, Any?> {
        return entries.associate { (key, value) ->
            key to when (value) {
                is JsonObject -> value.toFirestoreMap()
                is kotlinx.serialization.json.JsonPrimitive -> {
                    when {
                        value.isString -> value.content
                        else -> {
                            // Try to parse as number or boolean
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