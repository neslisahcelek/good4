package com.good4.schedule.domain

/** Kemer Denizcilik Fakültesi Denizcilik İşletmeleri Yönetimi 2026–2027 güz dönemi lisans programı. */
internal object MaritimeSchedules {
    private const val ACADEMIC_YEAR = "2026-2027"
    private const val TERM = "Güz Yarıyılı"
    private const val UPDATED_AT = "27.08.2026 Yönetim Kurulu Kararı"
    private const val SOURCE_URL =
        "https://webis.akdeniz.edu.tr/file/getfile?guid=fc3451a9-2085-4fde-b8c2-1cfd212f07e9"

    private data class CourseBlock(
        val day: ScheduleDay,
        val year: Int,
        val firstPeriod: Int,
        val lastPeriod: Int,
        val code: String,
        val name: String,
        val instructor: String,
        val classroom: String,
        val courseType: String = "Ders",
        val note: String? = null
    )

    private data class Period(val start: String, val end: String)

    private val periods = listOf(
        Period("08:30", "09:20"),
        Period("09:30", "10:20"),
        Period("10:30", "11:20"),
        Period("11:30", "12:20"),
        Period("13:30", "14:20"),
        Period("14:30", "15:20"),
        Period("15:30", "16:20"),
        Period("16:30", "17:20")
    )

