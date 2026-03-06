package com.good4.core.data.repository

import com.good4.core.domain.Error
import com.good4.core.domain.NetworkError
import com.good4.core.domain.Result
import com.good4.core.util.FirebaseDebugLogger
import com.good4.code.data.dto.CodeDto
import com.good4.product.data.dto.ProductDto
import com.good4.user.data.dto.UserDto
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.Query
import dev.gitlive.firebase.firestore.Timestamp
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
            val documentReference = if (data is UserDto) {
                firestore.collection(collectionPath).add(userDtoToFirestoreMap(data))
            } else {
                firestore.collection(collectionPath).add(
                    strategy = serializerForData(data),
                    data = data
                )
            }
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
                val result = decodeDocumentSnapshot(documentSnapshot, clazz)
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
            val document = firestore.collection(collectionPath).document(documentId)
            if (data is UserDto) {
                document.set(userDtoToFirestoreMap(data))
            } else {
                document.set(serializerForData(data), data)
            }
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
                    decodeDocumentSnapshot(document, clazz)
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
                    val decoded = decodeDocumentSnapshot(document, clazz)
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
                    val decoded = decodeDocumentSnapshot(document, clazz)
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
                    val decoded = decodeDocumentSnapshot(document, clazz)
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
                    val decoded = decodeDocumentSnapshot(document, clazz)
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
            "ProductDto" -> ProductDto.serializer() as KSerializer<T>
            "BusinessDto" -> com.good4.business.data.dto.BusinessDto.serializer() as KSerializer<T>
            "CampaignDto" -> com.good4.campaign.data.dto.CampaignDto.serializer() as KSerializer<T>
            "CodeDto" -> CodeDto.serializer() as KSerializer<T>
            "UserDto" -> UserDto.serializer() as KSerializer<T>
            "AppConfigDto" -> com.good4.config.data.dto.AppConfigDto.serializer() as KSerializer<T>
            else -> throw IllegalArgumentException("No serializer found for ${clazz.simpleName}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> serializerForData(data: T): KSerializer<T> {
        return when (data) {
            is com.good4.product.data.dto.ProductDto ->
                com.good4.product.data.dto.ProductDto.serializer() as KSerializer<T>
            is com.good4.business.data.dto.BusinessDto ->
                com.good4.business.data.dto.BusinessDto.serializer() as KSerializer<T>
            is com.good4.campaign.data.dto.CampaignDto ->
                com.good4.campaign.data.dto.CampaignDto.serializer() as KSerializer<T>
            is com.good4.code.data.dto.CodeDto ->
                com.good4.code.data.dto.CodeDto.serializer() as KSerializer<T>
            is com.good4.user.data.dto.UserDto ->
                com.good4.user.data.dto.UserDto.serializer() as KSerializer<T>
            else -> throw IllegalArgumentException("No serializer found for ${data::class.simpleName}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> decodeDocumentSnapshot(document: DocumentSnapshot, clazz: KClass<T>): T {
        return when (clazz.simpleName) {
            "UserDto" -> decodeUserDto(document) as T
            "ProductDto" -> decodeProductDto(document) as T
            "CodeDto" -> decodeCodeDto(document) as T
            else -> document.data(serializerFor(clazz))
        }
    }

    private fun decodeProductDto(document: DocumentSnapshot): ProductDto {
        return ProductDto(
            name = document.getOrNull("name"),
            description = document.getOrNull("description"),
            count = document.getOrNull("count"),
            pendingCount = document.getOrNull("pendingCount"),
            businessId = document.getOrNull("businessId"),
            createdAt = document.getEpochSeconds("createdAt"),
            discountPrice = document.getOrNull("discountPrice"),
            originalPrice = document.getOrNull("originalPrice"),
            imageUrl = document.getOrNull("image"),
            foodType = document.getOrNull("foodType"),
            totalDelivered = document.getOrNull("totalDelivered"),
            totalSuspended = document.getOrNull("totalSuspended")
        )
    }

    private fun decodeCodeDto(document: DocumentSnapshot): CodeDto {
        return CodeDto(
            value = document.getOrNull("value"),
            businessId = document.getOrNull("businessId"),
            productId = document.getOrNull("productId"),
            userId = document.getOrNull("userId"),
            status = document.getOrNull("status"),
            createdAt = document.getEpochSeconds("createdAt"),
            expiresAt = document.getEpochSeconds("expiresAt"),
            usedAt = document.getEpochSeconds("usedAt")
        )
    }

    private fun decodeUserDto(document: DocumentSnapshot): UserDto {
        return UserDto(
            email = document.getOrNull("email"),
            fullName = document.getOrNull("fullName"),
            phoneNumber = document.getOrNull("phoneNumber"),
            role = document.getOrNull("role"),
            verified = document.getOrNull("verified"),
            university = document.getOrNull("university"),
            major = document.getOrNull("major"),
            educationLevel = document.getOrNull("educationLevel"),
            credit = document.getOrNull("credit"),
            weeklyCreditOverride = document.getOrNull("weeklyCreditOverride"),
            lastCreditResetAt = document.getEpochSeconds("lastCreditResetAt"),
            registrationDate = document.getEpochSeconds("registrationDate")
        )
    }

    private fun userDtoToFirestoreMap(userDto: UserDto): Map<String, Any?> {
        return mapOf(
            "email" to userDto.email,
            "fullName" to userDto.fullName,
            "phoneNumber" to userDto.phoneNumber,
            "role" to userDto.role,
            "verified" to userDto.verified,
            "university" to userDto.university,
            "major" to userDto.major,
            "educationLevel" to userDto.educationLevel,
            "credit" to userDto.credit,
            "weeklyCreditOverride" to userDto.weeklyCreditOverride,
            "lastCreditResetAt" to userDto.lastCreditResetAt?.let { Timestamp(it, 0) },
            "registrationDate" to userDto.registrationDate?.let { Timestamp(it, 0) }
        )
    }

    private inline fun <reified T> DocumentSnapshot.getOrNull(field: String): T? {
        return try {
            if (!contains(field)) return null
            get(field)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Reads a timestamp field. Supports:
     * - Long (epoch seconds)
     * - Firestore Timestamp
     * - Old Kotlin datetime format (map with epochSeconds or value$kotlinx_datetime.epochSecond)
     */
    private fun DocumentSnapshot.getEpochSeconds(field: String): Long? {
        return try {
            if (!contains(field)) return null
            getOrNull<Long>(field)
                ?: getOrNull<Timestamp>(field)?.seconds
                ?: readEpochSecondsFromMap(getOrNull<Any>(field))
        } catch (_: Exception) {
            null
        }
    }

    private fun readEpochSecondsFromMap(value: Any?): Long? {
        if (value == null) return null
        val map = value as? Map<*, *> ?: return null
        return try {
            (map["epochSeconds"] as? Number)?.toLong()
                ?: ((map["value\$kotlinx_datetime"] as? Map<*, *>)?.get("epochSecond") as? Number)?.toLong()
        } catch (_: Exception) {
            null
        }
    }
}
