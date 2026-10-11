# Can commitleri: inceleme ve refactor — 11 Ekim 2026

## Kapsam ve sonuç

Kullanıcının istediği `e40cab3a8f090a7fcb3e3ca8d96d9ed5a4784c1e` commit'i **dahil**, güncel remote `main` üzerindeki Can commitleri incelendi. Başlangıçta yerelde olmayan son bildirim commit'i de fetch/fast-forward ile kapsama alındı.

- Diff base: `1a4c8c70d45c1c807b2dfbd15401679e37180176` — istenen ilk commit'in ebeveyni.
- İncelenen head: `da2636693267095b041bc6761eade1ddf6210e8b`.
- 26 commit, tümü `Canklnc`; tarihsel diff'te 82 dosya. Bu aralıkta merge commit yok.
- İnceleme `good4-code-review`, `good4-architecture`, `good4-firebase-guardrails` ile yürütüldü. Skill değişikliklerinde `skill-creator`, çoklu TSX değişikliklerinde React kontrol listesi kullanıldı.
- Üç alt ajan kupon/veri yaşam döngüsü, mail/başvurular ve mobil/KMP alanlarını aldı. Ana ajan web paneli, native yapılandırma, entegrasyon, skill güncellemeleri ve merkezi doğrulamayı yürüttü. Ajanlar son diff'i ayrıca okudu.

Aşağıdaki davranış bulguları düzeltildi. Kritik/Yüksek seviyede açık bir bulgu doğrulanmadı. Alt bölümde maliyet ve retention borçları ile canlı/cihaz doğrulamasının sınırları ayrıca belirtilmiştir. Commit, push, deploy veya canlı veri değişikliği yapılmadı.

## Düzeltilen bulgular

Satırlar düzeltme sonrası kaynak konumlarını gösterir; eşzamanlı çalışmalar nedeniyle ileride değişebilir.

