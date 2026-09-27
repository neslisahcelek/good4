package com.good4.schedule.domain

/** İlahiyat Fakültesinin 2026–2027 güz yarıyılı lisans programı. */
internal object TheologySchedules {
    private const val ACADEMIC_YEAR = "2026-2027"
    private const val TERM = "Güz Yarıyılı"
    private const val UPDATED_AT = "2026-2027 Güz · resmî PDF"
    private const val SOURCE_URL =
        "https://webis.akdeniz.edu.tr/uploads/1027/files/53f70545-3d76-40bf-91cc-376de9e06952.pdf"
    private const val PREPARATORY_YEAR_NOTE =
        "Kaynak PDF'de hazırlık sınıfları da bulunuyor; uygulamadaki sınıf seçenekleri 1–4 olduğu için burada lisans sınıfları gösteriliyor."

    private data class Period(val start: String, val end: String)

    private data class CourseBlock(
        val day: ScheduleDay,
        val year: Int,
        val firstPeriod: Int,
        val lastPeriod: Int,
        val name: String,
        val instructor: String,
        val section: String? = null,
        val classroom: String = "",
        val note: String? = null
    )

    private val weekdayPeriods = listOf(
        Period("08:30", "09:20"),
        Period("09:30", "10:20"),
        Period("10:30", "11:20"),
        Period("11:30", "12:20"),
        Period("13:30", "14:20"),
        Period("14:30", "15:20"),
        Period("15:30", "16:20"),
        Period("16:30", "17:20")
    )
    private val fridayPeriods = listOf(
        Period("08:30", "09:20"),
        Period("09:30", "10:20"),
        Period("10:30", "11:20"),
        Period("11:30", "12:20"),
        Period("14:30", "15:20"),
        Period("15:30", "16:20"),
        Period("16:30", "17:20"),
        Period("17:30", "18:20")
    )

    private fun room(year: Int, section: String): String = when (year to section) {
        1 to "A" -> "A-117"
        1 to "B" -> "B-118"
        2 to "A" -> "A-329"
        2 to "B" -> "A-330"
        3 to "A" -> "A-122"
        3 to "B" -> "A-123"
        4 to "A" -> "A-334"
        4 to "B" -> "B-335"
        else -> ""
    }

    private fun section(
        day: ScheduleDay,
        year: Int,
        firstPeriod: Int,
        lastPeriod: Int,
        section: String,
        name: String,
        instructor: String = "",
        note: String? = null
    ) = CourseBlock(
        day = day,
        year = year,
        firstPeriod = firstPeriod,
        lastPeriod = lastPeriod,
        name = name,
        instructor = instructor,
        section = "$section Şubesi",
        classroom = room(year, section),
        note = note
    )

    private fun shared(
        day: ScheduleDay,
        year: Int,
        firstPeriod: Int,
        lastPeriod: Int,
        name: String,
        instructor: String = "",
        note: String? = null
    ) = CourseBlock(day, year, firstPeriod, lastPeriod, name, instructor, note = note)

