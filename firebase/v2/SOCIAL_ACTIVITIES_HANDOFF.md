# Sosyal Etkinlikler — devir notu (çalışma dosyası)

> Dal: `feature/social-activities`. 7 Ekim 2026 kullanıcı talimatıyla sosyal özellik kapsamı
> 7 Ekim güvenlik düzeltmeleri aşağıda kayıtlıdır; canlı yayın kanıtı son doğrulama bölümünde ayrıca belirtilir.
> (`daily-menu-ratings` e9d1965'ten açıldı). Çalışma ağacında bu özellikle ilgisi olmayan başka
> commit edilmemiş değişiklikler de var (web landing, `firebase.json`, Xcode dosyaları, `AppCheck.swift`
> vb.); commit'e **eklenmemeli**. Canlı projeye (`good4tr-v2`) hiçbir şey deploy edilmedi.

## Ne yapıyor

Yalnızca okul e-postasını doğrulamış öğrenciler için, topluluk etkinliklerinden (`events`) **tamamen ayrı**
bir özellik: öğrenci sosyal ya da spor etkinliği açar, aynı üniversiteden öğrenciler katılma isteği gönderir,
organizatör kabul edince **birebir** sohbet açılır.

## Alınan kararlar

- Doğrulama kapısı Kampüs Dolabı ile aynı (`hasVerifiedCampusEmail`: `users.eduVerified` + `@ogr.akdeniz.edu.tr`). Bir yerde doğrulayan diğerinde tekrar doğrulamaz.
- Engelleme listesi, 24 saatlik kapatma ve yasaklı içerik ihlalleri Kampüs Dolabı ile **ortak** (`marketUserState`, `marketViolations`); günlük sayaçlar ayrı (`socialUserState`).
- Mesajlaşma birebir (organizatör ↔ kabul edilen katılımcı). Katılımcılar birbirini görmez.
- Kontenjan 1–10. Dolunca bekleyen istekler `closed` olur. Reddedilen, çıkarılan ya da yer kalmayan öğrenci hep "Yer kalmadı" görür (ret bildirimi gitmez).
- Admin ön onayı yok. Yasaklı kelime filtresi, şikayet, 3 ihlalde 24 saat kapatma (market ile ortak).
- **Buluşma yeri alanı yok** (kullanıcı kararı): yer sohbette konuşulur.
- Sosyal 12, spor 15 tür (`SOCIAL_ACTIVITY_TYPES` sunucuda, `SocialCatalog.kt` uygulamada). 6 Ekim kullanıcı isteğiyle piknik ve gönüllülük kaldırıldı, masa oyunları sosyal listenin başına alındı. Masa oyunlarında isteğe bağlı oyun seçimi (`game`): okey, tavla, satranç, uno, tabu, kart oyunları, diğer; dijital oyunlarda isteğe bağlı FIFA/PES seçimi.
- İsim: varsayılan **maskeli** ("A.. Y.."). Öğrenci kural ekranında ya da sonradan "Sosyal profilin" penceresinden "Adımı göster" derse "Ayşe Y." görünür. İsim **okuma anında** çözülür (`socialProfile.ts`), bu yüzden ayar değişikliği mevcut etkinlik ve sohbetlere de yansır. Belgelerde saklanan ad alanları hep maskeli ve sadece yedektir.
- Profil fotoğrafı: aynı üniversitenin doğrulanmış öğrencilerine görünür. Sunucuda yeniden kodlanır (kare, EXIF silinir), günde en fazla 5 değişiklik.
- Giriş noktası: ana sayfada "Sosyal" kutucuğu (`HomeShortcut.SOCIAL_ACTIVITIES`, `ReleaseFeatures.socialActivities`). Varsayılan üçüncü sırada; Kampüs Dolabı hemen altında. Önceden kaydedilmiş kullanıcı sıralaması korunur.
- Özellik sunucuda `app_config/social_activities.enabled == true` olmadan **kapalı** (fail-closed).

## Dosya haritası

Sunucu (`firebase/v2`):
- `functions/src/social.ts` — tüm servisler, limitler (`SOCIAL_LIMITS`), saklama süreleri (`SOCIAL_RETENTION_DAYS`), günlük temizlik, hesap silme (`eraseSocialData`).
- `functions/src/socialProfile.ts` — isim çözümleme, fotoğraf doğrulama ve işleme.
- `functions/src/social.test.ts` — 28 senaryo.
- `functions/src/index.ts` — callable'lar (`getSocial*`, `createSocialActivity`, `setSocialProfile` …) ve `cleanupSocialActivities` (her gün 04:45).
- `functions/src/market.ts` — yalnızca birkaç yardımcı `export` edildi, davranış değişmedi.
- `functions/src/push.ts` — bildirim başlığı özelliğe göre ("Etkinlikler" / "Kampüs Dolabı"); `push.test.ts` güncellendi.
- `firestore.indexes.json`, `firestore.rules` (sadece yorum), `rules.test.mjs` (sosyal koleksiyonlar istemciye kapalı testi), `CANONICAL_DATA_MODEL.md` (sosyal bölüm).
- `scripts/seed-social-local.mjs` — yerel emülatör örnek verisi (aşağıya bak).

Web admin: `web/src/components/SocialActivitiesAdmin.tsx` (şikayet/ihlal listesi ve moderasyon eylemleri), `web/src/socialStrings.ts` (`strings.xml` içindeki sosyal metinleri okur), `web/src/App.tsx` (sekme bağlantısı).

Uygulama (`composeApp/src/commonMain/kotlin/com/good4/social/`):
- `SocialModels.kt`, `SocialRepository.kt` (callable çağrıları), `SocialViewModels.kt` (iş kuralları ve durum).
- Ekranlar: `SocialScreen.kt` (akış + Etkinliklerim), `SocialCreateScreen.kt` (2 adım), `SocialActivityScreen.kt` (detay), `SocialRequestsScreen.kt`, `SocialChatScreens.kt` (mesajlar + sohbet), `SocialProfileSheet.kt`.
- `SocialPoster.kt` — afiş desenleri ve okey taşları/zar varyantı (Compose Canvas; spor koyu yeşil/limon, sosyal krem).
- `SocialCatalog.kt`, `SocialPresentation.kt` (hata kodu → metin, tarih/yer etiketleri), `SocialComponents.kt`, `SocialPreviews.kt`.
- Bağlantılar: `Route.kt`, `NavGraph.kt`, `Modules.kt`, `HomeLayout.kt`, `HomeShortcutAppearance.kt`, `ProductListScreen.kt`, `StudentHomeScreen.kt`, `ReleaseFeatures.kt`, `Colors.kt`, `strings.xml` (`social_*`).
- Push: `CampusPushNotifications.kt` + iOS/Android bildirim işleyicilerinde `activityId` yedeği.
- `CampusEmailVerificationCard` ve `ReportDialog` (market) genelleştirildi (başlık/neden listesi parametreleri).

## Doğrulama komutları

```bash
# Sunucu testleri. DİKKAT: çalışan yerel emülatörün üstünde ÇALIŞTIRMA; testler users ve sosyal
# koleksiyonları siler. Önce emülatörü kapat.
cd firebase/v2 && npm --prefix functions run build \
  && npx firebase emulators:exec --project demo-good4-v2 --only firestore \
     'cd functions && node --test --test-concurrency=1 lib/social.test.js lib/push.test.js'
npm run test:rules      # kural testleri
npm run test:functions  # tüm fonksiyon testleri (bkz. bilinen sorun)

# Uygulama
./gradlew :composeApp:compileStagingDebugKotlinAndroid :composeApp:compileKotlinIosSimulatorArm64
./gradlew :composeApp:testStagingDebugUnitTest
```

Son durum (6 Ekim 2026): sosyal 28 + push 14 = **42/42**, kural testleri **23/23**, uygulama birim testleri **81/81**; Android ve iOS Kotlin, iOS debug framework, `iosApp Test` Xcode ve web derlemeleri başarılı. Ayrıntılı çıktı kanıtları aşağıda.
Bilinen sorun: `npm run test:functions` içinde iki eski eşzamanlılık testi (kupon kullanımı, topluluk takibi) bazen "Transaction is invalid or closed" ile düşüyor; tek başına geçiyorlar, bu özellikle ilgisi yok.

## Yerelde simülatörde denemek

1. `npm run emulators:local` (`firebase/v2`), ardından `npm run seed:local` ve `scripts/seed-social-local.mjs` (başlığındaki komutla).
2. iOS: `iosApp.xcworkspace`, şema `iosApp Test`, Debug. Emülatör modu yalnızca `com.good4.iosApp.test` paketinde açılır. Makinede `GoogleService-Info-Test.plist` yoksa derlemeye `FIREBASE_CONFIG_FILE=GoogleService-Info-Staging.plist` verilebilir (emülatör modunda bu dosya okunmuyor).
3. Otomatik giriş: uygulamayı `-good4DemoLogin denizdemo@akdeniz.edu.tr LocalTest123!` argümanlarıyla başlat (`AppCheck.swift`'te, commit edilmemiş değişiklik).

## Kalan işler

- [x] **Keşfet kartlarını kesintisiz iki sütuna yerleştir.** Gün başlıkları tek etkinlikli günlerde sağ sütunu boş bırakıyordu; tarih sırası korunarak günler arası satır devam ediyor ve tam gün/tarih kartın üzerinde gösteriliyor. 6 Ekim 2026: `Tümü` ve `Sosyal` filtreleri iPhone 17 Pro / iOS 26.5 simülatöründe açıldı; cuma dil pratiği ve cumartesi film kartının yan yana durduğu doğrulandı. Android ve iOS Kotlin derlemeleri, iOS debug framework bağlantısı ve `iosApp Test` Xcode derlemesi geçti. Sunucu davranışı değişmedi.
- [x] **Profil ekranını (isim anahtarı + fotoğraf) simülatörde görsel kontrol et.** yenidemo: varsayılan kapalı anahtar, “A.. Y..” / “Ayşe Y.” önizlemesi ve onaydan akışa geçiş doğrulandı. denizdemo: fotoğraf seç/değiştir/kaldır ve isim aç/kapat denendi. selif hesabında organizatör satırı, istekler/katılanlar ve sohbet başlığında “Deniz A.” + yeni fotoğraf, ardından “D.. A..” + baş harfler doğrulandı. Fotoğraf adresinin emülatöre yönlenmesi ve yalnız isim değiştirirken fotoğraf dosyalarının korunması düzeltildi; test eklendi.
- [x] Admin web paneline **Sosyal Etkinlikler** şikayet sekmesi eklendi. Kampüs Dolabı bileşenleri/stilleri kullanılıyor. Liste, şikayet edilmiş sohbet görüntüleme, yok say, etkinliği kaldır, 1–3650 gün askıya al ve ihlal listesi yerel panelde denendi; sonuçlar/audit kayıtları veritabanından da doğrulandı.
- [x] Profil fotoğrafı için admin müdahalesi: `removeSocialReportedProfilePhoto` + panel düğmesi + 4 test. Açık şikayetin hedef kullanıcısının 3 fotoğraf alanını ve ilgili Storage klasörünü siler, audit yazar. Yetkisiz çağrı, yanlış klasör, Storage hatasında tekrar deneme ve eşzamanlı yeni fotoğraf korunması test edildi. Gerçek yerel Storage klasörünün panel düğmesinden sonra boşaldığı doğrulandı.
- [x] **6 Ekim arayüz istekleri:** kartlar eşit 4:5 ölçüsünde, iki sütun günler arasında devam ediyor; ana sayfa sosyal kutusunda sayı ve alt etiket kaldırıldı. Masa oyunları ilk sırada, Okey kalın + zar/taş çizimi, dijital oyunlarda isteğe bağlı FIFA/PES (tekrar dokunma seçimi kaldırır). Piknik/gönüllülük kaldırıldı; sosyal vurgu sıcak turuncu. Başlık her zaman etiketli/kalemli düzenlenebilir alan. Tarih etiketli kart + takvim + bugün/yarın; saat 24 saat biçiminde sayısal giriş veya kadran. Oluşturma düğmesinin son kart başlığını örtmesi giderildi: düğme artık ayrı alt alanda. “30” girişinin “03” olması hatası düzeltildi; 19:30, geçersiz 90 dakika ve klavye açık yerleşim tekrar denendi.
- [x] **7 Ekim arayüz istekleri:** başlık “Sosyal”; varsayılan ana sayfa sırası Topluluklar → Ders Programı → Sosyal → Kampüs Dolabı. Futbol topu yerine iki kişi ve konuşma balonu vektör simgesi eklendi. Sohbetteki gri “Buluşmayı kalabalık...” kutusu kullanıcı isteğiyle kaldırıldı; mesaj listesi kaydırma indeksi buna göre düzeltildi. denizdemo ile iPhone 17 Pro / iOS 26.5 simülatöründe ana sayfa, Sosyal akış, mesaj listesi ve Okey sohbeti açılarak doğrulandı. Önceden kaydedilmiş özel sıralamalar korunur.
- [ ] Fotoğraf silme dayanıklılığı: eski/silinen fotoğraf dosyaları en iyi çaba ile siliniyor; başarısız olursa kalır (hesap silme tüm `social-profiles/{uid}/` klasörünü siler ve Storage hatasında yeniden denenir). İstenirse Kampüs Dolabı'ndaki gibi kalıcı silme kuyruğuna alınabilir.
- [ ] KVKK/gizlilik metnine sosyal etkinlik verisi (etkinlik, istek, sohbet, profil fotoğrafı, saklama süreleri) eklenmeli; metni kullanıcı onaylamalı.
- [x] **7 Ekim commit öncesi inceleme ve beş bulgunun düzeltmesi:** ilk incelemede bulunan beş açık sonraki düzeltmede kapatıldı. Geçmiş başarısız inceleme çıktıları tarihsel kanıt olarak korunur; güncel sonuçlar son bölümde yer alır.
- [x] **P1 — Fotoğraf erişimi:** bütün fotoğraflı yanıtlar görüntüleyen ve fotoğraf sahibi için güncel aktif öğrenci/okul doğrulaması/aynı üniversite kontrolü yapar. Doğrulaması kalkınca metin geçmişi korunur, fotoğraf gizlenir; admin moderasyonu açık istisnadır.
- [x] **P1 — Fotoğraf indirme bağlantısı:** sosyal fotoğraflar ayrı private/no-store Storage deposuna tokensız yazılır. Kimlik doğrulamalı callable yanıtı JPEG data URI döndürür; Compose byte dizisini, panel mevcut img öğesini kullanır. Eski ve yetim nesne sürümleri için `scripts/privatize-social-photos.mjs` eklendi. Yerelde anonim yeni dosya HTTP 403, eski token 200→403 doğrulandı.
- [x] **P1 — Özellik kapatma anahtarı:** tüm öğrenci okuma ve yönetim/yazma yolları yalnız literal true kabul eder. Eksik/false/metin/sayı değerleri reddedilir; summary kapalı durum ve sıfır sayaç döndürür. Admin işlemleri kapalıyken çalışır.
- [x] **P2 — Sohbet imleci:** bütün metin ve sistem mesajları işlem içinde artan sequence alır. İstemci s: imlecini tutar ve 50 mesajı aşan sayfaları sırayla tüketir. Eski timestamp istemcileri için zaman damgaları kesin artar ve sayfalar eskiden yeniye verilir; numarasız eski geçmiş korunur.
- [x] **P2 — Moderasyon tekrarı:** açık şikayet kontrolü karar/audit ile aynı transaction içindedir. İkinci/eşzamanlı karar SOCIAL_REPORT_RESOLVED ile reddedilir; yeni bir şikayet olarak yeniden açılan rapor çözülebilir.
- [x] Canlıya alma: sonraki açık deploy izniyle fonksiyonlar → indeksler → kurallar ve admin paneli yayımlandı; özellik kapalı, demo verisi aktarılmadı. Sonuçlar son bölümde.
- [ ] Android'de simülatör/emülatörde ekranlar denenmedi (yalnızca derleme).
- [ ] Badminton ve bazı türlerin ikonları Material setindeki en yakın ikonlar (örn. badminton için raket); özel ikon istenirse değiştirilebilir.

## 6 Ekim 2026 doğrulama kanıtları

Çalışma dalı `feature/social-activities`. Commit/push/deploy yapılmadı; fiziksel telefona kurulum yapılmadı. Yalnız `demo-good4-v2` ve `iosApp Test` (`com.good4.iosApp.test`) kullanıldı. Test öncesi çalışan emülatör SIGINT ile durduruldu, 8285/9199/5105/9295/4105 portlarının boş olduğu kontrol edildi. Testler ayrı `emulators:exec` altında çalıştı; ardından `npm run emulators:local`, `npm run seed:local` ve `scripts/seed-social-local.mjs` ile veriler yeniden yüklendi. Panel denemelerinden sonra sosyal örnekler yeniden yüklendi; demo hesabının deneme askısı kaldırıldı.

Gerçek çıktı özetleri:

```text
Sunucu (social.test + push.test, emulators:exec --project demo-good4-v2 --only firestore):
  tests 42 / pass 42 / fail 0 / cancelled 0 / skipped 0 / todo 0
  Script exited successfully (code 0)
Kural (npm run test:rules):
  tests 23 / pass 23 / fail 0 / cancelled 0 / skipped 0 / todo 0
  Script exited successfully (code 0)
Gradle (:composeApp:compileStagingDebugKotlinAndroid :composeApp:compileKotlinIosSimulatorArm64
        :composeApp:testStagingDebugUnitTest :composeApp:linkDebugFrameworkIosSimulatorArm64):
  BUILD SUCCESSFUL in 1m 25s
  35 actionable tasks: 7 executed, 28 up-to-date
  JUnit XML toplamı: tests 81 / failures 0 / errors 0 / skipped 0
Xcode (iosApp Test, Debug, iPhone 17 Pro / iOS 26.5):
  ** BUILD SUCCEEDED **
Web (npm run test:web):
  tsc -b && vite build --config vite.config.ts
  62 modules transformed / built in 144ms / exit 0
```

Tam çıktılar: `/tmp/good4-social-final-server-tests.log`, `/tmp/good4-social-final-rules.log`, `/tmp/good4-social-final-gradle.log`, `/tmp/good4-social-final-xcode.log`, `/tmp/good4-social-final-web.log`. Yerel panel fotoğraf kaldırma doğrulaması: üç Firestore alanı silindi, gerçek Storage klasörü boş, `social.profile.photoRemoved` ve `social.conversation.viewed` audit kayıtları mevcut. Etkinlik kaldırma/askıya alma/yok sayma için durumlar ve audit kayıtları ayrıca kontrol edildi. Panel ekran kanıtı `/tmp/good4-social-admin-verified.png`; son simülatör akışı `/tmp/good4-social-simulator-verified.png`. Alt alana taşınan oluşturma düğmesiyle liste sona kaydırıldı; dil pratiği/film başlıkları ve kontenjan satırlarının tamamen göründüğü tekrar doğrulandı.

Açılıp kontrol edilen ekranlar: ana sayfa, Keşfet (Tümü/Sosyal), oluşturma tür listesi, masa oyunu/Okey detay formu, dijital oyun/FIFA/PES detay formu, başlık metin alanı (klavyeli), tarih takvimi, saat sayısal giriş (klavyeli) ve kadran, yenidemo ilk kullanım kuralları, denizdemo profil penceresi ve iOS galeri, diğer öğrenci için etkinlik detayı, istekler/katılanlar, sohbet (klavyeli), admin sosyal sekmesi. Durum çubuğu/ana ekran çubuğu ve formlarda taşma görülmedi. Yeni görünen sosyal metinler `strings.xml` / `social_*`; web paneli aynı kaynakları `socialStrings.ts` üzerinden okur. Önceden paylaşılan fotoğraf seçicide sosyal metinler için opsiyonel etiketler eklendi; diğer çağrılar mevcut varsayılanları korur. Seçici hatasının yanlışlıkla temizlenmesi de giderildi.

Bu çalışmada düzenlenen dosyalar (diğer önceden mevcut değişiklikler korunmuştur):

- `composeApp/src/commonMain/composeResources/values/strings.xml`
- `composeApp/src/commonMain/kotlin/com/good4/student/presentation/home/HomeShortcutAppearance.kt`
- `composeApp/src/commonMain/kotlin/com/good4/product/presentation/product_list/views/ProductListScreen.kt`
- `composeApp/src/commonMain/kotlin/com/good4/social/{SocialActivityScreen,SocialCatalog,SocialChatScreens,SocialComponents,SocialCreateScreen,SocialPoster,SocialPresentation,SocialProfileSheet,SocialRequestsScreen,SocialScreen,SocialViewModels}.kt`
- `composeApp/src/{commonMain,iosMain,androidMain}/kotlin/com/good4/core/presentation/components/ProductImagePicker.kt`
- `firebase/v2/functions/src/{index,social,socialProfile,social.test}.ts`
- `firebase/v2/web/src/App.tsx`
- `firebase/v2/web/src/components/SocialActivitiesAdmin.tsx`
- `firebase/v2/web/src/socialStrings.ts`
- `firebase/v2/scripts/seed-social-local.mjs`
- `firebase/v2/CANONICAL_DATA_MODEL.md`
- `firebase/v2/SOCIAL_ACTIVITIES_HANDOFF.md`

Tam çalışma ağacı `git status --short` çıktısı: `/tmp/good4-social-final-status.txt`. Listede bu işten önce var olan sosyal ve ilgisiz kullanıcı değişiklikleri de vardır; tümü bu çalışmada üretilmiş değildir.

## 7 Ekim 2026 — son arayüz ve commit öncesi inceleme kanıtları

İnceleme sırasında commit/push/deploy yapılmadı. Mevcut sosyal+push testleri geçiyor, fakat incelemede eklenen beş geçici regresyon senaryosu beklenen davranışın sağlanmadığını gösterdi. Geçici inceleme dosyası `/tmp/good4-social-precommit-repro.mjs`; çalışma ağacına eklenmedi ve yeni sunucu davranışı uygulanmadı. Düzeltmeler yapılırken kalıcı testler `social.test.ts` içine eklenmeli. Beş bulgu **Kalan işler** bölümünde açık olarak listelendi; commit için hazır olduğu söylenemez.

Testlerden önce çalışan yerel emülatör kapatıldı ve portların boş olduğu kontrol edildi. Sunucu ve kural testleri ayrı `emulators:exec --project demo-good4-v2 --only firestore` süreçlerinde çalıştırıldı. Ardından yerel emülatör yeniden açıldı, `seed:local` ve `seed-social-local.mjs` tekrar çalıştırıldı. Fotoğraf token kontrolü, yalnız yerel Storage emülatöründe geçici yapay bir dosyayla yapıldı; dosya ardından silindi.

```text
Sosyal + push + inceleme regresyonları:
  tests 47 / pass 42 / fail 5 / cancelled 0 / skipped 0 / todo 0
  Mevcut social.test (28) + push.test (14): 42/42 geçti.
  Ek inceleme senaryoları: 0/5 geçti (iki fotoğraf erişimi, kapatma anahtarı,
    eşit zaman damgalı mesaj, çözümlenmiş şikayet tekrarı).
Yerel fotoğraf indirme kontrolü:
  Girişsiz, indirme token'ı ile HTTP 200; tokensız HTTP 403.
Kural:
  tests 23 / pass 23 / fail 0 / cancelled 0 / skipped 0 / todo 0
  Script exited successfully (code 0)
Gradle (Android + iOS Kotlin + birim testleri + iOS debug framework):
  BUILD SUCCESSFUL in 1m 19s
  35 actionable tasks: 7 executed, 28 up-to-date
  JUnit XML toplamı: tests 81 / failures 0 / errors 0 / skipped 0
Xcode (iosApp Test / Debug):
  Embed framework Gradle adımı: BUILD SUCCESSFUL in 9m 57s
  27 actionable tasks: 14 executed, 13 up-to-date
  ** BUILD SUCCEEDED **
Web:
  62 modules transformed / built in 317ms / exit 0
git diff --check: temiz.
```

Tam çıktılar: `/tmp/good4-social-review-{server-tests,rules,gradle,xcode,web,seed,emulators}.log`. Xcode kaynak hazırlığı, üretilmiş yinelenmiş kaynak klasörlerinin taranması nedeniyle uzun sürdü; derleme sonunda başarılı oldu. Xcode proje dosyası ve AppCheck.swift değiştirilmedi. Yerel emülatör sıfırlandıktan sonra test uygulamasının eski oturumu yenilendi; kaynak değişikliği gerekmedi.

Simülatör kanıtları: `/tmp/good4-social-home-review.png` (Sosyal üçüncü sırada, yeni simge; Kampüs Dolabı dördüncü), `/tmp/good4-social-chat-review.png` (gri güvenlik kutusu kaldırılmış). Uygulama ana sayfada bırakıldı.

Bu son turda değişen kaynak dosyaları:
- `composeApp/src/commonMain/composeResources/values/strings.xml`
- `composeApp/src/commonMain/kotlin/com/good4/student/home/HomeLayout.kt`
- `composeApp/src/commonMain/kotlin/com/good4/student/presentation/home/HomeShortcutAppearance.kt`
- `composeApp/src/commonMain/kotlin/com/good4/social/SocialChatScreens.kt`
- `composeApp/src/commonTest/kotlin/com/good4/student/home/HomeLayoutTest.kt`
- `firebase/v2/SOCIAL_ACTIVITIES_HANDOFF.md`

Tam çalışma ağacı durum çıktısı: `/tmp/good4-social-review-status.txt` (önceden mevcut değişiklikler dahil).

## 7 Ekim 2026 — Neslişah deposuna teslim

Teslim kopyası `neslisahcelek/good4` deposunun güncel `main` commit'i `b93b176` üzerine yalnızca sosyal commit'i `d53f0ae` taşınarak hazırlandı. Metin kaynakları, tema renkleri, veri modeli ve indeks eklemeleri main'deki mevcut içerikle birlikte korundu. Okul e-postası doğrulama kartında main'in iki adımlı tasarımı korundu; sosyal ekranın opsiyonel metin parametreleri eklendi. Üç eski dal commit'i ve mağaza görselleri teslim değişikliğine dahil edilmedi. Asıl çalışma klasöründeki commit edilmemiş dosyalar değiştirilmedi.

Bu teslim kopyasında Android ve iOS Kotlin derlemeleri ile uygulama testleri geçti: tests 83 / failures 0 / errors 0 / skipped 0. Gradle: BUILD SUCCESSFUL in 1m 11s, 34 actionable tasks: 34 executed. Sunucu TypeScript ve web derlemeleri de geçti. Önceki emülatör test sonuçları yukarıdadır; bu turda çalışan emülatör üzerinde sunucu testleri çalıştırılmadı. Beş açık inceleme bulgusu korunuyor; GitHub teslimi taslak PR olarak hazırlanmıştır. Canlı projeye deploy veya veri aktarımı yapılmadı; demo örnekleri yalnızca yerel emülatör içindir.

## 7 Ekim 2026 — güvenlik düzeltmeleri ve tekrar doğrulama

Düzeltme, güncel main üzerine hazırlanan teslim commit'i `34e0fc8` üzerinden, kalıcı `social-security-fixes` çalışma ağacında geliştirildi. Ana çalışma ağacındaki sosyal dışı değişiklikler korunur. Güvenlik sınırı için bağımsız salt-okunur inceleme ve bir aday yama incelemesi yapıldı; ikinci incelemenin timestamp uyumluluğu bulgusu giderildi.

- Sosyal 37 + push 14: **51 test / 51 geçti / 0 hata / 0 atlandı**. Çalışan emülatör yokken `firebase emulators:exec --project demo-good4-v2 --only firestore` kullanıldı.
- `npm run test:rules`: **23 / 23 geçti, 0 hata**; ayrı emulators:exec oturumu.
- Android/iOS Kotlin derlemeleri + `testStagingDebugUnitTest`: **83 / 83 geçti, 0 failure/error/skipped**. Debug iOS framework dahil Gradle BUILD SUCCESSFUL, 1m 57s, 35 görev.
- Production modunda `npm run test:web`: başarılı.
- Sonrasında yalnız demo emülatörler açıldı ve base/social seed yeniden yüklendi. Kimlik doğrulamalı profil yükleme/summary JPEG yanıtı, yeni tokensız dosyada anonim HTTP 403, eski yapay token'ın 200→403 geçişi ve tekrar envanterde sıfır değişiklik doğrulandı. Firebase Storage emülatörü null token metadata güncellemesinde ayrı token listesini koruduğundan yalnız demo bucket'ta aynı byte'ların private yeniden yazılması kullanılır; canlıda metadata iptali uygulanır.
- Canlı salt-okunur hazırlık: Sosyal enabled=false, mevcut Social fonksiyonu ve `social-profiles/` nesnesi yok. Yayın öncesi aynı envanter/migration tekrar doğrulanmalı; daha önce indirilmiş byte veya önceden public önbelleğe alınmış yanıtlar uzaktan geri alınamaz.

Kalıcı tam çıktılar Codex Security artifact koleksiyonunda: `artifacts/verification/server.log`, `rules.log`, `gradle.log`, `web.log`, `photo-proof.log`. Simülatör ve deploy sonucu aşağıya tamamlandıktan sonra yazılır.

Simülatör tekrar kontrolü: iPhone 17 Pro / iOS 26.5, iosApp Test, demo emülatörü. Deniz profil penceresindeki private JPEG fotoğrafı ve Elif hesabındaki mesaj listesi/Okey sohbet başlığındaki aynı fotoğraf görsel olarak doğrulandı. Maskeli ad korunuyor; kaldırılmış gri uyarı dönmedi. Xcode BUILD SUCCEEDED. Canlı migration uygulama ve tekrar envanter sonucu: enabled=false, objects=0, changed=0, remainingChanges=0. Önceki yayın izni geçerlidir; yayın sırasında demo verisi aktarılmayacak.

### Canlı yayın ve teslim sonucu — 7 Ekim 2026

Kullanıcının sonraki açık commit/push/deploy talimatlarıyla düzeltme teslim edildi. Uzak düzeltme commit'i `f529626`, yerel geliştirme dalına taşınan eşdeğeri `8f8b9eb`. Neslişah deposundaki PR #10 güncellendi; main birleştirilmedi.

`good4tr-v2` yayını: 25 Sosyal fonksiyonu oluşturuldu, `deleteMyAccount` güncellendi (26 başarılı işlem). İndeksler, Firestore ve Storage kuralları yayımlandı; ilgili 12 indeks READY. Mevcut bildirim ayarı korundu. Admin web sürümü `9a59a86233a273ea`: önceki 30 dosyanın içeriği/hash'i ve bütün Hosting ayarları korundu; HTML, yalnız admin adreslerinde yeni modülü yükleyen küçük bir yönlendiriciye geçti. Genel site eski modülü kullanır. Yerel landing değişiklikleri yayına alınmadı.

Son canlı kontrol: Social enabled=false; socialActivities/socialConversations/socialUserState/socialReports her biri 0 kayıt; denizdemo/yenidemo/selif canlıda yok; kimliksiz feed HTTP 401 UNAUTHENTICATED. Fotoğraf migration son envanteri 0 nesne/0 değişiklik. Panel HTTPS 200 ve giriş ekranı açıldı; yeni modülde sosyal şikayet/fotoğraf kaldırma çağrıları doğrulandı. Canlı admin hesabıyla oturum açılmadı. Yerel admin panelinde private JPEG fotoğrafı img öğesinde görsel olarak tekrar doğrulandı.

Değişen kaynaklar: SocialComponents.kt, SocialModels.kt, SocialViewModels.kt, index.ts, social.ts, socialProfile.ts, social.test.ts, privatize-social-photos.mjs, CANONICAL_DATA_MODEL.md ve bu HANDOFF. Ana çalışma ağacındaki sosyal dışı 9 değiştirilmiş dosyanın SHA256 değerleri teslim öncesiyle aynı.

Tam yayın çıktıları kalıcı artifact koleksiyonunda `artifacts/verification/deploy-functions.log`, `deploy-indexes.log`, `deploy-rules.log`, `deploy-hosting.log`, `live-verification.log`, `live-migration.log`. Görsel kanıt: `profile.png`, `chat.png`, `admin-photo.png`, `live-panel.png`.
