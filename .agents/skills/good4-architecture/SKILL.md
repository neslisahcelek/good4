---
name: good4-architecture
description: Good4 V2'nin KMP, Compose, Firebase ve katmanlı mimari kurallarını uygular. Özellik tasarlarken, kod yazarken, refactor yaparken veya mimari karar verirken kullan.
---

# Good4 mimari rehberi

Bu skill, repodaki `AGENTS.md` ile birlikte okunur. Çelişki varsa kullanıcı talimatı ve güncel `AGENTS.md` önceliklidir. Önce mevcut örnekleri ve gerçek kaynak yollarını incele; eski dokümanlardaki yolu doğrulamadan kullanma. Yeni role, collection veya platform API'si eklerken akışın tüm katmanlarını takip et.

## Proje yapısı

- Mobil uygulama: Kotlin Multiplatform + Compose Multiplatform, ana kaynaklar `composeApp/src/commonMain`, `androidMain`, `iosMain`.
- Katmanlar: domain modelleri/kuralları, data DTO/repository/mapping, presentation Compose/ViewModel/UI state.
- Sunucu: Firebase V2 Cloud Functions `firebase/v2/functions`, Firestore kuralları `firebase/v2/firestore.rules`, Storage kuralları `firebase/v2/storage.rules`.
- Campus Closet ve topluluk akışları dahil mevcut ürün alanlarının kanonik veri modelini değiştirmeden önce `firebase/v2/CANONICAL_DATA_MODEL.md` ve yakın örnekleri incele.
- Tekrarlayan üretim ve review hataları için `docs/REPEATED_REVIEW_PATTERNS.md` esas alınır.

## KMP sınırları

- `commonMain` platformdan bağımsızdır. `android.*`, `com.android.*`, `Dispatchers.Main/IO` veya Android lifecycle artifact'ı ekleme. Coroutine işlerini `viewModelScope.launch` ile başlat.
- Flow UI'da `collectAsStateWithLifecycle()` ile toplanır; Flow Kotlin/Compose tarafında kalır, Compose Multiplatform için gereksiz Swift Flow köprüsü ekleme.
- Her `expect` için Android ve iOS `actual` gerekir. Platform seçicileri ve Firebase implementasyonları iki platformda da gerçek olmalı.
- `androidMain` ve `iosMain` platform modülleri gerçek `AuthRepository` ve `FirestoreRepository` implementasyonlarını bağlamalı. Common mock binding'ine güvenme.
- iOS `startKoin` ComposeUIViewController content lambda'sının dışında ve bir kez çalışır. Firebase/App Check servisleri ilk Firebase kullanımı öncesinde kurulur.
- Firestore kullanan DTO'ları iOS `FirestoreRepositoryIOSImpl.kt` serializer map'ine ekle. Timestamp alanları/nested DTO'lar için gerekli manuel decoder'ı da ekle. Callable HTTP JSON modellerini Firestore DTO'su sanma.
- Görsel seçici gibi platform UI'ları iOS'ta stub bırakılamaz; gerçek PHPicker/UIImagePicker akışını ve gerekli Info.plist izin açıklamalarını ekle.
- Bağımlılığı `commonMain`'e eklemeden her iki platform binary'sine gerekip gerekmediğini doğrula. Platform Firebase SDK bağımlılıklarını uygun source set'te tut.

## Katman ve UI state

- Repository veri erişimi yapar; ViewModel UI state ve kullanıcı aksiyonlarını yönetir; Composable state'i gösterip event iletir.
- UI state `StateFlow` içinde tutulur ve güncellenirken `MutableStateFlow.update {}` kullanılır. İş doğrulama ve iş kurallarını Composable'a taşıma.
- DTO'dan domain'e geçerken null alanlara güvenli fallback ver. UI'ya `Result.Error` ham exception'ı taşıma; anlaşılır, lokalize `UiText`/kaynak anahtarı üret.
- Kategori, durum ve hata metnini repository içinde kullanıcı metni olarak üretme. Compose kaynaklarını `composeApp/src/commonMain/composeResources/values/strings.xml` içinde tut; paleti `Colors.kt` üzerinden kullan.
- Ekran için mevcut ortak bileşenleri önce ara: `ProfileComponents`, `ProductFormFields`, `ProductListCard`, `StatCard`, `ImagePreviewBox`, `ButtonStandards`.
- Navigasyon değişikliğinde `Route`, NavGraph, `UserRole`, role home ve logout/geri dönüş akışlarını birlikte güncelle. Yeni role için benzer student/business/admin örneklerini izle.
- Yeni ekran eklerken `AnalyticsScreen` (`AnalyticsTracker.kt`) kataloğuna ekle ve `NavGraph` içindeki `LaunchedEffect(currentEntry?.destination)` rota eşlemesine bağla. Ekran alt sekmeleri varsa (ör. `NavigationBar`) tab geçişinde `analytics.screenView(...)` çağır.
- Ekranın iş akışlarında kritik buton, form ve sonuçları `Interaction` ve `AnalyticsTarget` katalog enum'ları üzerinden izle; ViewModel'e `analytics: AnalyticsTracker = NoOpAnalyticsTracker` enjekte et ve `Modules.kt` içinde `analytics = get()` tanımla. KVKK gereği serbest kullanıcı metni veya ID gönderme.
- Lazy list öğelerine stabil `key` ekle. Görünürlük/scroll state'ini ViewModel iş state'inin yerine koyma.
- Önemli yeni Compose ekranlarında Preview ekle; birden fazla ekranda yinelenen yapıyı ortaklaştır.

