---
name: good4-firebase-release
description: Good4 Firebase backend yayınını hazırlar, istenen deploy'u uygular ve canlı hazır olma durumunu doğrular. Functions, rules veya index deploy taleplerinde kullan; mobil mağaza yayını ayrı kapsamdadır.
---

# Good4 Firebase yayını

Repo köküne göre `firebase/v2/.firebaserc`, `firebase/v2/firebase.json`, `firebase/v2/README.md` ve değişiklik diff'ini oku. [Tekrarlayan hata kalıplarının](../../../docs/REPEATED_REVIEW_PATTERNS.md) “5 Ekim: yayın ve hazır olma” bölümünü uygula. Skill'in seçilmesi deploy izni değildir; kullanıcının mevcut talebi yetki veriyorsa yeniden onay isteme. Hedef gerçekten belirsizse bağımsız hazırlığı tamamla ve hedefi netleştir.

## Hedef ve kapsam

- Project alias/ID'yi yapılandırmadan çöz; CLI aktif projesine güvenme. Mevcut repo production alias'ı `good4tr-v2`, test alias'ı `good4tr-test`; bunları her yayında dosyadan tekrar doğrula. Komutlarda açık `--project PROJECT_ID` kullan.
- Firebase CLI kimliğinin hedef projeye erişimini `projects:list` ile doğrula. Token, servis hesabı veya `.env` içeriğini çıktıya taşıma.
- Güncel diff'teki servis değişikliklerini `functions/src/index.ts` export'larına kadar izle. Ortak servisi kullanan bütün ilgili exports kapsamda olmalı; yalnız yeni function'ları deploy etmek yeterli değildir.
- Firestore kuralları için `firebase.json` hangi dosyayı gösteriyorsa onu yayınla. Alternatif bounded dosyanın düzenlenmiş olması aktif yayına otomatik dahil edildiği anlamına gelmez.
- Hosting ve mobil mağaza yayını farklı kapsamdır. İstenen backend yayınında ilgisiz hedefleri ekleme. Uzaktan function/index silme isteği çıkarsa otomatik `--force` ile aşma; değişiklik kapsamında ve yetkili olduğunu doğrula.

## Hazırlık ve yürütme

- İlgili build/mevcut test sonuçlarını kontrol et; kaynak değiştiyse ilgili doğrulamayı yenile. Emülatör sonucu production composite index hazırlığını kanıtlamaz.
- Yeni sorgunun equality, range, order ve cursor alanlarını index tanımıyla eşleştir. Yeni index'i onu kullanacak istemci yayını öncesinde hazırla; eski istemcinin gereken index'ini gerekçesiz kaldırma.
- Önce bağımsız kurallar/index yayını, sonra etkilenen functions yayını yapılabilir. Deploy'u çalışma ağacındaki kaynak üzerinden yapıyorsan bunu belirt; commit/push'un ön koşul olduğunu varsayma.
- CLI `firebase/v2/node_modules/.bin/firebase`; komutlar `firebase/v2` dizininde çalışır. Functions predeploy TypeScript build'i çalıştırır. Gerekirse örnek kapsamı güncel exports'a göre oluştur:

```sh
node_modules/.bin/firebase deploy --project PROJECT_ID --only firestore:rules,firestore:indexes,storage --non-interactive
node_modules/.bin/firebase deploy --project PROJECT_ID --only functions:EXPORT_A,functions:EXPORT_B --non-interactive
```

Bu örneklerdeki hedefleri körlemesine yayınlama; yalnız değişen ve yetkilendirilmiş kaynakları seç. Yeni IAM/secret/env ihtiyacı varsa mevcut yapılandırmayı koru; canlı erişim kontrolünü deployment'ı geçirmek için gevşetme.

## Canlı doğrulama ve kapanış

- Deploy exit code ve her hedefin sonucunu kontrol et. Kısmi başarıyı tam başarı diye raporlama; yalnız başarısız hedefleri teşhisten sonra yeniden dene. Aynı yetki/yapılandırma hatası tekrarlanırsa kör retry'ı bırakıp gereken müdahaleyi bildir.
- İlgili functions'ın varlığı/runtime/bölgesini `functions:list --project PROJECT_ID` ile kontrol et. `ACTIVE` iddiası için Cloud Functions API veya Console'dan state'i ayrıca doğrula; liste tablosu tek başına bu durumu kanıtlamaz.
- `firestore:indexes --project PROJECT_ID --pretty` canlı index durumlarını gösterir. Yeni index için `READY` görmeden “kullanıma hazır” deme; yalnız deploy success çıktısı yeterli değildir.
- `CREATING` durumunda kontrolleri yaklaşık 30–60 saniye arayla yap; kullanıcıya anlamlı ilerleme aktar. Birkaç dakika sonra hâlâ hazırlık sürüyorsa canlı operation/Console durumunu incele, tamamlanmış deploy ile bekleyen index'i ayrı bildir. `NEEDS_REPAIR` için tekrar deploy döngüsüne girme.
- Gerçek kullanıcı verisi üreten smoke test'i sırf deploy doğrulamak için çalıştırma. Uygun mevcut test hesabı ve yetkilendirilmiş senaryo varsa kullan; aksi halde doğrulamanın sınırını belirt.
- Sonuçta proje, yayınlanan kaynaklar, function/index hazır olma durumu ve varsa eksik adımı yaz. Yerel loglar sır içerebilir; log/`.env` dosyalarını commit etme. Mobil istemci metinleri için yeni uygulama build'i gerektiğini backend deploy sonucundan ayır.
