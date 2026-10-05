package com.good4.campuscloset

import com.good4.core.presentation.components.StandardButtonLoadingIndicatorSize
import com.good4.core.presentation.*
import good4.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.good4.core.presentation.AppBackground
import com.good4.core.presentation.BorderMuted
import com.good4.core.presentation.ErrorRed
import com.good4.core.presentation.PrimaryGreen
import com.good4.core.presentation.SurfaceDefault
import com.good4.core.presentation.TextPrimary
import com.good4.core.presentation.TextSecondary
import com.good4.core.presentation.components.Good4Scaffold
import com.good4.core.presentation.components.Good4TopBar
import org.koin.compose.viewmodel.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusClosetListingScreen(
    listingId: String,
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    viewModel: CampusClosetListingViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var reporting by remember { mutableStateOf(false) }
    var offering by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }

    LaunchedEffect(listingId) { viewModel.load(listingId) }
    LaunchedEffect(state.removed) { if (state.removed) onBack() }
    LaunchedEffect(state.openConversationId) {
        state.openConversationId?.let {
            viewModel.consumeNavigation()
            onOpenChat(it)
        }
    }
    val detail = state.detail
    val listing = detail?.listing

    Good4Scaffold(
        topBar = {
            Good4TopBar(
                title = listing?.let { categoryLabel(it.category) } ?: stringResource(Res.string.campus_closet_ilan),
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.campus_closet_back)) }
                },
                actions = {
                    if (listing != null && !listing.isMine) {
                        IconButton(onClick = viewModel::toggleFavorite) {
                            Icon(
                                if (listing.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (listing.isFavorite) stringResource(Res.string.campus_closet_kaydedilenlerden_cikar) else stringResource(Res.string.campus_closet_save),
                                tint = if (listing.isFavorite) ErrorRed else TextSecondary
                            )
                        }
                        IconButton(onClick = { reporting = true }) {
                            Icon(Icons.Outlined.Flag, contentDescription = stringResource(Res.string.campus_closet_report), tint = TextSecondary)
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (detail != null) {
                ActionBar(
                    detail = detail,
                    busy = state.busy,
                    onVerify = onBack,
                    onMessage = viewModel::openChat,
                    onOffer = { offering = true },
                    onStatus = { action ->
                        if (action == "remove") confirmRemove = true
                        else viewModel.updateStatus(action)
                    }
                )
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().background(AppBackground).padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Center)
                )
                listing == null -> CenteredState(state.loadError?.asString() ?: stringResource(Res.string.campus_closet_ilan_bulunamadi), stringResource(Res.string.campus_closet_geri_don), onBack)
                else -> Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)
                ) {
                    PhotoPager(listing)
                    Column(
                        Modifier.padding(horizontal = 16.dp).padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    formatPrice(listing.price),
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (listing.price == 0) MaterialTheme.colorScheme.primary else TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (listing.status != "published") ListingStatusChip(listing.status)
                            }
                            Text(listing.title, fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        }
                        InfoRow(listing)
                        state.message?.let { MarketNotice(it) }
                        if (listing.isMine) SellerNotice(listing)
                        if (listing.canRenew(kotlinx.datetime.Clock.System.now().toEpochMilliseconds())) {
                            Button(
                                onClick = viewModel::renew,
                                enabled = !state.busy,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(stringResource(Res.string.campus_closet_30_gun_daha_yayinda_tut_hak, listing.renewsLeft)) }
                        }
                        if (listing.isMine && listing.status in listOf("pending", "published", "reserved")) {
                            OutlinedButton(
                                onClick = viewModel::beginPriceEdit,
                                enabled = !state.busy,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(Res.string.campus_closet_edit_price))
                            }
                        }
                        SellerCard(listing)
                        listing.description?.takeIf { it.isNotBlank() }?.let { DescriptionCard(it) }
                        SafetyCard()
                        if (listing.isMine && listing.status in listOf("pending", "rejected")) {
                            TextButton(onClick = { confirmRemove = true }, enabled = !state.busy, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                                Text(stringResource(Res.string.campus_closet_ilani_kaldir), color = ErrorRed)
                            }
                        }
                    }
                }
            }
        }
    }

    if (reporting) {
        ReportDialog(
            title = stringResource(Res.string.campus_closet_ilani_sikayet_et),
            onDismiss = { reporting = false },
            onSubmit = { reason, note ->
                reporting = false
                viewModel.report(reason, note)
            }
        )
    }
    if (offering && listing != null) {
        ModalBottomSheet(
            onDismissRequest = { offering = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = AppBackground
        ) {
            OfferSheet(price = listing.price) { percent ->
                offering = false
                viewModel.sendOffer(percent)
            }
        }
    }
    state.priceEdit?.let { draft ->
        PriceEditDialog(draft, viewModel::setEditPrice, viewModel::setEditFree,
            viewModel::dismissPriceEdit, viewModel::savePriceEdit)
    }
    if (confirmRemove && listing != null) {
        RemoveListingDialog(
            listingTitle = listing.title,
            onDismiss = { confirmRemove = false },
            onConfirm = {
                confirmRemove = false
                viewModel.updateStatus("remove")
            },
        )
    }
}

