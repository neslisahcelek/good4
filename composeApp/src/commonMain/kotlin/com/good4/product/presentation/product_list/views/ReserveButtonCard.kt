package com.good4.product.presentation.product_list.views

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.good4.core.presentation.DeepGreen
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.UiText
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.reserve_button_label
import org.jetbrains.compose.ui.tooling.preview.Preview


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
        textColor = TextPrimary,
        icon = null,
        backgroundColor = if (isLoading) TextSecondary else DeepGreen,
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

