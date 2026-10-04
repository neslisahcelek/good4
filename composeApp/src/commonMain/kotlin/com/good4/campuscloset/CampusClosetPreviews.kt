package com.good4.campuscloset

import androidx.compose.runtime.Composable
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.good4.core.presentation.Good4Theme
import good4.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Preview
@Composable
private fun MyListingsPreview() = Good4Theme {
    CampusClosetMyListingsContent(
        state = CampusClosetMyListingsState(isLoading = false, listings = listOf(
            MarketListing("preview", title = stringResource(Res.string.campus_closet_orn_kislik_mont_m_beden),
                price = 250, category = "clothing", status = "published", isMine = true)
        )),
        onBack = {}, onOpenListing = {}, onRetry = {}, onUpdateStatus = { _, _ -> }
    )
}

@Preview
@Composable
private fun ChatPreview() = Good4Theme {
    CampusClosetChatContent(
        conversationId = "preview_buyer", state = CampusClosetChatState(isLoading = false, isNew = true),
        onBack = {}, onOpenListing = {}, onDraftChange = {}, onMeetingPoint = {}, onSend = {}, onOffer = {},
        onRespondOffer = {}, onReport = { _, _ -> }, onBlock = {}
    )
}

@Preview
@Composable
private fun PriceEditPreview() = Good4Theme {
    PriceEditDialog(CampusClosetPriceEditState("preview", 250), {}, {}, {}, {})
}

@Preview
@Composable
private fun ListingCardDarkPreview() = Good4Theme(darkTheme = true) {
    ListingGridCard(MarketListing("preview", title = stringResource(Res.string.campus_closet_orn_kislik_mont_m_beden),
        price = 250, category = "clothing", status = "published"), showUniversity = false, onClick = {})
}
