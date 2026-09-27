package com.good4.schedule.domain

/** Turizm Fakültesinin 2026–2027 güz dönemi resmî haftalık çizelgelerinden aktarılan dersler. */
internal object TourismSchedules {
    private const val TURISM_BUSINESS_TR_SOURCE =
        "https://webis.akdeniz.edu.tr/uploads/1121/announcement/2026-2027%20G%C3%BCz%20D.P/Turizm%20%C4%B0%C5%9Fletmecili%C4%9Fi%20%25%20100%20T%C3%BCrk%C3%A7e%20%C3%96rg%C3%BCn%20%C3%96%C4%9Fretim%202026-2027%20G%C3%BCz%20Yar%C4%B1y%C4%B1l%C4%B1%20Ders%20Program%C4%B1.pdf"
    private const val TOURISM_MANAGEMENT_SOURCE =
        "https://webis.akdeniz.edu.tr/uploads/1121/announcement/2026-2027%20G%C3%BCz%20D.P/Tourism%20Management100%20English%202026-2027%20Fall%20Semester%20Programme.pdf"
    private const val GASTRONOMY_SOURCE =
        "https://webis.akdeniz.edu.tr/uploads/1121/announcement/2026-2027%20G%C3%BCz%20D.P/Gastronomi%20ve%20Mutfak%20Sanatlar%C4%B1%20%C3%96rg%C3%BCn%20%C3%96%C4%9Fretim%202026-2027%20G%C3%BCz%20Yar%C4%B1y%C4%B1l%C4%B1%20Ders%20Program%C4%B1.pdf"
    private const val TOURISM_GUIDANCE_SOURCE =
        "https://webis.akdeniz.edu.tr/uploads/1121/announcement/2026-2027%20G%C3%BCz%20D.P/Turizm%20Rehberli%C4%9Fi%20%C3%96rg%C3%BCn%20%C3%96%C4%9Fretim%202026-2027%20G%C3%BCz%20Yar%C4%B1y%C4%B1l%C4%B1%20Ders%20Program%C4%B1%20%281%29.pdf"
    private const val RECREATION_SOURCE =
        "https://webis.akdeniz.edu.tr/uploads/1121/announcement/2026-2027%20G%C3%BCz%20D.P/Rekreasyon%20Y%C3%B6netimi%20%C3%96rg%C3%BCn%20%C3%96%C4%9Fretim%202026-2027%20G%C3%BCz%20Yar%C4%B1y%C4%B1l%C4%B1%20Ders%20Program%C4%B1.pdf"
    private const val TOURISM_GASTRONOMY_SOURCE =
        "https://webis.akdeniz.edu.tr/uploads/1121/announcement/2026-2027%20G%C3%BCz%20D.P/Turizm%20ve%20Gastronomi%20Y%C3%B6netimi%20Programlar%C4%B1%20%C3%96rg%C3%BCn%20%C3%96%C4%9Fretim%202026-2027%20G%C3%BCz%20Yar%C4%B1y%C4%B1l%C4%B1%20Ders%20Program.pdf"

    private const val ACADEMIC_YEAR = "2026-2027"
    private const val TERM = "Güz Yarıyılı"
    private const val UPDATED_AT = "2026-09-23"

    /** 12:20'de öğle arası vardır; saatlik ders satırları PDF'deki saatlerle bire bir tutulur. */
    private val classPeriods = listOf(
        "08:30" to "09:20",
        "09:30" to "10:20",
        "10:30" to "11:20",
        "11:30" to "12:20",
        "13:30" to "14:20",
        "14:30" to "15:20",
        "15:30" to "16:20",
        "16:30" to "17:20",
        "17:30" to "18:20"
    )

    private data class SourceBlock(
        val day: ScheduleDay,
        val year: Int,
        val start: String,
        val end: String,
        val name: String,
        val instructor: String,
        val classroom: String,
        val section: String
    )

    /** DAY|YEAR|START|END|COURSE|INSTRUCTOR|ROOM|SECTION; "-" means not specified in the PDF. */
    private fun blocks(source: String): List<SourceBlock> = source.lineSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .mapNotNull { line ->
            val fields = line.split('|')
            if (fields.size != 8) return@mapNotNull null
            val year = fields[1].toIntOrNull() ?: return@mapNotNull null
            val start = fields[2]
            val end = fields[3]
            val name = fields[4].trim()
            if (year !in 1..4 || name.isBlank()) return@mapNotNull null
            SourceBlock(
                day = ScheduleDay.valueOf(fields[0]),
                year = year,
                start = start,
                end = end,
                name = name,
                instructor = fields[5].trim().takeUnless { it == "-" }.orEmpty(),
                classroom = fields[6].trim().takeUnless { it == "-" }.orEmpty(),
                section = fields[7].trim().takeUnless { it == "-" }.orEmpty()
            )
        }
        .toList()

