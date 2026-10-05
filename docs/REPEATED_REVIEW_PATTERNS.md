# Tekrarlayan code review hata kalıpları

Bu notlar, 4 ve 5 Ekim 2026 incelemelerinde doğrulanan hata kalıplarından çıkarıldı. Değişen özelliğe uygun bölümü uygulayın. 5 Ekim bulgularının kanıtı ve kapsamı [daily-menu-ratings incelemesinde](code-review-daily-menu-ratings-2026-10-05.md) bulunur.

## Firestore ve arka plan işleri

- Güvenlik kontrolünü sadece ekranda veya callable girişinde bırakmayın. Her hassas okuma/yazma yolunda hesabın aktifliğini ve gereken rolü sunucuda doğrulayın.
- Firestore transaction dışında alınan snapshot'ı silme/yazma kararında güncel kabul etmeyin. Durumu transaction içinde tekrar okuyup uygunluğu aynı transaction'da doğrulayın.
- Storage veya harici servis temizliğini ana Firestore kaydıyla atomikmiş gibi ele almayın. Kalıcı bir iş kaydı bırakın, başarılı olana kadar tekrar deneyin ve kullanıcıya tamamlanmamış silme için başarı bildirmeyin.
- Sayfalı temizlikte silinen kayıtların sorgu sırasını/cursor'ı kaydırabileceğini hesaba katın. Cursor ile ilerleyin ve boş/bitmiş sayfayı doğru ayırt edin.
- Kota ve eşzamanlı durum geçişlerini ayrı sorgu sayımlarıyla korumayın. Aynı kullanıcı state belgesinde transaction kullanarak oluşturma, yenileme ve yeniden etkinleştirme yollarını seri hale getirin.
- Callable isimleri iki özelliğin ortak API'si olduğunda payload biçimini belirsiz bırakmayın. Şemayı sürümleyin veya açık bir uyarlayıcıda doğrulanmış alanlara göre yönlendirin; aynı export adını iki kere tanımlamayın.

## KMP ve Compose

- Firebase App Check sağlayıcısını Firebase servisleri ilk kez oluşturulmadan önce kurun; platform başlatma kodunu `Application`/uygulama girişinde ve Compose lambda dışında tutun.
- Aynı isimli native bridge'leri farklı özellikler için paylaşmayın. Ortak paketlerde bridge ve arayüz adlarını özellik kapsamına göre ayırın.
- `commonMain` Android API'si veya platform dispatcher'ı kullanmamalı. UI `StateFlow` değerlerini lifecycle-aware collection ile okumalı; iş state'i ve doğrulama ViewModel'de kalmalı.
- Kullanıcıya görünen metinleri Compose kaynaklarına koyun. Repository/API katmanında ham kodları UI metni gibi taşımayın; ekranda lokalize hata sunun.

## Güvenlik ve dal entegrasyonu

- Dal birleştirmeden önce gerçek dal referansını ve merge-base'i doğrulayın. Eski/atasal dal yeni değişiklik getirmeyebilir; App Check gibi istenen özelliğin gerçekten hangi ref'te olduğunu dosya ve commit üzerinden kanıtlayın.
- Aynı FCM cihaz token'ı birden fazla kayıt biçiminde bulunabiliyorsa gönderimi token'a göre tekilleştirin; cihaz bazlı tercih ve sahiplik kontrollerini koruyun.
- App Check kodunun eklenmesi enforcement'ın açıldığı anlamına gelmez. Enforcement, Console ayarı ve canlı token doğrulaması ayrı ve açıkça doğrulanmalıdır.

## 5 Ekim: veri yaşam döngüsü

- Yeni UID bağlantılı koleksiyon veya alt koleksiyon eklendiğinde hesap silme kapsamını aynı değişiklikte izle. Oy belge kimliğinde UID bulunması da kişisel bağlantıdır; yalnız `userId` alanını aramak yeterli değildir.
- Hesap `deleting` olduktan sonra bağlı veriyi cursor ile tara. Oy/kayıt silinmesi ve bağlı sayacın azaltılması aynı transaction'da mevcut kayıt doğrulanarak yapılmalı; tekrar silme sayacı ikinci kez azaltmamalı.
- Geçerli token, aktif hesap anlamına gelmez. Kişisel okumada `disabled`/`deleting` durumları reddedilmeli; genel sayaçların okuma politikası ayrıca korunmalı. Alternatif rules dosyaları varsa sözleşmenin ikisinde de karşılığını kontrol et.

