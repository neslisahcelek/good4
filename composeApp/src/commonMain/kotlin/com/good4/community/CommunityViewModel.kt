package com.good4.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CommunityState(
    val communities: List<Community> = emptyList(),
    val selected: Community? = null,
    val entries: List<CommunityEntry> = emptyList(),
    val access: CommunityAccessDto = CommunityAccessDto(),
    val businesses: List<CommunityBusiness> = emptyList(),
    val isFollowing: Boolean = false,
    val followLoading: Boolean = false,
    val generatedCouponCode: String? = null,
    val generatedCouponEntryId: String? = null,
    val codeGenerating: Boolean = false,
    val registrationsByEntry: Map<String, List<CommunityEventRegistrationDto>> = emptyMap(),
    val registeredEventIds: Set<String> = emptySet(),
    val registrationLoadingIds: Set<String> = emptySet(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null
) {
    val canManage: Boolean get() = access.active && selected?.id in access.communityIds
}

class CommunityViewModel(private val repository: CommunityRepository) : ViewModel() {
    private val mutable = MutableStateFlow(CommunityState())
    val state = mutable.asStateFlow()
    private var loadingJob: Job? = null
    private var registrationJob: Job? = null

    init { load() }

    fun load() {
        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
            mutable.update { it.copy(loading = true, error = null) }
            try {
                val communities = repository.list()
                val access = repository.access()
                val businesses = repository.businesses()
                mutable.update { it.copy(communities = communities, access = access, businesses = businesses, loading = false) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.update { it.copy(loading = false, error = e.message) } }
        }
    }

    fun select(community: Community) {
        loadingJob?.cancel()
        registrationJob?.cancel()
        mutable.update {
            it.copy(
                selected = community,
                entries = emptyList(),
                loading = true,
                error = null,
                generatedCouponCode = null,
                generatedCouponEntryId = null,
                registrationsByEntry = emptyMap(),
                registeredEventIds = emptySet()
            )
        }
        loadingJob = viewModelScope.launch {
            try {
                val access = repository.access()
                val manager = access.active && community.id in access.communityIds
                val entries = repository.entries(community.id, manager)
                val following = repository.isFollowing(community.id)
                val events = entries.filter { it.data.kind == "event" && it.data.status == "published" }
                val registrations = if (manager) {
                    events.associate { it.id to repository.registrations(community.id, it.id) }
                } else emptyMap()
                val registered = if (manager) emptySet() else {
                    events.filter { repository.isRegistered(community.id, it.id) }.mapTo(mutableSetOf()) { it.id }
                }
                mutable.update {
                    it.copy(
                        access = access,
                        entries = entries,
                        isFollowing = following,
                        registrationsByEntry = registrations,
                        registeredEventIds = registered,
                        loading = false
                    )
                }
                if (manager) startRegistrationUpdates(community.id)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.update { it.copy(loading = false, error = e.message) } }
        }
    }

    fun back() {
        loadingJob?.cancel()
        registrationJob?.cancel()
        mutable.update {
            it.copy(
                selected = null,
                error = null,
                loading = false,
                entries = emptyList(),
                generatedCouponCode = null,
                generatedCouponEntryId = null,
                registrationsByEntry = emptyMap(),
                registeredEventIds = emptySet()
            )
        }
    }
    fun clearError() { mutable.update { it.copy(error = null) } }

    fun save(entryId: String?, entry: CommunityEntryDto, image: ByteArray?, onSaved: () -> Unit) = change(onSaved) { community ->
        val url = image?.let { uploadCommunityImage(community.id, it) } ?: entry.imageUrl
        repository.saveEntry(community.id, entryId, entry.copy(imageUrl = url))
    }
    fun cancel(entryId: String, onSaved: () -> Unit) = change(onSaved) { community -> repository.cancelEntry(community.id, entryId) }
    fun updateProfile(data: CommunityDto, logo: ByteArray?, cover: ByteArray?, onSaved: () -> Unit) = change(onSaved) { community ->
        val updated = data.copy(
            logoUrl = logo?.let { uploadCommunityImage(community.id, it) } ?: data.logoUrl,
            coverUrl = cover?.let { uploadCommunityImage(community.id, it) } ?: data.coverUrl
        )
        repository.saveCommunity(community.id, updated)
        mutable.update { state -> state.copy(selected = Community(community.id, updated), communities = state.communities.map { if (it.id == community.id) Community(it.id, updated) else it }) }
    }

    fun toggleFollow() {
        val community = mutable.value.selected ?: return
        if (mutable.value.followLoading) return
        val next = !mutable.value.isFollowing
        viewModelScope.launch {
            mutable.update { it.copy(followLoading = true, error = null) }
            try {
                repository.setFollowing(community.id, next)
                mutable.update { it.copy(isFollowing = next, followLoading = false) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.update { it.copy(followLoading = false, error = e.message) } }
        }
    }

    fun toggleRegistration(entry: CommunityEntry) {
        val community = mutable.value.selected ?: return
        if (entry.id in mutable.value.registrationLoadingIds) return
        val next = entry.id !in mutable.value.registeredEventIds
        viewModelScope.launch {
            mutable.update { it.copy(registrationLoadingIds = it.registrationLoadingIds + entry.id, error = null) }
            try {
                repository.setRegistration(community.id, entry, next)
                mutable.update {
                    it.copy(
                        registeredEventIds = if (next) it.registeredEventIds + entry.id else it.registeredEventIds - entry.id,
                        registrationLoadingIds = it.registrationLoadingIds - entry.id
                    )
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                mutable.update { it.copy(registrationLoadingIds = it.registrationLoadingIds - entry.id, error = e.message) }
            }
        }
    }

    fun createCouponCode(entry: CommunityEntry) {
        val community = mutable.value.selected ?: return
        if (mutable.value.codeGenerating) return
        viewModelScope.launch {
            mutable.update { it.copy(codeGenerating = true, generatedCouponCode = null, error = null) }
            try {
                val code = repository.createCouponCode(community.id, entry)
                mutable.update { it.copy(codeGenerating = false, generatedCouponCode = code, generatedCouponEntryId = entry.id) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.update { it.copy(codeGenerating = false, error = e.message) } }
        }
    }

    private fun startRegistrationUpdates(communityId: String) {
        registrationJob?.cancel()
        registrationJob = viewModelScope.launch {
            while (mutable.value.selected?.id == communityId && mutable.value.canManage) {
                val events = mutable.value.entries.filter { it.data.kind == "event" && it.data.status == "published" }
                val refreshed = runCatching {
                    events.associate { it.id to repository.registrations(communityId, it.id) }
                }.getOrNull()
                if (refreshed != null) mutable.update { it.copy(registrationsByEntry = refreshed) }
                delay(3_000)
            }
        }
    }

    private fun change(onSaved: () -> Unit, action: suspend (Community) -> Unit) {
        val community = mutable.value.selected ?: return
        if (mutable.value.saving || !mutable.value.canManage) return
        viewModelScope.launch {
            mutable.update { it.copy(saving = true, error = null) }
            try {
                action(community)
                mutable.update { it.copy(saving = false) }
                onSaved()
                select(mutable.value.selected ?: community)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.update { it.copy(saving = false, error = e.message) } }
        }
    }
}