    private fun entries(source: String, year: Int): List<ScheduleEntry> = blocks(source)
        .asSequence()
        .filter { it.year == year }
        .flatMap { block ->
            classPeriods.asSequence()
                .filter { (periodStart, periodEnd) -> periodStart >= block.start && periodEnd <= block.end }
                .map { (periodStart, periodEnd) ->
                    val type = when {
                        block.name.contains("laboratory", ignoreCase = true) ||
                            block.name.contains("lab", ignoreCase = true) -> "Laboratuvar"
                        block.name.contains("uygulama", ignoreCase = true) ||
                            block.name.contains("practice", ignoreCase = true) -> "Uygulama"
                        else -> "Ders"
                    }
                    ScheduleEntry(
                        day = block.day,
                        startTime = periodStart,
                        endTime = periodEnd,
                        courseCode = "",
                        courseName = block.name,
                        instructor = block.instructor,
                        classroom = block.classroom,
                        courseType = type,
                        section = block.section.takeUnless(String::isBlank)
                    )
                }
        }
        .toList()

    private fun schedules(
        department: String,
        sourceUrl: String,
        source: String
    ): List<ClassSchedule> = (1..4).map { year ->
        ClassSchedule(
            faculty = ClassSchedules.TOURISM_FACULTY,
            department = department,
            classYear = when (year) {
                1 -> ClassSchedules.FIRST_YEAR
                2 -> ClassSchedules.SECOND_YEAR
                3 -> ClassSchedules.THIRD_YEAR
                else -> ClassSchedules.FOURTH_YEAR
            },
            academicYear = ACADEMIC_YEAR,
            term = TERM,
            updatedAt = UPDATED_AT,
            sourcePage = 1,
            sourceUrl = sourceUrl,
            entries = entries(source, year)
        )
    }

