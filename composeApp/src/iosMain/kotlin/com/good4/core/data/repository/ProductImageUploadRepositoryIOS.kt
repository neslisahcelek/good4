@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.good4.core.data.repository

import com.good4.core.domain.Error
import com.good4.core.domain.NetworkError
import com.good4.core.domain.Result
import com.good4.core.util.FirebaseDebugLogger
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.storage.Data
import dev.gitlive.firebase.storage.storage
import kotlinx.cinterop.Pinned
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.pin
import kotlinx.datetime.Clock
import platform.Foundation.NSData
import platform.Foundation.NSMutableData
import platform.Foundation.appendBytes

class ProductImageUploadRepositoryIOS : ProductImageUploadRepository {

    override suspend fun uploadProductImage(jpegBytes: ByteArray): Result<String, Error> {
        return try {
            val objectName = "img_${Clock.System.now().toEpochMilliseconds()}.jpg"
            val objectPath = "product_images/$objectName"
            FirebaseDebugLogger.request(
                operation = "uploadProductImage",
                path = "storage/$objectPath",
                detail = "bytes=${jpegBytes.size}"
            )
            val nsData = jpegBytes.toNSData()
            val ref = Firebase.storage.reference
                .child("product_images")
                .child(objectName)
            ref.putData(Data(nsData))
            val url = ref.getDownloadUrl()
            FirebaseDebugLogger.success(
                operation = "uploadProductImage",
                path = "storage/$objectPath",
                detail = "url=$url"
            )
            Result.Success(url)
        } catch (e: Exception) {
            FirebaseDebugLogger.error(
                operation = "uploadProductImage",
                path = "storage/product_images",
                throwable = e,
                detail = "bytes=${jpegBytes.size}"
            )
            Result.Error(NetworkError(e.message ?: "Unknown"))
        }
    }

    override suspend fun deleteProductImage(imageUrl: String): Result<Unit, Error> {
        val objectPath = imageUrl.toProductImagePath() ?: return Result.Success(Unit)
        return try {
            FirebaseDebugLogger.request(
                operation = "deleteProductImage",
                path = "storage/$objectPath"
            )
            Firebase.storage.reference(objectPath).delete()
            FirebaseDebugLogger.success(
                operation = "deleteProductImage",
                path = "storage/$objectPath"
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            FirebaseDebugLogger.error(
                operation = "deleteProductImage",
                path = "storage/$objectPath",
                throwable = e
            )
            Result.Error(NetworkError(e.message ?: "Unknown"))
        }
    }
}

private fun String.toProductImagePath(): String? {
    if (isBlank()) return null
    val encodedPath = substringAfter("/o/", missingDelimiterValue = "")
        .substringBefore("?")
        .takeIf { it.isNotBlank() }
        ?: return null
    val path = encodedPath
        .replace("%2F", "/")
        .replace("%2f", "/")
    return path.takeIf { it.startsWith("product_images/") }
}

private fun ByteArray.toNSData(): NSData {
    val mutable: NSMutableData = NSMutableData()
    val pinned: Pinned<ByteArray> = this.pin()
    try {
        mutable.appendBytes(pinned.addressOf(0), size.toULong())
    } finally {
        pinned.unpin()
    }
    return mutable
}
