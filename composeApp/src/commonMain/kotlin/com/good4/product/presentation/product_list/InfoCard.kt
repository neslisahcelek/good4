package com.good4.product.presentation.product_list

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.good4.core.presentation.UiText
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    text: UiText,
    icon: Painter? = null,
    iconContentDescription: UiText? = null,
    textColor: Color = Color.DarkGray,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = if (onClick != null) {
            modifier.clickable { onClick() }
        } else modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .height(40.dp)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            if (icon != null) {
                Image(
                    painter = icon,
                    contentDescription = iconContentDescription?.asString()
                )
            }
            Text(
                text = text.asString(),
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Preview
@Composable
fun InfoCardPreview() {
    MaterialTheme {
        InfoCard(
            text = UiText.DynamicString("Örnek Metin")
        )
    }
}