    private val turizmIsletmeciligi = schedules(
        ClassSchedules.TOURISM_BUSINESS_TURKISH_DEPARTMENT,
        TURISM_BUSINESS_TR_SOURCE,
        """
            MONDAY|1|09:30|12:20|Turizm Etiği ve Mevzuatı|Dr.Öğr.Üyesi Abdullah Akgün|Amfi|-
            MONDAY|2|08:30|12:20|İngilizce III (Mesleki İngilizce)|Öğr.Gör. Aylin KART|Z-03|1. Şube
            MONDAY|2|13:30|16:20|Otel İşletmeciliği|Prof.Dr. Zeki AKINCI|204|-
            MONDAY|3|08:30|12:20|Almanca V|Öğr.Gör. Müjgan AKBÜLBÜL ÇELİK|Z-02|-
            MONDAY|3|13:30|15:20|Toplumsal Duyarlılık ve Katkı|Prof.Dr. Yıldırım YILMAZ|208|-
            MONDAY|3|08:30|12:20|İngilizce V (Mesleki İngilizce)|Öğr.Gör. Devrim ARDIÇ|103|1. Şube
            MONDAY|4|13:30|16:20|Girişimcilik|Prof.Dr. Rüya EHTİYAR|101|-
            MONDAY|4|13:30|16:20|Seminer|Prof.Dr. Nedim YÜZBAŞIOĞLU|205|-
            TUESDAY|1|08:30|12:20|Rusça I|Öğr.Gör. Soner OTRAKÇI|Z-02|-
            TUESDAY|1|13:30|17:20|İngilizce I (Mesleki İngilizce)|Öğr.Gör.Aylin KART|204|-
            TUESDAY|2|09:30|12:20|Yönetim ve Organizasyon|Prof.Dr. Ebru İÇİGEN|205|-
            TUESDAY|2|13:30|17:20|Rusça III|Öğr.Gör. Semra BAŞARAN GARİP|Toplantı Salonu|-
            TUESDAY|3|09:30|12:20|Örgütsel Davranış|Prof.Dr. Rüya EHTİYAR|AMFİ|-
            TUESDAY|3|13:30|15:20|Toplumsal Duyarlılık ve Katkı|Prof.Dr. Ebru İÇİGEN|206|-
            TUESDAY|3|15:30|17:20|Toplumsal Duyarlılık ve Katkı|Prof.Dr. Beykan ÇİZEL; Doç.Dr. Ece ÖMÜRİŞ; Dr.Öğr.Üyesi Abdullah AKGÜN; Öğr.Gör. Hasan KINAY|103; 206; Z-01; Lab|-
            TUESDAY|3|13:30|15:20|Toplumsal Duyarlılık ve Katkı|Prof.Dr. Rüya EHTİYAR|Z-01|-
            TUESDAY|4|09:30|12:20|Performans Ölçümü ve Yönetimi|Prof.Dr. Yıldırım YILMAZ|101|-
            TUESDAY|4|13:30|16:20|Yatırım Proje Analizi|Prof.Dr. Yıldırım YILMAZ|205|-
            WEDNESDAY|1|08:30|12:20|Fransızca I|Öğr.Gör. Ümmü Seher GÖKÜŞ|Z-01|-
            WEDNESDAY|1|13:30|16:20|İşletme Bilimine Giriş|Prof.Dr. Tahir ALBAYRAK|204|-
            WEDNESDAY|1|08:30|12:20|Almanca I|Öğr.Gör.Müjgan AKBÜLBÜL ÇELİK|Z-03|-
            WEDNESDAY|2|09:30|12:20|Turist Davranışı|Prof.Dr. Beykan ÇİZEL|204|-
            WEDNESDAY|2|13:30|16:20|Seyahat Acentacılığı ve Tur Operatörlüğü|Prof.Dr. Beykan ÇİZEL|101|-
            WEDNESDAY|3|09:30|12:20|İletişim ve Davranış|Doç.Dr. Ece ÖMÜRİŞ|Toplantı Salonu|-
            WEDNESDAY|3|13:30|16:20|İşletme Finansı|Arş.Gör.Dr. Hatice KARAKAŞ|Amfi|-
            WEDNESDAY|3|09:30|12:20|Etkinlik Yönetimi|Dr.Öğr.Üyesi Onur SELÇUK|104|-
            WEDNESDAY|4|13:30|17:20|İngilizce VII (Mesleki)|Öğr.Gör. Devrim ARDIÇ|Z-01|Şube A
            THURSDAY|1|09:30|12:20|Genel Turizm|Doç.Dr. Ece ÖMÜRİŞ|Z-06|-
            THURSDAY|2|09:30|12:20|Alternatif Turizm|Dr.Öğr.Üyesi Onur SELÇUK|101|-
            THURSDAY|2|13:30|16:20|Araştırma Yöntemleri|Arş.Gör.Dr. Hatice KARAKAŞ|Toplantı Salonu|-
            THURSDAY|3|09:30|12:20|İnsan Kaynakları Yönetimi|Prof.Dr. Akın AKSU|205|-
            THURSDAY|3|13:30|16:20|Yiyecek İçecek Yönetimi|Prof.Dr. Bahattin ÖZDEMİR|101|-
            THURSDAY|3|13:30|16:20|Otel Otomasyonları|Öğr.Gör. Hasan Kınay|Lab 1|-
            THURSDAY|4|08:30|12:20|Rusça VII|Öğr.Gör. Semra BAŞARAN GARİP|207|-
            THURSDAY|4|13:30|16:20|Stratejik Yönetim|Prof.Dr. Beykan ÇİZEL|104|-
            THURSDAY|4|08:30|12:20|İngilizce VII (Mesleki)|Öğr.Gör. Özlem UYANIK AKMAN|206|Şube B
            THURSDAY|4|08:30|12:20|Almanca VII|Öğr.Gör. Müjgan AKBÜLBÜL ÇELİK|103|-
            FRIDAY|2|08:30|12:20|Fransızca III|Öğr.Gör. Ümmü Seher GÖKÜŞ|Z-01|-
            FRIDAY|2|14:30|17:20|Muhasebe|Arş.Gör.Dr. Hatice KARAKAŞ|Amfi|-
            FRIDAY|2|08:30|12:20|İngilizce III (Mesleki İngilizce)|Öğr.Gör. Aylin KART|207|2. Şube
            FRIDAY|2|08:30|12:20|Almanca III|Öğr.Gör. Müjgan AKBBÜLBÜL ÇELİK|Z-03|-
            FRIDAY|3|14:30|18:20|Rusça V|Öğr.Gör. Semra BAŞARAN GARİP|207|-
            FRIDAY|3|14:30|18:20|İngilizce V (Mesleki İngilizce)|Öğr.Gör. Devrim ARDIÇ|206|2. Şube
            FRIDAY|4|09:30|12:20|Çağdaş Yönetim Yaklaşımları|Prof. Dr. Zeki Akıncı|204|-
            FRIDAY|4|14:30|17:20|Pazarlama Yönetimi|Prof.Dt. Tahir ALBAYRAK|Toplantı Salonu|-
        """
    )

