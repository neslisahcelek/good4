# Good4 Web Paneli: App Check, CSP ve Güvenli Hosting Yayını (Agent Görev Belgesi)

> **Bu belgeyi doğrudan bir AI Agent'a (veya geliştiriciye) prompt olarak verebilirsiniz.**
> Hedef: Good4 web panelinin (Admin, İşletme ve Topluluk) son güvenlik güncellemelerini test etmek, Firebase Hosting'e yayınlamak ve canlı alan adında App Check + CSP doğrulamasını kanıtlarıyla tamamlamaktır.

---

## 1. Agent'a Verilecek Görev (Prompt)

```text
Sen Good4 projesinin dağıtım ve güvenlik uzmanısın. Görevin:
1. `firebase/v2/web` klasöründeki testleri çalıştırmak ve üretim derlemesini (build) almak.
2. `firebase/v2/firebase.json` içindeki yeni Content-Security-Policy (CSP) ve güvenlik başlıklarının doğruluğunu kontrol etmek.
3. Yetkili `good4tr-v2` prodüksiyon projesine SADECE Hosting servisini yayınlamak (`firebase deploy --only hosting`). (Functions veya Firestore kuralları deploy edilmeyecektir).
4. Canlı web panelinde gizli pencerede oturum açarak:
   - Tarayıcı konsolunda CSP hatası olmadığını doğrulamak.
   - reCAPTCHA Enterprise / App Check token'ının üretilip üretilmediğini kontrol etmek (`X-Firebase-AppCheck` başlığı).
   - Google Cloud Logs Explorer üzerinden backend doğrulamasının `jsonPayload.verifications.app = "VALID"` olduğunu kanıtlamak.
5. Sonuçları token veya gizli anahtar içermeden raporlamak.
```

---

## 2. Sabit Yapılandırma Bilgileri

| Parametre | Değer |
| :--- | :--- |
| **Üretim Firebase Projesi** | `good4tr-v2` (Proje No: `654697131931`) |
| **Firebase Web App ID** | `1:654697131931:web:ceff2acfa4ff4529603b3e` |
| **App Check Sağlayıcısı** | reCAPTCHA Enterprise (`ReCaptchaEnterpriseProvider`) |
| **reCAPTCHA Site Key** | `6LdG2r0tAAAAAIOzRvZPbhrWE4m-Q4CBQGO-b4zO` (Herkese açık istemci anahtarı) |
| **Functions Bölgesi** | `europe-west1` |
| **Hosting Yayın Klasörü** | `firebase/v2/web/dist` |
| **Hosting Yapılandırması** | `firebase/v2/firebase.json` |

---

## 3. Adım Adım Yürütme Talimatları

### Adım 3.1: Yerel Test ve Üretim Derlemesi
Node.js 22+ kullanarak repo kökünden şu komutları çalıştır:

```bash
cd firebase/v2
npm --prefix web test
npm --prefix web run build
```

- **Beklenen Sonuç:**
  - 4 web testi başarıyla geçer (`pass 4, fail 0`).
  - Vite üretim derlemesi `dist/` klasörünü oluşturur (`campus-email-verification.html`, `index.html`, `assets/*`).
  - `.env` veya derleme parametrelerinde `VITE_USE_FIREBASE_EMULATORS=true` **bulunmamalıdır**.

---

### Adım 3.2: Sadece Hosting Yayını (Deploy)
Yalnızca Hosting servisini prodüksiyona yayınla:

```bash
# Oturum ve proje kontrolü
npx --no-install firebase login:list
npx --no-install firebase use good4tr-v2

# Yalnızca Hosting yayını
npx --no-install firebase deploy --only hosting --project good4tr-v2
```

> [!WARNING]
> Bu aşamada kesinlikle `--only functions` veya `--only firestore` çalıştırma! App Check enforcement mobil ve web doğrulamaları tamamlanana kadar açılmayacaktır.

---

### Adım 3.3: Canlı Tarayıcı Doğrulaması

1. Yayınlanan gerçek web adresini (ör. `https://good4tr-v2.web.app` veya özel alan adı) **Gizli Pencere (Incognito)** modunda aç.
2. Yetkili bir test admin veya işletme hesabı ile giriş yap.
3. Tarayıcı Geliştirici Araçları'nda (**F12**):
   - **Console Sekmesi:** Kırmızı `Content Security Policy (CSP)` engellemesi veya `reCAPTCHA error` olmadığını teyit et.
   - **Network (Ağ) Sekmesi:** Giriş sonrası arka planda tetiklenen ilk Callable fonksiyona (ör. `getPortalContext` veya `getAdminDashboard`) giden isteği incele.
   - İstek başlıklarında (Request Headers) `X-Firebase-AppCheck` başlığının bulunduğunu doğrula (Token değerini kaydetme veya kopyalama).

---

### Adım 3.4: Cloud Logging Doğrulaması (Sunucu Kanıtı)

Google Cloud Console -> **good4tr-v2** projesi -> **Logs Explorer** sayfasına git.
Aşağıdaki sorguyu çalıştır:

```text
resource.type="cloud_run_revision"
labels."firebase-log-type"="callable-request-verification"
jsonPayload.verifications.app="VALID"
```

- **Başarı Kriteri:** Giriş yaptığın dakikada ilgili fonksiyon için `jsonPayload.verifications.app = "VALID"` kaydının oluştuğunu gör.
- `verifications.app = "MISSING"` veya `"INVALID"` ise App Check entegrasyonu tamamlanmış sayılmaz.

---

## 4. Olası Hatalar ve Çözümleri

| Belirti / Hata | Olası Neden | Çözüm |
| :--- | :--- | :--- |
| **CSP İhlali (Refused to load...)** | `firebase.json` CSP politikasında eksik domain | Google reCAPTCHA veya API kaynağını `firebase.json` içindeki `Content-Security-Policy` direktifine ekle ve yeniden deploy et. |
| **App Check 403 / Token alınamıyor** | reCAPTCHA Enterprise izinli alan adları eksik | Cloud Console -> reCAPTCHA -> Site anahtarı ayarlarında panel alan adının (örn. `good4tr-v2.web.app`) kayıtlı olduğunu doğrula. |
| **Headers'da X-Firebase-AppCheck yok** | `initializeAppCheck` servislerden önce çağrılmamış | `firebase/v2/web/src/firebase.ts` içinde `initializeAppCheck`'in `getAuth` ve `getFunctions`'tan önce çalıştığından emin ol. |
| **Backend `app = "MISSING"` dönüyor** | Emülatör flag'i açık kalmış | `.env` dosyasında `VITE_USE_FIREBASE_EMULATORS` değerinin `false` olduğunu doğrula ve web build'i temizleyip tekrar al. |

---

## 5. Agent Teslim Raporu Şablonu

Agent görevi tamamladığında şu formatta rapor sunmalıdır:

```markdown
### Web Dağıtım ve App Check Doğrulama Raporu
- **Tarih / Saat**: YYYY-MM-DD HH:mm:ss
- **Yayınlanan Hosting URL'si**: https://...
- **Vite Build Durumu**: Başarılı (dist/ üretildi)
- **Web Testleri**: 4/4 Passed
- **Tarayıcı CSP Durumu**: 0 Hata / Sorunsuz yüklendi
- **İstemci Header Durumu**: `X-Firebase-AppCheck` isteğe eklendi (Görüldü)
- **Cloud Logging Kanıtı**: `jsonPayload.verifications.app = "VALID"` (Zaman damgası: HH:mm:ss)
- **Mevcut Enforcement Durumu**: Henüz açılmadı (İzleme modunda korundu)
```
