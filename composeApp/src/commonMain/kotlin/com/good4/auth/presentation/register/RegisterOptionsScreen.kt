package com.good4.auth.presentation.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.DeepGreen
import com.good4.core.presentation.PistachioGreen
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4Scaffold
import com.good4.core.presentation.components.Good4TopBar
import com.good4.core.presentation.components.StandardButtonHeight
import com.good4.core.util.singleClick
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.back
import good4.composeapp.generated.resources.business_register
import good4.composeapp.generated.resources.register_options_description
import good4.composeapp.generated.resources.register_options_title
import good4.composeapp.generated.resources.student_register
import good4.composeapp.generated.resources.supporter_register
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun RegisterOptionsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onNavigateToStudentRegister: () -> Unit,
    onNavigateToBusinessRegister: () -> Unit,
    onNavigateToSupporterRegister: () -> Unit
) {
    val onStudentClick = remember { singleClick { onNavigateToStudentRegister() } }
    val onBusinessClick = remember { singleClick { onNavigateToBusinessRegister() } }
    val onSupporterClick = remember { singleClick { onNavigateToSupporterRegister() } }

    Good4Scaffold(
        modifier = modifier,
        topBar = {
            Good4TopBar(
                title = stringResource(Res.string.register_options_title),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 20.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = stringResource(Res.string.register_options_description),
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onStudentClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(StandardButtonHeight),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.student_register),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onBusinessClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(StandardButtonHeight),
                    colors = ButtonDefaults.buttonColors(containerColor = PistachioGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.business_register),
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onSupporterClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(StandardButtonHeight),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DeepGreen.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.supporter_register),
                        color = DeepGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun RegisterOptionsScreenPreview() {
    MaterialTheme {
        RegisterOptionsScreen(
            onBackClick = {},
            onNavigateToStudentRegister = {},
            onNavigateToBusinessRegister = {},
            onNavigateToSupporterRegister = {}
        )
    }
}
