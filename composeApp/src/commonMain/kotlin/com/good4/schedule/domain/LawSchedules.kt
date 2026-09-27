package com.good4.schedule.domain

/** Akdeniz Üniversitesi Hukuk Fakültesinin 2026-2027 yıllık ders çizelgesi. */
internal object LawSchedules {
    private const val SOURCE_URL =
        "https://webis.akdeniz.edu.tr/uploads/1055/event/ders%20program%C4%B1.pdf"
    private const val ACADEMIC_YEAR = "2026-2027"
    private const val TERM = "Güz - Bahar Yıllık"
    private const val UPDATED_AT = "2026-08-28"
    private const val SOURCE_WARNING =
        "Kaynak çizelgede öğretim elemanı bilgileri yer almadığından hoca adları gösterilmiyor."

    private val classPeriods = listOf(
        "08:30" to "09:20", "09:30" to "10:20", "10:30" to "11:20",
        "11:30" to "12:20", "12:30" to "13:20", "13:30" to "14:20",
        "14:30" to "15:20", "15:30" to "16:20", "16:30" to "17:20",
        "17:30" to "18:20"
    )

    private data class Block(
        val day: ScheduleDay,
        val start: String,
        val end: String,
        val courseName: String,
        val classroom: String = ""
    )

    private fun block(
        day: ScheduleDay,
        start: String,
        end: String,
        courseName: String,
        classroom: String = ""
    ) = Block(day, start, end, courseName, classroom)

    private fun entries(blocks: List<Block>): List<ScheduleEntry> = blocks.flatMap { block ->
        classPeriods.asSequence()
            .filter { (start, end) -> start >= block.start && end <= block.end }
            .map { (start, end) ->
                ScheduleEntry(
                    day = block.day,
                    startTime = start,
                    endTime = end,
                    courseCode = "",
                    courseName = block.courseName,
                    instructor = "",
                    classroom = block.classroom,
                    courseType = "Ders"
                )
            }
            .toList()
    }

