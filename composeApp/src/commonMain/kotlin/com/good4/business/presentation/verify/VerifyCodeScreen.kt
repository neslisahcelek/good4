package com.good4.business.presentation.verify

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.InkBlack
import com.good4.core.presentation.Background
import com.good4.core.presentation.BrickRed
import com.good4.core.presentation.LimeGreen
import com.good4.core.presentation.SlateGray
import good4.composeapp.generated.resources.Res
import good4.composeapp.generated.resources.enter_code
import good4.composeapp.generated.resources.verify_code
import good4.composeapp.generated.resources.verify_code_button
import good4.composeapp.generated.resources.verify_code_input_label
import good4.composeapp.generated.resources.verify_code_new
import good4.composeapp.generated.resources.verify_code_placeholder
import good4.composeapp.generated.resources.verify_code_product_prefix
import good4.composeapp.generated.resources.verify_code_success
import good4.composeapp.generated.resources.emoji_ticket
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyCodeScreen(
    modifier: Modifier = Modifier,
    viewModel: VerifyCodeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = modifier,
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.verify_code),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(Res.string.emoji_ticket),
                fontSize = 64.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(Res.string.enter_code),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = InkBlack
            )

            Text(
                text = stringResource(Res.string.enter_code),
                fontSize = 14.sp,
                color = SlateGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = state.codeInput,
                onValueChange = { viewModel.onCodeInputChange(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.verify_code_input_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        viewModel.verifyCode()
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = InkBlack,
                    focusedLabelColor = InkBlack,
                    cursorColor = InkBlack
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.verifyCode() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !state.isLoading && state.codeInput.length == 6,
                colors = ButtonDefaults.buttonColors(
                    containerColor = InkBlack,
                    disabledContainerColor = InkBlack.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.verify_code_button),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Result Card
            when {
                state.verificationSuccess -> {
                    VerificationResultCard(
                        isSuccess = true,
                        productName = state.verifiedProductName,
                        message = stringResource(Res.string.verify_code_success)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.resetState() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LimeGreen,
                            disabledContainerColor = LimeGreen.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.verify_code_new),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                state.errorMessage != null -> {
                    VerificationResultCard(
                        isSuccess = false,
                        productName = null,
                        message = state.errorMessage.orEmpty()
                    )
                }
            }
        }
    }
}

@Composable
private fun VerificationResultCard(
    modifier: Modifier = Modifier,
    isSuccess: Boolean,
    productName: String?,
    message: String
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSuccess) LimeGreen.copy(alpha = 0.1f) else BrickRed.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isSuccess) Icons.Filled.CheckCircle else Icons.Filled.Close,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (isSuccess) LimeGreen else BrickRed
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = message,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSuccess) LimeGreen else BrickRed,
                textAlign = TextAlign.Center
            )

            if (productName != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.verify_code_product_prefix) + productName,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Preview
@Composable
fun VerifyCodeScreenPreview() {
    MaterialTheme {
        VerifyCodeScreen()
    }
}