    private val tourismManagementEnglish = schedules(
        ClassSchedules.TOURISM_MANAGEMENT_ENGLISH_DEPARTMENT,
        TOURISM_MANAGEMENT_SOURCE,
        """
            MONDAY|1|09:30|12:20|General Tourism|Dr.Öğr.Üyesi Zeynep KARSAVURAN|205|-
            MONDAY|2|13:30|16:20|Tourist Behavior|Dr. Öğr. Üyesi Edina Ajanovic|Z-05|-
            MONDAY|3|08:30|12:20|Almanca V|Öğr.Gör. Müjgan AKBÜLBÜL ÇELİK|Z-02|-
            MONDAY|3|13:30|17:20|English V (Occupatıonal)|Öğr.Gör. Özlem UYANIK AKMAN|Z-01|-
            MONDAY|4|13:30|17:20|English VII (Occupatıonal)|Öğr.Gör. Devrim ARDIÇ|207|-
            TUESDAY|1|08:30|12:20|Russian|Öğr.Gör. Soner OTRAKÇI|Z-02|-
            TUESDAY|1|13:30|16:20|Introduction to Business|Prof. Dr. Tahir Albayrak|207|-
            TUESDAY|2|09:30|12:20|Alternative Tourism|Dr. Öğr. Gör.Onur SELÇUK|Toplantı Salonu|-
            TUESDAY|2|13:30|17:20|Rusça III|Öğr.Gör. Semra BAŞARAN GARİP|Toplantı Salonu|-
            TUESDAY|3|08:30|10:20|Social Responsibility and Social Awareness|Prof.Dr. Tahir ALBAYRAK; Doç. Dr. Yeşim Helhel|206; 207|-
            TUESDAY|3|10:30|12:20|Social Responsibility and Social Awareness|Dr. Öğr. Üyesi Edina Ajanovic|206|-
            TUESDAY|3|13:30|16:20|Human Resource Management|Prof. Dr. Akın Aksu|Z-04|-
            TUESDAY|4|09:30|12:20|Contemporary Management Approaches|Prof.Dr. Akın AKSU|104|-
            WEDNESDAY|1|08:30|12:20|French I|Öğr.Gör. Ümmü Seher GÖKÜŞ|Z-01|-
            WEDNESDAY|1|13:30|16:20|Tourism Ethics and Legislation|Dr.Öğr.Üyesi Abdullah Akgün|Z-04|-
            WEDNESDAY|1|08:30|12:20|German I|Öğr.Gör.Müjgan AKBÜLBÜL ÇELİK|Z-03|-
            WEDNESDAY|2|09:30|12:20|Management and Organization|Dr.Öğr.Üyesi Zeynep KARSAVURAN|Z-05|-
            WEDNESDAY|2|13:30|16:20|Accounting|Doç. Dr. Yeşim Helhel|Z-05|-
            WEDNESDAY|3|09:30|12:20|Communication and Behaviour|Dr. Öğr. Üyesi Edina Ajanovic|207|-
            WEDNESDAY|3|13:30|16:20|Organizational Behavior|Dr. Öğr. Üyesi Edina Ajanovic|207|-
            WEDNESDAY|4|09:30|12:20|Performance Measurement and Management|Prof.Dr. Yıldırım YILMAZ|101|-
            WEDNESDAY|4|13:30|16:20|Investment Project Analysis|Prof. Dr. Yıldırım Yılmaz|Z-06|-
            THURSDAY|2|09:30|12:20|Hotel Management|Dr.Öğr.Üyesi Edina AJANOVİC|Toplantı Salonu|-
            THURSDAY|2|13:30|16:20|Research Methods|Dr.Öğr.Üyesi Abdullah AKGÜN|Z-04|-
            THURSDAY|2|13:30|16:20|Travel Agency and Tour Operating|Dr. Öğr. Üyesi Edina Ajanovic|103|-
            THURSDAY|3|09:30|12:20|Food and Beverage Management|Dr.Öğr.Üyesi Gürkan AYBEK|104|-
            THURSDAY|3|13:30|16:20|Event Management|Prof. Dr. Akın Aksu|Z-02|-
            THURSDAY|3|09:30|12:20|Hotel Operation Systems|Dr.Öğr.Üyesi Abdullah Akgün|Lab 1|-
            THURSDAY|4|08:30|12:20|Rusça VII|Öğr.Gör. Semra BAŞARAN GARİP|207|-
            THURSDAY|4|13:30|16:20|Strategic Management|Prof. Dr. Nedim Yüzbaşıoğlu|206|-
            THURSDAY|4|08:30|12:20|Almanca VII|Öğr.Gör. Müjgan AKBÜLBÜL ÇELİK|103|-
            FRIDAY|2|08:30|12:20|Fransızca III|Öğr.Gör. Ümmü Seher GÖKÜŞ|Z-01|-
            FRIDAY|2|14:30|18:20|English III (Occupatıonal)|Öğr.Gör. Özlem UYANIK AKMAN|204|-
            FRIDAY|2|08:30|12:20|Almanca III|Öğr.Gör. Müjgan AKBBÜLBÜL ÇELİK|Z-03|-
            FRIDAY|3|09:30|12:20|Business Finance|Doç. Dr. Yeşim Helhel|104|-
            FRIDAY|3|14:30|18:20|Rusça V|Öğr.Gör. Semra BAŞARAN GARİP|207|-
            FRIDAY|4|09:30|12:20|Tourism Marketing Management|Prof.Dr. Tahir ALBAYRAK|105|-
            FRIDAY|4|09:30|12:20|Seminar|Prof. Dr. Nedim Yüzbaşıoğlu|Toplantı Salonu|-
        """
    )

