package com.good4.schedule.domain

/**
 * 2026–2027 güz yarıyılı Güzel Sanatlar Fakültesi programları.
 * Kaynaklardaki birleştirilmiş ders hücreleri çalışma saatlerine ayrıştırılır;
 * ekranda her ders saati bağımsız satır olarak gösterilir.
 */
internal object FineArtsSchedules {
    private const val ACADEMIC_YEAR = "2026-2027"
    private const val TERM = "Güz Yarıyılı"

    private data class ScheduleSource(
        val department: String,
        val url: String,
        val updatedAt: String,
        val rows: String
    )

    private data class SourceRow(
        val day: ScheduleDay,
        val year: Int,
        val start: String,
        val end: String,
        val cellText: String
    )

    private data class CourseDetails(
        val code: String,
        val name: String,
        val instructor: String,
        val classroom: String,
        val courseType: String,
        val section: String?,
        val note: String?
    )

    private data class ClassPeriod(val start: String, val end: String)

    private val classPeriods = listOf(
        ClassPeriod("08:30", "09:20"),
        ClassPeriod("09:30", "10:20"),
        ClassPeriod("10:30", "11:20"),
        ClassPeriod("11:30", "12:20"),
        ClassPeriod("12:30", "13:20"),
        ClassPeriod("13:30", "14:20"),
        ClassPeriod("14:30", "15:20"),
        ClassPeriod("15:30", "16:20"),
        ClassPeriod("16:30", "17:20"),
        ClassPeriod("17:30", "18:20")
    )

    private val courseCodePattern = Regex(
        """(?:^|[^A-ZÇĞİÖŞÜ])((?:ATA|KPD|KRY|TDB|YBD|YDB|RES|HEYK|GRA|SER|FOT|MZB|FLM|FİLM|GEL|MOD)\s*-?\s*\d{3}(?:-\d{3})?|FLM)(?!\d)""",
        RegexOption.IGNORE_CASE
    )

    private val instructorMarker = Regex(
        """(?:Dr\.?\s*Öğr\.?\s*(?:Üyesi|Üye\.?|Üy\.?)|Öğr\.?\s*Üyesi\s*Dr\.?|Prof(?:esör)?\.?(?:\s*Dr\.?)?|Doç(?:ent)?\.?\s*(?:Dr\.?)?|Öğr\.?\s*Gör\.?(?:\s*Dr\.?)?|Öğrt\.?\s*Gör\.?|Arş\.?\s*Gör\.?(?:\s*Dr\.?)?|Araş\.?\s*Gör\.?(?:\s*Dr\.?)?)\s*""",
        RegexOption.IGNORE_CASE
    )

    private val classroomPattern = Regex(
        """#\s*\d+|Mac\s*Lab\.?|Piyano\s*\.?\s*Lab\.?|SKS\s+Piyano\s+L\.?|Bilgisayar\s*Lab\.?(?:\s+C\s+BLOK\s+Kat\s+\d+)?|Sanal\s*S\.?\s*\d+|SERİK\s+MYO|GSF\s+\d{3}|\bAS\s*-?\s*\d{2}\b|\bZ\d{2}\b|Atölye\s+[A-ZÇĞİÖŞÜ]{1,3}\s*-\s*\d{1,3}|\b[A-ZÇĞİÖŞÜ]{1,3}\s*-\s*\d{1,3}\b|\bK\.?\s*Amfi\b|\b(?:Amfi|Derslik|Atölye|Sınıf)\s*\d+\b|\b(?:Mavi|Kırmızı)\s+(?:Salon|Atölye|Amfi)\b|\b\d{3}\b""",
        RegexOption.IGNORE_CASE
    )

    private val sectionPattern = Regex(
        """\b(?:([A-D])\s*[-–]?\s*(\d+)\.?\s*Şube|(\d+)\.?\s*Şube|([A-D])\s*(?:Şubesi|Grubu)|(\d+)\.?\s*Grubu)\b""",
        RegexOption.IGNORE_CASE
    )

    private val selectionPattern = Regex(
        """\b(?:seçmeli|s\.?\s*d\.?)\b""",
        RegexOption.IGNORE_CASE
    )

    private val knownUnmarkedInstructors = listOf(
        "SERAP DUMAN İNCE", "SERAP DUMAN INCE",
        "MÜMİN BARIŞ", "MUMIN BARIS",
        "AYŞEN OLUK ERSÜMER", "AYSEN OLUK ERSUMER",
        "BİLAL ARIK", "TÜLİN ARSEVEN", "TULIN ARSEVEN",
        "OĞUZHAN ERSÜMER", "OGUZHAN ERSUMER",
        "RANA İĞNECİ SÜZEN", "RANA IGNECI SÜZEN", "RANA İĞNECİ", "RANA İĞNECCI",
        "RANA SÜZEN", "ZİHNİ", "ZIHNI", "BİRTAN BOZLU", "EZGİ BİLGİN", "EZGI BILGIN",
        "BERNA GÜNDOĞDU", "BURCAK AKINCI", "MERAL BAYRAM", "ÖMER YÖNDEM", "OMER YONDEM",
        "MİLLİ EĞİTİM", "ERTUNÇ UKSUL"
    )

