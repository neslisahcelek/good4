package com.good4.campuscloset

import good4.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.StringResource
import androidx.compose.runtime.Composable
import com.good4.core.presentation.UiText

internal val MARKET_CATEGORIES = listOf(
    "clothing" to Res.string.campus_closet_kiyafet_ayakkabi,
    "accessories" to Res.string.campus_closet_canta_aksesuar,
    "electronics" to Res.string.campus_closet_elektronik,
    "sports" to Res.string.campus_closet_spor_outdoor,
    "books" to Res.string.campus_closet_kitap_kirtasiye,
    "dorm" to Res.string.campus_closet_yurt_ev_esyasi,
    "hobby" to Res.string.campus_closet_hobi_muzik,
    "other" to Res.string.campus_closet_other
)

internal val MARKET_CONDITIONS = listOf(
    "new" to Res.string.campus_closet_yeni_etiketli,
    "likeNew" to Res.string.campus_closet_yeni_gibi,
    "good" to Res.string.campus_closet_iyi,
    "fair" to Res.string.campus_closet_kullanilmis
)

internal val MARKET_REPORT_REASONS = listOf(
    "prohibited" to Res.string.campus_closet_yasakli_urun,
    "scam" to Res.string.campus_closet_dolandiricilik_suphesi,
    "harassment" to Res.string.campus_closet_taciz_veya_hakaret,
    "inappropriate" to Res.string.campus_closet_uygunsuz_icerik,
    "other" to Res.string.campus_closet_other
)

@Composable
internal fun categoryLabel(id: String) = MARKET_CATEGORIES.firstOrNull { it.first == id }?.second?.let { stringResource(it) } ?: stringResource(Res.string.campus_closet_other)
@Composable
internal fun conditionLabel(id: String) = MARKET_CONDITIONS.firstOrNull { it.first == id }?.second?.let { stringResource(it) } ?: ""

@Composable
internal fun statusLabel(status: String) = when (status) {
    "pending" -> stringResource(Res.string.campus_closet_inceleniyor)
    "inactive" -> stringResource(Res.string.campus_closet_inactive)
    "published" -> stringResource(Res.string.campus_closet_yayinda)
    "reserved" -> stringResource(Res.string.campus_closet_rezerve)
    "sold" -> stringResource(Res.string.campus_closet_satildi)
    "rejected" -> stringResource(Res.string.campus_closet_yayinlanmadi)
    "expired" -> stringResource(Res.string.campus_closet_suresi_doldu)
    else -> stringResource(Res.string.campus_closet_kaldirildi)
}

/** Same rounding as the server's offer price. */
internal fun offerPrice(price: Int, percent: Int): Int = (price * (100 - percent) + 50) / 100

@Composable
internal fun formatPrice(price: Int): String {
    if (price == 0) return stringResource(Res.string.campus_closet_free)
    val digits = price.toString()
    val grouped = digits.reversed().chunked(3).joinToString(".").reversed()
    return stringResource(Res.string.campus_closet_price_value, grouped)
}

