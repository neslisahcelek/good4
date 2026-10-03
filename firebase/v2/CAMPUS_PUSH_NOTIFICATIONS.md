# Kampüs Dolabı telefon bildirimleri

Güncel kaynak: `/Users/cankilinc/Desktop/Good4/Good4 dev`.

## 3 Ekim 2026 — uygulanan değişiklikler

- Boş `sendPushToUser` taslağı yerine Firebase Admin SDK ile FCM gönderimi eklendi. Mevcut sunucu olayları yeni mesajı/teklifi, teklif yanıtını ve ilan onayını/reddini bu göndericiye iletir. Adminlere yeni ilan ve şikayet olayları için mevcut bildirim çağrıları da korunur.
- `registerPushDevice` ve `unregisterPushDevice` çağrıları eklendi. Kullanıcı kimliği istemcinin parametresinden değil doğrulanmış ana oturumdan alınır. Kayıt için aktif kullanıcı profili gerekir.
- `pushDevices/{sha256(token)}` sunucuya özel koleksiyondur. Mevcut Firestore kurallarındaki varsayılan yasak istemci erişimini kapatır; kural veya indeks değişikliği gerekmez. Ham FCM cihaz adresi yalnızca bu özel kayıtta ve SDK belleğinde kullanılır, çıktı veya loglara yazılmaz.
- Her FCM adresinin tek hesap sahibi vardır. Hesap değiştirince sahiplik değiştirilir. Eski hesabın gecikmiş çıkışı veya gönderim hatası yeni kaydı silemez. Hesap silme işlemi cihaz kayıtlarını da temizler.
- Cihaz kayıtları ön plana dönüşte en fazla altı saatte bir yenilenir; 30 gün yenilenmeyen kayıtlar gönderimde temizlenir. Kalıcı geçersiz cihaz hataları temizlenir; geçici servis/APNs hataları cihaz kaydını silmez.
- FCM gönderimi toplam beş saniyeyle sınırlıdır. Bildirim hatası/yavaşlaması, sunucuda kaydedilmiş mesajı veya ilanı başarısız bir işlem haline getirmez. Loglar yalnızca teslimat sayıları ve genel hata bilgisini içerir.
- Android: Messaging SDK, Android 13+ izni, Kampüs Dolabı bildirim kanalı, servis, küçük bildirim simgesi ve bildirime dokunma akışı eklendi.
- iOS: Firebase/Messaging 12.9.0, uygulama/bildirim delegeleri, APNs adres eşleme ve push yeteneği eklendi. Otomatik delegate müdahalesi kapatılıp eşleme açıkça yapılır. Debug için development, Release için production APNs ortamı kullanılır.
- Bildirim izni otomatik sorulmaz. Doğrulanmış öğrencinin Kampüs Dolabı akışındaki kısa “Yeni mesaj ve teklifleri kaçırma / Bildirimleri aç” kartı sistem izin penceresini açar. Ret, uygulamayı kullanmayı engellemez; sonradan ayarlardan açılabilir.
- Kilit ekranına özel mesaj içeriği yazılmaz. Mesaj/teklif bildirimi ilgili sohbete; ilan durumu bildirimi öğrencinin İlanlarım ekranına götürür. Giriş/splash bitene kadar yönlendirme bekler. Farklı hesap adına gelen bildirime erişim verilmez.
- Hesaptan çıkmadan önce sunucu cihaz kaydı kaldırılır ve SDK cihaz adresi silinir. Ağ sorunu uygulamanın çıkışını engellemez; sunucu kaydı temizlenemese bile kilit ekranı metni kişisel mesaj içeriği taşımaz ve sonraki hesap kaydı tek sahipliği korur. SDK geçici olarak adres döndüremezse son başarılı kayıt silinmez; iznin gerçekten kapatılmasıyla kaldırılır.
- Akışta kalmış eski “bağlantıyı yapıştır” metni Outlook ve gereksiz e-posta/spam hatırlatmasıyla değiştirildi.

## Kod doğrulaması

- Android V2 Kotlin ve iOS simülatör Kotlin derlemesi geçti.
- Sunucu: 13 bildirim testi + mevcut hesap silme ve güncel mesaj limiti/pazar testleri, toplam 33 test geçti. İzole `demo-good4-push` projesi ve yerel Firestore emülatörü kullanıldı; gerçek FCM gönderilmedi. Entegrasyon testi gerçek `sendMarketMessageService`, `respondMarketOfferService`, `reviewMarketListingService` çağrılarını yeni gönderici ve sahte FCM sağlayıcısıyla çalıştırdı; mesaj, teklif, teklif yanıtı, onay ve ret doğru cihazlara yönlendi. Yavaş sağlayıcı sınırı ayrıca test edildi.
- Mobil: 6 bildirim testi geçti. İzin reddi, giriş olmadan kayıt yapılmaması, hesap değişimi, cihaz adresi değişimi, yenileme sıklığı, yönlendirme kimliklerinin kontrolü ve eşzamanlı yeni bildirim korunması kapsandı.
- iOS tam Release simülatör derlemesi geçti. Swift hata-delegesi uyumsuzluğu `Swift.Error` ile düzeltildi; ikinci tam derleme bu uyarı olmadan geçti. Son geçici SDK adresi hatası düzeltmesi ayrıca güncel iOS Kotlin derlemesi ve mobil testleriyle doğrulandı.
- `com.good4.iosApp`, `good4tr-v2` bağlantısı doğrulanarak iPhone 17 Pro simülatörüne yüklenip açıldı. Simülatörde gerçek FCM cihaz kaydı bilinçli olarak kapalıdır; bu işlem APNs teslimat testi değildir. Fiziksel iPhone'a kurulum yapılmadı.

