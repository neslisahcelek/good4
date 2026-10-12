# İndirimler — commit öncesi review, 12 Ekim 2026

## Kapsam ve kullanıcı değişiklikleri

Base: 5b58b0b (main). İncelenen kapsam: yeni İndirimler özelliği ve ilgili çalışma ağacı patch’i. upstream/main: 1c43ec5; aradaki dört commit ayrıca korunacak.

Yemekhane import değişiklikleri, functions/index.ts içindeki yerel emülatör ve yemekhane satırları, web Firebase yerel önizleme bloğu, .claude/launch.json, firebase/community ve reklam-afisleri kapsam dışında bırakıldı. Finder kopyaları, gizli ortam dosyaları, cihaz/proje ayarları ve demo veri commit edilmez.

## Güvenlik bulguları

Engelleyici veya kalan bulgu yok. Aktif hesap/rol, işletme üyeliği ve sahiplik, öğrenci günlük hak ve toplam kota transaction’ları, paylaşılan dört haneli kod havuzu, versiyon/TTL, yalnız not_found hatasının deneme kotasını tüketmesi, yönetici audit atfı, Google Maps HTTPS adres allowlist’i ve hesap silme zinciri incelendi. Özel kod/hak/kullanım belgelerinin istemci okumaları kapalıdır. Sunucu harici Maps bağlantısına istek atmaz.

## Diğer bulgular

[Orta, düzeltildi] CouponTicketCard.kt: uzun işletme adı/indirim başlığı veya büyütülmüş metinde 164dp sabit yükseklik son tarih/aksiyon alanını sıkıştırıyordu. İndirim varyantında 164dp minimum yükseklik ve içeriğe göre satır yüksekliği kullanılır; topluluk kartının 104dp görünümü korunur.

Düzeltme sonrası kalan davranış bulgusu yok. Mimari/refactor geçişinde ek engelleyici öneri yok. UI/state/repository, hesap ve seçim nesli kontrolleri, iOS DTO serializer/timestamp çözümlemesi, Android/iOS DI/navigasyon, yerelleştirme, ayrı sayfalı admin işletme seçicisi ve alternatif kurallar kontrol edildi.

## Çalıştırılan kontroller

- iOS Simulator Kotlin + Android ProdDebug Kotlin: BUILD SUCCESSFUL.
- Ortak/Android birim testleri: 123/123 geçti; hata ve skip yok.
- Yayın paketindeki businessDiscounts, communityCoupons, campaigns, business, businessApplications, accountDeletion testleri: 91/91 geçti.
- Normal/bounded Firestore kural testleri: 28/28 geçti.
- Web TypeScript/Vite derlemesi başarılı; web testleri 9/9 geçti.
- Son sunucu/kural testleri yalnız demo-good4-v2 ve ayrı Firestore 8395 üzerinde çalıştırıldı; önizleme 8285 testlere verilmedi.
- Sunucu testinin ilk turundaki tek emülatör transaction-closed hatası temiz emülatörde test değiştirilmeden giderildi; son sonuç 91/91.
- Canlıda yedi servis ACTIVE, iki yeni indeks READY; panel paketi doğrulandı. Gerçek öğrenci kodu canlıda tüketilmedi. Simülatör demo ve mobil mağaza yayını ayrı tutuldu.
- git diff --check ve hedefli sır/yerel ayar taraması yapılır; commit index’i korunacak kaynaklardan ayrı hazırlanır.

## Karar

İndirimler değişikliği commit’e hazır. upstream/main’deki gizlilik, bildirim/analitik ve Sosyal düzeltmeleri birleştirilerek gönderilecek; birleşmiş mobil/web derlemeleri ayrıca kontrol edilecek.
