package com.good4.schedule.domain

/** Su Ürünleri Fakültesinin 2026–2027 güz dönemi güncel lisans programı. */
internal object FisheriesSchedules {
    private const val ACADEMIC_YEAR = "2026-2027"
    private const val TERM = "Güz Yarıyılı"
    private const val UPDATED_AT = "Güncel PDF · 2026-2027 Güz"
    private const val SOURCE_URL =
        "https://webis.akdeniz.edu.tr/uploads/1105/announcement/2026-2027%20G%C3%9CZ%20DERS%20PROGRAMI-G%C3%9CNCEL.pdf"

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
        CourseBlock(ScheduleDay.MONDAY, 1, 4, 4, "SÜM 109", "Su Ürünleri Mühendisliğine Giriş", "Dr. Öğr. Ü. M. Özbaş", "Z-1"),
        CourseBlock(ScheduleDay.MONDAY, 1, 5, 6, "SÜM 101", "Kimya", "Prof. Dr. Edip Bayram", "Z-1"),
        CourseBlock(ScheduleDay.MONDAY, 1, 7, 8, "SÜM 101", "Kimya Uygulaması", "Prof. Dr. Edip Bayram", "Öğrenci Laboratuvarı 2", "Uygulama"),
        CourseBlock(ScheduleDay.TUESDAY, 1, 1, 2, "YBD 101", "İngilizce I", "Doç. Dr. Abdullah Arslan", "Z-1"),
        CourseBlock(ScheduleDay.TUESDAY, 1, 5, 6, "ATA 101", "Atatürk İlkeleri ve İnkılap Tarihi I", "Öğr. Gör. Dr. Fatma Çetin Adıgüzel", "Z-6"),
        CourseBlock(ScheduleDay.TUESDAY, 1, 7, 7, "KPD 101", "Kariyer Planlama", "Doç. Dr. B. A. Balcı", "Z-1"),
        CourseBlock(ScheduleDay.WEDNESDAY, 1, 3, 4, "SÜM 103", "Fizik", "Prof. Dr. Rıza Erdem", "Z-1"),
        CourseBlock(ScheduleDay.WEDNESDAY, 1, 5, 6, "TDB 115", "Akademik Türkçe", "Öğr. Gör. Dürüye Kara", "Sanal Sınıf"),
        CourseBlock(ScheduleDay.WEDNESDAY, 1, 7, 8, "SÜM 105", "Matematik I", "Doç. Dr. Serap Kemali", "Z-1"),
        CourseBlock(ScheduleDay.THURSDAY, 1, 1, 2, "SÜM 107", "Genel Biyoloji", "Doç. Dr. İ. Tülay Çağatay", "Z-1"),
        CourseBlock(ScheduleDay.THURSDAY, 1, 3, 4, "SÜM 107", "Genel Biyoloji Uygulaması", "Doç. Dr. İ. Tülay Çağatay", "Öğrenci Laboratuvarı 4", "Uygulama"),
        CourseBlock(ScheduleDay.THURSDAY, 1, 5, 6, "ENF 107", "Bilgi Teknolojileri", "Öğr. Gör. Şahin Akbunar", "Sanal Sınıf"),
        CourseBlock(ScheduleDay.THURSDAY, 1, 7, 8, "TDB 101", "Türk Dili I", "Öğr. Gör. Dr. Bilal Nargöz", "Z-6"),
        CourseBlock(ScheduleDay.FRIDAY, 1, 6, 7, "", "Hobi Dersleri 1", "", "", "Seçmeli", "İsteğe bağlı seçmeli"),

