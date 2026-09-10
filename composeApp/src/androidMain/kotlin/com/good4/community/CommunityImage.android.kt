package com.good4.community

import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

actual suspend fun uploadCommunityImage(communityId: String, bytes: ByteArray): String {
    val ref = FirebaseStorage.getInstance().reference.child("community_images/$communityId/${UUID.randomUUID()}.jpg")
    require(bytes.isNotEmpty() && bytes.size < 5 * 1024 * 1024) { "Görsel 5 MB'dan küçük olmalı." }
    ref.putBytes(bytes, com.google.firebase.storage.StorageMetadata.Builder().setContentType("image/jpeg").build()).await()
    return ref.downloadUrl.await().toString()
}
