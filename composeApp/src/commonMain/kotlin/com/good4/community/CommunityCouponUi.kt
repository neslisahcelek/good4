package com.good4.community

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.good4.core.presentation.UiText
import good4.composeapp.generated.resources.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

internal fun couponSecondsRemaining(expiresAt: Instant?, now: Instant = Clock.System.now()): Int = expiresAt?.let {
    ((it.toEpochMilliseconds() - now.toEpochMilliseconds() + 999) / 1000)
        .coerceAtLeast(0).toInt()
} ?: 0

/** What a coupon card says about its last day: a countdown in the final days, the date otherwise. */
internal sealed interface CouponDeadline {
    data object Today : CouponDeadline
    data object Tomorrow : CouponDeadline
    data class DaysLeft(val days: Int) : CouponDeadline
    data class OnDate(val day: Int, val month: Int) : CouponDeadline
}

internal fun couponDeadline(lastDate: String, today: LocalDate): CouponDeadline? {
    val last = runCatching { LocalDate.parse(lastDate) }.getOrNull() ?: return null
    return when (val days = today.daysUntil(last)) {
        0 -> CouponDeadline.Today
        1 -> CouponDeadline.Tomorrow
        2, 3 -> CouponDeadline.DaysLeft(days)
        else -> CouponDeadline.OnDate(last.dayOfMonth, last.monthNumber)
    }
}

private val couponMonths: List<StringResource> = listOf(
    Res.string.community_coupon_month_1, Res.string.community_coupon_month_2, Res.string.community_coupon_month_3,
    Res.string.community_coupon_month_4, Res.string.community_coupon_month_5, Res.string.community_coupon_month_6,
    Res.string.community_coupon_month_7, Res.string.community_coupon_month_8, Res.string.community_coupon_month_9,
    Res.string.community_coupon_month_10, Res.string.community_coupon_month_11, Res.string.community_coupon_month_12
)

@Composable
internal fun couponDeadlineText(deadline: CouponDeadline): String = when (deadline) {
    CouponDeadline.Today -> stringResource(Res.string.community_coupon_deadline_today)
    CouponDeadline.Tomorrow -> stringResource(Res.string.community_coupon_deadline_tomorrow)
    is CouponDeadline.DaysLeft -> stringResource(Res.string.community_coupon_deadline_days, deadline.days)
    is CouponDeadline.OnDate -> stringResource(
        Res.string.community_coupon_deadline_date,
        deadline.day,
        stringResource(couponMonths[deadline.month - 1])
    )
}

internal fun couponOutcomeMessage(outcome: String): UiText? = when (outcome) {
    "issued", "already_issued" -> null
    "already_used_today" -> UiText.StringResourceId(Res.string.community_coupon_used_today)
    "limit_reached" -> UiText.StringResourceId(Res.string.community_coupon_limit_reached)
    "daily_limit_reached" -> UiText.StringResourceId(Res.string.community_coupon_daily_limit_reached)
    "coupon_inactive" -> UiText.StringResourceId(Res.string.community_coupon_inactive)
    else -> UiText.StringResourceId(Res.string.community_coupon_code_error)
}
@Composable
internal fun couponDiscountText(data: CommunityEntryDto): String = when (data.discountType) {
    "fixed" -> stringResource(Res.string.community_coupon_fixed, data.discountValue)
    "freeItem" -> stringResource(Res.string.community_coupon_free_item, data.freeItem)
    else -> stringResource(Res.string.community_coupon_percentage, data.discountValue)
}
@Composable
internal fun CommunityCouponCodeContent(
    code: String?,
    seconds: Int,
    busy: Boolean,
    message: UiText?,
    expired: Boolean,
    onIssue: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        message?.let { Text(it.asString(), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (code != null && seconds > 0) {
            Text(
                code,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 6.sp,
                color = MaterialTheme.colorScheme.primary
            )
            val formattedSeconds = (seconds % 60).toString().padStart(2, '0')
            Text(stringResource(Res.string.community_coupon_remaining, "${seconds / 60}:$formattedSeconds"))
            Text(stringResource(Res.string.community_coupon_tell_business))
            Text(
                stringResource(Res.string.community_coupon_single_use),
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            Button(onClick = onIssue, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(
                    if (busy) Res.string.community_coupon_generating
                    else if (expired) Res.string.community_coupon_new_code
                    else Res.string.community_coupon_show_checkout
                ))
            }
        }
    }
}

@Preview
@Composable
private fun CommunityCouponCodePreview() {
    MaterialTheme {
        Surface {
            CommunityCouponCodeContent("0042", 599, false, null, false, {})
        }
    }
}
