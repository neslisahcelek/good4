# daily-menu-ratings review ve refactor — 5 Ekim 2026

İncelenen head: `f6a8516` (`origin/daily-menu-ratings`). Karşılaştırma: `origin/main`, merge-base `4851bfb`. Kapsam, branch'in birleştirmeye getirdiği 47 dosyanın tamamıdır: menü puanlama, topluluk öğrenci/yönetici ekranları, etkinlik zamanları, kapak seçme/yükleme, katılım, kurallar ve ilgili testler. Aşağıdaki satırlar incelenen head'e aittir. Bulgular yerel `codex/daily-menu-review` branch'inin çalışma ağacında düzeltildi.

## Bulgular

[P1] Hesap silinirken yemek oylarını da temizle — firebase/v2/functions/src/mealRatings.ts:69

Yeni koleksiyon oyları UID belge kimliğiyle saklıyor; `eraseAccountData` bu koleksiyonu taramıyor. Hesap silme başarıyla bittiğinde kişinin oyu ve UID bağlantısı kalıyor. Düzeltme: hesap önce `deleting` yapıldıktan sonra özetler cursor ile taranıyor; oy ve ilgili sayacın azaltılması aynı transaction'da yapılıyor. Tekrar silme sayacı tekrar azaltmıyor.

[P2] Pasif hesabın kişisel oy okumasını kapat — firebase/v2/firestore.rules:199

Yeni özel oy kuralı yalnızca token ve UID eşitliğine bakıyor. Hesabı `disabled` veya `deleting` olan kişi token geçerliyken kendi özel oylarına erişebiliyor. Düzeltme: normal ve bounded kurallarda kişisel oy için `currentUserIsActive()` kullanıldı; genel sayaçların mevcut okuma sözleşmesi korundu.

[P2] Öğrenci etkinlik listesinde devam sayfasını erişilebilir tut — composeApp/src/commonMain/kotlin/com/good4/community/CommunitiesScreen.kt:598

Ekran yeniden düzenlenirken ortak `entriesCursor`/`loadMoreEntries` butonu kaldırılmış; yeni buton sadece yönetici akışında var. Öğrenci ilk 50 kaydın dışındaki etkinliklerine ve geçmişine erişemiyor. Düzeltme: seçilen topluluğun öğrenci/önizleme listesine devam butonu geri eklendi. Yönetici devam sayfasından gelen etkinliklerin katılımcı gözlemleri de başlatılıyor.

[P2] Kapak kaldırma isteğinde boş URL'yi gönder — composeApp/src/commonMain/kotlin/com/good4/community/CommunityManagerScreens.kt:732

Yeni “Kaldır” aksiyonu `imageUrl` alanını boşaltıyor; repository boş alanı payload'a koymadığı için sunucu mevcut kapağı koruyor. Düzeltme: etkinlik kaydında `imageUrl` açıkça gönderiliyor; boş değer gerçekten kapağı kaldırıyor.

[P2] Eski puan okumasının yeni oyu ezmesini önle — composeApp/src/commonMain/kotlin/com/good4/dining/presentation/AkdenizDiningMenuViewModel.kt:61

Menü açıldığında yapılan okuma, kullanıcı yeni oyunu kaydettikten sonra eski `myVote` ve sayaçlarla dönebilir. Gün yenilendiğinde de önceki günün sonucu yeni state'e yazılabiliyor. Düzeltme: menü istek nesli, öğün bazında oy sürümü, in-flight kontrolü ve iptal edilen işler eklendi; kullanıcı değişimi state'i sıfırlıyor. Topluluk kayıt yanıtları da seçim/kullanıcı nesliyle korunuyor.

[P2] Oy isteğini ekranda gösterilen tarihe bağla — composeApp/src/commonMain/kotlin/com/good4/dining/data/repository/MealRatingRepository.kt:42

Payload yalnızca öğün ve oy içeriyor. Gece yarısında önceki günün yemeğini gösteren ekrandan gönderilen istek sunucuda yeni günün oyu olarak kaydedilebiliyor; eski sonuç da yeni güne uygulanabiliyor. Düzeltme: yeni istemci `date` gönderiyor, sunucu İstanbul tarihiyle uyuşmayan isteği reddediyor. ViewModel de tarihi kontrol ediyor. Eski istemcilerin tarihsiz sözleşmesi destekleniyor.

[P2] Puan okuma hatasını sıfır oy gibi gösterme — composeApp/src/commonMain/kotlin/com/good4/dining/data/repository/MealRatingRepository.kt:30

Her `Result.Error`, belge yokmuş gibi boş sayaca çevriliyor. Ağ/izin/decode hatası kullanıcıya “hiç oy yok” şeklinde gösteriliyor ve kendi oyu kayboluyor. Düzeltme: sadece platform okuyucularının “Document not found” sonucu boş değer kabul ediliyor; diğer hatalar görünür kaynak mesajı ve puan okumayı yeniden deneme aksiyonuna dönüşüyor.

[P2] Öğün açılış saatinde puanlama alanını güncelle — composeApp/src/commonMain/kotlin/com/good4/dining/presentation/DailyMenu.kt:214

Saat sadece composition sırasında okunuyor. Örneğin sayfa 10:59'da açık kaldığında başka state değişmezse 11:00 sonrasında yemek puanlama kapalı kalıyor. Düzeltme: ekran saati periyodik state ile yenileniyor, lifecycle dönüşünde gün yenileme korunuyor; ViewModel de açılış saatini doğruluyor.

[P2] Süresi geçmiş taslağı tarihini değiştirmeden yayınlama — firebase/v2/functions/src/events.ts:159