Detaylı test çıktıları Mac'in geçici klasöründedir:

```text
/tmp/good4-campus-push-server-tests.log
/tmp/good4-campus-push-client-tests.log
/tmp/good4-campus-push-xcode.log
```

## Yayın ve gerçek telefon testi

3 Ekim 2026'da kullanıcının açık yayın/commit isteği üzerine aşağıdaki sekiz sunucu işlevi ve Hosting canlıya yayınlandı. Sekiz işlevin tamamı `europe-west1` bölgesinde **ACTIVE**. Mobil değişiklikler kaynak kodundadır; fiziksel telefon veya uygulama mağazası yayını yapılmadı. Apple bağlantısı aşağıdaki hesap eşleşmesi nedeniyle 4 Ekim'e bırakıldı. Yayın ve test ayrıntıları `CAMPUS_RELEASE_REVIEW.md` dosyasındadır.

Yayınlanan işlevler:

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

Claude'un aynı klasördeki ilan başına günlük 200 mesaj değişikliği sunucu testleriyle birlikte doğrulanarak mesaj işlevlerinin yayınında yer aldı. Hosting'de güncel üretim web çıktısı yayınlandı. Firestore kuralları/indeksleri, Storage kuralları ve diğer sunucu işlevleri bu yayına dahil edilmedi.

Gerçek iPhone teslimatı için Firebase projesinin iOS Cloud Messaging ayarlarında uygun APNs anahtarı/sertifikası ve Apple uygulama kimliğinin Push Notifications yeteneği gerekir. 3 Ekim'de Firebase Console, `good4tr-v2` → Cloud Messaging → `com.good4.iosApp` salt okunur kontrol edildi: FCM HTTP V1 API **Enabled**; development ve production APNs anahtarları **yok**; development ve production APNs sertifikaları **yok**. Ayar değiştirilmedi. Apple Developer erişimi kullanıcıya soruldu. Özel anahtarlar veya şifreler sohbete, komuta, repoya ya da loglara alınmaz.

Fiziksel iPhone kurulumu kök AGENTS.md gereği `tools/install-v2-iphone.sh`, `iosApp V2`, `DebugV2` ile yapılmalıdır. Başlangıçta bu kurulum dosyası ve yapılandırma mevcut değildi; farklı bir şema veya eski kaynakla telefon kurulumu yapılmaz.

Canlı doğrulama için güncel uygulamayı kullanacak iki öğrenci hesabı gerekir: bildirim izni, yeni mesaj/teklif, teklif yanıtı, ilan onayı/reddi, bildirime dokunma ve hesap çıkışı/değişimi gerçek telefonda kontrol edilmelidir. Başarılı SDK gönderimi gerçek telefonda görünmüş bildirim sayılmaz.

Apple bağlantısı için kullanıcı Apple Developer → Certificates, Identifiers & Profiles → Keys bölümünden APNs anahtarını kendi hesabında hazırlamalı ve Firebase Console → Project settings → Cloud Messaging → `com.good4.iosApp` → APNs Authentication Key bölümüne kendisi yüklemelidir. Anahtar oluşturma ve özel anahtar giriş/yükleme adımlarında kullanıcı kontrolü gerekir; özel anahtar içeriği Codex'e veya Claude'a verilmez. Ayrıntılı resmi kurulum: https://firebase.google.com/docs/cloud-messaging/ios/client.

### Apple hesabı kontrolü — 3 Ekim 2026

Kullanıcının Apple bağlantısını tamamlama isteği üzerine Apple Developer'daki açık hesap kontrol edildi. Açık hesap **AHMETCAN KILINC**, Team ID **5N68564396**. Bu hesabın uygulama listesinde `com.cankilinc.good4.staging`, `com.good4.iosApp.test.cankilinc` ve `com.good4.iosApp.v2` bulunuyor; `com.good4.iosApp` görünmüyor. Keys listesi boş.

Güncel kaynak projenin `iosApp Prod` / Release imzalama ekibi **NM79R577GW**, paket kimliği **com.good4.iosApp**. Bu nedenle açık hesapta yeni anahtar oluşturulmadı, Firebase'e anahtar yüklenmedi ve proje imzalama ekibi değiştirilmedi. Kullanıcıdan NM79R577GW ekibine erişen Apple Developer hesabıyla giriş yapması istendi. Doğru uygulama/ekip eşleşmesi görülmeden bağlantı tamamlanmış sayılmaz. Apple Developer sekmesi giriş için açık bırakıldı; herhangi bir şifre veya özel anahtar okunmadı.

Kullanıcı bu konuyu **4 Ekim 2026'da** incelemek üzere erteledi. Sonraki adım: önce doğru Apple ekibi ve uygulama kimliği eşleşmesini kesinleştirmek, ardından uygun APNs anahtarını Firebase'e bağlamak ve gerçek iPhone'da teslimatı doğrulamak. Otomatik hatırlatma oluşturulmadı.

## Kapsam ayrımı

Bu bildirim çalışması mesaj limitini, demo ilanları, ana giriş akışını veya e-posta gönderim yöntemini değiştirmez. Claude'un eşzamanlı mesaj limiti değişiklikleri bildirim çalışmasının dosya listesine eklenmemelidir. Mevcut kullanıcı/Claude değişiklikleri korunur.
