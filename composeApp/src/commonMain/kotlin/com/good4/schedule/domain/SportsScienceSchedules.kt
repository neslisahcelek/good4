package com.good4.schedule.domain

/** Spor Bilimleri Fakültesinin 2026-2027 güz yarıyılı resmî programları. */
internal object SportsScienceSchedules {
    private const val ACADEMIC_YEAR = "2026-2027"
    private const val TERM = "Güz Yarıyılı"

    private data class ScheduleSource(
        val url: String,
        val updatedAtByYear: List<String>,
        val rows: String
    )

    private data class SourceRow(
        val day: ScheduleDay,
        val year: Int,
        val start: String,
        val end: String,
        val cellText: String
    )

    private val courseCodePattern =
        Regex("""(?<![A-ZÇĞİÖŞÜ])([A-ZÇĞİÖŞÜ]{2,5}\s?\d{3}[A-Z]?)(?!\d)""")
    private val trailingParenthetical = Regex("""\(([^()]*)\)\s*$""")
    private val instructorMarker = Regex(
        """(?:Prof\.\s*Dr\.|Doç\.\s*Dr\.|Doç\.|Öğr\.\s*Gör\.(?:\s*Dr\.)?|Arş\.\s*Gör\.(?:\s*Dr\.)?|Araş\.\s*Gör\.(?:\s*Dr\.)?|Dr\.\s*Öğr\.\s*(?:Üyesi|Üye\.?)|Antrenör)\s+"""
    )

