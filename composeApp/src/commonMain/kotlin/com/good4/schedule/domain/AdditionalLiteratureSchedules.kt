package com.good4.schedule.domain

/** Additional Edebiyat Fakültesi fall 2026–2027 schedules from the faculty's published files. */
internal object AdditionalLiteratureSchedules {
    private data class RawBlock(
        val day: ScheduleDay,
        val year: Int,
        val start: String,
        val end: String,
        val sourceText: String
    )

    private data class Course(
        val code: String,
        val name: String,
        val instructor: String,
        val classroom: String,
        val type: String
    )

    private val codePattern = Regex("""\b([A-ZÇĞİÖŞÜ]{2,5}\s*\d{2,4})\b""", RegexOption.IGNORE_CASE)
    private val instructorPattern = Regex(
        """(?i)\b(?:Prof\.?\s*Dr\.?|Doç\.?\s*Dr\.?|Dr\.?\s*Öğr\.?\s*Üyesi|Öğr\.?\s*Gör\.?(?:\s*Dr\.?)?|Arş\.?\s*Gör\.?(?:\s*Dr\.?)?)"""
    )
    private val roomPattern = Regex(
        """(?i)(?:Derslik\s*[- ]?\s*[A-Z0-9-]+|[0-9]{2,3}\s*Nolu\s*Derslik|Z\s*[0-9]{2}\s*Nolu\s*Derslik|D\s*-?\s*[0-9]{3}|Z\s*[0-9]{2}|\([0-9]{3}\)|Bumin Kağan Amfisi|Erol Güngör Amfisi|Prof\. Dr\. Erol Güngör Amfisi|\(\*{3}\)|\(Çevrimiçi\))"""
    )
    private val initialsPattern = Regex("""\(([A-ZÇĞİÖŞÜ]\.\s*[A-ZÇĞİÖŞÜ](?:[a-zçğıöşü]+)?\.?)\)""")
    private val compactInstructorPattern = Regex(
        """(?:^|[-/])\s*((?:[A-ZÇĞİÖŞÜ]\.){1,3}\s*[A-ZÇĞİÖŞÜ]?[a-zçğıöşü]+(?:\s+[A-ZÇĞİÖŞÜ]?[a-zçğıöşü]+)*)"""
    )

    private val regularPeriods = listOf(
        "08:30" to "09:20", "09:30" to "10:20", "10:30" to "11:20", "11:30" to "12:20",
        "13:30" to "14:20", "14:30" to "15:20", "15:30" to "16:20", "16:30" to "17:20",
        "18:30" to "19:20", "19:30" to "20:20", "20:30" to "21:20"
    )
    private val eveningPeriods = listOf(
        "15:30" to "16:20", "16:30" to "17:20", "17:30" to "18:20", "18:25" to "19:15",
        "19:20" to "20:10", "20:15" to "21:05", "21:10" to "22:00", "22:05" to "22:55"
    )

    private const val PHILOSOPHY_SOURCE = "https://webis.akdeniz.edu.tr/uploads/1015/announcement/%2831643870%292026-2027%20Guz%20Felsefe%20Lisans%20Haftalik%20Ders%20Programi.xlsx.pdf"
    private const val ENGLISH_REGULAR_SOURCE = "https://webis.akdeniz.edu.tr/uploads/1015/announcement/%C3%B2ngiliz%20Dili%20(%C3%B4rg%C3%85n).pdf"
    private const val ENGLISH_EVENING_SOURCE = "https://webis.akdeniz.edu.tr/uploads/1015/announcement/%C3%B2ngiliz%20Dili%20(%C3%B2.%C3%B4).pdf"
    private const val RUSSIAN_SOURCE = "https://webis.akdeniz.edu.tr/uploads/1015/announcement/Rus%20Dili%20ve%20Edebiyat%C3%A7.pdf"
    private const val PSYCHOLOGY_SOURCE = "https://webis.akdeniz.edu.tr/uploads/1015/announcement/Psikoloji.pdf"
    private const val ART_HISTORY_SOURCE = "https://webis.akdeniz.edu.tr/uploads/1015/announcement/SANAT%20TARH%202026-27%20g%C3%BCz%20%C3%B6rg%C3%BCn%20haftal%C4%B1k%20ders%20program%C4%B1_1%20(1).pdf"
    private const val SOCIOLOGY_SOURCE = "https://webis.akdeniz.edu.tr/uploads/1015/announcement/Sosyoloji.pdf"
    private const val HISTORY_SOURCE = "https://webis.akdeniz.edu.tr/uploads/1015/announcement/Tarih%20B%C3%B6l%C3%BCm%20Ders%20Program%C4%B1%202026-2027%20G%C3%BCz-2.pdf"
    private const val TURKISH_REGULAR_SOURCE = "https://webis.akdeniz.edu.tr/uploads/1015/announcement/TDE%20%C3%96rg%C3%BCn%20.pdf"
    private const val TURKISH_EVENING_SOURCE = "https://webis.akdeniz.edu.tr/uploads/1015/announcement/TDE%20%C4%B0kinci%20%C3%96%C4%9Fretim.pdf"

