package com.good4.community

expect suspend fun uploadCommunityImage(communityId: String, bytes: ByteArray): String
