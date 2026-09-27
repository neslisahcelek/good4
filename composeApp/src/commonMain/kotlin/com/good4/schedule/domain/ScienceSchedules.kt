package com.good4.schedule.domain

/** 2026–2027 güz dönemi Fen Fakültesi programları. Kaynak tablolardaki saat aralıkları korunur. */
internal object ScienceSchedules {
    private data class Row(val year: Int, val entry: ScheduleEntry)

    private const val ACADEMIC_YEAR = "2026-2027"
    private const val TERM = "Güz Yarıyılı"
    private const val UPDATED_AT = "2026-09-23"

    private const val BIOLOGY_SOURCE =
        "https://webis.akdeniz.edu.tr/uploads/1063/Ders%20program%C4%B1/2026-2027%20G%C3%BCz%20Ders%20Program%C4%B1%20Biyoloji%20B%C3%B6l%C3%BCm%C3%BC.pdf"
    private const val PHYSICS_SOURCE =
        "https://webis.akdeniz.edu.tr/uploads/1063/Ders%20program%C4%B1/2026-2027%20G%C3%BCz%20Ders%20Program%C4%B1%20Fizik%20B%C3%B6l%C3%BCm%C3%BC.pdf"
    private const val CHEMISTRY_SOURCE =
        "https://webis.akdeniz.edu.tr/uploads/1063/Ders%20program%C4%B1/2026-2027%20Kimya%20Bolumu.pdf"
    private const val MATHEMATICS_SOURCE =
        "https://webis.akdeniz.edu.tr/uploads/1063/Ders%20program%C4%B1/2026-2027%20G%C3%BCz%20Ders%20Program%C4%B1%20Matematik%20B%C3%B6l%C3%BCm%C3%BC.pdf"
    private const val SPACE_SOURCE =
        "https://webis.akdeniz.edu.tr/file/getfile?guid=5249853d-1241-45de-bc40-0982384422c1"

    /** DAY|YEAR|START|END|CODE|COURSE|INSTRUCTOR|ROOM|NOTE */
    private fun rows(data: String): List<Row> = data.lineSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .mapNotNull { line ->
            val parts = line.split('|')
            if (parts.size < 8) return@mapNotNull null
            val name = parts[5].trim()
            val room = parts[7].trim()
            val isApplication = name.contains("uygulama", ignoreCase = true)
            val isLab = Regex("""\bLab\b""", RegexOption.IGNORE_CASE).containsMatchIn(name) ||
                Regex("""\bLab\b""", RegexOption.IGNORE_CASE).containsMatchIn(room) ||
                room.contains("laboratuvar", ignoreCase = true) ||
                room.contains("bilgisayar lab", ignoreCase = true)
            Row(
                year = parts[1].toInt(),
                entry = ScheduleEntry(
                    day = ScheduleDay.valueOf(parts[0]),
                    startTime = parts[2],
                    endTime = parts[3],
                    courseCode = parts[4].trim(),
                    courseName = name,
                    instructor = parts[6].trim(),
                    classroom = room,
                    courseType = when {
                        isApplication -> "Uygulama"
                        isLab -> "Laboratuvar"
                        else -> "Ders"
                    },
                    note = parts.getOrNull(8)?.trim()?.takeIf(String::isNotEmpty)
                )
            )
        }
        .toList()

    private fun schedules(
        department: String,
        source: String,
        data: String,
        pageForYear: (Int) -> Int = { 1 }
    ): List<ClassSchedule> {
        val rows = rows(data)
        val years = listOf(
            ClassSchedules.FIRST_YEAR,
            ClassSchedules.SECOND_YEAR,
            ClassSchedules.THIRD_YEAR,
            ClassSchedules.FOURTH_YEAR
        )
        return years.mapIndexed { index, classYear ->
            val year = index + 1
            ClassSchedule(
                faculty = ClassSchedules.SCIENCE_FACULTY,
                department = department,
                classYear = classYear,
                academicYear = ACADEMIC_YEAR,
                term = TERM,
                updatedAt = UPDATED_AT,
                sourcePage = pageForYear(year),
                sourceUrl = source,
                entries = rows.filter { it.year == year }.map(Row::entry)
            )
        }
    }

