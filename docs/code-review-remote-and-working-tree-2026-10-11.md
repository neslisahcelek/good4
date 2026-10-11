# Remote ve çalışma ağacı ortak incelemesi — 11 Ekim 2026

## Düzeltilmesi gereken bulgular

1. **[Orta] `composeApp/src/commonMain/kotlin/com/good4/notification/NotificationsViewModel.kt:162` — Bildirim onayı diğer tercihleri yeniden açabilir.** Yeni `enableAnnouncements()` yüklenmişliği doğrulamadan `mutable.value.preferences` değerini bütün alanlarıyla kaydediyor. State başlangıçta `eventUpdates=true`, `reminders=true`; ilk yenileme ise önce inbox, ardından tercihleri okuyor. Daha önce bu iki kategoriyi kapatmış bir kullanıcı yeni cihazda/yeniden kurulumda ana ekrana gelir ve okuma tamamlanmadan “Bildirimleri aç” seçerse, `NotificationRepository.savePreferences()` ve sunucudaki `updateNotificationPreferencesService()` bu iki alanı da `true` yazar. Başarısız tercih okuması da repository'de varsayılana çevriliyor. Duyuru tercihini sunucuda transaction ile yalnız ilgili alanı güncelleyerek kaydedin; mevcut tercihler okunmadan tam DTO yazmayın. Bu bulgu yeni remote commit'ten geliyor.

2. **[Orta] `composeApp/src/commonMain/kotlin/com/good4/navigation/NavGraph.kt:142` — İlk girişte iki izin penceresi birlikte açılıyor.** Production proje için bildirim eğitim kaydı yoksa StudentHome rotasına geçiş `PushSignals.education=true` yapıyor. Aynı StudentHome composition'ında `StudentHomeScreen.kt:80` analytics onayı UNKNOWN olduğunda ayrı bir `Dialog` açıyor. İki akış arasında bekleme/görünürlük koordinasyonu bulunmuyor; bir pencere diğerini örter ve kullanıcı izin akışlarını arka arkaya yığılmış olarak görür. Onayları ortak bir görünürlük state'i/kuyruk üzerinden sırayla gösterin; biri kapatılmadan diğerini açmayın. Bu bulgu remote bildirim değişikliği ile yerel analytics değişikliğinin birleşimidir. Kaynakla doğrulandı; cihazda görsel doğrulama yapılmadı.

3. **[Orta] `composeApp/src/commonMain/composeResources/files/good4-gizlilik-politikasi-v1.3.md:188` — Paket içi gizlilik açıklaması yeni Analytics davranışıyla çelişiyor.** Kayıt ekranı `RegistrationLegalAcknowledgements.kt:118` ile bu yerel dosyayı gösteriyor; dosya hâlâ Firebase Analytics kullanılmadığını söylüyor. Android/iOS SDK entegrasyonu ve isteğe bağlı toplama bu diff'te eklenmiş; web metni güncellenmiş, mobil kopya yalnız marka ekleri açısından değiştirilmiş. Paket içi gizlilik/aydınlatma kaynaklarını yeni veri/izin açıklamasıyla eşitleyin. Bu bir kaynak ve kullanıcı bilgilendirme tutarsızlığıdır; hukuki yeterlilik değerlendirmesi değildir.

4. **[Orta] `composeApp/src/commonMain/kotlin/com/good4/social/SocialRepository.kt:68` — Aynı hesaba yeniden giriş oturum neslini değiştirmeyebilir.** Singleton repository yalnız bir metodu çağrıldığında anlık UID'yi karşılaştırıyor; `authStateFlow` gözlemi veya logout invalidation bağlantısı yok. A hesabı → çıkış → aynı A hesabına giriş arasında Social repository'ye çağrı yapılmazsa cache UID'si ve access revision aynı kalır. Bu durumda eski cache yeni oturumda kullanılabilir; hâlâ çalışan eski feed/summary de `checkSession()` kontrolünü geçip yeni oturum state'ine uygulanabilir. UID eşit olsa bile auth çıkış/giriş olaylarında nesli artırın, bekleyen okumaları iptal edin ve badge/cache'i temizleyin. Bu bulgu yerel diff'teki yeni cache sözleşmesine ilişkindir; kaynak üzerinden doğrulandı, KMP testi çalıştırılamadı.