| Önem | Kaynak | Tetikleyici ve eski etki | Uygulanan düzeltme |
| --- | --- | --- | --- |
| Orta | `firebase/v2/functions/src/accountDeletion.ts:43` | Dört haneli işletme kodu cleanup sorgusundan sonra başka öğrenciye atanırsa, eski snapshot üzerinden yeni sahibin kodu silinebilirdi. Silinen personelin `redeemedBy` bağlantısı da kalıyordu. | Kod belgeleri transaction içinde yeniden okunuyor; güncel `studentId` eşleşmesi siliniyor, yalnız personel bağlantısı eşleşirse anonimleştiriliyor. Başka öğrencinin kodu korunuyor. |
| Orta | `firebase/v2/functions/src/accountDeletion.ts:256` | Yeni mail kota belgeleri clear-text `key` içinde UID tutuyordu ve hesap silme taramasına bağlı değildi. | `eduMail:account:<uid>` ve `eduMailRequest:<uid>` deterministic belgeleri hesap `deleting` olurken kaldırılıyor. Alıcı/genel kotaları korunuyor; hesap silerek ortak kota sıfırlanamıyor. Genel rate-limit retention sınırı aşağıda ayrıca yazılıdır. |
| Orta/maliyet | `firebase/v2/functions/src/eduVerification.ts:109`, `verificationEmail.ts:9` | Kesin gönderim reddi ile timeout/transport/5xx aynı şekilde kota iadesine girebiliyordu. İade edilen teslimat kotası üzerinden kesintide sürekli pahalı tekrar denemeleri mümkündü. | Kesin ret ayrı hata tipiyle taşınıyor; belirsiz kabul ihtimalinde teslim kotası korunuyor. İki callable alias'ının paylaştığı, geri verilmeyen 5/60 saniyelik deneme limiti eklendi. Hata detayları sağlayıcı gövdesini sızdırmıyor; eski teslim edilmiş kod ve uygun cooldown korunuyor. |
| Orta | `firebase/v2/functions/src/applicationSupport.ts:14`, `:34`; `index.ts:452` | Firestore profili henüz bulunmayan başvuruda aktiflik denetimi Auth hesabının `disabled` durumunu kapsamıyordu. Bekleyen başvurunun hedef hesabı onaydan önce silinmiş/devre dışı bırakılmış olabiliyordu. | Gönderimde güncel Auth kimliği ve `disabled`, onayda güncel hedef hesap varlığı/aktifliği kontrol ediliyor. Yönetici yetkisi Auth lookup'tan önce doğrulanıyor. Silinmiş/devre dışı başvuru reddedilebilir kalıyor; panel hataları anlaşılır metne çevriliyor. |
| Orta | `firebase/v2/functions/src/applicationSupport.ts:82`; `firebase/v2/web/src/App.tsx:2292` | Başvuruların ilk 200 kaydı sonrasında devam sözleşmesi yoktu; yönetici kalan kuyruğa erişemiyordu. | Ortak, sınırlandırılmış pending sorgusu; `createdAt` + belge ID sıralaması ve nanosecond dahil opaque cursor eklendi. Sınır belgesi onaylansa/silinse de devam çalışıyor. Her iki başvuru listesinde cursor, devam düğmesi ve stale-response kontrolü var. |
| Orta | `firebase/v2/functions/src/adminPortal.ts:416`; `firebase/v2/web/src/App.tsx:2242` | Bekleyen topluluk kuponları tek sabit sorguda kesiliyor ve sonraki kayıtlar panelden görünmüyordu. | 100'lük sayfa, `communityCouponsCursor`/`nextCursors.communityCoupons`, panelde devam ve ID bazlı birleştirme eklendi. Testin yakaladığı Admin SDK filter/cursor sırası da düzeltildi. |
| Orta/maliyet | `firebase/v2/functions/src/communityCoupons.ts:283`; `firebase/v2/web/src/App.tsx:1248` | Kupon listesi her istekte bütün işletme seçeneklerini okuyordu. Kupon devam sayfası bağımsız işletme sorgusunu da tekrarlıyordu. | Kupon ve işletme sorguları ayrı 100'lük sayfalarla çalışıyor. `businessesCursor`, `businessesNextCursor`, `businessesOnly` ve `includeBusinesses:false` sözleşmeleri; panelde işletme devam düğmesi eklendi. Kupon devamında yüklenen seçenekler korunuyor. |
| Orta | `firebase/v2/firestore.bounded.rules:136` | Normal kurallara eklenen kupon koleksiyonları bounded alternatifte yoktu; bu kurallar kullanıldığında öğrenci kupon okuması reddediliyordu. | Yalnız published kupon get/list ve azami 100 kayıt sınırı eklendi. Kod, claim ve redemption belgeleri istemcilere kapalı kaldı. Negatif kural senaryoları mevcut test içinde genişletildi. |
| Orta | `firebase/v2/web/src/communityCouponCode.ts:1`; `firebase/v2/web/src/App.tsx` kod girişi | Sunucu eski harfli kampanya kodlarını kabul ederken yeni web normalizasyonu harfleri siliyordu. TTL içinde hâlâ geçerli kod doğrulanamıyordu. | Alfanumerik büyük harf normalizasyonu ve eski 6–12 karakterli kampanya route'u korundu. 4/6/8 haneli sayısal yönlendirme ve baştaki sıfırlar korunuyor; input/helper uyumlu hale getirildi. |
| Orta | `firebase/v2/web/src/App.tsx:879`, `:1200`, `:2260`, `:3485` | Eski panel/context okuması hesap/sekme değiştikten veya yazma sonrası yenilemeden sonra dönerek güncel data/error/loading/cursor'ı ezebiliyordu. Aynı UID'ye yeniden giriş de eski oturumun sonucunu kabul edebiliyordu. | Mevcut account request scope gerçek Auth akışına bağlandı; success/error/finally nesil kontrolü eklendi. Community, admin, participant ve business usage yolları korundu. Yazma sonrası refresh eski okumayla tekilleştirilmiyor; StrictMode cleanup eski promise'i geçersiz kılıyor. Başvuru devam loading'i de aynı nesle bağlı. |
| Orta | `firebase/v2/web/src/App.tsx:1003` | Kaynak başına son 100 kayıt üzerinden çıkarılan metrikler gerçek günlük/aylık/toplam kullanım izlenimi veriyordu. | KPI ve geçmiş metinleri açıkça **listelenen** kayıtları ifade ediyor. Tam aggregate eklenmedi; ekranın gerçekte taşıdığı sınır görünür hale getirildi. |
| Orta | `composeApp/src/commonMain/kotlin/com/good4/community/CommunityViewModel.kt:186`, `:262`, `:406` | Bloklanan topluluğun yeni kuponları featured kartta kalabiliyor veya doğrudan seçilebiliyordu. | İlk featured filtre, anlık bloklama temizliği ve doğrudan seçim birlikte korundu. Yönetici kendi topluluğunu yönetmeye devam edebiliyor. Asenkron yükleme/seçim/devam sonuçları kullanıcı ve istek nesliyle doğrulanıyor. |
| Orta | `composeApp/src/commonMain/kotlin/com/good4/community/CommunityRepository.kt:309`; `CommunityViewModel.kt:328` | Yeni kupon koleksiyonunun okuma hatası çalışan etkinlik akışını da kapatıyordu; kupon hatası boş liste gibi görünüyordu. | Etkinlik ve kupon yüklemesi ayrıldı; kupona özel localized `selectedCouponsError` ve retry yolu eklendi. Featured kupon seçimi gereksiz etkinlik belgesi okuması yapmıyor. Cancellation hata/boş sonuç gibi yutulmuyor. |
| Orta/maliyet | `composeApp/src/commonMain/kotlin/com/good4/community/CommunityRepository.kt:377`; `firebase/v2/firestore.indexes.json` | Geçmişte published kalan süresi dolmuş kuponlar her girişte bütün sayfalardan okunup yalnız istemcide eleniyordu. | Sorguya sabit bir `endsAt >= now` filtresi bütün cursor sayfaları boyunca taşındı. Global ve community filtreli kupon sorguları için composite index tanımları eklendi. Henüz bitmemiş kuponların tam okunması maliyet sınırı olarak ayrıca yazılıdır. |
| Orta | `composeApp/src/commonMain/kotlin/com/good4/social/SocialScreen.kt:98`; `SocialActivityScreen.kt:101` | Okul doğrulama proof'i sheet açılmadan önce geldiyse, yalnız email değişimini izleyen effect devam akışını çalıştırmıyordu. | Effect sheet görünürlüğünü de izliyor; önceden bilinen proof ile sheet kapanıp devam/yenileme akışı çalışıyor. Eşzamanlı yeni social cache akışında da bu anahtar korunuyor. |
| Düşük | `composeApp/src/commonMain/kotlin/com/good4/community/CommunitiesScreen.kt:1657` | Kupon son tarih etiketi kart başına hatırlanan günle donuyor; gece yarısı veya uygulamaya dönüşte eski kalıyordu. | Kart tarihini üst ekranın canlı saatinden alıyor; lifecycle dönüşü saati/günü güncelliyor. |
| Orta | `iosApp/iosApp.xcodeproj/project.pbxproj:602` | Production Firebase'e bağlı yeni DebugV2 yapılandırması release bump dışında kalıp 1.1.1/build 11 taşıyordu; minimum sürüm kontrolünde gereksiz kilide girebilirdi. | DebugV2 de 1.1.6/build 18'e güncellendi; native V2 simulator derlemesi doğrulandı. |
| Düşük | `tools/install-v2-iphone.sh:5` | Kurulum yalnız Can'ın mutlak checkout yolunda çalışıyor, cihaz argümanı yokken onun UDID'sini seçiyordu. | Repo kaynak/çıktı/Firebase kontrolleri korunarak checkout yolu taşınabilir hale getirildi. Cihaz UDID'si açık girdi olarak zorunlu. |

