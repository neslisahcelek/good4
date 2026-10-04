# Tekrarlayan code review hata kalıpları

Bu notlar, 4 Ekim 2026 code review'unda tekrar görülmeye açık mimari ve güvenlik hatalarından çıkarıldı. Yeni backend ve Campus Closet değişikliklerinde aynı kontrolleri uygulayın.

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
