package com.good4.schedule.domain

/** Timetables for the 2026–2027 fall term, transcribed from the faculty's published files. */
internal object LiteratureSchedules {
    private const val GERMAN_SOURCE =
        "https://webis.akdeniz.edu.tr/uploads/1015/announcement/2026-2027%20Guz%20Alman%20Dili%20ve%20Edebiyat%C4%B1%20Haftal%C4%B1k%20Ders%20Program%C4%B1%20%281%29.xlsx"
    private const val ARCHAEOLOGY_SOURCE =
        "https://webis.akdeniz.edu.tr/uploads/1015/announcement/Arkeoloji..pdf"
    private const val GEOGRAPHY_SOURCE =
        "https://webis.akdeniz.edu.tr/uploads/1015/announcement/Co%C3%9Frafya.pdf"
    private const val ANCIENT_GREEK_SOURCE =
        "https://webis.akdeniz.edu.tr/uploads/1015/announcement/Eski%20Yunan%20Dili.pdf"
    private const val LATIN_SOURCE =
        "https://webis.akdeniz.edu.tr/uploads/1015/announcement/Latin%20Dili.pdf"

    private val classPeriods = listOf(
        "08:30" to "09:20",
        "09:30" to "10:20",
        "10:30" to "11:20",
        "11:30" to "12:20",
        "13:30" to "14:20",
        "14:30" to "15:20",
        "15:30" to "16:20",
        "16:30" to "17:20",
        "18:30" to "19:20",
        "19:30" to "20:20",
        "20:30" to "21:20"
    )

    /** Expands a merged timetable block into its individual 50-minute class-hour rows. */
    private fun block(
        day: ScheduleDay,
        start: String,
        end: String,
        code: String,
        name: String,
        instructor: String = "",
        classroom: String = "",
        note: String? = null,
        courseType: String = "Ders"
    ): List<ScheduleEntry> = classPeriods
        .filter { (periodStart, periodEnd) -> periodStart >= start && periodEnd <= end }
        .map { (periodStart, periodEnd) ->
            ScheduleEntry(
                day = day,
                startTime = periodStart,
                endTime = periodEnd,
                courseCode = code,
                courseName = name,
                instructor = instructor,
                classroom = classroom,
                courseType = courseType,
                note = note
            )
        }

    private fun schedule(
        department: String,
        classYear: String,
        sourceUrl: String,
        vararg blocks: List<ScheduleEntry>
    ) = ClassSchedule(
        faculty = ClassSchedules.LITERATURE_FACULTY,
        department = department,
        classYear = classYear,
        academicYear = "2026-2027",
        term = "Güz Yarıyılı",
        updatedAt = "2026-2027 Güz",
        sourcePage = 1,
        sourceUrl = sourceUrl,
        entries = blocks.flatMap { it }
    )

    private val monday = ScheduleDay.MONDAY
    private val tuesday = ScheduleDay.TUESDAY
    private val wednesday = ScheduleDay.WEDNESDAY
    private val thursday = ScheduleDay.THURSDAY
    private val friday = ScheduleDay.FRIDAY

