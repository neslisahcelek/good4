package com.good4.core.data.repository

import com.good4.business.data.dto.BusinessDto
import com.good4.calendar.AcademicCalendarEventDto
import com.good4.campaign.data.dto.CampaignDto
import com.good4.code.data.dto.CodeDto
import com.good4.config.data.dto.AppConfigDto
import com.good4.config.data.dto.HomeBannerDto
import com.good4.config.data.dto.UpdateNoticeDto
import com.good4.config.data.dto.UniversitiesConfigDto
import com.good4.core.domain.Error
import com.good4.core.domain.NetworkError
import com.good4.core.domain.Result
import com.good4.core.util.FirebaseDebugLogger
import com.good4.dining.data.dto.AkdenizDiningMenuDto
import com.good4.product.data.dto.ProductDto
import com.good4.user.data.dto.UserDto
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.Query
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.firestore
import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass

class FirestoreRepositoryIOSImpl : FirestoreRepository {
    private val firestore = Firebase.firestore

    override suspend fun <T : Any> queryNumericPage(collectionPath: String, orderByField: String, clazz: KClass<T>,
        pageSize: Long, cursor: NumericPageCursor?): Result<NumericDocumentPage<T>, Error> = try {
        require(pageSize in 1..100)
        var query = firestore.collection(collectionPath).orderBy(orderByField, Direction.DESCENDING)
            .orderBy(dev.gitlive.firebase.firestore.FieldPath.documentId, Direction.DESCENDING)
        cursor?.let { query = query.startAfter(it.value, it.id) }
        val documents = query.limit(pageSize).get().documents
        val items = documents.map { DocumentWithId(it.id, decodeDocumentSnapshot(it, clazz)) }
        val next = documents.lastOrNull()?.takeIf { documents.size.toLong() == pageSize }
            ?.let { NumericPageCursor(it.get<Long>(orderByField), it.id) }
        Result.Success(NumericDocumentPage(items, next))
    } catch (e: Exception) { Result.Error(NetworkError(e.message ?: "Page load failed")) }

