package com.good4.review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.good4.core.presentation.Good4Theme
import com.good4.core.presentation.PistachioGreen
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.StandardButtonHeight
import com.good4.core.presentation.components.StandardButtonLoadingIndicatorSize
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.good4_logo_transparent
import good4.composeapp.generated.resources.store_review_button
import good4.composeapp.generated.resources.store_review_description
import good4.composeapp.generated.resources.store_review_hint
import good4.composeapp.generated.resources.store_review_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun StoreReviewCard(state: StoreReviewState, onReview: () -> Unit, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val reviewLabel = stringResource(Res.string.store_review_button)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = SurfaceDefault,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.12f)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(16.dp), color = PistachioGreen) {
                    Image(
                        painter = painterResource(Res.drawable.good4_logo_transparent),
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp).size(32.dp)
                    )
                }
                Text(
                    text = stringResource(Res.string.store_review_title),
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = stringResource(Res.string.store_review_description),
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            // A single store action: local stars cannot submit or preselect a store rating.
            OutlinedButton(
                onClick = onReview,
                enabled = !state.isOpening,
                modifier = Modifier.fillMaxWidth().height(StandardButtonHeight)
                    .semantics { contentDescription = reviewLabel },
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, accent.copy(alpha = if (state.isOpening) 0.12f else 0.24f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = PistachioGreen,
                    contentColor = accent,
                    disabledContainerColor = PistachioGreen
                )
            ) {
                if (state.isOpening) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(StandardButtonLoadingIndicatorSize),
                        color = accent,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        repeat(5) {
                            Icon(Icons.Outlined.StarBorder, contentDescription = null, modifier = Modifier.size(30.dp))
                        }
                    }
                }
            }
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
//                Text(
//                    text = stringResource(Res.string.store_review_hint),
//                    color = TextSecondary,
//                    style = MaterialTheme.typography.bodySmall,
//                    textAlign = TextAlign.Center,
//                    modifier = Modifier.weight(1f, fill = false)
//                )
//            }
            state.error?.let {
                Text(it.asString(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Preview
@Composable
private fun StoreReviewCardPreview() {
    Good4Theme { StoreReviewCard(StoreReviewState(), {}, Modifier.padding(16.dp)) }
}

@Preview
@Composable
private fun StoreReviewCardDarkPreview() {
    Good4Theme(darkTheme = true) { StoreReviewCard(StoreReviewState(), {}, Modifier.padding(16.dp)) }
}
