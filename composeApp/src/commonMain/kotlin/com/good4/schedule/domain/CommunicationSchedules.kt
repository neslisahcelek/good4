package com.good4.schedule.domain

/** Akdeniz Üniversitesi İletişim Fakültesinin 2026-2027 güz dönemi çizelgeleri. */
internal object CommunicationSchedules {
    private const val SOURCE_URL = "https://webis.akdeniz.edu.tr/uploads/1057/icerik_stk/Duyurular/2026-2027%20E%C4%9Fitim-%C3%96%C4%9Fretim%20Y%C4%B1l%C4%B1%20G%C3%BCz%20D%C3%B6nemi%20T%C3%BCm%20B%C3%B6l%C3%BCmler%20Ders%20Program%C4%B1.xlsx"
    private const val ACADEMIC_YEAR = "2026-2027"
    private const val TERM = "Güz Yarıyılı"
    private const val UPDATED_AT = "2026-09-16"

    private val classPeriods = listOf(
        "08:30" to "09:20", "09:30" to "10:20", "10:30" to "11:20",
        "11:30" to "12:20", "12:30" to "13:20", "13:30" to "14:20",
        "14:30" to "15:20", "15:30" to "16:20", "16:30" to "17:20",
        "17:30" to "18:20", "18:30" to "19:20"
    )

    private data class SourceBlock(
        val day: ScheduleDay,
        val start: String,
        val end: String,
        val code: String,
        val name: String,
        val instructor: String,
        val classroom: String,
        val section: String
    )

    /** DAY|START|END|CODE|COURSE|INSTRUCTOR|ROOM|SECTION; '-' means absent in the source. */
    private fun blocks(source: String): List<SourceBlock> = source.lineSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .mapNotNull { line ->
            val fields = line.split('|', limit = 8)
            if (fields.size != 8) return@mapNotNull null
            val name = fields[4].takeUnless { it == "-" }.orEmpty()
            if (name.isBlank()) return@mapNotNull null
            SourceBlock(
                day = ScheduleDay.valueOf(fields[0]),
                start = fields[1],
                end = fields[2],
                code = fields[3].takeUnless { it == "-" }.orEmpty(),
                name = name,
                instructor = fields[5].takeUnless { it == "-" }.orEmpty(),
                classroom = fields[6].takeUnless { it == "-" }.orEmpty(),
                section = fields[7].takeUnless { it == "-" }.orEmpty()
            )
        }
        .toList()

    private fun entries(source: String): List<ScheduleEntry> = blocks(source).flatMap { block ->
        classPeriods.asSequence()
            .filter { (periodStart, periodEnd) -> periodStart >= block.start && periodEnd <= block.end }
            .map { (periodStart, periodEnd) ->
                val courseType = when {
                    block.name.contains("staj", ignoreCase = true) -> "Staj"
                    block.name.contains("uygulama", ignoreCase = true) ||
                        block.name.contains("lab", ignoreCase = true) -> "Uygulama"
                    else -> "Ders"
                }
                ScheduleEntry(
                    day = block.day,
                    startTime = periodStart,
                    endTime = periodEnd,
                    courseCode = block.code,
                    courseName = block.name,
                    instructor = block.instructor,
                    classroom = block.classroom,
                    courseType = courseType,
                    section = block.section.takeUnless(String::isBlank)
                )
            }
            .toList()
    }

    private fun schedules(department: String, sources: List<String>): List<ClassSchedule> = (1..4).map { year ->
        ClassSchedule(
            faculty = ClassSchedules.COMMUNICATION_FACULTY,
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
            entries = entries(sources[year - 1])
        )
    }

