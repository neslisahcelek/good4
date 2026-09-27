# Good4 V2 — gerçek iPad ekranlarından mağaza görselleri

27 Eylül 2026 tarihinde **iPad Pro 13-inch (M5), iOS 26.5 simülatöründe çalışan Good4 V2** uygulamasından alınmıştır. iPhone görselleri kullanılmamıştır. Altı tasarım ve altı ham ekran **2064 × 2752 px**, dikey, RGB PNG biçimindedir; şeffaflık veya kişisel bilgi taşıyan PNG metaverisi yoktur.

Tasarım önceki mağaza setiyle aynı yeşil arka plan ve Türkçe metinleri kullanır. Çerçeve iPad oranındadır; Dynamic Island içermez. Ham uygulama ekranları yalnızca orantılı ölçeklenerek çerçeveye yerleştirilmiştir. Uygulamanın metinleri, sayıları, ikonları ve içerikleri değiştirilmemiştir.

## Dosyalar

| Sıra | Mağaza görseli | Kaynak | Başlık | Alt yazı |
| --- | --- | --- | --- | --- |
| 1 | [01-home.png](01-home.png) | [Ham ekran](ham-ekranlar/01-home.png) | Kampüs hayatın / tek uygulamada | Hava durumu, günün menüsü ve kısayollar ana sayfanda |
| 2 | [02-schedule.png](02-schedule.png) | [Ham ekran](ham-ekranlar/02-schedule.png) | Ders programın / hep cebinde | Bölümüne ve sınıfına göre; derslik ve hocasıyla |
| 3 | [03-menu.png](03-menu.png) | [Ham ekran](ham-ekranlar/03-menu.png) | Yemekhane ve KYK / menüsü tek bakışta | Kahvaltıdan akşam yemeğine günün menüsü |
| 4 | [04-map.png](04-map.png) | [Ham ekran](ham-ekranlar/04-map.png) | Kampüste yolunu / kolayca bul | Fakülte, yemekhane ve ATM'ler tek haritada |
| 5 | [05-calendar.png](05-calendar.png) | [Ham ekran](ham-ekranlar/05-calendar.png) | Akademik takvimi / kaçırma | Sınav, tatil ve kayıt tarihleri bir arada |
| 6 | [06-edit.png](06-edit.png) | [Ham ekran](ham-ekranlar/06-edit.png) | Ana sayfanı / kendine göre düzenle | Kısayolları sırala, gizle ya da ekle |

## Çekim ve derleme bilgisi

- Kaynak: `v2` dalı, `3cef82c` commit'i.
- Uygulama: `com.good4.iosApp.v2`; `iosApp V2` şeması ve `DebugV2` yapılandırması.
- Simülatör: `F8F2F709-7180-43AD-B5D7-9EC275AEC6BB`; yerel iPad arayüzü 1032 × 1376 pt / 2064 × 2752 px.
- Açık tema; durum çubuğu 09:41. Tarih, hava durumu, menü ve ders verileri uygulamanın gerçek verileridir. Ders Programı ekranında 28 Eylül Pazartesi seçilmiştir. Günün Menüsü 27 Eylül verisini gösterir.
- Ekranlar XCUITest aracılığıyla uygulamada gezinerek alınmıştır. Altı ekranı kapsayan test başarıyla tamamlanmıştır.

**Yayın ayarı notu:** Projenin mevcut hedef cihaz ailesi yalnızca iPhone'dur (`TARGETED_DEVICE_FAMILY=1`). Bu çekimde uygulamayı yerel iPad arayüzünde çalıştırmak için derleme komutunda `TARGETED_DEVICE_FAMILY=1,2` geçici olarak verilmiştir. Kaynak proje ayarları değiştirilmemiştir. Bu dosyalar iPad önizleme derlemesinin gerçek ekranlarıdır; iPad desteğinin mağaza sürümünde açıldığı anlamına gelmez. iPad yayını yapılacaksa hedef cihaz ailesi ve iPad uyumluluğu ayrıca tamamlanmalıdır.

Derleme Desktop dışında DerivedData kullanılarak başarıyla tamamlandı:

```sh
xcodebuild -workspace iosApp/iosApp.xcworkspace \
  -scheme "iosApp V2" -configuration DebugV2 \
  -destination 'platform=iOS Simulator,id=F8F2F709-7180-43AD-B5D7-9EC275AEC6BB' \
  -derivedDataPath "$HOME/Library/Developer/Xcode/DerivedData/Good4DesignQA" \
  TARGETED_DEVICE_FAMILY=1,2 ENABLE_USER_SCRIPT_SANDBOXING=NO \
  CODE_SIGN_IDENTITY=- build
```

## Doğrulama

- 12/12 PNG: 2064 × 2752 px, RGB, şeffaflık yok; PNG parça türleri yalnızca IHDR/IDAT/IEND.
- Ham ekranların piksel değerleri XCUITest kayıtlarıyla birebir karşılaştırıldı.
- Tasarımların yuvarlatılmış köşeler dışındaki ekran alanı, ham görüntünün tek bir normal ölçeklemesiyle piksel düzeyinde karşılaştırıldı.
- Başlıklar ve alt yazılar verilen altı metinle aynı kaynaktan çizildi; Türkçe karakterler ve görsel yerleşim kontrol edildi.
- Mobil kaynak, Firebase, kurulum betiği veya bağımlılık değişikliği bu sete dahil değildir.

Boyut kaynağı: [Apple App Store Connect ekran görüntüsü ölçüleri](https://developer.apple.com/help/app-store-connect/reference/app-information/screenshot-specifications).
