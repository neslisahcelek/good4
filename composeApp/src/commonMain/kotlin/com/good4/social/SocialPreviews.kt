package com.good4.social

import androidx.compose.runtime.Composable
import com.good4.core.presentation.Good4Theme
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Duration.Companion.hours

private val previewMe = SocialMe(enabled = true, eduVerified = true, termsAccepted = true, universityName = "Akdeniz Üniversitesi")

private fun previewActivity(id: String, kind: String, type: String, title: String, inHours: Int, spots: Int = 2) = SocialActivity(
    id = id, kind = kind, type = type, title = title, startsAt = (Clock.System.now() + inHours.hours).toString(),
    capacity = 3, acceptedCount = 3 - spots, spotsLeft = spots, organizerName = "A.. Y..",
    universityName = "Akdeniz Üniversitesi", status = "open", note = "Raketim var, top getiriyorum."
)

@Preview
@Composable
private fun SocialFeedPreview() = Good4Theme {
    SocialContent(
        state = SocialHomeState(isLoading = false, me = previewMe, activities = listOf(
            previewActivity("a", "social", "coffee", "Sınav sonrası kahve", 3),
            previewActivity("b", "sport", "basketball", "3'e 3 basket", 5, spots = 1),
            previewActivity("c", "sport", "cycling", "Sahil turu", 26),
            previewActivity("d", "social", "board-games", "Kutu oyunu akşamı", 30)
        )),
        tab = 0, onTab = {}, onBack = {}, onOpenActivity = {}, onCreate = {}, onOpenInbox = {}, onOpenChat = {},
        onSelectKind = {}, onLoadMore = {}, onRetry = {}, onRetryMine = {}, onAcceptTerms = { _, _ -> },
        onUpdateProfile = { _, _, _ -> }, onProfilePhotoError = {}
    )
}

@Preview
@Composable
private fun SocialDetailPreview() = Good4Theme {
    SocialActivityContent(
        state = SocialActivityState(isLoading = false, detail = SocialActivityDetail(
            me = previewMe, activity = previewActivity("a", "sport", "tennis", "Akşam tenisi", 30).copy(level = "beginner"), sameCampus = true
        )),
        onBack = {}, onNote = {}, onJoin = {}, onWithdraw = {}, onOpenRequests = {}, onOpenChat = {}, onCancel = {}, onReport = { _, _ -> }
    )
}

@Preview
@Composable
private fun SocialCreatePreview() = Good4Theme {
    val start = (Clock.System.now() + 3.hours).toLocalDateTime(TimeZone.currentSystemDefault())
    SocialCreateContent(
        state = SocialCreateState(type = "tennis", date = start.date, hour = 18, minute = 0),
        generatedTitle = generatedTitle(start.date, 18, "tennis"), quickDates = listOf(start.date),
        onBack = {}, onClose = {}, onSelectKind = {}, onSelectType = {}, onTitle = {}, onNote = {}, onDate = {},
        onTime = { _, _ -> }, onLevel = {}, onGame = {}, onCapacity = {}, onSubmit = {}
    )
}
