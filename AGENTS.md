# Good4 Proje Talimatları (AI için)

Bu doküman, projede geliştirme yaparken **uyulması gereken genel kurallar**ı ve projeye **yeni bir kullanıcı tipi** (ör. `ADMIN2`, `MODERATOR`, `PARTNER`) eklerken hangi yapıları kullanman gerektiğini özetler. Aynı zamanda ortak bileşenler, tema/renk/strings gibi genel bilgiler içerir.

## 0) Genel geliştirme kuralları
- **Tek kaynak**: Color, string, ikon gibi değerleri mümkün olduğunca tek yerden yönet.
- **Hardcode yok**: UI metinleri `strings.xml` üzerinden alınmalı.
- **Tema uyumu**: Yeni ekranlar `Background / InkBlack / MintGreen / LimeGreen / SoftGray / Surface / BrickRed` paletine uymalı.
- **State tek yönlü**: UI sadece state okur; değişiklikler ViewModel üzerinden yapılır.
- **Hata yönetimi**: UI, `Result.Error`’ı kullanıcıya anlaşılır mesaj olarak göstermeli (snackbar/inline).
- **Null güvenliği**: DTO’dan domain’e mapping’de `?:` fallback kullan.
- **Komponentleşme**: Aynı pattern tekrar ediyorsa ortak komponent çıkar.
- **Preview**: Önemli Compose ekranları için `@Preview` ekle.
- **Navigation**: Route + NavGraph + UserRole değişiklikleri tutarlı olmalı.
- **Koin**: ViewModel injection `koinViewModel()` ile yapılır.
- **Performans**: `LazyColumn` içindeki item’lar `key` ile tanımlanmalı.
- **Tek sorumluluk**: Repository sadece veri getirir, ViewModel sadece UI state yönetir.

## 1) Mimari ve genel akış
- **Platform**: Kotlin Multiplatform + Compose Multiplatform
- **Katmanlar**:
    - `domain`: modeller + iş kuralları
    - `data`: repository + DTO
    - `presentation`: Compose UI + ViewModel + state
- **ViewModel**: `StateFlow` ile UI state yönetimi, `collectAsStateWithLifecycle` kullanılır.
- **DI**: Koin (`koinViewModel()` ile UI’da alınır)

## 2) Yeni kullanıcı tipi ekleme rehberi
Yeni bir kullanıcı tipi eklerken aşağıdaki yapıları aynı kalıpla çoğalt:

### 2.1 Domain
- `composeApp/src/commonMain/kotlin/com/good4/user/domain/UserRole.kt`
    - Yeni role ekle: ör. `ADMIN2("admin2")`

### 2.2 Navigation
- `composeApp/src/commonMain/kotlin/com/good4/navigation/Route.kt`
    - Yeni route ekle: `data object NewUserHome : Route()`
- `composeApp/src/commonMain/kotlin/com/good4/navigation/NavGraph.kt`
    - `composable<Route.NewUserHome>` ekle
    - `navigateToHome` içinde `when (userRole)` güncelle

### 2.3 Home Screen (Alt menü)
- Benzer örnekler:
    - `student/presentation/home/StudentHomeScreen.kt`
    - `business/presentation/home/BusinessHomeScreen.kt`
    - `admin/presentation/home/AdminHomeScreen.kt`
- Yapı: `Scaffold + NavigationBar + when (selectedItemIndex)`
- **Ortak Scaffold/TopBar**: Ekranlar `Good4Scaffold` ve `Good4TopBar` kullanmalı. `Good4Scaffold` AppBackground sağlar; ayrıca containerColor vermeyin. `Good4TopBar` ile tüm ekranlarda tek tip topbar görünümü korunur.

### 2.4 Dashboard / Products / Profile ekranları
- Ekranlar state + viewmodel ile kurulmalı, iş kuralı viewmodelde olmalı.
- Yeni kullanıcı tipine uygun ekranlar şunlardan türetilebilir:
    - Dashboard: `BusinessDashboardScreen`, `AdminDashboardScreen`
    - Products: `BusinessProductsScreen`, `AdminProductsScreen`
    - Profile: `StudentProfileScreen`, `BusinessProfileScreen`, `AdminProfileScreen`

