# Store review öncesi kaynak kodu incelemesi

Amaç, değişen iOS/Android kodunda reviewer'ın uygulamayı kullanmasını engelleyebilecek somut sorunları bulmaktır. Bu inceleme build, gizlilik politikası veya kapsamlı yayın hazırlığı denetimi içermez.

## Kapsam ve kontroller

- İstenen platformun HEAD ve çalışma ağacı diff'ini belirle. Son yayın referansı kullanıcıdan veya mevcut güvenilir bilgiden biliniyorsa onu esas al; yoksa açık bir commit aralığı varsayımı belirt. Referans bulmak için mağaza/remote servis araştırmasına girişme ve kapsamı tüm repoya genişletme.
- `good4-code-review` ile hedef diff'i ve davranışı anlamak için gerekli yakın tüketicileri incele. Öncelik crash/null/index hataları, takılan loading, görünmeyen hata/retry, bozuk buton ve eksik navigasyondur.
- Değişen giriş/doğrulama/rol akışlarında reviewer'ın ilgili ekranlara ulaşmasını kesebilecek kararları izle. Test hesabıyla giriş yapma veya hesap bilgisi isteme; kaynakta görülen koşulları raporla.
- Değişen update akışında sürüm karşılaştırması, soft/force ayrımı, geri dönüş, timeout ve mağazadan dönüş/lifecycle davranışını kaynak üzerinden incele. Canlı flag/config değerlerini okumaya veya değiştirmeye çalışma.
- Yeni kullanıcı içeriği/hesap ekranı varsa şikâyet, engelleme ve hesap silme gibi mevcut kullanıcı aksiyonlarının ilgili ekrandan erişilebilirliğini ve event → ViewModel → repository zincirini kontrol et. Bunu genel politika/hukuk uygunluğu audit'ine dönüştürme.
- Değişen iOS kodunda expect/actual, DI, native callback, DTO serializer ve platform izin akışlarının eksik bağlanmasını kontrol et. Metadata, screenshot, SDK uyumluluk takvimi ve production yapılandırması bu modun kapsamı değildir.
- Yalnız inceleme istenmişse kodu değiştirme. Düzeltme ayrıca yetkilendirilmişse kapsam içindeki bulguları giderip diff'i yeniden incele; build/test ayrıca istenmedikçe çalıştırma. Kozmetik refactor önerilerini gönderim engeli sayma.

## Kısa rapor

Önce somut sorunları önem sırasıyla dosya/satır, tetikleyen koşul, kullanıcı etkisi ve düzeltmeyle ver. Ardından incelenen platform/commit aralığını ve kaynak incelemesinin sınırını belirt. Bulgu yoksa “İncelenen kodda store review açısından engel bulunmadı” de; bunu uygulamanın bütünüyle yayına hazır olduğu veya mağazadan kesin kabul alacağı şeklinde sunma.

Kapsam dışındaki build, cihaz, Console, backend ve gizlilik kontrollerini eksik işler listesine dönüştürme; yalnız kaynak kodu incelendiğini tek cümleyle belirt.
