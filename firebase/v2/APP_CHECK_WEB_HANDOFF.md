# Good4 web App Check — geliştirici ve agent devir belgesi

Tarih: 2 Ekim 2026

## Agent'a verilecek görev

Bu belgeyi uygulayarak Good4 işletme/admin/topluluk web panelinin App Check
entegrasyonunu kontrol et, eksikse aşağıdaki değişikliği uygula, test et ve
Firebase Hosting'e yayınla. Ardından gerçek web alan adında token doğrulamasını
kontrol et ve sonucu kanıtlarıyla raporla. Geliştiricinin yayın yetkisini ve
yayınlanacak panel sürümünü doğrula; erişim yoksa gereken erişimi açıkça bildir.
Bu görev App Check zorunluluğunu açmayı veya mobil sürüm yayınlamayı kapsamaz.

## Mevcut durum ve kapsam

- Firebase Console ekranında **Good4 İşletme Paneli → Fraud Defense → Registered**
  görülüyor. Fraud Defense burada reCAPTCHA Enterprise sağlayıcısını ifade eder.
- Android ve iOS App Check kodu hazırlanmış durumda. Gerçek cihazdan başarılı
  doğrulama alındığı bu belgeyle garanti edilmiyor.
- Web değişikliği bu çalışma alanında uygulandı. TypeScript/Vite üretim
  derlemesi ve mevcut dört web testi geçti. Üretim paketinde site anahtarının
  bulunduğu ve geliştirme debug ayarının çıkarıldığı kontrol edildi.
- Bu görev kapsamında Hosting yayını ve canlı web token doğrulaması yapılmadı.
- Web değişiklikleri belge hazırlanırken commit edilmemişti. Başka geliştiricinin
  checkout'unda bulunduğunu varsayma; kaynak kodu kontrol et. Eksikse aşağıdaki
  kodu uygula veya bu dosyalara ait diff'i al.
- Çalışma alanında bildirim ve test ortamı gibi başka değişiklikler de var.
  App Check değişikliğini mevcut kodu koruyarak ekle ve geliştiricinin yayın için
  belirlediği panel sürümünü kullan. Tüm çalışma alanını körlemesine değiştirme.

## Sabit yapılandırma

| Alan | Değer |
| --- | --- |
| Üretim Firebase projesi | `good4tr-v2` |
| Firebase Web App ID | `1:654697131931:web:ceff2acfa4ff4529603b3e` |
| Functions bölgesi | `europe-west1` |
| App Check sağlayıcısı | `ReCaptchaEnterpriseProvider` |
| reCAPTCHA site key | `6LdG2r0tAAAAAIOzRvZPbhrWE4m-Q4CBQGO-b4zO` |
| Hosting yapılandırması | `firebase/v2/firebase.json` |
| Hosting yayın klasörü | `firebase/v2/web/dist` |

Site key herkese açık istemci yapılandırmasıdır. Secret key gerekmez. Firebase
API key, reCAPTCHA site key ve App Check debug token farklı değerlerdir.
Debug token ve kullanıcı oturum token'larını rapora veya Git'e ekleme.

## 1. Kaynak kodunu kontrol et / eksikse uygula

`firebase/v2/web/src/firebase.ts` dosyasında şu import bulunmalı:

```ts
import { initializeAppCheck, ReCaptchaEnterpriseProvider } from "firebase/app-check";
```

Mevcut `firebaseConfig` korunmalı. `initializeApp(firebaseConfig)` sonrasında,
**getAuth / getFunctions / getFirestore çağrılarından önce** şu kurulum olmalı:

```ts
const app = initializeApp(firebaseConfig);
const useFirebaseEmulators = import.meta.env.VITE_USE_FIREBASE_EMULATORS === "true";

if (!useFirebaseEmulators) {
  if (import.meta.env.DEV) {
    self.FIREBASE_APPCHECK_DEBUG_TOKEN = true;
  }
  initializeAppCheck(app, {
    provider: new ReCaptchaEnterpriseProvider("6LdG2r0tAAAAAIOzRvZPbhrWE4m-Q4CBQGO-b4zO"),
    isTokenAutoRefreshEnabled: true,
  });
}

export const auth = getAuth(app);
export const functions = getFunctions(app, "europe-west1");
```

Mevcut Firestore lazy yükleme, Auth persistence ve emülatör bağlantılarını koru.
Emülatör kontrollerinde aynı `useFirebaseEmulators` değerini kullanabilirsin.
Callable isteklerine elle header ekleme: Firebase Functions SDK bunu yapıyor.
Yeni paket gerekmiyor; App Check mevcut `firebase` bağımlılığında var.