## 3) Ortak bileşenler (tekrar kullan)
Projede ortaklaştırılmış component’ler vardır. Yeni ekran eklerken öncelikle bunları kullan.

### 3.1 Profil ekranı
- `composeApp/src/commonMain/kotlin/com/good4/core/presentation/components/ProfileComponents.kt`
    - `ProfileScreenScaffold`
    - `ProfileInfoCard`
    - `ProfileLogoutButton`
- Yeni kullanıcı tipi için profil ekranı bu iskeletle yapılmalı.

### 3.2 Ürün formu
- `composeApp/src/commonMain/kotlin/com/good4/core/presentation/components/ProductFormFields.kt`
    - Hem admin hem işletme ürün ekleme/düzenleme buradan yapılır.
    - Admin için `showBusinessSelector = true` kullanılır.

### 3.3 Ürün liste kartı
- `composeApp/src/commonMain/kotlin/com/good4/core/presentation/components/ProductListCard.kt`
    - Business + Admin ürün listelerinde kullanılır.

### 3.4 İstatistik kartı
- `composeApp/src/commonMain/kotlin/com/good4/core/presentation/components/StatCard.kt`
    - Dashboard’larda ortak kullanılır.

### 3.5 Görsel önizleme
- `composeApp/src/commonMain/kotlin/com/good4/core/presentation/components/ImagePreviewBox.kt`
    - Campaign/ürün görsel alanları için ortak kullanılır.

## 4) Theme / Colors
- Renkler: `composeApp/src/commonMain/kotlin/com/good4/core/presentation/Colors.kt`
- Kullanılan ana palette:
    - `Background` (ana arkaplan)
    - `InkBlack` (primary text)
    - `MintGreen` / `LimeGreen` (highlight / CTA)
    - `SoftGray` / `Surface` (kart yüzeyleri)
    - `BrickRed` (hata / çıkış butonu)
- Yeni eklenen ekranlar bu palete bağlı kalmalı.

## 5) Strings ve localization
- Strings: `composeApp/src/commonMain/composeResources/values/strings.xml`
- Yeni ekranlar mutlaka string kaynaklarını kullanmalı.
- Örnek:
    - `profile_title_student`, `profile_title_business`
    - `business_products_add_title`, `business_products_update_button`

## 6) Örnek ekran şablonları

### 6.1 Yeni kullanıcı Home
```kotlin
Good4Scaffold(
    bottomBar = { NavigationBar(...) }
) { padding ->
    Box(Modifier.padding(padding)) {
        when (selectedIndex) {
            0 -> NewDashboardScreen()
            1 -> NewProductsScreen()
            2 -> NewProfileScreen(onLogout = ...)
        }
    }
}
```

### 6.2 Profile
```kotlin
ProfileScreenScaffold(
    title = stringResource(Res.string.profile_title_student),
    isLoading = state.isLoading
) {
    // Avatar
    // InfoCard’lar
    // Logout
}
```

### 6.3 Dashboard Stat Kartları
```kotlin
StatCard(
    title = "Toplam Ürün",
    value = state.totalProducts.toString(),
    icon = Icons.Filled.Star,
    color = InkBlack
)
```

### 6.4 Product List
```kotlin
ProductListCard(
    product = product,
    currencySuffix = stringResource(Res.string.price_currency_suffix),
    showStoreName = true
)
```

### 6.5 Product Form
```kotlin
ProductFormFields(
    title = stringResource(Res.string.business_products_add_title),
    submitLabel = stringResource(Res.string.business_products_add_button),
    onSubmit = viewModel::addProduct,
    showBusinessSelector = true,
    businessOptions = state.businesses.map { SelectOption(it.id, it.name) },
    // ... diğer alanlar
)
```

## 7) Dikkat edilecekler
- **UI state** tüm verileri `StateFlow` içinde taşımalı.
- **UI logic** (validation, error) ViewModel’de olmalı.
- **UI** sadece state’i tüketmeli.
- Renk/strings hardcoded yapılmamalı.
- Yeni role eklerken: Route + NavGraph + UserRole + HomeScreen eş zamanlı güncellenmeli.

