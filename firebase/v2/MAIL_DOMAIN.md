# Good4 Kampüs Dolabı e-posta gönderici alan adı

## Durum — 6 Ekim 2026

Canlı proje `good4tr-v2`. Kullanıcı `good4tr.com` için **Continue** adımını ve aşağıdaki üç yeni GoDaddy kaydının eklenmesini açıkça onayladı. DNS kayıtlarını kullanıcı kendisi girip kaydetti; üçü de sonradan doğrudan konsol ve genel DNS üzerinden doğrulandı. Kullanıcının ayrı açık onayıyla **Verify** işlemi 6 Ekim 2026 saat 14:59:52 (UTC+03:00) başlatıldı; Firebase API `pendingCustomDomain=good4tr.com`, `customDomainState=SUCCEEDED` döndürdü, konsolda **Verification complete** görüldü. Kullanıcı daha sonra "etkinleştiri" diyerek **Apply custom domain** işlemini açıkça onayladı; işlem yapıldı. Saat 15:16:20 kontrolünde etkin alan adı `good4tr.com`, `useCustomDomain=true`; konsolda `noreply@good4tr.com` görüldü. Yeni `EMAIL_SIGNIN` test mailleri bu göndericiden Gmail gelen kutusuna SPF/DKIM/DMARC **pass** ile ulaştı. Kullanıcının ayrıca açık onayıyla Email address verification şablonunun Sender name alanı `Good4` olarak kaydedildi; fakat sonraki `EMAIL_SIGNIN` mailinin From başlığında bu ad görünmedi. Kampüs Dolabı mailinin özel görünen adı/konu/gövde özelleştirmesi tamamlanmadı; okul/Microsoft 365 teslimat testi henüz yok.

## İlk teslimat ölçümü

Yalnızca bağlı kullanıcının kontrolündeki kontrollü bir Gmail test adresine bir e-posta gönderildi. Gerçek öğrenci adreslerine gönderim yapılmadı. E-postadaki doğrulama bağlantısı açılmadı; öğrenci doğrulama kaydı veya Auth hesabı oluşturulmadı. Bu test posta taşımasını ölçer; uygulamanın öğrenci doğrulama akışının uçtan uca testi değildir.

Türkiye saati (UTC+03:00):

| Olay | Saat |
| --- | --- |
| Firebase `EMAIL_SIGNIN` gönderim isteğinin başlaması | 14:47:20.046913 |
| Firebase API'nin HTTP 200 yanıtı | 14:47:20.814989 |
| Alıcı Gmail sunucusunun `X-Received` zamanı | 14:47:21.167 |

- İstek → Gmail kabulü: **1,120087 saniye**.
- API kabul yanıtı → Gmail kabulü: **0,352011 saniye**.
- Firebase API süresi: **0,768076 saniye**.
- İleti `INBOX` etiketli, Gereksiz/Spam klasöründe değil.
- `From` ve `Return-Path`: `noreply@good4tr-v2.firebaseapp.com`.
- Konu: `Good4 uygulamasında oturum açın`.
- Alıcı `Authentication-Results`: `spf=pass`, `dkim=pass`; DKIM imza alanı `firebaseapp.com`.
- Gmail mesaj kimliği: `1a1110a2838ec4e5`.

API kabul zamanı gerçek SMTP gönderim zamanı değildir. Alıcı sunucunun başlığı teslimat için kullanıldı; posta kutusu uygulamasının ekranda iletiyi gösterme anı ayrıca ölçülmedi. Bu tek Gmail testi, Microsoft 365'teki gecikmenin nedenini kanıtlamaz. Kontrolümüzdeki bir Outlook/Microsoft 365 test adresi kullanıcıdan istendi; henüz verilmedi. Akdeniz öğrenci alan adının MX kaydı `ogr-akdeniz-edu-tr.mail.protection.outlook.com` olarak doğrulandı.

