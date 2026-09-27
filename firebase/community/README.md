# Good4 topluluk kurulumu

Uygulama akışı: Ana Sayfa → Topluluklar → Etkinlikler / Topluluğa Özel Kuponlar.
Yetkili kullanıcı kendi topluluğunda Etkinlik ekle, Kupon ekle ve topluluk adı ile üniversite bilgilerini düzenleme işlemlerini görür.
Kuponlar `pending` durumuyla kaydedilir ve bir Good4 işletmesiyle eşleştirilir. Yalnızca güvenilir yönetim aracı `published` yapar. Öğrenci yayımlanmış kupondan tek kullanımlık 6 haneli kod üretir; eşleştirilen işletme bu kodu mevcut **Kod Doğrula** ekranından kullanır.

## Test ortamının durumu

1. Mevcut Good4 kuralları topluluk korumalarıyla birleştirildi; Firestore ve Storage kuralları `good4tr-test` projesine dağıtıldı. Genel kullanıcı yazma kuralı `communities` ve `community_access` koleksiyonlarını özellikle dışarıda bırakır.
2. Google sağlayıcısı `good4tr-test` için etkin. Android `com.good4.test` debug SHA-1/SHA-256 değerleri eklendi ve yerel staging `google-services.json` OAuth istemcileriyle yenilendi.
3. iOS staging Firebase plist'i yerelde oluşturuldu ve Debug URL şeması tanımlandı. Firebase yapılandırma dosyaları güvenlik ve ortam ayrımı için Git tarafından izlenmez.
4. Demo topluluk, örnek etkinlik/kupon ve iki yönetici erişimi oluşturuldu: e-posta/şifreli demo kullanıcı ve `cannklnc7@gmail.com` Google hesabı.
5. Android staging derlemesi, demo e-posta girişi, topluluk ekranı ve yönetim araçları gerçek test Firebase projesiyle doğrulandı.

## Üretimden önce kalanlar

1. Aynı kuralları `good4tr` üretim projesinin mevcut kurallarıyla yeniden karşılaştırın; testteki dosyaları körlemesine üretime taşımayın.
2. Üretim Android imza SHA değerlerini ve üretim OAuth istemcilerini ekleyip prod `google-services.json` dosyasını yenileyin.
3. Üretim iOS plist ve ters istemci kimliğini ekleyin. CocoaPods kurulu bir ortamda `iosApp` içinde `pod install` çalıştırıp `Podfile.lock` güncellemesini ve tam Xcode derlemesini doğrulayın.
4. İlk gerçek toplulukları ve yönetici e-postalarını aşağıdaki güvenilir araçla tanımlayın. Uygulama hiçbir zaman kendi yönetim yetkisini atamaz.

## Yönetim aracı

`npm ci` kurulumundan sonra Application Default Credentials ile yetkili bir operatör çalıştırır. Kimlik dosyasını repoya veya uygulamaya eklemeyin. Komutlar proje kimliği ister; üretim projesi varsayılmaz.

```sh
node manage.mjs create good4tr-test topluluk-id profile.json
node manage.mjs assign good4tr-test topluluk-id yonetici@example.com
node manage.mjs revoke good4tr-test topluluk-id yonetici@example.com
node manage.mjs approve good4tr-test topluluk-id kupon-belge-id
node manage.mjs unpublish good4tr-test topluluk-id icerik-belge-id
```

`profile.json`: `{ "name": "Topluluk adı", "description": "Kısa açıklama", "logoUrl": "", "coverUrl": "" }`.
Atama mevcut kullanıcı profilini değiştirmez. Yeni Google yöneticisi için Firebase Auth kaydı ve sıfır yemek kredili bir Good4 öğrenci profili oluşturur; gerçek Google girişi e-postanın sahipliğini doğrular. Mevcut kayıtlar, roller ve krediler korunur. Yetkiler `community_access/{verified-email}` belgesindeki `active` ve `communityIds` alanlarında tutulur. Birden fazla topluluk/yönetici desteklenir. Good4 yöneticisi için uygulama içi ayrı atama/onay ekranı henüz yok; ilk kurulum ve kupon onayı bu araçla yapılır.

## Veri ve test

### Etkinlik QR girişi

