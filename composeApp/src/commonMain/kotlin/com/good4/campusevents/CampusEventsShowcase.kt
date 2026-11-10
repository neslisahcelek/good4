package com.good4.campusevents

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.datetime.LocalDateTime

/**
 * Debug-only preview of the events box with sample data taken from etkinlik.akdeniz.edu.tr listings.
 * Opened on iOS with `-campusEventsShowcase <full|empty>`; nothing here reads Firebase.
 */
object CampusEventsShowcaseData {
    private const val POSTER = "https://etkinlik.akdeniz.edu.tr/static/uploads/770dc52b_cakrlar_tur_postu.jpeg"

    private fun event(
        id: String, title: String, community: String, location: String,
        start: String, end: String?, category: String, description: String = "", image: String = ""
    ) = CampusEvent(id, title, community, location, description, LocalDateTime.parse(start), end?.let(LocalDateTime::parse), image, category)

    val events = listOf(
        event("144", "Kampüste Yoga", "Akdeniz Fizyoterapi ve Egzersiz Topluluğu", "Olbia yanı çim alan", "2026-11-11T18:00", "2026-11-11T19:30", "sports-nature",
            "Mat getirmeyi unutma, herkes katılabilir."),
        event("95", "Kareoke Gecesi", "Manavgat Akademi Topluluğu", "Manavgat Gençlik Merkezi", "2026-11-11T18:30", "2026-11-11T22:00", "social-entertainment"),
        event("133", "Konyaaltı Yer Eğitimi", "Havacılık Topluluğu", "Konyaaltı Sahili / Antalya", "2026-11-11T09:00", "2026-11-11T13:00", "sports-nature"),
        event("146", "Tanışma Toplantısı", "Kültür ve Turizm Topluluğu", "Akdeniz Üniversitesi Kampüs içi", "2026-11-12T17:00", "2026-11-12T18:30", "social-entertainment"),
        event("149", "Sillyon-Perge Antik Kent Gezisi", "Akdeniz ve Türk Uygarlıkları Araştırmaları Topluluğu", "Sillyon ve Perge Antik kentleri", "2026-11-13T08:30", "2026-11-13T18:00", "culture-arts",
            "Antik kentlerde rehberli gezi. Ulaşım topluluk tarafından sağlanacak."),
        event("141", "100 Meters İzleme Etkinliği", "Akdeniz Bilim Kurgu ve Fantezi Topluluğu", "Olbia B Salonu", "2026-11-13T18:00", "2026-11-13T20:30", "culture-arts"),
        event("67", "Kocain Mağarası Etkinliği", "Mağara Araştırmaları Topluluğu", "Döşemealtı / Antalya", "2026-11-13T12:00", "2026-11-13T18:00", "sports-nature"),
        event("125", "Adrasan'da Kamp & SUP", "Akdeniz Üniversitesi Kamp ve Ekstrem Sporları Topluluğu", "Kumluca, Karaöz", "2026-11-14T12:00", "2026-11-15T16:00", "sports-nature"),
        event("155", "Çakırlar Turu", "Bisiklet Topluluğu", "Stadyum otoparkı", "2026-11-15T09:30", "2026-11-15T15:00", "sports-nature", "Çakırlar bisiklet turu.", POSTER),
        event("136", "Bahçealtı - Boyun Doğa Yürüyüşü", "Dağcılık Topluluğu", "Gedeller", "2026-11-15T08:00", "2026-11-15T16:00", "sports-nature"),
        event("162", "LÖSEV Farkındalık Korteji", "Akdeniz Üniversitesi Motosiklet Topluluğu", "Üç Kapılar – Yalım Park", "2026-11-18T12:00", "2026-11-18T17:00", "other",
            "Kortej başlangıç noktası Üç Kapılar, varış noktası Yalım Park olmak üzere LÖSEV haftasında farkındalığı arttırmak amacıyla düzenlenecektir."),
        event("174", "Buz Pateni Etkinliği", "Buz Hokeyi Topluluğu", "Muratpaşa Belediyesi Buz Paten Pisti", "2026-11-21T13:00", "2026-11-21T15:00", "sports-nature"),
        event("170", "Bisiklet Bakım Workshop", "Bisiklet Topluluğu", "Stadyum otoparkı", "2026-11-28T12:00", "2026-11-28T14:00", "other")
    )
}

@Composable
fun CampusEventsShowcase(scenario: String) {
    var category by remember { mutableStateOf<String?>(null) }
    var open by remember { mutableStateOf<CampusEvent?>(null) }
    val events = if (scenario == "empty") emptyList() else CampusEventsShowcaseData.events
    val current = open
    if (current != null) {
        CampusEventDetailScreen(current, onBack = { open = null })
    } else {
        CampusEventsScreen(
            ui = CampusEventsUi(events, category),
            onBack = {},
            onSelectCategory = { category = it },
            onOpenEvent = { open = it }
        )
    }
}