    // Alman Dili ve Edebiyatı — Friday ADE 345 explicitly prints 14:30–16:20,
    // although its merged spreadsheet cell is positioned under 09:30–12:20.
    val german = listOf(
        schedule(
            ClassSchedules.GERMAN_LANGUAGE_AND_LITERATURE_DEPARTMENT,
            ClassSchedules.FIRST_YEAR,
            GERMAN_SOURCE,
            block(monday, "10:30", "12:20", "ADE 125", "Yazılı Anlatım", "Dr. Manuela VOLZ"),
            block(monday, "13:30", "15:20", "ADE 123", "Einführung in die Literaturwissenschaft", "Dr. Öğr. Üyesi Nihal KUBİLAY PINAR"),
            block(tuesday, "08:30", "10:20", "ATA 101", "Atatürk İlkeleri ve İnkılap Tarihi I"),
            block(tuesday, "15:30", "17:20", "YBD 101", "Yabancı Dil I"),
            block(wednesday, "10:30", "12:20", "TDB 101", "Türk Dili I"),
            block(wednesday, "13:30", "15:20", "ADE 131", "Einführung in die Kulturwissenschaft", "DAAD"),
            block(thursday, "10:30", "12:20", "ADE 121", "Dilbilgisi", "Dr. Manuela VOLZ"),
            block(thursday, "13:30", "15:20", "ADE 129", "Dil Bilimine Giriş", "Doç. Dr. Kemal DEMİR"),
            block(friday, "10:30", "12:20", "ADE 127", "Diskursive Landeskunde I", "DAAD")
        ),
        schedule(
            ClassSchedules.GERMAN_LANGUAGE_AND_LITERATURE_DEPARTMENT,
            ClassSchedules.SECOND_YEAR,
            GERMAN_SOURCE,
            block(monday, "10:30", "12:20", "ADE 247", "Anlambilim", "Doç. Dr. Kemal DEMİR"),
            block(monday, "13:30", "15:20", "ADE 241", "Orta Çağ-Barok Deutsche Kulturgeschichte", "Öğr. Gör. Tolgahan ERGÜN"),
            block(tuesday, "10:30", "12:20", "ADE 263", "Sözcük ve Yapı Bilgisi", "Doç. Dr. Kemal DEMİR"),
            block(tuesday, "13:30", "15:20", "ADE 243", "Orta Çağ-Barok Dönemi Alman Edebiyatı", "Dr. Öğr. Üyesi Sevgi ARKILIÇ SONGÖREN"),
            block(tuesday, "15:30", "17:20", "ADE 249", "Çeviribilim", "Dr. Manuela VOLZ"),
            block(tuesday, "18:30", "21:20", "PFE 201", "Eğitime Giriş", courseType = "Formasyon"),
            block(wednesday, "13:30", "15:20", "ADE 255", "Yazma Teknikleri", "Öğr. Gör. Tolgahan ERGÜN"),
            block(wednesday, "15:30", "17:20", "ADE 259", "Alman Dili Tarihi", "Öğr. Gör. Tolgahan ERGÜN"),
            block(wednesday, "18:30", "21:20", "PFE 203", "Eğitim Psikolojisi", courseType = "Formasyon"),
            block(thursday, "08:30", "10:20", "ADE 257", "Dil ve Düşünce", "Doç. Dr. Kemal DEMİR"),
            block(thursday, "10:30", "12:20", "ADE 253", "Bilimsel Araştırma Yöntemleri I", "Öğr. Gör. Tolgahan ERGÜN"),
            block(friday, "08:30", "10:20", "ADE 245", "Alman Edebiyatında Kısa Edebi Metinler", "Dr. Öğr. Üyesi Hatice GENÇ"),
            block(friday, "10:30", "12:20", "ADE 251", "Uygulamalı Dilbilgisi I", "Dr. Öğr. Üyesi Salih Özenici")
        ),
        schedule(
            ClassSchedules.GERMAN_LANGUAGE_AND_LITERATURE_DEPARTMENT,
            ClassSchedules.THIRD_YEAR,
            GERMAN_SOURCE,
            block(monday, "08:30", "10:20", "ADE 301", "Karşılaştırmalı Dilbilgisi I"),
            block(monday, "15:30", "17:20", "ADE 347", "Göstergebilim", "Doç. Dr. Kemal DEMİR"),
            block(monday, "18:30", "21:20", "PFE 301", "Eğitimde Ölçme ve Değerlendirme", courseType = "Formasyon"),
            block(tuesday, "08:30", "10:20", "ADE 351", "Eleştirel Medya Okur-Yazarlığı", "Doç. Dr. Kemal DEMİR"),
            block(tuesday, "10:30", "12:20", "ADE 349", "Alan Çevirisi", "Dr. Öğr. Üyesi Salih Özenici"),
            block(tuesday, "15:30", "17:20", "ADE 343", "Klasik-Romantik Dönemi Alman Edebiyatı", "Dr. Öğr. Üyesi Sevgi ARKILIÇ SONGÖREN"),
            block(wednesday, "08:30", "10:20", "ADE 353", "Postmigrantische Diskurse", "Doç. Dr. Kemal DEMİR"),
            block(wednesday, "10:30", "12:20", "ADE 357", "Wissenschaftliches Arbeiten und KI", "Öğr. Gör. Tolgahan ERGÜN"),
            block(thursday, "08:30", "10:20", "ADE 359", "Weimarer Klassik-Romantik & Nationalismus", "Öğr. Gör. Tolgahan ERGÜN"),
            block(thursday, "10:30", "12:20", "ADE 341", "Edebiyat ve Kadın", "Dr. Öğr. Üyesi Hatice GENÇ"),
            block(thursday, "13:30", "15:20", "ADE 355", "Çocuk ve Gençlik Edebiyatına Giriş", "Dr. Öğr. Üyesi Sevgi ARKILIÇ SONGÖREN"),
            block(thursday, "18:30", "21:20", "PFE 303", "Rehberlik ve Özel Eğitim", courseType = "Formasyon"),
            block(friday, "14:30", "16:20", "ADE 345", "Alman Edebiyatında Roman I", "Dr. Öğr. Üyesi Nihal KUBİLAY PINAR", note = "Excel hücresindeki açık saat: 14:30–16:20")
        ),
        schedule(
            ClassSchedules.GERMAN_LANGUAGE_AND_LITERATURE_DEPARTMENT,
            ClassSchedules.FOURTH_YEAR,
            GERMAN_SOURCE,
            block(monday, "09:30", "12:20", "ADE 443", "Edebiyat Eleştirisi Kuram ve Yöntemleri I", "Dr. Öğr. Üyesi Nihal KUBİLAY PINAR"),
            block(monday, "13:30", "15:20", "ADE 455", "Mesleki Dil Uygulaması I", "Dr. Manuela VOLZ"),
            block(tuesday, "13:30", "15:20", "ADE 447", "Uygulamalı Dilbilim I", "Doç. Dr. Kemal DEMİR"),
            block(tuesday, "15:30", "17:20", "ADE 457", "Yabancı Dil Öğretiminde Yaklaşımlar I", "Dr. Öğr. Üyesi Salih Özenici"),
            block(wednesday, "13:30", "15:20", "ADE 449", "Alman Modern Edebiyatı", "Dr. Öğr. Üyesi Hatice GENÇ"),
            block(wednesday, "15:30", "17:20", "ADE 451", "Savaş Sonrası Alman Edebiyatı Eser Örnekleri ve Analizi", "Dr. Öğr. Üyesi Sevgi ARKILIÇ SONGÖREN"),
            block(thursday, "10:30", "12:20", "ADE 459", "Eleştirel Söylem Analizi", "Dr. Öğr. Üyesi Salih Özenici"),
            block(thursday, "13:30", "15:20", "ADE 461", "Dil Kültür Etkileşimi", "Dr. Öğr. Üyesi Hatice GENÇ"),
            block(thursday, "15:30", "17:20", "ADE 445", "Erster und Zweiter Weltkrieg Kulturgeschichte", "Öğr. Gör. Tolgahan ERGÜN"),
            block(friday, "08:30", "10:20", "ADE 421", "Bilişsel Dilbilmi"),
            block(friday, "14:30", "16:20", "ADE 463", "Bitirme Tezi I")
        )
    )

