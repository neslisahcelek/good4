---
name: good4-code-review
description: Good4 V2 değişikliklerini güvenlik, doğruluk, KMP uyumu ve mimari regresyonlar açısından inceler. Kullanıcı review istediğinde ve commit/push/PR öncesinde kullan.
---

# Good4 code review

Review yaparken önce `AGENTS.md`, `.agents/skills/good4-architecture/SKILL.md` ve gerekiyorsa `docs/REPEATED_REVIEW_PATTERNS.md` oku. Güncel kod ve diff kanıttır; varsayımla bulgu yazma. İnceleme istenmişse kodu kendiliğinden değiştirme. Review tamamlanıp bulgular gösterilmeden commit/push yapma.

## Kapsam ve kullanıcı değişiklikleri

```bash
git status --short
git diff --stat
git diff
git diff --cached
git ls-files --others --exclude-standard
```

- Base/head ve değişen dosyaları belirle; gerekirse diff'in bağlamını kaynak dosyadan oku.
- Kullanıcının önceden var olan değişikliklerini ayır. Geri alma, izinsiz stage etme veya `git add -A` kullanma.
- `* 2.*` biçimli Finder kopyalarını ve gizli/yerel yapılandırmaları commit kapsamına alma.
- Her bulgu için değiştirilmiş satıra yakın dosya ve satır belirt; etki ve somut tetiklenme/istismar koşulunu açıkla.

## Güvenlik: repo özel kontrolleri

### Secret ve gizlilik

- Diff'te API key/token/parola/private key/service account/Bearer/`AIza...` benzeri sırları ara. `GoogleService-Info*.plist`, `google-services*.json`, `local.properties`, keystore, `.env*`, Firebase debug logları stage edilmemeli.
- `println`/log'larda token, e-posta, öğrenci numarası, konum veya özel içerik olmadığını kontrol et.
- Yeni kişisel veri `docs/legal` metinleriyle uyumlu mu, veri gereksiz/korumasız mı değerlendir.

### Firestore, Storage ve Cloud Functions

- İlgili kurallar: `firebase/v2/firestore.rules`, `firebase/v2/storage.rules`; toplulukta `firebase/community/*.rules`.
- `allow read/write: if true` veya yalnız `request.auth != null` ile açılan hassas erişim var mı? Sahiplik, `resource`/`request.resource`, alan allowlist'i, tür, uzunluk ve Storage boyut/contentType/path kontrolleri doğru mu?
- Kullanıcı rol, admin/community admin, doğrulama, sayaç veya puan alanını yazabiliyor mu? Başkalarının e-posta/telefon/öğrenci no gibi kişisel verileri listeleme sorgusundan açılıyor mu?
- Kuralların kapsadığı her hassas server/admin SDK callable veya HTTP yolunda auth + aktif hesap + rol + ownership denetimi var mı? Admin SDK rules'u atlar.
- İstemciden gelen UID/role/communityId gibi yetki girdilerine güveniliyor mu; IDOR veya block kontrolü atlanıyor mu? Mesaj/teklif/yanıt iki taraflı block denetimi yapıyor mu?
- Callable girdi şeması sınırlandırılmış mı? Hata stack/başka kullanıcı verisi/secret sızdırıyor mu? Harici çağrılarda SSRF, timeout ve yanıt doğrulaması var mı?
- Silme/cleanup kararı transaction'da güncel state ile mi veriliyor? Hesap önce `deleting` oluyor mu? Storage silmesi için durable retry kaydı var mı ve iş başarılı silmeden kaldırılıyor mu?
- `Promise.all` erken reject'i paralel yüklemeler sürerken cleanup başlatıyor mu? Gerekirse `allSettled` ile hepsinin bitişini bekle ve cleanup başarısızlığını kalıcılaştır.
- Cleanup sayfalaması silinen ilk sayfa yüzünden kilitleniyor mu? Sorgu snapshot'ı transaction'da yeniden doğrulanıyor mu? Kota oluşturma/yenileme/yeniden yayınlama yollarının hepsinde transaction ile korunuyor mu?
- Callable export adı birden çok özelliğin API'siyse payload açık ve doğrulanmış mı; export çakışması var mı?

### Mobil / KMP

- `commonMain`'de Android API/import, `Dispatchers.Main/IO`, Android lifecycle artifact'ı var mı? Lifecycle-aware collection ve platform actual'ları tamam mı?
- iOS/Android `platformModule` gerçek Auth/Firestore impl bağlıyor mu? iOS Koin, Compose lambda dışında mı? Firebase App Check, servisler kurulmadan önce mi?
- Firestore kullanan yeni DTO iOS serializer map ve timestamp/nested decoder'ına eklendi mi?
- UI gizlemesi tek güvenlik kontrolü mü? Deep link/WebView keyfi URL veya güvensiz JS bridge açıyor mu? ATS/certificate kontrolleri gevşetilmiş mi?
- Token/hassas veri düz metin tercihlerde mi? Yeni izin/entitlement dar kapsamlı ve Info.plist açıklaması ekli mi? Prod/staging flavor doğru Firebase projesine mi bağlı?
- iOS picker gerçek implementasyon mu, placeholder mı?

## Doğruluk, mimari ve kullanıcı deneyimi

