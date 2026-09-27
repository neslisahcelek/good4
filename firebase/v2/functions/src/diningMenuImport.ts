import { FieldValue, type Firestore } from "firebase-admin/firestore";
import { writeDiningMenu, type DiningMenuDay } from "./adminPortal.js";

// SKS publishes next week's lunch menu as a single designed image on this page, usually on Friday.
export const SKS_MENU_PAGE_URL = "https://sks.akdeniz.edu.tr/tr/haftalik_yemek_listesi-6391";
export const DINING_MENU_IMPORT_DOC = "system/dining_menu_import";
const IMPORT_ACTOR = "system:sks-menu-import";
const MENU_IMAGE_PATTERN = /<img[^>]+src="(https:\/\/webis\.akdeniz\.edu\.tr\/uploads\/1019\/yemekhane\/[^"]+\.(?:jpe?g|png))"/i;

export interface OcrLine {
  text: string;
  x0: number;
  y0: number;
  x1: number;
  y1: number;
}

export interface OcrResult {
  width: number;
  lines: OcrLine[];
}

export interface ParsedMenu {
  weekLabel: string;
  weekStart: string;
  weekEnd: string;
  days: DiningMenuDay[];
}

export interface DiningMenuImportDeps {
  fetchText(url: string): Promise<string>;
  fetchImage(url: string): Promise<Buffer>;
  recognize(image: Buffer): Promise<OcrResult>;
}

const DAY_NAMES: Record<string, string> = {
  PAZARTESI: "Pazartesi",
  SALI: "Salı",
  CARSAMBA: "Çarşamba",
  PERSEMBE: "Perşembe",
  CUMA: "Cuma",
};

const HEADER_PATTERN = /(\d{2})[.,](\d{2})[.,](\d{4})\s*([A-ZÇĞİÖŞÜ]{3,})/;
const TOTAL_PATTERN = /KAL\s*[:;.]?\s*(\d{2,4})/;
// "DOMATES ÇORBA(161)"; OCR sometimes reads "(" as "İ"/"I", e.g. "AYRANİ76)".
const ITEM_PATTERN = /^(.*?\S)\s*\(\s*(\d{1,4})\s*\)?$/;
const MISREAD_ITEM_PATTERN = /^(.*?\S)[İI]\s*(\d{1,4})\)$/;

function asciiFold(value: string): string {
  return value
    .replace(/[İIı]/g, "I")
    .replace(/Ç/g, "C")
    .replace(/Ğ/g, "G")
    .replace(/Ö/g, "O")
    .replace(/Ş/g, "S")
    .replace(/Ü/g, "U");
}