    private val sourceByDepartment = mapOf(
        ClassSchedules.PAINTING_DEPARTMENT to ScheduleSource(
            department = ClassSchedules.PAINTING_DEPARTMENT,
            url = "https://webis.akdeniz.edu.tr/uploads/1060/content/%2831533193%29son%20hali%20RESI%CC%87M%20BO%CC%88LU%CC%88MU%CC%88%202026-2027%20YILI%20GU%CC%88Z%20DO%CC%88NEMI%CC%87%20DERS%20PROGRAMI%2010.09.2026.pdf",
            updatedAt = "2026-09-10",
            rows = """
                MONDAY|1|08:30|10:20|TDB101 / TÜRK DİLİ I\nÖğr. Gör. Meral BAYRAM
MONDAY|1|10:30|12:20|YDB101 / İNGİLİZCE I\nÖĞR. GÖR. BERNA GÜNDOĞDU
MONDAY|1|13:30|15:20|ATA101 / ATATÜRK İLK. VE İNK.\nTARİHİ I\nÖğr. Gör. Dr. Fatma ÇETİN\nADIGÜZEL
MONDAY|1|15:30|16:20|KRY 101 / KARİYER PLANLAMA\nÖğr. Gör. Ebru DOĞAN AKTAŞ /\nOrtak Derslik Z-13
MONDAY|2|08:30|14:20|RES217 / DESEN III\nÖğr. Gör. Hande RASTGELDİ / Atölye\n210
MONDAY|2|14:30|17:20|SEÇMELİ RES285 / SULUBOYA\nUYGULAMALARI I\nProf. Dr. Fatih BAŞBUĞ / Atölye 203
MONDAY|3|09:30|11:20|RES389 / ÇAĞDAŞ TÜRK RESMİ I\nProf. Dr. Fatih BAŞBUĞ / Atölye 206
MONDAY|3|11:30|13:20|RES303 / ÖZGÜN BASKI I\nDoç. Nevin YAVUZ AZERİ / Atölye Z-16
MONDAY|3|13:30|14:20|RES303 / ÖZGÜN BASKI I\nDoç. Nevin YAVUZ AZERİ / Atölye Z-166
MONDAY|3|14:30|17:20|RES303 / ÖZGÜN BASKI I\nDoç. Nevin YAVUZ AZERİ / Atölye Z-16
MONDAY|4|09:30|12:20|RES437 / GÜNCEL SANAT I\nDoç. Ilgaz ÖZGEN TOPÇUOĞLU / Atölye 205
MONDAY|4|13:30|17:20|RES415 / DENEYSEL ATÖLYE V\nProf. Umut KAYAPINAR / Atölye 205
TUESDAY|1|10:30|12:20|RES103 / DESEN I\nÖğr. Gör. Hande RASTGELDİ /\nAtölye 210
TUESDAY|1|13:30|17:20|RES103 / DESEN I\nÖğr. Gör. Hande RASTGELDİ /\nAtölye 210
TUESDAY|2|09:30|17:20|RES201 / RESİM ATÖLYE I (B Grubu)\nDoç. Ilgaz Özgen TOPÇUOĞLU / Atölye\n202
TUESDAY|3|09:30|17:20|RES301 / RESİM ATÖLYE III (A Grubu)\nProf. Umut KAYAPINAR / Atölye 212
TUESDAY|4|08:30|18:20|RES427 / RESİM ATÖLYE V (B Grubu)\nProf. Dr. Fatih BAŞBUĞ / Atölye 205
WEDNESDAY|1|10:30|12:20|RES101 / TEMEL SANAT EĞİTİMİ I\nÖğr. Gör. Hande RASTGELDİ /\nAtölye 210
WEDNESDAY|1|13:30|17:20|RES101 / TEMEL SANAT EĞİTİMİ I\nÖğr. Gör. Hande RASTGELDİ /\nAtölye 210
WEDNESDAY|2|08:30|10:20|RES293 / SANAT TARİHİ III\nÖğr. Gör. Hayal Güleç / Derslik 302
WEDNESDAY|2|10:30|18:20|RES201/ RESİM ATÖLYE I (A Grubu)\nDoç. Nevin YAVUZ AZERİ / Atölye 202
WEDNESDAY|3|09:30|17:20|RES301 / RESİM ATÖLYE III (B Grubu)\nProf. Dr. Semih BÜYÜKKOL / Atölye 212
WEDNESDAY|4|08:30|18:20|RES427 / RESİM ATÖLYE V (A Grubu)\nProf. Sadettin SARI / Atölye 205
THURSDAY|1|09:30|15:20|RES119 / RESİM TEKNOLOJİLERİ\nVE UYGULAMALARI I / Atölye 203\nDoç. Sabriye ÖZTÜTÜNCÜ (A\nGrubu)\nDr. Öğr. Ü. Mehmet AYDIN AVCI (B\nGrubu)
THURSDAY|3|11:30|13:20|RES395 / SANAT FELSEFESİ\nProf. Dr. Semih BÜYÜKKOL / Derslik 206
THURSDAY|3|14:30|17:20|RES 397/ SEMBOLLER VE SANAT\nDoç. Dr. Terlan M. AZİZZADE / Derslik 302
THURSDAY|4|11:30|12:20|RES481 / RUS MODERNİZMİ\nDoç. Dr. Terlan M. AZİZZADE Derslik 302
THURSDAY|4|12:30|14:20|RES481 / RUS MODERNİZMİ\nDoç. Dr. Terlan M. AZİZZADE / Derslik 302
THURSDAY|4|14:30|16:20|RES429 / SANAT ESERLERİ ANALİZİ\nProf. Dr. Semih BÜYÜKKOL / Derslik 206
FRIDAY|1|10:30|12:20|RES105 / SANAT TARİHİ I\nÖğr. Gör. Hayal Güleç / Kırmızı\nAmfi Z-13
FRIDAY|2|08:30|10:20|RES287 / MİTOLOJİ VE İKONOGRAFİ I\nÖğr. Gör. Hayal Güleç / Derslik 207
FRIDAY|2|10:30|13:20|SEÇMELİ RES273 / PERSPEKTİF I /\nAtölye 203\nDoç. Sabriye ÖZTÜTÜNCÜ (A Grubu)\nDr. Öğr. Ü. Mehmet AYDIN AVCI (B\nGrubu)
FRIDAY|3|14:30|17:20|RES385 / MODERN DÜŞÜNCE VE SANAT I\nDoç. Dr. Terlan M. Azizzade / Derslik 302
FRIDAY|4|09:30|10:20|RES471 / SANAT VE ÇEVRE\nDoç. Dr. Terlan M.Azizzade / Derslik 302
FRIDAY|4|10:30|11:20|RES471 / SANAT VE ÇEVRE\nDoç. Dr. M.Azizzade / Derslik 302
FRIDAY|4|11:30|12:20|RES471 / SANAT VE ÇEVRE\nDoç. Dr. Terlan M.Azizzade / Derslik 302
FRIDAY|4|14:30|17:20|RES485 / SERGİLEME YÖNTEMLERİ / Atölye\n205\nDoç. Sabriye ÖZTÜTÜNCÜ (A Grubu)\nDr. Öğr. Ü. Mehmet AYDIN AVCI (B Grubu)
            """.trimIndent()
        ),
        ClassSchedules.SCULPTURE_DEPARTMENT to ScheduleSource(
            department = ClassSchedules.SCULPTURE_DEPARTMENT,
            url = "https://webis.akdeniz.edu.tr/uploads/1060/content/%2831617353%29HEYKEL%20SON%20HALI%202026_GUZ_lisans%20%281%29.docx.pdf",
            updatedAt = "2026-09-17",
            rows = """
                MONDAY|1|08:30|10:20|YDB101 İngilizce\nÖğr. Gör. Berna Gündoğdu
MONDAY|1|10:30|12:20|TDB101 Türk Dili I\nÖğr. Gör. Meral Bayram
MONDAY|1|13:30|15:20|ATA101 Atatürk İlkeleri ve İnkılap Tarihi I\nÖğr. Gör. Fatma Çetin Adıgüzel
MONDAY|1|15:30|16:20|KRY 101 Kariyer Planlama Öğr. Gör. Ebru Doğan\nAKTAŞ\nZ-13
MONDAY|2|08:30|12:20|HEYK205 Desen III\nDr. Öğr. Ü. Hülya Bozbıyık Uysal Z-07
MONDAY|2|12:30|18:20|HEYK 283 Metal Atölye\nDoç. Hanife Neris Yüksel B06
MONDAY|3|08:30|10:20|HEYK 351 Estetik\nDoç. Dr. Özcan Özkarakoç 302
MONDAY|3|10:30|12:20|HEYK 359/ Çağdaş Sanat Tarihi I\nDoç. Dr. Terlan M. Azizzade 302
MONDAY|3|12:30|15:20|HEYK343 Plastik Makyaj I\nÖğr. Gör. Işık Aslıhan -B02
MONDAY|3|15:30|16:20|HEYK 341 Form Araştırmaları I\nDr. Öğr. Ü. Selda Özturan B03
MONDAY|3|16:30|18:20|HEYK 341 Form Araştırmaları I\nDr. Öğr. Ü. Selda Özturan 207
MONDAY|4|09:30|12:20|HEYK459 Heykel Tasarım Uygulamaları I
MONDAY|4|15:30|16:20|HEYK 441 Anıt Heykel I\nDoç. Dr. Özcan Özkarakoç Z-10
MONDAY|4|16:30|17:20|HEYK 441 Anıt Heykel I\nDoç. Dr. Özcan Özkarakoç Z-1
MONDAY|4|17:30|18:20|HEYK 441 Anıt Heykel I\nDoç. Dr. Özcan Özkarakoç Z-10
TUESDAY|3|08:30|14:20|HEYK 345 Heykel Atölye III\nDoç. Dr. Özcan Özkarakoç, Doç. Hanife Neris Yüksel,\nDr. Öğr. Ü. Selda Özturan, Dr. Öğr. Ü. Hülya Bozbıyık\nUysal, Öğr. Gör. Işık Aslıhan, Arş. Gör. Mukaddes\nYörük Kalaycıoğlu
TUESDAY|3|14:30|17:20|HEYK 355 Fonksiyonel Heykel
TUESDAY|4|08:30|14:20|HEYK431 Heykel Atölye V\nDoç. Dr. Özcan Özkarakoç, Doç. Hanife Neris Yüksel,\nDr. Öğr. Ü. Selda Özturan, Dr. Öğr. Ü. Hülya Bozbıyık\nUysal, Öğr. Gör. Işık Aslıhan
TUESDAY|4|14:30|18:20|HEYK459 Heykel Tasarım Uygulamaları I\nDoç. Dr. Özcan Özkarakoç, Doç. Hanife Neris Yüksel,\nDr. Öğr. Ü. Selda Özturan, Dr. Öğr. Ü. Hülya Bozbıyık\nUysal, Öğr. Gör. Işık Aslıhan
WEDNESDAY|1|08:30|12:20|HEYK103 Desen I\nDr. Öğr. Ü. Selda Özturan Z-07
WEDNESDAY|1|13:30|18:20|HEYK111 Modelaj I\nDoç. Hanife Neris Yüksel B05
WEDNESDAY|2|08:30|10:20|HEYK 241 Sanat Tarihi III\nÖğr. Gör. Hayal Güleç- 302
WEDNESDAY|2|15:30|18:20|HEYK 279 Sanatsal Anatomi\nDoç. Dr. Özcan Özkarakoç Z10
WEDNESDAY|3|12:30|18:20|HEYK 363 Serbest Anlatım Atölyesi\nÖğr. Gör. Işık Aslıhan -B02
WEDNESDAY|4|08:30|10:20|HEYK435 Heykel ve Çevre I\nDr. Öğr. Ü. Hülya Bozbıyık Uysal 207
WEDNESDAY|4|10:30|13:20|HEYK 445 Desen VII\nDoç. Dr. Özcan Özkarakoç Z-10
WEDNESDAY|4|15:30|17:20|HEYK 439 Heykel Çözümlemeleri\nDr. Öğr. Ü. Selda Özturan 302
WEDNESDAY|4|17:30|18:20|HEYK 439 Heykel Çözümlemeleri
THURSDAY|1|09:30|15:20|HEYK 101 Temel Sanat Eğitimi I\nÖğr. Gör. Işık Aslıhan B02
THURSDAY|2|08:30|09:20|HEYK235 Heykel Atölye I\nDoç. Dr. Özcan Özkarakoç, Doç. Hanife Neris Yüksel,\nDr. Öğr. Ü. Selda Özturan, Dr. Öğr. Ü. Hülya Bozbıyık\nUysal,
THURSDAY|2|09:30|14:20|HEYK235 Heykel Atölye I\nDoç. Dr. Özcan Özkarakoç, Doç. Hanife Neris Yüksel,\nDr. Öğr. Ü. Selda Özturan, Dr. Öğr. Ü. Hülya Bozbıyık\nUysal
THURSDAY|2|14:30|17:20|HEYK213 Teknik Çizim ve Perspektif\nDoç. Dr. Özcan Özkarakoç As 08
THURSDAY|3|08:30|10:20|HEYK361 Sanat Sosyolojisi\nDoç. Dr. Terlan M. Azizzade 302
THURSDAY|4|14:30|16:20|HEYK 451 Türk Heykel Tarihi\nDoç. Hanife Neris Yüksel 207
THURSDAY|4|16:30|18:20|HEYK459 Heykel Tasarım Uygulamaları I
FRIDAY|1|10:30|12:20|HEYK117 Sanat Tarihi I\nÖğr. Gör. Hayal Güleç- Z13
FRIDAY|1|13:30|15:20|HEYK 115 Heykel Kalıp Döküm Teknikleri I\nÖğr. Gör. Işık Aslıhan B05
FRIDAY|2|08:30|10:20|HEYK277 Mitoloji ve İkonografi-I\nÖğr. Gör. Hayal Güleç- 207
FRIDAY|2|10:30|13:20|HEYK 211 Seramik Heykel Öğr. Gör. Işık Aslıhan B05
            """.trimIndent()
        ),
        ClassSchedules.GRAPHIC_DESIGN_DEPARTMENT to ScheduleSource(
            department = ClassSchedules.GRAPHIC_DESIGN_DEPARTMENT,
            url = "https://webis.akdeniz.edu.tr/uploads/1060/content/2026-2027%20gu%CC%88z%20yar%C4%B1y%C4%B1l%C4%B1%20ders%20program%C4%B1%201.pdf",
            updatedAt = "2026-09-17",
            rows = """
                MONDAY|2|13:30|17:20|GRA 273 Tipografi III\nDr. Öğr. Üyesi Bekir KİRİŞCAN - Mac Lab.
MONDAY|3|08:30|12:20|GRA 373 İllüstrasyon I\nDoç. Dr. Süleyman ÖZDERİN – AS-08
MONDAY|3|13:30|16:20|GRA 385 Grafik Tasarımda Mizah\nÖğr. Gör. Dr. Oktay BARKIN – 211
MONDAY|4|09:30|12:20|GRA 475 Yeni Medya ve Tasarımı\nArş. Gör. Günnar YAĞCILAR – 211
MONDAY|4|13:30|16:20|GRA 479 Deneysel Grafik Tasarım\nDoç. Dr. Süleyman ÖZDERİN – AS-08
TUESDAY|1|08:30|10:20|YBD 101 İngilizce I – Mavi Amfi\nÖğr. Gör. Burcak AKINCI
TUESDAY|1|10:30|12:20|ATA 101-102 Atatürk İl. ve İnk. Tar. I-II\nMavi Amfi\nÖğr. Gör. Dr. Bengi KÜMBÜL UZUNSAKAL
TUESDAY|1|13:30|15:20|TDB 101 Türk Dili I – Mavi Amfi\nÖğr. Gör. Meral BAYRAM
TUESDAY|1|15:30|16:20|KRY 101 Kariyer Planlama\nÖğr. Gör. Ebru DOĞAN AKTAŞ -
TUESDAY|2|09:30|12:20|GRA 281 Görüntü İşleme Teknikleri I\nÖğr. Gör. Tülin CANDEMİR – 130 MAC
TUESDAY|2|13:30|16:20|GRA 293 İnternet Reklamcılığı\nÖğr. Gör. Dr. Oktay BARKIN – AS-08
TUESDAY|3|10:30|12:20|GRA 397 Grafik Tasarım Tarihi\nArş. Gör. Günnar YAĞCILAR - 211
TUESDAY|3|13:30|17:20|GRA 371 Grafik Atölye III\nDoç. Dr. Aydın ZOR - Mac Lab.
TUESDAY|4|13:30|17:20|GRA 471 Grafik Atölye V\nDoç. Dr. Süleyman ÖZDERİN – AS-07-E\nÖğr. Gör. Tülin CANDEMİR – AS-09\nArş. Gör. Günnar YAĞCILAR – 211
WEDNESDAY|1|08:30|12:20|GRA 177 Bilgisayar Destekli Tas. Giriş\nDoç. Dr. Aydın ZOR - Mac Lab.
WEDNESDAY|1|13:30|18:20|GRA 171 Grafik Tas. Eğt. I –\nÖğr. Gör. Bilge YÜKSEL KİRİŞCAN - 211
WEDNESDAY|2|10:30|12:20|GRA 279 Sanat Tarihi III\nÖğr. Gör. Hayal GÜLEÇ – 305
WEDNESDAY|2|13:30|17:20|GRA 289 Editoryal Tasarım\nÖğr. Gör. Tülin CANDEMİR – 130 Mac
WEDNESDAY|3|09:30|12:20|GRA 393 Özgün Baskı\nÖğr. Gör. Kürşat KARIŞMAZ – Z-16
WEDNESDAY|3|13:30|15:20|GRA 377 Temel Fotoğraf Bilgisi\nÖğr. Gör. Serap DUMAN İNCE - AS-08
WEDNESDAY|4|08:30|12:20|GRA 473 Kurumsal Kimlik Kılavuzu Tas.\nÖğr. Gör. Bilge YÜKSEL KİRİŞCAN – 211
WEDNESDAY|4|13:30|16:20|GRA 483 Hareketli Görüntü Tasarımı\nÖğr. Gör. Dr. Oktay BARKIN – AS-09
THURSDAY|1|08:30|12:20|GRA 173 Tipografi I\nDr. Öğr. Üyesi Bekir KİRİŞCAN – AS-08
THURSDAY|1|13:30|17:20|GRA 175 Grafik Desen Doç. Dr. Süleyman\nÖZDERİN - Z 07
THURSDAY|2|09:30|13:20|GRA 271 Grafik Atölye I\nArş. Gör. Günnar YAĞCILAR - 211
THURSDAY|2|16:30|18:20|GRA 291 Baskı Teknikleri I\nDoç. Dr. Aydın ZOR - 208
THURSDAY|3|13:30|16:20|GRA 387 Sosyal Kampanya Tasarımı\nÖğr. Gör. Tülin CANDEMİR– 130 Mac
THURSDAY|4|09:30|12:20|GRA 487 Disiplinlerarası Tasarım\nÖğr. Gör. Tülin CANDEMİR – 130 Mac Lab.
THURSDAY|4|13:30|16:20|GRA 477 Toplumsal Duyarlılık ve Katkı\nDoç. Dr. Aydın ZOR – 211
FRIDAY|1|10:30|12:20|GRA 177 Sanat Tarihi I\nÖğr. Gör. Hayal GÜLEÇ - KIRMIZI AMFİ
FRIDAY|3|09:30|12:20|GRA 383 Tasarım Okumaları\nÖğr. Gör. Dr. Oktay BARKIN – 211
FRIDAY|3|14:30|18:20|GRA 389 Grafik Tas. İnovasyonÖğr. Gör.\nKürşat KARIŞMAZ – 130 Mac.
            """.trimIndent()
        ),
        ClassSchedules.CERAMIC_DEPARTMENT to ScheduleSource(
            department = ClassSchedules.CERAMIC_DEPARTMENT,
            url = "https://webis.akdeniz.edu.tr/uploads/1060/content/%2831433841%29Ders_programi%202026_2027%20gu%CC%88z%20%28SERAMIK%29.docx.pdf",
            updatedAt = "2026-09-17",
            rows = """
                MONDAY|1|08:30|10:20|TDB101 / TÜRK DİLİ I\nSanal S.10 Meral Bayram
MONDAY|1|10:30|12:20|YBD101 / İNGİLİZCE I\nÖğr. Gör. Güher Ceylan Kuşoğlu
MONDAY|1|13:30|15:20|ATA101 / ATATÜRK İLKE VE İNK. TARİHİ I\nÖğr. Gör. Dr. ÇETİN ADIGÜZEL
MONDAY|1|15:30|16:20|KPD101 KARİYER PLANLAMA\nÖğr. Gör. Ebru Doğan Aktaş
MONDAY|2|10:30|12:20|SER209 / SERAMİK KİMYASI\nArş. Gör. Dr. Cemal Aslan\n/z-12
MONDAY|2|15:30|18:20|SER 241 GELENEKSEL SERAMİK SANATI\nÖğr. Gör. E. Mülayim 206
MONDAY|3|09:30|12:20|SER313/ SERAMİK CAM TEKNOLOJİSİ III\nDoç. K. Tizgöl / 208
MONDAY|3|12:30|18:20|SER 319 / CAM TASARIMI\nDr.Öğr.Ü. E. Güner / Z-20
MONDAY|4|08:30|12:20|*SER 435 / SERAMİK ATÖLYE I Dr. Öğr. Üyesi Enver\nGÜNER,z-20
MONDAY|4|12:30|14:20|SER 461 / SERGİLEME ve SUNUM TEKNİKLERİ I\nDoç. F. Işıktan 206
MONDAY|4|15:30|17:20|SER411 / BİLİMSEL ARAŞTIRMA YÖNTEMLERİ\nDoç. F. Işıktan / 207
TUESDAY|1|09:30|13:20|SER103 / DESEN I\nDr.Öğr.Ü. E. Güner /Z-07
TUESDAY|2|08:30|12:20|SER213 / SERAMİK ŞEKİLLENDİRME YÖNTEMLERİ\nÖğr. Gör. E. Mülayim / Z-19 şube I
TUESDAY|2|13:30|17:20|SER213 / SERAMİK ŞEKİLLENDİRME YÖNTEMLERİ\nÖğr. Gör. E. Mülayim / Z-19 şube II
TUESDAY|3|08:30|11:20|SER 347/ SERAMİK RESTORASYONU VE\nKONSERVASYONU\nDoç. Dr. E. Çetintaş 207
TUESDAY|3|13:30|15:20|SER317 / ESTETİK\nÖğr. Gör. Ezgi BİLGİN Z-12
TUESDAY|4|08:30|13:20|*SER 435 / SERAMİK ATÖLYE I Doç. Kamuran Özlem\nSARNIÇ ,Doç. Figen IŞIKTAN,
TUESDAY|4|13:30|15:20|*SER 435 / SERAMİK ATÖLYE I Doç. Kamuran Özlem\nSARNIÇ ,Doç. Figen IŞIKTAN, Dr. Öğr. Üyesi Enver\nGÜNER,
TUESDAY|4|15:30|16:20|**SER 435 / SERAMİK ATÖLYE I Doç. Kamuran Özlem\nSARNIÇ ,Doç. Figen IŞIKTAN, Dr. Öğr. Üyesi Enver\nGÜNER,
TUESDAY|4|16:30|17:20|*SER 435 / SERAMİK ATÖLYE I Dr. Öğr. Üyesi Enver\nGÜNER,
WEDNESDAY|1|13:30|16:20|SER109 / ALÇI ŞEK. YÖN.TEMLERİ I\nDoç. Dr. E. Çetintaş / B-11
WEDNESDAY|2|08:30|10:20|SER233 SANAT TARİHİ III\nÖğrt. Gör. H. Güleç 302 RES293
WEDNESDAY|2|10:30|12:20|SER239 BİLGİSAYAR DESTEKLİ SERAMİK TASARIMI I\nDoç. Dr. E. Çetintaş / 301
WEDNESDAY|2|13:30|15:20|SER207 / SERAMİK CAM TEKNOLOJİSİ I\nDoç. K. Tizgöl / z-12
WEDNESDAY|2|15:30|18:20|SER243 / YÜZEY TAS. I\nDoç. Kamuran Özlem SARNIÇ / 207
WEDNESDAY|3|09:30|12:20|SER345 / ÇAMUR TORNASINDA SERBEST TASARIM\nÖğr. Gör. E. Mülayim / Z-19
WEDNESDAY|3|13:30|16:20|SER355/ SERAMİK\nHEDİYELİK EŞYA\nÖğr. Gör. E. Mülayim / Z19
WEDNESDAY|3|13:30|16:20|SER 343/ ÇİNİ TASARIMI\nDoç. F. Işıktan / B-12
WEDNESDAY|3|16:30|18:20|SER 321 / ERGONOMİ\n208 Dr.Öğr.Ü. E. Güner
THURSDAY|1|10:30|12:20|SER187 / SANAT TARİHİ I\nÖğrt. Gör. H. Güleç 305 FOT113
THURSDAY|1|12:30|15:20|SER107 / SERAMİĞE GİRİŞ I\nÖğr. Gör. E. Mülayim / Z-19
THURSDAY|2|09:30|11:20|SER237 SERAMİK TARİHİ\nDoç. Özlem Sarnıç / z-12
THURSDAY|2|15:30|18:20|SER249 SER. ASTAR VE BOYA UYG. I\nÖğr. Gör. E. Mülayim / B -14 b
THURSDAY|3|08:30|14:20|SER307/ SER TAS. I END\nDr.Öğr.Ü. E. Güner / B11
THURSDAY|3|08:30|14:20|SER305/ SER.TAS. I SAN.\nDoç. K.Tizgöl / Z-18
THURSDAY|3|15:30|18:20|SER353/MİMARİDE\nSERAMİK Doç. F. Işıktan Z-\n18
THURSDAY|3|15:30|18:20|SER 361 GÜZEL SANATLARDA BİLGİSAYAR DESTEKLİ TASARIM\nDoç. Dr. E. Çetintaş / 301
THURSDAY|4|11:30|14:20|SER 463 PORTFOLYO\nDoç. Kamuran Özlem SARNIÇ AS 09
THURSDAY|4|15:30|18:20|SER447 / ANİMASYON MODELLEME\nÖğr. Gör. Işık aslıhan / Z-19
FRIDAY|1|08:30|13:20|SER101 / TEMEL SANAT EĞİTİMİ I\nDoç. K. Ö. Sarnıç / AS-08
FRIDAY|1|14:30|16:20|SER105 / TEKNİK RESİM\nDoç. Dr. E. Çetintaş / AS-09
FRIDAY|2|08:30|12:20|SER203/ ALÇI MODEL KALIP HAZIRLAMA YÖNT.\nDoç. Dr. E. Çetintaş / B-11
FRIDAY|3|13:30|17:20|SER309/ DEKOR TASARIMI\nDoç. F. Işıktan / B-12
FRIDAY|4|16:30|18:20|SER 471 / UYGULAMA BECERİSİ\nDoç. K. Ö. Sarnıç AS 06
            """.trimIndent()
        ),
        ClassSchedules.MUSIC_DEPARTMENT to ScheduleSource(
            department = ClassSchedules.MUSIC_DEPARTMENT,
            url = "https://webis.akdeniz.edu.tr/uploads/1060/content/%2831479889%29Muzik%20Bolumu%202026-2027%20gu%CC%88z%20lisans%20haftalik%20program.docx.pdf",
            updatedAt = "2026-09-17",
            rows = """
                MONDAY|1|08:30|10:20|TDB 101 Türk Dili I (Z)\nÖğr. Gör. Meral BAYRAM
MONDAY|1|10:30|12:20|YBD 101 İngilizce I (Z)\nÖğr. Gör. Öğr. Gör. Güher Ceylan KUŞOĞLU
MONDAY|1|13:30|15:20|ATA 101 Atatürk İlkeleri ve İnkılap Tarihi I (Z)\nÖğr. Gör. Fatma Çetin ADIGÜZEL
MONDAY|1|15:30|16:20|KPD 101 Kariyer Planlama (Z)\nÖğr. Gör. Ebru Doğan Aktaş
MONDAY|2|09:30|12:20|MZB 227 Armoni I (Z)\nÖğr. Gör. Alev TÜRKAN (311)
MONDAY|2|13:30|16:20|MZB 237 (Seçmeli Ders) Popüler Müzik Tarihi (S)\nÖğr. Gör Ayşegül TULUMCU (311)
MONDAY|3|10:30|12:20|MZB 331 Transkripsiyon (Z)\nÖğr.Gör.Ebru Doğan AKTAŞ (312)
MONDAY|3|13:30|16:20|MZB 325 Müzik Tarihi ve Edebiyatı V (Z)\nÖmer YÖNDEM (K. Amfi)
MONDAY|4|09:30|12:20|MZB 421 Eser Dinleme ve Analiz I (Z)\nÖmer YÖNDEM (K.Amfi)
MONDAY|4|13:30|16:20|MZB 425 Müzik ve Felsefe (Z)\nArş. Gör. Dr. Begüm Fulya ADIZEL (312)
TUESDAY|1|09:30|12:20|MZB 127 Müzik Tarihi ve Edebiyatı I(Z)\nProf. Dr. Sibel PAŞAOĞLU (312)
TUESDAY|1|13:30|14:20|MZB 121 Müzik Teorisi I(Z)\nProf. Dr. Cengiz ŞENGÜL (311)
TUESDAY|1|14:30|16:20|MZB 121 Müzik Teorisi I (Z)\nProf. Dr. Cengiz ŞENGÜL (311)
TUESDAY|2|09:30|12:20|MZB 221 Geleneksel Türk Müz.Tar. ve Teo. Halk Müz.(Z)\nProf. Dr. Cengiz ŞENGÜL (311)
TUESDAY|2|13:30|16:20|MZB 223 Müzik Tarihi ve Edebiyatı III (Z)\nProf. Dr. Sibel PAŞAOĞLU (312)
TUESDAY|3|08:30|12:20|MZB 321 Osmanlıca-Türkçe Paleografya (Z)\nProf. Dr. Mehmet AK (302)
TUESDAY|3|13:30|16:20|MZB 335 (Seçmeli Ders) Jazz Tarihi ve Teorisi I (S)\nÖğr. Gör. Osman Gazi AKÇALI (K.Amfi)
TUESDAY|4|09:30|11:20|MZB 433 (Seçmeli Ders) Seminer (S)\nÖğr. Gör. Osman Gazi AKÇALI (K.Amfi)
TUESDAY|4|11:30|12:20|MZB 433 (Seçmeli Ders) Seminer (S)\nÖğr. Gör. Osman Gazi AKÇALI ((K.Amfi)
TUESDAY|4|13:30|16:20|MZB 429 Alanyazın Tarama I (Z)\nÖğr. Gör. Dr. İlay Bilge DENKTAŞ (302)
WEDNESDAY|1|10:30|12:20|MZB 119 Uygarlık ve Sanat Tarihi (Z)\nÖmer YÖNDEM (K.Amfi)
WEDNESDAY|1|13:30|16:20|MZB 125 Müzikolojiye Giriş, Kuram ve Yaklaşımlar I(Z)\nArş. Gör. Dr. Begüm Fulya ADIZEL (K.Amfi)
WEDNESDAY|2|09:30|12:20|MZB 235 (Seçmeli Ders) Türk Halk Müziği Repertuarı (S)\nÖğr. Gör. Osman Gazi AKÇALI (311)
WEDNESDAY|2|13:30|15:20|MZB 225 Etnomüzikolojiye Giriş, Kuram ve Yaklaşımlar\n(Z) Öğr. Gör. Alev TÜRKAN (312)
WEDNESDAY|2|15:30|17:20|MZB 233 Osmanlıca I (Z)\nProf. Dr. Mehmet AK (312)
WEDNESDAY|3|10:30|12:20|MZB 333 Seçmeli Ders (Etnografya ve Folklor)\nProf. Dr. Sibel PAŞAOĞLU (312)
WEDNESDAY|3|13:30|16:20|MZB 323 Müzikte Bil. Araştırma Yön. ve Yayın Etiği I (Z)\nProf. Dr. Cengiz ŞENGÜL (311)
WEDNESDAY|4|13:30|14:20|MZB 431 (Seçmeli Ders) Müzik ve Toplum (S)\nÖmer YÖNDEM (313)
WEDNESDAY|4|14:30|15:20|MZB 431(Seçmeli Ders) Müzik ve Toplum (S)\nÖmer YÖNDEM (313)
WEDNESDAY|4|15:30|16:20|MZB 431 (Seçmeli Ders) Müzik ve Toplum (S)\nÖmer YÖNDEM (313)
THURSDAY|1|09:30|12:20|MZB 123 Solfej ve Dikte I (Z)\nÖğr. Gör. Dr. Hanifi KERVANCIOĞLU (311)
THURSDAY|1|13:30|17:20|MZB 133 Piyano I (Z)\nÖğr.Gör. Osman Gazi AKÇALI\n(Piyano. Lab.)
THURSDAY|2|09:30|12:20|MZB 241 (Seçmeli Ders) Müzikte Yorumculuk (S)\nÖğr.Gör.Ebru Doğan AKTAŞ (313)
THURSDAY|3|09:30|12:20|MZB 327 Müzik Tür ve Biçimleri I (Z)\nÖğr. Gör. Alev TÜRKAN (K.Amfi)
THURSDAY|3|13:30|16:20|MZB339 Seçmeli Ders (Oda Müziği I)\nÖğr. Gör Ayşegül TULUMCU (311)
THURSDAY|4|09:30|12:20|MZB437 Seçmeli Ders (Oda Müziği III)\nÖğr. Gör Ayşegül TULUMCU (312)
THURSDAY|4|13:30|16:20|MZB 423 Müzikte Disiplinler Arası Çalışmalar I (Z)\nÖğr. Gör. Dr. İlay Bilge DENKTAŞ (312)
FRIDAY|2|08:30|12:20|MZB 231 Piyano III (Z)\nÖğr.Gör. Osman Gazi AKÇALI\n(Piyano. Lab.)
            """.trimIndent()
        ),
        ClassSchedules.PHOTOGRAPHY_DEPARTMENT to ScheduleSource(
            department = ClassSchedules.PHOTOGRAPHY_DEPARTMENT,
            url = "https://webis.akdeniz.edu.tr/uploads/1060/content/%2831481862%29Fotograf%202026-2027_%20Eg%CC%86itim-O%CC%88g%CC%86retim%20Yili%20Gu%CC%88z%20Yariyili%20Ders%20Program_3.9.2026.docx.pdf",
            updatedAt = "2026-09-03",
            rows = """
                MONDAY|1|10:30|12:20|FOT-121 Temel Fot. Bilgisi I-207\n(Öğr. Gör. M. Uluç CEYLANİ)
MONDAY|1|13:30|17:20|FOT 117 Fotoğraf ve Görüntü İşleme I-301\n(Öğr. Gör. Ahmet S. YILDIZ)
MONDAY|2|09:30|12:20|FOT 223 Belgesel Fotoğraf I 306\n(Doç. Uğur GÜNAY YAVUZ)
MONDAY|2|13:30|16:20|FOT 223 Belgesel Fotoğraf I 306\n(Doç. Uğur GÜNAY YAVUZ)
MONDAY|3|09:30|13:20|FOT 329 Portre I- 309\n(Dr. Öğr. Üyesi Nafia ÖZDEMİR)
MONDAY|3|13:30|15:20|FOT 351 Çağdaş Sanat Tarihi I-302\n(Doç.Dr.Terlan MEHDİYEVA AZİZZADE)
MONDAY|4|09:30|10:20|FOT 435 Dijital Sanatlar- 301\n(Öğr. Gör. Ahmet S. YILDIZ)
MONDAY|4|10:30|12:20|FOT 435 Dijital Sanatlar-301\n(Öğr. Gör. Ahmet S. YILDIZ)
MONDAY|4|13:30|16:20|FOT 445 Moda Fotoğrafı-I 310\n(Doç. Handan DAYI )
TUESDAY|1|08:30|10:20|YBD 101 İngilizce I Öğr. Gör.\nBurcak AKINCI
TUESDAY|1|10:30|12:20|ATA101-102 Atatürk İlkeleri ve İnkılap\nTarihi I-II Öğr. Gör. Dr. Bengi KÜMBÜL\nUZUNSAKAL
TUESDAY|1|13:30|15:20|TDB 101 Türk Dili I\nÖğr. Gör. Meral BAYRAM
TUESDAY|2|09:30|12:20|FOT 213 Fotoğraf Okuma-306\n(Doç. Uğur GÜNAY YAVUZ ) (S.D.)
TUESDAY|2|12:30|18:20|FOT 239 Fotoğraf ve Görüntü İşleme III\n(Öğr. Gör. Ahmet S. YILDIZ) 301
TUESDAY|3|08:30|11:20|FOT 347 Fotoğraf ve Görüntü İşleme V-301\n(Öğr. Gör. Ahmet S. YILDIZ)
TUESDAY|3|11:30|12:20|FOT 347 Fotoğraf ve Görüntü İşleme V 301\n(Öğr. Gör. Ahmet S. YILDIZ)
TUESDAY|3|13:30|17:20|FOT 325 Deneysel Fotoğraf I- 306\n(Doç. Handan DAYI)
TUESDAY|4|08:30|12:20|FOT 423 Tanıtım Fotoğrafı I-310\n(Öğr. Gör. M. Uluç CEYLANİ)
TUESDAY|4|13:30|14:20|FOT 441 Fot.Tasarım Uyg.III –310\n(Dr. Öğr. Üy.Nafia ÖZDEMİR)
TUESDAY|4|14:30|17:20|FOT 441 Fot.Tasarım Uyg.III – 310\n(Dr. Öğr. Üy.Nafia ÖZDEMİR)
WEDNESDAY|1|08:30|09:20|FOT 123 Temel Sanat Eğitimi I-As08\n(Öğr. Gör. Işık ASLIHAN)
WEDNESDAY|1|09:30|12:20|FOT 123 Temel Sanat Eğitimi I- As08\n(Öğr. Gör. Işık ASLIHAN)
WEDNESDAY|1|13:30|15:20|FOT 125 Görsel İletişime Giriş- 306\n(Doç. Uğur GÜNAY YAVUZ)
WEDNESDAY|1|15:30|17:20|FOT-103 Fotoğraf Tarihi I-306\n(Doç. Handan DAYI )
WEDNESDAY|2|10:30|12:20|FOT 235 Sanat Tarihi III- 305\n(Öğr. Gör. Hayal GÜLEÇ)
WEDNESDAY|2|12:30|15:20|FOT 219 Fotoğraf Kültürü-207\n(Doç. Handan DAYI) (S.D.)
WEDNESDAY|3|09:30|12:20|FOT 321 Soyut Fotoğraf 306\n(Doç. Uğur GÜNAY YAVUZ) (S.D.)
WEDNESDAY|3|13:30|14:20|FOT 317 Konsept ve Stil Yaratma I–208\n(Öğr. Gör. Ebru DİKMEN VAROL) (S.D.)
WEDNESDAY|3|14:30|15:20|FOT 317 Konsept ve Stil Yaratma I– 208\n(Öğr. Gör. Ebru DİKMEN VAROL) (S.D.)
WEDNESDAY|3|15:30|16:20|FOT 317 Konsept ve Stil Yaratma I–208\n(Öğr. Gör. Ebru DİKMEN VAROL)(S.D.)
WEDNESDAY|4|09:30|12:20|FOT 413 Moda Sektöründe Konsept ve Stil Yaratma-208\n(Öğr. Gör. Ebru DİKMEN VAROL) (S.D.)
WEDNESDAY|4|13:30|15:20|FOT 421 Sanat Felsefesi- 302\n( Öğr. Gör. Ezgi BİLGİN)
THURSDAY|1|10:30|11:20|FOT 113 Sanat Tarihi I- 302\n(Öğr. Gör. Hayal GÜLEÇ)
THURSDAY|1|11:30|12:20|FOT 113 Sanat Tarihi- 302\n(Öğr. Gör. Hayal GÜLEÇ)
THURSDAY|2|12:30|18:20|FOT 225 Stüdyo TeknikleriI -308\n(Öğr. Gör. M. Uluç CEYLANİ)
THURSDAY|3|08:30|12:20|FOT 349 Fotoğraf Tasarım Uygulama I-309\n(Öğr. Gör. M. Uluç CEYLANİ)
FRIDAY|3|14:30|17:20|FOT 315 Eser Dinleme ve Tanıma-SKS Piyano L.\n(Öğr. Gör. Osman Gazi AKÇALI)
FRIDAY|4|08:30|14:20|FOT 447 Atölye I-\nŞube 1/ (Dr. Öğr. Üy.Nafia ÖZDEMİR)\nŞube 2/ (Doç. Handan DAYI)\nŞube 3/ (Doç. Uğur GÜNAY YAVUZ)\nŞube 4/(Öğr. Gör. M. Uluç CEYLANİ)\nŞube 5/ (Öğr. Gör. Ahmet S. YILDIZ)
            """.trimIndent()
        ),
        ClassSchedules.CINEMA_TV_DEPARTMENT to ScheduleSource(
            department = ClassSchedules.CINEMA_TV_DEPARTMENT,
            url = "https://webis.akdeniz.edu.tr/uploads/1060/content/%2831500667%29Sinema-Televizyon%20Haftalik%20Ders%20Programi%20GUZ%202025-2026%20LISANS%2008.09.2026%20son.pdf",
            updatedAt = "2026-09-08",
            rows = """
                MONDAY|1|08:30|12:20|FLM105 FOTOGRAF I\nSERAP DUMAN INCE #304
MONDAY|1|13:30|15:20|FLM113 TEMEL SINEMA EGITIMI Serap Duman İnce\n#304
MONDAY|2|13:30|15:20|FLM209 SINEMA TARIHI\nAYSEN OLUK ERSÜMER #305
MONDAY|2|15:30|17:20|FLM 205 Sinema Kuramları Serap Duman İnce #304
MONDAY|3|09:30|12:20|FLM 351 TÜRK SİN. SOSYOLOJİSİ\nMümin Barış #303
MONDAY|4|13:30|15:20|FLM431 SEMINER I MÜMİN BARIŞ #303
MONDAY|4|15:30|17:20|FLM Yeni Türk Sineması Mümin Barış #303
TUESDAY|1|08:30|10:20|YDB101 YABANCI DIL BERNA GÜNDOĞDU Öğr. Gör.\nBurcak AKINCI (YÜZ YÜZE)
TUESDAY|1|10:30|12:20|ATA101 ATATURK ILKELERI\nÖğr. Gör. Dr. Bengi KÜMBÜL UZUNSAKAL (YÜZ YÜZE)
TUESDAY|1|13:30|15:20|TDB101 TURK DILI I\nMeral Bayram (YÜZYÜZE)
TUESDAY|1|15:30|16:20|FLM 127 ILETISIM VE TOPLUM\nBİLAL ARIK #303
TUESDAY|1|16:30|17:20|FLM 127 ILETISIM VE TOPLUM\nBİLAL ARIK #304
TUESDAY|2|15:30|17:20|FLM225 ROMAN KURAMI I\nTÜLİN ARSEVEN - #305
TUESDAY|3|13:30|17:20|FLM 323 BELGESEL FILM ATOLYE III Mümin Barış #303 /\nFLM 321 KISA FILM III Serap Duman İnce #304
TUESDAY|4|08:30|12:20|FLM435 Film Yapımı I\nÖğr. Üyesi Dr. Oğuzhan Ersümer\nÖğr. Üyesi Dr. Rana İğneci Süzen\nÖğr. Gör. Serap Duman İnce\nÖğr. Gör. Mümin Barış
TUESDAY|4|13:30|15:20|FLM423 Yönetmen Sineması I Oğuzhan Ersümer #305
WEDNESDAY|1|13:30|15:20|FLM119 SOSYOLOJI\nBİRTAN BOZLU #303
WEDNESDAY|2|08:30|09:20|FLM215/FLM 211 BELGESEL/\nSERAP DUMAN İNCE #304 /KISA FİLM ATÖLYE Rana\nİğnecci #303
WEDNESDAY|2|09:30|10:20|FLM215/FLM 211 BELGESEL/\nSERAP DUMAN İNCE #304 /KISA FİLM ATÖLYE Rana\nİğnecci #304
WEDNESDAY|2|10:30|12:20|FLM215/FLM 211 BELGESEL/\nSERAP DUMAN İNCE #304 /KISA FİLM ATÖLYE Rana\nİğneci Süzen #305
WEDNESDAY|3|08:30|09:20|FLM303 KURGU I\nZIHNI (Bilgisayar Lab. C BLOK Kat 4
WEDNESDAY|3|09:30|10:20|FLM303 KURGU I\nZIHNI (Bilgisayar Lab. C BLOK Kat 5
WEDNESDAY|3|10:30|11:20|FLM303 KURGU I\nZIHNI (Bilgisayar Lab. C BLOK Kat 6
WEDNESDAY|3|11:30|12:20|FLM303 KURGU I\nZIHNI (Bilgisayar Lab. C BLOK Kat 7
WEDNESDAY|3|13:30|15:20|FLM317 SINEMA ESTETIK FELSEFE\nOGUZHAN ERSUMER #305
WEDNESDAY|4|13:30|15:20|FLM419 ULKE SINEMALARI\nRANA SUZEN #304
WEDNESDAY|4|15:30|17:20|FLM433 BILIMSEL ARASTIRMA I\nERTUNÇ UKSUL #305
THURSDAY|1|08:30|10:20|FİLM117 Sanat Tarihi-I - Öğr.Gör. Hayal Güleç\nDerslik 304
THURSDAY|1|10:30|12:20|FLM 123 Sinema Kültürü Ayşen Oluk Ersümer #304
THURSDAY|2|10:30|12:20|FLM 233 SENARYO YAZIMI I\nRANA IGNECI SÜZEN #303
THURSDAY|2|13:30|15:20|FLM Sinema ve Psikoloji 223 RANA IGNECI SÜZEN #303
THURSDAY|2|15:30|17:20|FLM235/ Sanat Tarihi-III - Ezgi BİLGİN\n#304
THURSDAY|3|13:30|15:20|FLM 305 FİLM OKUMA Oğuzhan Ersümer #305
THURSDAY|4|13:30|15:20|FLM407 FILM ELESTIRI YAZIMI I\nAyşen OLUK ERSÜMER #304
FRIDAY|3|10:30|12:20|FLM 319 Anlatı Türleri Serap Duman İnce #304
            """.trimIndent()
        ),
        ClassSchedules.TRADITIONAL_TURKISH_ARTS_DEPARTMENT to ScheduleSource(
            department = ClassSchedules.TRADITIONAL_TURKISH_ARTS_DEPARTMENT,
            url = "https://webis.akdeniz.edu.tr/uploads/1060/content/%2831485110%29Geleneksel%20Turk%20Sanatlari%20LISANS%202026-2027%20GUZ%20DERS%20PROGRAMI%2027.08.2026%20son%20hali.docx.pdf",
            updatedAt = "2026-08-27",
            rows = """
                MONDAY|2|08:30|11:20|GEL 255DOKUMA TEKNİKLERİ VE\nTERMİNOLOJİSİ-I\nSERİK MYO\n(GSF 105)
MONDAY|2|11:30|13:20|GEL 247BİLGİSAYAR DESTEKLİ TASARIM-I\nMİLLİ EĞİTİM\n(GSF 106)
MONDAY|3|13:30|17:20|GEL 355BİLGİSAYARDA DESEN\nÇİZİM TEKNİKLERİ-I\nMİLLİ EĞİTİM\n(GSF 106)
MONDAY|4|13:30|17:20|GEL 405HALI KİLİM ONARIMI – I\nSERİK MYO\n(GSF 105)
TUESDAY|1|08:30|10:20|YBD 101 İNGİLİZCE I\nÖğr. Gör. Burak AKINCI\n(Yüz yüze)
TUESDAY|1|10:30|12:20|ATA 101 ATATÜRK İLKELERİ VE İNKILAP\nTARİHİ\nÖğr. Gör. Dr. Bengi KÜMBÜL UZUNSAKAL\n(Yüz yüze)
TUESDAY|1|13:30|15:20|TDB 101 TÜRK DİLİ I\nÖğr. Gör. Meral BAYRAM\n(Yüz yüze)
TUESDAY|3|08:30|12:20|GEL 373TEZHİP-I\nÖğr. Gör Züleyha ZOR\n(GSF 104)
TUESDAY|3|12:30|15:20|GEL 359HAT SANATI\nÖğr. Gör Züleyha ZOR\n(GSF 104)
TUESDAY|3|15:30|18:20|GEL 363GELENEKSEL\nSANATLARDA FOTOĞRAF VE\nBELGELEME\nÖğr. Gör Züleyha ZOR\n(GSF 104)
TUESDAY|4|08:30|14:20|GEL 401KUMAŞ TASARIMI – I\nProf. Mehmet Ali EROĞLU\n(GSF 103)
TUESDAY|4|14:30|18:20|GEL 451DENEYSEL DOKUMA\nTEKNİKLERİ-I\nProf. Mehmet Ali EROĞLU\n(GSF 103)
WEDNESDAY|1|08:30|14:20|GEL 127GELENEKSEL TÜRK SANATLARI-I\nDr.Öğr.Üyesi Zülbiye Sevgili POLAT\n(GSF 102)
WEDNESDAY|1|14:30|18:20|GEL 103DESEN I\nDoç.Dr.Mehmet SAĞ\n(GSF 105)
WEDNESDAY|2|08:30|12:20|GEL 241TÜRK DESENLERİ-I\nÖğr. Gör Züleyha ZOR\n(GSF 104)
WEDNESDAY|2|12:30|14:20|GEL 263TÜRK MİTOLOJİSİ\nDoç. Dr. Mehmet SAĞ\n(GSF 105)
WEDNESDAY|2|14:30|17:20|GEL 251GELENEKSEL ÇİNİ\nUYGULAMALARI-I\nÖğr. Gör Züleyha ZOR\n(GSF 104)
WEDNESDAY|3|08:30|10:20|GEL 357TÜRK SANATLARINDA\nESER ELEŞTİRİSİ\nDoç. Dr. Mehmet SAĞ\n(GSF 105)
WEDNESDAY|3|14:30|16:20|GEL 341BİLİMSEL ARAŞTIRMA\nYÖNTEMLERİ-I\nDr.Öğr.Üyesi Zülbiye Sevgili POLAT\n(GSF 102)
WEDNESDAY|3|16:30|18:20|GEL 365ÇAĞDAŞ SANAT-I\nDr.Öğr.Üyesi Zülbiye Sevgili POLAT\n(GSF 102)
WEDNESDAY|4|10:30|12:20|GEL 459GELENEKSEL\nSANATLARDA SERGİLEME\nTEKNİKLERİ\nDoç. Dr. Mehmet SAĞ\n(GSF 105)
THURSDAY|1|08:30|14:20|GEL 101TEMEL SANAT EĞİTİMİ I\nDoç.Dr.Mehmet SAĞ\n(GSF 105)
THURSDAY|1|15:30|16:20|KPD 101 KARİYER PLANLAMA\nÖğr. Gör. Ebru DOĞAN AKTAŞ\nGSF Mavi Amfi
THURSDAY|2|08:30|14:20|GEL 201HALI TASARIMI – I\nDr.Öğr.Üyesi Zülbiye Sevgili POLAT\n(GSF 102)
THURSDAY|2|14:30|16:20|GEL 257OSMANLI TÜRKÇESİ-I\nÖğr. Gör Züleyha ZOR\n(GSF 104)
THURSDAY|3|14:30|16:20|GEL 351KONSERVASYON VE\nRESTORASYON\nDr.Öğr.Üyesi Zülbiye Sevgili POLAT\n(GSF 102)
THURSDAY|4|09:30|11:20|GEL 453BİTİRME PROJESİ-I\nÖğr. Gör Züleyha ZOR\n(GSF 104)
THURSDAY|4|13:30|16:20|GEL 411SELÇUKLU KUMAŞ SANATI\nSERİK MYO\n(GSF 109)
FRIDAY|1|10:30|12:20|GEL 125 SANAT TARİHİ I\nÖğr. Gör. Hayal GÜLEÇ\nGSF Kırmızı Amfi
FRIDAY|1|12:30|14:20|GEL 105TEKSTİL HAMMADDELERİ I\nSERİK MYO\n(GSF 105)
FRIDAY|3|08:30|14:20|GEL 301KİLİM TASARIMI I\nSERİK MYO\n(GSF 109)
            """.trimIndent()
        ),
        ClassSchedules.TEXTILE_FASHION_DEPARTMENT to ScheduleSource(
            department = ClassSchedules.TEXTILE_FASHION_DEPARTMENT,
            url = "https://webis.akdeniz.edu.tr/uploads/1060/content/%2831433783%29Ders%20programi%202026-2027%20GU%CC%88Z%20Do%CC%88nemi%20Tekstil%20ve%20Moda%20Tasarimi%20Bo%CC%88lu%CC%88mu%CC%88.docx.pdf",
            updatedAt = "2026-09-17",
            rows = """
                MONDAY|1|08:30|10:20|TDB101-TÜRK DİLİ I\nÖğr.Gör.Meral BAYRAM
MONDAY|1|10:30|12:20|YDB101- İNGİLİZCE I\nÖğr.Gör.Demet TEKİNAY
MONDAY|1|13:30|15:20|ATA101 ATATÜRK İLKELERİ ve İNK.TARİHİ\nI\nÖğr. Gör. Fatma ÇETİN ADIGÜZEL
MONDAY|1|15:30|16:20|KPD101 KARİYER PLANLAMA\nÖğr.Gör. Ebru Doğan AKTAŞ\nZ13
MONDAY|2|13:30|17:20|MOD 223 KALIP HAZIRLAMA I\nDr.Öğr.Üyesi Fatma Bayraktar 108
MONDAY|3|08:30|12:20|MOD 329 DRAPAJ\nDr.Öğr.Üyesi Fatma BAYRAKTAR 107
MONDAY|3|13:30|15:20|MOD 323 ÇAĞDAŞ SANAT\nDoç. Dr. Kezban SÖNMEZ 208
TUESDAY|1|10:30|12:20|MOD 113 TEMEL SANAT EĞİTİMİ\nDoç.Dr.Zuhal BAŞBUĞ 108
TUESDAY|1|13:30|17:20|MOD 113 TEMEL SANAT EĞİTİMİ\nDoç.Dr.Zuhal BAŞBUĞ 108
TUESDAY|2|08:30|11:20|MOD 203 MODA TAS. İÇİN ÇİZ. VE SUN. TEK.\nÖğr.Gör.Mine YILDIRAN Z06
TUESDAY|2|13:30|16:20|MOD 235GİYSİ ERGONOMİSİ\nDr.Öğr.Üyesi Fatma Bayraktar 206
TUESDAY|3|09:30|11:20|MOD 327 ARAŞTIRMA TEKNİKLERİ\nDr.Öğr.Üyesi Fatma Bayraktar 206
TUESDAY|3|11:30|17:20|MOD 301 MODA TASARIM ATÖLYESİ III\nÖğr.Gör.Mine YILDIRAN Z06
TUESDAY|4|09:30|13:20|MOD 409 BİLGİSAYAR DESTEKLİ KALIP HAZIRLAMA\nDr.Öğr.Üyesi Ahsen UYSAL Z-08
TUESDAY|4|14:30|16:20|MOD 423 MODA PAZARLAMA\nDr.Öğr.Üyesi Ahsen UYSAL Z-08
WEDNESDAY|1|10:30|12:20|MOD 105 SANAT TARİHİ I\nDoç. Dr. Kezban SÖNMEZ 206
WEDNESDAY|1|13:30|17:20|MOD 103 DESEN I\nDoç.Dr.Zuhal BAŞBUĞ Z-07
WEDNESDAY|2|08:30|11:20|MOD 229 DOĞAL BOYA UYGULAMALARI\nDoç.Menekşe Suzan TEKER Z08
WEDNESDAY|2|11:30|13:20|MOD 237BİLGİSAYAR DES. MODA TAS.\nDr.Öğr.Üyesi Ahsen UYSAL Z-08
WEDNESDAY|2|13:30|14:20|MOD 237BİLGİSAYAR DES. MODA TAS.\nDr.Öğr.Üyesi Ahsen Uysal Z-08
WEDNESDAY|2|14:30|16:20|MOD 225 MODA TARİHİ\nDoç. Dr. Kezban SÖNMEZ 206
WEDNESDAY|3|08:30|11:20|MOD 341 TEKSTİL BASKI T EKNİKLERİ\nÖğr.Gör.Mine YILDIRAN Z06
WEDNESDAY|4|08:30|11:20|MOD 441 GİRİŞİMCİLİK\nDr.Öğr.Üyesi Fatma BAYRAKTAR 108
WEDNESDAY|4|11:30|17:20|MOD 443 KOLEKSİYON HAZIRLAMA I\nDoç.Menekşe Suzan TEKER 111 C -1.Şube\nÖğr.Gör.Mine YILDIRAN 111 B -2.Şube\nDr.Öğr.Üyesi Mariyam YEZİYEVA Z06-3.Şube\nDr.Öğr.Üyesi Fatma BAYRAKTAR 107-4.Şube
THURSDAY|1|08:30|11:20|MOD 111 TEKSTİL MALZEME BİLGİSİ\nÖğr.Gör.Mine YILDIRAN 108
THURSDAY|1|13:30|15:20|MOD 109 MODA TASARIMINA GİRİŞ\nÖğr.Gör.Mine YILDIRAN 208
THURSDAY|2|09:30|11:20|MOD 207 SANAT TARİHİ III\nDoç. Dr. Kezban SÖNMEZ 208
THURSDAY|2|11:30|15:20|MOD 201 MODA TASARIM ATÖLYESİ I\nDr.Öğr.Üyesi Ahsen UYSAL 108
THURSDAY|2|15:30|16:20|MOD 201 MODA TASARIM ATÖLYESİ I\nDDr.Öğr.Üyesi Ahsen UYSAL 108
THURSDAY|2|16:30|17:20|MOD 201 MODA TASARIM ATÖLYESİ I\nDr.Öğr.Üyesi Ahsen UYSAL 108
THURSDAY|3|11:30|17:20|MOD 309 TEKSTİL TASARIM ATÖLYESİ I\nDoç.Menekşe Suzan TEKER Z08
THURSDAY|4|08:30|11:20|MOD 439 REZERVE BOYAMA TEKNİKLERİ VE UYGULAMALARI\nDoç.Menekşe Suzan TEKER Z08
THURSDAY|4|11:30|17:20|MOD 401 MODA TASARIM ATÖLYESİ V\nDr.Öğr.Üyesi Mariyam YEZİYEVA Z06
FRIDAY|3|09:30|13:20|MOD 325 KALIP HAZIRLAMA III\nDr.Öğr.Üyesi Mariyam YEZİYEVA 108
FRIDAY|3|13:30|16:20|MOD 337 İMAJ VE STİL DANIŞMANLIĞI\nDr.Öğr.Üyesi Mariyam YEZİYEVA 107
            """.trimIndent()
        )
    )

