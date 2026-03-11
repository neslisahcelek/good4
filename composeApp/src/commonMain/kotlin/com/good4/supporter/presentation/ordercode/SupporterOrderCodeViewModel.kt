package com.good4.supporter.presentation.ordercode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.good4.core.domain.Result
import com.good4.core.presentation.UiText
import com.good4.order.data.repository.OrderRepository
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.order_code_loading_error
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SupporterOrderCodeViewModel(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SupporterOrderCodeState())
    val state = _state.asStateFlow()

    fun loadOrder(orderId: String) {
        if (_state.value.order != null || _state.value.isLoading) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = orderRepository.getOrder(orderId)) {
                is Result.Success -> {
                    _state.update { it.copy(order = result.data, isLoading = false) }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = UiText.StringResourceId(Res.string.order_code_loading_error)
                        )
                    }
                }
            }
        }
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