## Etkinleştirme sonrası teslimat ölçümü

Aynı kontrollü Gmail test adresine yalnızca bir yeni `EMAIL_SIGNIN` testi gönderildi. Türkiye saati (UTC+03:00):

| Olay | Saat |
| --- | --- |
| Gönderim isteğinin başlaması | 15:16:37.978926 |
| Firebase API HTTP 200 yanıtı | 15:16:38.835160 |
| Alıcı Gmail sunucusunun ilk `X-Received` zamanı | 15:16:39.332 |

- İstek → Gmail kabulü: **1,353074 saniye**.
- API kabul yanıtı → Gmail kabulü: **0,496840 saniye**.
- API süresi: yaklaşık **0,856 saniye**.
- `INBOX` etiketli; Spam/Gereksiz değil.
- `From`: `noreply@good4tr.com`; `Return-Path`: `<noreply@good4tr.com>`.
- SPF **pass**, DKIM **pass**, DMARC **pass**.
- DKIM alanı `good4tr.com`, seçici `firebase2`; DMARC From alanı `good4tr.com`.
- Konu hâlâ `Good4 uygulamasında oturum açın`; gönderen görünen adı henüz yok.
- Gmail mesaj kimliği `1a11124fd37ef194`.
- Bağlantı açılmadı veya tüketilmedi; gerçek öğrenciye gönderim yapılmadı.

Başlangıç ölçümü 1,12 saniye, yeni alan adıyla ölçüm 1,35 saniye. Bunlar iki tekil Gmail ölçümüdür; okul/Microsoft 365 teslimatındaki iyileşmenin kanıtı değildir.

### Gönderen adı kaydından sonraki test

Email address verification şablonunda Sender name `Good4` olarak ayrı açık kullanıcı onayıyla kaydedildi. API `verifyEmailTemplate.senderDisplayName=Good4` ve konsol görünümü bunu doğruladı. Yalnızca bu alan değiştirildi; konu/gövde ve action URL değiştirilmedi.

Aynı kontrollü Gmail adresine üçüncü `EMAIL_SIGNIN` testi gönderildi:

- İstek başlangıcı 15:18:36.976889; API HTTP 200 yanıtı 15:18:38.099633; Gmail alıcı `X-Received` zamanı 15:18:38.436 (UTC+03:00).
- İstek → alıcı kabulü **1,459111 saniye**; API kabulü → alıcı kabulü **0,336367 saniye**.
- Gelen kutusu; SPF/DKIM/DMARC **pass**, DKIM `d=good4tr.com`, `s=firebase2`.
- Gerçek `From` başlığı yalnızca `noreply@good4tr.com`; **Good4 görünen adı EMAIL_SIGNIN mailine yansımadı**.
- Konu `Good4 uygulamasında oturum açın` olarak kaldı; Gmail mesaj kimliği `1a11126ceac7a01c`.
- Bağlantı açılmadı; gerçek öğrenciye gönderilmedi.

Bu test, Email address verification şablonunun Sender name ayarının bu projedeki EMAIL_SIGNIN mailini değiştirdiği varsayımının yanlış olduğunu gösterdi. Özel konu/metin/görünen ad için ayrı gönderim çözümü değerlendirilmeli; gerekli kod/hizmet değişikliği henüz yapılmadı.

## DNS başlangıç durumu

İlk DNS sorgularında aşağıdaki değerler okundu. Alan adının ad sunucuları `ns17.domaincontrol.com`, `ns18.domaincontrol.com`. Sonraki kontrolde bu ortamın `dig @ns17...` yanıtında `AA` bayrağı olmadığı görüldü; bu sorguları doğrudan yetkili sunucu cevabının kanıtı olarak değerlendirmemek gerekir. Güncel yayın kontrolü ayrıca Google Public DNS'in HTTPS API'siyle yapıldı.

