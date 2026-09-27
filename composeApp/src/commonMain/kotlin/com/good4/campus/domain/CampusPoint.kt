package com.good4.campus.domain

data class CampusPoint(
    val name: String,
    val category: CampusPointCategory,
    val latitude: Double,
    val longitude: Double
)

enum class CampusPointCategory(val label: String) {
    LIBRARY("Kütüphane"),
    FACULTY("Fakülte"),
    ACADEMIC_UNIT("Akademik Birim"),
    SERVICE_BUILDING("Hizmet Binası"),
    DINING_HALL("Yemekhane"),
    BANK("Banka"),
    ATM("ATM"),
    SHOPPING("Çarşı"),
    MOSQUE("Cami"),
    SPORTS("Spor Alanı"),
    DORMITORY("KYK Yurdu")
}

object CampusPoints {
    val library = CampusPoint(
        name = "Merkez Kütüphane",
        category = CampusPointCategory.LIBRARY,
        latitude = 36.896061,
        longitude = 30.659010
    )

    val faculties = listOf(
        CampusPoint("Diş Hekimliği Fakültesi", CampusPointCategory.FACULTY, 36.89983302196165, 30.65920159814873),
        CampusPoint("Edebiyat Fakültesi", CampusPointCategory.FACULTY, 36.89149338402045, 30.642858482805146),
        CampusPoint("Eğitim Fakültesi", CampusPointCategory.FACULTY, 36.89307542410495, 30.644993598148734),
        CampusPoint("Fen Fakültesi", CampusPointCategory.FACULTY, 36.8991410463328, 30.65554857301538),
        CampusPoint("Güzel Sanatlar Fakültesi", CampusPointCategory.FACULTY, 36.894786514039176, 30.660804813492486),
        CampusPoint("Hemşirelik Fakültesi", CampusPointCategory.FACULTY, 36.8991710250676, 30.657479130686028),
        CampusPoint("Hukuk Fakültesi", CampusPointCategory.FACULTY, 36.893291357841264, 30.65370094417948),
        CampusPoint("İktisadi ve İdari Bilimler Fakültesi", CampusPointCategory.FACULTY, 36.89573363414939, 30.65243331534651),
        CampusPoint("İlahiyat Fakültesi", CampusPointCategory.FACULTY, 36.89043004036507, 30.642356728835892),
        CampusPoint("İletişim Fakültesi", CampusPointCategory.FACULTY, 36.89569659463057, 30.644913371163618),
        CampusPoint("Mimarlık Fakültesi", CampusPointCategory.FACULTY, 36.890572005481616, 30.64516002698462),
        CampusPoint("Mühendislik Fakültesi", CampusPointCategory.FACULTY, 36.89672295109028, 30.649650740476936),
        CampusPoint("Spor Bilimleri Fakültesi", CampusPointCategory.FACULTY, 36.89440595564857, 30.653952586507682),
        CampusPoint("Su Ürünleri Fakültesi", CampusPointCategory.FACULTY, 36.898342608826916, 30.64783103558401),
        CampusPoint("Tıp Fakültesi", CampusPointCategory.FACULTY, 36.898081486530764, 30.65893720023192),
        CampusPoint("Turizm Fakültesi", CampusPointCategory.FACULTY, 36.894677456629985, 30.656442771164105),
        CampusPoint("Uygulamalı Bilimler Fakültesi", CampusPointCategory.FACULTY, 36.89559967511239, 30.64847451349232),
        CampusPoint("Ziraat Fakültesi", CampusPointCategory.FACULTY, 36.89870006660158, 30.64988064232838)
    )

    val academicUnits = listOf(
        CampusPoint("Yabancı Diller Yüksekokulu", CampusPointCategory.ACADEMIC_UNIT, 36.8937548018029, 30.64270886899551),
        CampusPoint("Sağlık Hizmetleri Meslek Yüksekokulu", CampusPointCategory.ACADEMIC_UNIT, 36.891426097156284, 30.659310488193306),
        CampusPoint("Sosyal Bilimler Meslek Yüksekokulu", CampusPointCategory.ACADEMIC_UNIT, 36.90517174015554, 30.679735972114557),
        CampusPoint("Teknik Bilimler Meslek Yüksekokulu", CampusPointCategory.ACADEMIC_UNIT, 36.8972137902786, 30.65233228359916),
        CampusPoint("Enstitüler Binası", CampusPointCategory.ACADEMIC_UNIT, 36.8966958887911, 30.654636559347242)
    )

    val serviceBuildings = listOf(
        CampusPoint("SKS Binası", CampusPointCategory.SERVICE_BUILDING, 36.89610228517866, 30.654907451070887)
    )

    val diningHalls = listOf(
        CampusPoint("Merkezi Yemekhanesi", CampusPointCategory.DINING_HALL, 36.89531987664846, 30.65545530947255),
        CampusPoint("Diş Hekimliği Yemekhanesi", CampusPointCategory.DINING_HALL, 36.89955814867246, 30.658798826662306),
        CampusPoint("İlahiyat Yemekhanesi", CampusPointCategory.DINING_HALL, 36.89011977417819, 30.642257985803823)
    )

