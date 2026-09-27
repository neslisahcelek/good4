package com.good4.schedule.domain

/** Ziraat Fakültesi 2026–2027 güz dönemi ders programları. */
internal object AgricultureSchedules {
    private const val SOURCE_URL =
        "https://webis.akdeniz.edu.tr/file/getfile?guid=678164fd-3127-4d70-b89a-83e6448fe738"
    private const val ACADEMIC_YEAR = "2026-2027"
    private const val TERM = "Güz Yarıyılı"
    private const val UPDATED_AT = "2026-09-23"

    /** PDF'deki her 50 dakikalık ders saati. */
    private val classPeriods = listOf(
        "08:30" to "09:20",
        "09:30" to "10:20",
        "10:30" to "11:20",
        "11:30" to "12:20",
        "12:30" to "13:20",
        "13:30" to "14:20",
        "14:30" to "15:20",
        "15:30" to "16:20",
        "16:30" to "17:20"
    )

    private data class Row(val year: Int, val entry: ScheduleEntry)
    private data class Dataset(val department: String, val firstPage: Int, val rows: List<Row>)

    private val instructorPattern = Regex(
        """(?:Prof\.?\s*Dr\.?|Doç\.?\s*Dr\.?|Dr\.?\s*Öğr\.?\s*Üyesi|Öğr\.?\s*Gör\.?(?:\s*Dr\.?)?|Arş\.?\s*Gör\.?(?:\s*Dr\.?)?)""",
        RegexOption.IGNORE_CASE
    )
    private val departmentStaffPattern = Regex(
        """Bölüm\s+öğretim\s+üyeleri(?!\s+tarafından)""",
        RegexOption.IGNORE_CASE
    )
    private val parenthesesPattern = Regex("""\(([^()]*)\)""")
    private val sectionPattern = Regex("""\b\d{1,2}-[A-Z]\b""", RegexOption.IGNORE_CASE)
    private val roomPattern = Regex(
        """(?i)(amfi|lab|laborat|laboratory|sal\.?|salon|tasarım|tasarim|derslik|bil\.|bilgisayar|kon\.|top\.|blok|yonca|portakal|başak|mikroskop)"""
    )
    private val applicationLocationPattern = Regex(
        """(?i)(uygulama yeri|uygulamaların yerleri|uygulamaların yeri)"""
    )

    /** DAY|YEAR|START|END|RAW-CELL-TEXT */
    private fun rows(data: String): List<Row> = data.lineSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .mapNotNull { line ->
            val parts = line.split('|', limit = 5)
            if (parts.size != 5) return@mapNotNull null
            val sourceText = parts[4].split('^').joinToString(" ")
                .replace(Regex("""\s+"""), " ")
                .trim()
            // Instructor-like text can also be part of a classroom name (e.g. "Doç. Dr. Hakan FİDAN LAB.").
            // The actual instructor is the last title/name occurrence in these cells.
            val instructorMatch = instructorPattern.findAll(sourceText).lastOrNull()
            val staffMatch = if (instructorMatch == null) departmentStaffPattern.find(sourceText) else null
            val teacherStart = instructorMatch?.range?.first ?: staffMatch?.range?.first
            var coursePrefix = teacherStart?.let { sourceText.substring(0, it) } ?: sourceText
            if (staffMatch != null) coursePrefix = coursePrefix.trimEnd().removeSuffix("(").trimEnd()
            val instructor = when {
                instructorMatch != null -> sourceText.substring(instructorMatch.range.first)
                    .substringBefore("(").trim()
                staffMatch != null -> staffMatch.value
                else -> ""
            }

            var section: String? = sectionPattern.find(sourceText)?.value?.uppercase()
            var classroom = ""
            val notes = mutableListOf<String>()
            if (sourceText.contains("Seçmeli Ders", ignoreCase = true)) notes += "Seçmeli Ders"
            parenthesesPattern.findAll(sourceText).forEach { match ->
                val inside = match.groupValues[1].trim()
                val group = sectionPattern.find(inside)?.value
                if (group != null) section = group.uppercase()
                val withoutGroup = if (group == null) inside else {
                    inside.replace(group, "").trim().trim('/', '-', ' ')
                }
                when {
                    inside.contains("Seçmeli Ders", ignoreCase = true) -> Unit
                    applicationLocationPattern.containsMatchIn(inside) -> {
                        if (classroom.isBlank()) classroom = inside else notes += inside
                    }
                    group != null && withoutGroup.isBlank() -> Unit
                    group != null && roomPattern.containsMatchIn(withoutGroup) -> {
                        if (classroom.isBlank()) classroom = withoutGroup else notes += withoutGroup
                    }
                    group != null -> Unit
                    departmentStaffPattern.containsMatchIn(inside) -> Unit
                    roomPattern.containsMatchIn(inside) -> {
                        if (classroom.isBlank()) classroom = inside else notes += inside
                    }
                    else -> notes += inside
                }
            }
            val courseName = coursePrefix
                .replace(parenthesesPattern, " ")
                .replace(Regex("""\s+"""), " ")
                .trim()
                .trim('-', '–', ':')
            if (courseName.isBlank()) return@mapNotNull null
            val upperName = courseName.uppercase()
            val courseType = when {
                upperName.contains("UYG") || upperName.contains("UGY") -> "Uygulama"
                upperName.contains("LAB") || upperName.contains("LABORATORY") -> "Laboratuvar"
                else -> "Ders"
            }
            Row(
                year = parts[1].toIntOrNull() ?: return@mapNotNull null,
                entry = ScheduleEntry(
                    day = parseDay(parts[0]) ?: return@mapNotNull null,
                    startTime = parts[2],
                    endTime = parts[3],
                    courseCode = "",
                    courseName = courseName,
                    instructor = instructor.replace(Regex("""\s+"""), " ").trim(),
                    classroom = classroom.trim().trimEnd('/').trim(),
                    courseType = courseType,
                    note = notes.distinct().joinToString(" · ").ifBlank { null },
                    section = section
                )
            )
        }
        .toList()

    // The source rows use the Turkish day names printed in the faculty PDF.
    private val turkishDays = mapOf(
        "PAZARTESİ" to ScheduleDay.MONDAY,
        "SALI" to ScheduleDay.TUESDAY,
        "ÇARŞAMBA" to ScheduleDay.WEDNESDAY,
        "PERŞEMBE" to ScheduleDay.THURSDAY,
        "CUMA" to ScheduleDay.FRIDAY
    )

    private fun parseDay(value: String): ScheduleDay? =
        turkishDays[value.trim()] ?: ScheduleDay.entries.firstOrNull { it.name == value.trim() }

    private fun expand(entry: ScheduleEntry): List<ScheduleEntry> =
        classPeriods
            .filter { (start, end) -> start >= entry.startTime && end <= entry.endTime }
            .map { (start, end) -> entry.copy(startTime = start, endTime = end) }

