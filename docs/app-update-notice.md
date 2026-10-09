# Güncelleme kartının görünürlüğü ve metinleri

Firebase Console → Firestore Database → `app_config` koleksiyonu → `update_notice` belgesi.
Üretimde `good4tr-v2`, staging ortamında `good4tr-test` projesini kullan.

Belgeye `enabled` **boolean** alanını ve isteğe bağlı iki **string** alanını ekle:

```json
{
  "enabled": true,
  "title": "Good4’a yenilikler geldi",
  "message": "Yeni özellikleri keşfetmek için uygulamanı güncelle."
}
```

- `enabled`: Yalnızca boolean `true` kartı etkinleştirir. `false`, eksik belge/alan veya okuma hatası kartı kapatır. Kapatmak için bu alanı `false` yap.
- `title`: En fazla 120 karakter. Kartta en fazla iki satır gösterilir.
- `message`: En fazla 400 karakter. Kartta en fazla üç satır gösterilir.
- Eksik, boş, fazla uzun veya okunamayan alanlarda uygulamanın yerelleştirilmiş kaynak metni kullanılır.
- Metinler Android ve iOS için ortaktır. İndirme ve yeniden başlatma durumlarının metinleri uygulama kaynaklarından gelir.
- Kart yalnızca `enabled == true` ve mağazada yeni sürüm varsa gösterilir; indirme/hazır durumları da flag'e bağlıdır. 7 günlük erteleme geçerlidir; Android debug simülasyonu da flag'e uyar.
- Flag ve metinler ana ekran yeniden açıldığında veya uygulama ön plana geldiğinde, en fazla dakikada bir okunur. Ekran açıkken sürekli dinleme yapılmaz; Console değişiklikleri sonraki uygun kontrolde uygulanır. Flag kapalıyken mağaza sorgusu yapılmaz.
- Kullanıcıların belgeyi okuyabilmesi için `firebase/v2/firestore.rules` içindeki `update_notice` okuma izni ilgili Firebase projesine yayımlanmalıdır. İstemciden yazma ve koleksiyonu listeleme kapalıdır; Console veya yetkili Admin SDK kullanılmalıdır.

Kod değişikliği belgeyi oluşturmaz ve Firebase kurallarını otomatik yayımlamaz. Flag'i kullanabilmek için bu kodu içeren uygulama sürümü yayımlanmalıdır; eski sürümlerdeki kart bu flag ile kapatılamaz.
