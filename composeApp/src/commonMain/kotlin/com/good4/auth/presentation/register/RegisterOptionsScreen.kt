package com.good4.auth.presentation.register

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.good4.auth.presentation.components.AuthBackdrop
import com.good4.auth.presentation.components.AuthOptionCard
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4Scaffold
import com.good4.core.presentation.components.Good4TopBar
import com.good4.core.util.singleClick
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.back
import good4.composeapp.generated.resources.business_register
import good4.composeapp.generated.resources.business_register_description
import good4.composeapp.generated.resources.register_options_description
import good4.composeapp.generated.resources.register_options_heading
import good4.composeapp.generated.resources.register_options_title
import good4.composeapp.generated.resources.student_register
import good4.composeapp.generated.resources.student_register_description
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun RegisterOptionsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onNavigateToStudentRegister: () -> Unit,
    onNavigateToBusinessRegister: () -> Unit
) {
    val onStudentClick = remember { singleClick { onNavigateToStudentRegister() } }
    val onBusinessClick = remember { singleClick { onNavigateToBusinessRegister() } }

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
        AuthBackdrop(modifier = Modifier.padding(paddingValues), belowTopBar = true) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(Res.string.register_options_heading),
                    fontSize = 26.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(Res.string.register_options_description),
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                AuthOptionCard(
                    icon = Icons.Outlined.School,
                    title = stringResource(Res.string.student_register),
                    description = stringResource(Res.string.student_register_description),
                    onClick = onStudentClick,
                    highlighted = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                AuthOptionCard(
                    icon = Icons.Outlined.Storefront,
                    title = stringResource(Res.string.business_register),
                    description = stringResource(Res.string.business_register_description),
                    onClick = onBusinessClick
                )

                Spacer(modifier = Modifier.height(24.dp))
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
            onNavigateToBusinessRegister = {}
        )
    }
}
