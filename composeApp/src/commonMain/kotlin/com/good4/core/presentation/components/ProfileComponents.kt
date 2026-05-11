package com.good4.core.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.good4.core.presentation.ErrorRed
import com.good4.core.presentation.ErrorSnackbar
import com.good4.core.presentation.PistachioGreen
import com.good4.core.presentation.PrimaryGreen
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.UiText
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.delete_account
import good4.composeapp.generated.resources.delete_account_cancel_button
import good4.composeapp.generated.resources.delete_account_confirm_button
import good4.composeapp.generated.resources.delete_account_confirm_message
import good4.composeapp.generated.resources.delete_account_confirm_title
import good4.composeapp.generated.resources.delete_account_in_progress
import good4.composeapp.generated.resources.logout
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProfileScreenScaffold(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    errorMessage: UiText? = null,
    onDismissError: () -> Unit = {},
    topBar: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Good4NestedScaffold(
        modifier = modifier,
        topBar = topBar
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = TextPrimary)
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    content = content
                )

                ErrorSnackbar(
                    modifier = Modifier.align(Alignment.TopCenter),
                    errorMessage = errorMessage,
                    onDismiss = onDismissError,
                    addTopSafeArea = false
                )
            }
        }
    }
}

@Composable
fun ProfileInfoCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    value: String,
    isValueUnderlined: Boolean = false,
    trailingIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(clickableModifier),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceDefault
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PistachioGreen.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    if (title.isNotBlank()) {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = value,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        textDecoration = if (isValueUnderlined) TextDecoration.Underline else null
                    )
                }

                if (trailingIcon != null) {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        tint = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileSectionCard(
    modifier: Modifier = Modifier,
    verticalSpacing: Dp = 10.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDefault),
        border = BorderStroke(
            width = 1.dp,
            color = TextSecondary.copy(alpha = 0.2f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(verticalSpacing),
            content = content
        )
    }
}

@Composable
fun ProfilePrimaryLogoutButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .wrapContentWidth()
            .height(StandardButtonHeight),
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryGreen,
            contentColor = SurfaceDefault
        ),
        shape = RoundedCornerShape(28.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(Res.string.logout),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ProfileDeleteAccountButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = ErrorRed
        ),
        border = BorderStroke(
            width = 1.dp,
            color = ErrorRed
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.DeleteOutline,
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(Res.string.delete_account),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun DeleteAccountConfirmDialog(
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Good4ConfirmDialog(
        title = stringResource(Res.string.delete_account_confirm_title),
        message = stringResource(Res.string.delete_account_confirm_message),
        confirmLabel = stringResource(
            if (isDeleting) Res.string.delete_account_in_progress
            else Res.string.delete_account_confirm_button
        ),
        dismissLabel = stringResource(Res.string.delete_account_cancel_button),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        confirmColor = ErrorRed,
        enabled = !isDeleting
    )
}