## Mimari/refactor düzeltmeleri

- İşletme ve topluluk başvurularındaki Google kimliği, rolesiz hesap ve pending sayfalama tekrarları `applicationSupport.ts` içinde ortaklaştırıldı. Ürünlerin başvuru alanları ve onay transaction'ları ayrı kaldı.
- `EduVerificationRepository` içindeki Compose resource bağımlılığı kaldırıldı. Veri katmanı servis hata kodunu taşırken ViewModel mevcut `UiText` pattern'iyle localized state üretiyor; kart ve rezervasyon ekranı hazır state'i gösteriyor. `EDU_CODE_REQUEST_LIMIT` uygun bekleme mesajına bağlı.
- Okul doğrulama kartının kullanıcı metinleri `strings.xml` kaynaklarına taşındı; önemli doğrulama/kupon Preview'ları gerçek Good4 temasıyla çalışıyor. Mevcut instrumented kart beklentileri yeni etiketlerle uyumlandı.
- İşletme onboarding formu tekrar submit'i engelliyor ve kayıt sürerken alanları disabled fieldset ile sabitliyor. Scope'lu CSS fieldset görünümünü mevcut tasarımla koruyor.
- Etkinlik kaydındaki artık eski kupon payload dalı kaldırıldı. Yeni kupon sözleşmesinin canonical model ve pagination/index açıklamaları güncellendi.
- Emulator-only test korumasının tek bir geliştiricinin portuna bağlı olması kaldırıldı; yerel host ve açık `demo-*` proje zorunluluğu korunuyor.