function cleanText(value: string): string {
  return value
    .replace(/[“”"'`|©®«»{}]/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

/** "ŞEHRİYELİ BULGUR PİLAVI" -> "Şehriyeli Bulgur Pilavı" */
export function turkishTitleCase(value: string): string {
  return value
    .toLocaleLowerCase("tr-TR")
    .split(" ")
    .map((word) => (word ? word.charAt(0).toLocaleUpperCase("tr-TR") + word.slice(1) : word))
    .join(" ");
}

function addDays(isoDate: string, days: number): string {
  const date = new Date(`${isoDate}T12:00:00Z`);
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().slice(0, 10);
}

function mondayOf(isoDate: string): string {
  const weekday = new Date(`${isoDate}T12:00:00Z`).getUTCDay();
  return addDays(isoDate, weekday === 0 ? -6 : 1 - weekday);
}

export function diningWeekLabel(weekStart: string, weekEnd: string): string {
  const formatter = new Intl.DateTimeFormat("tr-TR", { day: "numeric", month: "short", year: "numeric", timeZone: "UTC" });
  return `${formatter.format(new Date(`${weekStart}T12:00:00Z`))} – ${formatter.format(new Date(`${weekEnd}T12:00:00Z`))}`;
}

const center = (line: OcrLine) => (line.x0 + line.x1) / 2;

/**
 * Turns the OCR lines of the SKS menu image into a weekly menu.
 * Each day is a card: a "28.09.2026 PAZARTESİ" header, dishes as "NAME(kcal)" (names may wrap)
 * and a "KAL:1085" total. A day is accepted only when its dish calories add up to that total,
 * which catches dishes the OCR skipped or misread.
 */
export function parseSksMenu(ocr: OcrResult): ParsedMenu {
  // Photo decorations around the cards come back as one- or two-letter fragments.
  const lines = ocr.lines
    .map((line) => ({ ...line, text: cleanText(line.text) }))
    .filter((line) => line.text.replace(/\s/g, "").length > 2 || /\d/.test(line.text));

  const headers = lines.flatMap((line) => {
    const match = HEADER_PATTERN.exec(line.text);
    if (!match) return [];
    const [, day, month, year, rawName] = match;
    const dayName = DAY_NAMES[asciiFold(rawName ?? "")];
    if (!dayName) return [];
    return [{ line, date: `${year}-${month}-${day}`, dayName }];
  });
  if (headers.length === 0) throw new Error("MENU_NO_DAYS");

  const days: DiningMenuDay[] = headers.map(({ line: header, date, dayName }) => {
    const columnTolerance = Math.min(ocr.width * 0.09, (header.x1 - header.x0) * 0.6);
    const column = lines
      .filter((line) => line !== header && Math.abs(center(line) - center(header)) <= columnTolerance && line.y0 > header.y0)
      .sort((left, right) => left.y0 - right.y0);
    const totalIndex = column.findIndex((line) => TOTAL_PATTERN.test(line.text));
    if (totalIndex < 0) throw new Error(`MENU_TOTAL_MISSING ${date}`);
    const total = Number(TOTAL_PATTERN.exec(column[totalIndex]!.text)![1]);

    const meals: string[] = [];
    let calories = 0;
    let pending = "";
    for (const line of column.slice(0, totalIndex)) {
      if (HEADER_PATTERN.test(line.text)) break;
      const text = pending ? `${pending} ${line.text}` : line.text;
      const match = ITEM_PATTERN.exec(text) ?? MISREAD_ITEM_PATTERN.exec(text);
      if (match) {
        meals.push(turkishTitleCase(match[1]!.trim()));
        calories += Number(match[2]);
        pending = "";
      } else {
        pending = text;
      }
    }
    if (meals.length === 0) throw new Error(`MENU_DAY_EMPTY ${date}`);
    // Same limits the admin panel enforces for hand-entered menus.
    if (meals.length > 10 || meals.some((meal) => meal.length > 120) || total > 5000) {
      throw new Error(`MENU_DAY_OUT_OF_BOUNDS ${date}`);
    }
    if (calories !== total) throw new Error(`MENU_CALORIE_MISMATCH ${date} ${calories}/${total}`);
    return { date, dayName, meals, calories: total };
  });

  days.sort((left, right) => left.date.localeCompare(right.date));
  if (new Set(days.map((day) => day.date)).size !== days.length) throw new Error("MENU_DAY_DUPLICATE");
  const weekStart = mondayOf(days[0]!.date);
  const weekEnd = addDays(weekStart, 4);
  if (days.some((day) => day.date < weekStart || day.date > weekEnd)) throw new Error("MENU_DAYS_NOT_ONE_WEEK");

  return { weekLabel: diningWeekLabel(weekStart, weekEnd), weekStart, weekEnd, days };
}

/**
 * Checks the SKS page for a new menu image, reads it and publishes it when every day validates.
 * A menu that fails validation never replaces the current one; the reason is kept in the status doc.
 */
export async function importSksDiningMenuService(
  database: Firestore,
  legacyTestDatabase: Firestore,
  deps: DiningMenuImportDeps,
  now = new Date(),
): Promise<"imported" | "unchanged" | "stale"> {
  const statusRef = database.doc(DINING_MENU_IMPORT_DOC);
  const status = await statusRef.get();
  try {
    const page = await deps.fetchText(SKS_MENU_PAGE_URL);
    const imageUrl = MENU_IMAGE_PATTERN.exec(page)?.[1];
    if (!imageUrl) throw new Error("MENU_IMAGE_NOT_FOUND");
    if (status.get("lastImportedImageUrl") === imageUrl) {
      await statusRef.set({ lastCheckedAt: FieldValue.serverTimestamp(), lastResult: "unchanged" }, { merge: true });
      return "unchanged";
    }

    const menu = parseSksMenu(await deps.recognize(await deps.fetchImage(imageUrl)));
    const current = await database.doc("app_config/akdeniz_dining_menu").get();
    const currentWeekStart = String(current.get("weekStart") ?? "");
    const today = now.toISOString().slice(0, 10);
    if (menu.weekEnd < today || (currentWeekStart && menu.weekStart < currentWeekStart)) {
      await statusRef.set({
        lastCheckedAt: FieldValue.serverTimestamp(),
        lastResult: "stale",
        lastImportedImageUrl: imageUrl,
        lastError: null,
      }, { merge: true });
      return "stale";
    }

    await writeDiningMenu(database, legacyTestDatabase, menu, IMPORT_ACTOR, { sourceImageUrl: imageUrl });
    await statusRef.set({
      lastCheckedAt: FieldValue.serverTimestamp(),
      lastImportedAt: FieldValue.serverTimestamp(),
      lastResult: "imported",
      lastImportedImageUrl: imageUrl,
      lastImportedWeekStart: menu.weekStart,
      lastError: null,
    }, { merge: true });
    return "imported";
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    await statusRef.set({ lastCheckedAt: FieldValue.serverTimestamp(), lastResult: "failed", lastError: message }, { merge: true });
    throw error;
  }
}