`firebase/v2/web/src/vite-env.d.ts` dosyasına şu global tipi ekle:

```ts
interface Window {
  FIREBASE_APPCHECK_DEBUG_TOKEN?: boolean | string;
}
```

Geliştirme modu debug provider kullanır. Tarayıcı konsolundaki debug token,
Firebase → App Check → Web uygulaması → **Manage debug tokens** listesine
eklendikten sonra sayfa yeniden yüklenmelidir. `npm run preview` üretim
paketini sunar; DEV debug provider'ı kullanmaz. Gerçek reCAPTCHA doğrulamasını
localhost üzerinden değerlendirme. Emülatör modunda App Check kurulumu atlanır.

## 2. Console ve alan adlarını kontrol et

1. Firebase Console'da `good4tr-v2` → Security → App Check → Apps yolunu aç.
2. Yukarıdaki Web App ID'nin **Registered** olduğunu ve kullanılan site key'in
   bu belgedeki değerle aynı olduğunu doğrula.
3. Google Cloud Console'da aynı proje → Fraud Defense / reCAPTCHA → ilgili
   anahtarın ayarlarını aç. Anahtar Web türünde ve skor tabanlı olmalı;
   checkbox challenge kullanılmamalı.
4. Panelin gerçekten sunulduğu alan adları anahtarın izinli alan adlarında
   bulunmalı. Kullanılıyorsa `good4tr-v2.web.app`, `good4tr-v2.firebaseapp.com`
   ve gerçek özel panel alan adını doğrula. Özel alan adını tahmin etme.
   Domain alanına protokol veya URL yolu ekleme. Üretim anahtarına localhost ekleme.
5. Mevcut Enforce durumunu kaydet. Bu görev sırasında değiştirme. Zaten açıksa
   eksik token kaynaklı hataları ayrıca raporla.

## 3. Derle ve test et

Node.js 22+ kullan. Aşağıdaki komutları repo kökünden başlat:

```sh
cd firebase/v2
npm ci
npm --prefix web ci
npm --prefix web test
npm --prefix web run build
git diff --check
```

Üretim derlemesinde `VITE_USE_FIREBASE_EMULATORS=true` bulunmamalı. Ortam
değişkenlerini ve varsa `.env*` dosyalarını kontrol et; gizli değerleri çıktıya
dökme. Vite bu ayarı derleme sırasında pakete gömer.

Beklenen sonuç: testler ve build başarılı; `web/dist` yeniden üretilmiş;
site key doğru; uygulamanın `self.FIREBASE_APPCHECK_DEBUG_TOKEN = true`
ataması üretim paketinden çıkarılmış. SDK içindeki debug token değişkenine
ait okuma kodu pakette bulunabilir; bunu debug provider'ın açık olmasıyla
karıştırma. Hata varsa yayından önce gider.

## 4. Yalnızca Hosting'e yayınla

Komutları `firebase/v2` klasöründe çalıştır. Firebase CLI bu klasörün mevcut
bağımlılığıdır. Yetkili geliştiricinin hesabını kullan:

```sh
npx --no-install firebase login
npx --no-install firebase projects:list
npx --no-install firebase hosting:sites:list --project good4tr-v2
```

`firebase.json` tek Hosting yapılandırması içeriyor ve `site` / `target`
belirtmiyor. Dolayısıyla varsayılan Hosting sitesini hedefler. Mevcut panelin
bu siteden sunulduğunu doğrula. Farklı bir site kullanılıyorsa doğru mevcut
yayın yapılandırmasını geliştiriciyle belirle; yeni site oluşturma.

Testler geçtikten ve yayınlanacak panel sürümü doğrulandıktan sonra:

```sh
npx --no-install firebase deploy --only hosting --project good4tr-v2
```

Bu görevde `functions`, `firestore` veya `storage` deploy etme; test projesini
hedefleme. Yayın çıktısındaki gerçek Hosting URL'sini ve sürümü kaydet.

## 5. Canlı doğrulamayı yap

1. Gerçek panel alan adını gizli pencerede aç. Yeni sürümün yüklendiğini kontrol et.
2. Geliştiricinin test için yetkilendirdiği panel hesabıyla giriş yap.
3. Tarayıcı geliştirici araçlarında Console ve Network sekmelerini izle.
   App Check / reCAPTCHA domain veya token hatası olmamalı.
4. Girişten sonra çağrılan `getPortalContext` gibi salt okuma işleminde
   `X-Firebase-AppCheck` header'ı bulunduğunu kontrol et. Token'ın kendisini
   kopyalama veya raporlama.