    private val gastronomy = schedules(
        ClassSchedules.GASTRONOMY_DEPARTMENT,
        GASTRONOMY_SOURCE,
        """
            MONDAY|1|08:30|12:20|İngilizce I|Öğr.Gör. Özlem UYANIK AKMAN|204|-
            MONDAY|1|13:30|17:20|Almanca I|Öğr.Gör. Melissa Rüya BERKER|Z-04|-
            MONDAY|2|13:30|15:20|Ekonomi|Prof.Dr. Mehmet ZAMBAK|Amfi|-
            MONDAY|3|08:30|10:20|Örgütsel Davranış|Prof.Dr. Rüya EHTİYAR|Z-06|-
            MONDAY|3|10:30|12:20|Toplumsal Duyarlılık ve Katkı|Prof.Dr. Bahattin ÖZDEMİR; Doç.Dr. Adem ARMAN; Doç. Dr. Bahar Gümüş|206; Toplantı Salonu; Z-04|-
            MONDAY|3|13:30|15:20|İnsan Kaynakları Yönetimi|Prof.Dr. Akın AKSU|Z-06|-
            MONDAY|3|15:30|17:20|İşletme Finansı|Arş.Gör.Dr. Hatice KARAKAŞ|Amfi|-
            MONDAY|4|10:30|12:20|Yatırım Proje Analizi|Doç.Dr. Yeşim HELHEL|Z-06|-
            MONDAY|4|13:30|15:20|Yemek Stilistliği|Öğr.Gör.Dr. Eniser ATABAY|104|-
            TUESDAY|1|13:30|15:20|Gastronomiye Giriş|Doç.Dr. Adem ARMAN|AMFİ|-
            TUESDAY|2|10:30|12:20|Bar ve İçkiler|Doç.Dr. Ferhan BALCI TORUN|Z-06|-
            TUESDAY|2|13:30|16:20|Gıda Bilimi ve Güvenliği|Doç.Dr. Bahar GÜMÜŞ|Z-06|-
            TUESDAY|3|10:30|12:20|Gıdalarda Duyusal Analiz|Doç.Dr. Bahar GÜMÜŞ|207|-
            TUESDAY|3|13:30|15:20|Girişimcilik|Doç.Dr. Ferhan BALCI TORUN|104|-
            TUESDAY|3|08:30|12:20|Dünya Mutfakları ve Uygulama|Öğr. Gör. Ümit ÇARBUĞA|Mutfak|1. Şube
            TUESDAY|3|13:30|17:20|Dünya Mutfakları ve Uygulama|Öğr. Gör. Ümit ÇARBUĞA|Mutfak|2. Şube
            TUESDAY|4|10:30|12:20|Stratejik Yönetim|Prof.Dr. Beykan ÇİZEL|204|-
            TUESDAY|4|13:30|17:20|Fransızca III|Öğr.Gör. Ümmü Seher GÖKÜŞ|Z-03|-
            WEDNESDAY|1|10:30|12:20|İşletme Bilimine Giriş|Prof.Dr. Bahattin ÖZDEMİR|205|-
            WEDNESDAY|2|08:30|10:20|İş Hukuku ve Sosyal Güvenlik|Öğr. Gör. Fahri Dutçu|Amfi|-
            WEDNESDAY|2|13:30|17:20|İngilizce III|Öğr.Gör. Özlem UYANIK AKMAN|105|-
            WEDNESDAY|3|08:30|12:20|Temel Yemek Pişirme II|Öğr. Gör. Ümit ÇARBUĞA|Mutfak|1. Şube
            WEDNESDAY|3|13:30|17:20|Temel Yemek Pişirme II|Öğr. Gör. Ümit ÇARBUĞA|Mutfak|2. Şube
            WEDNESDAY|4|10:30|12:20|Gıda Kimyası|Doç.Dr. Ferhan BALCI TORUN|Amfi|-
            WEDNESDAY|4|13:30|15:20|Yiyecek İçecek Yönetimi|Prof.Dr. Bahattin ÖZDEMİR|205|-
            THURSDAY|2|10:30|12:20|Muhasebe|Doç.Dr. Yeşim HELHEL|Amfi|-
            THURSDAY|2|15:30|17:20|Yönetim ve Organizasyon|Dr.Öğr. Üyesi Zeynep KARSAVURAN|Amfi|-
            THURSDAY|3|08:30|12:20|Fransızca I|Öğr.Gör. Ümmü Seher GÖKÜŞ|105|-
            THURSDAY|3|13:30|15:20|Toplumsal Duyarlılık ve Katkı|Doç.Dr. Ferhan BALCI TORUN|207|-
            THURSDAY|3|08:30|12:20|Rusça I|Öğr.Gör. Mehmet Soner OTRAKÇI|Z-05|-
            THURSDAY|4|08:30|12:20|Yaratıcı Yemek Pişirme|Doç.Dr. Adem ARMAN|Mutfak|1. Şube
            THURSDAY|4|13:30|17:20|Yaratıcı Yemek Pişirme|Doç.Dr. Adem ARMAN|Mutfak|2. Şube
            FRIDAY|1|09:30|12:20|Genel Turizm Bilgisi|Dr.Öğr.Üyesi Onur SELÇUK|Z-06|-
            FRIDAY|2|08:30|12:20|Almanca III|Öğr.Gör. Melissa Rüya BERKER|Amfi|-
            FRIDAY|3|08:30|12:20|Temel Yemek Pişirme II|Öğr. Gör. Ümit ÇARBUĞA|Mutfak|3. Şube
            FRIDAY|3|14:30|17:20|Temel Sanat ve Estetik|Öğr.Gör. Emel MÜLAYİM|Z-06|-
            FRIDAY|4|08:30|12:20|Rusça III|Öğr.Gör. Semra BAŞARAN GARİP|206|-
            FRIDAY|4|14:30|16:20|Şarap Bilimi|Dr.Öğr. Üyesi Gürkan AYBEK|Z-05|-
        """
    )