## 5 Ekim: asenkron state ve zaman

- İlk okuma yeni yazma sonrası dönebilir. Sonucu uygulamadan istek neslini, kullanıcıyı, gün/öğün veya seçilen topluluğu ve gerekiyorsa yazma sürümünü doğrula. Job iptali tek başına bütün geç dönmeleri önlemez; `CancellationException`'ı yutma.
- Gece yarısında önceki günün ekranından gönderilen oy yeni güne yazılmamalı. İstek gösterilen tarihi taşımalı ve sunucu özellik için geçerli zaman dilimiyle doğrulamalı. Eski istemci uyumluluğuna açıkça karar ver.
- Zamana bağlı buton yalnız composition anında saati okumamalı. Sayfa açıkken açılış saati/gün değişimini ve lifecycle dönüşünü güncelle; sunucu/ViewModel doğrulamasını görsel buton durumuna bırakma.
- Taslaktan yayına geçişte başlangıç alanı değişmese de geçmiş tarih kontrolünü çalıştır. Devam eden yayınlanmış etkinliğin değişmeyen başlangıçla düzenlenmesini bu kuraldan ayır. Çok günlük etkinlik keşfi başlangıçtan sonra da görünmeli; sorgu bitiş tarihini kapsamalı.

## 5 Ekim: form ve liste sözleşmeleri

- Güncellemede alanın gönderilmemesi, boş gönderilmesi ve yeni değer gönderilmesi farklı niyetler olabilir. Kapak kaldırmada boş `imageUrl` sunucuya ulaşmalı; repository'nin boş alanları atlaması mevcut kapağı yanlışlıkla korumamalı.
- Yeni UI düzeninde ilk sayfa ve devam sayfasını her rol için izle. Öğrenci butonunun yalnız yönetici ekranına taşınması sessiz bir erişim regresyonudur. Yeni sayfanın kayıt/katılımcı gözlemleri de başlamalı.
- Form alanları, doğrulama ve pending görsel iş state'inde kalmalı. Sistem geri tuşu ile üst kapatma aynı kaydedilmemiş-değişiklik davranışını kullanmalı. ViewModel state'ini süreç öldürülmesinden sonra kalıcı kurtarma gibi anlatma.
- Yalnız başarılı boş sorgu boş liste/sıfır sayaçtır. Ağ, yetki, decode ve hazır olmayan index hatalarını boş sonuca çevirme. Belge bulunamadı sonucu gerçekten yokluğu temsil ediyorsa ayrı ele al. UI yükleniyor/boş/hata durumlarını kaynak metniyle ayırmalı; hatada yeniden deneme olmalı.

## 5 Ekim: görsel hazırlama

- JPEG'e yeniden kodlama EXIF yön bilgisini kaldırır; decode zaten uygulamıyorsa dönüş/aynayı piksele aktar. Büyük fotoğrafı hedefe göre örnekle ve geçici kaynakları bırak.
- Fotoğraf seçimi sonrası hazırlama tamamlanmadan submit, payload'ın görselsiz kaydedilmesine yol açabilir. Hazırlama/yükleme durumlarını ayır; ilgili kayıt aksiyonunu ve submit yolunu koru.
- Sonradan biten eski seçim yeni seçimi veya kaldırmayı geri almamalı. Seçim nesli ve iptali kontrol et. Galeri/EXIF davranışını yalnız derleme ile doğrulanmış sayma.

## 5 Ekim: yayın ve hazır olma

- Composite index deploy'unun başarılı olması index'in kullanıma hazır olması değildir. Yeni sorguyu kullanan istemci yayınından önce canlı index `READY` olmalı; emülatör index ihtiyacını doğrulamaz.
- 5 Ekim cihaz logunda `events where status==published and endsAt>=...` sorgusu index `CREATING` iken `FAILED_PRECONDITION` verdi. Bu durum “etkinlik yok” diye gösterilmemeli; index hazırlandıktan sonra yeniden deneme sorguyu yenilemeli.
- Deploy'u açık proje ID'siyle yap, ortak servis değişikliklerinin etkilediği exports'ı birlikte kapsa ve her hedefin sonucunu kontrol et. Alternatif rules dosyası aktif `firebase.json` kaynağı değilse yayınlanmış sayılmaz. Backend deploy, mobil uygulama build/mağaza yayını değildir.
