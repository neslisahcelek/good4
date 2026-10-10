---
name: good4-pre-release
description: Good4 Android/iOS uygulamasının yeni sürümü çıkmadan önce sürüm, production yapılandırması, derleme, test, güvenlik, Firebase hazırlığı ve mağaza kontrollerini yap; kanıtlı yayın hazırlık raporu üret. Genel sürüm öncesi denetim taleplerinde kullan; store review öncesi taleplerde yalnız değişen kaynak kodunun inceleme risklerini kontrol et.
---

# Good4 sürüm öncesi kontrol

Amaç, yayın adayını gerçekten denetlemek ve eksikleri somut kanıtla göstermektir. Kontrol talebi geldiğinde erişilebilir kontrolleri çalıştır; yalnız bir kontrol listesi sunmakla yetinme. Skill ekleme veya düzenleme talebinde uygulamanın yayın kontrollerini başlatma.

## Kapsam ve çalışma biçimi

- Repo kökündeki `AGENTS.md`, [good4-code-review](../good4-code-review/SKILL.md) ve `docs/REPEATED_REVIEW_PATTERNS.md` oku. Firebase davranışını incelerken [good4-firebase-guardrails](../good4-firebase-guardrails/SKILL.md) uygula. Görsel akış değişmişse [good4-media](../good4-media/SKILL.md) kullan.
- `git status --short`, HEAD, staged/unstaged diff ve varsa son yayın referansına göre diff ile aday kapsamını belirle. Son yayın ref'ini isimden tahmin etme; mevcut tag/mağaza kaydı/kullanıcı bilgisiyle çöz. Bilinmiyorsa diff kapsamındaki belirsizliği yaz ve bağımsız kontrolleri sürdür.
- Kullanıcı tek platform belirtmediyse Android ve iOS'u kapsa. Raporun başına platform, ortam, Android versionName/versionCode, iOS MARKETING_VERSION/CURRENT_PROJECT_VERSION, commit ve çalışma ağacı değişikliklerini kaydet.
- Kullanıcının değişikliklerini koru. Bu skill denetim içindir; sürüm artırma, kaynak düzeltme, commit/push, deploy, mağazaya yükleme, canlı config/enforcement değişikliği veya gerçek kullanıcı verisi üreten test için mevcut kullanıcı yetkisini esas al. Denetim talebini bu işlemlere izin sayma. Düzeltme ayrıca istenmişse ilgili kontrolleri düzeltmeden sonra yenile.
- Mevcut kaynak/task/script adlarını çalıştırmadan önce doğrula. Bağımsız okumaları grupla; ortak Gradle, Xcode veya Firebase emulator kaynaklarını kullanan işleri çakıştırma. Logları ignored `output/` veya geçici dizinde tut; token, parola, özel kullanıcı verisi ve signing secret'larını rapora dökme.

## Store review öncesi: dar kapsam

Bu modu diğer kontrollerden önce seç. Kullanıcı “store review öncesi”, “App Store'a göndermeden önce” veya “Google Play incelemesi öncesi” kontrol isterse yalnız [kaynak kodu inceleme rehberini](references/store-review.md) uygula ve o rehberin kısa raporuyla bitir.

- Bu modda aşağıdaki sürüm/ortam, derleme/paket, backend, cihaz, mağaza bilgileri ve kapsamlı yayın kararı bölümlerini çalıştırma. Build, archive, imza, test komutu, gizlilik politikası/beyan denetimi, canlı Firebase sorgusu, mağaza Console erişimi veya dış URL kontrolü yapma.
- Başlangıç okumalarını repo talimatları, hedef diff, ilgili kaynaklar ve `good4-code-review` ile sınırla. Diğer skill'leri yalnız değişen kodu anlamak için gerektiğinde yükle; genel yayın denetimini başlatma.
- Kullanıcı ayrıca build, test veya daha geniş yayın denetimi isterse yalnız o ek kapsamı uygula. Store review talebini bu ek işlere yetki sayma.

Aşağıdaki bölümler genel “sürüm öncesi / yayına hazır mı” denetimleri içindir.

## Sürüm ve ortam

