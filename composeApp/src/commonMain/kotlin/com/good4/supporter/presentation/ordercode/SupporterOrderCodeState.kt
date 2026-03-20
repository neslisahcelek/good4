package com.good4.supporter.presentation.ordercode

import com.good4.core.presentation.UiText
import com.good4.order.domain.Order

data class SupporterOrderCodeState(
    val order: Order? = null,
    val businessAddress: String = "",
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null
)
