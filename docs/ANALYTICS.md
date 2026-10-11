# İsteğe bağlı mobil kullanım analitiği

Android/iOS ana öğrenci akışları Firebase Analytics SDK üzerinden ölçülür. Analytics ücretsizdir; bu entegrasyon Firestore yazımı, Function çağrısı, Remote Config okuması veya BigQuery export eklemez. Mevcut uygulama işlemlerinin backend maliyetleri değişmez. Web paneli ve yönetim işlemlerine event eklenmez.

## Veri ve izin sözleşmesi

- `interaction_click`: `feature`, `screen`, `action`, isteğe bağlı `target`.
- `interaction_result`: aynı parametreler + `status`: `success`, `error`, `validation_error`.
- İsimler ve değerler `analytics/AnalyticsTracker.kt` kataloğundadır. Ham string, UID, içerik kimliği, URL, e-posta, telefon, mesaj, kod, başlık, arama metni veya exception detayı kabul edilmez.
- SDK izin sonrasında otomatik kurulum/oturum ve cihaz bilgilerini de işler; Analytics verisi tamamen anonim olarak tanımlanmaz. Firebase kullanıcı kimliği veya `setUserId` kullanılmaz. IDFA/AD_ID, iOS IDFV ve reklam kişiselleştirme kapalıdır. Otomatik ekran raporlaması kapalıdır.
- İzin installation bazında cihazda saklanır; hesaplar veya cihazlar arasında senkronize edilmez. İlk öğrenci ana sayfasında sorulur. Ret kalıcıdır; pencereyi kapatma o uygulama çalışması için tekrar göstermeyi durdurur ve izin vermez. Ayarlardan değiştirilebilir.
- İzin öncesindeki aksiyonlar kuyruklanmaz. Bir işlem izin öncesinde başladıysa sonradan izin verilmesi onun sonucunu göndermez. İzin geri alınıp tekrar verilirse eski bekleyen sonuçlar gönderilmez. Tek işlem sonucu yalnız bir defa kaydedilir.
- İzin geri alındığında SDK toplaması kapatılır ve yerel analytics verisi sıfırlanır; bu işlem sunucuya ulaşmış veriyi geriye dönük silmez. Kullanıcı silme talepleri mevcut gizlilik başvuru süreciyle işlenir.
- Başarılı sonuç, mevcut işlem yanıtının uygulamaya ulaştığını ifade eder. Bağlantı kesilmesi sonrası sunucuda tamamlanmış ama istemciye ulaşmamış işlemler ayrıca sorgulanmaz. Kullanıcı akışının iptali sonuç event’i üretmez.

## Paket ayrımı

- Android staging debug varsayılan olarak emulator kullanır ve Analytics kalıcı kapalıdır. Gerçek staging için `-Pgood4.useFirebaseEmulators=false` ile derle; Firebase projesi `good4tr-test` olmalıdır.
- Android production debug paketleri Analytics’i manifest seviyesinde kalıcı kapatır. Release paketleri kullanıcı izniyle toplar.
- iOS Debug (test emulator) ve DebugV2, `Info-AnalyticsDisabled.plist` ile kalıcı kapalıdır. iOS Release, `Info.plist` ile izin verilene kadar kapalı başlar. `AppEnvironment` ve plist ayarları birlikte korunmalıdır.
- iOS staging cihazında DebugView doğrulaması için Release yapılandırmasını `PRODUCT_BUNDLE_IDENTIFIER=com.good4.iosApp.test FIREBASE_CONFIG_FILE=GoogleService-Info-Test.plist` ile derle. Release binary olduğundan mevcut ortam çözümleyicisi emulator’a bağlanmaz. İmzalama/provisioning profilini test bundle’ına uygun seç; production bundle’ıyla test verisi gönderme.

## Firebase / GA4 raporları

Bu çalışma canlı Analytics bağlantısını veya ayarlarını değiştirmez. Mobil yayın öncesinde:

1. `good4tr-test` ve `good4tr-v2` projelerinde Analytics bağlantısını ve Android/iOS app stream’lerini kontrol et. Eski `good4tr` projesini kullanma. Analytics etkin değilse proje sahibi bağlantıyı tamamlamalıdır.
2. GA4 **Yönetici → Veri görüntüleme → Özel tanımlar** bölümünde **event kapsamlı** `feature`, `screen`, `action`, `target`, `status` boyutlarını bu parametre adlarıyla ekle. Yüksek kardinaliteli içerik/kurulum kimliklerini özel boyut yapma.
3. Keşif raporunda `interaction_click` sayısını feature/screen/action/target ile grupla. İşlem sonuçlarında action/status dağılımını karşılaştır. Örneğin `publish_listing` için success sonuçlarının click sayısına oranı, izin veren kullanıcıların ölçülen tamamlanma oranıdır; tüm kullanıcıların oranı olarak sunma. Kullanıcıların yalnız kendi cihazındaki izinli ve teslim edilmiş event’leri ölçülür.
4. Kullanıcı/event düzeyindeki veri saklamayı 2 ay olarak ayarla; Google Signals, reklam bağlantıları ve BigQuery export açma. Mevcut bağlantılar varsa proje sahibi ayrıca incelemelidir; kod bunları kapatmış sayılmaz.
5. Güncellenmiş web gizlilik metnini mobil sürümden **önce** yayınla. App Store / Google Play beyanlarında ürün etkileşimi, kurulum/cihaz tanımlayıcısı, teknik kullanım ve IP’den türetilen yaklaşık bölge verilerini Analytics amacıyla bildir; isteğe bağlı izin ve reklam takibinin kapalı olduğunu belirt. Mevcut işlevsel veri kategorilerini kaldırma.

## Doğrulama

Temel testler:

```sh
./gradlew :composeApp:testProdDebugUnitTest --tests 'com.good4.analytics.AnalyticsTrackerTest'
./gradlew :composeApp:compileProdDebugKotlinAndroid :composeApp:compileKotlinIosSimulatorArm64
./gradlew :composeApp:connectedStagingDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.good4.analytics.AnalyticsConsentUiTest
```

Gerçek staging Android cihazında SDK teslimini kontrol etmek için emulator kapalı staging paketi kullan:

```sh
adb shell setprop debug.firebase.analytics.app com.good4.test
./gradlew :composeApp:connectedStagingDebugAndroidTest -Pgood4.useFirebaseEmulators=false -Pandroid.testInstrumentationRunnerArguments.class=com.good4.analytics.AnalyticsSDKSmokeTest
```

Firebase Analytics → DebugView’da izin öncesi/ret durumunda event olmadığını; izin sonrası tek tıklama için bir click, işlem yanıtı için bir result geldiğini doğrula. Menü/bildirim açma yalnız click üretir. Sayfa yenileme, recomposition ve kod durumu polling’i yeni event üretmez. Analytics ayarını kapat, uygulamayı yeniden aç ve yeni event gelmediğini doğrula; tekrar açtıktan sonra önceki event’lerin gelmediğini kontrol et. Android testini bitirirken:

```sh
adb shell setprop debug.firebase.analytics.app .none.
```

iOS gerçek staging Release paketinde scheme launch argument `-FIRDebugEnabled` ile aynı adımları uygula; sonra argümanı kaldır. DebugView doğrulaması Firebase console erişimi ve uygun staging paket/cihaz gerektirir; yalnız derleme veya fake transport testinin geçmesi canlı teslim kanıtı değildir.

### 11 Ekim 2026 doğrulama kaydı

- Android Kotlin ve iOS simulator Kotlin derlemeleri geçti. `iosApp V2 / DebugV2` tam Xcode simulator derlemesi native Analytics bridge dahil geçti.
- Android izin arayüzü cihazda 2/2 testi geçti: kabul, ayardan geri çekme ve ret tercihinin yeniden başlatmada korunması.
- Tracker birim testlerinin önceki 10 senaryosu geçti. Son incelemede izin kaydetme hatasından sonra tekrar deneme düzeltmesi ve 11. test eklendi; bu son tekrar çalıştırması kullanıcı tarafından durduruldu.
- Gerçek staging SDK smoke testi Samsung SM-A705FN Android 11 cihazında geçti. `good4tr-test` DebugView’da 4 `interaction_click` ve 3 `interaction_result` görüldü. Bunlar sabit sentetik test işlemleridir; backend işlemi oluşturmaz. Test sonunda SDK sıfırlandı ve debug cihaz işareti kaldırıldı.
- Web gizlilik metni derlemesi geçti. Canlı gizlilik yayını, özel boyutların oluşturulması ve mağaza beyanlarının gönderimi yapılmadı.
- iOS fiziksel staging cihazındaki izin/DebugView senaryoları ve mağaza Release arşivi yayın öncesi kontrol listesinde kalır; simulator derlemesi bu kontrollerin yerine geçmez.

Kaynaklar: [Firebase fiyatlandırması](https://firebase.google.com/pricing), [event raporları](https://firebase.google.com/docs/analytics/android/events), [iOS veri toplama kontrolleri](https://firebase.google.com/docs/analytics/ios/configure-data-collection), [DebugView](https://firebase.google.com/docs/analytics/debugview).
