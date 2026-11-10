package com.good4.community.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.good4.community.Community
import com.good4.community.CommunityDto
import com.good4.community.CommunityEntry
import com.good4.community.CommunityEntryDto
import com.good4.community.CommunityFeaturedEvent
import good4.composeapp.generated.resources.Res
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Debug-only design showcase for the community discovery redesign, with sample data only.
 * Opened on iOS with `-communitiesShowcase <scenario>`; a small developer pill cycles scenarios.
 * Nothing here talks to Firebase or the real screen state.
 */
object CommunityDiscoverShowcaseData {
    val scenarios = listOf(
        "full", "no-events", "no-coupons", "no-communities", "followed-empty", "search-empty",
        "all-empty", "loading", "error", "long-text", "single-event", "single-community", "poster-event", "followed"
    )

    private val today = LocalDate(2026, 11, 8)

    @OptIn(ExperimentalResourceApi::class)
    private val samplePoster = Res.getUri("drawable/akdeniz_campus_weather_day_courts.jpg")

    private fun community(id: String, name: String, description: String) =
        Community(id, CommunityDto(name = name, university = "Akdeniz Üniversitesi", description = description))

    private val bilisim = community("bilisim", "Akdeniz Bilişim Topluluğu", "Yazılım, yapay zekâ ve hackathon etkinlikleri")
    private val dag = community("dag", "Dağcılık ve Doğa Sporları", "Hafta sonu yürüyüşleri, kamp ve tırmanış")
    private val muzik = community("muzik", "Müzik Topluluğu", "Kampüs konserleri ve jam günleri")
    private val girisim = community("girisim", "Girişimcilik Kulübü", "Fikirden ürüne: atölyeler ve mentor buluşmaları")
    private val sinema = community("sinema", "Sinema ve Kısa Film", "Haftalık gösterimler ve çekim günleri")
    private val longName = community(
        "uzun", "Uluslararası Öğrenci Değişimi ve Kültürlerarası Diyalog Topluluğu",
        "Erasmus, değişim programları, dil kafeleri, kültür geceleri ve yurt dışı deneyimlerini paylaşmak isteyen herkes için uzun bir açıklama metni"
    )

    private val communities = listOf(bilisim, dag, muzik, girisim, sinema)

    private fun event(id: String, c: Community, title: String, date: String, time: String, poster: Boolean = false) =
        CommunityFeaturedEvent(
            c,
            CommunityEntry(id, CommunityEntryDto(kind = "event", title = title, date = date, time = time, location = "Kampüs", imageUrl = if (poster) samplePoster else ""))
        )

    private val events = listOf(
        event("e1", bilisim, "Yapay Zekâ Atölyesi: İlk Modelini Eğit", "2026-11-09", "18:00"),
        event("e2", dag, "Saklıkent Kanyon Yürüyüşü", "2026-11-14", "08:30"),
        event("e3", muzik, "Akustik Akşam", "2026-11-15", "20:00"),
        event("e4", sinema, "Kısa Film Gösterimi", "2026-11-19", "19:30")
    )

    private fun coupon(id: String, c: Community, title: String, type: String, value: Int, until: String, freeItem: String = "") =
        CommunityFeaturedEvent(
            c,
            CommunityEntry(id, CommunityEntryDto(kind = "coupon", title = title, date = until, discountType = type, discountValue = value, freeItem = freeItem, location = "Kampüs"))
        )

    private val coupons = listOf(
        coupon("c1", bilisim, "Kampüs Kafe'de kahve", "percentage", 20, "2026-11-09"),
        coupon("c2", girisim, "Kırtasiye alışverişinde indirim", "fixed", 50, "2026-11-30"),
        coupon("c3", muzik, "Konser bileti", "freeItem", 0, "2026-11-20", freeItem = "Ücretsiz konser bileti ve içecek"),
        coupon("c4", dag, "Kamp ekipmanı kiralamada indirim", "percentage", 15, "2026-12-15")
    )

    fun ui(scenario: String, query: String, followedOnly: Boolean): CommunityDiscoverUi {
        val base = CommunityDiscoverUi(today = today, query = query, followedOnly = followedOnly)
        val followedIds = if (scenario == "followed") setOf("bilisim", "muzik") else emptySet()
        fun list(all: List<Community>): List<Community> = all
            .filter { !followedOnly || it.id in followedIds }
            .filter { it.data.name.contains(query.trim(), ignoreCase = true) }
        return when (scenario) {
            "loading" -> base.copy(loading = true)
            "error" -> base.copy(loadFailed = true)
            "all-empty" -> base
            "no-events" -> base.copy(coupons = coupons, communities = list(communities))
            "no-coupons" -> base.copy(events = events, communities = list(communities))
            "no-communities" -> base.copy(events = events, coupons = coupons, communities = emptyList())
            "followed-empty" -> base.copy(events = events, coupons = coupons, communities = list(communities))
            "search-empty" -> base.copy(events = events, coupons = coupons, communities = list(communities))
            "long-text" -> base.copy(
                events = listOf(event("el", longName, "Kültürlerarası Diyalog Haftası Açılış Paneli ve Tanışma Etkinliği", "2026-11-10", "17:00")),
                coupons = coupons,
                communities = list(listOf(longName, bilisim, community("x", "İ", "")))
            )
            "single-event" -> base.copy(events = events.take(1), coupons = coupons, communities = list(communities))
            "single-community" -> base.copy(events = events, coupons = coupons, communities = list(listOf(dag)))
            "poster-event" -> base.copy(
                events = listOf(event("ep", bilisim, "Hackathon 2026: 24 Saatte Kampüs Uygulaması", "2026-11-09", "10:00", poster = true)) + events.drop(1),
                coupons = coupons,
                communities = list(communities)
            )
            else -> base.copy(events = events, coupons = coupons, communities = list(communities))
        }
    }

    /** Scenarios that start with a typed query or on the followed tab. */
    fun initialQuery(scenario: String) = if (scenario == "search-empty") "Satranç" else ""
    fun initialFollowed(scenario: String) = scenario == "followed-empty" || scenario == "followed"
}

@Composable
fun CommunityDiscoverShowcase(initialScenario: String) {
    var scenarioIndex by remember {
        mutableStateOf(CommunityDiscoverShowcaseData.scenarios.indexOf(initialScenario).coerceAtLeast(0))
    }
    val scenario = CommunityDiscoverShowcaseData.scenarios[scenarioIndex]
    var query by remember(scenario) { mutableStateOf(CommunityDiscoverShowcaseData.initialQuery(scenario)) }
    var followedOnly by remember(scenario) { mutableStateOf(CommunityDiscoverShowcaseData.initialFollowed(scenario)) }
    val ui = CommunityDiscoverShowcaseData.ui(scenario, query, followedOnly)

    Box(Modifier.fillMaxSize()) {
        CommunityDiscoverScreen(
            ui = ui,
            actions = CommunityDiscoverActions(
                onQueryChange = { query = it },
                onFollowedOnlyChange = { followedOnly = it }
            )
        )
        // Developer-only scenario switcher; this file is reachable only from the debug launch argument.
        Text(
            "▸ $scenario",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(12.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable { scenarioIndex = (scenarioIndex + 1) % CommunityDiscoverShowcaseData.scenarios.size }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