    fun schedulesFor(department: String?): List<ClassSchedule>? {
        val source = sourceByDepartment[department] ?: return null
        val rows = parseRows(source.rows)
        return (1..4).map { year ->
            ClassSchedule(
                faculty = ClassSchedules.FINE_ARTS_FACULTY,
                department = source.department,
                classYear = when (year) {
                    1 -> ClassSchedules.FIRST_YEAR
                    2 -> ClassSchedules.SECOND_YEAR
                    3 -> ClassSchedules.THIRD_YEAR
                    else -> ClassSchedules.FOURTH_YEAR
                },
                academicYear = ACADEMIC_YEAR,
                term = TERM,
                updatedAt = source.updatedAt,
                sourcePage = 1,
                sourceUrl = source.url,
                entries = rows.asSequence()
                    .filter { it.year == year }
                    .flatMap { row -> entries(source.department, row).asSequence() }
                    .toList()
            )
        }
    }

    private fun parseRows(rows: String): List<SourceRow> = rows.lineSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .mapNotNull { line ->
            val parts = line.split('|', limit = 5)
            if (parts.size != 5) return@mapNotNull null
            val day = when (parts[0]) {
                "MONDAY" -> ScheduleDay.MONDAY
                "TUESDAY" -> ScheduleDay.TUESDAY
                "WEDNESDAY" -> ScheduleDay.WEDNESDAY
                "THURSDAY" -> ScheduleDay.THURSDAY
                "FRIDAY" -> ScheduleDay.FRIDAY
                else -> return@mapNotNull null
            }
            val year = parts[1].toIntOrNull() ?: return@mapNotNull null
            SourceRow(
                day = day,
                year = year,
                start = parts[2],
                end = parts[3],
                cellText = parts[4].replace("\\n", "\n")
            )
        }
        .toList()

