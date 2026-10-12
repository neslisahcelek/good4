export function couponDay(nowMillis: number): string {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Europe/Istanbul",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(nowMillis);
  const partValue = (type: string) =>
    parts.find((item) => item.type === type)!.value;
  return `${partValue("year")}-${partValue("month")}-${partValue("day")}`;
}
