package com.good4.business.data.dto

import com.good4.business.domain.Business
import com.good4.business.domain.BusinessApprovalStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BusinessDto(
    @SerialName("name")
    val name: String? = null,
    @SerialName("ownerId")
    val ownerId: String? = null,
    @SerialName("phone")
    val phone: String? = null,
    @SerialName("address")
    val address: String? = null,
    @SerialName("addressUrl")
    val addressUrl: String? = null,
    @SerialName("city")
    val city: String? = null,
    @SerialName("district")
    val district: String? = null,
    @SerialName("approvalStatus")
    val approvalStatus: String? = null,
    @SerialName("approvalRequestedAt")
    val approvalRequestedAt: Long? = null,
    @SerialName("approvedAt")
    val approvedAt: Long? = null,
    @SerialName("approvedBy")
    val approvedBy: String? = null
)

fun BusinessDto.toBusiness(id: String): Business {
    return Business(
        id = id,
        name = name.orEmpty(),
        ownerId = ownerId.orEmpty(),
        phone = phone.orEmpty(),
        address = address.orEmpty(),
        addressUrl = addressUrl.orEmpty(),
        city = city.orEmpty(),
        district = district.orEmpty(),
        approvalStatus = BusinessApprovalStatus.fromValue(approvalStatus),
        approvalRequestedAt = approvalRequestedAt,
        approvedAt = approvedAt,
        approvedBy = approvedBy
    )
}

fun Business.toDto(): BusinessDto {
    return BusinessDto(
        name = name,
        ownerId = ownerId,
        phone = phone,
        address = address,
        addressUrl = addressUrl,
        city = city,
        district = district,
        approvalStatus = approvalStatus.value,
        approvalRequestedAt = approvalRequestedAt,
        approvedAt = approvedAt,
        approvedBy = approvedBy
    )
}