    private val tourismGuidance = schedules(
        ClassSchedules.TOURISM_GUIDANCE_DEPARTMENT,
        TOURISM_GUIDANCE_SOURCE,
        """
            MONDAY|1|09:30|12:20|Rehberliğe Giriş|Prof.Dr. Özlem GÜZEL|Z-05|-
            MONDAY|2|08:30|12:20|Fransızca I|Öğr.Gör. Ümmü Seher GÖKÜŞ|Z-01|-
            MONDAY|2|13:30|15:20|Türkiye Turizm Coğrafyası I|Prof.Dr. Özlem GÜZEL|105|-
            MONDAY|2|15:30|17:20|Anadolu Tarihi ve Sanatı III (Doğu Roma)|Doç.Dr. Havva KESKİN|208|-
            MONDAY|3|10:30|12:20|Eski Yunanca I|Dr.Öğr.Üyesi Nurşah ŞENGÜL|208|-
            MONDAY|3|13:30|15:20|Seyahat İşletmeciliği Paket Programları|Dr. Öğr. Üyesi Abdullah Akgün|Lab 1|-
            MONDAY|3|15:30|17:20|Toplumsal Duyarlılık ve Katkı|Dr.Öğr.Üyesi Nurşah ŞENGÜL|104|-
            MONDAY|3|15:30|17:20|Toplumsal Duyarlılık ve Katkı|Prof.Dr. Özlem GÜZEL|Z-03|-
            MONDAY|4|10:30|12:20|Özel İlgi Turizmi|Arş.Gör.Dr. Aylin GÜVEN HAMURİŞÇİ|104|-
            MONDAY|4|13:30|15:20|Anadolu Epigrafisi I|Dr.Öğr.Üyesi Nurşah ŞENGÜL|Z-03|-
            TUESDAY|1|08:30|12:20|İngilizce I|Öğr.Gör. Aylin KART|Z-05|-
            TUESDAY|1|15:30|17:20|Anadolu Tarihş ve Sanatı I (Tarih Öncesi)|Doç.Dr. Havva KESKİN|Amfi|-
            TUESDAY|2|08:30|12:20|Almanca I|Öğr.Gör. Melissa Rüya BERKER|Z-03|-
            TUESDAY|2|13:30|15:20|Turizm ve Sosyoloji|Prof.Dr. Meltem CEBER|103|-
            TUESDAY|2|15:30|17:20|Türkiye Faunası|Doç.Dr. Mustafa YAVUZ|104|-
            TUESDAY|3|08:30|12:20|Fransızca III|Öğr.Gör. Ümmü Seher GÖKÜŞ|Z-01|-
            TUESDAY|3|13:30|15:20|Dinler Tarihi I|Prof.Dr. Özlem GÜZEL|Z-05|-
            TUESDAY|3|15:30|17:20|Toplumsal Duyarlılık ve Katkı|Prof.Dr. Meltem CEBER|101|-
            TUESDAY|4|08:30|10:20|Bitirme Projesi I|Dr.Öğr.Üyesi Nurşah ŞENGÜL; Prof.Dr. Özlem GÜZEL; Prof.Dr. Meltem CEBER; Doç.Dr. Havva KESKİN; Arş.Gör.Dr. Aylin GÜVEN HAMURİŞÇİ|Dersin Hocası ile İletişim|-
            TUESDAY|4|10:30|12:20|Dünyada ve Türkiyede Antik Kentler I|Dr.Öğr.Üyesi Nurşah ŞENGÜL|208|-
            TUESDAY|4|13:30|15:20|Dünyada ve Türkiyede Antik Kentler I|Dr.Öğr.Üyesi Nurşah ŞENGÜL|208|-
            WEDNESDAY|1|10:30|12:20|Anadolu Tarihş ve Sanatı I (Tarih Öncesi)|Doç.Dr. Havva KESKİN|Z-06|-
            WEDNESDAY|2|10:30|12:20|Sağlık ve İlkyardım|ArşGör.Dr. Damla SEÇKİN|208|-
            WEDNESDAY|2|13:30|17:20|Rusça I|Öğr. Gör. Mehmet Soner OTRAKÇI|Z-03|-
            WEDNESDAY|3|08:30|10:20|Toplumsal Duyarlılık ve Katkı|Doç.Dr. Havva KESKİN|206|-
            WEDNESDAY|3|10:30|12:20|Eski Yunanca I|Dr.Öğr.Üyesi Nurşah ŞENGÜL|105|-
            WEDNESDAY|3|13:30|15:20|Mitoloji I|Dr.Öğr.Üyesi Nurşah ŞENGÜL|104|-
            WEDNESDAY|3|15:30|17:20|Kültürel Miras Yönetimi|Prof.Dr. Meltem CABER|205|-
            WEDNESDAY|4|13:30|15:20|Girişimcilik|Öğr.Gör. Nilüfer CENGİZ|Z-02|-
            WEDNESDAY|4|15:30|17:20|Antikçağ'da Günlük Yaşam (Yunan)|Doç. Dr. Havva Keskin|104|-
            THURSDAY|1|08:30|10:20|Arkeoloji I|Doç.Dr. Havva KESKİN|AMFİ|-
            THURSDAY|2|08:30|12:20|İngilizce III|Öğr.Gör. Devrim ARDIÇ|Z-03|1. Şube
            THURSDAY|2|13:30|17:20|İngilizce III|Öğr.Gör. Devrim ARDIÇ|Z-03|2. Şube
            THURSDAY|3|08:30|10:20|Toplumsal Duyarlılık ve Katkı|Arş.Gör.Dr. Aylin GÜVEN HAMURİŞÇİ|Z-04|-
            THURSDAY|3|10:30|12:20|Tur Planlaması ve Yönetimi|Prof.Dr. Meltem CABER|204|-
            THURSDAY|3|13:30|15:20|Sanat Tarihi I|Arş.Gör.Dr. Soner DEMİR|Z-05|-
            THURSDAY|3|15:30|17:20|Sosyal Bilimlerde Araştırma Yöntemleri|Dr.Öğr.Üyesi Onur SELÇUK|204|-
            THURSDAY|4|13:30|15:20|Destinasyon Yönetimi|Prof. Dr. Meltem CABER|204|-
            FRIDAY|1|09:30|12:20|Genel Turizm|Arş.Gör.Dr. Aylin GÜVEN HAMURİŞÇİ|Z-05|-
            FRIDAY|2|14:30|17:20|Seyahat Acentacılığı/ Tur Operatörlüğü|Arş.Gör.Dr. Aylin GÜVEN HAMURİŞÇİ|208|-
            FRIDAY|3|08:30|12:20|Almanca III|Öğr.Gör. Melissa Rüya Berker|Amfi|-
            FRIDAY|3|14:30|18:20|İngilizce V|Öğr.Gör. Aylin KART|104|-
            FRIDAY|3|08:30|12:20|Rusça III|Öğr.Gör Mehmet Soner OTRAKÇI|101|-
            FRIDAY|4|08:30|12:20|İngilizce VII|Öğr.Gör.Devrim ARDIÇ|103|-
            FRIDAY|4|14:30|18:20|Almanca V|Öğr.Gör. Müjgan AKBÜLBÜL ÇELİK|Z-03|-
            FRIDAY|4|14:30|18:20|Fransızca V|Öğr.Gör. Ümmü Seher GÖKÜŞ|105|-
            FRIDAY|4|14:30|18:20|Rusça V|Öğr.Gör Mehmet Soner OTRAKÇI|Z-02|-
        """
    )