    private val blocks = listOf(
        // 1. sınıf
        CourseBlock(ScheduleDay.MONDAY, 1, 5, 7, "DİY 103", "Mikro İktisat", "Doç. Dr. Mükerrem Oral", "Z-6"),
        CourseBlock(ScheduleDay.TUESDAY, 1, 1, 2, "YBD 101", "İngilizce I", "", "Z-6"),
        CourseBlock(ScheduleDay.TUESDAY, 1, 5, 6, "ATA 101", "Atatürk İlkeleri ve İnkılap Tarihi I", "", "Z-6"),
        CourseBlock(ScheduleDay.WEDNESDAY, 1, 2, 4, "DİY 109", "Denizcilik Tarihi ve Kültürü", "Prof. Dr. İsmet Balık", "Z-6"),
        CourseBlock(ScheduleDay.WEDNESDAY, 1, 5, 7, "DİY 105", "Denizcilik İşletmeleri Yönetimine Giriş", "Dr. Öğr. Üyesi Sonay Zeki Aydın", "Z-6"),
        CourseBlock(ScheduleDay.THURSDAY, 1, 2, 4, "ENF 101", "Bilgi Teknolojileri Kullanımı", "", "Uzaktan (online)"),
        CourseBlock(ScheduleDay.THURSDAY, 1, 6, 8, "TDB 101", "Türk Dili I", "", "Z-6"),
        CourseBlock(ScheduleDay.FRIDAY, 1, 5, 7, "DİY 101", "İşletme Matematiği I", "Öğr. Gör. Metehan Yaykaşlı", "Z-6"),

        // 2. sınıf
        CourseBlock(ScheduleDay.MONDAY, 2, 2, 4, "DİY 205", "Pazarlama Yönetimi", "Doç. Dr. Duygu Aydın Ünal", "Z-6"),
        CourseBlock(ScheduleDay.MONDAY, 2, 5, 7, "DİY 225", "Konteyner Sistemleri ve Yönetimi", "Dr. Öğr. Üyesi Sonay Zeki Aydın", "Z-9"),
        CourseBlock(ScheduleDay.TUESDAY, 2, 2, 4, "DİY 203", "Yönetim ve Organizasyon", "Dr. Öğr. Üyesi Murat Atalay", "Z-5"),
        CourseBlock(ScheduleDay.TUESDAY, 2, 5, 7, "DİY 223", "Gemi Kaynaklı Deniz Kirliliği ve Kontrolü", "Doç. Dr. Yaşar Özvarol", "Z-7"),
        CourseBlock(ScheduleDay.WEDNESDAY, 2, 2, 4, "DİY 209", "Deniz İşletmeciliği Etiği", "Prof. Dr. Hüseyin Şaşı", "Z-8"),
        CourseBlock(ScheduleDay.WEDNESDAY, 2, 5, 7, "DİY 201", "İşletme İstatistiği", "Prof. Dr. İsmet Balık", "Z-5"),
        CourseBlock(ScheduleDay.THURSDAY, 2, 2, 4, "DİY 211", "Deniz Hukuku", "Prof. Dr. İsmet Balık", "Z-6"),
        CourseBlock(ScheduleDay.THURSDAY, 2, 5, 7, "DİY 207", "Finansal Muhasebe", "Öğr. Gör. Dr. Mustafa Çeltikçi", "Z-5"),
        CourseBlock(ScheduleDay.FRIDAY, 2, 2, 4, "DİY 221", "Ekonomik Coğrafya", "Prof. Dr. Hüseyin Şaşı", "Z-8"),

        // 3. sınıf
        CourseBlock(ScheduleDay.MONDAY, 3, 2, 4, "DİY 301", "Yöneylem Araştırması", "Doç. Dr. Mükerrem Oral", "Z-5"),
        CourseBlock(ScheduleDay.MONDAY, 3, 5, 7, "DİY 305", "Deniz Meteorolojisi", "Prof. Dr. Hüseyin Şaşı", "Z-5"),
        CourseBlock(ScheduleDay.TUESDAY, 3, 2, 4, "DİY 323", "İş İngilizcesi", "Doç. Dr. Duygu Aydın Ünal", "Z-8"),
        CourseBlock(ScheduleDay.TUESDAY, 3, 5, 7, "DİY 303", "Finansal Yönetim", "Doç. Dr. Eda Oruç Erdoğan", "Z-5"),
        CourseBlock(ScheduleDay.WEDNESDAY, 3, 2, 4, "DİY 309", "Liman ve Terminal Yönetimi", "Dr. Öğr. Üyesi Sonay Zeki Aydın", "Z-5"),
        CourseBlock(ScheduleDay.WEDNESDAY, 3, 5, 7, "DİY 325", "İnsan Kaynakları Yönetimi", "Dr. Öğr. Üyesi Murat Atalay", "Z-7"),
        CourseBlock(ScheduleDay.THURSDAY, 3, 2, 4, "DİY 329", "Deniz Ürünleri Tedariği ve Pazarlaması", "Doç. Dr. Yaşar Özvarol", "Z-7"),
        CourseBlock(ScheduleDay.THURSDAY, 3, 5, 7, "DİY 307", "İşletme Lojistiği", "Dr. Öğr. Üyesi Sonay Zeki Aydın", "Z-8"),
        CourseBlock(ScheduleDay.FRIDAY, 3, 2, 4, "DİY 321", "Uluslararası Ticaret Hukuku", "Prof. Dr. İsmet Balık", "Z-7"),
        CourseBlock(ScheduleDay.FRIDAY, 3, 5, 7, "DİY 331", "Oşinografi", "Prof. Dr. Hüseyin Şaşı", "Z-8"),

        // 4. sınıf
        CourseBlock(ScheduleDay.MONDAY, 4, 2, 4, "DİY 423", "Gümrük Mevzuatı ve Gümrükleme İşlemleri", "Öğr. Gör. Dr. Çetin Polat", "Z-7"),
        CourseBlock(ScheduleDay.MONDAY, 4, 5, 7, "DİY 429", "Gemi Takip ve İzleme Sistemleri", "Doç. Dr. Yaşar Özvarol", "Z-7"),
        CourseBlock(ScheduleDay.TUESDAY, 4, 2, 4, "DİY 405", "Tedarik Zinciri Yönetimi", "Dr. Öğr. Üyesi Sonay Zeki Aydın", "Z-7 / Z-6"),
        CourseBlock(ScheduleDay.TUESDAY, 4, 5, 7, "DİY 437", "Girişimcilik ve İş Kurma", "Prof. Dr. Mustafa Ünlüsayın", "Z-8"),
        CourseBlock(ScheduleDay.WEDNESDAY, 4, 5, 7, "DİY 433", "Tehlikeli Madde Taşımacılığı", "Prof. Dr. Hüseyin Şaşı", "Z-8"),
        CourseBlock(ScheduleDay.THURSDAY, 4, 2, 4, "DİY 439", "Pazarlama Araştırması", "Doç. Dr. Duygu Aydın Ünal", "Z-8"),
        CourseBlock(ScheduleDay.THURSDAY, 4, 5, 7, "DİY 403", "Gemi Yönetimi", "Prof. Dr. İsmet Balık", "Z-6 / Z-7"),
        CourseBlock(ScheduleDay.FRIDAY, 4, 2, 4, "DİY 401", "Uluslararası İşletmecilik", "Dr. Öğr. Üyesi Murat Atalay", "Z-5"),
        CourseBlock(ScheduleDay.FRIDAY, 4, 5, 7, "DİY 431", "Deniz İş Kanunu", "Prof. Dr. İsmet Balık", "Z-7")
    )

    fun schedulesFor(department: String?): List<ClassSchedule>? {
        if (department != ClassSchedules.MARITIME_BUSINESS_DEPARTMENT) return null
        return (1..4).map { year ->
            ClassSchedule(
                faculty = ClassSchedules.MARITIME_FACULTY,
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
                sourceUrl = SOURCE_URL,
                entries = blocks.asSequence()
                    .filter { it.year == year }
                    .flatMap { block ->
                        periods.subList(block.firstPeriod - 1, block.lastPeriod).asSequence().map { period ->
                            ScheduleEntry(
                                day = block.day,
                                startTime = period.start,
                                endTime = period.end,
                                courseCode = block.code,
                                courseName = block.name,
                                instructor = block.instructor,
                                classroom = block.classroom,
                                courseType = block.courseType,
                                note = block.note
                            )
                        }
                    }
                    .toList()
            )
        }
    }
}
