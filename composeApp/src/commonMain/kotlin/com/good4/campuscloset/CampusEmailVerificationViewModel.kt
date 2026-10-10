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

    fun setEmail(email: String) {
        if (state.value.isSending || state.value.isConfirming || state.value.sentTo != null) return
        mutableState.update { it.copy(email = email.take(254), error = null) }
    }

    fun onResume() {
        refreshCountdown()
        statusCheck?.cancel()
        statusCheck = viewModelScope.launch {
            try {
                repository.verifiedEmail()?.let { email ->
                    countdown?.cancel()
                    mutableState.update { it.copy(verifiedEmail = email, error = null) }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { /* Sending/confirmation exposes actionable errors. */ }
        }
    }
    fun onPause() { statusCheck?.cancel() }

    fun sendCode() {
        val snapshot = state.value
        if (snapshot.isSending || snapshot.isConfirming || snapshot.resendSeconds > 0 || snapshot.verifiedEmail != null) return
        val email = snapshot.sentTo ?: snapshot.email
        if (!isCampusStudentEmail(email)) {
            mutableState.update { it.copy(error = UiText.StringResourceId(Res.string.campus_closet_yalnizca_ogr_akdeniz_edu_tr_uzantili_ogrenci_adresleri_kabul)) }
            return
        }
        // Set synchronously: repeated taps cannot start overlapping requests.
        mutableState.update { it.copy(isSending = true, error = null) }
        viewModelScope.launch {
            try {
                when (val result = repository.requestCode(email)) {
                    is EduCodeRequestResult.AlreadyVerified -> mutableState.update { it.copy(isSending = false, verifiedEmail = result.email) }
                    is EduCodeRequestResult.Sent -> {
                        mutableState.update { it.copy(isSending = false, email = result.email, sentTo = result.email, code = "", codeBlocked = false) }
                        startCountdown(result.resendAfterSeconds)
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                mutableState.update { it.copy(isSending = false, error = campusCodeError(error)) }
                // Failed delivery never starts a cooldown. The server remains authoritative.
            }
        }
    }

    fun setCode(input: String) {
        val snapshot = state.value
        if (snapshot.isSending || snapshot.isConfirming || snapshot.codeBlocked || snapshot.sentTo == null || snapshot.verifiedEmail != null) return
        val code = input.filter { it in '0'..'9' }.take(6)
        if (code == snapshot.code) return
        mutableState.update { it.copy(code = code, error = null) }
        if (code.length == 6) confirmCode()
    }

    fun confirmCode() {
        val snapshot = state.value
        if (snapshot.isSending || snapshot.isConfirming || snapshot.codeBlocked || snapshot.code.length != 6 || snapshot.sentTo == null || snapshot.verifiedEmail != null) return
        mutableState.update { it.copy(isConfirming = true, error = null) }
        viewModelScope.launch {
            try {
                when (val result = repository.confirmCode(snapshot.code)) {
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
            catch (error: Exception) { mutableState.update { it.copy(isConfirming = false, error = campusCodeError(error)) } }
        }
    }

    fun changeEmail() {
        if (state.value.isSending || state.value.isConfirming) return
        mutableState.update { it.copy(sentTo = null, code = "", codeBlocked = false, error = null) }
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
    "CAMPUS_EMAIL_ACCOUNT_MISMATCH" -> UiText.StringResourceId(Res.string.campus_email_code_account_mismatch)
    "CAMPUS_EMAIL_PROOF_INVALID" -> UiText.StringResourceId(Res.string.campus_email_code_unavailable)
    "EDU_EMAIL_REQUIRED", "EDU_EMAIL_INVALID" -> UiText.StringResourceId(Res.string.campus_closet_yalnizca_ogr_akdeniz_edu_tr_uzantili_ogrenci_adresleri_kabul)
    "EDU_EMAIL_SEND_FAILED" -> UiText.StringResourceId(Res.string.campus_email_code_send_failed)
    "EDU_EMAIL_SEND_IN_PROGRESS", "EDU_CODE_RESEND_TOO_SOON" -> UiText.StringResourceId(Res.string.campus_email_code_wait)
    "EDU_CODE_SEND_LIMIT" -> UiText.StringResourceId(Res.string.campus_email_code_send_limit)
    "EDU_CODE_RECIPIENT_LIMIT" -> UiText.StringResourceId(Res.string.campus_email_code_recipient_limit)
    "EDU_CODE_ACCOUNT_DAILY_LIMIT" -> UiText.StringResourceId(Res.string.campus_email_code_account_daily_limit)
    "EDU_CODE_DAILY_LIMIT" -> UiText.StringResourceId(Res.string.campus_email_code_daily_limit)
    "EDU_CODE_FORMAT_INVALID" -> UiText.StringResourceId(Res.string.campus_email_code_label)
    "EDU_CODE_NOT_REQUESTED" -> UiText.StringResourceId(Res.string.campus_email_code_request_first)
    else -> campusEmailVerificationError(error)
}
