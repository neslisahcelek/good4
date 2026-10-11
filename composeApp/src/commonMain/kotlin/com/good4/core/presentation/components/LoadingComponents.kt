package com.good4.core.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.good4.core.presentation.Good4Theme
import com.good4.core.presentation.TextSecondary
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Centers loading in the available content area. Pass a bounded modifier for list/card loading. */
@Composable
fun Good4LoadingState(
    modifier: Modifier = Modifier.fillMaxSize(),
    label: String? = null
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(40.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 4.dp
        )
        label?.let {
            Text(it, color = TextSecondary, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
    }
}

@Preview
@Composable
private fun Good4LoadingStatePreview() {
    Good4Theme { Good4LoadingState() }
}
