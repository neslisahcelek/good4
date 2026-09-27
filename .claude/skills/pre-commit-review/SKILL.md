---
name: pre-commit-review
description: Good4 V2'de commit atmadan önce yapılan bütün değişiklikleri güvenlik öncelikli olarak gözden geçirir (sızan secret, Firestore/Storage kuralları, Cloud Functions yetki kontrolleri, istemci tarafı güven varsayımları, kişisel veri). Commit, push ya da PR öncesinde, ayrıca kullanıcı "review et", "commit at", "güvenlik kontrolü yap" dediğinde kullan.
---

# Commit öncesi review (güvenlik öncelikli)

Kural: **Review bitip bulgular kullanıcıya gösterilmeden commit atılmaz.** Kritik veya yüksek bulgu varsa commit durur; önce düzeltilir ya da kullanıcı açıkça onaylar.

## 1. Kapsamı çıkar

```bash
git status --short
git diff --stat
git diff
git diff --cached
git ls-files --others --exclude-standard
```

- Bu oturumda **senin** yaptığın değişikliklerle kullanıcının önceden var olan değişikliklerini ayır. Kullanıcı değişikliklerini (ör. `tools/install-v2-iphone.sh`) geri alma, izinsiz stage etme; yalnızca bulguları raporla.
- `git add -A` / `git add .` kullanma; dosyaları tek tek stage et.
- Finder kopyası `* 2.*` dosyaları (`admin 2.ts`, `firestore 2.rules` vb.) commit'e girmemeli.

## 2. Güvenlik kontrol listesi

Her değişen dosyayı ilgili başlıklara göre incele. Şüpheli her satırı `dosya:satır` olarak not et.

### Secret ve yapılandırma sızıntısı
- API key, token, private key, servis hesabı JSON'u, parola, `Bearer`, `-----BEGIN` ya da `AIza...` gibi değerler diff'te var mı?
  ```bash
  git diff --cached -U0 | grep -nEi 'api[_-]?key|secret|token|password|passwd|private[_-]?key|BEGIN [A-Z ]*PRIVATE|AIza[0-9A-Za-z_-]{20,}|service[_-]?account|client[_-]?secret'
  ```
- Hiçbiri stage edilmemeli: `GoogleService-Info*.plist`, `google-services*.json`, `local.properties`, `*.keystore`, `*.jks`, `*.p8`, `*.p12`, `.env*`, `firebase-debug*.log`, `firestore-debug.log`. `.gitignore` kurallarının delinmediğini `git check-ignore -v <dosya>` ile doğrula.
- Log ve `println` çağrıları token, e-posta, öğrenci numarası ya da konum basmamalı.

### Firestore / Storage kuralları (`firebase/v2/firestore.rules`, `storage.rules`, `firebase/community/*.rules`)
- `allow read, write: if true;` veya `request.auth != null` tek başına geniş yetki veriyor mu?
- Yazma kuralları `request.auth.uid` ile sahipliği kontrol ediyor mu? `resource.data` ve `request.resource.data` ayrımı doğru mu?
- Kullanıcı kendi rolünü, admin veya topluluk yöneticisi alanını, doğrulama bayrağını (`eduVerified` vb.), sayaçları ya da puanları yazabiliyor mu? Bu alanlar yalnızca sunucuda yazılmalı.
- `request.resource.data.keys().hasOnly([...])` ile alan beyaz listesi, tür ve uzunluk sınırları var mı?
- Liste sorguları, başkalarının kişisel verisini (e-posta, telefon, öğrenci no) açığa çıkarıyor mu?
- Storage: boyut ve `contentType` sınırı ile yol sahipliği (`/users/{uid}/...`) kontrol ediliyor mu?
- Kural değiştiyse `firebase/v2/rules.test.mjs` güncellendi mi? Yeni yetki reddi için negatif test var mı?

