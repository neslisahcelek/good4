package com.good4.admin.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.InkBlack
import com.good4.core.presentation.MintGreen
import com.good4.core.presentation.components.ProfileInfoCard
import com.good4.core.presentation.components.ProfileLogoutButton
import com.good4.core.presentation.components.ProfileScreenScaffold
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.email
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AdminProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: AdminProfileViewModel = koinViewModel(),
    onLogout: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ProfileScreenScaffold(
        title = "Admin Profili",
        isLoading = state.isLoading,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MintGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = InkBlack
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = state.adminName,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = InkBlack
        )

        Text(
            text = state.adminEmail,
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        ProfileInfoCard(
            icon = Icons.Filled.Email,
            title = stringResource(Res.string.email),
            value = state.adminEmail
        )

        Spacer(modifier = Modifier.height(12.dp))

        ProfileInfoCard(
            icon = Icons.Filled.Settings,
            title = "Rol",
            value = "Admin"
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
fun AdminProfileScreenPreview() {
    MaterialTheme {
        AdminProfileScreen(onLogout = {})
    }
}
