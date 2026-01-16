package com.good4.product.presentation.product_list.views

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import com.good4.core.presentation.UiText
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.amount_with_unit
import good4.composeapp.generated.resources.ic_lock
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun AmountCard(
    modifier: Modifier = Modifier,
    amount: Int
) {
    val text: UiText = UiText.StringResourceId(
        id = Res.string.amount_with_unit,
        args = arrayOf(amount)
    )
    val icon: Painter = painterResource(Res.drawable.ic_lock)
    InfoCard(
        modifier = modifier,
        text = text,
        icon = icon,
        backgroundColor = Color.LightGray
    )
}

@Preview
@Composable
fun AmountCardPreview() {
    MaterialTheme {
        AmountCard(amount = 3)
    }
}