    private val biology = schedules(
        ClassSchedules.BIOLOGY_DEPARTMENT,
        BIOLOGY_SOURCE,
        """
MONDAY|1|08:30|10:20|ATA 101|Atatürk İlkeleri ve İnkılap Tarihi I|Doç. Dr. Mustafa MALHUT|Amfi B|
MONDAY|1|10:30|12:20|TDB 101|Türk Dili I|Öğr. Gör. Dr. Mehmet KÖYYAR|Amfi|
MONDAY|1|13:30|15:20|YBD 101|İngilizce I|Öğr. Gör. Demet TEKİNAY|Amfi B|
MONDAY|2|08:30|11:20|BİY 203|Temel Biyoistatistik|Prof. Dr. Nuray KAYA / Dr. Öğr. Üyesi T. YILDIRIM|B Blok D-5|
MONDAY|2|13:30|16:20|BİY 217|Omurgasızlar Biyolojisi|Prof. Dr. B. ÇIPLAK|D-3|
MONDAY|3|08:30|11:20|BİY 305|Biyokimya I|Prof. Dr. E. AYDEMİR|D-3|
MONDAY|3|13:30|15:20|BİY 375|Biyokimya I Lab. 1.Grup|Prof. Dr. E. AYDEMİR|Lab-2|
MONDAY|3|15:30|17:20|BİY 375|Biyokimya I Lab. 2.Grup|Prof. Dr. E. AYDEMİR|Lab-2|
MONDAY|4|09:30|11:20|BİY 465|Tıbbi Bitkiler|Prof. Dr. A. AKSOY|D-4|
MONDAY|4|09:30|11:20|BİY 411|Drosophila Genetiği|Prof. Dr. B. KAYA|D-8|
MONDAY|4|13:30|15:20|BİY 413|Biyoteknoloji|Prof. Dr. B. KAYA|D-8|
MONDAY|4|13:30|15:20|BİY 403|Çevre Kimyası|Prof. Dr. H. ÇETİN|D-4|
MONDAY|4|15:30|17:20|BİY 419|Biyolojik Araştırma Veri Analiz Yönt.|Prof. Dr. N. KAYA|B-Blok Bilgisayar Lab.|
MONDAY|4|15:30|17:20|BİY 441|Mesleki Yabancı Dil III|Dr. Öğr. Üyesi T. YILDIRIM|D-4|
TUESDAY|1|08:30|10:20|BİY 123|Biyolojide Güncel Konular|Doç. Dr. M. YAVUZ|Amfi-B|
TUESDAY|1|10:30|12:20|KİM 161|Genel Kimya I|Prof. Dr. Esin AKARSU|Amfi-B|
TUESDAY|1|13:30|15:20|BİY 101|Genel Biyoloji I|Prof. Dr. B. KAYA / Doç. Dr. N. KAYMAK|Amfi-B|
TUESDAY|2|08:30|10:20|BİY 259|Tohumsuz Bitkiler Biyolojisi Lab. 1.Grup|Prof. Dr. R. S. GÖKTÜRK|Lab-1|
TUESDAY|2|10:30|12:20|BİY 259|Tohumsuz Bitkiler Biyolojisi Lab. 2.Grup|Prof. Dr. H. AKGÜL|Lab-1|
TUESDAY|2|15:30|17:20|BİY 253|Temel Biyoistatistik Uygulama|Prof. Dr. N. KAYA / Dr. Öğr. Üyesi T. YILDIRIM|Amfi B|
TUESDAY|3|09:30|12:20|BİY 301|Bitki Fizyolojisi|Prof. Dr. A. AKSOY / Araş. Gör. Dr. Uğurcan BARAN|D-4|
TUESDAY|3|13:30|15:20|BİY 371|Bitki Fizyolojisi Lab. 1.Grup|Prof. Dr. A. AKSOY|Lab-2|
TUESDAY|3|15:30|17:20|BİY 371|Bitki Fizyolojisi Lab. 2.Grup|Araş. Gör. Dr. Uğurcan BARAN|Lab-2|
TUESDAY|4|08:30|10:20|BİY 429|Enzimoloji|Prof. Dr. E. AYDEMİR|D-8|
TUESDAY|4|08:30|10:20|BİY 437|İnsan Anatomisi|Arş. Gör. Dr. M. YILDIRIM|D-3|
TUESDAY|4|10:30|12:20|BİY 415|Bitki Genetiği|Prof. Dr. N. KAYA|D-8|
TUESDAY|4|10:30|12:20|BİY 437|İnsan Anatomisi Uygulama|Arş. Gör. Dr. M. YILDIRIM|D-3|
TUESDAY|4|13:30|15:20|BİY 463|Biyolojik Mücadele|Prof. Dr. H. ÇETİN|D-3|
TUESDAY|4|13:30|15:20|BİY 475|Epigenetik|Prof. Dr. E. AYDEMİR|D-4|
TUESDAY|4|15:30|17:20|BİY 477|Gen Mühendisliği|Prof. Dr. A. ÖZKAN|D-8|
TUESDAY|4|15:30|17:20|BİY 467|Makrofunguslar|Prof. Dr. H. AKGÜL|D-4|
WEDNESDAY|1|08:30|10:20|BİY 109|Genel Biyoloji Lab. 2.Grup|Prof. Dr. N. KAYA / Dr. Öğr. Üyesi E.B.T. ÖZTÜRK|Lab-1|
WEDNESDAY|1|10:30|12:20|BİY 109|Genel Biyoloji Lab. 1.Grup|Prof. Dr. B. KAYA / Doç. Dr. N. KAYMAK|Lab-1|
WEDNESDAY|1|13:30|15:20|BİY 101|Genel Biyoloji I|Prof. Dr. N. KAYA / Dr. Öğr. Üyesi E.B.T. ÖZTÜRK|Amfi-B|
WEDNESDAY|2|09:30|12:20|KİM 261|Organik Kimya|Prof. Dr. E. BAYRAM|Amfi-B|
WEDNESDAY|2|13:30|16:20|BİY 219|Tohumsuz Bitkiler Biyolojisi|Prof. Dr. R.S. GÖKTÜRK / Prof. Dr. H. AKGÜL|D-8|
WEDNESDAY|3|08:30|10:20|BİY 381|Veri Bilimi için R Kodlama|Arş. Gör. Dr. O. ULUAR|Lab-2|
WEDNESDAY|3|08:30|10:20|BİY 307|Türkiye’nin Biyolojik Zenginlikleri|Prof. Dr. R.S. GÖKTÜRK|D-3|
WEDNESDAY|3|10:30|12:20|BİY 377|Palinolojiye Giriş|Prof. Dr. C. AYKURT|D-3|
WEDNESDAY|3|10:30|12:20|BİY 345|Hayvan Davranışları|Prof. Dr. A. ERDOĞAN|D-4|
WEDNESDAY|3|13:30|15:20|BİY 333|Parazitoloji|Prof. Dr. B. ÇIPLAK|D-4|
WEDNESDAY|3|15:30|17:20|BİY 333|Parazitoloji Uygulama|Prof. Dr. B. ÇIPLAK|Lab-1|
WEDNESDAY|3|15:30|17:20|BİY 313|Mikrobiyal Ekoloji|Dr. Öğr. Üyesi B.E.T. ÖZTÜRK|D-4|
WEDNESDAY|4|10:30|12:20|BİY 473|Ekoloji Lab. 1.Grup|Prof. Dr. H. ÇETİN|Lab-2|
WEDNESDAY|4|13:30|15:20|BİY 473|Ekoloji Lab. 2.Grup|Doç. Dr. O. ÜNAL|Lab-2|
WEDNESDAY|4|15:30|17:20|BİY 471|Ekoloji|Prof. Dr. H. ÇETİN / Doç. Dr. O. ÜNAL|Amfi-B|
THURSDAY|1|08:30|10:20|FİZ 165|Genel Fizik|Arş. Gör. Dr. Kübra BAYRAK|Amfi-B|
THURSDAY|2|08:30|10:20|BİY 251|Hücre Biyolojisi Lab. 1.Grup|Prof. Dr. E. AYDEMİR|Lab-1|
THURSDAY|2|10:30|12:20|BİY 251|Hücre Biyolojisi Lab. 2.Grup|Prof. Dr. E. AYDEMİR|Lab-1|
THURSDAY|2|13:30|16:20|BİY 201|Hücre Biyolojisi|Prof. Dr. E. AYDEMİR|Amfi-B|
THURSDAY|3|09:30|12:20|BİY 303|Temel Genetik|Prof. Dr. B. KAYA / Prof. Dr. N. KAYA|D-3|
THURSDAY|3|13:30|15:20|BİY 353|Temel Genetik Lab. 1.Grup|Prof. Dr. B. KAYA / Prof. Dr. N. KAYA|Lab-1|
THURSDAY|3|15:30|17:20|BİY 353|Temel Genetik Lab. 2.Grup|Prof. Dr. N. KAYA|Lab-1|
THURSDAY|4|08:30|10:20|BİY 425|Ekonomik Botanik|Doç. Dr. O. ÜNAL|D-8|
THURSDAY|4|08:30|10:20|BİY 497|Adli Biyokimya|Öğr. Gör. Dr. A. M. ÖRME|D-4|
THURSDAY|4|10:30|12:20|BİY 445|Embriyoloji|Prof. Dr. A. ÖZKAN|D-8|
THURSDAY|4|13:30|15:20|BİY 493|Bakteriyoloji|Dr. Öğr. Üyesi B.E.T. ÖZTÜRK|D-3|
THURSDAY|4|13:30|15:20|BİY 469|Deniz Biyolojisi|Doç. Dr. N. KAYMAK|D-4|
THURSDAY|4|15:30|17:20|BİY 491|Girişimcilik|Öğr. Gör. M. YAYKAŞLI|D-4|Sosyal Bilimler MYO
THURSDAY|4|15:30|17:20|BİY 495|İklim Değişimi Biyolojisi|Arş. Gör. Dr. O. ULUAR|D-3|
FRIDAY|1|10:30|12:20|FİZ 185|Genel Fizik Lab.|Arş. Gör. Dr. Kübra BAYRAK|Fizik Lab 1|
FRIDAY|1|13:30|17:20|BİY 119|Mesleki Etik ve Laboratuvar Güvenliği|Dr. Öğr. Üyesi T. YILDIRIM|Amfi-B|
FRIDAY|2|08:30|10:20|BİY 257|Omurgasızlar Biyolojisi Lab. 1.Grup|Araş. Gör. Dr. O. ULUAR|Lab-1|
FRIDAY|2|10:30|12:20|BİY 257|Omurgasızlar Biyolojisi Lab. 2.Grup|Prof. Dr. B. ÇIPLAK|Lab-1|
FRIDAY|3|10:30|12:20|BİY 341|Mesleki Yabancı Dil I|Dr. Öğr. Üyesi T. YILDIRIM|D-4|
FRIDAY|4|08:30|10:20|BİY 449|Bitirme Çalışması I||Öğretim Üyesi Odası|
FRIDAY|4|10:30|12:20|BİY 457|Hayvan Hücre Kültürü|Prof. Dr. A. ÖZKAN|D-8|
        """.trimIndent()
    )