    private val datasets = listOf(
        Dataset(ClassSchedules.HORTICULTURE_DEPARTMENT, 2, rows("""
PAZARTESİ|1|08:30|10:20|MATEMATİK-I^(3-B)^Arş. Gör. Dr. Mehmet^CİCİMEN
ÇARŞAMBA|1|08:30|10:20|TEKNİK RESİM^(Tasarım-II)^Prof. Dr. Can ERTEKİN
CUMA|1|08:30|10:20|KİMYA^(Başak Amfi)^Dr. Öğr. Üyesi Kudret^AKPINAR
SALI|1|10:30|12:20|FİZİK-I^(3-B)^Öğr. Gör. Dr. Fatih PERİNÇEK
ÇARŞAMBA|1|10:30|12:20|TEKNİK RESİM^UYG. (Tasarım-II)^Prof. Dr. Can ERTEKİN
PERŞEMBE|1|10:30|12:20|ATATÜRK İLKELERİ VE^İNKILAP TARİHİ-I^(3-B)^Doç. Dr. Ahmet KISA
CUMA|1|10:30|11:20|ÜNİVERSİTE YAŞAMINA^GEÇİŞ^(1-A)^Doç. Dr. Tuğçe ÖZSAN KILIÇ
CUMA|1|11:30|12:20|KARİYER PLANLAMA^(1-A)^Doç. Dr. Tuğçe ÖZSAN KILIÇ
SALI|1|13:30|15:20|BOTANİK^(2-B)^Prof. Dr. Ramazan Süleyman^GÖKTÜRK
CUMA|1|14:30|16:20|TÜRK DİLİ-I^(Başak Amfi)^Öğr. Gör. Sinan ORUÇOĞLU
SALI|1|15:30|17:20|BOTANİK^UYG (Mikroskop Lab.)^Prof. Dr. Ramazan Süleyman^GÖKTÜRK
ÇARŞAMBA|1|15:30|17:20|METEOROLOJİ^(4-F)^Prof. Dr. Harun KAMAN
PERŞEMBE|1|15:30|17:20|İNGİLİZCE-I^(3-B)^Öğr. Gör. Seda AKSUNGUR
PAZARTESİ|2|08:30|10:20|FİDE VE FİDANCILIK^TEKNİĞİ^(1-A)^Prof. Dr. Ahmet Naci ONUS
SALI|2|08:30|10:20|MANTAR^YETİŞTİRİCİLİĞİ^(4-E)^Prof. Dr. Ersin POLAT
ÇARŞAMBA|2|08:30|10:20|BAHÇE BİTKİLERİ^MEKANİZASYONU^UYG.^Prof. Dr. Murad ÇANAKCI^(Uygulama yeri dersin öğretim^üyesinden öğrenilecektir)^(Seçmeli Ders)
PERŞEMBE|2|08:30|10:20|GIDA TEKNOLOJİSİ VE GIDA^GÜVENLİĞİ^(4-B)^Öğr. Gör. Dr. Emrah EROĞLU^(Seçmeli Ders)
CUMA|2|08:30|10:20|BİYOKİMYA^(1-A)^Dr. Öğr. Üyesi İlhami TOZLU^(Seçmeli Ders)
PAZARTESİ|2|10:30|12:20|BAHÇE BİTKİLERİ^MEKANİZASYONU^(4-F)^Prof. Dr. Murad ÇANAKCI^(Seçmeli Ders)
SALI|2|10:30|12:20|TARIM EKONOMİSİ^(4-E)^Prof. Dr. İbrahim YILMAZ
ÇARŞAMBA|2|10:30|12:20|MANTAR^YETİŞTİRİCİLİĞİ^UYG.(2-A)^Prof. Dr. Ersin POLAT
PERŞEMBE|2|10:30|12:20|GIDA TEKNOLOJİSİ VE GIDA^GÜVENLİĞİ^UYG.(4-B)^Öğr. Gör. Dr. Emrah EROĞLU^(Seçmeli Ders)
CUMA|2|10:30|12:20|İSTATİSTİK^(4-B)^Prof. Dr. Mehmet Ziya FIRAT^(Seçmeli Ders)
PAZARTESİ|2|13:30|15:20|TARIMSAL YAPILAR^(3-B)^Doç. Dr. Nefise Yasemin^TEZCAN^(Seçmeli Ders)
SALI|2|13:30|15:20|BİTKİ FİZYOLOJİSİ^(4-A)^Dr. Öğr. Üyesi İlhami^TOZLU
ÇARŞAMBA|2|13:30|15:20|GENEL MEYVECİLİK^(1-A)^Prof. Dr. Hamide GÜBBÜK
PERŞEMBE|2|13:30|15:20|İÇ DIŞ MEKAN SÜS^BİTKİLERİ YETİŞTİRİCİLİĞİ^UYG. (2-D)^Prof. Dr. Songül SEVER MUTLU^(Seçmeli Ders)
PAZARTESİ|2|15:30|17:20|ENTOMOLOJİ^(Başak Amfi)^Arş. Gör. Dr. Hilal Şule^TOSUN
SALI|2|15:30|17:20|İÇ DIŞ MEKAN SÜS^BİTKİLERİ^YETİŞTİRİCİLİĞİ^(4-A)^Prof. Dr. Songül SEVER^MUTLU^(Seçmeli Ders)
ÇARŞAMBA|2|15:30|17:20|TARIMSAL KİRLİLİK VE^ÇEVRE^(1-A)^Doç. Dr. Sevinç ATEŞ^(Seçmeli Ders)
PERŞEMBE|2|15:30|17:20|GENEL BAĞCILIK^(3-A)^Prof. Dr. İlknur POLAT
SALI|3|08:30|10:20|SERT KABUKLU^MEYVELER^(1-A)^Prof. Dr. Şadiye GÖZLEKÇİ
ÇARŞAMBA|3|08:30|10:20|ÖRTÜALTI SEBZE^YETİŞTİRİCİLİĞİ^UYG.^Prof. Dr. Ersin POLAT^(Uygulama yeri dersin öğretim^üyesinden öğrenilecektir)
PERŞEMBE|3|08:30|10:20|BAHÇE BİTKİLERİ^HASTALIKLARI^UYG. (Mikroskop Lab.)^Prof. Dr. Hüseyin BASIM
CUMA|3|08:30|10:20|SUBTROPİK MEYVELER^(2-D)^Prof. Dr. Hamide GÜBBÜK
PAZARTESİ|3|10:30|12:20|ÖRTÜALTI SEBZE^YETİŞTİRİCİLİĞİ^(Başak Amfi)^Prof. Dr. Ersin POLAT
SALI|3|10:30|12:20|BAHÇE BİTKİLERİ ISLAHI^(1-A)^Prof. Dr. Ahmet Naci ONUS
ÇARŞAMBA|3|10:30|12:20|BAHÇE BİTKİLERİNDE^SEKTÖREL İLİŞKİLER^(Tevfik Aksoy Kon. Sal.)^Prof. Dr. Mustafa ERKAN^(Seçmeli Ders)
CUMA|3|10:30|12:20|SUBTROPİK MEYVELER^UYG.^(2-D)^Prof. Dr. Hamide GÜBBÜK
PERŞEMBE|3|13:30|15:20|BİTKİSEL ÜRETİMDE^ORGANİK TARIM^(3-B)^Prof. Dr. Halil DEMİR^(Seçmeli Ders)
CUMA|3|13:30|15:20|BAHÇE BİTKİLERİ^UYGULAMALARI-I^(Uygulamaların yerleri bölüm^öğretim üyeleri tarafından^belirlenecektir)
ÇARŞAMBA|3|13:30|14:20|SERT KABUKLU^MEYVELER^UYG. (2-D)^Prof. Dr. Şadiye GÖZLEKÇİ
ÇARŞAMBA|3|14:30|17:20|BİTKİ HASTALIKLARI VE^ZARARLILARI İLE^MÜCADELE YÖNTEMLERİ^(2-B)^Prof. Dr. Fedai ERLER^(Seçmeli Ders)
SALI|3|15:30|17:20|BAHÇE BİTKİLERİ^HASTALIKLARI^(2-B)^Prof. Dr. Hüseyin BASIM
PERŞEMBE|3|15:30|17:20|BİTKİ BÜYÜME^DÜZENLEYİCİLERİ^(1-C)^Prof. Dr. Songül SEVER^MUTLU^(Seçmeli Ders)
CUMA|3|15:30|17:20|BİTKİSEL ÜRETİM^UYGULAMALARI-I^(Uygulamaların yerleri bölüm^öğretim üyeleri tarafından^belirlenecektir)
PAZARTESİ|4|08:30|10:20|TURUNÇGİLLER^(4-F)^Dr. Öğr. Üyesi İlhami TOZLU
SALI|4|08:30|10:20|BAHÇE ÜRÜNLERİNİN^MUHAFAZASI VE PAZARA^HAZIRLANMASI^(2-A)^Prof. Dr. Mustafa ERKAN
ÇARŞAMBA|4|08:30|10:20|BAHÇE BİTKİLERİ^ZARARLILARI^(2-B)^Prof. Dr. Fedai ERLER
PERŞEMBE|4|08:30|10:20|SERİN İKLİM SEBZELERİ^(1-A)^Prof. Dr. Halil DEMİR
CUMA|4|08:30|10:20|ENDÜSTRİYEL BAHÇE^ÜRÜNLERİ^(1-C)^Doç. Dr. Adem DOĞAN^(Seçmeli Ders)
SALI|4|10:30|12:20|BAHÇE ÜRÜNLERİNİN^MUHAFAZASI VE PAZARA^HAZIRLANMASI^UYG.^Prof. Dr. Mustafa ERKAN
ÇARŞAMBA|4|10:30|11:20|BAHÇE BİTKİLERİ^ZARARLILARI^UYG. (2-B)^Prof. Dr. Fedai ERLER
PERŞEMBE|4|10:30|12:20|SERİN İKLİM SEBZELERİ^UYG.(1-A)^Prof. Dr. Halil DEMİR
CUMA|4|10:30|12:20|MEYVE ISLAHI^(1-C)^Dr. Öğr. Üyesi İlhami TOZLU^(Seçmeli Ders)
PAZARTESİ|4|12:30|13:20|BİTİRME ÇALIŞMASI-II^(Bölüm öğretim üyeleri)
SALI|4|12:30|13:20|BİTİRME ÇALIŞMASI-II^(Bölüm öğretim üyeleri)
PAZARTESİ|4|13:30|15:20|SEBZE ISLAHI^(3-A)^Prof. Dr. Ahmet Naci ONUS^(Seçmeli Ders)
SALI|4|13:30|15:20|SERT VE YUMUŞAK^ÇEKİRDEKLİ MEYVELER^(1-B)^Prof. Dr. Şadiye GÖZLEKCİ
PERŞEMBE|4|13:30|15:20|GÜBRELER VE^GÜBRELEME^(Başak Amfi)^Prof. Dr. Mustafa KAPLAN^(Seçmeli Ders)
PAZARTESİ|4|15:30|17:20|TURUNÇGİLLER^UYG.^(3-A)^Dr. Öğr. Üyesi İlhami^TOZLU
ÇARŞAMBA|4|15:30|17:20|SERT VE YUMUŞAK^ÇEKİRDEKLİ MEYVELER^UYG. (Yonca Amfi)^Prof. Dr. Şadiye^GÖZLEKÇİ
PERŞEMBE|4|15:30|17:20|GÜBRELER VE^GÜBRELEME^UYG. (Başak Amfi)^Prof. Dr. Mustafa KAPLAN^(Seçmeli Ders)
        """.trimIndent())),
        Dataset(ClassSchedules.PLANT_PROTECTION_DEPARTMENT, 6, rows("""
PAZARTESİ|1|08:30|10:20|MATEMATİK-I^(3-B)^Arş. Gör. Dr. Mehmet^CİCİMEN
SALI|1|08:30|10:20|BİYOKİMYA^(Başak Amfi)^Prof. Dr. Mehmet KARACA
ÇARŞAMBA|1|08:30|10:20|ZOOLOJİ^(3-A)^Doç. Dr. Mustafa YAVUZ
PERŞEMBE|1|08:30|10:20|İNGİLİZCE-I^(Başak Amfi)^Öğr. Gör. Burçak AKINCI
CUMA|1|08:30|10:20|KİMYA^(Başak Amfi)^Dr. Öğr. Üyesi Kudret^AKPINAR
SALI|1|10:30|12:20|FİZİK-I^(3-B)^Öğr. Gör. Dr. Fatih PERİNÇEK
ÇARŞAMBA|1|10:30|12:20|ZOOLOJİ^UYG. (Mikroskop Lab.)^Doç. Dr. Mustafa YAVUZ
PERŞEMBE|1|10:30|12:20|ATATÜRK İLKELERİ VE^İNKILAP TARİHİ-I^(3-B)^Öğr. Gör. Dr. Ahmet KISA
CUMA|1|10:30|12:20|TÜRK DİLİ-I^(3-B)^Öğr. Gör. Sinan ORUÇOĞLU
SALI|1|13:30|15:20|BOTANİK^(2-B)^Prof. Dr. Ramazan Süleyman^GÖKTÜRK
SALI|1|15:30|17:20|BOTANİK^UYG (Mikroskop Lab.)^Prof. Dr. Ramazan Süleyman^GÖKTÜRK
PERŞEMBE|1|16:30|17:20|KARİYER PLANLAMA^(4-E)^Doç. Dr. Hüseyin UYSAL
PAZARTESİ|2|08:30|10:20|BAHÇE BİTKİLERİ^(1-B)^Prof. Dr. Halil DEMİR^(Seçmeli Ders)
ÇARŞAMBA|2|08:30|10:20|TARIM EKONOMİSİ^(4-D)^Arş. Gör. Dr. Merve YILMAZ
PERŞEMBE|2|08:30|10:20|GENEL BAĞCILIK^(2-C)^Prof. Dr. İlknur POLAT^(Seçmeli Ders)
CUMA|2|08:30|10:20|GENEL MEYVECİLİK^(1-B)^Prof. Dr. Şadiye GÖZLEKÇİ^(Seçmeli Ders)
SALI|2|09:30|12:20|MÜHENDİSLİK MEKANİĞİ^(4-F)^Prof. Dr. Can ERTEKİN
PAZARTESİ|2|10:30|12:20|BAHÇE BİTKİLERİ^UYG.^Prof. Dr. Halil DEMİR^(Seçmeli Ders)
ÇARŞAMBA|2|10:30|12:20|İSTATİSTİK^(3-A)^Prof. Dr. Mehmet Ziya FIRAT
PERŞEMBE|2|10:30|12:20|ENTOMOLOJİ^(2-A)^Doç. Dr. Cengiz İKTEN
PAZARTESİ|2|13:30|15:20|METEOROLOJİ^(4-F)^Prof. Dr. Ahmet KURUNÇ^(Seçmeli Ders)
SALI|2|13:30|15:20|FİTOPATOLOJİ^(2-D)^Prof. Dr. Hüseyin BASIM
ÇARŞAMBA|2|13:30|15:20|TAHIL BAKLAGİL VE YEM^BİTKİLERİ YETİŞTİRİCİLİĞİ^(3-B)^Doç. Dr. Safinaz ELMASULU^(Seçmeli Ders)
PERŞEMBE|2|13:30|15:20|TARIM TARİHİ VE^DEONTOLOJİSİ^(4-B)^Doç. Dr. Yavuz^TAŞCIOĞLU
PAZARTESİ|2|15:30|17:20|TARIMSAL YAPILAR^(4-F)^Prof. Dr. Kenan BÜYÜKTAŞ^(Seçmeli Ders)
SALI|2|15:30|17:20|BİTKİ FİZYOLOJİSİ^(Başak Amfi)^Dr. Öğr. Üyesi Deniz HAZAR
ÇARŞAMBA|2|15:30|17:20|TAHIL BAKLAGİL VE YEM^BİTKİLERİ YETİŞTİRİCİLİĞİ^UYG. (3-B)^Doç. Dr. Safinaz ELMASULU^(Seçmeli Ders)
PERŞEMBE|2|15:30|17:20|MOLEKÜLER^BİYOLOJİ^(2-B)^Prof. Dr. Hüseyin^BASIM
PAZARTESİ|3|08:30|10:20|BÖCEK MORFOLOJİSİ VE^FİZYOLOJİSİ^(4-E)^Doç. Dr. Fatih DAĞLI
SALI|3|08:30|10:20|BÖCEK^MORFOLOJİSİ VE^FİZYOLOJİSİ^UYG. (Mikroskop^Lab.)^Doç. Dr. Fatih DAĞLI
ÇARŞAMBA|3|08:30|10:20|ÖRTÜALTI SEBZE^YETİŞTİRİCİLİĞİ^UYG.^Prof. Dr. Ersin POLAT^(Uygulama yeri dersin öğretim^üyesinden öğrenilecektir)^(Seçmeli Ders)
ÇARŞAMBA|3|08:30|10:20|BAHÇE BİTKİLERİ^MEKANİZASYONU^UYG.^Prof. Dr. Murad^ÇANAKCI^(Seçmeli Ders)
CUMA|3|08:30|10:20|BAHÇE BİTKİLERİ^HASTALIKLARI^UYG. (Mikroskop Lab.)^Prof. Dr. Hüseyin^BASIM^(Seçmeli Ders)
PERŞEMBE|3|09:30|12:20|BİTKİ^HASTALIKLARI VE^ZARARLILARI İLE^MÜCADELE^YÖNTEMLERİ^(2-B)^Prof. Dr. Özer ÇALIŞ
PAZARTESİ|3|10:30|12:20|ÖRTÜALTI^SEBZE^YETİŞTİRİCİLİĞİ^(Başak Amfi)^Prof. Dr. Ersin^POLAT^(Seçmeli Ders)
PAZARTESİ|3|10:30|12:20|BAHÇE BİTKİLERİ^MEKANİZASYONU^(4-F)^Prof. Dr. Murad^ÇANAKCI^(Seçmeli Ders)
PAZARTESİ|3|10:30|12:20|BAHÇE^BİTKİLERİ^HASTALIKLARI^(2-B)^Prof. Dr. Hüseyin^BASIM^(Seçmeli Ders)
SALI|3|10:30|12:20|YABANCI OTLARLA^MÜCADELE^(2-A)^Doç. Dr. Yasin Emre^KİTİŞ
ÇARŞAMBA|3|10:30|12:20|AKILLI TARIM^TEKNOLOJİLERİ^(4-D)^Prof. Dr. Mehmet TOPAKCI
CUMA|3|10:30|12:20|BİTKİ ISLAHININ^TEMEL İLKELERİ^UYG.(2-A)^Prof. Dr. Bülent UZUN
PAZARTESİ|3|13:30|14:20|BAKTERİYOLOJİ^UYG. (Mik. Lab.)^Prof. Dr. Hüseyin BASIM
SALI|3|13:30|15:20|YABANCI OTLARLA^MÜCADELE^UYG.(2-A)^Doç. Dr. Yasin Emre^KİTİŞ
ÇARŞAMBA|3|13:30|15:20|BİTKİ ISLAHININ TEMEL^İLKELERİ^(2-A)^Prof. Dr. Bülent UZUN
PERŞEMBE|3|13:30|15:20|ORGANİK TARIMDA^BİTKİ KORUMA^(2-C)^Prof. Dr. Hüseyin^BASIM^(Seçmeli Ders)
PAZARTESİ|3|14:30|15:20|VİROLOJİ UYG.^(Doç.Dr. Hakan FİDAN Lab.)^Doç. Dr. Hakan FİDAN
PAZARTESİ|3|15:30|17:20|VİROLOJİ^(1-A)^Doç. Dr. Hakan FİDAN
SALI|3|15:30|17:20|ARAŞTIRMA YAZIM^VE SUNUM^TEKNİKLERİ^(3-B)^Prof. Dr. Orhan^ÖZÇATALBAŞ^(Seçmeli Ders)
ÇARŞAMBA|3|15:30|17:20|BAKTERİYOLOJİ^(2-A)^Prof. Dr. Hüseyin BASIM
PERŞEMBE|3|15:30|17:20|KENTSEL^ENTOMOLOJİ^(2-C)^Doç. Dr. Utku^YÜKSELBABA^(Seçmeli Ders)
PAZARTESİ|4|08:30|10:20|ÖRTÜALTI HASTALIK VE^ZARARLILARI^(2-A)^Prof. Dr. Fedai ERLER^(Seçmeli Ders)
SALI|4|08:30|10:20|BİTKİ FUNGAL^HASTALIKLARI^(4-D)^Prof. Dr. Özer ÇALIŞ
ÇARŞAMBA|4|08:30|10:20|BAHÇE BİTKİLERİ^ZARARLILARI^(2-B)^Prof. Dr. Fedai ERLER
PERŞEMBE|4|08:30|10:20|BİTKİ KORUMA MEVZUATI^(1-B)^Doç. Dr. Fatih DAĞLI^(Seçmeli Ders)
CUMA|4|08:30|10:20|TAHIL BAKLAGİL VE YEM^BİTKİLERİ HASTALIKLARI^(4-D)^Prof. Dr. Özer ÇALIŞ^(Seçmeli Ders)
PAZARTESİ|4|10:30|12:20|YABANCI OTLARIN^ETNOBOTANİK KULLANIMI^(3-A)^Doç. Dr. Yasin Emre KİTİŞ^(Seçmeli Ders)
PERŞEMBE|4|10:30|12:20|YEMEKLİK MANTAR^HASTALIK VE ZARARLILARI^(2-C)^Prof. Dr. Hüseyin BASIM^(Seçmeli Ders)
CUMA|4|10:30|12:20|PARK SÜS BİTKİLERİ^HASTALIK VE ZARARLILARI^(1-B)^Prof. Dr. Fedai ERLER^(Seçmeli Ders)
SALI|4|10:30|11:20|MEYVE VE BAĞ ZARARLILARI^UYG. (Mik. Lab.)^Prof. Dr. Fedai ERLER^(Seçmeli Ders)
ÇARŞAMBA|4|10:30|11:20|BAHÇE BİTKİLERİ^ZARARLILARI^UYG. (2-B)^Prof. Dr. Fedai ERLER
SALI|4|11:30|12:20|BİTKİ BAKTERİ^HASTALIKLARI^UYG. (Mikroskop Lab.)^Prof. Dr. Hüseyin BASIM
PAZARTESİ|4|12:30|13:20|BİTİRME ÇALIŞMASI^(Bölüm öğretim üyeleri)
SALI|4|12:30|13:20|BİTİRME ÇALIŞMASI^(Bölüm öğretim üyeleri)
PAZARTESİ|4|13:30|15:20|BİTKİ PARAZİTİ^NEMATODLAR^(2-D)^Prof. Dr. Zübeyir DEVRAN
SALI|4|13:30|14:20|BİTKİ PARAZİTİ^NEMATODLAR^UYG. (2-C)^Prof. Dr. Zübeyir DEVRAN
PERŞEMBE|4|13:30|14:20|BİTKİ FUNGAL^HASTALIKLARI^UYG. (Mikroskop Lab.)^Prof. Dr. Özer ÇALIŞ
SALI|4|14:30|15:20|TAHIL BAKLAGİL VE YEM^BİTKİLERİ HASTALIKLARI^UYG.(2-C)^Prof. Dr. Özer ÇALIŞ^(Seçmeli Ders)
ÇARŞAMBA|4|14:30|15:20|BİTKİ VİRÜS HASTALIKLARI^UYG. (Doç. Dr. Hakan FİDAN^LAB.)^Doç. Dr. Hakan FİDAN
PERŞEMBE|4|14:30|15:20|TARLA BİTKİLERİ ZARARLILARI^UYG. (Mik. Lab.)^Doç. Dr. Utku YÜKSELBABA
PAZARTESİ|4|15:30|17:20|BİTKİ BAKTERİ^HASTALIKLARI^(2-D)^Prof. Dr. Hüseyin BASIM
SALI|4|15:30|17:20|TARLA BİTKİLERİ^ZARARLILARI^(2-A)^Doç. Dr. Utku YÜKSELBABA
ÇARŞAMBA|4|15:30|17:20|BİTKİ VİRÜS HASTALIKLARI^(3-A)^Doç. Dr. Hakan FİDAN
PERŞEMBE|4|15:30|17:20|MEYVE VE BAĞ^ZARARLILARI^(2-D)^Prof. Dr. Fedai ERLER^(Seçmeli Ders)
        """.trimIndent())),
        Dataset(ClassSchedules.AGRICULTURAL_ECONOMICS_DEPARTMENT, 10, rows("""
PAZARTESİ|1|08:30|10:20|BOTANİK^(Başak Amfi)^Doç. Dr. Orhan ÜNAL
ÇARŞAMBA|1|08:30|10:20|METEOROLOJİ^(4-F)^Doç. Dr. Bekir Sıtkı KARATAŞ
PERŞEMBE|1|08:30|10:20|MATEMATİK-I^(Portakal Amfi)^Arş. Gör. Dr. Mehmet^CİCİMEN
CUMA|1|08:30|10:20|TÜRK DİLİ-I^(3-B)^Öğr. Gör. Sinan ORUÇOĞLU
PAZARTESİ|1|10:30|12:20|BOTANİK^UYG. (Mikroskop Lab.)^Doç. Dr. Orhan ÜNAL
SALI|1|10:30|12:20|TARIMSAL KURULUŞ VE^ORGANİZASYONLAR^(4-C)^Doç. Dr. Yavuz TAŞCIOĞLU
PERŞEMBE|1|10:30|12:20|ATATÜRK İLKELERİ VE^İNKILAP TARİHİ-I^(3-B)^Öğr. Gör. Dr. Ahmet KISA
ÇARŞAMBA|1|10:30|11:20|KARİYER PLANLAMA^(Tevfik Aksoy Kon. Sal.)^Doç. Dr. Yavuz TAŞCIOĞLU
PAZARTESİ|1|13:30|15:20|TEKNİK RESİM^(Tasarım-1)^Arş. Gör. Dr. İsmail BOYAR
ÇARŞAMBA|1|13:30|15:20|İSTATİSTİK-I^(Bil. Lab.)^Prof. Dr. Süleyman^KARAMAN
PERŞEMBE|1|13:30|15:20|TARIM TARİHİ VE^DEONTOLOJİSİ^(4-B)^Doç. Dr. Yavuz TAŞCIOĞLU
PAZARTESİ|1|15:30|17:20|TEKNİK RESİM^UYG. (Tasarım-1)^Arş. Gör. Dr. İsmail BOYAR
PERŞEMBE|1|15:30|17:20|İNGİLİZCE-I^(3-B)^Öğr. Gör. Seda AKSUNGUR
PERŞEMBE|2|08:30|10:20|MALİYE^(Tasarım-II)^Doç. Dr. Rahmiye Figen^CEYLAN HOZER
CUMA|2|08:30|10:20|HAYVAN YETİŞTİRME VE^BESLEME^(Yonca Amfi)^Öğr. Gör. Dr. Nilgün YAPICI
SALI|2|10:30|12:20|TARIM EKONOMİSİNDE^MESLEKİ İNGİLİZCE^(4-A)^Prof. Dr. Burhan ÖZKAN^(Seçmeli Ders)
ÇARŞAMBA|2|10:30|12:20|MİKROEKONOMİ^(Yonca Amfi)^Prof. Dr. Burhan ÖZKAN
PERŞEMBE|2|10:30|12:20|MAKROEKONOMİ^(Tasarım-I1)^Doç. Dr. Makbule Nisa^MENCET YELBOĞA
CUMA|2|10:30|12:20|HAYVAN YETİŞTİRME VE^BESLEME^UYG. (Yonca Amfi)^Öğr. Gör. Dr. Nilgün YAPICI
PAZARTESİ|2|13:30|15:20|BİTKİ KORUMA^(1-A)^Prof. Dr. Özer ÇALIŞ
SALI|2|13:30|15:20|TARIMDA İŞ HUKUKU^(4-D)^Prof. Dr. Cengiz SAYIN
ÇARŞAMBA|2|13:30|15:20|TARIMSAL BİLİŞİM^(4-A)^Prof. Dr. Orhan^ÖZÇATALBAŞ^(Seçmeli Ders)
PERŞEMBE|2|13:30|15:20|BAHÇE BİTKİLERİ^(1-A)^Doç. Dr. Tuğçe ÖZSAN KILIÇ
SALI|2|15:30|17:20|TARIMDA FİYAT ANALİZİ^(4-D)^Doç. Dr. Rahmiye Figen^CEYLAN HOZER^(Seçmeli Ders)
PERŞEMBE|2|15:30|17:20|BAHÇE BİTKİLERİ^UYG.^(4-B)^Doç. Dr. Tuğçe ÖZSAN KILIÇ
SALI|3|08:30|10:20|TOPLUMSAL DUYARLILIK^VE KATKI^(1-C)^Doç. Dr. Hüseyin UYSAL
PAZARTESİ|3|10:30|12:20|DOĞAL KAYNAK VE^ÇEVRE EKONOMİSİ^(Tasarım II)^Doç. Dr. Makbule Nisa^MENCET YELBOĞA
ÇARŞAMBA|3|10:30|12:20|TARIM POLİTİKASI-I^(Portakal Amfi)^Prof. Dr. Cengiz SAYIN
CUMA|3|10:30|12:20|KIRSAL EKONOMİ VE^PLANLAMA^(4-E)^Doç. Dr. Yavuz TAŞCIOĞLU^(Seçmeli Ders)
PAZARTESİ|3|13:30|15:20|TARIM EKONOMİSİ^İSTATİSTİĞİ-II^(4-B)^Prof. Dr. Süleyman^KARAMAN
SALI|3|13:30|15:20|ULUSLARARASI İKTİSAT^(Tasarım-I)^Doç. Dr. Rahmiye Figen^CEYLAN HOZER^(Seçmeli Ders)
ÇARŞAMBA|3|13:30|15:20|TARIMSAL PAZARLAMA^(Tasarım-I)^Prof. Dr. Metin Göksel^AKPINAR
PERŞEMBE|3|13:30|15:20|TARIMSAL MUHASEBE^(Yonca Amfi)^Prof. Dr. Handan AKÇAÖZ
PAZARTESİ|3|15:30|17:20|KALKINMA EKONOMİSİ^(Tasarım II)^Doç. Dr. Makbule Nisa^MENCET YELBOĞA^(Seçmeli Ders)
SALI|3|15:30|17:20|ARAŞTIRMA YAZIM^VE SUNUM^TEKNİKLERİ^(3-B)^Prof. Dr. Orhan^ÖZÇATALBAŞ^(Seçmeli Ders)
ÇARŞAMBA|3|15:30|17:20|BAHÇE BİTKİLERİNDE^TOPRAKSIZ^YETİŞTİRİCİLİK^(4-D)^Prof. Dr. Nafiye ÜNAL^(Seçmeli Ders)
PERŞEMBE|3|15:30|17:20|TARIMSAL MUHASEBE^UYG. (Yonca Amfi)^Prof. Dr. Handan AKÇAÖZ
CUMA|3|15:30|17:20|TARIM EKONOMİSİ^UYGULAMALARI^(Uygulamaların yerleri bölüm^öğretim üyeleri tarafından^belirlenecektir)
ÇARŞAMBA|4|08:30|10:20|İŞLETME YÖNETİMİ^(4-B)^Prof. Dr. İbrahim YILMAZ
CUMA|4|08:30|10:20|KIRSAL KALKINMA^(4-E)^Doç. Dr. Yavuz TAŞCIOĞLU
SALI|4|10:30|12:20|TARIMDA RİSK^YÖNETİMİ VE SİGORTA^(Portakal Amfi)^Prof. Dr. Handan AKÇAÖZ^(Seçmeli Ders)
ÇARŞAMBA|4|10:30|12:20|TARIMDA GİRİŞİMCİLİK^(Tevfik Aksoy Kon. Sal.)^Prof. Dr. Süleyman^KARAMAN^(Seçmeli Ders)
PERŞEMBE|4|10:30|12:20|AVRUPA BİRLİĞİ ORTAK^TARIM POLİTİKASI^(Portakal Amfi)^Prof. Dr. Cengiz SAYIN^(Seçmeli Ders)
PAZARTESİ|4|13:30|15:20|TARIMSAL ÜRETİMDE İŞ^SAĞLIĞI VE GÜVENLİĞİ^(Portakal Amfi)^Prof. Dr. Murad ÇANAKCI^(Seçmeli Ders)
SALI|4|13:30|15:20|PROJE HAZIRLAMA VE^DEĞERLENDİRME^TEKNİĞİ^(Yonca Amfi)^Prof. Dr. Burhan ÖZKAN
ÇARŞAMBA|4|13:30|15:20|DIŞ TİCARET POLİTİKASI^VE UYGULAMLARI^(Yonca Amfi)^Prof. Dr. Cengiz SAYIN
CUMA|4|13:30|15:20|BİTİRME ÇALIŞMASI^(Bölüm öğretim üyeleri)
PERŞEMBE|4|13:30|16:20|EKONOMETRİ^(4-E)^Prof. Dr. İbrahim YILMAZ
PAZARTESİ|4|15:30|17:20|TARIM TOPRAKLARININ^KİRLENMESİ^(3-B)^Prof. Dr. Şule ORMAN^(Seçmeli Ders)
SALI|4|15:30|17:20|PROJE HAZIRLAMA VE^DEĞERLENDİRME^TEKNİĞİ^UYG. (Yonca Amfi)^Prof. Dr. Burhan ÖZKAN
ÇARŞAMBA|4|15:30|17:20|SATIŞ TEKNİKLERİ^(4-A)^Prof. Dr. Metin Göksel^AKPINAR^(Seçmeli Ders)
        """.trimIndent())),
        Dataset(ClassSchedules.AGRICULTURAL_MACHINERY_DEPARTMENT, 14, rows("""
SALI|1|08:30|09:20|KARİYER PLANLAMA^(Yonca Amfi)^Dr. Öğr. Üyesi Sefai BİLGİN
ÇARŞAMBA|1|08:30|10:20|TARIM MAKİNALARI^UYG.^Prof. Dr. İbrahim AKINCI
PERŞEMBE|1|08:30|10:20|İNGİLİZCE-I^(Başak Amfi)^Öğr. Gör. Burçak AKINCI
CUMA|1|08:30|10:20|KİMYA^(Başak Amfi)^Dr. Öğr. Üyesi Kudret^AKPINAR
SALI|1|10:30|12:20|TARIM MAKİNALARI^(4-D)^Prof. Dr. İbrahim AKINCI
ÇARŞAMBA|1|10:30|12:20|MATEMATİK-I^(Başak Amfi)^Arş. Gör. Dr. Mehmet^CİCİMEN
PERŞEMBE|1|10:30|12:20|ATATÜRK İLKELERİ VE^İNKILAP TARİHİ-I^(Başak Amfi)^Öğr. Gör. Murat BOZ
CUMA|1|10:30|12:20|TÜRK DİLİ-I^(3-B)^Öğr. Gör. Sinan ORUÇOĞLU
PAZARTESİ|1|13:30|15:20|BOTANİK^(Yonca Amfi)^Doç. Dr. Orhan ÜNAL
SALI|1|13:30|15:20|FİZİK-I^(Başak Amfi)^Öğr. Gör. Dr. Nigar ALATA
ÇARŞAMBA|1|13:30|15:20|TEKNİK RESİM^(Tasarım-II)^Prof. Dr. Mehmet TOPAKCI
PAZARTESİ|1|15:30|17:20|BOTANİK^UYG. (Mikroskop Lab.)^Doç. Dr. Orhan ÜNAL
ÇARŞAMBA|1|15:30|17:20|TEKNİK RESİM^UYG. (Tasarım-II)^Prof. Dr. Mehmet TOPAKCI
ÇARŞAMBA|2|08:30|10:20|MAKİNA MALZEME^BİLGİSİ^(Tasarım-I)^Prof. Dr. Hüseyin Kürşat^ÇELİK
PERŞEMBE|2|08:30|10:20|BAHÇE^BİTKİLERİ^(2-A)^Prof. Dr. Ersin^POLAT^(Seçmeli Ders)
PERŞEMBE|2|08:30|10:20|HAYVAN^YETİŞTİRME VE^BESLEME^(Yonca Amfi)^Öğr. Gör. Dr. Nilgün^YAPICI^(Seçmeli Ders)
PAZARTESİ|2|09:30|12:20|DİFERANSİYEL^DENKLEMLER^(4-D)^Prof. Dr. Davut KARAYEL
ÇARŞAMBA|2|10:30|12:20|TARIM EKONOMİSİ^(1-B)^Doç. Dr. Rahmiye Figen^CEYLAN HOZER^(Seçmeli Ders)
PERŞEMBE|2|10:30|12:20|BAHÇE^BİTKİLERİ^UYG. Prof. Dr.^Ersin POLAT^(Seçmeli Ders)
PERŞEMBE|2|10:30|12:20|HAYVAN^YETİŞTİRME VE^BESLEME^UYG. (Yonca Amfi)^Öğr. Gör. Dr. Nilgün^YAPICI^(Seçmeli Ders)
CUMA|2|10:30|12:20|İSTATİSTİK^(4-A)^Prof. Dr. Burak^KARACAÖREN
PAZARTESİ|2|13:30|15:20|BİTKİ KORUMA^(1-A)^Prof. Dr. Özer ÇALIŞ
SALI|2|13:30|15:20|FİDE VE FİDANCILIK^TEKNİĞİ^(1-A)^Prof. Dr. Nafiye ÜNAL^(Seçmeli Ders)
PERŞEMBE|2|13:30|15:20|GÜBRELER VE GÜBRELEME^(Başak Amfi)^Prof. Dr. Mustafa KAPLAN^(Seçmeli Ders)
ÇARŞAMBA|2|13:30|16:20|STATİK VE^MUKAVEMET^(4-E)^Prof. Dr. Hüseyin Kürşat^ÇELİK
PAZARTESİ|2|15:30|17:20|TARIMSAL YAPILAR^(4-F)^Prof. Dr. Kenan BÜYÜKTAŞ^(Seçmeli Ders)
SALI|2|15:30|17:20|BİTKİ FİZYOLOJİSİ^(Başak Amfi)^Dr. Öğr. Üyesi Deniz HAZAR^(Seçmeli Ders)
PERŞEMBE|2|15:30|17:20|GÜBRELER VE GÜBRELEME^UYG. (Başak Amfi)^Prof. Dr. Mustafa KAPLAN^(Seçmeli Ders)
PAZARTESİ|3|08:30|10:20|KIRSAL ALANDA SU^GETİRME^(4-A)^Doç. Dr. Nefise Yasemin^TEZCAN^(Seçmeli Ders)
ÇARŞAMBA|3|08:30|10:20|POMPAJ TESİS VE^MAKİNALARI^(2-D)^Prof. Dr. Davut KARAYEL^(Seçmeli Ders)
CUMA|3|08:30|10:20|HAYVANCILIKTA^MEKANİZASYON^(4-F)^Prof. Dr. Can ERTEKİN^(Seçmeli Ders)
PERŞEMBE|3|09:30|11:20|MAKİNA İMALAT^YÖNTEMLERİ^(4-E)^Prof. Dr. Hüseyin Kürşat^ÇELİK
SALI|3|09:30|12:20|MAKİNA ELEMANLARI^(Yonca Amfi)^Prof. Dr. Hüseyin Kürşat^ÇELİK
PAZARTESİ|3|10:30|12:20|TOPRAK İŞLEME^MAKİNALARI^(4-A)^Prof. Dr. İbrahim AKINCI
CUMA|3|10:30|12:20|TOPLUMSAL DUYARLILIK^VE KATKI^(4-D)^Arş. Gör. Dr. İsmail BOYAR
PERŞEMBE|3|11:30|12:20|MAKİNA İMALAT^YÖNTEMLERİ^UYG.(4-E)^Prof. Dr. Hüseyin Kürşat^ÇELİK
PAZARTESİ|3|13:30|15:20|AKADEMİK YAZIM VE^SUNUM TEKNİKLERİ^(2-C)^Dr. Öğr. Üyesi Sefai BİLGİN^(Seçmeli Ders)
SALI|3|13:30|15:20|BİLGİSAYAR DESTEKLİ^ÇİZİM^(Enf. Böl. Lab. 5)^Prof. Dr. Mehmet TOPAKCI
CUMA|3|13:30|15:20|TARIM MAKİNALARI^UYGULAMALARI–I^(Uygulamaların yerleri bölüm^öğretim üyeleri tarafından^belirlenecektir)
PAZARTESİ|3|15:30|17:20|TOPRAKSIZ TARIM VE^BİTKİ BESLEME^(4-B)^Doç. Dr. İlker SÖNMEZ^(Seçmeli Ders)
SALI|3|15:30|17:20|BİLGİSAYAR DESTEKLİ^ÇİZİM^UYG. (Enf. Böl. Lab. 5)^Prof. Dr. Mehmet TOPAKCI
ÇARŞAMBA|3|14:30|17:20|TERMODİNAMİK VE^ISI TRANSFERİ^(2-D)^Prof. Dr. Can ERTEKİN
CUMA|3|15:30|17:20|TARIM MÜHENDİSLİĞİ^UYGULAMALARI–I^(Uygulamaların yerleri bölüm^öğretim üyeleri tarafından^belirlenecektir)
PAZARTESİ|4|08:30|10:20|ÜRÜN İŞLEME TEKNİĞİ^(2-C)^Prof. Dr. Can ERTEKİN^(Seçmeli Ders)
PERŞEMBE|4|08:30|10:20|HASAT HARMAN^MEKANİZASYONU^(4-A)^Prof. Dr. Can ERTEKİN
PAZARTESİ|4|10:30|12:20|TARIMSAL^İKLİMLENDİRME^TEKNİĞİ^(4-E)^Prof. Dr. Ahmet KÜRKLÜ
ÇARŞAMBA|4|10:30|12:20|KALİTE VE^STANDARTLAR^(Tasarım-I)^Prof. Dr. Murad ÇANAKCI^(Seçmeli Ders)
PERŞEMBE|4|10:30|12:20|TARIM TRAKTÖRLERİ^(4-F)^Prof. Dr. İbrahim AKINCI
PAZARTESİ|4|12:30|13:20|MEZUNİYET TEZİ PROJESİ^(Bölüm öğretim üyeleri tarafından^belirlenecektir)
SALI|4|12:30|13:20|MEZUNİYET TEZİ PROJESİ^(Bölüm öğretim üyeleri tarafından^belirlenecektir)
PAZARTESİ|4|13:30|15:20|TARIM MAKİNALARINDA^COĞRAFİ BİLGİ^SİSTEMLERİ^UYGULAMALARI^(Enf. Lab. 5)^Prof. Dr. Mehmet TOPAKCI^(Seçmeli Ders)
ÇARŞAMBA|4|13:30|15:20|TOPRAK BİTKİ^ATMOSFER SİSTEMİ^(4-F)^Prof. Dr. Harun KAMAN^(Seçmeli Ders)
PERŞEMBE|4|13:30|15:20|TARIM MAKİNALARI^UYGULAMALARI–III^(Uygulamaların yerleri bölüm^öğretim üyeleri tarafından^belirlenecektir)
PAZARTESİ|4|15:30|17:20|YENİLENEBİLİR ENERJİ^KAYNAKLARI^(Yonca Amfi)^Doç. Dr. Nuri ÇAĞLAYAN
PERŞEMBE|4|15:30|17:20|TARIM MÜHENDİSLİĞİ^UYGULAMALARI–III^(Uygulamaların yerleri bölüm^öğretim üyeleri tarafından^belirlenecektir)
        """.trimIndent())),
        Dataset(ClassSchedules.AGRICULTURAL_BIOTECHNOLOGY_DEPARTMENT, 18, rows("""
PAZARTESİ|1|08:30|10:20|BOTANİK^(Başak Amfi)^Doç. Dr. Orhan ÜNAL
PERŞEMBE|1|08:30|10:20|MATEMATİK-I^(Portakal Amfi)^Arş. Gör. Dr. Mehmet^CİCİMEN
PAZARTESİ|1|10:30|12:20|BOTANİK^UYG. (Mikroskop Lab.)^Doç. Dr. Orhan ÜNAL
ÇARŞAMBA|1|10:30|12:20|METEOROLOJİ^(4-F)^Doç. Dr. Bekir Sıtkı KARATAŞ
PERŞEMBE|1|10:30|12:20|ATATÜRK İLKELERİ VE^İNKILAP TARİHİ-I^(3-B)^Öğr. Gör. Dr. Ahmet KISA
CUMA|1|10:30|12:20|KİMYA^(Başak Amfi)^Doç. Dr. Naciye ERKAN
PAZARTESİ|1|13:30|15:20|INTRODUCTION TO^BIOTECHNOLOGY^(4-A)^Doç. Dr. DEMİR ÖZDEMİR
ÇARŞAMBA|1|13:30|15:20|ZOOLOJİ^(Başak Amfi)^Doç. Dr. Mustafa YAVUZ
CUMA|1|14:30|16:20|TÜRK DİLİ-I^(Başak Amfi)^Öğr. Gör. Sinan ORUÇOĞLU
PAZARTESİ|1|15:30|17:20|INTRODUCTION TO^BIOTECHNOLOGY^UYG.(4-A)^Doç. Dr. DEMİR ÖZDEMİR
ÇARŞAMBA|1|15:30|17:20|ZOOLOJİ^UYG. (Mikroskop Lab.)^Doç. Dr. Mustafa YAVUZ
PERŞEMBE|1|16:30|17:20|KARİYER PLANLAMA^(4-E)^Doç. Dr. Hüseyin UYSAL
PAZARTESİ|2|08:30|10:20|ENTOMOLOJİ^(2-B)^Arş. Gör. Dr. Hilal Şule^TOSUN^(Seçmeli Ders)
ÇARŞAMBA|2|08:30|10:20|BAHÇE BİTKİLERİ^MEKANİZASYONU^UYG.^Prof. Dr. Murad ÇANAKCI^(Seçmeli Ders)
PERŞEMBE|2|08:30|10:20|BAHÇE BİTKİLERİ^(1-C)^Prof. Dr. Hamide GÜBBÜK^(Seçmeli Ders)
CUMA|2|08:30|10:20|HAYVAN YETİŞTİRME VE^BESLEME^(Yonca Amfi)^Öğr. Gör. Dr. Nilgün YAPICI^(Seçmeli Ders)
SALI|2|08:30|11:20|MİKROBİYOLOJİ^(Tasarım II)^Prof. Dr. Mehmet Fatih^CENGİZ
PAZARTESİ|2|10:30|12:20|BAHÇE BİTKİLERİ^MEKANİZASYONU^(4-F)^Prof. Dr. Murad ÇANAKCI^(Seçmeli Ders)
PERŞEMBE|2|10:30|12:20|BAHÇE BİTKİLERİ^UYG.^Prof. Dr. Hamide GÜBBÜK^(Seçmeli Ders)
CUMA|2|10:30|12:20|HAYVAN YETİŞTİRME VE^BESLEME^UYG.(Yonca Amfi)^Öğr. Gör. Dr. Nilgün YAPICI^(Seçmeli Ders)
SALI|2|11:30|12:20|PLANT PHYSIOLOGY-I^(Tasarım-II)^Prof. Dr. Songül SEVER^MUTLU
PAZARTESİ|2|13:30|15:20|TARIMSAL ÜRETİMDE İŞ^SAĞLIĞI VE GÜVENLİĞİ^(Portakal Amfi)^Prof. Dr. Murad ÇANAKCI^(Seçmeli Ders)
SALI|2|13:30|15:20|PLANT PHYSIOLOGY-I^(Tasarım-II)^Prof. Dr. Songül SEVER^MUTLU
PERŞEMBE|2|13:30|15:20|İSTATİSTİKSEL^YÖNTEMLER^(3-C)^Prof. Dr. Kemal KARABAĞ
CUMA|2|14:30|17:20|MOLECULAR BIOLOGY I^(3-C)^Doç. Dr. Aysun ÖZÇELİK
ÇARŞAMBA|2|15:30|16:20|HAYVANSAL ÜRETİMDE^BİYOTEKNOLOJİK UYGULAMALAR^(Tasarım-1)^Prof. Dr. Kemal KARABAĞ^(Seçmeli Ders)
PAZARTESİ|2|15:30|17:20|HAYVANSAL ÜRETİMDE^BİYOTEKNOLOJİK^UYGULAMALAR^(1-B)^Prof. Dr. Kemal KARABAĞ^(Seçmeli Ders)
SALI|2|15:30|17:20|ARAŞTIRMA YAZIM VE^SUNUM TEKNİKLERİ^(3-B)^Prof. Dr. Orhan ÖZÇATALBAŞ^(Seçmeli Ders)
PERŞEMBE|2|15:30|17:20|İSTATİSTİKSEL^YÖNTEMLER^UYG. (3-C)^Prof. Dr. Kemal KARABAĞ
ÇARŞAMBA|2|16:30|17:20|HAYVANSAL ÜRETİMDE^BİYOTEKNOLOJİK UYGULAMALAR^UYG. (Tasarım-1)^Prof. Dr. Kemal KARABAĞ^(Seçmeli Ders)
PAZARTESİ|3|09:30|12:20|BIOTECHNOLOGY^LABORATORY^TECHNIQUES-II^(4. Blok 435 Nolu Lab./4-C)^Doç. Dr. Hatice İKTEN
PERŞEMBE|3|09:30|12:20|BİTKİ HASTALIKLARI VE^ZARARLILARI İLE^MÜCADELE YÖNTEMLERİ^(2-B)^Prof. Dr. Fedai ERLER^(Seçmeli Ders)
CUMA|3|09:30|12:20|MOLECULAR BIOLOGY II^(Tasarım-1)^Doç. Dr. Aysun ÖZÇELİK
SALI|3|10:30|12:20|YABANCI OTLARLA^MÜCADELE^(2-A)^Doç. Dr. Yasin Emre KİTİŞ^(Seçmeli Ders)
SALI|3|13:30|15:20|YABANCI OTLARLA^MÜCADELE^UYG.(2-A)^Doç. Dr. Yasin Emre KİTİŞ^(Seçmeli Ders)
ÇARŞAMBA|3|13:30|15:20|BİTKİ ISLAHI^(2-C)^Prof. Dr. Nedim MUTLU
PERŞEMBE|3|13:30|16:20|FUNDAMENTALS OF^GENETICS^(1-B)^Doç. Dr. Münevver AKSOY
SALI|3|15:30|17:20|BİTKİ DOKU KÜLTÜRÜ^TEKNİKLERİ^(Tasarım II)^Prof. Dr. Esin ARI
ÇARŞAMBA|3|15:30|17:20|BİTKİ ISLAHI^UYG. (2-C)^Prof. Dr. Nedim MUTLU
ÇARŞAMBA|4|08:30|10:20|PLANT BIOTECHNOLOGY^(2-C)^Doç. Dr. Hatice İKTEN
SALI|4|09:30|12:20|HAYVAN BESLEMEDE^BİYOTEKNOLOJİ^(Bil. Lab.)^Doç. Dr. Demir ÖZDEMİR^(Seçmeli Ders)
CUMA|4|09:30|12:20|CELL BIOLOGY-I^(2-C)^Doç. Dr. Münevver AKSOY
PAZARTESİ|4|10:30|12:20|ACADEMİC ENGLISH-II^(Bil. Lab.)^Doç. Dr. Münevver AKSOY^(Seçmeli Ders)
ÇARŞAMBA|4|10:30|12:20|PLANT BIOTECHNOLOGY^UGY. (2-C)^Doç. Dr. Hatice İKTEN
PERŞEMBE|4|10:30|12:20|INTRODUCTION TO^BIOINFORMATICS^(Tasarım-1)^Prof. Dr. Mehmet Aydın^AKBUDAK
PAZARTESİ|4|12:30|13:20|BİTİRME ÇALIŞMASI-I^Bölüm Öğretim Üyeleri
SALI|4|12:30|13:20|BİTİRME ÇALIŞMASI-I^Bölüm Öğretim Üyeleri
PAZARTESİ|4|13:30|15:20|HAYVANLARDA^HASTALIKLARA KARŞI^GENETİK DAYANIKLILIK^(1-C)^Prof. Dr. Hasan MEYDAN^(Seçmeli Ders)
SALI|4|13:30|15:20|HAYVAN FİZYOLOJİSİ^(4-F)^Prof. Dr. Fetih GÜLYÜZ^(Seçmeli Ders)
ÇARŞAMBA|4|13:30|15:20|RECOMBINANT DNA^TECHNIQUES^(1-B)^Prof. Dr. Mehmet Aydın^AKBUDAK
PERŞEMBE|4|13:30|15:20|GÜBRELER VE^GÜBRELEME^(Başak Amfi)^Prof. Dr. Mustafa^KAPLAN^(Seçmeli Ders)
PERŞEMBE|4|13:30|15:20|ÜREME^BİYOLOJİSİ VE^YAPAY^TOHUMLAMA^(Portakal Amfi)^Prof. Dr. Fetih^GÜLYÜZ^(Seçmeli Ders)
PAZARTESİ|4|15:30|17:20|ÜREME BİYOLOJİSİ VE^YAPAY TOHUMLAMA^UYG.(4-E)^Prof. Dr. Fetih GÜLYÜZ^(Seçmeli Ders)
SALI|4|15:30|17:20|HAYVANLARDA^HASTALIKLARA KARŞI^GENETİK DAYANIKLILIK^UYG. (1-B)^Prof. Dr. Hasan MEYDAN^(Seçmeli Ders)
PERŞEMBE|4|15:30|17:20|GÜBRELER VE^GÜBRELEME^UYG. (Başak Amfi)^Prof. Dr. Mustafa KAPLAN^(Seçmeli Ders)
        """.trimIndent())),
        Dataset(ClassSchedules.AGRICULTURAL_STRUCTURES_DEPARTMENT, 22, rows("""
PERŞEMBE|1|08:30|10:20|İNGİLİZCE-I^(Başak Amfi)^Öğr. Gör. Burçak AKINCI
CUMA|1|08:30|10:20|KİMYA^(Başak Amfi)^Dr. Öğr. Üyesi Kudret^AKPINAR
ÇARŞAMBA|1|10:30|12:20|MATEMATİK-I^(Başak Amfi)^Arş. Gör. Dr. Mehmet^CİCİMEN
PERŞEMBE|1|10:30|12:20|ATATÜRK İLKELERİ VE^İNKILAP TARİHİ-I^(Başak Amfi)^Öğr. Gör. Murat BOZ
CUMA|1|10:30|12:20|TÜRK DİLİ-I^(3-B)^Öğr. Gör. Sinan ORUÇOĞLU
PAZARTESİ|1|13:30|15:20|BOTANİK^(Yonca Amfi)^Doç. Dr. Orhan ÜNAL
SALI|1|13:30|15:20|FİZİK-I^(Başak Amfi)^Öğr. Gör. Dr. Nigar ALATA
ÇARŞAMBA|1|13:30|15:20|METEOROLOJİ^(4-D)^Prof. Dr. Ahmet KURUNÇ
PERŞEMBE|1|13:30|15:20|TARIMSAL YAPILAR VE^SULAMAYA GİRİŞ^(4-D)^Prof. Dr. Ahmet KURUNÇ
PAZARTESİ|1|15:30|17:20|BOTANİK^UYG. (Mikroskop Lab.)^Doç. Dr. Orhan ÜNAL
SALI|1|15:30|17:20|BİTKİ FİZYOLOJİSİ^(Başak Amfi)^Dr. Öğr. Üyesi Deniz HAZAR^(Seçmeli Ders)
ÇARŞAMBA|1|16:30|17:20|KARİYER PLANLAMA^(Portakal Amfi)^Arş. Gör. Dr. Begüm POLAT
PAZARTESİ|2|08:30|10:20|ENTOMOLOJİ^(2-B)^Arş. Gör. Dr. Hilal Şule^TOSUN^(Seçmeli Ders)
SALI|2|08:30|10:20|TEKNİK RESİM^(Tasarım-I)^Prof. Dr. Kenan^BÜYÜKTAŞ
ÇARŞAMBA|2|08:30|10:20|TARIM EKONOMİSİ^(2-A)^Doç. Dr. Yavuz TAŞCIOĞLU^(Seçmeli Ders)
PERŞEMBE|2|08:30|10:20|BAHÇE BİTKİLERİ^(2-A)^Prof. Dr. Ersin POLAT^(Seçmeli Ders)
CUMA|2|08:30|10:20|HAYVAN YETİŞTİRME VE^BESLEME^(Yonca Amfi)^Öğr. Gör. Dr. Nilgün YAPICI^(Seçmeli Ders)
PAZARTESİ|2|10:30|12:20|MÜHENDİSLİK^MATEMATİĞİ^(1-B)^Prof. Dr. Dursun BÜYÜKTAŞ
SALI|2|10:30|12:20|TEKNİK RESİM^(Tasarım-1)^Prof. Dr. Kenan^BÜYÜKTAŞ
ÇARŞAMBA|2|10:30|12:20|KIRSAL YERLEŞİM TEKNİĞİ^(1-C)^Arş. Gör. Dr. Begüm POLAT^(Seçmeli Ders)
PERŞEMBE|2|10:30|12:20|BAHÇE BİTKİLERİ^UYG.^Prof. Dr. Ersin POLAT^(Seçmeli Ders)
CUMA|2|10:30|12:20|HAYVAN YETİŞTİRME VE^BESLEME^UYG. (Yonca Amfi)^Öğr. Gör. Dr. Nilgün YAPICI^(Seçmeli Ders)
SALI|2|13:30|15:20|İSTATİSTİK^(4-B)^Prof. Dr. Burak^KARACAÖREN
ÇARŞAMBA|2|13:30|15:20|TAHIL BAKLAGİL VE YEM^BİTKİLERİ YETİŞTİRİCİLİĞİ^(3-B)^Doç. Dr. Safinaz ELMASULU^(Seçmeli Ders)
PERŞEMBE|2|13:30|15:20|MÜHENDİSLİK^MATEMATİĞİ^UYG. (1-C)^Prof. Dr. Dursun BÜYÜKTAŞ
PAZARTESİ|2|15:30|17:20|STATİK^(2-C)^Doç. Dr. Nefise Yasemin^TEZCAN
SALI|2|15:30|17:20|BİTKİ FİZYOLOJİSİ^(Başak Amfi)^Dr. Öğr. Üyesi Deniz^HAZAR^(Seçmeli Ders)
ÇARŞAMBA|2|15:30|17:20|TAHIL BAKLAGİL VE YEM^BİTKİLERİ YETİŞTİRİCİLİĞİ^UYG. (3-B)^Doç. Dr. Safinaz ELMASULU^(Seçmeli Ders)
PERŞEMBE|2|15:30|17:20|UZAKTAN ALGILAMA VE^COĞRAFİ BİLGİ^SİSTEMLERİ İLKELERİ^(4-F)^Dr. Öğr. Üyesi Gülçin Ece^ASLAN
PAZARTESİ|3|08:30|10:20|KIRSAL ALANDA SU^GETİRME^(4-A)^Doç. Dr. Nefise Yasemin^TEZCAN^(Seçmeli Ders)
PERŞEMBE|3|08:30|10:20|AKIŞKANLAR MEKANİĞİ^VE HİDROLİK^(4-D)^Prof. Dr. Ahmet KURUNÇ
PAZARTESİ|3|10:30|12:20|TOPRAK VE SU YAPILARI^(Tasarım-I)^Doç. Dr. Bekir Sıtkı KARATAŞ^(Seçmeli Ders)
SALI|3|10:30|12:20|ZEMİN MEKANİĞİ^(1-B)^Dr. Öğr. Üyesi Gülçin Ece^ASLAN^(Seçmeli Ders)
ÇARŞAMBA|3|10:30|12:20|ARAZİ^TOPLULAŞTIRMASI^(4-C)^Dr. Öğr. Üyesi Gülçin Ece^ASLAN
PERŞEMBE|3|10:30|12:20|AKIŞKANLAR MEKANİĞİ^VE HİDROLİK^UYG. (4-D)^Prof. Dr. Ahmet KURUNÇ
SALI|3|13:30|15:20|BETONARME^(1-C)^Doç. Dr. Nefise Yasemin^TEZCAN^(Seçmeli Ders)
ÇARŞAMBA|3|13:30|15:20|TOPRAK BİTKİ^ATMOSFER SİSTEMİ^(4-F)^Prof. Dr. Harun KAMAN
PERŞEMBE|3|13:30|15:20|MÜHENDİSLİK^ÖLÇMELERİ^(Tasarım-I)^Doç. Dr. Cihan KARACA
PAZARTESİ|3|15:30|17:20|TARIM TOPRAKLARININ^KİRLENMESİ^(3-B)^Prof. Dr. Şule ORMAN^(Seçmeli Ders)
ÇARŞAMBA|3|15:30|17:20|ARAZİ^TOPLULAŞTIRMASI^UyYG. (4-C)^Dr. Öğr. Üyesi Gülçin Ece^ASLAN
PERŞEMBE|3|15:30|17:20|MÜHENDİSLİK^ÖLÇMELERİ^UYG. (Tasarım-I)^Doç. Dr. Cihan KARACA
SALI|4|08:30|10:20|SU KAYNAKLARININ^PLANLANMASI^(2-C)^Prof. Dr. Dursun BÜYÜKTAŞ^(Seçmeli Ders)
PERŞEMBE|4|08:30|10:20|SERA TASARIMI^UYG. (Tarımsal Yap. Ve Sul.^Böl. Topl. Sal)^Doç. Dr. Nefise Yasemin^TEZCAN
PAZARTESİ|4|10:30|12:20|PROJE HAZIRLAMA VE^DEĞERLENDİRME^TEKNİĞİ^(2-C)^Arş. Gör. Dr. Begüm POLAT^(Seçmeli Ders)
SALI|4|10:30|12:20|SERA TASARIMI^(Tarımsal Yap. Ve Sul. Böl.^Topl. Sal)^Doç. Dr. Nefise Yasemin^TEZCAN
ÇARŞAMBA|4|10:30|12:20|SULAMA SİSTEMLERİNİN^TASARIMI^UYG. (2-D)^Prof. Dr. Dursun BÜYÜKTAŞ
PERŞEMBE|4|10:30|12:20|DRENAJ SİSTEMLERİNİN^TASARIMI^(1-C)^Prof. Dr. Dursun BÜYÜKTAŞ
CUMA|4|10:30|12:20|DRENAJ SİSTEMLERİNİN^TASARIMI^UYG.(Tasarım-II)^Prof. Dr. Dursun BÜYÜKTAŞ
PAZARTESİ|4|13:30|15:20|SULAMA SİSTEMLERİNİN^TASARIMI^(1-B)^Prof. Dr. Dursun BÜYÜKTAŞ
SALI|4|13:30|15:20|SULAMA MAKİNALARI^(Portakal Amfi)^Prof. Dr. Davut KARAYEL^(Seçmeli Ders)
ÇARŞAMBA|4|13:30|15:20|TARIMSAL YAPILARIN^TASARIMI^(4-C)^Prof. Dr. Kenan BÜYÜKTAŞ
PERŞEMBE|4|13:30|15:20|GÜBRELER VE^GÜBRELEME^(Başak Amfi)^Prof. Dr. Mustafa KAPLAN^(Seçmeli Ders)
CUMA|4|13:30|15:20|BİTİRME ÇALIŞMASI-I^(Bölüm öğretim üyeleri)^(Seçmeli Ders)
SALI|4|15:30|17:20|TERMODİNAMİK^(Portakal Amfi)^Dr. Öğr. Üyesi Sefai BİLGİN^(Seçmeli Ders)
ÇARŞAMBA|4|15:30|17:20|TARIMSAL YAPILARIN^TASARIMI^(Bil. Lab.)^Prof. Dr. Kenan BÜYÜKTAŞ
PERŞEMBE|4|15:30|17:20|GÜBRELER VE^GÜBRELEME^UYG. (Başak Amfi)^Prof. Dr. Mustafa KAPLAN^(Seçmeli Ders)
CUMA|4|15:30|17:20|TARIMSAL YAPILAR VE^SULAMA^UYGULAMALARI-I^(Uygulamaların yerleri bölüm^öğretim üyeleri tarafından^belirlenecektir)^(Seçmeli Ders)
        """.trimIndent())),
        Dataset(ClassSchedules.FIELD_CROPS_DEPARTMENT, 26, rows("""
PAZARTESİ|1|08:30|10:20|TARLA BİTKİLERİNE^GİRİŞ-I^(2-D)^Prof. Dr. Hüseyin ÇANCI
SALI|1|08:30|10:20|FİZİK-I^(3-B)^Öğr. Gör. Dr. Fatih PERİNÇEK
CUMA|1|08:30|10:20|TÜRK DİLİ-I^(3-B)^Öğr. Gör. Sinan ORUÇOĞLU
SALI|1|10:30|12:20|BOTANİK^(Başak Amfi)^Prof. Dr. Candan AYKURT
ÇARŞAMBA|1|10:30|12:20|MATEMATİK-I^(Başak Amfi)^Arş. Gör. Dr. Mehmet^CİCİMEN
PERŞEMBE|1|10:30|12:20|ATATÜRK İLKELERİ VE^İNKILAP TARİHİ-I^(Başak Amfi)^Öğr. Gör. Murat BOZ
CUMA|1|10:30|12:20|KİMYA^(Başak Amfi)^Doç. Dr. Naciye ERKAN
PAZARTESİ|1|10:30|11:20|KARİYER PLANLAMA^(2-D)^Doç. Dr. Safinaz ELMASULU
PAZARTESİ|1|13:30|15:20|METEOROLOJİ^(4-E)^Doç. Dr. Bekir Sıtkı KARATAŞ
SALI|1|13:30|15:20|BOTANİK^UYG. (Mikroskop Lab.)^Prof. Dr. Candan AYKURT
SALI|1|15:30|17:20|EKONOMİ^(2-D)^Arş. Gör. Dr. Miray KALAYCI
PERŞEMBE|1|15:30|17:20|İNGİLİZCE-I^(3-B)^Öğr. Gör. Seda AKSUNGUR
PAZARTESİ|2|08:30|10:20|ENTOMOLOJİ^(2-B)^Arş. Gör. Dr. Hilal Şule^TOSUN
SALI|2|08:30|10:20|BİYOKİMYA^(Başak Amfi)^Prof. Dr. Mehmet KARACA
ÇARŞAMBA|2|08:30|10:20|TARIM EKONOMİSİ^(4-D)^Arş. Gör. Dr. Merve^YILMAZ^(Seçmeli Ders)
PERŞEMBE|2|08:30|10:20|GIDA TEKNOLOJİSİ^VE GIDA^GÜVENLİĞİ^(4-B)^Öğr. Gör. Dr. Emrah^EROĞLU^(Seçmeli Ders)
PERŞEMBE|2|08:30|10:20|HAYVAN^YETİŞTİRME VE^BESLEME^(Yonca Amfi)^Öğr. Gör. Dr. Nilgün^YAPICI^(Seçmeli Ders)
PAZARTESİ|2|10:30|12:20|BİTKİ FİZYOLOJİSİ^(4-B)^Prof. Dr. Hüseyin ÇANCI
SALI|2|10:30|12:20|BAHÇE BİTKİLERİ^(4-B)^Doç. Dr. Adem DOĞAN
ÇARŞAMBA|2|10:30|12:20|MİKROBİYOLOJİ^(4-B)^Prof. Dr. Hüseyin BASIM^(Seçmeli Ders)
PERŞEMBE|2|10:30|12:20|GIDA^TEKNOLOJİSİ VE^GIDA GÜVENLİĞİ^UYG.(4-B)^Öğr. Gör. Dr. Emrah^EROĞLU^(Seçmeli Ders)
PERŞEMBE|2|10:30|12:20|HAYVAN^YETİŞTİRME VE^BESLEME^UYG. (Yonca Amfi)^Öğr. Gör. Dr. Nilgün^YAPICI^(Seçmeli Ders)
CUMA|2|10:30|12:20|İSTATİSTİK^(4-B)^Prof. Dr. M. Ziya^FIRAT
PAZARTESİ|2|13:30|15:20|SERİN İKLİM TAHILLARI^(2-A)^Doç. Dr. Mehmet TEKİN
SALI|2|13:30|15:20|FİDE VE FİDANCILIK^TEKNİĞİ^(1-A)^Prof. Dr. Nafiye ÜNAL^(Seçmeli Ders)
ÇARŞAMBA|2|13:30|16:20|MÜHENDİSLİK^MEKANİĞİ^(Portakal Amfi)^Prof. Dr. Davut KARAYEL
PAZARTESİ|2|15:30|17:20|SERİN İKLİM TAHILLARI^UYG. (2-A)^Doç. Dr. Mehmet TEKİN
SALI|2|15:30|17:20|TERMODİNAMİK^(Portakal Amfi)^Dr. Öğr. Üyesi Sefai BİLGİN
PERŞEMBE|2|15:30|17:20|BAHÇE BİTKİLERİ^UYG.^(4-D)^Doç. Dr. Adem DOĞAN
PAZARTESİ|3|08:30|10:20|BİLGİSAYAR DESTEKLİ^TASARIM^(Enf. Böl. Lab. 5)^Doç. Dr. Nuri ÇAĞLAYAN
SALI|3|08:30|10:20|AKILLI TARIM^TEKNOLOJİLERİ^(4-B)^Prof. Dr. Mehmet TOPAKCI
PERŞEMBE|3|09:30|12:20|BİTKİ HASTALIKLARI VE^ZARARLILARI İLE^MÜCADELE YÖNTEMLERİ^(2-B)^Prof. Dr. Fedai ERLER^(Seçmeli Ders)
PAZARTESİ|3|10:30|12:20|BİLGİSAYAR DESTEKLİ^TASARIM^UYG. (Enf. Böl. Lab. 5)^Doç. Dr. Nuri ÇAĞLAYAN
ÇARŞAMBA|3|10:30|12:20|ORGANİK TARIM^(Tarla Bitkileri Toplantı^Salonu)^Öğr. Gör. Safinaz ELMASULU^(2021 ve Öncesi Öğrenciler^İçin)
PAZARTESİ|3|13:30|15:20|BİTKİ ISLAHININ TEMEL^İLKELERİ^(4-D)^Prof. Dr. M. İlhan ÇAĞIRGAN
ÇARŞAMBA|3|13:30|15:20|BİTKİ GEN KAYNAKLARI^(4-B)^Doç. Dr. Bilal AYDINOĞLU
PERŞEMBE|3|13:30|15:20|SİLAJ YAPIM^TEKNOLOJİSİ^(2-B)^Doç. Dr. Bilal AYDINOĞLU^(Seçmeli Ders)
CUMA|3|13:30|15:20|TARLA BİTKİLERİ^ÜRETİM^UYGULAMALARI-I^(Uygulamaların yerleri bölüm^öğretim üyeleri tarafından^belirlenecektir)^(Seçmeli Ders)
PAZARTESİ|3|15:30|17:20|BİTKİ ISLAHININ TEMEL^İLKELERİ^UYG. (4-D)^Prof. Dr. M. İlhan ÇAĞIRGAN
SALI|3|15:30|17:20|ÇEŞİT GELİŞTİRME^(2-C)^Prof. Dr. Mehmet ARSLAN^(Seçmeli Ders)
ÇARŞAMBA|3|15:30|17:20|MOLEKÜLER BİYOLOJİ^(Başak Amfi)^Prof. Dr. Mehmet KARACA
PERŞEMBE|3|15:30|17:20|TOHUMLUK BİLİMİ VE^TEKNOLOJİSİ^(1-A)^Prof. Dr. Mehmet ARSLAN
CUMA|3|15:30|17:20|LABORATUVAR^UYGULAMALARI^(Uygulamaların yerleri bölüm^öğretim üyeleri tarafından^belirlenecektir)^(Seçmeli Ders)
PAZARTESİ|4|08:30|10:20|TARLA BİTKİLERİ^HASTALIKLARI^UYG. (Mikroskop Lab.)^Prof. Dr. Özer ÇALIŞ^(Seçmeli Ders)
PAZARTESİ|4|08:30|10:20|GENETİK MÜHENDİSLİĞİ^(1-C)^Prof. Dr. Mehmet^KARACA^(Seçmeli Ders)
SALI|4|08:30|10:20|YEM BİTKİLERİ VE^ISLAHI^(2-B)^Doç. Dr. Bilal AYDINOĞLU
ÇARŞAMBA|4|08:30|10:20|TIBBİ VE AROMATİK^BİTKİLER^(1-A)^Prof. Dr. Kenan TURGUT
PERŞEMBE|4|08:30|10:20|YEMEKLİK BAKLAGİLLER^VE ISLAHI^(3-A)^Prof. Dr. Hüseyin ÇANCI
CUMA|4|08:30|10:20|BİTKİ DOKU^KÜLTÜRLERİ^(3-A)^Prof. Dr. Kenan TURGUT
PAZARTESİ|4|10:30|12:20|TARLA BİTKİLERİ^HASTALIKLARI^(1-A)^Prof. Dr. Özer ÇALIŞ^(Seçmeli Ders)
PAZARTESİ|4|10:30|12:20|GENETİK^MÜHENDİSLİĞİ^UYG.(1-C)^Prof. Dr. Mehmet^KARACA^(Seçmeli Ders)
SALI|4|10:30|12:20|YEM BİTKİLERİ VE^ISLAHI^UYG. (2-B)^Doç. Dr. Bilal AYDINOĞLU
ÇARŞAMBA|4|10:30|12:20|TIBBİ VE AROMATİK BİTKİLER^UYG.(1-A)^Prof. Dr. Kenan TURGUT
PERŞEMBE|4|10:30|12:20|YEMEKLİK BAKLAGİLLER VE^ISLAHI^UYG. (3-A)^Prof. Dr. Hüseyin ÇANCI
CUMA|4|10:30|12:20|BİTKİ DOKU^KÜLTÜRLERİ^UYG.(3-A)^Prof. Dr. Kenan^TURGUT
PAZARTESİ|4|12:30|13:20|BİTİRME ÇALIŞMASI-I^(Bölüm öğretim üyeleri)^(Seçmeli Ders)
SALI|4|12:30|13:20|BİTİRME ÇALIŞMASI-I^(Bölüm öğretim üyeleri)^(Seçmeli Ders)
PAZARTESİ|4|13:30|15:20|MOLEKÜLER MARKERLERİN BİTKİ^ISLAHINDA KULLANIMI^(2-B)^Prof. Dr. Bülent UZUN
SALI|4|13:30|15:20|NİŞASTA ŞEKER^BİTKİLERİ VE ISLAHI^(3-A)^Prof. Dr. Engin YOL
PERŞEMBE|4|13:30|15:20|GÜBRELER VE^GÜBRELEME^(Başak Amfi)^Prof. Dr. Mustafa^KAPLAN
PERŞEMBE|4|13:30|15:20|BİTKİ ISLAHI^YÖNTEMLERİ^(2. Blok Toplantı^Salonu)^Prof. Dr. Bülent^UZUN
PAZARTESİ|4|15:30|17:20|TOPRAKSIZ TARIM VE BİTKİ BESLEME^(4-B)^Doç. Dr. İlker SÖNMEZ^(Seçmeli Ders)
SALI|4|15:30|17:20|NİŞASTA ŞEKER^BİTKİLERİ VE ISLAHI^UYG. (3-A)^Prof. Dr. Engin YOL
ÇARŞAMBA|4|15:30|17:20|BAHÇE BİTKİLERİNDE TOPRAKSIZ^YETİŞTİRİCİLİK^(4-D)^Prof. Dr. Nafiye ÜNAL^(Seçmeli Ders)
PERŞEMBE|4|15:30|17:20|GÜBRELER VE^GÜBRELEME^UYG. (Başak^Amfi) Prof. Dr.^Mustafa^KAPLAN
PERŞEMBE|4|15:30|17:20|BİTKİ ISLAHI^YÖNTEMLERİ^UYG.(2. Blok^Toplantı Salonu)^Prof. Dr. Bülent^UZUN
        """.trimIndent())),
        Dataset(ClassSchedules.SOIL_SCIENCE_DEPARTMENT, 30, rows("""
SALI|1|08:30|10:20|FİZİK-I^(3-B)^Öğr. Gör. Dr. Fatih PERİNÇEK
ÇARŞAMBA|1|08:30|10:20|MATEMATİK-I^(Yonca Amfi)^Arş. Gör. Dr. Mehmet^CİCİMEN
PERŞEMBE|1|08:30|10:20|İNGİLİZCE-I^(Başak Amfi)^Öğr. Gör. Burçak AKINCI
CUMA|1|08:30|10:20|TÜRK DİLİ-I^(3-B)^Öğr. Gör. Sinan ORUÇOĞLU
SALI|1|10:30|12:20|BOTANİK^(Başak Amfi)^Prof. Dr. Candan AYKURT
PERŞEMBE|1|10:30|12:20|ATATÜRK İLKELERİ VE^İNKILAP TARİHİ-I^(Başak Amfi)^Öğr. Gör. Murat BOZ
CUMA|1|10:30|12:20|KİMYA^(Başak Amfi)^Doç. Dr. Naciye ERKAN
SALI|1|13:30|15:20|BOTANİK^UYG. (Mikroskop Lab.)^Prof. Dr. Candan AYKURT
PAZARTESİ|1|15:30|17:20|TOPRAK BİLİMİ VE BİTKİ^BESLEMEYE GİRİŞ^(3-C)^Doç. Dr. İlker UZ
ÇARŞAMBA|1|15:30|17:20|METEOROLOJİ^(4-B)^Doç. Dr. Cihan KARACA
SALI|1|16:30|17:20|KARİYER PLANLAMA^(3-C)^Doç Dr. İsmail Emrah TAVALI
PAZARTESİ|2|08:30|10:20|BAHÇE BİTKİLERİ^(Yonca Amfi)^Prof. Dr. Şadiye GÖZLEKÇİ
ÇARŞAMBA|2|08:30|10:20|TARIM EKONOMİSİ^(2-A)^Doç. Dr. Yavuz^TAŞCIOĞLU^(Seçmeli Ders)
PERŞEMBE|2|08:30|10:20|GIDA TEKNOLOJİSİ VE GIDA^GÜVENLİĞİ^(4-B)^Öğr. Gör. Dr. Emrah EROĞLU^(Seçmeli Ders)
PERŞEMBE|2|08:30|10:20|HAYVAN YETİŞTİRME^VE BESLEME^(Yonca Amfi)^Öğr. Gör. Dr. Nilgün^YAPICI^(Seçmeli Ders)
CUMA|2|08:30|10:20|BİYOKİMYA^(1-A)^Dr. Öğr. Üyesi^İlhami TOZLU
SALI|2|09:30|12:20|TOPRAK BOZULMASI^VE ISLAHI^(3-C)^Prof. Dr. Erdem YILMAZ
PAZARTESİ|2|10:30|12:20|BAHÇE BİTKİLERİ^UYG.^(Yonca Amfi)^Prof. Dr. Şadiye GÖZLEKÇİ
ÇARŞAMBA|2|10:30|12:20|İSTATİSTİK^(3-A)^Prof. Dr. Mehmet Ziya^FIRAT^(Seçmeli Ders)
PERŞEMBE|2|10:30|12:20|GIDA TEKNOLOJİSİ VE GIDA^GÜVENLİĞİ^UYG.(4-B)^Öğr. Gör. Dr. Emrah EROĞLU^(Seçmeli Ders)
PERŞEMBE|2|10:30|12:20|HAYVAN YETİŞTİRME^VE BESLEME^UYG.(Yonca Amfi)^Öğr. Gör. Dr. Nilgün^YAPICI^(Seçmeli Ders)
PAZARTESİ|2|13:30|15:20|BİTKİ FİZYOLOJİSİ^(Başak Amfi)^Dr. Öğr. Üyesi İnci TOLAY
SALI|2|13:30|15:20|TARIMSAL^ÜRETİMDE İŞ^SAĞLIĞI VE^GÜVENLİĞİ^(4-E)^Prof. Dr. Murad^ÇANAKCI^(Seçmeli Ders)
ÇARŞAMBA|2|13:30|15:20|TAHIL BAKLAGİL VE^YEM BİTKİLERİ^YETİŞTİRİCİLİĞİ^(3-B)^Doç. Dr. Safinaz ELMASULU^(Seçmeli Ders)
PERŞEMBE|2|13:30|15:20|MİNEROLOJİ VE PETROGRAFİ^(4-A)^Prof. Dr. Erdem YILMAZ
PAZARTESİ|2|15:30|17:20|ENTOMOLOJİ^(Başak Amfi)^Arş. Gör. Dr. Hilal Şule^TOSUN^(Seçmeli Ders)
ÇARŞAMBA|2|15:30|17:20|TAHIL BAKLAGİL VE^YEM BİTKİLERİ^YETİŞTİRİCİLİĞİ^UYG. (3-B)^Doç. Dr. Safinaz ELMASULU^(Seçmeli Ders)
PERŞEMBE|2|15:30|17:20|MİNEROLOJİ VE PETROGRAFİ^UYG. (4-A)^Prof. Dr. Erdem YILMAZ
PAZARTESİ|3|08:30|10:20|ÖRTÜALTI HASTALIK VE^ZARARLILARI^(4-B)^Doç. Dr. Hakan FİDAN^(Seçmeli Ders)
SALI|3|08:30|10:20|TOPLUMSAL DUYARLILIK^VE KATKI^(1-C)^Doç. Dr. Hüseyin UYSAL
ÇARŞAMBA|3|08:30|10:20|ÖRTÜALTI SEBZE^YETİŞTİRİCİLİĞİ^UYG.^Prof. Dr. Ersin POLAT^(Uygulama yeri dersin öğretim^üyesinden öğrenilecektir)^(Seçmeli Ders)
CUMA|3|08:30|10:20|BAHÇE BİTKİLERİ^HASTALIKLARI^UYG. (Mikroskop Lab.)^Prof. Dr. Hüseyin BASIM^(Seçmeli Ders)
PAZARTESİ|3|10:30|12:20|ÖRTÜALTI^SEBZE^YETİŞTİRİCİLİĞİ^(Başak Amfi)^Prof. Dr. Ersin^POLAT^(Seçmeli Ders)
PAZARTESİ|3|10:30|12:20|BAHÇE^BİTKİLERİ^HASTALIKLARI^(2-B)^Prof. Dr. Hüseyin^BASIM^(Seçmeli Ders)
SALI|3|10:30|12:20|TOPRAK BİLİMİ VE BİTKİ^BESLEMEDE MESLEKİ^İNGİLİZCE-I^(2-C)^Dr. Öğr. Üyesi İnci TOLAY^(Seçmeli Ders)
PERŞEMBE|3|10:30|12:20|FİDE VE FİDANCILIK^TEKNİĞİ^(1-B)^Doç. Dr. Sevinç ATEŞ^(Seçmeli Ders)
PAZARTESİ|3|13:30|15:20|SEBZE ISLAHI^(3-A)^Prof. Dr. Ahmet Naci ONUS^(Seçmeli Ders)
SALI|3|13:30|15:20|TOPRAK FİZİĞİ^(3-B)^Prof. Dr. Erdem YILMAZ
ÇARŞAMBA|3|13:30|15:20|ARAZİ KULLANIMI VE^HAVZA YÖNETİMİ^(3-A)^Doç. Dr. Sevda ALTUNBAŞ^BERİTANLI
PERŞEMBE|3|13:30|15:20|TOPRAK KİMYASI^(3-A)^Prof. Dr. Sahriye SÖNMEZ
PAZARTESİ|3|15:30|17:20|TARIM TOPRAKLARININ^KİRLENMESİ^(3-B)^Prof. Dr. Şule ORMAN
SALI|3|15:30|17:20|TOPRAK FİZİĞİ^UYG. (Lab.)^Prof. Dr. Erdem YILMAZ
ÇARŞAMBA|3|15:30|17:20|ARAZİ KULLANIMI VE^HAVZA YÖNETİMİ^UYG.(3. Blok Top. Sal.)^Doç. Dr. Sevda ALTUNBAŞ^BERİTANLI
PERŞEMBE|3|15:30|17:20|TOPRAK KİMYASI^UYG. (Lab.)^Prof. Dr. Sahriye SÖNMEZ
PAZARTESİ|4|08:30|10:20|ORGANİK TARIMDA^TOPRAK VERİMLİLİĞİ VE^GÜBRELEME^(3-A)^Doç. Dr. İlker SÖNMEZ
SALI|4|08:30|10:20|TOPRAK GENETİĞİ^(3-A)^Doç. Dr. Sevda ALTUNBAŞ^BERİTANLI
ÇARŞAMBA|4|09:30|12:20|KÜLTÜR BİTKİLERİNİN^GÜBRELENMESİ^(3-C)^Prof. Dr. Şule ORMAN
SALI|4|10:30|12:20|TOPRAK GENETİĞİ^UYG.(3-A)^Doç. Dr. Sevda ALTUNBAŞ^BERİTANLI
PERŞEMBE|4|10:30|12:20|AVRUPA BİRLİĞİ ORTAK^TARIM POLİTİKASI^(Portakal Amfi)^Prof. Dr. Cengiz SAYIN^(Seçmeli Ders)
CUMA|4|10:30|12:20|BİTİRME ÇALIŞMASI-I^(Bölüm öğretim üyeleri)
ÇARŞAMBA|4|13:30|15:20|TOPRAK BİYOLOJİSİ VE^BİYOKİMYASI^(3-C)^Doç. Dr. İlker UZ
PERŞEMBE|4|13:30|15:20|GÜBRELER VE^GÜBRELEME^(Başak Amfi)^Prof. Dr. Mustafa KAPLAN
CUMA|4|13:30|15:20|TOPRAK BİLİMİ VE BİTKİ^BESLEME^UYGULAMALARI–I^(Uygulamaların yerleri bölüm^öğretim üyeleri tarafından^belirlenecektir)^(Seçmeli Ders)
PAZARTESİ|4|15:30|17:20|TOPRAKSIZ TARIM VE^BİTKİ BESLEME^(4-B)^Doç. Dr. İlker SÖNMEZ
ÇARŞAMBA|4|15:30|17:20|TOPRAK BİYOLOJİSİ VE^BİYOKİMYASI^UYG.(3-C)^Doç. Dr. İsmail Emrah^TAVALI
PERŞEMBE|4|15:30|17:20|GÜBRELER VE^GÜBRELEME^UYG. (Başak Amfi)^Prof. Dr. Mustafa KAPLAN
CUMA|4|15:30|17:20|TOPRAK BİLİMİ VE BİTKİ^BESLEMEDE SEKTÖR^UYGULAMALARI–I^(Uygulamaların yerleri bölüm^öğretim üyeleri tarafından^belirlenecektir)^(Seçmeli Ders)
        """.trimIndent())),
        Dataset(ClassSchedules.ANIMAL_SCIENCE_DEPARTMENT, 34, rows("""
CUMA|1|08:30|09:20|KARİYER PLANLAMA^(Tasarım-1)^Doç. Dr. Eymen DEMİR
PAZARTESİ|1|08:30|10:20|BOTANİK^(Başak Amfi)^Doç. Dr. Orhan ÜNAL
ÇARŞAMBA|1|08:30|10:20|MATEMATİK-I^(Yonca Amfi)^Arş. Gör. Dr. Mehmet^CİCİMEN
PERŞEMBE|1|08:30|10:20|İNGİLİZCE-I^(Başak Amfi)^Öğr. Gör. Burçak AKINCI
PAZARTESİ|1|10:30|12:20|BOTANİK^UYG. (Mikroskop Lab.)^Doç. Dr. Orhan ÜNAL
PERŞEMBE|1|10:30|12:20|ATATÜRK İLKELERİ VE^İNKILAP TARİHİ-I^(Başak Amfi)^Öğr. Gör. Murat BOZ
CUMA|1|10:30|12:20|KİMYA^(Başak Amfi)^Doç. Dr. Naciye ERKAN
SALI|1|13:30|15:20|FİZİK-I^(Başak Amfi)^Öğr. Gör. Dr. Nigar ALATA
ÇARŞAMBA|1|13:30|15:20|ZOOLOJİ^(Başak Amfi)^Doç. Dr. Mustafa YAVUZ
CUMA|1|14:30|16:20|TÜRK DİLİ-I^(Başak Amfi)^Öğr. Gör. Sinan ORUÇOĞLU
SALI|1|15:30|17:20|ZOOTEKNİYE GİRİŞ^(1-A)^Dr. Öğr. Üyesi Emine ŞAHİN^SEMERCİ
ÇARŞAMBA|1|15:30|17:20|ZOOLOJİ^UYG. (Mikroskop Lab.)^Doç. Dr. Mustafa YAVUZ
PAZARTESİ|2|08:30|10:20|BAHÇE^BİTKİLERİ^(Yonca amfi)^Prof. Dr. Şadiye^GÖZLEKÇİ^(Seçmeli Ders)
PAZARTESİ|2|08:30|10:20|KÜMES^HAYVANLARI^YETİŞTİRME^UYGULAMALARI I^(3-C)^Prof. Dr. Doğan^NARİNÇ^(Seçmeli Ders)
SALI|2|08:30|10:20|BİLGİSAYAR DESTEKLİ ÇİZİM^(Enf. Böl. Lab. 5)^Arş. Gör. Dr. İsmail BOYAR
PERŞEMBE|2|08:30|10:20|İSTATİSTİK^(4-F)^Prof. Dr. Burak^KARACAÖREN
CUMA|2|08:30|10:20|LABARATUVAR ANALİZ^TEKNİKLERİ^(2-B)^Doç. Dr. Cüneyt DİNÇER
ÇARŞAMBA|2|09:30|12:20|HAYVAN BESLEME^BİYOKİMYASI^(4-E)^Dr. Öğr. Üyesi Firdevs^KORKMAZ TURGUD
PAZARTESİ|2|10:30|12:20|BAHÇE^BİTKİLERİ^UYG.^(Yonca Amfi)^Prof. Dr. Şadiye^GÖZLEKÇİ^(Seçmeli Ders)
PAZARTESİ|2|10:30|12:20|KÜMES^HAYVANLARI^YETİŞTİRME^UYGULAMALARI I^UYG. (3-C)^Prof. Dr. Doğan^NARİNÇ^(Seçmeli Ders)
SALI|2|10:30|12:20|BİLGİSAYAR DESTEKLİ ÇİZİM^UYG. (Enf. Böl. Lab. 5)^Arş. Gör. Dr. İsmail BOYAR
PERŞEMBE|2|10:30|12:20|TARIM EKONOMİSİ^(4-A)^Prof. Dr. Handan AKÇAÖZ
CUMA|2|10:30|12:20|LABARATUVAR ANALİZ^TEKNİKLERİ^UYG.(2-B)^Doç. Dr. Cüneyt DİNÇER
PAZARTESİ|2|13:30|15:20|TARIMSAL YAPILAR^(3-B)^Doç. Dr. Nefise Yasemin^TEZCAN^(Seçmeli Ders)
SALI|2|13:30|15:20|FİDE VE FİDANCILIK TEKNİĞİ^(1-A)^Prof. Dr. Nafiye ÜNAL^(Seçmeli Ders)
ÇARŞAMBA|2|13:30|15:20|TAHIL BAKLAGİL VE YEM^BİTKİLERİ^YETİŞTİRİCİLİĞİ^(3-B)^Doç. Dr. Safinaz ELMASULU^(Seçmeli Ders)
PERŞEMBE|2|13:30|15:20|BİTKİSEL ÜRETİMDE ARI^KULLANIMI^(4-F)^Prof. Dr. Fehmi GÜREL
CUMA|2|13:30|17:20|ZOOTEKNİ^UYGULAMALARI^(Uygulamaların yerleri^bölüm öğretim üyeleri^tarafından belirlenecektir)
PAZARTESİ|2|15:30|17:20|ENTOMOLOJİ^(Başak Amf)^Arş. Gör. Dr. Hilal Şule^TOSUN^(Seçmeli Ders)
ÇARŞAMBA|2|15:30|17:20|TAHIL BAKLAGİL VE YEM^BİTKİLERİ^YETİŞTİRİCİLİĞİ^UYG. (3-B)^Doç. Dr. Safinaz ELMASULU^(Seçmeli Ders)
PAZARTESİ|3|08:30|10:20|RUMİNANT BESLEME^(Portakal Amfi)^Dr. Öğr. Üyesi Firdevs^KORKMAZ TURGUD
SALI|3|08:30|10:20|AKILLI TARIM^TEKNOLOJİLERİ^(4-B)^Prof. Dr. Mehmet TOPAKCI^(Seçmeli Ders)
ÇARŞAMBA|3|08:30|10:20|KANATLI HAYVAN^YETİŞTİRME-I^(3-B)^Prof. Dr. Doğan NARİNÇ
PERŞEMBE|3|08:30|10:20|YEMLER BİLGİSİ VE^TEKNOLOJİSİ^(3-C)^Dr. Öğr. Üyesi Firdevs^KORKMAZ TURGUD
CUMA|3|08:30|10:20|HAYVANCILIKTA^MEKANİZASYON^(4-F)^Prof. Dr. Can ERTEKİN^(Seçmeli Ders)
PAZARTESİ|3|10:30|12:20|RUMİNAN BESLEME^UYG.(Portakal Amfi)^Dr. Öğr. Üyesi Firdevs^KORKMAZ TURGUD
SALI|3|10:30|12:20|TOPLUMSAL DUYARLILIK^VE KATKI^(1-C)^Doç. Dr. Hüseyin UYSAL
ÇARŞAMBA|3|10:30|12:20|KANATLI HAYVAN^YETİŞTİRME-I^UYG. (3-B)^Prof. Dr. Doğan NARİNÇ
PERŞEMBE|3|10:30|12:20|YEMLER BİLGİSİ VE^TEKNOLOJİSİ^UYG. (3-C)^Dr. Öğr. Üyesi Firdevs^KORKMAZ TURGUD
PAZARTESİ|3|13:30|15:20|HAYVAN YETİŞTİRME^İLKELERİ^(3-C)^Doç. Dr. Eymen DEMİR^(Seçmeli Ders)
SALI|3|13:30|15:20|HAYVAN FİZYOLOJİSİ^(4-F)^Prof. Dr. Fetih GÜLYÜZ
PERŞEMBE|3|13:30|15:20|ÜREME BİYOLOJİSİ VE^YAPAY TOHUMLAMA^(Portakal Amfi)^Prof. Dr. Fetih GÜLYÜZ
ÇARŞAMBA|3|14:30|17:20|BİTKİ HASTALIKLARI VE^ZARARLILARI İLE^MÜCADELE YÖNTEMLERİ^(2-B)^Prof. Dr. Özer ÇALIŞ^(Seçmeli Ders)
PAZARTESİ|3|15:30|17:20|ÜREME BİYOLOJİSİ VE^YAPAY TOHUMLAMA^UYG. (4-E)^Prof. Dr. Fetih GÜLYÜZ
SALI|3|15:30|17:20|ARAŞTIRMA^YAZIM VE^SUNUM^TEKNİKLERİ^(3-B) Prof. Dr.^Orhan^ÖZÇATALBAŞ^(Seçmeli Ders)
SALI|3|15:30|17:20|HAYVAN^DAVRANIŞLARI^(4-F)^Prof. Dr. Doğan^NARİNÇ^(Seçmeli Ders)
PERŞEMBE|3|15:30|17:20|KARMA YEM^MİKROSKOPİSİ^(2-A)^Dr. Öğr. Üyesi Firdevs^KORKMAZ TURGUD^(Seçmeli Ders)
SALI|4|08:30|10:20|KANATLI HAYVAN BESLEME^(2-D)^Dr. Öğr. Üyesi Firdevs KORKMAZ^TURGUD
ÇARŞAMBA|4|08:30|10:20|HAYVANCILIKTA^MOLEKÜLER^GENETİK^(4-A)^Dr. Öğr. Üyesi Emine^ŞAHİN SEMERCİ
CUMA|4|08:30|10:20|HAYVANSAL ÜRETİMDE^YENİLİKÇİ^YAKLAŞIMLAR^(3-C)^Prof. Dr. Doğan NARİNÇ
PAZARTESİ|4|10:30|12:20|VARYASYON^KAYNAKLARI^(3-B)^Prof. Dr. Burak^KARACAÖREN^(Seçmeli Ders)
ÇARŞAMBA|4|10:30|12:20|HAYVAN^BARINAKLARI^(4-A)^Prof. Dr. Kenan^BÜYÜKTAŞ^(Seçmeli Ders)
CUMA|4|10:30|12:20|HAYVANSAL ÜRETİMDE^YENİLİKÇİ^YAKLAŞIMLAR^UYG.(3-C)^Prof. Dr. Doğan NARİNÇ
PAZARTESİ|4|12:30|13:20|BİTİRME ÇALIŞMASI^(Bölüm öğretim üyeleri)
PAZARTESİ|4|13:30|15:20|HAYVAN ISLAHI^(Portakal Amfi)^Doç. Dr. Aşkın GALİÇ
SALI|4|13:30|15:20|HAYVANSAL^EKOLOJİ^(3-C)^Prof. Dr. Doğan^NARİNÇ^(Seçmeli Ders)
SALI|4|13:30|15:20|RASYON^HESAPLAMA^(Zootekni Böl. Top.^Sal.)^Dr. Öğr. Üyesi^FİRDEVS KORKMAZ^TURGUD^(Seçmeli Ders)
ÇARŞAMBA|4|13:30|15:20|GIDA TEKNOLOJİSİ VE^GIDA GÜVENLİĞİ^(1-C)^Öğr. Gör. Dr. Emrah^EROĞLU
PERŞEMBE|4|13:30|15:20|SİLAJ YAPIM^TEKNOLOJİSİ^(2-B)^Doç. Dr. Bilal AYDINOĞLU^(Seçmeli Ders)
PAZARTESİ|4|15:30|17:20|HAYVAN ISLAHI^UYG. (Portakal Amfi)^Doç. Dr. Aşkın GALİÇ
SALI|4|15:30|17:20|KANATLI HAYVAN BESLEME^UYG. (Tasarım-1)^Dr. Öğr. Üyesi Firdevs KORKMAZ^TURGUD
ÇARŞAMBA|4|15:30|17:20|GIDA TEKNOLOJİSİ^VE GIDA GÜVENLİĞİ^UYG.(1-C)^Öğr. Gör. Dr. Emrah^EROĞLU
        """.trimIndent()))
    )

