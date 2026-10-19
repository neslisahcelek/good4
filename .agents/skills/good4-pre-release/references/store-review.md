# Store review öncesi kaynak kodu ve uyumluluk incelemesi

Bu rehber, değişen iOS ve Android kodunda ve yapılandırmasında Apple App Store ve Google Play Store inceleme görevlilerinin (reviewer) uygulamayı reddetmesine yol açabilecek sorunları denetler. 

Tam kapsamlı mağaza gereksinimleri ve aksiyon planı için **[good4-store-compliance](../../good4-store-compliance/SKILL.md)** skill'indeki REQ-01..REQ-20 denetim standartları esas alınır.

## Kapsam ve kontroller

1. **Hedef Kapsam ve Diff**:
   - İstenen platformun (iOS / Android / Her ikisi) HEAD ve çalışma ağacı diff'ini belirle.
   - Son yayın referansı biliniyorsa commit aralığını netleştir.

2. **Kritik Mağaza Ret Noktaları (REQ Kontrol Listesi)**:
   - **Çökme ve Null Güvenliği (REQ-05)**: KMP DTO mapping'lerinde null-safety (`?:` fallback), `kotlinx.serialization` model uyumsuzlukları, takılan sonsuz loading/spinner durumları, offline/hata anında kullanıcıya gösterilmeyen retry UI'ı.
   - **Giriş ve Reviewer Erişimi (REQ-06, REQ-09)**: Reviewer'ın ilgili ekranlara (Kampüs Dolabı, Sosyal, vb.) ulaşmasını engelleyen login/doğrulama duvarları. Reviewer demo hesabının gereksinimlerini tespit et.
   - **Hesap Silme (REQ-07)**: Yeni profil/hesap akışlarında self-serve "Hesabımı Sil" butonunun (`ProfileDeleteAccountButton`, `DeleteAccountConfirmDialog`) erişilebilirliği ve iOS'ta Sign in with Apple token iptali (`revokeAppleTokenIfNeeded()`). Google Play için web silme linki varlığı.
   - **Ödeme ve İkinci El Modeli (REQ-08)**: Kampüs Dolabı veya etkinliklerde harici ödeme/IAP atlatması izlenimi veren buton veya metin olmaması (Good4 komisyonsuz, ödemesiz elden teslim modelidir).
   - **Kullanıcı İçeriği (UGC) ve Moderasyon (REQ-13)**: Kampüs Dolabı ilanları veya Sosyal etkinlik paylaşımlarında şikayet et (report), kullanıcı engelle (block) butonları ve EULA/Kullanım Sözleşmesi bağlantılarının varlığı.
   - **Platform Ayrımı ve İzole UI (REQ-12)**: iOS ekranlarında veya metinlerinde "Android", "Play Store" ibarelerinin veya Android sistem ikonlarının yer almaması.
   - **Sosyal Giriş Eşitliği (REQ-19)**: iOS tarafında Google ile giriş varsa, Sign in with Apple butonunun (`AppleSignInButton`) eşit görünürlükte bulunması.
   - **İzin Açıklamaları ve Bildirimler (REQ-02, REQ-03)**: `Info.plist` içinde `NSCameraUsageDescription` vb. anahtarların açık ve amaca uygun olması; Android'de `POST_NOTIFICATIONS` ve Photo Picker uyumu.

3. **İnceleme Sınırları**:
   - Yalnız inceleme istenmişse kodu değiştirme. Düzeltme talep edilmişse bulguları giderip diff'i yenile.
   - Mağaza ret riski taşımayan kozmetik refactor önerilerini ret engeli olarak sunma.

## Rapor Formatı

Raporu [good4-store-compliance](../../good4-store-compliance/SKILL.md) formatına uygun olarak sun:
- **Özet:** Genel ret riski ve değerlendirilen platformlar.
- **Bulgular (REQ-XX):** Tespit edilen somut riskler, dosya/satır numaraları, Apple Guideline / Google Play Policy atıfları ve düzeltme adımları.
- **Öncelik Sırası:** İncelemeyi doğrudan durduracak (crash, account deletion, login wall) kritik engellerden düşüğe sıralama.
- **Store Review Notları:** App Store Connect ve Google Play Console için hazırlanmış İngilizce/Türkçe inceleme notu taslağı.
