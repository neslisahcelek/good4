# Good4 panel adresi

4 Ekim 2026'da kullanıcı kalıcı panel adresi olarak `panel.good4tr.com` seçti.

- Firebase Hosting sitesi ve proje: `good4tr-v2`.
- Özel alan adı Firebase'de zaten kayıtlı; yeni bir Hosting sitesi oluşturulmadı.
- Firebase Authentication yetkili alan adlarına `panel.good4tr.com` eklendi ve okunarak doğrulandı.
- `web/src/main.tsx` bu alan adında kök yolu yönetim portalına yönlendirir.
- Canlı web çıktısına yalnızca bu alan adı koşulu eklendi. Mevcut canlı dosyalar temel alındı; yerel, henüz yayımlanmamış landing değişiklikleri bu yayına alınmadı.
- TypeScript ve üretim JavaScript sözdizimi kontrolleri geçti. Yayından sonra ana modül ve HTML doğrulandı; diğer on dosyanın içeriği önceki canlı sürümle aynı.

## DNS bağlantısı

Kullanıcı aynı gün computer use ile kaydın değiştirilmesine açıkça izin verdi. GoDaddy'deki mevcut `panel` CNAME hedefi `enchanting-lollipop-9c3081.netlify.app` yerine `good4tr-v2.web.app` olarak güncellendi. GoDaddy başarı bildirimi ve kayıt satırındaki yeni değer görüldü; TTL 1 saat olarak korundu.

Kaydedilmiş kayıt:

| Tür | Ad | Yeni hedef |
| --- | --- | --- |
| CNAME | panel | good4tr-v2.web.app |

Kök alan adı ve `www` zaten Firebase'e bağlıdır. Diğer DNS kayıtları değiştirilmedi. Kanıt görüntüsü: `/Users/cankilinc/.codex/visualizations/2026/10/04/good4-panel-domain/godaddy-panel-dns.jpg`.

Kaydın değiştirilmesinden hemen sonraki Google DNS sorgusu eski hedefi döndürdü. Firebase de hâlâ `HOST_MISMATCH`, `OWNERSHIP_MISSING` ve `CERT_VALIDATING` durumlarını gösterdi; normal HTTPS isteği HTTP 404 döndü. DNS yayılımı ve sertifika tamamlanmadan adresin çalıştığı söylenmedi.

DNS değiştikten sonra Firebase özel alan adı durumunda `HOST_ACTIVE`, `OWNERSHIP_ACTIVE`, `CERT_ACTIVE` koşullarını ve normal HTTPS bağlantısını doğrula. Ardından kullanıcının kendi hesabıyla gerçek giriş kontrolü yapılmalıdır. Önizleme bağlantısındaki oturum kalıcı alan adına otomatik taşınmaz.

Yayın hazırlığı ve önceki canlı dosyaların kopyası: `/tmp/good4-panel-domain/`. Bu geçici dizin kalıcı yedek sayılmaz; Firebase Hosting sürüm geçmişi önceki yayına dönüş için kullanılabilir.