    // Arkeoloji
    val archaeology = listOf(
        schedule(
            ClassSchedules.ARCHAEOLOGY_DEPARTMENT,
            ClassSchedules.FIRST_YEAR,
            ARCHAEOLOGY_SOURCE,
            block(monday, "10:30", "12:20", "ARK 151", "Arkeolojiye Giriş I", "Prof. Dr. Taner Korkut", "D-409", "Zorunlu"),
            block(tuesday, "08:30", "10:20", "ATA 101", "Atatürk İlkeleri ve İnkılap Tarihi I", "Öğr. Gör. Murat Boz", "D-108", "Zorunlu"),
            block(tuesday, "10:30", "12:20", "YDB 101", "İngilizce I", "Öğr. Gör. Demet Tekinay", "D-108", "Zorunlu"),
            block(wednesday, "13:30", "15:20", "ARK 155", "Tarihi Coğrafya I", "Prof. Dr. Gül Işın", "D-408", "Zorunlu"),
            block(thursday, "10:30", "12:20", "ARK 153", "Mitoloji ve İkonografi I", "Öğr. Gör. Süleyman Bulut", "D-408", "Zorunlu"),
            block(thursday, "13:30", "15:20", "ARK 159", "Prehistorya I", "Prof. Dr. Burçin Erdoğu", "D-409", "Zorunlu"),
            block(thursday, "15:30", "17:20", "TDB 101", "Türk Dili I", "Öğr. Gör. Betül Bilgin", note = "Zorunlu"),
            block(friday, "11:30", "12:20", "KPD 101", "Kariyer Planlaması", "Arş. Gör. Dr. Uygar Ozan Usanmaz", "D-408", "Zorunlu"),
            block(friday, "13:30", "15:20", "ARK 157", "Arkeolojik Araştırma ve Belgeleme Yöntemleri", "Dr. Öğr. Üyesi Şevket Aktaş", "D-409", "Zorunlu")
        ),
        schedule(
            ClassSchedules.ARCHAEOLOGY_DEPARTMENT,
            ClassSchedules.SECOND_YEAR,
            ARCHAEOLOGY_SOURCE,
            block(tuesday, "13:30", "15:20", "ARK 255", "Erken Demirçağ Seramiği", "Doç. Dr. Erkan Dündar", "D-409", "Zorunlu"),
            block(tuesday, "15:30", "17:20", "ARK 261", "Akdeniz ve Ege Prehistoryası", "Prof. Dr. Burçin Erdoğu", "D-408", "Seçmeli"),
            block(wednesday, "08:30", "10:20", "ARK 257", "Akhamenid Sanatı", "Öğr. Gör. Süleyman Bulut", "D-409", "Seçmeli"),
            block(wednesday, "10:30", "12:20", "ARK 259", "Antik Yunan’da Sosyal Yaşam", "Öğr. Gör. Süleyman Bulut", "D-410", "Seçmeli"),
            block(wednesday, "13:30", "15:20", "ARK 263", "Sualtı Arkeolojisi I", "Doç. Dr. Erkan Dündar", "D-409", "Seçmeli"),
            block(wednesday, "15:30", "17:20", "ARK 251", "Prehistorya II", "Prof. Dr. Burçin Erdoğu", "D-409", "Zorunlu"),
            block(thursday, "15:30", "17:20", "ARK 253", "Protohistorya ve Önasya II", "Doç. Dr. Salih Gökhan Tiryaki", "D-408", "Zorunlu")
        ),
        schedule(
            ClassSchedules.ARCHAEOLOGY_DEPARTMENT,
            ClassSchedules.THIRD_YEAR,
            ARCHAEOLOGY_SOURCE,
            block(tuesday, "10:30", "12:20", "ARK 363", "Eski Mısır Uygarlığı", "Prof. Dr. Gül Işın", "D-408", "Seçmeli"),
            block(tuesday, "13:30", "15:20", "ARK 355", "Eski Hellence I", "Doç. Dr. Mehmet Ertan Yıldız", "D-407", "Zorunlu"),
            block(tuesday, "15:30", "17:20", "ARK 351", "Klasik Dönem Plastiği", "Doç. Dr. Erkan Dündar", "D-409", "Zorunlu"),
            block(wednesday, "10:30", "12:20", "ARK 355", "Eski Hellence I", "Doç. Dr. Mehmet Ertan Yıldız", "D-407", "Zorunlu"),
            block(wednesday, "13:30", "15:20", "ARK 370", "Toplumsal Duyarlılık ve Katkı Projeleri", "Doç. Dr. Salih Gökhan Tiryaki", "D-407", "Seçmeli"),
            block(wednesday, "15:30", "17:20", "ARK 365", "Demirçağ Mezopotamya Kültürleri", "Doç. Dr. Salih Gökhan Tiryaki", "D-407", "Seçmeli"),
            block(thursday, "10:30", "12:20", "ARK 357", "Klasik Dönem Seramiği", "Prof. Dr. Gül Işın", "D-407", "Zorunlu"),
            block(thursday, "13:30", "15:20", "ARK 367", "Portre Sanatının Gelişimi", "Arş. Gör. Dr. Yaşar Arlı", "D-410", "Seçmeli"),
            block(thursday, "15:30", "17:20", "ARK 361", "Bilimsel Araştırma Yöntemleri", "Arş. Gör. Dr. Uygar Ozan Usanmaz", "D-407", "Seçmeli"),
            block(friday, "08:30", "10:20", "ARK 353", "Arkaik-Hellenistik Dönem Numizmatiği", "Öğr. Gör. Süleyman Bulut", "D-409", "Zorunlu"),
            block(friday, "10:30", "12:20", "ARK 359", "Klasik Dönem Mimarisi", "Dr. Öğr. Üyesi Şevket Aktaş", "D-409", "Zorunlu")
        ),
        schedule(
            ClassSchedules.ARCHAEOLOGY_DEPARTMENT,
            ClassSchedules.FOURTH_YEAR,
            ARCHAEOLOGY_SOURCE,
            block(monday, "08:30", "10:20", "ARK 457", "Teorik Bitirme Çalışması", "Prof. Dr. Taner Korkut / Doç. Dr. Erkan Dündar", note = "Seçmeli"),
            block(monday, "10:30", "12:20", "ARK 453", "Roma Mimarisi", "Prof. Dr. Nevzat Çevik", "D-410", "Zorunlu"),
            block(monday, "13:30", "15:20", "ARK 455", "Antik Resim Sanatı", "Prof. Dr. Taner Korkut", "D-410", "Zorunlu"),
            block(wednesday, "13:30", "15:20", "ARK 463", "Mozaik Sanatı", "Prof. Dr. Taner Korkut", "D-410", "Seçmeli"),
            block(thursday, "08:30", "10:20", "ARK 457", "Teorik Bitirme Çalışması", "Arş. Gör. Dr. Yaşar Arlı", note = "Seçmeli"),
            block(thursday, "13:30", "15:20", "ARK 459", "Arkeoloji Tarihi", "Doç. Dr. Salih Gökhan Tiryaki", "D-408", "Seçmeli"),
            block(friday, "08:30", "10:20", "ARK 461", "Kuramsal Müzecilik", "Arş. Gör. Dr. Yaşar Arlı", "D-410", "Seçmeli"),
            block(friday, "10:30", "12:20", "ARK 451", "Roma Dönemi Plastiği", "Arş. Gör. Dr. Yaşar Arlı", "D-410", "Zorunlu"),
            block(friday, "15:30", "17:20", "ARK 457", "Teorik Bitirme Çalışması", "Prof. Dr. Nevzat Çevik / Prof. Dr. Gül Işın / Prof. Dr. Burçin Erdoğu / Doç. Dr. Salih Gökhan Tiryaki / Dr. Öğr. Üyesi Şevket Aktaş / Öğr. Gör. Süleyman Bulut / Arş. Gör. Dr. Uygar Ozan Usanmaz", note = "Seçmeli")
        )
    )