- İş kuralı repository/UI içine sızmış mı? Repository yalnız veri erişimi; ViewModel iş/UI state; Composable state tüketip event gönderiyor mu?
- DTO → domain null fallback, `Result.Error` için anlaşılır localized UI hata yolu var mı?
- Filtre/arama değişince eski async sonuçlar veya cursor yeni sayfaya karışabilir mi? Request generation ve cancellation korunuyor mu? Sayfalama hatası görünür ve retry edilebilir mi?
- `StateFlow.update {}` kullanımı, lifecycle-aware collection, lazy list stable key ve ortak component kullanımını denetle.
- Kullanıcı metinleri `strings.xml`'de mi; kategori/status/error etiketleri presentation kaynak kimliğine map ediliyor mu? Compose resource formatında yüzde literal'i (`%%`) doğru mu?
- `!!`, zorla cast, boş/null/index sınırları, tekrar submit, loading/error/empty state, yarış ve yeniden deneme koşullarını kontrol et.
- Route/UserRole/NavGraph/home ve logout akışları tutarlı mı? Tema paleti ve mevcut ortak form/profile/card/button bileşenleri kullanılıyor mu?
- Hata olumsuz veya dayanıklılık sınırında ne olur? Örn. Storage/Firestore kısmi başarısızlık, hesap devre dışı, konuşma cleanup ile eşzamanlı yazı.

## Bulgulardan türetilen senaryolar

Değişen alana göre [tekrarlayan hata kalıplarının](../../../docs/REPEATED_REVIEW_PATTERNS.md) 5 Ekim bölümlerini oku ve somut tetikleyiciyi kaynak üzerinden izle:

- Yeni UID bağlantılı koleksiyon: hesap silme taraması, transaction'da sayaç azaltma ve tekrar silme; özel okumada `disabled`/`deleting` hesabın reddi. Normal ve bounded kurallar kullanılıyorsa ikisini de kontrol et.
- Asenkron okuma/yazma: ilk okuma yeni oy sonrası dönüyor; gün, kullanıcı veya seçilen topluluk değişiyor. İş iptali yanında sonucu uygulamadan önce nesil/sürüm doğrulamasını denetle.
- Zaman: ekran açılış saatini ve gece yarısını açık halde geçiyor; geçmiş taslak başlangıcı değiştirilmeden yayına alınıyor; çok günlük etkinlik başladıktan sonra hâlâ bitmemiş durumda.
- Form/liste: kapağın kaldırılması, görsel hazırlanırken submit, sistem geri tuşu ve öğrenci/yönetici devam sayfası. Kontrolün yeni UI yolunda erişilebilir olduğunu doğrula.
- Hata/boş sonuç: ağ/izin/decode hatası boş liste veya sıfır sayaca çevrilmemeli. İlk sayfa ve devam sayfası için görünür hata ve yeniden deneme olmalı.

Görsel değişikliğinde [good4-media](../good4-media/SKILL.md) içindeki dönüşüm ve hazırlama kontrollerini uygula. Sorgu/index değişikliğinde deploy sonucunu emülatör testinden çıkarma; yayın talebi varsa [good4-firebase-release](../good4-firebase-release/SKILL.md) üzerinden canlı hazır olma durumunu doğrula. Review tek başına deploy yetkisi vermez.

## Secret taraması için örnek

```bash
git diff --cached -U0 | grep -nEi 'api[_-]?key|secret|token|password|passwd|private[_-]?key|BEGIN [A-Z ]*PRIVATE|AIza[0-9A-Za-z_-]{20,}|service[_-]?account|client[_-]?secret'
```

Stage edilmiş diff yoksa aynı kontrolü incelemenin diff kapsamına göre staged/unstaged diff üzerinde yap.

## Doğrulama

- Yalnız değişiklikle ilgili mevcut test/derleme komutlarını seç; test/verify kullanıcı tarafından istenmediyse yeni test ekleme veya tüm suite'i gereksiz çalıştırma.
- Firebase kuralları/functions değişikliklerinde uygunsa `npm --prefix firebase/v2 run test:rules` ve `npm --prefix firebase/v2 run test:functions`.
- KMP değişikliğinde ilgili hedefi kullan; ör. `./gradlew :composeApp:compileKotlinIosSimulatorArm64` veya etkilenen Android/common target.
- Bağımlılık değişikliğinde paket kaynağı/sürümünü gözden geçir; Node production bağımlılıkları için uygun olduğunda `npm audit --omit=dev`.
- Çalışmayan/çalıştırılmayan komutları ve kısıtları açıkça yaz. Komutu çalıştırmadıysan başarılı gibi gösterme.
- Güvenlik kuralı değişince mevcut kural testlerini incele; yeni yetki reddi için negatif test eksikliğini bulgu olarak belirt.

## Rapor

Önce yalnızca düzeltilmesi gereken bulguları önem sırasıyla yaz:

`[Kritik|Yüksek|Orta|Düşük] dosya:satır — Sorun, gerçekleşme koşulu/etkisi ve uygulanabilir düzeltme.`

Sonra kapsamı, kontrolleri ve sınırlamaları özetle. Bulgu yoksa açıkça “Bulgu yok” de ve hangi ana alanları kontrol ettiğini belirt. Commit öncesi review ise kullanıcı değişikliklerini, güvenlik bulgularını, diğer bulguları, çalıştırılan kontrolleri ve “commit'e hazır / önce düzelt” kararını ayrı başlıklarla raporla. Kritik/Yüksek bulgu varken commit atma.