    private val philosophyData = """
MONDAY|1|10:30|12:20|FEL 117 - Mitoloji / Dr. Öğr. Üyesi Özlem TOPCAN / (Derslik Z-02)
MONDAY|2|08:30|12:20|PSİ 241 - Psikolojiye Giriş / Öğr. Gör. Süheyla ÖZEN / (Derslik 402)
MONDAY|2|15:30|17:20|FEL 291 - Latince I / Arş. Gör. Dr. Erkan KURUL / (Derslik Z-01)
MONDAY|3|10:30|12:20|FEL 363 - Din Felsefesi ve Tasavvuf / Prof. Dr. Süleyman DÖNMEZ / (Derslik Z-01)
MONDAY|3|14:30|17:20|FEL 325 - 18. Yüzyıl Felsefesi / Prof. Dr. Çetin BALANUYE / (Derslik Z-02)
MONDAY|4|09:30|12:20|FEL 433 - Matematik ve Mantık Felsefesi / Dr. Öğr. Üyesi Ali Bilge ÖZTÜRK / (Derslik Z-04)
MONDAY|4|13:30|15:20|FEL 413 - Eleştirel Okumalar I / Arş. Gör. Dr. Elif ALTEN GÜLER / (Derslik Z-01)
MONDAY|4|15:30|17:20|FEL 443 - Anadolu ve Felsefe / Prof. Dr. Süleyman DÖNMEZ / (Derslik Z-04)
TUESDAY|1|08:30|10:20|ATA 101 - Atatürk İlke ve İnkılap / Tarihi I / Öğr. Gör. Murat BOZ / Coğrafya Bölümü Derslik-108
TUESDAY|1|10:30|12:20|YBD 101 - İngilizce I / Öğr. Gör. Demet TEKİNAY / Coğrafya Bölümü Derslik-108
TUESDAY|2|08:30|10:20|FEL 247 - Temel Yunanca I / Arş. Gör. Dr. Fatma AVCU / (Derslik Z-01)
TUESDAY|2|10:30|12:20|FEL 215 - İslam Felsefesi / Prof. Dr. Şahin FİLİZ / (Derslik Z-02)
TUESDAY|2|14:30|17:20|FEL 215 - Varlık Felsefesi / Doç. Dr. Şahin ÖZÇINAR / (Derslik Z-02)
TUESDAY|3|10:30|12:20|FEL 327 - Din Felsefesi / Dr. Öğr. Üyesi Özlem TOPCAN / (Derslik Z-03)
TUESDAY|3|13:30|17:20|TDP 381 - Toplumsal Duyarlılık ve Katkı Projeleri I / Prof. Dr. Şahin FİLİZ / (Derslik Z-03)
TUESDAY|4|09:30|12:20|FEL 423 - Çağdaş Türk Felsefesi / Prof. Dr. Süleyman DÖNMEZ / (Derslik Z-04)
WEDNESDAY|1|10:30|12:20|FEL 109 - Felsefe Terimleri / Doç. Dr. Ekin KAYNAK ILTAR / (Derslik Z-02)
WEDNESDAY|1|13:30|17:20|FEL 121 - Bilim Tarihi / Doç. Dr. Ekin KAYNAK ILTAR / (Derslik Z-02)
WEDNESDAY|2|10:30|12:20|FEL 259 - Uygarlık Tarihi I / Dr. Öğr. Üyesi Özlem TOPCAN / (Derslik Z-01)
WEDNESDAY|2|13:30|15:20|FEL 217 - Rönesans Felsefesi / Doç. Dr. Eray YAĞANAK / (Derslik Z-03)
WEDNESDAY|2|15:30|17:20|FEL 265 - İlk Çağ Sofistleri / Öğr. Gör. Serpil SATI / (Derslik Z-01)
WEDNESDAY|3|08:30|12:20|PSİ 377 - Sosyal Psikoloji II / Öğr. Gör. Dr. Enes YALÇIN / (Derslik Z-03)
WEDNESDAY|3|13:30|15:20|FEL 329 - Akademik Araştırma Teknikleri / Prof. Dr. Şahin FİLİZ / (Derslik Z-04)
WEDNESDAY|3|15:30|17:20|FEL 309 - Etik Kuramları / Prof. Dr. Şahin FİLİZ / (Derslik Z-04) / FEL 349 - Felsefe ve Film I / Arş. Gör. Dr. Orkan ERNALBANT / (Derslik Z-03)
WEDNESDAY|4|09:30|12:20|FEL 427 - Felsefi Antropoloji / Prof. Dr. Şahin FİLİZ / (Derslik Z-04)
WEDNESDAY|4|13:30|15:20|FEL 429 - Postmodernizm / Dr. Öğr. Üyesi Özlem TOPCAN / (Derslik Z-01)
THURSDAY|1|08:30|12:20|FEL 115 - İlk Çağ Felsefe Tarihi / Doç. Dr. Önder BİLGİN / (Derslik Z-01)
THURSDAY|1|13:30|15:20|TDB 101 - Türk Dili I / Öğr. Gör. Betül BİLGİN / Pr. Dr. Erol GÜNGÖR AMFİSİ
THURSDAY|2|08:30|12:20|FEL 213 - Modern Mantık - I / Öğr. Gör. Serpil SATI TOKTAŞ / (Derslik Z-02)
THURSDAY|2|15:30|17:20|FEL 299 - Felsefi Paradokslar / Arş. Gör. Dr. Orkan ERNALBANT / (Derslik Z-04)
THURSDAY|3|13:30|17:20|SOS 383 - Güncel Sosyolojik Araştırmalar / Prof. Dr. Şahin FİLİZ / (Derslik Z-02)
THURSDAY|4|14:30|17:20|FEL 421 - 20. Yüzyıl Felsefesi / Doç. Dr. Şahin ÖZÇINAR / (Derslik Z-03)
FRIDAY|1|14:30|17:20|FEL 103 - Felsefeye Giriş / Öğr. Gör. Serpil SATI TOKTAŞ / (Derslik Z-01)
FRIDAY|2|08:30|12:20|SOS 231 - Sosyolojiye Giriş / Doç. Dr. Sinem Burcu UĞUR / (Derslik Z-02)
FRIDAY|3|10:30|12:20|FEL 313 - Metin Analizleri I / Öğr. Gör. Serpil SATI TOKTAŞ / (Derslik Z-01)
FRIDAY|3|14:30|16:20|FEL 349 - Felsefe ve Film I / Arş. Gör. Dr. Orkan ERNALBANT / (Derslik Z-03)
FRIDAY|4|08:30|10:20|Lisans Bitirme Tezi
    """.trimIndent()
    private val englishRegularData = """
MONDAY|1|13:30|14:20|İDE 151 Akademik Yazma ve Etik / (508) / Öğr. Gör. Ahmet Kütük
MONDAY|1|14:30|15:20|İDE 151 Akademik Yazma ve Etik / (508) / Öğr. Gör. Ahmet Kütük
MONDAY|2|08:30|09:20|İDE 291 Dünya Uygarlıkları / (510) / Prof. Dr. Arda Arıkan
MONDAY|2|09:30|10:20|İDE 291 Dünya Uygarlıkları / (510) / Prof. Dr. Arda Arıkan
MONDAY|3|13:30|14:20|İDE 369 Film ve Edebiyat / (Bumin Kağan Amfisi) / Öğr. Gör. Dr. Burak Yiğit
MONDAY|3|14:30|15:20|İDE 369 Film ve Edebiyat / (Bumin Kağan Amfisi) / Öğr. Gör. Dr. Burak Yiğit
MONDAY|4|15:30|16:20|İDE 453 Shakespeare: Dönemi ve / Şiirleri / (509) / Öğr. Gör. Dr. Burak Yiğit
MONDAY|4|16:30|17:20|İDE 453 Shakespeare: Dönemi / ve Şiirleri / (509) / Öğr. Gör. Dr. Burak Yiğit
TUESDAY|1|08:30|09:20|ATA 101 Atatürk İlkeleri ve / İnkılap Tarihi I / (508) / Öğr. Gör. Dr. Ahmet Kısa
TUESDAY|1|09:30|10:20|ATA 101 Atatürk İlkeleri ve / İnkılap Tarihi I / (508) / Öğr. Gör. Dr. Ahmet Kısa
TUESDAY|1|10:30|11:20|İDE 157 Kültür Çalışmalarına Giriş I / (509) / Öğr. Gör. Dr. Burak Yiğit
TUESDAY|1|11:30|12:20|İDE 157 Kültür Çalışmalarına Giriş I / (509) / Öğr. Gör. Dr. Burak Yiğit
TUESDAY|1|13:30|14:20|İDE 153 Dilbilime Giriş / (508) / Öğr. Gör. Ahmet Kütük
TUESDAY|1|14:30|15:20|İDE 153 Dilbilime Giriş / (508) / Öğr. Gör. Ahmet Kütük
TUESDAY|1|15:30|16:20|YDB 101 Almanca I / (508) / Öğr. Gör. Arzu Aydemir Ümit
TUESDAY|1|16:30|17:20|YDB 101 Almanca I / (508) / Öğr. Gör. Arzu Aydemir Ümit
TUESDAY|2|08:30|09:20|İDE 297 Batı Medeniyetinin / Doğulu Kökleri I / (508) / Öğr. Gör. Dr. Burak Yiğit
TUESDAY|2|09:30|10:20|İDE 297 Batı Medeniyetinin / Doğulu Kökleri I / (508) / Öğr. Gör. Dr. Burak Yiğit
TUESDAY|2|10:30|11:20|İDE 259 İngilizce Türkçe Çeviri / (508) / Doç. Dr. Orkun Kocabıyık
TUESDAY|2|11:30|12:20|İDE 259 İngilizce Türkçe Çeviri / (508) / Doç. Dr. Orkun Kocabıyık
TUESDAY|2|15:30|16:20|İDE 255 İngiliz Edebiyatının Ana / Hatları I / (510) / Doç. Dr. Orkun Kocabıyık
TUESDAY|2|16:30|17:20|İDE 255 İngiliz Edebiyatının / Ana Hatları I / (510) / Doç. Dr. Orkun Kocabıyık
TUESDAY|3|15:30|16:20|İDE 355 İngiliz Tiyatrosu I / (509) / Dr. Öğr. Üyesi Emine Şentürk
TUESDAY|3|16:30|17:20|İDE 355 İngiliz Tiyatrosu I / (509) / Dr. Öğr. Üyesi Emine Şentürk
TUESDAY|4|15:30|16:20|İDE 451 Edebiyat Eleştirisi I / (508) / Öğr. Gör. Dr. Burak Yiğit
TUESDAY|4|16:30|17:20|İDE 451 Edebiyat Eleştirisi I / (508) / Öğr. Gör. Dr. Burak Yiğit
WEDNESDAY|1|10:30|11:20|TDB 101 Türk Dili I / (***) / Öğr. Gör. Betül Bilgin
WEDNESDAY|1|11:30|12:20|TDB 101 Türk Dili I / (***) / Öğr. Gör. Betül Bilgin
WEDNESDAY|1|13:30|14:20|İDE 155 Dinleme ve Sesletim / (510) / Doç. Dr. H. Sezgi Saraç Durgun
WEDNESDAY|1|14:30|15:20|İDE 155 Dinleme ve Sesletim / (510) / Doç. Dr. H. Sezgi Saraç Durgun
WEDNESDAY|1|15:30|16:20|İDE 159 Öykü ve Roman / İncelemesi / (509) / Dr. Öğr. Üyesi Emine Şentürk
WEDNESDAY|1|16:30|17:20|İDE 159 Öykü ve Roman / İncelemesi / (509) / Dr. Öğr. Üyesi Emine Şentürk
WEDNESDAY|2|10:30|11:20|İDE 295 Dil, Bilim ve Toplum I / (508) / Doç. Dr. H. Sezgi Saraç Durgun
WEDNESDAY|2|11:30|12:20|İDE 295 Dil, Bilim ve Toplum I / (508) / Doç. Dr. H. Sezgi Saraç Durgun
WEDNESDAY|2|13:30|14:20|İDE 251 Mitoloji I / (509) / Doç. Dr. M. Galip Zorba
WEDNESDAY|2|14:30|15:20|İDE 251 Mitoloji I / (509) / Doç. Dr. M. Galip Zorba
WEDNESDAY|3|08:30|09:20|TDP 301 Toplumsal Duyarlılık / ve Katkı Projeleri I / (536) / Prof. Dr. Arda Arıkan / --------------------- / TDP 301 Toplumsal Duyarlılık / ve Katkı Projeleri I / (533) / Doç. Dr. H. Sezgi Saraç Durgun / --------------------- / TDP 301 Toplumsal Duyarlılık / ve Katkı Projeleri I / (535) / Doç. Dr. M. Galip Zorba / --------------------- / TDP 301 Toplumsal Duyarlılık / ve Katkı Projeleri I / (531) / Doç. Dr. Orkun Kocabıyık / --------------------- / TDP 301 Toplumsal Duyarlılık / ve Katkı Projeleri I / (534) / Dr. Öğr. Üyesi Emine Şentürk / --------------------- / TDP 301 Toplumsal Duyarlılık / ve Katkı Projeleri I / (527) / Öğr. Gör. Dr. Burak Yiğit
WEDNESDAY|3|09:30|10:20|TDP 301 Toplumsal Duyarlılık ve / Katkı Projeleri I / (536) / Prof. Dr. Arda Arıkan / --------------------- / TDP 301 Toplumsal Duyarlılık ve / Katkı Projeleri I / (533) / Doç. Dr. H. Sezgi Saraç Durgun / --------------------- / TDP 301 Toplumsal Duyarlılık ve / Katkı Projeleri I / (535) / Doç. Dr. M. Galip Zorba / --------------------- / TDP 301 Toplumsal Duyarlılık ve / Katkı Projeleri I / (531) / Doç. Dr. Orkun Kocabıyık / --------------------- / TDP 301 Toplumsal Duyarlılık ve / Katkı Projeleri I / (534) / Dr. Öğr. Üyesi Emine Şentürk / --------------------- / TDP 301 Toplumsal Duyarlılık ve / Katkı Projeleri I / (527) / Öğr. Gör. Dr. Burak Yiğit
WEDNESDAY|3|13:30|14:20|İDE 353 İngiliz Romanı I / (508) / Dr. Öğr. Üyesi Emine Şentürk
WEDNESDAY|3|14:30|15:20|İDE 353 İngiliz Romanı I / (508) / Dr. Öğr. Üyesi Emine Şentürk
WEDNESDAY|3|15:30|16:20|İDE 351 İngiliz Öykücülüğü / (508) / Doç. Dr. Orkun Kocabıyık
WEDNESDAY|3|16:30|17:20|İDE 351 İngiliz Öykücülüğü / (508) / Doç. Dr. Orkun Kocabıyık
WEDNESDAY|4|10:30|11:20|İDE 461 Karşılaştırmalı Edebiyat / (510) / Doç. Dr. Orkun Kocabıyık
WEDNESDAY|4|11:30|12:20|İDE 461 Karşılaştırmalı Edebiyat / (510) / Doç. Dr. Orkun Kocabıyık
WEDNESDAY|4|15:30|16:20|İDE 457 Uygulamalı Dilbilim I / (510) / Doç. Dr. H. Sezgi Saraç Durgun
WEDNESDAY|4|16:30|17:20|İDE 457 Uygulamalı Dilbilim I / (510) / Doç. Dr. H. Sezgi Saraç Durgun
THURSDAY|2|08:30|09:20|İDE 293 Bilim ve Edebiyat I / (509) / Doç. Dr. M. Galip Zorba
THURSDAY|2|09:30|10:20|İDE 293 Bilim ve Edebiyat I / (509) / Doç. Dr. M. Galip Zorba
THURSDAY|3|13:30|14:20|İDE 357 Amerikan Öykücülüğü / (509) / Doç. Dr. M. Galip Zorba
THURSDAY|3|14:30|15:20|İDE 357 Amerikan Öykücülüğü / (509) / Doç. Dr. M. Galip Zorba
THURSDAY|4|10:30|11:20|İDE 467 Fantastik Edebiyat ve Yüzüklerin / Efendisi / (509) / Doç. Dr. M. Galip Zorba
THURSDAY|4|11:30|12:20|İDE 467 Fantastik Edebiyat ve / Yüzüklerin Efendisi / (509) / Doç. Dr. M. Galip Zorba
FRIDAY|2|10:30|11:20|İDE 267 Çocuk Edebiyatına Giriş / (510) / Öğr. Gör. Gözde Yurtsever Bodur
FRIDAY|2|11:30|12:20|İDE 267 Çocuk Edebiyatına Giriş / (510) / Öğr. Gör. Gözde Yurtsever Bodur
FRIDAY|2|13:30|14:20|İDE 253 İngiliz Kültür Tarihi I / (510) / Öğr. Gör. Gözde Yurtsever Bodur
FRIDAY|2|14:30|15:20|İDE 253 İngiliz Kültür Tarihi I / (510) / Öğr. Gör. Gözde Yurtsever Bodur
FRIDAY|3|15:30|16:20|İDE 363 Dil ve Kültür Öğretimi / (510) / Öğr. Gör. Gözde Yurtsever Bodur
FRIDAY|3|16:30|17:20|İDE 363 Dil ve Kültür Öğretimi / (510) / Öğr. Gör. Gözde Yurtsever / Bodur
FRIDAY|4|13:30|14:20|İDE 455 Amerikan Kültür Tarihi / (508) / Prof. Dr. Arda Arıkan
FRIDAY|4|14:30|15:20|İDE 455 Amerikan Kültür Tarihi / (508) / Prof. Dr. Arda Arıkan
    """.trimIndent()
    private val artHistoryData = """
MONDAY|1|09:30|12:20|SAT 107 10-14. YÜZYIL ASYA TÜRK SANATI I- D302- Prof.Dr. Osman / ERAVŞAR
MONDAY|1|13:30|15:20|SAT 111 İSLAM ÖNCESİ TÜRK SANATI I- / D303 Prof. Dr. Abdullah KARAÇAĞ
MONDAY|2|13:30|15:20|SAT 213 ORTAÇAĞ ARKEOLOJİSİ VE KAZI / TEKNİKLERİ I D302- Prof.Dr. Osman ERAVŞAR
MONDAY|3|10:30|12:20|SAT 345 BİZANS DÖNEMİ ANITSAL MİMARİ / PLASTİĞİ I D303 Arş.
MONDAY|3|13:30|15:20|SAT 307 TÜRK İSLAM TASVİR SANATI I - / D301 Öğr. Gör. Dr. Hacer TUNCER
MONDAY|3|15:30|17:20|SAT 309 BEYLİKLER DÖNEMİ SANATI D302 / Öğr. Gör. Dr. Hacer TUNCER
MONDAY|4|11:30|15:20|SAT 403 OSMANLI RESİM SANATI I D304 Öğr. Gör. Dr. Yasemin ECESOY
MONDAY|4|15:30|17:20|SAT 411 CUMHURİYET DÖNEMİ TÜRK / SANATI I D304 Öğr. Gör. Dr. Yasemin ECESOY
TUESDAY|1|08:30|10:20|ATA 101 ATATÜRK İLKELERİ VE İNKILAP TARİHİ I / D302 / Öğr. Gör. Dr. Koray ERGİN
TUESDAY|1|10:30|12:20|YBD 101 İNGİLİZCE I D302 ÖĞR. GÖR. Ahmet / KÜTÜK
TUESDAY|1|15:30|17:20|SAT 101 SANAT TARİHİNE GİRİŞ VE / TERMİNOLOJI-I D303 Öğr. Gör. Dr. Hacer
TUESDAY|2|10:30|12:20|SAT 203 OSMANLI TÜRKÇESİ METİNLER I / D303 Arş. Gör. Dr. Abdullah ZARARSIZ
TUESDAY|2|13:30|15:20|SAT 209 TÜRK-İSLAM EL SANATLARI I - / D302 Öğr. Gör. Dr. Hacer TUNCER
TUESDAY|3|08:30|10:20|SAT 323 OSMANLI TÜRKÇESİ EPİGRAFİ I / D303 Arş. Gör. Dr. Abdullah ZARARSIZ
TUESDAY|3|10:30|12:20|SAT 343 İLHANLI SANATI I D301 Prof.Dr. Osman / ERAVŞAR
TUESDAY|3|13:30|15:20|SAT 305 SANAT TARİHİ BİLİMSEL / ARAŞTIRMA TEKNİKLERİ I D304- Doç. Dr. Lokman / TAY
TUESDAY|3|15:30|17:20|SAT 315 SERAMİK SANATI TARİHİ I- D302 / Doç. Dr. Lale AVŞAR
TUESDAY|4|10:30|12:20|SAT 415 BİZANS EL SANATLARI I D304 Öğr. / Gör. Dr. Şamil YİRŞEN
TUESDAY|4|13:30|15:20|SAT 401 KLASİK OTSAMYANLI MİMARİSİ I / D303 Prof. Dr. Yıldıray ÖZBEK
TUESDAY|4|15:30|17:20|SAT 417 SANAT FELSEFESİ VE ESTETİK I / D304 Öğr. Gör. Dr. Yasemin ECESOY
WEDNESDAY|1|13:30|15:20|SAT 103 MİTOLOJİ VE İKONOGRAFİ I- D301 / Öğr. Gör. Dr. Şamil YİRŞEN
WEDNESDAY|1|15:30|16:20|KPD 101 KARİYER / PLANLAMA D301 Arş.
WEDNESDAY|2|13:30|15:20|SAT 205 TÜRK MİMARİSİNDE SÜSLEME I / D302 Prof. Dr. Abdullah KARAÇAĞ
WEDNESDAY|2|15:30|17:20|SAT 201 ANADOLU SELÇUKLU MİMARİSİ I / D302 Prof. Dr. Mustafa DENKTAŞ
WEDNESDAY|3|10:30|12:20|SAT 331 BİZANS SİYASAL VE KULTUR / TARİHİ I D304 Öğr. Gör. Dr. Şamil YİRŞEN
WEDNESDAY|3|13:30|15:20|SAT 341 MEMLUK SANATI- D303 Prof. Dr. / Yıldıray ÖZBEK
WEDNESDAY|3|15:30|17:20|SAT 303 AVRUPA PLASTİK SANATLARI I- / D304 Öğr. Gör. Dr. Yasemin ECESOY
WEDNESDAY|4|10:30|12:20|SAT 401 KLASİK OSMANLI MİMARİSİ I / D303 Prof. Dr. Yıldıray ÖZBEK
WEDNESDAY|4|13:30|15:20|SAT 409 SANAT AKIMLARI D304 Öğr. Gör. Dr. / Yasemin ECESOY
THURSDAY|1|10:30|12:20|SAT 113 ANTİK ÇAĞ SANATI-I D302 Arş. / Gör. Dr. Elif GÜNGÖR
THURSDAY|1|13:30|15:20|SAT 105 ERKEN İSLAM SANATI I- D302 Doç. / Dr. Lokman TAY
THURSDAY|1|15:30|17:20|TDB 101 TÜRK DİLİ I (EROL GÜNGÖR AMFİSİ) Öğr. / Gör. Betül BİLGİN
THURSDAY|3|08:30|10:20|SAT 313 PROJE I Öğr. Gör. Dr. Esra TAY
THURSDAY|3|10:30|12:20|SAT 301 MÜZEOLOJİ- D303 Doç. Dr. Lale / AVŞAR
THURSDAY|3|13:30|15:20|SAT 311 BİZANS RESİM SANATI I D303 Öğr. / Gör. Dr. Şamil YİRŞEN
THURSDAY|3|15:30|17:20|SAT 329 TURK MEZAR KULTURU VE / MEZARTAŞLARI I D303 Prof. Dr. Abdullah
THURSDAY|4|15:30|17:20|SAT 407 TÜRK SİVİL MİMARİSİ I D304 Öğr. / Gör. Dr. Esra TAY
FRIDAY|1|13:30|17:20|SAT 109 TEKNİK RESİM VE RÖLÖVE I- D305 Doç. Dr. Lokman TAY
FRIDAY|2|10:30|12:20|SAT 207 BİZANS MİMARİSİ I- D303 Prof. Dr. / Burcu CEYLAN DUGGAN
FRIDAY|2|13:30|15:20|SAT 211 AVRUPA SANATI I D303- Prof. Dr. / Burcu CEYLAN DUGGAN
FRIDAY|4|08:30|10:20|SAT 405 BİTİRME ÇALIŞMASI Prof. Dr. / Mustafa DENKTAŞ, Prof. Dr. Yıldıray / ÖZBEK,Prof.Dr. Osman ERAVŞAR, Prof. Dr.
FRIDAY|4|15:30|17:20|SAT 419 ANADOLU ESKİ KENTLERİ I D303 Prof. / Dr. Burcu CEYLAN DUGGAN
    """.trimIndent()
    private val historyData = """
MONDAY|1|13:30|15:20|TAR143 Osmanlıca I-G.Dinç (D201)
MONDAY|1|15:30|16:20|KPD101 Kariyer Planlama- / D.Çakılcı (D201)
MONDAY|2|08:30|10:20|TAR263 Haçlı Seferleri Tarihi I-Z.Güngör (D203)
MONDAY|2|10:30|12:20|TAR273 Anadolu Selçuklu Devleti ve Beylikler Tarihi-Z.Güngör / (D203)
MONDAY|2|13:30|15:20|TAR251 Osmanlı Tarih Metinleri I-G.Yılmaz Diko (D203)
MONDAY|2|15:30|17:20|TAR257 Nümismatik I-Ö.Tatar (D203) / TAR267 Antikçağ Tarihi Kaynakları-E.Alten Güler (D205)
MONDAY|3|08:30|09:20|TAR305 Tarih Semineri I- / E.Taşbaş (D204)
MONDAY|3|13:30|15:20|TAR343 Türk Hukuk Tarihi I-D.Çakılcı (D204)
MONDAY|3|15:30|17:20|TAR387 Osmanlı Kültürü ve Medeniyeti I-F.Şimşek Touati / (D202)
MONDAY|4|08:30|09:20|TAR427 Çağdaş Dünya / Tarihi I-L.Derviş (D202)
MONDAY|4|09:30|12:20|TAR405 Osmanlı Tarihi IV (1876-1918)-S.Tunç (D202)
MONDAY|4|13:30|15:20|TAR479 Türk Siyasal Hayatı I-S.Tunç (D202) / TAR477 Türkiye İtalya İlişkileri-A.Zararsız (D206)
MONDAY|4|15:30|17:20|TAR401 Çağdaş Dünya Tarihi-L.Derviş (D204)
TUESDAY|1|08:30|10:20|ATA101 Atatürk İlkeleri ve İnkılap Tarihi I- / Öğr. Gör. Dr. Koray Ergin (D302)
TUESDAY|1|10:30|12:20|YBD101 İngilizce I- / Öğr. Gör. Ahmet Kütük (D302)
TUESDAY|2|08:30|10:20|TAR247 Anadolu Selçuklu Devleti Tarihi-Z.Güngör / (D203)
TUESDAY|2|10:30|12:20|TAR243 Eskiçağ Uygarlık Tarihi-D.S.Lenger (D203)
TUESDAY|2|13:30|15:20|TAR271 Karşılaştırmalı Uygarlıklar Tarihi I-E.Taşbaş (D203)
TUESDAY|2|15:30|17:20|TAR255 Kafkasya Tarihi-E.Taşbaş (D203)
TUESDAY|3|08:30|09:20|TAR305 Tarih Semineri I- / E.Taşbaş (D201)
TUESDAY|3|10:30|12:20|TAR389 Osmanlı’da Gündelik Hayat I-H.Akın Zorba (D201)
TUESDAY|3|13:30|15:20|TAR329 Rusya Tarihi I-Ş.Doğan (D201) / TAR317 Türk Siyasi Düşünceler Tarihi I-S.Tunç (D202)
TUESDAY|3|15:30|17:20|TAR359 Roma İmparatorluk Tarihi I-D.S.Lenger (D202)
TUESDAY|4|08:30|09:20|TAR427 Çağdaş Dünya / Tarihi I-L.Derviş (D202)
TUESDAY|4|09:30|12:20|TAR411 Milli Mücadele Tarihi-A.Yiğit (D202)
TUESDAY|4|13:30|15:20|TAR473 Yakın Dönem Türkiye Tarihi (1950-1980)-A.Yiğit / (D204)
TUESDAY|4|15:30|17:20|TAR457 Türk Dış Politikası (1950-1980)-A.Yiğit (D206)
WEDNESDAY|1|08:30|10:20|TAR105 İslam Tarihi I-Z.Güngör (D201)
WEDNESDAY|1|10:30|12:20|TAR127 İslam Öncesi Türk Tarihi-E.Kalan (D201)
WEDNESDAY|1|13:30|15:20|TAR123 Tarih Metodolojisi I-Ş.Doğan (D201)
WEDNESDAY|1|15:30|17:20|TDB101 Türk Dili I- / Öğr. Gör. Betül Bilgin (Erol Güngör Amfisi)
WEDNESDAY|2|08:30|10:20|TAR219 Türkiye'nin Tarihsel Coğrafyası I-G.Yılmaz / Diko (D206)
WEDNESDAY|2|10:30|12:20|TAR201 Türk Tarihçiliği ve Tarihçileri I-M.Güçlü (D202)
WEDNESDAY|2|13:30|15:20|TAR251 Osmanlı Tarih Metinleri I-G.Yılmaz Diko (D203)
WEDNESDAY|2|15:30|17:20|TAR265 Türk İslam Sanatları ve Mimarisi Tarihi (İslam Dev. ve / Selçuklu)-D.Çakılcı (D203)
WEDNESDAY|3|08:30|09:20|TAR305 Tarih Semineri I- / E.Taşbaş (D202)
WEDNESDAY|3|09:30|12:20|TAR373 Osmanlı Tarihi II (1566-1789)-A.Zararsız (D203)
WEDNESDAY|3|13:30|15:20|TAR325 Seyyahlar ve Anadolu (1839-1923)-M.Güçlü (D202)
WEDNESDAY|3|15:30|17:20|TAR375 Osmanlı Kurumları I-N.A.Aksoy (D201)
WEDNESDAY|4|10:30|12:20|TAR443 Rusya Türklerinde Fikir Hareketleri-Ş.Doğan (D204) / TAR461 Balkan Tarihi I-N.A.Aksoy (D205)
WEDNESDAY|4|13:30|15:20|TAR471 Osmanlı Yenileşme Tarihi I-D.Çakılcı (D204) / TAR425 Tarih Felsefesi I-E.Alten Güler (D205)
WEDNESDAY|4|15:30|17:20|TAR401 Çağdaş Dünya Tarihi-L.Derviş (D204)
THURSDAY|1|10:30|12:20|TAR101 Doğu Avrupa Türk Tarihi (IV-X. Yüzyıl)-E.Kalan / (D201)
THURSDAY|1|13:30|15:20|TAR147 Eskiçağ Tarihi I-M.Arslan (D201)
THURSDAY|1|15:30|17:20|TAR143 Osmanlıca I-G.Dinç (D201)
THURSDAY|2|09:30|10:20|TAR205 Selçuklu Sosyal ve / Ekonomik Tarihi-Z.Güngör / (D206)
THURSDAY|2|10:30|12:20|TAR273 Anadolu Selçuklu Devleti ve Beylikler Tarihi-Z.Güngör / (D203)
THURSDAY|2|13:30|15:20|TAR207 Bizans Tarihi-E.Alten Güler (D203)
THURSDAY|2|15:30|17:20|TAR215 Anadolu'da İskender-E.Alten Güler (D203)
THURSDAY|3|09:30|12:20|TAR307 Yeniçağ Avrupa Tarihi-F.Şimşek Touati (D202)
THURSDAY|3|13:30|15:20|TAR331 Türk Göçleri I-E.Taşbaş (D202) / TAR351 Türk Denizcilik Tarihi I-F.Şimşek Touati (D205)
THURSDAY|3|15:30|17:20|TAR313 Türk Kültür Tarihi I-S.Tan (D202) TAR / 323 Türkistan Tarihi - E. Kalan (D206)
THURSDAY|4|08:30|09:20|TAR417 Bitirme Çalışması / I-Tüm şubeler
THURSDAY|4|10:30|12:20|TAR459 Türk Basın-Yayın Tarihi I-M.Güçlü (D204)
THURSDAY|4|13:30|15:20|TAR433 Cumhuriyet Tarihinin Kaynakları-M.Güçlü (D206) / TAR423 Osmanlı Arşiv Belgeleri I-S.Tan (D206)
THURSDAY|4|15:30|17:20|TAR475 Dünya Göç Tarihi I-E.Taşbaş (D204)
FRIDAY|2|09:30|12:20|TAR205 Selçuklu Sosyal ve / Ekonomik Tarihi-Z.Güngör / (D206)
FRIDAY|3|09:30|12:20|TAR391 Osmanlı Kurumları-N.A.Aksoy (D202)
FRIDAY|3|14:30|17:20|TAR341 Osmanlı Paleografyası I-H.Durgun (D203) / TAR393 Arapça I-Z.Güngör (D201) / TAR395 Helence I-E.Alten Güler (D202) / TAR399 Rusça I-L.Derviş (D204) / TAR397 Latince I-(Bu dönem açılmayacak)
FRIDAY|4|08:30|09:20|TAR417 Bitirme Çalışması / I-Tüm şubeler
FRIDAY|4|10:30|12:20|TAR413 Türkiye Cumhuriyeti Tarihi I-A.Yiğit (1923-1950) / (D204)
FRIDAY|4|15:30|17:20|TAR403 Osmanlı Sosyo-Ekonomik Tarihi I-G.Yılmaz Diko / (D205)
    """.trimIndent()
    private val turkishRegularData = """
MONDAY|1|09:30|12:20|Türkiye Türkçesi I (E.D.) (307)
MONDAY|2|13:30|15:20|Eski Türk Edebiyatı I (F.Ö.) (308)
MONDAY|2|15:30|17:20|Türkiye Türkçesi III (E.A.) (308)
MONDAY|3|15:30|17:20|Tarihî Türkiye Türkçesinin Söz Dizimi I (E.D.) (309)
MONDAY|4|15:30|17:20|Eski Türk Edebiyatı V (F.Ö.) (310)
TUESDAY|1|08:30|10:20|Atatürk İlkeleri ve İnkılap Tarihi I (302)
TUESDAY|1|10:30|12:20|İngilizce I (302)
TUESDAY|1|13:30|15:20|Eski Türk Edebiyatına Giriş I (Ş.K.N.) (307)
TUESDAY|2|10:30|12:20|Dilbilimi I (E.A.) (308)
TUESDAY|2|13:30|15:20|Osmanlı Türkçesi III (E.D.) (308)
TUESDAY|2|15:30|17:20|Edebî Akımlar (O.K.) (308)
TUESDAY|3|13:30|15:20|Çağdaş Türk Lehçeleri I (A.A.) (309)
TUESDAY|3|15:30|17:20|Eski Türk Edebiyatı III (Ş.K.N.) (309)
TUESDAY|4|15:30|17:20|Türkiye Türkçesi Ağızları I (E.A.) (310)
WEDNESDAY|1|10:30|12:20|Türk Dili Tarihi I (A.A.) (307)
WEDNESDAY|1|13:30|15:20|Yeni Türk Edebiyatına Giriş I (O.K.) (307)
WEDNESDAY|1|15:30|17:20|Türk Dili I (Prof. Dr. Erol Güngör Amfisi)
WEDNESDAY|2|08:30|10:20|Eski Anadolu Türkçesi I (A.C.) (308)
WEDNESDAY|2|10:30|12:20|Orhun Türkçesi (A.Kök) (308)
WEDNESDAY|2|13:30|15:20|Yeni Türk Edebiyatı I (B.K.) (308)
WEDNESDAY|3|10:30|12:20|Karahanlı Türkçesi (S.Ü.) (309)
WEDNESDAY|3|13:30|15:20|Toplumsal Duyarlılık ve Katkı I (S.Ü.) (309)
WEDNESDAY|3|15:30|17:20|Yeni Türk Edebiyatı III (B.K.) (309)
WEDNESDAY|4|13:30|15:20|Türk Halk Edebiyatı Metin İncelemeleri I (E.O.) (310)
WEDNESDAY|4|15:30|17:20|Türk Dünyası Edebiyatları I (A.A.) (310)
WEDNESDAY|4|15:30|17:20|Yeni Türk Edebiyatında Eleştiri (O.K.) (308)
THURSDAY|1|10:30|12:20|Osmanlı Türkçesi I (B.G.) (307)
THURSDAY|1|13:30|15:20|Türk Halk Edebiyatına Giriş I (Ü.Y.Y.) (307)
THURSDAY|2|10:30|12:20|Türk Halk Edebiyatı I (A.Kartal) (308)
THURSDAY|3|10:30|12:20|Türk Halk Edebiyatı III (ÜYY) (309)
THURSDAY|3|13:30|15:20|Rusça I (T.O.) (309)
THURSDAY|4|08:30|10:20|Bitirme Çalışması I (Tüm Şubeler)
THURSDAY|4|13:30|15:20|Çağatay Türkçesi I (B.G.) (310)
THURSDAY|4|15:30|17:20|Türk Halk Edebiyatı V (A. Kartal) (310)
FRIDAY|1|10:30|12:20|Osmanlı Türkçesi I (B.G.) (307)
FRIDAY|3|10:30|12:20|Türk Halk Bilimi I (A.Kartal) (309)
FRIDAY|3|13:30|15:20|Dünya Edebiyatı I (T.O.) (309)
FRIDAY|4|08:30|10:20|Bitirme Çalışması I (Tüm Şubeler)
FRIDAY|4|15:30|17:20|Yeni Türk Edebiyatı V (T.O.) (310)
    """.trimIndent()

