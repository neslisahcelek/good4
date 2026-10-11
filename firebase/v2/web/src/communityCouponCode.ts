export function normalizeCouponCode(value: string): string {
  return value.replace(/\D/g, "").slice(0, 8);
}
export function couponCodeRoute(code: string): "communityCoupon" | "legacy" | "campaign" | null {
  if (!/^\d+$/.test(code)) return null;
  return code.length === 4
    ? "communityCoupon"
    : code.length === 6
      ? "legacy"
      : code.length === 8
        ? "campaign"
        : null;
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
