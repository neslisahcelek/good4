# Güncelleme ve Zorunlu Güncelleme (Force Update) Yapılandırması

Firebase Console → Firestore Database → `app_config` koleksiyonu → `update_notice` belgesi.
Üretimde `good4tr-v2`, staging ortamında `good4tr-test` projesini kullan.

Belge alanları:

```json
{
  "enabled": true,
  "title": "Good4’a yenilikler geldi",
  "message": "Yeni özellikleri keşfetmek için uygulamanı güncelle.",
  "forceUpdate": true,
  "minVersionCodeAndroid": 18,
  "minVersionIos": "1.1.5"
}
```

### Alan Açıklamaları
- `enabled` *(boolean, opsiyonel)*: Ana ekrandaki isteğe bağlı güncelleme kartını (Soft Update Card) kontrol eder. Yalnızca boolean `true` kartı etkinleştirir. `false` veya eksik olduğunda kart gösterilmez.
- `title` *(string, opsiyonel)*: En fazla 120 karakter. Güncelleme kartında veya ForceUpdateScreen başlığında gösterilir. Boşsa yerel kaynak metni kullanılır.
- `message` *(string, opsiyonel)*: En fazla 400 karakter. Güncelleme açıklama metni. Boşsa yerel kaynak metni kullanılır.
- `forceUpdate` *(boolean, opsiyonel)*: `true` yapıldığında aşağıdaki sürüm koşullarını sağlayan eski istemcilerde `ForceUpdateScreen` açılır ve uygulama kilitlenir. `false` veya eksik olduğunda zorunlu güncelleme uygulanmaz.
- `minVersionCodeAndroid` *(integer, opsiyonel)*: Android için izin verilen en küçük `versionCode` (ör. 18). Cihazdaki `versionCode < minVersionCodeAndroid` ise zorunlu güncelleme tetiklenir.
- `minVersionIos` *(string, opsiyonel)*: iOS için izin verilen en küçük semantik sürüm (ör. `"1.1.5"`). Cihazdaki sürüm bu değerden küçükse zorunlu güncelleme tetiklenir.

### Zorunlu Güncelleme (ForceUpdateScreen) Davranışı
- Uygulama açılışında (`Splash` ekranı) ve uygulama arka plandan ön plana döndüğünde kontrol edilir.
- Eski sürüm tespit edildiğinde kullanıcı doğrudan `Route.ForceUpdate` ekranına yönlendirilir ve geri yığın temizlenir.
- Android geri tuşu/hareketi etkisizdir; vazgeç veya kapat butonu bulunmaz.
- Tek buton olan "Uygulamayı Güncelle", platforma göre Google Play Store veya Apple App Store sayfasını açar.
- Oturumu açık olmayan kullanıcıların da kontrol edilebilmesi için `firestore.rules` içinde `update_notice` belgesinin `get` izni herkese açıktır.

### İsteğe Bağlı Güncelleme (Soft Update Card) Davranışı
- `forceUpdate` kapalı veya sürüm minimumun üzerindeyse, mağazada yeni bir sürüm olduğunda ana ekranda isteğe bağlı `AppUpdateCard` gösterilir (`enabled == true` koşuluyla) ve kullanıcı 7 gün erteleyebilir.