    private val sourceByDepartment = mapOf(
        ClassSchedules.BODY_EDUCATION_AND_SPORT_DEPARTMENT to ScheduleSource(
            url = "https://webis.akdeniz.edu.tr/uploads/1073/announcement/ders%20program%C4%B1/2026-2027%20G%C3%BCz/BES1%20(2).pdf",
            updatedAtByYear = listOf("2026-09-03", "2026-09-14", "2026-09-14", "2026-09-14"),
            rows = """
                MONDAY|1|10:30|12:20|MBZ101 Eğitime Giriş (D. 1)
                MONDAY|1|13:30|16:20|TDB105 Türk Dili 1 (D. 6)
                TUESDAY|1|09:30|12:20|ENF105 Bilişim Teknolojileri (Online)
                TUESDAY|1|13:30|16:20|BES103 Yüzme (Yüzme Havuzu)
                WEDNESDAY|1|09:30|12:20|BES107 İnsan Anatomisi ve Kinesiyoloji (D. 4)
                THURSDAY|1|08:30|10:20|YBD101 İngilizce 1 (D. 10)
                THURSDAY|1|10:30|12:20|ATA101 Atatürk İlkeleri ve İnkilap Tarihi 1 (D. 10)
                THURSDAY|1|13:30|15:20|BES105 Beden Eğitimi ve Sporun Temelleri (D. 6)
                FRIDAY|1|14:30|17:20|BES331 Takım Sporları 1 (Voleybol) (Mavi Salon)
                MONDAY|2|08:30|10:20|BES548 Beden Eğitimi Öğretim Modelleri (AES) (D. 1) BES551 Beden Eğitimi ve Spor Öğretiminde Eylem Araştırması (AES) (D. 4) BES554 Egzersiz Psikolojisi (AES) (D. 5) BES557 Serbest Zaman Eğitimi (AES) (D. 7) BES558 Sınıf İçi Öğrenmelerin Değerlendirilmesi (AES) (D. 6) BES564 Spor Felsefesi (AES) (D. 8) BES565 Rekreasyon (AES) (D. 3 ) BES567 Saha ve Malzeme Bilgisi (AES) (D. 9)
                MONDAY|2|10:30|12:20|BES552 Beden Eğitimi ve Spor Tarihi (AES) (D. 6) BES555 Geleneksel Türk Sporları (AES) (D. 3 ) BES556 Savunma Sporları (AES) (SD) BES559 Temel Müzik Eğitimi (AES) (D. 9) BES563 Sporda Çocuk Koruma (AES) (D. 12 ) BES568 Su Güvenliği ve Can Kurtarma (AES) (Yüzme Havuzu)
                MONDAY|2|12:30|13:20|GKZ402 Toplumsal Duyarlılık ve Katkı Prof. Dr. Adnan TURGUT Doç. Dr. Zehra CERTEL Doç. Dr. İlkay ORHAN Doç. Dr. Serdar ÖZÇETİN Öğr. Gör. Nurdan Oytun TATAR
                MONDAY|2|13:30|15:20|MBZ205 Öğretim İlke ve Yöntemleri (D. 8)
                MONDAY|2|15:30|17:20|GKS201 Bağımlılık ve Bağımlılıkla Mücadele (GKS ) (D. 4) MBS401 Karşılaştırmalı Eğitim (MBS) (D. 10)
                TUESDAY|2|08:30|10:20|BES566 Macera Temelli Eğitim (AES) (D. 1) GKS209 Ekonomi ve Girişimcilik (GKS) (D. 8) GKS309 Sivil Savunma ve Afet Yönetimi (GKS) (D. 10) MBS308 Karakter ve Değer Eğitimi (MBS) (D. 4) MBS408 Yetişkin Eğitimi ve Hayat Boyu Öğrenme (MBS) (D. 6)
                TUESDAY|2|10:30|12:20|GKS206 İnsan İlişkileri ve İletişim (GKS) (D. 4) GKS207 Bilim ve Araştırma Etiği (GKS) (SD) GKS303 Mesleki İngilizce (GKS) (D. 8) GKS305 Sanat ve Estetik (GKS) (D. 10)
                TUESDAY|2|12:30|13:20|GKZ402 Toplumsal Duyarlılık ve Katkı Prof. Dr. Adnan TURGUT Doç. Dr. Zehra CERTEL Doç. Dr. İlkay ORHAN Doç. Dr. Serdar ÖZÇETİN Öğr. Gör. Nurdan Oytun TATAR
                TUESDAY|2|13:30|16:20|GKZ402 Toplumsal Duyarlılık ve Katkı Prof. Dr. Bahri GÜRPINAR Prof. Dr. Mustafa ALTINKÖK Doç. Dr. H. Tolga ESEN Dr. Öğr. Üye. Alkan UĞURLU Öğr. Gör. Dr. Vedat ÇETİNKAYA Öğr. Gör. Yılmaz SEVGÜL Öğr. Gör. Birol UNSAL
                WEDNESDAY|2|08:30|10:20|GKS210 Kültür ve Dil (GKS) (D. 9) MBS206 Eğitimde Drama (MBS) (SD) MBS302 Eleştirel ve Analitik Düşünme (MBS) (SD) MBS402 Öğrenme Güçlüğü (MBS) (D. 1) MBS407 Okul Dışı Öğrenme Ortamları (MBS) (D. 10)
                WEDNESDAY|2|10:30|12:20|MBS205 Dikkat Eksikliği ve Hiperaktivite Bozukluğu (MBS) (D. 1) MBS301 Eğitimde Program Dışı Etkinlikler (MBS) (D. 10) MBS405 Müze Eğitimi (MBS) (D. 9)
                WEDNESDAY|2|12:30|13:20|GKZ402 Toplumsal Duyarlılık ve Katkı Prof. Dr. Adnan TURGUT Doç. Dr. Zehra CERTEL Doç. Dr. İlkay ORHAN Doç. Dr. Serdar ÖZÇETİN Öğr. Gör. Nurdan Oytun TATAR
                WEDNESDAY|2|13:30|15:20|MBZ201 Öğretim Teknolojileri (D. 4)
                WEDNESDAY|2|15:30|17:20|MBZ309 Eğitim Sosyolojisi (D. 2)
                THURSDAY|2|09:30|12:20|BES241 Takım Sporları 3 (Hentbol) (Mavi Salon)
                THURSDAY|2|13:30|15:20|BES109 Hareket Eğitimi (Cimnastik Salonu )
                THURSDAY|2|15:30|17:20|BES109 Hareket Eğitimi (D. 9)
                FRIDAY|2|08:30|10:20|GKS208 Kariyer Planlama ve Geliştirme (GKS) (D. 2) GKS310 Güzel ve Etkili Konuşma (GKS) (D. 3 ) MBS406 Sürdürülebilir Kalkınma ve Eğitim (MBS) (D. 12 )
                FRIDAY|2|10:30|12:20|BES203 Beden Eğitimi - Spor Öğrenme ve Öğretim Yaklaşımları (D. 12 )
                FRIDAY|2|14:30|17:20|BES205 Motor Gelişim (D. 9)
                MONDAY|3|08:30|10:20|BES548 Beden Eğitimi Öğretim Modelleri (AES) (D. 1) BES551 Beden Eğitimi ve Spor Öğretiminde Eylem Araştırması (AES) (D. 4) BES554 Egzersiz Psikolojisi (AES) (D. 5) BES557 Serbest Zaman Eğitimi (AES) (D. 7) BES558 Sınıf İçi Öğrenmelerin Değerlendirilmesi (AES) (D. 6) BES564 Spor Felsefesi (AES) (D. 8) BES565 Rekreasyon (AES) (D. 3 ) BES567 Saha ve Malzeme Bilgisi (AES) (D. 9)
                MONDAY|3|10:30|12:20|BES552 Beden Eğitimi ve Spor Tarihi (AES) (D. 6) BES555 Geleneksel Türk Sporları (AES) (D. 3 ) BES556 Savunma Sporları (AES) (SD) BES559 Temel Müzik Eğitimi (AES) (D. 9) BES563 Sporda Çocuk Koruma (AES) (D. 12 ) BES568 Su Güvenliği ve Can Kurtarma (AES) (Yüzme Havuzu)
                MONDAY|3|13:30|15:20|BES309 Sivil Savunma ve Afet Yönetimi (D. 10)
                MONDAY|3|15:30|17:20|GKS201 Bağımlılık ve Bağımlılıkla Mücadele (GKS) (D. 4) MBS401 Karşılaştırmalı Eğitim (MBS) (D. 10)
                TUESDAY|3|08:30|10:20|BES566 Macera Temelli Eğitim (AES) (D. 1) GKS209 Ekonomi ve Girişimcilik (GKS) (D. 8) GKS309 Sivil Savunma ve Afet Yönetimi (GKS) (D. 10) MBS308 Karakter ve Değer Eğitimi (MBS) (D. 4) MBS408 Yetişkin Eğitimi ve Hayat Boyu Öğrenme (MBS) (D. 6)
                TUESDAY|3|10:30|12:20|GKS206 İnsan İlişkileri ve İletişim (GKS) (D. 4) GKS207 Bilim ve Araştırma Etiği (GKS) (SD) GKS303 Mesleki İngilizce (GKS) (D. 8) GKS305 Sanat ve Estetik (GKS) (D. 10)
                TUESDAY|3|13:30|15:20|MBZ307 Sınıf Yönetimi (D. 6)
                WEDNESDAY|3|08:30|10:20|GKS210 Kültür ve Dil (GKS) (D. 9) MBS206 Eğitimde Drama (MBS) (SD) MBS402 Öğrenme Güçlüğü (MBS) (D. 1) MBS407 Okul Dışı Öğrenme Ortamları (MBS) (D. 10)
                WEDNESDAY|3|10:30|12:20|MBS205 Dikkat Eksikliği ve Hiperaktivite Bozukluğu (MBS) (MBS) (D. 1) MBS301 Eğitimde Program Dışı Etkinlikler (MBS) (D. 10) MBS302 Eleştirel ve Analitik Düşünme (MBS) (SD) MBS405 Müze Eğitimi (MBS) (D. 9)
                WEDNESDAY|3|13:30|15:20|BES109 Hareket Eğitimi (Gazi Mustafa Kemal Spor Salonu)
                WEDNESDAY|3|15:30|17:20|BES109 Hareket Eğitimi (D. 10)
                THURSDAY|3|13:30|15:20|BES307 Beden Eğitimi ve Spor Öğretimi (D. 12 )
                THURSDAY|3|15:30|17:20|BES307 Beden Eğitimi ve Spor Öğretimi (Gazi Mustafa Kemal Spor Salonu)
                FRIDAY|3|08:30|10:20|GKS208 Kariyer Planlama ve Geliştirme (GKS) (D. 2) GKS310 Güzel ve Etkili Konuşma (GKS) (D. 3 ) MBS406 Sürdürülebilir Kalkınma ve Eğitim (MBS) (D. 12 )
                FRIDAY|3|10:30|12:20|MBZ305 Eğitimde Ahlak ve Etik (D. 4)
                FRIDAY|3|14:30|16:20|BES303 Antrenman Bilgisi (D. 12 )
                MONDAY|4|08:30|10:20|BES548 Beden Eğitimi Öğretim Modelleri (AES) (D. 1) BES551 Beden Eğitimi ve Spor Öğretiminde Eylem Araştırması (AES) (D. 4) BES554 Egzersiz Psikolojisi (AES) (D. 5) BES557 Serbest Zaman Eğitimi (AES) (D. 7) BES558 Sınıf İçi Öğrenmelerin Değerlendirilmesi (AES) (D. 6) BES564 Spor Felsefesi (AES) (D. 8) BES565 Rekreasyon (AES) (D. 3 ) BES567 Saha ve Malzeme Bilgisi (AES) (D. 9)
                MONDAY|4|10:30|12:20|BES552 Beden Eğitimi ve Spor Tarihi (AES) (D. 6) BES555 Geleneksel Türk Sporları (AES) (D. 3 ) BES556 Savunma Sporları (AES) (SD) BES559 Temel Müzik Eğitimi (AES) (D. 9) BES563 Sporda Çocuk Koruma (AES) (D. 12 ) BES568 Su Güvenliği ve Can Kurtarma (AES) (Yüzme Havuzu)
                MONDAY|4|13:30|15:20|MBZ405 Özel Eğitim ve Kaynaştırma (D. 9)
                MONDAY|4|15:30|17:20|MBS401 Karşılaştırmalı Eğitim (MBS) (D. 10)
                TUESDAY|4|08:30|10:20|BES566 Macera Temelli Eğitim (AES) (D. 1) MBS308 Karakter ve Değer Eğitimi (MBS) (D. 4) MBS408 Yetişkin Eğitimi ve Hayat Boyu Öğrenme (MBS) (D. 6)
                TUESDAY|4|10:30|12:20|MBZ401 Öğretmenlik Uygulaması 1
                TUESDAY|4|13:30|17:20|MBZ401 Öğretmenlik Uygulaması 1
                WEDNESDAY|4|08:30|10:20|MBS206 Eğitimde Drama (MBS) (SD) MBS402 Öğrenme Güçlüğü (MBS) (D. 1) MBS407 Okul Dışı Öğrenme Ortamları (MBS) (D. 10)
                WEDNESDAY|4|10:30|12:20|MBS205 Dikkat Eksikliği ve Hiperaktivite Bozukluğu (MBS) (D. 1) MBS301 Eğitimde Program Dışı Etkinlikler (MBS) (D. 10) MBS302 Eleştirel ve Analitik Düşünme (MBS) (SD) MBS405 Müze Eğitimi (MBS) (D. 9)
                WEDNESDAY|4|13:30|15:20|BES405 Egzersiz ve Beslenme (D. 10)
                WEDNESDAY|4|15:30|17:20|MBZ401 Öğretmenlik Uygulaması 1 Prof. Dr. Bahri GÜRPINAR Prof. Dr. Meriç ERASLAN Doç. Dr. Zehra CERTEL Doç. Dr. H. Tolga ESEN Dr. Öğr. Üye. Alkan UĞURLU Öğr. Gör. Dr. Vedat ÇETİNKAYA
                THURSDAY|4|09:30|10:20|BES407 Eğitsel Oyunlar (D. 1)
                THURSDAY|4|10:30|12:20|BES407 Eğitsel Oyunlar (Gazi Mustafa Kemal Spor Salonu)
                THURSDAY|4|13:30|16:20|BES403 Halk Oyunları (Halk Oyunları S.)
                FRIDAY|4|08:30|10:20|MBS406 Sürdürülebilir Kalkınma ve Eğitim (MBS) (D. 12 )
                FRIDAY|4|10:30|12:20|MBZ401 Öğretmenlik Uygulaması 1 Doç. Dr. İlkay ORHAN
            """.trimIndent()
        ),
        ClassSchedules.SPORTS_MANAGEMENT_DEPARTMENT to ScheduleSource(
            url = "https://webis.akdeniz.edu.tr/uploads/1073/announcement/ders%20program%C4%B1/2026-2027%20G%C3%BCz/Y%C3%96N1%20(3).pdf",
            updatedAtByYear = listOf("2026-09-14", "2026-09-03", "2026-09-03", "2026-09-03"),
            rows = """
                MONDAY|1|08:30|09:20|ATA101 Atatürk İlkeleri ve İnk.Tarihi 1 (D. 2)
                MONDAY|1|09:30|10:20|ATA101 Atatürk İlkeleri ve İnk.Tarihi1 (D. 2)
                MONDAY|1|10:30|12:20|YBD101 İngilizce 1 (D. 10)
                MONDAY|1|13:30|16:20|SYB107 Spor Bilimlerine Giriş (D. 12 )
                TUESDAY|1|09:30|12:20|ENF101 Bilgi Teknolojileri Kullanımı (Online)
                TUESDAY|1|13:30|16:20|SYB105 Yönetim Bilimlerine Giriş (D. 5)
                THURSDAY|1|08:30|10:20|TDB101 Türk Dili 1 (D. 4)
                THURSDAY|1|10:30|11:20|KPD101 Kariyer Planlama (D. 1)
                FRIDAY|1|09:30|12:20|SYB101 Atletizm (Atletizm Pisti)
                FRIDAY|1|14:30|17:20|SYB103 Spor Tarihi (Prof. Dr. Kamil Özer Konferans Salonu)
                TUESDAY|2|09:30|12:20|SYB201 Spor Sosyolojisi (D. 5)
                TUESDAY|2|13:30|16:20|SYB207 Spor Ekonomisi (D. 10)
                WEDNESDAY|2|13:30|16:20|SYB211 Spor Fizyolojisi (D. 12 )
                THURSDAY|2|09:30|12:20|SYB205 Cimnastik (Cimnastik Salonu )
                THURSDAY|2|13:30|16:20|SYB209 Spor Pedagojisi (D. 5)
                FRIDAY|2|09:30|12:20|Seçmeli Takım Sporları 1 STS201 Basketbol (Gazi Mustafa Kemal Spor Salonu) STS203 Voleybol (Mavi Salon) STS209 Futbol (Zeki Budak Sahası )
                FRIDAY|2|14:30|17:20|SYB203 Spor Psikolojisi (D. 4)
                MONDAY|3|10:30|12:20|SYB301 Toplumsal Duyarlılık ve Katkı - 1 (D. 5)
                MONDAY|3|13:30|16:20|SYB307 Spor Bilimlerinde İstatistik (Bilgisayar Lab.)
                TUESDAY|3|09:30|12:20|SYB305 Spor Organizasyonları (D. 2)
                TUESDAY|3|13:30|16:20|SYB313 Mesleki İngilizce 1 () (D. 12 )
                WEDNESDAY|3|13:30|16:20|Seçmeli Alan Bilgisi 1 SYB319 Davranış Bilimleri (D. 3 ) SYB335 Sponsorluk (D. 7) SYB337 Spor Kültürü ve Ahlakı (D. 11 )
                THURSDAY|3|09:30|12:20|SYB303 Antrenman Bilgisi (D. 5)
                THURSDAY|3|13:30|16:20|SYB309 Sporda Örgütsel Davranış (D. 1)
                MONDAY|4|08:30|10:20|SYB415 Satranç 1 (SD) SYB427 Okçuluk 1 (SD) SYB433 Badminton 1 (Mavi Salon)
                MONDAY|4|10:30|11:20|SYB415 Satranç 1 (Satranç Salonu) SYB423 Tenis 1 (K 1-2) SYB427 Okçuluk 1 (Okçuluk Sal. Stadyum) SYB433 Badminton 1 (SD)
                MONDAY|4|11:30|12:20|SYB415 Satranç 1 (Satranç Salonu) SYB423 Tenis 1 (K 3-4) SYB427 Okçuluk 1 (Okçuluk Sal. Stadyum) SYB433 Badminton 1 (SD)
                MONDAY|4|13:30|16:20|SYB409 Girişimcilik ve Proje Yönetimi (D. 5)
                TUESDAY|4|08:30|10:20|SYB417 Voleybol 1 (SD)
                TUESDAY|4|10:30|12:20|SYB417 Voleybol 1 (Mavi Salon)
                TUESDAY|4|13:30|16:20|SYB403 Sporda İnsan Kaynakları Yönetimi (D. 9)
                WEDNESDAY|4|08:30|10:20|SYB413 Atletizm 1 (SD) SYB419 Hentbol 1 (Mavi Salon) SYB421 Futbol 1 (SD) SYB429 Yüzme 1 (Yüzme Havuzu) SYB431 Basketbol 1 (Gazi Mustafa Kemal Spor Salonu)
                WEDNESDAY|4|10:30|11:20|SYB413 Atletizm 1 (Atletizm Pisti) SYB419 Hentbol 1 (SD) SYB421 Futbol 1 (Zeki Budak Sahası ) SYB423 Tenis 1 (K 5-6) SYB429 Yüzme 1 (Yüzme Havuzu) SYB431 Basketbol 1 (SD)
                WEDNESDAY|4|11:30|12:20|SYB413 Atletizm 1 (Atletizm Pisti) SYB419 Hentbol 1 (SD) SYB421 Futbol 1 (Zeki Budak Sahası ) SYB423 Tenis 1 (K 7-8) SYB429 Yüzme 1 (Yüzme Havuzu) SYB431 Basketbol 1 (SD)
                WEDNESDAY|4|13:30|16:20|SYB401 Spor ve Medya (D. 5)
                THURSDAY|4|08:30|09:20|SYB411 Spor Bilimlerinde Araştırma Projesi - 1 Prof. Dr. Cenk TEMEL Öğr. Gör. Yılmaz KAPLAN
                THURSDAY|4|09:30|12:20|SYB405 Kamu Yönetimi (D. 12 )
                THURSDAY|4|13:30|15:20|SYB411 Spor Bilimlerinde Araştırma Projesi - 1 Prof. Dr. Mustafa AKIL Prof. Dr. Burhanettin HACICAFEROĞLU Prof. Dr. Hasan ŞAHAN Prof. Dr. Evren TERCAN KAAS Doç. Dr. K. Alparslan ERMAN Doç. Dr. M. Emre ERYÜCEL
                THURSDAY|4|16:30|17:20|SYB411 Spor Bilimlerinde Araştırma Projesi - 1 Prof. Dr. Cenk TEMEL Öğr. Gör. Yılmaz KAPLAN
                FRIDAY|4|08:30|10:20|SYB425 Vücut Geliştirme ve Fitness 1 (SD)
                FRIDAY|4|10:30|12:20|SYB425 Vücut Geliştirme ve Fitness 1 (Öğrenci Fitness Merkezi)
                FRIDAY|4|14:30|17:20|SYB407 Sporda Muhasebe ve Finans Yönetimi (D. 6)
            """.trimIndent()
        ),
        ClassSchedules.RECREATION_DEPARTMENT to ScheduleSource(
            url = "https://webis.akdeniz.edu.tr/file/getfile?guid=335768b0-3384-447b-be39-b1997ae0406e",
            updatedAtByYear = listOf("2026-09-03", "2026-09-03", "2026-09-03", "2026-09-03"),
            rows = """
                MONDAY|1|09:30|12:20|REK105 Atletizm (Atletizm Pisti)
                MONDAY|1|13:30|16:20|REK155 Spor ve Serbest Zaman Sosyolojisi (D. 2)
                TUESDAY|1|09:30|12:20|ENF101 Bilgi Teknolojileri Kullanımı (Online)
                TUESDAY|1|13:30|16:20|REK157 Spor ve Rekreasyonun Temelleri (D. 8)
                WEDNESDAY|1|08:30|10:20|REK269 Golf (Zeki Budak Sahası )
                WEDNESDAY|1|10:30|12:20|REK241 Okçuluk&Dart (Okçuluk Sal. Stadyum) REK243 Badminton (Mavi Salon)
                WEDNESDAY|1|13:30|14:20|KPD101 Kariyer Planlama (Prof. Dr. Kamil Özer Konferans Salonu)
                THURSDAY|1|08:30|10:20|ATA101 Atatürk İlkeleri ve İnkılâp Tarihi I (D. 6)
                THURSDAY|1|10:30|12:20|YBD101 İngilizce I () (D. 6)
                THURSDAY|1|13:30|15:20|TDB101 Türk Dili I (D. 8)
                FRIDAY|1|09:30|12:20|REK137 Fiziksel Aktivite ve Sağlık (D. 10)
                FRIDAY|1|14:30|16:20|REK101 Anatomi (D. 2)
                MONDAY|2|08:30|10:20|REK249 Basketbol (Gazi Mustafa Kemal Spor Salonu)
                MONDAY|2|10:30|12:20|REK207 Sosyal İletişim ve Grup Yönetimi (SD)
                MONDAY|2|13:30|16:20|REK261 Engellilerde Rekreasyon (D. 1)
                TUESDAY|2|08:30|09:20|REK251 Futbol (Zeki Budak Sahası ) REK271 Hentbol (Mavi Salon) REK273 Voleybol (Gazi Mustafa Kemal Spor Salonu) REK275 Tenis (K 5-6)
                TUESDAY|2|09:30|10:20|REK251 Futbol (Zeki Budak Sahası ) REK271 Hentbol (Mavi Salon) REK273 Voleybol (Gazi Mustafa Kemal Spor Salonu) REK275 Tenis (K 7-8)
                TUESDAY|2|10:30|12:20|REK255 Egzersiz Psikolojisi (D. 6)
                TUESDAY|2|13:30|16:20|REK407 Suda Cankurtarma (Yüzme Havuzu)
                WEDNESDAY|2|10:30|12:20|REK263 Rekreasyonda Program Geliştirme (D. 2)
                WEDNESDAY|2|13:30|16:20|REK259 Rekreasyon Yönetimi (D. 9)
                THURSDAY|2|09:30|12:20|REK215 Fiziksel Uygunluk (D. 8)
                THURSDAY|2|13:30|16:20|REK265 Rekreasyonda Multimedya Uygulamaları (Bilgisayar Lab.)
                FRIDAY|2|09:30|15:20|REK201 İngilizce I (D. 1)
                MONDAY|3|10:30|12:20|REK347 Bilimsel Araştırma Teknikleri (D. 8)
                MONDAY|3|13:30|16:20|REK351 Etkinlik Tasarımı ve Uygulaması (SD)
                TUESDAY|3|08:30|10:20|REK359 Pilates (Mücadele Sporları Salonu)
                TUESDAY|3|10:30|16:20|REK301 İngilizce III (D. 1)
                WEDNESDAY|3|08:30|09:20|REK357 Rekreasyon Uygulaması I (SD)
                WEDNESDAY|3|09:30|12:20|REK309 Açık Alan Rekreasyonu (D. 8)
                WEDNESDAY|3|13:30|15:20|REK331 Kaya Tırmanma (Tırmanma Duvarı) REK333 Scuba (Yüzme Havuzu)
                WEDNESDAY|3|15:30|17:20|REK357 Rekreasyon Uygulaması I (SD)
                THURSDAY|3|09:30|12:20|GNC301 Gönüllülük Çalışmaları (SD) REK321 Yaşlılarda Fiziksel Aktivite (D. 9) REK363 Spor Beslenmesi ve Besinsel Ergojenik Yardımcılar (Bilgisayar Lab.) REK413 Kampüs Rekreasyonu (SD)
                THURSDAY|3|13:30|15:20|REK353 Golf (Zeki Budak Sahası )
                FRIDAY|3|08:30|10:20|REK349 Yoga (Mücadele Sporları Salonu)
                FRIDAY|3|10:30|12:20|REK345 Rekreasyonda İletişim ve Halkla İlişkiler (SD)
                FRIDAY|3|14:30|17:20|REK346 Rekreasyonda Ofis Uygulamaları (Bilgisayar Lab.)
                MONDAY|4|08:30|09:20|REK463 Rekreasyonel Spor Yönetimi I (SD)
                MONDAY|4|09:30|12:20|REK457 Turizm Rekreasyonu I (SD) REK459 Terapötik Rekreasyon I (SD) REK463 Rekreasyonel Spor Yönetimi I (SD)
                MONDAY|4|13:30|15:20|REK455 Yüzme I (SD) REK439 Golf I (Zeki Budak Sahası )
                MONDAY|4|15:30|16:20|REK439 Golf I (Zeki Budak Sahası )
                TUESDAY|4|08:30|09:20|REK455 Yüzme I (Yüzme Havuzu)
                TUESDAY|4|09:30|10:20|REK429 Basketbol I (SD) REK445 Scuba I (Yüzme Havuzu) REK475 Oryantiring I (SD) REK455 Yüzme I (Yüzme Havuzu)
                TUESDAY|4|10:30|12:20|REK429 Basketbol I (SD) REK435 Fitness I (SD) REK437 Futbol I (SD) REK445 Scuba I (Yüzme Havuzu) REK453 Voleybol I (SD) REK473 Okçuluk I (SD) REK475 Oryantiring I (SD) REK477 Pilates I (SD) REK451 Tenis I (SD) REK455 Yüzme I (Yüzme Havuzu)
                TUESDAY|4|13:30|14:20|REK451 Tenis I (K 5-6) REK431 Badminton I (Mavi Salon) REK435 Fitness I (Atletik Terapi ve Reh. Salonu) REK437 Futbol I (Zeki Budak Sahası ) REK445 Scuba I (SD) REK453 Voleybol I (Gazi Mustafa Kemal Spor Salonu) REK473 Okçuluk I (Okçuluk Sal. Stadyum) REK475 Oryantiring I (SD) REK477 Pilates I (Mücadele Sporları Salonu)
                TUESDAY|4|14:30|15:20|REK451 Tenis I (K 7-8) REK431 Badminton I (Mavi Salon) REK435 Fitness I (Atletik Terapi ve Reh. Salonu) REK437 Futbol I (Zeki Budak Sahası ) REK445 Scuba I (SD) REK453 Voleybol I (Gazi Mustafa Kemal Spor Salonu) REK473 Okçuluk I (Okçuluk Sal. Stadyum) REK475 Oryantiring I (SD) REK477 Pilates I (Mücadele Sporları Salonu)
                TUESDAY|4|15:30|16:20|REK451 Tenis I (K 5-6) REK431 Badminton I (Mavi Salon) REK435 Fitness I (Atletik Terapi ve Reh. Salonu) REK437 Futbol I (Zeki Budak Sahası ) REK445 Scuba I (SD) REK453 Voleybol I (Gazi Mustafa Kemal Spor Salonu) REK473 Okçuluk I (Okçuluk Sal. Stadyum) REK475 Oryantiring I (SD) REK477 Pilates I (Mücadele Sporları Salonu)
                TUESDAY|4|16:30|17:20|REK431 Badminton I (SD) REK435 Fitness I (SD) REK437 Futbol I (SD) REK453 Voleybol I (SD) REK473 Okçuluk I (SD) REK477 Pilates I (SD) REK451 Tenis I (K 7-8)
                WEDNESDAY|4|09:30|10:20|REK457 Turizm Rekreasyonu I (D. 7) REK459 Terapötik Rekreasyon I (D. 11 ) REK461 Açık Alan Rekreasyonu I (D. 3 )
                WEDNESDAY|4|10:30|12:20|REK457 Turizm Rekreasyonu I (D. 7) REK459 Terapötik Rekreasyon I (D. 11 ) REK461 Açık Alan Rekreasyonu I (D. 3 ) REK463 Rekreasyonel Spor Yönetimi I (D. 12 )
                WEDNESDAY|4|13:30|17:20|REK311 Rekreasyonel Drama I (D. 1)
                THURSDAY|4|10:30|12:20|REK403 Toplumsal Duyarlılık ve Katkı I (D. 4)
                FRIDAY|4|08:30|14:20|REK401 İngilizce V (D. 5)
                FRIDAY|4|14:30|16:20|REK431 Badminton I (SD) REK439 Golf I (SD) REK429 Basketbol I (Gazi Mustafa Kemal Spor Salonu) REK461 Açık Alan Rekreasyonu I (SD)
                FRIDAY|4|16:30|17:20|REK439 Golf I (SD) REK429 Basketbol I (Gazi Mustafa Kemal Spor Salonu) REK461 Açık Alan Rekreasyonu I (SD)
            """.trimIndent()
        ),
        ClassSchedules.COACHING_EDUCATION_DEPARTMENT to ScheduleSource(
            url = "https://webis.akdeniz.edu.tr/uploads/1073/announcement/ders%20program%C4%B1/2026-2027%20G%C3%BCz/ANT1%20(2).pdf",
            updatedAtByYear = listOf("2026-09-03", "2026-09-03", "2026-09-14", "2026-09-03"),
            rows = """
                MONDAY|1|09:30|10:20|Seçmeli Sporlar 1 ANB503 Dağcılık (SD) ANB504 Futbol (SD) ANB507 Hentbol (SD) ANB517 Yoga (SD)
                MONDAY|1|10:30|12:20|Seçmeli Sporlar 1 ANB503 Dağcılık (Tırmanma Duvarı) ANB504 Futbol (Zeki Budak Sahası ) ANB507 Hentbol (Mavi Salon) ANB517 Yoga (Mücadele Sporları Salonu)
                MONDAY|1|13:30|14:20|Seçmeli Sporlar 1 ANB501 Badminton (Mavi Salon) ANB502 Basketbol (Gazi Mustafa Kemal Spor Salonu) ANB505 Golf (Zeki Budak Sahası ) ANB506 Güreş (Mücadele Sporları Salonu) ANB508 Okçuluk (Okçuluk Sal. Stadyum) ANB509 Oryantiring (SD) ANB511 Ritmik Cimnastik (Cimnastik Salonu ) ANB512 Scuba (Yüzme Havuzu) ANB515 Tenis (K 7-8)
                MONDAY|1|14:30|15:20|Seçmeli Sporlar 1 ANB516 Voleybol (SD) ANB501 Badminton (Mavi Salon) ANB502 Basketbol (Gazi Mustafa Kemal Spor Salonu) ANB505 Golf (Zeki Budak Sahası ) ANB506 Güreş (Mücadele Sporları Salonu) ANB508 Okçuluk (Okçuluk Sal. Stadyum) ANB509 Oryantiring (SD) ANB511 Ritmik Cimnastik (Cimnastik Salonu ) ANB512 Scuba (Yüzme Havuzu) ANB515 Tenis (K 5-6)
                MONDAY|1|15:30|16:20|Seçmeli Sporlar 1 ANB501 Badminton (SD) ANB502 Basketbol (SD) ANB505 Golf (SD) ANB506 Güreş (SD) ANB508 Okçuluk (SD) ANB509 Oryantiring (SD) ANB511 Ritmik Cimnastik (SD) ANB512 Scuba (Yüzme Havuzu) ANB515 Tenis (SD) ANB516 Voleybol (Mavi Salon)
                MONDAY|1|16:30|17:20|Seçmeli Sporlar 1 ANB516 Voleybol (Mavi Salon)
                TUESDAY|1|09:30|12:20|ENF101 Bilgi Teknolojileri Kullanımı (Online)
                TUESDAY|1|13:30|17:20|ANB107 Hareket Eğitimi (Cimnastik Salonu )
                WEDNESDAY|1|09:30|12:20|ANB103 Atletizm (Atletizm Pisti)
                WEDNESDAY|1|13:30|15:20|ANB101 Spor Bilimlerine Giriş (D. 2)
                WEDNESDAY|1|15:30|17:20|Seçmeli Sporlar 1 ANB510 Plaj Voleybolu (Plaj Voleybolu Sah.) ANB513 Spor Tırmanış (Tırmanma Duvarı) ANB514 Tai-Chi (Mücadele Sporları Salonu)
                THURSDAY|1|08:30|10:20|ATA101 Atatürk İlkeleri Ve İnkilap Tarihi 1 (D. 2)
                THURSDAY|1|10:30|12:20|TDB101 Türk Dili 1 (D. 2)
                THURSDAY|1|13:30|15:20|YBD101 İngilizce 1 (D. 4)
                FRIDAY|1|08:30|09:20|Seçmeli Sporlar 1 ANB510 Plaj Voleybolu (SD) ANB513 Spor Tırmanış (SD) ANB514 Tai-Chi (SD)
                FRIDAY|1|09:30|10:20|ANB105 Üniversite Yaşamına Uyum (D. 4)
                FRIDAY|1|10:30|12:20|ANB109 İnsan Anatomisi (D. 2)
                MONDAY|2|10:30|12:20|ANB211 Sporcu Beslenmesi (D. 2)
                MONDAY|2|13:30|15:20|ANB209 Motor Gelişim (D. 4)
                MONDAY|2|15:30|17:20|ANB209 Motor Gelişim (Gazi Mustafa Kemal Spor Salonu)
                TUESDAY|2|08:30|10:20|Seçmeli Teorik Dersler ANB275 Mesleki İngilizce (D. 7 )
                TUESDAY|2|10:30|12:20|Seçmeli Teorik Dersler ANB271 Spor Masajı (Atletik Terapi ve Reh. Salonu) ANB273 Sivil Savunma ve Afet Yönetimi (D. 3 ) ANB277 Yaşlılarda Spor (D. 11 ) ANB281 Sporda Yönetim ve Organizasyon (D. 9)
                TUESDAY|2|13:30|15:20|ANB205 Spor Biyomekaniği (D. 4)
                TUESDAY|2|15:30|17:20|Seçmeli Teorik Dersler ANB279 Gelişim ve Öğrenme (D. 3 )
                WEDNESDAY|2|08:30|10:20|ANB203 Sporda Öğretim Yöntemleri (D. 2)
                WEDNESDAY|2|10:30|12:20|ANB203 Sporda Öğretim Yöntemleri (Gazi Mustafa Kemal Spor Salonu)
                WEDNESDAY|2|13:30|14:20|ANB251 Atletizm UD1 (Atletizm Pisti) ANB251 Basketbol UD1 (SD) ANB251 Güreş UD1 (Mücadele Sporları Salonu) ANB251 Hentbol UD1 (SD) ANB251 Tenis UD1 (K 5-6) ANB251 Voleybol UD1 (Mavi Salon) ANB251 Yüzme UD1 (Yüzme Havuzu) ANB251 Golf UD1 (Zeki Budak Sahası )
                WEDNESDAY|2|14:30|15:20|ANB251 Atletizm UD1 (Atletizm Pisti) ANB251 Basketbol UD1 (SD) ANB251 Güreş UD1 (Mücadele Sporları Salonu) ANB251 Hentbol UD1 (SD) ANB251 Tenis UD1 (K 7-8) ANB251 Voleybol UD1 (Mavi Salon) ANB251 Yüzme UD1 (Yüzme Havuzu) ANB251 Golf UD1 (Zeki Budak Sahası )
                WEDNESDAY|2|15:30|16:20|ANB251 Atletizm UD1 (SD) ANB251 Basketbol UD1 (Gazi Mustafa Kemal Spor Salonu) ANB251 Güreş UD1 (SD) ANB251 Hentbol UD1 (Mavi Salon) ANB251 Tenis UD1 (K 5-6) ANB251 Yüzme UD1 (Yüzme Havuzu) ANB251 Golf UD1 (SD)
                WEDNESDAY|2|16:30|17:20|ANB251 Atletizm UD1 (SD) ANB251 Basketbol UD1 (Gazi Mustafa Kemal Spor Salonu) ANB251 Güreş UD1 (SD) ANB251 Hentbol UD1 (Mavi Salon) ANB251 Tenis UD1 (K 7-8) ANB251 Yüzme UD1 (Yüzme Havuzu) ANB251 Golf UD1 (SD)
                THURSDAY|2|08:30|09:20|ANB251 Futbol UD1 (SD) ANB251 Okçuluk UD1 (SD) ANB251 Badminton UD1 (Mavi Salon) ANB251 Scuba UD1 (SD) ANB251 Spor Tırmanış UD1 (SD)
                THURSDAY|2|09:30|10:20|ANB251 Futbol UD1 (SD) ANB251 Okçuluk UD1 (SD) ANB251 Badminton UD1 (SD) ANB251 Scuba UD1 (SD) ANB251 Spor Tırmanış UD1İ (SD)
                THURSDAY|2|10:30|12:20|ANB251 Futbol UD1 (Zeki Budak Sahası ) ANB251 Okçuluk UD1 (Okçuluk Sal. Stadyum) ANB251 Badminton UD1 (SD) ANB251 Scuba UD1 (Yüzme Havuzu) ANB251 Spor Tırmanış UD1 (Tırmanma Duvarı)
                THURSDAY|2|13:30|15:20|ANB201 Antrenman Bilimi 1 (D. 2)
                FRIDAY|2|08:30|09:20|ANB251 Oryantiring UD1 (SD)
                FRIDAY|2|09:30|12:20|ANB207 Egzersiz Fizyolojisi 1 (D. 6)
                FRIDAY|2|14:30|16:20|ANB251 Oryantiring UD1 (SD) ANB251 Voleybol UD1 (SD)
                FRIDAY|2|16:30|17:20|ANB251 Oryantiring UD1 (SD)
                MONDAY|3|09:30|10:20|Seçmeli Sporlar 3 ANB503 Dağcılık (SD) ANB504 Futbol (SD) ANB507 Hentbol (SD) ANB515 Tenis (SD) ANB517 Yoga (SD)
                MONDAY|3|10:30|11:20|Seçmeli Sporlar 3 ANB503 Dağcılık (Tırmanma Duvarı) ANB504 Futbol (Zeki Budak Sahası ) ANB507 Hentbol (Mavi Salon) ANB515 Tenis (K 5-6) ANB517 Yoga (Mücadele Sporları Salonu)
                MONDAY|3|11:30|12:20|Seçmeli Sporlar 3 ANB503 Dağcılık (Tırmanma Duvarı) ANB504 Futbol (Zeki Budak Sahası ) ANB507 Hentbol (Mavi Salon) ANB515 Tenis (K 7-8) ANB517 Yoga (Mücadele Sporları Salonu)
                MONDAY|3|13:30|14:20|Seçmeli Sporlar 3 ANB501 Badminton (Mavi Salon) ANB509 Oryantiring (SD) ANB502 Basketbol (Gazi Mustafa Kemal Spor Salonu) ANB505 Golf (Zeki Budak Sahası ) ANB506 Güreş (Mücadele Sporları Salonu) ANB508 Okçuluk (Okçuluk Sal. Stadyum) ANB511 Ritmik Cimnastik (Cimnastik Salonu ) ANB512 Scuba (Yüzme Havuzu)
                MONDAY|3|14:30|15:20|Seçmeli Sporlar 3 ANB516 Voleybol (SD) ANB501 Badminton (Mavi Salon) ANB502 Basketbol (Gazi Mustafa Kemal Spor Salonu) ANB505 Golf (Zeki Budak Sahası ) ANB506 Güreş (Mücadele Sporları Salonu) ANB508 Okçuluk (Okçuluk Sal. Stadyum) ANB509 Oryantiring (SD) ANB511 Ritmik Cimnastik (Cimnastik Salonu ) ANB512 Scuba (Yüzme Havuzu)
                MONDAY|3|15:30|16:20|Seçmeli Sporlar 3 ANB502 Basketbol (SD) ANB505 Golf (SD) ANB506 Güreş (SD) ANB508 Okçuluk (SD) ANB511 Ritmik Cimnastik (SD) ANB512 Scuba (Yüzme Havuzu) ANB501 Badminton (SD) ANB509 Oryantiring (SD) ANB516 Voleybol (Mavi Salon)
                MONDAY|3|16:30|17:20|Seçmeli Sporlar 3 ANB516 Voleybol (Mavi Salon)
                TUESDAY|3|09:30|10:20|ANB301 Eğitsel Oyunlar (D. 9)
                TUESDAY|3|10:30|12:20|ANB301 Eğitsel Oyunlar (Gazi Mustafa Kemal Spor Salonu)
                TUESDAY|3|13:30|15:20|ANB307 Toplumsal Duyarlılık ve Katkı I (D. 2)
                WEDNESDAY|3|09:30|12:20|ANB309 Spor Yaralanmaları ve Rehabilitasyon (D. 6)
                WEDNESDAY|3|13:30|15:20|ANB303 Çocuk ve Spor (D. 6)
                WEDNESDAY|3|15:30|17:20|Seçmeli Sporlar 3 ANB510 Plaj Voleybolu (Plaj Voleybolu Sah.) ANB513 Spor Tırmanış (Tırmanma Duvarı) ANB514 Tai-Chi (Mücadele Sporları Salonu)
                THURSDAY|3|08:30|10:20|Seçmeli Teorik Dersler 3 ANB375 Müsabakaya Psikolojik Hazırlık (D. 3 )
                THURSDAY|3|10:30|12:20|Seçmeli Teorik Dersler 3 ANB371 Mesleki İngilizce (D. 3 ) ANB373 Antrenman Planı ve Periyotlaması (D. 7) ANB379 Laboratuvar Performans Testleri (Egzersiz Fizyolojisi ve Performans Lab.) ANB381 SPA ve Wellness Uygulamaları (Atletik Terapi ve Reh. Salonu) ANB387 Ezgersiz Psikolojisi (D. 11 )
                THURSDAY|3|13:30|14:20|Uzmanlık Alan Eğitimi 3 ANB351 Atletizm UD3 (Atletizm Pisti) ANB351 Basketbol UD3 (Gazi Mustafa Kemal Spor Salonu) ANB351 Futbol UD3 (SD) ANB351 Güreş UD3 (SD) ANB351 Hentbol UD3 (SD) ANB351 Okçuluk UD3 (Okçuluk Sal. Stadyum) ANB351 Voleybol UD3 (Mavi Salon) ANB351 Tenis UD3 (K 5-6) ANB351 Yüzme UD3 (Yüzme Havuzu)
                THURSDAY|3|14:30|15:20|Uzmanlık Alan Eğitimi 3 ANB351 Atletizm UD3 (Atletizm Pisti) ANB351 Basketbol UD3 (Gazi Mustafa Kemal Spor Salonu) ANB351 Futbol UD3 (SD) ANB351 Güreş UD3 (SD) ANB351 Hentbol UD3 (SD) ANB351 Okçuluk UD3 (Okçuluk Sal. Stadyum) ANB351 Voleybol UD3 (Mavi Salon) ANB351 Tenis UD3 (K 7-8) ANB351 Yüzme UD3 (Yüzme Havuzu)
                THURSDAY|3|15:30|16:20|Uzmanlık Alan Eğitimi 3 ANB351 Atletizm UD3 (SD) ANB351 Basketbol UD3 (SD) ANB351 Futbol UD3 (Zeki Budak Sahası ) ANB351 Güreş UD3 (Mücadele Sporları Salonu) ANB351 Hentbol UD3 (Mavi Salon) ANB351 Okçuluk UD3 (SD) ANB351 Voleybol UD3 (SD) ANB351 Tenis UD3 (K 5-6) ANB351 Yüzme UD3 (Yüzme Havuzu)
                THURSDAY|3|16:30|17:20|Uzmanlık Alan Eğitimi 3 ANB351 Atletizm UD3 (SD) ANB351 Basketbol UD3 (SD) ANB351 Futbol UD3 (Zeki Budak Sahası ) ANB351 Güreş UD3 (Mücadele Sporları Salonu) ANB351 Hentbol UD3 (Mavi Salon) ANB351 Okçuluk UD3 (SD) ANB351 Voleybol UD3 (SD) ANB351 Tenis UD3 (K 7-8) ANB351 Yüzme UD3 (Yüzme Havuzu)
                FRIDAY|3|08:30|09:20|Seçmeli Sporlar 3 ANB510 Plaj Voleybolu (SD) ANB513 Spor Tırmanış (SD) ANB514 Tai-Chi (SD)
                FRIDAY|3|09:30|11:20|ANB305 Fiziksel Uygunluk (D. 8)
                FRIDAY|3|14:30|17:20|ANB311 Atletik Performans Geliştirme Yöntemleri II (D. 8)
                MONDAY|4|08:30|09:20|ANB405 İŞME Uygulaması I Doç. Dr. Ali IŞIN Öğr. Gör. Dr. Soner ÖZDEMİR Antrenör İlkay KAYA Doç. Dr. İlkay ORHAN Prof. Dr. Asuman SAHAN GÜRLER Öğr. Gör. Dr. Nazmi BAYKÖSE
                MONDAY|4|09:30|10:20|ANB405 İŞME Uygulaması I Doç. Dr. Ali IŞIN Öğr. Gör. Dr. Soner ÖZDEMİR Antrenör İlkay KAYA Doç. Dr. İlkay ORHAN Prof. Dr. Asuman SAHAN GÜRLER
                MONDAY|4|10:30|12:20|ANB403 Bilimsel Araştırma Yöntemleri (D. 4)
                MONDAY|4|12:30|13:20|ANB401 Araştırma Projesi I
                MONDAY|4|13:30|14:20|ANB405 İŞME Uygulaması I Öğr. Gör. Dr. M.Fatih ERÖZ Öğr. Gör. Dr. Nazmi BAYKÖSE
                MONDAY|4|14:30|15:20|ANB405 İŞME Uygulaması I Öğr. Gör. Dr. M.Fatih ERÖZ
                MONDAY|4|15:30|16:20|ANB405 İŞME Uygulaması I Öğr. Gör. Tufan DAĞSEVEN Prof. Dr. Adnan TURGUT
                MONDAY|4|16:30|17:20|ANB405 İŞME Uygulaması I Öğr. Gör. Dr. Rıza DARENDELİOĞLU Öğr. Gör. Tufan DAĞSEVEN Öğr. Gör. Dr. Mehmet Ali ÖZÇELİK Prof. Dr. Adnan TURGUT
                TUESDAY|4|10:30|12:20|ANB405 İŞME Uygulaması I
                TUESDAY|4|12:30|13:20|ANB401 Araştırma Projesi I
                TUESDAY|4|13:30|17:20|ANB405 İŞME Uygulaması I
                WEDNESDAY|4|10:30|12:20|ANB405 İŞME Uygulaması I
                WEDNESDAY|4|12:30|13:20|ANB405 İŞME Uygulaması I Öğr. Gör. Dr. Rıza DARENDELİOĞLU Öğr. Gör. Dr. Mehmet Ali ÖZÇELİK
                WEDNESDAY|4|13:30|17:20|ANB405 İŞME Uygulaması I
                THURSDAY|4|10:30|12:20|ANB405 İŞME Uygulaması I
                THURSDAY|4|13:30|17:20|ANB405 İŞME Uygulaması I
                FRIDAY|4|10:30|12:20|ANB405 İŞME Uygulaması I
                FRIDAY|4|13:30|17:20|ANB405 İŞME Uygulaması I
            """.trimIndent()
        ),
    )