    private val englishEveningData = """
MONDAY|3|17:30|19:15|İDE 369 Film ve Edebiyat (Bumin Kağan Amfisi) / Öğr. Gör. Dr. Burak Yiğit
MONDAY|4|15:30|17:20|İDE 453 Shakespeare: Dönemi ve Şiirleri (509) / Öğr. Gör. Dr. Burak Yiğit
TUESDAY|3|15:30|17:20|İDE 355 İngiliz Tiyatrosu I (509) / Dr. Öğr. Üyesi Emine Şentürk
TUESDAY|4|15:30|17:20|İDE 451 Edebiyat Eleştirisi I (508) / Öğr. Gör. Dr. Burak Yiğit
WEDNESDAY|3|15:30|17:20|İDE 351 İngiliz Öykücülüğü (508) / Doç. Dr. Orkun Kocabıyık
WEDNESDAY|3|17:30|19:15|İDE 353 İngiliz Romanı I (508) / Dr. Öğr. Üyesi Emine Şentürk
WEDNESDAY|3|19:20|21:05|TDP 301 Toplumsal Duyarlılık ve Katkı Projeleri I (şube ve öğretim elemanı seçenekleri: 536 Prof. Dr. Arda Arıkan; 533 Doç. Dr. H. Sezgi Saraç Durgun; 535 Doç. Dr. M. Galip Zorba; 531 Doç. Dr. Orkun Kocabıyık; 534 Dr. Öğr. Üyesi Emine Şentürk; 527 Öğr. Gör. Dr. Burak Yiğit)
WEDNESDAY|4|15:30|17:20|İDE 457 Uygulamalı Dilbilim I (510) / Doç. Dr. H. Sezgi Saraç Durgun
WEDNESDAY|4|17:30|19:15|İDE 461 Karşılaştırmalı Edebiyat (510) / Doç. Dr. Orkun Kocabıyık
FRIDAY|3|15:30|17:20|İDE 357 Amerikan Öykücülüğü (509) / Doç. Dr. M. Galip Zorba
FRIDAY|4|15:30|17:20|İDE 455 Amerikan Kültür Tarihi (508) / Prof. Dr. Arda Arıkan
    """.trimIndent()
    private val russianData = """
MONDAY|1|10:30|12:20|RDE 105 - Rus Kültür Tarihi I / Öğr. Gör. Serdar Metreş / Z08 Nolu Derslik
MONDAY|1|13:30|15:20|RDE 111 - Edebiyat Bilimi I / Prof. Dr. Reyhan Çelik / Z08 Nolu Derslik
MONDAY|2|08:30|10:20|TDP 217 - Toplumsal Duyarlılık ve Katkı Projeleri / Öğr. Gör. Serdar Metreş / Z09 Nolu Derslik
MONDAY|4|15:30|17:20|RDE 413 - XX. Yüzyıl Rus Edebiyatı / Prof. Dr. Reyhan Çelik / Z07 Nolu Derslik
TUESDAY|1|08:30|10:20|ATA 101 - Atatürk İlkeleri ve İnkılap Tarihi I / Öğr. Gör. Derya Öge Set / 102 Nolu Derslik
TUESDAY|1|10:30|12:20|YBD 101 - İngilizce I / Doç. Dr. Abdullah Arslan / 102 Nolu Derslik
TUESDAY|2|08:30|10:20|RDE 225 - Kelime Bilgisi I / Öğr. Gör. Dr. Renata Aktaş / Z09 Nolu Derslik
TUESDAY|2|13:30|15:20|RDE 203 - XIX. Yüzyıl Rus Edebiyatı / Doç. Dr. Eda Havva Tan Metreş / Z09 Nolu Derslik
TUESDAY|3|10:30|12:20|RDE 305 - Edebiyat Teorisi / Prof. Dr. Reyhan Çelik / Z10 Nolu Derslik
TUESDAY|3|13:30|15:20|RDE 325 - Kültürel Coğrafya / Araş. Gör. Dr. Gökhan Gökdemir / Z08 Nolu Derslik
TUESDAY|3|15:30|17:20|RDE 313 - XX. Yüzyıl Rus Toplumsal Yaşamı / Doç. Dr. Eda Havva Tan Metreş / Z10 Nolu Derslik
TUESDAY|4|08:30|10:20|RDE 413 - XX. Yüzyıl Rus Edebiyatı / Prof. Dr. Reyhan Çelik / Z07 Nolu Derslik
TUESDAY|4|10:30|12:20|RDE 401 - Üslup Bilgisi / Öğr. Gör. Dr. Renata Aktaş / Z07 Nolu Derslik
WEDNESDAY|1|08:30|10:20|RDE 109 - Okuma Anlama I / Öğr. Gör. Serdar Metreş / Z08 Nolu Derslik
WEDNESDAY|1|10:30|12:20|RDE 107 - Rus Edebiyatına Giriş / Öğr. Gör. Serdar Metreş / Z08 Nolu Derslik
WEDNESDAY|1|13:30|15:20|TDB 101 - Türk Dili I / Öğr. Gör. Betül Bilgin / Prof. Dr. Erol Güngör Amfisi
WEDNESDAY|1|15:30|17:20|RDE 101 - Rusça Dilbilgisi / Dr. Öğr. Üyesi Fatih Düzgün / Z08 Nolu Derslik
WEDNESDAY|2|09:30|12:20|RDE 201 - Morfoloji / Dr. Öğr. Üyesi Fatih Düzgün / Z09 Nolu Derslik
WEDNESDAY|2|13:30|15:20|RDE 227 - Rusya Coğrafyası / Araş. Gör. Dr. Gökhan Gökdemir / Z09 Nolu Derslik
WEDNESDAY|3|08:30|10:20|RDE 315 - Pratik Rusça / Öğr. Gör. Dr. Renata Aktaş / Z10 Nolu Derslik
WEDNESDAY|3|13:30|15:20|RDE 303 - Klasik Rus Romanı / Dr. Öğr. Üyesi Tarana Oktan / Z10 Nolu Derslik
WEDNESDAY|4|10:30|12:20|RDE 403 - Metin Çözümlemesi / Öğr. Gör. Dr. Renata Aktaş / Z07 Nolu Derslik
WEDNESDAY|4|13:30|15:20|RDE 405 - İş Metinleri Çevirisi I / Dr. Öğr. Üyesi Fatih Düzgün / Z07 Nolu Derslik
THURSDAY|1|13:30|17:20|RDE 103 - Sözlü ve Yazılı Anlatım I / Öğr. Gör. Semra Başaran / Z08 Nolu Derslik
THURSDAY|2|13:30|15:20|RDE 203 - XIX. Yüzyıl Rus Edebiyatı / Doç. Dr. Eda Havva Tan Metreş / Z09 Nolu Derslik
THURSDAY|2|15:30|17:20|RDE 213 - Çeviri Kuram ve Yöntemleri I / Doç. Dr. Eda Havva Tan Metreş / Z09 Nolu Derslik
THURSDAY|3|10:30|12:20|RDE 307 - Basın Yayın Çevirisi / Dr. Öğr. Üyesi Fatih Düzgün / Z10 Nolu Derslik
THURSDAY|3|13:30|16:20|RDE 301 - Sentaks / Öğr. Gör. Dr. Renata Aktaş / Z07 Nolu Derslik
THURSDAY|4|10:30|12:20|RDE 409 - Pratik Rusça / Öğr. Gör. Dr. Renata Aktaş / Z07 Nolu Derslik
THURSDAY|4|13:30|15:20|RDE 419 - Rusça Öğretim Metodları I / Dr. Öğr. Üyesi Fatih Düzgün / Z10 Nolu Derslik
FRIDAY|1|15:30|17:20|RDE 101 - Rusça Dilbilgisi / Dr. Öğr. Üyesi Fatih Düzgün / Z08 Nolu Derslik
FRIDAY|2|10:30|12:20|RDE 229 - Kültürel Atölye Çalışmaları I / Dr. Öğr. Üyesi Fatih Düzgün / Z09 Nolu Derslik
FRIDAY|2|13:30|15:20|RDE 207 - Uygulamalı Metin Analizi / Dr. Öğr. Üyesi Fatih Düzgün / Z09 Nolu Derslik
FRIDAY|3|10:30|12:20|RDE 327 - Kültürel Atölye Çalışmaları III / Öğr. Gör. Serdar Metreş / Z10 Nolu Derslik
FRIDAY|4|08:30|10:20|RDE 407 - Bitirme Projesi I / Prof. Dr. Reyhan Çelik, Doç. Dr. Eda Havva Tan Metreş, Dr. Öğr. Üyesi Fatih Düzgün / Z07 Nolu Derslik
    """.trimIndent()
    private val psychologyData = """
MONDAY|1|11:30|12:20|KPD 101 - Kariyer Planlama / Doç. Dr. Aydın Çivilidağ / D 208
MONDAY|1|13:30|17:20|PSİ 103 - İstatistik I / Öğr. Gör. Dr. Süheyla Özen / D 210
MONDAY|2|13:30|16:20|PSİ 207 - Öğrenme Psikolojisi / Arş. Gör. Dr. Turan Gündüz / D 208
MONDAY|3|09:30|12:20|PSİ 313 - Çocuk İstismarı ve İhmali / Prof. Dr. Seda Bayraktar / D 209
MONDAY|4|09:30|12:20|PSİ 409 - Spor Psikolojisi / Öğr. Gör. Dr. Nazmi Bayköse / D 207
MONDAY|4|13:30|16:20|PSİ 403 - Adli Psikoloji / Prof. Dr. Seda Bayraktar / D 209
TUESDAY|1|08:30|10:20|ATA 101 - Atatürk İlkeleri ve İnkılap Tarihi I / Doç. Dr. Mustafa Malhut / D 208
TUESDAY|1|13:30|15:20|YBD 101 - İngilizce I / Öğr. Gör. Hülya Çelik / D 208
TUESDAY|2|09:30|12:20|PSİ 209 - Psikoloji Tarihi / Öğr. Gör. Dr. Gonca Köse / D 209
TUESDAY|3|09:30|12:20|PSİ 321 - Ölçme Yöntemleri / Arş. Gör. Dr. Turan Gündüz / D 207
TUESDAY|3|13:30|17:20|PSİ 301 - Araştırma Yöntemleri / Arş. Gör. Dr. Turan Gündüz / D 209
TUESDAY|4|09:30|12:20|PSİ 405 - Endüstri ve Örgüt Psikolojisi / Doç. Dr. Aydın Çivilidağ / D 210
TUESDAY|4|13:30|16:20|PSİ 415 - Psikolojik Danışmanlık ve Rehberlik / Doç. Dr. Aydın Çivilidağ / D 210
WEDNESDAY|1|09:30|12:20|PSİ 101 - Psikolojiye Giriş I / Prof. Dr. Evrim Öztop / D 208
WEDNESDAY|1|13:30|16:20|PSİ 107 - Sosyolojiye Giriş / Arş. Gör. Dr. Elif Şahin / D 503
WEDNESDAY|2|13:30|16:20|PSİ 203 - Sosyal Psikoloji I / Dr. Öğr. Üyesi Enes Yalçın / D 208
WEDNESDAY|3|09:30|12:20|PSİ 303 - Psikopatoloji I / Dr. Öğr. Üyesi Ece Varlık Özsoy / D 207
WEDNESDAY|3|13:30|16:20|PSİ 319 - Kişilik Kuramları / Öğr. Gör. Dr. Gonca Köse / D 209
WEDNESDAY|4|09:30|12:20|PSİ 429 - Çocuk ve Ergen Ruh Sağlığı / Dr. Öğr. Üyesi Mahperi Uluyol / D 210
WEDNESDAY|4|13:30|17:20|PSİ 407 - Alan Çalışması III / Arş. Gör. Dr. Turan Gündüz ve Prof. Dr. Evrim Öztop / D 210 ve D 207
THURSDAY|1|10:30|12:20|TDB 101 - Türk Dili I / Öğr. Gör. Betül Bilgin / Prof. Dr. Erol Güngör Amfisi
THURSDAY|2|08:30|12:20|PSİ 201 - Psikolojide İstatistik Uygulamaları / Prof. Dr. Ayça Özen Çıplak / D 208
THURSDAY|3|09:30|12:20|PSİ 315 - Terminoloji / Dr. Öğr. Üyesi Ece Varlık Özsoy ve Mahperi Uluyol / D 207 ve D 210
THURSDAY|3|13:30|16:20|PSİ 305 - Fizyolojik Psikoloji / Doç. Dr. Deniz Kantar / D 208
THURSDAY|4|08:30|12:20|PSİ 411 - Sosyal Psikolojide Araştırma Yöntemleri / Dr. Öğr. Üyesi Enes Yalçın / D 209
THURSDAY|4|13:30|17:20|PSİ 401 - Görüşme Teknikleri / Öğr. Gör. Dr. Gonca Köse, Dr. Öğr. Üyesi Ece Varlık Özsoy ve Mahperi Uluyol / D 209, D 207 ve D 210
    """.trimIndent()
    private val sociologyData = """
MONDAY|1|08:30|12:20|SOS 103 - Psikolojiye Giriş / Öğr. Gör. Dr. Süheyla Gözen / D-402
MONDAY|1|13:30|16:20|SOS 115 - Eğitim Sosyolojisi / Doç. Dr. Hasan Hüseyin Aygül / D-402
MONDAY|2|09:30|12:20|SOS 205 - Yaşlılık ve Ölüm Sosyolojisi / Doç. Dr. Özge Zeybekoğlu / D-403
MONDAY|3|09:30|12:20|SOS 315 - Küreselleşme Tartışmaları / Doç. Dr. Gökhan V. Köktürk
MONDAY|3|13:30|16:20|SOS 309 - Turizm Sosyolojisi / Doç. Dr. Özge Zeybekoğlu / D-404
MONDAY|4|08:30|09:20|SOS 413 - Bitirme Çalışması
MONDAY|4|09:30|12:20|SOS 409 - Akademik Yazma ve Etik / Prof. Dr. Sevinç Güçlü, Doç. Dr. Suat Kolukırık, Doç. Dr. Hasan Hüseyin Aygül
MONDAY|4|13:30|16:20|SOS 407 - Sosyal Problemler / Prof. Dr. Nurşen Adak, Doç. Dr. Özge Zeybekoğlu, Doç. Dr. Maissam Nimer
TUESDAY|1|08:30|10:20|ATA 101 - Atatürk İlkeleri ve İnkılap Tarihi I / Doç. Dr. Mustafa Mahmur / D-208
TUESDAY|1|10:30|12:20|SOS 113 - Sosyolojiye Giriş / Prof. Dr. Gönül Demez / D-402
TUESDAY|1|13:30|15:20|YBD 101 - İngilizce I / Öğr. Gör. Hülya Çelik
TUESDAY|1|15:30|17:20|SOS 113 - Sosyolojiye Giriş / Prof. Dr. Gönül Demez / D-402
TUESDAY|2|09:30|12:20|SOS 203 - Aile Sosyolojisi / Prof. Dr. Nurşen Adak / D-403
TUESDAY|2|13:30|16:20|SOS 211 - Felsefe Tarihi I / Doç. Dr. Önder Bilgin / D-403
TUESDAY|3|10:30|12:20|TDP 301 - Toplumsal Duyarlılık ve Katkı Projeleri I / Doç. Dr. Maissam Nimer / D-404
TUESDAY|3|13:30|16:20|SOS 325 - Matematiksel Sosyolojiye Giriş / Prof. Dr. Özgür Arun / D-404
TUESDAY|4|08:30|09:20|SOS 413 - Bitirme Çalışması
TUESDAY|4|09:30|12:20|SOS 403 - Çağdaş Sosyoloji Kuramları / Prof. Dr. Sevinç Güçlü / D-404
TUESDAY|4|13:30|16:20|SOS 419 - Kültür ve Kimlik Sosyolojisi / Doç. Dr. Özge Zeybekoğlu, Doç. Dr. Maissam Nimer / D-401
WEDNESDAY|1|09:30|12:20|SOS 107 - Sosyal Antropoloji / Prof. Dr. Sevinç Güçlü / D-402
WEDNESDAY|1|13:30|16:20|SOS 109 - Hukuka Giriş / Doç. Dr. Yılmaz Yurtseven / D-402
WEDNESDAY|2|10:30|12:20|ENF 213 - Bilişim Teknolojileri Bağımlılığı / Öğr. Gör. Dr. T. Fatih Kalak / D-403
WEDNESDAY|2|13:30|16:20|SOS 207 - Dijital Sosyoloji / Prof. Dr. Suat Kolukırık / D-403
WEDNESDAY|3|09:30|12:20|SOS 303 - Göç Sosyolojisi / Prof. Dr. Suat Kolukırık / D-404
WEDNESDAY|3|13:30|16:20|SOS 301 - Kent Sosyolojisi / Prof. Dr. Sevinç Güçlü / D-401
WEDNESDAY|4|08:30|09:20|SOS 413 - Bitirme Çalışması
WEDNESDAY|4|09:30|12:20|SOS 411 - Endüstri ve Çalışma Sosyolojisi / Doç. Dr. Elife Kart / D-404
WEDNESDAY|4|13:30|16:20|SOS 417 - Popüler Kültür ve Gündelik Yaşam / Prof. Dr. Gönül Demez, Doç. Dr. Özge Zeybekoğlu, Doç. Dr. Maissam Nimer / D-404
THURSDAY|1|10:30|12:20|TDB 101 - Türk Dili I / Öğr. Gör. Betül Bilgin / Prof. Dr. Erol Güngör Amfisi
THURSDAY|1|13:30|16:20|SOS 105 - Felsefeye Giriş / Öğr. Gör. Serpil Satı Toktaş / D-402
THURSDAY|2|09:30|12:20|SOS 201 - Sosyoloji Tarihi I / Doç. Dr. Elife Kart / D-403
THURSDAY|2|13:30|16:20|SOS 213 - Sosyal Psikoloji / Dr. Öğr. Üyesi Enes Yalçın / D-403
THURSDAY|3|09:30|12:20|SOS 307 - Sosyal Tabakalaşma ve Eşitsizlik / Doç. Dr. Gökhan V. Köktürk / D-404
THURSDAY|3|13:30|16:20|SOS 305 - İktisat Sosyolojisi / Arş. Gör. Dr. Çağrı Elmas / D-401
THURSDAY|4|08:30|09:20|SOS 413 - Bitirme Çalışması
THURSDAY|4|09:30|12:20|SOS 421 - Kırsal Alan Sosyolojisi / Prof. Dr. Sevinç Güçlü, Prof. Dr. Suat Kolukırık, Doç. Dr. Özge Zeybekoğlu / D-401
THURSDAY|4|13:30|16:20|SOS 423 - İklim Değişikliği ve Toplum / Doç. Dr. Elife Kart, Arş. Gör. Dr. M. Suzan Ilık / D-404
FRIDAY|2|09:30|12:20|SOS 219 - Sosyolojide Yöntem / Doç. Dr. Hasan Hüseyin Aygül / D-404
FRIDAY|2|13:30|16:20|SOS 215 - Sosyal Bilimlerde Bilgisayar Destekli Nicel Veri Analizi I / Doç. Dr. Özden Özbay / D-403
FRIDAY|3|09:30|12:20|SOS 323 - Mesleki Yabancı Dil / Doç. Dr. Özden Özbay / D-401
FRIDAY|4|09:30|12:20|SOS 415 - Spor Sosyolojisi / Arş. Gör. Dr. Birtan Bozlu / D-404
    """.trimIndent()
    private val turkishEveningData = """
MONDAY|4|15:30|17:20|Eski Türk Edebiyatı V (F.Ö.) (310)
TUESDAY|4|15:30|17:20|Türkiye Türkçesi Ağızları I (E.A.) (310)
WEDNESDAY|4|15:30|17:20|Yeni Türk Edebiyatında Eleştiri (O.K.) (308)
WEDNESDAY|4|17:30|19:15|Türk Halk Edebiyatı Metin İncelemeleri I (E.O.) (310)
THURSDAY|4|15:30|17:20|Türk Halk Edebiyatı V (A. Kartal) (310)
THURSDAY|4|17:30|19:15|Çağatay Türkçesi I (B.G.) (310)
THURSDAY|4|19:20|20:10|Bitirme Çalışması I (Tüm Şubeler)
FRIDAY|4|15:30|17:20|Yeni Türk Edebiyatı V (T.O.) (310)
FRIDAY|4|19:20|20:10|Bitirme Çalışması I (Tüm Şubeler)
    """.trimIndent()