    // Coğrafya
    val geography = listOf(
        schedule(
            ClassSchedules.GEOGRAPHY_DEPARTMENT,
            ClassSchedules.FIRST_YEAR,
            GEOGRAPHY_SOURCE,
            block(monday, "13:30", "16:20", "COG 1109", "Klimatoloji I", "Prof. Dr. Tuncer DEMİR", "Derslik 110"),
            block(tuesday, "08:30", "10:20", "ATA 101", "Atatürk İlkeleri ve İnkılâp Tarihi I", "Öğr. Gör. Murat BOZ", "108"),
            block(tuesday, "10:30", "12:20", "YBD 101", "İngilizce I", "Öğr. Gör. Demet TEKİNAY", "108"),
            block(tuesday, "13:30", "15:20", "COG 1101", "Coğrafya’ya Giriş", "Prof. Dr. Cemali SARI", "Derslik 109"),
            block(wednesday, "09:30", "12:20", "COG 1107", "Kartografya", "Dr. Öğr. Üyesi Çağlar ÇAKIR", "Derslik 108"),
            block(wednesday, "13:30", "14:20", "COG 1107", "Kartografya", "Dr. Öğr. Üyesi Çağlar ÇAKIR", "Derslik 108"),
            block(wednesday, "14:30", "16:20", "COG 111", "Nüfus Coğrafyası", "Doç. Dr. Halil HADİMLİ", "Derslik 110"),
            block(thursday, "09:30", "12:20", "COG 1105", "Jeomorfolojiye Giriş", "Prof. Dr. Tuncer DEMİR", "Derslik 108"),
            block(thursday, "13:30", "15:20", "TDB 101", "Türk Dili I", "Öğr. Gör. Betül BİLGİN", "Prof. Dr. Erol GÜNGÖR Amfisi"),
            block(thursday, "15:30", "17:20", "COG 1103", "Coğrafi İstatistik I", "Dr. Öğr. Üyesi M. Tahsin ŞAHİN", "Derslik 108")
        ),
        schedule(
            ClassSchedules.GEOGRAPHY_DEPARTMENT,
            ClassSchedules.SECOND_YEAR,
            GEOGRAPHY_SOURCE,
            block(monday, "13:30", "16:20", "COG 1205", "Coğrafi Bilgi Sistemleri I", "Doç. Dr. Ebru AKKÖPRÜ", "Derslik 109"),
            block(tuesday, "10:30", "12:20", "COG 1211", "Şehir Coğrafyası", "Doç. Dr. Halil HADİMLİ", "Derslik 109"),
            block(tuesday, "13:30", "15:20", "G1209", "Kültürel Coğrafya", "Prof. Dr. Mustafa ERTÜRK", "Derslik 108"),
            block(wednesday, "08:30", "12:20", "COG 1201", "Arazi Çalışması I", "Prof. Dr. Mustafa ERTÜRK", "Derslik 109"),
            block(wednesday, "13:30", "15:20", "COG 1213", "Ziraat Coğrafyası", "Prof. Dr. İhsan BULUT", "Derslik 109"),
            block(wednesday, "15:30", "17:20", "COG 1207", "Karst Jeomorfolojisi", "Dr. Öğr. Üyesi Çağlar ÇAKIR", "Derslik 109"),
            block(thursday, "10:30", "12:20", "COG 1215", "Anadolu’nun Tarihi Coğrafyası", "Doç. Dr. Halil HADİMLİ", "Derslik 109"),
            block(thursday, "13:30", "15:20", "COG 1203", "Beşeri Coğrafyada Araştırma Yöntemleri", "Dr. Öğr. Üyesi M. Tahsin ŞAHİN", "Derslik 109"),
            block(friday, "13:30", "15:20", "COG 1219", "Biyocoğrafya", "Arş. Gör. Dr. Emirhan BERBEROĞLU", "Derslik 109")
        ),
        schedule(
            ClassSchedules.GEOGRAPHY_DEPARTMENT,
            ClassSchedules.THIRD_YEAR,
            GEOGRAPHY_SOURCE,
            block(monday, "13:30", "16:20", "COG 1301", "Asya Coğrafyası", "Prof. Dr. Mustafa ERTÜRK", "Derslik 108"),
            block(tuesday, "09:30", "12:20", "COG 1311", "Turizm Coğrafyası", "Prof. Dr. Cemali SARI", "Derslik 110"),
            block(tuesday, "13:30", "16:20", "COG 1329", "Coğrafi Bilgi Sistemlerinde Mekansal Analizler", "Doç. Dr. Ebru AKKÖPRÜ", "Derslik 110"),
            block(wednesday, "10:30", "12:20", "COG 1307", "Deniz ve Kıyı Jeomorfolojisi", "Prof. Dr. Tuncer DEMİR", "Derslik 110"),
            block(wednesday, "15:30", "17:20", "COG 1325", "Türk Dünyası Coğrafyası", "Prof. Dr. İhsan BULUT", "Derslik 107"),
            block(thursday, "09:30", "12:20", "COG 1313", "Türkiye Fiziki Coğrafyası", "Dr. Öğr. Üyesi Çağlar ÇAKIR", "Derslik 110"),
            block(thursday, "15:30", "17:20", "COG 1309", "Kurak ve Yarıkurak Bölgeler Jeomorfolojisi", "Dr. Öğr. Üyesi Çağlar ÇAKIR", "Derslik 110"),
            block(friday, "10:30", "12:20", "COG 1303", "Bitki Coğrafyası", "Arş. Gör. Dr. Emirhan BERBEROĞLU", "Derslik 110"),
            block(friday, "13:30", "15:20", "COG 1315", "Arazi Çalışması III", "Dr. Öğr. Üyesi Çağlar ÇAKIR", "Derslik 110"),
            block(friday, "15:30", "17:20", "COG 1327", "Uygulamalı Klimatoloji", "Dr. Öğr. Üyesi Çağlar ÇAKIR", "Derslik 110")
        ),
        schedule(
            ClassSchedules.GEOGRAPHY_DEPARTMENT,
            ClassSchedules.FOURTH_YEAR,
            GEOGRAPHY_SOURCE,
            block(monday, "09:30", "12:20", "COG 1411", "Volkan Coğrafyası", "Doç. Dr. Ebru AKKÖPRÜ", "Derslik 107"),
            block(monday, "14:30", "17:20", "", "Türkiye Sanayi Coğrafyası", "Arş. Gör. Dr. Samet ALKAN", "107"),
            block(tuesday, "09:30", "12:20", "COG 1405", "Kent ve Bölge Planlaması", "Dr. Öğr. Üyesi M. Tahsin ŞAHİN", "Derslik 107"),
            block(tuesday, "14:30", "17:20", "COG 1407", "Türkiye Kıyı Bölgeleri", "Doç. Dr. Halil HADİMLİ", "Derslik 107"),
            block(wednesday, "09:30", "12:20", "COG 1401", "Afrika Coğrafyası", "Dr. Öğr. Üyesi M. Tahsin ŞAHİN", "107"),
            block(wednesday, "13:30", "15:20", "COG 1429", "Mesleki İngilizce", "Prof. Dr. Tuncer DEMİR", "107"),
            block(wednesday, "15:30", "17:20", "COG 1419", "Politik Coğrafya", "Prof. Dr. Mustafa ERTÜRK", "Derslik 108"),
            block(thursday, "09:30", "12:20", "COG 1409", "Uzaktan Algılama", "Doç. Dr. Ebru AKKÖPRÜ", "Derslik 107"),
            block(thursday, "13:30", "16:20", "COG 1421", "Türkiye Kuvaterner Coğrafyası", "Doç. Dr. Ebru AKKÖPRÜ", "Derslik 107"),
            block(thursday, "16:30", "17:20", "COG 1419", "Politik Coğrafya", "Prof. Dr. Mustafa ERTÜRK", "Derslik 108"),
            block(friday, "08:30", "10:20", "COG 1403", "Bitirme Çalışması I", "Prof. Dr. Tuncer DEMİR / Prof. Dr. İhsan BULUT / Prof. Dr. Mustafa ERTÜRK / Prof. Dr. Cemali SARI / Doç. Dr. Ebru AKKÖPRÜ / Doç. Dr. Halil HADİMLİ / Dr. Öğr. Üyesi Çağlar ÇAKIR / Dr. Öğr. Üyesi M. Tahsin ŞAHİN")
        )
    )

