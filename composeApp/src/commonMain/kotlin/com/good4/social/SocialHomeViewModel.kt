package com.good4.social

import com.good4.analytics.*

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.core.presentation.UiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class SocialHomeState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val loadError: UiText? = null,
    val me: SocialMe? = null,
    /** null = Tümü, otherwise "social" or "sport". */
    val kind: String? = null,
    val activities: List<SocialActivity> = emptyList(),
    val nextAfter: String? = null,
    val isLoadingMore: Boolean = false,
    val loadMoreError: UiText? = null,
    val mine: SocialMine? = null,
    val mineLoading: Boolean = false,
    val mineRefreshing: Boolean = false,
    val mineError: UiText? = null,
    val acceptingTerms: Boolean = false,
    val termsError: UiText? = null,
    val profileSaving: Boolean = false,
    val profileError: UiText? = null
)

class SocialHomeViewModel(private val repository: SocialHomeDataSource, private val analytics: AnalyticsTracker = NoOpAnalyticsTracker) : ViewModel() {
    private val _state = MutableStateFlow(SocialHomeState())
    val state = _state.asStateFlow()
    private var loadJob: Job? = null
    private var paginationJob: Job? = null
    private var mineJob: Job? = null
    private var requestVersion = 0L
    private var mineVersion = 0L
    private var uid = repository.currentUid
    private var revision = repository.revision
    private var mineSavedAt = 0L
    private var hasResumed = false
    /** Stores the entire loaded list and its matching cursor, including empty results. */
    private val pages = mutableMapOf<String?, CachedSocialFeed>()
    private fun now() = Clock.System.now().toEpochMilliseconds()

    fun onResume(showMine: Boolean) {
        load(forceRefresh = hasResumed)
        // Start the adjacent tab while Discover loads; a tab tap shares this work.
        if (_state.value.me?.enabled != false) loadMine(forceRefresh = hasResumed && showMine)
        hasResumed = true
    }

    fun refreshAfterVerification() {
        stopReadsForProfileChange()
        _state.update { it.copy(activities = emptyList(), nextAfter = null, mine = null) }
        viewModelScope.launch {
            repository.invalidateHomeCache()
            load()
            loadMine(forceRefresh = false)
        }
    }

    private fun syncSession() {
        val currentUid = repository.currentUid
        val currentRevision = repository.revision
        if (currentUid != uid || currentRevision != revision) {
            ++requestVersion
            ++mineVersion
            loadJob?.cancel()
            paginationJob?.cancel()
            mineJob?.cancel()
            pages.clear()
            mineSavedAt = 0
            if (currentUid != uid) _state.update { SocialHomeState() }
            else _state.update { it.copy(me = null, activities = emptyList(), nextAfter = null, mine = null,
                isLoadingMore = false, isRefreshing = false, mineLoading = false, mineRefreshing = false) }
            uid = currentUid
            revision = currentRevision
        }
    }

    private fun cachedPage(kind: String?): CachedSocialFeed? {
        bestPage(kind)?.let { return it }
        // Only a complete Tümü list can prove that a filtered tab is complete or empty.
        val all = bestPage(null)
        return all?.takeIf {
            kind != null && usable(it) && it.feed.nextAfter == null
        }?.let { it.copy(feed = it.feed.copy(activities = it.feed.activities.filter { activity -> activity.kind == kind })) }
    }

    private fun bestPage(kind: String?): CachedSocialFeed? =
        listOfNotNull(pages[kind], repository.cachedFeed(kind)).filter(::usable).maxByOrNull { it.savedAt }

    private fun usable(cached: CachedSocialFeed): Boolean {
        val me = repository.cachedMe() ?: _state.value.me
        return cached.revision == revision && now() - cached.savedAt in 0..SOCIAL_FEED_CACHE_MAX_AGE_MS
            && (me == null || cached.feed.me.hasSameFeedScope(me))
    }

    fun load(forceRefresh: Boolean = true) {
        syncSession()
        if (loadJob?.isActive == true || _state.value.profileSaving || _state.value.acceptingTerms) return
        val kind = _state.value.kind
        val cached = cachedPage(kind)
        val cachedMe = repository.cachedMe()
        val known = cached?.feed?.activities ?: if (kind != null)
            bestPage(null)?.feed?.activities?.filter { it.kind == kind }.orEmpty()
        else if (cachedMe == null || _state.value.me?.hasSameFeedScope(cachedMe) == true) _state.value.activities else emptyList()
        _state.update {
            it.copy(me = cachedMe ?: it.me ?: cached?.feed?.me, activities = known,
                nextAfter = cached?.feed?.nextAfter, isLoading = cached == null && known.isEmpty(),
                isRefreshing = !(cached == null && known.isEmpty()), loadError = null, loadMoreError = null, isLoadingMore = false)
        }
        if (!forceRefresh && cached != null && now() - cached.savedAt in 0 until SOCIAL_FEED_FRESH_MS) {
            pages[kind] = cached
            _state.update { it.copy(isLoading = false, isRefreshing = false) }
            return
        }
        val version = ++requestVersion
        val owner = uid
        val dataRevision = revision
        paginationJob?.cancel()
        loadJob = viewModelScope.launch {
            homeAttempt { repository.feed(kind, null) }
                .onSuccess { feed ->
                    if (!isCurrent(version, owner, dataRevision)) return@onSuccess
                    val previousMe = _state.value.me
                    if (previousMe?.hasSameFeedScope(feed.me) == false) {
                        pages.clear()
                        mineSavedAt = 0
                    }
                    pages[kind] = CachedSocialFeed(feed, now(), dataRevision)
                    _state.update { it.copy(isLoading = false, isRefreshing = false, me = feed.me,
                        activities = feed.activities, nextAfter = feed.nextAfter) }
                }
                .onFailure { error ->
                    if (isCurrent(version, owner, dataRevision)) _state.update {
                        it.copy(isLoading = false, isRefreshing = false, loadError = socialErrorMessage(error))
                    }
                }
        }
    }

