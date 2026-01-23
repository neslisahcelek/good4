package com.good4.business.data.dto

import com.good4.business.domain.Business
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
    @SerialName("city")
    val city: String? = null,
    @SerialName("district")
    val district: String? = null
)

fun BusinessDto.toBusiness(id: String): Business {
    return Business(
        id = id,
        name = name.orEmpty(),
        ownerId = ownerId.orEmpty(),
        phone = phone.orEmpty(),
        address = address.orEmpty(),
        city = city.orEmpty(),
        district = district.orEmpty()
    )
}

fun Business.toDto(): BusinessDto {
    return BusinessDto(
        name = name,
        ownerId = ownerId,
        phone = phone,
        address = address,
        city = city,
        district = district
    )
}

