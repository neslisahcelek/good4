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
