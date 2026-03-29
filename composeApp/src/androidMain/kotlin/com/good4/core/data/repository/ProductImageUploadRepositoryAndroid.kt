package com.good4.core.data.repository

import com.good4.core.domain.Error
import com.good4.core.domain.NetworkError
import com.good4.core.domain.Result
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ProductImageUploadRepositoryAndroid : ProductImageUploadRepository {

    override suspend fun uploadProductImage(jpegBytes: ByteArray): Result<String, Error> {
        return try {
            val ref = Firebase.storage.reference.child("product_images/${UUID.randomUUID()}.jpg")
            ref.putBytes(jpegBytes).await()
            val uri = ref.downloadUrl.await()
            Result.Success(uri.toString())
        } catch (e: Exception) {
            Result.Error(NetworkError(e.message ?: "Unknown"))
        }
    }
}