    private val schedulesByDepartment: Map<String, List<ClassSchedule>> by lazy {
        val classYears = listOf(
            ClassSchedules.FIRST_YEAR,
            ClassSchedules.SECOND_YEAR,
            ClassSchedules.THIRD_YEAR,
            ClassSchedules.FOURTH_YEAR
        )
        datasets.associate { dataset ->
            val schedules = classYears.mapIndexed { index, classYear ->
                val year = index + 1
                ClassSchedule(
                    faculty = ClassSchedules.AGRICULTURE_FACULTY,
                    department = dataset.department,
                    classYear = classYear,
                    academicYear = ACADEMIC_YEAR,
                    term = TERM,
                    updatedAt = UPDATED_AT,
                    sourcePage = dataset.firstPage + index,
                    sourceUrl = SOURCE_URL,
                    entries = dataset.rows
                        .filter { it.year == year }
                        .flatMap { expand(it.entry) }
                        .sortedWith(
                            compareBy<ScheduleEntry>(
                                { it.day.ordinal },
                                { it.startTime },
                                { it.courseName },
                                { it.section.orEmpty() }
                            )
                        )
                )
            }
            dataset.department to schedules
        }
    }

    fun schedulesFor(department: String?): List<ClassSchedule>? =
        schedulesByDepartment[department]
}
