package com.good4.schedule.domain

enum class ScheduleDay(val label: String) {
    MONDAY("Pazartesi"),
    TUESDAY("Salı"),
    WEDNESDAY("Çarşamba"),
    THURSDAY("Perşembe"),
    FRIDAY("Cuma")
}

data class ScheduleEntry(
    val day: ScheduleDay,
    val startTime: String,
    val endTime: String,
    val courseCode: String,
    val courseName: String,
    val instructor: String,
    val classroom: String,
    val courseType: String,
    val note: String? = null,
    val section: String? = null
)

data class ClassSchedule(
    val faculty: String,
    val department: String,
    val classYear: String,
    val academicYear: String,
    val term: String,
    val updatedAt: String,
    val sourcePage: Int,
    val sourceUrl: String,
    val entries: List<ScheduleEntry>,
    val sourceWarning: String? = null
)

object ClassSchedules {
    const val BUSINESS_FACULTY = "İktisadi ve İdari Bilimler Fakültesi"
    const val SCIENCE_FACULTY = "Fen Fakültesi"
    const val AGRICULTURE_FACULTY = "Ziraat Fakültesi"
    const val ENGINEERING_FACULTY = "Mühendislik Fakültesi"
    const val ARCHITECTURE_FACULTY = "Mimarlık Fakültesi"
    const val APPLIED_SCIENCES_FACULTY = "Uygulamalı Bilimler Fakültesi"
    const val EDUCATION_FACULTY = "Eğitim Fakültesi"
    const val LITERATURE_FACULTY = "Edebiyat Fakültesi"
    const val NURSING_FACULTY = "Hemşirelik Fakültesi"
    const val TOURISM_FACULTY = "Turizm Fakültesi"
    const val COMMUNICATION_FACULTY = "İletişim Fakültesi"
    const val LAW_FACULTY = "Hukuk Fakültesi"
    const val SPORT_SCIENCES_FACULTY = "Spor Bilimleri Fakültesi"
    const val FINE_ARTS_FACULTY = "Güzel Sanatlar Fakültesi"
    const val FISHERIES_FACULTY = "Su Ürünleri Fakültesi"
    const val THEOLOGY_FACULTY = "İlahiyat Fakültesi"
    const val DENTAL_FACULTY = "Diş Hekimliği Fakültesi"
    const val BIOLOGY_DEPARTMENT = "Biyoloji"
    const val PHYSICS_DEPARTMENT = "Fizik"
    const val CHEMISTRY_DEPARTMENT = "Kimya"
    const val MATHEMATICS_DEPARTMENT = "Matematik"
    const val SPACE_SCIENCES_DEPARTMENT = "Uzay Bilimleri ve Teknolojileri"
    const val HORTICULTURE_DEPARTMENT = "Bahçe Bitkileri"
    const val PLANT_PROTECTION_DEPARTMENT = "Bitki Koruma"
    const val AGRICULTURAL_ECONOMICS_DEPARTMENT = "Tarım Ekonomisi"
    const val AGRICULTURAL_MACHINERY_DEPARTMENT = "Tarım Makinaları ve Teknolojileri Mühendisliği"
    const val AGRICULTURAL_BIOTECHNOLOGY_DEPARTMENT = "Tarımsal Biyoteknoloji"
    const val AGRICULTURAL_STRUCTURES_DEPARTMENT = "Tarımsal Yapılar ve Sulama"
    const val FIELD_CROPS_DEPARTMENT = "Tarla Bitkileri"
    const val SOIL_SCIENCE_DEPARTMENT = "Toprak Bilimi ve Bitki Besleme"
    const val ANIMAL_SCIENCE_DEPARTMENT = "Zootekni"
    const val BUSINESS_DEPARTMENT = "İşletme"
    const val ECONOMICS_DEPARTMENT = "İktisat"
    const val ECONOMETRICS_DEPARTMENT = "Ekonometri"
    const val PUBLIC_FINANCE_DEPARTMENT = "Maliye"
    const val LABOR_ECONOMICS_DEPARTMENT = "Çalışma Ekonomisi ve Endüstri İlişkileri"
    const val POLITICAL_SCIENCE_DEPARTMENT = "Siyaset Bilimi ve Kamu Yönetimi"
    const val INTERNATIONAL_RELATIONS_DEPARTMENT = "Uluslararası İlişkiler"
    const val COMPUTER_ENGINEERING_DEPARTMENT = "Bilgisayar Mühendisliği"
    const val AI_DATA_ENGINEERING_DEPARTMENT = "Yapay Zeka ve Veri Mühendisliği"
    const val ELECTRICAL_ELECTRONICS_ENGINEERING_DEPARTMENT = "Elektrik-Elektronik Mühendisliği"
    const val ENVIRONMENTAL_ENGINEERING_DEPARTMENT = "Çevre Mühendisliği"
    const val FOOD_ENGINEERING_DEPARTMENT = "Gıda Mühendisliği"
    const val CIVIL_ENGINEERING_DEPARTMENT = "İnşaat Mühendisliği"
    const val GEOLOGY_ENGINEERING_DEPARTMENT = "Jeoloji Mühendisliği"
    const val MECHANICAL_ENGINEERING_DEPARTMENT = "Makine Mühendisliği"
    const val ARCHITECTURE_DEPARTMENT = "Mimarlık"
    const val INTERIOR_ARCHITECTURE_DEPARTMENT = "İç Mimarlık"
    const val SCIENCE_EDUCATION_DEPARTMENT = "Fen Bilgisi Eğitimi"
    const val ENGLISH_TEACHING_DEPARTMENT = "İngilizce Öğretmenliği"
    const val ELEMENTARY_MATHEMATICS_EDUCATION_DEPARTMENT = "İlköğretim Matematik Eğitimi"
    const val EARLY_CHILDHOOD_EDUCATION_DEPARTMENT = "Okul Öncesi Eğitimi"
    const val SPECIAL_EDUCATION_DEPARTMENT = "Özel Eğitim Öğretmenliği"
    const val COUNSELING_DEPARTMENT = "Rehberlik ve Psikolojik Danışmanlık"
    const val CLASSROOM_TEACHING_DEPARTMENT = "Sınıf Öğretmenliği"
    const val SOCIAL_STUDIES_TEACHING_DEPARTMENT = "Sosyal Bilgiler Öğretmenliği"
    const val TURKISH_TEACHING_DEPARTMENT = "Türkçe Öğretmenliği"
    const val NURSING_DEPARTMENT = "Hemşirelik"
    const val GERMAN_LANGUAGE_AND_LITERATURE_DEPARTMENT = "Alman Dili ve Edebiyatı"
    const val ARCHAEOLOGY_DEPARTMENT = "Arkeoloji"
    const val GEOGRAPHY_DEPARTMENT = "Coğrafya"
    const val ANCIENT_GREEK_DEPARTMENT = "Eski Yunan Dili ve Edebiyatı"
    const val LATIN_LANGUAGE_AND_LITERATURE_DEPARTMENT = "Latin Dili ve Edebiyatı"
    const val PHILOSOPHY_DEPARTMENT = "Felsefe"
    const val ENGLISH_LITERATURE_REGULAR_DEPARTMENT = "İngiliz Dili ve Edebiyatı (Örgün)"
    const val ENGLISH_LITERATURE_EVENING_DEPARTMENT = "İngiliz Dili ve Edebiyatı (İkinci Öğretim)"
    const val RUSSIAN_LANGUAGE_AND_LITERATURE_DEPARTMENT = "Rus Dili ve Edebiyatı"
    const val PSYCHOLOGY_DEPARTMENT = "Psikoloji"
    const val ART_HISTORY_DEPARTMENT = "Sanat Tarihi"
    const val SOCIOLOGY_DEPARTMENT = "Sosyoloji"
    const val HISTORY_DEPARTMENT = "Tarih"
    const val TURKISH_LITERATURE_REGULAR_DEPARTMENT = "Türk Dili ve Edebiyatı (Örgün)"
    const val TURKISH_LITERATURE_EVENING_DEPARTMENT = "Türk Dili ve Edebiyatı (İkinci Öğretim)"
    const val FINANCE_BANKING_DEPARTMENT = "Finans ve Bankacılık"
    const val MARKETING_DEPARTMENT = "Pazarlama"
    const val INSURANCE_DEPARTMENT = "Sigortacılık"
    const val INTERNATIONAL_TRADE_LOGISTICS_DEPARTMENT = "Uluslararası Ticaret ve Lojistik"
    const val MANAGEMENT_INFORMATION_SYSTEMS_DEPARTMENT = "Yönetim Bilişim Sistemleri"
    const val TOURISM_BUSINESS_TURKISH_DEPARTMENT = "Turizm İşletmeciliği"
    const val TOURISM_MANAGEMENT_ENGLISH_DEPARTMENT = "Tourism Management"
    const val GASTRONOMY_DEPARTMENT = "Gastronomi ve Mutfak Sanatları"
    const val TOURISM_GUIDANCE_DEPARTMENT = "Turizm Rehberliği"
    const val RECREATION_MANAGEMENT_DEPARTMENT = "Rekreasyon Yönetimi"
    const val BODY_EDUCATION_AND_SPORT_DEPARTMENT = "Beden Eğitimi ve Spor"
    const val SPORTS_MANAGEMENT_DEPARTMENT = "Spor Yöneticiliği"
    const val RECREATION_DEPARTMENT = "Rekreasyon"
    const val PAINTING_DEPARTMENT = "Resim"
    const val SCULPTURE_DEPARTMENT = "Heykel"
    const val GRAPHIC_DESIGN_DEPARTMENT = "Grafik"
    const val CERAMIC_DEPARTMENT = "Seramik"
    const val MUSIC_DEPARTMENT = "Müzik"
    const val PHOTOGRAPHY_DEPARTMENT = "Fotoğraf"
    const val CINEMA_TV_DEPARTMENT = "Sinema-TV"
    const val TRADITIONAL_TURKISH_ARTS_DEPARTMENT = "Geleneksel Türk Sanatları"
    const val TEXTILE_FASHION_DEPARTMENT = "Tekstil ve Moda Tasarımı"
    const val COACHING_EDUCATION_DEPARTMENT = "Antrenörlük Eğitimi"
    const val TOURISM_AND_GASTRONOMY_MANAGEMENT_DEPARTMENT = "Turizm ve Gastronomi Yönetimi Programları"
    const val JOURNALISM_DEPARTMENT = "Gazetecilik"
    const val PUBLIC_RELATIONS_DEPARTMENT = "Halkla İlişkiler ve Tanıtım"
    const val RADIO_TV_CINEMA_DEPARTMENT = "Radyo Televizyon ve Sinema"
    const val ADVERTISING_DEPARTMENT = "Reklamcılık"
    const val LAW_DEPARTMENT = "Hukuk"
    const val FISHERIES_ENGINEERING_DEPARTMENT = "Su Ürünleri Mühendisliği"
    const val THEOLOGY_DEPARTMENT = "İlahiyat"
    const val DENTAL_DEPARTMENT = "Diş Hekimliği"
    const val FIRST_YEAR = "1. Sınıf"
    const val SECOND_YEAR = "2. Sınıf"
    const val THIRD_YEAR = "3. Sınıf"
    const val FOURTH_YEAR = "4. Sınıf"
    const val FIFTH_YEAR = "5. Sınıf"

    private const val BUSINESS_SOURCE_URL =
        "https://webis.akdeniz.edu.tr/uploads/1044/content/%C4%B0%C5%9EL%20Lisans.pdf"

    private const val ECONOMICS_SOURCE_URL =
        "https://webis.akdeniz.edu.tr/file/getfile?guid=ac2cfe16-00ac-471b-a302-5d535e23e288"

    private const val ECONOMETRICS_SOURCE_URL =
        "https://webis.akdeniz.edu.tr/uploads/1049/content/2026-2027%20Gu%CC%88z%20Do%CC%88nemi%20Ekonometri%20Bo%CC%88lu%CC%88mu%CC%88%20Lisans%20Ders%20Program%C4%B1%2014%20Eylu%CC%88l.pdf"

    private const val PUBLIC_FINANCE_SOURCE_URL =
        "https://webis.akdeniz.edu.tr/uploads/1046/Dosyalar/Ders%20Programlar%C4%B1/2026-2027/2026-2027%20G%C3%BCz%20D%C3%B6nemi%20Maliye%20B%C3%B6l%C3%BCm%C3%BC%20Ders%20Program%C4%B11.pdf"

    private const val LABOR_ECONOMICS_SOURCE_URL =
        "https://webis.akdeniz.edu.tr/uploads/1048/content/2026-2027%20G%C3%BCz%20%C3%87EE%C4%B0%20Ders%20program%C4%B1%20%20kopyas%C4%B1.pdf"

    private const val POLITICAL_SCIENCE_SOURCE_URL =
        "https://webis.akdeniz.edu.tr/uploads/1045/content/Ders%20Programlar%C4%B1/SBKY%202026-2027%20lisans%20%C3%B6rg%C3%BCn%20g%C3%BCz%20ders%20program%C4%B1.pdf"

    private const val INTERNATIONAL_RELATIONS_SOURCE_URL =
        "https://webis.akdeniz.edu.tr/uploads/1047/Ders%20Programlar%C4%B1/Gu%CC%88z%20o%CC%88rgu%CC%88n%20%20%281%29.pdf"

    private const val COMPUTER_ENGINEERING_SOURCE_URL =
        "https://docs.google.com/spreadsheets/d/e/2PACX-1vQlSw5jj3NiDuC6epB3h-9iEOJarXkZrkpPyzTdVp_u9vCeBvAiXsXVZkWbkxS0w5vHEbuuWJzob_0r/pubhtml?gid=1769266558&headers=false&single=true&widget=true"

    private const val AI_DATA_ENGINEERING_SOURCE_URL =
        "https://docs.google.com/spreadsheets/d/e/2PACX-1vQlSw5jj3NiDuC6epB3h-9iEOJarXkZrkpPyzTdVp_u9vCeBvAiXsXVZkWbkxS0w5vHEbuuWJzob_0r/pubhtml?gid=716482959&headers=false&single=true&widget=true"

    private const val ELECTRICAL_ELECTRONICS_ENGINEERING_SOURCE_URL =
        "https://webis.akdeniz.edu.tr/file/getfile?guid=eed9f8a7-27fc-4d8c-8065-6cfffe59d01e"

    private const val ENVIRONMENTAL_ENGINEERING_SOURCE_URL =
        "https://webis.akdeniz.edu.tr/uploads/1114/announcement/2026-27G_CevreMuh_DersProgrami_v4.pdf"

    private const val FOOD_ENGINEERING_SOURCE_URL =
        "https://webis.akdeniz.edu.tr/uploads/1110/announcement/2026-2027%20EG%CC%86I%CC%87TI%CC%87M-O%CC%88G%CC%86RETI%CC%87M%20YILI%20GU%CC%88Z%20DO%CC%88NEMI%CC%87%20DERS%20PROGRAMI%203108.pdf"

    private const val CIVIL_ENGINEERING_SOURCE_URL =
        "https://webis.akdeniz.edu.tr/uploads/1118/announcement/2026-2027%20G%C3%9CZ%20D%C3%96NEM%C4%B0%20L%C4%B0SANS%20DERS%20PROGRAMI-08092026.pdf"

    private const val GEOLOGY_ENGINEERING_SOURCE_URL =
        "https://webis.akdeniz.edu.tr/uploads/1111/content/Ders%20Programlar%C4%B1/IMG/26-27_Guz-Lisans-DP.png"

    private const val MECHANICAL_ENGINEERING_SOURCE_URL =
        "https://webis.akdeniz.edu.tr/uploads/1112/announcement/YeniKlasor%203/2026-2027_G%C3%BCz_D%C3%B6nemi_Makine_M%C3%BChendisli%C4%9Fi_B%C3%B6l%C3%BCm%C3%BC_Ders_Program_17.09.2026.pdf"

    private const val ARCHITECTURE_SOURCE_URL =
        "https://webis.akdeniz.edu.tr/uploads/1107/ders_programi/26-27%20g%C3%BCz%20ders%20programlar%C4%B1/M%C4%B0M-%2831401199%29Mimarlik%20-%202026-2027%20Guz%20Haftalik%20Ders%20Programi%202.pdf"

    private const val INTERIOR_ARCHITECTURE_SOURCE_URL =
        "https://webis.akdeniz.edu.tr/uploads/1107/ders_programi/26-27%20g%C3%BCz%20ders%20programlar%C4%B1/%C4%B0%C3%87T-%2831243622%29BKK%2022-1%20%28Haftalik%20Ders%20Programlari%29-1-3%20%281%29.pdf"

    private const val SCIENCE_EDUCATION_SOURCE_URL =
        "https://webis.akdeniz.edu.tr/uploads/1089/announcement/2026-2027%20G%C3%BCz/ders%20programlar%C4%B1/Fen%20Bilgisi%20Eg%CC%86itimi%20ABD%202026-2027%20Eg%CC%86itim-O%CC%88g%CC%86retim%20Y%C4%B1l%C4%B1%20Gu%CC%88z%20Do%CC%88nemi%20Haftal%C4%B1k%20Ders%20Program%C4%B1.pdf"

    private const val ENGLISH_TEACHING_SOURCE_URL =
        "https://webis.akdeniz.edu.tr/uploads/1089/announcement/2026-2027%20G%C3%BCz/ders%20programlar%C4%B1/%C4%B0ngiliz%20Dili%20E%C4%9Fitimi%202026-2027%20G%C3%BCz%20Yar%C4%B1y%C4%B1l%C4%B1%20Ders%20Program%C4%B1.pdf"

    private const val ELEMENTARY_MATHEMATICS_EDUCATION_SOURCE_URL =
        "https://webis.akdeniz.edu.tr/uploads/1089/announcement/2026-2027%20G%C3%BCz/ders%20programlar%C4%B1/ILK%20MAT_L%C4%B0SANS%20DERS%20PROGRAMI%20ASIL%202026-2027%20G%C3%9CZ.pdf"

    private const val EARLY_CHILDHOOD_EDUCATION_SOURCE_URL =
        "https://webis.akdeniz.edu.tr/uploads/1089/announcement/2026-2027%20G%C3%BCz/ders%20programlar%C4%B1/Okul%20O%CC%88ncesi%20Eg%CC%86itimi%20Anabilim%20Dal%C4%B1%202026-2027%20Gu%CC%88z%20Yar%C4%B1y%C4%B1l%C4%B1%20Haftal%C4%B1k%20Ders%20Program%C4%B1.pdf"

    private const val SPECIAL_EDUCATION_SOURCE_URL =
        "https://webis.akdeniz.edu.tr/uploads/1089/announcement/2026-2027%20G%C3%BCz/ders%20programlar%C4%B1/O%CC%88ZEL_EG%CC%86I%CC%87TI%CC%87M_O%CC%88G%CC%86RT_LI%CC%87SANS_Haftal%C4%B1k%20Ders%20Program%C4%B1%202026-2027%20G%C3%9CZ%20(1).pdf"

    private const val COUNSELING_SOURCE_URL =
        "https://webis.akdeniz.edu.tr/uploads/1089/announcement/2026-2027%20G%C3%BCz/ders%20programlar%C4%B1/RPD%202026-27.%20%2831.08.26%29.pdf"

    private const val CLASSROOM_TEACHING_SOURCE_URL =
        "https://webis.akdeniz.edu.tr/uploads/1089/announcement/SINIF%20E%C4%9E%C4%B0T%C4%B0M%C4%B0%202026-2027%20G%C3%9CZ%20YARIYILI%20DERS%20PROGRAMI%20G%C3%BCncellendi..pdf"

    private const val SOCIAL_STUDIES_TEACHING_SOURCE_URL =
        "https://webis.akdeniz.edu.tr/uploads/1089/announcement/06-SB%C3%96%20SOSYAL%20B%C4%B0LG%C4%B0LER%20E%C4%9E%C4%B0T%C4%B0M%C4%B0%20A.B.D.%202026-2027%20G%C3%9CZ%20YARIYILI%20DERS%20PROGRAMI.pdf"

    private const val SOCIAL_STUDIES_PRACTICUM_INSTRUCTORS =
        "Prof. Dr. Yüksel KAŞTAN / Doç. Dr. Osman AKHAN / Prof. Dr. Ayhan AKIŞ / Prof. Dr. Nadire Emel AKHAN / Doç. Dr. Meltem Begüm SAATÇI ATA / Prof. Dr. Ahmet KÖÇ / Doç. Dr. Serpil DEMİREZEN / Arş. Gör. Saim TURAN / Arş. Gör. Tuğçe ÇAL PEKTAŞ"

    private const val TURKISH_TEACHING_SOURCE_URL =
        "https://webis.akdeniz.edu.tr/uploads/1089/announcement/Ders%20Program%C4%B1%20%28T%C3%BCrk%C3%A7e%29%20G%C3%BCncel%20%281%29.pdf"

    private const val NURSING_SOURCE_URL =
        "https://webis.akdeniz.edu.tr/uploads/1036/content/2026-2027%20G%C3%9CZ%20D%C3%96NEM%C4%B0%20DERS%20PROGRAMI%201.09.2026.docx"

    private const val FINANCE_BANKING_SOURCE_URL =
        "https://webis.akdeniz.edu.tr/uploads/1028/announcement/26-27Fall-Schedule/FAB.pdf"

    private const val MARKETING_SOURCE_URL =
        "https://webis.akdeniz.edu.tr/uploads/1028/announcement/26-27Fall-Schedule/PAZ.pdf"

    private const val INSURANCE_SOURCE_URL =
        "https://webis.akdeniz.edu.tr/uploads/1028/announcement/26-27Fall-Schedule/SİG.pdf"

    private const val INTERNATIONAL_TRADE_LOGISTICS_SOURCE_URL =
        "https://webis.akdeniz.edu.tr/uploads/1028/announcement/26-27Fall-Schedule/UTL.pdf"

    private const val MANAGEMENT_INFORMATION_SYSTEMS_SOURCE_URL =
        "https://webis.akdeniz.edu.tr/uploads/1028/announcement/26-27Fall-Schedule/YBS.pdf"

    val businessFirstYear = ClassSchedule(
        faculty = BUSINESS_FACULTY,
        department = BUSINESS_DEPARTMENT,
        classYear = FIRST_YEAR,
        academicYear = "2026-2027",
        term = "Güz Yarıyılı",
        updatedAt = "14.09.2026",
        sourcePage = 1,
        sourceUrl = BUSINESS_SOURCE_URL,
        entries = listOf(
            ScheduleEntry(
                day = ScheduleDay.MONDAY,
                startTime = "10:30",
                endTime = "12:20",
                courseCode = "TDB 101",
                courseName = "Türk Dili I",
                instructor = "Öğr. Gör. Arzu TIRAK ASLAN",
                classroom = "A Blok 101",
                courseType = "Zorunlu"
            ),
            ScheduleEntry(
                day = ScheduleDay.MONDAY,
                startTime = "13:30",
                endTime = "16:20",
                courseCode = "ENF 101",
                courseName = "Bilgi Teknolojileri Kullanımı",
                instructor = "Öğr. Gör. Ramazan UYAR",
                classroom = "Sanal 01",
                courseType = "Zorunlu"
            ),
            ScheduleEntry(
                day = ScheduleDay.TUESDAY,
                startTime = "09:30",
                endTime = "12:20",
                courseCode = "İŞL 101",
                courseName = "Matematik I",
                instructor = "Doç. Dr. Neylan KAYA",
                classroom = "Turkuaz Amfi",
                courseType = "Zorunlu"
            ),
            ScheduleEntry(
                day = ScheduleDay.TUESDAY,
                startTime = "13:30",
                endTime = "16:20",
                courseCode = "İŞL 103",
                courseName = "İktisada Giriş I",
                instructor = "Prof. Dr. Mehmet ZANBAK",
                classroom = "A Blok 206",
                courseType = "Zorunlu"
            ),
            ScheduleEntry(
                day = ScheduleDay.WEDNESDAY,
                startTime = "08:30",
                endTime = "10:20",
                courseCode = "YDB 101",
                courseName = "İngilizce I",
                instructor = "Öğr. Gör. Demet TEKİNAY",
                classroom = "Turkuaz Amfi",
                courseType = "Zorunlu"
            ),
            ScheduleEntry(
                day = ScheduleDay.WEDNESDAY,
                startTime = "10:30",
                endTime = "12:20",
                courseCode = "ATA 101",
                courseName = "Atatürk İlkeleri ve İnkılap Tarihi I",
                instructor = "Öğr. Gör. Bengi KÜMBÜL UZUNSAKAL",
                classroom = "Turkuaz Amfi",
                courseType = "Zorunlu"
            ),
            ScheduleEntry(
                day = ScheduleDay.WEDNESDAY,
                startTime = "12:30",
                endTime = "13:20",
                courseCode = "KPD 101",
                courseName = "Kariyer Planlama",
                instructor = "Prof. Dr. Burcu DEMİREL",
                classroom = "Yavuz Tekelioğlu Konferans Salonu B Blok",
                courseType = "Zorunlu"
            ),
            ScheduleEntry(
                day = ScheduleDay.WEDNESDAY,
                startTime = "13:30",
                endTime = "15:20",
                courseCode = "TDB 115",
                courseName = "Akademik Türkçe I",
                instructor = "Öğr. Gör. Dürüye KAYA",
                classroom = "Uzaktan",
                courseType = "Seçmeli",
                note = "Yabancı öğrenciler için"
            ),
            ScheduleEntry(
                day = ScheduleDay.THURSDAY,
                startTime = "13:30",
                endTime = "16:20",
                courseCode = "İŞL 105",
                courseName = "İşletme Yönetimine Giriş",
                instructor = "Doç. Dr. Kemal KÖKSAL",
                classroom = "Turkuaz Amfi",
                courseType = "Zorunlu"
            ),
            ScheduleEntry(
                day = ScheduleDay.FRIDAY,
                startTime = "09:30",
                endTime = "12:20",
                courseCode = "İŞL 117",
                courseName = "Genel Muhasebe I",
                instructor = "Prof. Dr. Burcu DEMİREL",
                classroom = "Turkuaz Amfi",
                courseType = "Zorunlu"
            )
        )
    )

    val businessSecondYear = ClassSchedule(
        faculty = BUSINESS_FACULTY,
        department = BUSINESS_DEPARTMENT,
        classYear = SECOND_YEAR,
        academicYear = "2026-2027",
        term = "Güz Yarıyılı",
        updatedAt = "14.09.2026",
        sourcePage = 3,
        sourceUrl = BUSINESS_SOURCE_URL,
        entries = listOf(
            ScheduleEntry(
                day = ScheduleDay.MONDAY,
                startTime = "09:30",
                endTime = "12:20",
                courseCode = "İŞL 221",
                courseName = "Sürdürülebilirlik Yönetimi",
                instructor = "Doç. Dr. A. Eren ÖZDEMİR",
                classroom = "A Blok 204",
                courseType = "Seçmeli"
            ),
            ScheduleEntry(
                day = ScheduleDay.MONDAY,
                startTime = "13:30",
                endTime = "16:20",
                courseCode = "İŞL 205",
                courseName = "Yönetim ve Organizasyon",
                instructor = "Doç. Dr. Janset Ö. AYTEMUR",
                classroom = "A Blok 101",
                courseType = "Zorunlu"
            ),
            ScheduleEntry(
                day = ScheduleDay.TUESDAY,
                startTime = "09:30",
                endTime = "12:20",
                courseCode = "ENF 125",
                courseName = "Yapay Zeka Okuryazarlığı",
                instructor = "Öğr. Gör. Dr. T. Fatih KASALAK",
                classroom = "Lab 02 ENF",
                courseType = "Seçmeli"
            ),
            ScheduleEntry(
                day = ScheduleDay.TUESDAY,
                startTime = "09:30",
                endTime = "12:20",
                courseCode = "İŞL 225",
                courseName = "Şirketler Muhasebesi",
                instructor = "Prof. Dr. Adnan DÖNMEZ",
                classroom = "A Blok 105",
                courseType = "Seçmeli"
            ),
            ScheduleEntry(
                day = ScheduleDay.TUESDAY,
                startTime = "13:30",
                endTime = "16:20",
                courseCode = "İŞL 211",
                courseName = "Pazarlama İlkeleri",
                instructor = "Doç. Dr. Umut KUBAT DOKUMACI",
                classroom = "A Blok 102",
                courseType = "Zorunlu"
            ),
            ScheduleEntry(
                day = ScheduleDay.WEDNESDAY,
                startTime = "09:30",
                endTime = "12:20",
                courseCode = "İŞL 213",
                courseName = "Finansal Okuryazarlık",
                instructor = "Doç. Dr. Eda ORUÇ ERDOĞAN",
                classroom = "A Blok 102",
                courseType = "Zorunlu"
            ),
            ScheduleEntry(
                day = ScheduleDay.WEDNESDAY,
                startTime = "13:30",
                endTime = "15:20",
                courseCode = "İŞL 219",
                courseName = "Hukuka Giriş",
                instructor = "Dr. Öğr. Üyesi Ali ERDEM",
                classroom = "Turkuaz Amfi",
                courseType = "Zorunlu"
            ),
            ScheduleEntry(
                day = ScheduleDay.WEDNESDAY,
                startTime = "15:30",
                endTime = "18:20",
                courseCode = "İŞL 287",
                courseName = "İngilizce Okuma Konuşma",
                instructor = "Öğr. Gör. Hülya ÇELİK",
                classroom = "Mavi Amfi",
                courseType = "Seçmeli"
            ),
            ScheduleEntry(
                day = ScheduleDay.THURSDAY,
                startTime = "09:30",
                endTime = "12:20",
                courseCode = "İŞL 201",
                courseName = "İstatistik I",
                instructor = "Doç. Dr. Nesrin ALKAN",
                classroom = "Turkuaz Amfi",
                courseType = "Zorunlu"
            ),
            ScheduleEntry(
                day = ScheduleDay.THURSDAY,
                startTime = "13:30",
                endTime = "16:20",
                courseCode = "İŞL 203",
                courseName = "Mikro İktisat",
                instructor = "Prof. Dr. Ahmet BAYANER",
                classroom = "A Blok 101",
                courseType = "Zorunlu"
            )
        )
    )

    val businessThirdYear = ClassSchedule(
        faculty = BUSINESS_FACULTY,
        department = BUSINESS_DEPARTMENT,
        classYear = THIRD_YEAR,
        academicYear = "2026-2027",
        term = "Güz Yarıyılı",
        updatedAt = "14.09.2026",
        sourcePage = 5,
        sourceUrl = BUSINESS_SOURCE_URL,
        entries = listOf(
            ScheduleEntry(
                day = ScheduleDay.MONDAY,
                startTime = "09:30",
                endTime = "12:20",
                courseCode = "İŞL 315",
                courseName = "Üretim Yönetimi I",
                instructor = "Prof. Dr. Gökhan AKYÜZ",
                classroom = "Turkuaz Amfi",
                courseType = "Zorunlu"
            ),
            ScheduleEntry(
                day = ScheduleDay.MONDAY,
                startTime = "13:30",
                endTime = "16:20",
                courseCode = "İŞL 305",
                courseName = "Pazarlama Yönetimi",
                instructor = "Prof. Dr. Eyyup YARAŞ",
                classroom = "Turkuaz Amfi",
                courseType = "Zorunlu"
            ),
            ScheduleEntry(
                day = ScheduleDay.TUESDAY,
                startTime = "09:30",
                endTime = "12:20",
                courseCode = "İŞL 303",
                courseName = "Algoritma ve Yazılım Dilleri",
                instructor = "Prof. Dr. Can Deniz KÖKSAL",
                classroom = "A Blok 101",
                courseType = "Seçmeli"
            ),
            ScheduleEntry(
                day = ScheduleDay.TUESDAY,
                startTime = "13:30",
                endTime = "16:20",
                courseCode = "İŞL 301",
                courseName = "Yöneylem Araştırması",
                instructor = "Doç. Dr. Neylan KAYA",
                classroom = "A Blok 101",
                courseType = "Zorunlu"
            ),
            ScheduleEntry(
                day = ScheduleDay.WEDNESDAY,
                startTime = "09:30",
                endTime = "12:20",
                courseCode = "İŞL 309",
                courseName = "İşletme Finansı I",
                instructor = "Prof. Dr. Mehmet ŞEN",
                classroom = "A Blok 101",
                courseType = "Zorunlu"
            ),
            ScheduleEntry(
                day = ScheduleDay.THURSDAY,
                startTime = "09:30",
                endTime = "12:20",
                courseCode = "İŞL 311",
                courseName = "Ticaret Hukuku",
                instructor = "Öğr. Gör. Dr. Duygu TURGUT ÖNEL",
                classroom = "A Blok 101",
                courseType = "Zorunlu"
            ),
            ScheduleEntry(
                day = ScheduleDay.THURSDAY,
                startTime = "13:30",
                endTime = "16:20",
                courseCode = "İŞL 395",
                courseName = "Pazarlama İletişimi",
                instructor = "Arş. Gör. Dr. Yağmur ÖZ",
                classroom = "A Blok 102",
                courseType = "Seçmeli"
            ),
            ScheduleEntry(
                day = ScheduleDay.FRIDAY,
                startTime = "09:30",
                endTime = "12:20",
                courseCode = "İŞL 307",
                courseName = "Maliyet Muhasebesi",
                instructor = "Prof. Dr. Adnan DÖNMEZ",
                classroom = "A Blok 202",
                courseType = "Zorunlu"
            )
        )
    )

    val businessFourthYear = ClassSchedule(
        faculty = BUSINESS_FACULTY,
        department = BUSINESS_DEPARTMENT,
        classYear = FOURTH_YEAR,
        academicYear = "2026-2027",
        term = "Güz Yarıyılı",
        updatedAt = "14.09.2026",
        sourcePage = 7,
        sourceUrl = BUSINESS_SOURCE_URL,
        entries = listOf(
            ScheduleEntry(
                day = ScheduleDay.MONDAY,
                startTime = "09:30",
                endTime = "12:20",
                courseCode = "İŞL 459",
                courseName = "İşletme Ekonomisi Uygulamaları",
                instructor = "Doç. Dr. M. Koray ÇETİN",
                classroom = "A Blok 205",
                courseType = "Zorunlu"
            ),
            ScheduleEntry(
                day = ScheduleDay.MONDAY,
                startTime = "13:30",
                endTime = "16:20",
                courseCode = "İŞL 409",
                courseName = "Pazarlama Araştırmaları",
                instructor = "Prof. Dr. Serkan AKINCI",
                classroom = "A Blok 104",
                courseType = "Seçmeli"
            ),
            ScheduleEntry(
                day = ScheduleDay.MONDAY,
                startTime = "13:30",
                endTime = "16:20",
                courseCode = "İŞL 467",
                courseName = "Muhasebe Standartları",
                instructor = "Prof. Dr. Adnan DÖNMEZ",
                classroom = "A Blok 103",
                courseType = "Seçmeli"
            ),
            ScheduleEntry(
                day = ScheduleDay.TUESDAY,
                startTime = "09:30",
                endTime = "12:20",
                courseCode = "İŞL 401",
                courseName = "Stratejik Yönetim",
                instructor = "Doç. Dr. A. Eren ÖZDEMİR",
                classroom = "A Blok 104",
                courseType = "Zorunlu"
            ),
            ScheduleEntry(
                day = ScheduleDay.TUESDAY,
                startTime = "13:30",
                endTime = "16:20",
                courseCode = "İŞL 411",
                courseName = "Proje Analizi",
                instructor = "Prof. Dr. Gökhan AKYÜZ",
                classroom = "A Blok 103",
                courseType = "Seçmeli"
            ),
            ScheduleEntry(
                day = ScheduleDay.TUESDAY,
                startTime = "13:30",
                endTime = "16:20",
                courseCode = "İŞL 441",
                courseName = "Bilgisayar Destekli Muhasebe",
                instructor = "Doç. Dr. Burçin TUTCU",
                classroom = "Bilgisayar Lab. A",
                courseType = "Seçmeli"
            ),
            ScheduleEntry(
                day = ScheduleDay.WEDNESDAY,
                startTime = "09:30",
                endTime = "12:20",
                courseCode = "İŞL 461",
                courseName = "Vergi Uygulamaları",
                instructor = "Doç. Dr. Murat ERDOĞAN",
                classroom = "A Blok 103",
                courseType = "Zorunlu"
            ),
            ScheduleEntry(
                day = ScheduleDay.WEDNESDAY,
                startTime = "13:30",
                endTime = "16:20",
                courseCode = "İŞL 423",
                courseName = "Yeni Finans Teknikleri",
                instructor = "Doç. Dr. Emel BACHA SİMOES",
                classroom = "A Blok 205",
                courseType = "Seçmeli"
            ),
            ScheduleEntry(
                day = ScheduleDay.WEDNESDAY,
                startTime = "13:30",
                endTime = "16:20",
                courseCode = "İŞL 433",
                courseName = "Yönetimde İstatistik Yöntemleri",
                instructor = "Doç. Dr. Nesrin ALKAN",
                classroom = "A Blok 101 + LAB A",
                courseType = "Seçmeli"
            ),
            ScheduleEntry(
                day = ScheduleDay.WEDNESDAY,
                startTime = "13:30",
                endTime = "16:20",
                courseCode = "İŞL 471",
                courseName = "İşletme Yönetiminde Vaka Çalışmaları",
                instructor = "Doç. Dr. Janset AYTEMUR",
                classroom = "A Blok 206",
                courseType = "Seçmeli"
            ),
            ScheduleEntry(
                day = ScheduleDay.THURSDAY,
                startTime = "09:30",
                endTime = "12:20",
                courseCode = "İŞL 413",
                courseName = "İnovasyon Yönetimi",
                instructor = "Prof. Dr. Tuğba YENİDOĞAN",
                classroom = "A Blok 102",
                courseType = "Seçmeli"
            ),
            ScheduleEntry(
                day = ScheduleDay.THURSDAY,
                startTime = "09:30",
                endTime = "12:20",
                courseCode = "İŞL 475",
                courseName = "Yapay Zeka ve Makine Öğrenmesi",
                instructor = "Prof. Dr. Ömür TOSUN",
                classroom = "A Blok 103 + Bilgisayar Lab. A",
                courseType = "Seçmeli"
            ),
            ScheduleEntry(
                day = ScheduleDay.THURSDAY,
                startTime = "13:30",
                endTime = "16:20",
                courseCode = "İŞL 403",
                courseName = "İş Hukuku",
                instructor = "Öğr. Gör. Fahri DUTÇU",
                classroom = "A Blok 104",
                courseType = "Seçmeli"
            ),
            ScheduleEntry(
                day = ScheduleDay.FRIDAY,
                startTime = "09:30",
                endTime = "12:20",
                courseCode = "İŞL 407",
                courseName = "Girişimcilik ve İş Kurma",
                instructor = "Doç. Dr. F. Nuray ATSAN",
                classroom = "A Blok 103",
                courseType = "Seçmeli"
            )
        )
    )