    private val schedulesByYear = listOf(
        listOf(
            // 1. Sınıf
            block(ScheduleDay.MONDAY, "08:30", "10:20", "İktisat", "Amfi 1"),
            block(ScheduleDay.MONDAY, "10:30", "12:20", "Anayasa Hukuku", "Amfi 1"),
            block(ScheduleDay.MONDAY, "13:30", "15:20", "Medeni Hukuk", "Amfi 1"),
            block(ScheduleDay.MONDAY, "15:30", "17:20", "Sanat ve Hukuk (G)", "Amfi 1"),
            block(ScheduleDay.TUESDAY, "08:30", "10:20", "İngilizce", "Amfi 1"),
            block(ScheduleDay.TUESDAY, "10:30", "12:20", "Atatürk İlkeleri ve İnkılap Tarihi", "Amfi 1"),
            block(ScheduleDay.TUESDAY, "13:30", "15:20", "Anayasa Hukuku", "Amfi 1"),
            block(ScheduleDay.WEDNESDAY, "08:30", "10:20", "Siyaset Bilimine Giriş (G) / Kişilerarası İletişim (G)", "Amfi 1 / Derslik 7"),
            block(ScheduleDay.WEDNESDAY, "10:30", "12:20", "Siyasi Tarih I (G) / Siyasi Tarih II (B)", "Amfi 1"),
            block(ScheduleDay.WEDNESDAY, "13:30", "15:20", "Medeni Hukuk", "Amfi 1"),
            block(ScheduleDay.WEDNESDAY, "15:30", "17:20", "Türk Dili", "Amfi 1"),
            block(ScheduleDay.THURSDAY, "09:30", "12:20", "Roma Hukuku", "Amfi 1"),
            block(ScheduleDay.THURSDAY, "13:30", "14:20", "Klavye Kullanımı Teknik (B) / Diksiyon (G) / Bilim Felsefesi (B)", "LAB-4 / Amfi 1 / Amfi 8 / Derslik 7"),
            block(ScheduleDay.THURSDAY, "14:30", "15:20", "Klavye Kullanımı Teknik (B) / Diksiyon (G) / Özel Hukuk Tüzel Kişiler (B) / Bilim Felsefesi (B)", "LAB-4 / Amfi 1 / Amfi 8 / Derslik 7"),
            block(ScheduleDay.THURSDAY, "15:30", "16:20", "Bilimsel Araştırma Teknikleri (B) / Özel Hukuk Tüzel Kişiler (B)", "Amfi 1"),
            block(ScheduleDay.THURSDAY, "16:30", "17:20", "Bilimsel Araştırma Teknikleri (B)", "Amfi 1"),
            block(ScheduleDay.FRIDAY, "10:30", "12:20", "Hukuk Başlangıcı", "Amfi 1"),
            block(ScheduleDay.FRIDAY, "14:30", "15:20", "Kariyer Planlama (G)", "Amfi 1")
        ),
        listOf(
            // 2. Sınıf
            block(ScheduleDay.MONDAY, "08:30", "10:20", "Yargı Örgütü (G)", "Amfi 2"),
            block(ScheduleDay.MONDAY, "10:30", "12:20", "Borçlar Hukuku Genel Hükümler", "Amfi 2"),
            block(ScheduleDay.MONDAY, "13:30", "15:20", "Mali Hukuk", "Amfi 2"),
            block(ScheduleDay.MONDAY, "15:30", "17:20", "Anayasa Yargısı (G) / Seçim Hukuku (B)", "Amfi 2"),
            block(ScheduleDay.TUESDAY, "08:30", "10:20", "Türk Hukuk Tarihi", "Amfi 2"),
            block(ScheduleDay.TUESDAY, "10:30", "12:20", "Borçlar Hukuku Genel Hükümler", "Amfi 2"),
            block(ScheduleDay.TUESDAY, "13:30", "15:20", "Hukuk Metodolojisi (G) / Hukuk Felsefesi Tarihi (B)", "Amfi 2"),
            block(ScheduleDay.TUESDAY, "15:30", "17:20", "Türkiye Ekonomisi (G)", "Amfi 2"),
            block(ScheduleDay.WEDNESDAY, "09:30", "12:20", "Ceza Hukuku Genel Hükümler", "Amfi 2"),
            block(ScheduleDay.WEDNESDAY, "13:30", "15:20", "Genel Kamu Hukuku", "Amfi 2"),
            block(ScheduleDay.WEDNESDAY, "15:30", "17:20", "Adalet Psikolojisi (G) / Siyasi Partiler Hukuku (G) / İslam Hukukunun Genel Esasları (B) / Gönüllülük Çalışmaları (B)", "Amfi 2 / Derslik 7"),
            block(ScheduleDay.THURSDAY, "09:30", "12:20", "Uluslararası Kamu Hukuku", "Amfi 2"),
            block(ScheduleDay.THURSDAY, "13:30", "15:20", "Bilgi Teknolojileri Kullanımı", "Sanal-1"),
            block(ScheduleDay.THURSDAY, "15:30", "17:20", "Çocuk Hakları Hukuku (B)", "Amfi 2"),
            block(ScheduleDay.FRIDAY, "10:30", "12:20", "Yapay Zeka ve Hukuk (B)", "Amfi 2"),
            block(ScheduleDay.FRIDAY, "14:30", "17:20", "İdare Hukuku", "Amfi 2")
        ),
        listOf(
            // 3. Sınıf
            block(ScheduleDay.MONDAY, "08:30", "10:20", "Medeni Usul Hukuku", "Amfi 3"),
            block(ScheduleDay.MONDAY, "10:30", "12:20", "Hukukta Kadın (G) / Tüketici Hukuku (B)", "Amfi 3"),
            block(ScheduleDay.MONDAY, "14:30", "17:20", "Borçlar Hukuku Özel Hükümler", "Amfi 3"),
            block(ScheduleDay.TUESDAY, "08:30", "11:20", "Eşya Hukuku", "Amfi 3"),
            block(ScheduleDay.TUESDAY, "13:30", "15:20", "Vergi Hukuku", "Amfi 3"),
            block(ScheduleDay.TUESDAY, "15:30", "17:20", "Uluslararası Örgütler Hukuku (G) / Avrupa Birliği Kurumsal Hukuku (B) / Kişisel Verilerin Korunması (G) / Sivil Havacılık Hukuku (B) / Hukuk Kliniği I (B) / Otonom Araçlarda Sorumluluk ve Sigorta (B) / Uluslararası Deniz Hukuku ve Türkiye (G)", "Amfi 1 / Amfi 3 / Derslik 7 / Amfi 8"),
            block(ScheduleDay.WEDNESDAY, "08:30", "10:20", "Medeni Usul Hukuku", "Amfi 3"),
            block(ScheduleDay.WEDNESDAY, "10:30", "12:20", "Ticaret Hukuku I (A ve B şubesi)", "Amfi 3 / Amfi 8"),
            block(ScheduleDay.WEDNESDAY, "13:30", "16:20", "Ceza Hukuku Özel Hükümler", "Amfi 3"),
            block(ScheduleDay.WEDNESDAY, "16:30", "17:20", "Özel Öğretim Yöntemleri (G-B)", "Amfi 2"),
            block(ScheduleDay.THURSDAY, "08:30", "10:20", "İmar ve Çevre Hukuku (G) / Kooperatif Hukuku (G)", "Amfi 3 / Derslik 5"),
            block(ScheduleDay.THURSDAY, "10:30", "12:20", "Ticaret Hukuku I (A ve B şubesi)", "Amfi 3 / Amfi 8"),
            block(ScheduleDay.THURSDAY, "13:30", "16:20", "Hukuk Felsefesi ve Sosyolojisi", "Amfi 3"),
            block(ScheduleDay.FRIDAY, "08:30", "10:20", "Karşılaştırmalı Hukuk (B)", "Amfi 3"),
            block(ScheduleDay.FRIDAY, "10:30", "12:20", "İdari Yargılama Hukuku", "Amfi 3"),
            block(ScheduleDay.FRIDAY, "13:30", "14:20", "Kriminoloji (G) / Bilişim Hukuku (B)", "Amfi 3 / Derslik 7"),
            block(ScheduleDay.FRIDAY, "14:30", "16:20", "Kriminoloji (G) / Hukuk ve Etik (G) / Bilişim Hukuku (B)", "Amfi 3 / Derslik 7"),
            block(ScheduleDay.FRIDAY, "16:30", "17:20", "Hukuk ve Etik (G)", "Derslik 7")
        ),
        listOf(
            // 4. Sınıf
            block(ScheduleDay.MONDAY, "08:30", "10:20", "Avukatlık ve Noterlik (B)", "Amfi 4"),
            block(ScheduleDay.MONDAY, "10:30", "12:20", "İcra-İflas Hukuku", "Amfi 4"),
            block(ScheduleDay.MONDAY, "13:30", "16:20", "Milletlerarası Özel Hukuk", "Amfi 4"),
            block(ScheduleDay.MONDAY, "16:30", "18:20", "Öğretmenlik Uygulaması (B)", "Amfi 4 / Derslik 6 / Derslik 7"),
            block(ScheduleDay.TUESDAY, "08:30", "10:20", "İcra-İflas Hukuku", "Amfi 4"),
            block(ScheduleDay.TUESDAY, "10:30", "12:20", "Miras Hukuku", "Amfi 4"),
            block(ScheduleDay.TUESDAY, "13:30", "15:20", "Ticaret Hukuku (A ve B şubesi)", "Amfi 4 / Amfi 7"),
            block(ScheduleDay.TUESDAY, "15:30", "18:20", "Fikri Haklar (G) / Sınai Haklar (B) / İnternet Hukuku (G) / Toplu İş Hukuku (B)", "Amfi 4 / Amfi 7"),
            block(ScheduleDay.WEDNESDAY, "08:30", "10:20", "Medya Hukuku (B)", "Amfi 4"),
            block(ScheduleDay.WEDNESDAY, "10:30", "12:20", "İş ve Sosyal Güvenlik Hukuku (A ve B şubesi)", "Amfi 4 / Amfi 7"),
            block(ScheduleDay.WEDNESDAY, "13:30", "15:20", "İnsan Hakları Hukuku", "Amfi 4"),
            block(ScheduleDay.WEDNESDAY, "15:30", "17:20", "Girişimcilik (B) / Yolcu Yük Taşıma Hukuku I (G) / Arabuluculuk (B) / Mesleki Almanca 4 (B) / Tebligat / İnfaz Hukuku", "Amfi 4 / Derslik 5 / Derslik 8"),
            block(ScheduleDay.THURSDAY, "09:30", "12:20", "Deniz Ticareti ve Sigorta Hukuku (A ve B şubesi)", "Amfi 4 / Amfi 7"),
            block(ScheduleDay.THURSDAY, "13:30", "14:20", "Adli Tıp", "Amfi 4"),
            block(ScheduleDay.THURSDAY, "15:30", "17:20", "E-Devlet ve UYAP (B) / Kriminalistik (B) / Türk Hukukunda Trafik Sigortaları (B) / Hukuk Kliniği II (G) / Yolcu Yük Taşıma Hukuku II (B) / Yargı Etiği (B) / Tasarım Hukuku (G)", "LAB-4 / Amfi 4 / Derslik 6 / Derslik 7 / Amfi 8"),
            block(ScheduleDay.FRIDAY, "08:30", "10:20", "Sermaye Piyasası Hukuku (B)", "Amfi 4"),
            block(ScheduleDay.FRIDAY, "10:30", "12:20", "İş ve Sosyal Güvenlik Hukuku (A ve B şubesi)", "Amfi 4 / Amfi 7"),
            block(ScheduleDay.FRIDAY, "13:30", "17:20", "Ceza Muhakemesi Hukuku", "Amfi 4 / Derslik 5")
        )
    )

    fun schedulesFor(department: String?): List<ClassSchedule>? {
        if (department != ClassSchedules.LAW_DEPARTMENT) return null
        return schedulesByYear.mapIndexed { index, blocks ->
            ClassSchedule(
                faculty = ClassSchedules.LAW_FACULTY,
                department = ClassSchedules.LAW_DEPARTMENT,
                classYear = when (index) {
                    0 -> ClassSchedules.FIRST_YEAR
                    1 -> ClassSchedules.SECOND_YEAR
                    2 -> ClassSchedules.THIRD_YEAR
                    else -> ClassSchedules.FOURTH_YEAR
                },
                academicYear = ACADEMIC_YEAR,
                term = TERM,
                updatedAt = UPDATED_AT,
                sourcePage = 1,
                sourceUrl = SOURCE_URL,
                entries = entries(blocks),
                sourceWarning = SOURCE_WARNING
            )
        }
    }
}
