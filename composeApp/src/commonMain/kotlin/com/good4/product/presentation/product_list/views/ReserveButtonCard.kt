package com.good4.product.presentation.product_list.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.material3.MaterialTheme
import com.good4.core.presentation.UiText
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.reserve_button_label
import com.good4.core.presentation.InkBlack
import com.good4.core.presentation.LimeGreen
import com.good4.core.presentation.SlateGray


@Composable
fun ReserveButtonCard(
    modifier: Modifier = Modifier,
    label: UiText = UiText.StringResourceId(id = Res.string.reserve_button_label),
    onClick: (() -> Unit)? = null,
    isLoading: Boolean = false
) {
    InfoCard(
        modifier = modifier,
        text = label,
        textColor = InkBlack,
        icon = null,
        backgroundColor = if (isLoading) SlateGray else LimeGreen,
        onClick = onClick,
        isLoading = isLoading
    )
}

@Preview
@Composable
fun ReserveButtonCardPreview() {
    MaterialTheme {
        ReserveButtonCard()
    }
}