| Tür | Ad | Mevcut değer | İşlem |
| --- | --- | --- | --- |
| A | @ | `199.36.158.100` | Korunacak |
| CNAME | www | `good4tr-v2.web.app` | Korunacak |
| CNAME | panel | `good4tr-v2.web.app` | Korunacak |
| TXT | @ | `v=spf1 include:_spf.firebasemail.com include:secureserver.net ~all` | Firebase SPF zaten var; ikinci SPF eklenmeyecek |
| TXT | @ | `firebase=good4tr` | Eski sahiplik kaydı; silinmeyecek/değiştirilmeyecek |
| TXT | @ | `hosting-site=good4tr-v2` | Korunacak |
| TXT | @ | `google-site-verification=1thrfiWcUwnxGEyqKuNcLbaRzXEeQhqt5yqFKmyhWCo` | Korunacak |
| TXT | _dmarc | `v=DMARC1; p=quarantine; adkim=r; aspf=r; rua=mailto:dmarc_rua@onsecureserver.net;` | Mevcut DMARC korunacak; `p=none` eklenmeyecek |

Standart Firebase DKIM adları `firebase1._domainkey` ve `firebase2._domainkey` için başlangıç sorgusu CNAME döndürmedi. Kullanıcı `good4tr.com` için alan adı sihirbazında ilerlemeye açık onay verdi. Konsolun **Verify domain** ekranında aşağıdaki gerçek kayıtlar görüntülendi; DNS ekleme/kaydetme onayı ayrıca istendi.

| Tür | GoDaddy adı | Firebase'in istediği değer | Plan |
| --- | --- | --- | --- |
| TXT | @ | `v=spf1 include:_spf.firebasemail.com ~all` | Mevcut birleşik SPF bunu zaten kapsıyor; değişiklik yok |
| TXT | @ | `firebase=good4tr-v2` | Yenilenmiş GoDaddy listesinde doğrudan görüldü ve Google HTTPS DNS cevabında yayımlandığı doğrulandı; eski `firebase=good4tr` korundu; TTL 1 saat |
| CNAME | firebase1._domainkey | `mail-good4tr-com.dkim1._domainkey.firebasemail.com.` | GoDaddy'de kaydedildiği görüldü; TTL 1 saat |
| CNAME | firebase2._domainkey | `mail-good4tr-com.dkim2._domainkey.firebasemail.com.` | GoDaddy'de kaydedildiği görüldü; TTL 1 saat |

**Eklenen kayıtların tam listesi:** TXT `@ = firebase=good4tr-v2`; CNAME `firebase1._domainkey = mail-good4tr-com.dkim1._domainkey.firebasemail.com.`; CNAME `firebase2._domainkey = mail-good4tr-com.dkim2._domainkey.firebasemail.com.`. Üç kaydın TTL'si 1 saat / 3600 saniye. Firebase konsolu doğrulamanın 48 saate kadar sürebileceğini bildiriyor.

Kaydetme sonrası ve saat 14:59:46'da `ns17.domaincontrol.com`, `ns18.domaincontrol.com` ve `8.8.8.8` sorgularında yeni TXT ve iki CNAME bu ortamdan henüz görünmedi. Buna rağmen Firebase kendi doğrulamasını başarılı tamamladı ve konsolda **Verification complete** gösterdi. Bu gözlemler ayrı tutuluyor; yayılım sonrası DNS yeniden okunmalı. Mevcut birleşik SPF, DMARC, kök A, `www` ve `panel` hedefleri başlangıç değerleriyle aynı. GoDaddy'de aynı kayıtlar tekrar eklenmemeli.

### Sonraki kontrol — TXT kaydı mevcut

