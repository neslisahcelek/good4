package com.good4.supporter.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.good4.core.presentation.PrimaryGreen
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.components.Good4TopBar
import com.good4.core.presentation.components.ProfileInfoCard
import com.good4.core.presentation.components.ProfilePrimaryLogoutButton
import com.good4.core.presentation.components.ProfileScreenScaffold
import com.good4.core.presentation.components.StatCard
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.account_info
import good4.composeapp.generated.resources.account_settings_title
import good4.composeapp.generated.resources.email
import good4.composeapp.generated.resources.profile_donations_unit
import good4.composeapp.generated.resources.profile_phone_label
import good4.composeapp.generated.resources.profile_title_supporter
import good4.composeapp.generated.resources.profile_total_donations_label
import good4.composeapp.generated.resources.unknown_initial
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SupporterProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: SupporterProfileViewModel = koinViewModel(),
    onBackClick: () -> Unit,
    onLogout: () -> Unit,
    onOpenAccountSettings: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ProfileScreenScaffold(
        modifier = modifier,
        isLoading = state.isLoading,
        errorMessage = null,
        onDismissError = {},
        topBar = {
            Good4TopBar(
                title = stringResource(Res.string.profile_title_supporter),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) {
        SupporterAvatar(
            fullName = state.user?.fullName,
            unknownLabel = stringResource(Res.string.unknown_initial)
        )

        Spacer(modifier = Modifier.height(16.dp))

        SupporterUserInfo(fullName = state.user?.fullName)

        Spacer(modifier = Modifier.height(28.dp))

        DonationStats(
            totalDonations = state.user?.totalDonations ?: 0
        )

        Spacer(modifier = Modifier.height(12.dp))

        ProfileInfoCard(
            icon = Icons.Filled.Email,
            title = stringResource(Res.string.email),
            value = state.user?.email.orEmpty()
        )

        Spacer(modifier = Modifier.height(12.dp))

        state.user?.phoneNumber
            ?.takeIf { it.isNotBlank() }
            ?.let { phoneNumber ->
                ProfileInfoCard(
                    icon = Icons.Filled.Phone,
                    title = stringResource(Res.string.profile_phone_label),
                    value = phoneNumber
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

        ProfileInfoCard(
            icon = Icons.Filled.Person,
            title = stringResource(Res.string.account_info),
            value = stringResource(Res.string.account_settings_title),
            trailingIcon = Icons.Filled.ChevronRight,
            onClick = onOpenAccountSettings
        )

        Spacer(modifier = Modifier.height(24.dp))

        ProfilePrimaryLogoutButton(
            onClick = {
                viewModel.logout()
                onLogout()
            }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SupporterAvatar(
    modifier: Modifier = Modifier,
    fullName: String?,
    unknownLabel: String
) {
    Box(
        modifier = modifier
            .padding(top = 50.dp)
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
private fun SupporterUserInfo(modifier: Modifier = Modifier, fullName: String?) {
    Text(
        text = fullName ?: "",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary,
        modifier = modifier
    )
}

@Composable
private fun DonationStats(
    modifier: Modifier = Modifier,
    totalDonations: Int
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp)
    ) {
        StatCard(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(Res.string.profile_total_donations_label),
            value = "$totalDonations ${stringResource(Res.string.profile_donations_unit)}",
            icon = Icons.Filled.Favorite,
            color = PrimaryGreen
        )
//        Spacer(modifier = Modifier.height(12.dp))
//        StatCard(
//            modifier = Modifier.fillMaxWidth(),
//            title = stringResource(Res.string.profile_total_meals_label),
//            value = "$totalMeals ${stringResource(Res.string.profile_meals_unit)}",
//            icon = Icons.Filled.Star,
//            color = DeepGreen
//        )
    }
}

@Preview
@Composable
fun SupporterProfileScreenPreview() {
    MaterialTheme {
        SupporterProfileScreen(
            onBackClick = {},
            onLogout = {},
            onOpenAccountSettings = {}
        )
    }
}