## Eşzamanlı çalışma ağacı ile entegrasyon

İnceleme başında çalışma ağacı temizdi. Süreç içinde başka çalışma sosyal feed/cache ve kampüs test erişimi değişiklikleri ile staged dosyalar ekledi. Bunlar bu 26 commit'in tarihsel bulgusu veya bu incelemenin toplu refactor'u olarak sayılmadı. Stage/reset/geri alma yapılmadı; ortak derleme güncel ağacı kapsadı.

Bu yeni değişiklikle mail reservation arasında somut bir entegrasyon hatası bulundu ve küçük bir düzeltme uygulandı: önceki saat `sendCount=5` iken yeni saatte ilk gönderim sürerken test bağlantısı kesilirse `disconnectCampusTestService` eski sayacı yeni pencereye `6` olarak taşıyordu. Kullanıcı tek gönderime rağmen saatlik limite takılıyordu.

`eduVerification.ts:155` reservation'ın kendi `pendingSendCount` değerini saklıyor; `campusAccess.ts:77` disconnect bunu kullanıp geçici alanları kaldırıyor. Eski reservation formatına fallback aynı pencere olup olmadığını kontrol ediyor. Mevcut disconnect testine önceki pencere fixture'ı ve yeni pencerenin sayacının `1` olması beklentisi eklendi. Bu entegrasyon düzeltmesi dışında yeni test erişimi/sosyal feature'ının tamamı için kapsamlı kabul testi iddiası yoktur.

## Skill güncellemeleri

Kullanıcının açık talebi üzerine aşağıdaki yerel skill'ler güncellendi; üçü de `skill-creator` doğrulayıcısından geçti.