    // Eski Yunan Dili ve Edebiyatı — BD (bölüm dışı) rows are not class-year rows.
    val ancientGreek = listOf(
        schedule(
            ClassSchedules.ANCIENT_GREEK_DEPARTMENT,
            ClassSchedules.FIRST_YEAR,
            ANCIENT_GREEK_SOURCE,
            block(monday, "15:30", "17:20", "YDE 111", "Genel Arkeoloji", "Yaşar ARLI", "D-101"),
            block(tuesday, "08:30", "10:20", "ATA 101", "Atatürk İlkeleri ve İnkılâp Tarihi I", "Derya ÖGE SET", "D-102"),
            block(tuesday, "10:30", "12:20", "YBD 101", "İngilizce I", "Abdullah ARSLAN", "D-102"),
            block(tuesday, "15:30", "17:20", "YBD 103", "Almanca I", "Arzu AYDEMİR ÜMİT", "D-508"),
            block(wednesday, "08:30", "10:20", "YDE 107", "Arkaik ve Klasik Dönem Siyasi Tarihi", "Mustafa ADAK", "D-103"),
            block(wednesday, "10:30", "12:20", "YDE 105", "Eski Yunan Toplum Yapısı ve Günlük Yaşam", "Fatma AVCU", "D-102"),
            block(wednesday, "13:30", "15:20", "TDB 101", "Türk Dili I", "Betül BİLGİN", "Prof. Dr. Erol GÜNGÖR Amfisi"),
            block(wednesday, "15:30", "17:20", "YDE 101", "Eski Yunanca Gramer I", "Burak TAKMER", "D-104"),
            block(thursday, "10:30", "12:20", "YDE 109", "Yunan Mitolojisi", "Fatma AVCU", "D-103"),
            block(thursday, "13:30", "17:20", "YDE 103", "Eski Yunanca Alıştırmalar I", "Nihal TÜNER ÖNEN", "D-102"),
            block(friday, "10:30", "15:20", "YDE 101", "Eski Yunanca Gramer I", "Burak TAKMER", "D-102")
        ),
        schedule(
            ClassSchedules.ANCIENT_GREEK_DEPARTMENT,
            ClassSchedules.SECOND_YEAR,
            ANCIENT_GREEK_SOURCE,
            block(monday, "10:30", "12:20", "YDE 203", "Eski Yunanca Alıştırmalar III", "Burak TAKMER", "D-102"),
            block(monday, "13:30", "15:20", "YDE 201", "Eski Yunanca Gramer III", "Nuray GÖKALP", "D-102"),
            block(monday, "15:30", "17:20", "YDE 205", "Eski Yunan Edebiyatı Tarihi I", "Nuray GÖKALP", "D-102"),
            block(tuesday, "08:30", "10:20", "YDE 215", "Eskiçağ Anadolu Coğrafyası", "M. Ertan YILDIZ", "D-101"),
            block(tuesday, "10:30", "12:20", "YDE 201", "Eski Yunanca Gramer III", "Nuray GÖKALP", "D-103"),
            block(tuesday, "13:30", "15:20", "YDE 217", "Roma Toplum Yapısı ve Günlük Yaşam", "Asuman COŞKUN ABUAGLA", "D-104"),
            block(wednesday, "08:30", "12:20", "YDE 207", "Latince Gramer I", "Mehmet OKTAN", "D-101"),
            block(thursday, "08:30", "10:20", "YDE 201", "Eski Yunanca Gramer III", "Nuray GÖKALP", "D-103"),
            block(thursday, "10:30", "12:20", "YDE 203", "Eski Yunanca Alıştırmalar III", "Burak TAKMER", "D-104"),
            block(thursday, "13:30", "15:20", "YDE 207", "Latince Gramer I", "Mehmet OKTAN", "D-101"),
            block(friday, "08:30", "10:20", "YDE 211", "Roma Krallık ve Cumhuriyet Tarihi", "Fatih YILMAZ", "D-103"),
            block(friday, "10:30", "15:20", "YDE 209", "Latince Alıştırmalar I", "Mehmet OKTAN", "D-101"),
            block(friday, "15:30", "17:20", "YDE 221", "Klasik ve Hellenistik Felsefe Tarihi", "Fatih ONUR", "D-104")
        ),
        schedule(
            ClassSchedules.ANCIENT_GREEK_DEPARTMENT,
            ClassSchedules.THIRD_YEAR,
            ANCIENT_GREEK_SOURCE,
            block(monday, "08:30", "10:20", "YDE 321", "Nümizmatik I", "Süleyman BULUT", "D-101"),
            block(monday, "10:30", "15:20", "YDE 307", "Latince Gramer III", "Hüseyin UZUNOĞLU", "D-101"),
            block(monday, "15:30", "17:20", "YDE 313", "Latin Edebiyatı Tarihi I", "Asuman COŞKUN ABUAGLA", "D-104"),
            block(tuesday, "08:30", "10:20", "YDE 319", "Klasik Filoloji Bilim Tarihi I", "Fatih ONUR", "D-103"),
            block(tuesday, "13:30", "15:20", "YDE 307", "Latince Gramer III", "Hüseyin UZUNOĞLU", "D-103"),
            block(tuesday, "15:30", "17:20", "YDE 303", "Eski Yunan Edebiyatı Tarihi III", "Nuray GÖKALP", "D-101"),
            block(wednesday, "13:30", "15:20", "YDE 305", "Eski Yunan Epigrafisi I", "Eda AKYÜREK ŞAHİN", "D-102"),
            block(wednesday, "15:30", "17:20", "YDE 317", "Klasik Filoloji Özel Konular I", "Mustafa ADAK", "D-103"),
            block(thursday, "08:30", "12:20", "YDE 309", "Latince Alıştırmalar III", "Hüseyin UZUNOĞLU", "D-101"),
            block(thursday, "13:30", "15:20", "YDE 301", "Eski Yunan Dilinin Kaynakları I", "Burak TAKMER", "D-103"),
            block(friday, "13:30", "15:20", "YDE 305", "Eski Yunan Epigrafisi I", "Eda AKYÜREK ŞAHİN", "D-104"),
            block(friday, "15:30", "17:20", "YDE 301", "Eski Yunan Dilinin Kaynakları I", "Burak TAKMER", "D-102")
        ),
        schedule(
            ClassSchedules.ANCIENT_GREEK_DEPARTMENT,
            ClassSchedules.FOURTH_YEAR,
            ANCIENT_GREEK_SOURCE,
            block(monday, "08:30", "10:20", "YDE 405", "Bitirme Çalışması I", note = "M. Ertan Yıldız hariç bütün öğretim üyeleri"),
            block(monday, "10:30", "12:20", "YDE 405", "Bitirme Çalışması I", note = "Burak Takmer ve Hüseyin Uzunoğlu hariç bütün öğretim üyeleri"),
            block(tuesday, "10:30", "12:20", "YDE 403", "Eski Yunan Epigrafisi III", "Mustafa ADAK", "D-104"),
            block(tuesday, "13:30", "15:20", "YDE 411", "Eski Yunan Şiiri I", "Fatih ONUR", "D-102"),
            block(wednesday, "08:30", "12:20", "YDE 413", "Latin Dilinin Kaynakları I", "Asuman COŞKUN ABUGLA", "D-104"),
            block(wednesday, "13:30", "15:20", "YDE 403", "Eski Yunan Epigrafisi III", "Mustafa ADAK", "D-103"),
            block(wednesday, "15:30", "17:20", "YDE 421", "Metodoloji I", "Fatih ONUR", "D-101"),
            block(thursday, "08:30", "12:20", "YDE 401", "Eski Yunan Dilinin Kaynakları III", "Nihal TÜNER ÖNEN", "D-102"),
            block(thursday, "15:30", "17:20", "YDE 417", "Latin Edebiyatı Tarihi III", "Asuman COŞKUN ABUAGLA", "D-104"),
            block(friday, "08:30", "10:20", "YDE 405", "Bitirme Çalışması I", "Burak TAKMER / M. Ertan YILDIZ / Hüseyin UZUNOĞLU"),
            block(friday, "13:30", "17:20", "YDE 415", "Latin Epigrafisi I", "Fatih YILMAZ", "D-103")
        )
    )

