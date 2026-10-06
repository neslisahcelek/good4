package com.good4.campuscloset

import good4.composeapp.generated.resources.*
import com.good4.core.presentation.UiText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

private suspend fun <T> attempt(block: suspend () -> T): kotlin.Result<T> = try {
    kotlin.Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Exception) {
    kotlin.Result.failure(error)
}

// ---------------------------------------------------------------------------
// Feed
// ---------------------------------------------------------------------------

data class CampusClosetFeedState(
    val isLoading: Boolean = true,
    val loadError: UiText? = null,
    val me: MarketMe? = null,
    val category: String? = null,
    val query: String = "",
    val listings: List<MarketListing> = emptyList(),
    val nextBefore: String? = null,
    val isLoadingMore: Boolean = false,
    val acceptingTerms: Boolean = false,
    val termsError: UiText? = null,
    val message: UiText? = null
)

class CampusClosetFeedViewModel(private val repository: CampusClosetFeedDataSource) : ViewModel() {
    private val _state = MutableStateFlow(CampusClosetFeedState())
    val state = _state.asStateFlow()
    private var loadJob: Job? = null
    private var searchJob: Job? = null
    private var paginationJob: Job? = null
    private var requestVersion = 0L

    fun load() {
        val category = _state.value.category
        val query = _state.value.query
        val version = ++requestVersion
        loadJob?.cancel()
        paginationJob?.cancel()
        _state.update { it.copy(isLoadingMore = false) }
        // Show the page saved on this device at once; the server's answer replaces it a moment later.
        if (_state.value.listings.isEmpty() && category == null && query.isBlank()) {
            repository.cachedFeed()?.let { cached ->
                _state.update { it.copy(isLoading = false, me = cached.me, listings = cached.listings, nextBefore = cached.nextBefore) }
            }
        }
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = it.listings.isEmpty(), loadError = null) }
            attempt { repository.feed(category, null, query) }
                .onSuccess { feed ->
                    if (version != requestVersion) return@onSuccess
                    _state.update {
                        it.copy(isLoading = false, me = feed.me, listings = feed.listings, nextBefore = feed.nextBefore)
                    }
                }
                .onFailure { error ->
                    if (version == requestVersion) _state.update { it.copy(isLoading = false, loadError = campusClosetErrorMessage(error)) }
                }
        }
    }

    fun selectCategory(category: String?) {
        if (category == _state.value.category) return
        searchJob?.cancel()
        _state.update { it.copy(category = category, listings = emptyList(), nextBefore = null, message = null) }
        load()
    }

    /** Searches every listing on the server, waiting for a short typing pause. */
    fun setQuery(value: String) {
        val query = value.take(60)
        if (query == _state.value.query) return
        ++requestVersion
        loadJob?.cancel()
        paginationJob?.cancel()
        _state.update { it.copy(query = query, isLoadingMore = false, message = null) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            _state.update { it.copy(listings = emptyList(), nextBefore = null) }
            load()
        }
    }

    /** Optimistic: the heart turns at once and reverts if the server refuses. */
    fun toggleFavorite(listing: MarketListing) {
        val saved = !listing.isFavorite
        fun mark(value: Boolean) = _state.update { state ->
            state.copy(listings = state.listings.map { if (it.id == listing.id) it.copy(isFavorite = value) else it })
        }
        mark(saved)
        viewModelScope.launch {
            attempt { repository.setFavorite(listing.id, saved) }
                .onFailure { error -> mark(!saved); _state.update { it.copy(message = campusClosetErrorMessage(error)) } }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    fun loadMore() {
        val snapshot = _state.value
        val before = snapshot.nextBefore ?: return
        if (snapshot.isLoadingMore || snapshot.isLoading) return
        val version = requestVersion
        _state.update { it.copy(isLoadingMore = true, message = null) }
        paginationJob = viewModelScope.launch {
            attempt { repository.feed(snapshot.category, before, snapshot.query) }
                .onSuccess { feed ->
                    if (version != requestVersion) return@onSuccess
                    _state.update {
                        it.copy(isLoadingMore = false,
                            listings = (it.listings + feed.listings).distinctBy(MarketListing::id), nextBefore = feed.nextBefore)
                    }
                }
                .onFailure { error ->
                    if (version == requestVersion) _state.update {
                        it.copy(isLoadingMore = false, message = campusClosetErrorMessage(error))
                    }
                }
        }
    }

    fun acceptTerms(version: Int) {
        if (_state.value.acceptingTerms) return
        viewModelScope.launch {
            _state.update { it.copy(acceptingTerms = true, termsError = null) }
            attempt { repository.acceptTerms(version) }
                .onSuccess {
                    _state.update { it.copy(acceptingTerms = false, me = it.me?.copy(termsAccepted = true)) }
                }
                .onFailure { error ->
                    _state.update { it.copy(acceptingTerms = false, termsError = campusClosetErrorMessage(error)) }
                }
        }
    }
}

// ---------------------------------------------------------------------------
// Listing detail
// ---------------------------------------------------------------------------

data class CampusClosetPriceEditState(
    val listingId: String,
    val currentPrice: Int,
    val priceText: String = if (currentPrice > 0) currentPrice.toString() else "",
    val isFree: Boolean = currentPrice == 0
) {
    val price: Int? get() = if (isFree) 0 else priceText.toIntOrNull()
    val canSave: Boolean get() = price?.let {
        it in 0..CampusClosetLimits.MAX_PRICE && (isFree || it > 0) && it != currentPrice
    } == true
    fun withPrice(value: String) = copy(priceText = value.filter(Char::isDigit).take(6))
}

data class CampusClosetListingState(
    val isLoading: Boolean = true,
    val loadError: UiText? = null,
    val detail: MarketListingDetail? = null,
    val busy: Boolean = false,
    val message: UiText? = null,
    val openConversationId: String? = null,
    val removed: Boolean = false,
    val priceEdit: CampusClosetPriceEditState? = null
)

class CampusClosetListingViewModel(private val repository: CampusClosetRepository) : ViewModel() {
    private val _state = MutableStateFlow(CampusClosetListingState())
    val state = _state.asStateFlow()

    fun load(listingId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = it.detail == null, loadError = null) }
            attempt { repository.listing(listingId) }
                .onSuccess { detail -> _state.update { it.copy(isLoading = false, detail = detail) } }
                .onFailure { error -> _state.update { it.copy(isLoading = false, loadError = campusClosetErrorMessage(error)) } }
        }
    }

    /** Opens the buyer's conversation; it is created by the first message sent from the chat screen. */
    fun openChat() {
        val detail = _state.value.detail ?: return
        val conversationId = detail.conversationId ?: repository.conversationIdFor(detail.listing.id) ?: return
        _state.update { it.copy(openConversationId = conversationId) }
    }

    fun sendOffer(percent: Int) {
        val detail = _state.value.detail ?: return
        val conversationId = detail.conversationId ?: repository.conversationIdFor(detail.listing.id) ?: return
        runAction {
            repository.sendOffer(conversationId, percent)
            _state.update { it.copy(openConversationId = conversationId) }
        }
    }

    fun updateStatus(action: String) {
        val listingId = _state.value.detail?.listing?.id ?: return
        runAction {
            repository.updateListingStatus(listingId, action)
            if (action == "remove") _state.update { it.copy(removed = true) } else load(listingId)
        }
    }

    fun report(reason: String, note: String) {
        val listingId = _state.value.detail?.listing?.id ?: return
        runAction {
            repository.report("listing", listingId, reason, note)
            _state.update { it.copy(message = UiText.StringResourceId(Res.string.campus_closet_sikayetin_alindi_good4_ekibi_inceleyecek)) }
        }
    }

    fun beginPriceEdit() {
        val listing = _state.value.detail?.listing ?: return
        _state.update { it.copy(priceEdit = CampusClosetPriceEditState(listing.id, listing.price)) }
    }
    fun setEditPrice(value: String) = _state.update { it.copy(priceEdit = it.priceEdit?.withPrice(value)) }
    fun setEditFree(value: Boolean) = _state.update { it.copy(priceEdit = it.priceEdit?.copy(isFree = value)) }
    fun dismissPriceEdit() = _state.update { it.copy(priceEdit = null) }
    fun savePriceEdit() {
        val draft = _state.value.priceEdit ?: return
        if (!draft.canSave || _state.value.busy) return
        dismissPriceEdit()
        updatePrice(draft.price!!)
    }

    fun updatePrice(price: Int) {
        val listingId = _state.value.detail?.listing?.id ?: return
        runAction {
            repository.updatePrice(listingId, price)
            _state.update { state ->
                state.copy(
                    detail = state.detail?.let { it.copy(listing = it.listing.copy(price = price)) },
                    message = UiText.StringResourceId(Res.string.campus_closet_fiyat_guncellendi)
                )
            }
        }
    }

    fun renew() {
        val listingId = _state.value.detail?.listing?.id ?: return
        runAction {
            repository.renew(listingId)
            load(listingId)
            _state.update { it.copy(message = UiText.StringResourceId(Res.string.campus_closet_ilan_30_gun_daha_yayinda)) }
        }
    }

    fun toggleFavorite() {
        val listing = _state.value.detail?.listing ?: return
        val saved = !listing.isFavorite
        fun mark(value: Boolean) = _state.update { state ->
            state.copy(detail = state.detail?.let { it.copy(listing = it.listing.copy(isFavorite = value)) })
        }
        mark(saved)
        viewModelScope.launch {
            attempt { repository.setFavorite(listing.id, saved) }
                .onFailure { error -> mark(!saved); _state.update { it.copy(message = campusClosetErrorMessage(error)) } }
        }
    }

    fun consumeNavigation() = _state.update { it.copy(openConversationId = null) }
    fun clearMessage() = _state.update { it.copy(message = null) }

    private fun runAction(block: suspend () -> Unit) {
        if (_state.value.busy) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            attempt { block() }
                .onSuccess { _state.update { it.copy(busy = false) } }
                .onFailure { error -> _state.update { it.copy(busy = false, message = campusClosetErrorMessage(error)) } }
        }
    }
}

