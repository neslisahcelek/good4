# Good4 mağaza görselleri — 5 Ekim 2026

Topluluk yeniden tasarımı, QR bilet, Günün Menüsü (😋 😐 😕 puanlama) ve topluluk yönetimi için hazırlanan set. Ders programı ve akademik takvim görselleri 27 Eylül setinden aynen alındı.

| Sıra | Dosya | Başlık | Alt yazı |
| --- | --- | --- | --- |
| 1 | `01-communities.png` | Kampüsteki etkinlikler / tek yerde | Toplulukları keşfet, yaklaşan etkinlikleri kaçırma |
| 2 | `02-ticket.png` | Kaydol, QR biletinle / hemen gir | Girişte biletini göster, sıra bekleme |
| 3 | `03-menu.png` | Yemekhane ve KYK / menüsü tek bakışta | Kahvaltıdan akşam yemeğine günün menüsü |
| 4 | `04-manager.png` | Topluluğunu / cebinden yönet | Etkinlik oluştur, kayıt ve girişleri takip et |
| 5 | `05-schedule.png` | Ders programın / hep cebinde | (27 Eylül seti) |
| 6 | `06-calendar.png` | Akademik takvimi / kaçırma | (27 Eylül seti) |

| Klasör | Boyut | Kaynak |
| --- | --- | --- |
| `ios/` | 1320 × 2868 (iPhone 6,9") | iPhone 17 Pro simülatörü |
| `ipad/` | 2064 × 2752 (iPad 13") | iPad Pro 13" (M5) simülatörü, `TARGETED_DEVICE_FAMILY=1,2` ile yalnızca çekim için |
| `android/` | 1080 × 1920 | Pixel 10 Pro emülatörü |
| `android-tablet-7/` | 1080 × 1920 | Aynı emülatör, 600 dp genişlik (288 dpi) |
| `android-tablet-10/` | 1440 × 2560 | Aynı emülatör, 800 dp genişlik (288 dpi) |

Google Play tablet alanları için uygulama dışı metin içermeyen `ham-ekranlar/` dosyaları kullanılmalıdır.

- Ekranlar yeni sürümün kaynağından (`daily-menu-ratings` dalı) yerel Firebase emülatörüne (`demo-good4-v2`) bağlı derlemeyle alındı. Topluluk adları, etkinlik afişleri, menü, oy ve kayıt sayıları örnek veridir; gerçek kullanıcı verisi içermez. Afişler Good4 renklerinde (krem zemin, koyu yeşil yazı) üretildi.
- Durum çubuğu 09:41'e sabitlendi; Android'de bildirim simgeleri kapatıldı.
- Bu görseller yeni sürüm (PR #6 ve #7) yayınlanınca mağazaya yüklenmelidir.
