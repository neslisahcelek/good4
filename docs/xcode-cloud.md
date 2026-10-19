# Good4 Xcode Cloud Kurulum ve Bağlantı Rehberi

Bu belge, Good4 Kotlin Multiplatform (KMP) iOS uygulamasının **Xcode Cloud** üzerinde otomatik derlenmesi, arşivlenmesi ve TestFlight / App Store dağıtımına bağlanması için gereken tüm adımları ve yapılandırmaları içerir.

---

## 1. Mimari ve Hazırlıklar

Xcode Cloud standart sanal makineleri (macOS runners) varsayılan olarak **Java (JDK)** içermez. Good4 projesi Kotlin Multiplatform ve Compose Multiplatform kullandığından, iOS arşivleme sırasında Gradle (`embedAndSignAppleFrameworkForXcode`) çalıştırılır.

Bu durum için projede şu altyapı hazırlanmıştır:
1. **`ci_scripts/ci_post_clone.sh`**:
   - Xcode Cloud runner'ında OpenJDK 17'yi otomatik olarak Adoptium üzerinden indirir ve yapılandırır.
   - Gradle için `JAVA_HOME` ve `~/.gradle/gradle.properties` ayarlarını yapar.
   - `./gradlew :composeApp:compileKotlinIosArm64` ile Kotlin derleyicisini önceden ısıtır.
   - `FIREBASE_SOURCE_FIRESTORE=1` ile Swift Package Manager bağımlılıklarını kilitli sürümlerle çözer.
2. **`ci_scripts/ci_pre_xcodebuild.sh`**:
   - Gizli olan `GoogleService-Info-Prod.plist` dosyasını Xcode Cloud ortam değişkenindeki (`GOOGLE_SERVICE_INFO_PROD_BASE64`) base64 verisinden üretir.
   - Gerekli derleme ortam bayraklarını (`FIREBASE_SOURCE_FIRESTORE=1`) doğrular.
3. **`iosApp/ci_scripts` symlink**:
   - Xcode Cloud'un projeyi `iosApp/` alt dizininde veya repo kökünde aramasından bağımsız olarak betikleri bulmasını sağlar.
4. **Xcode Build Phase (`[Kotlin] Embed and Sign Apple Framework`)**:
   - Derleme esnasında `JAVA_HOME` kaybolsa dahi kurulan JDK'yı otomatik tespit eder.

---

## 2. Xcode Cloud Workflow Oluşturma Adımları

Xcode Cloud bağlantısı iki yolla başlatılabilir: **Xcode IDE** üzerinden veya **App Store Connect** web arayüzünden.

### Seçenek A: Xcode Üzerinden Bağlantı (Önerilen)
1. Xcode uygulamasında projeyi açın: `iosApp/iosApp.xcodeproj`
2. Üst menüden **Product** > **Xcode Cloud** > **Create Workflow...** seçeneğine tıklayın.
3. Good4 (`com.good4.iosApp`) uygulamasını seçin.
4. Açılan sihirbazda bir sonraki adıma geçip **Edit Workflow** butonuna tıklayın.

### Seçenek B: App Store Connect Üzerinden Bağlantı
1. [App Store Connect](https://appstoreconnect.apple.com/)'e giriş yapın.
2. **Uygulamalarım** > **Good4** sayfasına gidin.
3. Üst sekmeden **Xcode Cloud** sekmesini seçin.
4. **Get Started** / **Create Workflow** butonuna tıklayın.

---

## 3. Workflow Ayarları (Adım Adım)

Aşağıdaki yapılandırmayı uygulayın:

### 3.1 General (Genel)
- **Workflow Name**: `Good4 Prod Release` (veya isteğe göre `Good4 TestFlight`)
- **Primary Repository**: Projenin bağlı olduğu GitHub deposu (`good4`)
- **Project or Workspace**: `iosApp/iosApp.xcodeproj`
- **Restrict Editing**: Yalnızca yöneticiler düzenleyebilir olarak seçilebilir.

### 3.2 Start Conditions (Tetikleyiciler)
- **Branch Changes**:
  - **Source Branch**: `main` (veya yayın dalları)
  - **Change Type**: `Push Files`
  - *İsteğe bağlı olarak PR (Pull Request) veya Tag tetikleyicisi de eklenebilir.*

### 3.3 Actions (İşlemler)
1. Mevcut eylemi **Archive** olarak ayarlayın (veya yeni bir eylem ekleyip **Archive** seçin):
   - **Platform**: `iOS`
   - **Scheme**: `iosApp Prod`
   - **Configuration**: `Release`
   - **Deployment Preparation**: `TestFlight and App Store`

### 3.4 Post-Actions (İşlem Sonrası)
- Arşiv başarılı olduğunda otomatik TestFlight grubuna düşmesi için:
  - **Add Post-Action (+)** > **TestFlight Internal Testing** seçin.
  - Test grubunuzu (ör. `Internal Testers`) ekleyin.

---

## 4. Gerekli Ortam Değişkenleri (Environment Variables)

Xcode Cloud panelinde **Environment** sekmesine gidin ve aşağıdaki değişkenleri ekleyin:

| Anahtar (Key) | Değer (Value) | Gizli (Secret) | Açıklama |
| :--- | :--- | :---: | :--- |
| `GOOGLE_SERVICE_INFO_PROD_BASE64` | *`<Base64 Değeri>`* | **Evet (Check)** | Firebase Prod konfigürasyonunun base64 şifreli içeriği. |
| `FIREBASE_SOURCE_FIRESTORE` | `1` | **Hayır** | Firestore ve C++ kütüphanelerinin kaynak kodundan derlenip dSYM üretmesini sağlar. |

> [!IMPORTANT]
> **`GOOGLE_SERVICE_INFO_PROD_BASE64` Değerini Alma:**
> Mac Terminalinizde aşağıdaki komutu çalıştırarak değeri doğrudan panonuza kopyalayabilirsiniz:
> ```bash
> base64 -i iosApp/GoogleService-Info-Prod.plist | pbcopy
> ```
> Ardından Xcode Cloud Environment alanına yapıştırın ve **Secret** kutucuğunu işaretleyin.

---

## 5. GitHub Repository İzinleri (App Store Connect GitHub App)

Eğer deponuz daha önce Xcode Cloud'a bağlanmadıysa:
1. Xcode Cloud kurulumu sırasında Apple, GitHub üzerinde **Apple Xcode Cloud** uygulamasını yetkilendirmenizi isteyecektir.
2. Açılan GitHub sayfasında `good4` deposuna erişim izni verin (**Grant Access**).
3. Yetkilendirme tamamlandıktan sonra Xcode Cloud depoyu otomatik olarak doğrulayacaktır.

---

## 6. İlk Derlemeyi Başlatma ve Doğrulama

1. Workflow kaydedildikten sonra **Start Build** butonuna basarak ilk çalıştırmayı tetikleyin.
2. Derleme adımlarını izleyin:
   - **Clone**: Depo klonlanır.
   - **ci_post_clone**: OpenJDK 17 kurulur, Gradle ısıtılır, SPM bağımlılıkları çözülür.
   - **ci_pre_xcodebuild**: `GoogleService-Info-Prod.plist` üretilir ve ortam doğrulanır.
   - **xcodebuild (Archive)**: KMP framework'ü derlenir, MapLibre & Firestore dSYM'leri eklenir ve `.ipa` arşivi üretilir.
   - **TestFlight**: Arşiv başarıyla App Store Connect / TestFlight'a aktarılır.
