package com.good4.campuscloset

import good4.composeapp.generated.resources.*
import com.good4.core.presentation.UiText
import com.good4.eduverification.EduCodeConfirmResult
import com.good4.eduverification.EduCodeRequestResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.datetime.Clock
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
    val code: String = "",
    val verifiedEmail: String? = null,
    val testAvailable: Boolean = false,
    val sessionId: String? = null,
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val isConfirming: Boolean = false,
    val resendSeconds: Int = 0,
    val codeBlocked: Boolean = false,
    val error: UiText? = null
)

class CampusEmailVerificationViewModel(
    private val repository: CampusEmailCodeSource,
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) : ViewModel() {
    private val mutableState = MutableStateFlow(CampusEmailVerificationState())
    val state = mutableState.asStateFlow()
    private var countdown: Job? = null
    private var resendUntilMillis = 0L
    private var statusCheck: Job? = null
    private var revision = repository.accessRevision
    private var version = 0L

    fun setEmail(email: String) {
        if (state.value.testAvailable) return
        if (state.value.isSending || state.value.isConfirming || state.value.sentTo != null) return
        mutableState.update { it.copy(email = email.take(254), error = null) }
    }

    fun onResume() {
        if (revision != repository.accessRevision) {
            ++version
            revision = repository.accessRevision
            countdown?.cancel()
            resendUntilMillis = 0
            mutableState.update { CampusEmailVerificationState() }
        }
        mutableState.update { it.copy(isLoading = true) }
        refreshCountdown()
        statusCheck?.cancel()
        val currentVersion = version
        statusCheck = viewModelScope.launch {
            try {
                val status = repository.status()
                if (currentVersion != version) return@launch
                mutableState.update { it.copy(
                    isLoading = false,
                    verifiedEmail = if (status.campusAccessAllowed) status.email ?: status.testEmail else null,
                    testAvailable = status.testAvailable,
                    email = if (status.testAvailable) status.testEmail.orEmpty() else it.email,
                    error = null
                ) }
                if (status.resendAfterSeconds > state.value.resendSeconds) startCountdown(status.resendAfterSeconds)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { if (version == currentVersion) mutableState.update { it.copy(isLoading = false, error = campusCodeError(error)) } }
        }
    }
    fun onPause() { statusCheck?.cancel() }

    fun sendCode() {
        val snapshot = state.value
        if (snapshot.isLoading || snapshot.isSending || snapshot.isConfirming || snapshot.resendSeconds > 0 || snapshot.verifiedEmail != null) return
        val email = snapshot.sentTo ?: snapshot.email
        if (!snapshot.testAvailable && !isCampusStudentEmail(email)) {
            mutableState.update { it.copy(error = UiText.StringResourceId(Res.string.campus_closet_yalnizca_ogr_akdeniz_edu_tr_uzantili_ogrenci_adresleri_kabul)) }
            return
        }
        // Set synchronously: repeated taps cannot start overlapping requests.
        val currentVersion = version
        mutableState.update { it.copy(isSending = true, error = null) }
        viewModelScope.launch {
            try {
                val result = repository.requestCode(email)
                if (currentVersion != version) return@launch
                when (result) {
                    is EduCodeRequestResult.AlreadyVerified -> mutableState.update { it.copy(isSending = false, verifiedEmail = result.email) }
                    is EduCodeRequestResult.Sent -> {
                        mutableState.update { it.copy(isSending = false, email = result.email, sentTo = result.email, code = "", codeBlocked = false, sessionId = result.sessionId) }
                        startCountdown(result.resendAfterSeconds)
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (version == currentVersion) mutableState.update { it.copy(isSending = false, error = campusCodeError(error)) }
                // Failed delivery never starts a cooldown. The server remains authoritative.
            }
        }
    }

    fun setCode(input: String) {
        val snapshot = state.value
        if (snapshot.isLoading || snapshot.isSending || snapshot.isConfirming || snapshot.codeBlocked || snapshot.sentTo == null || snapshot.verifiedEmail != null) return
        val code = input.filter { it in '0'..'9' }.take(6)
        if (code == snapshot.code) return
        mutableState.update { it.copy(code = code, error = null) }
        if (code.length == 6) confirmCode()
    }

    fun confirmCode() {
        val snapshot = state.value
        if (snapshot.isLoading || snapshot.isSending || snapshot.isConfirming || snapshot.codeBlocked || snapshot.code.length != 6 || snapshot.sentTo == null || snapshot.verifiedEmail != null) return
        val currentVersion = version
        mutableState.update { it.copy(isConfirming = true, error = null) }
        viewModelScope.launch {
            try {
                val result = repository.confirmCode(snapshot.code, snapshot.sessionId)
                if (currentVersion != version) return@launch
                when (result) {
                    is EduCodeConfirmResult.Verified -> {
                        countdown?.cancel()
                        mutableState.update { it.copy(isConfirming = false, verifiedEmail = result.email, code = "") }
                    }
                    is EduCodeConfirmResult.InvalidCode -> mutableState.update {
                        it.copy(isConfirming = false, code = "", codeBlocked = result.attemptsLeft == 0,
                            error = if (result.attemptsLeft == 0) UiText.StringResourceId(Res.string.campus_email_code_limit)
                            else UiText.StringResourceId(Res.string.campus_email_code_wrong, arrayOf(result.attemptsLeft)))
                    }
                    EduCodeConfirmResult.Expired -> mutableState.update {
                        it.copy(isConfirming = false, code = "", codeBlocked = true, error = UiText.StringResourceId(Res.string.campus_email_code_expired))
                    }
                    EduCodeConfirmResult.TooManyAttempts -> mutableState.update {
                        it.copy(isConfirming = false, code = "", codeBlocked = true, error = UiText.StringResourceId(Res.string.campus_email_code_limit))
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { if (version == currentVersion) mutableState.update { it.copy(isConfirming = false, error = campusCodeError(error)) } }
        }
    }

    fun changeEmail() {
        if (state.value.isSending || state.value.isConfirming) return
        mutableState.update { it.copy(sentTo = null, code = "", sessionId = null, codeBlocked = false, error = null) }
        // Changing the address does not bypass the account's resend countdown.
    }

    private fun refreshCountdown() {
        val remaining = ((resendUntilMillis - nowMillis() + 999) / 1_000).coerceAtLeast(0).toInt()
        mutableState.update { it.copy(resendSeconds = remaining) }
    }

    private fun startCountdown(seconds: Int) {
        countdown?.cancel()
        resendUntilMillis = nowMillis() + seconds * 1_000L
        refreshCountdown()
        countdown = viewModelScope.launch {
            while (state.value.resendSeconds > 0) {
                delay(1_000)
                refreshCountdown()
            }
        }
    }

}

internal fun campusCodeError(error: Throwable): UiText = when (error.message) {
    "CAMPUS_TEST_EMAIL_UNAVAILABLE", "CAMPUS_TEST_RECIPIENT_FIXED", "CAMPUS_TEST_SESSION_INVALID" -> UiText.StringResourceId(Res.string.campus_test_unavailable)
    "CAMPUS_EMAIL_ACCOUNT_MISMATCH" -> UiText.StringResourceId(Res.string.campus_email_code_account_mismatch)
    "CAMPUS_EMAIL_PROOF_INVALID" -> UiText.StringResourceId(Res.string.campus_email_code_unavailable)
    "EDU_EMAIL_REQUIRED", "EDU_EMAIL_INVALID" -> UiText.StringResourceId(Res.string.campus_closet_yalnizca_ogr_akdeniz_edu_tr_uzantili_ogrenci_adresleri_kabul)
    "EDU_EMAIL_SEND_FAILED" -> UiText.StringResourceId(Res.string.campus_email_code_send_failed)
    "EDU_EMAIL_SEND_IN_PROGRESS", "EDU_CODE_RESEND_TOO_SOON", "EDU_CODE_REQUEST_LIMIT" -> UiText.StringResourceId(Res.string.campus_email_code_wait)
    "EDU_CODE_SEND_LIMIT" -> UiText.StringResourceId(Res.string.campus_email_code_send_limit)
    "EDU_CODE_RECIPIENT_LIMIT" -> UiText.StringResourceId(Res.string.campus_email_code_recipient_limit)
    "EDU_CODE_ACCOUNT_DAILY_LIMIT" -> UiText.StringResourceId(Res.string.campus_email_code_account_daily_limit)
    "EDU_CODE_DAILY_LIMIT" -> UiText.StringResourceId(Res.string.campus_email_code_daily_limit)
    "EDU_CODE_FORMAT_INVALID" -> UiText.StringResourceId(Res.string.campus_email_code_label)
    "EDU_CODE_NOT_REQUESTED" -> UiText.StringResourceId(Res.string.campus_email_code_request_first)
    else -> campusEmailVerificationError(error)
}