// ---------------------------------------------------------------------------
// New listing
// ---------------------------------------------------------------------------

data class CampusClosetNewListingState(
    val category: String? = null,
    val condition: String? = null,
    val title: String = "",
    val description: String = "",
    val priceText: String = "",
    val isFree: Boolean = false,
    val photos: List<ByteArray> = emptyList(),
    val submitting: Boolean = false,
    val error: UiText? = null,
    val submitted: Boolean = false
) {
    val price: Int? get() = if (isFree) 0 else priceText.toIntOrNull()
    val validationMessage: UiText?
        get() = when {
            photos.isEmpty() -> UiText.StringResourceId(Res.string.campus_closet_en_az_1_fotograf_ekle)
            category == null -> UiText.StringResourceId(Res.string.campus_closet_urunun_kategorisini_sec)
            condition == null -> UiText.StringResourceId(Res.string.campus_closet_urunun_durumunu_sec)
            title.trim().length < 3 -> UiText.StringResourceId(Res.string.campus_closet_baslik_en_az_3_karakter_olmali)
            description.trim().length < 10 -> UiText.StringResourceId(Res.string.campus_closet_aciklama_en_az_10_karakter_olmali)
            price == null -> UiText.StringResourceId(Res.string.campus_closet_bir_fiyat_yaz_veya_ucretsiz_secenegini_ac)
            price!! !in 0..CampusClosetLimits.MAX_PRICE -> UiText.StringResourceId(Res.string.campus_closet_fiyat_100_000_yi_gecemez)
            !isFree && price == 0 -> UiText.StringResourceId(Res.string.campus_closet_0_icin_ucretsiz_secenegini_ac)
            else -> null
        }
    val canSubmit: Boolean
        get() = !submitting && !submitted && validationMessage == null
}

