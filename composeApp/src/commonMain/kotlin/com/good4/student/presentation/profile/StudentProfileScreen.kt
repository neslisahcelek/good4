package com.good4.student.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.good4.core.presentation.components.DeleteAccountConfirmDialog
import com.good4.core.presentation.components.ProfileDeleteAccountButton
import com.good4.core.presentation.components.ProfileInfoCard
import com.good4.core.presentation.components.ProfileLogoutButton
import com.good4.core.presentation.components.ProfileScreenScaffold
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.placeholder_dash
import good4.composeapp.generated.resources.profile_education_level_label
import good4.composeapp.generated.resources.profile_major_label
import good4.composeapp.generated.resources.profile_university_label
import good4.composeapp.generated.resources.unknown_initial
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun StudentProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: StudentProfileViewModel = koinViewModel(),
    onLogout: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isAccountDeleted) {
        if (state.isAccountDeleted) {
            onLogout()
        }
    }

    if (state.isDeleteDialogVisible) {
        DeleteAccountConfirmDialog(
            isDeleting = state.isDeleting,
            onConfirm = viewModel::deleteAccount,
            onDismiss = viewModel::hideDeleteAccountDialog
        )
    }

    ProfileScreenScaffold(
        isLoading = state.isLoading,
        modifier = modifier,
        errorMessage = state.deleteErrorMessage,
        onDismissError = viewModel::clearDeleteError
    ) {
        Box(
            modifier = Modifier
                .padding(top = 32.dp)
                .size(100.dp)
                .clip(CircleShape)
                .background(PistachioGreen),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = state.user?.fullName?.firstOrNull()?.uppercase()
                    ?: stringResource(Res.string.unknown_initial),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = state.user?.fullName ?: "",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Text(
            text = state.user?.email ?: "",
            fontSize = 14.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        ProfileInfoCard(
            icon = Icons.Filled.Info,
            title = stringResource(Res.string.profile_university_label),
            value = state.user?.university ?: stringResource(Res.string.placeholder_dash)
        )

        Spacer(modifier = Modifier.height(12.dp))

        ProfileInfoCard(
            icon = Icons.Filled.Person,
            title = stringResource(Res.string.profile_major_label),
            value = state.user?.major ?: stringResource(Res.string.placeholder_dash)
        )

        Spacer(modifier = Modifier.height(12.dp))

        ProfileInfoCard(
            icon = Icons.Filled.Info,
            title = stringResource(Res.string.profile_education_level_label),
            value = state.user?.educationLevel ?: stringResource(Res.string.placeholder_dash)
        )

        Spacer(modifier = Modifier.height(32.dp))

        ProfileLogoutButton(
            onClick = {
                viewModel.logout()
                onLogout()
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(48.dp))

        ProfileDeleteAccountButton(
            onClick = viewModel::showDeleteAccountDialog
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Preview
@Composable
fun StudentProfileScreenPreview() {
    MaterialTheme {
        StudentProfileScreen(onLogout = {})
    }
}