Başlangıç kontrolü yalnızca başlangıç değeri değişmişse çalışıyor. Dün için kaydedilen taslak bugün aynı zamanlarla “Yayınla” üzerinden yayınlanabiliyor. Düzeltme: taslaktan yayına geçiş de geçmiş başlangıç kontrolüne dahil edildi; devam eden yayınlanmış etkinliğin değişmeyen başlangıcıyla düzenlenmesi korunuyor.

[P2] Yönetici formunda sistem geri dönüşünü ve form state'ini koru — composeApp/src/commonMain/kotlin/com/good4/community/CommunityManagerScreens.kt:594

Yeni tam ekran form yalnızca üstteki kapatma aksiyonunda değişiklik onayı gösteriyor; Android geri tuşu üst navigasyona geçerek kaydedilmemiş alanları kaybettiriyor. Alanlar ve görsel de Composable `remember` içinde tutulduğu için Activity yeniden oluşturulmasında kayboluyor. Düzeltme: yönetici sayfa/form geri handler'ları eklendi; taslak, görsel ve doğrulama `CommunityViewModel` StateFlow state'ine taşındı. Süreç öldürülmesinden sonra otomatik form kurtarma bu değişikliğin kapsamında değildir.

[P2] Android fotoğrafının EXIF yönünü JPEG'e aktar — composeApp/src/androidMain/kotlin/com/good4/community/CoverImagePicker.android.kt:49

Yeni picker ham `BitmapFactory.decodeStream` çıktısını JPEG'e çevirirken yön bilgisini kaldırıyor. EXIF ile döndürülen telefon fotoğrafları kapak olarak yan veya ters kaydediliyor. Düzeltme: EXIF dönüş/ayna bilgisi piksele uygulanıyor. Büyük görseller bounds üzerinden örneklenerek çözülüyor; geçici bitmap'ler geri bırakılıyor. EXIF bilgisi okunamayan desteklenen biçimler normal yönle hazırlanıyor.

[P2] Kapak hazırlanırken kaydetmeyi engelle — composeApp/src/commonMain/kotlin/com/good4/community/CommunityManagerScreens.kt:682

Fotoğraf seçimi sonrası asenkron hazırlama sürerken yayın/taslak butonları aktif. Kullanıcı hemen kaydederse henüz gelmemiş görsel payload'a alınmıyor ve form kapanıyor. Düzeltme: iki kayıt aksiyonu da picker'ın `preparing` durumunda kapatıldı.

[P2] Çok günlük etkinliği sorguda bitişine kadar tut — composeApp/src/commonMain/kotlin/com/good4/community/CommunityRepository.kt:297

Yeni bitiş-tarihi filtresi çok günlük etkinliği tutmaya çalışıyor; öncesindeki Firestore sorgusu hâlâ `startsAt >= now` şartıyla onu başlar başlamaz dışarıda bırakıyor. Düzeltme: keşfet sorgusu `endsAt >= now` kullanıyor; `status + endsAt` composite index'i eklendi.

## Refactor ve doğrulama

Yeni menü ve yönetici/öğrenci ekranlarının ana metinleri, durumlar, form hata etiketleri ve callable hata mesajları Compose kaynaklarına taşındı. Aynı metinler için mevcut kaynaklar tekrar kullanıldı. Ortak topluluk renkleri `Colors.kt`, aksiyon ölçüleri `ButtonStandards.kt` üzerinden alınır. Yeni form state'i UI tarafından okunur ve değişiklikler ViewModel olaylarıyla yapılır. Menü yayın kontrolü oy transaction'ına taşındı.

- Android `compileProdDebugKotlinAndroid`: başarılı.
- iOS `compileKotlinIosSimulatorArm64`: başarılı.
- `testProdDebugUnitTest`: 81 test, 0 hata, 0 atlanan.
- Functions TypeScript build: başarılı.
- Firestore/Storage emülatörlerinde communityPortal, communityImages, events, mealRatings, accountDeletion ve mevcut rules testleri: 59 test, 0 hata, 0 atlanan.
- Geçici emülatör doğrulaması: yanlış gün/gece yarısı isteği reddi, UID oy temizliği, sayaç azaltma ve idempotent hesap silme, geçmiş taslak yayını reddi, normal/bounded kurallarda pasif hesabın özel oy erişimi reddi başarılı. Kalıcı yeni test dosyası eklenmedi.
- `git diff --check`: başarılı.

Genel değerlendirme: doğrulanan bulgular düzeltildi; Android/iOS derlemeleri ve ilgili mevcut testler başarılı. ViewModel yarış durumları kod üzerinden denetlendi; gerçek cihazda ekran/geri tuşu, EXIF fotoğraf ve galeriden seçim akışı bu oturumda çalıştırılmadı. Firebase emülatörleri prod composite index zorunluluğunu doğrulamaz. Commit veya push yapılmadı. AGENTS.md'de sözü edilen `.githooks/pre-commit` dosyası bu checkout'ta mevcut olmadığı için hook çalıştırılamadı.

## Production deploy — 5 Ekim 2026

Kullanıcının deploy talebiyle `good4tr-v2` projesine Firestore kuralları/index tanımları ve Storage kuralları yayınlandı. İlgili sekiz function (`uploadCommunityEventImage`, `rateMeal`, `getCommunityPortalDashboard`, `saveCommunityPortalEntry`, `cancelCommunityPortalEntry`, `setEventRegistration`, `recordEventAttendance`, `deleteMyAccount`) europe-west1 bölgesinde başarıyla deploy edildi; canlı API üzerinden tamamı `ACTIVE` olarak doğrulandı. Yeni `events: status + endsAt` index'i canlı API üzerinden `READY` olarak doğrulandı. Mobil uygulama mağaza yayını yapılmadı. Deploy logu: `/private/tmp/good4-daily-deploy-production.log`.
