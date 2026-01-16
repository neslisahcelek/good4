package com.good4.product.presentation.product_list.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.ic_logo
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.painterResource
import androidx.compose.material3.Text
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import good4.composeapp.generated.resources.address_icon_description
import com.good4.core.presentation.UiText

@Composable
fun AddressRow(
    modifier: Modifier = Modifier,
    address: UiText
) {
    Row(modifier = modifier) {
        Image(
            painter = painterResource(Res.drawable.ic_logo),
            contentDescription = stringResource(Res.string.address_icon_description)
        )
        Text(
            text = address.asString(),
            color = Color.Gray,
            modifier = Modifier.padding(start = 4.dp).weight(1f)
        )
    }
}

@Preview
@Composable
fun AddressRowPreview() {
    MaterialTheme {
        AddressRow(address = UiText.DynamicString("Yakut Çarşısı Sokak Kahvecisi Konyaaltı/Antalya"))
    }
}