internal fun campusClosetErrorMessage(error: Throwable): UiText = when (error.message) {
    "MARKET_EDU_REQUIRED" -> UiText.StringResourceId(Res.string.campus_closet_bu_islem_icin_edu_tr_adresini_dogrulaman_gerekiyor)
    "MARKET_TERMS_REQUIRED", "MARKET_TERMS_VERSION_OUTDATED" -> UiText.StringResourceId(Res.string.campus_closet_devam_etmek_icin_kampus_dolabi_kurallarini_kabul_et)
    "MARKET_SUSPENDED" -> UiText.StringResourceId(Res.string.campus_closet_kampus_dolabi_erisimin_gecici_olarak_kapatildi)
    "MARKET_DISABLED" -> UiText.StringResourceId(Res.string.campus_closet_kampus_dolabi_su_an_bakimda_daha_sonra_tekrar_dene)
    "MARKET_CONTENT_BLOCKED" ->
        UiText.StringResourceId(Res.string.campus_closet_bu_urunun_satisi_kampus_dolabi_nda_yasaktir_yasak_urun)
    "MARKET_CONTENT_BLOCKED_SUSPENDED" ->
        UiText.StringResourceId(Res.string.campus_closet_yasak_urun_iceren_tekrarlanan_denemeler_nedeniyle_kampus_dolabi_erisimin)
    "MARKET_PHONE_IN_LISTING" -> UiText.StringResourceId(Res.string.campus_closet_ilan_metnine_telefon_numarasi_yazilamaz_numarani_anlastigin_kisiyle_mesajlarda)
    "MARKET_DAILY_LISTING_LIMIT" -> UiText.StringResourceId(Res.string.campus_closet_bugun_en_fazla_5_ilan_verebilirsin_yarin_tekrar_dene)
    "MARKET_ACTIVE_LISTING_LIMIT" -> UiText.StringResourceId(Res.string.campus_closet_ayni_anda_en_fazla_15_aktif_ilanin_olabilir_satilan)
    "MARKET_DAILY_MESSAGE_LIMIT" -> UiText.StringResourceId(Res.string.campus_closet_bu_ilan_icin_bugunku_200_mesaj_hakkin_doldu_yarin)
    "MARKET_DAILY_CONVERSATION_LIMIT" -> UiText.StringResourceId(Res.string.campus_closet_bugun_en_fazla_15_yeni_konusma_baslatabilirsin)
    "MARKET_BLOCKED" -> UiText.StringResourceId(Res.string.campus_closet_bu_konusmaya_mesaj_gonderilemiyor)
    "MARKET_LISTING_UNAVAILABLE", "MARKET_LISTING_NOT_FOUND" -> UiText.StringResourceId(Res.string.campus_closet_bu_ilan_artik_yayinda_degil)
    "MARKET_OTHER_CAMPUS" -> UiText.StringResourceId(Res.string.campus_closet_bu_ilan_baska_bir_kampuse_ait_kampus_dolabi_elden)
    "MARKET_OFFER_PENDING" -> UiText.StringResourceId(Res.string.campus_closet_onceki_teklifin_henuz_yanitlanmadi)
    "MARKET_OFFER_UNAVAILABLE" -> UiText.StringResourceId(Res.string.campus_closet_bu_ilana_teklif_verilemiyor)
    "MARKET_OFFER_NOT_PENDING" -> UiText.StringResourceId(Res.string.campus_closet_bu_teklif_zaten_yanitlanmis)
    "MARKET_NOT_PARTICIPANT" -> UiText.StringResourceId(Res.string.campus_closet_bu_konusmaya_erisimin_yok)
    "MARKET_REPORT_SELF" -> UiText.StringResourceId(Res.string.campus_closet_kendi_ilanini_sikayet_edemezsin)
    "MARKET_PHOTOS_INVALID" -> UiText.StringResourceId(Res.string.campus_closet_en_az_1_en_fazla_3_fotograf_ekle)
    "MARKET_PHOTO_INVALID", "MARKET_PHOTO_CONTENT_INVALID" -> UiText.StringResourceId(Res.string.campus_closet_fotograf_okunamadi_baska_bir_fotograf_dene)
    "MARKET_TITLE_INVALID", "MARKET_TITLE_REQUIRED" -> UiText.StringResourceId(Res.string.campus_closet_baslik_3_60_karakter_olmali)
    "MARKET_DESCRIPTION_INVALID", "MARKET_DESCRIPTION_REQUIRED" -> UiText.StringResourceId(Res.string.campus_closet_aciklama_10_600_karakter_olmali)
    "MARKET_PRICE_INVALID" -> UiText.StringResourceId(Res.string.campus_closet_fiyat_0_ile_100_000_arasinda_tam_sayi_olmali)
    "MARKET_LISTING_STATUS_INVALID" -> UiText.StringResourceId(Res.string.campus_closet_bu_ilanin_durumu_bu_isleme_izin_vermiyor)
    "MARKET_RENEW_LIMIT" -> UiText.StringResourceId(Res.string.campus_closet_bu_ilan_en_fazla_3_kez_uzatilabilir_hala_satiliksa)
    "MARKET_FAVORITE_LIMIT" -> UiText.StringResourceId(Res.string.campus_closet_en_fazla_100_ilan_kaydedebilirsin_eskilerden_bazilarini_kaldir)
    "MARKET_QUERY_INVALID" -> UiText.StringResourceId(Res.string.campus_closet_arama_en_fazla_60_karakter_olabilir)
    "MARKET_MESSAGE_INVALID" -> UiText.StringResourceId(Res.string.campus_closet_mesaj_en_fazla_500_karakter_olabilir)
    "ACCOUNT_NOT_ACTIVE" -> UiText.StringResourceId(Res.string.campus_closet_hesabin_henuz_aktif_degil)
    "ROLE_NOT_ALLOWED" -> UiText.StringResourceId(Res.string.campus_closet_kampus_dolabi_yalnizca_ogrenci_hesaplari_icin)
    else -> UiText.StringResourceId(Res.string.campus_closet_islem_tamamlanamadi_baglantini_kontrol_edip_tekrar_dene)
}

