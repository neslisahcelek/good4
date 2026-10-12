package com.good4.discounts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.*
import com.good4.core.presentation.components.CouponTicketCard
import com.good4.core.presentation.components.Good4Scaffold
import com.good4.core.presentation.components.Good4TopBar
import good4.composeapp.generated.resources.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DiscountsScreen(onBack: () -> Unit, viewModel: DiscountViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DiscountsContent(state, onBack, viewModel::refresh, viewModel::loadMore, viewModel::select, viewModel::issue, viewModel::locationOpenResult)
}

@Composable
private fun DiscountButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)) {
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DiscountsContent(state: DiscountState, onBack: () -> Unit, onRefresh: () -> Unit, onMore: () -> Unit, onSelect: (Discount?) -> Unit, onIssue: () -> Unit, onLocationResult: (Boolean) -> Unit) {
    Good4Scaffold(topBar = {
        Good4TopBar(title = stringResource(Res.string.discount_title), navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.discount_back)) }
        })
    }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(key = "hint") { Text(stringResource(Res.string.discount_hint), color = TextSecondary) }
            state.error?.let { error -> item(key = "error") { Text(error.asString(), color = MaterialTheme.colorScheme.error) } }
            if (state.loading) item(key = "loading") { Text(stringResource(Res.string.discount_loading), color = TextSecondary) }
            if (!state.loading && state.discounts.isEmpty()) item(key = "empty") { Text(stringResource(Res.string.discount_empty), color = TextSecondary) }
            items(state.discounts, key = Discount::id) { discount -> DiscountCard(discount) { onSelect(discount) } }
            if (state.nextCursor != null) item(key = "more") { DiscountButton(stringResource(Res.string.discount_more), !state.loading, onMore) }
            item(key = "refresh") { DiscountButton(stringResource(Res.string.discount_refresh), !state.loading, onRefresh) }
        }
    }
    val uriHandler = LocalUriHandler.current
    state.selected?.let { discount ->
        Dialog(onDismissRequest = { onSelect(null) }) {
            Surface(shape = RoundedCornerShape(24.dp), color = SurfaceDefault, border = BorderStroke(1.dp, BorderMuted)) {
                Column(Modifier.fillMaxWidth().heightIn(max = 650.dp).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(discount.data.title, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(discount.data.businessName, color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(Res.string.discount_percentage, discount.data.discountValue), color = MaterialTheme.colorScheme.primary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(Res.string.discount_conditions), color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text(discount.data.conditions, color = TextPrimary)
                    Surface(shape = RoundedCornerShape(16.dp), color = SurfaceMuted) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            DiscountDateRow(stringResource(Res.string.discount_start_label), discountDate(discount.data.startsAt))
                            DiscountDateRow(stringResource(Res.string.discount_end_label), discountDate(discount.data.endsAt))
                        }
                    }
                    if (discount.data.mapsUrl.isNotBlank()) {
                        OutlinedButton(
                            onClick = { onLocationResult(runCatching { uriHandler.openUri(discount.data.mapsUrl) }.isSuccess) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(Res.string.discount_maps), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    state.locationError?.let { Text(it.asString(), color = MaterialTheme.colorScheme.error) }
                    Text(stringResource(Res.string.discount_daily), color = TextSecondary)
                    state.codeMessage?.let { Text(it.asString(), color = MaterialTheme.colorScheme.error) }
                    if (state.code != null && state.seconds > 0) {
                        Text(state.code, fontSize = 48.sp, letterSpacing = 6.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.CenterHorizontally))
                        Text(stringResource(Res.string.discount_remaining, "${state.seconds / 60}:${(state.seconds % 60).toString().padStart(2, '0')}"), color = TextPrimary)
                        Text(stringResource(Res.string.discount_tell), color = TextSecondary)
                    } else DiscountButton(stringResource(if (state.issuing) Res.string.discount_generating else Res.string.discount_checkout), !state.issuing, onIssue)
                    TextButton(onClick = { onSelect(null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.textButtonColors(contentColor = TextPrimary)) {
                        Text(stringResource(Res.string.discount_close))
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscountDateRow(label: String, date: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(date, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun discountDate(seconds: Long): String {
    val date = Instant.fromEpochSeconds(seconds).toLocalDateTime(TimeZone.of("Europe/Istanbul"))
    return stringResource(Res.string.discount_date, date.dayOfMonth, date.monthNumber, date.year, "${date.hour.toString().padStart(2, '0')}:${date.minute.toString().padStart(2, '0')}")
}

@Composable
private fun DiscountCard(discount: Discount, onClick: () -> Unit) {
    CouponTicketCard(
        title = discount.data.businessName,
        subtitle = discount.data.title,
        amount = stringResource(Res.string.discount_percent_short, discount.data.discountValue),
        stubLabel = stringResource(Res.string.community_coupon_stub_discount),
        deadlineLabel = stringResource(Res.string.discount_until, discountDate(discount.data.endsAt)),
        prominentMetadata = true,
        actionLabel = stringResource(Res.string.community_coupon_get_code),
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    )
}

@Preview
@Composable
private fun DiscountsPreview() {
    Good4Theme {
        DiscountsContent(DiscountState(discounts = listOf(Discount("coffee", BusinessDiscountDto(businessName = "Kampüs Kahve", title = "Kahvelerde indirim", conditions = "Sıcak kahvelerde geçerlidir.", discountValue = 20, startsAt = 1791705600, endsAt = 1792569600)))), {}, {}, {}, {}, {}, {})
    }
}
