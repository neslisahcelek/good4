# Kampüs erişimi ve admin edu doğrulama testi

Kampüs Dolabı, Sosyal ve Askıda Yemek için erişim kararı **yalnız**
`firebase/v2/functions/src/campusAccess.ts` üzerinden üretilir. Yeni özellikler
`requireCampusAccess(user, errorCode)` veya `resolveCampusAccess(user)` kullanmalı;
özelliklere `role == good4Admin`, UID/e-posta allowlist'i veya test bypass'ı eklememeli.

## Kanıt ve erişim ayrı kavramlardır

- `eduVerified`, `eduEmail`, `eduVerifiedAt` ve `eduEmailClaims` yalnız gerçek
  `@ogr.akdeniz.edu.tr` doğrulamasını temsil eder. Test için bu alanları değiştirmeyin.
- `campusAccessAllowed` erişim kararıdır. `accessSource`: `edu_email`, `admin_test`, `none`.
- `universityDomain` erişimi olan hesaplarda `akdeniz.edu.tr` olur. Test hesabının normal
  e-posta domain'inden kampüs çıkarmayın; sahte okul adresi üretmeyin.
- Politika hesabın aktifliğini ve kampüs üyesi rolünü kontrol eder. Test yetkisi yalnız
  aktif `good4Admin` için geçerlidir. Yetki **tek başına erişim açmaz**.
- Kota, şartların kabulü, engelleme, askıya alma, sahiplik ve özellik anahtarları
  ayrıca uygulanır. Moderasyon yetkisi kampüs üyeliğinden ayrı kalır.
- Öğrenci faydaları için `requireCampusAccess(user, errorCode, "student")`, özel
  profil fotoğrafı için merkezi `studentAccessAllowed` kararı kullanılır. Test yetkisi
  olmayan adminlerin mevcut fotoğraf/Askıda Yemek sınırı genişletilmez.

## Sunucu tarafından yönetilen alanlar

`users/{uid}`:

- `adminEduTest`: `{ enabled, email, grantId, grantedAt }`; doğrulanmış Firebase Auth
  giriş e-postasına bağlı test yetkisi.
- `adminEduTestVerification`: `{ email, grantId, verifiedAt }`; gerçek kodla tamamlanan
  test doğrulaması. E-posta ve grantId mevcut yetkiyle eşleşmelidir.
- `adminEduTestGeneration`: kaldırma işleminde artar; kaldırma öncesinde başlamış bir
  isteğin sonradan doğrulama oturumu oluşturmasını engeller.

Normal ve bounded Firestore kullanıcı güncelleme allowlist'leri bu alanları içermez.
İstemciler ve mobil adminler bu alanları doğrudan yazamaz. Yetki yönetimi yalnız
operatör betiğiyle yapılır; `app_config/admin_edu_test.uid` tek yetkili hesabı izler.
Betik başka hesaba yetki verildiğinde önceki hesabın yetki ve test kanıtını atomik kaldırır. Hesap silme, bu yapılandırmadaki aynı UID kaydını da transaction içinde kaldırır.

## Doğrulama ve bağlantı kaldırma

Mevcut `requestEduVerification` / `requestCampusEmailCode` uçları ortak kod servisini
kullanır. Sunucu merkezi politikadan test modunu seçer; istemci modu veya alıcıyı seçemez.
Test alıcısı Firebase Auth'tan alınır; e-posta doğrulanmış, hesap etkin ve adres mevcut
yetkiyle aynı olmalıdır. Gerçek posta sağlayıcısı, 6 haneli rastgele kod, hash saklama,
10 dakika kod ömrü ve 5 yanlış deneme sınırı korunur.

Test kodu yanıtında `sessionId` bulunur; onay isteği aynı kimliği taşımalıdır. Normal
öğrenci istemcileri için bu alan zorunlu değildir. Eski e-posta bağlantısı uçları yalnız
gerçek öğrenci doğrulamasını destekler; test için kullanılmaz.