5. Google Cloud Logging'de `good4tr-v2` projesinin aynı saate ait callable
   doğrulama kayıtlarını kontrol et. Tüm severity seviyelerini dahil et.
   Başarı ölçütü `jsonPayload.verifications.app = "VALID"` olmalı.

   Örnek Logs Explorer filtresi:

   ```text
   labels."firebase-log-type"="callable-request-verification"
   jsonPayload.verifications.app="VALID"
   ```

   `Callable request verification passed` mesajı tek başına yeterli değildir:
   zorunluluk kapalıyken `MISSING` durumu da bu mesajla kaydedilebilir. İlgili
   web işleminin zamanını ve işlevini eşleştir. App Check `MISSING` veya
   `INVALID` ise doğrulama tamamlanmış sayılmaz.
6. Web akışı Firestore isteği yapıyorsa Firebase App Check metriklerinde
   ilgili isteklerin **Verified** olduğunu da kontrol et. Panelin yalnızca
   açılması veya işlemin 200 dönmesi yeterli kanıt değildir.

Giriş için kişisel hesap/şifre veya yeni kullanıcı oluşturma gerekirse
geliştiricinin yetkilendirdiği yöntemi kullan. Kontrol amacıyla gerçek kampanya
oluşturma, kod kullanma, içerik silme veya kullanıcı değiştirme.

## Sorun çıkarsa

| Belirti | Kontrol |
| --- | --- |
| reCAPTCHA domain / invalid site key hatası | Gerçek alan adı, anahtar türü, Firebase kaydındaki anahtar eşleşmesi |
| App Check 403 | Web kaydı, doğru proje ve site key, domain ayarları, debug token kaydı |
| Üretimde debug token mesajı | Yayınlanan paketin DEV/debug ayarını ve eski önbelleği kontrol et |
| Header yok | Yeni paket yüklenmiş mi; App Check hizmetlerden önce başlamış mı; emülatör bayrağı açık mı? |
| Header var, `app=INVALID` | Token backend tarafından kabul edilmemiş; sağlayıcı/proje/anahtar eşleşmesini kontrol et |
| Firebase CLI permission denied | Yayın için Hosting, kurulum için App Check/reCAPTCHA, doğrulama için Logs erişimini geliştirici sağlamalı |

Yayın sonrasında panelde sorun oluşursa Hosting → Release history üzerinden
önceki bilinen çalışan sürüme dön. Yalnızca bir istemci hatasını çözmek için
global App Check veya başka servislerin koruma ayarlarını değiştirme.

## Teslim ölçütleri ve agent raporu

- [ ] App Check kodu doğru site key ile, Firebase hizmetlerinden önce kurulmuş.
- [ ] Otomatik token yenilemesi açık; üretim debug provider kullanmıyor.
- [ ] reCAPTCHA domain ve Firebase Web kayıtları doğrulanmış.
- [ ] Web testleri ve üretim derlemesi geçmiş.
- [ ] Doğru Hosting sitesine yayın yapılmış; URL ve yayın sürümü kaydedilmiş.
- [ ] Canlı giriş/salt okuma akışı başarılı ve header mevcut.
- [ ] İlgili callable işlem için backend `app=VALID` kanıtı görülmüş.
- [ ] Mevcut enforcement ayarları korunmuş.

Agent; değiştirdiği dosyaları, test sonuçlarını, yayın URL'sini/sürümünü,
doğrulama zamanını ve token içermeyen doğrulama kanıtını raporlasın. Erişim
veya canlı doğrulama eksikse bunu açıkça ayırsın; yalnızca Registered durumuna
ve başarılı build'e bakarak tüm entegrasyonu tamamlanmış ilan etmesin.

Android, iOS ve webin desteklenen sürümlerindeki doğrulamalar tamamlandıktan
sonra zorunluluğu açmak ayrı bir görevdir. Callable Functions için kaynakta
`ENFORCE_APP_CHECK=true` ile etkinleştirme ve Functions redeploy gerekir;
Firestore/Storage için ayrı Console ayarları vardır. Bu belge bunları açma
talimatı vermez.

## Resmî kaynaklar

- [Web App Check / reCAPTCHA Enterprise](https://firebase.google.com/docs/app-check/web/recaptcha-enterprise-provider)
- [Web debug provider](https://firebase.google.com/docs/app-check/web/debug-provider)
- [Callable doğrulama metrikleri](https://firebase.google.com/docs/app-check/monitor-functions-metrics)
- [Cloud Functions enforcement](https://firebase.google.com/docs/app-check/cloud-functions)
- [Firebase Hosting yayını](https://firebase.google.com/docs/hosting/quickstart)