class CampusClosetNewListingViewModel(private val repository: CampusClosetRepository) : ViewModel() {
    private val _state = MutableStateFlow(CampusClosetNewListingState())
    val state = _state.asStateFlow()

    fun setCategory(value: String) = _state.update { it.copy(category = value, error = null) }
    fun setCondition(value: String) = _state.update { it.copy(condition = value, error = null) }
    fun setTitle(value: String) = _state.update { it.copy(title = value.take(CampusClosetLimits.MAX_TITLE), error = null) }
    fun setDescription(value: String) =
        _state.update { it.copy(description = value.take(CampusClosetLimits.MAX_DESCRIPTION), error = null) }
    fun setPrice(value: String) = _state.update { it.copy(priceText = value.filter(Char::isDigit).take(6), error = null) }
    fun setFree(value: Boolean) = _state.update { it.copy(isFree = value, error = null) }
    fun addPhoto(bytes: ByteArray) = _state.update {
        if (it.photos.size >= CampusClosetLimits.MAX_PHOTOS) it else it.copy(photos = it.photos + bytes, error = null)
    }
    fun removePhoto(index: Int) = _state.update { it.copy(photos = it.photos.filterIndexed { i, _ -> i != index }) }
    fun showError(message: String) = showError(UiText.DynamicString(message))
    private fun showError(message: UiText) = _state.update { it.copy(error = message) }

