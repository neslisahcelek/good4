package com.good4.product.presentation.product_list.views

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import com.good4.core.presentation.SoftGray
import com.good4.core.presentation.UiText
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.amount_unit_label
import good4.composeapp.generated.resources.ic_lock
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun AmountCard(
    modifier: Modifier = Modifier,
    amount: Int
) {
    val unitLabel = stringResource(Res.string.amount_unit_label)
    val text: UiText = UiText.DynamicString("$amount $unitLabel")
    val icon: Painter = painterResource(Res.drawable.ic_lock)
    InfoCard(
        modifier = modifier,
        text = text,
        icon = icon,
        backgroundColor = SoftGray
    )
}

@Preview
@Composable
fun AmountCardPreview() {
    MaterialTheme {
        AmountCard(amount = 3)
    }
}