    // Latin Dili ve Edebiyatı — BD (bölüm dışı) rows are not class-year rows.
    val latin = listOf(
        schedule(
            ClassSchedules.LATIN_LANGUAGE_AND_LITERATURE_DEPARTMENT,
            ClassSchedules.FIRST_YEAR,
            LATIN_SOURCE,
            block(monday, "15:30", "17:20", "LDE 111", "Genel Arkeoloji", "Yaşar ARLI", "D-101"),
            block(tuesday, "08:30", "10:20", "ATA 101", "Atatürk İlkeleri ve İnkılâp Tarihi I", "Derya ÖGE SET", "D-102"),
            block(tuesday, "10:30", "12:20", "YBD 101", "İngilizce I", "Abdullah ARSLAN", "D-102"),
            block(tuesday, "13:30", "15:20", "LDE 105", "Roma Toplum Yapısı ve Günlük Yaşam", "Asuman COŞKUN ABUAGLA", "D-104"),
            block(tuesday, "15:30", "17:20", "YBD 103", "Almanca I", "Arzu AYDEMİR ÜMİT", "D-508"),
            block(wednesday, "08:30", "12:20", "LDE 101", "Latince Gramer I", "Mehmet OKTAN", "D-101"),
            block(wednesday, "13:30", "15:20", "TDB 101", "Türk Dili I", "Betül BİLGİN", "Prof. Dr. Erol GÜNGÖR Amfisi"),
            block(thursday, "10:30", "12:20", "LDE 109", "Yunan Mitolojisi", "Fatma AVCU", "D-103"),
            block(thursday, "13:30", "15:20", "LDE 101", "Latince Gramer I", "Mehmet OKTAN", "D-101"),
            block(friday, "08:30", "10:20", "LDE 107", "Roma Krallık ve Cumhuriyet Tarihi", "Fatih YILMAZ", "D-103"),
            block(friday, "10:30", "15:20", "LDE 103", "Latince Alıştırmalar I", "Mehmet OKTAN", "D-101")
        ),
        schedule(
            ClassSchedules.LATIN_LANGUAGE_AND_LITERATURE_DEPARTMENT,
            ClassSchedules.SECOND_YEAR,
            LATIN_SOURCE,
            block(monday, "10:30", "15:20", "LDE 201", "Latince Gramer III", "Hüseyin UZUNOĞLU", "D-101"),
            block(monday, "15:30", "17:20", "LDE 205", "Latin Edebiyatı Tarihi I", "Asuman COŞKUN ABUAGLA", "D-104"),
            block(tuesday, "08:30", "10:20", "LDE 215", "Eskiçağ Anadolu Coğrafyası", "M. Ertan YILDIZ", "D-101"),
            block(tuesday, "13:30", "15:20", "LDE 201", "Latince Gramer III", "Hüseyin UZUNOĞLU", "D-103"),
            block(wednesday, "08:30", "10:20", "LDE 212", "Arkaik ve Klasik Dönem Siyasi Tarihi", "Mustafa ADAK", "D-103"),
            block(wednesday, "10:30", "12:20", "LDE 217", "Eski Yunan Toplum Yapısı ve Günlük Yaşam", "Fatma AVCU", "D-102"),
            block(wednesday, "15:30", "17:20", "LDE 207", "Eski Yunanca Gramer I", "Burak TAKMER", "D-104"),
            block(thursday, "08:30", "12:20", "LDE 203", "Latince Alıştırmalar III", "Hüseyin UZUNOĞLU", "D-101"),
            block(thursday, "13:30", "17:20", "LDE 209", "Eski Yunanca Alıştırmalar I", "Nihal TÜNER ÖNEN", "D-102"),
            block(friday, "10:30", "15:20", "LDE 207", "Eski Yunanca Gramer I", "Burak TAKMER", "D-102"),
            block(friday, "15:30", "17:20", "LDE 221", "Klasik ve Hellenistik Felsefe Tarihi", "Fatih ONUR", "D-104")
        ),
        schedule(
            ClassSchedules.LATIN_LANGUAGE_AND_LITERATURE_DEPARTMENT,
            ClassSchedules.THIRD_YEAR,
            LATIN_SOURCE,
            block(monday, "08:30", "10:20", "LDE 321", "Nümizmatik I", "Süleyman BULUT", "D-101"),
            block(monday, "10:30", "12:20", "LDE 309", "Eski Yunanca Alıştırmalar III", "Burak TAKMER", "D-102"),
            block(monday, "13:30", "15:20", "LDE 307", "Eski Yunanca Gramer III", "Nuray GÖKALP", "D-102"),
            block(monday, "15:30", "17:20", "LDE 313", "Eski Yunan Edebiyatı Tarihi I", "Nuray GÖKALP", "D-102"),
            block(tuesday, "08:30", "10:20", "LDE 319", "Klasik Filoloji Bilim Tarihi I", "Fatih ONUR", "D-103"),
            block(tuesday, "10:30", "12:20", "LDE 307", "Eski Yunanca Gramer III", "Nuray GÖKALP", "D-103"),
            block(wednesday, "08:30", "12:20", "LDE 301", "Latin Dilinin Kaynakları I", "Asuman COŞKUN ABUAGLA", "D-104"),
            block(wednesday, "15:30", "17:20", "LDE 317", "Klasik Filoloji Özel Konular I", "Mustafa ADAK", "D-103"),
            block(thursday, "08:30", "10:20", "LDE 307", "Eski Yunanca Gramer III", "Nuray GÖKALP", "D-103"),
            block(thursday, "10:30", "12:20", "LDE 309", "Eski Yunanca Alıştırmalar III", "Burak TAKMER", "D-104"),
            block(thursday, "15:30", "17:20", "LDE 303", "Latin Edebiyatı Tarihi III", "Asuman COŞKUN ABUAGLA", "D-104"),
            block(friday, "13:30", "17:20", "LDE 305", "Latin Epigrafisi I", "Fatih YILMAZ", "D-103")
        ),
        schedule(
            ClassSchedules.LATIN_LANGUAGE_AND_LITERATURE_DEPARTMENT,
            ClassSchedules.FOURTH_YEAR,
            LATIN_SOURCE,
            block(monday, "08:30", "10:20", "LDE 405", "Bitirme Çalışması I", note = "M. Ertan Yıldız hariç bütün öğretim üyeleri"),
            block(monday, "10:30", "12:20", "LDE 405", "Bitirme Çalışması I", note = "Burak Takmer ve Hüseyin Uzunoğlu hariç bütün öğretim üyeleri"),
            block(monday, "13:30", "17:20", "LDE 403", "Latin Epigrafisi III", "E. Akdoğu Arca", "D-103"),
            block(tuesday, "08:30", "10:20", "LDE 411", "Latin Şiiri I", "Asuman COŞKUN ABUAGLA", "D-104"),
            block(tuesday, "15:30", "17:20", "LDE 419", "Eski Yunan Edebiyatı Tarihi III", "Nuray GÖKALP", "D-101"),
            block(wednesday, "13:30", "15:20", "LDE 415", "Eski Yunan Epigrafisi I", "Eda AKYÜREK ŞAHİN", "D-102"),
            block(wednesday, "15:30", "17:20", "LDE 421", "Metodoloji I", "Fatih ONUR", "D-101"),
            block(thursday, "08:30", "12:20", "LDE 401", "Latin Dilinin Kaynakları III", "Ebru AKDOĞU ARCA", "Kütüphane"),
            block(thursday, "13:30", "15:20", "LDE 413", "Eski Yunan Dilinin Kaynakları I", "Burak TAKMER", "D-103"),
            block(friday, "08:30", "10:20", "LDE 405", "Bitirme Çalışması I", "Burak TAKMER / M. Ertan YILDIZ / Hüseyin UZUNOĞLU"),
            block(friday, "13:30", "15:20", "LDE 415", "Eski Yunan Epigrafisi I", "Eda AKYÜREK ŞAHİN", "D-102"),
            block(friday, "15:30", "17:20", "LDE 413", "Eski Yunan Dilinin Kaynakları I", "Burak TAKMER", "D-102")
        )
    )

    fun schedulesFor(department: String?): List<ClassSchedule>? = when (department) {
        ClassSchedules.GERMAN_LANGUAGE_AND_LITERATURE_DEPARTMENT -> german
        ClassSchedules.ARCHAEOLOGY_DEPARTMENT -> archaeology
        ClassSchedules.GEOGRAPHY_DEPARTMENT -> geography
        ClassSchedules.ANCIENT_GREEK_DEPARTMENT -> ancientGreek
        ClassSchedules.LATIN_LANGUAGE_AND_LITERATURE_DEPARTMENT -> latin
        else -> AdditionalLiteratureSchedules.schedulesFor(department)
    }
}
