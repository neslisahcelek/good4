package com.good4.social

/** A successful empty page is cached just like a nonempty one. */
data class CachedSocialFeed(val feed: SocialFeed, val savedAt: Long, val revision: Long)

data class CachedSocialMine(val mine: SocialMine, val savedAt: Long, val revision: Long)

internal const val SOCIAL_FEED_FRESH_MS = 2 * 60_000L
internal const val SOCIAL_FEED_CACHE_MAX_AGE_MS = 10 * 60_000L

internal fun SocialMe.hasSameFeedScope(other: SocialMe): Boolean =
    enabled == other.enabled && campusAccessAllowed == other.campusAccessAllowed && accessSource == other.accessSource && eduVerified == other.eduVerified && eduEmail == other.eduEmail

interface SocialHomeDataSource {
    val currentUid: String?
    /** Changes after mutations or an account change, invalidating old reads and cached pages. */
    val revision: Long
    fun cachedFeed(kind: String?): CachedSocialFeed? = null
    fun cachedMe(): SocialMe? = null
    fun cachedMine(): CachedSocialMine? = null
    suspend fun invalidateHomeCache()
    suspend fun feed(kind: String?, after: String?): SocialFeed
    suspend fun mine(): SocialMine
    suspend fun acceptTerms(version: Int, showName: Boolean? = null)
    suspend fun updateProfile(showName: Boolean?, photo: ByteArray?, removePhoto: Boolean): SocialMe
}