Kullanıcının paylaştığı ikinci rapor üzerine yeniden kontrol edildi. Yerel `dig` sorguları hâlâ eski kök TXT listesini döndürürken GoDaddy sayfası yeniden yüklenip DNS listesinin ikinci sayfası okundu: **TXT / @ / firebase=good4tr-v2 / 1 Saat** satırı doğrudan görüldü. Google Public DNS'in `https://dns.google/resolve?name=good4tr.com&type=TXT` yanıtı da `firebase=good4tr-v2` dahil beş kök TXT kaydını döndürdü. İki DKIM CNAME kaydı da yayında. TXT'nin eksik olduğu veya yeniden eklenmesi gerektiği sonucu doğru değil; bu kontrolde DNS kaydı eklenmedi/değiştirilmedi.

GoDaddy ekran kanıtı `godaddy-txt-confirmed.png`, HTTPS DNS cevapları `dns-https-confirmed.json` olarak kanıt klasörüne kaydedildi. Bu kontrolde Firebase `pendingCustomDomain=good4tr.com`, `customDomainState=SUCCEEDED` idi; daha sonra ayrı kullanıcı onayıyla etkinleştirildi. `callbackUri` aynı kaldı. Microsoft Safe Links olası bir etken olarak incelenebilir, ancak bu projede gecikmeye veya tek kullanımlık bağlantının tüketilmesine neden olduğuna dair ölçüm yok. Tek Gmail testi okul adreslerine gönderimde gecikmenin hangi tarafta oluştuğunu göstermez.

## Firebase ayarları ve şablon sınırı

İlk yapılandırma: gönderim yöntemi `DEFAULT`, dil `tr`, özel alan adı durumu `NOT_STARTED`. Devam adresi `https://good4tr-v2.firebaseapp.com/campus-email-verification`; uygulamaya ait istek süresi sunucuda 15 dakika.

Hedef gönderici `Good4 <noreply@good4tr.com>`. Özel alan adı sihirbazı bunun bütün Authentication e-posta şablonlarına uygulanacağını bildiriyor. Alan adının başlatılması, gerekli DNS kayıtlarının eklenmesi, doğrulama ve alan adının etkinleştirilmesi ayrı adımlar; kaydetme/onaylama işlemlerinden önce kullanıcı onayı alınmalı.

Etkinleştirme sonrası API: `customDomain=good4tr.com`, `useCustomDomain=true`. Bekleyen doğrulama alanları temizlenerek `customDomainState=NOT_STARTED`, istek tarihi epoch değerine döndü; bu durum etkin alan adının kapalı olduğu anlamına gelmez. Etkinlik hem `useCustomDomain=true`, hem konsolun From alanı hem de test mailinin gerçek başlıklarıyla doğrulandı. `callbackUri=https://good4tr-v2.firebaseapp.com/__/auth/action` korundu. Email address verification ekranında yalnızca Sender name `Good4` olarak ayrı açık onayla kaydedildi. Sonraki testte bu ayar EMAIL_SIGNIN mailine yansımadı.

Konsolda e-posta bağlantısıyla giriş için ayrı bir konu/gövde şablonu görünmüyor. Mevcut “Email address verification” şablonu farklı bir işlem (`VERIFY_EMAIL`); bunu değiştirmenin Kampüs Dolabı'nın `EMAIL_SIGNIN` postasını değiştirdiği varsayılmayacak. İstenen `Good4 öğrenci e-postanı doğrula` konusu ve 15 dakika bilgisini içeren özel gövde henüz uygulanmadı. Tam özel ileti gerekiyorsa Firebase Admin SDK ile bağlantı üretip ayrı posta taşıması kullanmak değerlendirilmeli; kod ve gönderim hizmeti değişiklikleri kullanıcıya gösterilmeden yapılmayacak.

## Saklanan kanıtlar ve devam

Önceki e-posta ayarları, DNS başlangıç kayıtları ve bağlantı kodlarını içermeyen test başlıkları `/Users/cankilinc/.codex/backups/good4-v2-mail-domain-2026-10-06/` klasöründe saklanıyor.

