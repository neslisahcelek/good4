# Good4 mağaza incelemesine gönderim kontrolü

Bu rehber yalnız gönderim öncesi denetimde okunur. Ana SKILL.md'nin aday, paket, cihaz, kanıt ve yetki kuralları geçerlidir. Kontrol çalıştırmak upload, submit veya canlı flag değişikliği yetkisi vermez.

## Aday ve reviewer erişimi

- App Store Connect veya Play Console'da gönderim için seçilen build/version, bundle/package, işlem/validation durumu ve hedef kanalın incelenen adayla eşleşmesini doğrula. Console erişimi yoksa yerel paketin doğrulanmasını Console doğrulaması gibi sunma.
- Önceki ret mesajı erişilebiliyorsa ilgili gerekçeyi bu adayda yeniden kontrol et. Hesap sözleşmeleri veya eksik Console alanlarını geçmiş dokümandan güncel engel sayma; mevcut durumu doğrula.
- Yetkili test hesabıyla reviewer'ın ilk açılış → giriş → rol ana ekranı → değişen özellik yolunu izle. .edu.tr doğrulaması, üniversite/rol kısıtları, MFA/OTP ve konum gereksinimlerinin inceleme erişimini kesip kesmediğini kontrol et. Erişim kısıtlarını review sırasında gizlemek için auth/rules gevşetme; gerekli meşru erişim adımlarını inceleme notuna koy.
- Review hesabının aktif ve gerekli doğrulamaları tamamlanmış olduğunu, örnek içeriğin özellikleri değerlendirmeye yettiğini doğrula. Parolaları Git'e, rapora veya genel inceleme notuna yazma; mağazanın ilgili güvenli erişim alanlarını kullan. Hesap oluşturmak veya veri yazmak için mevcut yetkiyi kontrol et.
- Güncel `app_config/update_notice` minimum sürümlerinin aday build'i kilitlemediğini ve mağaza bağlantılarının doğru uygulamayı açtığını doğrula. Mağazada henüz mevcut olmayan sürüme force update yönlendirmesi gönderim riskidir. Okuma hatası ve mağazadan dönüş davranışını incele; kontrolü geçirmek için canlı flag'i kendiliğinden değiştirme.

## Ürün davranışı ve beyanlar

- Hesap oluşturma varsa uygulamadaki hesap silme yolunu ve mağazanın güncel silme beyanı/bağlantı gerekliliklerini kontrol et. Gerçek silme testi yalnız yetkili disposable test hesabıyla yapılır. Kaynak incelemesi veri silmenin canlı kanıtı değildir.
- Sosyal, mesajlaşma, topluluk ve Kampüs Dolabı kapsamında kullanıcı/içerik bildirme, engelleme, moderasyonun yetkili yöneticiye ulaşması ve destek yolunu izle. Güncel UGC politikasının bu adaya uygulanabilir şartlarını resmi kaynakla eşleştir; yalnız bir rapor butonunun bulunmasını tüm moderasyon şartlarının karşılanması sayma.
- Privacy/Data Safety/App Privacy beyanlarını adayın topladığı/paylaştığı veriler ve SDK davranışıyla karşılaştır. Yeni sohbet, profil, görsel ve push verilerinin metinlerde karşılığı var mı incele. İzin açıklamaları ve PrivacyInfo.xcprivacy/required-reason API beyanlarını mevcut SDK/paketle doğrula.
- Gizlilik, destek ve uygulanabilir hesap silme URL'lerini gerçekten aç; HTTP başarı koduna ek olarak sayfanın doğru içerik ve erişilebilirlik sunduğunu kontrol et. `docs/legal` veya eski metadata taslağını canlı sayfa kanıtı kabul etme.
- Açıklama, What's New, screenshot, yaş/içerik derecelendirmesi, reklam ve ödeme beyanları adayın görünür özellikleriyle eşleşmeli. `docs/app-store-v2-metadata-draft.md` taslaktır; eski V1 akışları ve eksik sosyal özellik anlatımı varsa belirt. Mağazada kaydedilmiş alanları ayrıca doğrula.
- Mevcut Apple/Google/e-posta giriş seçeneklerini ve iptal/hata akışlarını kontrol et. Giriş seçeneklerinin mağaza politikasına uygunluğunu güncel resmi şartlara ve uygulanabilir istisnalara göre değerlendir.

## Kapanış

Ana rapora mağaza gönderimi sonucunu ekle: aday/build ve hedef mağaza, engeller, doğrulanamayan alanlar, reviewer'ın izleyeceği kısa yol ve gönderimden önce tamamlanması gereken somut adımlar. Hassas erişim bilgisi ekleme. Yerel teknik hazırlık ile Console/gönderim hazırlığını ayrı belirt; zorunlu kontroller eksikse koşulsuz hazır deme. Salt kozmetik önerileri gönderim engeli yapma.

## Resmi kaynaklar

Aşağıdaki bağlantılar 10 Ekim 2026 tarihinde kontrol edildi. Skill her çalıştırıldığında ilgili güncel gereklilikleri tekrar doğrula; burada sabit SDK/API son tarihi veya evrensel politika istisnası varsayma.

- [Apple App Review Guidelines](https://developer.apple.com/app-store/review/guidelines/): tamamlanmış uygulama, UGC, giriş ve gizlilik şartları.
- [Apple: Complete review information](https://developer.apple.com/help/app-review/before-submitting-for-review/complete-review/): reviewer erişimi ve gerekli inceleme bilgileri.
- [Apple: Account deletion](https://developer.apple.com/support/offering-account-deletion-in-your-app/): uygulama içi hesap silme.
- [Google Play: Prepare your app for review](https://support.google.com/googleplay/android-developer/answer/9859455?hl=en): erişim yönergeleri, metadata ve App content beyanları.
- [Google Play: User Generated Content](https://support.google.com/googleplay/android-developer/answer/9876937?hl=en): içerik/ kullanıcı bildirme, engelleme ve moderasyon.
