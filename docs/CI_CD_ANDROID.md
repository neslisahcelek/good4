# Good4 Android CI/CD Kurulum Kılavuzu: GitHub Actions & Google Play Console

Bu doküman, Good4 Android uygulamasının `master` branch'ine her push/merge yapıldığında otomatik olarak derlenip imzalanarak **Google Play Console Dahili Test (Internal Testing)** kanalına yüklenmesi için gereken kurulum adımlarını açıklar.

---

## 1. İş Akışı Özeti

- **Tetikleyici:** `master` branch'ine yapılan her `push` veya PR merge işlemi (veya GitHub Actions sekmesinden manuel `Run workflow`).
- **Sürüm Numarası (versionCode):** GitHub Actions derleme numarası (`github.run_number`) baz alınarak her derlemede otomatik olarak artırılır (`21 + run_number`). Elle kodda sürüm artırma zorunluluğu yoktur.
- **İmzalama:** GitHub Secrets'ta saklanan `release.jks` ve şifreler ile otomatik yapılır.
- **Dağıtım:** AAB ve R8/Proguard mapping dosyaları doğrudan Google Play Console Dahili Test kanalına yüklenir ve test kullanıcılarına anında dağıtılır.

---

## 2. GitHub Secrets Yapılandırması

GitHub Repository sayfanızda **Settings > Secrets and variables > Actions > New repository secret** yolunu izleyerek aşağıdaki 5 secret'ı ekleyin:

| Secret Adı | Açıklama | Değer / Nasıl Alınır? |
| :--- | :--- | :--- |
| `ANDROID_KEYSTORE_BASE64` | `keystore/release.jks` dosyasının Base64 metni | Aşağıdaki PowerShell komutuyla üretin |
| `ANDROID_STORE_PASSWORD` | Keystore şifresi | `keystore.properties` içindeki `storePassword` |
| `ANDROID_KEY_ALIAS` | Key alias | `keystore.properties` içindeki `keyAlias` (`good4`) |
| `ANDROID_KEY_PASSWORD` | Key şifresi | `keystore.properties` içindeki `keyPassword` |
| `PLAY_STORE_JSON_KEY` | Google Play API Service Account JSON anahtarı | Bölüm 3'teki adımlarla indirilen dosya içeriği |

### `ANDROID_KEYSTORE_BASE64` Değerini Alma (PowerShell):
Terminalde proje kök dizinindeyken şu komutu çalıştırın ve panoya kopyalanan metni GitHub Secret olarak yapıştırın:
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("keystore/release.jks")) | Set-Clipboard
```
*(Metin doğrudan panoya kopyalanacaktır; `Ctrl+V` ile GitHub'a yapıştırabilirsiniz.)*

---

## 3. Google Play Console & Google Cloud Servis Hesabı Kurulumu

Google Play'e dışarıdan otomatik AAB yükleyebilmek için Google Play Developer API erişimine sahip bir Servis Hesabı (Service Account) oluşturulmalıdır. Bu işlem bir defaya mahsus yapılır:

### Adım 1: Google Play Console'da API Erişimini Açma
1. [Google Play Console](https://play.google.com/console)'a **Hesap Sahibi (Account Owner)** olarak giriş yapın.
2. Sol menünün en altındaki **Geliştirici hesabı (Developer account) > API Erişimi (API Access)** sayfasına gidin.
3. Mevcut bir Google Cloud projesine bağlanmadıysa *"Yeni bir Google Cloud projesi bağla"* deyin (veya mevcut Firebase/Google Cloud projenizi seçin).
4. **"Yeni bir servis hesabı oluştur" (Create new service account)** butonuna tıklayın ve açılan kutudaki **Google Cloud Platform** bağlantısına tıklayın.

### Adım 2: Google Cloud Console'da Servis Hesabı ve JSON Key Oluşturma
1. Google Cloud Console'da açılan Servis Hesapları sayfasında üstteki **"Create Service Account" (Servis Hesabı Oluştur)** butonuna tıklayın.
2. Ad olarak `good4-play-publisher` yazın ve **Create and Continue**'ya tıklayın.
3. Rol (Role) seçimi isteğe bağlıdır, boş bırakıp **Continue** ve ardından **Done** diyebilirsiniz.
4. Oluşturulan servis hesabının sağındaki **üç noktaya (Actions) > Manage keys (Anahtarları yönet)** tıklayın.
5. **Add Key > Create new key > JSON** seçeneğini seçip **Create** deyin.
6. Bilgisayarınıza inen `.json` dosyasını bir metin düzenleyiciyle açın, tüm içeriğini kopyalayıp GitHub'daki `PLAY_STORE_JSON_KEY` secret'ına yapıştırın.

### Adım 3: Google Play Console'da Servis Hesabına İzin Verme
1. Tekrar **Google Play Console > API Erişimi (API Access)** sayfasına dönün ve sayfayı yenileyin.
2. Yeni oluşturduğunuz servis hesabını listede göreceksiniz. Yanındaki **"Erişim İzni Ver" (Grant Access)** butonuna tıklayın.
3. **Uygulama İzinleri (App Permissions):** `Good4` (`com.good4`) uygulamasını seçin.
4. **Hesap İzinleri (Account Permissions):**
   - *Sürümleri yönetme (Manage releases)* altındaki:
     - "Dahili test sürümlerini yönetme" (Manage internal testing releases)
     - "Üretim ve test sürümlerini yönetme" (Manage production and test tracks)
     - "Sürümleri yayınlama" (Release to production, exclude devices, and use app signing)
   - "Uygulama ayrıntılarını ve mağaza girişlerini yönetme" (Edit store listing)
5. **"Kullanıcıyı Davet Et" (Invite user)** butonuna basarak işlemi tamamlayın.

---

## 4. Nasıl Kullanılır?

### Otomatik Dağıtım (Önerilen)
Kodlarınızı test edip tamamladıktan sonra `master` branch'ine push veya merge yapın:
```bash
git checkout master
git merge main # veya git push origin master
git push origin master
```
GitHub Actions otomatik olarak:
1. Projeyi derler.
2. `versionCode` değerini otomatik belirler.
3. İmzalı AAB'yi üretir.
4. Google Play Console Dahili Test kanalına yükler.
5. Dahili test grubundaki kullanıcılara güncelleme anında ulaşır.

### Manuel Çalıştırma (Dry-Run veya Farklı Kanallar)
1. GitHub reponuzda **Actions** sekmesine gidin.
2. Sol menüden **Android Release (Google Play)** seçin.
3. Sağdaki **Run workflow** butonuna tıklayın:
   - **Dağıtım Kanalı:** `internal` (varsayılan), `alpha`, `beta` veya `production`.
   - **Dry Run:** Sadece AAB üretip GitHub'a indirme bağlantısı oluşturmak, Play Store'a yükleme yapmamak için işaretleyin.

---

## 5. Dahili Testten Canlıya (Production) Geçiş

Dahili testte doğruladığınız sürümü mağazada genel kullanıma açmak için tekrar derleme yapmanıza gerek yoktur:
1. [Google Play Console](https://play.google.com/console) > Good4 sayfasına gidin.
2. **Test > Dahili test (Internal testing)** sekmesini açın.
3. Yayındaki sürümün yanındaki **"Sürümü tanıt" (Promote release) > "Üretim" (Production)** seçeneğini seçin.
4. Sürüm notlarını onaylayıp Google incelemesine gönderin.