- Öğrenci etkinliğe kayıt olduğunda rastgele UUID bilet anahtarı oluşturulur. QR biçimi `good4:event:v1/{communityId}/{eventId}/{ticketToken}`; ad, e-posta veya kullanıcı kimliği içermez.
- Bilet, etkinlik ayrıntısından ve topluluk içindeki **Etkinlik kayıtlarım** filtresinden tekrar açılır. Eski kayıtlara bilet ilk açılışta eklenir.
- Yönetici **Topluluğu Yönet → Etkinlik → Katılımcıları Yönet · QR giriş** yolundan kamera taramasını, aramayı, manuel giriş onayını ve geri almayı kullanır.
- `registrations/{userId}` kayıt niyetini, `attendance/{userId}` gerçek girişi tutar. Giriş belgesinde `checkedInBy`, `checkedInAt` (epoch saniye), `method` ve taramada kullanılan `ticketToken` bulunur.
- Her giriş sunucu işlemiyle mevcut etkinlik/kayıt/bilet üzerinden doğrulanır. Eşzamanlı iki okutma tek giriş oluşturur. Girişten sonra öğrenci kaydını silemez; yönetici önce hatalı girişi geri alabilir.
- Yönetici takipçi toplamını ve her etkinliğin kayıt/giriş sayılarını anlık dinler; ekran kapanınca dinleyiciler durur. Takipçi adları arayüzde gösterilmez.
- Android Google kod tarayıcısını, iOS VisionKit kamera tarayıcısını kullanır. iOS simülatörü kamera taramasını desteklemez; manuel giriş ekranı kullanılabilir. Gerçek kamerayla uçtan uca kabul kontrolü fiziksel cihaz gerektirir.
- Firestore kuralları için QR yetkisi, iptal, yanlış topluluk, eşzamanlı giriş ve takipçi erişimi testleri `npm test` kapsamındadır. Android arayüz testleri gerçek Firebase yerine yerel fixture kullanır.

### Görsel depolama notu

Test projesinin Storage alanı Spark planı nedeniyle yüklemeyi reddediyor. Mevcut demo kapak/logo `hosting/public/community_images/demo-toplulugu` üzerinden sunuluyor; bu, uygulamadan yeni görsel yükleme sorununu çözmez.

- `communities/{id}`: name, description, logoUrl, coverUrl.
- `communities/{id}/followers/{userId}`: takip eden kullanıcı ve takip zamanı; belgeyi yalnızca sahibi okuyup değiştirebilir.
- `communities/{id}/entries/{eventId}/registrations/{userId}`: öğrencinin etkinlik kaydı; öğrenci kendi kaydını yönetir, topluluk yöneticisi katılımcıları ve canlı toplamı görür.
- `communities/{id}/entries/{couponId}/claims/{userId}`: öğrenci başına tek aktif kupon hakkı; aynı kupon için tekrar kod üretimini engeller.
- `communities/{id}/entries/{entryId}`: kind (`event` / `coupon`), title, description, date (ISO tarih), time, location, imageUrl, code (eski kısa kodlarla uyumluluk), businessId, status (`published` / `pending` / `cancelled`).
- `community_coupon_codes/{sixDigitCode}`: kullanıcıya özel tek kullanımlık kupon kodu. Yalnızca kod sahibi ve eşleştirilmiş işletmenin sahibi okuyabilir; yalnızca işletme `used` durumuna geçirebilir.
- `community_images/{communityId}/{uuid}.jpg`: en fazla 5 MB görsel.
- Yükleme başarılı olup belge kaydı başarısız olursa Storage'da kullanılmayan görsel kalabilir; periyodik temizlik henüz yok.
- Tarih ve saat kullanıcıya seçicilerle sunulur. Görsel cihaz galerisinden seçilir.
- `npm test`: yalnızca `demo-good4-community` Firestore/Storage emülatörlerinde 12 yetki ve eşzamanlı giriş testi. Üretime bağlanmaz.
- Android `:composeApp:connectedStagingDebugAndroidTest`: öğrenci keşif/kupon akışı ve yönetici önizleme akışı, yalnızca yerel fixture repository kullanır.

Gerçek Google hesap seçiciyle giriş, gerçek yöneticinin Storage yüklemesi, kupon onayının test Firebase'de görünmesi ve iOS uygulamasının tam derlemesi ayrıca uçtan uca doğrulanmalıdır. Bu modül henüz mağazaya/üretime dağıtılmadı.

Kaynaklar: [Firebase Android Google girişi](https://firebase.google.com/docs/auth/android/google-signin), [Firebase iOS Google girişi](https://firebase.google.com/docs/auth/ios/google-signin), [AndroidX Test sürümleri](https://developer.android.com/jetpack/androidx/releases/test).
