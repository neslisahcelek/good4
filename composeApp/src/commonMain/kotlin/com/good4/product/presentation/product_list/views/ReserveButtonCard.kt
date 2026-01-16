package com.good4.product.presentation.product_list.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import com.good4.core.presentation.UiText
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.reserve_button_label
import com.good4.core.presentation.LightGreen


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
        textColor = Color.Black,
        icon = null,
        backgroundColor = if (isLoading) Color.Gray else LightGreen,
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