@Composable
private fun PhotoPager(listing: MarketListing) {
    val photos = listing.photos
    val shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
    if (photos.isEmpty()) {
        ListingPhoto(null, Modifier.fillMaxWidth().aspectRatio(1.15f).clip(shape), category = listing.category, iconSize = 64)
        return
    }
    val pagerState = rememberPagerState { photos.size }
    Box(Modifier.clip(shape)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().aspectRatio(1f)) { page ->
            ListingPhoto(photos[page].url, Modifier.fillMaxSize(), category = listing.category, iconSize = 64)
        }
        if (photos.size > 1) {
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.5f)
            ) {
                Text(
                    "${pagerState.currentPage + 1}/${photos.size}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun InfoChip(icon: ImageVector?, label: String, accent: Color? = null) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceDefault,
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.55f))
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = accent ?: TextSecondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(label, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun InfoRow(listing: MarketListing) {
    val style = CATEGORY_STYLES[listing.category] ?: CATEGORY_STYLES[null]!!
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        InfoChip(style.icon, stringResource(style.label), style.accent)
        conditionLabel(listing.condition).takeIf { it.isNotBlank() }?.let { InfoChip(null, it) }
        formatRelativeTime(listing.publishedAt ?: listing.createdAt).takeIf { it.isNotBlank() }?.let {
            InfoChip(Icons.Outlined.Schedule, it)
        }
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceDefault,
        border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.55f)),
        shadowElevation = 1.dp
    ) { content() }
}

@Composable
private fun SellerCard(listing: MarketListing) {
    SectionCard {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(PrimaryGreen.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    listing.sellerName.firstOrNull()?.uppercase() ?: stringResource(Res.string.campus_closet_o),
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 18.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (listing.isMine) stringResource(Res.string.campus_closet_senin_ilanin) else listing.sellerName,
                    fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary
                )
                Text(listing.universityName, fontSize = 12.sp, color = TextSecondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(Res.string.campus_closet_dogrulanmis_ogrenci), fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun DescriptionCard(text: String) {
    SectionCard {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(Res.string.campus_closet_aciklama), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(text, fontSize = 15.sp, lineHeight = 22.sp, color = TextPrimary)
        }
    }
}

@Composable
private fun SafetyCard() {
    SectionCard {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.campus_closet_elden_teslim), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                Text(
                    stringResource(Res.string.campus_closet_kampuste_kalabalik_bir_yerde_bulus_urunu_gormeden_odeme_ya),
                    fontSize = 12.sp, lineHeight = 17.sp, color = TextSecondary
                )
            }
            Spacer(Modifier.width(12.dp))
            TiltedIcon(Icons.Outlined.Shield, PrimaryGreen)
        }
    }
}

@Composable
private fun SellerNotice(listing: MarketListing) {
    when (listing.status) {
        "pending" -> MarketNotice(stringResource(Res.string.campus_closet_ilanin_inceleniyor_yaklasik_30_dakika_icinde_yayina_alinir), color = ClosetOfferAccent)
        "rejected" -> MarketNotice(
            stringResource(Res.string.campus_closet_ilanin_yayinlanmadi) + (listing.rejectReason?.let { stringResource(Res.string.campus_closet_gerekce, it) } ?: ""),
            color = ErrorRed
        )
        "reserved" -> MarketNotice(stringResource(Res.string.campus_closet_ilan_rezerve_olarak_gorunuyor_yeni_teklif_alinmiyor), color = ClosetOfferAccent)
        "inactive" -> MarketNotice(stringResource(Res.string.campus_closet_inactive_notice), color = TextSecondary)
        "sold" -> MarketNotice(stringResource(Res.string.campus_closet_ilan_satildi_olarak_isaretlendi), color = TextSecondary)
        "expired" -> MarketNotice(
            if (listing.renewsLeft > 0) stringResource(Res.string.campus_closet_ilanin_suresi_doldu_ve_yayindan_kalkti_30_gun_daha)
            else stringResource(Res.string.campus_closet_ilanin_suresi_doldu_ve_yayindan_kalkti_hala_satiliksa_yeni),
            color = ErrorRed
        )
        "published", "reserved" -> listing.daysLeft(kotlinx.datetime.Clock.System.now().toEpochMilliseconds())?.let { days ->
            MarketNotice(
                if (days == 0) stringResource(Res.string.campus_closet_ilanin_bugun_yayindan_kalkacak) else stringResource(Res.string.campus_closet_ilanin_gun_sonra_yayindan_kalkacak, days),
                color = if (days <= 3) ClosetOfferAccent else TextSecondary
            )
        }
    }
}