    val banks = listOf(
        CampusPoint("Türkiye İş Bankası", CampusPointCategory.BANK, 36.893092414432495, 30.65915042883589),
        CampusPoint("TEB Akdeniz Üniversitesi", CampusPointCategory.BANK, 36.89398472171325, 30.65794879915582)
    )

    val atms = listOf(
        CampusPoint("Garanti BBVA ATM", CampusPointCategory.ATM, 36.89229431619213, 30.66272313106932),
        CampusPoint("Yapı Kredi ATM", CampusPointCategory.ATM, 36.89230349738417, 30.662501274096833),
        CampusPoint("İş Bankası ATM", CampusPointCategory.ATM, 36.89250195464266, 30.66274403701501),
        CampusPoint("TEB ATM", CampusPointCategory.ATM, 36.892491842653534, 30.662835071854666),
        CampusPoint("İş Bankası ATM", CampusPointCategory.ATM, 36.89308962663782, 30.659231823972785),
        CampusPoint("TAM ATM", CampusPointCategory.ATM, 36.894820818846085, 30.654892733357187),
        CampusPoint("TEB ATM", CampusPointCategory.ATM, 36.894878636870445, 30.654671592188937),
        CampusPoint("İş Bankası ATM", CampusPointCategory.ATM, 36.89780577699787, 30.653281555138527),
        CampusPoint("İş Bankası ATM", CampusPointCategory.ATM, 36.89168454257005, 30.645415471350915),
        CampusPoint("Ziraat Bankası ATM", CampusPointCategory.ATM, 36.89168454257005, 30.645415471350915),
        CampusPoint("TEB Bankası ATM", CampusPointCategory.ATM, 36.89168454257005, 30.645415471350915)
    )

    val shoppingAreas = listOf(
        CampusPoint("Olbia Çarşısı", CampusPointCategory.SHOPPING, 36.89378553887194, 30.659606867160758),
        CampusPoint("Yakut Çarşısı", CampusPointCategory.SHOPPING, 36.89809750965234, 30.65314007573253),
        CampusPoint("Ceypark", CampusPointCategory.SHOPPING, 36.89159439653883, 30.641893677063596)
    )

    val mosques = listOf(
        CampusPoint("Akdeniz Üniversitesi Cami", CampusPointCategory.MOSQUE, 36.88903258921914, 30.642778246450522)
    )

    val sportsAreas = listOf(
        CampusPoint("Tenis Kortları", CampusPointCategory.SPORTS, 36.894739399146054, 30.652939651267175),
        CampusPoint("Yüzme Havuzu", CampusPointCategory.SPORTS, 36.89426326077809, 30.651465689481668),
        CampusPoint("Olimpik Yüzme Havuzu", CampusPointCategory.SPORTS, 36.88985501759496, 30.656174984851422),
        CampusPoint("Akdeniz Üniversitesi Stadyumu", CampusPointCategory.SPORTS, 36.8939463659205, 30.646853512329148),
        CampusPoint("Halı Saha", CampusPointCategory.SPORTS, 36.894632223111884, 30.649599303792563),
        CampusPoint("Fitness Salonu", CampusPointCategory.SPORTS, 36.89431948671232, 30.650524441464057),
        CampusPoint("Basketbol Sahası", CampusPointCategory.SPORTS, 36.89696644118668, 30.658222163807267)
    )

    val dormitories = listOf(
        CampusPoint("Bezmialem Kız Yurdu", CampusPointCategory.DORMITORY, 36.89229870153711, 30.658330615717176),
        CampusPoint("Elmalılı Hamdi Yazır KYK Kız Öğrenci Yurdu", CampusPointCategory.DORMITORY, 36.889409781872246, 30.658026256846643),
        CampusPoint("Falez Kız Yurdu", CampusPointCategory.DORMITORY, 36.89044304157243, 30.657368176131797),
        CampusPoint("Muratpaşa Erkek Yurdu", CampusPointCategory.DORMITORY, 36.891186097163406, 30.639048906540946),
        CampusPoint("Şehzade Korkut Erkek Yurdu", CampusPointCategory.DORMITORY, 36.89198975666881, 30.639059056639567),
        CampusPoint("Ahmet Hamdi Akseki Erkek Yurdu", CampusPointCategory.DORMITORY, 36.902998771446555, 30.6608312135052)
    )

    val all = listOf(library) + faculties + academicUnits + serviceBuildings + diningHalls + banks + atms +
        shoppingAreas + mosques + sportsAreas + dormitories
}

/** Case- and diacritic-insensitive Turkish matching, so "muhendislik" finds "Mühendislik". */
private fun String.searchKey(): String = lowercase()
    .replace("i̇", "i").replace('ı', 'i').replace('ş', 's').replace('ğ', 'g')
    .replace('ü', 'u').replace('ö', 'o').replace('ç', 'c').replace('â', 'a').replace('î', 'i')

/** Points whose name or category contains every word of [query]; names starting with it come first. */
fun CampusPoints.search(query: String): List<CampusPoint> {
    val terms = query.searchKey().split(' ').filter(String::isNotBlank)
    if (terms.isEmpty()) return emptyList()
    return all
        .filter { point -> "${point.name} ${point.category.label}".searchKey().let { text -> terms.all { it in text } } }
        .sortedBy { point -> if (point.name.searchKey().startsWith(terms.first())) 0 else 1 }
}
