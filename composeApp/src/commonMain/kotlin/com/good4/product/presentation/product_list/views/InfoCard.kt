package com.good4.product.presentation.product_list.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.UiText
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.products
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    text: UiText,
    icon: Painter? = null,
    iconContentDescription: UiText? = null,
    textColor: Color = TextSecondary,
    backgroundColor: Color = SurfaceDefault,
    onClick: (() -> Unit)? = null,
    isLoading: Boolean = false
) {
    Card(
        modifier = if (onClick != null) {
            modifier.clickable { onClick() }
        } else modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .height(44.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            if (icon != null) {
                Image(
                    painter = icon,
                    contentDescription = iconContentDescription?.asString()
                )
            }
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = textColor,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = text.asString(),
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}

@Preview
@Composable
fun InfoCardPreview() {
    MaterialTheme {
        InfoCard(
            text = UiText.StringResourceId(Res.string.products)
        )
    }
}