## 8) Data / Repository / DTO kuralları
- **DTO** modelleri Firestore/Network ham verisini temsil eder.
- **Domain** modelleri (UI’da kullanılanlar) DTO’dan türetilir ve null güvenliği sağlar.
- **Repository** sadece DTO getirir, mapping’i ya repository içinde ya da ayrı mapper fonksiyonlarıyla yapar.
- **Hata yönetimi** `Result` ile yapılır (`Result.Success` / `Result.Error`). UI’ya sadece `UiText` veya string taşınır.
- **Yeni koleksiyon eklerken** `FirestoreRepository.getCollectionWithIds(...)` pattern’i kullanılır.
- **Date/Time** için `kotlinx.datetime.Clock.System.now().toString()` kullanımı mevcut örnekle uyumlu.

Örnek DTO + mapping:
```kotlin
data class FooDto(
    val title: String? = null,
    val count: Int? = null
)

data class Foo(
    val id: String,
    val title: String,
    val count: Int
)

// Repository içinde
Foo(
    id = doc.id,
    title = dto.title ?: "",
    count = dto.count ?: 0
)
```

## 9) Önerilen klasör yapısı (yeni kullanıcı tipi için)
```
composeApp/src/commonMain/kotlin/com/good4/<newuser>/presentation/
  home/
  dashboard/
  products/
  profile/
```

## 10) Hazır referans dosyalar
- `student/presentation/*`  -> öğrenci akışı
- `business/presentation/*` -> işletme akışı
## 11) Firebase Yapılandırması ve Google Sign-In Kuralları

### 11.1 Ortam ve Firebase Proje Eşleşmesi
- **Production (Prod)**: Firebase Projesi **`good4tr-v2`** (Proje No: `654697131931`, Paket: `com.good4`)
- **Staging (Test)**: Firebase Projesi **`good4tr-test`** (Proje No: `449563145023`, Paket: `com.good4.test`)
- *Önemli Not*: Eski `good4tr` (Proje No: `737367442886`) v1 projesidir; Prod derlemelerinde kesinlikle v1 projesi bilgileri veya Web Client ID'si kullanılmamalıdır.

### 11.2 `google-services.json` Dosya Konumları
- **Prod**: `composeApp/src/prod/google-services.json` ve kök `google-services.json`
- **Staging**: `composeApp/src/staging/google-services.json`
- Her iki `google-services.json` içinde `"oauth_client"` dizisinde `"client_type": 3` (Web Client ID) tanımlı olmak zorundadır. Boş bırakılırsa Android `R.string.default_web_client_id` kaynağı oluşmaz ve Google açılmaz.

### 11.3 Keystore ve SHA-1/SHA-256 Parmak İzleri
Firebase Console'da ilgili Android uygulamasının altına şu parmak izlerinin eklenmiş olduğundan emin olunmalıdır:
- **Debug Key (`composeApp/debug.keystore`)**:
  - SHA-1: `FC:35:13:7B:4E:7A:91:22:A5:BE:13:45:2C:1A:2F:FB:44:81:A7:BB`
- **Release/Upload Key (`keystore/release.jks`)**:
  - SHA-1: `56:BC:AC:58:5A:77:68:46:30:98:50:29:BB:CB:BE:43:87:8B:DA:ED`
  - SHA-256: `EA:3C:68:0A:1E:FE:01:43:E1:83:48:07:ED:62:C5:75:47:4C:79:5C:F1:94:29:7F:D9:DB:E6:E9:62:02:7C:E7`
- **Google Play App Signing (Play Store / Internal Testing Sürümleri İçin)**:
  - SHA-1: `E4:98:A1:A0:20:9E:A1:04:20:2F:13:01:87:1D:4F:55:E5:C6:30:39`
  - SHA-256: `46:87:CF:AB:9B:09:6B:13:6A:F3:1D:46:63:AE:E8:1F:7A:FC:00:1B:4F:EA:F6:37:3C:58:85:C6:75:61:DC:D7`

### 11.4 R8 / Resource Shrinking Koruması
- `composeApp/src/main/res/raw/keep.xml` içinde `tools:keep="@string/default_web_client_id"` kuralı korunmalıdır.
- `composeApp/proguard-rules.pro` içinde Credential Manager keep kuralları kalmalıdır:
  `-keep class androidx.credentials.** { *; }`
  `-keep class com.google.android.libraries.identity.googleid.** { *; }`