## Liste yükleme ve kategori geçişleri

- İlk yüklemeyi arka plan yenilemesinden ayır. Başarılı boş sonuç da yüklenmiş state'tir; boş listeyi her dönüşte yeniden tam ekran loading'e çevirme.
- Kategori cache'i yüklenen tüm öğeleri ve onlara ait cursor'ı birlikte saklamalı. Sayfalı bir genel listenin yerel alt kümesini tam kategori sonucu sayma; genel liste tamamlandıysa filtreyi ağ çağrısı olmadan türetebilirsin.
- Lifecycle resume ve tab effect aynı okumayı iki kez başlatmamalı. Yüklemeyi ViewModel'de tekilleştir; güncel tab cache'ini kullan, yeniden deneme ve yazma sonrası dönüşte yenile.
- Sık kullanılan komşu tabın ilk isteğini yalnız tıklamada başlatmak geçişi ağ süresine bağlar. Okuma maliyeti sınırlıysa ekran girişinde paralel hazırlamayı değerlendir; tab tıklaması devam eden isteği paylaşsın ve kişisel sonuç hesap bazında cache'lensin. Kişisel listeyi keşfet listesinden türetme veya bütün tabları koşulsuz önceden yükleme.
- Feed zaten ekran durumunu taşıyorsa her filtre değişiminde ayrıca özet çağırma. Paralel status okumaları profil/şart kabulü yazmasını ezmemeli; sonuçları kullanıcı ve veri/istek nesliyle doğrula. UID, kampüs doğrulaması veya ilgili yazma değişince eski cache'i geçersiz kıl.
- Cache gösterilirken yenileme hatasını gizleme. Devam sayfası hatasında öğeleri ve cursor'ı koruyup görünür yeniden deneme sağla.

## Firebase ve sunucu güven sınırı

- İstemci gizlenmiş buton, UID veya Firebase client rules tek başına yetkilendirme değildir. Admin SDK kullanan her hassas okuma/yazma yolunda hesap aktifliğini ve rol/ownership yetkisini sunucuda doğrula.
- Hesap `disabled`/`deleting` durumlarını pazarın askıya alınmasıyla karıştırma. Pasif hesap özel konuşma, favori ve inbox verisine erişemez.
- Mesaj, teklif ve yanıt gibi aynı iki tarafı etkileyen uçlar aynı taraflar arası block denetimini uygular.
- Callable girdisinde tür, uzunluk ve allowlist doğrula. Kimliği istemciden gelen `uid` yerine auth bağlamından al; başka kişinin kaynağı için IDOR/ownership kontrolü yap.
- Firestore transaction dışı snapshot ile silme/yazma kararı verme. İşlem içinde güncel durumu tekrar oku ve kararı atomik kaydet.
- Firestore + Storage/harici servis tek transaction değildir: kalıcı retry işi/outbox kaydı kullan, başarıya kadar koru ve tamamlanmamış işi başarı olarak bildirme.
- Hesap silmede önce transaction içinde `deleting` işaretle, yeni yazmaları kapat, sonra bağlı kayıtları temizle. Yeniden deneme kimlik doğrulamalı olmalı.
- Sayfalı cleanup'ta silinen kayıtların sorguda kalıp cursor'ı kilitlemediğini doğrula. Cursor ilerlet veya yalnız işlenmemiş kayıtları seç.
- Kota kontrollerini yarış koşullarına açık sorgu sayımlarıyla yapma. İlgili kullanıcı state belgesindeki transaction ile oluşturma, yenileme ve yeniden yayınlama geçişlerini seri hale getir; güncel `MARKET_LIMITS.maxActiveListings` kaynağını oku.
- External HTTP URL'lerini kullanıcı girdisinden doğrudan alma; timeout, yanıt doğrulaması, hata ayrıntısı sızıntısı ve SSRF riskini kontrol et.
- Firestore/Storage kurallarında sahiplik, alan allowlist'i, tip/boyut sınırı uygula; kullanıcının rol, admin, doğrulama, sayaç veya puan alanlarını değiştiremediğini doğrula.
- Yeni Firestore koleksiyonu için mevcut `getCollectionWithIds(...)` pattern'ini kullan. Veri modelini `CANONICAL_DATA_MODEL.md` ile uyumlu tut.