    private fun parseRows(source: String): List<SourceRow> = source.lineSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .mapNotNull { line ->
            val fields = line.split('|', limit = 5)
            if (fields.size != 5) return@mapNotNull null
            SourceRow(
                day = ScheduleDay.valueOf(fields[0]),
                year = fields[1].toIntOrNull() ?: return@mapNotNull null,
                start = fields[2],
                end = fields[3],
                cellText = fields[4]
            )
        }
        .toList()

    private fun entries(rows: List<SourceRow>): List<ScheduleEntry> = rows.flatMap { row ->
        val matches = courseCodePattern.findAll(row.cellText).toList()
        if (matches.isEmpty()) return@flatMap emptyList()
        val groupLabel = row.cellText.substring(0, matches.first().range.first).trim()
        matches.mapIndexed { index, match ->
            val segmentEnd = matches.getOrNull(index + 1)?.range?.first ?: row.cellText.length
            var courseText = row.cellText.substring(match.range.last + 1, segmentEnd).trim()
            var classroom = ""

            trailingParenthetical.find(courseText)?.let { suffix ->
                val room = suffix.groupValues[1].trim()
                if (isClassroom(room)) {
                    classroom = room
                    courseText = courseText.substring(0, suffix.range.first).trim()
                }
            }

            val instructorMatch = instructorMarker.find(courseText)
            val instructor = instructorMatch?.let { courseText.substring(it.range.first).trim() }.orEmpty()
            if (instructorMatch != null) {
                courseText = courseText.substring(0, instructorMatch.range.first).trim()
            }

            val elective = courseText.contains("(SD)", ignoreCase = true) ||
                groupLabel.startsWith("Seçmeli", ignoreCase = true)
            courseText = courseText.replace(Regex("""\s*\(SD\)\s*$"""), "").trim()
            val note = groupLabel.takeIf(String::isNotBlank)
            val courseType = when {
                courseText.contains("uygulama", ignoreCase = true) ||
                    courseText.contains("lab", ignoreCase = true) -> "Uygulama"
                elective -> "Seçmeli"
                else -> "Ders"
            }

            ScheduleEntry(
                day = row.day,
                startTime = row.start,
                endTime = row.end,
                courseCode = match.groupValues[1].replace(" ", ""),
                courseName = courseText.ifBlank { row.cellText.trim() },
                instructor = instructor,
                classroom = classroom,
                courseType = courseType,
                note = note
            )
        }
    }