- `.agents/skills/good4-code-review/SKILL.md`: review/düzeltme/refactor sırasında kanıtlanmış, tekrar kullanılabilir ve rehberde eksik kalıpların ilgili **yerel Good4** skill'lerine ve tekrar kalıpları kaynağına ayrı onay olmadan eklenmesine izin veren kural. Yalnız review'da uygulama kodunu değiştirmeme kuralı korunuyor. Yetki commit/push/deploy/canlı işlem/global skill veya dosya sistemi izinlerini genişletmiyor.
- `.agents/skills/good4-firebase-guardrails/SKILL.md`: kesin/belirsiz mail teslimatı, geri verilmeyen ortak deneme limiti, tekrar kullanılan ID'de transaction ownership, actor UID bağlantıları, bağımsız lookup sayfaları, sınırlı örnekten toplam türetmeme, profilsiz Auth aktiflik sınırı.
- `.agents/skills/good4-architecture/SKILL.md`: yeni içerikte bütün block yolları, bağımsız feed hata state'i, success/error/finally nesli, aynı UID yeniden giriş, yazma sonrası refresh, önceden gelen proof, canlı tarih/lifecycle, production debug sürümü ve taşınabilir kurulum betikleri.
- `docs/REPEATED_REVIEW_PATTERNS.md`: 11 Ekim bölümü bu bulguların tetikleyici ve korunacak sözleşmelerini kaydediyor. Tek seferlik ürün tercihleri zorunlu kurala dönüştürülmedi.

## Commit bazında inceleme kaydı

Her commit kendi diff'i ve aralığın birleşmiş son davranışı üzerinden değerlendirildi. Aşağıdaki sonuç sütunu ilgili ana kontrolü gösterir; düzeltmeler yukarıda birleştirilmiştir.

| Commit | Ana değişiklik | İnceleme sonucu |
| --- | --- | --- |
| `e40cab3` | Google sign-in/App Check CSP | Hosting CSP ve panel giriş tüketicileri incelendi; yeni doğrulanmış açık yok. |
| `765cb51` | Topluluk panel girişi | Portal ayrımı, rol/route ve hatalı giriş yolu incelendi. |
| `d681f33` | Landing metni | Metin değişikliği kontrol edildi. |
| `ac0046a` | DebugV2, V2 scheme, iPhone betiği | Sürüm ve kurulum taşınabilirliği düzeltildi. |
| `9509f92` | Test bundle emulator/App Check | Prod/test ortam sınırı ve native derleme incelendi; canlı attestation denenmedi. |
| `4bcebb4` | Mail/domain dokümanları | Secret placeholder'ları, istemci/domain sözleşmesi kontrol edildi. |
| `2025529` | İşletme Google panel girişi | Staff/community giriş ayrımı ve Auth akışı incelendi. |
| `dcf3def` | Brevo okul doğrulama | Request/confirm, active student, quota, hata, mobil sheet zinciri incelendi ve düzeltildi. |
| `c54db90` | Social Storage test mock | Mevcut migration testi ilgili backend koşusunda çalıştı. |
| `2f4d1f1` | Başarısız teslimatta kota koruma | Kesin/belirsiz hata ve bağımsız deneme limiti düzeltildi. |
| `06647bb` | Üç dakikalık resend | Cooldown/önceki kod koruması ve mevcut senaryolar doğrulandı. |
| `87673f8` | Brevo gizlilik metinleri | Mobil/web metin ve legal version tüketicileri karşılaştırıldı; ilgili testler geçti. |
| `7617b99` | Google ile işletme başvurusu | Auth aktifliği, başvuru/onay yetkisi ve pagination düzeltildi. |
| `1294da9` | Sayısal kampanya kodları | Eski geçerli harfli kodların web uyumluluğu düzeltildi; backend kampanya senaryoları çalıştı. |
| `4620d60` | Panel tasarımı/onboarding | Form pending davranışı, örnek KPI, rol/async state ve React kontrolü uygulandı. |
| `a61ffcb` | Kupon koleksiyonları/backend | Sahiplik, quota/claim/redeem transaction'ları, cleanup, normal/bounded rules ve index'ler incelendi. |
| `41bd6f8` | Panel kupon yönetim/doğrulama | Admin kuyruk devamı, bağımsız işletme lookup ve kod girişi düzeltildi. |
| `7974df0` | Mobil kupon | Repository→ViewModel→UI→iOS serializer zinciri; block, hata, maliyet ve saat düzeltmeleri. |
| `b4a963d` | 1.1.6 release bump | Android/iOS sürüm eşleşmesi, geride kalan DebugV2 düzeltmesi. |
| `f9bed61` | Social okul email sheet | Önceden gelen proof ile devam etme yolu düzeltildi. |
| `ce4d38a` | Ortak okul email sheet | State/error/resource/Preview ve consumer uyumu düzeltildi. |
| `96470f6` | `@ogr.akdeniz.edu.tr` sınırı | Sunucu ve istemci domain doğrulaması, studentAuth/edu/campus testleri incelendi. |
| `27d3c4f` | Hava durumuna göre kampüs fotoğrafları | Görsel kaynaklar, durum eşlemesi, Outlook actual'ları ve 2 mevcut weather testi kontrol edildi. |
| `bb09603` | Admin panel temizliği | Pending listeleri, devam, örnek sayaç ve stale refresh yolları incelendi. |
| `be80898` | Merkez yemekhanesi öğle etiketi | Değişen etiket/consumer kontrol edildi; ek bulgu yok. |
| `da26366` | Reddedilen push credential | APNs credential hatasının cihaz bazında başarısız kalıp diğer cihaz gönderimini durdurmaması ilgili testlerle kontrol edildi. |

