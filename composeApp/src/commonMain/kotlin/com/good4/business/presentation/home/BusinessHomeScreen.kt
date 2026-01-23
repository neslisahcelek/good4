package com.good4.business.presentation.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun BusinessHomeScreenRoot(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit
) {

}

@Preview
@Composable
fun BusinessHomeScreenPreview() {
    MaterialTheme {
        BusinessHomeScreenRoot(onLogout = {})
    }
}
