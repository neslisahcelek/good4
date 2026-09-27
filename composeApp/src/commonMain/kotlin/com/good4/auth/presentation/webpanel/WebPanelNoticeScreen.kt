package com.good4.auth.presentation.webpanel

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.good4.auth.data.repository.AuthRepository
import com.good4.auth.presentation.components.AuthBackdrop
import com.good4.auth.presentation.components.AuthCard
import com.good4.auth.presentation.components.AuthLogoBadge
import com.good4.auth.presentation.components.AuthPrimaryButton
import com.good4.auth.presentation.components.AuthSecondaryButton
import com.good4.core.data.local.StartupSessionCache
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4Scaffold
import config.ReleaseFeatures
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Business and admin accounts land here on the V2 backend: their in-app panels
 * still use V1 collections, so they are pointed to the web panel instead.
 */
@Composable
fun WebPanelNoticeScreen(
    onSignedOut: () -> Unit,
    authRepository: AuthRepository = koinInject(),
    startupSessionCache: StartupSessionCache = koinInject()
) {
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    var signingOut by remember { mutableStateOf(false) }

    Good4Scaffold { paddingValues ->
        AuthBackdrop {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(56.dp))
                AuthLogoBadge()
                Spacer(Modifier.height(22.dp))
                Text(
                    text = "Yönetim paneli web'de",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "İşletme ve yönetici hesapları Good4 web panelinden yönetilir. Kod doğrulama, kampanya ve etkinlik işlemleri için panele tarayıcınızdan giriş yapın.",
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(28.dp))
                AuthCard {
                    AuthPrimaryButton(
                        text = "Web panelini aç",
                        onClick = { uriHandler.openUri(ReleaseFeatures.WEB_PANEL_URL) }
                    )
                    Spacer(Modifier.height(12.dp))
                    AuthSecondaryButton(
                        text = if (signingOut) "Çıkış yapılıyor…" else "Çıkış yap",
                        enabled = !signingOut,
                        onClick = {
                            signingOut = true
                            scope.launch {
                                startupSessionCache.clear(authRepository.currentUser?.uid)
                                authRepository.signOut()
                                onSignedOut()
                            }
                        }
                    )
                }
                Spacer(Modifier.fillMaxWidth().height(32.dp))
            }
        }
    }
}