## Doğrulama kanıtı

| Kontrol | Sonuç ve sınır |
| --- | --- |
| Functions TypeScript build | `npm --prefix firebase/v2/functions run build` başarılı; son mail reservation entegrasyonu dahil. |
| Web build/test | `npm --prefix firebase/v2/web run build` başarılı; mevcut 6 web testi geçti. Son kod route ve request-scope beklentileri dahil. |
| İlgili backend senaryoları | Firestore emulator'da kupon, deletion, adminPortal, campaigns, edu/campus, başvurular, notifications, legal, studentAuth ve social için ilk koşu **187 test: 184 geçti, 1 hata, 2 atlandı**. Hata yeni admin kupon cursor sorgusundaki filter sırasıydı; düzeltildi. Auth emulator gerektiren iki senaryo yalnız Firestore koşusunda atlandı. |
| Etkilenen backend tekrar koşusu | adminPortal/accountDeletion/businessApplications/communityApplications/eduVerification için **56/56 geçti**; önceki hata dahil. Bütün 187 test yeniden koşuldu iddiası yoktur. |
| Normal + bounded Firestore rules | `rules.test.mjs` ve `cost-rules.test.mjs`: **25/25 geçti**. Published bounded okuma ve private koleksiyon reddi dahil. |
| Son mail/kampüs entegrasyonu | `eduVerification.test.js` ve `campusAccess.test.js`: güncel kaynakla **30/30 geçti**; reservation/disconnect yeni pencere senaryosu dahil. İlk entegrasyon koşusunda 29 senaryodan biri, eşzamanlı eklenen generation kontrolünün hata koduyla eski beklenti uyuşmadığı için başarısızdı. Güncel `CAMPUS_TEST_SESSION_INVALID` beklentisiyle tekrar koşu geçti. |
| Android/iOS Kotlin | `compileProdDebugKotlinAndroid`, `compileKotlinIosSimulatorArm64` başarılı; son ilgili mobil state/resource değişiklikleri dahil. |
| Mobil mevcut unit testleri | `CampusEmailCodeViewModelTest` **7**, `CommunityCouponStateTest` **4**, `CampusWeatherPhotoTest` **2**: toplam **13/13**, fail/error/skip yok. |
| Native iOS | `iosApp V2` / `DebugV2` / generic iOS Simulator, `CODE_SIGNING_ALLOWED=NO`: **BUILD SUCCEEDED**. Gerçek cihaz veya imzalı archive kanıtı değildir. |
| Yapılandırma/syntax | `bash -n tools/install-v2-iphone.sh`, pbxproj `plutil -lint`, `git diff --check` başarılı. |
| Skill metadata | Üç değişen SKILL.md için `quick_validate.py`: **Skill is valid!** |
| Secret kontrolü | Tarihsel diff ve çalışma ağacındaki ek satırlarda private-key, Google API key formatı, Brevo key, GitHub token ve literal bearer yüksek sinyal taraması: **0 eşleşme**. Anahtar değerleri çıktıya basılmadı. Bu, bütün olası secret biçimleri için matematiksel kanıt değildir. |

