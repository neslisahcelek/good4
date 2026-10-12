package com.good4.discounts

import com.good4.auth.data.repository.AuthRepository
import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.domain.Result
import com.good4.core.network.callV2Function
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Serializable
data class BusinessDiscountDto(
    val businessId: String = "",
    val businessName: String = "",
    val title: String = "",
    val conditions: String = "",
    val mapsUrl: String = "",
    val discountValue: Int = 0,
    val totalLimit: Int? = null,
    val redemptionCount: Int = 0,
    val startsAt: Long = 0,
    val endsAt: Long = 0,
    val status: String = "",
    val version: Int = 0
) {
    fun isAvailable(nowSeconds: Long): Boolean = status == "published" &&
        startsAt <= nowSeconds && nowSeconds < endsAt &&
        (totalLimit == null || redemptionCount < totalLimit)
}

data class Discount(val id: String, val data: BusinessDiscountDto)
data class DiscountPage(val discounts: List<Discount>, val nextCursor: String?)
data class DiscountCode(val outcome: String, val code: String? = null, val expiresAt: Instant? = null)

interface DiscountDataSource {
    val accounts: Flow<String?>
    suspend fun load(cursor: String?): DiscountPage
    suspend fun issue(discount: Discount): DiscountCode
}

class DiscountRepository(
    private val auth: AuthRepository,
    private val firestore: FirestoreRepository
) : DiscountDataSource {
    override val accounts = auth.authStateFlow.map { it?.uid }

    override suspend fun load(cursor: String?): DiscountPage {
        return when (val result = firestore.queryPage(
            "businessDiscounts", mapOf("status" to "published"), BusinessDiscountDto::class,
            pageSize = 50, cursor = cursor,
            minimumTimestamp = "endsAt" to Clock.System.now().epochSeconds
        )) {
            is Result.Success -> DiscountPage(
                result.data.items.map { Discount(it.id, it.data) }, result.data.nextCursor
            )
            is Result.Error -> error("DISCOUNTS_LOAD_FAILED")
        }
    }

    override suspend fun issue(discount: Discount): DiscountCode {
        val response = callV2Function("issueBusinessDiscountCode", buildJsonObject {
            put("discountId", discount.id)
            put("version", discount.data.version)
        })
        return DiscountCode(
            outcome = response["outcome"]?.jsonPrimitive?.content ?: "error",
            code = response["code"]?.jsonPrimitive?.content,
            expiresAt = response["expiresAt"]?.jsonPrimitive?.content?.let(Instant::parse)
        )
    }
}
