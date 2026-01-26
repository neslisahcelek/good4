package com.good4.admin.presentation.campaigns

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.good4.core.presentation.DeepGreen
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.add_campaign
import good4.composeapp.generated.resources.admin_campaigns_add_title
import good4.composeapp.generated.resources.admin_campaigns_image_url_helper
import good4.composeapp.generated.resources.admin_campaigns_image_url_label
import good4.composeapp.generated.resources.admin_campaigns_image_url_placeholder
import org.jetbrains.compose.resources.stringResource

@Composable
fun AddCampaignSheet(
    modifier: Modifier = Modifier,
    state: AdminCampaignsState,
    viewModel: AdminCampaignsViewModel,
    onDismiss: () -> Unit
) {
    LaunchedEffect(state.addSuccess) {
        if (state.addSuccess) {
            onDismiss()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = stringResource(Res.string.admin_campaigns_add_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = state.campaignImageUrl,
            onValueChange = { viewModel.onImageUrlChange(it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.admin_campaigns_image_url_label)) },
            placeholder = { Text(stringResource(Res.string.admin_campaigns_image_url_placeholder)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TextPrimary,
                focusedLabelColor = TextPrimary,
                cursorColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(Res.string.admin_campaigns_image_url_helper),
            fontSize = 12.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(16.dp))

        state.errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Button(
            onClick = { viewModel.addCampaign() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !state.isAddLoading,
            colors = ButtonDefaults.buttonColors(containerColor = DeepGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (state.isAddLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(4.dp),
                    color = SurfaceDefault,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = stringResource(Res.string.add_campaign),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
