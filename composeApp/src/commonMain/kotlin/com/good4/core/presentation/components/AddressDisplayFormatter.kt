package com.good4.core.presentation.components

import androidx.compose.runtime.Composable
import com.good4.core.util.toDisplayAddress
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.product_address_maps_hint
import org.jetbrains.compose.resources.stringResource

@Composable
fun toDisplayAddressOrNull(rawAddress: String?): String? {
    return rawAddress
        ?.let { toDisplayAddress(it, stringResource(Res.string.product_address_maps_hint)) }
        ?.takeIf { it.isNotBlank() }
}