        // 2. sınıf
        CourseBlock(ScheduleDay.MONDAY, 2, 2, 2, "SÜM 245", "Balık Anatomisi ve Fizyolojisi Uygulaması", "Prof. Dr. Z. Arzu Becer Öcal", "Öğrenci Laboratuvarı 4", "Uygulama"),
        CourseBlock(ScheduleDay.MONDAY, 2, 3, 4, "SÜM 245", "Balık Anatomisi ve Fizyolojisi", "Prof. Dr. Z. Arzu Becer Öcal", "Z-2"),
        CourseBlock(ScheduleDay.MONDAY, 2, 5, 6, "SÜM 221", "Canlı Yem Üretimi", "Dr. Öğr. Ü. Mehmet Özbaş", "Z-2"),
        CourseBlock(ScheduleDay.MONDAY, 2, 7, 8, "SÜM 205", "Balık Biyolojisi", "Prof. Dr. Z. Arzu Becer Öcal", "Z-2"),
        CourseBlock(ScheduleDay.TUESDAY, 2, 3, 4, "SÜM 263", "Su Ürünlerinde Bilimsel Araştırma Yöntemleri", "Doç. Dr. İ. Tülay Çağatay", "Z-2"),
        CourseBlock(ScheduleDay.TUESDAY, 2, 5, 6, "SÜM 207", "Genel Mikrobiyoloji", "Prof. Dr. Jale Korun", "Z-2"),
        CourseBlock(ScheduleDay.TUESDAY, 2, 7, 8, "SÜM 207", "Genel Mikrobiyoloji Uygulaması", "Prof. Dr. Jale Korun", "Öğrenci Laboratuvarı 6", "Uygulama"),
        CourseBlock(ScheduleDay.WEDNESDAY, 2, 2, 2, "SÜM 217", "İş Sağlığı ve Güvenliği", "Doç. Dr. Ahmet Coşgun", "Z-2"),
        CourseBlock(ScheduleDay.WEDNESDAY, 2, 3, 4, "SÜM 203", "Biyokimya", "Prof. Dr. Pınar Yerlikaya Kebapçıoğlu", "Z-2"),
        CourseBlock(ScheduleDay.WEDNESDAY, 2, 5, 6, "SÜM 215", "Su Ürünlerinde Gıda Laboratuvar Tekniği", "Prof. Dr. Pınar Yerlikaya Kebapçıoğlu", "Z-2"),
        CourseBlock(ScheduleDay.WEDNESDAY, 2, 7, 7, "SÜM 213", "Sucul Omurgasızlar", "Doç. Dr. Olgaç Güven", "Z-2"),
        CourseBlock(ScheduleDay.WEDNESDAY, 2, 8, 8, "SÜM 213", "Sucul Omurgasızlar Uygulaması", "Doç. Dr. Olgaç Güven", "Öğrenci Laboratuvarı 4", "Uygulama"),
        CourseBlock(ScheduleDay.THURSDAY, 2, 3, 4, "SÜM 211", "Deniz Meteorolojisi ve Navigasyon", "Prof. Dr. M. Cengiz Deval", "Z-2"),
        CourseBlock(ScheduleDay.THURSDAY, 2, 5, 6, "SÜM 211", "Deniz Meteorolojisi ve Navigasyon Uygulaması", "Prof. Dr. M. Cengiz Deval", "Teknik Çizim Odası", "Uygulama"),
        CourseBlock(ScheduleDay.THURSDAY, 2, 7, 8, "SÜM 201", "İstatistik", "Doç. Dr. Baki Aydın", "Z-2"),

        // 3. sınıf
        CourseBlock(ScheduleDay.MONDAY, 3, 2, 3, "SÜM 309", "İç Su Balıkları Yetiştiriciliği", "Prof. Dr. Süleyman Akhan", "Z-3"),
        CourseBlock(ScheduleDay.MONDAY, 3, 4, 4, "SÜM 309", "İç Su Balıkları Yetiştiriciliği Uygulaması", "Prof. Dr. Süleyman Akhan", "Öğrenci Laboratuvarı 5", "Uygulama"),
        CourseBlock(ScheduleDay.MONDAY, 3, 5, 5, "SÜM 303", "Av Araçları ve Avlama Yöntemleri Uygulaması", "Doç. Dr. Turhan Kebapçıoğlu", "Araştırma Laboratuvarı 6", "Uygulama"),
        CourseBlock(ScheduleDay.MONDAY, 3, 6, 7, "SÜM 303", "Av Araçları ve Avlama Yöntemleri", "Doç. Dr. Turhan Kebapçıoğlu", "Z-3"),
        CourseBlock(ScheduleDay.TUESDAY, 3, 2, 3, "SÜM 301", "Akvaryum Balıkları Yetiştiriciliği", "Prof. Dr. Erkan Gümüş", "Z-3"),
        CourseBlock(ScheduleDay.TUESDAY, 3, 4, 4, "SÜM 301", "Akvaryum Balıkları Yetiştiriciliği Uygulaması", "Prof. Dr. Erkan Gümüş", "Akvaryum Ünitesi", "Uygulama"),
        CourseBlock(ScheduleDay.TUESDAY, 3, 5, 6, "SÜM 323", "Su Ürünleri Malzeme Bilgisi ve Mekanizasyon", "Doç. Dr. B. Ahmet Balcı", "Z-3"),
        CourseBlock(ScheduleDay.TUESDAY, 3, 7, 8, "SÜM 313", "İç Su Balıkları Avcılığı", "Dr. Öğr. Ü. Cenkmen R. Beğburs", "Z-3"),
        CourseBlock(ScheduleDay.WEDNESDAY, 3, 3, 4, "TDP 303", "Toplumsal Duyarlılık ve Katkı", "Doç. Dr. Mesut Yılmaz", "Z-3"),
        CourseBlock(ScheduleDay.WEDNESDAY, 3, 5, 6, "SÜM 307", "Su Ürünleri İşleme Teknolojisi I", "Prof. Dr. Nalan Gökoğlu", "Z-3"),
        CourseBlock(ScheduleDay.WEDNESDAY, 3, 7, 7, "SÜM 307", "Su Ürünleri İşleme Teknolojisi I Uygulaması", "Prof. Dr. Nalan Gökoğlu", "Öğrenci Laboratuvarı 1", "Uygulama"),
        CourseBlock(ScheduleDay.THURSDAY, 3, 3, 4, "SÜM 319", "Deniz Akvaryumları ve Yetiştiriciliği", "Doç. Dr. B. Ahmet Balcı", "Z-3"),
        CourseBlock(ScheduleDay.THURSDAY, 3, 5, 6, "SÜM 321", "Akuatik Farmakoloji", "Prof. Dr. Jale Korun", "Z-3"),
        CourseBlock(ScheduleDay.FRIDAY, 3, 1, 2, "SÜM 377", "Yarıyıl Stajı", "", "Z-3", note = "Eski öğrenciler için"),
        CourseBlock(ScheduleDay.FRIDAY, 3, 3, 4, "SÜM 379", "Mesleki Uygulama", "", "Z-3", note = "Eski öğrenciler için"),