    private fun isClassroom(value: String): Boolean {
        val room = value.trim()
        return room.startsWith("D.", ignoreCase = true) ||
            room.equals("Online", ignoreCase = true) ||
            room.startsWith("K ") ||
            room.contains("sal", ignoreCase = true) ||
            room.contains("havuz", ignoreCase = true) ||
            room.contains("pisti", ignoreCase = true) ||
            room.contains("sah", ignoreCase = true) ||
            room.contains("lab", ignoreCase = true) ||
            room.contains("fitness", ignoreCase = true) ||
            room.contains("konferans", ignoreCase = true)
    }

    fun schedulesFor(department: String?): List<ClassSchedule>? {
        val selectedDepartment = department ?: return null
        val source = sourceByDepartment[selectedDepartment] ?: return null
        val yearLabels = listOf(
            ClassSchedules.FIRST_YEAR,
            ClassSchedules.SECOND_YEAR,
            ClassSchedules.THIRD_YEAR,
            ClassSchedules.FOURTH_YEAR
        )
        val rows = parseRows(source.rows)
        return yearLabels.mapIndexed { index, classYear ->
            val year = index + 1
            ClassSchedule(
                faculty = ClassSchedules.SPORT_SCIENCES_FACULTY,
                department = selectedDepartment,
                classYear = classYear,
                academicYear = ACADEMIC_YEAR,
                term = TERM,
                updatedAt = source.updatedAtByYear[index],
                sourcePage = year,
                sourceUrl = source.url,
                entries = entries(rows.filter { it.year == year })
            )
        }
    }
}