## Senaryo bazlı kurallar

Menü puanlama, etkinlik düzenleme, kişisel koleksiyon veya sayfalı ekran değişikliğinde [tekrarlayan hata kalıplarının](../../../docs/REPEATED_REVIEW_PATTERNS.md) “5 Ekim: veri yaşam döngüsü”, “5 Ekim: asenkron state ve zaman” ve “5 Ekim: form ve liste sözleşmeleri” bölümlerinden ilgili olanı oku. Yeni koleksiyonun hesap silme kapsamını, eski okumanın yeni yazmayı ezmesini, tarih/kullanıcı değişimini ve alanı göndermemek ile boş göndermek arasındaki farkı tasarımda ele al.

- Görsel seçme, hazırlama veya yükleme değişikliğinde [good4-media](../good4-media/SKILL.md) rehberini kullan.
- Firebase deploy istenirse [good4-firebase-release](../good4-firebase-release/SKILL.md) rehberini kullan. Bu yönlendirme kendiliğinden deploy yetkisi vermez.

## İçerik ve istek yaşam döngüsü

- Yeni keşfedilebilir içerik türünü bloklama/erişim filtresine bütün yollarıyla bağla: ilk liste, öne çıkan kart, anlık bloklama ve doğrudan seçim. Bağımsız bir koleksiyonun yükleme hatası, komşu başarılı listenin verisini kapatmamalı; kendi lokalize hata ve yeniden deneme state'ini taşımalı.
- İstek nesli yalnız başarı sonucunu değil hata, cursor ve loading/finally güncellemelerini de korumalı. Aynı UID'ye yeniden giriş veya A→B→A sekme dönüşünde eski istek yeni oturum/nesille eşleşmemeli; yazma sonrası refresh'i yazmadan önce başlayan okumayla tekilleştirme. Yeni nesil, iptal ettiği isteğin loading sahipliğini de devralmalı.
- Doğrulama gibi devam akışlarında proof pencere açılmadan önce gelebilir; pending intent/pencere görünürlüğü değişince devam aksiyonunu da değerlendir. Zamana bağlı kart metinlerini tek canlı saatten türet; gece yarısı ve lifecycle dönüşünü per-card `remember` saatiyle dondurma.
- Yeni debug/config/scheme sürümünü kopyalarken kullanılan Firebase ortamı ve zorunlu güncelleme kontrolüyle karşılaştır; production'a bağlı debug yapılandırması eski uygulama sürümünde kalmamalı. Paylaşılan kurulum betiklerinde geliştiriciye ait mutlak checkout yolu veya varsayılan cihaz kimliği yerine kaynak ağacını ve çıktı ortamını doğrula, cihazı açık girdi olarak al.

Somut tetikleyiciler için [11 Ekim kupon/başvuru kalıpları](../../../docs/REPEATED_REVIEW_PATTERNS.md#11-ekim-kupon-başvuru-ve-teslimat-sözleşmeleri) bölümünü kullan.

## Değişiklik akışı

1. İlgili source set'leri, route/state/repository zincirini, platform binding'lerini ve yakın testleri keşfet.
2. Değişiklik planında etkilenen platformları, veri güven sınırını ve geriye dönük uyumluluğu belirle.
3. En küçük tutarlı değişikliği uygula; string/renk/ikonları merkezileştir.
4. Sadece değişen alana uygun doğrulamayı çalıştır. Kullanıcı test istemediyse test ekleme veya gereksiz tam suite çalıştırma.
5. Derleme/doğrulamayı çalıştırmadıysan sonucu açıkça belirt; çalışmış gibi raporlama.

## Kapanış kontrolü

- commonMain platform bağımsız mı; iOS ve Android actual/DI tamam mı?
- State tek yönlü mü; ViewModel iş state'inin sahibi mi?
- Strings, renkler, ortak bileşenler ve stabil lazy key'ler kullanıldı mı?
- Sunucu tarafı auth/role/ownership/block/aktif hesap kontrolleri eklendi mi?
- Transaction, retry, cursor, kota ve paralel başarısızlık senaryoları doğru mu?
- Firestore DTO serializer/decoder, rules ve canonical schema gereken yerlerde eşleşiyor mu?
- Gerekli Info.plist, flavor ve kaynak güncellemeleri yapıldı mı?
- Yeni ekran eklendiyse `AnalyticsScreen` ve `NavGraph` screen tracking eşlendi mi; ViewModel etkileşimleri kataloglanmış enum'larla bağlandı mı?
