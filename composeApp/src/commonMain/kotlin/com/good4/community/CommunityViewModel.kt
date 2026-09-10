package com.good4.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CommunityState(
    val communities: List<Community> = emptyList(),
    val selected: Community? = null,
    val entries: List<CommunityEntry> = emptyList(),
    val access: CommunityAccessDto = CommunityAccessDto(),
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

    init { load() }

    fun load() {
        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
            mutable.update { it.copy(loading = true, error = null) }
            try {
                val communities = repository.list()
                val access = repository.access()
                mutable.update { it.copy(communities = communities, access = access, loading = false) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.update { it.copy(loading = false, error = e.message) } }
        }
    }

    fun select(community: Community) {
        loadingJob?.cancel()
        mutable.update { it.copy(selected = community, entries = emptyList(), loading = true, error = null) }
        loadingJob = viewModelScope.launch {
            try {
                val access = repository.access()
                val entries = repository.entries(community.id, access.active && community.id in access.communityIds)
                mutable.update { it.copy(access = access, entries = entries, loading = false) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.update { it.copy(loading = false, error = e.message) } }
        }
    }

    fun back() { loadingJob?.cancel(); mutable.update { it.copy(selected = null, error = null, loading = false, entries = emptyList()) } }
    fun clearError() { mutable.update { it.copy(error = null) } }

    fun save(entryId: String?, entry: CommunityEntryDto, image: ByteArray?, onSaved: () -> Unit) = change(onSaved) { community ->
        val url = image?.let { uploadCommunityImage(community.id, it) } ?: entry.imageUrl
        repository.saveEntry(community.id, entryId, entry.copy(imageUrl = url))
    }
    fun cancel(entryId: String, onSaved: () -> Unit) = change(onSaved) { community -> repository.cancelEntry(community.id, entryId) }
    fun updateProfile(data: CommunityDto, image: ByteArray?, onSaved: () -> Unit) = change(onSaved) { community ->
        val updated = data.copy(logoUrl = image?.let { uploadCommunityImage(community.id, it) } ?: data.logoUrl)
        repository.saveCommunity(community.id, updated)
        mutable.update { state -> state.copy(selected = Community(community.id, updated), communities = state.communities.map { if (it.id == community.id) Community(it.id, updated) else it }) }
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
