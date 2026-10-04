@file:OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)

package com.good4.community

import com.good4.core.network.callV2Function
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.io.encoding.Base64

internal fun communityImageUploadData(bytes: ByteArray): JsonObject {
    require(bytes.isNotEmpty() && bytes.size <= 5 * 1024 * 1024) { "COMMUNITY_IMAGE_SIZE_INVALID" }
    val contentType = when {
        bytes.size >= 3 && bytes[0] == 0xff.toByte() && bytes[1] == 0xd8.toByte() && bytes[2] == 0xff.toByte() -> "image/jpeg"
        bytes.size >= 8 && bytes.copyOfRange(0, 8).contentEquals(byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)) -> "image/png"
        bytes.size >= 12 && bytes.copyOfRange(0, 4).decodeToString() == "RIFF" && bytes.copyOfRange(8, 12).decodeToString() == "WEBP" -> "image/webp"
        else -> error("COMMUNITY_IMAGE_CONTENT_INVALID")
    }
    return buildJsonObject {
        put("contentType", contentType)
        put("base64", Base64.Default.encode(bytes))
    }
}

suspend fun uploadCommunityImage(bytes: ByteArray): String {
    val response = callV2Function("uploadCommunityEventImage", communityImageUploadData(bytes))
    return response["imageUrl"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
        ?: error("COMMUNITY_IMAGE_INVALID")
}
