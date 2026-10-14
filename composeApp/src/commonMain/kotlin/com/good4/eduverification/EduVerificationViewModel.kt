package com.good4.eduverification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.analytics.AnalyticsTracker
import com.good4.analytics.Interaction
import com.good4.analytics.InteractionStatus
import com.good4.analytics.NoOpAnalyticsTracker
import com.good4.campuscloset.isCampusStudentEmail
import com.good4.core.presentation.UiText
import good4.composeapp.generated.resources.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EduVerificationState(
    val isLoading: Boolean = true,
    val loadError: UiText? = null,
    val isVerified: Boolean = false,
    val campusAccessAllowed: Boolean = isVerified,
    val accessSource: String = "none",
    val testAvailable: Boolean = false,
    val isDisconnecting: Boolean = false,
    val sessionId: String? = null,
    val verifiedEmail: String? = null,
    val optedIn: Boolean = false,
    val email: String = "",
    val code: String = "",
    val codeSentTo: String? = null,
    val resendSeconds: Int = 0,
    val isSending: Boolean = false,
    val isConfirming: Boolean = false,
    val errorMessage: UiText? = null
)

class EduVerificationViewModel(
    private val repository: EduVerificationSource,
    private val analytics: AnalyticsTracker = NoOpAnalyticsTracker
) : ViewModel() {

    private val _state = MutableStateFlow(EduVerificationState())
    val state = _state.asStateFlow()
    private var countdown: Job? = null

    private var version = 0L
    private var operationGeneration = 0L
    private var refreshJob: Job? = null
    init {
        refresh()
        viewModelScope.launch {
            var revision = CampusAccessSession.revision(repository.currentUid)
            CampusAccessSession.changes.collect { change ->
                if (change.uid == repository.currentUid && change.revision != revision) {
                    revision = change.revision
                    ++operationGeneration
                    countdown?.cancel()
                    _state.update { EduVerificationState() }
                    refresh()
                }
            }
        }
    }

    fun refresh() {
        val owner = repository.currentUid
        val currentVersion = ++version
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadError = null) }
            try {
                val status = repository.status()
                if (owner != repository.currentUid || version != currentVersion) return@launch
                _state.update {
                    it.copy(isLoading = false, isVerified = status.verified, campusAccessAllowed = status.campusAccessAllowed,
                        accessSource = status.accessSource, testAvailable = status.testAvailable, isDisconnecting = false,
                        email = if (status.testAvailable) status.testEmail.orEmpty() else it.email,
                        verifiedEmail = if (status.accessSource == "admin_test") status.testEmail else status.email)
                }
                if (status.resendAfterSeconds > 0) startCountdown(status.resendAfterSeconds)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (version == currentVersion && owner == repository.currentUid) _state.update { it.copy(isLoading = false, loadError = eduVerificationErrorMessage(error)) }
            }
        }
    }

    fun onOptInChange(optedIn: Boolean) {
        _state.update { it.copy(optedIn = optedIn, errorMessage = null) }
    }

    fun onEmailChange(email: String) {
        if (_state.value.testAvailable) return
        _state.update { it.copy(email = email.take(254), errorMessage = null) }
    }

    fun onCodeChange(code: String) {
        val digits = code.filter(Char::isDigit).take(6)
        _state.update { it.copy(code = digits, errorMessage = null) }
    }

    fun changeEmail() {
        _state.update { it.copy(codeSentTo = null, code = "", sessionId = null, errorMessage = null) }
    }

    fun sendCode() {
        val snapshot = _state.value
        if (snapshot.isLoading || snapshot.isSending || snapshot.isConfirming || snapshot.isDisconnecting || snapshot.resendSeconds > 0) return
        val tracking = analytics.click(Interaction.EDU_VERIFY_REQUEST)
        val email = snapshot.email.trim()
        if (!snapshot.testAvailable && !isCampusStudentEmail(email)) {
            analytics.result(tracking, InteractionStatus.VALIDATION_ERROR)
            _state.update { it.copy(errorMessage = UiText.StringResourceId(Res.string.campus_closet_yalnizca_ogr_akdeniz_edu_tr_uzantili_ogrenci_adresleri_kabul)) }
            return
        }
        val operationVersion = operationGeneration
        val owner = repository.currentUid
        _state.update { it.copy(isSending = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val result = repository.requestCode(email)
                if (operationGeneration != operationVersion || repository.currentUid != owner) return@launch
                analytics.result(tracking, InteractionStatus.SUCCESS)
                when (result) {
                    is EduCodeRequestResult.AlreadyVerified -> _state.update {
                        it.copy(isSending = false, isVerified = !snapshot.testAvailable, campusAccessAllowed = true, accessSource = if (snapshot.testAvailable) "admin_test" else "edu_email", verifiedEmail = result.email)
                    }
                    is EduCodeRequestResult.Sent -> {
                        _state.update { it.copy(isSending = false, codeSentTo = result.email, code = "", sessionId = result.sessionId) }
                        startCountdown(result.resendAfterSeconds)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                analytics.result(tracking, InteractionStatus.ERROR)
                if (operationGeneration == operationVersion && repository.currentUid == owner) _state.update { it.copy(isSending = false, errorMessage = eduVerificationErrorMessage(error)) }
            }
        }
    }

    fun confirmCode() {
        val snapshot = _state.value
        if (snapshot.isLoading || snapshot.isConfirming || snapshot.isSending || snapshot.isDisconnecting) return
        val tracking = analytics.click(Interaction.EDU_VERIFY_CONFIRM)
        if (snapshot.code.length != 6) {
            analytics.result(tracking, InteractionStatus.VALIDATION_ERROR)
            _state.update { it.copy(errorMessage = UiText.StringResourceId(Res.string.edu_verification_code_format)) }
            return
        }
        val operationVersion = operationGeneration
        val owner = repository.currentUid
        _state.update { it.copy(isConfirming = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val result = repository.confirmCode(snapshot.code, snapshot.sessionId)
                if (operationGeneration != operationVersion || repository.currentUid != owner) return@launch
                val message = when (result) {
                    is EduCodeConfirmResult.Verified -> {
                        analytics.result(tracking, InteractionStatus.SUCCESS)
                        countdown?.cancel()
                        _state.update {
                            it.copy(
                                isConfirming = false,
                                isVerified = !snapshot.testAvailable,
                                campusAccessAllowed = true,
                                accessSource = if (snapshot.testAvailable) "admin_test" else "edu_email",
                                verifiedEmail = result.email.ifBlank { snapshot.codeSentTo }
                            )
                        }
                        return@launch
                    }
                    is EduCodeConfirmResult.InvalidCode -> {
                        analytics.result(tracking, InteractionStatus.VALIDATION_ERROR)
                        UiText.StringResourceId(Res.string.campus_email_code_wrong, arrayOf(result.attemptsLeft))
                    }
                    EduCodeConfirmResult.Expired -> {
                        analytics.result(tracking, InteractionStatus.ERROR)
                        UiText.StringResourceId(Res.string.campus_email_code_expired)
                    }
                    EduCodeConfirmResult.TooManyAttempts -> {
                        analytics.result(tracking, InteractionStatus.ERROR)
                        UiText.StringResourceId(Res.string.campus_email_code_limit)
                    }
                }
                _state.update { it.copy(isConfirming = false, errorMessage = message) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                analytics.result(tracking, InteractionStatus.ERROR)
                if (operationGeneration == operationVersion && repository.currentUid == owner) _state.update { it.copy(isConfirming = false, errorMessage = eduVerificationErrorMessage(error)) }
            }
        }
    }

    fun disconnectTest() {
        if (_state.value.isDisconnecting || _state.value.isSending || _state.value.isConfirming) return
        val tracking = analytics.click(Interaction.EDU_VERIFY_DISCONNECT)
        _state.update { it.copy(isDisconnecting = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                repository.disconnectTest()
                analytics.result(tracking, InteractionStatus.SUCCESS)
                refresh()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                analytics.result(tracking, InteractionStatus.ERROR)
                _state.update { it.copy(isDisconnecting = false, errorMessage = eduVerificationErrorMessage(error)) }
            }
        }
    }

    private fun startCountdown(seconds: Int) {
        countdown?.cancel()
        countdown = viewModelScope.launch {
            for (remaining in seconds downTo 0) {
                _state.update { it.copy(resendSeconds = remaining) }
                if (remaining > 0) delay(1_000)
            }
        }
    }
}

internal fun eduVerificationErrorMessage(error: Throwable): UiText = UiText.StringResourceId(when (error.message) {
    "CAMPUS_TEST_EMAIL_UNAVAILABLE", "CAMPUS_TEST_RECIPIENT_FIXED", "CAMPUS_TEST_SESSION_INVALID" -> Res.string.campus_test_unavailable
    "EDU_EMAIL_REQUIRED", "EDU_EMAIL_INVALID" -> Res.string.campus_closet_yalnizca_ogr_akdeniz_edu_tr_uzantili_ogrenci_adresleri_kabul
    "EDU_EMAIL_IN_USE" -> Res.string.edu_verification_email_in_use
    "EDU_EMAIL_SEND_IN_PROGRESS", "EDU_CODE_RESEND_TOO_SOON", "EDU_CODE_REQUEST_LIMIT" -> Res.string.campus_email_code_wait
    "EDU_CODE_SEND_LIMIT" -> Res.string.campus_email_code_send_limit
    "EDU_CODE_RECIPIENT_LIMIT" -> Res.string.campus_email_code_recipient_limit
    "EDU_CODE_ACCOUNT_DAILY_LIMIT" -> Res.string.campus_email_code_account_daily_limit
    "EDU_CODE_DAILY_LIMIT" -> Res.string.campus_email_code_daily_limit
    "EDU_EMAIL_SEND_FAILED" -> Res.string.campus_email_code_send_failed
    "EDU_CODE_FORMAT_INVALID" -> Res.string.edu_verification_code_format
    "EDU_CODE_NOT_REQUESTED" -> Res.string.campus_email_code_request_first
    "ROLE_NOT_ALLOWED" -> Res.string.edu_verification_student_only
    "ACCOUNT_NOT_ACTIVE" -> Res.string.edu_verification_account_inactive
    "EDU_STATUS_LOAD_FAILED" -> Res.string.edu_verification_status_load_error
    else -> Res.string.edu_verification_error
})
