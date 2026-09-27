package com.good4.student.presentation.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.PistachioGreen
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.UiText
import com.good4.core.presentation.components.Good4TopBar
import com.good4.core.presentation.components.ProfileScreenScaffold
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.account_settings_title
import good4.composeapp.generated.resources.logout
import good4.composeapp.generated.resources.placeholder_dash
import good4.composeapp.generated.resources.profile_major_label
import good4.composeapp.generated.resources.profile_title_student
import good4.composeapp.generated.resources.profile_university_label
import good4.composeapp.generated.resources.unknown_initial
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun StudentProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: StudentProfileViewModel = koinViewModel(),
    onBackClick: () -> Unit,
    onLogout: () -> Unit,
    onOpenAccountSettings: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val user = state.user
    val placeholder = stringResource(Res.string.placeholder_dash)

    ProfileScreenScaffold(
        isLoading = state.isLoading,
        modifier = modifier,
        errorMessage = state.errorMessage?.let { UiText.DynamicString(it) },
        onDismissError = {},
        topBar = {
            Good4TopBar(
                title = stringResource(Res.string.profile_title_student),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                }
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDefault),
                border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(PistachioGreen.copy(alpha = 0.36f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user?.fullName?.firstOrNull()?.uppercase()
                                ?: stringResource(Res.string.unknown_initial),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = user?.fullName?.ifBlank { "Öğrenci" }.orEmpty(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = user?.email.orEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = PistachioGreen.copy(alpha = 0.22f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (state.isCommunityManager) {
                                    Icons.Outlined.Groups
                                } else {
                                    Icons.Filled.School
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (state.isCommunityManager) {
                                    "Topluluk yöneticisi"
                                } else {
                                    "Öğrenci hesabı"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (state.isCommunityManager) "Topluluk Bilgileri" else "Eğitim Bilgileri",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.padding(start = 4.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDefault),
                    border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.35f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        if (state.isCommunityManager) {
                            ProfileDetailRow(
                                label = "Topluluk",
                                value = state.communityName.ifBlank {
                                    user?.fullName?.ifBlank { placeholder } ?: placeholder
                                }
                            )
                            HorizontalDivider(color = BorderMuted.copy(alpha = 0.45f))
                            ProfileDetailRow(
                                label = stringResource(Res.string.profile_university_label),
                                value = state.communityUniversity.ifBlank {
                                    user?.university?.ifBlank { placeholder } ?: placeholder
                                }
                            )
                        } else {
                            ProfileDetailRow(
                                label = stringResource(Res.string.profile_university_label),
                                value = user?.university ?: placeholder
                            )
                            HorizontalDivider(color = BorderMuted.copy(alpha = 0.45f))
                            ProfileDetailRow(
                                label = "Fakülte",
                                value = user?.faculty ?: placeholder
                            )
                            HorizontalDivider(color = BorderMuted.copy(alpha = 0.45f))
                            ProfileDetailRow(
                                label = stringResource(Res.string.profile_major_label),
                                value = user?.major ?: placeholder
                            )
                            HorizontalDivider(color = BorderMuted.copy(alpha = 0.45f))
                            ProfileDetailRow(
                                label = "Sınıf",
                                value = user?.classYear ?: placeholder
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenAccountSettings),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDefault),
                border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(PistachioGreen.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.account_settings_title),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "Profil ve hesap bilgilerini düzenle",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    viewModel.logout()
                    onLogout()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderMuted),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.logout),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun ProfileDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.9f),
            fontSize = 13.sp,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            modifier = Modifier.weight(1.4f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview
@Composable
fun StudentProfileScreenPreview() {
    MaterialTheme {
        StudentProfileScreen(
            onBackClick = {},
            onLogout = {},
            onOpenAccountSettings = {}
        )
    }
}