    fun submit() {
        val snapshot = _state.value
        if (!snapshot.canSubmit) {
            snapshot.validationMessage?.let(::showError)
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, error = null) }
            attempt {
                repository.createListing(
                    NewListingDraft(
                        category = snapshot.category!!,
                        condition = snapshot.condition!!,
                        title = snapshot.title.trim(),
                        description = snapshot.description.trim(),
                        price = snapshot.price!!
                    ),
                    snapshot.photos
                )
            }
                .onSuccess { _state.update { it.copy(submitting = false, submitted = true) } }
                .onFailure { error -> _state.update { it.copy(submitting = false, error = campusClosetErrorMessage(error)) } }
        }
    }
}

// ---------------------------------------------------------------------------
// My listings and inbox
// ---------------------------------------------------------------------------

data class CampusClosetMyListingsState(
    val isLoading: Boolean = true,
    val loadError: UiText? = null,
    val listings: List<MarketListing> = emptyList(),
    val busyId: String? = null,
    val message: UiText? = null,
    val priceEdit: CampusClosetPriceEditState? = null
)

class CampusClosetMyListingsViewModel(private val repository: CampusClosetRepository) : ViewModel() {
    private val _state = MutableStateFlow(CampusClosetMyListingsState())
    val state = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = it.listings.isEmpty(), loadError = null) }
            attempt { repository.myListings() }
                .onSuccess { listings -> _state.update { it.copy(isLoading = false, listings = listings) } }
                .onFailure { error -> _state.update { it.copy(isLoading = false, loadError = campusClosetErrorMessage(error)) } }
        }
    }

    fun updateStatus(listingId: String, action: String) {
        if (_state.value.busyId != null) return
        viewModelScope.launch {
            _state.update { it.copy(busyId = listingId, message = null) }
            attempt { repository.updateListingStatus(listingId, action) }
                .onSuccess { _state.update { it.copy(busyId = null) }; load() }
                .onFailure { error -> _state.update { it.copy(busyId = null, message = campusClosetErrorMessage(error)) } }
        }
    }

    fun renew(listingId: String) {
        if (_state.value.busyId != null) return
        viewModelScope.launch {
            _state.update { it.copy(busyId = listingId, message = null) }
            attempt { repository.renew(listingId) }
                .onSuccess { _state.update { it.copy(busyId = null) }; load() }
                .onFailure { error -> _state.update { it.copy(busyId = null, message = campusClosetErrorMessage(error)) } }
        }
    }

    fun beginPriceEdit(listing: MarketListing) = _state.update {
        it.copy(priceEdit = CampusClosetPriceEditState(listing.id, listing.price))
    }
    fun setEditPrice(value: String) = _state.update { it.copy(priceEdit = it.priceEdit?.withPrice(value)) }
    fun setEditFree(value: Boolean) = _state.update { it.copy(priceEdit = it.priceEdit?.copy(isFree = value)) }
    fun dismissPriceEdit() = _state.update { it.copy(priceEdit = null) }
    fun savePriceEdit() {
        val draft = _state.value.priceEdit ?: return
        if (!draft.canSave || _state.value.busyId != null) return
        dismissPriceEdit()
        updatePrice(draft.listingId, draft.price!!)
    }

    fun updatePrice(listingId: String, price: Int) {
        if (_state.value.busyId != null) return
        viewModelScope.launch {
            _state.update { it.copy(busyId = listingId, message = null) }
            attempt { repository.updatePrice(listingId, price) }
                .onSuccess {
                    _state.update { state ->
                        state.copy(busyId = null, listings = state.listings.map { if (it.id == listingId) it.copy(price = price) else it })
                    }
                }
                .onFailure { error -> _state.update { it.copy(busyId = null, message = campusClosetErrorMessage(error)) } }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}

data class CampusClosetInboxState(
    val isLoading: Boolean = true,
    val loadError: UiText? = null,
    val conversations: List<MarketConversation> = emptyList()
)

class CampusClosetInboxViewModel(private val repository: CampusClosetRepository) : ViewModel() {
    private val _state = MutableStateFlow(CampusClosetInboxState())
    val state = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = it.conversations.isEmpty(), loadError = null) }
            attempt { repository.inbox() }
                .onSuccess { inbox -> _state.update { it.copy(isLoading = false, conversations = inbox.conversations) } }
                .onFailure { error -> _state.update { it.copy(isLoading = false, loadError = campusClosetErrorMessage(error)) } }
        }
    }
}