    private fun economicsEntry(
        day: ScheduleDay,
        startTime: String,
        endTime: String,
        courseCode: String,
        courseName: String,
        instructor: String,
        classroom: String
    ) = ScheduleEntry(
        day = day,
        startTime = startTime,
        endTime = endTime,
        courseCode = courseCode,
        courseName = courseName,
        instructor = instructor,
        classroom = classroom,
        courseType = "Ders"
    )

    val economicsFirstYear = ClassSchedule(
        faculty = BUSINESS_FACULTY,
        department = ECONOMICS_DEPARTMENT,
        classYear = FIRST_YEAR,
        academicYear = "2026-2027",
        term = "Güz Yarıyılı",
        updatedAt = "05.09.2026",
        sourcePage = 1,
        sourceUrl = ECONOMICS_SOURCE_URL,
        entries = listOf(
            economicsEntry(ScheduleDay.MONDAY, "09:30", "13:20", "IKT 105", "Matematik I", "Doç. Dr. Neylan KAYA", "A Blok Derslik 202"),
            economicsEntry(ScheduleDay.MONDAY, "13:30", "16:20", "IKT 113", "Genel Muhasebe I", "Doç. Dr. Güler Ferhan ÜNAL UYAR", "A Blok Derslik 202"),
            economicsEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "TDB 101", "Türk Dili I", "Öğr. Gör. Arzu TIRAK ASLAN", "B Blok Amfi 1"),
            economicsEntry(ScheduleDay.WEDNESDAY, "08:30", "10:20", "YBD 101", "İngilizce I", "Öğr. Gör. Demet TEKİNAY", "A Blok Turkuaz Amfi"),
            economicsEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "ATA 101", "Atatürk İlkeleri ve İnkılap Tarihi I", "Öğr. Gör. Bengi KÜMBÜL UZUNSAKAL", "A Blok Turkuaz Amfi"),
            economicsEntry(ScheduleDay.WEDNESDAY, "12:30", "13:20", "KPD 101", "Kariyer Planlama", "Prof. Dr. Burcu DEMİREL", "B Blok Yavuz Tekelioğlu Konferans Salonu"),
            economicsEntry(ScheduleDay.WEDNESDAY, "13:30", "16:20", "TDB 115", "Akademik Türkçe", "Öğr. Gör. Dürüye KAYA", "Uzaktan"),
            economicsEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "IKT 103", "Hukuka Giriş", "Arş. Gör. Dr. Emel K. UÇAR", "A Blok Derslik 203"),
            economicsEntry(ScheduleDay.FRIDAY, "09:30", "13:20", "IKT 101", "İktisada Giriş I", "Arş. Gör. Dr. Huriye ALKIN", "B Blok Mor Amfi"),
            economicsEntry(ScheduleDay.FRIDAY, "14:30", "16:20", "IKT 107", "Sosyoloji", "Arş. Gör. Dr. Özgür BAL", "B Blok Amfi 6")
        )
    )

    val economicsSecondYear = ClassSchedule(
        faculty = BUSINESS_FACULTY,
        department = ECONOMICS_DEPARTMENT,
        classYear = SECOND_YEAR,
        academicYear = "2026-2027",
        term = "Güz Yarıyılı",
        updatedAt = "05.09.2026",
        sourcePage = 2,
        sourceUrl = ECONOMICS_SOURCE_URL,
        entries = listOf(
            economicsEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "IKT 201", "Mikro İktisat I", "Prof. Dr. Gülden BÖLÜK", "B Blok Amfi 4"),
            economicsEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "IKT 207", "İstatistik I", "Doç. Dr. Nesrin ALKAN", "A Blok Derslik 203"),
            economicsEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "IKT 229", "İktisatçılar için Matematik I", "Doç. Dr. Zafer Barış GÜL", "B Blok Amfi 5"),
            economicsEntry(ScheduleDay.WEDNESDAY, "13:30", "16:20", "IKT 203", "Makro İktisat I", "Prof. Dr. Koray DUMAN", "B Blok Mor Amfi"),
            economicsEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "IKT 205", "Avrupa İktisat Tarihi", "Dr. Öğr. Üyesi Damla DUMAN", "B Blok Amfi 2"),
            economicsEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "IKT 233", "Siyaset Bilimine Giriş", "Dr. Öğr. Üyesi Barış AYDIN", "A Blok Derslik 105")
        )
    )

    val economicsThirdYear = ClassSchedule(
        faculty = BUSINESS_FACULTY,
        department = ECONOMICS_DEPARTMENT,
        classYear = THIRD_YEAR,
        academicYear = "2026-2027",
        term = "Güz Yarıyılı",
        updatedAt = "05.09.2026",
        sourcePage = 3,
        sourceUrl = ECONOMICS_SOURCE_URL,
        entries = listOf(
            economicsEntry(ScheduleDay.MONDAY, "09:30", "12:20", "IKT 325", "Sosyal Bilimlerde Araştırma Yöntemleri", "Prof. Dr. Osman KARKACIER", "B Blok Amfi 2"),
            economicsEntry(ScheduleDay.MONDAY, "13:30", "16:20", "IKT 321", "Proje Değerlendirme", "Prof. Dr. Osman KARKACIER", "B Blok Amfi 1"),
            economicsEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "IKT 327", "Finansal Piyasalar ve Kurumlar", "Prof. Dr. Ayşegül ATEŞ", "B Blok Amfi 5"),
            economicsEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "IKT 303", "Ekonometri I", "Doç. Dr. Ünal TÖNGÜR", "B Blok Mor Amfi"),
            economicsEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "IKT 323", "Kamu Maliyesi", "Araş. Gör. Dr. İlyas ÖZKÖK", "A Blok Derslik 201"),
            economicsEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "IKT 329", "Güncel Ekonomik Sorunlar", "Prof. Dr. Koray DUMAN", "B Blok Amfi 6"),
            economicsEntry(ScheduleDay.WEDNESDAY, "13:30", "16:20", "IKT 389", "Kent Ekonomisi", "Doç. Dr. Ümit SEYFETTİNOĞLU", "B Blok Amfi 3"),
            economicsEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "IKT 301", "Para Teorisi", "Prof. Dr. Sayım IŞIK", "B Blok Amfi 5"),
            economicsEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "IKT 307", "Uluslararası İktisat I", "Prof. Dr. Kemal TÜRKCAN", "B Blok Amfi 5"),
            economicsEntry(ScheduleDay.FRIDAY, "14:30", "17:20", "IKT 319", "Çevre ve Doğal Kaynaklar Ekonomisi", "Prof. Dr. Ali KOÇ", "B Blok Amfi 5")
        )
    )

    val economicsFourthYear = ClassSchedule(
        faculty = BUSINESS_FACULTY,
        department = ECONOMICS_DEPARTMENT,
        classYear = FOURTH_YEAR,
        academicYear = "2026-2027",
        term = "Güz Yarıyılı",
        updatedAt = "05.09.2026",
        sourcePage = 4,
        sourceUrl = ECONOMICS_SOURCE_URL,
        entries = listOf(
            economicsEntry(ScheduleDay.MONDAY, "12:30", "13:20", "IKT 441", "Araştırma Projesi", "Tüm Öğretim Üyeleri", ""),
            economicsEntry(ScheduleDay.MONDAY, "13:30", "16:20", "IKT 405", "Türkiye Ekonomisi I", "Doç. Dr. Şükrü ERDEM", "B Blok Amfi 4"),
            economicsEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "IKT 411", "Büyüme Teorileri", "Doç. Dr. Zafer Barış GÜL", "A Blok Mavi Amfi"),
            economicsEntry(ScheduleDay.TUESDAY, "12:30", "13:20", "IKT 441", "Araştırma Projesi", "Tüm Öğretim Üyeleri", ""),
            economicsEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "IKT 459", "Döngüsel İktisat", "Prof. Dr. Selim ÇAĞATAY", "B Blok Amfi 4"),
            economicsEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "IKT 425", "Dünya Ekonomisi", "Prof. Dr. Ayşegül ATEŞ", "B Blok Amfi 4"),
            economicsEntry(ScheduleDay.WEDNESDAY, "13:30", "16:20", "IKT 461", "Enerji Ekonomisi", "Prof. Dr. Gülden BÖLÜK", "B Blok Amfi 4"),
            economicsEntry(ScheduleDay.WEDNESDAY, "13:30", "16:20", "IKT 421", "Mali Tablolar Analizi", "Doç. Dr. Burçin TUTCU", "A Blok Derslik 104"),
            economicsEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "IKT 435", "Bölgesel İktisat", "Doç. Dr. Ümit SEYFETTİNOĞLU", "B Blok Amfi 6"),
            economicsEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "IKT 427", "İş Hukuku", "Öğr. Gör. Fahri DUTÇU", "A Blok Derslik 104"),
            economicsEntry(ScheduleDay.FRIDAY, "09:30", "12:20", "IKT 401", "İktisadi Düşünce Tarihi", "Dr. Öğr. Üyesi Damla DUMAN", "B Blok Amfi 2")
        )
    )

    private fun programEntry(
        day: ScheduleDay,
        startTime: String,
        endTime: String,
        courseCode: String,
        courseName: String,
        instructor: String,
        classroom: String,
        courseType: String = "Ders",
        note: String? = null,
        section: String? = null
    ) = ScheduleEntry(
        day = day,
        startTime = startTime,
        endTime = endTime,
        courseCode = courseCode,
        courseName = courseName,
        instructor = instructor,
        classroom = classroom,
        courseType = courseType,
        note = note,
        section = section
    )

    private fun programSchedule(
        faculty: String = BUSINESS_FACULTY,
        department: String,
        classYear: String,
        updatedAt: String,
        sourcePage: Int,
        sourceUrl: String,
        entries: List<ScheduleEntry>,
        sourceWarning: String? = null
    ) = ClassSchedule(
        faculty = faculty,
        department = department,
        classYear = classYear,
        academicYear = "2026-2027",
        term = "Güz Yarıyılı",
        updatedAt = updatedAt,
        sourcePage = sourcePage,
        sourceUrl = sourceUrl,
        entries = entries,
        sourceWarning = sourceWarning
    )

    private fun engineeringSchedule(
        department: String,
        classYear: String,
        sourceUrl: String,
        sourcePage: Int,
        updatedAt: String = "2026-2027 Güz",
        entries: List<ScheduleEntry>
    ) = programSchedule(
        faculty = ENGINEERING_FACULTY,
        department = department,
        classYear = classYear,
        updatedAt = updatedAt,
        sourcePage = sourcePage,
        sourceUrl = sourceUrl,
        entries = entries
    )

    private fun architectureSchedule(
        department: String,
        classYear: String,
        sourceUrl: String,
        sourcePage: Int,
        updatedAt: String = "2026-2027 Güz",
        entries: List<ScheduleEntry>
    ) = programSchedule(
        faculty = ARCHITECTURE_FACULTY,
        department = department,
        classYear = classYear,
        updatedAt = updatedAt,
        sourcePage = sourcePage,
        sourceUrl = sourceUrl,
        entries = entries
    )

    private fun appliedSciencesSchedule(
        department: String = FINANCE_BANKING_DEPARTMENT,
        classYear: String,
        sourcePage: Int,
        sourceUrl: String = FINANCE_BANKING_SOURCE_URL,
        entries: List<ScheduleEntry>
    ) = programSchedule(
        faculty = APPLIED_SCIENCES_FACULTY,
        department = department,
        classYear = classYear,
        updatedAt = "20.09.2026 18:26",
        sourcePage = sourcePage,
        sourceUrl = sourceUrl,
        entries = entries
    )

    private fun educationSchedule(
        department: String = SCIENCE_EDUCATION_DEPARTMENT,
        classYear: String,
        sourcePage: Int,
        sourceUrl: String = SCIENCE_EDUCATION_SOURCE_URL,
        updatedAt: String = "19.09.2026",
        entries: List<ScheduleEntry>
    ) = programSchedule(
        faculty = EDUCATION_FACULTY,
        department = department,
        classYear = classYear,
        updatedAt = updatedAt,
        sourcePage = sourcePage,
        sourceUrl = sourceUrl,
        entries = entries
    )

    private fun nursingSchedule(
        classYear: String,
        entries: List<ScheduleEntry>
    ) = programSchedule(
        faculty = NURSING_FACULTY,
        department = NURSING_DEPARTMENT,
        classYear = classYear,
        updatedAt = "01.09.2026",
        sourcePage = 1,
        sourceUrl = NURSING_SOURCE_URL,
        entries = entries,
        sourceWarning = if (classYear == THIRD_YEAR || classYear == FOURTH_YEAR) {
            "Resmî 2026–2027 dosyasındaki bu sınıf sayfasının başlığı 2025–2026 yazıyor. Programı kullanmadan önce fakülteyle doğrulayın."
        } else null
    )

    val laborEconomicsFirstYear = programSchedule(
        department = LABOR_ECONOMICS_DEPARTMENT,
        classYear = FIRST_YEAR,
        updatedAt = "18.09.2026",
        sourcePage = 1,
        sourceUrl = LABOR_ECONOMICS_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "CEK 103", "Siyaset Bilimine Giriş", "Dr. Öğr. Üyesi Kadriye OKUDAN DERNEK", "B Blok Amfi 3", "Zorunlu"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "CEK 101", "İktisada Giriş I", "Dr. Öğr. Üyesi Ayten YAĞMUR", "C-3", "Zorunlu"),
            programEntry(ScheduleDay.TUESDAY, "10:30", "12:20", "TDB 101", "Türk Dili I", "Öğr. Gör. Arzu TIRAK ASLAN", "B Blok Mor Amfi", "Zorunlu"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "CEK 105", "Hukuka Giriş", "Öğr. Gör. Aycan DEMİR", "C-4", "Zorunlu"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "10:20", "YBD 101", "İngilizce I", "Öğr. Gör. Aslı TAŞER", "A Blok Mavi Amfi", "Zorunlu"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "ATA 101", "Atatürk İlkeleri ve İnkılap Tarihi I", "Öğr. Gör. Koray ERGİN", "B Blok Mor Amfi", "Zorunlu"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "TDB 115", "Akademik Türkçe I", "Öğr. Gör. Duriye KARA", "Online", "Seçmeli"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "CEK 113", "Genel Muhasebe I", "Öğr. Gör. Dr. Mustafa TERZİOĞLU", "C-6", "Zorunlu"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "CEK 109", "Sosyoloji", "Doç. Dr. Beyhan AKSOY", "C-8", "Zorunlu"),
            programEntry(ScheduleDay.FRIDAY, "12:30", "15:20", "CEK 107", "Matematik I", "Öğr. Gör. Ahmet TEMİZEL", "C-6", "Zorunlu")
        )
    )

    val laborEconomicsSecondYear = programSchedule(
        department = LABOR_ECONOMICS_DEPARTMENT,
        classYear = SECOND_YEAR,
        updatedAt = "18.09.2026",
        sourcePage = 2,
        sourceUrl = LABOR_ECONOMICS_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "CEK 205", "İstatistik (Fosil)", "Doç. Dr. Kemal KÖKSAL", "Belirtilmemiş", "Seçmeli"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "CEK 213", "İdare Hukuku", "Öğr. Gör. Dr. Gülden ATİLLA ÖZTÜRK", "C-9", "Zorunlu"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "CEK 211", "Çalışma Ekonomisi I", "Doç. Dr. Şerife DURMAZ", "C-3", "Zorunlu"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "CEK 209", "Sosyal Politika I", "Prof. Dr. Mete Kaan NAMAL", "C-6", "Zorunlu"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "CEK 219", "Psikolojiye Giriş", "Doç. Dr. Şerife DURMAZ", "C-5", "Zorunlu"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "16:20", "CEK 207", "Yönetim ve Organizasyon", "Doç. Dr. Beyhan AKSOY", "C-3", "Zorunlu"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "CEK 201", "Mikro İktisat", "Prof. Dr. Muhammed KARATAŞ", "C-8", "Zorunlu"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "CEK 225", "Kariyer Yönetimi", "Doç. Dr. Şerife DURMAZ", "C-1", "Seçmeli"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "CEK 231", "Ticaret Hukuku", "Öğr. Gör. Dr. Duygu TURGUT GÜNEL", "A Blok 103", "Seçmeli"),
            programEntry(ScheduleDay.FRIDAY, "13:30", "16:20", "CEK 217", "Sosyal Bilimlerde Araştırma Yöntemleri", "Dr. Cansu TEKİN", "C-3", "Zorunlu")
        )
    )

    val laborEconomicsThirdYear = programSchedule(
        department = LABOR_ECONOMICS_DEPARTMENT,
        classYear = THIRD_YEAR,
        updatedAt = "18.09.2026",
        sourcePage = 4,
        sourceUrl = LABOR_ECONOMICS_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "CEK 307", "Sosyal Psikoloji", "Prof. Dr. Rabia ÇİZEL", "C-1", "Zorunlu"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "CEK 321", "Banka ve Sermaye Piyasaları", "Prof. Dr. Muhammed KARATAŞ", "C-2", "Seçmeli"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "CEK 329", "Kamu Maliyesi I", "Dr. Öğr. Üyesi Ayten YAĞMUR", "C-1", "Zorunlu"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "CEK 319", "Örgüt Sosyolojisi", "Doç. Dr. Beyhan AKSOY", "C-2", "Zorunlu"),
            programEntry(ScheduleDay.WEDNESDAY, "12:30", "14:20", "TDP 303", "Toplumsal Duyarlılık ve Katkı", "Şube 5-6", "Belirtilmemiş", "Ders"),
            programEntry(ScheduleDay.WEDNESDAY, "14:30", "17:20", "CEK 327", "Yönetim Psikolojisi", "Doç. Dr. Şerife DURMAZ", "C-1", "Seçmeli"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "CEK 303", "Bireysel İş Hukuku", "Öğr. Gör. Fahri DUTÇU", "C-1", "Zorunlu"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "CEK 313", "Sendikacılık", "Doç. Dr. Taner AKPINAR", "C-2", "Zorunlu"),
            programEntry(ScheduleDay.THURSDAY, "16:30", "19:20", "CEK 411", "Endüstri İlişkileri I (Fosil)", "Dr. Cansu TEKİN", "Belirtilmemiş", "Seçmeli"),
            programEntry(ScheduleDay.FRIDAY, "09:30", "12:20", "CEK 333", "Endüstri İlişkileri", "Dr. Cansu TEKİN", "C-1", "Zorunlu"),
            programEntry(ScheduleDay.FRIDAY, "12:30", "14:20", "TDP 303", "Toplumsal Duyarlılık ve Katkı", "Şubeler 1-4", "Belirtilmemiş", "Ders")
        )
    )

    val laborEconomicsFourthYear = programSchedule(
        department = LABOR_ECONOMICS_DEPARTMENT,
        classYear = FOURTH_YEAR,
        updatedAt = "18.09.2026",
        sourcePage = 6,
        sourceUrl = LABOR_ECONOMICS_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "CEK 425", "Sosyal Güvenlik", "Doç. Dr. Taner AKPINAR", "C-2", "Zorunlu"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "CEK 429", "Proje Döngüsü Yönetimi I", "Prof. Dr. Mete Kaan NAMAL", "C-1", "Seçmeli"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "CEK 433", "Toplu Pazarlık Ekonomisi", "Dr. Öğr. Üyesi Ayten YAĞMUR", "C-1", "Seçmeli"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "CEK 417", "Vergi Hukuku", "Doç. Dr. Derya YAYMAN", "C-5", "Seçmeli"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "CEK 427", "Ulusal ve Uluslararası Kuruluşlar", "Prof. Dr. Muhammed KARATAŞ", "C-1", "Seçmeli"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "16:20", "CEK 405", "Türkiye Ekonomisi", "Prof. Dr. Abdulkadir KÖKOCAK", "C-2", "Zorunlu"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "CEK 431", "Yoksulluk Araştırmaları", "Doç. Dr. Şerife DURMAZ", "C-2", "Seçmeli"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "CEK 401", "İnsan Kaynakları Yönetimi", "Prof. Dr. Rabia ÇİZEL", "C-5", "Zorunlu"),
            programEntry(ScheduleDay.FRIDAY, "09:30", "12:20", "CEK 421", "Ekonomik Büyüme ve Kalkınma", "Prof. Dr. Abdulkadir KÖKOCAK", "C-2", "Seçmeli")
        )
    )

    val politicalScienceFirstYear = programSchedule(
        department = POLITICAL_SCIENCE_DEPARTMENT,
        classYear = FIRST_YEAR,
        updatedAt = "08.09.2026",
        sourcePage = 1,
        sourceUrl = POLITICAL_SCIENCE_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "KMY 111", "Kamu Yönetimine Giriş", "Doç. Dr. Serkan DORU", "Mor Amfi", "Zorunlu"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "KMY 105", "Toplum Bilimi I", "Dr. Öğr. Üyesi Barış AYDIN", "C6", "Zorunlu"),
            programEntry(ScheduleDay.TUESDAY, "15:30", "17:20", "TDB 101", "Türk Dili", "Öğr. Gör. Arzu TIRAK ASLAN", "Turkuaz", "Zorunlu"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "10:20", "YBD 101", "İngilizce I", "Doç. Dr. Abdullah ARSLAN", "C3", "Zorunlu"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "ATA 101", "Atatürk İlkeleri ve İnkılap Tarihi I", "Doç. Dr. Mustafa MALHUT", "Mavi Amfi", "Zorunlu"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "TDB 115", "Akademik Türkçe", "Öğr. Gör. Dürüye KARA", "Online", "Seçmeli"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "KMY 103", "Siyaset Bilimi I", "Doç. Dr. Yavuz Selim ALKAN", "Mor Amfi", "Zorunlu"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "KMY 107", "Hukuka Giriş", "Dr. Öğr. Üyesi Ali ERDEM", "205", "Zorunlu"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "17:20", "KMY 113", "İktisada Giriş I", "Dr. Huriye ALKIN", "Amfi I", "Zorunlu")
        )
    )

    val politicalScienceSecondYear = programSchedule(
        department = POLITICAL_SCIENCE_DEPARTMENT,
        classYear = SECOND_YEAR,
        updatedAt = "08.09.2026",
        sourcePage = 2,
        sourceUrl = POLITICAL_SCIENCE_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "KMY 207", "Siyasal Düşünceler Tarihi I", "Doç. Dr. Ceren KALFA", "C4", "Zorunlu"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "KMY 201", "Yönetim Bilimi I", "Doç. Dr. V. Alpay GÜNAL", "Amfi 5", "Zorunlu"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "KMY 223", "E-Devlet Teknolojileri ve Kullanımı", "Doç. Dr. Serkan DORU", "Amfi VI", "Seçmeli"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "KMY 213", "Türk Siyasal Yaşamı I", "Dr. Öğr. Üyesi Kadriye OKUDAN DERNEK", "C4", "Zorunlu"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "16:20", "KMY 221", "Türkiye’nin Toplumsal Yapısı", "Doç. Dr. Turan ŞENER", "Amfi I", "Zorunlu"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "KMY 211", "Kentleşme ve Konut Politikası", "Prof. Dr. Ferhunde HAYIRSEVER TOPÇU", "C4", "Zorunlu"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "17:20", "KMY 219", "Medeni Hukuk", "Öğr. Gör. Aycan DEMİR", "C4", "Zorunlu")
        )
    )

    val politicalScienceThirdYear = programSchedule(
        department = POLITICAL_SCIENCE_DEPARTMENT,
        classYear = THIRD_YEAR,
        updatedAt = "08.09.2026",
        sourcePage = 3,
        sourceUrl = POLITICAL_SCIENCE_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "KMY 313", "Yerel Yönetimler I", "Prof. Dr. Hakan ALTINTAŞ", "Amfi 5", "Zorunlu"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "KMY 387", "Mesleki İngilizce", "Dr. Ayşe KALAV", "Amfi 6", "Seçmeli"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "KMY 325", "Çağdaş Siyasal Akımlar", "Doç. Dr. Ceren KALFA", "Amfi II", "Zorunlu"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "KMY 317", "İleri Bilgi ve İletişim Teknolojileri", "Öğr. Gör. Dr. Turgut Fatih KASALAK", "Lab 07", "Seçmeli"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "KMY 307", "Örgüt Kuramları", "Dr. Öğr. Üyesi Bengi DEMİRCİ", "105", "Zorunlu"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "KMY 319", "Türkiye Ekonomisi", "Prof. Dr. Abdülkadir KÖKOCAK", "104", "Zorunlu"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "16:20", "KMY 311", "Kamu Maliyesi", "Doç. Dr. Birsen NACAR KARABACAK", "102", "Zorunlu"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "16:20", "KMY 341", "Genel Muhasebe", "Prof. Dr. Burcu DEMİREL", "103", "Seçmeli"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "KMY 301", "İdare Hukuku", "Dr. Öğr. Üyesi Ali ERDEM", "205", "Zorunlu"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "KMY 303", "Siyaset Sosyolojisi", "Prof. Dr. Faruk ATAAY", "Mor Amfi", "Zorunlu"),
            programEntry(ScheduleDay.FRIDAY, "09:30", "12:20", "KMY 305", "Uluslararası İlişkiler ve Dış Politika", "Doç. Dr. Yavuz Selim ALKAN", "Amfi IV", "Zorunlu"),
            programEntry(ScheduleDay.FRIDAY, "09:30", "12:20", "KMY 355", "Mikro İktisat", "Prof. Dr. Muhammed KARATAŞ", "C5"),
            programEntry(ScheduleDay.FRIDAY, "09:30", "12:20", "KMY 391", "Yönetim Felsefesi", "Öğr. Gör. Dr. Beril ALEV", "Amfi VI"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "17:20", "KMY 323", "Uluslararası Çevre Politikaları", "Prof. Dr. Ferhunde HAYIRSEVER TOPÇU", "Amfi II")
        )
    )

    val politicalScienceFourthYear = programSchedule(
        department = POLITICAL_SCIENCE_DEPARTMENT,
        classYear = FOURTH_YEAR,
        updatedAt = "08.09.2026",
        sourcePage = 4,
        sourceUrl = POLITICAL_SCIENCE_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "KMY 435", "Kamu Politikaları Analizi", "Prof. Dr. Faruk ATAAY", "Mor Amfi", "Zorunlu"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "KMY 403", "Personel Yönetimi", "Prof. Dr. Erol ESEN", "Amfi III", "Zorunlu"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "KMY 443", "Kent Sosyolojisi", "Prof. Dr. Hakan ALTINTAŞ", "Amfi II", "Seçmeli"),
            programEntry(ScheduleDay.TUESDAY, "15:30", "18:20", "KMY 485", "Mevzuat Okuma ve Çözümleme", "Doç. Dr. Ahmet ALPTEKİN DURU", "Amfi III", "Seçmeli"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "ENF 126", "Yapay Zeka Tabanlı Analitik Yaklaşımlar", "Öğr. Gör. Dr. Evren SEZGİN", "Lab 07", "Seçmeli"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "16:20", "KMY 455", "Mesleki Uygulamalar", "Prof. Dr. Hakan ALTINTAŞ", "Amfi II", "Zorunlu"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "18:20", "KMY 405", "Siyaset Bilimi ve Kamu Yönetiminde Eleştirel Düşünce", "Dr. Öğr. Üyesi Barış AYDIN", "C4", "Seçmeli"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "KMY 413", "Türk Dış Politikası I", "Dr. Öğr. Üyesi Kadriye OKUDAN DERNEK", "206", "Seçmeli"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "KMY 409", "Avrupa Birliği ve Kamu Yönetimi", "Prof. Dr. Erol ESEN", "Amfi I", "Seçmeli"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "KMY 411", "Ticaret Hukuku", "Öğr. Gör. Dr. Duygu TURGUT GÜNEL", "103", "Seçmeli"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "KMY 447", "Siyaset Psikolojisi", "Doç. Dr. Turan ŞENER", "Amfi 6", "Seçmeli"),
            programEntry(ScheduleDay.THURSDAY, "16:30", "19:20", "KMY 419", "Kamu Özel Ortaklığı Yönetimi", "Dr. Öğr. Üyesi Ali ERDEM", "Amfi III", "Seçmeli"),
            programEntry(ScheduleDay.FRIDAY, "09:30", "12:20", "KMY 407", "Karşılaştırmalı Yönetim Yapıları", "Doç. Dr. Serkan DORU", "Amfi VI", "Seçmeli"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "17:20", "KMY 401", "Türkiye’nin Yönetim Yapısı", "Doç. Dr. V. Alpay GÜNAL", "Mor Amfi", "Zorunlu")
        )
    )

    val internationalRelationsFirstYear = programSchedule(
        department = INTERNATIONAL_RELATIONS_DEPARTMENT,
        classYear = FIRST_YEAR,
        updatedAt = "19.09.2026",
        sourcePage = 1,
        sourceUrl = INTERNATIONAL_RELATIONS_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "10:30", "13:20", "ULS 101", "İktisada Giriş", "Dr. Öğr. Üyesi Ayten YAĞMUR", "C-3", "Zorunlu"),
            programEntry(ScheduleDay.MONDAY, "14:30", "17:20", "ULS 103", "Siyaset Bilimine Giriş", "Doç. Dr. Fulya ÖZKAN", "C-5", "Zorunlu"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "09:20", "ULS 107", "Hukuka Giriş", "Öğr. Gör. Aycan DEMİR", "C-4", "Zorunlu"),
            programEntry(ScheduleDay.TUESDAY, "10:30", "12:20", "ULS 109", "Matematik I", "Prof. Dr. Gültekin TINAZTEPE", "C-5", "Zorunlu"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "ULS 107", "Hukuka Giriş", "Öğr. Gör. Aycan DEMİR", "C-4", "Zorunlu"),
            programEntry(ScheduleDay.TUESDAY, "15:30", "17:20", "TDB 101", "Türk Dili I", "Öğr. Gör. Arzu TIRAK ASLAN", "Turkuaz Amfi", "Zorunlu"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "10:20", "YBD 101", "İngilizce I", "Doç. Dr. Abdullah ARSLAN", "C-3", "Zorunlu"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "ATA 101", "Atatürk İlkeleri ve İnkılap Tarihi I", "Doç. Dr. Mustafa MALHUT", "Mavi Amfi", "Zorunlu"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "TDB 115", "Akademik Türkçe", "Öğr. Gör. Duriye KARA", "Uzaktan", "Seçmeli"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "ULS 105", "Sosyoloji", "Arş. Gör. Dr. Özgür BAL", "C-9", "Zorunlu")
        )
    )

    val internationalRelationsSecondYear = programSchedule(
        department = INTERNATIONAL_RELATIONS_DEPARTMENT,
        classYear = SECOND_YEAR,
        updatedAt = "19.09.2026",
        sourcePage = 3,
        sourceUrl = INTERNATIONAL_RELATIONS_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "ULS 205", "Uluslararası Örgütler", "Arş. Gör. Dr. Taylan SEYİRCİ", "C-5", "Zorunlu"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "ULS 209", "İdare Hukuku", "Öğr. Gör. Dr. Gülden ATİLLA ÖZTÜRK", "C-9", "Seçmeli"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "ULS 217", "Medeni Hukuk", "Öğr. Gör. Aycan DEMİR", "C-4", "Seçmeli"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "ULS 213", "Siyasal Düşünceler Tarihi", "Doç. Dr. Ceren KALFA", "C-9", "Zorunlu"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "16:20", "ULS 201", "Siyasi Tarih I", "Doç. Dr. Fulya ÖZKAN", "C-8", "Zorunlu"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "ULS 283", "İngilizce Okuma Konuşma", "Öğr. Gör. Burçak AKINCI", "A Blok 204", "Seçmeli"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "17:20", "ULS 289", "Uluslararası Politika", "Arş. Gör. Dr. Yusuf Kenan POLAT", "C-1", "Seçmeli")
        )
    )

    val internationalRelationsThirdYear = programSchedule(
        department = INTERNATIONAL_RELATIONS_DEPARTMENT,
        classYear = THIRD_YEAR,
        updatedAt = "19.09.2026",
        sourcePage = 5,
        sourceUrl = INTERNATIONAL_RELATIONS_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "ULS 305", "Uluslararası İlişkiler Kuramları", "Arş. Gör. Dr. Mustafa ÇAKIR", "C-9", "Zorunlu"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "ULS 301", "Türk Dış Politikası", "Prof. Dr. Şenol KANTARCI", "C-8", "Zorunlu"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "ULS 309", "Devletler Özel Hukuku", "Öğr. Gör. Dr. Gülden ATİLLA ÖZTÜRK", "A-102", "Seçmeli"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "ULS 343", "Savaşlar ve Devrimler Tarihi", "Arş. Gör. Dr. Arda DİLMAÇ", "C-9", "Seçmeli"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "ULS 321", "Bölgesel Politikalar: Ortadoğu", "Doç. Dr. Fulya ÖZKAN", "C-8", "Seçmeli"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "ULS 319", "Uluslararası İktisat", "Doç. Dr. Atiye Beyhan AKAY", "C-6", "Zorunlu"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "16:20", "ULS 341", "Dış Ticaret", "Doç. Dr. Zeynep ÇİMEN", "C-5", "Seçmeli"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "16:20", "ULS 327", "Bölgesel Politikalar: Balkanlar", "Dr. Öğr. Üyesi Durmuş Ali KOLTUK", "C-6", "Seçmeli"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "ULS 335", "Avrupa Çalışmaları", "Doç. Dr. Ramazan İZOL", "C-5", "Seçmeli"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "ULS 303", "Türk Siyasal Yaşamı", "Doç. Dr. Ramazan İZOL", "C-6", "Zorunlu"),
            programEntry(ScheduleDay.FRIDAY, "13:30", "16:20", "ULS 381", "Mesleki İngilizce I", "Öğr. Gör. Hülya ÇELİK", "C-2", "Seçmeli")
        )
    )

    val internationalRelationsFourthYear = programSchedule(
        department = INTERNATIONAL_RELATIONS_DEPARTMENT,
        classYear = FOURTH_YEAR,
        updatedAt = "19.09.2026",
        sourcePage = 7,
        sourceUrl = INTERNATIONAL_RELATIONS_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "ULS 401", "Uluslararası İlişkilerde Güncel Sorunlar", "Doç. Dr. Mustafa ÖZTÜRK", "C-8", "Zorunlu"),
            programEntry(ScheduleDay.MONDAY, "14:30", "17:20", "ULS 475", "Sosyal Bilimlerde Araştırma Yöntemleri", "Arş. Gör. Dr. Pınar ARIKAN SİNKAYA", "C-6", "Zorunlu"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "ULS 409", "Bölgesel Politikalar: Kafkasya", "Prof. Dr. Hayati AKTAŞ", "C-8", "Seçmeli"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "ULS 427", "Akdeniz’de Bölgesel Güvenlik", "Dr. Öğr. Üyesi Durmuş Ali KOLTUK", "C-2", "Seçmeli"),
            programEntry(ScheduleDay.TUESDAY, "14:30", "17:20", "ULS 431", "Uluslararası Göç Politikaları", "Doç. Dr. Sanem ÖZER", "C-3", "Seçmeli"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "ULS 419", "Rusya Çalışmaları", "Doç. Dr. Mustafa ÖZTÜRK", "C-8", "Seçmeli"),
            programEntry(ScheduleDay.WEDNESDAY, "14:30", "17:20", "ULS 429", "Bölgesel Politikalar: Latin Amerika", "Doç. Dr. Ceren UYSAL OĞUZ", "C-9", "Seçmeli"),
            programEntry(ScheduleDay.THURSDAY, "14:30", "17:20", "ULS 407", "Uluslararası Hukuk I", "Arş. Gör. Dr. Selin ERKUL", "C-3", "Zorunlu"),
            programEntry(ScheduleDay.FRIDAY, "15:30", "18:20", "ULS 471", "Diplomatik İngilizce", "Öğr. Gör. Seda AKSUNGUR", "C-8", "Seçmeli")
        )
    )

    val computerEngineeringFirstYear = programSchedule(
        faculty = ENGINEERING_FACULTY,
        department = COMPUTER_ENGINEERING_DEPARTMENT,
        classYear = FIRST_YEAR,
        updatedAt = "Yayımlanmış Google Sheets",
        sourcePage = 1,
        sourceUrl = COMPUTER_ENGINEERING_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "CSE 101T", "Computer Programming I (1/2)", "Dr. Öğr. Üyesi Joseph William LEDET", "Amfi 4"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "CSE 181", "Natural Sciences (1/2)", "Prof. Dr. Melih GÜNAY", "Amfi 1"),
            programEntry(ScheduleDay.MONDAY, "15:30", "17:20", "CSE 105", "Introduction to Computer Science", "Dr. Öğr. Üyesi Mustafa Berkay YILMAZ", "Amfi 1"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "MAT 151", "Mathematics I", "Prof. Dr. Mehmet CENKCİ", "Hukuk Fak. Amfi 5"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "CSE 181", "Natural Sciences (2/2)", "Prof. Dr. Melih GÜNAY", "Amfi 1"),
            programEntry(ScheduleDay.TUESDAY, "15:30", "17:20", "TDB 101", "Turkish Language I (Şb 2 – BM)", "Öğr. Gör. Ömer Bahadır İLTER", "Amfi 1"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "MAT 151", "Mathematics I", "Prof. Dr. Mehmet CENKCİ", "Amfi 3 / Hukuk Fak. Amfi 5"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "FİZ 175", "Physics I Laboratory (Grup 1)", "Prof. Dr. Yasemin KÜÇÜK", "Fen Fak. Lab"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "17:20", "FİZ 175", "Physics I Laboratory (Grup 2)", "Prof. Dr. Yasemin KÜÇÜK", "Fen Fak. Lab"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "FİZ 173", "Physics I", "Prof. Dr. Yasemin KÜÇÜK", "Amfi 4"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "14:20", "CSE 101T", "Computer Programming I (2/2)", "Dr. Öğr. Üyesi Joseph William LEDET", "Amfi 4"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "10:20", "CSE 101L", "Computer Programming I Lab (Şb 1)", "Dr. Öğr. Üyesi Joseph William LEDET", "YLab1 + YLab2"),
            programEntry(ScheduleDay.FRIDAY, "10:30", "12:20", "CSE 101L", "Computer Programming I Lab (Şb 2)", "Dr. Öğr. Üyesi Joseph William LEDET", "YLab1 + YLab2")
        )
    )

    val computerEngineeringSecondYear = programSchedule(
        faculty = ENGINEERING_FACULTY,
        department = COMPUTER_ENGINEERING_DEPARTMENT,
        classYear = SECOND_YEAR,
        updatedAt = "Yayımlanmış Google Sheets",
        sourcePage = 1,
        sourceUrl = COMPUTER_ENGINEERING_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "ATA 101", "Atatürk İlkeleri ve İnkılap Tarihi I (Şb 1)", "Öğr. Gör. Dr. Nurdan ÇETİNKAYA", "BB01"),
            programEntry(ScheduleDay.MONDAY, "13:30", "17:20", "CSE 221", "Discrete Mathematics I", "Dr. Öğr. Üyesi Murat AK", "D206"),
            programEntry(ScheduleDay.TUESDAY, "10:30", "12:20", "CSE 211", "Digital Design Lab (Şb 1-2)", "Arş. Gör. Dr. Erdinç TÜRK", "D204"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "17:20", "CSE 203", "Object-Oriented Analysis and Design", "Prof. Dr. Ümit Deniz ULUŞAR", "Amfi 4"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "CSE 201", "Data Structures", "Arş. Gör. Dr. Taha Yiğit ALKAN", "Amfi 1"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "17:20", "CSE 211", "Digital Design", "Arş. Gör. Dr. Erdinç TÜRK", "Amfi 1"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "CSE 213", "Microcontroller Programming", "Prof. Dr. Alper BİLGE", "D206"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "CSE 201", "Data Structures Lab (Şb 1)", "Arş. Gör. Dr. Taha Yiğit ALKAN", "YLab1 + YLab2"),
            programEntry(ScheduleDay.THURSDAY, "15:30", "17:20", "CSE 201", "Data Structures Lab (Şb 2)", "Arş. Gör. Dr. Taha Yiğit ALKAN", "YLab1 + YLab2"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "15:20", "CSE 213", "Microcontroller Programming Lab (Şb 1)", "Prof. Dr. Alper BİLGE", "YLab1"),
            programEntry(ScheduleDay.FRIDAY, "15:30", "16:20", "CSE 213", "Microcontroller Programming Lab (Şb 2)", "Prof. Dr. Alper BİLGE", "YLab1")
        )
    )

    val computerEngineeringThirdYear = programSchedule(
        faculty = ENGINEERING_FACULTY,
        department = COMPUTER_ENGINEERING_DEPARTMENT,
        classYear = THIRD_YEAR,
        updatedAt = "Yayımlanmış Google Sheets",
        sourcePage = 1,
        sourceUrl = COMPUTER_ENGINEERING_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "08:30", "09:20", "CSE 301", "Algorithms Lab (Şb 1)", "Dr. Öğr. Üyesi Hüseyin Gökhan AKÇAY", "YLab1"),
            programEntry(ScheduleDay.MONDAY, "09:30", "10:20", "CSE 301", "Algorithms Lab (Şb 2)", "Dr. Öğr. Üyesi Hüseyin Gökhan AKÇAY", "YLab1"),
            programEntry(ScheduleDay.MONDAY, "10:30", "11:20", "CSE 301", "Algorithms Lab (Şb 3)", "Dr. Öğr. Üyesi Hüseyin Gökhan AKÇAY", "YLab1"),
            programEntry(ScheduleDay.MONDAY, "11:30", "12:20", "CSE 301", "Algorithms Lab (Şb 4)", "Dr. Öğr. Üyesi Hüseyin Gökhan AKÇAY", "YLab1"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "CSE 303", "Fundamentals of Operating Systems (1/2)", "Doç. Dr. Taner DANIŞMAN", "D205"),
            programEntry(ScheduleDay.MONDAY, "15:30", "17:20", "CSE 341", "Fundamentals of System Administration (1/2)", "Dr. Öğr. Üyesi Joseph William LEDET", "D205"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "12:20", "CSE 377", "Game Programming", "Doç. Dr. Alper ÖZCAN", "D206"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "17:20", "CSE 381", "Principles of User Interface Design", "Dr. Öğr. Üyesi Mustafa Berkay YILMAZ", "YLab1"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "12:20", "CSE 301", "Algorithms", "Dr. Öğr. Üyesi Hüseyin Gökhan AKÇAY", "Amfi 2"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "CSE 303", "Fundamentals of Operating Systems (2/2)", "Doç. Dr. Taner DANIŞMAN", "D205"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "17:20", "CSE 341", "Fundamentals of System Administration (2/2)", "Dr. Öğr. Üyesi Joseph William LEDET", "D205"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "17:20", "CSE 351", "Design Patterns", "Prof. Dr. Alper BİLGE", "D205"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "12:20", "CSE 321", "Statistical Inference and Computation", "Prof. Dr. Alper BİLGE", "D206")
        )
    )

    val computerEngineeringFourthYear = programSchedule(
        faculty = ENGINEERING_FACULTY,
        department = COMPUTER_ENGINEERING_DEPARTMENT,
        classYear = FOURTH_YEAR,
        updatedAt = "Yayımlanmış Google Sheets",
        sourcePage = 1,
        sourceUrl = COMPUTER_ENGINEERING_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "CSE 435", "Formal Languages and Automata", "Dr. Öğr. Üyesi Murat AK", "D206"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "CSE 425", "Network Security (1/2)", "Arş. Gör. Dr. Manolya ATALAY", "YLab2"),
            programEntry(ScheduleDay.MONDAY, "15:30", "17:20", "CSE 409", "Intro. to Natural Language Processing (1/2)", "Prof. Dr. Melih GÜNAY", "D204"),
            programEntry(ScheduleDay.MONDAY, "15:30", "17:20", "CSE 427", "Wireless Sensor Networks (1/2)", "Arş. Gör. Dr. Manolya ATALAY", "YLab2"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "12:20", "CSE 413", "Optimization Theory and Applications", "Arş. Gör. Dr. Taha Yiğit ALKAN", "D205"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "CSE 481", "Engineering Economics", "Doç. Dr. Alper ÖZCAN", "D206"),
            programEntry(ScheduleDay.TUESDAY, "15:30", "17:20", "CSE 409", "Intro. to Natural Language Processing (2/2)", "Prof. Dr. Melih GÜNAY", "D206"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "CSE 439", "Distributed and Parallel Computing (1/2)", "Doç. Dr. Taner DANIŞMAN", "D206"),
            programEntry(ScheduleDay.THURSDAY, "08:30", "12:20", "CSE 445", "Deep Learning", "Dr. Öğr. Üyesi Hüseyin Gökhan AKÇAY", "D205"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "CSE 427", "Wireless Sensor Networks (2/2)", "Arş. Gör. Dr. Manolya ATALAY", "D206"),
            programEntry(ScheduleDay.THURSDAY, "15:30", "17:20", "CSE 425", "Network Security (2/2)", "Arş. Gör. Dr. Manolya ATALAY", "D206"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "10:20", "CSE 439", "Distributed and Parallel Computing (2/2)", "Doç. Dr. Taner DANIŞMAN", "D206"),
            programEntry(ScheduleDay.FRIDAY, "10:30", "12:20", "CSE 483", "Entrepreneurship", "Prof. Dr. Ümit Deniz ULUŞAR", "D205"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "18:20", "CSE 415", "Fundamentals of Cloud Computing", "Arş. Gör. Dr. Taha Yiğit ALKAN", "Amfi 2"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "18:20", "CSE 433", "Advanced Mobile Programming", "Dr. Öğr. Üyesi Mustafa Berkay YILMAZ", "D206")
        )
    )

    val aiDataEngineeringFirstYear = programSchedule(
        faculty = ENGINEERING_FACULTY,
        department = AI_DATA_ENGINEERING_DEPARTMENT,
        classYear = FIRST_YEAR,
        updatedAt = "Yayımlanmış Google Sheets",
        sourcePage = 1,
        sourceUrl = AI_DATA_ENGINEERING_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "CSE 101T", "Computer Programming I (1/2)", "Dr. Öğr. Üyesi Joseph William LEDET", "Amfi 4"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "CSE 181", "Natural Sciences (1/2)", "Prof. Dr. Melih GÜNAY", "Amfi 1"),
            programEntry(ScheduleDay.MONDAY, "15:30", "17:20", "AIE 105", "Introduction to AI and Data Engineering", "Dr. Öğr. Üyesi Mustafa Berkay YILMAZ", "Amfi 1"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "MAT 151", "Mathematics I", "Prof. Dr. Mehmet CENKCİ", "Fen Fak."),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "CSE 181", "Natural Sciences (2/2)", "Prof. Dr. Melih GÜNAY", "Amfi 1"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "MAT 151", "Mathematics I", "Prof. Dr. Mehmet CENKCİ", "Amfi 3"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "FİZ 175", "Physics I Laboratory (Grup 1)", "Prof. Dr. Yasemin KÜÇÜK", "Fen Fak. Lab"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "17:20", "FİZ 175", "Physics I Laboratory (Grup 2)", "Prof. Dr. Yasemin KÜÇÜK", "Fen Fak. Lab"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "FİZ 173", "Physics I", "Prof. Dr. Yasemin KÜÇÜK", "Amfi 4"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "14:20", "CSE 101T", "Computer Programming I (2/2)", "Dr. Öğr. Üyesi Joseph William LEDET", "Amfi 4"),
            programEntry(ScheduleDay.THURSDAY, "15:30", "17:20", "TDB 101", "Turkish Language I (Şb 5 – YZVM)", "Türk Dili Bölüm Başkanlığı", "Amfi 1"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "10:20", "CSE 101L", "Computer Programming I Lab (Şb 1)", "Dr. Öğr. Üyesi Joseph William LEDET", "YLab1 + YLab2"),
            programEntry(ScheduleDay.FRIDAY, "10:30", "12:20", "CSE 101L", "Computer Programming I Lab (Şb 2)", "Dr. Öğr. Üyesi Joseph William LEDET", "YLab1 + YLab2")
        )
    )

    val aiDataEngineeringSecondYear = programSchedule(
        faculty = ENGINEERING_FACULTY,
        department = AI_DATA_ENGINEERING_DEPARTMENT,
        classYear = SECOND_YEAR,
        updatedAt = "Yayımlanmış Google Sheets",
        sourcePage = 1,
        sourceUrl = AI_DATA_ENGINEERING_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "ATA 101", "Atatürk İlkeleri ve İnkılap Tarihi I (Şb 2)", "Öğr. Gör. Dr. Koray ERGİN", "BB04"),
            programEntry(ScheduleDay.MONDAY, "13:30", "17:20", "CSE 221", "Discrete Mathematics I", "Dr. Öğr. Üyesi Murat AK", "D206"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "17:20", "CSE 203", "Object-Oriented Analysis and Design", "Prof. Dr. Ümit Deniz ULUŞAR", "Amfi 4"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "CSE 201", "Data Structures", "Prof. Dr. Ümit Deniz ULUŞAR", "Amfi 1"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "AIE 205", "Fundamentals of Data Science", "Doç. Dr. Taner DANIŞMAN", "YLab1"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "CSE 201", "Data Structures Lab (Şb 1)", "Prof. Dr. Ümit Deniz ULUŞAR", "YLab1 + YLab2"),
            programEntry(ScheduleDay.THURSDAY, "15:30", "17:20", "CSE 201", "Data Structures Lab (Şb 2)", "Prof. Dr. Ümit Deniz ULUŞAR", "YLab1 + YLab2"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "12:20", "CSE 321", "Statistical Inference and Computation", "Prof. Dr. Alper BİLGE", "D206")
        )
    )

    val aiDataEngineeringThirdYear = programSchedule(
        faculty = ENGINEERING_FACULTY,
        department = AI_DATA_ENGINEERING_DEPARTMENT,
        classYear = THIRD_YEAR,
        updatedAt = "Yayımlanmış Google Sheets",
        sourcePage = 1,
        sourceUrl = AI_DATA_ENGINEERING_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "08:30", "09:20", "CSE 301", "Algorithms Lab (Şb 1)", "Dr. Öğr. Üyesi Hüseyin Gökhan AKÇAY", "YLab1"),
            programEntry(ScheduleDay.MONDAY, "09:30", "10:20", "CSE 301", "Algorithms Lab (Şb 2)", "Dr. Öğr. Üyesi Hüseyin Gökhan AKÇAY", "YLab1"),
            programEntry(ScheduleDay.MONDAY, "10:30", "11:20", "CSE 301", "Algorithms Lab (Şb 3)", "Dr. Öğr. Üyesi Hüseyin Gökhan AKÇAY", "YLab1"),
            programEntry(ScheduleDay.MONDAY, "11:30", "12:20", "CSE 301", "Algorithms Lab (Şb 4)", "Dr. Öğr. Üyesi Hüseyin Gökhan AKÇAY", "YLab1"),
            programEntry(ScheduleDay.MONDAY, "15:30", "17:20", "CSE 409", "Intro. to Natural Language Processing (1/2)", "Prof. Dr. Melih GÜNAY", "D204"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "12:20", "CSE 413", "Optimization Theory and Applications", "Arş. Gör. Dr. Taha Yiğit ALKAN", "D205"),
            programEntry(ScheduleDay.TUESDAY, "15:30", "17:20", "CSE 409", "Intro. to Natural Language Processing (2/2)", "Prof. Dr. Melih GÜNAY", "D206"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "12:20", "CSE 301", "Algorithms", "Dr. Öğr. Üyesi Hüseyin Gökhan AKÇAY", "Amfi 2"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "AIE 335", "Data Pipelines and Infrastructure (2. blok)", "Arş. Gör. Dr. Erdinç TÜRK", "D206"),
            programEntry(ScheduleDay.THURSDAY, "08:30", "12:20", "CSE 445", "Deep Learning", "Dr. Öğr. Üyesi Hüseyin Gökhan AKÇAY", "D205"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "AIE 335", "Data Pipelines and Infrastructure", "Arş. Gör. Dr. Erdinç TÜRK", "BB01"),
            programEntry(ScheduleDay.FRIDAY, "13:30", "17:20", "CSE 415", "Fundamentals of Cloud Computing", "Arş. Gör. Dr. Taha Yiğit ALKAN", "D205")
        )
    )

    val aiDataEngineeringFourthYear = programSchedule(
        faculty = ENGINEERING_FACULTY,
        department = AI_DATA_ENGINEERING_DEPARTMENT,
        classYear = FOURTH_YEAR,
        updatedAt = "Yayımlanmış Google Sheets",
        sourcePage = 1,
        sourceUrl = AI_DATA_ENGINEERING_SOURCE_URL,
        entries = emptyList(),
        sourceWarning = "Yapay Zeka ve Veri Mühendisliği programında henüz 4. sınıf bulunmuyor; bölümün yayımladığı programda yalnızca 1-3. sınıflar yer alıyor."
    )

    // Mühendislik Fakültesi 2026-2027 güz dönemi bölüm programları.
    // Kaynak tablolar haftalık çizelge biçiminde olduğundan dersler gün/saat kartlarına dönüştürüldü.
    val electricalEngineeringFirstYear = engineeringSchedule(
        department = ELECTRICAL_ELECTRONICS_ENGINEERING_DEPARTMENT,
        classYear = FIRST_YEAR,
        sourceUrl = ELECTRICAL_ELECTRONICS_ENGINEERING_SOURCE_URL,
        sourcePage = 1,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "ATA101", "Atatürk İlkeleri ve İnkılap Tarihi I", "", "Amfi 3"),
            programEntry(ScheduleDay.MONDAY, "13:30", "14:20", "KPD101", "Kariyer Planlama", "", "D202"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "EEE1005", "Ayrık Matematik ve Algoritmalar", "", "Amfi 3 ve Yazılım Lab. 1"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "MAT151", "Mathematics-I", "", "Dz01"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "12:20", "EEM1001", "Elektrik-Elektronik Mühendisliğine Giriş - Şube 1", "", "Dz01"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "12:20", "EEM1001", "Elektrik-Elektronik Mühendisliğine Giriş - Şube 2", "", "D202"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "TDB101", "Türk Dili-I", "", "Amfi 1"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "17:20", "MAT151", "Mathematics-I", "", "Dz01"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "KIM175", "Fundamentals of Chemistry", "", "Dz01"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "FIZ175", "Physics Laboratory", "", "Fen Fakültesi"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "17:20", "FIZ173", "Physics-I", "", "Dz01")
        )
    )

    val electricalEngineeringSecondYear = engineeringSchedule(
        department = ELECTRICAL_ELECTRONICS_ENGINEERING_DEPARTMENT,
        classYear = SECOND_YEAR,
        sourceUrl = ELECTRICAL_ELECTRONICS_ENGINEERING_SOURCE_URL,
        sourcePage = 1,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "EEM201", "Mühendislik Matematiği", "", "Dz01"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "EEM213", "Mantıksal Devreler", "", "Dz01"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "12:20", "EEM203", "Devre Teorisi-I - Şube 1", "", "D202"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "17:20", "EEM203", "Devre Teorisi-I - Şube 2", "", "D202"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "12:20", "EEM209", "Probability Theory & Statistical Analysis", "", "D205"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "17:20", "EEM207", "Elektromanyetik Alan Teorisi - Şube 1", "", "BB01"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "17:20", "EEM207", "Elektromanyetik Alan Teorisi - Şube 2", "", "Amfi 3"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:20", "EEM201", "Mühendislik Matematiği", "", "D202"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "EEM217", "Devre Laboratuvarı I", "", "Ölçme Lab. - 1. kat"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "17:20", "EEM215", "Mantıksal Devreler Laboratuvarı", "", "Ölçme Lab. - 1. kat")
        )
    )

    val electricalEngineeringThirdYear = engineeringSchedule(
        department = ELECTRICAL_ELECTRONICS_ENGINEERING_DEPARTMENT,
        classYear = THIRD_YEAR,
        sourceUrl = ELECTRICAL_ELECTRONICS_ENGINEERING_SOURCE_URL,
        sourcePage = 1,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "08:30", "12:20", "EEM322", "Electronics-II - Şube 1", "", "D202"),
            programEntry(ScheduleDay.MONDAY, "08:30", "12:20", "EEM322", "Electronics-II - Şube 2", "", "D205"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "EEM309", "Electrical Machines Laboratory", "", "Güç Elektroniği Lab. - Zemin kat"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "12:20", "EEM341", "Elektrik Kumanda Devreleri ve PLC Programlama", "", "Dz01"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "EEM313", "İleri Bilgisayar Programlama", "", "Yazılım Lab. 2"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "10:20", "EEM307", "Electrical Machines", "", "D203"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "EEM391", "Introduction to Economics", "", "D203"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "16:20", "EEM305", "Sinyaller ve Sistemler", "", "D202"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "EEM357", "Elektrik Enerji Dağıtımı", "", "D203"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "EEM381", "Introduction to Microwave Theory", "", "C205"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "EEM307", "Electrical Machines", "", "D203"),
            programEntry(ScheduleDay.FRIDAY, "09:30", "12:20", "EEM325", "Electronics Laboratory II", "", "Ölçme Lab. - 1. kat"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "17:20", "EEM315", "Mühendislikte Hesaplamalı Yöntemler", "", "Yazılım Lab. 2")
        )
    )

    val electricalEngineeringFourthYear = engineeringSchedule(
        department = ELECTRICAL_ELECTRONICS_ENGINEERING_DEPARTMENT,
        classYear = FOURTH_YEAR,
        sourceUrl = ELECTRICAL_ELECTRONICS_ENGINEERING_SOURCE_URL,
        sourcePage = 1,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "08:30", "11:20", "EEE487", "Fundamentals of Antenna Theory", "", "D203"),
            programEntry(ScheduleDay.MONDAY, "08:30", "11:20", "EEE451", "PLC Uygulamaları ve SCADA Sistemleri", "", "Bz01"),
            programEntry(ScheduleDay.MONDAY, "12:30", "13:20", "EEM401", "Mezuniyet Projesi-I", "", ""),
            programEntry(ScheduleDay.MONDAY, "14:30", "17:20", "EEE473", "Cellular Communication", "", "D202"),
            programEntry(ScheduleDay.MONDAY, "14:30", "17:20", "EEE417", "Deep Learning", "", "D203"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "EEE425", "Sayısal Sinyal İşleme", "", "D203"),
            programEntry(ScheduleDay.TUESDAY, "12:30", "13:20", "EEM401", "Mezuniyet Projesi-I", "", ""),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "EEM443", "Bilim Tarihi", "", "D203"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "EEE477", "Digital Communication Laboratory", "", "Sayısal Elektronik Lab."),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "EEE447", "Otomatik Kontrol Laboratuvarı", "", "Tesis Lab."),
            programEntry(ScheduleDay.WEDNESDAY, "14:30", "17:20", "EEE431", "Image Processing", "", "D203"),
            programEntry(ScheduleDay.WEDNESDAY, "14:30", "17:20", "EEE459", "Power System Analysis", "", "C210"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "EEE411", "İleri Mantık Devre Tasarımı", "", "C210"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "EEE499", "Yenilenebilir Enerji Kaynakları", "", "Bz01"),
            programEntry(ScheduleDay.THURSDAY, "14:30", "17:20", "EEE483", "Elektromanyetik Uyumluluğa Giriş", "", "D202"),
            programEntry(ScheduleDay.THURSDAY, "14:30", "17:20", "EEE427", "Biyomedikal Mühendislik Uygulamalı Çözümler", "", "Dz01"),
            programEntry(ScheduleDay.FRIDAY, "09:30", "11:20", "EEE441", "İş Sağlığı ve Güvenliği", "", "D202"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "17:20", "EEE457", "High Voltage Techniques", "", "D202")
        )
    )

    val environmentalEngineeringFirstYear = engineeringSchedule(
        department = ENVIRONMENTAL_ENGINEERING_DEPARTMENT,
        classYear = FIRST_YEAR,
        sourceUrl = ENVIRONMENTAL_ENGINEERING_SOURCE_URL,
        sourcePage = 1,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "", "Introduction to Environmental Engineering", "", "C202"),
            programEntry(ScheduleDay.MONDAY, "15:30", "18:20", "", "Basic Computer Algorithms", "", "CAD1"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "", "Physics I", "", "C202"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "", "Physics I Lab.", "", "Fen Fakültesi"),
            programEntry(ScheduleDay.TUESDAY, "15:30", "17:20", "", "Mathematics I", "", "C202"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "", "Mathematics I", "", "C202"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "17:20", "", "Genel Kimya I", "", "C202"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:20", "", "Türk Dili I", "", "Amfi 1"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "", "Technical English II", "", "C202"),
            programEntry(ScheduleDay.THURSDAY, "16:30", "18:20", "", "Kariyer Planlama", "", "C202")
        )
    )

    val environmentalEngineeringSecondYear = engineeringSchedule(
        department = ENVIRONMENTAL_ENGINEERING_DEPARTMENT,
        classYear = SECOND_YEAR,
        sourceUrl = ENVIRONMENTAL_ENGINEERING_SOURCE_URL,
        sourcePage = 1,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "", "Atatürk İlkeleri ve İnkılap Tarihi I", "", "BB04"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "", "Akışkanlar Mekaniği", "", "C202"),
            programEntry(ScheduleDay.MONDAY, "16:30", "18:20", "", "Diferansiyel Denklemler", "", "C202"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "", "Çevre Mikrobiyolojisi", "", "C204"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "", "Çevre Mikrobiyolojisi Laboratuvarı", "", "Laboratuvar"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "", "Diferansiyel Denklemler", "", "C206"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "", "Fundamentals of Environmental Engineering", "", "C204"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "", "Environmental Chemistry", "", "C204"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "12:20", "", "Teknik Resim", "", "TRS"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "17:20", "", "Ölçme Bilgisi", "", "C202")
        )
    )

    val environmentalEngineeringThirdYear = engineeringSchedule(
        department = ENVIRONMENTAL_ENGINEERING_DEPARTMENT,
        classYear = THIRD_YEAR,
        sourceUrl = ENVIRONMENTAL_ENGINEERING_SOURCE_URL,
        sourcePage = 1,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "", "Su Temini Mühendisliği - Teorik", "", "C204"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "", "Su Temini Mühendisliği - Uygulama", "", "TRS/CAD1"),
            programEntry(ScheduleDay.MONDAY, "15:30", "17:20", "", "Bilgisayar Okuma ve Yazma Becerileri", "", "C204"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "", "Toprak ve Yeraltı Suyu Kirliliği", "", "BTO"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "", "Physical Unit Operations", "", "C204"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "", "Su Kalitesi Kontrolü", "", "C201"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "16:20", "", "Kimyasal Prosesler", "", "C201"),
            programEntry(ScheduleDay.THURSDAY, "08:30", "12:20", "", "Katı Atık Yönetimi", "", "C202")
        )
    )

    val environmentalEngineeringFourthYear = engineeringSchedule(
        department = ENVIRONMENTAL_ENGINEERING_DEPARTMENT,
        classYear = FOURTH_YEAR,
        sourceUrl = ENVIRONMENTAL_ENGINEERING_SOURCE_URL,
        sourcePage = 1,
        entries = listOf(
            programEntry(ScheduleDay.TUESDAY, "10:30", "12:20", "", "Atıksuların Arıtılması", "", "C201"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "", "Atıksuların Arıtılması", "", "C201"),
            programEntry(ScheduleDay.TUESDAY, "15:30", "17:20", "", "Çevre Hukuku", "", "C201"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "", "Microbiology of WW", "", "C204"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "", "Çevresel Modelleme - Teorik", "", "C205"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "17:20", "", "Çevresel Modelleme - Uygulama", "", "CAD1"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "", "Air Pollution Modelling", "", "C201"),
            programEntry(ScheduleDay.FRIDAY, "09:30", "12:20", "", "Air Pollution Control", "", "C202"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "16:20", "", "Yeşil Dönüşüm Uygulamaları", "", "Uzaktan Eğitim")
        )
    )

    val foodEngineeringFirstYear = engineeringSchedule(
        department = FOOD_ENGINEERING_DEPARTMENT,
        classYear = FIRST_YEAR,
        sourceUrl = FOOD_ENGINEERING_SOURCE_URL,
        sourcePage = 1,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "08:30", "10:15", "YBD101", "İngilizce I", "Öğr. Gör. Burçak AKINCI", "Amfi 3"),
            programEntry(ScheduleDay.MONDAY, "10:30", "12:15", "ATA101", "Atatürk İlkeleri ve İnkılâp Tarihi I", "Öğr. Gör. Dr. Fatma ÇETİN ADIGÜZEL", "Amfi 3"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:15", "MAT167", "Matematik I", "Doç. Dr. Muhammet Cihat DAĞLI", "C206"),
            programEntry(ScheduleDay.MONDAY, "16:30", "17:15", "MAT167", "Matematik I Uyg.", "Doç. Dr. Muhammet Cihat DAĞLI", "C206"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "10:15", "KİM171", "Genel Kimya I", "Dr. Öğr. Üyesi Saadettin Yavuz UĞURLU", "C207"),
            programEntry(ScheduleDay.TUESDAY, "10:30", "12:15", "GIDA199", "Biyoloji", "Arş. Gör. Dr. Gürcü Aybige ÇAKMAK", "C207"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:15", "GIDA101", "Temel Bilgisayar ve Algoritma", "Dr. Öğr. Üyesi Saadettin Yavuz UĞURLU", "Enformatik Lab-01"),
            programEntry(ScheduleDay.TUESDAY, "15:30", "17:15", "GIDA101", "Temel Bilgisayar ve Algoritma Uyg.", "Dr. Öğr. Üyesi Saadettin Yavuz UĞURLU", "Enformatik Lab-01"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:15", "FİZ167", "Fizik I", "Prof. Dr. İsmail Hakkı SARPÜN", "C207"),
            programEntry(ScheduleDay.THURSDAY, "08:30", "10:15", "KİM171", "Genel Kimya I Uyg.", "Dr. Öğr. Üyesi Saadettin Yavuz UĞURLU", "B102"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:15", "TDB101", "Türk Dili I", "Öğr. Gör. Ömer Bahadır İLTER", "Amfi 1"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:15", "FİZ167", "Fizik I Uyg.", "Prof. Dr. İsmail Hakkı SARPÜN", "C207"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "10:15", "GIDA113", "Gıda Mühendisliğine Giriş", "Doç. Dr. Barçın KARAKAŞ BUDAK", "C206")
        )
    )

    val foodEngineeringSecondYear = engineeringSchedule(
        department = FOOD_ENGINEERING_DEPARTMENT,
        classYear = SECOND_YEAR,
        sourceUrl = FOOD_ENGINEERING_SOURCE_URL,
        sourcePage = 2,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:15", "GIDA217", "Mühendislik İstatistiği", "Prof. Dr. M. Ziya FIRAT", "C207"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:15", "GIDA215", "Kütle ve Enerji Denklikleri", "Doç. Dr. Elif AYKIN DİNÇER", "C207"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "10:15", "GIDA239", "Diferansiyel Denklemler", "Dr. Öğr. Üyesi Mutlu GÜLOĞLU", "C206"),
            programEntry(ScheduleDay.TUESDAY, "10:30", "12:15", "GIDA213", "Gıda Kimyası I", "Prof. Dr. Mustafa ERBAŞ", "C206"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:15", "GIDA209", "Genel Mikrobiyoloji", "Doç. Dr. Reha Onur AZİZOĞLU", "C207"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "10:15", "GIDA235", "Analitik Kimya", "Prof. Dr. Numan HODA", "C207"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:15", "GIDA235", "Analitik Kimya Uyg.", "Prof. Dr. Numan HODA", "C207"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "16:15", "GIDA239", "Diferansiyel Denklemler", "Dr. Öğr. Üyesi Mutlu GÜLOĞLU", "C206"),
            programEntry(ScheduleDay.WEDNESDAY, "16:30", "17:15", "GIDA239", "Diferansiyel Denklemler Uyg.", "Dr. Öğr. Üyesi Mutlu GÜLOĞLU", "C206"),
            programEntry(ScheduleDay.THURSDAY, "08:30", "09:15", "GIDA241", "Bilim Felsefesi ve Mühendislik Etiği", "Prof. Dr. Osman Kadir TOPUZ", "C207"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "14:15", "GIDA209", "Genel Mikrobiyoloji Uyg.", "Doç. Dr. Reha Onur AZİZOĞLU", "LAB"),
            programEntry(ScheduleDay.FRIDAY, "10:30", "11:15", "GIDA245", "Girişimcilik", "Öğr. Gör. Metehan YAYKAŞLI", "B102"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "15:15", "GIDA233", "Fizikokimya", "Doç. Dr. Muammer DEMİR", "Amfi 3")
        )
    )

    val foodEngineeringThirdYear = engineeringSchedule(
        department = FOOD_ENGINEERING_DEPARTMENT,
        classYear = THIRD_YEAR,
        sourceUrl = FOOD_ENGINEERING_SOURCE_URL,
        sourcePage = 3,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "08:30", "10:15", "GIDA333", "Gıda Teknolojisi", "Doç. Dr. Muammer DEMİR · Doç. Dr. Mehmet TORUN · Doç. Dr. Firuze ERGİN ZEREN", "C206"),
            programEntry(ScheduleDay.MONDAY, "10:30", "12:15", "GIDA325", "Kütle Aktarımı", "Prof. Dr. Mustafa Kemal USLU", "Amfi 2"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:15", "GIDA327", "Temel İşlemler I", "Prof. Dr. Mustafa KARHAN", "Amfi 1"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:15", "GIDA453", "Gıda Mevzuatı", "Doç. Dr. Mehmet TORUN", "C206", "Seçmeli"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:15", "GIDA463", "Teknik İngilizce", "Doç. Dr. Barçın KARAKAŞ BUDAK", "B102", "Seçmeli"),
            programEntry(ScheduleDay.TUESDAY, "15:30", "17:15", "GIDA467", "Endüstriyel Mikrobiyoloji", "Prof. Dr. İrfan TURHAN", "C206", "Seçmeli"),
            programEntry(ScheduleDay.TUESDAY, "15:30", "17:15", "GIDA411", "Gıda Hijyeni", "Doç. Dr. Firuze ERGİN ZEREN", "B102", "Seçmeli"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "10:15", "GIDA333", "Gıda Teknolojisi", "Doç. Dr. Muammer DEMİR · Doç. Dr. Mehmet TORUN · Doç. Dr. Firuze ERGİN ZEREN", "C206"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:15", "GIDA451", "Duyusal Analiz", "Prof. Dr. Mustafa Kemal USLU", "C206", "Seçmeli"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:15", "GIDA415", "Beslenme İlkeleri", "Prof. Dr. Mustafa ERBAŞ", "B102", "Seçmeli"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:15", "GIDA323", "Isı Aktarımı", "Prof. Dr. Ayhan TOPUZ", "C206"),
            programEntry(ScheduleDay.THURSDAY, "15:30", "17:15", "GIDA465", "Gıda Makine ve Ekipmanları", "Prof. Dr. Ahmet KÜÇÜKÇETİN", "B102", "Seçmeli"),
            programEntry(ScheduleDay.THURSDAY, "15:30", "17:15", "GIDA457", "Mikrobiyal Kalite Kontrol", "Doç. Dr. Firuze ERGİN ZEREN", "C206", "Seçmeli"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "10:15", "GIDA247", "Turizm İşletmeciliğinde Yönetim ve Organizasyon", "Dr. Öğr. Üyesi Zeynep KARSAVURAN", "B102", "Seçmeli"),
            programEntry(ScheduleDay.FRIDAY, "13:30", "17:15", "GIDA329", "Gıda Analizleri I", "Doç. Dr. Firuze ERGİN ZEREN · Arş. Gör. Dr. Gürcü Aybige ÇAKMAK", "LAB")
        )
    )

    val foodEngineeringFourthYear = engineeringSchedule(
        department = FOOD_ENGINEERING_DEPARTMENT,
        classYear = FOURTH_YEAR,
        sourceUrl = FOOD_ENGINEERING_SOURCE_URL,
        sourcePage = 4,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "10:30", "12:15", "GIDA355", "Meyve ve Sebze İşleme Teknolojisi", "Prof. Dr. Ayhan TOPUZ", "B102", "Seçmeli"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:15", "GIDA355", "Meyve ve Sebze İşleme Teknolojisi Uyg.", "Arş. Gör. Dr. Serenay AŞIK AYGÜN", "LAB", "Seçmeli"),
            programEntry(ScheduleDay.MONDAY, "15:30", "17:15", "GIDA433", "Enstrümental Analiz Uyg.", "Dr. Öğr. Üyesi Murat YANAT", "B102"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "10:15", "GIDA433", "Enstrümental Analiz", "Dr. Öğr. Üyesi Murat YANAT", "B102"),
            programEntry(ScheduleDay.TUESDAY, "10:30", "12:15", "GIDA352", "Fermente Ürünler Teknolojisi", "Doç. Dr. Barçın KARAKAŞ BUDAK", "C206", "Seçmeli"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:15", "GIDA352", "Fermente Ürünler Teknolojisi Uyg.", "Arş. Gör. Dr. Gürcü Aybige ÇAKMAK", "LAB", "Seçmeli"),
            programEntry(ScheduleDay.TUESDAY, "15:30", "17:15", "GIDA467", "Endüstriyel Mikrobiyoloji", "Prof. Dr. İrfan TURHAN", "C206", "Seçmeli"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:15", "GIDA419", "Beyaz Et Teknolojisi", "Doç. Dr. Elif AYKIN DİNÇER", "B102", "Seçmeli"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:15", "GIDA453", "Gıda Mevzuatı", "Doç. Dr. Mehmet TORUN", "C206", "Seçmeli"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:15", "GIDA463", "Teknik İngilizce", "Doç. Dr. Barçın KARAKAŞ BUDAK", "B102", "Seçmeli"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "17:15", "GIDA435", "Gıda Analizleri II", "Prof. Dr. Osman Kadir TOPUZ · Arş. Gör. Dr. Serenay AŞIK AYGÜN", "LAB"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "17:15", "GIDA411", "Gıda Hijyeni", "Doç. Dr. Firuze ERGİN ZEREN", "B102", "Seçmeli"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:15", "GIDA451", "Duyusal Analiz", "Prof. Dr. Mustafa Kemal USLU", "C206", "Seçmeli"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:15", "GIDA415", "Beslenme İlkeleri", "Prof. Dr. Mustafa ERBAŞ", "B102", "Seçmeli"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:15", "GIDA357", "Meşrubat Teknolojisi", "Prof. Dr. Mustafa KARHAN", "C207", "Seçmeli"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:15", "GIDA351", "Süt Teknolojisi", "Prof. Dr. Ahmet KÜÇÜKÇETİN", "B102", "Seçmeli"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:15", "GIDA357", "Meşrubat Teknolojisi Uyg.", "Prof. Dr. Mustafa KARHAN", "LAB", "Seçmeli"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:15", "GIDA351", "Süt Teknolojisi Uyg.", "Prof. Dr. Ahmet KÜÇÜKÇETİN", "LAB", "Seçmeli"),
            programEntry(ScheduleDay.THURSDAY, "15:30", "17:15", "GIDA465", "Gıda Makine ve Ekipmanları", "Prof. Dr. Ahmet KÜÇÜKÇETİN", "B102", "Seçmeli"),
            programEntry(ScheduleDay.THURSDAY, "15:30", "17:15", "GIDA457", "Mikrobiyal Kalite Kontrol", "Doç. Dr. Firuze ERGİN ZEREN", "C206", "Seçmeli"),
            programEntry(ScheduleDay.FRIDAY, "10:30", "12:15", "GIDA431", "Gıda İşletmelerinin Projelendirilmesi ve Organizasyon", "Prof. Dr. Muharrem CERTEL", "C207"),
            programEntry(ScheduleDay.FRIDAY, "13:30", "15:15", "PFE401", "Öğretmenlik Uygulaması", "Doç. Dr. Reha Onur AZİZOĞLU", "C207"),
            programEntry(ScheduleDay.FRIDAY, "15:30", "16:15", "GIDA331", "Birim Dışı Uygulama-I", "Dr. Öğr. Üyesi Murat YANAT", ""),
            programEntry(ScheduleDay.FRIDAY, "16:30", "17:15", "GIDA331", "Birim Dışı Uygulama-II", "Dr. Öğr. Üyesi Murat YANAT", "")
        )
    )

    val civilEngineeringFirstYear = engineeringSchedule(
        department = CIVIL_ENGINEERING_DEPARTMENT,
        classYear = FIRST_YEAR,
        sourceUrl = CIVIL_ENGINEERING_SOURCE_URL,
        sourcePage = 1,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "", "İngilizce I", "", "BB04"),
            programEntry(ScheduleDay.MONDAY, "15:30", "17:20", "", "İnşaat Mühendisliğine Giriş", "", "Amfi 2"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "", "Genel Kimya", "", "Amfi 2"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "17:20", "", "Matematik I", "", "C211"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "12:20", "", "Temel Bilgisayar ve Algoritma", "", "Enformatik Lab."),
            programEntry(ScheduleDay.THURSDAY, "08:30", "12:20", "", "Bilgisayar Uygulamalı Teknik Resim - Şube 1", "", "TRS"),
            programEntry(ScheduleDay.THURSDAY, "15:30", "17:20", "", "Türk Dili I", "", "Amfi 1"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "12:20", "", "Fizik I", "", "C213"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "18:20", "", "Bilgisayar Uygulamalı Teknik Resim - Şube 2", "", "TRS")
        )
    )

    val civilEngineeringSecondYear = engineeringSchedule(
        department = CIVIL_ENGINEERING_DEPARTMENT,
        classYear = SECOND_YEAR,
        sourceUrl = CIVIL_ENGINEERING_SOURCE_URL,
        sourcePage = 1,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "", "Atatürk İlkeleri ve İnkılapları Tarihi I", "", "Amfi 4"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "12:20", "", "Mukavemet I", "", "C213"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "", "Ölçme Bilgisi", "", "C213"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "", "Yapı Elemanları", "", "C213"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "16:20", "", "Malzeme Bilimi", "", "C213"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "", "Dinamik", "", "C213"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "17:20", "", "Diferansiyel Denklemler", "", "C213"),
            programEntry(ScheduleDay.FRIDAY, "09:30", "12:20", "", "Sayısal Analiz", "", "C209")
        )
    )

    val civilEngineeringThirdYear = engineeringSchedule(
        department = CIVIL_ENGINEERING_DEPARTMENT,
        classYear = THIRD_YEAR,
        sourceUrl = CIVIL_ENGINEERING_SOURCE_URL,
        sourcePage = 1,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "", "Yapı İşletmesi", "", "C213"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "", "Hidrolik", "", "C213"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "12:20", "", "Betonarme I - Şube 1", "", "C203"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "12:20", "", "Betonarme I - Şube 2", "", "C209"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "16:20", "", "Hidroloji", "", "C209"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "", "Yapı Statiği II", "", "C209"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "11:20", "", "Karayolları Mühendisliği", "", "Amfi 2"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "18:20", "", "Zemin Mekaniği I", "", "C213")
        )
    )

    val civilEngineeringFourthYear = engineeringSchedule(
        department = CIVIL_ENGINEERING_DEPARTMENT,
        classYear = FOURTH_YEAR,
        sourceUrl = CIVIL_ENGINEERING_SOURCE_URL,
        sourcePage = 1,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "", "Su Temini ve Kanalizasyon", "", "C209"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "", "Yapıların Yalıtımı ve Koruması", "", "C203", "Seçmeli"),
            programEntry(ScheduleDay.MONDAY, "17:30", "18:20", "", "Öğretmenlik Uygulaması", "", "", "İsteğe bağlı"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "", "Karayolu Tasarımı", "", "C212", "Seçmeli"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "", "Betonarme Yapı Tasarımı - Şube 1", "", "C203", "Seçmeli"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "", "Betonarme Yapı Tasarımı - Şube 2", "", "C209", "Seçmeli"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "", "Planlama Teknikleri I", "", "C203", "Seçmeli"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "", "Uygarlık Tarihi", "", "C209", "Seçmeli"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "16:20", "", "Temel İnşaatı II", "", "C203", "Seçmeli"),
            programEntry(ScheduleDay.WEDNESDAY, "16:30", "18:20", "", "İnşaat Mühendisliğinde Sürdürülebilirlik", "", "C203", "Seçmeli"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "", "Çelik Yapı Tasarımı", "", "C203", "Seçmeli"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "", "Depreme Dayanıklı Yapı Tasarımı İlkeleri", "", "C203", "Seçmeli"),
            programEntry(ScheduleDay.THURSDAY, "16:30", "18:20", "", "Seminer Çalışması", "", "", "Ders"),
            programEntry(ScheduleDay.FRIDAY, "09:30", "12:20", "", "Yapı Elemanlarının Burkulması", "", "C203", "Seçmeli"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "17:20", "", "Kıyı Mühendisliğine Giriş", "", "C209", "Seçmeli")
        )
    )

    val geologyEngineeringFirstYear = engineeringSchedule(
        department = GEOLOGY_ENGINEERING_DEPARTMENT,
        classYear = FIRST_YEAR,
        sourceUrl = GEOLOGY_ENGINEERING_SOURCE_URL,
        sourcePage = 1,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "", "Atatürk İlkeleri ve İnkılap Tarihi I", "", "Amfi 1"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "", "İngilizce I", "", "Amfi 2"),
            programEntry(ScheduleDay.TUESDAY, "10:30", "12:20", "", "Fizik I", "", "C211"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "17:20", "", "Matematik I", "", "C211"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "", "Jeoloji Mühendisliğine Giriş", "", "C208"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "17:20", "", "Genel Kimya I", "", "C202"),
            programEntry(ScheduleDay.THURSDAY, "08:30", "12:20", "", "Temel Bilgisayar ve Algoritma", "", "CAD-I"),
            programEntry(ScheduleDay.THURSDAY, "14:30", "15:20", "", "Kariyer Planlama", "", "B102"),
            programEntry(ScheduleDay.THURSDAY, "15:30", "17:20", "", "Türk Dili I", "", "Amfi 1"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "10:20", "", "Fizik I (Laboratuvar)", "", "Fen Fakültesi A Blok Fizik Bölümü Laboratuvarı")
        )
    )

    val geologyEngineeringSecondYear = engineeringSchedule(
        department = GEOLOGY_ENGINEERING_DEPARTMENT,
        classYear = SECOND_YEAR,
        sourceUrl = GEOLOGY_ENGINEERING_SOURCE_URL,
        sourcePage = 1,
        entries = listOf(
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "", "Optik Mineraloji", "", "C208"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "17:20", "", "Yapısal Jeoloji", "", "C210"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "", "Mühendislikte İstatistik", "", "CAD-I"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "16:20", "", "Kristalografi", "", "C208"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "", "Sedimantoloji", "", "C208"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "17:20", "", "Diferansiyel Denklemler", "", "C213"),
            programEntry(ScheduleDay.FRIDAY, "09:30", "12:20", "", "Statik ve Mukavemet", "", "C211")
        )
    )

    val geologyEngineeringThirdYear = engineeringSchedule(
        department = GEOLOGY_ENGINEERING_DEPARTMENT,
        classYear = THIRD_YEAR,
        sourceUrl = GEOLOGY_ENGINEERING_SOURCE_URL,
        sourcePage = 1,
        entries = listOf(
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "", "Tünel Jeolojisi", "", "C210"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "17:20", "", "Jeokimya", "", "C208"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "", "Sedimanter Petrografi", "", "C211"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "17:20", "", "Maden Yatakları", "", "C211"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "", "Madencilik Bilgisi", "", "C211"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "", "Kıyı ve Deniz Jeolojisi", "", "C211"),
            programEntry(ScheduleDay.FRIDAY, "10:30", "12:20", "", "Toplumsal Duyarlılık ve Katkı", "", "D201"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "18:20", "", "Kaya Mekaniği", "", "C211")
        )
    )

    val geologyEngineeringFourthYear = engineeringSchedule(
        department = GEOLOGY_ENGINEERING_DEPARTMENT,
        classYear = FOURTH_YEAR,
        sourceUrl = GEOLOGY_ENGINEERING_SOURCE_URL,
        sourcePage = 1,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "", "Uzaktan Algılama", "", "C208"),
            programEntry(ScheduleDay.MONDAY, "13:30", "17:20", "", "Mühendislik Jeolojisi", "", "C208"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "17:20", "", "Petrol Jeolojisi", "", "B102")
        )
    )

    val mechanicalEngineeringFirstYear = engineeringSchedule(
        department = MECHANICAL_ENGINEERING_DEPARTMENT,
        classYear = FIRST_YEAR,
        sourceUrl = MECHANICAL_ENGINEERING_SOURCE_URL,
        sourcePage = 1,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "08:30", "10:20", "", "İngilizce I", "B. AKINCI", "Amfi 3"),
            programEntry(ScheduleDay.MONDAY, "10:30", "11:20", "", "Kariyer Planlama", "İ. F. YAKA", "D201"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "", "Makine Mühendisliğine Giriş", "G. ALTAN", "C212"),
            programEntry(ScheduleDay.MONDAY, "15:30", "17:20", "", "Almanca I", "A. AYDEMİR ÜMİT", "BB04"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "12:20", "", "Matematik I", "Ç. SEKİN", "Amfi 4"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "", "Genel Kimya", "C. ASLAN", "BB04"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "12:20", "", "Temel Bilgisayar ve Algoritma - Şube 1", "O. ORAL", "Enformatik Lab. 3 (İletişim Fakültesi)"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "17:20", "", "Temel Bilgisayar ve Algoritma - Şube 2", "O. ORAL", "Enformatik Lab. 3 (İletişim Fakültesi)"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "", "Teknik Resim - Şube 1", "V. KOVAN", "Teknik Resim Salonu"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:20", "", "Türk Dili I", "S. ORUÇOĞLU", "Amfi 2"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "", "Teknik Resim - Şube 2", "T. TEZEL", "Teknik Resim Salonu"),
            programEntry(ScheduleDay.FRIDAY, "09:30", "12:20", "", "Fizik I", "N. TUNÇEL", "Amfi 3"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "15:20", "", "Hobi Dersleri", "", "")
        )
    )

    val mechanicalEngineeringSecondYear = engineeringSchedule(
        department = MECHANICAL_ENGINEERING_DEPARTMENT,
        classYear = SECOND_YEAR,
        sourceUrl = MECHANICAL_ENGINEERING_SOURCE_URL,
        sourcePage = 1,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "", "Atatürk İlkeleri ve İnkılap Tarihi I", "M. MALHUT", "C203"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "", "Malzeme Bilimi", "H. E. ÇAMURLU", "D201"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "", "Mukavemet I", "G. ALTAN", "D201"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "", "Ölçme Tekniği ve Değerlendirme", "A. ÇAĞLAR", "Amfi 2"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "", "Termodinamik I", "A. DOĞAN", "Amfi 4"),
            programEntry(ScheduleDay.THURSDAY, "08:30", "12:20", "", "Diferansiyel Denklemler", "E. ŞÜKRÜOĞLU", "Amfi 3"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "", "Sayısal Analiz", "O. ÖZBALCI", "Amfi 3")
        )
    )

    val mechanicalEngineeringThirdYear = engineeringSchedule(
        department = MECHANICAL_ENGINEERING_DEPARTMENT,
        classYear = THIRD_YEAR,
        sourceUrl = MECHANICAL_ENGINEERING_SOURCE_URL,
        sourcePage = 1,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "", "Teknik İngilizce I", "H. E. ÇAMURLU", "D204"),
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "", "İş Hukuku", "B. F. İŞÇİ", "C212"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "", "Makine Elemanları I", "V. KOVAN", "Amfi 4"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "", "Otomatik Kontrol", "H. ERSOY", "D201"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "17:20", "", "Akışkanlar Mekaniği", "İ. ATMACA", "Amfi 4"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "", "İmal Yöntemleri I", "T. TEZEL", "D201"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "", "Mekanizma Tekniği", "D. E. ŞAHİN", "D201"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "10:20", "", "Girişimcilik ve İş Kurma", "M. YAYKAŞLI", "D204"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "10:20", "", "İletişim", "F. KAYAN", "D201"),
            programEntry(ScheduleDay.FRIDAY, "10:30", "12:20", "", "Toplumsal Duyarlılık ve Katkı", "F. KAYAN", "D201")
        )
    )

    val mechanicalEngineeringFourthYear = engineeringSchedule(
        department = MECHANICAL_ENGINEERING_DEPARTMENT,
        classYear = FOURTH_YEAR,
        sourceUrl = MECHANICAL_ENGINEERING_SOURCE_URL,
        sourcePage = 1,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "08:30", "10:20", "", "Makine Proje", "", ""),
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "", "Bilgisayar Destekli Modelleme I", "V. KOVAN", "Yazılım Lab. 2"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "", "Isı Değiştiricileri", "İ. ATMACA", "D204"),
            programEntry(ScheduleDay.MONDAY, "15:30", "17:20", "", "Kompozit Malzeme Mekaniği", "G. ALTAN", "C212"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "10:20", "", "Enerji ve Çevre", "A. GÜNGÖR", "D204"),
            programEntry(ScheduleDay.TUESDAY, "10:30", "12:20", "", "Takım Tezgâhları", "E. S. TOPAL", "BB04"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "", "Kalite Güvenliği", "A. GÜNGÖR", "D204"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "", "Toz Metalurjisine Giriş", "H. E. ÇAMURLU", "C212"),
            programEntry(ScheduleDay.TUESDAY, "15:30", "17:20", "", "Dış Akışlar", "B. DEDA ALTAN", "D204"),
            programEntry(ScheduleDay.TUESDAY, "17:30", "19:20", "", "Eklemeli İmalat", "R. E. ECE", "Uzaktan (TUSAŞ)"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "10:20", "", "Hidrolik Pnömatik", "A. ÇAĞLAR", "D201"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "", "Buhar Kazanları", "A. ÇAĞLAR", "D201"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "", "Makine Konstrüksiyonu", "V. KOVAN", "D204"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "", "Enerji Santralleri", "İ. F. YAKA", "D204"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "17:20", "", "Sonlu Elemanlar Yöntemine Giriş", "H. ERSOY", "Yazılım Lab. 2"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "17:20", "", "Yenilenebilir Enerji Kaynakları", "O. ÖZBALCI", "C212"),
            programEntry(ScheduleDay.THURSDAY, "08:30", "10:20", "", "Kaynak Tekniği", "E. BAL", "C212"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:20", "", "Yakıtlar ve Yanma", "A. DOĞAN", "D204"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:20", "", "Kalıp Tasarımı", "E. S. TOPAL", "C212"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "", "Soğutma Tekniği", "A. ÇOŞGUN", "D204"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "", "Esnek İmalat Sistemleri", "E. S. TOPAL", "C212"),
            programEntry(ScheduleDay.THURSDAY, "15:30", "17:20", "", "İklimlendirme Esasları", "A. ÇOŞGUN", "D204"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "12:20", "", "Makine Laboratuvarı", "", "Amfi 1"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "16:20", "", "Robot Tekniğine Giriş", "E. YILDIZ", "C212"),
            programEntry(ScheduleDay.FRIDAY, "16:30", "18:20", "", "Bitirme Çalışması", "", "")
        )
    )

    val architectureFirstYear = architectureSchedule(
        department = ARCHITECTURE_DEPARTMENT,
        classYear = FIRST_YEAR,
        sourceUrl = ARCHITECTURE_SOURCE_URL,
        sourcePage = 1,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "MİM 123", "Mimarlıkta Bilgisayar Uygulamaları I", "Öğr. Gör. Sadık Gökhan EKİNCİ", "Enformatik LAB 03"),
            programEntry(ScheduleDay.MONDAY, "13:30", "14:20", "MİM 141", "Matematik", "Görevlendirme (Öğr. Gör. Ahmet TEMİZEL)", "Mim Stü 1"),
            programEntry(ScheduleDay.MONDAY, "15:30", "17:20", "MİM 131", "Mimarlığa Giriş", "Öğr. Gör. Dr. Sadık Gökhan EKİNCİ", "Mim Stü 1"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "12:20", "MİM 111", "Mimari Tasarım I ve İfade Teknikleri", "Ş1: Dr. Öğr. Üyesi Evren ÜLKERYILDIZ KALE · Ş2: Arş. Gör. Dr. Şerife İNCEDEMİR · Ş3: Arş. Gör. Dr. Sezen Başak ÖZÜNUR ŞAHİN", "Mim Stü 1 / Mim Stü 3"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "17:20", "MİM 111", "Mimari Tasarım I ve İfade Teknikleri", "Ş1: Dr. Öğr. Üyesi Evren ÜLKERYILDIZ KALE · Ş2: Arş. Gör. Dr. Şerife İNCEDEMİR · Ş3: Arş. Gör. Dr. Sezen Başak ÖZÜNUR ŞAHİN · Ş4: Öğr. Gör. Dr. Sema BALÇIK", "Mim Stü 1 / Mim Stü 3"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "12:20", "MİM 113", "Temel Tasarım", "Ş1: Dr. Öğr. Üyesi Evren ÜLKERYILDIZ KALE · Ş2: Arş. Gör. Dr. Şerife İNCEDEMİR · Ş3: Arş. Gör. Dr. Sezen Başak ÖZÜNUR ŞAHİN · Ş4: Öğr. Gör. Dr. Sema BALÇIK", "Mim Stü 1"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "17:20", "MİM 113", "Temel Tasarım", "Ş1: Dr. Öğr. Üyesi Evren ÜLKERYILDIZ KALE · Ş2: Arş. Gör. Dr. Şerife İNCEDEMİR · Ş3: Arş. Gör. Dr. Sezen Başak ÖZÜNUR ŞAHİN", "Mim Stü 1"),
            programEntry(ScheduleDay.THURSDAY, "08:30", "10:20", "ATA 101", "Atatürk İlkeleri ve İnkılap Tarihi I", "Öğr. Gör. Murat BOZ", "PMB Stüdyo II"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:20", "YBD 101", "İngilizce I", "Öğr. Gör. Ahmet KÜTÜK", "PMB Stüdyo II"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "TDB 101", "Türk Dili I", "Öğr. Gör. Sinan ORUÇOĞLU", "PMB Stüdyo II")
        )
    )

    val architectureSecondYear = architectureSchedule(
        department = ARCHITECTURE_DEPARTMENT,
        classYear = SECOND_YEAR,
        sourceUrl = ARCHITECTURE_SOURCE_URL,
        sourcePage = 1,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "MİM 243", "Mimarlık ve Strüktürel Sistemler I", "Dr. Öğr. Üy. Arzu ER", "Mim Stü 2"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "MİM 280", "İş Sağlığı ve Güvenliği II", "Dr. Öğr. Üy. Arzu ER", "Mim Stü 2"),
            programEntry(ScheduleDay.MONDAY, "15:30", "17:20", "MİM 241", "Mimarlıkta Yapı ve Yapım Yöntemleri I", "Dr. Öğr. Üy. Arzu ER", "Mim Stü 2"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "12:20", "MİM 211", "Mimari Tasarım III", "Ş1: Prof. Dr. H. Tuğba ÖRMECİOĞLU · Ş2: Prof. Dr. İkbal ERBAŞ · Ş3: Arş. Gör. Dr. Ayşe YILDIRIM ATEŞ", "Ş1, Ş2: Mim Stü 2 · Ş3: Mim Stü 3"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "17:20", "MİM 211", "Mimari Tasarım III", "Ş1: Prof. Dr. H. Tuğba ÖRMECİOĞLU · Ş2: Prof. Dr. İkbal ERBAŞ · Ş3: Arş. Gör. Dr. Ayşe YILDIRIM ATEŞ", "Ş1, Ş2: Mim Stü 2 · Ş3: Mim Stü 3"),
            programEntry(ScheduleDay.THURSDAY, "08:30", "10:20", "MİM 213", "Mimari Alan Araştırma Stajı", "Doç. Dr. Serkan KILIÇ", "Mim Stü 2"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:20", "MİM 047", "Mimarlıkta Ekolojik Tasarım", "Prof. Dr. Hacer MUTLU DANACI", "Mim Stü 2"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:20", "MİM 501", "Mimari Tasarımda Yer ve Bağlam", "Arş. Gör. Dr. Sezen Başak ÖZÜNUR ŞAHİN", "Mim Stü 4", "Seçmeli"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "MİM 503", "Çağdaş Mimarlıkta Ekoloji", "Prof. Dr. Hacer MUTLU DANACI", "Mim Stü 2"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "MİM 014", "Serbest El Mimari İfade Teknikleri", "Prof. Dr. Kemal Reha KAVAS", "Mim Stü 3"),
            programEntry(ScheduleDay.THURSDAY, "15:30", "17:20", "MİM 502", "Konutun Kültürel ve Mekansal Analizi", "Arş. Gör. Dr. Şerife İNCEDEMİR", "Mim Stü 3"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "10:20", "MİM 233", "Mimarlık Tarihi I", "Doç. Dr. Serkan KILIÇ", "Mim Stü 2"),
            programEntry(ScheduleDay.FRIDAY, "10:30", "12:20", "MİM 245", "Yapı Malzemeleri", "Prof. Dr. İlknur AKINER", "Mim Stü 2"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "16:20", "MİM 028", "Malzeme ve Taşıyıcı Sistemler", "Prof. Dr. İlknur AKINER", "Mim Stü 2", "Seçmeli"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "16:20", "MİM 091", "Çocuk Mekanları Tasarımı", "Arş. Gör. Dr. Ayşe YILDIRIM ATEŞ", "Mim Stü 1", "Seçmeli"),
            programEntry(ScheduleDay.FRIDAY, "16:30", "18:20", "MİM 004", "Mimari Tasarımda Ergonomi", "Arş. Gör. Dr. Ayşe YILDIRIM ATEŞ", "Mim Stü 2", "Seçmeli")
        )
    )

    val architectureThirdYear = architectureSchedule(
        department = ARCHITECTURE_DEPARTMENT,
        classYear = THIRD_YEAR,
        sourceUrl = ARCHITECTURE_SOURCE_URL,
        sourcePage = 1,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "08:30", "10:20", "MİM 508", "Yapı İşlerinde İş Sağlığı ve Güvenliği I", "Dr. Öğr. Üy. Arzu ER", "Mim Stü 2", "Seçmeli"),
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "MİM 507", "Yapay Zeka Destekli Dijital Stüdyo", "Prof. Dr. H. Tuğba ÖRMECİOĞLU", "Mim Stü 1", "Seçmeli"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "MİM 082", "Asma-Germe Strüktürler", "Prof. Dr. H. Tuğba ÖRMECİOĞLU", "Mim Stü 3", "Seçmeli"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "MİM 032", "Şantiye Yönetim ve Organizasyonu", "Prof. Dr. İkbal ERBAŞ", "AZ-16", "Seçmeli"),
            programEntry(ScheduleDay.MONDAY, "15:30", "17:20", "MİM 313", "Kentsel Planlama", "Dr. Öğr. Üy. Çağdaş SAYDAM", "Mim Stü 3"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "12:20", "MİM 311", "Mimari Tasarım V", "Ş1: Prof. Dr. İlknur AKINER · Ş2: Doç. Dr. Mehmet İNCEOĞLU · Ş3: Dr. Öğr. Üy. Çağdaş SAYDAM · Ş4: Dr. Öğr. Üy. İbrahim BAKIR · Ş5: Öğr. Gör. Dr. Sadık Gökhan EKİNCİ", "Ş1, Ş2, Ş5: Mim Stü 4 · Ş3, Ş4: AZ-16"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "17:20", "MİM 311", "Mimari Tasarım V", "Ş1: Prof. Dr. İlknur AKINER · Ş2: Doç. Dr. Mehmet İNCEOĞLU · Ş3: Dr. Öğr. Üy. Çağdaş SAYDAM · Ş4: Dr. Öğr. Üy. İbrahim BAKIR · Ş5: Öğr. Gör. Dr. Sadık Gökhan EKİNCİ", "Ş1, Ş2, Ş5: Mim Stü 4 · Ş3, Ş4: AZ-16"),
            programEntry(ScheduleDay.THURSDAY, "08:30", "10:20", "MİM 343", "Mekanik Sistemler", "Görevlendirme - Öğr. Gör. Mehmet KEMER", "Mim Stü 3"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:20", "MİM 333", "Mimarlık Tarihi III", "Prof. Dr. Kemal Reha KAVAS", "Mim Stü 1"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "MİM 341", "Mimari Uygulama Projesi I (Şube 1-3)", "Ş1: Dr. Öğr. Üy. Arzu ER · Ş2: Dr. Öğr. Üy. İbrahim BAKIR · Ş3: Öğr. Gör. Dr. Sema BALCIK", "Ş1: Mim Stü 1 · Ş2-3: AZ-16"),
            programEntry(ScheduleDay.THURSDAY, "16:30", "19:15", "MİM 341", "Mimari Uygulama Projesi I (Şube 4-6)", "Ş4: Dr. Öğr. Üy. Arzu ER · Ş5: Dr. Öğr. Üy. İbrahim BAKIR · Ş6: Öğr. Gör. Dr. Sema BALCIK", "Ş4: Mim Stü 1 · Ş5-6: AZ-16"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "10:20", "MİM 315", "Toplumsal Duyarlılık ve Katkı", "Arş. Gör. Dr. Ayşe YILDIRIM ATEŞ", "Mim Stü 3"),
            programEntry(ScheduleDay.FRIDAY, "10:30", "12:20", "MİM 363", "Mimari Koruma ve Restorasyon", "Doç. Dr. Serkan KILIÇ", "Mim Stü 3")
        )
    )

    val architectureFourthYear = architectureSchedule(
        department = ARCHITECTURE_DEPARTMENT,
        classYear = FOURTH_YEAR,
        sourceUrl = ARCHITECTURE_SOURCE_URL,
        sourcePage = 1,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "08:30", "10:20", "MİM 048", "Anadolu Selçuklu Mimarisi I", "Doç. Dr. Serkan KILIÇ", "Mim Stü 3", "Seçmeli"),
            programEntry(ScheduleDay.MONDAY, "08:30", "10:20", "MİM 036", "Yapılarda Yangın Korunumu", "Görevlendirme (Öğr. Gör. Dr. A. Nihat AKÇAL)", "Mim Stü 4", "Seçmeli"),
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "MİM 067", "Mimarlık ve Sanat I", "Öğr. Gör. Dr. Sadık Gökhan EKİNCİ", "Mim Stü 4", "Seçmeli"),
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "MİM 060", "Likya Kentleri ve Mimarisi", "Doç. Dr. Serkan KILIÇ", "Mim Stü 4", "Seçmeli"),
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "MİM 063", "Mimari Portfolyo Tasarımı I", "Dr. Öğr. Üy. Sıdıka Benan ÇELİKEL", "Mim Stü 3", "Seçmeli"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "MİM 451", "İmar Mevzuatı", "Öğr. Gör. Sadık Gökhan EKİNCİ", "Mim Stü 4"),
            programEntry(ScheduleDay.MONDAY, "15:30", "17:20", "MİM 088", "İşletme Ekonomisi ve Mimarlık", "Görevlendirme (Doç. Dr. Koray ÇETİN)", "Mim Stü 4", "Seçmeli"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "12:20", "MİM 411", "Mimari Tasarım VII", "Ş1: Prof. Dr. Kemal Reha KAVAS · Ş2: Prof. Dr. Hacer MUTLU DANACI · Ş3: Doç. Dr. Mehmet İNCEOĞLU · Ş4: Dr. Öğr. Üy. İbrahim BAKIR · Ş5: Dr. Öğr. Üy. Çağdaş SAYDAM · Ş6: Öğr. Gör. Dr. Sadık Gökhan EKİNCİ", "Ş1: AZ-16 · Ş2, Ş6: AZ-12 · Ş3: Mim Stü 2 · Ş4, Ş5: Mim Stü 4"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "17:20", "MİM 411", "Mimari Tasarım VII", "Ş1: Prof. Dr. Kemal Reha KAVAS · Ş2: Prof. Dr. Hacer MUTLU DANACI · Ş3: Doç. Dr. Mehmet İNCEOĞLU · Ş4: Dr. Öğr. Üy. İbrahim BAKIR · Ş5: Dr. Öğr. Üy. Çağdaş SAYDAM · Ş6: Öğr. Gör. Dr. Sadık Gökhan EKİNCİ", "Ş1: AZ-16 · Ş2, Ş6: AZ-12 · Ş3: Mim Stü 2 · Ş4, Ş5: Mim Stü 4"),
            programEntry(ScheduleDay.THURSDAY, "08:30", "10:20", "MİM 414", "Mimari Uygulama (Ofis) Stajı-II", "Öğr. Gör. Dr. Sema BALCIK", "Mim Stü 4"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:20", "MİM 046", "Kent Sosyolojisi", "Dr. Öğr. Üy. Evren ÜLKERYILDIZ KALE", "Mim Stü 3", "Seçmeli"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "MİM 043", "Anıtsal Mimarlıkta Restorasyon", "Doç. Dr. Serkan KILIÇ", "Mim Stü 4", "Seçmeli"),
            programEntry(ScheduleDay.THURSDAY, "15:30", "17:20", "MİM 076", "Mimarlık Tarihinde Fotoğraf", "Doç. Dr. Serkan KILIÇ", "Mim Stü 4", "Seçmeli"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "10:20", "MİM 414", "Mimari Uygulama (Ofis) Stajı-II", "Öğr. Gör. Dr. Sema BALCIK", "Mim Stü 4")
        )
    )

    val interiorArchitectureFirstYear = architectureSchedule(
        department = INTERIOR_ARCHITECTURE_DEPARTMENT,
        classYear = FIRST_YEAR,
        sourceUrl = INTERIOR_ARCHITECTURE_SOURCE_URL,
        sourcePage = 1,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "08:30", "12:20", "İÇT 157", "Desen", "Öğr. Gör. Sabriye ÖZTÜTÜNCÜ", "İÇM1"),
            programEntry(ScheduleDay.MONDAY, "13:30", "17:20", "İÇT 143", "Bilgisayar Destekli Çizim", "Öğr. Gör. H. ELİNÇ", "Enformatik Lab."),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "12:20", "İÇT 141", "Teknik Resim I - Şube 1", "Öğr. Gör. Aydın UÇAR", "İÇM1"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "12:20", "İÇT 141", "Teknik Resim I - Şube 2", "Arş. Gör. M. AÇIKEL", "İÇM2"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "İÇT 153", "Mimarlık Tarihi", "Doç. Dr. Ayşegül DURUKAN ARSLAN", "İÇM2"),
            programEntry(ScheduleDay.THURSDAY, "08:30", "10:20", "ATA 101", "Atatürk İlkeleri ve İnkılap Tarihi I", "", "Sanal sınıf"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:20", "YBD 101", "İngilizce I", "Öğr. Gör. G. ASLI", "Sanal sınıf"),
            programEntry(ScheduleDay.THURSDAY, "15:30", "17:20", "TDB 1001", "Türk Dili I", "S. ORUÇOĞLU", "SBP Stüdyo 4"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "14:20", "İÇT 111", "Temel Tasarım I - Şube 2", "Öğr. Gör. A. Hikmet BAŞAYTAÇ", "İÇM2"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "14:20", "İÇT 111", "Temel Tasarım I - Şube 1", "Dr. Öğr. Ü. Sıdıka Benan ÇELİKEL", "İÇM3")
        )
    )

    val interiorArchitectureSecondYear = architectureSchedule(
        department = INTERIOR_ARCHITECTURE_DEPARTMENT,
        classYear = SECOND_YEAR,
        sourceUrl = INTERIOR_ARCHITECTURE_SOURCE_URL,
        sourcePage = 1,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "İÇT 273", "Bilgisayar Destekli Tasarım", "Öğr. Gör. H. ELİNÇ", "Enformatik Lab."),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "İÇT 281", "Serbest El Çizim", "Öğr. Gör. Sabriye ÖZTÜTÜNCÜ", "İÇM1"),
            programEntry(ScheduleDay.WEDNESDAY, "11:30", "13:20", "İÇT 223", "Ölçek ve İnsan", "Dr. Öğr. Üye. Esra ORHAN YILMAZ", "AZ12"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "17:20", "İÇT 253", "Taşıyıcı Sistemler", "Arş. Gör. M. AÇIKEL", "İÇM1"),
            programEntry(ScheduleDay.THURSDAY, "08:30", "14:20", "İÇT 211", "Tasarım Stüdyosu I - Şube 1", "Prof. Dr. Zuhal KAYNAKCI ELİNÇ", "İÇM2"),
            programEntry(ScheduleDay.THURSDAY, "08:30", "14:20", "İÇT 211", "Tasarım Stüdyosu I - Şube 2", "Dr. Öğr. Üye. Esra ORHAN YILMAZ", "İÇM4"),
            programEntry(ScheduleDay.THURSDAY, "08:30", "14:20", "İÇT 211", "Tasarım Stüdyosu I - Şube 3", "Öğr. Gör. A. Hikmet BAŞAYTAÇ", "İÇM3"),
            programEntry(ScheduleDay.THURSDAY, "14:30", "17:20", "İÇT 225", "İç Mimarlık Kavramları", "Doç. Dr. Özgü ÖZTURAN", "İÇM2"),
            programEntry(ScheduleDay.FRIDAY, "09:30", "12:20", "İÇT 241", "Malzeme Bilgisi", "Dr. Öğr. Üye. Esra ORHAN YILMAZ", "İÇM1")
        )
    )

    val interiorArchitectureThirdYear = architectureSchedule(
        department = INTERIOR_ARCHITECTURE_DEPARTMENT,
        classYear = THIRD_YEAR,
        sourceUrl = INTERIOR_ARCHITECTURE_SOURCE_URL,
        sourcePage = 1,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "İÇT 353", "Tesisat Bilgisi", "Öğr. Gör. Mehmet KEMER", "İÇM2"),
            programEntry(ScheduleDay.MONDAY, "15:30", "17:20", "İÇT 333", "Mesleki Uygulama I", "Öğr. Gör. Aydın UÇAR", "İÇM2"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "12:20", "İÇT 341", "İnce Yapı", "Öğr. Gör. Arif Nihat KARAKUŞ", "İÇM1"),
            programEntry(ScheduleDay.TUESDAY, "14:30", "17:20", "İÇT 369", "Tasarımda İnovasyon ve Girişimcilik - Şube 1", "Doç. Dr. Ayşegül DURUKAN ARSLAN", "İÇM2"),
            programEntry(ScheduleDay.TUESDAY, "14:30", "17:20", "İÇT 369", "Tasarımda İnovasyon ve Girişimcilik - Şube 2", "Öğr. Gör. Aydın UÇAR", "İÇM3"),
            programEntry(ScheduleDay.TUESDAY, "14:30", "17:20", "İÇT 367", "İç Mekanda Renk ve Işık", "Dr. Öğr. Üye. Esra ORHAN YILMAZ", "İÇM2"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "14:20", "İÇT 312", "Tasarım Stüdyosu III - Şube 1", "Öğr. Gör. Alperen AK", "AZ14"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "14:20", "İÇT 312", "Tasarım Stüdyosu III - Şube 2", "Prof. Dr. Şebnem ERTAŞ BEŞİR", "İÇM3"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "14:20", "İÇT 312", "Tasarım Stüdyosu III - Şube 3", "Doç. Dr. Özgü ÖZTURAN", "İÇM4"),
            programEntry(ScheduleDay.WEDNESDAY, "14:30", "17:20", "İÇT 373", "İç Mekanda Detay Tasarımı", "Öğr. Gör. A. Hikmet BAŞAYTAÇ", "İÇM3"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "18:30", "İÇT 369", "Tasarımda İnovasyon ve Girişimcilik - Şube 3", "Doç. Dr. Ayşegül DURUKAN ARSLAN", "İÇM2"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "18:30", "İÇT 369", "Tasarımda İnovasyon ve Girişimcilik - Şube 4", "Öğr. Gör. Aydın UÇAR", "İÇM4"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "İÇT 321", "Çevre Tasarımı I", "Öğr. Gör. Arif Nihat KARAKUŞ", "AZ16"),
            programEntry(ScheduleDay.THURSDAY, "14:30", "18:30", "İÇT 343", "Mobilya Tasarımı II - Şube 2", "Öğr. Gör. A. Hikmet BAŞAYTAÇ", "İÇM3"),
            programEntry(ScheduleDay.THURSDAY, "14:30", "18:30", "İÇT 343", "Mobilya Tasarımı II - Şube 3", "Öğr. Gör. Alperen AK", "İÇM4"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "09:20", "İÇT 315", "Toplumsal Duyarlılık ve Katkı", "Arş. Gör. Ayşe YILDIRIM ATEŞ", "İÇM3"),
            programEntry(ScheduleDay.FRIDAY, "10:30", "12:20", "İÇT 363", "İç Mekanda Koruma ve Restorasyon", "Doç. Dr. Serkan KILIÇ", "İÇM3")
        )
    )

    val interiorArchitectureFourthYear = architectureSchedule(
        department = INTERIOR_ARCHITECTURE_DEPARTMENT,
        classYear = FOURTH_YEAR,
        sourceUrl = INTERIOR_ARCHITECTURE_SOURCE_URL,
        sourcePage = 1,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "İÇT 423", "Mekan Analizi", "Öğr. Gör. Aydın UÇAR", "İÇM2"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "İÇT 413", "Deneysel Tasarım Stüdyosu - Şube 1", "Prof. Dr. Zuhal KAYNAKCI ELİNÇ", "İÇM3"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "İÇT 413", "Deneysel Tasarım Stüdyosu - Şube 2", "Arş. Gör. M. AÇIKEL", "İÇM4"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "14:20", "İÇT 411", "Tasarım Stüdyosu V - Şube 1", "Doç. Dr. Özgü ÖZTURAN", "İÇM4"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "14:20", "İÇT 411", "Tasarım Stüdyosu V - Şube 2", "Öğr. Gör. Aydın UÇAR", "İÇM2"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "14:20", "İÇT 411", "Tasarım Stüdyosu V - Şube 3", "Arş. Gör. M. AÇIKEL", "İÇM3"),
            programEntry(ScheduleDay.TUESDAY, "14:30", "17:20", "İÇT 417", "İç Mekanda Koruma ve Yeniden Kullanım - Şube 1", "Prof. Dr. Şebnem ERTAŞ BEŞİR", "İÇM1"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "11:20", "İÇT 495", "İç Kent Mobilyaları Tasarımı", "Öğr. Gör. A. Hikmet BAŞAYTAÇ", "AZ12"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "13:20", "İÇT 439", "Biçim Tasarım İlişkisi", "Dr. Öğr. Ü. Enver GÜNER", "AZ15"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "İÇT 417", "İç Mekanda Koruma ve Yeniden Kullanım - Şube 2", "Prof. Dr. Şebnem ERTAŞ BEŞİR", "İÇM1"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "İÇT 437", "Binalarda Bitkilendirme", "Öğr. Gör. Nihat KARAKUŞ", "AZ12"),
            programEntry(ScheduleDay.THURSDAY, "14:30", "17:20", "İÇT 433", "Kurum Kimliği ve Kurumsal Tasarım I", "Dr. Öğr. Üye. Esra ORHAN YILMAZ", "İÇM1")
        )
    )

    val econometricsFirstYear = ClassSchedule(
        faculty = BUSINESS_FACULTY,
        department = ECONOMETRICS_DEPARTMENT,
        classYear = FIRST_YEAR,
        academicYear = "2026-2027",
        term = "Güz Yarıyılı",
        updatedAt = "14.09.2026",
        sourcePage = 1,
        sourceUrl = ECONOMETRICS_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "EKN 101", "Matematik I", "Arş. Gör. Dr. Buse Eda AKYÜZ ULUCAN", "B Blok Amfi 4"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "EKN 103", "Genel Muhasebe I", "Prof. Dr. Burcu DEMİREL", "A Blok 102"),
            programEntry(ScheduleDay.TUESDAY, "10:30", "12:20", "TDB 101", "Türk Dili I", "Öğr. Gör. Arzu TIRAK ASLAN", "B Blok Mor Amfi"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "EKN 109", "İşletme Yönetimine Giriş", "Doç. Dr. Janset AYTEMUR", "A Blok 204"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "10:20", "YBD 101", "İngilizce I", "Öğr. Gör. Aslı TAŞER", "A Blok Mavi Amfi"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "ATA 101", "Atatürk İlkeleri ve İnkılap Tarihi I", "Öğr. Gör. Dr. Koray ERGİN", "B Blok Mor Amfi"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "TDB 115", "Akademik Türkçe", "Öğr. Gör. Dürüye KARA", "Uzaktan (Online)"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "EKN 105", "İktisada Giriş I", "Prof. Dr. Ahmet BAYANER", "A Blok 202"),
            programEntry(ScheduleDay.THURSDAY, "12:30", "13:20", "KPD 101", "Kariyer Planlama Dersi", "Dr. Öğr. Üyesi M. Serhan SEKRETER", "B Blok Amfi 2"),
            programEntry(ScheduleDay.THURSDAY, "15:30", "18:20", "EKN 107", "Hukuka Giriş", "Öğr. Üyesi Aycan DEMİR", "Hukuk Fak. Derslik I")
        )
    )

    val econometricsSecondYear = ClassSchedule(
        faculty = BUSINESS_FACULTY,
        department = ECONOMETRICS_DEPARTMENT,
        classYear = SECOND_YEAR,
        academicYear = "2026-2027",
        term = "Güz Yarıyılı",
        updatedAt = "14.09.2026",
        sourcePage = 2,
        sourceUrl = ECONOMETRICS_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "EKN 205", "Mikro İktisat", "Prof. Dr. Mehmet ZANBAK", "A Blok 205"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "EKN 201", "İstatistiksel Analiz I", "Prof. Dr. Mehmet MERT", "B Blok Amfi 6"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "EKN 207", "Kamu Maliyesi", "Arş. Gör. Dr. İlyas ÖZKÖK", "A Blok 201"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "18:20", "EKN 211", "İngilizce Okuma ve Konuşma", "Öğr. Gör. Hülya ÇELİK", "A Blok Mavi Amfi"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "EKN 219", "Bilgisayar Programlama I", "Öğr. Gör. Dr. Evren SEZGİN", "Enformatik LAB 07"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "EKN 203", "İleri Matematik", "Prof. Dr. Pınar KAYA SAMUT", "B Blok Amfi 1"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "17:20", "EKN 209", "Borçlar Hukuku", "Öğr. Gör. Fahri DUTÇU", "B Blok Amfi 4")
        )
    )

    val econometricsThirdYear = ClassSchedule(
        faculty = BUSINESS_FACULTY,
        department = ECONOMETRICS_DEPARTMENT,
        classYear = THIRD_YEAR,
        academicYear = "2026-2027",
        term = "Güz Yarıyılı",
        updatedAt = "14.09.2026",
        sourcePage = 3,
        sourceUrl = ECONOMETRICS_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "EKN 331", "Yapay Zeka Algoritmaları", "Doç. Dr. Mehmet KAYAKUŞ", "B Blok Amfi 2"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "EKN 323", "Regresyon Analizi", "Arş. Gör. Dr. Buse Eda AKYÜZ ULUCAN", "A Blok 204"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "EKN 310", "Finansal Analiz", "Prof. Dr. Aslıhan BOZCUK", "A Blok 205"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "16:20", "EKN 301", "Ekonometri I", "Doç. Dr. Çiğdem DEMİR TOKER", "B Blok Amfi 6"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "EKN 321", "Matematiksel İstatistik", "Dr. Öğr. Üyesi Ayça BÜYÜKYILMAZ ERCAN", "B Blok Amfi 4"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "EKN 303", "Yöneylem Araştırması I", "Prof. Dr. Emre İPEKÇİ ÇETİN", "B Blok Amfi 4"),
            programEntry(ScheduleDay.FRIDAY, "09:30", "12:20", "EKN 307", "Türkiye Ekonomisi", "Öğr. Gör. Nermin BİLEK", "B Blok Amfi 3"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "17:20", "EKN 325", "Mesleki İngilizce I", "Öğr. Gör. Recep KAZANCI", "B Blok Amfi 3")
        )
    )

    val econometricsFourthYear = ClassSchedule(
        faculty = BUSINESS_FACULTY,
        department = ECONOMETRICS_DEPARTMENT,
        classYear = FOURTH_YEAR,
        academicYear = "2026-2027",
        term = "Güz Yarıyılı",
        updatedAt = "14.09.2026",
        sourcePage = 4,
        sourceUrl = ECONOMETRICS_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "EKN 407", "Yönetim Bilgi Sistemleri", "Doç. Dr. Mehmet KAYAKUŞ", "B Blok Amfi 6"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "EKN 403", "Zaman Serileri Analizi", "Prof. Dr. Mehmet MERT", "B Blok Amfi 3"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "EKN 405", "Çok Değişkenli İstatistiksel Analiz", "Dr. Öğr. Üyesi Ayça BÜYÜKYILMAZ ERCAN", "B Blok Amfi 1"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "EKN 421", "Çok Kriterli Karar Verme", "Prof. Dr. Emre İPEKÇİ ÇETİN", "B Blok Amfi 5"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "16:20", "EKN 429", "İstatistiksel Kalite Kontrol", "Dr. Öğr. Üyesi M. Serhan SEKRETER", "B Blok Amfi 5"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "EKN 409", "Karar Verme ve Oyun Teorisi", "Prof. Dr. Pınar KAYA SAMUT", "B Blok Amfi 3"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "EKN 401", "Uygulamalı Ekonometri", "Doç. Dr. Çiğdem DEMİR TOKER", "B Blok Amfi 3")
        )
    )

    val maliyeFirstYear = ClassSchedule(
        faculty = BUSINESS_FACULTY,
        department = PUBLIC_FINANCE_DEPARTMENT,
        classYear = FIRST_YEAR,
        academicYear = "2026-2027",
        term = "Güz Yarıyılı",
        updatedAt = "05.09.2026",
        sourcePage = 1,
        sourceUrl = PUBLIC_FINANCE_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "MLY 115", "Genel Muhasebe I", "Prof. Dr. Mustafa YILDIRAN", "A Blok Mavi"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "MLY 103", "Matematik I", "Doç. Dr. Neylan KAYA", "A Blok Mavi"),
            programEntry(ScheduleDay.TUESDAY, "10:30", "12:20", "TDB 101", "Türk Dili I", "Öğr. Gör. Arzu TIRAK ASLAN", "B Blok Mor"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "MLY 101", "İktisada Giriş", "Doç. Dr. Servet AKYOL", "A Blok Mavi"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "10:20", "YBD 101", "İngilizce I", "Öğr. Gör. Aslı TAŞER", "A Blok Mavi"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "ATA 101", "Atatürk İlkeleri ve İnkılap Tarihi", "Öğr. Gör. Dr. Koray ERGİN", "B Blok Mor"),
            programEntry(ScheduleDay.WEDNESDAY, "12:30", "13:20", "KPD 101", "Kariyer Planlama", "Prof. Dr. Burcu DEMİREL", "B Blok Yavuz Tekelioğlu"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "TDB 115", "Akademik Türkçe", "Öğr. Gör. Dürüye KARA", "Uzaktan/Online"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:20", "MLY 107", "İşletme Yönetimine Giriş", "Doç. Dr. Janset AYTEMUR", "A Blok Mavi"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "MLY 105", "Hukuka Giriş", "Doç. Dr. Ahmet Alptekin DURU", "A Blok Mavi"),
            programEntry(ScheduleDay.FRIDAY, "09:30", "12:20", "MLY 119", "Siyaset Bilimine Giriş", "Dr. Öğr. Üyesi Kadriye OKUDAN DERNEK", "A Blok Mavi")
        )
    )

    val maliyeSecondYear = ClassSchedule(
        faculty = BUSINESS_FACULTY,
        department = PUBLIC_FINANCE_DEPARTMENT,
        classYear = SECOND_YEAR,
        academicYear = "2026-2027",
        term = "Güz Yarıyılı",
        updatedAt = "05.09.2026",
        sourcePage = 2,
        sourceUrl = PUBLIC_FINANCE_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "MLY 215", "İdare Hukuku", "Öğr. Gör. Dr. Gülden AGÖKTÜRK", "Hukuk Fak. Derslik-3"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "MLY 229", "Medeni Hukuk", "Öğr. Gör. Aycan DEMİR", "Hukuk Fak. Derslik-3"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "MLY 201", "Mikro İktisat", "Prof. Dr. Zeliha GÖKER", "A Blok 202"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "MLY 209", "Kamu Maliyesi I", "Prof. Dr. Hale BALSEVEN", "A Blok 202"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "MLY 235", "Kamu Yönetimi", "Dr. Öğr. Üyesi Bengi DEMİRCİ", "A Blok 204"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "18:20", "MLY 287", "İngilizce Okuma ve Konuşma", "Öğr. Gör. Hülya ÇELİK", "A Blok Mavi"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "MLY 231", "İktisat Tarihi", "Dr. İlyas ÖZKÖK", "A Blok 201"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "MLY 217", "Şirketler Muhasebesi", "Öğr. Gör. Dr. Mustafa TERZİOĞLU", "A Blok 203")
        )
    )

    val maliyeThirdYear = ClassSchedule(
        faculty = BUSINESS_FACULTY,
        department = PUBLIC_FINANCE_DEPARTMENT,
        classYear = THIRD_YEAR,
        academicYear = "2026-2027",
        term = "Güz Yarıyılı",
        updatedAt = "05.09.2026",
        sourcePage = 3,
        sourceUrl = PUBLIC_FINANCE_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "MLY 337", "Vergi Hukuku", "Doç. Dr. Derya YAYMAN", "A Blok 201"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "MLY 345", "Menkul ve Gayrimenkul Değ. Yöntemleri", "Prof. Dr. Mustafa YILDIRAN", "A Blok 201"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "MLY 329", "Ekonometri", "Prof. Dr. Can Tansel TUĞCU", "A Blok 201"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "MLY 301", "Kamu Ekonomisi I", "Prof. Dr. Zeliha GÖKER", "A Blok 202"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "MLY 319", "Teoride ve Uygulamada Hazine", "Doç. Dr. Birsen NACAR KARABACAK", "A Blok 202"),
            programEntry(ScheduleDay.FRIDAY, "09:30", "12:20", "MLY 333", "İktisadi Düşünceler Tarihi", "Dr. İlyas ÖZKÖK", "A Blok 201"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "17:20", "MLY 339", "Kamu Bütçesi I", "Prof. Dr. Yakup KARABACAK", "A Blok 201")
        )
    )

    val maliyeFourthYear = ClassSchedule(
        faculty = BUSINESS_FACULTY,
        department = PUBLIC_FINANCE_DEPARTMENT,
        classYear = FOURTH_YEAR,
        academicYear = "2026-2027",
        term = "Güz Yarıyılı",
        updatedAt = "05.09.2026",
        sourcePage = 4,
        sourceUrl = PUBLIC_FINANCE_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "MLY 439", "Mali Yargılama Hukuku", "Doç. Dr. Derya YAYMAN", "A Blok 201"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "MLY 401", "Maliye Politikası I", "Doç. Dr. Servet AKYOL", "A Blok 203"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "MLY 433", "Türk Vergi Sistemi II", "Doç. Dr. Derya YAYMAN", "A Blok 203"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "MLY 415", "Bilgisayar Uygulamalı Muhasebe", "Doç. Dr. Burçin TUTCU", "A Blok Lab"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "MLY 431", "Devlet ve Ekonomi", "Prof. Dr. Mustafa YILDIRAN", "A Blok 203"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "16:20", "MLY 445", "Sağlık Ekonomisi", "Doç. Dr. Servet AKYOL", "A Blok 201"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "MLY 451", "Para Teorisi ve Politikası", "Prof. Dr. Can Tansel TUĞCU", "A Blok 201"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "17:20", "MLY 405", "Yerel Yönetimler Maliyesi", "Doç. Dr. Birsen NACAR KARABACAK", "A Blok 203")
        )
    )

    val financeBankingFirstYear = appliedSciencesSchedule(
        classYear = FIRST_YEAR,
        sourcePage = 1,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "", "Muhasebe I", "A. CİĞER", "D4"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "", "Genel İşletme", "Ş. ÖZDEMİR ERSÖZ", "D1"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "", "Genel Matematik", "Y. ERYILMAZ", "İletişim B Blok D8"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "", "İngilizce I", "A. KÜTÜK", "İletişim B Blok D8"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "17:20", "", "Türk Dili I", "N. KILINÇ İNEVİ", "İletişim B Blok D10"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "", "Temel Hukuk", "G. ÖZTÜRK", "D8"),
            programEntry(ScheduleDay.THURSDAY, "12:30", "13:20", "", "Kariyer Planlama", "A. KAYA", "D1"),
            programEntry(ScheduleDay.FRIDAY, "09:30", "12:20", "", "Mikro İktisat", "N. ÖKSÜZ NARİNÇ", "İletişim B Blok D9"),
            programEntry(ScheduleDay.FRIDAY, "15:30", "17:20", "", "Atatürk İlkeleri ve İnkılap Tarihi I", "D. ÖGE SET", "İletişim B Blok D8")
        )
    )

    val financeBankingSecondYear = appliedSciencesSchedule(
        classYear = SECOND_YEAR,
        sourcePage = 2,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "", "Finansal Yönetim I", "N. AVŞARLIGİL", "İletişim B Blok D4"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "", "Sigortacılık", "Ö. ÇITAK", "İletişim B Blok D9"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "12:20", "", "Uygulamalı Girişimcilik", "Ö. ÇITAK", "D4"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "", "Bankacılığa Giriş", "F. YETİZ", "İletişim B Blok D8"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "", "Ticaret Hukuku", "D. TURGUT GÜNEL", "D3"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "16:20", "", "İş Etiği", "S. AYDIN", "D3"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "", "İstatistik I", "N. AVŞARLIGİL", "İletişim B Blok D4"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "", "Maliyet Muhasebesi", "A. KAYA", "D3"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "17:20", "", "Mikro İktisat", "N. ÖKSÜZ NARİNÇ", "İletişim B Blok D4")
        )
    )

    val financeBankingThirdYear = appliedSciencesSchedule(
        classYear = THIRD_YEAR,
        sourcePage = 3,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "", "Para Teorisi ve Politikası", "N. AVŞARLIGİL", "İletişim B Blok D10"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "", "Sözleşmeler Hukuku", "D. TURGUT GÜNEL", "İletişim B Blok D7"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "", "Hizmet Pazarlaması", "H. KÖSEOĞLU", "İletişim B Blok D9"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "", "Türev Piyasalar I", "U. ÜNLÜ", "İletişim B Blok D6"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "16:20", "", "Portföy Yönetimi", "U. ÜNLÜ", "İletişim B Blok D6"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "", "Araştırma Yöntemleri", "N. ÖKSÜZ NARİNÇ", "Enformatik LAB 1"),
            programEntry(ScheduleDay.FRIDAY, "13:30", "16:20", "", "Bank. Hizmet ve Ürünler", "F. YILDIRIM", "İletişim B Blok D5")
        )
    )

    val financeBankingFourthYear = appliedSciencesSchedule(
        classYear = FOURTH_YEAR,
        sourcePage = 4,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "", "Borsa Uygulamaları", "U. ÜNLÜ", "Enformatik LAB"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "", "Bankacılık ve Finansta Güncel Konular", "U. ÜNLÜ", "Enformatik LAB"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "", "Uluslararası Finans", "N. AVŞARLIGİL", "İletişim B Blok D4"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "", "Mali Tablolar Analizi", "A. CİĞER", "D7"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "16:20", "", "Merkez Bankacılığı", "Y. ERYILMAZ", "İletişim B Blok D4"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "", "Uluslararası Muhasebe Standartları", "F. YILDIRIM", "İletişim B Blok D9"),
            programEntry(ScheduleDay.FRIDAY, "09:30", "12:20", "", "Bankacılıkta Risk Yönetimi", "F. YILDIRIM", "İletişim B Blok D5")
        )
    )

    // PAZ.pdf bu yayınında yalnızca 3. sınıf ve 4. sınıf örgün programlarını içeriyor.
    // Profilde sınıf seçimi tüm yıllar için ortak olduğu için 1. ve 2. sınıfı
    // şimdilik boş program olarak gösteriyoruz; ilgili PDF geldiğinde doldurulabilir.
    val marketingFirstYear = appliedSciencesSchedule(
        department = MARKETING_DEPARTMENT,
        classYear = FIRST_YEAR,
        sourcePage = 1,
        sourceUrl = MARKETING_SOURCE_URL,
        entries = emptyList()
    )

    val marketingSecondYear = appliedSciencesSchedule(
        department = MARKETING_DEPARTMENT,
        classYear = SECOND_YEAR,
        sourcePage = 1,
        sourceUrl = MARKETING_SOURCE_URL,
        entries = emptyList()
    )

    val marketingThirdYear = appliedSciencesSchedule(
        department = MARKETING_DEPARTMENT,
        classYear = THIRD_YEAR,
        sourcePage = 1,
        sourceUrl = MARKETING_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "", "Finansal Yönetim", "Ö. ÇITAK", "D1"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "", "İnsan Kaynakları Yönetimi", "C. TUFAN", "D6"),
            programEntry(ScheduleDay.TUESDAY, "10:30", "13:20", "", "Üretim Yönetimi", "R. YETKİN ÖZBÜK", "D7"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "", "Maliyet Muhasebesi", "A. KAYA", "D4"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "13:20", "", "Örgütsel İletişim", "C. TUFAN", "D6"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "16:20", "", "Pazarlama Araştırması I", "N. BÜYÜKDAĞ", "D7"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "11:20", "", "Mesleki İngilizce I", "İ. DÖRTYOL", "D3"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "", "Reklamcılık", "N. BATU", "İletişim B Blok D4"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "17:20", "", "Hizmet Pazarlaması", "A. KANGAL", "İletişim B Blok D10")
        )
    )

    val marketingFourthYear = appliedSciencesSchedule(
        department = MARKETING_DEPARTMENT,
        classYear = FOURTH_YEAR,
        sourcePage = 2,
        sourceUrl = MARKETING_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "", "Uygulamalı Girişimcilik", "M. YAYKAŞLI", "İletişim B Blok D8"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "", "Pazarlamada Güncel Konular", "İ. DÖRTYOL", "İletişim B Blok D8"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "", "Dijital Pazarlama", "N. BATU", "İletişim B Blok D5"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "16:20", "", "Halkla İlişkiler", "N. BATU", "İletişim B Blok D5"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "", "Kurumsal Yönetim ve Sosyal Sorumluluk", "N. BÜYÜKDAĞ", "D7"),
            programEntry(ScheduleDay.FRIDAY, "10:30", "12:20", "", "Müşteri İlişkileri Yönetimi", "A. KANGAL", "D6")
        )
    )

    // SİG.pdf bu yayınında 2, 3 ve 4. sınıf örgün programlarını içeriyor.
    val insuranceFirstYear = appliedSciencesSchedule(
        department = INSURANCE_DEPARTMENT,
        classYear = FIRST_YEAR,
        sourcePage = 1,
        sourceUrl = INSURANCE_SOURCE_URL,
        entries = emptyList()
    )

    val insuranceSecondYear = appliedSciencesSchedule(
        department = INSURANCE_DEPARTMENT,
        classYear = SECOND_YEAR,
        sourcePage = 1,
        sourceUrl = INSURANCE_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "", "Aktüerya İstatistiği", "A. TÜRKMEN", "D10"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "", "Örgütsel Davranış", "C. TUFAN", "D6"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "", "Hayat Dışı Sigortalar", "Ş. ÖZDEMİR ERSÖZ", "D3"),
            programEntry(ScheduleDay.WEDNESDAY, "11:30", "14:20", "", "Finansal Yönetim I", "Y. KILIÇ", "D1"),
            programEntry(ScheduleDay.WEDNESDAY, "14:30", "17:20", "", "Pazarlama İlkeleri", "A. TÜRKMEN", "İletişim B Blok D7"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "", "Sigorta Muhasebesi", "F. YILDIRIM", "İletişim B Blok D10")
        )
    )

    val insuranceThirdYear = appliedSciencesSchedule(
        department = INSURANCE_DEPARTMENT,
        classYear = THIRD_YEAR,
        sourcePage = 2,
        sourceUrl = INSURANCE_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "", "Risk Yönetimi I", "F. YETİZ", "İletişim B Blok D7"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "", "Müşteri İlişkileri Yönetimi", "A. TÜRKMEN", "İletişim B Blok D8"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "", "Reasürans", "H. YALAZ", "D8"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "", "Tarım Sigortaları", "H. YALAZ", "D8"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "16:20", "", "Taşınmaz Sigortaları", "D. TURGUT GÜNEL", "İletişim B Blok D9"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:20", "", "Toplumsal Duyarlılık ve Katkı", "N. EKŞİLİ", "D1"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "", "Finansal Okuryazarlık", "Y. KILIÇ", "D2")
        )
    )

    val insuranceFourthYear = appliedSciencesSchedule(
        department = INSURANCE_DEPARTMENT,
        classYear = FOURTH_YEAR,
        sourcePage = 3,
        sourceUrl = INSURANCE_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "", "Sağlık Sigortaları", "F. YETİZ", "D8"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "", "Finansal Sigortalar", "H. YALAZ", "D8"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "", "Sosyal Güvenlik, Emeklilik ve Hayat Sigortaları", "D. TURGUT GÜNEL", "İletişim B Blok D6"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "", "Mesleki İngilizce", "A. TÜRKMEN", "İletişim B Blok D7"),
            programEntry(ScheduleDay.WEDNESDAY, "14:30", "17:20", "", "Finansal Tablolar Analizi", "Y. KILIÇ", "D4"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "", "Sigorta Aracıları Yönetimi", "C. TUFAN", "D6")
        )
    )

    val internationalTradeLogisticsFirstYear = appliedSciencesSchedule(
        department = INTERNATIONAL_TRADE_LOGISTICS_DEPARTMENT,
        classYear = FIRST_YEAR,
        sourcePage = 1,
        sourceUrl = INTERNATIONAL_TRADE_LOGISTICS_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "", "İşletme Matematiği", "O. YAKIT", "İletişim B Blok D5"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "", "Lojistik İlkeleri", "A. COŞKUN", "İletişim B Blok D7"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "11:20", "", "İktisada Giriş I", "H. TOPUZ", "İletişim B Blok D5"),
            programEntry(ScheduleDay.TUESDAY, "11:30", "12:20", "", "Kariyer Planlama", "H. TOPUZ", "İletişim B Blok D5"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "", "Türk Dili I", "N. KILINÇ İNEVİ", "İletişim B Blok D10"),
            programEntry(ScheduleDay.WEDNESDAY, "14:30", "16:20", "", "Hukukun Temel Kavramları", "Ş. YİRMİBEŞOĞLU", "D5"),
            programEntry(ScheduleDay.FRIDAY, "10:30", "12:20", "", "İngilizce I", "B. AKINCI", "İletişim B Blok D10"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "10:20", "", "Atatürk İlkeleri ve İnkılap Tarihi I", "B. KÜMBÜL UZUNSAKAL", "İletişim B Blok D10"),
            programEntry(ScheduleDay.FRIDAY, "13:30", "16:20", "", "İşletme Yönetimine Giriş", "I. HATİPOĞLU", "D5")
        )
    )

    val internationalTradeLogisticsSecondYear = appliedSciencesSchedule(
        department = INTERNATIONAL_TRADE_LOGISTICS_DEPARTMENT,
        classYear = SECOND_YEAR,
        sourcePage = 2,
        sourceUrl = INTERNATIONAL_TRADE_LOGISTICS_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "", "Genel Muhasebe I", "A. CİĞER", "D3"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "", "Dış Ticaretin Finansmanı", "H. ÖZEKİCİOĞLU", "İletişim B Blok D6"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "", "Mikro İktisat", "A. AKAY", "D5"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "13:20", "", "Ticaret Hukuku", "Ş. YİRMİBEŞOĞLU", "D5"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "", "Depolama ve Dağıtım", "İ. KARAYÜN", "İletişim B Blok D5"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "", "Uluslararası İşletmecilik", "A. COŞKUN", "İletişim B Blok D5"),
            programEntry(ScheduleDay.FRIDAY, "13:30", "16:20", "", "Lojistik Bilgi Sistemleri", "O. YAKIT", "D2")
        )
    )

    val internationalTradeLogisticsThirdYear = appliedSciencesSchedule(
        department = INTERNATIONAL_TRADE_LOGISTICS_DEPARTMENT,
        classYear = THIRD_YEAR,
        sourcePage = 3,
        sourceUrl = INTERNATIONAL_TRADE_LOGISTICS_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "", "Uluslararası İktisat", "H. ÖZEKİCİOĞLU", "D9"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "", "E-Ticaret", "O. YAKIT", "İletişim B Blok D5"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "", "Maliyet Muhasebesi", "A. KAYA", "D3"),
            programEntry(ScheduleDay.TUESDAY, "12:30", "14:20", "", "Toplumsal Duyarlılık ve Katkı 2", "H. ÖZEKİCİOĞLU", "İletişim B Blok D7"),
            programEntry(ScheduleDay.TUESDAY, "14:30", "17:20", "", "İnsan Kaynakları Yönetimi", "C. TUFAN", "İletişim B Blok D5"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "10:20", "", "Toplumsal Duyarlılık ve Katkı", "H. TOPUZ", "D4"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "13:20", "", "Tedarik Zinciri Yönetimi", "F. MERDİVENCİ", "D2"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "16:20", "", "Lojistikte İstatistik Uygulamaları", "F. MERDİVENCİ", "D2"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "", "Finansal Yönetim", "Y. KILIÇ", "D4"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "", "Üretim Yönetimi", "F. MERDİVENCİ", "D5"),
            programEntry(ScheduleDay.FRIDAY, "09:30", "12:20", "", "Havayolu Yönetimi ve Lojistiği", "I. HATİPOĞLU", "D2")
        )
    )

    val internationalTradeLogisticsFourthYear = appliedSciencesSchedule(
        department = INTERNATIONAL_TRADE_LOGISTICS_DEPARTMENT,
        classYear = FOURTH_YEAR,
        sourcePage = 4,
        sourceUrl = INTERNATIONAL_TRADE_LOGISTICS_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "", "Performansa Dayalı Lojistik", "A. COŞKUN", "İletişim B Blok D9"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "", "Lojistikte Atölye Uygulamaları", "İ. KARAYÜN", "İletişim B Blok D6"),
            programEntry(ScheduleDay.TUESDAY, "10:30", "12:20", "", "Lojistik ve Taşımacılık Hukuku", "Ş. YİRMİBEŞOĞLU", "D5"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "17:20", "", "Girişimcilik ve KOBİ", "N. EKŞİLİ", "İletişim B Blok D4"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "11:20", "", "Sigorta ve Risk Yönetimi", "Y. KILIÇ", "D9"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "18:20", "", "Gümrük Rejimleri ve Serbest Bölgeler", "C. KAYGUSUZ", "İİBF A Blok Turkuaz Amfi"),
            programEntry(ScheduleDay.FRIDAY, "09:30", "12:20", "", "Tersine Lojistik", "O. YAKIT", "İletişim B Blok D4"),
            programEntry(ScheduleDay.FRIDAY, "15:30", "18:20", "", "Afet ve İnsani Yardım Lojistiği", "İ. KARAYÜN", "İletişim B Blok D9")
        )
    )

    val managementInformationSystemsFirstYear = appliedSciencesSchedule(
        department = MANAGEMENT_INFORMATION_SYSTEMS_DEPARTMENT,
        classYear = FIRST_YEAR,
        sourcePage = 1,
        sourceUrl = MANAGEMENT_INFORMATION_SYSTEMS_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "11:20", "", "Bilişim Sistemleri ve Teknolojilerine Giriş", "G. TONGUÇ", "Enformatik LAB 7"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "", "İktisada Giriş I", "A. AKAY", "D2"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "14:20", "", "Kariyer Planlama", "C. TUFAN", "İletişim B Blok D5"),
            programEntry(ScheduleDay.TUESDAY, "14:30", "17:20", "", "Pazarlama İlkeleri", "A. TÜRKMEN", "İletişim B Blok D7"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "10:20", "", "İngilizce I", "H. ÇELİK", "İletişim B Blok D10"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "", "Atatürk İlkeleri ve İnkılap Tarihi I", "N. ÇETİNKAYA", "İletişim B Blok D9"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "", "Türk Dili I", "N. KILINÇ İNEVİ", "İletişim B Blok D10"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "", "Algoritma ve Programlamaya Giriş", "T. YÖRÜK", "UBF Lab 1"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "", "Matematik", "B. ŞİMŞEK", "İletişim B Blok D6")
        )
    )

    val managementInformationSystemsSecondYear = appliedSciencesSchedule(
        department = MANAGEMENT_INFORMATION_SYSTEMS_DEPARTMENT,
        classYear = SECOND_YEAR,
        sourcePage = 2,
        sourceUrl = MANAGEMENT_INFORMATION_SYSTEMS_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "", "Örgütsel Davranış", "C. TUFAN", "İletişim B Blok D6"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "", "Web Tabanlı Programlama - 1", "G. TONGUÇ", "Enformatik LAB 7"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "", "Uygulamalı Girişimcilik", "N. EKŞİLİ", "İletişim B Blok D9"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "", "Proje Yönetimi", "M. SEKRETER", "D2"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "", "Genel Muhasebe I", "A. CİĞER", "D7"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "16:20", "", "Veritabanına Giriş", "K. ÇEVİK", "UBF Lab2"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "", "İstatistik I", "B. ŞİMŞEK", "D5"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "", "Nesne Yönelimli Programlama - 1", "T. YÖRÜK", "UBF Lab 1")
        )
    )

    val managementInformationSystemsThirdYear = appliedSciencesSchedule(
        department = MANAGEMENT_INFORMATION_SYSTEMS_DEPARTMENT,
        classYear = THIRD_YEAR,
        sourcePage = 3,
        sourceUrl = MANAGEMENT_INFORMATION_SYSTEMS_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "", "Bilgi Sistemleri Entegrasyonu", "M. EMEK", "UBF Lab2"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "", "Mobil Uygulama Geliştirme", "M. EMEK", "UBF Lab2"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "", "Yöneylem Araştırması I", "Ö. TOSUN", "İletişim B Blok D10"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "", "Bilgisayarla Görme", "K. ÇEVİK", "UBF Lab2"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "10:20", "", "Toplumsal Duyarlılık ve Katkı 2", "C. TUFAN", "İletişim B Blok D4"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "13:20", "", "Karar Verme Teknikleri", "S. IRMAK", "İletişim B Blok D4"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "16:20", "", "Stratejik Yönetim", "C. TUFAN", "D6"),
            programEntry(ScheduleDay.THURSDAY, "08:30", "10:20", "", "Toplumsal Duyarlılık ve Katkı", "N. EKŞİLİ", "İletişim B Blok D6"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "13:20", "", "Tüketici Davranışları", "A. TÜRKMEN", "İletişim B Blok D7"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "", "İnsan Kaynakları Yönetimi", "C. TUFAN", "İletişim B Blok D8"),
            programEntry(ScheduleDay.FRIDAY, "09:30", "12:20", "", "Yapay Zeka ve Öğrenen Algoritmalar", "S. IRMAK", "UBF Lab2")
        )
    )

    val managementInformationSystemsFourthYear = appliedSciencesSchedule(
        department = MANAGEMENT_INFORMATION_SYSTEMS_DEPARTMENT,
        classYear = FOURTH_YEAR,
        sourcePage = 4,
        sourceUrl = MANAGEMENT_INFORMATION_SYSTEMS_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "", "Maliyet Muhasebesi", "A. KAYA", "D2"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "", "Kurumsal Veri Yönetimi", "M. EMEK", "UBF Lab2"),
            programEntry(ScheduleDay.TUESDAY, "15:30", "18:20", "", "Yönetim Bilişim Sistemlerinde Güncel Konular", "T. YÖRÜK", "UBF Lab 1"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "", "Derin Öğrenme", "K. ÇEVİK", "UBF Lab2"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "18:20", "", "Robotik Uygulamaları", "G. TONGUÇ.", "Enformatik LAB 7"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "", "Müşteri İlişkileri Yönetimi", "A. TÜRKMEN", "İletişim B Blok D7"),
            programEntry(ScheduleDay.THURSDAY, "16:30", "19:15", "", "Kurumsal Kaynak Planlama", "G. TONGUÇ.", "Enformatik LAB 7"),
            programEntry(ScheduleDay.FRIDAY, "13:30", "16:20", "", "Uygulamalı Veri Madenciliği", "S. IRMAK", "UBF Lab2")
        )
    )

    val scienceEducationFirstYear = educationSchedule(
        classYear = FIRST_YEAR,
        sourcePage = 1,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "", "Eğitime Giriş", "Doç. Dr. Gülnar ÖZYILDIRIM", "Sınıf Z22"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "", "Bilişim Teknolojileri", "Öğr. Gör. Dr. T. Fatih KASALAK", "Sanal Sınıf"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "", "Fizik 1", "Prof. Dr. Erol EROĞLU", "Z22"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "", "Fizik 1 Laboratuvarı", "Prof. Dr. Erol EROĞLU", "B Blok Kat 5 Fizik Lab."),
            programEntry(ScheduleDay.TUESDAY, "15:30", "17:20", "", "Genel Matematik 1", "Doç. Dr. Burak KURT", "Sınıf Z22"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "", "Kimya 1", "Doç. Dr. Furkan ÖZEN", "Sınıf Z21"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "", "Türk Dili 1", "Öğr. Gör. Dr. Emre KAYASANDIK", "331"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:20", "", "Kimya Laboratuvarı 1", "Doç. Dr. Furkan ÖZEN", "A Blok Kat 5 Kimya Lab."),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "", "Eğitim Felsefesi", "Öğr. Gör. Dr. Mehmet DUGAN", "Sınıf Z21"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "10:20", "", "Atatürk İlkeleri ve İnkılap T.", "Öğr. Gör. Dr. Koray ERGİN", "147-B"),
            programEntry(ScheduleDay.FRIDAY, "10:30", "12:20", "", "Yabancı Dil 1 (İngilizce)", "Öğr. Gör. Güler Ceylan KUŞOĞLU", "147-C")
        )
    )

    val scienceEducationSecondYear = educationSchedule(
        classYear = SECOND_YEAR,
        sourcePage = 2,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "08:30", "10:20", "", "Bağımlılık ve Bağımlılıkla Mücadele", "Dr. Öğr. Üyesi Nesrin EMRE", "Sınıf Z22", "Seçmeli"),
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "", "Yapay Zekaya Giriş", "Prof. Dr. Erol EROĞLU", "A Blok Bilgisayar Lab. 1", "Seçmeli"),
            programEntry(ScheduleDay.MONDAY, "13:30", "17:20", "", "Kimya 3", "Prof. Dr. Memduh Sami TANER", "Sınıf Z23"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "10:20", "", "Biyoloji Laboratuvarı 1", "Doç. Dr. Ayşe Gül NASIRCILAR", "A Blok Kat 5 Fen Bilgisi Lab."),
            programEntry(ScheduleDay.TUESDAY, "10:30", "12:20", "", "Öğretim İlke ve Yöntemleri", "Doç. Dr. Miray DAĞYAR", "Sınıf Z24"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "", "Fizik 3", "Doç. Dr. Canel EKE", "Sınıf Z24"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "10:20", "", "Eğitim Hukuku", "Doç. Dr. Gamze KASALAK", "Sınıf Z22", "Seçmeli"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "", "Fen ve Teknoloji Kaynaklı Sorunlar", "Doç. Dr. Fatih Serdar YILDIRIM", "Sınıf Z23", "Seçmeli"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "", "Türkiye'nin Biyolojik Zenginlikleri", "Prof. Dr. İsmail Gökhan DENİZ", "Sınıf Z22", "Seçmeli"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "", "Eğitimde Araştırma Yöntemleri", "Prof. Dr. Bayram BIÇAK", "Sınıf Z24"),
            programEntry(ScheduleDay.THURSDAY, "08:30", "10:20", "", "Beslenme ve Sağlık", "Dr. Öğr. Üyesi Nesrin EMRE", "Sınıf Z23", "Seçmeli"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:20", "", "Fen Öğrenme ve Öğretim Yaklaşımları", "Prof. Dr. Esme HACIEMİNOĞLU", "Sınıf Z23"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "", "Ekonomi ve Girişimcilik", "Prof. Dr. Memduh Sami TANER", "Sınıf Z24", "Seçmeli"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "12:20", "", "Biyoloji 1", "Prof. Dr. Hakan SERT", "Sınıf Z22")
        )
    )

    val scienceEducationThirdYear = educationSchedule(
        classYear = THIRD_YEAR,
        sourcePage = 3,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "", "Eğitimde Proje Hazırlama", "Prof. Dr. Memduh Sami TANER", "Sınıf Z24", "Seçmeli"),
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "", "Eğitimde Program Dışı Etkinlikler", "Prof. Dr. İsmail Gökhan DENİZ", "Sınıf Z21"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "", "Fen Öğretimi 1", "Doç. Dr. Mustafa DOĞRU", "A Blok Kat 5 Fen Bilgisi Lab."),
            programEntry(ScheduleDay.TUESDAY, "08:30", "11:20", "", "Fen Öğretimi Laboratuvar Uygulamaları 1", "Dr. Öğr. Üyesi Nesrin EMRE", "Sınıf Z21"),
            programEntry(ScheduleDay.TUESDAY, "10:30", "12:20", "", "Öğretim İlke ve Yöntemleri", "Doç. Dr. Miray DAĞYAR", "Sınıf Z24"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "", "Biyokimya", "Doç. Dr. Ayşe Gül NASIRCILAR", "Sınıf Z23", "Seçmeli"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "10:20", "", "İlk Yardım Eğitimi", "Dr. Öğr. Üyesi Nesrin EMRE", "Sınıf Z23", "Seçmeli"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "", "Türk Eğitim Sistemi ve Okul Yönetimi", "Doç. Dr. Gamze KASALAK", "Sınıf Z25"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "", "Medya Okuryazarlığı", "Doç. Dr. Fatih Serdar YILDIRIM", "Sınıf Z22", "Seçmeli"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "17:20", "", "İnsan Anatomisi ve Fizyolojisi", "Doç. Dr. Nilüfer GÜLMEN", "Sınıf Z24", "Seçmeli"),
            programEntry(ScheduleDay.THURSDAY, "08:30", "10:20", "", "Eğitimde Ölçme ve Değerlendirme", "Doç. Dr. Esin KOĞAR YILMAZ", "Sınıf Z24"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:20", "", "Astronomi", "Prof. Dr. Fatma GÖK", "Sınıf Z24"),
        )
    )

    val scienceEducationFourthYear = educationSchedule(
        classYear = FOURTH_YEAR,
        sourcePage = 4,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "", "Okullarda Rehberlik", "Öğr. Gör. Fazilet BARÇIN KARA", "Sınıf Z23"),
            programEntry(ScheduleDay.TUESDAY, "10:30", "12:20", "", "Çevre Eğitimi", "Doç. Dr. Ayşe Gül NASIRCILAR", "Sınıf Z23"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "", "Öğretmenlik Uygulaması I", "Doç. Dr. Mustafa DOĞRU / Doç. Dr. Furkan ÖZEN / Dr. Öğr. Üyesi Nesrin EMRE", "İlgili Öğretim Üyesi Ofisi"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "10:20", "", "Öğretmenlik Uygulaması I", "Prof. Dr. Sait BULUT", "İlgili Öğretim Üyesi Ofisi"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "", "Yenilenebilir Enerji Kaynakları", "Prof. Dr. Mustafa HOŞTUT", "Sınıf Z24", "Seçmeli"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "", "Disiplinler Arası Fen Öğretimi", "Doç. Dr. Nilüfer GÜLMEN", "Sınıf Z25"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "17:20", "", "Fen Bilgisi Ders Kitabı İncelemesi", "Doç. Dr. Fatih Serdar YILDIRIM", "Sınıf Z25", "Seçmeli"),
            programEntry(ScheduleDay.THURSDAY, "08:30", "10:20", "", "Öğretmenlik Uygulaması I", "Doç. Dr. Fatih Serdar YILDIRIM", "İlgili Öğretim Üyesi Ofisi"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:20", "", "Okul Dışı Öğrenme Ortamları", "Doç. Dr. Fatih Serdar YILDIRIM", "Sınıf Z25", "Seçmeli"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "", "Mikro Öğretim", "Prof. Dr. Esme HACIEMİNOĞLU", "Sınıf Z23", "Seçmeli"),
            programEntry(ScheduleDay.THURSDAY, "15:30", "17:20", "", "Öğretmenlik Uygulaması I", "Prof. Dr. Esme HACIEMİNOĞLU", "İlgili Öğretim Üyesi Ofisi"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "10:20", "", "Atatürk İlkeleri ve İnkılap T.", "Öğr. Gör. Dr. Koray ERGİN", "147-B"),
            programEntry(ScheduleDay.FRIDAY, "10:30", "12:20", "", "Yabancı Dil 1 (İngilizce)", "Öğr. Gör. Güler Ceylan KUŞOĞLU", "147-C")
        )
    )

    val englishTeachingFirstYear = educationSchedule(
        department = ENGLISH_TEACHING_DEPARTMENT,
        classYear = FIRST_YEAR,
        sourcePage = 1,
        sourceUrl = ENGLISH_TEACHING_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "AZİÖ105", "Dinleme ve Sesletim 1", "Hüseyin KAFES", "320 - Dinleme Dersliği"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "GKİÖ103", "Yabancı Dil I (Rusça)", "Elena GROMOVA", "220"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "ENF105", "Bilişim Teknolojileri", "Evren SEZGİN", "Sanal01"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "AZİÖ107", "Sözlü İletişim Becerileri 1", "Simla COURSE", "220", section = "1. Şube"),
            programEntry(ScheduleDay.TUESDAY, "15:30", "17:20", "MBZ105", "Eğitim Sosyolojisi", "Mimar TÜRKKAHRAMAN", "147-C"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "10:20", "GKİÖ101", "Yabancı Dil I (Almanca)", "Arzu AYDEMİR ÜMİT", "220"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "AZİÖ107", "Sözlü İletişim Becerileri 1", "Simla COURSE", "220", section = "2. Şube"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "AZİÖ103", "Yazma Becerileri 1", "Başak Eda AZİZOĞLU", "218"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "17:20", "MBZ101", "Eğitime Giriş", "Ali SABANCI", "147-C"),
            programEntry(ScheduleDay.THURSDAY, "08:30", "09:20", "MBZ305", "Eğitimde Ahlak ve Etik", "Ramazan GÖK", "147-C"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "13:20", "TDE105", "Türk Dili 1", "Emre KAYASANDIK", "147-C"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "AZİÖ101", "Okuma Becerileri 1", "Funda ÖLMEZ ÇAĞLAR", "219"),
            programEntry(ScheduleDay.THURSDAY, "15:30", "16:20", "KPD101", "Kariyer Planlama", "H. Nuran CANER", "125"),
            programEntry(ScheduleDay.FRIDAY, "13:30", "15:20", "ATA105", "Atatürk İlkeleri ve İnkılap Tarihi 1", "Koray ERGİN", "147-C")
        )
    )

    val englishTeachingSecondYear = educationSchedule(
        department = ENGLISH_TEACHING_DEPARTMENT,
        classYear = SECOND_YEAR,
        sourcePage = 2,
        sourceUrl = ENGLISH_TEACHING_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "MBS207", "Eğitim Hukuku", "Kemal KAYIKÇI", "219"),
            programEntry(ScheduleDay.MONDAY, "15:30", "17:20", "AZİÖ207", "Eleştirel Okuma ve Yazma", "Ersen VURAL", "222"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "AZİÖ205", "Dilbilimi 1", "Hüseyin KAFES", "320 - Dinleme Dersliği"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "MBZ205", "Öğretim İlke ve Yöntemleri", "Günseli ORHON", "147-C"),
            programEntry(ScheduleDay.TUESDAY, "15:30", "17:20", "ASİÖ203", "İngilizce Sözcük Bilgisi Öğretimi", "Ersen VURAL", "222"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "12:20", "MBZ207", "Eğitim Teknolojisi", "Oğuzhan ATABEK", "218"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "ENF213", "Bilişim Teknolojileri Bağımlılığı", "T. Fatih KASALAK", "Sanal01"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "GKS207", "Bilim ve Araştırma Etiği", "Fatih YILDIZ", "220"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "17:20", "MBS201", "Açık ve Uzaktan Öğrenme", "Saim TURAN", "329"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "AZİÖ209", "İngilizce Öğrenme ve Öğretim Yaklaşımları 1", "Funda ÖLMEZ ÇAĞLAR", "218"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "AZİÖ203", "İngiliz Edebiyatı 1", "Başak Eda AZİZOĞLU", "218")
        )
    )

    val englishTeachingThirdYear = educationSchedule(
        department = ENGLISH_TEACHING_DEPARTMENT,
        classYear = THIRD_YEAR,
        sourcePage = 3,
        sourceUrl = ENGLISH_TEACHING_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "GKS301", "Medya Okuryazarlığı", "Evren CAPPELLARO", "218"),
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "GKS305", "Sanat ve Estetik", "Zeynep Çiğdem ÇENGEL", "220"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "GKZ301", "Toplumsal Duyarlılık ve Katkı Projeleri", "Hüseyin KAFES", "218", section = "1. Şube"),
            programEntry(ScheduleDay.MONDAY, "15:30", "17:20", "GKZ301", "Toplumsal Duyarlılık ve Katkı Projeleri", "Hüseyin KAFES", "218", section = "2. Şube"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "AZİÖ305", "Dil ve Edebiyat Öğretimi 1", "F. Özlem SAKA", "219"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "AZİÖ301", "Çocuklara Yabancı Dil Öğretimi 1", "Binnur GENÇ İLTER", "221", section = "1. Şube"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "AZİÖ305", "Dil ve Edebiyat Öğretimi 1", "F. Özlem SAKA", "219", section = "2. Şube"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:20", "ASİÖ305", "Uygulamalı Dilbilim", "Hüseyin KAFES", "320 - Dinleme Dersliği"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "ASİÖ303", "Söylem Çözümlemesi ve Dil Öğretimi", "Simla COURSE", "220"),
            programEntry(ScheduleDay.THURSDAY, "15:30", "17:20", "MBS303", "Eğitimde Program Geliştirme", "Harun ŞAHİN", "220"),
            programEntry(ScheduleDay.THURSDAY, "15:30", "17:20", "MBS305", "Eğitimde Proje Hazırlama", "Memduh Sami Taner", "119"),
            programEntry(ScheduleDay.FRIDAY, "09:30", "12:20", "AZİÖ303", "İngilizce Dil Becerilerinin Öğretimi 1", "Şeyma KOÇ", "222"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "16:20", "MBZ307", "Sınıf Yönetimi", "Çiğdem APAYDIN", "218")
        )
    )

    val englishTeachingFourthYear = educationSchedule(
        department = ENGLISH_TEACHING_DEPARTMENT,
        classYear = FOURTH_YEAR,
        sourcePage = 4,
        sourceUrl = ENGLISH_TEACHING_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "08:30", "09:20", "GKZ401", "Toplumsal Duyarlılık ve Katkı Projeleri (Uygulama)", "Binnur GENÇ İLTER / F. Özlem SAKA / Başak Eda AZİZOĞLU / Simla COURSE / Ersen VURAL / Funda ÖLMEZ ÇAĞLAR / Şeyma KOÇ", "Çeşitli sınıflar"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "AZİÖ401", "İngilizce Öğretiminde Ders İçeriği Geliştirme", "F. Özlem SAKA", "219", section = "1. Şube"),
            programEntry(ScheduleDay.TUESDAY, "10:30", "12:20", "MBS403", "Mikro Öğretim", "Şirin KÜÇÜKAVCI", "221"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "MBZ405", "Özel Eğitim ve Kaynaştırma", "Abdullah ÇİFTÇİ", "147-C"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "16:20", "AZİÖ401", "İngilizce Öğretiminde Ders İçeriği Geliştirme", "F. Özlem SAKA", "219", section = "2. Şube"),
            programEntry(ScheduleDay.WEDNESDAY, "16:30", "17:20", "GKZ401", "Toplumsal Duyarlılık ve Katkı Projeleri (Teori)", "Binnur GENÇ İLTER / F. Özlem SAKA / Başak Eda AZİZOĞLU / Simla COURSE / Ersen VURAL / Funda ÖLMEZ ÇAĞLAR / Şeyma KOÇ", "Çeşitli sınıflar"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "MBS401", "Karşılaştırmalı Eğitim", "Harun ŞAHİN", "221"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:20", "MBS405", "Müze Eğitimi", "Tülin TÜMTÜRK", "Z-27"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "ASİÖ401", "İngilizce Öğretiminde Materyal Tasarımı", "Binnur GENÇ İLTER", "221"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "ASİÖ403", "Tümleşik Dil Becerilerinin Öğretimi", "Şeyma KOÇ", "222"),
            programEntry(ScheduleDay.THURSDAY, "15:30", "17:20", "MBZ401", "Öğretmenlik Uygulaması 1 (Teori)", "Binnur GENÇ İLTER / Hüseyin KAFES / F. Özlem SAKA / Başak Eda AZİZOĞLU / Simla COURSE / Ersen VURAL / Funda ÖLMEZ ÇAĞLAR / Şeyma KOÇ", "Çeşitli sınıflar"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "12:20", "MBZ401", "Öğretmenlik Uygulaması 1 (Uygulama)", "Uygulama", "Uygulama"),
            programEntry(ScheduleDay.FRIDAY, "13:30", "15:20", "MBZ401", "Öğretmenlik Uygulaması 1 (Uygulama)", "Uygulama", "Uygulama")
        )
    )

    val elementaryMathematicsEducationFirstYear = educationSchedule(
        department = ELEMENTARY_MATHEMATICS_EDUCATION_DEPARTMENT,
        classYear = FIRST_YEAR,
        sourcePage = 1,
        sourceUrl = ELEMENTARY_MATHEMATICS_EDUCATION_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "", "Bilgisayar Destekli Matematik Öğretimi", "B. KURT", "232"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "", "Bilişim Teknolojileri", "T. F. KASALAK", "Sanal Sınıf"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "09:20", "", "Analiz 1", "S.S. EVCAN", "232"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "", "Soyut Matematik", "Z. EKEN", "231"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "", "Olasılık", "G. TINAZTEPE", "232"),
            programEntry(ScheduleDay.TUESDAY, "15:30", "17:20", "", "Eğitimde Ahlak ve Etik", "G. ÖZYILDIRIM", "233"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "13:20", "", "Türk Dili I", "E. KAYASANDIK", "233"),
            programEntry(ScheduleDay.WEDNESDAY, "14:30", "17:20", "", "Sayıların Öğretimi I", "Ş. KOZA ÇİFTÇİ KARADAĞ", "318"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:20", "", "Beslenme ve Sağlık", "N. EMRE", "231"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "", "Matematiğin Temelleri I", "B. KURT", "231"),
            programEntry(ScheduleDay.THURSDAY, "15:30", "17:20", "", "Eğitime Giriş", "S. KARATAŞ", "232"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "10:20", "", "Atatürk İlkeleri ve İnkılap Tarihi I", "D. ÖGE SET", "147A"),
            programEntry(ScheduleDay.FRIDAY, "10:30", "12:20", "", "Yabancı Dil I (İngilizce)", "E. DÖNÜŞ", "147A")
        )
    )

    val elementaryMathematicsEducationSecondYear = educationSchedule(
        department = ELEMENTARY_MATHEMATICS_EDUCATION_DEPARTMENT,
        classYear = SECOND_YEAR,
        sourcePage = 1,
        sourceUrl = ELEMENTARY_MATHEMATICS_EDUCATION_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "08:30", "09:20", "", "Bilim ve Araştırma Etiği", "M. SAMİ TANER", "232"),
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "", "Geometri ve Ölçme Eğitimi", "A. ÖZKAYA", "318"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "", "Geometri ve Ölçme Eğitimi", "A. ÖZKAYA", "318"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "10:20", "", "Analiz 1", "S.S. EVCAN", "232"),
            programEntry(ScheduleDay.TUESDAY, "12:30", "15:20", "", "Eğitimde Proje Hazırlama", "G. KARAMIK", "233"),
            programEntry(ScheduleDay.TUESDAY, "15:30", "17:20", "", "Matematik Öğretiminde Kavram Yanılgıları", "Z. EKEN", "234"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "", "Lineer Cebir I", "S. SEZER", "232"),
            programEntry(ScheduleDay.WEDNESDAY, "12:30", "15:20", "", "Eğitimde Drama", "G. KARAMIK", "Z28"),
            programEntry(ScheduleDay.THURSDAY, "08:30", "09:20", "", "Sayılar Teorisi", "S. SEZER", "232"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:20", "", "Analiz 1", "S.S. EVCAN", "232"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "", "Öğretim İlke ve Yöntemleri", "M. DAĞYAR", "232"),
            programEntry(ScheduleDay.THURSDAY, "15:30", "17:20", "", "Eğitimde Program Geliştirme", "İ. ÖNAL", "233")
        )
    )

    val elementaryMathematicsEducationThirdYear = educationSchedule(
        department = ELEMENTARY_MATHEMATICS_EDUCATION_DEPARTMENT,
        classYear = THIRD_YEAR,
        sourcePage = 1,
        sourceUrl = ELEMENTARY_MATHEMATICS_EDUCATION_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "", "Geometri ve Ölçme Eğitimi", "A. ÖZKAYA", "318", note = "1. Şube"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "", "İstatistik", "B. KURT", "233"),
            programEntry(ScheduleDay.TUESDAY, "10:30", "12:20", "", "Cebir", "S. SEZER", "233"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "", "Karşılaştırmalı Eğitim", "S.S. EVCAN", "230"),
            programEntry(ScheduleDay.TUESDAY, "15:30", "17:20", "", "Matematik Öğretiminde Kavram Yanılgıları", "Z. EKEN", "234", note = "2. Şube"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "10:20", "", "Matematik Öğretiminde Etkinlik Geliştirme", "G. KARAMIK", "318", note = "1. Şube"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "", "Matematik Öğretiminde Etkinlik Geliştirme", "G. KARAMIK", "318", note = "2. Şube"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "", "Medya Okuryazarlığı", "B. ASMA", "230"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "", "Mesleki İngilizce", "R. KARATAŞ", "231"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "16:20", "", "İlkokul Matematik Öğretimi", "R. KARATAŞ", "230"),
            programEntry(ScheduleDay.WEDNESDAY, "16:30", "18:20", "", "Sınıf İçi Öğrenmelerin Değerlendirilmesi", "M. A. KARAGÖZ", "234"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "", "Geometri ve Ölçme Eğitimi", "A. ÖZKAYA", "318", note = "2. Şube"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "", "Sınıf Yönetimi", "S. KARATAŞ", "233"),
            programEntry(ScheduleDay.THURSDAY, "15:30", "17:20", "", "Eğitimde Program Geliştirme", "İ. ÖNAL", "233"),
            programEntry(ScheduleDay.FRIDAY, "09:30", "12:20", "", "Sayıların Öğretimi", "Ş. KOZA ÇİFTÇİ KARADAĞ", "318", note = "1. Şube"),
            programEntry(ScheduleDay.FRIDAY, "13:30", "16:20", "", "Sayıların Öğretimi", "Ş. KOZA ÇİFTÇİ KARADAĞ", "318", note = "2. Şube")
        )
    )

    val elementaryMathematicsEducationFourthYear = educationSchedule(
        department = ELEMENTARY_MATHEMATICS_EDUCATION_DEPARTMENT,
        classYear = FOURTH_YEAR,
        sourcePage = 1,
        sourceUrl = ELEMENTARY_MATHEMATICS_EDUCATION_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.TUESDAY, "10:30", "12:20", "", "Mantıksal Akıl Yürütme", "B. KURT", "234"),
            programEntry(ScheduleDay.TUESDAY, "12:30", "13:20", "", "Öğretmenlik Uygulaması I", "Z. EKEN", "Uygulama"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "", "Karşılaştırmalı Eğitim", "S.S. EVCAN", "230"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "", "Mikro Öğretim", "Z. EKEN", "234"),
            programEntry(ScheduleDay.TUESDAY, "15:30", "17:20", "", "Matematik Öğretiminde Kavram Yanılgıları", "Z. EKEN", "234", note = "2. Şube"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "10:20", "", "Öğretmenlik Uygulaması I", "Ş. KOZA ÇİFTÇİ KARADAĞ / R. KARATAŞ / B. KURT", "Uygulama"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "", "Matematik Öğretiminde Kavram Yanılgıları", "Z. EKEN", "234", note = "1. Şube"),
            programEntry(ScheduleDay.WEDNESDAY, "12:30", "13:20", "", "Öğretmenlik Uygulaması I", "Z. EKEN", "Uygulama"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "", "Özel Eğitim ve Kaynaştırma", "A. ÇİFTÇİ", "234"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "16:20", "", "İlkokul Matematik Öğretimi", "R. KARATAŞ", "230"),
            programEntry(ScheduleDay.WEDNESDAY, "16:30", "18:20", "", "Sınıf İçi Öğrenmelerin Değerlendirilmesi", "M. A. KARAGÖZ", "234"),
            programEntry(ScheduleDay.THURSDAY, "08:30", "09:20", "", "Öğretmenlik Uygulaması I", "G. KARAMIK", "Uygulama"),
            programEntry(ScheduleDay.THURSDAY, "12:30", "13:20", "", "Öğretmenlik Uygulaması I", "G. KARAMIK", "Uygulama"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "", "Matematikte Problem Çözme", "R. KARATAŞ", "234", note = "1. Şube"),
            programEntry(ScheduleDay.THURSDAY, "15:30", "17:20", "", "Matematikte Problem Çözme", "R. KARATAŞ", "234", note = "2. Şube")
        )
    )

    val earlyChildhoodEducationFirstYear = educationSchedule(
        department = EARLY_CHILDHOOD_EDUCATION_DEPARTMENT,
        classYear = FIRST_YEAR,
        sourcePage = 1,
        sourceUrl = EARLY_CHILDHOOD_EDUCATION_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.TUESDAY, "08:30", "10:20", "", "Eğitim Hukuku", "Prof. Dr. Kemal KAYIKÇI", "147-B", "Seçmeli", note = "I. Grup · Meslek Bilgisi"),
            programEntry(ScheduleDay.TUESDAY, "10:30", "12:20", "", "Eğitime Giriş", "Prof. Dr. İlhan GÜNBAYI", "324"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "", "Erken Çocukluk Eğitimine Genel Bakış", "Doç. Dr. Hale KOÇER", "323"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "13:20", "", "Türk Dili I", "Öğr. Gör. Dr. Emre KAYASANDIK", "233"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "", "Akademik Okuryazarlık", "Doç. Dr. Yunus PINAR", "147-B"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "17:20", "", "Okul Dışı Öğrenme Ortamları", "Prof. Dr. Zeliha YAZICI", "325", "Seçmeli", note = "I. Grup · Meslek Bilgisi"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:20", "", "Genel Psikoloji", "Doç. Dr. Yunus PINAR", "324"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "", "İnsan Anatomisi ve Fizyolojisi", "Uzm. Dr. Özge ALKAN", "147-B"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "10:20", "", "Atatürk İlkeleri ve İnkılap Tarihi I", "Öğr. Gör. Derya ÖGE SET", "147-A"),
            programEntry(ScheduleDay.FRIDAY, "10:30", "12:20", "", "İngilizce I", "Öğr. Gör. Esra DÖNÜŞ", "147-A")
        )
    )

    val earlyChildhoodEducationSecondYear = educationSchedule(
        department = EARLY_CHILDHOOD_EDUCATION_DEPARTMENT,
        classYear = SECOND_YEAR,
        sourcePage = 2,
        sourceUrl = EARLY_CHILDHOOD_EDUCATION_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "", "Erken Çocukluk Eğitiminde Drama", "Öğr. Gör. Tülin TÜMTÜRK", "Z-27", section = "A Şubesi"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "", "Çocukla İletişim", "Öğr. Gör. Dr. Ceren KOCA", "324", "Seçmeli", note = "II. Grup · Alan Eğitimi", section = "A Şubesi"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "", "Erken Çocukluk Eğitiminde Drama", "Öğr. Gör. Tülin TÜMTÜRK", "Z-27", section = "B Şubesi"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "10:20", "", "Türk Eğitim Tarihi", "Prof. Dr. Selçuk UYGUN", "323"),
            programEntry(ScheduleDay.TUESDAY, "10:30", "12:20", "", "Çocuk İhmali, İstismarı ve Koruyucu Hizmetler", "Prof. Dr. Zeliha YAZICI", "321", "Seçmeli", note = "II. Grup · Alan Eğitimi"),
            programEntry(ScheduleDay.TUESDAY, "10:30", "12:20", "", "Çocukta Sosyal Beceri Eğitimi", "Doç. Dr. Yakup YILDIRIM", "322", "Seçmeli", note = "II. Grup · Alan Eğitimi"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "", "Erken Çocuklukta Oyun", "Doç. Dr. Emine Ela ŞİMŞEK", "321"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "", "Erken Çocukluk Döneminde Gelişim I", "Doç. Dr. Yakup YILDIRIM", "323"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "", "Yaratıcı Düşünce ve Geliştirilmesi", "Doç. Dr. Hale KOÇER", "323"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "17:20", "", "Erken Çocuklukta Dil Edinimi", "Prof. Dr. Nihat BAYAT", "323"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:20", "", "Bilimsel Araştırma Yöntemleri", "Doç. Dr. Alper SİNAN", "323"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "", "Eğitim Psikolojisi", "Doç. Dr. Yunus PINAR", "323")
        )
    )

    val earlyChildhoodEducationThirdYear = educationSchedule(
        department = EARLY_CHILDHOOD_EDUCATION_DEPARTMENT,
        classYear = THIRD_YEAR,
        sourcePage = 3,
        sourceUrl = EARLY_CHILDHOOD_EDUCATION_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "", "Erken Çocuklukta Ritim, Dans ve Orff Eğitimi", "Öğr. Gör. Dr. Serkan ERTÜRK", "Müzik Dersliği", "Seçmeli", note = "III. Grup · Alan Eğitimi"),
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "", "Çocuklar için Felsefe", "Öğr. Gör. Dr. Ceren KOCA", "324", "Seçmeli", note = "III. Grup · Alan Eğitimi"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "", "Okul Öncesi Eğitim Programları I", "Doç. Dr. Hale KOÇER", "323"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "10:20", "", "Eğitimde Ölçme ve Değerlendirme", "Arş. Gör. Dr. İbrahim Hakkı TEZCİ", "322"),
            programEntry(ScheduleDay.TUESDAY, "10:30", "13:20", "", "Eğitim Teknolojisi", "Doç. Dr. Oğuzhan ATABEK", "323"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "", "Erken Çocukluk Eğitiminde Araştırma", "Prof. Dr. Zeliha YAZICI", "322", "Seçmeli", note = "III. Grup · Alan Eğitimi"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "", "Erken Çocuklukta Dil Öğretimi", "Prof. Dr. Nihat BAYAT", "324", "Seçmeli", note = "III. Grup · Alan Eğitimi"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "", "Erken Çocuklukta Duyu Eğitimi", "Doç. Dr. Begümhan YÜKSEL", "147-A", "Seçmeli", note = "III. Grup · Alan Eğitimi"),
            programEntry(ScheduleDay.TUESDAY, "15:30", "17:20", "", "Erken Çocuklukta Özel Eğitim", "Doç. Dr. Begümhan YÜKSEL", "147-A"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "", "İnsan İlişkileri ve İletişim", "Prof. Dr. Nihat BAYAT", "322", "Seçmeli", note = "II. Grup · Genel Kültür"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "17:20", "", "Türk Eğitim Sistemi ve Okul Yönetimi", "Doç. Dr. Gamze KASALAK", "322"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:20", "", "Kaynaştırma Eğitimi", "Doç. Dr. Begümhan YÜKSEL", "322"),
            programEntry(ScheduleDay.THURSDAY, "15:30", "17:20", "", "Çocuğu Tanıma ve Değerlendirme", "Doç. Dr. Yunus PINAR", "322"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "10:20", "", "İnsan Hakları ve Demokrasi Eğitimi", "Dr. Öğr. Üyesi Özgür AYDIN", "324", "Seçmeli", note = "II. Grup · Genel Kültür"),
            programEntry(ScheduleDay.FRIDAY, "10:30", "13:20", "", "Erken Çocuklukta Görsel Sanat Eğitimi", "Öğr. Gör. Dr. Ezgi BİLGİN", "324")
        )
    )

    val earlyChildhoodEducationFourthYear = educationSchedule(
        department = EARLY_CHILDHOOD_EDUCATION_DEPARTMENT,
        classYear = FOURTH_YEAR,
        sourcePage = 4,
        sourceUrl = EARLY_CHILDHOOD_EDUCATION_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.TUESDAY, "08:30", "10:20", "", "Aile Eğitimi ve Katılımı", "Doç. Dr. Yakup YILDIRIM", "324"),
            programEntry(ScheduleDay.TUESDAY, "10:30", "12:20", "", "Türk Kültür Coğrafyası", "Prof. Dr. Ahmet KÖÇ", "147-B", "Seçmeli", note = "IV. Grup · Genel Kültür"),
            programEntry(ScheduleDay.TUESDAY, "10:30", "12:20", "", "Türk Musikisi", "Öğr. Gör. Dr. Serkan ERTÜRK", "147-A", "Seçmeli", note = "IV. Grup · Genel Kültür"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "", "Okullarda Rehberlik", "Prof. Dr. Demet EROL", "147-B"),
            programEntry(ScheduleDay.TUESDAY, "15:30", "17:20", "", "Aile Eğitimi ve Katılımı", "Doç. Dr. Yakup YILDIRIM", "147-B"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "", "Öğretmenlik Uygulaması I", "Prof. Dr. Zeliha YAZICI / Prof. Dr. Nihat BAYAT", "Uygulama"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "", "Türk Sanatı Tarihi", "Öğr. Gör. Dr. Serkan ERTÜRK", "321", "Seçmeli", note = "IV. Grup · Genel Kültür"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "", "Toplumsal Cinsiyet Eğitimi", "Prof. Dr. Zeliha YAZICI", "324", "Seçmeli", note = "IV. Grup · Alan Eğitimi"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "", "Erken Çocukluk Dönemi Çevre Eğitimi", "Doç. Dr. Emine Ela ŞİMŞEK", "321", "Seçmeli", note = "IV. Grup · Alan Eğitimi"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "17:20", "", "Öğretmenlik Uygulaması I", "Doç. Dr. Yunus PINAR / Doç. Dr. Hale KOÇER / Doç. Dr. Emine Ela ŞİMŞEK / Doç. Dr. Yakup YILDIRIM / Öğr. Gör. Dr. Serkan ERTÜRK", "Uygulama"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:20", "", "Kaynaştırma Eğitimi", "Doç. Dr. Begümhan YÜKSEL", "322"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "", "Okul Öncesi Eğitim Kurumlarında Yönetim ve Liderlik", "Doç. Dr. Ramazan GÖK", "324", "Seçmeli", note = "IV. Grup · Alan Eğitimi"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "", "Öğretmenlik Uygulaması I", "Doç. Dr. Begümhan YÜKSEL", "Uygulama"),
            programEntry(ScheduleDay.THURSDAY, "15:30", "17:20", "", "Okula Uyum ve Erken Okuryazarlık Eğitimi", "Doç. Dr. Begümhan YÜKSEL", "324")
        )
    )

    val specialEducationFirstYear = educationSchedule(
        department = SPECIAL_EDUCATION_DEPARTMENT,
        classYear = FIRST_YEAR,
        sourcePage = 1,
        sourceUrl = SPECIAL_EDUCATION_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "08:30", "10:20", "AZÖE 103", "Zihin Yetersizliği ve Otizm Spektrum Bozukluğu", "Doç. Dr. Adile Emel SARDOHAN YILDIRIM", "326"),
            programEntry(ScheduleDay.MONDAY, "13:30", "14:20", "KPD 101", "Kariyer Planlama", "Doç. Dr. Adile Emel SARDOHAN YILDIRIM", "326"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "ENF 105", "Bilişim Teknolojileri", "Öğr. Gör. Evren SEZGİN", "Sanal 01"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "MBZ 101", "Eğitime Giriş", "Prof. Dr. Kemal KAYIKÇI", "326"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "MBZ 103", "Eğitim Psikolojisi", "Dr. Öğr. Üyesi Şirin KÜÇÜKAVCI", "326"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "17:20", "AZÖE 101", "Özel Eğitim", "Öğr. Gör. Dr. Abdullah ÇİFTÇİ", "326"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "TBD 105", "Türk Dili I", "Öğr. Gör. Dr. Emre KAYASANDIK", "129"),
            programEntry(ScheduleDay.FRIDAY, "10:30", "11:20", "YBD 109", "İngilizce I", "Öğr. Gör. Hülya ÇELİK", "147-B"),
            programEntry(ScheduleDay.FRIDAY, "11:30", "12:20", "YBD 109", "İngilizce I", "Öğr. Gör. Esra ÇELİK", "147-B"),
            programEntry(ScheduleDay.FRIDAY, "13:30", "15:20", "ATA 105", "Atatürk İlkeleri ve İnkılap Tarihi", "Öğr. Gör. Derya ÖGE SET", "134")
        )
    )

    val specialEducationSecondYear = educationSchedule(
        department = SPECIAL_EDUCATION_DEPARTMENT,
        classYear = SECOND_YEAR,
        sourcePage = 2,
        sourceUrl = SPECIAL_EDUCATION_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "ASÖE 201", "Çoklu Yetersizlik ve Eğitimleri", "Doç. Dr. Adile Emel SARDOHAN YILDIRIM", "326"),
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "EMÖE 105", "Special and Inclusive Education", "Öğr. Gör. Dr. Abdullah ÇİFTÇİ", "229"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "AZÖE 201", "Erken Çocuklukta Özel Eğitim", "Dr. Öğr. Üyesi Ömür GÜREL", "328"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "EMÖE 106", "Language and Behavioural Interventions in Special Education", "Öğr. Gör. Dr. Abdullah ÇİFTÇİ", "229"),
            programEntry(ScheduleDay.TUESDAY, "10:30", "12:20", "MBZ 203", "Türk Eğitim Tarihi", "Prof. Dr. Selçuk UYGUN", "326"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "AZÖE 203", "Uygulamalı Davranış Analizi", "Doç. Dr. Nesrin SÖNMEZ", "326"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "MBZ 207", "Eğitimde Araştırma Yöntemleri", "Arş. Gör. Dr. Ertunç UŞKUL", "328"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "ENF 213", "Bilişim Teknolojileri Bağımlılığı", "Öğr. Gör. Dr. T. Fatih KASALAK", "Sanal 01"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:20", "AZÖE 209", "Türk İşaret Dili", "Özgül KEPÇE", "326"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "AZÖE 205", "Özel Eğitimde Değerlendirme", "Doç. Dr. Nesrin SÖNMEZ", "326"),
            programEntry(ScheduleDay.FRIDAY, "10:30", "12:20", "ASÖE 207", "OSB'de Temel Becerilerin Kazandırılması", "Dr. Öğr. Üyesi Ömür GÜREL", "326"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "17:20", "AZÖE 207", "Özel Eğitimde Bütünleştirme", "Arş. Gör. Dr. Yavuz Erhan KANPOLAT", "328")
        )
    )

    val specialEducationThirdYear = educationSchedule(
        department = SPECIAL_EDUCATION_DEPARTMENT,
        classYear = THIRD_YEAR,
        sourcePage = 3,
        sourceUrl = SPECIAL_EDUCATION_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "AZÖE 301", "Özel Eğitimde Okuma-Yazma Öğretimi", "Dr. Öğr. Üyesi Ömür GÜREL", "328"),
            programEntry(ScheduleDay.TUESDAY, "10:30", "12:20", "MBZ 301", "Türk Eğitim Sistemi ve Okul Yönetimi", "Doç. Dr. Gülnar ÖZYILDIRIM", "328"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "MBZ 303", "Eğitimde Ölçme ve Değerlendirme", "Prof. Dr. Hakan KOĞAR", "229"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "AZÖE 309", "Özel Eğitimde Fiziksel Eğitim ve Spor", "Prof. Dr. Cenk TEMEL", "328"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "AZÖE 305", "Özel Eğitimde Matematik Öğretimi", "Doç. Dr. Nesrin SÖNMEZ", "328"),
            programEntry(ScheduleDay.THURSDAY, "15:30", "17:20", "ASÖE 301", "Öğrenme Güçlüğü İçin Öğrenme Stratejileri", "Öğr. Gör. Dr. Abdullah ÇİFTÇİ", "328"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "10:20", "ASÖE 307", "Görme Yetersizliğinde Erken Çocukluk Dönemi Eğitimi", "Dr. Öğr. Üyesi Ömür GÜREL", "326"),
            programEntry(ScheduleDay.FRIDAY, "10:30", "12:20", "AZÖE 307", "Özel Eğitimde Sanatsal Becerilerin Öğretimi", "Arş. Gör. Dr. Yavuz Erhan KANPOLAT", "328"),
            programEntry(ScheduleDay.FRIDAY, "13:30", "16:20", "AZÖE 303", "Özel Eğitimde Fen ve Sosyal Bilgiler Öğretimi", "Dr. Öğr. Üyesi Ömür GÜREL", "326")
        )
    )

    val specialEducationFourthYear = educationSchedule(
        department = SPECIAL_EDUCATION_DEPARTMENT,
        classYear = FOURTH_YEAR,
        sourcePage = 4,
        sourceUrl = SPECIAL_EDUCATION_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "10:20", "MSÖE 407", "Mikro Öğretim", "Doç. Dr. Nesrin SÖNMEZ", "328"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "10:20", "AZÖE 401", "Özel Eğitimde Materyal Tasarımı", "Doç. Dr. Adile Emel SARDOHAN YILDIRIM", "326"),
            programEntry(ScheduleDay.TUESDAY, "12:30", "13:20", "AZÖE 401", "Özel Eğitimde Materyal Tasarımı", "Doç. Dr. Adile Emel SARDOHAN YILDIRIM", "326"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "ASÖE 407", "Zihin Yetersizliğinde Öğretim Uyarlamaları", "Öğr. Gör. Dr. Abdullah ÇİFTÇİ", "328"),
            programEntry(ScheduleDay.TUESDAY, "15:30", "17:20", "AÖE 405", "İş ve Meslek Becerilerinin Öğretimi", "Arş. Gör. Dr. Yavuz Erhan KANPOLAT", "328"),
            programEntry(ScheduleDay.WEDNESDAY, "12:30", "13:20", "MBZ 401", "Öğretmenlik Uygulaması I", "Prof. Dr. Hilmi DEMİRKAYA (326) / Prof. Dr. Cenk TEMEL (328) / Öğr. Gör. Dr. Abdullah ÇİFTÇİ (229) / Doç. Dr. Nesrin SÖNMEZ (235) / Doç. Dr. Adile Emel SARDOHAN YILDIRIM (236) / Dr. Öğr. Üyesi Ömür GÜREL (234)", "Çeşitli sınıflar"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "MSÖE 401-1", "Eğitimde Drama", "Öğr. Gör. Tülin TÜMTÜRK", "Z-27"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "17:20", "MSÖE 401-2", "Eğitimde Drama", "Öğr. Gör. Tülin TÜMTÜRK", "Z-27"),
            programEntry(ScheduleDay.THURSDAY, "08:30", "09:20", "MBZ 401", "Öğretmenlik Uygulaması I", "Arş. Gör. Dr. Yavuz Erhan KANPOLAT", "229"),
            programEntry(ScheduleDay.THURSDAY, "12:30", "13:20", "MBZ 401", "Öğretmenlik Uygulaması I", "Prof. Dr. Hilmi DEMİRKAYA (326) / Prof. Dr. Cenk TEMEL (328) / Öğr. Gör. Dr. Abdullah ÇİFTÇİ (229) / Doç. Dr. Nesrin SÖNMEZ (235) / Doç. Dr. Adile Emel SARDOHAN YILDIRIM (236) / Dr. Öğr. Üyesi Ömür GÜREL (234)", "Çeşitli sınıflar"),
            programEntry(ScheduleDay.FRIDAY, "16:30", "17:20", "MBZ 401", "Öğretmenlik Uygulaması I", "Arş. Gör. Dr. Yavuz Erhan KANPOLAT", "328")
        )
    )

    val counselingFirstYear = educationSchedule(
        department = COUNSELING_DEPARTMENT,
        classYear = FIRST_YEAR,
        sourcePage = 1,
        sourceUrl = COUNSELING_SOURCE_URL,
        updatedAt = "22.09.2026",
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "ENF 105", "Bilişim Teknolojileri", "Öğr. Gör. Dr. Fatih KAVALAK", "Sanal sınıf"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "PDR 107", "Psikolojiye Giriş", "Öğr. Gör. Fazilet BARÇIN KARA", "133"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "PDR 103", "Sosyolojiye Giriş", "Prof. Dr. Mimar TÜRKKAHRAMAN", "131"),
            programEntry(ScheduleDay.TUESDAY, "15:30", "17:20", "AZPD 103", "Kültürel Antropoloji", "Öğr. Gör. Dr. Seval APAYDIN", "Ofis", note = "Eşleşmeyen"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "PDR 105", "Felsefeye Giriş", "Öğr. Gör. Serpil SATI", "131"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:20", "PDR 101", "Eğitime Giriş", "Prof. Dr. Erdoğan KÖSE", "131"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "TDB 105", "Türk Dili I", "Öğr. Gör. Dr. Emre KAYASANDIK", "A129"),
            programEntry(ScheduleDay.FRIDAY, "10:30", "12:20", "YBD 109", "Yabancı Dil I", "Öğr. Gör. Hülya ÇELİK", "147-B"),
            programEntry(ScheduleDay.FRIDAY, "13:30", "15:20", "ATA 105", "Atatürk İlkeleri ve İnkılap Tarihi I", "Öğr. Gör. Derya ÖGE SET", "A134", note = "A ve B şubeleri")
        )
    )

    val counselingSecondYear = educationSchedule(
        department = COUNSELING_DEPARTMENT,
        classYear = SECOND_YEAR,
        sourcePage = 2,
        sourceUrl = COUNSELING_SOURCE_URL,
        updatedAt = "22.09.2026",
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "10:30", "13:20", "PDR 203", "Öğretim İlke ve Yöntemleri", "Prof. Dr. Günseli ORHON", "131"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "PDRA 201", "Pozitif Psikoloji", "Prof. Dr. Tuğba SARI", "129"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "PDR 201", "Eğitimde Ölçme ve Değerlendirme", "Prof. Dr. Güçlü ŞEKERCİOĞLU", "132"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "PDRA 203", "Çocuk İstismarı ve İhmali", "Doç. Dr. Evrim ÇETİNKAYA YILDIZ", "130"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "PDR 211", "Sosyal Psikoloji", "Doç. Dr. Aydın ÇİVİLİDAĞ", "133"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "16:20", "PDR 205", "İstatistik Uygulamaları", "Doç. Dr. Bilal Barış ALKAN", "133"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "PDR 207", "Öğrenme Psikolojisi", "Öğr. Gör. Dr. Seval APAYDIN", "133"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "PDR 209", "Gelişim Psikolojisi II", "Prof. Dr. Demet EROL", "132")
        )
    )

    val counselingThirdYear = educationSchedule(
        department = COUNSELING_DEPARTMENT,
        classYear = THIRD_YEAR,
        sourcePage = 3,
        sourceUrl = COUNSELING_SOURCE_URL,
        updatedAt = "22.09.2026",
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "PDR 305", "Psikolojik Danışma İlke ve Teknikleri", "Doç. Dr. Arzu TAŞDELEN KARÇKAY", "133"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "PDRM 301", "Mesleki İngilizce", "Prof. Dr. Demet EROL", "130"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "PDR 307", "Psikolojik Danışma Kuramları", "Doç. Dr. Melike KOÇYİĞİT ÖZYİĞİT", "134"),
            programEntry(ScheduleDay.TUESDAY, "12:30", "15:20", "PDR 309", "Kariyer Danışmanlığı", "Öğr. Gör. Dr. Seval APAYDIN", "134"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "PDRM 303", "Eğitimde Proje Hazırlama", "Prof. Dr. Memduh Sami TAMER", "129"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "16:20", "PDR 311", "Psikolojik Testler", "Dr. Merve AYVALLI KARAGÖZ", "134"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:20", "PDR 301", "Eğitim Yönetimi", "Doç. Dr. Ramazan GÖK", "132"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "PDR 303", "Davranış Bozuklukları I", "Prof. Dr. S. Gülfem ÇAKIR ÇELEBİ", "133"),
            programEntry(ScheduleDay.THURSDAY, "16:30", "18:20", "ASPD 305", "Okulda Ruh Sağlığı", "Öğr. Gör. Dr. Seval APAYDIN", "Ofis", note = "Eşleşmeyen")
        )
    )

    val counselingFourthYear = educationSchedule(
        department = COUNSELING_DEPARTMENT,
        classYear = FOURTH_YEAR,
        sourcePage = 5,
        sourceUrl = COUNSELING_SOURCE_URL,
        updatedAt = "22.09.2026",
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "08:30", "12:20", "PDR 405", "Bireyle Psikolojik Danışma Uygulamaları (BPDU)", "Doç. Dr. Melike KOÇYİĞİT ÖZYİĞİT", "Grup odası, A Blok"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "PDRA 403", "Yakın İlişkiler Psikolojisi", "Doç. Dr. Melike KOÇYİĞİT ÖZYİĞİT", "131"),
            programEntry(ScheduleDay.MONDAY, "15:30", "17:20", "PDRA 405", "Psikoeğitim Grupları", "Prof. Dr. Tuğba SARI", "131"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "12:20", "PDR 405", "Bireyle Psikolojik Danışma Uygulamaları (BPDU)", "Doç. Dr. Evrim ÇETİNKAYA YILDIZ / Öğr. Gör. Dr. Seval APAYDIN / Doç. Dr. Arzu TAŞDELEN KARÇKAY / Prof. Dr. Tuğba SARI", "PDR grup odaları (A/B Blok), 115, 215"),
            programEntry(ScheduleDay.TUESDAY, "12:30", "13:20", "PDR 405", "Bireyle Psikolojik Danışma Uygulamaları (BPDU)", "Prof. Dr. Tuğba SARI", "215"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "PDR 403", "Kısa Süreli Çözüm Odaklı Psikolojik Danışma", "Prof. Dr. S. Gülfem ÇAKIR ÇELEBİ", "129"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "PDRM 403", "Krize Müdahale", "Öğr. Gör. Dr. Seval APAYDIN", "130"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "PDRG 403", "Bilim Tarihi (1)", "Prof. Dr. Memduh Sami TANER", "129"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "17:20", "PDRG 403", "Bilim Tarihi (2)", "Prof. Dr. Memduh Sami TANER", "129"),
            programEntry(ScheduleDay.THURSDAY, "08:30", "12:20", "PDR 405", "Bireyle Psikolojik Danışma Uygulamaları (BPDU)", "Prof. Dr. S. Gülfem ÇAKIR ÇELEBİ", "PDR grup görüşme odası, A Blok"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "PDR 403", "Kısa Süreli Çözüm Odaklı Psikolojik Danışma", "Doç. Dr. Arzu TAŞDELEN KARÇKAY", "129"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "PDR 407", "Meslek Etiği ve Yasal Konular", "Doç. Dr. Evrim ÇETİNKAYA YILDIZ", "131"),
            programEntry(ScheduleDay.THURSDAY, "15:30", "17:20", "PDRM 407", "Anne-Baba Eğitimi", "Öğr. Gör. Fazilet BARÇIN KARA", "131"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "10:20", "PDR 401", "Okullarda RPD Uygulamaları I", "Prof. Dr. Demet EROL / Doç. Dr. Evrim ÇETİNKAYA YILDIZ / Prof. Dr. Tuğba SARI / Prof. Dr. Gülfem ÇAKIR ÇELEBİ / Doç. Dr. Arzu TAŞDELEN KARÇKAY / Doç. Dr. Melike KOÇYİĞİT ÖZYİĞİT / Öğr. Gör. Dr. Seval APAYDIN / Öğr. Gör. Fazilet BARÇIN KARA", "Uygulama / Ofis")
        )
    )

    val classroomTeachingFirstYear = educationSchedule(
        department = CLASSROOM_TEACHING_DEPARTMENT,
        classYear = FIRST_YEAR,
        sourcePage = 1,
        sourceUrl = CLASSROOM_TEACHING_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "ENF 105", "Bilişim Teknolojileri", "Evren SEZGİN", "Sanal 01"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "SÖMB 101", "Eğitime Giriş", "Ayşenur K. CANBULAT", "B118"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "10:20", "SGK 109", "Türkiye Coğrafyası ve Jeopolitiği", "Prof. Dr. Hilmi DEMİRKAYA", "B119"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "SAE 105", "Sözlü Anlatım", "Yasin ÖZKARA", "B120"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "16:20", "SAE 103", "İlkokulda Temel Matematik", "Zeynep EKEN", "B117"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "13:20", "TDB 105", "Türk Dili I", "Emre KAYASANDIK", "B117"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "SGK 113", "Türk Tarihi ve Kültürü", "Doç. Dr. Nursel KÖKSAL", "B118"),
            programEntry(ScheduleDay.FRIDAY, "10:30", "12:20", "YBD 109", "Yabancı Dil I (İngilizce)", "Hülya ÇELİK", "B118"),
            programEntry(ScheduleDay.FRIDAY, "13:30", "15:20", "ATA 105", "Atatürk İlkeleri ve İnkılap Tarihi I", "Koray ERGİN", "147-C")
        )
    )

    val classroomTeachingSecondYear = educationSchedule(
        department = CLASSROOM_TEACHING_DEPARTMENT,
        classYear = SECOND_YEAR,
        sourcePage = 1,
        sourceUrl = CLASSROOM_TEACHING_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "SSGK 219", "Bilim Tarihi ve Doğası", "Prof. Dr. Aziz ASLAN", "B119"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "SAE 209", "Çocuk Edebiyatı", "Tülin TÜMTÜRK YILMAZ", "Z-27"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "SÖM 207", "Türk Eğitim Tarihi", "Nursel KÖKSAL", "B120"),
            programEntry(ScheduleDay.TUESDAY, "15:30", "17:20", "SSGK 215", "Etkili İletişim", "Tülin TÜMTÜRK YILMAZ", "Z-27"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "10:20", "SZGK 213", "Kültür ve Sanat", "Sabahat BURAK", "B223"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "SÖM 205", "İlkokul Programı", "Demet SEBAN", "B122"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "SÖM 203", "Eğitim Teknolojisi", "Oğuzhan ATABEK", "B123"),
            programEntry(ScheduleDay.THURSDAY, "08:30", "09:20", "SAE 207", "Fen Bilimleri Laboratuvar Uygulamaları", "Evren CAPPELLARO", "B123", note = "Teorik"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "11:20", "SAE 207", "Fen Bilimleri Laboratuvar Uygulamaları", "Evren CAPPELLARO", "B123", note = "Uygulama"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "SÖM 201", "Öğretim İlke ve Yöntemleri", "Ayşe Nur K. CANBULAT", "B120")
        )
    )

    val classroomTeachingThirdYear = educationSchedule(
        department = CLASSROOM_TEACHING_DEPARTMENT,
        classYear = THIRD_YEAR,
        sourcePage = 1,
        sourceUrl = CLASSROOM_TEACHING_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "MBZ 305", "Eğitimde Ahlak ve Etik", "Yasin ÖZKARA", "Öğretim üyesi ofisi"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "SÖM 319", "Toplumsal Duyarlılık ve Katkı Projeleri I", "Demet SEBAN / Aziz ASLAN / Yasin ÖZKARA / Nursel KÖKSAL / Yusuf AYDIN", "Öğretim üyelerinin ofisleri"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "10:20", "SAE 317", "Matematik Öğretimi I", "Ramazan KARATAŞ", "B119", note = "Teorik", section = "2. Şube"),
            programEntry(ScheduleDay.TUESDAY, "10:30", "12:20", "SAE 317", "Matematik Öğretimi I", "Ramazan KARATAŞ", "B119", note = "Uygulama", section = "2. Şube"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "17:20", "SAE 317", "Matematik Öğretimi I", "Ramazan KARATAŞ", "B119", note = "Teorik", section = "1. Şube"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "11:20", "SAE 313", "İlkokulda Yabancı Dil Öğretimi", "Seda AKSUNGUR", "B123", note = "Teorik"),
            programEntry(ScheduleDay.WEDNESDAY, "11:30", "12:20", "SAE 313", "İlkokulda Yabancı Dil Öğretimi", "Seda AKSUNGUR", "B123", note = "Uygulama"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "SAE 311", "İlk Okuma ve Yazma Öğretimi", "Yasin ÖZKARA", "B122", note = "Teorik"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "17:20", "SAE 311", "İlk Okuma ve Yazma Öğretimi", "Yasin ÖZKARA", "B122", note = "Uygulama"),
            programEntry(ScheduleDay.THURSDAY, "08:30", "12:20", "SAE 315", "Hayat Bilgisi Öğretimi", "Nursel KÖKSAL", "B121"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "SSÖM 303", "Okul Dışı Öğrenme Ortamları", "Tülin TÜMTÜRK YILMAZ", "Z-27"),
            programEntry(ScheduleDay.THURSDAY, "15:30", "17:20", "SSÖM 307", "Eğitimde Drama ve Tiyatro Uygulamaları", "Tülin TÜMTÜRK YILMAZ", "Z-27"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "10:20", "SAE 309", "Fen Bilimleri Öğretimi", "Evren CAPPELLARO", "B123", note = "Teorik"),
            programEntry(ScheduleDay.FRIDAY, "10:30", "12:20", "SAE 309", "Fen Bilimleri Öğretimi", "Evren CAPPELLARO", "B123", note = "Uygulama")
        )
    )

    val classroomTeachingFourthYear = educationSchedule(
        department = CLASSROOM_TEACHING_DEPARTMENT,
        classYear = FOURTH_YEAR,
        sourcePage = 1,
        sourceUrl = CLASSROOM_TEACHING_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "08:30", "10:20", "SÖM 403", "Öğretmenlik Uygulaması I", "Aziz ASLAN / Ayşenur K. CANBULAT / Nursel KÖKSAL / Demet SEBAN / Yasin ÖZKARA / Evren CAPPELLARO / Yusuf AYDIN", "Öğretim üyelerinin ofisleri", note = "Teorik"),
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "SSAE 415", "Sınıf İçi Öğrenmelerin Değerlendirilmesi", "Ayşe Nur K. CANBULAT", "B120", section = "1. Şube"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "SÖM 401", "Rehberlik", "Fazilet Berçin KARA", "B121"),
            programEntry(ScheduleDay.MONDAY, "15:30", "17:20", "SSAE 415", "Sınıf İçi Öğrenmelerin Değerlendirilmesi", "Ayşe Nur K. CANBULAT", "B120", section = "2. Şube"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "12:20", "SÖM 403", "Öğretmenlik Uygulaması I", "İlgili öğretim elemanı", "Uygulama okulu", note = "Uygulama"),
            programEntry(ScheduleDay.TUESDAY, "14:30", "17:20", "SAE 411", "Müzik Öğretimi", "Sabahat BURAK", "Müzik Dersliği, B Blok 3. Kat", section = "1. Şube"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "12:20", "SÖM 403", "Öğretmenlik Uygulaması I", "İlgili öğretim elemanı", "Uygulama okulu", note = "Uygulama"),
            programEntry(ScheduleDay.WEDNESDAY, "14:30", "17:20", "SAE 411", "Müzik Öğretimi", "Sabahat BURAK", "Müzik Dersliği, B Blok 3. Kat", section = "2. Şube"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "SAE 413", "Görsel Sanatlar Öğretimi", "Ezgi BİLGİN", "B122"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "SÖM 405", "Özel Eğitim", "Abdullah ÇİFTÇİ", "B123"),
            programEntry(ScheduleDay.THURSDAY, "15:30", "17:20", "SSÖM 409", "Karşılaştırmalı Eğitim", "Demet SEBAN", "B118")
        )
    )

    val socialStudiesTeachingFirstYear = educationSchedule(
        department = SOCIAL_STUDIES_TEACHING_DEPARTMENT,
        classYear = FIRST_YEAR,
        sourcePage = 1,
        sourceUrl = SOCIAL_STUDIES_TEACHING_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "", "Bilişim Teknolojileri", "Öğr. Gör. Dr. Evren SEZGİN", "Sanal S"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "", "Eğitim Psikolojisi", "Dr. Öğr. Üyesi Şirin KÜÇÜK AVCI", "333"),
            programEntry(ScheduleDay.WEDNESDAY, "11:30", "13:20", "", "Eğitime Giriş", "Doç. Dr. Ali SABANCI", "329"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "16:20", "", "Türk Dili I", "Öğr. Gör. Dr. Emre KAYASANDIK", "331"),
            programEntry(ScheduleDay.THURSDAY, "08:30", "10:20", "", "Genel Fiziki Coğrafya", "Prof. Dr. Hilmi DEMİRKAYA", "332"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:20", "", "Sosyal Bilgilerin Temelleri", "Prof. Dr. Nadire Emel AKHAN", "331"),
            programEntry(ScheduleDay.THURSDAY, "12:30", "13:20", "", "Kariyer Planlama", "Doç. Dr. Serpil DEMİREZEN", "331"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "10:20", "", "Atatürk İlkeleri ve İnkılap Tarihi I", "Öğr. Gör. Derya ÖGE SET", "147-A"),
            programEntry(ScheduleDay.FRIDAY, "10:30", "12:20", "", "Yabancı Dil I", "Öğr. Gör. Esra DÖNÜŞ", "147-A")
        )
    )

    val socialStudiesTeachingSecondYear = educationSchedule(
        department = SOCIAL_STUDIES_TEACHING_DEPARTMENT,
        classYear = SECOND_YEAR,
        sourcePage = 1,
        sourceUrl = SOCIAL_STUDIES_TEACHING_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "08:30", "10:20", "", "Orta Çağ Tarihi", "Prof. Dr. Ahmet KÖÇ", "329"),
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "", "Türk Eğitim Tarihi", "Doç. Dr. Osman AKHAN", "331"),
            programEntry(ScheduleDay.MONDAY, "15:30", "17:20", "", "Çocuk Psikolojisi", "Arş. Gör. Dr. Tuğçe ÇAL", "332", section = "A Şubesi"),
            programEntry(ScheduleDay.MONDAY, "15:30", "17:20", "", "Açık ve Uzaktan Öğrenme", "Arş. Gör. Dr. Saim TURAN", "329", section = "B Şubesi"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "10:20", "", "Türkiye'nin Fiziki Coğrafyası", "Prof. Dr. Hilmi DEMİRKAYA", "333"),
            programEntry(ScheduleDay.TUESDAY, "10:30", "12:20", "", "Öğretim İlke ve Yöntemleri", "Doç. Dr. Osman AKHAN", "331"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "", "Sosyal Bilgiler Öğretiminde Drama", "Prof. Dr. Nadire Emel AKHAN", "Z-28"),
            programEntry(ScheduleDay.TUESDAY, "15:30", "17:20", "", "Bilim Tarihi ve Felsefesi", "Arş. Gör. Dr. Tuğçe ÇAL", "332"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "10:20", "", "İlk Türk-İslam Devletleri Tarihi", "Prof. Dr. Ahmet KÖÇ", "331"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "", "Görsel ve Yazılı Kaynakların Kullanımı", "Prof. Dr. Yüksel KAŞTAN", "333"),
            programEntry(ScheduleDay.THURSDAY, "08:30", "10:20", "", "Sosyal Bilgiler Öğrenme ve Öğretim Yaklaşımları", "Doç. Dr. Serpil DEMİREZEN", "331"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:20", "", "Bilim ve Araştırma Etiği", "Prof. Dr. Yüksel KAŞTAN", "329")
        )
    )

    val socialStudiesTeachingThirdYear = educationSchedule(
        department = SOCIAL_STUDIES_TEACHING_DEPARTMENT,
        classYear = THIRD_YEAR,
        sourcePage = 1,
        sourceUrl = SOCIAL_STUDIES_TEACHING_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "", "Sanat ve Estetik", "Arş. Gör. Dr. Tuğçe ÇAL", "333"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "10:20", "", "Yeni ve Yakın Çağ Tarihi", "Prof. Dr. Ahmet KÖÇ", "329"),
            programEntry(ScheduleDay.TUESDAY, "10:30", "12:20", "", "Sosyal Bilgiler Dersinde Etkinlik Geliştirme", "Prof. Dr. Nadire Emel AKHAN", "332", section = "A Şubesi"),
            programEntry(ScheduleDay.TUESDAY, "10:30", "12:20", "", "Sosyal Bilgiler Öğretiminde Kavram ve Beceri Geliştirme", "Doç. Dr. Serpil DEMİREZEN", "333", section = "B Şubesi"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "", "Günümüz Türk Dünyası", "Doç. Dr. Osman AKHAN", "329"),
            programEntry(ScheduleDay.TUESDAY, "15:30", "17:20", "", "Eğitimde Ölçme ve Değerlendirme", "Arş. Gör. Dr. Ertunç UŞKUL", "333"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "", "Eğitimde Proje Hazırlama", "Prof. Dr. Nadire Emel AKHAN", "331", section = "A Şubesi"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "", "Eğitimde Program Dışı Etkinlikler", "Doç. Dr. Serpil DEMİREZEN", "330", section = "B Şubesi"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "", "Türk Eğitim Sistemi ve Okul Yönetimi", "Doç. Dr. Ali SABANCI", "332"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "17:20", "", "Sosyal Bilgiler Öğretiminde Tarihsel Kanıt, Yerel ve Sözlü Tarih", "Prof. Dr. Yüksel KAŞTAN", "333"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:20", "", "Osmanlı Tarihi I", "Doç. Dr. Meltem Begüm SAATÇI ATA", "333"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "", "Sosyal Bilgiler Öğretimi I", "Doç. Dr. Serpil DEMİREZEN", "330")
        )
    )

    val socialStudiesTeachingFourthYear = educationSchedule(
        department = SOCIAL_STUDIES_TEACHING_DEPARTMENT,
        classYear = FOURTH_YEAR,
        sourcePage = 1,
        sourceUrl = SOCIAL_STUDIES_TEACHING_SOURCE_URL,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "", "Okullarda Rehberlik", "Öğr. Gör. Dr. Seval APAYDIN", "332"),
            programEntry(ScheduleDay.MONDAY, "12:30", "13:20", "", "Öğretmenlik Uygulaması I", SOCIAL_STUDIES_PRACTICUM_INSTRUCTORS, "Uygulama / Ofis"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "", "Okul Dışı Öğrenme Ortamları", "Doç. Dr. Osman AKHAN", "329"),
            programEntry(ScheduleDay.TUESDAY, "12:30", "13:20", "", "Öğretmenlik Uygulaması I", SOCIAL_STUDIES_PRACTICUM_INSTRUCTORS, "Uygulama / Ofis"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "", "Afetler ve Afet Yönetimi", "Prof. Dr. Ayhan AKIŞ", "331"),
            programEntry(ScheduleDay.TUESDAY, "15:30", "17:20", "", "Mikro Öğretim", "Doç. Dr. Serpil DEMİREZEN", "331", section = "A Şubesi"),
            programEntry(ScheduleDay.TUESDAY, "15:30", "17:20", "", "Karşılaştırmalı Eğitim", "Arş. Gör. Dr. Saim TURAN", "329", section = "B Şubesi"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "", "Sanat ve Müze Eğitimi", "Prof. Dr. Nadire Emel AKHAN", "329"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "17:20", "", "Medya Okuryazarlığı ve Eğitimi", "Arş. Gör. Dr. Saim TURAN", "332"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:20", "", "Sınıf İçi Öğrenmelerin Değerlendirilmesi", "Prof. Dr. Hilmi DEMİRKAYA", "332"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "", "Türkiye Cumhuriyeti Tarihi I", "Prof. Dr. Yüksel KAŞTAN", "329"),
            programEntry(ScheduleDay.THURSDAY, "15:30", "17:20", "", "Küreselleşme ve Toplum", "Prof. Dr. Yüksel KAŞTAN", "329")
        )
    )

    val turkishTeachingFirstYear = educationSchedule(
        department = TURKISH_TEACHING_DEPARTMENT,
        classYear = FIRST_YEAR,
        sourcePage = 1,
        sourceUrl = TURKISH_TEACHING_SOURCE_URL,
        updatedAt = "25.09.2026",
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "10:30", "11:20", "", "Osmanlı Türkçesi", "Mevlüt GÜLMEZ", "217", section = "A Şubesi"),
            programEntry(ScheduleDay.MONDAY, "11:30", "12:20", "", "Osmanlı Türkçesi", "Mevlüt GÜLMEZ", "217", section = "B Şubesi"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "", "Bilişim Teknolojileri", "T. Fatih KASALAK", "Sanal Sınıf"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "", "Türk Dili I", "Bekir DİREKCİ", "224", section = "A Şubesi"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "", "Türk Dili I", "Bekir DİREKCİ", "224", section = "B Şubesi"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "10:20", "", "Edebiyat ve Bilgi Kuramları", "Emel ÇOPUR", "217"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "", "Türk Dil Bilgisi I", "Mevlüt GÜLMEZ", "125", section = "A Şubesi"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "", "Türk Dil Bilgisi I", "Yusuf TEPELİ", "124", section = "B Şubesi"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "", "Osmanlı Türkçesi", "Emel ÇOPUR", "124", section = "B Şubesi"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "", "Eğitime Giriş", "Erdoğan KÖSE", "117"),
            programEntry(ScheduleDay.THURSDAY, "15:30", "17:20", "", "Eğitim Felsefesi", "Mehmet DUGAN", "117"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "10:20", "", "Atatürk İlkeleri ve İnkılap Tarihi I", "Koray ERGİN", "147-B"),
            programEntry(ScheduleDay.FRIDAY, "10:30", "12:20", "", "Yabancı Dil I (İngilizce)", "Güher CEYLAN KUŞOĞLU", "147-C")
        )
    )

    val turkishTeachingSecondYear = educationSchedule(
        department = TURKISH_TEACHING_DEPARTMENT,
        classYear = SECOND_YEAR,
        sourcePage = 1,
        sourceUrl = TURKISH_TEACHING_SOURCE_URL,
        updatedAt = "25.09.2026",
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "", "Öğretim Teknolojileri", "Burak ASMA", "223"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "", "Karakter ve Değerler Eğitimi", "İmran Nazike AVCI", "117"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "10:20", "", "Türk Halk Edebiyatı I", "Adile YILMAZ", "117", section = "A Şubesi"),
            programEntry(ScheduleDay.TUESDAY, "10:30", "12:20", "", "Yeni Türk Edebiyatı I", "Tülin ARSEVEN", "117"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "", "Türk Dilbilgisi 3", "Yusuf TEPELİ", "124", section = "A Şubesi"),
            programEntry(ScheduleDay.TUESDAY, "15:30", "17:20", "", "Türk Dilbilgisi 3", "Yusuf TEPELİ", "124", section = "B Şubesi"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "10:20", "", "Türk Halk Edebiyatı I", "Adile YILMAZ", "117", section = "B Şubesi"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "", "Türkçe Öğrenme ve Öğretim Yaklaşımları", "Ümit YILDIZ", "325", section = "A Şubesi"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "", "Türkçe Öğrenme ve Öğretim Yaklaşımları", "Betül KOPARAN", "117", section = "B Şubesi"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "", "Bilişim Teknolojileri Bağımlılığı", "T. Fatih KASALAK", "Sanal Sınıf"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "17:20", "", "Eski Türk Edebiyatı I", "Emel ÇOPUR", "124"),
            programEntry(ScheduleDay.THURSDAY, "08:30", "10:20", "", "Eğitim Hukuku", "Pınar ŞİMŞEK", "223"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:20", "", "Çocuk Edebiyatı", "Pınar ŞİMŞEK", "223"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "", "Öğretim İlke ve Yöntemleri", "İpek ÖNAL", "223"),
            programEntry(ScheduleDay.THURSDAY, "15:30", "17:20", "", "Bilim Tarihi ve Felsefesi", "Fatih YILDIZ", "223")
        )
    )

    val turkishTeachingThirdYear = educationSchedule(
        department = TURKISH_TEACHING_DEPARTMENT,
        classYear = THIRD_YEAR,
        sourcePage = 1,
        sourceUrl = TURKISH_TEACHING_SOURCE_URL,
        updatedAt = "25.09.2026",
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "08:30", "09:20", "", "Toplumsal Duyarlılık ve Katkı Projeleri", "Burak ASMA", "İlgili öğretim üyesi ofisi"),
            programEntry(ScheduleDay.MONDAY, "09:30", "10:20", "", "Toplumsal Duyarlılık ve Katkı Projeleri", "Adile YILMAZ / Burak ASMA / Mehmet CANBULAT / Ümit YILDIZ", "İlgili öğretim üyesi ofisi"),
            programEntry(ScheduleDay.MONDAY, "10:30", "11:20", "", "Toplumsal Duyarlılık ve Katkı Projeleri", "Adile YILMAZ / Mehmet CANBULAT / Ümit YILDIZ", "İlgili öğretim üyesi ofisi"),
            programEntry(ScheduleDay.MONDAY, "11:30", "13:20", "", "Sınıf Yönetimi", "İlhan GÜNBAYI", "117"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "", "Dinleme Eğitimi", "Berker KURT", "Dinleme Dersliği", section = "A Şubesi"),
            programEntry(ScheduleDay.MONDAY, "13:30", "16:20", "", "Dinleme Eğitimi", "Ümit YILDIZ", "325", section = "B Şubesi"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "09:20", "", "Toplumsal Duyarlılık ve Katkı Projeleri", "Ahmet Zeki GÜVEN", "İlgili öğretim üyesi ofisi"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "", "Okuma Eğitimi", "Ahmet Zeki GÜVEN", "217", section = "A Şubesi"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "", "Okuma Eğitimi", "Bilal ŞİMŞEK", "223", section = "B Şubesi"),
            programEntry(ScheduleDay.TUESDAY, "12:30", "13:20", "", "Toplumsal Duyarlılık ve Katkı Projeleri", "Ahmet Zeki GÜVEN", "İlgili öğretim üyesi ofisi"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "", "Öyküleyici Metin İncelemeleri", "Tülin ARSEVEN", "217"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "", "Eski Anadolu Türkçesi", "Mevlüt GÜLMEZ", "223"),
            programEntry(ScheduleDay.TUESDAY, "15:30", "17:20", "", "Balkanlarda Türkçe Öğretimi", "Ahmet Zeki GÜVEN", "217"),
            programEntry(ScheduleDay.TUESDAY, "15:30", "17:20", "", "Türk Halk Bilimi", "Adile YILMAZ", "223"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "10:20", "", "Eğitimde Proje Hazırlama", "Betül KOPARAN", "223"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "10:20", "", "Eleştirel ve Analitik Düşünme", "Ahmet Zeki GÜVEN", "224"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "", "Dilbilimi", "Mehmet CANBULAT", "217"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "", "Anlambilimi", "Yusuf TEPELİ", "223"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "", "Eleştirel Okuma", "İmran Nazike AVCI", "117"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "17:20", "", "Eğitimde Ahlak ve Etik", "Yusuf AYDIN", "223", section = "B Şubesi")
        )
    )

    val turkishTeachingFourthYear = educationSchedule(
        department = TURKISH_TEACHING_DEPARTMENT,
        classYear = FOURTH_YEAR,
        sourcePage = 1,
        sourceUrl = TURKISH_TEACHING_SOURCE_URL,
        updatedAt = "25.09.2026",
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "08:30", "10:20", "", "Türkçe Ders Kitabı İncelemeleri", "Berker KURT", "325"),
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "", "Dünya Edebiyatı Okumaları", "Berker KURT", "125"),
            programEntry(ScheduleDay.MONDAY, "11:30", "13:20", "", "Türkçe Öğretiminde Sınıf İçi Uygulama Örnekleri", "Ümit YILDIZ", "325"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "", "Mikro Öğretim", "Burak ASMA", "223"),
            programEntry(ScheduleDay.MONDAY, "15:30", "17:20", "", "Öğretmenlik Uygulaması I", "Ahmet Zeki GÜVEN", "İlgili öğretim üyesi ofisi"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "10:20", "", "Öğretmenlik Uygulaması I", "Berker KURT", "İlgili öğretim üyesi ofisi"),
            programEntry(ScheduleDay.TUESDAY, "10:30", "12:20", "", "Öğretmenlik Uygulaması I", "Betül KOPARAN / Pınar ŞİMŞEK / Ümit YILDIZ", "İlgili öğretim üyesi ofisi"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "", "Tiyatro ve Drama Uygulamaları", "Ahmet Zeki GÜVEN", "325"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "", "Tiyatro ve Drama Uygulamaları", "İmran Nazike AVCI", "B Blok 320"),
            programEntry(ScheduleDay.TUESDAY, "15:30", "17:20", "", "Özel Eğitim ve Kaynaştırma", "Abdullah ÇİFTÇİ", "117"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "10:20", "", "Türkçenin Yazım Sorunları", "Mevlüt GÜLMEZ", "222"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "10:20", "", "Eski Türkçe Metin İncelemeleri", "Yusuf TEPELİ", "124"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "10:20", "", "Yabancılara Türkçe Öğretimi için Hazırlanmış Ders Kitapları ve Materyallerin Değerlendirilmesi", "Ümit YILDIZ", "125"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "", "Dil Bilgisi Öğretimi", "Bekir DİREKCİ", "224", section = "A Şubesi"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "", "Dil Bilgisi Öğretimi", "Bekir DİREKCİ", "224", section = "B Şubesi"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "17:20", "", "İki Dilli Türk Çocuklarına Türkçe Öğretimi", "Betül KOPARAN", "217"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "17:20", "", "Türkçe Öğretiminde Çağdaş Teknoloji Kullanımı", "Bilal ŞİMŞEK", "224"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "", "Öğretmenlik Uygulaması I", "Mevlüt GÜLMEZ", "İlgili öğretim üyesi ofisi")
        )
    )

    val nursingFirstYear = nursingSchedule(
        classYear = FIRST_YEAR,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "08:30", "10:20", "", "Yabancı Dil I", "", "Amfi 8", section = "A Şubesi"),
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "", "Atatürk İlkeleri ve İnkılap Tarihi I", "", "Amfi 8", section = "A Şubesi"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "", "Türk Dili I", "", "Amfi 8", section = "A Şubesi"),
            programEntry(ScheduleDay.TUESDAY, "10:30", "12:20", "", "İş Sağlığı ve Güvenliği", "", "Amfi 8", section = "A Şubesi"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "17:20", "", "Anatomi (Teorik)", "", "Amfi 8", section = "A Şubesi"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "12:20", "", "Fizyoloji", "", "Amfi 8", section = "A Şubesi"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "", "Bakım Davranışlarını Geliştirme", "", "Amfi 8", "Seçmeli", note = "Grup I", section = "A Şubesi"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:20", "", "Mikrobiyoloji ve Parazitoloji", "", "Amfi 8", section = "A Şubesi"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "", "Girişimcilik", "", "Amfi 8", "Alttan alınan", note = "Yalnızca alttan alan öğrenciler", section = "A Şubesi"),
            programEntry(ScheduleDay.THURSDAY, "15:30", "17:20", "", "Kişilerarası İlişkiler", "", "Amfi 8", section = "A Şubesi"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "12:20", "", "Hemşirelik Esasları-I", "", "Amfi 8", section = "A Şubesi"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "16:20", "", "Anatomi (Lab.)", "", "", section = "A Şubesi"),
            programEntry(ScheduleDay.MONDAY, "08:30", "12:20", "", "Hemşirelik Esasları-I", "", "Amfi 6", section = "B Şubesi"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "", "Hemşirelik Felsefesi", "", "Amfi 6", "Seçmeli", note = "Grup I", section = "B Şubesi"),
            programEntry(ScheduleDay.MONDAY, "15:30", "17:20", "", "Türk Dili I", "", "Amfi 6", section = "B Şubesi"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "10:20", "", "İş Sağlığı ve Güvenliği", "", "Amfi 6", section = "B Şubesi"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "10:20", "", "Yabancı Dil I", "", "Amfi 6", section = "B Şubesi"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "", "Atatürk İlkeleri ve İnkılap Tarihi I", "", "Amfi 6", section = "B Şubesi"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "17:20", "", "Fizyoloji", "", "Amfi 6", section = "B Şubesi"),
            programEntry(ScheduleDay.THURSDAY, "08:30", "12:20", "", "Anatomi (Teorik)", "", "Amfi 6", section = "B Şubesi"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "", "Mikrobiyoloji ve Parazitoloji", "", "Amfi 6", section = "B Şubesi"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "10:20", "", "Kişilerarası İlişkiler", "", "Amfi 6", section = "B Şubesi"),
            programEntry(ScheduleDay.FRIDAY, "10:30", "12:20", "", "Anatomi (Lab.)", "", "", section = "B Şubesi")
        )
    )

    val nursingSecondYear = nursingSchedule(
        classYear = SECOND_YEAR,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "08:30", "12:20", "", "İç Hastalıkları Hemşireliği (Teorik)", "", "Amfi 1", section = "A Şubesi"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "", "İç Hastalıkları Hemşireliği (Lab)", "", "", section = "A Şubesi"),
            programEntry(ScheduleDay.MONDAY, "15:30", "17:20", "", "İç Hastalıkları Hemşireliği (Uyg)", "", "", section = "A Şubesi"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "12:20", "", "İç Hastalıkları Hemşireliği (Uyg)", "", "", section = "A Şubesi"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "17:20", "", "İç Hastalıkları Hemşireliği (Uyg)", "", "", section = "A Şubesi"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "", "Yapay Zeka Okuryazarlığı", "", "Enformatik LAB-01", "Seçmeli", note = "Grup V", section = "A Şubesi"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "", "Beslenmeye Giriş", "", "Amfi 1", section = "A Şubesi"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "17:20", "", "Sağlık Bakım Etiği", "", "Amfi 1", "Seçmeli", note = "Grup IV", section = "A Şubesi"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:20", "", "Patoloji", "", "Amfi 1", section = "A Şubesi"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "17:20", "", "Mesleki Yabancı Dil I", "", "Amfi 1", section = "A Şubesi"),
            programEntry(ScheduleDay.FRIDAY, "09:30", "10:20", "", "İngilizce Literatür Tarama", "", "Amfi 1", "Seçmeli", note = "Grup V", section = "A Şubesi"),
            programEntry(ScheduleDay.FRIDAY, "10:30", "12:20", "", "Biyokimya", "", "Amfi 1", section = "A Şubesi"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "17:20", "", "İngilizce Literatür Tarama", "", "Amfi 1", "Seçmeli", note = "Grup V", section = "A Şubesi"),
            programEntry(ScheduleDay.MONDAY, "08:30", "10:20", "", "Beslenmeye Giriş", "", "Amfi 3", section = "B Şubesi"),
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "", "Farmakoloji", "", "Amfi 3", "Alttan alınan", note = "2020–2021 kataloğuna tabi olup alttan alanlar", section = "B Şubesi"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "", "Patoloji", "", "Amfi 3", section = "B Şubesi"),
            programEntry(ScheduleDay.MONDAY, "15:30", "17:20", "", "Hemşirelik Tarihi ve Deontoloji", "", "Amfi 3", "Seçmeli", note = "Grup IV", section = "B Şubesi"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "12:20", "", "Mesleki Yabancı Dil I", "", "Amfi 3", section = "B Şubesi"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "", "Yapay Zeka Tabanlı Analitik Yaklaşımlar", "", "Enformatik LAB-07", "Seçmeli", note = "Grup V", section = "B Şubesi"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "12:20", "", "İç Hastalıkları Hemşireliği (Teorik)", "", "Amfi 3", section = "B Şubesi"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "", "İç Hastalıkları Hemşireliği (Lab)", "", "", section = "B Şubesi"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "17:20", "", "İç Hastalıkları Hemşireliği (Uyg)", "", "", section = "B Şubesi"),
            programEntry(ScheduleDay.THURSDAY, "08:30", "12:20", "", "İç Hastalıkları Hemşireliği (Uyg)", "", "", section = "B Şubesi"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "17:20", "", "İç Hastalıkları Hemşireliği (Uyg)", "", "", section = "B Şubesi"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "10:20", "", "Biyokimya", "", "Amfi 3", section = "B Şubesi"),
            programEntry(ScheduleDay.FRIDAY, "11:30", "12:20", "", "İngilizce Literatür Değerlendirme", "", "Amfi 3", "Seçmeli", note = "Grup V", section = "B Şubesi"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "17:20", "", "İngilizce Literatür Değerlendirme", "", "Amfi 3", "Seçmeli", note = "Grup V", section = "B Şubesi")
        )
    )

    val nursingThirdYear = nursingSchedule(
        classYear = THIRD_YEAR,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "08:30", "12:20", "", "Kadın Sağlığı ve Hastalıkları Hemşireliği (Teorik)", "", "Amfi 5", section = "A Şubesi"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "", "Kadın Sağlığı ve Hastalıkları Hemşireliği (Lab)", "", "", section = "A Şubesi"),
            programEntry(ScheduleDay.MONDAY, "15:30", "17:20", "", "Kadın Sağlığı ve Hastalıkları Hemşireliği (Uyg)", "", "", section = "A Şubesi"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "12:20", "", "Kadın Sağlığı ve Hastalıkları Hemşireliği (Uyg)", "", "", section = "A Şubesi"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "17:20", "", "Kadın Sağlığı ve Hastalıkları Hemşireliği (Uyg)", "", "", section = "A Şubesi"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "", "Biyoistatistik", "", "Amfi 5", section = "A Şubesi"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "", "Toplumsal Duyarlılık ve Katkı-1", "", "Amfi 5", note = "Şubelerden biri seçilmeli", section = "A Şubesi"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "17:20", "", "Toplumsal Duyarlılık ve Katkı-2", "", "Amfi 5", note = "Şubelerden biri seçilmeli", section = "A Şubesi"),
            programEntry(ScheduleDay.THURSDAY, "08:30", "12:20", "", "Hemşirelikte Yönetim (Teorik)", "", "Amfi 5", section = "A Şubesi"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "17:20", "", "Hemşirelikte Yönetim (Uyg)", "", "", section = "A Şubesi"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "12:20", "", "Mesleki Yabancı Dil III", "", "Amfi 5", section = "A Şubesi"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "16:20", "", "Acil Bakım Hemşireliği", "", "Amfi 5", "Seçmeli", note = "Grup IX", section = "A Şubesi"),
            programEntry(ScheduleDay.MONDAY, "08:30", "12:20", "", "Mesleki Yabancı Dil III", "", "Amfi 7", section = "B Şubesi"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "", "Travma Hemşireliği", "", "Amfi 7", "Seçmeli", note = "Grup IX", section = "B Şubesi"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "12:20", "", "Hemşirelikte Yönetim (Teorik)", "", "Amfi 7", section = "B Şubesi"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "17:20", "", "Hemşirelikte Yönetim (Uyg)", "", "", section = "B Şubesi"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "12:20", "", "Kadın Sağlığı ve Hastalıkları Hemşireliği (Teorik)", "", "Amfi 7", section = "B Şubesi"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "", "Kadın Sağlığı ve Hastalıkları Hemşireliği (Lab)", "", "", section = "B Şubesi"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "17:20", "", "Kadın Sağlığı ve Hastalıkları Hemşireliği (Uyg)", "", "", section = "B Şubesi"),
            programEntry(ScheduleDay.THURSDAY, "08:30", "12:20", "", "Kadın Sağlığı ve Hastalıkları Hemşireliği (Uyg)", "", "", section = "B Şubesi"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "17:20", "", "Kadın Sağlığı ve Hastalıkları Hemşireliği (Uyg)", "", "", section = "B Şubesi"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "10:20", "", "Toplumsal Duyarlılık ve Katkı-3", "", "Amfi 7", note = "Şubelerden biri seçilmeli", section = "B Şubesi"),
            programEntry(ScheduleDay.FRIDAY, "10:30", "12:20", "", "Toplumsal Duyarlılık ve Katkı-4", "", "Amfi 7", note = "Şubelerden biri seçilmeli", section = "B Şubesi"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "16:20", "", "Biyoistatistik", "", "Amfi 7", section = "B Şubesi")
        )
    )

    val nursingFourthYear = nursingSchedule(
        classYear = FOURTH_YEAR,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "08:30", "12:20", "", "Ruh Sağlığı ve Hastalıkları Hemşireliği (Teorik)", "", "Amfi 2", section = "A Şubesi"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "", "Ruh Sağlığı ve Hastalıkları Hemşireliği (Lab)", "", "", section = "A Şubesi"),
            programEntry(ScheduleDay.MONDAY, "15:30", "17:20", "", "Ruh Sağlığı ve Hastalıkları Hemşireliği (Uyg)", "", "", section = "A Şubesi"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "12:20", "", "Ruh Sağlığı ve Hastalıkları Hemşireliği (Uyg)", "", "", section = "A Şubesi"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "17:20", "", "Ruh Sağlığı ve Hastalıkları Hemşireliği (Uyg)", "", "", section = "A Şubesi"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "12:20", "", "Mesleki Yabancı Dil V", "", "Amfi 2", section = "A Şubesi"),
            programEntry(ScheduleDay.THURSDAY, "08:30", "12:20", "", "Halk Sağlığı Hemşireliği (Teorik)", "", "Amfi 2", section = "A Şubesi"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "17:20", "", "Halk Sağlığı Hemşireliği (Uyg.)", "", "", section = "A Şubesi"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "12:20", "", "Halk Sağlığı Hemşireliği (Uyg.)", "", "", section = "A Şubesi"),
            programEntry(ScheduleDay.FRIDAY, "13:30", "17:20", "", "Halk Sağlığı Hemşireliği (Uyg.)", "", "", section = "A Şubesi"),
            programEntry(ScheduleDay.MONDAY, "08:30", "12:20", "", "Halk Sağlığı Hemşireliği (Teorik)", "", "Amfi 4", section = "B Şubesi"),
            programEntry(ScheduleDay.MONDAY, "13:30", "17:20", "", "Halk Sağlığı Hemşireliği (Uyg.)", "", "", section = "B Şubesi"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "12:20", "", "Halk Sağlığı Hemşireliği (Uyg.)", "", "", section = "B Şubesi"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "17:20", "", "Halk Sağlığı Hemşireliği (Uyg.)", "", "", section = "B Şubesi"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "17:20", "", "Mesleki Yabancı Dil V", "", "Amfi 4", section = "B Şubesi"),
            programEntry(ScheduleDay.THURSDAY, "08:30", "12:20", "", "Ruh Sağlığı ve Hastalıkları Hemşireliği (Teorik)", "", "Amfi 4", section = "B Şubesi"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "", "Ruh Sağlığı ve Hastalıkları Hemşireliği (Lab)", "", "", section = "B Şubesi"),
            programEntry(ScheduleDay.THURSDAY, "15:30", "17:20", "", "Ruh Sağlığı ve Hastalıkları Hemşireliği (Uyg)", "", "", section = "B Şubesi"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "12:20", "", "Ruh Sağlığı ve Hastalıkları Hemşireliği (Uyg)", "", "", section = "B Şubesi"),
            programEntry(ScheduleDay.FRIDAY, "13:30", "17:20", "", "Ruh Sağlığı ve Hastalıkları Hemşireliği (Uyg)", "", "", section = "B Şubesi")
        )
    )


    private const val HEALTH_SCIENCES_FACULTY = "Sağlık Bilimleri Fakültesi"
    private const val DIETETICS_DEPARTMENT = "Beslenme ve Diyetetik"
    private const val DIETETICS_SOURCE_URL =
        "https://webis.akdeniz.edu.tr/file/getfile?guid=3c6d7c60-a2bd-457f-8fef-faf564951dd1"

    private fun dieteticsSchedule(
        classYear: String,
        sourcePage: Int,
        entries: List<ScheduleEntry>
    ) = programSchedule(
        faculty = HEALTH_SCIENCES_FACULTY,
        department = DIETETICS_DEPARTMENT,
        classYear = classYear,
        updatedAt = "01.09.2026",
        sourcePage = sourcePage,
        sourceUrl = DIETETICS_SOURCE_URL,
        entries = entries
    )

    val dieteticsFirstYear = dieteticsSchedule(
        classYear = FIRST_YEAR,
        sourcePage = 1,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "12:20", "BES 107", "Temel Kimya ve Laboratuvar Uygulaması-I", "", "D6"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "BES 111", "Beslenme İlkeleri-I", "", "D6"),
            programEntry(ScheduleDay.MONDAY, "15:30", "17:20", "BES 125", "İletişim Becerileri", "", "D6"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "10:20", "KPD 101", "Kariyer Planlama", "", "D6"),
            programEntry(ScheduleDay.TUESDAY, "10:30", "12:20", "BES 123", "Dünya Mutfakları", "", "D6"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "BES 109", "Temel Matematik", "", "D6"),
            programEntry(ScheduleDay.TUESDAY, "15:30", "17:20", "BES 131", "Beslenme ve Medya", "", "D6"),
            programEntry(ScheduleDay.WEDNESDAY, "10:30", "12:20", "ATA 101", "Atatürk İlkeleri ve İnkılap Tarihi I", "", "Merkezi Derslik Amfi-2"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "TDB 101", "Türk Dili I", "", "Merkezi Derslik Amfi-2"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "17:20", "YBD 101", "Yabancı Dil-I (İngilizce-I)", "", "Merkezi Derslik Amfi-2"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "10:20", "BES 121", "Dünyadaki Besin Kaynakları", "", "D6"),
            programEntry(ScheduleDay.THURSDAY, "10:30", "12:20", "BES 107", "Temel Kimya ve Laboratuvar Uygulaması-I", "", "D6"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "16:20", "BES 113", "Beslenme İlkeleri Uygulaması-I", "", "D6"),
            programEntry(ScheduleDay.FRIDAY, "09:30", "11:20", "BES 105", "Demografik Yapı ve Sağlık", "", "D6"),
            programEntry(ScheduleDay.FRIDAY, "11:30", "12:20", "BES 103", "Mesleki Oryantasyon", "", "D7")
        )
    )

    val dieteticsSecondYear = dieteticsSchedule(
        classYear = SECOND_YEAR,
        sourcePage = 2,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "09:30", "10:20", "BES 229", "Besin Tüketim Durumunun Saptanması", "", "D7"),
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "BES 213", "Besin Kimyası ve Analizleri-I", "", "D7"),
            programEntry(ScheduleDay.MONDAY, "13:30", "15:20", "BES 207", "Fizyoloji-I", "", "D7"),
            programEntry(ScheduleDay.MONDAY, "15:30", "16:20", "BES 223", "Yaşlılıkta Beslenme", "", "D7"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "BES 215", "Besin Kimyası ve Analizleri Uygulaması-I", "", "D7 + LAB", courseType = "Uygulama", section = "Şube I"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "BES 205", "Anatomi-I", "", "D7"),
            programEntry(ScheduleDay.TUESDAY, "15:30", "16:20", "BES 221", "Egzersiz ve Beslenme", "", "D7"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "BES 211", "Mesleki Yabancı Dil-I", "", "D7"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "BES 209", "Genel Mikrobiyoloji I (T)", "", "D7"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "17:20", "BES 209", "Genel Mikrobiyoloji I (U)", "", "LAB"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "17:20", "BES 201", "Beslenme Biyokimyası-I", "", "D7")
        )
    )

    val dieteticsThirdYear = dieteticsSchedule(
        classYear = THIRD_YEAR,
        sourcePage = 3,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "10:30", "12:20", "BES 315", "Yetişkin Hastalıklarında Diyet Tedavisi-I", "", "D8"),
            programEntry(ScheduleDay.MONDAY, "13:30", "14:20", "BES 343", "Akademik Yayın Tarama, Yorumlama ve Araştırma Planlama", "", "D8"),
            programEntry(ScheduleDay.MONDAY, "14:30", "16:20", "BES 339", "Diyet İlkeleri ve Popüler Diyetler", "", "D8"),
            programEntry(ScheduleDay.TUESDAY, "09:30", "12:20", "BES 307", "Toplumda Beslenme Durumunun Saptanması", "", "D8"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "15:20", "BES 311", "Çocuk Hastalıklarında Beslenme Tedavisi-I", "", "D8"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "BES 317", "Yetişkin Hastalıklarında Diyet Tedavisi Uygulaması-I", "", "D8"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "14:20", "BES 321", "Fonksiyonel Besinler ve Sağlık", "", "D8"),
            programEntry(ScheduleDay.WEDNESDAY, "14:30", "15:20", "BES 341", "Beslenme Durumu Tarama Testleri", "", "D8"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "BES 313", "Çocuk Hastalıklarında Beslenme Tedavisi Uygulaması-I", "", "D8"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "14:20", "BES 327", "Kanser ve Beslenme", "", "D8"),
            programEntry(ScheduleDay.THURSDAY, "14:30", "16:20", "BES 325", "Beslenme Bilimlerinde Yapay Zeka ve Bilgisayar Uygulamaları", "", "D8"),
            programEntry(ScheduleDay.FRIDAY, "09:30", "10:20", "BES 335", "Genel Toksikoloji", "", "D8"),
            programEntry(ScheduleDay.FRIDAY, "10:30", "12:20", "BES 309", "Besin Kontrolü ve Mevzuatı", "", "D8"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "17:20", "BES 303", "Toplu Beslenme Yapılan Kurumlarda Beslenme-I", "", "D8")
        )
    )

    val dieteticsFourthYear = dieteticsSchedule(
        classYear = FOURTH_YEAR,
        sourcePage = 4,
        entries = listOf(
            programEntry(ScheduleDay.MONDAY, "08:30", "11:20", "BES 407", "Beslenme ve Diyetetik Alanında Mesleki Uygulamalar-I", "", "", courseType = "Teorik", note = "Teorik (T)"),
            programEntry(ScheduleDay.MONDAY, "11:30", "12:20", "BES 407", "Beslenme ve Diyetetik Alanında Mesleki Uygulamalar-I", "", "", courseType = "Uygulama", note = "Uygulama (U)"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "12:20", "BES 407", "Beslenme ve Diyetetik Alanında Mesleki Uygulamalar-I", "", "", courseType = "Uygulama", note = "Uygulama (U)"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "BES 407", "Beslenme ve Diyetetik Alanında Mesleki Uygulamalar-I", "", "", courseType = "Uygulama", note = "Uygulama (U)"),
            programEntry(ScheduleDay.THURSDAY, "08:30", "09:20", "BES 407", "Beslenme ve Diyetetik Alanında Mesleki Uygulamalar-I", "", "", courseType = "Uygulama", note = "Uygulama (U)"),
            programEntry(ScheduleDay.MONDAY, "08:30", "11:20", "BES 408", "Beslenme ve Diyetetik Alanında Mesleki Uygulamalar-III", "", "", courseType = "Teorik", note = "Teorik (T)"),
            programEntry(ScheduleDay.MONDAY, "11:30", "12:20", "BES 408", "Beslenme ve Diyetetik Alanında Mesleki Uygulamalar-III", "", "", courseType = "Uygulama", note = "Uygulama (U)"),
            programEntry(ScheduleDay.TUESDAY, "08:30", "12:20", "BES 408", "Beslenme ve Diyetetik Alanında Mesleki Uygulamalar-III", "", "", courseType = "Uygulama", note = "Uygulama (U)"),
            programEntry(ScheduleDay.WEDNESDAY, "09:30", "12:20", "BES 408", "Beslenme ve Diyetetik Alanında Mesleki Uygulamalar-III", "", "", courseType = "Uygulama", note = "Uygulama (U)"),
            programEntry(ScheduleDay.THURSDAY, "08:30", "09:20", "BES 408", "Beslenme ve Diyetetik Alanında Mesleki Uygulamalar-III", "", "", courseType = "Uygulama", note = "Uygulama (U)"),
            programEntry(ScheduleDay.MONDAY, "13:30", "17:20", "BES 409", "Beslenme ve Diyetetik Alanında Mesleki Uygulamalar-II", "", "", courseType = "Uygulama", note = "Uygulama (U)"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "BES 409", "Beslenme ve Diyetetik Alanında Mesleki Uygulamalar-II", "", "", courseType = "Teorik", note = "Teorik (T)"),
            programEntry(ScheduleDay.TUESDAY, "16:30", "17:20", "BES 409", "Beslenme ve Diyetetik Alanında Mesleki Uygulamalar-II", "", "", courseType = "Uygulama", note = "Uygulama (U)"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "09:20", "BES 409", "Beslenme ve Diyetetik Alanında Mesleki Uygulamalar-II", "", "", courseType = "Uygulama", note = "Uygulama (U)"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "17:20", "BES 409", "Beslenme ve Diyetetik Alanında Mesleki Uygulamalar-II", "", "", courseType = "Uygulama", note = "Uygulama (U)"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "11:20", "BES 409", "Beslenme ve Diyetetik Alanında Mesleki Uygulamalar-I", "", "", courseType = "Uygulama", note = "Uygulama (U)"),
            programEntry(ScheduleDay.THURSDAY, "11:30", "12:20", "BES 409", "Beslenme ve Diyetetik Alanında Mesleki Uygulamalar-II", "", "", courseType = "Uygulama", note = "Uygulama (U)"),
            programEntry(ScheduleDay.MONDAY, "13:30", "17:20", "BES 410", "Beslenme ve Diyetetik Alanında Mesleki Uygulamalar-IV", "", "", courseType = "Uygulama", note = "Uygulama (U)"),
            programEntry(ScheduleDay.TUESDAY, "13:30", "16:20", "BES 410", "Beslenme ve Diyetetik Alanında Mesleki Uygulamalar-IV", "", "", courseType = "Teorik", note = "Teorik (T)"),
            programEntry(ScheduleDay.TUESDAY, "16:30", "17:20", "BES 410", "Beslenme ve Diyetetik Alanında Mesleki Uygulamalar-IV", "", "", courseType = "Uygulama", note = "Uygulama (U)"),
            programEntry(ScheduleDay.WEDNESDAY, "08:30", "09:20", "BES 410", "Beslenme ve Diyetetik Alanında Mesleki Uygulamalar-IV", "", "", courseType = "Uygulama", note = "Uygulama (U)"),
            programEntry(ScheduleDay.WEDNESDAY, "15:30", "17:20", "BES 410", "Beslenme ve Diyetetik Alanında Mesleki Uygulamalar-IV", "", "", courseType = "Uygulama", note = "Uygulama (U)"),
            programEntry(ScheduleDay.THURSDAY, "09:30", "12:20", "BES 410", "Beslenme ve Diyetetik Alanında Mesleki Uygulamalar-IV", "", "", courseType = "Uygulama", note = "Uygulama (U)"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "BES 407", "Beslenme ve Diyetetik Alanında Mesleki Uygulamalar-II", "", "", courseType = "Uygulama", note = "Uygulama (U)"),
            programEntry(ScheduleDay.WEDNESDAY, "13:30", "15:20", "BES 408", "Beslenme ve Diyetetik Alanında Mesleki Uygulamalar-III", "", "", courseType = "Uygulama", note = "Uygulama (U)"),
            programEntry(ScheduleDay.THURSDAY, "13:30", "15:20", "BES 431", "Biyoistatistik", "", "Enformatik LAB-1"),
            programEntry(ScheduleDay.THURSDAY, "15:30", "17:20", "BES 405", "Mezuniyet Projesi-I", "", "D7", courseType = "Ders", note = "ŞUBE 1-2-3"),
            programEntry(ScheduleDay.FRIDAY, "08:30", "10:20", "BES 403", "Seminer-I", "", "D7", courseType = "Ders", note = "Şube 1-2"),
            programEntry(ScheduleDay.FRIDAY, "10:30", "11:20", "BES 423", "Beslenme ve Diyetetik Alanında Danışmanlık Hizmetleri ile Branşlaşma", "", "D7"),
            programEntry(ScheduleDay.FRIDAY, "11:30", "12:20", "BES 425", "Beslenme ve Genetik", "", "D6"),
            programEntry(ScheduleDay.FRIDAY, "14:30", "15:20", "BES 427", "Beslenme ve Diyetetik Alanında Etik", "", "D6")
        )
    )

    fun find(faculty: String?, department: String?, classYear: String?): ClassSchedule? {
        val schedules = when (faculty) {
            BUSINESS_FACULTY -> when (department) {
                BUSINESS_DEPARTMENT -> listOf(businessFirstYear, businessSecondYear, businessThirdYear, businessFourthYear)
                ECONOMICS_DEPARTMENT -> listOf(economicsFirstYear, economicsSecondYear, economicsThirdYear, economicsFourthYear)
                ECONOMETRICS_DEPARTMENT -> listOf(econometricsFirstYear, econometricsSecondYear, econometricsThirdYear, econometricsFourthYear)
                PUBLIC_FINANCE_DEPARTMENT -> listOf(maliyeFirstYear, maliyeSecondYear, maliyeThirdYear, maliyeFourthYear)
                LABOR_ECONOMICS_DEPARTMENT -> listOf(laborEconomicsFirstYear, laborEconomicsSecondYear, laborEconomicsThirdYear, laborEconomicsFourthYear)
                POLITICAL_SCIENCE_DEPARTMENT -> listOf(politicalScienceFirstYear, politicalScienceSecondYear, politicalScienceThirdYear, politicalScienceFourthYear)
                INTERNATIONAL_RELATIONS_DEPARTMENT -> listOf(internationalRelationsFirstYear, internationalRelationsSecondYear, internationalRelationsThirdYear, internationalRelationsFourthYear)
                else -> return null
            }
            ENGINEERING_FACULTY -> when (department) {
                COMPUTER_ENGINEERING_DEPARTMENT -> listOf(computerEngineeringFirstYear, computerEngineeringSecondYear, computerEngineeringThirdYear, computerEngineeringFourthYear)
                AI_DATA_ENGINEERING_DEPARTMENT -> listOf(aiDataEngineeringFirstYear, aiDataEngineeringSecondYear, aiDataEngineeringThirdYear, aiDataEngineeringFourthYear)
                ELECTRICAL_ELECTRONICS_ENGINEERING_DEPARTMENT -> listOf(electricalEngineeringFirstYear, electricalEngineeringSecondYear, electricalEngineeringThirdYear, electricalEngineeringFourthYear)
                ENVIRONMENTAL_ENGINEERING_DEPARTMENT -> listOf(environmentalEngineeringFirstYear, environmentalEngineeringSecondYear, environmentalEngineeringThirdYear, environmentalEngineeringFourthYear)
                FOOD_ENGINEERING_DEPARTMENT -> listOf(foodEngineeringFirstYear, foodEngineeringSecondYear, foodEngineeringThirdYear, foodEngineeringFourthYear)
                CIVIL_ENGINEERING_DEPARTMENT -> listOf(civilEngineeringFirstYear, civilEngineeringSecondYear, civilEngineeringThirdYear, civilEngineeringFourthYear)
                GEOLOGY_ENGINEERING_DEPARTMENT -> listOf(geologyEngineeringFirstYear, geologyEngineeringSecondYear, geologyEngineeringThirdYear, geologyEngineeringFourthYear)
                MECHANICAL_ENGINEERING_DEPARTMENT -> listOf(mechanicalEngineeringFirstYear, mechanicalEngineeringSecondYear, mechanicalEngineeringThirdYear, mechanicalEngineeringFourthYear)
                else -> return null
            }
            ARCHITECTURE_FACULTY -> when (department) {
                ARCHITECTURE_DEPARTMENT -> listOf(architectureFirstYear, architectureSecondYear, architectureThirdYear, architectureFourthYear)
                INTERIOR_ARCHITECTURE_DEPARTMENT -> listOf(interiorArchitectureFirstYear, interiorArchitectureSecondYear, interiorArchitectureThirdYear, interiorArchitectureFourthYear)
                else -> return null
            }
            APPLIED_SCIENCES_FACULTY -> when (department) {
                FINANCE_BANKING_DEPARTMENT -> listOf(financeBankingFirstYear, financeBankingSecondYear, financeBankingThirdYear, financeBankingFourthYear)
                MARKETING_DEPARTMENT -> listOf(marketingFirstYear, marketingSecondYear, marketingThirdYear, marketingFourthYear)
                INSURANCE_DEPARTMENT -> listOf(insuranceFirstYear, insuranceSecondYear, insuranceThirdYear, insuranceFourthYear)
                INTERNATIONAL_TRADE_LOGISTICS_DEPARTMENT -> listOf(internationalTradeLogisticsFirstYear, internationalTradeLogisticsSecondYear, internationalTradeLogisticsThirdYear, internationalTradeLogisticsFourthYear)
                MANAGEMENT_INFORMATION_SYSTEMS_DEPARTMENT -> listOf(managementInformationSystemsFirstYear, managementInformationSystemsSecondYear, managementInformationSystemsThirdYear, managementInformationSystemsFourthYear)
                else -> return null
            }
            EDUCATION_FACULTY -> when (department) {
                SCIENCE_EDUCATION_DEPARTMENT -> listOf(scienceEducationFirstYear, scienceEducationSecondYear, scienceEducationThirdYear, scienceEducationFourthYear)
                ENGLISH_TEACHING_DEPARTMENT -> listOf(englishTeachingFirstYear, englishTeachingSecondYear, englishTeachingThirdYear, englishTeachingFourthYear)
                ELEMENTARY_MATHEMATICS_EDUCATION_DEPARTMENT -> listOf(elementaryMathematicsEducationFirstYear, elementaryMathematicsEducationSecondYear, elementaryMathematicsEducationThirdYear, elementaryMathematicsEducationFourthYear)
                EARLY_CHILDHOOD_EDUCATION_DEPARTMENT -> listOf(earlyChildhoodEducationFirstYear, earlyChildhoodEducationSecondYear, earlyChildhoodEducationThirdYear, earlyChildhoodEducationFourthYear)
                SPECIAL_EDUCATION_DEPARTMENT -> listOf(specialEducationFirstYear, specialEducationSecondYear, specialEducationThirdYear, specialEducationFourthYear)
                COUNSELING_DEPARTMENT -> listOf(counselingFirstYear, counselingSecondYear, counselingThirdYear, counselingFourthYear)
                CLASSROOM_TEACHING_DEPARTMENT -> listOf(classroomTeachingFirstYear, classroomTeachingSecondYear, classroomTeachingThirdYear, classroomTeachingFourthYear)
                SOCIAL_STUDIES_TEACHING_DEPARTMENT -> listOf(socialStudiesTeachingFirstYear, socialStudiesTeachingSecondYear, socialStudiesTeachingThirdYear, socialStudiesTeachingFourthYear)
                TURKISH_TEACHING_DEPARTMENT -> listOf(turkishTeachingFirstYear, turkishTeachingSecondYear, turkishTeachingThirdYear, turkishTeachingFourthYear)
                else -> return null
            }
            NURSING_FACULTY -> when (department) {
                NURSING_DEPARTMENT -> listOf(nursingFirstYear, nursingSecondYear, nursingThirdYear, nursingFourthYear)
                else -> return null
            }
            HEALTH_SCIENCES_FACULTY -> when (department) {
                DIETETICS_DEPARTMENT -> listOf(dieteticsFirstYear, dieteticsSecondYear, dieteticsThirdYear, dieteticsFourthYear)
                else -> return null
            }
            TOURISM_FACULTY -> TourismSchedules.schedulesFor(department) ?: return null
            COMMUNICATION_FACULTY -> CommunicationSchedules.schedulesFor(department) ?: return null
            LAW_FACULTY -> LawSchedules.schedulesFor(department) ?: return null
            SPORT_SCIENCES_FACULTY -> SportsScienceSchedules.schedulesFor(department) ?: return null
            FINE_ARTS_FACULTY -> FineArtsSchedules.schedulesFor(department) ?: return null
            FISHERIES_FACULTY -> FisheriesSchedules.schedulesFor(department) ?: return null
            THEOLOGY_FACULTY -> TheologySchedules.schedulesFor(department) ?: return null
            DENTAL_FACULTY -> DentalSchedules.schedulesFor(department) ?: return null
            LITERATURE_FACULTY -> LiteratureSchedules.schedulesFor(department) ?: return null
            SCIENCE_FACULTY -> ScienceSchedules.schedulesFor(department) ?: return null
            AGRICULTURE_FACULTY -> AgricultureSchedules.schedulesFor(department) ?: return null
            else -> return null
        }
        // A saved class year can outlive a faculty switch (e.g. 5. Sınıf outside dentistry), so never index blindly.
        return when (classYear) {
            FIRST_YEAR -> schedules.getOrNull(0)
            SECOND_YEAR -> schedules.getOrNull(1)
            THIRD_YEAR -> schedules.getOrNull(2)
            FOURTH_YEAR -> schedules.getOrNull(3)
            FIFTH_YEAR -> schedules.getOrNull(4)
            else -> null
        }
    }
}
