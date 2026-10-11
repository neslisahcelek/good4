export function normalizeCouponCode(value: string): string {
  return value.toUpperCase().replace(/[^A-Z0-9]/g, "").slice(0, 12);
}
export function couponCodeRoute(code: string): "communityCoupon" | "legacy" | "campaign" | null {
  if (/^\d{4}$/.test(code)) return "communityCoupon";
  if (/^\d{6}$/.test(code)) return "legacy";
  // The server still accepts older alphanumeric campaign codes during their TTL.
  if (/^\d{8}$/.test(code) || (/^[A-Z0-9]{6,12}$/.test(code) && /[A-Z]/.test(code))) return "campaign";
  return null;
}

export function formatCommunityCouponDate(value: string): string {
  return new Intl.DateTimeFormat("tr-TR", {
    timeZone: "Europe/Istanbul",
    day: "numeric",
    month: "long",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}
