# Yerel test ve yayın devri

Son güncelleme: 2 Ekim 2026. Kod değişiklikleri hazırlanmıştır; prod'a yayın yapılmamıştır. Canlı bütçe, App Check zorunluluğu ve ikinci aşama liste kuralları henüz etkin değildir.

## 1. Emülatörleri başlat

Node.js 22 ve Java 21 gerekir. Bu Mac'te Android Studio'nun Java kurulumu kullanılabilir. İlk kurulumda `firebase/v2`, `functions` ve `web` dizinlerinde `npm ci` çalıştır.

Birinci terminal:

```sh
cd /Users/neslisahcelek/Documents/GitHub/good4/firebase/v2
export JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home'
export PATH="$JAVA_HOME/bin:$PATH"
npm run emulators:local
```

Bu komut backend'i ve emülatöre bağlanan web panelini derler. Terminali açık tut. Web: <http://127.0.0.1:5005>; emülatör yönetimi: <http://127.0.0.1:4105>.

İkinci terminal:

```sh
cd /Users/neslisahcelek/Documents/GitHub/good4/firebase/v2
npm run seed:local -- --large
```

Yalnızca `demo-good4-v2` içine sentetik veri yazılır: 125 etkinlik ve ilk etkinlikte 125 katılımcı. Parola tüm hesaplarda `LocalTest123!`:

| Hesap | Kullanım |
| --- | --- |
| `local-student@akdeniz.edu.tr` | Mobil öğrenci |
| `local-manager@akdeniz.edu.tr` | Topluluk paneli |
| `local-admin@akdeniz.edu.tr` | Yönetici paneli |

Bu hesaplar canlı Firebase'de bulunmaz. Emülatör kapanınca istemci canlı ortama geçmez; bağlantı hatası beklenir. Yerel testte gerçek bildirim dağıtımı, OCR ve zamanlanmış prod işleri çalışmaz. Bu rehberdeki testler için test projesine Blaze bağlamak gerekmez.

## 2. Mobil uygulamayı aç

Android Studio'da Android emülatörünü aç. Repo kökünden:

```sh
./gradlew :composeApp:installStagingDebug
```

`stagingDebug`, varsayılan olarak bilgisayara `10.0.2.2` üzerinden bağlanır. Fiziksel Android cihazda bilgisayarın yerel IP'sini `-Pgood4.firebaseEmulatorHost=192.168.x.x` ile ver; cihaz ve bilgisayar aynı ağda olmalı. `prodDebug` canlı prod'a bağlanır; yerel test için seçme. `-Pgood4.useFirebaseEmulators=false` de yerel bağlantıyı kapatır; bu rehberde kullanılmaz.

iOS için `iosApp/iosApp.xcodeproj` dosyasını Xcode'da aç, **iosApp Test / Debug** ve bir **Simulator** seç. iOS yerel adresi `127.0.0.1` olduğundan bu yapılandırma fiziksel iPhone için uygun değildir. Yerel SDK JSON/plist dosyaları Git'e dahil değildir; dosyaların konumları [mobil rehberde](mobile/README.md) bulunur. Öğrenci hesabıyla yerel e-posta/parola girişini kullan.

## 3. Elle doğrulama

1. **Gereksiz okumalar:** Öğrenci ana ekranını iki dakika açık tut, sonra arka plana alıp dön. Emülatör Firestore isteklerinde V2 akışından eski `codes/products` sorgusu veya 30 saniyelik rezervasyon yenilemesi oluşmamalı.
2. **Panel yenilemesi:** Topluluk hesabıyla paneli aç. İki dakika beklemek 15 saniyelik tekrarlı dashboard çağrısı oluşturmamalı. Ekran açılışı, kullanıcı yenilemesi ve başarılı değişiklik sonrası yenileme çalışmalı. Aynı anda aynı yenilemeye basmak istekleri çoğaltmamalı.
3. **Sayfalama:** Etkinlikler 50 / 50 / 25 olarak yüklenmeli; ID'ler tekrarlanmamalı veya atlanmamalı. İlk açılışta katılımcılar indirilmemeli. İlk etkinliğin katılımcı bölümünü açınca 50 kişi, devamında 50 ve 25 kişi gelmeli. Etkinlikler arasında hızlı geçişte önceki etkinliğin katılımcıları görünmemeli.
4. **Mobil listeler:** Öne çıkan etkinlikler en fazla 20 gelecek etkinlik göstermeli. Topluluk etkinliklerinde devamını yükleme çalışmalı. Geçmiş etkinlik öne çıkanlara girmemeli.
5. **Önbellek:** Aynı oturumda tekrar açıldığında topluluklar 15 dakika, banner/menü 1 saat, akademik takvim 24 saat yeniden okunmamalı. Çıkış ve hesap değişiminde önceki hesabın kişisel verisi görünmemeli. İçerik değişikliği önbellek süresi dolana kadar görünmeyebilir.
6. **Kritik işlemler:** Öğrenciyle takip, etkinlik kaydı ve kaydı iptal etme; yönetici/topluluk hesabıyla etkinlik düzenleme çalışmalı. Hesap silmeyi ayrı sentetik hesapla dene; işlem hesabı kaldırır.
7. **Durdurma kontrolü:** Yönetici bildirim bölümünden ek işleri elle durdur ve tekrar aç. Öğrenci giriş/kayıt/takip akışı çalışmaya devam etmeli. Gerçek FCM gönderimi yerelde kapalıdır; kota, tekrar deneme ve sahte $8 bütçe olayının davranışı otomatik backend testlerinde doğrulanır.
8. **Medya/OCR:** Büyük veya geçersiz görsel anlaşılır hata vermeli. Sunucunun kabul ettiği banner en fazla 1.600 piksel ve 512 KiB olmalı. OCR'nin aynı hatalı görseli günde bir kez denemesi ve son geçerli menüyü koruması backend testleriyle doğrulanır; yerelde zamanlayıcıyı açma.

