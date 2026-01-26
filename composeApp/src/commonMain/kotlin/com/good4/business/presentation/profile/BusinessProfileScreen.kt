package com.good4.business.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.PistachioGreen
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.ProfileInfoCard
import com.good4.core.presentation.components.ProfileLogoutButton
import com.good4.core.presentation.components.ProfileScreenScaffold
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.profile_address_label
import good4.composeapp.generated.resources.profile_phone_label
import good4.composeapp.generated.resources.profile_title_business
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BusinessProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: BusinessProfileViewModel = koinViewModel(),
    onLogout: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ProfileScreenScaffold(
        title = stringResource(Res.string.profile_title_business),
        isLoading = state.isLoading,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(PistachioGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Home,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = state.businessName,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Text(
            text = state.ownerName,
            fontSize = 14.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        ProfileInfoCard(
            icon = Icons.Filled.LocationOn,
            title = stringResource(Res.string.profile_address_label),
            value = state.address
        )

        Spacer(modifier = Modifier.height(12.dp))

        ProfileInfoCard(
            icon = Icons.Filled.Phone,
            title = stringResource(Res.string.profile_phone_label),
            value = state.phone
        )

        Spacer(modifier = Modifier.height(32.dp))

        ProfileLogoutButton(
            onClick = {
                viewModel.logout()
                onLogout()
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Preview
@Composable
fun BusinessProfileScreenPreview() {
    MaterialTheme {
        BusinessProfileScreen(onLogout = {})
    }
}
