# Firestore Frameworks and Symbols in Good4 iOS

Bu doküman, Good4 iOS uygulamasında Firebase Firestore bağımlılıklarının dağıtımı, Swift Package Manager (SPM) yapılandırması ve sembol (dSYM) yönetimini açıklar.

---

## 1. Dağıtım Modeli: Standart Resmi İkili Paketler (Binary Distribution)

Good4 iOS projesi, Google Firebase iOS SDK'nın resmi ve varsayılan olarak sağladığı **ikili (prebuilt binary)** paket grafiğini (`abseil-cpp-binary`, `grpc-binary`) kullanır.

### Avantajları:
1. **Hızlı Derleme (Build Performance):**
   - Abseil ve gRPC devasa C++ projeleridir. Önceden derlenmiş framework'ler kullanıldığında Xcode Cloud ve yerel derleme süreleri **5-8 dakika daha hızlı** tamamlanır.
2. **Sıfır Dış Bağımlılık / Konfigürasyon:**
   - Apple Xcode Cloud veya yerel Xcode üzerinde özel ortam değişkeni (`FIREBASE_SOURCE_FIRESTORE`) tanımlama zorunluluğu yoktur; proje doğrudan "tak-çalıştır" derlenir.
3. **Uygulama Sembolleri:**
   - Good4'un tüm Kotlin, Compose ve Swift kodları tam sembolik dSYM üretir; Crashlytics ve TestFlight üzerinde uygulama çökmeleri satır numaralarıyla izlenir.

---

## 2. Xcode Cloud ve CI/CD Entegrasyonu

* **`ci_scripts/ci_post_clone.sh`**:
  `xcodebuild -resolvePackageDependencies -project iosApp/iosApp.xcodeproj -scheme "iosApp Prod"` komutu ile kilitli `Package.resolved` dosyasını sorunsuz olarak çözer.
* **`iosApp/scripts/firestore-archive-symbols.sh`**:
  Arşivleme esnasında prebuilt ikili framework'leri kabul eder ve arşivin başarıyla TestFlight'a yüklenmesini sağlar.
* **`iosApp/scripts/open-xcode.sh`**:
  Projeyi yerel Xcode ile doğrudan açmak için kullanılır. Herhangi bir ortam değişkeni önkoşulu yoktur.

---

## 3. Opsiyonel: Kaynak Koddan Derleme (Source Distribution)

Eğer gelecekte Firebase iç C++ kütüphanelerinin dSYM sembollerini de arşive dahil etmek gerekirse:
1. Terminal veya CI ortamında `FIREBASE_SOURCE_FIRESTORE=1` tanımlanarak `xcodebuild -resolvePackageDependencies` çalıştırılır.
2. Bu durumda Swift Package Manager otomatik olarak `abseil-cpp-SwiftPM`, `grpc-ios` ve `boringSSL-SwiftPM` paketlerini çözerek kaynak koddan derleme moduna geçer.