## Mimari/refactor

- **[Düşük] `composeApp/src/commonMain/kotlin/com/good4/dining/presentation/AkdenizDiningMenuViewModel.kt:117` — Aynı guard iki kez bulunuyor.** Yeni kontrol, satır 125'teki `ratingInFlight`/`myVote` kontrolünü aynı değişmeyen `current` snapshot'ıyla yineliyor. Aradaki kod bu state'i değiştirmiyor. İlk kontrol analytics click'inden önce kalmalı; ikinci kontrol kaldırılabilir. Davranış yarışı değil, değişen kapsamdaki gereksiz kontrol ve bakım borcudur.

## Kapsam ve entegrasyon

- Base (işlem öncesi HEAD): `da2636693267095b041bc6761eade1ddf6210e8b`.
- Head / `origin/main`: `6bfeed5edea5743bf282bc2488ebb6f1ce0ea192`.
- Yeni gelen tek commit: `6bfeed5 feat(notifications): ask for notifications on the student home`.
- `git fetch origin` ardından `git merge --ff-only origin/main` uygulandı; merge conflict oluşmadı.
- Yerel değişiklikler geçici stash ile korundu ve `--index` ile geri yüklendi. İşlem öncesi ve sonrası staged patch dosyaları byte düzeyinde aynı bulundu. Başlangıçtaki 133 status girdisinin tamamı korundu.
- İnceleme devam ederken dışarıdan değişen `iosApp/scripts/open-xcode.sh` ve `docs/ios-firestore-symbols.md` de okundu; bu dosyalar bu chat tarafından değiştirilmedi.
- Commit aralığı, staged/unstaged diff ve birleşmiş son kaynak birlikte incelendi. Ana alanlar: bildirim izin/tercih zinciri, analytics izin/SDK/DI ve privacy kaynakları, kampüs test politikası/OTP/disconnect, hesap silme, başvuru/kupon sayfalaması, kurallar/indexler, topluluk block/error akışları, Social cache/istek nesilleri, ortak loading bileşenleri ve Android/iOS yapılandırması.
- Uygulama koduna review düzeltmesi uygulanmadı; commit, push veya deploy yapılmadı. Stash yedeği korundu.

## Doğrulama

- `npm --prefix firebase/v2/functions run build`: başarılı.
- `npm --prefix firebase/v2/web test`: 6/6 başarılı.
- `npm --prefix firebase/v2/web run build`: başarılı.
- `npm --prefix firebase/v2 run test:rules`: Java 21 ile yerel emülatörde 25/25 başarılı.
- `npm --prefix firebase/v2 run test:functions`: Java 21 ile 316 test; 310 başarılı, 0 başarısız, 6 atlandı. İki gerçek Auth e-posta-link testi ve dört Storage yükleme testi ilgili emülatörler çalışmadığı için kapsam dışında kaldı.
- `git diff --check` ve `git diff --cached --check`: başarılı.
- Birleşik diff'in eklenen satırlarında Google API key/private key/uzun literal credential taraması: eşleşme yok. Bu sonuç genel bir secret yokluğu garantisi değildir.
- Android/iOS derleme ve Android unit test komutu Gradle önbelleği sandbox engeli nedeniyle başlamadı; dış erişim isteği kullanıcı tarafından reddedildi. Mobil build/test sonucu doğrulanamadı.
- iOS plist/privacy manifest sözdizimi (`plutil -lint`) ve iki kurulum/açılış betiğinin shell sözdizimi (`bash -n`): başarılı.
- Normal rules testi bounded alternatif kuralları çalıştırmaz; cost-rules testi ayrıca çalıştırılmadı. Canlı index READY durumu, Analytics teslimi, mağaza arşivi ve cihaz UI akışları doğrulanmadı.

**Karar:** Önce bulguları düzelt. Kritik/Yüksek kaynak bulgusu saptanmadı; bu, çalıştırılamayan mobil doğrulamanın veya canlı yayın kontrollerinin yerine geçmez.