    private val physics = schedules(
        ClassSchedules.PHYSICS_DEPARTMENT,
        PHYSICS_SOURCE,
        """
MONDAY|1|08:30|10:20||Atatürk İlkeleri ve İnkılap Tarihi I|Doç. Dr. Mustafa MALHUT|Amfi B (Örgün)|
MONDAY|1|10:30|12:20||Türk Dili I|Öğr. Gör. Dr. Mehmet KÖYYAR|Amfi A (Örgün)|
MONDAY|1|13:30|15:20||İngilizce I|Öğr. Gör. Demet TEKİNAY|Amfi B (Örgün)|
MONDAY|2|08:30|12:20|FİZ 217|Optik Lab.|Öğr. Gör. Dr. Mustafa DERNEK|Fizik Lab. 1|
MONDAY|2|13:30|15:20|FİZ 227|Fizikçiler için Matematik I|Doç. Dr. Yusuf KÜÇÜKAKÇA|D5|
MONDAY|3|08:30|10:20|FİZ 327|Fizikte Özel Fonksiyonlar|Prof. Dr. Orhan BAYRAK|D5|
MONDAY|3|13:30|15:20|FİZ 319|Kuantum Fiziği|Prof. Dr. Mesut KARAKOÇ|D6|
MONDAY|3|15:30|17:20|FİZ 323|Elektromanyetik Teori I|Prof. Dr. Yusuf SUCU|D6|
MONDAY|4|08:30|10:20|FİZ 409|Bitirme Çalışması I|||
MONDAY|4|10:30|12:20|FİZ 413|Atom ve Molekül Fiziği|Prof. Dr. Orhan BAYRAK|D6|
MONDAY|4|13:30|16:20|FİZ 483|Fizikte Ölçme Teknikleri I|Prof. Dr. Nina TUNÇEL|D7|
TUESDAY|1|08:30|10:20|FİZ 107|Fizik I (Mekanik)|Doç. Dr. Yusuf KÜÇÜKAKÇA|D6|
TUESDAY|1|10:30|12:20|KİM 163|Genel Kimya I|Doç. Dr. Ömer KESMEZ|D7|
TUESDAY|1|13:30|15:20|MAT 161|Genel Matematik I|Doç. Dr. Levent KARGIN|D5|
TUESDAY|1|15:30|16:20|KPD 101|Kariyer Planlama Dersi|Doç. Dr. Yusuf KÜÇÜKAKÇA|Uzaktan|
TUESDAY|2|10:30|12:20|FİZ 213|Optik|Prof. Dr. Melike B. YÜCEL|D5|
TUESDAY|2|13:30|17:20|FİZ 219|Temel Elektronik Lab.|Prof. Dr. İsmail BOZTOSUN|Fizik Lab. 2|
TUESDAY|3|08:30|12:20|FİZ 339|İleri Fizik Lab.|Öğr. Gör. Dr. Mustafa DERNEK|Fizik Lab. 2|
TUESDAY|4|09:30|12:20|FİZ 463|Parçacık Fiziği I|Prof. Dr. Yusuf SUCU|Seminer Salonu|
TUESDAY|4|13:30|15:20|FİZ 411|Katıhal Fiziği|Prof. Dr. Rıza ERDEM|D6|
TUESDAY|4|15:30|17:20|FİZ 415|Nükleer Fizik I|Prof. Dr. İsmail H. SARPUN|D7|
WEDNESDAY|1|08:30|10:20|MAT 161|Genel Matematik I|Doç. Dr. Levent KARGIN|D7|
WEDNESDAY|1|10:30|12:20|KİM 163|Genel Kimya I|Doç. Dr. Ömer KESMEZ|D7|
WEDNESDAY|1|15:30|17:20|FİZ 107|Fizik I (Mekanik)|Doç. Dr. Yusuf KÜÇÜKAKÇA|D6|
WEDNESDAY|2|08:30|10:20|FİZ 225|Fizikte Bilgisayar Uygulamaları|Prof. Dr. Mesut KARAKOÇ|B Blok Bilgisayar Lab.|
WEDNESDAY|2|10:30|12:20|FİZ 213|Optik|Prof. Dr. Melike B. YÜCEL|D5|
WEDNESDAY|2|13:30|15:20|FİZ 227|Fizikçiler için Matematik I|Doç. Dr. Yusuf KÜÇÜKAKÇA|D7|
WEDNESDAY|3|13:30|15:20|FİZ 319|Kuantum Fiziği|Prof. Dr. Mesut KARAKOÇ|D6|
WEDNESDAY|3|15:30|17:20|FİZ 323|Elektromanyetik Teori I|Prof. Dr. Yusuf SUCU|D7|
WEDNESDAY|4|08:30|10:20|FİZ 413|Atom ve Molekül Fiziği|Prof. Dr. Orhan BAYRAK|D6|
WEDNESDAY|4|10:30|12:20|FİZ 415|Nükleer Fizik I|Prof. Dr. İsmail H. SARPUN|D6|
WEDNESDAY|4|13:30|16:20|FİZ 489|Nanobilim|Prof. Dr. Melike B. YÜCEL|D5|
THURSDAY|1|09:30|12:20|FİZ 111|Fizik Lab. 1|Öğr. Gör. Dr. Mustafa DERNEK|Fizik Lab. 1|
THURSDAY|2|10:30|12:20|FİZ 227|Fizikçiler için Matematik I|Doç. Dr. Yusuf KÜÇÜKAKÇA|D5|
THURSDAY|2|13:30|15:20|FİZ 215|Temel Elektronik|Prof. Dr. İsmail BOZTOSUN|D6|
THURSDAY|2|15:30|17:20|FİZ 215|Temel Elektronik|Prof. Dr. İsmail BOZTOSUN|D6|
THURSDAY|3|10:30|12:20|FİZ 319|Kuantum Fiziği|Prof. Dr. Mesut KARAKOÇ|D6|
THURSDAY|3|14:30|17:20|FİZ 341|Termodinamik|Prof. Dr. Rıza ERDEM|D7|
THURSDAY|4|08:30|10:20|FİZ 411|Katıhal Fiziği|Prof. Dr. Rıza ERDEM|D6|
THURSDAY|4|10:30|12:20|FİZ 465|Teorik Mekanik II|Prof. Dr. Yusuf SUCU|D7|
THURSDAY|4|13:30|14:20|FİZ 465|Teorik Mekanik II|Prof. Dr. Yusuf SUCU|D7|
THURSDAY|4|14:30|17:20|FİZ 467|Görelilik Kuramı I|Doç. Dr. Yusuf KÜÇÜKAKÇA|Seminer Salonu|
FRIDAY|1|08:30|10:20|MAT 161|Genel Matematik I|Doç. Dr. Levent KARGIN|D6|
FRIDAY|1|10:30|12:20|FİZ 107|Fizik I (Mekanik)|Doç. Dr. Yusuf KÜÇÜKAKÇA|D6|
FRIDAY|2|14:30|16:20|FİZ 225|Fizikte Bilgisayar Uygulamaları|Prof. Dr. Mesut KARAKOÇ|B Blok Bilgisayar Lab.|
FRIDAY|3|08:30|10:20|FİZ 327|Fizikte Özel Fonksiyonlar|Prof. Dr. Orhan BAYRAK|D5|
FRIDAY|3|10:30|12:20|FİZ 323|Elektromanyetik Teori I|Prof. Dr. Yusuf SUCU|D5|
FRIDAY|4|08:30|12:20|FİZ 485|Mesleki İngilizce I|Prof. Dr. Yasemin KÜÇÜK|D7|
FRIDAY|4|14:30|17:20|FİZ 469|Radyasyon Fiziğine Giriş|Prof. Dr. Nina TUNÇEL|D7|
        """.trimIndent()
    )

