# Sosyal Etkinlikler — devir notu (çalışma dosyası)

> Dal: `feature/social-activities`. 7 Ekim 2026 kullanıcı talimatıyla sosyal özellik kapsamı
> yerel bir geliştirme commit'ine alınmıştır; aşağıdaki beş inceleme bulgusu hâlâ açıktır.
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
- [x] **7 Ekim commit öncesi inceleme yapıldı.** Aşağıdaki beş bulgu raporlandı; mevcut testlerin geçmesi bu eksikleri kapsamıyordu. Kullanıcının sonraki açık “commit et” talimatıyla mevcut çalışma yerel geliştirme commit'ine alındı; bulgular düzeltilmiş değildir.
- [ ] **P1 — Fotoğraf erişimi:** sohbet/istek okumalarında güncel okul doğrulaması ve aynı üniversite koşulu uygulanmalı. Doğrulaması kaldırılan katılımcı/organizatör hâlâ diğer kişinin fotoğraf URL'sini alıyor (iki emülatör regresyon senaryosu).
- [ ] **P1 — Fotoğraf indirme bağlantısı:** sosyal fotoğraflar `marketDeps.photos.save` üzerinden kalıcı indirme token'ı ve `public,max-age=86400` ile kaydediliyor. Bağlantıyı bilen anonim istemci, Storage kurallarına rağmen dosyayı indirebiliyor. Yerel yapay dosyada token ile HTTP 200, tokensız HTTP 403 doğrulandı. Fotoğraf sunumunda her okumada yetki kontrolü sağlanmalı.
- [ ] **P1 — Özellik kapatma anahtarı:** `enabled=false` olduğunda akış/sohbet okuma çağrıları veri döndürmeye devam ediyor. Yazma doğrulamasındaki kapatma koşulu okuma yollarında da uygulanmalı.
- [ ] **P2 — Sohbet imleci:** yalnız `createdAt` ile `endBefore` kullanılması aynı milisaniyedeki yeni mesajı atlıyor. İmleç eşit zaman damgalarını kaybetmeyecek şekilde düzeltilmeli.
- [ ] **P2 — Moderasyon tekrarı:** `resolveSocialReportService` açık şikayet koşulunu kontrol etmiyor. Çözülmüş şikayete ikinci karar uygulanabiliyor ve önceki kararın üzerine yazılıyor; eski panel/yeniden deneme senaryosu korunmalı.
- [ ] Canlıya alma sırası (kullanıcı onayıyla): fonksiyonlar → indeksler → kurallar; özellik `app_config/social_activities.enabled=false` ile kapalı başlar.
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