Yerel koşu logları:

- `/private/tmp/good4-can-review-firebase.log`
- `/private/tmp/good4-can-review-firebase-final.log`
- `/private/tmp/good4-can-review-mail-integration.log`
- `/private/tmp/good4-can-review-mail-integration-final.log`
- `/private/tmp/good4-can-review-kmp-final.log`
- `/private/tmp/good4-can-review-web-final.log`
- `/private/tmp/good4-can-review-ios.log`

Mevcut test senaryoları ve assertion'lar güncellendi; bu inceleme için yeni test fonksiyonları/suite oluşturulmadı. `campusAccess.test.ts` yeni eşzamanlı çalışmaya aittir; burada mevcut disconnect senaryosu genişletildi. Android instrumented test beklentisi güncellendi ancak cihazda instrumented koşu yapılmadı.

## Kalan sınırlar ve öneriler

1. **Orta/maliyet — henüz bitmemiş kuponların toplam okuması:** server zaman filtresi geçmiş birikimini kaldırıyor; mobil hâlâ aktif/gelecek published kuponların bütün sayfalarını topluyor. Çok sayıda eşzamanlı kayıt olduğunda ilk girişin okuması büyür. Ürün devam UX'iyle birlikte featured/global ve topluluk kuponları gerçek istemci pagination'a taşınabilir; bu incelemede liste bütünlüğü korunarak zaman filtresi uygulandı.
2. **Düşük/gizlilik-bakım — genel rate-limit retention:** `communityCouponIssue:<uid>` ve yeni callable gruplarının `group:<uid>` teknik anahtarları özel server-only `rateLimits` belgelerinde kalabilir. İki yeni mail account key'si temizlendi; bütün rate-limit grupları için retention/hesap bağını kaldırma politikası daha geniş ortak servis çalışmasıdır. `resetAt` limitin süresinin dolmasıdır; kaynakta kendi başına belge silme kanıtı değildir. Client erişimi açılmadı.
3. **Kullanım geçmişi:** kupon ve askıda yemek kaynakları son 100'er kayıtla sınırlı. Ekran metni buna uygun düzeltildi. Gerçek tüm-zaman/gün/ay aggregate raporu eklenmedi.
4. **Index/rules yayını:** yeni kupon ve başvuru index tanımları hazır; canlı proje index `READY`, rules/functions deploy ve gerçek App Check enforcement bu review kapsamında yapılmadı. Emulator başarısı canlı index ihtiyacının/hazır olmasının kanıtı değildir.
5. **Cihaz/harici servis:** gerçek iPhone kurulum, Outlook açılışı, görsel QA, imzalı build, Brevo gerçek teslimat ve APNs credential canlı testi yapılmadı. Native simulator build ve mevcut unit/emulator testleri bu alanların tamamını doğrulamaz.
6. **Yarış testinin sınırı:** yeniden kullanılan kodun cleanup query snapshot'ından sonra başka kullanıcıya atanması transaction kaynak kodunda ele alındı. Tam bu araya deterministik reassignment sokan ayrı test koşulmadı; mevcut deletion/claim senaryoları geçti.

Bu kalan maddeler canlıya hazır raporu olarak yorumlanmamalıdır. Değişiklikler çalışma ağacında review edilebilir durumdadır; yayımlama veya commit kapsamı ayrıca belirlenebilir.