    private val chemistry = schedules(
        ClassSchedules.CHEMISTRY_DEPARTMENT,
        CHEMISTRY_SOURCE,
        """
MONDAY|1|08:30|10:20||Atatürk İlkeleri ve İnkılap Tarihi I|Doç. Dr. Mustafa MALHUT|Amfi B|
MONDAY|1|10:30|12:20||Türk Dili I|Öğr. Gör. Mehmet KÖYYAR|Amfi A|
MONDAY|1|13:30|15:20||İngilizce I|Öğr. Gör. Demet TEKİNAY|Amfi B|
MONDAY|2|08:30|10:20||Organik Kimya I|Prof. Dr. G. Turgut Cin|D-9|
MONDAY|2|10:30|12:20||Anorganik Kimya I|Prof. Dr. M. Akarsu|D-9|
MONDAY|2|13:30|16:20||Kimya için Teknik İngilizce I|Prof. Dr. M. Akarsu|D-9|
MONDAY|3|08:30|10:20||Gıda Kimyası|Prof. Dr. S. Tunç|D-10|
MONDAY|3|10:30|12:20||Organik Kimya III|Prof. Dr. G. Turgut Cin|D-10|
MONDAY|3|13:30|17:20||Gıda Kimyası Lab.|Prof. Dr. S. Tunç|Lab 243-244|
MONDAY|4|08:30|09:20||Bitirme Çalışması I|||
MONDAY|4|09:30|12:20||Adli Kimya I|Prof. Dr. O. Duman|Seminer Salonu|
MONDAY|4|13:30|15:20||Fiziksel Kimya III|Prof. Dr. P. Çamurlu|D-10|
MONDAY|4|15:30|17:20||Biyokimya I|Doç. Dr. S. Aksu|D-10|
TUESDAY|1|10:30|12:20||Genel Kimya I|Prof. Dr. Ö. Topel|D-9|
TUESDAY|1|15:30|17:20||Analiz I|Prof. Dr. M. Demirci|B-Blok D-6|
TUESDAY|2|08:30|10:20||Organik Kimya I|Prof. Dr. G. Turgut Cin|D-10|
TUESDAY|2|13:30|16:20||Nanoteknolojiye Giriş|Prof. Dr. E. Akarsu|Seminer Salonu|
TUESDAY|3|08:30|10:20||Fiziksel Kimya I|Prof. Dr. Ö. Topel|D-9|
TUESDAY|3|10:30|12:20||Organik Kimya III|Prof. Dr. G. Turgut Cin|D-10|
TUESDAY|3|13:30|17:20||Organik Kimya Lab. I|Prof. Dr. G. Turgut Cin|Lab 253|
TUESDAY|4|08:30|12:20||Fiziksel Kimya Lab. II|Prof. Dr. P. Çamurlu|Lab 243-244|
TUESDAY|4|13:30|16:20||Kimyacılar İçin İstatistik|Prof. Dr. S. Tunç|D-10|
TUESDAY|4|13:30|16:20||Girişimcilik|Öğr. Gör. Metehan Yaykaşlı|D-9|
TUESDAY|4|16:30|17:20||Bitirme Çalışması I|||
WEDNESDAY|1|13:30|15:20||Fizik I|Öğr. Gör. Dr. M. Dernek|B-Blok D-6|
WEDNESDAY|1|15:30|17:20||Analiz I|Prof. Dr. M. Demirci|B-Blok D-6|
WEDNESDAY|2|08:30|10:20||Analitik Kimya I|Prof. Dr. O. Duman|D-10|
WEDNESDAY|2|10:30|12:20||Kalitatif Analiz Lab.|Prof. Dr. O. Duman|Lab 243-244|
WEDNESDAY|2|13:30|17:20||Kalitatif Analiz Lab.|Prof. Dr. O. Duman|Lab 243-244|
WEDNESDAY|3|13:30|16:20||Polimer Kimyasına Giriş|Prof. Dr. P. Çamurlu|D-10|
WEDNESDAY|4|08:30|10:20||Fiziksel Kimya III|Prof. Dr. P. Çamurlu|D-9|
WEDNESDAY|4|10:30|12:20||Biyokimya I|Doç. Dr. S. Aksu|D-9|
WEDNESDAY|4|13:30|16:20||Boya ve Kaplama Teknolojileri|Prof. Dr. M. Akarsu|D-9|
WEDNESDAY|4|16:30|17:20||Bitirme Çalışması I|||
THURSDAY|1|08:30|10:20||Genel Kimya I|Prof. Dr. Ö. Topel|D-9|
THURSDAY|1|13:30|17:20||Genel Kimya Lab. I|Prof. Dr. Ö. Topel|Lab 243-244|
THURSDAY|2|08:30|10:20||Analitik Kimya I|Prof. Dr. O. Duman|D-10|
THURSDAY|2|10:30|12:20||Anorganik Kimya I|Prof. Dr. M. Akarsu|D-10|
THURSDAY|2|13:30|16:20||Hücre Biyokimyası|Dr. Öğr. Üyesi İ. Birsen|B Blok D-6|
THURSDAY|2|13:30|16:20||Çevre Kimyası|Prof. Dr. E. Bayram|Seminer Salonu|
THURSDAY|3|10:30|12:20||Fiziksel Kimya I|Prof. Dr. Ö. Topel|D-9|
THURSDAY|3|13:30|16:20||Anorganik Kimya III|Dr. Öğr. Üyesi N. Kiraz|D-9|
THURSDAY|4|08:30|12:20||Endüstriyel Anorganik Prosesler Lab.|Doç. Dr. Ö. Kesmez|Lab 243-244|
THURSDAY|4|13:30|17:20||Endüstriyel Anorganik Prosesler|Doç. Dr. Ö. Kesmez|D-10|
THURSDAY|4|16:30|17:20||Bitirme Çalışması I|||
FRIDAY|1|10:30|12:20||Fizik I|Öğr. Gör. Dr. M. Dernek|D-9|
FRIDAY|1|13:30|15:20||Fizik I (Fizik Bölümü Lab)|Öğr. Gör. Dr. M. Dernek|Fizik Bölümü Laboratuvarı|
FRIDAY|3|08:30|12:20||Anorganik Kimya Lab.|Dr. Öğr. Üyesi N. Kiraz|Lab 243-244|
FRIDAY|4|08:30|09:20||Bitirme Çalışması I|||
FRIDAY|4|09:30|11:20||Staj Çalışması|Prof. Dr. E. Bayram|Seminer Salonu|
FRIDAY|4|11:30|12:20||Bitirme Çalışması I|||
FRIDAY|4|13:30|17:20||Biyokimya Lab.|Doç. Dr. S. Aksu|Lab 244|
        """.trimIndent()
    )

