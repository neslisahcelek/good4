package com.good4.dining.presentation

import com.good4.dining.domain.AkdenizDiningMenu

data class AkdenizDiningMenuState(
    val menu: AkdenizDiningMenu? = null,
    val isLoading: Boolean = true
)