- `composeApp/build.gradle.kts`, `iosApp/iosApp.xcodeproj/project.pbxproj`, shared schemes, `firebase/v2/mobile/README.md` ve `firebase/v2/.firebaserc` üzerinden güncel eşleşmeleri doğrula. Mevcut prod: Android `prodRelease` / `com.good4`, iOS `iosApp Prod` / `Release` / `com.good4.iosApp`, Firebase `good4tr-v2` (`654697131931`). Staging: `good4tr-test` (`449563145023`). Eski `good4tr` (`737367442886`) veya `.v2` uygulama kayıtları prod adaya karışmamalı.
- Android versionCode ve iOS build numarasını ilgili mağazanın son yüklenen numarasıyla karşılaştır; platform numaralarının birbirine eşit olmasını şart koşma. Görünen sürümün planlanan sürümle tutarlılığını kontrol et. Mağaza kaydına erişilemiyorsa numara artışını doğrulanmamış işaretle; Git geçmişi tek başına mağaza kanıtı değildir.
- Prod adayında emulator bağlantısı, debug mock, `SIMULATE_APP_UPDATE`, debug App Check provider/token veya staging endpoint bulunmamalı. `prodDebug` gerçek production'a bağlanır; test hesabı olmadan veri değiştiren smoke test yapma. Staging debug varsayılan olarak emülatöre bağlanabilir; bunu bulut staging testi sanma.
- Android `composeApp/src/prod/google-services.json` ve varsa kök fallback'in project/package eşleşmesini doğrula. Google Services plugin'in seçtiği matching client'ta `client_type: 3` Web OAuth ID ve üretilen `default_web_client_id` bulunmalı. ID'yi uydurma veya eski V1 client'ını kullanma.
- `composeApp/src/main/res/raw/keep.xml` içindeki `default_web_client_id` koruması ile `composeApp/proguard-rules.pro` Credential Manager / googleid keep kurallarını denetle. Debug Google girişinin çalışması küçültülmüş release'in çalıştığını kanıtlamaz.
- iOS `GoogleService-Info-Prod.plist`, build phase'in pakete kopyaladığı plist, bundle ID ve URL scheme/reversed client ID eşleşmesini kontrol et. İzin açıklamaları, entitlements, Push ve App Attest capability/provisioning uyumunu doğrula.

## Derleme, test ve yayın paketi

Repo kökünden, güncel task'lar mevcutsa Android için:

```sh
./gradlew :composeApp:compileKotlinMetadata :composeApp:compileKotlinIosSimulatorArm64 :composeApp:testProdDebugUnitTest
./gradlew :composeApp:lintProdRelease :composeApp:bundleProdRelease
./gradlew :composeApp:signingReport
```

- Tek platform kapsamında diğer platform task'larını çıkar. Unit test/lint sonuçları, test sayıları ve exit code'u incele; yalnız komutun başladığını görmek başarı değildir. Lint baseline veya skip edilen testler yeni hataları saklıyor mu değerlendir.
- Üretilen AAB'nin güncel adaydan geldiğini, package/version değerlerini ve upload sertifikasını mevcut araçlarla doğrula. `keystore.properties` varlığı veya başarılı bundle task'ı imzalı mağaza paketi kanıtı değildir. Mevcut `composeApp/prod/release/*.aab` dosyalarını güncel çıktı sayma.
- Upload/debug/Play App Signing SHA parmak izlerini `AGENTS.md` ile ve erişilebilen Firebase kayıtlarıyla karşılaştır. Play dağıtımındaki Google girişini yerel upload imzasıyla karıştırma. Parolaları komut argümanlarına/loglara yazma.
- iOS için mevcut `iosApp Prod` scheme'inde Release device derlemesini çalıştır. Uygun başlangıç komutu:

```sh
xcodebuild -project iosApp/iosApp.xcodeproj -scheme 'iosApp Prod' -configuration Release -destination 'generic/platform=iOS' -derivedDataPath /private/tmp/good4-pre-release-derived CODE_SIGNING_ALLOWED=NO build
```

- İmzasız build yalnız derleme kontrolüdür. Mevcut signing erişimiyle mümkünse ayrı bir geçici çıktı yoluna imzalı Release archive üret; archive'ın bundle/version, embedded provisioning, entitlements ve Firebase plist değerlerini kontrol et. İmzasız build, simulator derlemesi veya Kotlin framework başarısından App Store archive/export başarısı çıkarma. Signing yapılandırmasını kontrolü geçirmek için değiştirme.
- Bağımlılık değişiklikleri, iOS native symbols/dSYM ve gerekiyorsa `docs/ios-maplibre-symbols.md` içindeki adımları incele. Yeni/güncel mağaza platform gerekliliklerini kontrol ederken resmi Apple/Google kaynaklarını kullan; değişebilen gereklilikleri hafızadan kesin kabul etme.

## Kod, backend ve eski istemciler

