package com.good4.supporter.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
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
import com.good4.core.presentation.DeepGreen
import com.good4.core.presentation.PistachioGreen
import com.good4.core.presentation.PrimaryGreen
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.DeleteAccountConfirmDialog
import com.good4.core.presentation.components.ProfileDeleteAccountButton
import com.good4.core.presentation.components.ProfileLogoutButton
import com.good4.core.presentation.components.ProfileScreenScaffold
import com.good4.core.presentation.components.StatCard
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.profile_donations_unit
import good4.composeapp.generated.resources.profile_meals_unit
import good4.composeapp.generated.resources.profile_total_donations_label
import good4.composeapp.generated.resources.profile_total_meals_label
import good4.composeapp.generated.resources.unknown_initial
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SupporterProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: SupporterProfileViewModel = koinViewModel(),
    onLogout: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isAccountDeleted) {
        if (state.isAccountDeleted) onLogout()
    }

    if (state.isDeleteDialogVisible) {
        DeleteAccountConfirmDialog(
            isDeleting = state.isDeleting,
            onConfirm = viewModel::deleteAccount,
            onDismiss = viewModel::hideDeleteAccountDialog
        )
    }

    ProfileScreenScaffold(
        modifier = modifier,
        isLoading = state.isLoading,
        errorMessage = state.deleteErrorMessage,
        onDismissError = viewModel::clearDeleteError
    ) {
        SupporterAvatar(
            fullName = state.user?.fullName,
            unknownLabel = stringResource(Res.string.unknown_initial)
        )

        Spacer(modifier = Modifier.height(16.dp))

        SupporterUserInfo(fullName = state.user?.fullName, email = state.user?.email)

        Spacer(modifier = Modifier.height(28.dp))

        DonationStats(
            totalDonations = state.user?.totalDonations ?: 0,
            totalMeals = state.user?.totalMeals ?: 0
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

        ProfileDeleteAccountButton(onClick = viewModel::showDeleteAccountDialog)

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SupporterAvatar(modifier: Modifier = Modifier, fullName: String?, unknownLabel: String) {
    Box(
        modifier = modifier
            .padding(top = 32.dp)
            .size(100.dp)
            .clip(CircleShape)
            .background(PistachioGreen),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = fullName?.firstOrNull()?.uppercase() ?: unknownLabel,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}

@Composable
private fun SupporterUserInfo(modifier: Modifier = Modifier, fullName: String?, email: String?) {
    Text(
        text = fullName ?: "",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary,
        modifier = modifier
    )
    Text(
        text = email ?: "",
        fontSize = 14.sp,
        color = TextSecondary
    )
}

@Composable
private fun DonationStats(
    modifier: Modifier = Modifier,
    totalDonations: Int,
    totalMeals: Int
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        StatCard(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(Res.string.profile_total_donations_label),
            value = "$totalDonations ${stringResource(Res.string.profile_donations_unit)}",
            icon = Icons.Filled.Favorite,
            color = PrimaryGreen
        )
        Spacer(modifier = Modifier.height(12.dp))
        StatCard(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(Res.string.profile_total_meals_label),
            value = "$totalMeals ${stringResource(Res.string.profile_meals_unit)}",
            icon = Icons.Filled.Star,
            color = DeepGreen
        )
    }
}

@Preview
@Composable
fun SupporterProfileScreenPreview() {
    MaterialTheme {
        SupporterProfileScreen(onLogout = {})
    }
}