`disconnectCampusTest` yalnız çağıranın test bağlantısını kaldırır; test kanıtını ve
bekleyen kod/oturumu aynı transaction'da geçersizleştirir. Posta hâlâ gönderiliyorsa
gecikmiş sonuç kabul edilmez. 3 dakika yeniden gönderim beklemesi, hesap/saat,
alıcı/saat ve günlük posta kotaları korunur. Tamamlanmış işlem geçmişi, şart kabulü,
ilanlar ve konuşmalar silinmez. Gerçek edu kanıtı varsa bu işlem onu kaldırmaz.

Yetki verme, kaldırma ve kodla test doğrulama `auditLogs` içine kaydedilir. Kod,
Auth token'ı veya posta sağlayıcısının yanıtları loglanmaz.

## Mobil sözleşme

`getCampusEmailVerificationStatus` gerçek `verified`/`email` alanlarına ek olarak
`campusAccessAllowed`, `accessSource`, `testAvailable`, `testEmail` ve
`resendAfterSeconds` döndürür. Dolap/Sosyal `me` yanıtları da erişim kararını taşır.

`CampusAccessRepository` ve UID'ye bağlı `CampusAccessSession` mobil durum ve
önbellek geçersizleştirmesinin ortak kaynağıdır. UI admin rolü yorumlamaz; yalnız
sunucu kararını gösterir. `EduVerificationState.isVerified` gerçek kanıttır;
erişim kapıları `campusAccessAllowed` kullanmalıdır. Yeni veri erişimleri geç gelen
sonucu uygulamadan önce UID ve erişim sürümünü doğrulamalıdır.

Yetkili hesap giriş/oturum geri yükleme sonrasında sunucu kontrolüyle mobil ana ekrana
geçer. Test durumu hesap ayarlarında görünür; Askıda Yemek kısayolu yalnız test yetkili
hesap için açılır. Test sonucu “Admin test doğrulaması” olarak etiketlenir.
Yeni mobil sürüm gereklidir; eski sürümler admin test akışını desteklemez.

## Operatör kullanımı ve yayın

Betik varsayılan olarak salt okunur önizleme yapar; ADC kimlik doğrulaması gerekir.
UID ve proje açıkça verilmelidir. Production `good4tr-v2`, staging `good4tr-test`;
eski `good4tr` projesi kabul edilmez.

```sh
node firebase/v2/scripts/manage-campus-test.mjs --project good4tr-v2 --uid TARGET_UID --action grant
node firebase/v2/scripts/manage-campus-test.mjs --project good4tr-v2 --uid TARGET_UID --action grant --apply
node firebase/v2/scripts/manage-campus-test.mjs --project good4tr-v2 --uid TARGET_UID --action revoke --apply
```

Production deploy ve `--apply` açık kullanıcı yetkilendirmesi gerektirir. Önce güncel
proje/UID/rolü doğrulayın. Ortak modülü kullanan campus status, OTP, Dolap, Sosyal,
fotoğraf okuma ve kampanya exports'ını birlikte yayınlayın; yalnız yeni callable'ı
yayınlamak eski erişim kapılarını değiştirmez. Yetki kaldırma geri alma yoludur.

Maliyet: özellik erişim kontrolleri mevcut kullanıcı snapshot'ını kullanır, yeni
koleksiyon taraması/listener yoktur. Durum çağrısı iki belge okur. Test kodu ayrıca
tek Firebase Auth kullanıcı sorgusu ve normal posta gönderim akışını kullanır.
Üniversite posta sistemine teslimat, normal giriş e-postasıyla test edilmiş sayılmaz.

## Gerekli doğrulamalar

İlgili testler: `campusAccess.test.ts`, mevcut edu/campus verification, Dolap, Sosyal,
kampanya testleri ve Firestore korumalı alan testleri. İzin verilen/olmayan hesaplar,
yanlış/süresi dolmuş kod, eski sessionId, grant değişimi, eşzamanlı kaldırma/gönderim,
yeniden doğrulama, korunmuş kotalar ve gerçek edu kanıtının korunmasını kapsayın.
Mobilde Android/iOS derlemelerini, `CampusEmailCodeViewModelTest` ve
`EduVerificationViewModelTest` testlerini çalıştırın. Lifecycle yenilemesi devam eden
gönderim/onay sonucunu kaybetmemeli; erişim sürümü değişimi ise eski sonucu reddetmelidir.
