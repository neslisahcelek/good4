package com.good4.suspendedmeal

import com.good4.community.V2OrganizationDto
import com.good4.core.data.repository.FirestoreRepository
import com.good4.core.domain.Result
import com.good4.core.network.callV2Function
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** V2 campaign (campaigns/{id}); timestamps are epoch seconds on both platforms. */
@Serializable
data class V2CampaignDto(
    val organizationId: String = "",
    val title: String = "",
    val description: String = "",
    val startsAt: Long = 0,
    val endsAt: Long = 0,
    val status: String = "",
    val totalLimit: Int? = null,
    val redemptionCount: Int = 0
)

/** The student's own code (campaignCodes/{code}), polled while the code is on screen. */
@Serializable
data class V2CampaignCodeDto(
    val status: String = "",
    val expiresAt: Long = 0
)

data class SuspendedMeal(
    val id: String,
    val title: String,
    val description: String,
    val businessName: String,
    val endsAtMillis: Long,
    /** Null when the campaign has no total limit. */
    val remaining: Int?
)

sealed interface CodeRequestResult {
    data class Code(val code: String, val expiresAtMillis: Long) : CodeRequestResult
    data object AlreadyUsed : CodeRequestResult
}

class SuspendedMealRepository(
    private val store: FirestoreRepository
) {
    /** Published campaigns running right now that still have meals left. */
    suspend fun activeMeals(): Result<List<SuspendedMeal>, com.good4.core.domain.Error> {
        val result = store.queryCollectionWithIds("campaigns", "status", "published", V2CampaignDto::class)
        if (result !is Result.Success) return result as Result.Error
        val nowSeconds = Clock.System.now().epochSeconds
        val running = result.data.filter { (_, campaign) ->
            nowSeconds in campaign.startsAt until campaign.endsAt &&
                (campaign.totalLimit == null || campaign.redemptionCount < campaign.totalLimit)
        }
        val names = running.map { it.data.organizationId }.distinct().associateWith { id ->
            (store.getDocument("organizations", id, V2OrganizationDto::class) as? Result.Success)?.data?.name.orEmpty()
        }
        return Result.Success(
            running.map { (id, campaign) ->
                SuspendedMeal(
                    id = id,
                    title = campaign.title,
                    description = campaign.description,
                    businessName = names[campaign.organizationId].orEmpty().ifBlank { "İşletme" },
                    endsAtMillis = campaign.endsAt * 1000,
                    remaining = campaign.totalLimit?.let { (it - campaign.redemptionCount).coerceAtLeast(0) }
                )
            }.sortedBy { it.endsAtMillis }
        )
    }

    /** Issues (or returns the still valid) 10-minute code; the server checks edu verification and limits. */
    suspend fun requestCode(campaignId: String): CodeRequestResult {
        val response = callV2Function("issueCampaignCode", buildJsonObject { put("campaignId", campaignId) })
        val outcome = response["outcome"]?.jsonPrimitive?.content
        if (outcome == "already_redeemed") return CodeRequestResult.AlreadyUsed
        val code = response["code"]?.jsonPrimitive?.content ?: error("Kod alınamadı.")
        val expiresAt = response["expiresAt"]?.jsonPrimitive?.content
            ?.let { runCatching { Instant.parse(it).toEpochMilliseconds() }.getOrNull() }
            ?: error("Kod alınamadı.")
        return CodeRequestResult.Code(code, expiresAt)
    }

    /** "issued", "redeemed" or "expired"; null when it cannot be read right now. */
    suspend fun codeStatus(code: String): String? =
        (store.getDocument("campaignCodes", code, V2CampaignCodeDto::class) as? Result.Success)?.data?.status
}
