package com.good4.student.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.auth.data.repository.AuthRepository
import com.good4.user.domain.UserRole
import config.ReleaseFeatures
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch

data class HomeLayoutState(
    val uid: String = "",
    val shortcuts: List<HomeShortcut> = emptyList(),
    val layout: HomeLayout = HomeLayout()
)

/** Used only by StudentHome and its editor route, including community managers with a student role. */
class HomeLayoutViewModel(
    private val auth: AuthRepository,
    private val store: HomeLayoutStore
) : ViewModel() {
    private val _state = MutableStateFlow(stateFor(auth.currentUser?.uid))
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            auth.authStateFlow.distinctUntilChangedBy { it?.uid }.collectLatest { user ->
                // Replace the previous account immediately, before observing layout edits.
                _state.value = stateFor(user?.uid)
                store.revision.collect { _state.value = stateFor(user?.uid) }
            }
        }
    }

    private fun stateFor(uid: String?): HomeLayoutState {
        if (uid.isNullOrBlank()) return HomeLayoutState()
        val shortcuts = availableHomeShortcuts(UserRole.STUDENT, ReleaseFeatures.suspendedMeals, ReleaseFeatures.campusCloset)
        return HomeLayoutState(uid, shortcuts, store.read(uid, shortcuts.map { it.definition }))
    }

    fun move(id: String, toVisible: Boolean, index: Int): Boolean {
        val current = _state.value
        // A pending gesture from the previous account must not write to its layout after sign-out.
        if (current.uid.isBlank() || auth.currentUser?.uid != current.uid) return false
        val updated = current.layout.move(id, toVisible, index) ?: return false
        if (updated != current.layout) {
            store.write(current.uid, updated)
            _state.value = current.copy(layout = updated)
        }
        return true
    }

    fun reset() {
        val current = _state.value
        if (current.uid.isBlank() || auth.currentUser?.uid != current.uid) return
        val defaults = HomeLayout().resolve(current.shortcuts.map { it.definition })
        store.write(current.uid, defaults)
        _state.value = current.copy(layout = defaults)
    }
}