// ---------------------------------------------------------------------------
// Chat
// ---------------------------------------------------------------------------

data class CampusClosetChatState(
    val isLoading: Boolean = true,
    val loadError: UiText? = null,
    /** True while the buyer has not sent the first message yet. */
    val isNew: Boolean = false,
    val conversation: MarketConversation? = null,
    val messages: List<MarketMessage> = emptyList(),
    val draft: String = "",
    val sending: Boolean = false,
    val error: UiText? = null,
    val info: UiText? = null
)

class CampusClosetChatViewModel(private val repository: CampusClosetRepository) : ViewModel() {
    private val _state = MutableStateFlow(CampusClosetChatState())
    val state = _state.asStateFlow()
    private var conversationId: String = ""
    private var pollJob: Job? = null
    private var lastActivityAt = 0L

    fun load(id: String) {
        conversationId = id
        viewModelScope.launch { refresh(full = true) }
    }

    /** Polls while the chat is on screen: every 4 s after recent activity, every 15 s when quiet. */
    fun startPolling() {
        pollJob?.cancel()
        markActivity()
        pollJob = viewModelScope.launch {
            while (isActive) {
                val quiet = Clock.System.now().toEpochMilliseconds() - lastActivityAt > 120_000
                delay(if (quiet) 15_000 else 4_000)
                if (!_state.value.isNew && _state.value.loadError == null) refresh(full = false)
            }
        }
    }

    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    private fun markActivity() {
        lastActivityAt = Clock.System.now().toEpochMilliseconds()
    }

    private suspend fun refresh(full: Boolean) {
        val after = if (full) null else _state.value.messages.lastOrNull()?.createdAt
        attempt { repository.messages(conversationId, after) }
            .onSuccess { thread ->
                if (thread.messages.isNotEmpty() && !full) markActivity()
                _state.update {
                    it.copy(
                        isLoading = false,
                        isNew = false,
                        loadError = null,
                        conversation = thread.conversation,
                        messages = if (full) thread.messages else (it.messages + thread.messages).distinctBy(MarketMessage::id)
                    )
                }
            }
            .onFailure { error ->
                val ownNewConversation = error.message == "MARKET_NOT_PARTICIPANT"
                    && repository.currentUid?.let { conversationId.endsWith("_$it") } == true
                _state.update {
                    when {
                        ownNewConversation -> it.copy(isLoading = false, isNew = true)
                        full -> it.copy(isLoading = false, loadError = campusClosetErrorMessage(error))
                        else -> it
                    }
                }
            }
    }

    fun setDraft(value: String) = _state.update { it.copy(draft = value.take(CampusClosetLimits.MAX_MESSAGE), error = null) }

    fun appendMeetingPoint(point: String) {
        viewModelScope.launch {
            appendToDraft(org.jetbrains.compose.resources.getString(Res.string.campus_closet_bulusma_icin_uygun_mu_hangi_saat_olur, point))
        }
    }

    fun appendToDraft(text: String) = _state.update {
        val joined = if (it.draft.isBlank()) text else "${it.draft.trimEnd()} $text"
        it.copy(draft = joined.take(CampusClosetLimits.MAX_MESSAGE))
    }

    fun send() {
        val text = _state.value.draft.trim()
        if (text.isEmpty() || _state.value.sending) return
        runSend(clearDraft = true) { repository.sendMessage(conversationId, text) }
    }

