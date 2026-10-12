export function istanbulDateTime(value: string): string {
  const parts = new Intl.DateTimeFormat("sv-SE", {
    timeZone: "Europe/Istanbul",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
  }).formatToParts(new Date(value));
  const part = (type: string) =>
    parts.find((item) => item.type === type)?.value ?? "";
  return `${part("year")}-${part("month")}-${part("day")}T${part("hour")}:${part("minute")}`;
}
export function discountDates(startsAt: string, endsAt: string) {
  const start = new Date(`${startsAt}:00+03:00`);
  const end = new Date(`${endsAt}:00+03:00`);
  if (
    !Number.isFinite(start.getTime()) ||
    !Number.isFinite(end.getTime()) ||
    start >= end
  )
    throw new Error("Başlangıç, bitiş tarihinden önce olmalı.");
  return { startsAt: start.toISOString(), endsAt: end.toISOString() };
}
export function discountNumbers(percentage: string, quota: string) {
  const discountValue = Number(percentage);
  const totalLimit = quota.trim() ? Number(quota) : null;
  if (!/^\d+$/.test(percentage) || discountValue < 1 || discountValue > 100)
    throw new Error("Yüzde indirimi 1 ile 100 arasında gir.");
  if (
    totalLimit !== null &&
    (!/^\d+$/.test(quota) || totalLimit < 1 || totalLimit > 1000000)
  )
    throw new Error(
      "Toplam limiti pozitif bir tam sayı olarak gir veya sınırsız için boş bırak.",
    );
  return { discountValue, totalLimit };
}

export function discountSaveError(failure: unknown): string {
  if (!(failure instanceof Error)) return "İndirim kaydedilemedi. Tekrar dene.";
  // Local form validators already return readable messages; callable errors carry a code.
  if (!("code" in failure)) return failure.message;
  const messages: Record<string, string> = {
    LIMIT_BELOW_USAGE: "Toplam limit, gerçekleşen kullanım sayısından az olamaz.",
    BUSINESS_NOT_ACTIVE: "Seçilen işletme artık aktif değil. İşletmeleri yenileyip tekrar dene.",
    BUSINESS_NOT_EDITABLE: "Kayıtlı indirimin işletmesi değiştirilemez.",
    DISCOUNT_NOT_FOUND: "İndirim artık bulunamıyor. Listeyi yenileyip tekrar dene.",
    MAPS_URL_INVALID: "Geçerli bir Google Maps HTTPS konum veya paylaşım bağlantısı gir.",
    DISCOUNT_INVALID: "Yüzde indirimi 1 ile 100 arasında gir.",
    LIMIT_INVALID: "Toplam limiti 1 ile 1.000.000 arasında gir veya sınırsız için boş bırak.",
    DATE_INVALID: "Geçerli bir tarih ve saat seç.",
    DATE_ORDER_INVALID: "Başlangıç, bitiş tarihinden önce olmalı.",
    TITLE_REQUIRED: "İndirim başlığını gir.",
    TITLE_INVALID: "İndirim başlığı en fazla 100 karakter olmalı.",
    CONDITIONS_REQUIRED: "İndirimin kapsam ve koşullarını gir.",
    CONDITIONS_INVALID: "Kapsam ve koşullar en fazla 2.000 karakter olmalı.",
  };
  if (messages[failure.message]) return messages[failure.message];
  if (failure.code === "functions/permission-denied" || failure.code === "functions/unauthenticated") {
    return "Bu işlem için aktif yönetici oturumu gerekiyor. Yeniden giriş yap.";
  }
  if (failure.code === "functions/resource-exhausted") return "Çok sık işlem yaptın. Biraz bekleyip tekrar dene.";
  return "İndirim kaydedilemedi. Tekrar dene.";
}
