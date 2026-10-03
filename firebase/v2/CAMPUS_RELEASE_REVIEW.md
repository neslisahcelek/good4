# Kampüs Dolabı — 3 Ekim 2026 inceleme notu

Kaynak klasörü: `/Users/cankilinc/Desktop/Good4/Good4 dev`.
Firebase projesi: `good4tr-v2`.

Kullanıcı, Apple bildirim hesabı konusunu ertesi güne bıraktı; mevcut geliştirmelerin commit edilmesini ve sunucu/web kodunun canlıya alınmasını istedi. Bu not Claude ile yapılacak kod incelemesi içindir.

## İnceleme kapsamı

- Kampüs Dolabı ilan akışı, mesajlaşma, teklifler, şikayet/engelleme ve yönetim paneli.
- Yalnızca `@ogr.akdeniz.edu.tr` öğrenci adresinin doğrulanması; numara girişi ve sabit uzantı; kısa Outlook/spam hatırlatması; tarayıcıda tek tıkla onay. Ana Google/Apple hesabı korunur.
- Android/iOS bildirim kaydı, izin, çıkış/hesap değişimi ve bildirime dokununca ilgili ekrana geçiş. Sunucu kilit ekranına özel mesaj metni göndermez.
- Claude'un mesaj limiti değişikliği: kullanıcı başına, ilan başına, İstanbul takvim gününde 200 mesaj; farklı ilanların hakları ayrıdır. Eşzamanlı gönderim testi sınırın aşılmadığını doğrular.
- Web indirme sayfası, `/admin` panel yolu, Kampüs Dolabı moderasyon bölümü ve ayrı okul e-postası onay sayfası.

## Yayın kapsamı

Hosting ve aşağıdaki sekiz sunucu işlevi başarıyla yayınlandı. Yayın sonrasında sekiz işlevin tamamı `europe-west1` bölgesinde **ACTIVE** olarak doğrulandı:

```text
registerPushDevice
unregisterPushDevice
sendMarketMessage
respondMarketOffer
reviewMarketListing
createMarketListing
reportMarketContent
deleteMyAccount
```

Firestore kuralları/indeksleri, Storage kuralları ve diğer sunucu işlevleri bu yayının hedefi değildir. Mobil kaynak kodunun commit edilmesi App Store/Google Play yayını veya fiziksel telefona kurulum anlamına gelmez.

Canlı web adresleri:
- İndirme sayfası: https://good4tr-v2.web.app/
- Yönetim paneli: https://good4tr-v2.web.app/admin
- Okul e-postası onay sayfası: https://good4tr-v2.web.app/campus-email-verification

Üç yol da HTTP 200 verdi ve yayınlanan HTML ile bütün JS/CSS dosyaları yerel üretim derlemesiyle birebir eşleşti. Onay sayfasının `Referrer-Policy` değeri `no-referrer`. Yeni cihaz kayıt/kaldırma işlevlerine oturumsuz istekler HTTP 401 `UNAUTHENTICATED` verdi; gerçek kullanıcı veya cihaz kaydı oluşturulmadı.

## Doğrulama

- Web üretim derlemesi ve 10 bağlantı testi başarılı; üretim çıktısında yerel test ekranının metinleri bulunmuyor.
- Android V2 birim testleri: 35 test, sıfır hata. iOS simülatör Kotlin derlemesi başarılı. Önceki tam iOS Release simülatör derlemesi de başarılıydı; bu turda fiziksel telefona kurulum yapılmadı.
- Geniş sunucu test çalışması: 133 testin 129'u geçti, iki Auth emülatörü testi atlandı ve mevcut topluluk takibi/kupon eşzamanlılık testlerinde iki `Transaction is invalid or closed` emülatör hatası oluştu.
- Hata veren iki dosya ve yayınla ilgili bildirim, hesap silme, Kampüs Dolabı, okul doğrulama testleri ayrı bir demo proje adında tekrar çalıştırıldı: 64 testin 62'si geçti, iki Auth emülatörü testi atlandı, sıfır hata. İlk çalışmadaki iki başarısız test de bu çalışmada geçti. İlgisiz uygulama kodu değiştirilmedi.
- Gizli anahtar dosyası ve yaygın kimlik bilgisi kalıpları için commit adayları kontrol edildi; özel anahtar dosyası veya bu kalıplara uyan kimlik bilgisi bulunmadı. Gmail uygulama şifresi/APNs özel anahtarı okunmadı veya eklenmedi.

Test/yayın çıktıları yalnızca yerel geçici klasördedir:

```text
/tmp/good4-release-server-tests.log
/tmp/good4-release-server-targets.log
/tmp/good4-release-mobile-checks.log
/tmp/good4-release-web-build.log
/tmp/good4-release-web-tests.log
/tmp/good4-campus-release-deploy.log
/tmp/good4-release-live-checks.json
```

## 4 Ekim 2026'ya kalan konu

Apple Developer'da açık hesap **AHMETCAN KILINC / 5N68564396**. Bu hesapta `com.good4.iosApp` yok; güncel kaynak projedeki Release ekibi **NM79R577GW**. Doğru uygulama/ekip eşleşmesi netleşmeden anahtar oluşturulmadı veya Firebase'e yüklenmedi. Firebase'de iOS APNs anahtarı/sertifikası yok; gerçek iPhone bildirim teslimatı doğrulanmış değildir.

AGENTS.md'nin zorunlu tuttuğu `tools/install-v2-iphone.sh`, `iosApp V2` şeması ve `DebugV2` yapılandırması mevcut değildi. Fiziksel telefon için önce bu kaynak/kurulum eşleşmesi çözülmelidir; eski çalışma klasöründen veya farklı şemayla kurulum yapılmamalıdır.

İncelemede özellikle cihaz kaydının tek hesap sahibi olması, gecikmiş çıkışın başka hesabın kaydını silememesi, sunucudaki yetki/katılımcı denetimleri, günlük ilan bazlı mesaj sınırı ve bildirim hatasının kaydedilmiş işlemi başarısız göstermemesi kontrol edilmelidir. APNs özel anahtarı veya giriş şifresi sohbete, repoya ve loglara konmamalıdır.