- Code review'u son yayın diff'i ve adayın çalışma ağacı üzerinde uygula. Hassas erişim, hesap silme, rol/ownership, kişisel veri logları, iOS DTO serializer eşleşmesi, loading/error/retry ve yeni navigation akışları önceliklidir. Diff veya son yayın ref'i eksikse review kapsamının sınırını belirt.
- Yeni callable, collection, query/index, rules veya veri modeli değişikliği varsa `firebase/v2/firebase.json`, functions exports ve indeks tanımlarını istemcinin kullandığı ad/bölge/payload ile eşleştir. Eski mağaza istemcileri yeni backend ile, yeni aday mevcut backend ile çalışabiliyor mu incele. İstemci gereksinimlerini karşılayan backend sırasını ve geri dönüş sınırlarını raporla.
- Backend etkilenmişse mevcut script'leri doğruladıktan sonra ilgili testleri çalıştır. Repo örnekleri: `npm --prefix firebase/v2/functions run build`, `npm --prefix firebase/v2 run test:rules`, `npm --prefix firebase/v2 run test:functions`; cost/schema/storage değişikliklerinde ilgili mevcut testleri ekle. Emulator hedefi `demo-good4-v2` olmalı; testleri canlı projeye yönlendirme.
- Production'a yeni bağımlılık varsa yetkili salt okunur erişimle gerekli functions/rules/config ve index durumlarını denetle. Her komutta açık project ID kullan. Yeni index için canlı `READY` kanıtı gerekir; emülatör testleri veya deploy success bunu kanıtlamaz. Gerekli deploy yetkilendirilmişse [good4-firebase-release](../good4-firebase-release/SKILL.md) üzerinden yürüt; aksi halde eksik bağımlılığı raporla.
- App Check başlangıcı Firebase kullanımından önce olmalı. Android Play Integrity ve gerçek iOS cihazında App Attest doğrulaması gerekir; enforcement kapalıyken başarılı istek attestation kanıtı değildir. Enforce ayarını kendiliğinden açma; eski istemci uyumluluğunu da kontrol et.

## Cihaz kabulü ve mağaza bilgileri

Mevcut cihaz ve uygun test hesabıyla yapılabilen kontrolleri uygula. Veri oluşturan/silen senaryolar için kullanıcı yetkisini ve test verisi sınırlarını koru. Erişilemeyen senaryoları bekleyen olarak kaydet; kaynak incelemesini cihaz testi diye sunma.

- Android küçültülmüş release ve gerçek iOS Release/TestFlight: soğuk açılış, önceki mağaza sürümünden güncelleme, oturum korunması, çıkış/yeniden giriş; mevcut e-posta/Google/Apple giriş yolları, doğrulama ve iptal/hata akışları.
- Rol bazlı ana ekranlar ve değişen özellikler; topluluk/etkinlik/menü/market gibi etkilenmiş akışlarda liste, detay ve temel aksiyon. Ağ kesintisi, erişim reddi, boş sonuç, yeniden deneme ve uygulamayı arka plana alma.
- Görsel akış değişmişse galeriden seçim, hazırlama sırasında submit, kapak kaldırma ve izin reddi. Push değişmişse izin reddi/kabulü, gerçek cihazda alım ve doğru ekrana açılış. Zaman akışında gerekiyorsa gece yarısı/gün değişimi. Formlarda sistem geri tuşu.
- Kullanılan Android/iOS mağaza güncelleme akışı ve store URL'leri; `docs/app-update-notice.md` ile erteleme/fallback davranışı. Debug simülasyonunu gerçek mağaza update kanıtı sayma.
- `docs/legal`, mağaza metadata taslakları ve güncel ekran görüntülerini sürüm davranışıyla karşılaştır: gizlilik/data safety beyanı, hesap silme yolu ve bağlantıları, review hesabı/yönergeleri, destek URL'leri, yeni izinlerin gerekçesi, iOS `PrivacyInfo.xcprivacy`. Taslak dokümandaki geçmiş test/deploy iddialarını güncel kanıt olarak kullanma.
- Console erişimi varsa mağaza validation uyarılarını ve seçilen dağıtım kanalını salt okunur incele; otomatik submit/rollout başlatma. Cihaza, imzalamaya veya Console'a erişim yoksa kullanıcıya gereken somut adımı ver.

## Kanıtlı sonuç

Türkçe bir rapor üret; uzun raporu istenirse veya okunabilirliği artırıyorsa `docs/` altında tarihli Markdown dosyasına kaydet. Her kontrolü `GEÇTİ`, `BAŞARISIZ`, `DOĞRULANAMADI` veya gerekçeli `KAPSAM DIŞI` olarak işaretle. Komut/cihaz/mağaza kanıtı, aday sürümü, zaman ve ilgili dosya/satır veya log yolunu ekle. Kaynak değişmişse eski başarılı sonucu yeniden kullanma; aynı kaynak için geçerli kanıtı gereksiz tekrar üretme.

Önce yayın kararını ve engelleri, sonra kontrol sonuçlarını sun:

- **Yayına hazır:** Hedef platform için gerekli kontrollerin tamamı kanıtlı geçti; yayın engeli yok.
- **Yayın engelli:** Başarısız kritik kontrol, güvenlik/veri kaybı, yanlış ortam, imza/sürüm sorunu veya hazır olmayan zorunlu backend bağımlılığı var.
- **Kontroller eksik:** Derleme/test geçmiş olsa bile zorunlu signing, mağaza veya gerçek cihaz kontrolü doğrulanamadı. Bunu koşulsuz hazır olarak sunma.

Tek platform istenmişse diğer platform için hazır kararı verme. Her engelde etkisini, uygulanabilir düzeltmeyi ve tekrar yapılacak kontrolü belirt. Rapor sonunda yayın/deploy yapılıp yapılmadığını açıkça yaz; skill'in çalışması kendi başına yayın başlatmaz.
