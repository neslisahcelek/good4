---
name: good4-media
description: Good4 KMP görsel seçme, hazırlama, yükleme ve kaldırma akışlarını uygular veya inceler. Android/iOS picker, kapak ve fotoğraf dönüşümü değişikliklerinde kullan.
---

# Good4 görsel akışları

Önce mevcut picker ve yükleme çağrısını Android, iOS, commonMain ve sunucu boyunca izle. Repodaki gerçek yolları doğrula; bir özelliğin picker'ını tüm uygulamaya kopyalama. Temel mimari için [good4-architecture](../good4-architecture/SKILL.md), somut hata örnekleri için [tekrarlayan hata kalıplarının](../../../docs/REPEATED_REVIEW_PATTERNS.md) “5 Ekim: görsel hazırlama” bölümünü kullan.

## Piksel ve platform

- Android'de ham decode sonrası EXIF bilgisini atan yeniden JPEG üretimi yön/ayna bilgisini kaybettirir. Decode API'si bunu zaten uygulamıyorsa EXIF dönüşünü ve aynalamayı piksele uygula; iki kez döndürme.
- Büyük kaynak görseli tam boy belleğe almadan önce boyutlarını oku ve hedefe göre örnekle. Stream'leri kapat; yalnız sahip olduğun, artık kullanılmayan geçici bitmap'leri geri bırak.
- iOS picker gerçek seçme/iptal akışını sağlamalı. Kullanılan API gerektiriyorsa izin açıklamalarını doğrula; Android çözümünü commonMain'e taşıma.
- Görsel biçimi, byte boyutu, hedef oran ve sunucu sınırlarını mevcut ortak spec'ten al. EXIF okunamaması ile piksel decode edilememesini ayır; okunabilir görseli yalnız EXIF yok diye reddetme.

## Seçimden kayda sözleşme

- Hazırlama ile yükleme farklı aşamalardır. Hazırlama tamamlanmadan kayıt aksiyonu etkinleşmemeli; ViewModel submit yolu da eksik payload ile kaydı önlemeli.
- Kullanıcı ikinci görseli seçer, kaldırır veya formu kapatırsa önceki hazırlamanın sonucu yeni seçimi ezmemeli. İptal ve seçim nesli/sürümü kontrolü kullan.
- “Alan yok = mevcut değeri koru”, “boş değer = kaldır”, “yeni değer = değiştir” sözleşmesini istemci payload'ı ve sunucuda birlikte doğrula. URL alanını koşullu atlamak kaldırma işlemini sessizce etkisiz bırakmamalı.
- Alanlar, pending görsel ve hata iş state'inde tutulmalı. Picker görünürlüğü gibi geçici UI state'i ayrı kalabilir; sistem geri tuşu ve üst kapatma aynı kayıp-onay davranışını sağlamalı.

## Sunucu ve doğrulama

- Admin SDK yüklemesinde aktif hesap, rol/ownership, object path ve byte içerik/boyut denetimini sunucuda uygula. Client Storage kuralları callable yetkisini telafi etmez.
- Değişiklik Storage silmesi içeriyorsa başarısız temizliğin kalıcı retry kaydını incele. URL'yi kaldırmak ile dosyayı silmek aynı işlem değildir; ürünün mevcut saklama politikasını koru.
- Değişen platformları derle. Cihaz doğrulaması gerekiyorsa döndürülmüş/aynalı fotoğraf, büyük görsel, iptal, hızlı ikinci seçim, hazırlama sırasında submit ve kaldırıp tekrar açma senaryolarını seç.
- Yalnız derleme yapılmışsa EXIF/galeri akışı cihazda doğrulanmış gibi raporlama. Review talebinde bulguları bildir; açık düzeltme yetkisi yoksa kod değiştirme.