    fun selectKind(kind: String?) {
        analytics.click(Interaction.SOCIAL_CATEGORY, AnalyticsTarget.category(kind))
        if (kind == _state.value.kind) return
        ++requestVersion
        loadJob?.cancel()
        paginationJob?.cancel()
        _state.update { it.copy(kind = kind, activities = emptyList(), nextAfter = null) }
        load(forceRefresh = false)
    }

    private fun isCurrent(version: Long, owner: String?, dataRevision: Long): Boolean =
        version == requestVersion && owner == repository.currentUid && dataRevision == repository.revision

    fun loadMore() {
        val snapshot = _state.value
        val after = snapshot.nextAfter ?: return
        if (snapshot.isLoadingMore || snapshot.isLoading || snapshot.isRefreshing) return
        val version = requestVersion
        val owner = uid
        val dataRevision = revision
        _state.update { it.copy(isLoadingMore = true, loadMoreError = null) }
        paginationJob = viewModelScope.launch {
            homeAttempt { repository.feed(snapshot.kind, after) }
                .onSuccess { feed ->
                    if (!isCurrent(version, owner, dataRevision)) return@onSuccess
                    _state.update { it.copy(isLoadingMore = false, nextAfter = feed.nextAfter,
                        activities = (it.activities + feed.activities).distinctBy(SocialActivity::id)) }
                    pages[snapshot.kind]?.let { cached ->
                        pages[snapshot.kind] = cached.copy(feed = cached.feed.copy(
                            activities = _state.value.activities, nextAfter = feed.nextAfter))
                    }
                }
                .onFailure { error ->
                    if (isCurrent(version, owner, dataRevision)) _state.update {
                        it.copy(isLoadingMore = false, loadMoreError = socialErrorMessage(error))
                    }
                }
        }
    }

    fun loadMine(forceRefresh: Boolean = true) {
        syncSession()
        if (mineJob?.isActive == true) return
        repository.cachedMine()?.takeIf {
            it.revision == revision && now() - it.savedAt in 0..SOCIAL_FEED_CACHE_MAX_AGE_MS && it.savedAt >= mineSavedAt
        }?.let { cached ->
            mineSavedAt = cached.savedAt
            _state.update { it.copy(mine = cached.mine, mineLoading = false) }
        }
        if (!forceRefresh && mineSavedAt != 0L && now() - mineSavedAt in 0 until SOCIAL_FEED_FRESH_MS) return
        val version = ++mineVersion
        val owner = uid
        val dataRevision = revision
        _state.update { it.copy(mineLoading = it.mine == null, mineRefreshing = it.mine != null, mineError = null) }
        mineJob = viewModelScope.launch {
            fun current() = version == mineVersion && owner == repository.currentUid && dataRevision == repository.revision
            homeAttempt { repository.mine() }
                .onSuccess { mine ->
                    if (!current()) return@onSuccess
                    mineSavedAt = now()
                    _state.update { it.copy(mineLoading = false, mineRefreshing = false, mine = mine) }
                }
                .onFailure { error ->
                    if (current()) _state.update { it.copy(mineLoading = false, mineRefreshing = false, mineError = socialErrorMessage(error)) }
                }
        }
    }

    private fun stopReadsForProfileChange() {
        ++requestVersion
        loadJob?.cancel()
        paginationJob?.cancel()
        _state.update { it.copy(isLoading = false, isRefreshing = false, isLoadingMore = false) }
    }

    fun acceptTerms(version: Int, showName: Boolean) {
        if (_state.value.acceptingTerms) return
        stopReadsForProfileChange()
        _state.update { it.copy(acceptingTerms = true, termsError = null) }
        viewModelScope.launch {
            homeAttempt { repository.acceptTerms(version, showName) }
                .onSuccess { _state.update { it.copy(acceptingTerms = false, me = it.me?.copy(
                    termsAccepted = true, nameMode = if (showName) "shown" else "masked")) } }
                .onFailure { error -> _state.update { it.copy(acceptingTerms = false, termsError = socialErrorMessage(error)) } }
            load()
            loadMine(forceRefresh = false)
        }
    }

    fun updateProfile(showName: Boolean? = null, photo: ByteArray? = null, removePhoto: Boolean = false) {
        if (_state.value.profileSaving) return
        stopReadsForProfileChange()
        _state.update { it.copy(profileSaving = true, profileError = null) }
        viewModelScope.launch {
            homeAttempt { repository.updateProfile(showName, photo, removePhoto) }
                .onSuccess { me -> _state.update { it.copy(profileSaving = false, me = me) } }
                .onFailure { error -> _state.update { it.copy(profileSaving = false, profileError = socialErrorMessage(error)) } }
            load()
            loadMine(forceRefresh = false)
        }
    }

    fun profilePhotoError(message: String) = _state.update { it.copy(profileError = UiText.DynamicString(message)) }
}

private suspend fun <T> homeAttempt(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    Result.failure(error)
}
