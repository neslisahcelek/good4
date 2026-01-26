package com.good4.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.good4.core.presentation.SurfaceMuted
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.product_image_description
import org.jetbrains.compose.resources.stringResource

@Composable
fun ImagePreviewBox(
    imageUrl: String,
    placeholderText: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp),
    contentScale: ContentScale = ContentScale.Crop
) {
    val resolvedDescription = contentDescription ?: stringResource(Res.string.product_image_description)
    Box(
        modifier = modifier
            .clip(shape)
            .background(SurfaceMuted),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = resolvedDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else {
            Text(
                text = placeholderText,
                fontSize = 28.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