    private val recreation = schedules(
        ClassSchedules.RECREATION_MANAGEMENT_DEPARTMENT,
        RECREATION_SOURCE,
        """
            MONDAY|1|13:30|15:20|Turizm ve Kültür|Doç.Dr. Yakın EKİN|Toplantı Salonu|-
            MONDAY|2|10:30|12:20|Terapötik Rekreasyona Giriş|Doç. Dr. Gülseren YURCU|207|-
            MONDAY|2|13:30|17:20|Yabancı Dil III (İngilizce)|Öğr.Gör. Aylin KART|Z-02|-
            MONDAY|3|13:30|16:20|Wellness&Spa Turizmi|Doç. Dr. Gülseren YURCU|206|-
            MONDAY|4|13:30|17:20|II. Yabancı Dil III (İngilizce)|Öğr.Gör. Aylin KART|Z-02|-
            TUESDAY|1|13:30|16:20|Genel Turizm|Prof. Dr. Yusuf Yılmaz|105|-
            TUESDAY|2|09:30|12:20|Muhasebe|Dr. Öğr. Üyesi Abdullah AKGÜN|Z-04|-
            TUESDAY|2|13:30|15:20|Park ve Açık Alan Rekreasyon Kaynakları|Doç.Dr. Yakın EKİN|101|-
            TUESDAY|3|09:30|12:20|Sahne Makyajı|Doç. Dr. Gülseren YURCU|Atölye|-
            TUESDAY|3|15:30|17:20|İletişim ve İnsan İlişkileri|Doç.Dr. Yakın EKİN|208|-
            TUESDAY|4|08:30|10:20|Rekreatif Alan Tasarımı|Öğr.Gör. Hakan Elinç|Enformatik - LAB 1|-
            TUESDAY|4|10:30|12:20|Yeni Yönetim Teknikleri|Prof. Dr. Yusuf Yılmaz|105|-
            WEDNESDAY|1|09:30|12:20|İşletme Bilimine Giriş|Prof.Dr. Murad Alpaslan KASALAK|Z-04|-
            WEDNESDAY|2|13:30|16:20|Rekreasyon Psikolojisi|Doç. Dr. Gülseren YURCU|208|-
            WEDNESDAY|3|08:30|10:20|Müşteri İlişkileri Yönetimi|Prof. Dr. Yusuf Yılmaz|205|-
            WEDNESDAY|3|10:30|12:20|Sağlık ve İlk Yardım|Arş.Gör.Dr. Arzu TAT ÇATAL|Z-02|-
            WEDNESDAY|3|13:30|15:20|Eğlence Hizmetlerine Giriş|Prof.Dr. Murad Alpaslan KASALAK|Toplantı Salonu|-
            WEDNESDAY|4|10:30|12:20|Kentsel Rekreasyon|Doç.Dr. Yakın EKİN|206|-
            WEDNESDAY|4|13:30|15:20|Rekreasyon ve Spor Turizmi|Doç.Dr. Yakın EKİN|206|-
            WEDNESDAY|4|15:30|17:20|Rekreasyonel Tesis Yönetimi|Prof.Dr. Murad Alpaslan KASALAK|206|-
            THURSDAY|1|08:30|12:20|Yabancı Dil I (Almanca I)|Öğr.Gör. Melissa Rüya BERKER|Z-02|-
            THURSDAY|1|08:30|12:20|Yabancı Dil I (Rusça)|Öğr.Gör. Mehmet Soner OTRAKÇI|Z-05|-
            THURSDAY|2|10:30|12:20|Ekonomi|Prof.Dr. Murad Alpaslan KASALAK|Z-04|-
            THURSDAY|2|13:30|16:20|Yönetim ve Organizasyon|Prof. Dr. Yusuf Yılmaz|105|-
            THURSDAY|3|08:30|12:20|II. Yabancı Dil I (Almanca I)|Öğr.Gör. Melissa Rüya BERKER|Z-02|-
            THURSDAY|3|13:30|16:20|Terapötik Rekreasyon Uygulamaları|Doç. Dr. Gülseren YURCU|Atölye|-
            THURSDAY|3|08:30|12:20|II. Yabancı Dil I (Rusça)|Öğr.Gör. Mehmet Soner OTRAKÇI|Z-05|-
            THURSDAY|4|13:30|16:20|Stratejik Yönetim|Prof.Dr. Murad Alpaslan KASALAK|Z-01|-
            FRIDAY|1|08:30|12:20|Yabancı Dil I (İngilizce)|Öğr.Gör. Özlem UYANIK AKMAN|Z-02|-
            FRIDAY|2|08:30|12:20|Yabancı Dil III (Rusça)|Öğr.Gör. Semra BAŞARAN GARİP|206|-
            FRIDAY|2|14:30|18:20|Yabancı Dil III (Almanca)|Öğr. Gör. Melissa Rüya BERKER|Z-01|-
            FRIDAY|3|08:30|12:20|II. Yabancı Dil I (İngilizce)|Öğr.Gör. Özlem UYANIK AKMAN|Z-02|-
            FRIDAY|4|08:30|12:20|II. Yabancı Dil III (Rusça)|Öğr.Gör. Semra BAŞARAN GARİP|206|-
            FRIDAY|4|14:30|18:20|II.Yabancı Dil III (Almanca)|Öğr. Gör. Melissa Rüya BERKER|Z-01|-
        """
    )

