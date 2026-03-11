package com.good4.supporter.presentation.products

sealed interface SupporterProductListAction {
    data object OnDismissError : SupporterProductListAction
    data object OnRefresh : SupporterProductListAction
}
