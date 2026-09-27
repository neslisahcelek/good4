package com.good4.suspendedmeal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.PistachioGreen
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4NestedScaffold
import com.good4.core.presentation.components.Good4TopBar
import com.good4.eduverification.EduVerificationCard
import com.good4.eduverification.EduVerificationViewModel
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

/** V2 Askıda Yemek: edu-verified students take a 10-minute code and show it at the business. */
@Composable
fun SuspendedMealsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SuspendedMealsViewModel = koinViewModel(),
    eduViewModel: EduVerificationViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val eduState by eduViewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(eduState.isVerified) {
        if (eduState.isVerified) viewModel.load()
    }

    Good4NestedScaffold(
        modifier = modifier,
        topBar = {
            Good4TopBar(
                title = "Askıda Yemek",
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(Modifier.fillMaxSize().background(AppBackground).padding(paddingValues)) {
            when {
                eduState.isLoading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Center))
                !eduState.isVerified -> Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
                ) {
                    EduVerificationCard(
                        state = eduState,
                        onOptInChange = eduViewModel::onOptInChange,
                        onEmailChange = eduViewModel::onEmailChange,
                        onSendCode = eduViewModel::sendCode,
                        onCodeChange = eduViewModel::onCodeChange,
                        onConfirmCode = eduViewModel::confirmCode,
                        onChangeEmail = eduViewModel::changeEmail
                    )
                }
                state.activeCode != null -> CodeCard(
                    code = state.activeCode!!,
                    meal = state.meals.firstOrNull { it.id == state.activeCode?.mealId },
                    onDone = viewModel::dismissCode
                )
                state.isLoading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Center))
                state.loadError != null -> CenteredMessage(state.loadError!!, actionLabel = "Tekrar dene", onAction = viewModel::load)
                state.meals.isEmpty() -> CenteredMessage(
                    "Şu an askıda yemek yok.\nYeni askıda yemekler eklendiğinde burada görünecek.",
                    actionLabel = "Yenile",
                    onAction = viewModel::load
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    state.message?.let { message ->
                        item { MessageBanner(message, onDismiss = viewModel::clearMessage) }
                    }
                    items(state.meals, key = { it.id }) { meal ->
                        MealCard(
                            meal = meal,
                            requesting = state.requestingMealId == meal.id,
                            enabled = state.requestingMealId == null,
                            onRequestCode = { viewModel.requestCode(meal.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MealCard(meal: SuspendedMeal, requesting: Boolean, enabled: Boolean, onRequestCode: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = SurfaceDefault, shadowElevation = 1.dp) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = PistachioGreen) {
                    Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp).size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(meal.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(meal.businessName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (meal.description.isNotBlank()) {
                Text(meal.description, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            }
            Text(
                listOfNotNull(
                    meal.remaining?.let { "$it adet kaldı" },
                    "Son gün: ${meal.endsAtMillis.toTurkishDateTime()}"
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onRequestCode,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (requesting) CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                else Text("Kodu al")
            }
        }
    }
}

@Composable
private fun CodeCard(code: ActiveMealCode, meal: SuspendedMeal?, onDone: () -> Unit) {
    var now by remember { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(code.code) {
        while (true) {
            now = Clock.System.now().toEpochMilliseconds()
            delay(1_000)
        }
    }
    val secondsLeft = ((code.expiresAtMillis - now) / 1000).coerceAtLeast(0)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = SurfaceDefault, shadowElevation = 2.dp) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                meal?.let {
                    Text(it.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary, textAlign = TextAlign.Center)
                    Text(it.businessName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                when {
                    code.redeemed -> {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
                        Text("Afiyet olsun!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Kodun işletme tarafından kullanıldı.", color = TextSecondary, textAlign = TextAlign.Center)
                    }
                    secondsLeft == 0L -> {
                        Text("Kodun süresi doldu", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Listeye dönüp yeni bir kod alabilirsin.", color = TextSecondary, textAlign = TextAlign.Center)
                    }
                    else -> {
                        Text("İşletmede bu kodu göster", color = TextSecondary)
                        Text(
                            code.code.chunked(4).joinToString(" "),
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp,
                            color = TextPrimary
                        )
                        Text(
                            "Geçerlilik: ${secondsLeft / 60}:${(secondsLeft % 60).toString().padStart(2, '0')}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onDone) { Text(if (code.redeemed || secondsLeft == 0L) "Listeye dön" else "Kapat", color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun MessageBanner(message: String, onDismiss: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = PistachioGreen) {
        Row(Modifier.padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, Modifier.weight(1f).padding(vertical = 12.dp), color = TextPrimary)
            TextButton(onClick = onDismiss) { Text("Tamam", color = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
private fun CenteredMessage(message: String, actionLabel: String, onAction: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, color = TextSecondary, textAlign = TextAlign.Center)
        TextButton(onClick = onAction) { Text(actionLabel, color = MaterialTheme.colorScheme.primary) }
    }
}

private val MonthsTr = listOf("Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran", "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık")

private fun Long.toTurkishDateTime(): String {
    val time = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.of("Europe/Istanbul"))
    return "${time.dayOfMonth} ${MonthsTr[time.monthNumber - 1]} ${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
}