Panel okumaları kullanıcı başına 6/dakika; takip/etkinlik işlemleri 30/dakika; cihaz kaydı 10/dakika sınırındadır. Sınırı bilerek aşınca `resource-exhausted` beklenir; tekrar denemeden önce bir dakika bekle.

## 4. Otomatik kontroller

Önce çalışan emülatörü birinci terminalde Ctrl+C ile kapat; test komutları kendi emülatörünü açar. Java değişkenleri ayarlı terminalde:

```sh
cd /Users/neslisahcelek/Documents/GitHub/good4/firebase/v2
npm test
npm run test:cost-rules
```

İkinci komut henüz yayınlanmayan `firestore.bounded.rules` için tekil okumanın ve 50 belgeli listenin kabul edildiğini, limitsiz/101 belgeli sorgunun reddedildiğini doğrular.

Mobil kontroller, repo kökünden:

```sh
./gradlew :composeApp:compileStagingDebugKotlinAndroid :composeApp:compileProdDebugKotlinAndroid :composeApp:compileKotlinIosSimulatorArm64 :composeApp:testStagingDebugUnitTest
```

Son doğrulamada 107 backend testi, 21 Firestore kural testi (ikinci aşama dahil), 4 şema testi, Android ve iOS ortak Kotlin derlemeleri, Android'de 32 birim testi ve web build'i geçti. Tam Swift/Xcode uygulama derlemesi ve cihazdaki uçtan uca kabul testleri ayrıca yapılmalıdır; ortak Kotlin derlemesi bunların yerine geçmez.

## 5. Yayın öncesi bekleyen canlı işler

- **Bütçe:** Prod Billing Budgets API açıldı; bütçeleri okuma/kurma işlemi mevcut hesabın faturalandırma yetkisi nedeniyle **403** aldı. $10 bütçe ve $3/$5/$8/$10 uyarıları kurulmuş değildir. Faturalandırma hesabında gerekli bütçe yönetim yetkisi sağlandıktan sonra `node scripts/cost-controls.mjs --apply-budget` çalıştırılmalı; oluşturulan bütçe ve Pub/Sub bağlantısı doğrulanmalı. Bütçe Functions tetikleyicisi de yayınlanmalı.
- **Kesin tavan yok:** $10 hedef ve $8'de ek işleri durdurma politikası fatura garantisi değildir. Bütçe bildirimleri gecikebilir. Functions instance sınırları harcama hızını azaltır; Firestore okuma ücretlerini tamamen engellemez.
- **İlk yayın:** Kaldırmalar, sayfalama, önbellek, worker/kota ve kaynak sınırları aynı backend/istemci sürümleriyle yayınlanmalı. Canlı Functions hâlen eski kaynak ayarlarını kullanıyor. Test projesinin canlı V2 şema geçişi yapılmadı; yerel test bu geçişe bağlı değildir.
- **İkinci aşama:** Android, iOS ve web App Check doğrulandıktan sonra Functions doğrulaması ve doğrudan Firestore App Check zorunluluğu açılmalı. Yeni istemcilerin sorguları doğrulanınca `firebase.bounded.json` ile kurallar yayınlanmalı. Bu ayarlar bu commit'te canlıya uygulanmaz.
- **Artifact temizliği:** Yetkili hesapla `npx firebase functions:artifacts:setpolicy --project production --location europe-west1 --days 7` çalıştırılıp mevcut politikayla birlikte incelenmeli. Canlı politika değiştirilmedi.
- **GitHub:** Workflow ve script kaldırıldı. `main` dal koruması API'si entegrasyona 403 döndü; GitHub Settings → Branches/Rulesets içinde eski AI kontrolü zorunluysa kaldırılmalı.
- **Diğer kullanım:** reCAPTCHA anahtarının proje sahipliği/ücretli kullanımı, Vercel ve GitHub ücretli kullanım ayarları bu işlemde doğrulanmadı. API anahtarı kısıtları mevcut istemci sağlayıcılarıyla birlikte ayrıca denetlenmeli.
- **Kalan sınırlar:** İşletme geçmişi son 50 kayıtla sınırlıdır; eski kupon yönetiminde topluluk başına ilk 50 bekleyen kupon yüklenir. Bu bölümlerde büyük veriyle devam sayfası gerekir. Mobil yönetici kayıt/katılım okumaları da gerçek büyük veriyle ayrıca ölçülmelidir. Bu nedenle bütün okuma maliyetlerinin kontrol altına alındığı varsayılmamalı.

Prod'a yayın, canlı bütçe ve cihaz testleri kullanıcıya devredilmiştir. [Bildirim rehberi](PUSH_NOTIFICATIONS.md) fiziksel cihaz/FCM kabul adımlarını içerir.