        // 4. sınıf
        CourseBlock(ScheduleDay.MONDAY, 4, 2, 3, "SÜM 403", "Su Ürünleri İşleme Teknolojisi II", "Prof. Dr. Mustafa Ünlüsayın", "Z-4"),
        CourseBlock(ScheduleDay.MONDAY, 4, 4, 4, "SÜM 403", "Su Ürünleri İşleme Teknolojisi II Uygulaması", "Prof. Dr. Mustafa Ünlüsayın", "Öğrenci Laboratuvarı 1 / 6", "Uygulama"),
        CourseBlock(ScheduleDay.MONDAY, 4, 5, 6, "SÜM 405", "Sportif Balıkçılık", "Dr. Öğr. Ü. Cenkmen R. Beğburs", "Z-4"),
        CourseBlock(ScheduleDay.TUESDAY, 4, 3, 4, "SÜM 409", "Deniz Hukuku ve Su Ürünleri Mevzuatı", "Prof. Dr. Serpil Yılmaz", "Z-4"),
        CourseBlock(ScheduleDay.TUESDAY, 4, 5, 6, "SÜM 419", "Akuatik Moleküler Biyoloji ve Biyoteknoloji", "Doç. Dr. İ. Tülay Çağatay", "Z-4"),
        CourseBlock(ScheduleDay.WEDNESDAY, 4, 2, 3, "SÜM 407", "Balıkçılık Biyolojisi ve Popülasyon Dinamiği", "Prof. Dr. Z. Arzu Becer Öcal", "Z-4"),
        CourseBlock(ScheduleDay.WEDNESDAY, 4, 4, 4, "SÜM 407", "Balıkçılık Biyolojisi ve Popülasyon Dinamiği Uygulaması", "Prof. Dr. Z. Arzu Becer Öcal", "Öğrenci Laboratuvarı 4", "Uygulama"),
        CourseBlock(ScheduleDay.WEDNESDAY, 4, 5, 6, "SÜM 425", "Orkinos Yetiştirme Tekniği", "Prof. Dr. Mehmet Gökoğlu", "Z-4"),
        CourseBlock(ScheduleDay.WEDNESDAY, 4, 7, 8, "SÜM 457", "Yapay Zeka Uygulamaları", "Doç. Dr. Mesut Yılmaz", "Z-4"),
        CourseBlock(ScheduleDay.THURSDAY, 4, 2, 2, "SÜM 455", "Meslek Etiği", "Doç. Dr. B. Ahmet Balcı", ""),
        CourseBlock(ScheduleDay.THURSDAY, 4, 3, 4, "SÜM 491", "Yük İstifi", "Doç. Dr. Turhan Kebapçıoğlu", "Z-4"),
        CourseBlock(ScheduleDay.THURSDAY, 4, 5, 6, "SÜM 459", "Teknik İngilizce", "Doç. Dr. Baki Aydın", "Z-4"),
        CourseBlock(ScheduleDay.FRIDAY, 4, 5, 8, "SÜM 413", "Mühendislikte Tasarım", "Fakülte Öğretim Üyeleri", "Z-4", note = "PDF notu: Prof. Dr. Jale Korun, Dekan")
    )

    fun schedulesFor(department: String?): List<ClassSchedule>? {
        if (department != ClassSchedules.FISHERIES_ENGINEERING_DEPARTMENT) return null
        return (1..4).map { year ->
            ClassSchedule(
                faculty = ClassSchedules.FISHERIES_FACULTY,
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