    private fun entries(department: String, row: SourceRow): List<ScheduleEntry> {
        val periods = classPeriods.filter { it.start >= row.start && it.end <= row.end }
        val selectedPeriods = periods.ifEmpty { listOf(ClassPeriod(row.start, row.end)) }
        val details = parseCell(department, row.cellText)
        return selectedPeriods.flatMap { period ->
            details.map { detail ->
                ScheduleEntry(
                    day = row.day,
                    startTime = period.start,
                    endTime = period.end,
                    courseCode = detail.code,
                    courseName = detail.name,
                    instructor = detail.instructor,
                    classroom = detail.classroom,
                    courseType = detail.courseType,
                    note = detail.note,
                    section = detail.section
                )
            }
        }
    }

    private fun parseCell(department: String, sourceText: String): List<CourseDetails> {
        val text = sourceText
            .replace('/', ' ')
            .replace(Regex("""\s+"""), " ")
            .trim()

        if (
            department == ClassSchedules.CINEMA_TV_DEPARTMENT &&
            text.contains("FLM215", ignoreCase = true) &&
            text.contains("FLM 211", ignoreCase = true) &&
            text.contains("KISA FİLM ATÖLYE", ignoreCase = true)
        ) {
            val rooms = classroomPattern.findAll(text).map { it.value.trim() }.distinct().toList()
            val teacherNames = knownUnmarkedInstructors.filter { name -> text.contains(name, ignoreCase = true) }
            val shortFilmInstructor = Regex(
                """KISA FİLM ATÖLYE\s+(.+?)(?=#\s*\d+|$)""",
                RegexOption.IGNORE_CASE
            ).find(text)?.groupValues?.getOrNull(1)?.trim().orEmpty()
            return listOf(
                CourseDetails("FLM 215", "Belgesel", teacherNames.firstOrNull() ?: "SERAP DUMAN İNCE", rooms.firstOrNull().orEmpty(), "Ders", null, null),
                CourseDetails("FLM 211", "Kısa Film Atölye", shortFilmInstructor, rooms.lastOrNull().orEmpty(), "Uygulama", null, null)
            )
        }

        if (
            department == ClassSchedules.CINEMA_TV_DEPARTMENT &&
            text.contains("FLM 323", ignoreCase = true) &&
            text.contains("FLM 321", ignoreCase = true)
        ) {
            val rooms = classroomPattern.findAll(text).map { it.value.trim() }.distinct().toList()
            return listOf(
                CourseDetails("FLM 323", "Belgesel Film Atölye III", "Mümin Barış", rooms.getOrElse(0) { "" }, "Uygulama", null, null),
                CourseDetails("FLM 321", "Kısa Film III", "Serap Duman İnce", rooms.getOrElse(1) { "" }, "Uygulama", null, null)
            )
        }

        if (
            department == ClassSchedules.CINEMA_TV_DEPARTMENT &&
            text.contains("Sinema ve Psikoloji", ignoreCase = true)
        ) {
            val room = classroomPattern.findAll(text).lastOrNull()?.value.orEmpty()
            return listOf(
                CourseDetails(
                    code = "FLM 223",
                    name = "Sinema ve Psikoloji",
                    instructor = "RANA İĞNECİ SÜZEN",
                    classroom = room,
                    courseType = "Ders",
                    section = null,
                    note = null
                )
            )
        }

        val codeMatch = courseCodePattern.find(text)
        val code = codeMatch?.groups?.get(1)?.value
            ?.replace(Regex("""\s+"""), " ")
            ?.replace(Regex("""\s*-\s*"""), "-")
            ?.trim()
            .orEmpty()

        val afterCode = if (codeMatch == null) text else text.substring(codeMatch.range.last + 1)
        val markers = instructorMarker.findAll(text).toList()
        val firstMarkerPosition = markers.firstOrNull()?.range?.first
        val namedInstructors = knownUnmarkedInstructors
            .mapNotNull { name ->
                val index = text.indexOf(name, ignoreCase = true)
                if (index >= 0 && (codeMatch == null || index > codeMatch.range.last)) index to text.substring(index, index + name.length)
                else null
            }
            .sortedBy { it.first }
            .distinctBy { it.first }
        val firstNamedInstructorPosition = namedInstructors.firstOrNull()?.first
        val instructorPosition = listOfNotNull(firstMarkerPosition, firstNamedInstructorPosition).minOrNull()

        val titleStart = if (codeMatch == null) 0 else codeMatch.range.last + 1
        val titleEnd = listOfNotNull(
            instructorPosition?.takeIf { it >= titleStart },
            classroomPattern.findAll(text.substring(titleStart)).firstOrNull()?.range?.first?.let { it + titleStart }
        ).minOrNull() ?: text.length

        var courseName = text.substring(titleStart, titleEnd)
            .replace(Regex("""(?i)\b(?:ATA|KPD|KRY|TDB|YBD|YDB|RES|HEYK|GRA|SER|FOT|MZB|FLM|FİLM|GEL|MOD)\s*-?\s*\d{3}(?:-\d{3})?\b"""), " ")
            .replace(Regex("""[|/\\-]+"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .trim('.', ':', ';', ',', '–', '—')
            .ifBlank { if (codeMatch == null) cleanText(text) else "" }

        val instructorText = when {
            firstMarkerPosition != null -> (
                namedInstructors.filter { it.first < firstMarkerPosition }.map { it.second } +
                    text.substring(firstMarkerPosition)
                ).joinToString(" ")
                .replace(classroomPattern, " ")
                .replace(sectionPattern, " ")
                .replace(Regex("""[|/\\]+"""), " ")
                .replace(Regex("""(?i)(Öğr\\.?\\s*Gör\\.?\\s*){2,}"""), "Öğr. Gör. ")
                .replace(Regex("""\s+"""), " ")
                .trim()
            namedInstructors.isNotEmpty() -> namedInstructors.joinToString(" ") { it.second }
            department == ClassSchedules.TRADITIONAL_TURKISH_ARTS_DEPARTMENT &&
                text.contains("MİLLİ EĞİTİM", ignoreCase = true) -> "MİLLİ EĞİTİM"
            else -> ""
        }

        val classroomSource = if (codeMatch == null) text else text.removeRange(codeMatch.range)
        val classrooms = classroomPattern.findAll(classroomSource)
            .map { it.value.replace(Regex("""\s+"""), " ").trim() }
            .distinct()
            .toList()
        val sections = sectionPattern.findAll(text)
            .map { it.value.replace(Regex("""\s+"""), " ").trim() }
            .distinct()
            .toList()
        val isSelection = selectionPattern.containsMatchIn(text)
        val isApplication = Regex("""(?i)uygulama|atölye|laboratuvar|\blab\b""").containsMatchIn(courseName)
        val courseType = when {
            isSelection -> "Seçmeli"
            isApplication -> "Uygulama"
            else -> "Ders"
        }
        val note = sections.takeIf { it.isNotEmpty() }?.joinToString(", ") { "Şube $it" }

        return listOf(
            CourseDetails(
                code = code,
                name = courseName,
                instructor = instructorText,
                classroom = classrooms.joinToString(", "),
                courseType = courseType,
                section = sections.takeIf { it.isNotEmpty() }?.joinToString(" / "),
                note = note
            )
        )
    }

    private fun cleanText(text: String): String = text
        .replace(classroomPattern, " ")
        .replace(instructorMarker, " ")
        .replace(sectionPattern, " ")
        .replace(Regex("""(?i)\b(?:ATA|KPD|KRY|TDB|YBD|YDB|RES|HEYK|GRA|SER|FOT|MZB|FLM|FİLM|GEL|MOD)\s*-?\s*\d{3}(?:-\d{3})?\b"""), " ")
        .replace(Regex("""[|/\\-]+"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()
}
