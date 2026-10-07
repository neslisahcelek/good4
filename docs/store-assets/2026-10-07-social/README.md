# Good4 Sosyal — mağaza görselleri, 7 Ekim 2026

27 Eylül / 5 Ekim setindeki aynı limon yeşili geçiş, koyu yeşil yuvarlak başlık ve telefon çerçevesi korunur. Önceki set değiştirilmez. Uygulama ekranı yeniden çizilmez; gerçek emülatör/simülatör görüntüsü oranı bozulmadan çerçevede ölçeklenir. Başlık ve alt yazılar shared strings.xml içindeki social_store_* kaynaklarından okunur.

| Sıra | Başlık | Alt yazı | iOS | Android |
| --- | --- | --- | --- | --- |
| 07-social | Kampüste plan yap, / birlikte katıl | Spor, masa oyunları ve kahve buluşmaları | Hazır | Hazır |
| 08-social-chat | Birlikte planla, / birebir konuş | İsteğin kabul edilince sohbet başlar | Hazır | Hazır |

## Hazır dosyalar

- App Store: [Sosyal akış](ios/07-social.png), [birebir sohbet](ios/08-social-chat.png), **1320×2868 px**. [Önizleme](ios-onizleme.png).
- Google Play: [Sosyal akış](android/07-social.png), [birebir sohbet](android/08-social-chat.png), **1080×1920 px**. [Önizleme](android-onizleme.png).
- [Yükleme PNG paketi](good4-social-store-png.zip): dört tam boyutlu PNG. Ham ekranlar ve düzenlenebilir SVG'ler platform klasörlerinde ayrıca korunur.

Çekimler: iPhone 17 Pro / iOS 26.5 / iosApp Test ve Pixel 10 Pro / Android 17 API 37.1 / com.good4.test. Her platform kendi gerçek ekran görüntüsünü kullanır; yalnız demo-good4-v2 ve örnek öğrenci hesabı. Android'in eski yerel oturumu emülatör sıfırlaması sonrası geçersiz olduğundan test uygulamasının verisi temizlenip örnek Elif hesabıyla yeniden giriş yapıldı. Telefon kurulumu yapılmadı.

Dört PNG gözle kontrol edildi: başlık/alt yazılar sığıyor, ekranın oranı korunuyor, kartlar aynı ölçüde iki sütun ve sohbet maskeli ad/private profil fotoğrafıyla görünüyor. Çıktılar 24 bit RGB PNG; alpha, EXIF, XMP ve IPTC yok. Önizlemeler yükleme dosyası değildir.

İki yeni görsel, önceki altı görsele eklenerek sekiz görsel oluşturacak şekilde planlandı. Boyut referansları: [Apple screenshot specifications](https://developer.apple.com/help/app-store-connect/reference/app-information/screenshot-specifications/), [Google Play preview assets](https://support.google.com/googleplay/android-developer/answer/9866151/add-preview-assets-to-showcase-your-app?hl=en).

Yeniden üretim: kökten `node docs/store-assets/2026-10-07-social/render.mjs`. Ham çekimler platform/ham-ekranlar altında; renderer başlıkları shared strings.xml'den okur ve PNG boyutu/RGB/alpha kontrolünü yapar. Mağazaya yükleme veya mobil sürüm yayını yapılmadı.
