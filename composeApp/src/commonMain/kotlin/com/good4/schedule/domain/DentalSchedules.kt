package com.good4.schedule.domain

/** Diş Hekimliği Fakültesinin 2026-2027 güz dönemi haftalık ders programı. */
internal object DentalSchedules {
    private const val ACADEMIC_YEAR = "2026-2027"
    private const val TERM = "Güz Yarıyılı"
    private const val UPDATED_AT = "2026-09-23"
    private const val SOURCE_URL =
        "https://webis.akdeniz.edu.tr/uploads/1070/content/G%C3%9CZ%20-%202026-2027%20HAFTALIK%20yeni%20Ders%20program%C4%B1%20-%20Kopya.pdf"
    private const val SOURCE_WARNING =
        "Kaynak PDF'de ders kodları ve öğretim elemanı adları yer almıyor. Klinik ve uygulama dersleri için ayrıca bir derslik belirtilmediğinden konum alanı boş bırakıldı."

    private data class Period(val start: String, val end: String)

    private data class CourseBlock(
        val day: ScheduleDay,
        val year: Int,
        val firstPeriod: Int,
        val lastPeriod: Int,
        val name: String,
        val courseType: String = "Ders",
        val note: String? = null
    )

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

    private fun amphi(year: Int): String = when (year) {
        1 -> "Sarı Amfi · Z-23"
        2 -> "Turuncu Amfi · Z-22"
        3 -> "Melon Amfi · Z-26"
        else -> "Turkuaz Amfi · Z-25"
    }

    private fun course(
        day: ScheduleDay,
        year: Int,
        firstPeriod: Int,
        lastPeriod: Int,
        name: String,
        courseType: String = "Ders",
        note: String? = null
    ) = CourseBlock(day, year, firstPeriod, lastPeriod, name, courseType, note)

    private fun practice(
        day: ScheduleDay,
        year: Int,
        firstPeriod: Int,
        lastPeriod: Int,
        name: String
    ) = course(day, year, firstPeriod, lastPeriod, name, "Uygulama")

    private fun clinic(
        day: ScheduleDay,
        year: Int,
        firstPeriod: Int,
        lastPeriod: Int
    ) = course(day, year, firstPeriod, lastPeriod, "Klinik Uygulama", "Klinik")