    fun sendOffer(percent: Int) = runSend(clearDraft = false) { repository.sendOffer(conversationId, percent) }

    fun respondOffer(accept: Boolean) = runSend(clearDraft = false) { repository.respondOffer(conversationId, accept) }

    fun block() = runSend(clearDraft = false) {
        repository.block(conversationId)
        _state.update { it.copy(info = UiText.StringResourceId(Res.string.campus_closet_kullanici_engellendi_bu_konusmaya_artik_mesaj_gonderilemez)) }
    }

    fun unblock() = runSend(clearDraft = false) {
        repository.unblock(conversationId)
        _state.update { it.copy(info = UiText.StringResourceId(Res.string.campus_closet_engel_kaldirildi)) }
    }

    fun report(reason: String, note: String) = runSend(clearDraft = false) {
        repository.report("conversation", conversationId, reason, note)
        _state.update { it.copy(info = UiText.StringResourceId(Res.string.campus_closet_sikayetin_alindi_good4_ekibi_konusmayi_inceleyecek)) }
    }

    fun clearInfo() = _state.update { it.copy(info = null) }

    private fun runSend(clearDraft: Boolean, block: suspend () -> Unit) {
        if (_state.value.sending) return
        viewModelScope.launch {
            _state.update { it.copy(sending = true, error = null) }
            attempt { block() }
                .onSuccess {
                    markActivity()
                    _state.update { it.copy(sending = false, draft = if (clearDraft) "" else it.draft) }
                    refresh(full = _state.value.messages.isEmpty())
                }
                .onFailure { error -> _state.update { it.copy(sending = false, error = campusClosetErrorMessage(error)) } }
        }
    }
}

// ---------------------------------------------------------------------------
// Saved listings and blocked users
// ---------------------------------------------------------------------------

data class CampusClosetFavoritesState(
    val isLoading: Boolean = true,
    val loadError: UiText? = null,
    val listings: List<MarketListing> = emptyList(),
    val message: UiText? = null
)

class CampusClosetFavoritesViewModel(private val repository: CampusClosetRepository) : ViewModel() {
    private val _state = MutableStateFlow(CampusClosetFavoritesState())
    val state = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = it.listings.isEmpty(), loadError = null) }
            attempt { repository.favorites() }
                .onSuccess { listings -> _state.update { it.copy(isLoading = false, listings = listings) } }
                .onFailure { error -> _state.update { it.copy(isLoading = false, loadError = campusClosetErrorMessage(error)) } }
        }
    }

    fun remove(listing: MarketListing) {
        val before = _state.value.listings
        _state.update { state -> state.copy(listings = state.listings.filterNot { it.id == listing.id }) }
        viewModelScope.launch {
            attempt { repository.setFavorite(listing.id, false) }
                .onFailure { error -> _state.update { it.copy(listings = before, message = campusClosetErrorMessage(error)) } }
        }
    }
}

data class CampusClosetBlockedState(
    val isLoading: Boolean = true,
    val loadError: UiText? = null,
    val blocked: List<MarketBlockedUser> = emptyList(),
    val busyId: String? = null,
    val message: UiText? = null
)

class CampusClosetBlockedViewModel(private val repository: CampusClosetRepository) : ViewModel() {
    private val _state = MutableStateFlow(CampusClosetBlockedState())
    val state = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = it.blocked.isEmpty(), loadError = null) }
            attempt { repository.blocked() }
                .onSuccess { blocked -> _state.update { it.copy(isLoading = false, blocked = blocked) } }
                .onFailure { error -> _state.update { it.copy(isLoading = false, loadError = campusClosetErrorMessage(error)) } }
        }
    }

    fun unblock(conversationId: String) {
        if (_state.value.busyId != null) return
        viewModelScope.launch {
            _state.update { it.copy(busyId = conversationId, message = null) }
            attempt { repository.unblock(conversationId) }
                .onSuccess {
                    _state.update { state ->
                        state.copy(busyId = null, blocked = state.blocked.filterNot { it.conversationId == conversationId })
                    }
                }
                .onFailure { error -> _state.update { it.copy(busyId = null, message = campusClosetErrorMessage(error)) } }
        }
    }
}