/** Sticky bottom bar: the one or two actions that matter for this viewer. */
@Composable
private fun ActionBar(
    detail: MarketListingDetail,
    busy: Boolean,
    onVerify: () -> Unit,
    onMessage: () -> Unit,
    onOffer: () -> Unit,
    onStatus: (String) -> Unit
) {
    val listing = detail.listing
    val me = detail.me
    val content: (@Composable () -> Unit)? = when {
        listing.isMine -> when (listing.status) {
            "published", "reserved" -> { { BarButtons(stringResource(Res.string.campus_closet_yayindan_kaldir), { onStatus("remove") }, stringResource(Res.string.campus_closet_satildi), { onStatus("markSold") }, busy) } }
            "sold" -> { { BarButtons(stringResource(Res.string.campus_closet_yayindan_kaldir), { onStatus("remove") }, stringResource(Res.string.campus_closet_tekrar_yayina_al), { onStatus("markAvailable") }, busy) } }
            else -> null
        }
        !me.eduVerified -> { { BarButtons(null, {}, stringResource(Res.string.campus_closet_dogrula_mesaj_gonder), onVerify, busy) } }
        me.suspendedUntil != null -> { { BarText(stringResource(Res.string.campus_closet_kampus_dolabi_erisimin_gecici_olarak_kapali)) } }
        !detail.sameCampus && detail.conversationId == null -> { { BarText(stringResource(Res.string.campus_closet_bu_ilan_baska_bir_kampuste_yalnizca_kendi_kampusundeki_ilanlara)) } }
        else -> {
            {
                val canOffer = listing.status == "published" && listing.price > 0
                BarButtons(
                    if (canOffer) stringResource(Res.string.campus_closet_teklif_ver) else null, onOffer,
                    if (detail.conversationId != null) stringResource(Res.string.campus_closet_mesajlara_git) else stringResource(Res.string.campus_closet_mesaj_gonder), onMessage,
                    busy,
                    secondaryIcon = Icons.Outlined.LocalOffer,
                    primaryIcon = Icons.Outlined.ChatBubbleOutline
                )
            }
        }
    }
    if (content == null) return
    Surface(color = SurfaceDefault, shadowElevation = 8.dp) {
        Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
            content()
        }
    }
}

@Composable
private fun BarText(text: String) {
    Text(text, fontSize = 13.sp, lineHeight = 18.sp, color = TextSecondary)
}

@Composable
private fun BarButtons(
    secondaryLabel: String?,
    onSecondary: () -> Unit,
    primaryLabel: String,
    onPrimary: () -> Unit,
    busy: Boolean,
    secondaryIcon: ImageVector? = null,
    primaryIcon: ImageVector? = null
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        if (secondaryLabel != null) {
            OutlinedButton(
                onClick = onSecondary,
                enabled = !busy,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
            ) {
                secondaryIcon?.let {
                    Icon(it, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                }
                Text(secondaryLabel, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        }
        Button(
            onClick = onPrimary,
            enabled = !busy,
            modifier = Modifier.weight(if (secondaryLabel != null) 1.3f else 1f).height(50.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            if (busy) {
                CircularProgressIndicator(Modifier.size(StandardButtonLoadingIndicatorSize), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                primaryIcon?.let {
                    Icon(it, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(primaryLabel, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun OfferSheet(price: Int, onOffer: (Int) -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(stringResource(Res.string.campus_closet_teklif_ver), fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Text(
            stringResource(Res.string.campus_closet_ilan_fiyati_teklifin_saticiya_mesaj_olarak_gider_satici_kabul, formatPrice(price)),
            fontSize = 13.sp, lineHeight = 18.sp, color = TextSecondary
        )
        CampusClosetLimits.OFFER_PERCENTS.forEach { percent ->
            Surface(
                onClick = { onOffer(percent) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = SurfaceDefault,
                border = BorderStroke(1.dp, BorderMuted.copy(alpha = 0.55f)),
                shadowElevation = 1.dp
            ) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(Res.string.campus_closet_indirim, percent), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.weight(1f))
                    Text(formatPrice(offerPrice(price, percent)), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
