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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.good4.core.presentation.InkBlack
import com.good4.core.presentation.LimeGreen

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
            text = "Yeni Kampanya Ekle",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = InkBlack
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = state.campaignImageUrl,
            onValueChange = { viewModel.onImageUrlChange(it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Kampanya Resim URL") },
            placeholder = { Text("https://...") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = InkBlack,
                focusedLabelColor = InkBlack,
                cursorColor = InkBlack
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Kampanya için bir resim URL'i girin. Resim ana sayfada gösterilecektir.",
            fontSize = 12.sp,
            color = Color.Gray
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
            colors = ButtonDefaults.buttonColors(containerColor = LimeGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (state.isAddLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(4.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Kampanya Ekle",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
