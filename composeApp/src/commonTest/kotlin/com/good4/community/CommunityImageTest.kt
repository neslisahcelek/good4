@file:OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)

package com.good4.community

import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CommunityImageTest {
    private val jpeg = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 0xd9.toByte())

    @Test
    fun supportedImagesKeepTheirTypeAndBytesInTheCallablePayload() {
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)
        val webp = "RIFF0000WEBP".encodeToByteArray()
        for ((contentType, bytes) in listOf("image/jpeg" to jpeg, "image/png" to png, "image/webp" to webp)) {
            val data = communityImageUploadData(bytes)
            assertEquals(contentType, data["contentType"]?.jsonPrimitive?.content)
            assertContentEquals(bytes, Base64.Default.decode(data["base64"]!!.jsonPrimitive.content))
        }
    }

    @Test
    fun emptyAndOversizedImagesAreRejectedAndTheLimitIsInclusive() {
        assertEquals("COMMUNITY_IMAGE_SIZE_INVALID", assertFailsWith<IllegalArgumentException> {
            communityImageUploadData(byteArrayOf())
        }.message)
        val maxBytes = 5 * 1024 * 1024
        val bytes = ByteArray(maxBytes)
        jpeg.copyInto(bytes)
        assertEquals("image/jpeg", communityImageUploadData(bytes)["contentType"]?.jsonPrimitive?.content)
        assertEquals("COMMUNITY_IMAGE_SIZE_INVALID", assertFailsWith<IllegalArgumentException> {
            communityImageUploadData(bytes.copyOf(maxBytes + 1))
        }.message)
    }

    @Test
    fun unsupportedAndTruncatedImagesAreRejected() {
        for (bytes in listOf("GIF89a".encodeToByteArray(), "RIFF".encodeToByteArray(), jpeg.copyOf(2))) {
            assertEquals("COMMUNITY_IMAGE_CONTENT_INVALID", assertFailsWith<IllegalStateException> {
                communityImageUploadData(bytes)
            }.message)
        }
    }
}
