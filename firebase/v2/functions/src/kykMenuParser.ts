// Pure parser shared by the callable and the web panel (no firebase-admin imports).

/** One day of the monthly KYK dormitory menu (same list for every dorm in Antalya). */
export interface KykMenuDay {
  date: string;
  breakfast: string[];
  dinner: string[];
}

const MONTHS: Record<string, number> = {
  OCAK: 1, ŞUBAT: 2, MART: 3, NİSAN: 4, MAYIS: 5, HAZİRAN: 6,
  TEMMUZ: 7, AĞUSTOS: 8, EYLÜL: 9, EKİM: 10, KASIM: 11, ARALIK: 12,
};

/**
 * Parses the transcribed wall list, e.g.
 *   27 EYLÜL 2026 / KAHVALTI / - Haşlanmış yumurta / AKŞAM YEMEĞİ / - Tarhana çorbası
 * Shared with the web panel so the preview matches what the server stores.
 */
export function parseKykMenuText(text: string): { days: KykMenuDay[]; errors: string[] } {
  const days: KykMenuDay[] = [];
  const errors: string[] = [];
  let day: KykMenuDay | null = null;
  let section: "breakfast" | "dinner" | null = null;
  text.split(/\r?\n/).forEach((raw, index) => {
    const line = raw.trim();
    if (!line) return;
    const upper = line.toLocaleUpperCase("tr-TR");
    const header = upper.match(/^(\d{1,2})\s+([A-ZÇĞİÖŞÜ]+)\s+(\d{4})$/);
    if (header) {
      const month = MONTHS[header[2]!];
      if (!month) {
        errors.push(`${index + 1}. satır: ay adı anlaşılamadı (${line})`);
        day = null;
        return;
      }
      const date = `${header[3]}-${String(month).padStart(2, "0")}-${header[1]!.padStart(2, "0")}`;
      day = { date, breakfast: [], dinner: [] };
      days.push(day);
      section = null;
      return;
    }
    if (upper.startsWith("KAHVALTI")) {
      section = "breakfast";
      return;
    }
    if (upper.startsWith("AKŞAM")) {
      section = "dinner";
      return;
    }
    const item = line.replace(/^[-•*–]\s*/, "").trim();
    if (!day || !section) {
      errors.push(`${index + 1}. satır tarih veya öğün başlığından önce geliyor: ${line}`);
      return;
    }
    if (item) (day as KykMenuDay)[section].push(item);
  });
  return { days, errors };
}