    private val mathematics = schedules(
        ClassSchedules.MATHEMATICS_DEPARTMENT,
        MATHEMATICS_SOURCE,
        """
MONDAY|1|08:30|10:20|ATA 101|Atatürk İlkeleri ve İnkılap Tarihi I|Koray ERGİN|Amfi A|
MONDAY|1|10:30|12:20|MAT 109|Analitik Geometri I|Ayşe Yılmaz CEYLAN|Amfi A|
MONDAY|1|13:30|15:20|YBD 101|İngilizce I|Aslı TAŞER|Amfi A|
FRIDAY|1|08:30|10:20|FİZ 163|Fizik I|Yusuf KÜÇÜKAKÇA|D7|Ders kapatılmıştır; eski müfredat kapsamında yalnızca alttan alan öğrenciler içindir.
TUESDAY|1|10:30|12:20|TDB 101|Türk Dili I|Mehmet KÖYYAR|Amfi A|
TUESDAY|1|13:30|15:20|MAT 111|Analiz I|M. Cihat DAĞLI|Amfi A|
TUESDAY|1|15:30|17:20|MAT 109|Analitik Geometri I|Ayşe Yılmaz CEYLAN|Amfi A|
WEDNESDAY|1|10:30|12:20|MAT 103|Matematiğin Temelleri I|Damla GÜN|D10|
WEDNESDAY|1|13:30|15:20|MAT 111|Analiz I|M. Cihat DAĞLI|Amfi A|
THURSDAY|1|13:30|15:20|MAT 103|Matematiğin Temelleri I|Damla GÜN|Amfi A|
THURSDAY|1|15:30|17:20|MAT 111|Analiz I|M. Cihat DAĞLI|Amfi A|
FRIDAY|1|08:30|12:20|MAT 117|Bilgisayar Programlama I|Ahmet SINAK|Bilgisayar Lab. + D12|
MONDAY|2|10:30|12:20|MAT 225|İleri Analiz I|Simten BAYRAKÇI|D8|
MONDAY|2|13:30|15:20|MAT 219|Diferansiyel Denklemler I|Özkan ÖCAL|D10|
MONDAY|2|15:30|17:20|MAT 223|Olasılık|Füsun YALÇIN|Amfi A|
TUESDAY|2|13:30|15:20|MAT 225|İleri Analiz I|Simten BAYRAKÇI|D8|
WEDNESDAY|2|08:30|10:20|MAT 225|İleri Analiz I|Simten BAYRAKÇI|D9|
WEDNESDAY|2|10:30|12:20|MAT 223|Olasılık|Füsun YALÇIN|Amfi A|
WEDNESDAY|2|15:30|17:20|MAT 219|Diferansiyel Denklemler I|Özkan ÖCAL|D09|
THURSDAY|2|10:30|12:20|MAT 217|Bilgisayar Programlama I|Ahmet SINAK|Bilgisayar Lab. + D12|
THURSDAY|2|13:30|17:20|MAT 221|Doğrusal Cebir I|Ortaç ÖNEŞ|D10|
MONDAY|3|10:30|12:20|MAT 307|Karmaşık Analiz I|Mümün CAN|D11|
MONDAY|3|13:30|15:20|MAT 311|Diferansiyel Geometri I|Mustafa ÖZDEMİR|D7|
MONDAY|3|15:30|17:20|TDP 301|Toplumsal Duyarlılık ve Katkı Projeleri I|Damla GÜN / Çağla SEKİN / Elif ŞÜKRÜOĞLU / Mehmet CİCİMEN / Nihal GÜMÜŞBAŞ ÖZTÜRK / Murat KARAÇAYIR|D7|
TUESDAY|3|08:30|10:20|MAT 315|Topoloji I|Mustafa DEMİRCİ|D8|
TUESDAY|3|13:30|15:20|MAT 313|Sayısal Analiz I|Gültekin SOYLU|D7|
WEDNESDAY|3|10:30|12:20|MAT 311|Diferansiyel Geometri I|Mustafa ÖZDEMİR|D7|
WEDNESDAY|3|13:30|15:20|MAT 313|Sayısal Analiz I|Gültekin SOYLU|D11|
WEDNESDAY|3|15:30|17:20|MAT 309|Cebir I|Ortaç ÖNEŞ|D11|
THURSDAY|3|08:30|10:20|MAT 315|Topoloji I|Mustafa DEMİRCİ|D9|
THURSDAY|3|10:30|12:20|MAT 309|Cebir I|Ortaç ÖNEŞ|D11|
FRIDAY|3|10:30|12:20|MAT 307|Karmaşık Analiz I|Mümün CAN|D7|
MONDAY|4|08:30|10:20|MAT 403|Lineer Cebirin Uygulamaları I|Mustafa ALKAN|D10|
MONDAY|4|08:30|10:20|MAT 411|Reel Analiz|Simten BAYRAKÇI|D8|
MONDAY|4|10:30|12:20|MAT 469|Kriptografiye Giriş|Ahmet SINAK|D10|
MONDAY|4|13:30|15:20|MAT 439|Eliptik Fonksiyonlar Teorisine Giriş|Mehmet CENKCI|D11|
MONDAY|4|15:30|17:20|MAT 457|Mesleki Yabancı Dil|Ayşe Yılmaz CEYLAN|D10|
MONDAY|4|15:30|17:20|MAT 415|Uygulamalı Matematik I|Özkan ÖCAL|D8|
TUESDAY|4|08:30|10:20|MAT 403|Lineer Cebirin Uygulamaları I|Mustafa ALKAN|D10|
TUESDAY|4|08:30|10:20|MAT 411|Reel Analiz|Simten BAYRAKÇI|D8|
TUESDAY|4|10:30|12:20|MAT 449|Fonksiyonel Analiz I|Yılmaz ŞİMŞEK|D10|
TUESDAY|4|13:30|17:20|MAT 465|SPSS Uygulamalı İstatistiksel Analiz I|Füsun YALÇIN|Bilgisayar Lab.|Laboratuvar uygulaması
TUESDAY|4|13:30|15:20|MAT 457|Mesleki Yabancı Dil|Ayşe Yılmaz CEYLAN|D10|
TUESDAY|4|15:30|17:20|MAT 415|Uygulamalı Matematik I|Özkan ÖCAL|D8|
WEDNESDAY|4|08:30|10:20|MAT 467|Sayısal Optimizasyon|Murat KARAÇAYIR|D7|
WEDNESDAY|4|08:30|12:20|MAT 431|Halka Teorisine Giriş|Ortaç ÖNEŞ|D11|
WEDNESDAY|4|13:30|15:20|MAT 443|Fourier Analizi|Melih ERYİĞİT|D9|
WEDNESDAY|4|13:30|15:20|MAT 421|Ayrık Matematik|Levent KARGIN|D8|
WEDNESDAY|4|15:30|17:20|MAT 449|Fonksiyonel Analiz I|Yılmaz ŞİMŞEK|D10|
THURSDAY|4|08:30|10:20|MAT 467|Sayısal Optimizasyon|Murat KARAÇAYIR|Bilgisayar Lab.|Laboratuvar uygulaması
THURSDAY|4|13:30|15:20|MAT 463|Analizde Seçme Konular I|Levent KARGIN|D11|
THURSDAY|4|13:30|15:20|MAT 445|Sayılar Teorisi|Nihal GÜMÜŞBAŞ ÖZTÜRK|D8|
THURSDAY|4|15:30|17:20|MAT 469|Kriptografiye Giriş|Ahmet SINAK|Bilgisayar Lab.|Laboratuvar uygulaması
FRIDAY|4|08:30|10:20|MAT 439|Eliptik Fonksiyonlar Teorisine Giriş|Mehmet CENKCI|D11|
FRIDAY|4|10:30|12:20|MAT 463|Analizde Seçme Konular I|Levent KARGIN|D11|
FRIDAY|4|10:30|12:20|MAT 445|Sayılar Teorisi|Nihal GÜMÜŞBAŞ ÖZTÜRK|D8|
FRIDAY|4|13:30|15:20|MAT 443|Fourier Analizi|Melih ERYİĞİT|D7|
FRIDAY|4|13:30|15:20|MAT 421|Ayrık Matematik|Levent KARGIN|D8|
FRIDAY|4|15:30|17:20|MAT 413|Bitirme Çalışması I|Öğretim Üyeleri|D10|
        """.trimIndent(),
        pageForYear = { it }
    )

