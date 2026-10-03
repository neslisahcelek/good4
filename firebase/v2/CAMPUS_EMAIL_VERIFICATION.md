# Kampüs Dolabı: Akdeniz öğrenci e-postası doğrulaması

Yalnızca tam `@ogr.akdeniz.edu.tr` alan adı kabul edilir. `@akdeniz.edu.tr`, başka üniversiteler ve alt alan adları kabul edilmez. Ana Google/Apple girişi korunur; bu ekran yalnızca Kampüs Dolabı içindedir.

## Akış

1. Ana Good4 oturumuyla `beginCampusEmailVerification` çağrılır. Sunucu öğrenci rolünü ve aktif hesabı kontrol eder; 15 dakikalık isteği ana UID'ye bağlar ve yalnızca istek kimliğinin özetini saklar.
2. Ayrı, adlandırılmış Firebase Auth oturumu Firebase'in kendi e-posta bağlantısını okul adresine gönderir. SMTP, Gmail uygulama şifresi ve `mail` koleksiyonu kullanılmaz.
3. Yeni mobil sürüm, mobil uygulama kimliği eklemeden bağlantı gönderir (iOS SDK'sının varsayılan `iOSBundleID` değeri açıkça `nil` yapılır). Yeni postadaki bağlantı tarayıcıdaki kısa onay sayfasını açar. Eski postalar için yerel bağlantı işleyicisi korunur.
4. Tarayıcı `requestId` ve `oobCode` değerlerini yalnızca bellekte ayrıştırır, adres çubuğunu temizler ve `completeCampusEmailVerificationFromBrowser` çağrısına HTTPS POST ile iletir. Kullanıcıdan giriş, e-posta tekrarı veya bağlantı kopyalama istenmez. Sayfa kendi ana/okul Auth oturumu açmaz; yönetim paneli oturumunu kullanmaz.
5. Sunucu ana UID ve okul adresini tarayıcıdan almaz: rastgele istek kimliğinin SHA-256 özetiyle bekleyen isteği bulur. Firebase'in `accounts:signInWithEmailLink` API'sinde tek kullanımlık okul kanıtını tüketir. Bu çağrıda ana hesabın `idToken` değeri kesinlikle kullanılmaz. Admin SDK kanıtı yeniden doğrular; alan adı, süre, yeni Auth oturumu, aktif öğrenci rolü, istek bağı ve adresin başka hesaba ait olmaması kontrol edilir. `requestId` tek başına doğrulayamaz.
6. Ana `users/{uid}` belgesinde `eduEmail` ve `eduVerified` güncellenir. Ana Auth e-postası ve sağlayıcıları değişmez. Yalnızca başarılı, aynı istek + aynı okul kodu için SHA-256 makbuzu tutulur; kod/token saklanmaz. Aynı postaya yeniden tıklamak doğrulama başarılıysa sonucu tekrar gösterir. Geçici okul Auth kimliği güvenli koşullarla temizlenir.
7. Sunucu başarı döndürdükten sonra sayfada yalnızca yeşil tik, **“E-postan onaylandı”** ve **“Good4 uygulamasına geri dönebilirsin.”** görünür. Hatalı veya süresi dolmuş kanıtta başarı gösterilmez.
8. Uygulama ön plana gelince ana oturumla `getCampusEmailVerificationStatus` çağrısını yapar. Kampüs Dolabı açık ve istek bekliyorsa 5 saniyede bir kontrol eder; arka planda kontrolü durdurur. Başarıda yerel istek temizlenir, doğrulama penceresi kapanır ve ilan listesi yenilenir. Yapıştırma alanı ve öğrenciye gösterilen Firebase açıklamaları kaldırıldı.

Bu yeni tarayıcı tamamlama akışı 3 Ekim 2026 tarihinde kullanıcının açık onayıyla canlıya yayınlandı. Aşağıdaki eski kayıtlar önceki aşamaları anlatır; en sondaki yayın kaydı güncel durumdur.

İlan oluşturma, mesaj ve teklif işlemleri yalnızca bu tam alan adına ait doğrulanmış `eduEmail` ile çalışır. Doğrulanmamış kullanıcılar ilanlara göz atabilir. Eski `.edu.tr` kod doğrulaması ve diğer özellikler korunur.

## Canlıya alma

3 Ekim 2026'da kullanıcı onayıyla `good4tr-v2` projesine yayınlandı. Aşağıdaki 21 işlev `ACTIVE`, Kampüs Dolabı'na ait 9 indeks `READY` durumunda doğrulandı.

1. Authentication'da **Email link (passwordless sign-in)** etkinleştirildi. Yalnızca `signIn.email.passwordRequired` alanı güncellendi. Email/Password ve mevcut Google/Apple sağlayıcıları korunur. API, varsayılan `false` değerini yanıtta atlayabilir. E-posta gönderim yöntemi `DEFAULT`, yani Firebase'in kendi posta sistemi olarak doğrulandı.
2. Aşağıdaki 21 Functions hedefi `europe-west1` bölgesinde yayınlandı (18 Kampüs Dolabı işlevi, 2 doğrulama işlevi ve doğrulama isteğini hesap silmede temizleyen `deleteMyAccount`):

```sh
cd "/Users/cankilinc/Desktop/Good4/Good4 dev/firebase/v2"
npx firebase deploy --project good4tr-v2 --only functions:beginCampusEmailVerification,functions:completeCampusEmailVerification,functions:acceptMarketTerms,functions:createMarketListing,functions:updateMarketListingStatus,functions:sendMarketMessage,functions:respondMarketOffer,functions:markMarketConversationRead,functions:blockMarketUser,functions:reportMarketContent,functions:getMarketSummary,functions:getMarketFeed,functions:getMarketListing,functions:listMyMarketListings,functions:listMarketConversations,functions:getMarketMessages,functions:listMarketModerationQueue,functions:reviewMarketListing,functions:resolveMarketReport,functions:getMarketReportConversation,functions:deleteMyAccount
```

3. Kampüs Dolabı'na ait 9 indeks Firestore Admin API üzerinden oluşturuldu: 7 `marketListings`, 1 `marketConversations`, 1 `marketReports`. Hepsi `READY` durumunda doğrulandı. Mevcut indeksler silinmedi; `firestore.indexes.json` dosyasının tamamı yayınlanmadı.
4. Mobil sürümde Hosting bağlantılarını açacak ayarlar bulunmalıdır: `good4tr-v2.firebaseapp.com` için iOS Associated Domains ve Android App Links. Kod bunları ekler. Android Firebase Auth 23.2.0 ve iOS Firebase Auth 12.9.0 kullanılır. `ActionCodeSettings.linkDomain` verilmez; Firebase varsayılan Hosting alan adını seçer. Varsayılan `firebaseapp.com` alan adı özel `linkDomain` olarak verilirse Firebase `INVALID_HOSTING_LINK_DOMAIN` hatası döndürür. Hosting'in kayıtlı uygulama kimliğiyle mobil paket kimliği eşleşmelidir. Canlı ilişkilendirme dosyaları iOS `NM79R577GW.com.good4.iosApp` ve Android `com.good4` için mevcut. Bu çalışma sırasında iPhone'a uygulama kurulmadı.
5. Gerçek öğrenci hesabında Google/Apple ile giriş yapılır, Kampüs Dolabı'ndan okul adresine bağlantı gönderilir ve açılır. Aynı Good4 UID ve ana e-posta korunmalı; `eduVerified: true` ve doğru `eduEmail` oluşmalı; ilan/mesaj/teklif açılmalıdır.

`firebase.json`, Firestore kuralları, `firestore.indexes.json` ve Hosting bu çalışma sırasında değiştirilmedi. SMTP eklentisi kurulmadı. Commit, push ve stage yapılmadı.

## Doğrulama

- Auth + Firestore emülatöründe 42 sunucu testi geçti. Bunlardan biri Firebase istemci SDK'sıyla gerçek emülatör e-posta bağlantısını ayrı Auth oturumunda tüketir ve üretimde kullanılan Admin token doğrulayıcısını çağırır.
- Firestore erişim kurallarına ait 20 test geçti; doğrulama belgeleri istemciden okunamaz/yazılamaz.
- Android V2 derlemesi ve 27 ortak birim testi geçti. Ortak iOS kodu ve tam iOS simülatör derlemesi geçti.
- Canlı 21 uç noktanın tamamı oturumsuz istekleri `401 UNAUTHENTICATED` ile reddetti.
- Yayın sonrası Authentication kontrolünde Google ve Apple sağlayıcıları etkin kaldı; e-posta bağlantısı etkin ve gönderim yöntemi `DEFAULT` olarak doğrulandı.
- Canlı Firebase'de iOS ve Android için e-posta bağlantısı oluşturuldu ve uygulama ayrıştırıcısının beklediği alan adı, işlem tipi ve istek kimliği doğrulandı. `returnOobLink: true` kullanıldığı için bu kontrol e-posta göndermedi; bağlantılar tüketilmedi ve saklanmadı.
- Gerçek Akdeniz posta kutusuna teslimat ve fiziksel cihazda bağlantının açılması henüz denenmedi.

Firebase belgeleri: [iOS e-posta bağlantısı](https://firebase.google.com/docs/auth/ios/email-link-auth), [Android e-posta bağlantısı](https://firebase.google.com/docs/auth/android/email-link-auth), [sunucuda ID token doğrulaması](https://firebase.google.com/docs/auth/admin/verify-id-tokens).

## 3 Ekim 2026: dil ve bağlantı sorunlarının düzeltmesi

- Canlı Auth yapılandırmasında yalnızca `notification.defaultLocale` alanı `en` → `tr` olarak güncellendi. `signIn` ayarları aynı kaldı; Google, Apple ve e-posta bağlantısıyla giriş açık olarak tekrar doğrulandı. Gönderim dili ayrıca yalnızca okul doğrulamasının ayrı Android/iOS Auth örneğinde `tr` olarak ayarlandı.
- Firebase Console'da herkese açık proje adı zaten `Good4`; değiştirilmedi. Firebase'in varsayılan oturum açma postası tam bir Kampüs Dolabı tasarımına çevrilmedi. Tamamen özel konu/gövde için kendi posta gönderim hizmeti ve Admin SDK ile üretilen bağlantı gerekir.
- Normal öğrenci akışı: okul adresini gir → e-postadaki düğmeye dokun → uygulamada doğrulama tamamlansın. Bağlantı yapıştırma yalnızca uygulama açılmazsa kullanılan yedek yöntemdir; gönderim öncesinde gösterilmez. Bekleyen yerel istek yoksa veya bağlantı son isteğe ait değilse açıklayıcı hata gösterilir. Başka cihaz ve eski bağlantı aynı uyuşmazlığı yaratabildiği için hata mesajı bunlardan birini kesin neden olarak iddia etmez.
- Mobil ayrıştırıcı, tam Auth kanıtını (`mode=signIn`, `oobCode`, `apiKey`, geçerli ve eşleşen `requestId`) taşıyan açılış URL'lerini de kabul eder. Yalnızca `requestId` taşıyan bir sayfa adresi doğrulama kanıtı değildir. Yabancı alan adları, yinelenen kritik parametreler ve aşırı iç içe bağlantılar reddedilir.
- Web sayfası, tam Auth bağlantısı varsa kullanıcı dokunuşuyla kopyalamaya izin verir; yoksa e-postadaki asıl düğmenin bağlantısını kopyalamayı açıklar. Parametreler görünür sayfa içeriğine, uygulama depolamasına veya loglara yazılmaz; ek bir sunucu çağrısıyla iletilmez. Bu sayfa bilgisayarda doğrulamayı kendi başına tamamlamaz.
- Auth kimliği temizliği başarıyla tamamlanan Firestore işleminin ardından yapılır. Yalnızca ana UID'den farklı, Good4 profili olmayan, adresi kanıtla eşleşen ve tek sağlayıcısı `password` olan okul kimliği silinir. Firebase'de e-posta bağlantısı bu sağlayıcı adıyla temsil edilir. Temizlik başarısız olsa da doğrulama geri alınmaz; yalnızca hata kategorisi loglanır. Bu fonksiyon değişikliği canlıya yayınlanmadı.
- Canlı AASA dosyasında `NM79R577GW.com.good4.iosApp` ve `/__/auth/links` yolunu kapsayan kural var. Android `assetlinks.json`, `com.good4` paketini ve Console'daki iki SHA-256 izini içeriyor; yerel debug sertifikasının izi bunlardan biriyle eşleşiyor. Bu dosyaların doğruluğu fiziksel cihazda bağlantının açıldığını tek başına kanıtlamaz.
- Web derlemesi ve 6 bağlantı testi, iOS/Android Kotlin derlemeleri ve 29 mobil test geçti. Auth + Firestore emülatöründe 29 sunucu testi geçti; gerçek emülatör e-posta bağlantısı tüketme ve geçici Auth kimliğinin silinmesi de dahil. 1 günlük Hosting önizlemesi: https://good4tr-v2--campus-link-iof2pak0.web.app/campus-email-verification (4 Ekim'de sona erer). Önizleme alan adı Auth yetkili alan adlarına eklenmedi.
- Tam `iosApp Prod` / Release / arm64 simülatör derlemesi de geçti ve yeni sürüm iPhone 17 Pro simülatörüne kuruldu. Telefonun kendisine kurulum yapılmadı.
- Canlı test öncesinde ana hesabın e-postası `a***@gmail.com`, sağlayıcısı `google.com`, `eduVerified` değeri `false`; eski okul isteği süresi dolmuş ve tüketilmemiş olarak okundu. Simülatör okul adresi giriş ekranında bırakıldı; otomasyonla alan doldurulamadığı için yeni gerçek e-posta gönderildiği iddia edilmiyor. Kullanıcıdan kendi okul adresiyle gönderip posta dilini kontrol etmesi istendi. Gerçek yeni postanın açılması ve doğrulama sonucu henüz bekleniyor. Gerçek bağlantının parametre adları bu test gerçekleşmeden doğrulanmış sayılmaz.

Göndereni `good4tr.com` yapmak için kullanıcı Firebase Authentication → Templates → Customize domain ekranında alan adını seçmeli, verilen TXT/CNAME kayıtlarını DNS sağlayıcısına aynen eklemeli ve Firebase'in doğrulamayı tamamlamasını beklemeli. Kayıt değerleri Console'dan alınmalıdır; tahmini değer kullanılmaz. Bu işlemde DNS ve alan adı ayarları değiştirilmedi.

Daha sade bilgisayar akışı için ayrı öneri: tarayıcı okul adresinin sahipliğini doğrulasın, sonuç ana Good4 hesabının bekleyen isteğine sunucuda bağlansın ve uygulama geri dönüldüğünde sonucu alsın. Bunun için hesap bağı, süre, tek kullanım ve açık kullanıcı onayı korunmalı; bağlantıya yalnızca GET isteği geldi diye hesap doğrulanmamalı (posta güvenlik tarayıcıları bağlantıları açabilir). Bu ek sunucu/web akışı bu düzeltmede uygulanmadı.


## 3 Ekim 2026: tek tıkla tarayıcı onayı — yayına hazır, henüz canlı değil

Önceki düzeltme bölümündeki kopyala/yapıştır yaklaşımı bu yeni sürümde kaldırıldı. Yeni kullanıcı isteğiyle tarayıcı gerçekten onayı tamamlıyor; yalnızca başarı yazısı göstermiyor.

- Kullanıcının açtığı gerçek sayfada şu parametre adları doğrulandı: `requestId`, `apiKey`, `oobCode`, `mode`, `lang`. Değerleri rapora, dosyaya veya loga yazılmadı.
- Yeni web yönlendirmesinin canlı Firebase tarafından üretilen bağlantı biçimi, posta gönderilmeden ve kullanıcı oluşturulmadan kontrol edildi: `/__/auth/action` yolu, `apiKey`, `mode`, `oobCode`, `continueUrl`, `lang` parametreleri. Sunucunun kullandığı genel API anahtarıyla yapılan geçersiz kanıt kontrolü `INVALID_OOB_CODE` döndürdü; erişim kısıtıyla engellenmedi. Hiçbir Auth/anahtar ayarı değiştirilmedi.
- Öğrenci ekranı: “İlan vermek için okul e-postanı doğrula.” Gönderim sonrası: “Outlook uygulamanı kontrol et. Doğrulamadan sonra ilan verebilirsin.”
- Yeni tarayıcı uç noktası ana Good4 oturumu istemez; iki bağımsız kanıtı sunucuda kontrol eder. Durum uç noktası yalnızca giriş yapmış öğrencinin kendi durumunu döndürür. Bekleyen ve tamamlanan istek sorguları tek alanla yapılır; yeni birleşik indeks veya kural gerekmez.
- Açılış sayfasında `Referrer-Policy: no-referrer` hazırlanmıştır. URL kodları sayfa içeriğine, tarayıcı deposuna ve uygulama loguna yazılmaz. Sunucuya yalnızca doğrulamayı tamamlamak için gönderilir.
- Auth + Firestore emülatöründe 37 sunucu testi geçti. Yeni testler tarayıcıda onay, requestId tek başına yetersizliği, sahte/yanlış adresli kanıt, süre, pasif hesap, eski istek, adres sahipliği, tekrar tıklama ve gerçek Firebase REST kanıt tüketimini kapsar.
- Web derlemesi ve 10 test geçti; yinelenen React etkileri kodu iki kere tüketmez, sunucu onayı olmadan başarı görünmez. Son Kotlin iOS/Android derlemeleri ve 29 mobil test de geçti. Tam `iosApp Prod` / Release / arm64 iOS simülatör derlemesi başarılı; yeni sürüm iPhone 17 Pro simülatörüne kuruldu. Fiziksel iPhone'a kurulum yapılmadı.
- Ayrı HTTP uçtan uca emülatör testi geçti: ana Google hesabıyla istek → tarayıcının kullandığı ayrıştırıcı ve işlem → oturumsuz gerçek callable HTTP çağrısı → Firebase Auth kanıtı → doğru hesaba onay → ana oturumla durum sorgusu. Ana e-posta/Google sağlayıcısı korundu, geçici okul kimliği silindi. Canlı öğrenci hesabında başarı henüz iddia edilmiyor.

Yalnızca bu doğrulama için bekleyen yayın hedefleri:

```sh
cd "/Users/cankilinc/Desktop/Good4/Good4 dev/firebase/v2"
npx firebase deploy --project good4tr-v2 --only functions:completeCampusEmailVerification,functions:completeCampusEmailVerificationFromBrowser,functions:getCampusEmailVerificationStatus,hosting
```

Yeni web derlemesi 1 günlük `campus-link` önizlemesine gönderildi; HTTP 200, güncel derleme ve `Referrer-Policy: no-referrer` doğrulandı.

Hosting önizlemesi yeni sunucu işlevlerini yayınlamaz. Gerçek postayla yeni akışın çalışması için bu üç işlev ve Hosting birlikte yayına alınmalı, yeni mobil sürüm kurulmalı, ardından yeni doğrulama postası gönderilmelidir. Eski onaylı başlangıç işlevi (`beginCampusEmailVerification`) değişmedi. Başka işlevler, kurallar, indeksler, DNS veya Console ayarları bu yayının kapsamına dahil değildir.

Önerilen posta konusu: **Good4 | Okul e-postanı doğrula**. Gövde: “Kampüs Dolabı'nda ilan vermek için okul e-postanı doğrula.” Düğme: **E-postamı doğrula**. Son satır: “Doğrulama tamamlanınca Good4 uygulamasına geri dönebilirsin.” Bu metin öneridir; Firebase'in hazır oturum açma e-postası tam özel gövdeye dönüştürülmedi. Tam Good4 şablonu için ayrı posta gönderimi gerekir; mevcut tek tıkla onay işlevi Firebase'in kendi gönderimiyle çalışacak şekilde hazırlanmıştır.

Bu çalışmada commit, push veya stage yapılmadı. Canlı Hosting/fonksiyon yayını yapılmadı. Gelen gerçek postanın Türkçe olduğu kullanıcı ekran görüntüsünden doğrulandı.

Bu son sadeleştirmede değişen dosyalar:
- `functions/src/campusEmailVerification.ts`, `functions/src/campusEmailVerification.test.ts`, `functions/src/index.ts`
- `web/src/CampusEmailLinkPage.tsx`, `web/src/campusEmailLink.ts`, `web/campusEmailLink.test.mjs`, `web/src/styles.css`
- `firebase.json` (yalnızca doğrulama yolunun yönlendiren adresi paylaşmaması için ek başlık)
- `composeApp/src/commonMain/kotlin/com/good4/campuscloset/CampusEmailVerification.kt`, `CampusEmailVerificationViewModel.kt`, `CampusEmailVerificationCard.kt`, `CampusClosetScreen.kt`
- `composeApp/src/androidMain/kotlin/com/good4/campuscloset/CampusEmailVerification.android.kt`
- `iosApp/iosApp/IOSApp.swift` (yalnızca bu e-postanın bağlantı ayarı)
- Bu belge.


Kullanıcı isteğiyle eklenen hatırlatma: doğrulama penceresinde gönderim öncesinde ve sonrasında “Doğrulama e-postana Outlook üzerinden ulaşabilirsin. Gereksiz E-posta/Spam klasörünü kontrol etmeyi unutma.” gösterilir. Gönderim sonrasında ayrıca “Doğrulamadan sonra ilan verebilirsin.” yazılır. Bu son metin güncellemesinin iOS/Android Kotlin derlemeleri geçti.


Canlıya almadan görmek için yerel etkileşimli önizleme hazırlandı: `http://127.0.0.1:4182/`. Test düğmesi, üretim onay sayfasının ayrı geçici kopyasında gerçek Auth + Firestore + Functions emülatör akışını tamamlar. Gerçek hesaba ve posta kutusuna dokunmaz; demo kaynakları `/tmp/good4-campus-browser-demo` içindedir, repo veya canlı Hosting'e eklenmedi. Test hesabında onay ve Akdeniz alan adı sunucudan ayrıca doğrulandı. Onay ekranı görüntüsü çalışma görsellerine kaydedildi.

Outlook/Spam metnini içeren son `iosApp Prod` Release derlemesi de başarılı oldu; yeni sürüm iPhone 17 Pro simülatörüne kuruldu. Canlı yayın yapılmadı.


## 3 Ekim 2026: tek tıkla tarayıcı onayı canlıya yayınlandı

Kullanıcı “tamam canlıya almadan önce şu UI'ı düzelt sonra alabilirsin” diyerek yayını onayladı. Yerel önizlemedeki “Test doğrulamasını aç” düğmesinin üst kenara kayan metni ortalandı ve yüksekliği 44 px yapıldı. Bu demo `/tmp` altında kalır; test ekranı ve emülatör kanıtları canlıya eklenmedi.

Yayınlanan üç işlevin tamamı `europe-west1` bölgesinde **ACTIVE**:
- `completeCampusEmailVerification`
- `completeCampusEmailVerificationFromBrowser`
- `getCampusEmailVerificationStatus`

Hosting onay sayfası ayrı `campus-email-verification.html` girişiyle yayınlandı. Yeni giriş `src/campus-email-main.tsx` ve yalnızca bu sayfanın `src/campusEmailPage.css` dosyasını kullanır. `vite.config.ts` çoklu girişi derler. Eski, yok sayılan `vite.config.js` dosyasının öncelik almasını önlemek için `package.json` içindeki build komutu açıkça `--config vite.config.ts` kullanır. `firebase.json` onay yolunu bu bağımsız girişe yönlendirir.

Canlı yönetim paneli ve diğer sayfalar korunarak yayın yapıldı: önceki canlı sürümün `index.html`, üç mevcut JS/CSS dosyası, logolar ve yönlendirme dosyaları alınarak yeni onay sayfasının üretim çıktısı eklendi. Önceki uygulama dosyalarının tamamının Hosting içerik özetleri yayından sonra birebir aynı olarak doğrulandı. Firebase'in yönettiği `/__/firebase/init.*` dosyaları CLI tarafından yeniden oluşturulur. Yerel çalışma ağacındaki başka panel değişiklikleri bu yayına alınmadı.

Kullanılan yayınlar:

```sh
cd "/Users/cankilinc/Desktop/Good4/Good4 dev/firebase/v2"
npx firebase deploy --project good4tr-v2 --only functions:completeCampusEmailVerification,functions:completeCampusEmailVerificationFromBrowser,functions:getCampusEmailVerificationStatus --non-interactive
npx firebase deploy --project good4tr-v2 --only hosting --public /tmp/good4-campus-live-release --non-interactive
```

Hosting canlı sürümü: `62316d0977f03ce4`. `https://good4tr-v2.firebaseapp.com/campus-email-verification` HTTP 200 verir; bağımsız üretim dosyasıyla eşleşir ve `Referrer-Policy: no-referrer` başlığını taşır. Sadece istek kimliğiyle onay denemesi canlıda HTTP 400 `INVALID_ARGUMENT`; oturumsuz kendi durumunu okuma denemesi HTTP 401 `UNAUTHENTICATED` döndürdü.

Son web derlemesi ve 10 bağlantı testi geçti. Önceden tamamlanan 37 sunucu testi, gerçek callable HTTP emülatör testi, 29 mobil test ve iOS/Android derlemeleri geçerli. Güncel mobil kopya ve bağlantı ayarları iPhone 17 Pro simülatörüne kurulmuş durumdadır; fiziksel iPhone veya uygulama mağazası için yeni sürüm yayınlanmadı.

Canlı öğrenci hesabında yeni postaya tıklayıp `eduVerified` sonucunu kontrol etme adımı kullanıcıdan istendi. Bu adım tamamlanmadan gerçek öğrenci hesabında başarı iddia edilmiyor. Ana Google/Apple hesabı değiştirilmez. Postanın hazır oturum açma metni bu yayın kapsamında özel gövdeye çevrilmedi; önceki özel metin önerisi hâlâ öneri olarak kalır.

Başka işlev, kural, indeks, DNS veya SMTP eklentisi yayınlanmadı. Commit, push ve stage yapılmadı.

Yayın sonrası gerçek öğrenci hesabı salt okunur olarak kontrol edildi: `eduVerified: false`, henüz okul adresi yok. Başlangıç kaydıyla karşılaştırmada ana Auth e-postasının SHA-256 özeti ve Google sağlayıcısı aynı. Yeni postaya kullanıcı dokunuşu henüz tamamlanmadı; bu yüzden canlı öğrenci doğrulama başarısı raporlanmıyor. Son yerel test onayı kısa üretim görünümüyle tekrar başarılı oldu.


## 3 Ekim 2026: gerçek öğrenci doğrulaması başarılı ve kural ekranı sadeleştirildi

Kullanıcı yeni postadaki bağlantıyı açıp doğrulamanın tamamlandığını bildirdi. Ardından canlı Firestore/Auth kayıtları salt okunur kontrol edildi: `eduVerified: true`, `eduEmail` alanının tam alan adı `ogr.akdeniz.edu.tr`. Başlangıç kaydıyla karşılaştırıldığında ana e-postanın özeti ve Google sağlayıcısı aynı kaldı. Gerçek okul bağlantısı/kodu okunmadı veya raporlanmadı.

Kullanıcının sonraki UI isteği üzerine `CampusClosetComponents.kt` içindeki kural başlığından tokmak simgesi ve buna ait boşluk kaldırıldı. “Buluşmalarını kampüs içinde…” ile başlayan madde kaldırıldı. Diğer kurallar ve kabul işlemi korundu. Bu mobil değişiklik için yeni simülatör derlemesi başlatıldı; yeni sunucu/Hosting yayını gerekmiyor.

Kullanıcı ilk e-posta tıklamasında “açılır pencere engellendi” gördüğünü, ikinci tıklamada işlemin tamamlandığını bildirdi. Good4 onay sayfası kodunda `window.open` kullanılmıyor. Microsoft, Outlook web'de açılır pencere engelleyicilerinin sorun oluşturabildiğini belgeliyor: https://support.microsoft.com/en-us/outlook/supported-browsers-for-outlook-on-the-web-and-outlook-com . Bu yüzden olayın Outlook/tarayıcı tarafında olması olası; ilk uyarı yeniden üretilmedi ve kesin neden olarak raporlanmıyor. Tarayıcının engelleme ayarları değiştirilmedi.


Kullanıcının ek isteğiyle okul adresi giriş kutusu sadeleştirildi: düzenlenebilir değer yalnızca öğrenci numarasıdır; `@ogr.akdeniz.edu.tr` ayrı, her zaman görünür bir metin olarak kutunun sağında durur. Bu metin düzenlenmez, arkasına imleç konamaz; dokunulunca numara alanı odaklanır. Sayısal klavye açılır, baştaki sıfırlar korunur. Uygulama gönderirken numaraya sabit uzantıyı ekler. Aynı Akdeniz adresinin tamamı yapıştırılırsa numara kısmı alınır; yabancı alan adı yapıştırılması sessizce başka bir alıcıya dönüştürülmez. Sunucu doğrulaması ve ana Auth hesabı değişmedi.

İlk simülatör derlemesi sistemin Java başlatıcısında beklediği için durduruldu. Yeniden derleme mevcut JBR 21 yolu açıkça verilerek başlatıldı; kaynak veya proje derleme ayarları bu nedenle değiştirilmedi.

Son mobil değişikliklerin tam `iosApp Prod` / Release / arm64 simülatör derlemesi başarılı oldu. Ürün paketinin `com.good4.iosApp` ve bağlı projenin `good4tr-v2` olduğu doğrulandı; güncel uygulama iPhone 17 Pro simülatörüne kuruldu ve başlatıldı. Android V2 Kotlin derleme kontrolü de 27 saniyede başarılı tamamlandı. Fiziksel telefona kurulum veya mağaza yayını yapılmadı. Bu değişiklikler için ek sunucu/Hosting yayını, commit, push veya stage yapılmadı.
