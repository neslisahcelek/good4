package com.good4.discounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.community.couponOutcomeMessage
import com.good4.community.couponSecondsRemaining
import com.good4.core.presentation.UiText
import good4.composeapp.generated.resources.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class DiscountState(
    val nowSeconds: Long = Clock.System.now().epochSeconds,
    val discounts: List<Discount> = emptyList(),
    val selected: Discount? = null,
    val nextCursor: String? = null,
    val loading: Boolean = false,
    val issuing: Boolean = false,
    val error: UiText? = null,
    val codeMessage: UiText? = null,
    val locationError: UiText? = null,
    val code: String? = null,
    val expiresAt: Instant? = null,
    val seconds: Int = 0
)

class DiscountViewModel(
    private val repository: DiscountDataSource,
    private val now: () -> Instant = { Clock.System.now() }
) : ViewModel() {
    private val _state = MutableStateFlow(DiscountState())
    val state = _state.asStateFlow()
    private var account: String? = null
    private var generation = 0L
    private var loadGeneration = 0L
    private var selectionGeneration = 0L
    private var loadJob: Job? = null
    private var issueJob: Job? = null

    init {
        viewModelScope.launch {
            repository.accounts.collect { uid ->
                // Every auth emission is a new session, including a same-UID re-login.
                generation++
                loadGeneration++
                selectionGeneration++
                loadJob?.cancel()
                issueJob?.cancel()
                account = uid
                _state.update { DiscountState() }
                if (uid != null) refresh()
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(1000)
                _state.update { current ->
                    val nowSeconds = now().epochSeconds
                    val selectedExpired = current.selected?.data?.isAvailable(nowSeconds) == false
                    current.copy(
                        nowSeconds = nowSeconds,
                        discounts = current.discounts.filter { it.data.isAvailable(nowSeconds) },
                        code = if (selectedExpired) null else current.code,
                        expiresAt = if (selectedExpired) null else current.expiresAt,
                        codeMessage = if (selectedExpired) discountOutcomeMessage("coupon_inactive") else current.codeMessage,
                        seconds = if (selectedExpired) 0 else couponSecondsRemaining(current.expiresAt, now())
                    )
                }
            }
        }
    }

    fun refresh() = load(null)
    fun loadMore() { _state.value.nextCursor?.let(::load) }
    private fun load(cursor: String?) {
        if (account == null || (cursor != null && _state.value.loading)) return
        loadJob?.cancel()
        val session = generation
        val request = ++loadGeneration
        _state.update { it.copy(loading = true, error = null) }
        loadJob = viewModelScope.launch {
            try {
                val page = repository.load(cursor)
                if (request != loadGeneration || session != generation) return@launch
                _state.update {
                    it.copy(
                        nowSeconds = now().epochSeconds,
                        discounts = ((if (cursor == null) emptyList() else it.discounts) + page.discounts)
                            .distinctBy(Discount::id).filter { discount -> discount.data.isAvailable(now().epochSeconds) },
                        nextCursor = page.nextCursor, loading = false
                    )
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (request == loadGeneration && session == generation) _state.update { it.copy(loading = false, error = UiText.StringResourceId(Res.string.discount_load_error)) }
            }
        }
    }

    fun select(discount: Discount?) {
        selectionGeneration++
        issueJob?.cancel()
        _state.update { it.copy(selected = discount, code = null, expiresAt = null, seconds = 0, issuing = false, codeMessage = null, locationError = null) }
    }

    fun locationOpenResult(opened: Boolean) {
        _state.update {
            it.copy(locationError = if (opened) null else UiText.StringResourceId(Res.string.discount_maps_error))
        }
    }

    fun issue() {
        val discount = _state.value.selected ?: return
        if (account == null || _state.value.issuing) return
        if (!discount.data.isAvailable(now().epochSeconds)) {
            _state.update { it.copy(codeMessage = discountOutcomeMessage("coupon_inactive")) }
            return
        }
        issueJob?.cancel()
        val session = generation
        val selection = selectionGeneration
        _state.update { it.copy(issuing = true, codeMessage = null) }
        issueJob = viewModelScope.launch {
            try {
                val response = repository.issue(discount)
                if (session != generation || selection != selectionGeneration) return@launch
                _state.update {
                    it.copy(issuing = false, code = response.code, expiresAt = response.expiresAt,
                        seconds = couponSecondsRemaining(response.expiresAt, now()), codeMessage = discountOutcomeMessage(response.outcome))
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (session == generation && selection == selectionGeneration) _state.update { it.copy(issuing = false, codeMessage = UiText.StringResourceId(Res.string.discount_code_error)) }
            }
        }
    }
}


internal fun discountOutcomeMessage(outcome: String): UiText? = when (outcome) {
    "coupon_inactive" -> UiText.StringResourceId(Res.string.discount_inactive)
    "limit_reached" -> UiText.StringResourceId(Res.string.discount_limit_reached)
    else -> couponOutcomeMessage(outcome)
}