    override suspend fun <T : Any> queryPage(collectionPath: String, conditions: Map<String, Any>, clazz: KClass<T>,
        pageSize: Long, cursor: String?, minimumTimestamp: Pair<String, Long>?): Result<DocumentPage<T>, Error> = try {
        require(pageSize in 1..100)
        var query: Query = firestore.collection(collectionPath)
        conditions.forEach { (field, value) -> query = query.where { field equalTo value } }
        minimumTimestamp?.let { (field, seconds) ->
            query = query.where { field greaterThanOrEqualTo Timestamp(seconds, 0) }.orderBy(field)
        }
        query = query.orderBy(dev.gitlive.firebase.firestore.FieldPath.documentId)
        cursor?.let { query = query.startAfter(firestore.collection(collectionPath).document(it).get()) }
        val documents = query.limit(pageSize).get().documents
        Result.Success(DocumentPage(documents.map { DocumentWithId(it.id, decodeDocumentSnapshot(it, clazz)) },
            documents.lastOrNull()?.id.takeIf { documents.size.toLong() == pageSize }))
    } catch (e: Exception) { Result.Error(NetworkError(e.message ?: "Page load failed")) }

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
            FirebaseDebugLogger.error(
                operation = "addDocument",
                path = collectionPath,
                throwable = e
            )
            Result.Error(NetworkError(e.message ?: "Unknown error"))
        }
    }

    override suspend fun reserveProductAndCreateCode(
        productId: String,
        code: CodeDto
    ): Result<String, Error> {
        if (productId.isBlank()) {
            return Result.Error(NetworkError("Product path cannot be empty"))
        }
        return try {
            val codeId = firestore.runTransaction {
                val productRef = firestore.collection("products").document(productId)
                val productSnapshot = get(productRef)
                val currentPending = if (productSnapshot.contains("pendingCount")) {
                    productSnapshot.get<Long>("pendingCount")
                } else {
                    0L
                }

                if (currentPending <= 0L) {
                    throw IllegalStateException("Product out of stock")
                }

                val codeRef = firestore.collection("codes").document
                update(productRef, "pendingCount" to currentPending - 1L)
                set(codeRef, serializerForData(code), code)
                codeRef.id
            }

            Result.Success(codeId)
        } catch (e: Exception) {
            FirebaseDebugLogger.error(
                operation = "reserveProductAndCreateCode",
                path = "products/$productId",
                throwable = e
            )
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
            val document = firestore.collection(collectionPath).document(documentId)
            if (data is UserDto) {
                document.set(
                    userDtoToFirestoreMap(data).toMutableMap().apply {
                        this["updatedAt"] = FieldValue.serverTimestamp
                    }
                )
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

    override suspend fun updateFields(
        collectionPath: String,
        documentId: String,
        fields: Map<String, Any?>
    ): Result<Unit, Error> {
        if (documentId.isBlank()) {
            FirebaseDebugLogger.error(
                operation = "updateFields",
                path = collectionPath,
                detail = "documentId is empty"
            )
            return Result.Error(NetworkError("Document path cannot be empty"))
        }
        return try {
            firestore.collection(collectionPath)
                .document(documentId)
                .update(fields)
            Result.Success(Unit)
        } catch (e: Exception) {
            FirebaseDebugLogger.error(
                operation = "updateFields",
                path = collectionPath,
                throwable = e,
                detail = "documentId=$documentId, fields=${fields.keys}"
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
            if (collectionPath == "users") {
                firestore.runTransaction {
                    set(firestore.document("userTombstones/$documentId"), mapOf(
                        "userId" to documentId,
                        "deletedAt" to kotlinx.datetime.Clock.System.now().epochSeconds
                    ))
                    delete(firestore.document("users/$documentId"))
                }
            } else {
                firestore.collection(collectionPath).document(documentId).delete()
            }
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
            val querySnapshot = firestore.collection(collectionPath).limit(100).get()
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
            val querySnapshot = firestore.collection(collectionPath).limit(100).get()
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
                .where { field equalTo value }
                .limit(100)
                .get()
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
                query = query.where { field equalTo value }
            }
            val querySnapshot = query.limit(100).get()
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
                query = query.where { field equalTo value }
            }
            if (orderByField != null) {
                val direction = if (descending) Direction.DESCENDING else Direction.ASCENDING
                query = query.orderBy(orderByField, direction)
            }
            val querySnapshot = query.limit(limit).get()
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

    /**
     * Tek kaynak: Yeni Firestore DTO eklendiğinde sadece bu map'e bir satır ekle.
     * serializerFor ve serializerForData bu map'i kullanır.
     */
    private val dtoSerializers: Map<String, KSerializer<*>> = mapOf(
        "NotificationDto" to com.good4.notification.NotificationDto.serializer(),
        "NotificationPreferencesDto" to com.good4.notification.NotificationPreferencesDto.serializer(),
        "CommunityDto" to com.good4.community.CommunityDto.serializer(),
        "CommunityEntryDto" to com.good4.community.CommunityEntryDto.serializer(),
        "CommunityAccessDto" to com.good4.community.CommunityAccessDto.serializer(),
        "CommunityFollowDto" to com.good4.community.CommunityFollowDto.serializer(),
        "CommunityEventRegistrationDto" to com.good4.community.CommunityEventRegistrationDto.serializer(),
        "EventAttendanceDto" to com.good4.community.EventAttendanceDto.serializer(),
        "CommunityCouponClaimDto" to com.good4.community.CommunityCouponClaimDto.serializer(),
        "CommunityCouponCodeDto" to com.good4.community.CommunityCouponCodeDto.serializer(),
        "V2OrganizationDto" to com.good4.community.V2OrganizationDto.serializer(),
        "V2MembershipDto" to com.good4.community.V2MembershipDto.serializer(),
        "V2UserRoleDto" to com.good4.community.V2UserRoleDto.serializer(),
        "V2EventDto" to com.good4.community.V2EventDto.serializer(),
        "V2EventRegistrationDto" to com.good4.community.V2EventRegistrationDto.serializer(),
        "ProductDto" to ProductDto.serializer(),
        "BusinessDto" to BusinessDto.serializer(),
        "CampaignDto" to CampaignDto.serializer(),
        "CodeDto" to CodeDto.serializer(),
        "UserDto" to UserDto.serializer(),
        "AppConfigDto" to AppConfigDto.serializer(),
        "HomeBannerDto" to HomeBannerDto.serializer(),
        "UpdateNoticeDto" to UpdateNoticeDto.serializer(),
        "AcademicCalendarEventDto" to AcademicCalendarEventDto.serializer(),
        "UniversitiesConfigDto" to UniversitiesConfigDto.serializer(),
        "AkdenizDiningMenuDto" to AkdenizDiningMenuDto.serializer(),
        "KykMenuDayDto" to com.good4.dining.data.dto.KykMenuDayDto.serializer(),
        "MealRatingSummaryDto" to com.good4.dining.data.repository.MealRatingSummaryDto.serializer(),
        "MealRatingVoteDto" to com.good4.dining.data.repository.MealRatingVoteDto.serializer(),
        "FeedbackSubmissionDto" to com.good4.feedback.FeedbackSubmissionDto.serializer()
    )

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> serializerFor(clazz: KClass<T>): KSerializer<T> {
        val ser = dtoSerializers[clazz.simpleName]
            ?: throw IllegalArgumentException("No serializer for ${clazz.simpleName}. Add to dtoSerializers in FirestoreRepositoryIOSImpl.")
        return ser as KSerializer<T>
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> serializerForData(data: T): KSerializer<T> {
        val ser = dtoSerializers[data::class.simpleName]
            ?: throw IllegalArgumentException("No serializer for ${data::class.simpleName}. Add to dtoSerializers in FirestoreRepositoryIOSImpl.")
        return ser as KSerializer<T>
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> decodeDocumentSnapshot(document: DocumentSnapshot, clazz: KClass<T>): T {
        return when (clazz.simpleName) {
            "UserDto" -> decodeUserDto(document) as T
            "ProductDto" -> decodeProductDto(document) as T
            "CodeDto" -> decodeCodeDto(document) as T
            "V2OrganizationDto" -> decodeV2OrganizationDto(document) as T
            "V2MembershipDto" -> decodeV2MembershipDto(document) as T
            "V2UserRoleDto" -> com.good4.community.V2UserRoleDto(role = document.getOrNull("role") ?: "") as T
            "V2EventDto" -> decodeV2EventDto(document) as T
            "V2CampaignDto" -> com.good4.suspendedmeal.V2CampaignDto(
                organizationId = document.getOrNull("organizationId") ?: "", title = document.getOrNull("title") ?: "",
                description = document.getOrNull("description") ?: "", startsAt = document.getEpochSeconds("startsAt") ?: 0,
                endsAt = document.getEpochSeconds("endsAt") ?: 0, status = document.getOrNull("status") ?: "",
                totalLimit = document.getAsInt("totalLimit"), redemptionCount = document.getAsInt("redemptionCount") ?: 0
            ) as T
            "V2CampaignCodeDto" -> com.good4.suspendedmeal.V2CampaignCodeDto(
                status = document.getOrNull("status") ?: "", expiresAt = document.getEpochSeconds("expiresAt") ?: 0
            ) as T
            "V2EventRegistrationDto" -> decodeV2EventRegistrationDto(document) as T
            "CommunityFollowDto" -> com.good4.community.CommunityFollowDto(
                userId = document.getOrNull("userId") ?: "",
                followedAt = document.getEpochSeconds("followedAt") ?: 0,
            ) as T
            "CampusWeatherDto" -> com.good4.weather.CampusWeatherDto(
                temperature = document.getAsDouble("temperature"),
                label = document.getOrNull("label"),
                source = document.getOrNull("source"),
                updatedAtMillis = document.getOrNull<Long>("updatedAtMillis")
            ) as T
            "EduStatusDto" -> com.good4.eduverification.EduStatusDto(
                eduEmail = document.getOrNull("eduEmail"),
                eduVerified = document.getOrNull("eduVerified")
            ) as T
            else -> document.data(serializerFor(clazz))
        }
    }

    private fun decodeV2OrganizationDto(document: DocumentSnapshot) = com.good4.community.V2OrganizationDto(
        name = document.getOrNull("name") ?: "", type = document.getOrNull("type") ?: "",
        status = document.getOrNull("status") ?: "", university = document.getOrNull("university") ?: "",
        description = document.getOrNull("description") ?: "", logoUrl = document.getOrNull("logoUrl") ?: "",
        coverUrl = document.getOrNull("coverUrl") ?: ""
    )

    private fun decodeV2MembershipDto(document: DocumentSnapshot) = com.good4.community.V2MembershipDto(
        userId = document.getOrNull("userId") ?: "", role = document.getOrNull("role") ?: "",
        status = document.getOrNull("status") ?: ""
    )

    private fun decodeV2EventDto(document: DocumentSnapshot) = com.good4.community.V2EventDto(
        organizationId = document.getOrNull("organizationId") ?: "", title = document.getOrNull("title") ?: "",
        description = document.getOrNull("description") ?: "", startsAt = document.getEpochSeconds("startsAt") ?: 0,
        endsAt = document.getEpochSeconds("endsAt") ?: 0, timezone = document.getOrNull("timezone") ?: "Europe/Istanbul",
        location = document.getOrNull("location") ?: "", imageUrl = document.getOrNull("imageUrl") ?: "",
        capacity = document.getAsInt("capacity") ?: 0, registrationCount = document.getAsInt("registrationCount") ?: 0,
        attendanceCount = document.getAsInt("attendanceCount") ?: 0, status = document.getOrNull("status") ?: "published",
        categoryId = document.getOrNull("categoryId") ?: ""
    )

    private fun decodeV2EventRegistrationDto(document: DocumentSnapshot) = com.good4.community.V2EventRegistrationDto(
        eventId = document.getOrNull("eventId") ?: "", organizationId = document.getOrNull("organizationId") ?: "",
        userId = document.getOrNull("userId") ?: "", displayName = document.getOrNull("displayName") ?: "",
        status = document.getOrNull("status") ?: "registered", registeredAt = document.getEpochSeconds("registeredAt") ?: 0,
        updatedAt = document.getEpochSeconds("updatedAt") ?: 0
    )

    private fun decodeProductDto(document: DocumentSnapshot): ProductDto {
        return ProductDto(
            name = document.getOrNull("name"),
            description = document.getOrNull("description"),
            pendingCount = document.getOrNull("pendingCount"),
            dailyPendingLimit = document.getAsInt("dailyPendingLimit"),
            isDonation = document.getOrNull("isDonation"),
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
            displayName = document.getOrNull("displayName"),
            phoneNumber = document.getOrNull("phoneNumber"),
            role = document.getOrNull("role"),
            verified = document.getOrNull("verified"),
            status = document.getOrNull("status"),
            university = document.getOrNull("university"),
            faculty = document.getOrNull("faculty"),
            major = document.getOrNull("major"),
            classYear = document.getOrNull("classYear"),
            educationLevel = document.getOrNull("educationLevel"),
            credit = document.getAsInt("credit"),
            weeklyCreditOverride = document.getAsInt("weeklyCreditOverride"),
            lastCreditResetAt = document.getEpochSeconds("lastCreditResetAt"),
            registrationDate = document.getEpochSeconds("registrationDate"),
            createdAt = document.getEpochSeconds("createdAt"),
            updatedAt = document.getEpochSeconds("updatedAt"),
            totalDonations = document.getAsInt("totalDonations"),
            totalMeals = document.getAsInt("totalMeals")
        )
    }

    private fun userDtoToFirestoreMap(userDto: UserDto): Map<String, Any?> {
        return mapOf(
            "email" to userDto.email,
            "fullName" to userDto.fullName,
            "displayName" to userDto.displayName,
            "phoneNumber" to userDto.phoneNumber,
            "role" to userDto.role,
            "verified" to userDto.verified,
            "status" to userDto.status,
            "university" to userDto.university,
            "faculty" to userDto.faculty,
            "major" to userDto.major,
            "classYear" to userDto.classYear,
            "educationLevel" to userDto.educationLevel,
            "credit" to userDto.credit,
            "weeklyCreditOverride" to userDto.weeklyCreditOverride,
            "lastCreditResetAt" to userDto.lastCreditResetAt?.toFirestoreTimestampOrNull(),
            "registrationDate" to userDto.registrationDate?.toFirestoreTimestampOrNull(),
            "createdAt" to userDto.createdAt?.toFirestoreTimestampOrNull(),
            "updatedAt" to userDto.updatedAt?.toFirestoreTimestampOrNull(),
            "totalDonations" to userDto.totalDonations,
            "totalMeals" to userDto.totalMeals
        )
    }

    private fun DocumentSnapshot.getAsDouble(field: String): Double? =
        getOrNull<Double>(field) ?: getOrNull<Long>(field)?.toDouble()

    private fun DocumentSnapshot.getAsInt(field: String): Int? {
        getOrNull<Int>(field)?.let { return it }
        getOrNull<Long>(field)?.toInt()?.let { return it }
        getOrNull<Double>(field)?.toInt()?.let { return it }
        getOrNull<String>(field)?.toIntOrNull()?.let { return it }
        return null
    }

    private inline fun <reified T> DocumentSnapshot.getOrNull(field: String): T? {
        return try {
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
            (
                getOrNull<Long>(field)
                    ?: getOrNull<Timestamp>(field)?.seconds
                    ?: readEpochSecondsFromMap(getOrNull<Any>(field))
                )?.normalizeEpochSeconds()
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

    private fun Long.normalizeEpochSeconds(): Long? {
        val seconds = if (this > MAX_FIRESTORE_EPOCH_SECONDS || this < MIN_FIRESTORE_EPOCH_SECONDS) {
            this / 1_000
        } else {
            this
        }
        return seconds.takeIf { it in MIN_FIRESTORE_EPOCH_SECONDS..MAX_FIRESTORE_EPOCH_SECONDS }
    }

    private fun Long.toFirestoreTimestampOrNull(): Timestamp? =
        normalizeEpochSeconds()?.let { Timestamp(it, 0) }

    private companion object {
        const val MIN_FIRESTORE_EPOCH_SECONDS = -62_135_596_800L
        const val MAX_FIRESTORE_EPOCH_SECONDS = 253_402_300_799L
    }

    private fun toIntOrNull(value: Any?): Int? {
        return when (value) {
            is Int -> value
            is Long -> value.toInt()
            is Double -> value.toInt()
            is Float -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        }
    }

    private fun toDoubleOrNull(value: Any?): Double? {
        return when (value) {
            is Double -> value
            is Float -> value.toDouble()
            is Int -> value.toDouble()
            is Long -> value.toDouble()
            is String -> value.toDoubleOrNull()
            else -> null
        }
    }
}
