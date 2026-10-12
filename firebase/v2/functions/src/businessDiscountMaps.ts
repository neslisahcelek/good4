/** Optional Google Maps share link. Never fetches or resolves the supplied URL. */
export function normalizeGoogleMapsUrl(value: unknown): string {
  if (typeof value !== "string" || value.length > 2048) {
    throw new Error("Google Maps bağlantısını kontrol et.");
  }
  const link = value.trim();
  if (!link) return "";
  let url: URL;
  try {
    url = new URL(link);
  } catch {
    throw new Error("Geçerli bir Google Maps bağlantısı gir.");
  }
  const mapsPath =
    url.pathname === "/maps" || url.pathname.startsWith("/maps/");
  const allowed =
    (url.hostname === "maps.app.goo.gl" && url.pathname.length > 1) ||
    (url.hostname === "goo.gl" && url.pathname.startsWith("/maps/")) ||
    ([
      "google.com",
      "www.google.com",
      "google.com.tr",
      "www.google.com.tr",
    ].includes(url.hostname) &&
      mapsPath) ||
    url.hostname === "maps.google.com";
  if (
    url.protocol !== "https:" ||
    url.username ||
    url.password ||
    url.port ||
    !allowed
  ) {
    throw new Error(
      "Google Maps’in HTTPS konum veya paylaşım bağlantısını gir.",
    );
  }
  return url.href;
}