    private val blocks = listOf(
        // 1. sınıf — Pazartesi
        section(ScheduleDay.MONDAY, 1, 1, 2, "A", "Siyer", "Dr. Öğr. Üyesi Furkan Erbaş"),
        section(ScheduleDay.MONDAY, 1, 1, 2, "B", "Kur'an Okuma ve Tecvid I", "Öğr. Gör. Dr. Hasan Alkan"),
        section(ScheduleDay.MONDAY, 1, 3, 4, "A", "Kur'an Okuma ve Tecvid I", "Öğr. Gör. Dr. Süleyman Aykut"),
        section(ScheduleDay.MONDAY, 1, 3, 4, "B", "Siyer", "Dr. Öğr. Üyesi Furkan Erbaş"),
        section(ScheduleDay.MONDAY, 1, 5, 6, "C", "Kur'an Okuma ve Tecvid I", "Ayşegül Özcan"),

        // 1. sınıf — Salı
        section(ScheduleDay.TUESDAY, 1, 1, 2, "A", "Tefsir Tarihi ve Usulü", "Dr. Öğr. Üyesi Hatice Teber"),
        section(ScheduleDay.TUESDAY, 1, 1, 2, "B", "Kur'an Okuma ve Tecvid I", "Öğr. Gör. Dr. Hasan Alkan"),
        section(ScheduleDay.TUESDAY, 1, 3, 4, "A", "Kur'an Okuma ve Tecvid I", "Öğr. Gör. Dr. Süleyman Aykut"),
        section(ScheduleDay.TUESDAY, 1, 3, 4, "B", "Tefsir Tarihi ve Usulü", "Dr. Öğr. Üyesi Hatice Teber"),
        section(ScheduleDay.TUESDAY, 1, 3, 4, "C", "Kur'an Okuma ve Tecvid I", "Ayşegül Özcan"),
        shared(ScheduleDay.TUESDAY, 1, 5, 6, "İngilizce I", "Öğr. Gör. Esra Dönüş"),
        shared(ScheduleDay.TUESDAY, 1, 7, 8, "Atatürk İlkeleri ve İnkılap Tarihi I", "Öğr. Gör. Dr. Ahmet Kısa"),

        // 1. sınıf — Çarşamba
        section(ScheduleDay.WEDNESDAY, 1, 1, 2, "B", "Arapça I", "Öğr. Gör. Dr. Muhammed Özcan"),
        section(ScheduleDay.WEDNESDAY, 1, 3, 4, "A", "Siyer", "Dr. Öğr. Üyesi Furkan Erbaş"),
        section(ScheduleDay.WEDNESDAY, 1, 3, 4, "B", "İslam İnanç Esasları", "Prof. Dr. Ali Kürşat Turgut"),
        section(ScheduleDay.WEDNESDAY, 1, 5, 6, "A", "Arapça I", "Dr. Öğr. Üyesi Turan Bahşi"),
        section(ScheduleDay.WEDNESDAY, 1, 5, 6, "B", "Siyer", "Dr. Öğr. Üyesi Furkan Erbaş"),
        shared(ScheduleDay.WEDNESDAY, 1, 7, 8, "Hadis İlimleri ve Usulü I", "Prof. Dr. Zişan Türcan"),

        // 1. sınıf — Perşembe
        section(ScheduleDay.THURSDAY, 1, 1, 2, "A", "Tefsir Tarihi ve Usulü", "Dr. Öğr. Üyesi Hatice Teber"),
        section(ScheduleDay.THURSDAY, 1, 3, 4, "A", "İslam İnanç Esasları", "Prof. Dr. Sabri Yılmaz"),
        section(ScheduleDay.THURSDAY, 1, 3, 4, "B", "Tefsir Tarihi ve Usulü", "Dr. Öğr. Üyesi Hatice Teber"),
        shared(ScheduleDay.THURSDAY, 1, 6, 6, "Üniversite Hayatına Uyum", "Doç. Dr. Şeref Göküş"),
        shared(ScheduleDay.THURSDAY, 1, 7, 8, "Türk Dili I", "Öğr. Gör. Ali Karagöz"),

        // 2. sınıf — Pazartesi
        section(ScheduleDay.MONDAY, 2, 1, 2, "A", "Kur'an Okuma ve Tecvid III", "Öğr. Gör. Dr. Süleyman Aykut"),
        section(ScheduleDay.MONDAY, 2, 1, 2, "B", "Tefsir I", "Arş. Gör. Dr. Nazife Göksu"),
        section(ScheduleDay.MONDAY, 2, 1, 2, "C", "Kur'an Okuma ve Tecvid III", "Öğr. Gör. Dr. Fatma Dursun"),
        section(ScheduleDay.MONDAY, 2, 3, 4, "A", "Tefsir I", "Arş. Gör. Dr. Aziz Karabulut"),
        section(ScheduleDay.MONDAY, 2, 3, 4, "B", "Kur'an Okuma ve Tecvid III", "Öğr. Gör. Dr. Hasan Alkan"),
        section(ScheduleDay.MONDAY, 2, 3, 4, "C", "İtikadî İslam Mezhepleri", "Prof. Dr. Ömer Faruk Teber"),
        shared(ScheduleDay.MONDAY, 2, 5, 6, "Eğitime Giriş", "Doç. Dr. Mustafa Fatih Ay", "Kaynak hücresinde ayrıca '=12:30' notu bulunuyor."),
        shared(ScheduleDay.MONDAY, 2, 7, 8, "Din Sosyolojisi", "Prof. Dr. Bahset Karslı"),

        // 2. sınıf — Salı
        section(ScheduleDay.TUESDAY, 2, 1, 2, "A", "İslam Hukuk Usulü", "Dr. Öğr. Ü. Zeki Yaka"),
        section(ScheduleDay.TUESDAY, 2, 1, 2, "B", "İslam Tarihi II", "Dr. Öğr. Üyesi Muhammet Fatih Duman"),
        section(ScheduleDay.TUESDAY, 2, 3, 4, "A", "İslam Tarihi II", "Dr. Öğr. Üyesi Muhammet Fatih Duman"),
        section(ScheduleDay.TUESDAY, 2, 3, 4, "B", "İslam Hukuk Usulü", "Dr. Öğr. Ü. Zeki Yaka"),
        section(ScheduleDay.TUESDAY, 2, 5, 6, "A", "Hadis I", "Dr. Öğr. Ü. Mehmet Dilek"),
        section(ScheduleDay.TUESDAY, 2, 5, 6, "B", "Hadis I", "Prof. Dr. Zişan Türcan"),
        shared(ScheduleDay.TUESDAY, 2, 7, 8, "İslam Tarihi I", "Dr. Öğr. Üyesi Muhammet Fatih Duman"),

        // 2. sınıf — Çarşamba
        section(ScheduleDay.WEDNESDAY, 2, 1, 2, "A", "Hadis I", "Dr. Öğr. Ü. Mehmet Dilek"),
        section(ScheduleDay.WEDNESDAY, 2, 1, 2, "B", "İslam Hukuk Usulü", "Dr. Öğr. Ü. Zeki Yaka"),
        section(ScheduleDay.WEDNESDAY, 2, 3, 4, "A", "İslam Hukuk Usulü", "Dr. Öğr. Ü. Zeki Yaka"),
        section(ScheduleDay.WEDNESDAY, 2, 3, 4, "B", "Hadis I", "Prof. Dr. Zişan Türcan"),
        shared(ScheduleDay.WEDNESDAY, 2, 5, 6, "Rehberlik ve Özel Eğitim", "Doç. Dr. Mehmet Çınar", "Kaynak hücresinde ayrıca '12:30-15:20' notu bulunuyor."),
        shared(ScheduleDay.WEDNESDAY, 2, 7, 8, "Arap Dili ve Belâgatı III", "Öğr. Gör. Dr. Kemal Şimşek"),

        // 2. sınıf — Perşembe
        section(ScheduleDay.THURSDAY, 2, 1, 2, "A", "Tefsir I", "Arş. Gör. Dr. Aziz Karabulut"),
        section(ScheduleDay.THURSDAY, 2, 1, 2, "B", "Dini Mûsiki", "Dr. Öğr. Üyesi İbrahim Odabaşı"),
        section(ScheduleDay.THURSDAY, 2, 3, 4, "A", "Kur'an Okuma ve Tecvid III", "Öğr. Gör. Dr. Süleyman Aykut"),
        section(ScheduleDay.THURSDAY, 2, 3, 4, "B", "Tefsir I", "Arş. Gör. Dr. Nazife Göksu"),
        section(ScheduleDay.THURSDAY, 2, 5, 7, "A", "Eğitim Psikolojisi", "Prof. Dr. Sema Eryücel"),
        section(ScheduleDay.THURSDAY, 2, 5, 7, "B", "Eğitim Psikolojisi", "Doç. Dr. Mehmet Çınar"),

        // 2. sınıf — Cuma
        section(ScheduleDay.FRIDAY, 2, 1, 2, "B", "Kur'an Okuma ve Tecvid III", "Öğr. Gör. Dr. Hasan Alkan"),
        section(ScheduleDay.FRIDAY, 2, 3, 4, "A", "Türk İslam Sanatları Tarihi", "Öğr. Gör. Dr. Osman Öztürk"),
        section(ScheduleDay.FRIDAY, 2, 3, 4, "B", "Mantık", "Dr. Öğr. Üyesi Mehmet Dugan"),
        section(ScheduleDay.FRIDAY, 2, 3, 4, "C", "Kur'an Okuma ve Tecvid III", "Öğr. Gör. Dr. Fatma Dursun"),
        section(ScheduleDay.FRIDAY, 2, 5, 6, "A", "Mantık", "Dr. Öğr. Üyesi Mehmet Dugan"),
        section(ScheduleDay.FRIDAY, 2, 5, 6, "B", "Türk İslam Sanatları Tarihi", "Öğr. Gör. Dr. Osman Öztürk"),
        section(ScheduleDay.FRIDAY, 2, 5, 6, "C", "Osmanlı Türkçesi", "Doç. Dr. Mehmet Şahin"),

        // 3. sınıf — Pazartesi
        section(ScheduleDay.MONDAY, 3, 1, 2, "A", "Fıkıh I", "Doç. Dr. Hasan Kayapınar"),
        section(ScheduleDay.MONDAY, 3, 1, 2, "B", "Hadis III", "Dr. Öğr. Ü. İsa Akalın"),
        section(ScheduleDay.MONDAY, 3, 3, 4, "A", "Hadis III", "Dr. Öğr. Ü. İsa Akalın"),
        section(ScheduleDay.MONDAY, 3, 3, 4, "B", "Fıkıh I", "Doç. Dr. Hasan Kayapınar"),
        section(ScheduleDay.MONDAY, 3, 5, 6, "A", "Arap Dili ve Belâgatı V", "Prof. Dr. N. Nihal İnce"),
        section(ScheduleDay.MONDAY, 3, 5, 6, "B", "Kur'an Okuma ve Tecvid V", "Öğr. Gör. Dr. Fatma Dursun"),

        // 3. sınıf — Salı
        section(ScheduleDay.TUESDAY, 3, 1, 2, "A", "Felsefe Tarihi", "Dr. Öğr. Üyesi Mehmet Fatih Deniz"),
        section(ScheduleDay.TUESDAY, 3, 1, 2, "B", "Dinler Tarihi I", "Dr. Öğr. Üy. Ayşe A. Ambaroğlu"),
        section(ScheduleDay.TUESDAY, 3, 3, 4, "A", "Dinler Tarihi I", "Dr. Öğr. Üy. Ayşe A. Ambaroğlu"),
        section(ScheduleDay.TUESDAY, 3, 3, 4, "B", "Felsefe Tarihi", "Dr. Öğr. Üyesi Mehmet Fatih Deniz"),
        shared(ScheduleDay.TUESDAY, 3, 5, 6, "Arapça Modern Metinler", "Doç. Dr. Encümen Bayram"),
        shared(ScheduleDay.TUESDAY, 3, 5, 6, "İslam Hukuk Sosyolojisi", "Dr. Öğr. Ü. Zeki Yaka"),
        shared(ScheduleDay.TUESDAY, 3, 5, 6, "Türk Musikisi Nazariyatı I", "Dr. Öğr. Üyesi İbrahim Odabaşı"),
        shared(ScheduleDay.TUESDAY, 3, 5, 6, "Dinlerde Misyonerlik Faaliyetleri", "Dr. Öğr. Üyesi Ayşe A. Ambaroğlu"),
        shared(ScheduleDay.TUESDAY, 3, 5, 6, "Tasavvuf Düşünürleri", "Doç. Dr. Şeyda Öztürk"),
        shared(ScheduleDay.TUESDAY, 3, 7, 8, "Öğretim Teknolojileri", "Doç. Dr. Şeref Göküş"),

        // 3. sınıf — Çarşamba
        section(ScheduleDay.WEDNESDAY, 3, 1, 2, "A", "Kelâm II", "Prof. Dr. Sabri Yılmaz"),
        section(ScheduleDay.WEDNESDAY, 3, 1, 2, "B", "Tasavvuf I", "Doç. Dr. Şeyda Öztürk"),
        section(ScheduleDay.WEDNESDAY, 3, 3, 4, "A", "Kur'an Okuma ve Tecvid V", "Öğr. Gör. Dr. Süleyman Aykut"),
        section(ScheduleDay.WEDNESDAY, 3, 3, 4, "B", "Kelâm II", "Prof. Dr. Sabri Yılmaz"),
        section(ScheduleDay.WEDNESDAY, 3, 3, 4, "C", "Kur'an Okuma ve Tecvid V", "Rıdvan Orhan"),
        shared(ScheduleDay.WEDNESDAY, 3, 5, 6, "İslam Eğitim Tarihi", "Doç. Dr. Şeref Göküş"),
        shared(ScheduleDay.WEDNESDAY, 3, 5, 6, "Farsça", "Doç. Dr. Mehmet Şahin"),
        shared(ScheduleDay.WEDNESDAY, 3, 5, 6, "İslam Mezhepleri Tarihi: Klasik Kaynaklar", "Prof. Dr. Ömer Faruk Teber"),
        shared(ScheduleDay.WEDNESDAY, 3, 5, 6, "Din Sanat ve Estetik", "Prof. Dr. Rıfat Atay"),
        shared(ScheduleDay.WEDNESDAY, 3, 5, 6, "Kur'an Tarihi", "Dr. Öğr. Ü. Eyüp Yaka"),

        // 3. sınıf — Perşembe
        section(ScheduleDay.THURSDAY, 3, 1, 2, "A", "Tasavvuf I", "Prof. Dr. Ahmet Ögke"),
        section(ScheduleDay.THURSDAY, 3, 1, 2, "B", "Tefsir III", "Dr. Öğr. Üyesi Eyüp Yaka"),
        section(ScheduleDay.THURSDAY, 3, 3, 4, "A", "Tefsir III", "Dr. Öğr. Üyesi Eyüp Yaka"),
        section(ScheduleDay.THURSDAY, 3, 3, 4, "B", "Arap Dili ve Belâgatı V", "Öğr. Gör. Dr. Sami Çakmakpınar"),
        shared(ScheduleDay.THURSDAY, 3, 5, 6, "İslam Düşünce Tarihinde Maturidilik", "Prof. Dr. Sabri Yılmaz"),
        shared(ScheduleDay.THURSDAY, 3, 5, 6, "Hüsn-i Hat", "Öğr. Gör. Dr. Osman Öztürk"),
        shared(ScheduleDay.THURSDAY, 3, 5, 6, "Sosyoloji Tarihi", "Prof. Dr. Bahset Karslı"),
        shared(ScheduleDay.THURSDAY, 3, 5, 6, "Peygamberler Tarihi", "Öğr. Gör. Dr. Hasan Alkan"),
        shared(ScheduleDay.THURSDAY, 3, 5, 6, "Tefsir Metinleri Tenkidi", "Öğr. Gör. Dr. Fatma Dursun"),

        // 3. sınıf — Cuma
        shared(ScheduleDay.FRIDAY, 3, 3, 4, "Hadisleri Anlama ve Yorumlama Yöntemleri", "Dr. Öğr. Ü. Mehmet Dilek"),
        shared(ScheduleDay.FRIDAY, 3, 3, 4, "Batıda İslam Düşüncesi Araştırmaları", "Prof. Dr. A. Kürşat Turgut"),
        shared(ScheduleDay.FRIDAY, 3, 3, 4, "Osmanlıca Felsefî Metinler", "Prof. Dr. Rıfat Atay"),
        shared(ScheduleDay.FRIDAY, 3, 3, 4, "Sosyal Psikoloji", "Prof. Dr. Sema Eryücel"),

        // 4. sınıf — Pazartesi
        shared(ScheduleDay.MONDAY, 4, 2, 4, "Özel Öğretim Yöntemleri", "Doç. Dr. Mustafa Fatih Ay", "Kaynakta ders hücresinde '=09:30' notu bulunuyor."),
        shared(ScheduleDay.MONDAY, 4, 5, 6, "Mukayeseli İslam Hukuku", "Dr. Öğr. Ü. Zeki Yaka"),
        shared(ScheduleDay.MONDAY, 4, 5, 6, "Hadis Problemleri", "Dr. Öğr. Ü. Mehmet Dilek"),
        shared(ScheduleDay.MONDAY, 4, 5, 6, "Türk Din Musikisi Repertuvarı I", "Dr. Öğr. Üyesi İbrahim Odabaşı"),
        section(ScheduleDay.MONDAY, 4, 7, 8, "B", "Hadis V", "Dr. Öğr. Üyesi İsa Akalın"),

        // 4. sınıf — Salı
        section(ScheduleDay.TUESDAY, 4, 1, 2, "A", "Kur'an Okuma ve Tecvid VII", "Öğr. Gör. Dr. Süleyman Aykut"),
        section(ScheduleDay.TUESDAY, 4, 1, 2, "B", "Fıkıh III", "Doç. Dr. Hasan Kayapınar"),
        section(ScheduleDay.TUESDAY, 4, 3, 4, "A", "Fıkıh III", "Doç. Dr. Hasan Kayapınar"),
        section(ScheduleDay.TUESDAY, 4, 3, 4, "B", "Kur'an Okuma ve Tecvid VII", "Öğr. Gör. Dr. Hasan Alkan"),
        shared(ScheduleDay.TUESDAY, 4, 5, 6, "Sınıf Yönetimi", "Doç. Dr. Şeref Göküş"),
        shared(ScheduleDay.TUESDAY, 4, 7, 8, "Arapça Kelam Metinleri", "Prof. Dr. Sabri Yılmaz"),
        shared(ScheduleDay.TUESDAY, 4, 7, 8, "İngilizce Felsefî Metinler", "Dr. Öğr. Üyesi Mehmet Fatih Deniz"),
        shared(ScheduleDay.TUESDAY, 4, 7, 8, "İslam Bilim Tarihi", "Dr. Öğr. Üyesi Mehmet Dugan"),
        shared(ScheduleDay.TUESDAY, 4, 7, 8, "Öğrenme Psikolojisi", "Doç. Dr. Mehmet Çınar"),

        // 4. sınıf — Çarşamba
        section(ScheduleDay.WEDNESDAY, 4, 1, 2, "A", "Hadis IV", "Prof. Dr. Nevzat Tartı"),
        section(ScheduleDay.WEDNESDAY, 4, 1, 2, "B", "Tefsir V", "Öğr. Gör. Dr. Fatma Dursun"),
        section(ScheduleDay.WEDNESDAY, 4, 3, 4, "A", "Tefsir V", "Öğr. Gör. Dr. Fatma Dursun"),
        section(ScheduleDay.WEDNESDAY, 4, 3, 4, "B", "Hadis IV", "Prof. Dr. Nevzat Tartı"),
        section(ScheduleDay.WEDNESDAY, 4, 5, 6, "A", "Mantık Yanlışları", "Dr. Öğr. Üyesi Mehmet Dugan"),
        section(ScheduleDay.WEDNESDAY, 4, 5, 6, "B", "Mantık Yanlışları", "Dr. Öğr. Üyesi Mehmet Dugan"),
        section(ScheduleDay.WEDNESDAY, 4, 5, 6, "A", "Din Hizmetlerinde Rehberlik ve İletişim", "Doç. Dr. Mustafa Fatih Ay"),
        section(ScheduleDay.WEDNESDAY, 4, 5, 6, "B", "Din Hizmetlerinde Rehberlik ve İletişim", "Doç. Dr. Mustafa Fatih Ay"),
        section(ScheduleDay.WEDNESDAY, 4, 5, 6, "C", "Kur'an Okuma ve Tecvid VII", "Rıdvan Orhan"),

        // 4. sınıf — Perşembe
        section(ScheduleDay.THURSDAY, 4, 3, 4, "A", "İslam Felsefesi II", "Prof. Dr. A. Kürşat Turgut"),
        section(ScheduleDay.THURSDAY, 4, 5, 6, "B", "İslam Felsefesi II", "Prof. Dr. A. Kürşat Turgut"),
        shared(ScheduleDay.THURSDAY, 4, 7, 8, "Tarih Usul ve Tenkidi", "Dr. Öğr. Üyesi Muhammet Fatih Duman"),
        shared(ScheduleDay.THURSDAY, 4, 7, 8, "Kur'an Metni Analizi", "Dr. Öğr. Ü. Eyüp Yaka"),
        shared(ScheduleDay.THURSDAY, 4, 7, 8, "Türk Tasavvuf Edebiyatı", "Prof. Dr. Ahmet Ögke"),

        // 4. sınıf — Cuma
        shared(ScheduleDay.FRIDAY, 4, 3, 4, "Osmanlıca Edebi Metinler", "Doç. Dr. Mehmet Şahin"),
        shared(ScheduleDay.FRIDAY, 4, 3, 4, "İslam Mezhepler Tarihi Metinleri", "Prof. Dr. Ömer Faruk Teber"),
        shared(ScheduleDay.FRIDAY, 4, 3, 4, "İletişim Sosyolojisi", "Prof. Dr. Bahset Karslı"),
        shared(ScheduleDay.FRIDAY, 4, 5, 6, "Mitoloji ve Anadolu Mitleri", "Dr. Öğr. Üyesi Mehmet Fatih Deniz"),
        shared(ScheduleDay.FRIDAY, 4, 5, 6, "Batıda ve Türkiye'de Yeni Dini Hareketler", "Dr. Öğr. Üyesi Ayşe A. Ambaroğlu")
    )

    fun schedulesFor(department: String?): List<ClassSchedule>? {
        if (department != ClassSchedules.THEOLOGY_DEPARTMENT) return null
        return (1..4).map { year ->
            ClassSchedule(
                faculty = ClassSchedules.THEOLOGY_FACULTY,
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
                sourceWarning = PREPARATORY_YEAR_NOTE,
                entries = blocks.asSequence()
                    .filter { it.year == year }
                    .flatMap { block ->
                        val periods = if (block.day == ScheduleDay.FRIDAY) fridayPeriods else weekdayPeriods
                        periods.subList(block.firstPeriod - 1, block.lastPeriod).asSequence().map { period ->
                            ScheduleEntry(
                                day = block.day,
                                startTime = period.start,
                                endTime = period.end,
                                courseCode = "",
                                courseName = block.name,
                                instructor = block.instructor,
                                classroom = block.classroom,
                                courseType = "Ders",
                                note = block.note,
                                section = block.section
                            )
                        }
                    }
                    .toList()
            )
        }
    }
}
