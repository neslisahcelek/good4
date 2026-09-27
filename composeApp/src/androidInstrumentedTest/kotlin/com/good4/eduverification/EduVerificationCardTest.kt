package com.good4.eduverification

import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.good4.MainActivity
import org.junit.Rule
import org.junit.Test

class EduVerificationCardTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun optInRevealsEmailThenCodeStep() {
        var sentEmail: String? = null
        var confirmedCode: String? = null
        compose.activityRule.scenario.onActivity { activity -> activity.setContent {
            var state by remember { mutableStateOf(EduVerificationState(isLoading = false)) }
            MaterialTheme {
                EduVerificationCard(
                    state = state,
                    onOptInChange = { state = state.copy(optedIn = it) },
                    onEmailChange = { state = state.copy(email = it) },
                    onSendCode = {
                        sentEmail = state.email
                        state = state.copy(codeSentTo = state.email, resendSeconds = 60)
                    },
                    onCodeChange = { state = state.copy(code = it.filter(Char::isDigit).take(6)) },
                    onConfirmCode = { confirmedCode = state.code },
                    onChangeEmail = { state = state.copy(codeSentTo = null) }
                )
            }
        } }

        compose.onNodeWithText(".edu.tr e-posta adresiniz").assertDoesNotExist()
        compose.onNodeWithText("Edu mailimi aktif ederek Askıda Yemek'ten faydalanmak istiyorum").performClick()
        compose.onNodeWithText("Doğrulama kodu gönder").assertIsNotEnabled()
        compose.onNodeWithText(".edu.tr e-posta adresiniz").performTextInput("can@ogr.akdeniz.edu.tr")
        compose.onNodeWithText("Doğrulama kodu gönder").assertIsEnabled().performClick()
        assert(sentEmail == "can@ogr.akdeniz.edu.tr")

        compose.onNodeWithText("6 haneli kod can@ogr.akdeniz.edu.tr adresine gönderildi", substring = true).assertExists()
        compose.onNodeWithText("Tekrar gönder (60 sn)").assertIsNotEnabled()
        compose.onNodeWithText("Doğrula").assertIsNotEnabled()
        compose.onNodeWithText("Doğrulama kodu").performTextInput("12a3456")
        compose.onNodeWithText("Doğrula").assertIsEnabled().performClick()
        assert(confirmedCode == "123456")
    }

    @Test fun errorMessageIsShown() {
        compose.activityRule.scenario.onActivity { activity -> activity.setContent {
            MaterialTheme {
                EduVerificationCard(
                    state = EduVerificationState(
                        isLoading = false,
                        optedIn = true,
                        email = "can@gmail.com",
                        errorMessage = "Geçerli bir .edu.tr e-posta adresi girin."
                    ),
                    onOptInChange = {}, onEmailChange = {}, onSendCode = {},
                    onCodeChange = {}, onConfirmCode = {}, onChangeEmail = {}
                )
            }
        } }
        compose.onNodeWithText("Geçerli bir .edu.tr e-posta adresi girin.").assertExists()
    }
}