    private fun rawBlocks(data: String): List<RawBlock> = data.lineSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .mapNotNull { line ->
            val fields = line.split('|', limit = 5)
            if (fields.size != 5) return@mapNotNull null
            val day = ScheduleDay.entries.firstOrNull { it.name == fields[0] } ?: return@mapNotNull null
            val year = fields[1].toIntOrNull() ?: return@mapNotNull null
            RawBlock(day, year, fields[2], fields[3], fields[4])
        }
        .toList()

    private fun parseCourses(source: String): List<Course> {
        val normalized = source.replace(Regex("""\s*/\s*"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        val matches = codePattern.findAll(normalized).toList()
        val distinctCodes = matches.map { it.groupValues[1].replace(Regex("""\s+"""), "").uppercase() }.distinct()
        val segments = if (distinctCodes.size > 1) {
            matches.mapIndexed { index, match ->
                val next = matches.getOrNull(index + 1)?.range?.first ?: normalized.length
                normalized.substring(match.range.first, next)
            }
        } else listOf(normalized)
        return segments.mapNotNull(::parseCourse)
    }

    private fun parseCourse(source: String): Course? {
        val codeMatch = codePattern.find(source)
        val code = codeMatch?.groupValues?.get(1)?.replace(Regex("""\s+"""), " ")?.trim().orEmpty()
        val roomMatches = roomPattern.findAll(source).map { it.value.trim().trim('(', ')') }.distinct().toList()
        val teacherMatches = instructorPattern.findAll(source).toList()
        val initialsMatch = if (teacherMatches.isEmpty()) initialsPattern.find(source) else null
        val compactInstructorMatch = if (teacherMatches.isEmpty() && initialsMatch == null) {
            compactInstructorPattern.find(source)
        } else null
        val roomStart = roomPattern.find(source)?.range?.first ?: source.length
        val initialsStart = initialsMatch?.range?.first ?: source.length
        val titleStart = codeMatch?.range?.last?.plus(1) ?: 0
        val teacherStart = teacherMatches.firstOrNull()?.range?.first
            ?: initialsStart.takeIf { initialsMatch != null }
            ?: compactInstructorMatch?.range?.first
            ?: source.length
        val titleEnd = minOf(teacherStart, roomStart).coerceAtLeast(titleStart)
        var name = source.substring(titleStart.coerceAtMost(source.length), titleEnd.coerceAtMost(source.length))
            .replace('/', ' ')
            .trim(' ', '-', '–', '—', ':', ';', ',', '.')
            .replace(Regex("""\s+"""), " ")
            .trim()
        if (name.isBlank()) name = source.substringAfter(code, source).trim()
        if (name.isBlank()) name = source.trim()
        val instructors = teacherMatches.mapIndexed { index, match ->
            val nextTeacher = teacherMatches.getOrNull(index + 1)?.range?.first ?: source.length
            val nextRoom = roomPattern.findAll(source).firstOrNull { it.range.first > match.range.first }?.range?.first ?: source.length
            val end = minOf(nextTeacher, nextRoom)
            source.substring(match.range.first, end).trim(' ', '/', '-', '–', '—', ',', ';')
        }.filter(String::isNotBlank).distinct()
        val instructor = if (instructors.isNotEmpty()) instructors.joinToString(" / ")
            else initialsMatch?.groupValues?.getOrNull(1)
                ?: compactInstructorMatch?.groupValues?.getOrNull(1).orEmpty()
        val classroom = roomMatches.joinToString(" / ")
        val type = if (source.contains("FORMASYON", ignoreCase = true)) "Formasyon" else "Ders"
        return Course(code, name, instructor, classroom, type)
    }

    private fun program(
        department: String,
        sourceUrl: String,
        data: String,
        evening: Boolean = false
    ): List<ClassSchedule> {
        val rows = rawBlocks(data)
        val parsed = rows.flatMap { row -> parseCourses(row.sourceText).map { row to it } }
        val canonicalNames = parsed.groupBy { (row, course) ->
            listOf(row.day.name, row.year.toString(), course.code, course.instructor, course.classroom, course.type).joinToString("|")
        }.mapValues { (_, values) -> values.maxByOrNull { it.second.name.length }!!.second.name }
        return listOf(
            ClassSchedules.FIRST_YEAR, ClassSchedules.SECOND_YEAR,
            ClassSchedules.THIRD_YEAR, ClassSchedules.FOURTH_YEAR
        ).mapIndexed { yearIndex, yearLabel ->
            val year = yearIndex + 1
            val entries = parsed.filter { it.first.year == year }.flatMap { (row, course) ->
                val selectedPeriods = (if (evening) eveningPeriods else regularPeriods)
                    .filter { (start, end) -> start >= row.start && end <= row.end }
                val periods = selectedPeriods.ifEmpty { listOf(row.start to row.end) }
                periods.map { (start, end) ->
                    course.copy(name = canonicalNames[
                        listOf(row.day.name, row.year.toString(), course.code, course.instructor, course.classroom, course.type).joinToString("|")
                    ] ?: course.name).let {
                        ScheduleEntry(row.day, start, end, it.code, it.name, it.instructor, it.classroom, it.type)
                    }
                }
            }.distinctBy { listOf(it.day, it.startTime, it.endTime, it.courseCode, it.courseName, it.instructor, it.classroom, it.courseType).joinToString("|") }
                .sortedWith(compareBy<ScheduleEntry>({ it.day.ordinal }, { it.startTime }))
            ClassSchedule(
                faculty = ClassSchedules.LITERATURE_FACULTY,
                department = department,
                classYear = yearLabel,
                academicYear = "2026-2027",
                term = "Güz Yarıyılı",
                updatedAt = "2026-2027 Güz",
                sourcePage = 1,
                sourceUrl = sourceUrl,
                entries = entries,
                sourceWarning = when {
                    department == ClassSchedules.ENGLISH_LITERATURE_EVENING_DEPARTMENT && year <= 2 ->
                        "Gönderilen ikinci öğretim çizelgesinde yalnızca 3. ve 4. sınıf sayfaları yer alıyor; bu iki sınıf programı kaynakta boş bırakılmış."
                    department == ClassSchedules.ENGLISH_LITERATURE_EVENING_DEPARTMENT ->
                        "Dosyanın başlığında “örgün” yazsa da kullanıcı tarafından verilen kaynak ikinci öğretim dosyası ve ders saatleri 15.30’dan sonra. Kaynak başlığı tutarsız."
                    department == ClassSchedules.TURKISH_LITERATURE_EVENING_DEPARTMENT && year <= 3 ->
                        "Gönderilen ikinci öğretim çizelgesinde yalnızca 4. sınıf dersleri bulunuyor; 1–3. sınıflar kaynakta boş."
                    department == ClassSchedules.PSYCHOLOGY_DEPARTMENT ->
                        "Psikoloji PDF'sindeki ayrı Formasyon sütunu yıl/bölüm ders hücrelerinin dışında olduğu için sınıf programına dahil edilmedi."
                    else -> null
                }
            )
        }
    }

    fun schedulesFor(department: String?): List<ClassSchedule>? = when (department) {
        ClassSchedules.PHILOSOPHY_DEPARTMENT -> program(department, PHILOSOPHY_SOURCE, philosophyData)
        ClassSchedules.ENGLISH_LITERATURE_REGULAR_DEPARTMENT -> program(department, ENGLISH_REGULAR_SOURCE, englishRegularData)
        ClassSchedules.ENGLISH_LITERATURE_EVENING_DEPARTMENT -> program(department, ENGLISH_EVENING_SOURCE, englishEveningData, evening = true)
        ClassSchedules.RUSSIAN_LANGUAGE_AND_LITERATURE_DEPARTMENT -> program(department, RUSSIAN_SOURCE, russianData)
        ClassSchedules.PSYCHOLOGY_DEPARTMENT -> program(department, PSYCHOLOGY_SOURCE, psychologyData)
        ClassSchedules.ART_HISTORY_DEPARTMENT -> program(department, ART_HISTORY_SOURCE, artHistoryData)
        ClassSchedules.SOCIOLOGY_DEPARTMENT -> program(department, SOCIOLOGY_SOURCE, sociologyData)
        ClassSchedules.HISTORY_DEPARTMENT -> program(department, HISTORY_SOURCE, historyData)
        ClassSchedules.TURKISH_LITERATURE_REGULAR_DEPARTMENT -> program(department, TURKISH_REGULAR_SOURCE, turkishRegularData)
        ClassSchedules.TURKISH_LITERATURE_EVENING_DEPARTMENT -> program(department, TURKISH_EVENING_SOURCE, turkishEveningData, evening = true)
        else -> null
    }
}