    private val spaceSciences = schedules(
        ClassSchedules.SPACE_SCIENCES_DEPARTMENT,
        SPACE_SOURCE,
        """
MONDAY|1|08:30|10:20||Atatürk İlkeleri ve İnkılap Tarihi I|Öğr. Gör. Dr. Koray ERGİN|Amfi A|
MONDAY|1|10:30|12:20|UBT 101|Uzay Bilimleri ve Teknolojilerine Giriş|Prof. Dr. Volkan BAKIŞ|D2|
MONDAY|1|13:30|15:20||İngilizce I|Öğr. Gör. Aslı TAŞER|Amfi A|
MONDAY|1|15:30|17:20|MAT 171|Genel Matematik I|Doç. Dr. Melih ERYİĞİT|D1|
MONDAY|2|09:30|12:20|UBT 205|Hava Fotoğrafları ve Görsel Yorumlama|Prof. Dr. Namık Kemal SÖNMEZ|D1|
MONDAY|2|13:30|17:20|FİZ 217|Optik Laboratuvarı|Prof. Dr. Melike B. YÜCEL|Fizik Laboratuvarı I|
MONDAY|3|09:30|12:20|UBT 319|Yörünge Dinamiği|Dr. Öğr. Üyesi Burçin DÖNMEZ|D3|
MONDAY|3|13:30|15:20|UBT 331|Foton Algılayıcıları I|Dr. Öğr. Üyesi Burçin DÖNMEZ|D2|
MONDAY|3|15:30|17:20|FİZ 323|Elektromanyetik Teori I|Prof. Dr. Yusuf SUCU|A Blok D6|
MONDAY|4|09:30|12:20|UBT 401|Tayfbilim|Prof. Dr. Timur ŞAHİN|Bilgisayar Laboratuvarı|
TUESDAY|1|08:30|10:20|FİZ 107|Fizik I (Mekanik)|Doç. Dr. Yusuf KÜÇÜKAKÇA|A Blok D6|
TUESDAY|1|10:30|12:20|TDB 101|Türk Dili I|Öğr. Gör. Dr. Mehmet KÖYYAR|Amfi A|
TUESDAY|1|13:30|15:20|MAT 171|Genel Matematik I|Doç. Dr. Melih ERYİĞİT|D1|
TUESDAY|1|15:30|17:20|MAT 109|Analitik Geometri I|Prof. Dr. Mustafa ÖZDEMİR|D2|
TUESDAY|2|08:30|10:20|UBT 211|Yörünge Mekaniği|Doç. Dr. Murat KAPLAN|D4|
TUESDAY|2|10:30|12:20|FİZ 213|Optik|Prof. Dr. Melike B. YÜCEL|A Blok D6|
TUESDAY|2|13:30|17:20|UBT 207|Bilgisayar Destekli Tasarım|Prof. Dr. Serdar SELİM|Bilgisayar Laboratuvarı|
TUESDAY|3|09:30|10:20|UBT 315|Değişen Yıldızlar|Prof. Dr. Hicran BAKIŞ|D2|
TUESDAY|3|10:30|12:20|UBT 305|Yakın Uzay ve Parçacıklar|Prof. Dr. Ali KILÇIK|D3|
TUESDAY|3|13:30|15:20|UBT 303|Astrofizik I|Prof. Dr. Hicran BAKIŞ|D2|
TUESDAY|3|15:30|17:20|UBT 331|Foton Algılayıcıları I|Dr. Öğr. Üyesi Burçin DÖNMEZ|D4|
TUESDAY|4|09:30|12:20|UBT 417|LASER/LIDAR|Doç. Dr. Nusret DEMİR|D1|
TUESDAY|4|13:30|16:20|UBT 425|Güneş Fiziği|Prof. Dr. Ali KILÇIK|D3|
WEDNESDAY|1|09:30|12:20|FİZ 175|Fizik I Lab.|Doç. Dr. Yusuf KÜÇÜKAKÇA|Fizik Laboratuvarı I|
WEDNESDAY|1|13:30|15:20|MAT 109|Analitik Geometri I|Prof. Dr. Mustafa ÖZDEMİR|D2|
WEDNESDAY|1|15:30|17:20|FİZ 107|Fizik I (Mekanik)|Doç. Dr. Yusuf KÜÇÜKAKÇA|A Blok D6|
WEDNESDAY|2|10:30|12:20|FİZ 213|Optik|Prof. Dr. Melike B. YÜCEL|A Blok D5|
WEDNESDAY|2|13:30|15:20|MAT 265|Sayısal Analiz I|Arş. Gör. Dr. Murat KARAÇAYIR|D4|
WEDNESDAY|2|15:30|17:20|MAT 263|Diferansiyel Denklemler|Doç. Dr. Melih ERYİĞİT|D4|
WEDNESDAY|3|09:30|10:20|UBT 323|Astrometri|Doç. Dr. Murat KAPLAN|D2|
WEDNESDAY|3|10:30|12:20|UBT 323|Astrometri|Doç. Dr. Murat KAPLAN|Bilgisayar Laboratuvarı|Laboratuvar uygulaması
WEDNESDAY|3|10:30|12:20|UBT 327|Digital Terrain Modelling|Doç. Dr. Nusret DEMİR|D1|
WEDNESDAY|3|13:30|15:20|UBT 303|Astrofizik I|Prof. Dr. Hicran BAKIŞ|D3|
WEDNESDAY|3|15:30|17:20|FİZ 323|Elektromanyetik Teori I|Prof. Dr. Yusuf SUCU|A Blok D7|
WEDNESDAY|4|09:30|12:20|UBT 413|Uzay İtki Sistemleri|Dr. Öğr. Üyesi Burçin DÖNMEZ|D4|
WEDNESDAY|4|13:30|16:20|UBT 405|Dijital Görüntü İşleme Teknikleri|Doç. Dr. Nusret DEMİR|D1|
THURSDAY|1|10:30|12:20|KİM 167|Genel Kimya|Dr. Öğr. Üyesi İlknur BİRSEN|D1|
THURSDAY|1|13:30|15:20|MAT 171|Genel Matematik I|Doç. Dr. Melih ERYİĞİT|D1|
THURSDAY|2|10:30|12:20|MAT 265|Sayısal Analiz I|Arş. Gör. Dr. Murat KARAÇAYIR|D4|
THURSDAY|2|13:30|15:20|UBT 213|Bilimsel Araştırma için İngilizce I|Prof. Dr. Timur ŞAHİN|D2|
THURSDAY|2|08:30|10:20|UBT 211|Yörünge Mekaniği|Doç. Dr. Murat KAPLAN|D4|
THURSDAY|3|09:30|12:20|UBT 301|Coğrafi Bilgi Sistemlerine Giriş|Araş. Gör. Dr. Nagihan ASLAN|D3|
THURSDAY|3|14:30|17:20|FİZ 341|Termodinamik|Prof. Dr. Rıza ERDEM|A Blok D7|
THURSDAY|4|10:30|12:20|UBT 419|Uzay Fiziğinde Seçilmiş Konular|Arş. Gör. Dr. Efecan TUNÇ|D2|
THURSDAY|4|13:30|16:20|UBT 423|İnsansız Hava Araçları ve Uygulamaları|Arş. Gör. Dr. Nagihan ASLAN|D3|
FRIDAY|1|10:30|12:20|FİZ 107|Fizik I (Mekanik)|Doç. Dr. Yusuf KÜÇÜKAKÇA|A Blok D6|
FRIDAY|2|10:30|12:20|MAT 263|Diferansiyel Denklemler|Doç. Dr. Melih ERYİĞİT|D4|
FRIDAY|3|10:30|12:20|FİZ 323|Elektromanyetik Teori I|Prof. Dr. Yusuf SUCU|A Blok D5|
FRIDAY|4|09:30|12:20|UBT 403|Optik Tasarım ve Kaplamalar|Dr. Öğr. Üyesi Burçin DÖNMEZ|D3|
FRIDAY|4|14:30|16:20|UBT 407|Bitirme Çalışması|Tüm Öğretim Üyeleri||
        """.trimIndent()
    )

    fun schedulesFor(department: String?): List<ClassSchedule>? = when (department) {
        ClassSchedules.BIOLOGY_DEPARTMENT -> biology
        ClassSchedules.PHYSICS_DEPARTMENT -> physics
        ClassSchedules.CHEMISTRY_DEPARTMENT -> chemistry
        ClassSchedules.MATHEMATICS_DEPARTMENT -> mathematics
        ClassSchedules.SPACE_SCIENCES_DEPARTMENT -> spaceSciences
        else -> null
    }
}