### Cloud Functions (`firebase/v2/functions/src`)
- Her callable/HTTP fonksiyonu başta `context.auth` / `request.auth` kontrolü yapıyor mu? Admin, topluluk portalı ve işletme uçlarında rol, custom claim ya da Firestore'daki rol **sunucuda** doğrulanıyor mu?
- Girdi doğrulaması: tür, uzunluk, izin verilen değerler. İstemciden gelen `uid`, `role`, `communityId` gibi değerlere körü körüne güvenilmemeli; kimlik `auth.uid`'den alınmalı.
- Başka bir kullanıcının kaynağına erişirken IDOR kontrolü yapılıyor mu (ör. topluluk admini yalnızca kendi topluluğunu düzenleyebilmeli)?
- Admin SDK kuralları atlar; bu yüzden fonksiyon içindeki yetki kontrolü tek savunmadır.
- Harici HTTP çağrıları (hava durumu, scraper vb.): URL kullanıcıdan gelmemeli (SSRF), zaman aşımı olmalı, yanıt doğrulanmalı.
- Hata mesajları iç ayrıntı, stack trace ya da başka kullanıcının verisini döndürmemeli.
- Secret'lar `defineSecret` / ortam değişkeniyle gelmeli, kaynak koda gömülmemeli.
- Hesap silme ve Apple token iptali gibi akışlarda kısmi başarısızlık veri bırakıyor mu?

### Mobil istemci (`composeApp/src`, `iosApp`)
- Yetki kararları yalnızca istemcide mi veriliyor? Admin paneli ve topluluk yönetimi butonlarını gizlemek güvenlik değildir; arkasında kural veya fonksiyon kontrolü olmalı.
- WebView / deep link: gelen URL doğrulanıyor mu, JavaScript köprüsü açık mı, keyfi URL açılabiliyor mu?
- `http://` bağlantı, ATS istisnası (`NSAllowsArbitraryLoads`) ya da sertifika doğrulamasını kapatan kod eklendi mi?
- Hassas veri (token, oturum) `UserDefaults` / `SharedPreferences` içinde düz metin mi duruyor? Keychain veya şifreli depolama kullanılmalı.
- `Info.plist` izin açıklamaları ve yeni entitlement'lar gerekli mi, kapsamı dar mı?
- Build flavor karışıklığı: `v2` / `prod` build'i staging projesine (`good4tr-test`) bağlanmamalı. `EMAIL_VERIFICATION_REQUIRED` gibi güvenlik bayrakları gevşetilmemeli.

### Bağımlılıklar
- `package.json`, `package-lock.json`, `gradle/libs.versions.toml` ya da `Podfile` değiştiyse yeni paketin kaynağını ve sürümünü kontrol et. Node tarafında `npm audit --omit=dev` çalıştır (`firebase/v2/functions`).

### Kişisel veri ve moderasyon
- Yeni toplanan kişisel veri gizlilik metniyle (`docs/legal`) uyumlu mu?
- Kullanıcı içeriği (etkinlik, yorum, rapor) sınırlanıyor ve moderasyona uygun mu?

## 3. Doğruluk ve kalite (kısa)
- Mantık hataları, null/boş durumlar, crash riski (`!!`, zorla cast, index taşması).
- Yarım kalan kod, debug çıktısı, yorum satırına alınmış blok, TODO.
- Metinler Türkçe ve tutarlı mı?

## 4. Doğrulama komutları
Yalnızca değişen alanlarla ilgili olanları çalıştır:

```bash
# Firestore kuralları ve functions (emülatör gerekir)
npm --prefix firebase/v2 run test:rules
npm --prefix firebase/v2 run test:functions
```

```bash
# Kotlin ortak kod derlemesi
./gradlew :composeApp:compileKotlinIosSimulatorArm64
```

Çalıştıramadığın komutu atladığını açıkça söyle; geçmiş gibi raporlama.

## 5. Rapor formatı
Commit öncesinde kullanıcıya şunu göster:

1. **Kapsam:** hangi dosyalar commit'e girecek, hangileri kullanıcının ayrı değişikliği.
2. **Güvenlik bulguları:** her biri için önem (Kritik / Yüksek / Orta / Düşük), `dosya:satır`, sorun, somut istismar senaryosu ve önerilen düzeltme. Bulgu yoksa "güvenlik bulgusu yok" de ve neleri kontrol ettiğini listele.
3. **Diğer bulgular:** doğruluk ve kalite.
4. **Çalıştırılan kontroller** ve sonuçları.
5. **Karar:** "commit'e hazır" ya da "önce şunlar düzeltilmeli".

Kritik veya yüksek bulgu varsa commit atma; kullanıcıya sor.
