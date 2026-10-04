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
import kotlinx.coroutines.launch

data class CampusEmailVerificationState(
    val email: String = "",
    val sentTo: String? = null,
    val verifiedEmail: String? = null,
    val isSending: Boolean = false,
    val isConfirming: Boolean = false,
    val resendSeconds: Int = 0,
    val hasReceivedLink: Boolean = false,
    val error: UiText? = null
)

class CampusEmailVerificationViewModel(private val repository: CampusEmailVerificationRepository) : ViewModel() {
    private val restored = repository.pending()
    private val mutableState = MutableStateFlow(CampusEmailVerificationState(
        email = restored?.email.orEmpty(), sentTo = restored?.email
    ))
    val state = mutableState.asStateFlow()
    private var countdown: Job? = null
    private var verificationWatch: Job? = null
    private var isVisible = false
    private var receivedLink: CampusEmailLink? = null

    init {
        viewModelScope.launch {
            CampusEmailVerificationLinks.pending.collect { link -> if (link != null) confirm(link) }
        }
    }

    fun setEmail(email: String) { mutableState.update { it.copy(email = email.take(254), error = null) } }

    fun onResume() {
        isVisible = true
        watchVerification()
    }

    fun onPause() {
        isVisible = false
        verificationWatch?.cancel()
    }

    private fun watchVerification() {
        if (!isVisible) return
        verificationWatch?.cancel()
        verificationWatch = viewModelScope.launch {
            do {
                if (!state.value.isConfirming && !state.value.isSending) {
                    try {
                        val email = repository.verifiedEmail()
                        if (email != null) {
                            countdown?.cancel()
                            receivedLink = null
                            mutableState.update { it.copy(verifiedEmail = email, error = null, hasReceivedLink = false) }
                            return@launch
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        // A temporary network error while checking is silent;
                        // foreground resume or the next tick retries it.
                    }
                }
                if (state.value.sentTo == null || repository.pending() == null) return@launch
                delay(5_000)
            } while (isVisible && state.value.verifiedEmail == null)
        }
    }

    fun sendLink() {
        val snapshot = state.value
        if (snapshot.isSending || snapshot.isConfirming || snapshot.resendSeconds > 0) return
        if (!isCampusStudentEmail(snapshot.email)) {
            mutableState.update { it.copy(error = UiText.StringResourceId(Res.string.campus_closet_yalnizca_ogr_akdeniz_edu_tr_uzantili_ogrenci_adresleri_kabul)) }
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(isSending = true, error = null) }
            try {
                when (val result = repository.request(snapshot.email)) {
                    is CampusEmailRequestResult.AlreadyVerified -> mutableState.update {
                        it.copy(isSending = false, verifiedEmail = result.email)
                    }
                    is CampusEmailRequestResult.Ready -> {
                        mutableState.update { it.copy(isSending = false, sentTo = result.pending.email) }
                        startCountdown(result.resendAfterSeconds)
                        watchVerification()
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.update { it.copy(isSending = false, error = campusEmailVerificationError(error)) }
                startCountdown(60)
            }
        }
    }

    fun retryLink() { receivedLink?.let { viewModelScope.launch { confirm(it) } } }

    fun changeEmail() {
        if (state.value.isSending || state.value.isConfirming) return
        repository.forgetPending()
        verificationWatch?.cancel()
        receivedLink = null
        CampusEmailVerificationLinks.pending.value?.let(CampusEmailVerificationLinks::clear)
        mutableState.update { it.copy(sentTo = null, error = null, hasReceivedLink = false) }
    }

    private suspend fun confirm(link: CampusEmailLink) {
        if (state.value.isConfirming) return
        receivedLink = link
        CampusEmailVerificationLinks.clear(link)
        mutableState.update { it.copy(isConfirming = true, error = null, hasReceivedLink = true) }
        try {
            val email = repository.complete(link)
            countdown?.cancel()
            verificationWatch?.cancel()
            receivedLink = null
            mutableState.update { it.copy(isConfirming = false, verifiedEmail = email, hasReceivedLink = false) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            mutableState.update { it.copy(isConfirming = false, error = campusEmailVerificationError(error)) }
        }
    }

    private fun startCountdown(seconds: Int) {
        countdown?.cancel()
        countdown = viewModelScope.launch {
            for (remaining in seconds downTo 0) {
                mutableState.update { it.copy(resendSeconds = remaining) }
                if (remaining > 0) delay(1_000)
            }
        }
    }
}
