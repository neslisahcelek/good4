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
- Lazy list öğelerine stabil `key` ekle. Görünürlük/scroll state'ini ViewModel iş state'inin yerine koyma.
- Önemli yeni Compose ekranlarında Preview ekle; birden fazla ekranda yinelenen yapıyı ortaklaştır.

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