    private val tourismAndGastronomy = schedules(
        ClassSchedules.TOURISM_AND_GASTRONOMY_MANAGEMENT_DEPARTMENT,
        TOURISM_GASTRONOMY_SOURCE,
        """
            MONDAY|1|09:30|12:20|Introduction to Economics|Dr.Öğr.Üyesi Gürkan AYBEK|105|-
            MONDAY|1|13:30|16:20|Tourism and Tourism Management|Dr.Öğr.Üyesi Zeynep KARSAVURAN|103|-
            TUESDAY|1|08:30|12:20|Russian I|Öğr.Gör. Semra BAŞARAN GARİP|103|-
            TUESDAY|1|13:30|16:20|Sustainability|Dr.Öğr.Üyesi Onur SELÇUK|Z-02|-
            WEDNESDAY|1|09:30|12:20|Food Safety and Hygiene|Doç.Dr. Barçın KARAKAŞ BUDAK|103|-
            WEDNESDAY|1|13:30|16:20|Occupational Health and Safety|Dr.Öğr.Üyesi Onur SELÇUK|103|-
            THURSDAY|1|10:30|12:20|Critical Thinking|Dr.Öğr.Üyesi Zeynep KARSAVURAN|208|-
            THURSDAY|1|13:30|16:20|Introduction to Gastronomy|Dr.Öğr.Üyesi Gürkan AYBEK|208|-
            FRIDAY|1|14:30|16:20|Special Interest Tourism|Dr.Öğr.Üyesi Onur SELÇUK|103|-
        """
    )

fun schedulesFor(department: String?): List<ClassSchedule>? = when (department) {
        ClassSchedules.TOURISM_BUSINESS_TURKISH_DEPARTMENT -> turizmIsletmeciligi
        ClassSchedules.TOURISM_MANAGEMENT_ENGLISH_DEPARTMENT -> tourismManagementEnglish
        ClassSchedules.GASTRONOMY_DEPARTMENT -> gastronomy
        ClassSchedules.TOURISM_GUIDANCE_DEPARTMENT -> tourismGuidance
        ClassSchedules.RECREATION_MANAGEMENT_DEPARTMENT -> recreation
        ClassSchedules.TOURISM_AND_GASTRONOMY_MANAGEMENT_DEPARTMENT -> tourismAndGastronomy
        else -> null
    }
}