1. **Verify**, **Apply custom domain**, DNS yayını ve Gmail testinin SPF/DKIM/DMARC kontrolü tamamlandı.
2. Sender name `Good4` şablon kaydı ve EMAIL_SIGNIN testi tamamlandı; ad bu akışa yansımadı.
3. Kampüs Dolabı için `Good4` görünen adı, istenen özel konu ve 15 dakika bilgisini içeren gövde için gerekli gönderim çözümünü kullanıcıya göster; gerekli kod/hizmet değişikliklerinden önce onay bekle.
4. Kullanıcı kontrollü Outlook/Microsoft 365 test adresi sağlarsa orada başlıklar, klasör ve süreyi ölç.
5. Sonuçları bu dosyaya ekle. Gmail testinden okul teslimat sorununun çözüldüğü sonucunu çıkarma.

Uygulama/sunucu kodu değiştirilmedi; kod testleri bu aşamada çalıştırılmadı. Kullanıcının çalışma ağacındaki değişiklikler korunuyor. Commit veya stage yapılmadı.

Kaynaklar: [Firebase özel e-posta alan adı](https://firebase.google.com/docs/auth/email-custom-domain), [Admin SDK ile özel e-posta bağlantısı](https://firebase.google.com/docs/auth/admin/email-action-links), [EMAIL_SIGNIN gönderim API'si](https://docs.cloud.google.com/identity-platform/docs/reference/rest/v1/accounts/sendOobCode).

## Test hazırlığı

6 Ekim 2026, 15:27:54 Türkiye saati (UTC+03:00). Kullanıcının okul adresini açıkça doğrulaması ve silinecek alan/belgeler için ayrı açık onayı sonrasında yalnızca kendi hesabının Kampüs Dolabı edu doğrulaması kaldırıldı. Bu görevde mail gönderilmedi.

Hesap önce yalnızca okunarak, normalize edilmiş okul adresinin SHA-256 hex anahtarıyla `eduEmailClaims` belgesinden bulundu. Aynı adresle `users` sorgusu tek hesap döndürdü ve sahiplik UID'siyle eşleşti. Hesap bir öğrenci hesabıydı (Google ile giriş, Akdeniz okul adresi); kimlik bilgileri bu dokümana yazılmadı. Başlangıçta `eduVerified=true` idi; sahiplik ve `campusEmailVerifications` belgeleri mevcuttu.

Onaylanan işlem, belge sürümü koşullarıyla tek atomik Firestore commit'inde uygulandı:

- Kullanıcı belgesinden yalnızca `eduEmail`, `eduVerified`, `eduVerifiedAt` alanları kaldırıldı; kullanıcı belgesi silinmedi ve `eduVerified=false` atanmadı.
- Bu okul adresinin, aynı UID'ye ait olduğu yeniden kontrol edilen `eduEmailClaims` belgesi silindi.
- Aynı UID'nin `campusEmailVerifications` belgesi silindi.

Silme sonrasında üç yer yeniden okundu:

| Kontrol | Sonuç |
| --- | --- |
| Kullanıcı belgesi | Mevcut |
| `eduEmail` | Alan yok |
| `eduVerified` | Alan yok |
| `eduVerifiedAt` | Alan yok |
| İlgili `eduEmailClaims` belgesi | Yok |
| İlgili `campusEmailVerifications` belgesi | Yok |
| Kullanıcı belgesinin diğer alanları | İşlem öncesi değerlerle birebir aynı |

Yazma kapsamı yalnızca bu kullanıcı belgesinin üç alanı ve belirtilen iki belgeydi. Auth hesabına, başka kullanıcılara, Kampüs Dolabı ilanlarına, mesajlara ve `marketUserState` belgesine yazma yapılmadı. Kaynak kodu değiştirilmedi; commit veya stage yapılmadı. Kullanıcı Good4Test uygulamasından edu doğrulamasını yeniden başlatabilir; Microsoft 365 teslimat testi bu hazırlık işlemi sırasında yapılmadı.