internal fun campusEmailVerificationError(error: Throwable): UiText = when (error.message) {
    "CAMPUS_EMAIL_INVALID" -> UiText.StringResourceId(Res.string.campus_closet_yalnizca_ogr_akdeniz_edu_tr_uzantili_ogrenci_adresleri_kabul)
    "EDU_EMAIL_IN_USE" -> UiText.StringResourceId(Res.string.campus_closet_bu_universite_adresi_baska_bir_good4_hesabiyla_dogrulanmis)
    "CAMPUS_EMAIL_RESEND_TOO_SOON" -> UiText.StringResourceId(Res.string.campus_closet_yeni_baglanti_istemeden_once_bir_dakika_bekleyin)
    "CAMPUS_EMAIL_SEND_LIMIT" -> UiText.StringResourceId(Res.string.campus_closet_cok_fazla_baglanti_istendi_bir_saat_sonra_tekrar_deneyin)
    "CAMPUS_EMAIL_ACCOUNT_MISMATCH" -> UiText.StringResourceId(Res.string.campus_closet_baglantiyi_istediginiz_good4_hesabiyla_giris_yapin)
    "CAMPUS_EMAIL_NO_LOCAL_REQUEST" -> UiText.StringResourceId(Res.string.campus_closet_bu_cihazda_bekleyen_dogrulama_istegi_yok_baglantiyi_baska_cihazda)
    "CAMPUS_EMAIL_REQUEST_MISMATCH" -> UiText.StringResourceId(Res.string.campus_closet_bu_baglanti_bu_cihazdaki_son_dogrulama_istegine_ait_degil)
    "CAMPUS_EMAIL_LINK_EXPIRED" -> UiText.StringResourceId(Res.string.campus_closet_baglantinin_suresi_doldu_yeni_dogrulama_baglantisi_isteyin)
    "CAMPUS_EMAIL_PROOF_INVALID", "CAMPUS_EMAIL_SIGN_IN_FAILED" -> UiText.StringResourceId(Res.string.campus_closet_baglanti_dogrulanamadi_en_son_gelen_e_postayi_acin_veya)
    "CAMPUS_EMAIL_SEND_FAILED" -> UiText.StringResourceId(Res.string.campus_closet_e_posta_gonderilemedi_bir_dakika_sonra_tekrar_deneyin)
    "ROLE_NOT_ALLOWED" -> UiText.StringResourceId(Res.string.campus_closet_bu_dogrulama_yalnizca_ogrenci_hesaplari_icin)
    "ACCOUNT_NOT_ACTIVE", "AUTHENTICATION_REQUIRED" -> UiText.StringResourceId(Res.string.campus_closet_oturumunuzu_kontrol_edip_tekrar_giris_yapin)
    else -> UiText.StringResourceId(Res.string.campus_closet_islem_tamamlanamadi_baglantinizi_kontrol_edip_tekrar_deneyin)
}