    private val schedulesByDepartment = mapOf<String, List<String>>(
        ClassSchedules.JOURNALISM_DEPARTMENT to listOf(
            """
                MONDAY|13:30|15:20|İLE 101|İletişime Giriş (AMFİ 10)|Doç. Dr. Bahar URHAN|-|-
                MONDAY|15:30|17:20|GZT 105|Temel Hukuk GZT RTS (AMFİ 1)|Öğr. Gör. Begüm Birsen ASLAN ÇELİK|-|-
                TUESDAY|10:30|12:20|GZT 109|Sosyoloji (AMFİ 1)|Prof. Dr. Özgür ARUN|-|-
                TUESDAY|13:30|16:20|GZT 101|Gazetecilik I (AMFİ 10)|Prof. Dr. Tülay BEKTAŞ|-|-
                WEDNESDAY|08:30|10:20|İLE 103|Ofis Uygulamaları (LAB-06)|Öğr. Gör. Şahin AKBUNAR|-|-
                WEDNESDAY|13:30|15:20|ENF 114|Bilgisayar Destekli Çizim ve Tasarım (LAB-01) / ENF125 Yapay Zeka Okuryazarlığı (LAB-02) (13.30-16.20)|Öğr. Gör. Ramazan UYAR / Öğr. Gör. Dr. Evren SEZGİN|-|-
                WEDNESDAY|15:30|17:20|ENF 116|Çoklu Ortam Uygulamaları (LAB-05)/ENF125 Yapay Zeka Okuryazarlığı (LAB-02) (13.30-16.20)|Öğr. Gör. Şahin AKBUNAR|-|-
                THURSDAY|10:30|12:20|TDB 101|Türk Dili (GZT-RTS) (AMFİ 1)|Ögr.Gör. Bilal Nargöz|-|-
                THURSDAY|13:30|15:20|YBD 101|İngilizce I ( GZT-RTS ) (AMFİ 1)|Ögr. Gör. Hülya Çelik|-|-
                THURSDAY|15:30|17:20|ATA 101-102|Atatürk İlkeleri ve İnkılap Tarihi (yüz yüze) (RTS-GZT) (AMFİ 1)|Öğr. Gör. Dr. Koray Ergin|-|-
            """.trimIndent(),
            """
                MONDAY|10:30|12:20|İLE 201|İletişim Kuramları I (AMFİ 9)|Prof.Dr. Emel ARIK|-|-
                MONDAY|13:30|16:20|GZT 203|Yeni Medya (AMFİ 9)|Doç. Dr. Tuba LİVBERBER|-|-
                TUESDAY|09:30|12:20|GZT 201|Gazete Yayımlama Teknikleri I (AMFİ 10)|Prof. Dr. Mustafa ŞEKER|-|-
                TUESDAY|13:30|15:20|RTS 231|Kamera ve Işık (DERSLİK 21)|Öğr. Gör. Dr. Emrah Onur KARATAŞ|-|-
                TUESDAY|15:30|17:20|GZT 231|Türkiye’nin Toplumsal Yapısı (DERSLİK 22)|Araş. Gör. Dr. Mustafa AKBAYIR|-|-
                WEDNESDAY|09:30|12:20|GZT 209|Haber Toplama ve Yazma Teknikleri (GZT-HİT) (AMFİ 9)|Prof. Dr. Tülay BEKTAŞ/ Arş.Gör. E.Ahsen DEMİRCİOĞLU|-|-
                WEDNESDAY|13:30|15:20|TDB 115|Akademik Türkçe I (HİT-RTS-GZT-RLM) (AMFİ 1)|Ögr.Gör. Dürüye KARA|-|-
                WEDNESDAY|15:30|17:20|GZT 221|Uluslararası İlişkiler (GZT-RTS) (AMFİ 10)|Dr. Öğr. Üyesi Onur ÖKSÜZ|-|-
                THURSDAY|10:30|12:20|İLE 233|Sunum Teknikleri ( RTS-GZT-RKL) (AMFİ 7)|Öğr. Gör. Memduh TURHANOĞULLARI|-|-
                THURSDAY|13:30|15:20|GZT 229|/ HİT 227 Mesleki İngilizce I (HİT-RTS-GZT) (AMFİ 2)|Ögr.Gör. Esra Dönüş|-|-
                THURSDAY|15:30|17:20|GZT 215|Sosyal Psikoloji (AMFİ 9)|Doç. Dr. Emel AKSOY|-|-
                FRIDAY|13:30|15:20|İLE 207|Seçmeli Yabancı Dil Almanca- Rusça-Fransızca I (HİT-RKL-GZT-RTS) (AMFİ 1)|Öğr. Gör. Arzu AYDEMİR ÜMİT|-|-
                FRIDAY|15:30|17:20|GZT 233|Radyo yapım ve Yönetim (DERSLİK 20)|Öğr. Gör. Memduh TURHANOĞULLARI|-|-
            """.trimIndent(),
            """
                MONDAY|10:30|12:20|GZT 325|Medya Okuryazarlığı ( RTS-GZT-RKL) (AMFİ 8)|Doç. Dr. Sibel KARADUMAN|-|-
                MONDAY|13:30|15:20|GZT 341|İş Hayatı İçin İngilizce 1 (DERSLİK 21)|Öğr. Gör. Hülya ÇELİK|-|-
                MONDAY|15:30|17:20|GZT 301|Siyasal İletişim (DERSLİK 21)|Dr. Öğr. Ü. Onur ÖKSÜZ|-|-
                TUESDAY|08:30|09:20|GZT 395|İç Staj I (AKÜN HABER AJANSI)|Prof. Dr. Tülay BEKTAŞ|-|-
                TUESDAY|09:30|12:20|GZT 398|Sosyal Bilimlerde Nicel Araştırma Yöntemleri (AMFİ 9)|Doç. Dr. Tuba LİVBERBER|-|-
                TUESDAY|13:30|15:20|İLE 317|İletişim Etiği (RKL-GZT) (AMFİ 1)|Doç. Dr. Murad KARADUMAN|-|-
                TUESDAY|15:30|17:20|GZT 303|İletişimin Ekonomi Politiği (AMFİ 9)|Doç. Dr. Tuba LİVBERBER|-|-
                WEDNESDAY|08:30|09:20|GZT 395|İç Staj I (AKÜN HABER AJANSI)|Prof. Dr. Tülay BEKTAŞ|-|-
                WEDNESDAY|09:30|12:20|GZT 399|Yeni Medya Kuramları (AMFİ 10)|Prof.Dr. Emel ARIK|-|-
                WEDNESDAY|12:30|13:20|GZT 395|İç Staj I (AKÜN HABER AJANSI)|Prof. Dr. Tülay BEKTAŞ|-|-
                WEDNESDAY|13:30|15:20|GZT 397|Televizyon Haberciliği (AMFİ 10)|Ögr.Gör. Dr. Ayşen Yalman|-|-
                WEDNESDAY|15:30|17:20|GZT 321|Toplumsal Cinsiyet ve Medya (AMFİ 9)|Doç. Dr. Emel AKSOY|-|-
                THURSDAY|08:30|10:20|GZT 395|İç Staj I (AKÜN HABER AJANSI)|Prof. Dr. Tülay BEKTAŞ|-|-
                THURSDAY|10:30|12:20|HİT 311|Tüketici Davranışı (RTS-GZT-HİT) (AMFİ 6)/GZT 307 Basın Fotoğrafçılığı (HİT-GZT) (AMFİ 9)|Doç.Dr. Zuhal Gök DEMİR/ Dr. Öğr. Üyesi Hasan ÜSTÜN|-|-
                THURSDAY|13:30|16:20|GZT 301|Uygulamalı Gazetecilik I (AMFİ 10)|Prof.Dr. Mustafa ŞEKER|-|-
                THURSDAY|16:30|17:20|GZT 395|İç Staj I (AKÜN HABER AJANSI)|Prof. Dr. Tülay BEKTAŞ|-|-
                FRIDAY|10:30|12:20|İLE 305|Grafik ve Animasyon ( RTS-GZT-RKL ) (AMFİ 7)|Öğr. Gör. Zihni Durmuş|-|-
                FRIDAY|14:30|17:20|GZT 305|Gazete Tasarımı I (LAB 3)|Öğr. Gör. Dr. Emrah Onur KARATAŞ|-|-
            """.trimIndent(),
            """
                MONDAY|08:30|10:20|İLE 401|Bitirme Projesi I|Tüm Öğretim Üyeleri|-|-
                MONDAY|10:30|12:20|GZT 423|İnternet Gazeteciliği (AMFİ 10)|Doç. Dr. Murad KARADUMAN|-|-
                MONDAY|13:30|15:20|GZT 431|Dijital Okuryazarlık (DERSLİK 22)|Araş. Gör. Dr. Mustafa AKBAYIR|-|-
                TUESDAY|09:30|11:20|GZT 403|Veri Gazeteciliği (DERSLİK 21)|Ögr.Gör. Dr. Ayşen Yalman|-|-
                TUESDAY|13:30|16:20|GZT 427|Uzmanlaşmış Gazetecilik 1 (AMFİ 9)|Prof.Dr. Emel ARIK|-|-
                WEDNESDAY|10:30|12:20|İLE 407|Sivil Toplum ve Medya / GZT 407 Sivil Toplum ve Gazetecilik GZT/RTS (DERSLİK 22)|Dr. Öğr. Üyesi Onur ÖKSÜZ|-|-
                WEDNESDAY|13:30|15:20|İLE 409|Göstergebilimsel Çözümleme (HİT-RKL-GZT) (AMFİ 5)|Prof. Dr. Ayşad GÜDEKLİ|-|-
                THURSDAY|10:30|12:20|GZT 425|Haber Çözümlemeleri (DERSLİK 21)|Ögr. Gör. Ayşen YALMAN|-|-
                THURSDAY|13:30|15:20|GZT 429|Basında Çalışma Hayatı (AMFİ 9)|Dr. Öğr. Üyesi Hasan ÜSTÜN|-|-
                FRIDAY|08:30|12:20|GZT 421|Haber Kuram ve Uygulamaları (AMFİ 10)|Tüm Öğretim üyeleri|-|-
            """.trimIndent()
        ),
        ClassSchedules.PUBLIC_RELATIONS_DEPARTMENT to listOf(
            """
                MONDAY|10:30|12:20|HİT 105|Sanat Tarihi (DERSLİK 17)|Öğr. Gör. Dr. ŞAMİL YİRŞEN|-|-
                MONDAY|13:30|15:20|HİT 101|Halkla İlişkiler I (AMFİ 5)|Prof. Dr. Ahmet AYHAN|-|-
                TUESDAY|08:30|10:20|HİT 107|Çağdaş Edebiyat (HİT-RKL) (AMFİ 6)|Prof. Dr. Tülin ARSEVEN|-|-
                TUESDAY|10:30|12:20|İLE 101|İletişime Giriş (AMFİ 6)|Prof. Dr. Sibel HOŞTUT|-|-
                TUESDAY|13:30|15:20|İLE 109|Sosyoloji (HİT-RKL) (AMFİ 4)|Prof. Dr. Merih TAŞKAYA|-|-
                WEDNESDAY|08:30|10:20|HİT 103|İşletme Yönetimi- (HİT-RKL) (AMFİ 6)|Doç. Dr. Janset ÖZEN AYTEMUR|-|-
                WEDNESDAY|10:30|12:20|ENF 114|Bilgisayar Destekli Çizim ve Tasarım(LAB-01)/ENF116 Çoklu Ortam Uygulamaları(LAB-05)|Öğr. Gör. Ramazan UYAR/Öğr. Gör. Şahin AKBUNAR|-|-
                THURSDAY|10:30|12:20|YBD 101|İngilizce I (HİT-RKL) (AMFİ 4)|Öğr. Gör. Esra DÖNÜŞ|-|-
                THURSDAY|13:30|15:20|TDB 101|Türk Dili 1 (HİT-RKL) ÇOK AMAÇLI SALON (ZEMİN KAT)|Öğr. Gör. Dr. Bilal NARGÖZ|-|-
                THURSDAY|15:30|17:20|ATA 101-102|Atatürk İlkeleri ve İnkılap Tarihi (Yüz yüze) (HİT-RKL) (AMFİ 3)|Öğr. Gör. Murat BOZ|-|-
                FRIDAY|14:30|17:20|REK 109|Dijital İçerik Üretimi (DERSLİK 18)|Araş. Gör. Dr. Mehmet Emre GÜL|-|-
            """.trimIndent(),
            """
                MONDAY|10:30|12:20|İLE 201|İletişim Kuramları (HİT-RKL) (AMFİ 5)|Doç. Dr. Bahar URHAN|-|-
                MONDAY|13:30|15:20|HİT 231|Reklamcılık I (AMFİ 6)|Dr. Öğr. Üyesi Nurettin Mert BATU|-|-
                TUESDAY|08:30|13:20|HİT 209|Halkla İlişkiler Uygulamaları I (AMFİ 5)|Doç. Dr. Çiğdem KARAKAYA|-|-
                TUESDAY|13:30|15:20|İLE 233|Sunum Teknikleri (DERSLİK 17)|Arş. Gör. Dr. Ayşe HİMMETOĞLU|-|-
                TUESDAY|15:30|17:20|-|Görsel İletişimde Temel Tasarım (HİT-RKL)|İletişim Araştırma ve Uygulama Merkezi- B218 / Öğr. Gör. Dr. Emrah Onur KARATAŞ|-|-
                WEDNESDAY|09:30|11:20|HİT 225|Haber Toplama ve Yazma Teknikleri (HİT GZT) (AMFİ 9)|Prof. Dr. Narin Tülay BEKTAŞ|-|-
                WEDNESDAY|10:30|12:20|REK 229|İşitsel İçerik Üretimi (DERSLİK 17)|Arş. Gör. Dr. Mehmet Emre GÜL|-|-
                WEDNESDAY|13:30|15:20|HİT 205|Halkla İlişkiler Yazarlığı (AMFİ 6) / TDB115 Akademik Türkçe I AMFİ 1|Doç. Dr. Çiğdem KARAKAYA/ Öğr. Gör. Dürüye KARA|-|-
                WEDNESDAY|15:30|17:20|HİT 213|Halkla İlişkilerde Ortam ve Araçlar (DERSLİK 18)|Doç. Dr. Fulya ERENDAĞ SÜMER|-|-
                THURSDAY|09:30|12:20|HİT 243|Sosyal Bilimlerde Nicel Araştırma Yöntemleri (AMFİ 5)|Doç. Dr. Fulya ERENDAĞ SÜMER|-|-
                THURSDAY|13:30|15:20|HİT 227|Mesleki İngilizce I (HİT-GZT-RTS) (AMFİ 2)|Öğr. Gör. Esra DÖNÜŞ|-|-
                THURSDAY|15:30|17:20|-|Görsel İletişimde Temel Tasarım (HİT-RKL)|İletişim Araştırma ve Uygulama Merkezi- B218 / Öğr. Gör. Dr. Emrah Onur KARATAŞ|-|-
                FRIDAY|10:30|12:20|HİT 215|Sosyal Psikoloji (HİT-RKL-RTS) (AMFİ 2)|Prof. Dr. Seçil DEREN VAN HET HOF|-|-
                FRIDAY|13:30|15:20|İLE 207|Seçmeli Yabancı Dil Almanca- Rusça-Fransızca I (HİT-RKL-GZT-RTS) (AMFİ 1)/REK 221 Dijital Reklamcılık (DERSLİK 17)|Öğr. Gör. Arzu AYDEMİR ÜMİT/Doç. Dr. Yeşim ÇELİK|-|-
            """.trimIndent(),
            """
                MONDAY|08:30|10:20|HİT 317|Proje Yönetimi I (AMFİ 5)|-|-|-
                MONDAY|10:30|12:20|HİT 307|Halkla İlişkilerde Kriz Yönetimi (AMFİ 6)|Prof. Dr. Ahmet AYHAN|-|-
                MONDAY|13:30|16:20|TDP 301|Toplumsal Duyarlılık ve Katkı I (AMFİ 2)|Arş. Gör. Dr. Selda SARAL GÜNEŞ|-|-
                TUESDAY|10:30|12:20|HİT 323|Toplumsal Cinsiyet ve Medya (DERSLİK 18)|Prof. Dr. Ayşad GÜDEKLİ|-|-
                TUESDAY|13:30|16:20|HİT 319|Sosyal Sorumluluk Uygulamaları (DERSLİK 18)|Prof. Dr. Sibel HOŞTUT|-|-
                WEDNESDAY|08:30|10:20|HİT 321|İç Staj I (Atölyeler/AREKA)|Prof. Dr. Seçil Deren VAN HET HOF|-|-
                WEDNESDAY|10:30|12:20|İLE 301|Siyasal İletişim (HİT-RKL) AMFİ 5|Prof. Dr. Seçil DEREN VAN HET HOF|-|-
                WEDNESDAY|13:30|15:20|HİT 313|Kurum Kimliği (DERSLİK 17)|Doç. Dr. Zuhal GÖK DEMİR|-|-
                THURSDAY|08:30|10:20|REK 329|Marka Yönetimi (HİT-RKL) Çok Amaçlı Salon (Zemin Kat)|Doç. Dr. Hediye AYDOĞAN|-|-
                THURSDAY|10:30|12:20|HİT 311|Tüketici Davranışı (HİT-RTS-GZT) AMFİ 6/GZT 307 Basın Fotoğrafçılığı (AMFİ 9)|Doç. Dr. Zuhal GÖK DEMİR/Dr. Öğr. Üyesi Hasan ÜSTÜN|-|-
                THURSDAY|13:30|17:20|HİT 317|Proje Yönetimi I (AMFİ 5)|-|-|-
                FRIDAY|14:30|18:20|HİT 321|İç Staj I (Atölyeler/AREKA)|Prof. Dr. Seçil Deren VAN HET HOF|-|-
            """.trimIndent(),
            """
                MONDAY|10:30|12:20|REK 433|Yaratıcı Endüstriler (HİT-RKL) (AMFİ 4)|Doç. Dr. Bekir KİRİŞCAN|-|-
                MONDAY|13:30|15:20|HİT 421|Uluslararası Halkla İlişkiler (DERSLİK 18)|Prof. Dr. Sibel HOŞTUT|-|-
                TUESDAY|09:30|11:20|İLE 407|Sivil Toplum ve Medya (HİT-RKL) AMFİ 2|Prof. Dr. Seçil DEREN VAN HET HOF|-|-
                TUESDAY|13:30|15:20|HİT 459|İletişim Etiği (RKL-GZT-HİT) (AMFİ 1)|Doç. Dr. Murad KARADUMAN|-|-
                TUESDAY|15:30|17:20|HİT 455|Kamusal İletişim ve Protokol (AMFİ 6)|Prof. Dr. Ahmet AYHAN|-|-
                WEDNESDAY|10:30|12:20|HİT 457|Sağlık İletişimi (DERSLİK 18)|Doç. Dr. Yasemin BİLİŞLİ|-|-
                WEDNESDAY|13:30|15:20|İLE 409|Göstergebilimsel Çözümleme (HİT-RKL-GZT) AMFİ 5|Prof. Dr. Ayşad GÜDEKLİ|-|-
                THURSDAY|10:30|12:20|HİT 461|Türkiye'nin Toplumsal Yapısı (AMFİ 2)|Doç. Dr. Turan ŞENER|-|-
                THURSDAY|13:30|17:20|HİT 405|Halkla İlişkiler Araştırmaları I|-|-|-
                FRIDAY|08:30|10:20|HİT 449|Kitle İletişim Mevzuatı ve Etik (DERSLİK 17)|Prof. Dr. Seçil DEREN VAN HET HOF|-|-
                FRIDAY|12:30|14:20|HİT 401|Bitirme Projesi I|-|-|-
                FRIDAY|14:30|16:20|HİT 451|İnsan Kaynakları Yönetimi (AMFİ 5)|Doç Dr. Fulya ALMAZ|-|-
            """.trimIndent()
        ),
        ClassSchedules.RADIO_TV_CINEMA_DEPARTMENT to listOf(
            """
                MONDAY|10:30|12:20|İLE 101|İletişime Giriş (AMFİ 7 )|Prof. Dr. Bilal ARIK|-|-
                MONDAY|13:30|15:20|RTS 103|Radyo Televizyona Giriş (AMFİ 7)|Öğr. Gör. Zihni Durmuş|-|-
                MONDAY|15:30|17:20|İLE 105|Temel Hukuk ( GZT- RTS ) (AMFİ 1)|Öğr. Gör. Begüm Birsen ASLAN ÇELİK|-|-
                WEDNESDAY|08:30|10:20|İLE 103|Ofis Uygulamaları ( LAB 2 )|Öğr. Gör. Ramazan UYAR|-|-
                WEDNESDAY|10:30|12:20|RTS 107|Sosyoloji ( DERSLİK 19)|Doç. Dr. Fatih YILDIZ|-|-
                WEDNESDAY|12:30|15:20|ENF 125|Yapay Zeka Okuryazarlığı ( LAB 2 )|Öğr. Gör. Dr. Evren SEZGİN|-|-
                WEDNESDAY|15:30|16:20|ENF 114|Bilgisayar Destekli Çizim ve Tasarım(LAB-01)/ENF116 - Çoklu Ortam Uygulamaları(LAB-05)|-|-|-
                THURSDAY|10:30|12:20|TDB 101|Türk Dili ve Edebiyatı ( GZT-RTS ) (AMFİ 1)|Ögr.Gör. Bilal Nargöz|-|-
                THURSDAY|13:30|15:20|YBD 101|İngilizce I ( GZT-RTS ) (AMFİ 1)|Öğr. Gör. Hülya ÇELİK|-|-
                THURSDAY|15:30|17:20|ATA 101-102|Atatürk İlkeleri ve İnkılap Tarihi (yüz yüze) ( RTS-GZT ) (AMFİ 1)|Öğr. Gör. Dr. Koray ERGİN|-|-
            """.trimIndent(),
            """
                MONDAY|10:30|12:20|RTS 215|Metin Yazarlığı (Derslik 19)|Öğr. Gör. Dr. Rıdvan Yücel|-|-
                MONDAY|13:30|15:20|RTS 219|Kamera ve Işık (AMFİ 8)|Öğr. Gör. Dr. Gökhan Evecen|-|-
                MONDAY|15:30|17:20|RTS 213|TV Program Türleri (DERSLİK 20)|Öğr. Gör. Zihni Durmuş|-|-
                TUESDAY|10:30|12:20|RTS 227|Türkiye’nin Toplumsal Yapısı (AMFİ 8)|Doç. Dr. Mustafa Sami MENCET|-|-
                TUESDAY|13:30|15:20|İLE 201|İletişim Kuramları I (AMFİ 7 )|Prof. Dr. Levent Yaylagül|-|-
                TUESDAY|15:30|17:20|RTS 231|Uygulamalı Fotoğrafçılık ( AMFİ 8)|Prof. Dr. Tugay Arat|-|-
                WEDNESDAY|08:30|10:20|RTS 211|Görsel İletişim Tasarımı (LAB 3)|Öğr. Gör. Dr. Emrah Onur KARATAŞ|-|-
                WEDNESDAY|10:30|12:20|İLE 209|Haber Toplama ve Yazma Teknikleri (DERSLİK 20)|Öğr. Gör. Dr. Rıdvan Yücel|-|-
                WEDNESDAY|13:30|15:20|TDB 115|Akademik Türkçe I (HİT-RTS-GZT-RLM) AMFİ 1|Ögr.Gör. Dürüye KARA|-|-
                WEDNESDAY|15:30|17:20|RTS 229|Uluslararası İlişkiler ( GZT-RTS ) (AMFİ 9)|Dr. Öğr. Üyesi Onur ÖKSÜZ|-|-
                THURSDAY|10:30|12:20|RTS 233|Sunum Teknikleri ( RTS-GZT-RKL ) (AMFİ 7)|Öğr. Gör. Memduh Turhanoğulları|-|-
                THURSDAY|13:30|15:20|HİT 227|Mesleki İngilizce I ( HİT-GZT-RTS ) (AMFİ 2)|Öğr. Gör. Esra DÖNÜŞ|-|-
                THURSDAY|15:30|17:20|RTS 201|Senaryo Yazarlığı ( DERSLİK 20 )|Doç. Dr. Mustafa Sami MENCET|-|-
                FRIDAY|09:30|11:20|RTS 225|Sosyal Psikoloji ( HİT-RKL-RTS ) (AMFİ 2)|Prof. Dr. Seçil DEREN VAN HET HOF|-|-
                FRIDAY|13:30|15:20|İLE 207|Seçmeli Yabancı Dil Almanca- Rusça-Fransızca I ( HİT-RKL-GZT-RTS ) (AMFİ 1)|Öğr. Gör. Arzu AYDEMİR ÜMİT|-|-
            """.trimIndent(),
            """
                MONDAY|10:30|12:20|İLE 323|Medya Okuryazarlığı ( RTS-GZT-RKL) (AMFİ 8)|Doç. Dr. Sibel KARADUMAN|-|-
                MONDAY|13:30|15:20|İLE 311|İletişim Felsefesi (Derslik 19)|Prof. Dr. Nurdan AKINER|-|-
                MONDAY|15:30|17:20|RTS 307|Reklam ve Tanıtım Filmi Yapımı (AMFİ 8)|Öğr. Gör. Memduh Turhanoğulları|-|Tek Numaralı Öğrenciler
                TUESDAY|10:30|12:20|RTS 341|İletişim Etiği (AMFİ 7)|Prof. Dr. M. Bilal ARIK|-|-
                TUESDAY|13:30|15:20|RTS 303|Film Yapım Yönetim (AMFİ (8)|Öğr. Gör. Dr. Gökhan Evecen|-|-
                TUESDAY|15:30|17:20|İLE 301|Siyasal İletişim (DERSLİK 19 )|Öğr. Gör. Dr. Rıdvan Yücel|-|-
                WEDNESDAY|10:30|12:20|RTS 305|Dünya Sineması (AMFİ 8 )|Öğr. Gör. Dr. Gökhan Evecen|-|-
                WEDNESDAY|13:30|15:20|RTS 301|TV Yapım ve Yönetim ( AMFİ 7 )|Öğr. Gör. Zihni Durmuş|-|-
                WEDNESDAY|15:30|17:20|RTS 313|Basın Fotoğrafçılığı (AMFİ (7)|Prof. Dr. Tugay Arat|-|-
                THURSDAY|08:30|10:20|RTS 307|Reklam ve Tanıtım Filmi Yapımı (AMFİ 8)|Öğr. Gör. Memduh Turhanoğulları|-|Çift Numaralı Öğrenciler
                THURSDAY|10:30|12:20|HİT 311|Tüketici Davranışı ( RTS-GZT-HİT ) (AMFİ 6)|Doç.Dr. Zuhal Gök DEMİR|-|-
                THURSDAY|13:30|15:20|RTS 309|Sinema Türleri ve Akımlar ( AMFİ 8 )|Doç. Dr. Gül Yaşartürk|-|-
                THURSDAY|15:30|17:20|RTS 311|İnteraktif Medya Tasarımı ( LAB 3 )|Öğr. Gör. Dr. Evren SEZGİN|-|-
                FRIDAY|08:30|10:20|İLE 305|Grafik ve Animasyon (AMFİ 7) (RTS-GZT)|Öğr. Gör. Zihni Durmuş|-|Tek Numaralı Öğrenciler
                FRIDAY|10:30|12:20|İLE 305|Grafik ve Animasyon (AMFİ 7) (RTS-REK)|Öğr. Gör. Zihni Durmuş|-|Çift Numaralı Öğrenciler
                FRIDAY|13:30|15:20|RTS 339|Medyada Müzik Kullanımı ( DERSLİK 20 )|Prof. Dr Hasan Arapgirlioglu|-|-
            """.trimIndent(),
            """
                MONDAY|10:30|12:20|İLE 411|Göstergebilimsel Çözümleme (AMFİ 4)|Prof. Dr. Gülseren ŞENDUR ATABEK|-|-
                MONDAY|13:30|15:20|RTS 405|Medya Ekonomisi (Derslik 20)|Prof. Dr. Levent Yaylagül|-|-
                TUESDAY|10:30|12:20|RTS 413|Televizyon, Kültür Temsil (Derslik 19 )|Doç. Dr. Sibel KARADUMAN|-|-
                TUESDAY|12:30|13:20|RTS 401|Medya Uygulamaları|Bölüm Hocaları|-|-
                TUESDAY|13:30|15:20|RTS 407|Sanat ve Yaratıcılık ( DERSLİK 19 )|Prof. Dr. Günseli ORHON|-|-
                TUESDAY|15:30|17:20|RTS 417|Belgesel Sinema Uygulamaları ( DERSLİK 20)|Öğr. Gör. Dr. Gökhan Evecen|-|-
                WEDNESDAY|10:30|12:20|İLE 407|Sivil Toplum ve Medya / GZT 407 Sivil Toplum ve Gazetecilik GZT/RTS DERSLİK 22|Dr. Öğr. Üyesi Onur ÖKSÜZ|-|-
                WEDNESDAY|12:30|13:20|RTS 401|Medya Uygulamaları|Bölüm Hocaları|-|-
                THURSDAY|10:30|12:20|RTS 409|Popüler Sinema ( AMFİ 7 )|Prof. Dr. Gülseren ŞENDUR ATABEK|-|-
                THURSDAY|12:30|13:20|RTS 401|Medya Uygulamaları|Bölüm Hocaları|-|-
                FRIDAY|10:30|12:20|RTS 415|Dijital Medya Uygulamaları (DERSLİK 19 )|Arş. Gör. Dr. Mehmet Emre Gül|-|-
                FRIDAY|12:30|13:20|RTS 401|Medya Uygulamaları|Bölüm Hocaları|-|-
                FRIDAY|13:30|15:20|RTS 403|Sanat Tarihi (DERSLİK 20)|Öğr. Gör.Şamil YİRŞEN|-|-
            """.trimIndent()
        ),
        ClassSchedules.ADVERTISING_DEPARTMENT to listOf(
            """
                MONDAY|13:30|16:20|REK 109|Dijital İçerik Üretimi (RKL) (Derslik 25)|Araş. Gör. Dr. Mehmet Emre GÜL|-|-
                TUESDAY|08:30|10:20|REK 111|Çağdaş Edebiyat (HİT-RKL) (AMFİ 6)|Prof. Dr. Tülin ARSEVEN|-|-
                TUESDAY|10:30|12:20|İLE 101|İletişime Giriş (AMFİ 4)|Doç. Dr. Yeşim ÇELİK|-|-
                TUESDAY|13:30|15:20|REK 107|Sosyoloji (HİT-RKL) (AMFİ 4)|Prof. Dr. Merih TAŞKAYA|-|-
                WEDNESDAY|10:30|12:20|ENF 114|Bilgisayar Destekli Çizim ve Tasarım- Şube 1 (LAB-02) / ENF116 Çoklu Ortam Uygulamaları- Şube 1- (LAB-06)|Öğr.Gör. Ramazan UYAR / Öğr. Gör. Şahin AKBUNAR|-|-
                WEDNESDAY|13:30|15:20|REK 103|Reklama Giriş I (AMFİ 4)|Doç. Dr. Yeşim ÇELİK|-|-
                WEDNESDAY|15:30|17:20|REK 113|Sanat Okuryazarlığı (AMFİ 4)|Öğr. Gör. Ezgi BİLGİN|-|-
                THURSDAY|10:30|12:20|YBD 101|İngilizce I (HİT-RKL) (AMFİ 4)|Öğr. Gör. Esra DÖNÜŞ|-|-
                THURSDAY|13:30|15:20|TDB 101|Türk Dili 1 (HİT-RKL) Çok Amaçlı Salon (Zemin Kat)|Öğr. Gör. Dr. Bilal NARGÖZ|-|-
                THURSDAY|15:30|17:20|ATA 101-102|Atatürk İlkeleri ve İnkılap Tarihi (yüz yüze) (HİT-RKL) (AMFİ 3)|Öğr. Gör. Murat BOZ|-|-
                FRIDAY|10:30|12:20|REK 105|Temel Hukuk (AMFİ 4)|Dr. Öğr. Üyesi Özgür AYDIN|-|-
            """.trimIndent(),
            """
                MONDAY|10:30|12:20|İLE 201|İletişim Kuramları I (HİT-RKL) (AMFİ 5)|Doç. Dr. Bahar URHAN|-|-
                MONDAY|13:30|15:20|REK 221|Dijital Reklamcılık (RKL) (AMFİ 3)|Doç. Dr. Yeşim ÇELİK|-|-
                MONDAY|15:30|17:20|HİT 227|Halkla İlişkiler I (AMFİ 6)|Araş. Gör. Dr. Ayşe HİMMETOĞLU|-|-
                TUESDAY|10:30|12:20|REK 219|Sosyal Bilimlerde Nicel Araştırma Yöntemleri Çok Amaçlı Salon (Zemin Kat)|Dr. Öğr. Üyesi Fatma YARDİBİ|-|-
                TUESDAY|13:30|15:20|REK 223|Mesleki İngilizce I (AMFİ 3)|Doç. Dr. Hediye AYDOĞAN|-|-
                TUESDAY|15:30|17:20|REK 225|Görsel İletişimde Temel Tasarım (HİT-RKL) (Çok Amaçlı Salon (Zemin Kat)|Öğr. Gör. Dr. Emrah Onur KARATAŞ|-|-
                WEDNESDAY|08:30|10:20|HİT 103|İşletme Yönetimi- (HİT-RKL) (AMFİ 6)|Doç. Dr. Janset ÖZEN AYTEMUR|-|-
                WEDNESDAY|13:30|15:20|TDB 115|Akademik Türkçe I (HİT-RTS-GZT-RKL) (AMFİ 1)/ RTS 219 - Kamera ve Işık (Derslik 23)|Ögr.Gör. Dürüye KARA / Araş. Gör. Dr. Mehmet Emre GÜL|-|-
                WEDNESDAY|15:30|17:20|REK 229|İşitsel İçerik Üretimi (RKL) (Derslik 23)|Arş. Gör. Dr. Mehmet Emre GÜL|-|-
                THURSDAY|10:30|12:20|İLE 233|Sunum Teknikleri ( RTS-GZT-RKL) (AMFİ 7)|Öğr. Gör. Memduh Turhanoğulları|-|-
                THURSDAY|13:30|15:20|REK 201|Reklamda Yaratıcılık (AMFİ 4)|Prof. Dr. Merih TAŞKAYA|-|-
                THURSDAY|15:30|17:20|REK 225|Görsel İletişimde Temel Tasarım (HİT-RKL)|İletişim Araştırma ve Uygulama Merkezi- B218 / Öğr. Gör. Dr. Emrah Onur KARATAŞ|-|-
                FRIDAY|09:30|11:20|REK 227|Sosyal Psikoloji (HİT-RKL-RTS) (AMFİ 2)|Prof. Dr. Seçil DEREN VAN HET HOF|-|-
                FRIDAY|13:30|15:20|İLE 207|Seçmeli Yabancı Dil Almanca- Rusça-Fransızca I (HİT-RKL-GZT-RTS) (AMFİ 1)|Öğr. Gör. Arzu AYDEMİR ÜMİT|-|-
            """.trimIndent(),
            """
                MONDAY|10:30|12:20|İLE 323|Medya Okuryazarlığı ( RTS-GZT-RKL) (AMFİ 8)|Doç. Dr. Sibel KARADUMAN|-|-
                TUESDAY|09:30|12:20|REK 301|Reklam Yapım Aşamaları ve Uygulamaları I (AMFİ 3)|Doç. Dr. Hediye AYDOĞAN|-|-
                TUESDAY|13:30|16:20|TDP 301|Toplumsal Duyarlılık ve Katkı Projeleri I (Derslik 25)|Dr. Öğr. Üyesi Fatma YARDİBİ|-|-
                WEDNESDAY|08:30|10:20|REK 339|Toplumsal Cinsiyet ve Medya (Derslik 25)|Doç. Dr. Gülten ADALI|-|-
                WEDNESDAY|10:30|12:20|İLE 301|Siyasal İletişim (HİT-RKL) (AMFİ 5)|Prof. Dr. Seçil DEREN VAN HET HOF|-|-
                WEDNESDAY|13:30|15:20|REK 309|Reklam Fotoğrafçılığı (Derslik 25)|Tayfun DAYI (usta öğretici)|-|-
                THURSDAY|08:30|10:20|REK 329|Marka Yönetimi (HİT-RKL)- Çok Amaçlı Salon (Zemin Kat)|Doç. Dr. Hediye AYDOĞAN|-|-
                THURSDAY|10:30|12:20|REK 335|Reklamda Psikolojik Temeller (Derslik 25)|Doç. Dr. Gülten ADALI|-|-
                THURSDAY|13:30|15:20|REK 303|Reklam Yazarlığı (AMFİ 6)|Doç. Dr. Yeşim ÇELİK|-|-
                THURSDAY|15:30|17:20|REK 325|Dijital Reklam Uygulamaları (Derslik 23)|Öğr. Gör. Kürşat KARIŞMAZ|-|-
                FRIDAY|10:30|12:20|İLE 305|Grafik ve Animasyon ( RTS-GZT-RKL ) (AMFİ 7)|Öğr. Gör. Zihni DURMUŞ|-|-
                FRIDAY|14:30|17:20|REK 333|İç Staj I (Çok Amaçlı Salon (Zemin Kat)|Prof. Dr. Merih TAŞKAYA|-|-
            """.trimIndent(),
            """
                MONDAY|08:30|10:20|REK 429|Bitirme Projesi I (Öğr. Üyesi Odası)|Tüm Öğretim Üyeleri|-|-
                MONDAY|10:30|12:20|REK 433|Yaratıcı Endüstriler (HİT-RKL) (AMFİ 4)|Doç. Dr. Bekir KİRİŞCAN|-|-
                MONDAY|13:30|15:20|REK 405|Reklam Mevzuatı ve Etik (Derslik 25)|Öğr. Gör. Dr. Begüm Birsen ARSLAN ÇELİK|-|-
                MONDAY|15:30|17:20|REK 401|Reklam Araştırmaları I (Öğr. Üyesi Odası)|Tüm Öğretim Üyeleri|-|-
                TUESDAY|09:30|11:20|İLE 407|Sivil Toplum ve Medya (HİT-RKL) (AMFİ 2)|Prof. Dr. Seçil DEREN VAN HET HOF|-|-
                TUESDAY|13:30|15:20|İLE 315|İletişim Etiği-AMFİ 1 (RKL-GZT-)|Doç. Dr. Murad KARADUMAN|-|-
                WEDNESDAY|10:30|12:20|REK 417|Kültürel Çalışmalar (Derslik 23)|Doç. Dr. Gülten ADALI|-|-
                WEDNESDAY|13:30|15:20|İLE 409|Göstergebilimsel Çözümleme (HİT-RKL-GZT) (AMFİ 5)|Prof. Dr. Ayşad GÜDEKLİ|-|-
                THURSDAY|13:30|15:20|REK 413|Estetik (Derslik 25)|Öğr. Gör. Ezgi BİLGİN|-|-
                FRIDAY|08:30|10:20|REK 401|Reklam Araştırmaları I (Öğr. Üyesi Odası)|Tüm Öğretim Üyeleri|-|-
                FRIDAY|10:30|12:20|REK 407|Reklam Uygulamalarında Ölçme ve Değerlendirme (AMFİ 3)|Doç. Dr. Yeşim ÇELİK|-|-
            """.trimIndent()
        )
    )

    fun schedulesFor(department: String?): List<ClassSchedule>? {
        val sources = department?.let(schedulesByDepartment::get) ?: return null
        return schedules(department, sources)
    }
}

