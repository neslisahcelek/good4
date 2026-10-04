# Güncelleme kartının metinleri

Firebase Console → Firestore Database → `app_config` koleksiyonu → `update_notice` belgesi.
Üretimde `good4tr-v2`, staging ortamında `good4tr-test` projesini kullan.

Belgeye iki **string** alanı ekle:

```json
{
  "title": "Good4’a yenilikler geldi",
  "message": "Yeni özellikleri keşfetmek için uygulamanı güncelle."
}
```

- `title`: En fazla 120 karakter. Kartta en fazla iki satır gösterilir.
- `message`: En fazla 400 karakter. Kartta en fazla üç satır gösterilir.
- Eksik, boş, fazla uzun veya okunamayan alanlarda uygulamanın yerelleştirilmiş kaynak metni kullanılır.
- Metinler Android ve iOS için ortaktır. İndirme ve yeniden başlatma durumlarının metinleri uygulama kaynaklarından gelir.
- Bu belge kartı zorla göstermez. Gerçek mağaza güncellemesi koşulları ve 7 günlük erteleme geçerlidir; Android debug simülasyonunda da aynı metinler okunur.
- Metinler ana ekran yeniden açıldığında veya uygulama ön plana geldiğinde, en fazla dakikada bir okunur. Ekran açıkken sürekli dinleme yapılmaz.
- Kullanıcıların belgeyi okuyabilmesi için `firebase/v2/firestore.rules` içindeki `update_notice` okuma izni ilgili Firebase projesine yayımlanmalıdır. İstemciden yazma ve koleksiyonu listeleme kapalıdır; Console veya yetkili Admin SDK kullanılmalıdır.

Kod değişikliği belgeyi oluşturmaz ve Firebase kurallarını otomatik yayımlamaz. Bu kodu içermeyen eski uygulamalar uzaktaki metni kullanamaz.
