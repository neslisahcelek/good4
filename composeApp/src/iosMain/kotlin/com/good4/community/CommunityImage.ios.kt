@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.good4.community

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.storage.Data
import dev.gitlive.firebase.storage.storage
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSMutableData
import platform.Foundation.NSUUID
import platform.Foundation.appendBytes

actual suspend fun uploadCommunityImage(communityId: String, bytes: ByteArray): String {
    require(bytes.isNotEmpty() && bytes.size < 5 * 1024 * 1024) { "Görsel 5 MB'dan küçük olmalı." }
    val data = NSMutableData()
    bytes.usePinned { data.appendBytes(it.addressOf(0), bytes.size.toULong()) }
    val ref = Firebase.storage.reference.child("community_images/$communityId/${NSUUID().UUIDString}.jpg")
    ref.putData(Data(data), dev.gitlive.firebase.storage.storageMetadata { contentType = "image/jpeg" })
    return ref.getDownloadUrl()
}