    private val blocks = listOf(
        // 1. sınıf
        course(ScheduleDay.MONDAY, 1, 2, 2, "Diş Hekimliği Tarihi"),
        course(ScheduleDay.MONDAY, 1, 5, 6, "Tıbbi Organik Kimya"),
        course(ScheduleDay.TUESDAY, 1, 2, 2, "Diş Morfolojisi ve Manipülasyonu"),
        course(ScheduleDay.TUESDAY, 1, 3, 4, "Tıbbi Biyoloji - Genetik"),
        course(ScheduleDay.TUESDAY, 1, 5, 6, "Biyofizik"),
        course(ScheduleDay.TUESDAY, 1, 7, 8, "Türk Dili"),
        course(ScheduleDay.WEDNESDAY, 1, 1, 2, "Yabancı Dil"),
        course(ScheduleDay.WEDNESDAY, 1, 5, 6, "Atatürk İlkeleri ve İnkılap Tarihi"),
        course(ScheduleDay.THURSDAY, 1, 3, 4, "Girişimcilik"),
        course(ScheduleDay.THURSDAY, 1, 5, 5, "Tıp Bilişimi ve Biyoistatistik"),
        course(ScheduleDay.THURSDAY, 1, 7, 7, "Rusça", "Seçmeli", "Grup 2"),
        course(ScheduleDay.THURSDAY, 1, 8, 8, "Almanca", "Seçmeli", "Grup 2"),
        practice(ScheduleDay.FRIDAY, 1, 1, 4, "Diş Morfolojisi ve Manipülasyonu"),
        practice(ScheduleDay.FRIDAY, 1, 6, 8, "Diş Morfolojisi ve Manipülasyonu"),

        // 2. sınıf
        practice(ScheduleDay.MONDAY, 2, 1, 4, "Endodonti"),
        course(ScheduleDay.MONDAY, 2, 5, 6, "Anatomi"),
        practice(ScheduleDay.MONDAY, 2, 7, 8, "Anatomi"),
        practice(ScheduleDay.TUESDAY, 2, 1, 4, "Restoratif Diş Tedavisi"),
        course(ScheduleDay.TUESDAY, 2, 5, 6, "Restoratif Diş Tedavisi"),
        course(ScheduleDay.TUESDAY, 2, 7, 8, "Protetik Diş Tedavisi"),
        course(ScheduleDay.WEDNESDAY, 2, 1, 1, "Maddeler Bilgisi"),
        course(ScheduleDay.WEDNESDAY, 2, 2, 2, "Endodonti"),
        course(ScheduleDay.WEDNESDAY, 2, 3, 4, "Biyokimya"),
        course(ScheduleDay.WEDNESDAY, 2, 5, 6, "Fizyoloji"),
        course(ScheduleDay.WEDNESDAY, 2, 7, 8, "Mikrobiyoloji-İmmünoloji"),
        practice(ScheduleDay.THURSDAY, 2, 1, 8, "Protetik Diş Tedavisi"),
        course(ScheduleDay.FRIDAY, 2, 1, 4, "Histoloji Embriyoloji"),
        course(ScheduleDay.FRIDAY, 2, 6, 6, "Mesleki İngilizce"),

        // 3. sınıf
        course(ScheduleDay.MONDAY, 3, 1, 1, "Dental Anestezi"),
        course(ScheduleDay.MONDAY, 3, 2, 2, "Patoloji"),
        course(ScheduleDay.MONDAY, 3, 3, 3, "Restoratif Diş Tedavisi"),
        course(ScheduleDay.MONDAY, 3, 4, 4, "Farmakoloji"),
        practice(ScheduleDay.MONDAY, 3, 5, 8, "Restoratif Diş Tedavisi"),
        course(ScheduleDay.TUESDAY, 3, 2, 2, "Sosyal Tıp ve Tıp Etiği"),
        course(ScheduleDay.TUESDAY, 3, 3, 3, "Çocuk Diş Hekimliği"),
        practice(ScheduleDay.TUESDAY, 3, 5, 8, "Endodonti"),
        practice(ScheduleDay.WEDNESDAY, 3, 1, 8, "Protetik Diş Tedavisi"),
        course(ScheduleDay.THURSDAY, 3, 2, 2, "Endodonti"),
        course(ScheduleDay.THURSDAY, 3, 3, 4, "Ağız Diş ve Çene Cerrahisi"),
        course(ScheduleDay.THURSDAY, 3, 5, 6, "Protetik Diş Tedavisi"),
        course(ScheduleDay.THURSDAY, 3, 7, 8, "Ortodonti"),
        course(ScheduleDay.FRIDAY, 3, 1, 2, "Periodontoloji"),
        course(ScheduleDay.FRIDAY, 3, 3, 4, "Ağız Diş ve Çene Radyolojisi"),

        // 4. sınıf
        clinic(ScheduleDay.MONDAY, 4, 1, 4),
        course(ScheduleDay.MONDAY, 4, 5, 5, "Klinik Tıp Bilimleri"),
        course(ScheduleDay.MONDAY, 4, 6, 6, "Restoratif Diş Tedavisi"),
        course(ScheduleDay.MONDAY, 4, 7, 8, "Çocuk Diş Hekimliği"),
        clinic(ScheduleDay.TUESDAY, 4, 1, 4),
        course(ScheduleDay.TUESDAY, 4, 5, 6, "Ağız Diş ve Çene Radyolojisi"),
        course(ScheduleDay.TUESDAY, 4, 7, 8, "Periodontoloji"),
        clinic(ScheduleDay.WEDNESDAY, 4, 1, 4),
        course(ScheduleDay.WEDNESDAY, 4, 5, 6, "Ağız Diş Çene Hastalıkları"),
        course(ScheduleDay.WEDNESDAY, 4, 7, 8, "Protetik Diş Tedavisi"),
        clinic(ScheduleDay.THURSDAY, 4, 1, 4),
        course(ScheduleDay.THURSDAY, 4, 5, 6, "Ortodonti"),
        course(ScheduleDay.THURSDAY, 4, 7, 7, "Endodonti"),
        clinic(ScheduleDay.FRIDAY, 4, 1, 4),
        course(ScheduleDay.FRIDAY, 4, 6, 7, "Ağız Diş ve Çene Cerrahisi"),

        // 5. sınıf
        course(ScheduleDay.MONDAY, 5, 1, 1, "İmplantüstü Protezler ve Çene-Yüz Protezi"),
        clinic(ScheduleDay.MONDAY, 5, 2, 8),
        course(ScheduleDay.TUESDAY, 5, 1, 1, "Çene-Yüz Cerrahisi"),
        clinic(ScheduleDay.TUESDAY, 5, 2, 8),
        course(ScheduleDay.WEDNESDAY, 5, 1, 1, "İlk Yardım ve Acil Tedavi"),
        clinic(ScheduleDay.WEDNESDAY, 5, 2, 8),
        clinic(ScheduleDay.THURSDAY, 5, 2, 8),
        course(ScheduleDay.FRIDAY, 5, 2, 2, "Halk Sağlığı"),
        course(ScheduleDay.FRIDAY, 5, 3, 3, "Adli Tıp")
    )

    fun schedulesFor(department: String?): List<ClassSchedule>? {
        if (department != ClassSchedules.DENTAL_DEPARTMENT) return null
        return (1..5).map { year ->
            val classYear = when (year) {
                1 -> ClassSchedules.FIRST_YEAR
                2 -> ClassSchedules.SECOND_YEAR
                3 -> ClassSchedules.THIRD_YEAR
                4 -> ClassSchedules.FOURTH_YEAR
                else -> ClassSchedules.FIFTH_YEAR
            }
            ClassSchedule(
                faculty = ClassSchedules.DENTAL_FACULTY,
                department = ClassSchedules.DENTAL_DEPARTMENT,
                classYear = classYear,
                academicYear = ACADEMIC_YEAR,
                term = TERM,
                updatedAt = UPDATED_AT,
                sourcePage = 1,
                sourceUrl = SOURCE_URL,
                sourceWarning = SOURCE_WARNING,
                entries = blocks.asSequence()
                    .filter { it.year == year }
                    .flatMap { block ->
                        periods.subList(block.firstPeriod - 1, block.lastPeriod).asSequence().map { period ->
                            ScheduleEntry(
                                day = block.day,
                                startTime = period.start,
                                endTime = period.end,
                                courseCode = "",
                                courseName = block.name,
                                instructor = "",
                                classroom = if (block.courseType == "Ders" || block.courseType == "Seçmeli") {
                                    amphi(year)
                                } else {
                                    ""
                                },
                                courseType = block.courseType,
                                note = block.note
                            )
                        }
                    }
                    .sortedWith(compareBy<ScheduleEntry>({ it.day.ordinal }, { it.startTime }))
                    .toList()
            )
        }
    }
}
