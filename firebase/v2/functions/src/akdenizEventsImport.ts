import { FieldValue, Timestamp, type Firestore } from "firebase-admin/firestore";
import { HttpsError } from "firebase-functions/v2/https";
import { requireActiveActor } from "./shared.js";

/**
 * Campus events box: public student-community events imported from etkinlik.akdeniz.edu.tr (used with the
 * university's permission) into `campusEvents`, kept apart from Good4 community events. Good4 admins can
 * replace any event's picture with their own design from the panel; imports never touch that design.
 */
export const AKDENIZ_EVENTS_SOURCE = "akdeniz-etkinlik";
export const AKDENIZ_EVENTS_BASE_URL = "https://etkinlik.akdeniz.edu.tr";

const MAX_EVENTS = 200;
const DEFAULT_DURATION_MS = 2 * 60 * 60 * 1000;
const TURKEY_OFFSET = "+03:00";

export interface AkdenizEventListing {
  id: string;
  community: string;
  category: string;
  title: string;
  location: string;
  /** yyyy-MM-dd */
  date: string;
  /** HH:mm */
  time: string;
  imagePath: string;
}

export interface AkdenizEventDetail {
  community: string;
  category: string;
  title: string;
  startDate: string;
  endDate: string;
  startTime: string;
  endTime: string;
  location: string;
  description: string;
  imagePath: string;
}

export interface AkdenizImportDeps {
  fetchText: (url: string) => Promise<string>;
  /** Downloads the site's poster, shrinks it and stores it; returns the public URL. Missing in tests. */
  copySourceImage?: (sourceUrl: string, eventId: string) => Promise<string>;
}

export interface AkdenizImportResult {
  listed: number;
  imported: number;
  removed: number;
  imagesCopied: number;
}

const ENTITIES: Record<string, string> = { amp: "&", lt: "<", gt: ">", quot: "\"", apos: "'", nbsp: " " };

export function decodeHtml(value: string): string {
  return value
    .replace(/<br\s*\/?>/gi, "\n")
    .replace(/<[^>]+>/g, "")
    .replace(/&#(\d+);/g, (_, code) => String.fromCodePoint(Number(code)))
    .replace(/&#x([0-9a-f]+);/gi, (_, code) => String.fromCodePoint(parseInt(code, 16)))
    .replace(/&([a-z]+);/gi, (match, name) => ENTITIES[name.toLowerCase()] ?? match);
}

function clean(value: string): string {
  return decodeHtml(value).replace(/[ \t\r\f\v]+/g, " ").replace(/\s*\n\s*/g, "\n").trim();
}

function oneLine(value: string): string {
  return clean(value).replace(/\s+/g, " ").trim();
}

const BACK_VOWELS = new Set(["A", "I", "O", "U", "a", "ı", "o", "u"]);
const FRONT_VOWELS = new Set(["E", "İ", "Ö", "Ü", "e", "i", "ö", "ü"]);

/**
 * The site upper-cases titles without Turkish rules, so every "i" became "I" ("GECESI").
 * Keep the upper case but restore "İ" where vowel harmony clearly asks for it; ambiguous letters stay "I".
 */
/** Loanwords and place names that break vowel harmony, fixed by hand. */
const UPPERCASE_EXCEPTIONS: Record<string, string> = {
  ANTIK: "ANTİK", SILLYON: "SİLLYON", ANATOMISI: "ANATOMİSİ", ANATOMI: "ANATOMİ", KOCAIN: "KOCAİN", MÜZIK: "MÜZİK", TEKNIK: "TEKNİK",
};

export function fixTurkishUppercase(title: string): string {
  return title.split(/([^A-Za-zÇĞİÖŞÜçğıöşü]+)/).map((word) => {
    const bare = word.replace(/[^A-ZÇĞİÖŞÜ]/g, "");
    const exception = UPPERCASE_EXCEPTIONS[bare];
    if (exception) return word.replace(bare, exception);
    const letters = [...word];
    return letters.map((char, index) => {
      if (char !== "I") return char;
      for (let i = index - 1; i >= 0; i--) {
        if (FRONT_VOWELS.has(letters[i]!)) return "İ";
        if (BACK_VOWELS.has(letters[i]!) && letters[i] !== "I") return "I";
        if (letters[i] === "I") continue;
      }
      for (let i = index + 1; i < letters.length; i++) {
        if (FRONT_VOWELS.has(letters[i]!)) return "İ";
        if (BACK_VOWELS.has(letters[i]!) && letters[i] !== "I") return "I";
      }
      return "İ";
    }).join("");
  }).join("");
}

/** Words the site writes in capitals on purpose (acronyms, brands); title casing leaves them alone. */
const KEEP_UPPER = new Set(["SUP", "LÖSEV", "AKÜBİT", "KADEM", "SKS", "ODTÜ", "TÜBİTAK", "DJ", "AI"]);
const LOWER_WORDS = new Set(["ve", "ile", "de", "da", "ya", "veya", "için"]);

/** "KAREOKE GECESİ" -> "Kareoke Gecesi"; titles are read in a list, so capitals everywhere shout. */
export function eventTitleCase(title: string): string {
  let first = true;
  // Letter runs are cased one by one so "KAMP&SUP" and "SILLYON-PERGE" keep each part right.
  return title.split(/([^A-Za-zÇĞİÖŞÜçğıöşü]+)/).map((run) => {
    if (!/[A-Za-zÇĞİÖŞÜçğıöşü]/.test(run)) return run;
    let out: string;
    if (KEEP_UPPER.has(run) || run.length <= 1) out = run;
    else {
      const lower = run.toLocaleLowerCase("tr-TR");
      out = !first && LOWER_WORDS.has(lower) ? lower : lower.charAt(0).toLocaleUpperCase("tr-TR") + lower.slice(1);
    }
    first = false;
    return out;
  }).join("");
}

const MONTHS: Record<string, string> = {
  january: "01", february: "02", march: "03", april: "04", may: "05", june: "06",
  july: "07", august: "08", september: "09", october: "10", november: "11", december: "12",
  ocak: "01", şubat: "02", mart: "03", nisan: "04", mayıs: "05", haziran: "06",
  temmuz: "07", ağustos: "08", eylül: "09", ekim: "10", kasım: "11", aralık: "12",
};

/** "13 November 2026" or "13.11.2026" -> "2026-11-13". */
export function parseSiteDate(value: string): string | null {
  const text = value.trim();
  const dotted = /^(\d{1,2})\.(\d{1,2})\.(\d{4})$/.exec(text);
  if (dotted) return `${dotted[3]}-${dotted[2]!.padStart(2, "0")}-${dotted[1]!.padStart(2, "0")}`;
  const named = /^(\d{1,2})\s+([A-Za-zÇĞİÖŞÜçğıöşü]+)\s+(\d{4})$/.exec(text);
  if (!named) return null;
  const month = MONTHS[named[2]!.toLocaleLowerCase("tr-TR")] ?? MONTHS[named[2]!.toLowerCase()];
  return month ? `${named[3]}-${month}-${named[1]!.padStart(2, "0")}` : null;
}

function parseTime(value: string): string | null {
  const match = /^(\d{1,2}):(\d{2})$/.exec(value.trim());
  if (!match) return null;
  const hour = Number(match[1]);
  const minute = Number(match[2]);
  return hour < 24 && minute < 60 ? `${String(hour).padStart(2, "0")}:${match[2]}` : null;
}

export function parseEventList(html: string): AkdenizEventListing[] {
  const cards = html.split(/<div class="event-card"/).slice(1);
  const items: AkdenizEventListing[] = [];
  for (const card of cards) {
    const id = /href="\/detay\/(\d+)"/.exec(card)?.[1];
    const community = /<div class="community-info">([\s\S]*?)<\/div>/.exec(card)?.[1];
    const title = /<h3>([\s\S]*?)<\/h3>/.exec(card)?.[1];
    const details = /<div class="card-details">([\s\S]*?)<\/div>/.exec(card)?.[1] ?? "";
    const when = /(\d{1,2}\.\d{1,2}\.\d{4})\s*\|\s*(\d{1,2}:\d{2})/.exec(details);
    if (!id || !community || !title || !when) continue;
    const location = oneLine(details.split(/<br\s*\/?>/i)[0] ?? "");
    items.push({
      id,
      community: oneLine(community),
      category: oneLine(/data-category="([^"]*)"/.exec(card)?.[1] ?? ""),
      title: oneLine(title),
      location,
      date: parseSiteDate(when[1]!) ?? "",
      time: parseTime(when[2]!) ?? "",
      imagePath: /<img src="([^"]+)"/.exec(card)?.[1] ?? "",
    });
  }
  return items;
}

function labelledValue(html: string, label: string): string {
  const pattern = new RegExp(`${label}<\\/span>\\s*<span[^>]*>([\\s\\S]*?)<\\/span>`, "i");
  return pattern.exec(html)?.[1] ?? "";
}

export function parseEventDetail(html: string): AkdenizEventDetail | null {
  const main = /<main[\s\S]*?<\/main>/.exec(html)?.[0];
  if (!main) return null;
  const title = /<h2[^>]*>([\s\S]*?)<\/h2>/.exec(main)?.[1];
  if (!title) return null;
  const range = oneLine(labelledValue(main, "Tarih Aralığı")).split(/\s+-\s+/);
  const hours = oneLine(labelledValue(main, "Etkinlik Saatleri")).split(/\s*[—–-]\s*/);
  const badges = [...main.matchAll(/<span style="background: var\(--accent\)[^>]*>([\s\S]*?)<\/span>/g)].map((m) => oneLine(m[1]!));
  const community = /<i class="fas fa-users"><\/i>([\s\S]*?)<\/span>/.exec(main)?.[1] ?? "";
  const description = /Etkinlik Hakkında<\/h4>\s*<p[^>]*>([\s\S]*?)<\/p>/.exec(main)?.[1] ?? "";
  return {
    community: oneLine(community),
    category: badges[0] ?? "",
    title: oneLine(title),
    startDate: parseSiteDate(range[0] ?? "") ?? "",
    endDate: parseSiteDate(range[1] ?? range[0] ?? "") ?? "",
    startTime: parseTime(hours[0] ?? "") ?? "",
    endTime: parseTime(hours[1] ?? "") ?? "",
    location: oneLine(labelledValue(main, "Etkinlik Mekanı")),
    description: clean(description),
    imagePath: /<img src="([^"]+)"/.exec(main)?.[1] ?? "",
  };
}

/** Site categories folded into Good4's canonical event categories. */
export function mapCategory(siteCategory: string): string {
  const value = siteCategory.toLocaleLowerCase("tr-TR");
  if (/(bisiklet|hokey|paten|yürüyüş|kamp|paraşüt|spor|tırmanış|dalış|koşu|yoga|sup|mağara)/.test(value)) return "sports-nature";
  if (/(gezi|film|sinema|konser|tiyatro|sergi|müzik|kültür|sanat)/.test(value)) return "culture-arts";
  if (/(konferans|seminer|panel|söyleşi|sempozyum)/.test(value)) return "academic-science";
  if (/(kariyer|girişim)/.test(value)) return "career-entrepreneurship";
  if (/(eğlence|toplantı|tanışma|parti)/.test(value)) return "social-entertainment";
  if (/(gönüllü|yardım|bağış)/.test(value)) return "volunteering";
  return "other";
}

function absoluteImage(path: string): string {
  if (!path || /\/default\.(jpe?g|png)$/i.test(path)) return "";
  if (/^https:\/\//i.test(path)) return path.startsWith(AKDENIZ_EVENTS_BASE_URL) ? path : "";
  return path.startsWith("/") ? `${AKDENIZ_EVENTS_BASE_URL}${path}` : "";
}

function instant(date: string, time: string): number | null {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(date) || !/^\d{2}:\d{2}$/.test(time)) return null;
  const ms = Date.parse(`${date}T${time}:00${TURKEY_OFFSET}`);
  return Number.isFinite(ms) ? ms : null;
}

export interface PreparedEvent {
  externalId: string;
  community: string;
  title: string;
  description: string;
  location: string;
  sourceImageUrl: string;
  categoryId: string;
  startsAtMs: number;
  endsAtMs: number;
}

/** Combines the list card and the detail page; the detail wins when it has a value. */
export function prepareEvent(listing: AkdenizEventListing, detail: AkdenizEventDetail | null): PreparedEvent | null {
  const startDate = detail?.startDate || listing.date;
  const startTime = detail?.startTime || listing.time;
  const startsAtMs = instant(startDate, startTime);
  if (startsAtMs === null) return null;
  const endDate = detail?.endDate || startDate;
  let endsAtMs = detail?.endTime ? instant(endDate, detail.endTime) : null;
  if (endsAtMs === null || endsAtMs <= startsAtMs) endsAtMs = startsAtMs + DEFAULT_DURATION_MS;
  const title = eventTitleCase(fixTurkishUppercase(detail?.title || listing.title)).slice(0, 120);
  const community = (detail?.community || listing.community).slice(0, 120);
  if (!title || !community) return null;
  return {
    externalId: listing.id,
    community,
    title,
    description: (detail?.description ?? "").slice(0, 4000),
    location: (detail?.location || listing.location).slice(0, 200),
    sourceImageUrl: absoluteImage(detail?.imagePath || listing.imagePath),
    // The site often files sports under generic labels ("Ortak Etkinlik"), so the title is read too.
    categoryId: mapCategory(`${detail?.category || listing.category} ${title}`),
    startsAtMs,
    endsAtMs,
  };
}

async function mapLimited<T, R>(items: T[], limit: number, task: (item: T) => Promise<R>): Promise<R[]> {
  const results: R[] = new Array(items.length);
  let next = 0;
  await Promise.all(Array.from({ length: Math.min(limit, items.length) }, async () => {
    while (next < items.length) {
      const index = next++;
      results[index] = await task(items[index]!);
    }
  }));
  return results;
}

export const CAMPUS_EVENTS_COLLECTION = "campusEvents";

export function campusEventIdFor(externalId: string): string {
  return `${AKDENIZ_EVENTS_SOURCE}-${externalId}`;
}

export async function importAkdenizEventsService(
  database: Firestore, deps: AkdenizImportDeps, nowMs = Date.now(),
): Promise<AkdenizImportResult> {
  const listings = parseEventList(await deps.fetchText(`${AKDENIZ_EVENTS_BASE_URL}/`)).slice(0, MAX_EVENTS);
  // An empty or broken page must never be read as "every event was removed".
  if (listings.length === 0) throw new Error("AKDENIZ_EVENTS_LIST_EMPTY");

  const details = await mapLimited(listings, 4, async (listing) => {
    try {
      return parseEventDetail(await deps.fetchText(`${AKDENIZ_EVENTS_BASE_URL}/detay/${listing.id}`));
    } catch {
      return null;
    }
  });
  const prepared = listings
    .map((listing, index) => prepareEvent(listing, details[index] ?? null))
    .filter((event): event is PreparedEvent => event !== null && event.endsAtMs > nowMs);

  const collection = database.collection(CAMPUS_EVENTS_COLLECTION);
  const existing = new Map(
    (await collection.where("externalSource", "==", AKDENIZ_EVENTS_SOURCE).get()).docs.map((doc) => [doc.id, doc]),
  );

  // Site posters are several megabytes; copy each new one once, shrunk, and reuse it while the source is unchanged.
  let imagesCopied = 0;
  const copiedImages = await mapLimited(prepared, 2, async (event) => {
    const id = campusEventIdFor(event.externalId);
    const previous = existing.get(id);
    if (!event.sourceImageUrl) return "";
    if (previous?.get("sourceImagePath") === event.sourceImageUrl && previous.get("sourceImageUrl")) {
      return String(previous.get("sourceImageUrl"));
    }
    if (!deps.copySourceImage) return "";
    try {
      const url = await deps.copySourceImage(event.sourceImageUrl, id);
      imagesCopied++;
      return url;
    } catch {
      return "";
    }
  });

  const writer = database.bulkWriter();
  const seen = new Set<string>();
  prepared.forEach((event, index) => {
    const id = campusEventIdFor(event.externalId);
    seen.add(id);
    // posterUrl / posterObject belong to the admin's design and are never written here.
    writer.set(collection.doc(id), {
      title: event.title,
      communityName: event.community,
      location: event.location,
      description: event.description,
      startsAt: Timestamp.fromMillis(event.startsAtMs),
      endsAt: Timestamp.fromMillis(event.endsAtMs),
      categoryId: event.categoryId,
      sourceImagePath: event.sourceImageUrl,
      sourceImageUrl: copiedImages[index] ?? "",
      status: "published",
      externalSource: AKDENIZ_EVENTS_SOURCE,
      externalId: event.externalId,
      updatedAt: FieldValue.serverTimestamp(),
      ...(existing.has(id) ? {} : { createdAt: FieldValue.serverTimestamp() }),
    }, { merge: true });
  });

  // Upcoming events that vanished from the site were withdrawn there; hide them here too.
  let removed = 0;
  for (const [id, doc] of existing) {
    const endsAt = doc.get("endsAt");
    if (seen.has(id) || doc.get("status") !== "published" || !(endsAt instanceof Timestamp) || endsAt.toMillis() <= nowMs) continue;
    writer.update(doc.ref, { status: "removed", updatedAt: FieldValue.serverTimestamp() });
    removed++;
  }
  await writer.close();
  await database.doc("system/campus_events_import").set({
    lastRunAt: FieldValue.serverTimestamp(), listed: listings.length, imported: seen.size, removed, imagesCopied,
  }, { merge: true });

  return { listed: listings.length, imported: seen.size, removed, imagesCopied };
}

/* ---------- Admin panel: custom poster designs ---------- */

const POSTER_TYPES = new Set(["image/jpeg", "image/png", "image/webp"]);
const POSTER_MAX_BYTES = 5 * 1024 * 1024;

/** Validates an uploaded poster (type, base64, size, file signature) and returns its bytes. */
export function decodePosterUpload(input: Record<string, unknown>): { bytes: Buffer; contentType: string } {
  const contentType = input.contentType;
  if (typeof contentType !== "string" || !POSTER_TYPES.has(contentType)) {
    throw new HttpsError("invalid-argument", "CAMPUS_EVENT_POSTER_TYPE_INVALID");
  }
  const encoded = input.base64;
  const maxEncodedLength = Math.ceil(POSTER_MAX_BYTES / 3) * 4;
  if (typeof encoded !== "string" || encoded.length === 0 || encoded.length > maxEncodedLength
      || encoded.length % 4 !== 0 || !/^[A-Za-z0-9+/]+={0,2}$/.test(encoded)) {
    throw new HttpsError("invalid-argument", "CAMPUS_EVENT_POSTER_INVALID");
  }
  const bytes = Buffer.from(encoded, "base64");
  if (bytes.length === 0 || bytes.length > POSTER_MAX_BYTES || bytes.toString("base64") !== encoded) {
    throw new HttpsError("invalid-argument", "CAMPUS_EVENT_POSTER_INVALID");
  }
  const signatureMatches = contentType === "image/jpeg"
    ? bytes[0] === 0xff && bytes[1] === 0xd8 && bytes[2] === 0xff
    : contentType === "image/png"
      ? bytes.subarray(0, 8).equals(Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]))
      : bytes.subarray(0, 4).toString("ascii") === "RIFF" && bytes.subarray(8, 12).toString("ascii") === "WEBP";
  if (!signatureMatches) throw new HttpsError("invalid-argument", "CAMPUS_EVENT_POSTER_CONTENT_INVALID");
  return { bytes, contentType };
}

function requireEventId(value: unknown): string {
  if (typeof value !== "string" || !/^[A-Za-z0-9_-]{1,128}$/.test(value)) {
    throw new HttpsError("invalid-argument", "CAMPUS_EVENT_ID_INVALID");
  }
  return value;
}

export interface PosterStorage {
  /** Stores the normalized poster and returns where it lives. */
  save: (eventId: string, bytes: Buffer, contentType: string) => Promise<{ url: string; objectName: string }>;
  remove: (objectName: string) => Promise<void>;
}

export async function setCampusEventPosterService(
  database: Firestore, actorUid: string, input: Record<string, unknown>, storage: PosterStorage,
): Promise<{ eventId: string; posterUrl: string }> {
  const eventId = requireEventId(input.eventId);
  const upload = decodePosterUpload(input);
  const ref = database.doc(`${CAMPUS_EVENTS_COLLECTION}/${eventId}`);
  await database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, actorUid, ["good4Admin"]);
    if (!(await transaction.get(ref)).exists) throw new HttpsError("not-found", "CAMPUS_EVENT_NOT_FOUND");
  });
  const saved = await storage.save(eventId, upload.bytes, upload.contentType);
  const previousObject = await database.runTransaction(async (transaction) => {
    const current = await transaction.get(ref);
    if (!current.exists) throw new HttpsError("not-found", "CAMPUS_EVENT_NOT_FOUND");
    transaction.update(ref, {
      posterUrl: saved.url, posterObject: saved.objectName,
      posterUpdatedAt: FieldValue.serverTimestamp(), posterUpdatedBy: actorUid,
    });
    transaction.create(database.collection("auditLogs").doc(), {
      action: "campusEvent.posterSet", actorUid, targetType: "campusEvent", targetId: eventId,
      metadata: { objectName: saved.objectName }, createdAt: FieldValue.serverTimestamp(),
    });
    return String(current.get("posterObject") ?? "");
  }).catch(async (error) => {
    // The event vanished between upload and write; do not leave an orphan file behind.
    await storage.remove(saved.objectName).catch(() => undefined);
    throw error;
  });
  if (previousObject && previousObject !== saved.objectName) await storage.remove(previousObject).catch(() => undefined);
  return { eventId, posterUrl: saved.url };
}

export async function clearCampusEventPosterService(
  database: Firestore, actorUid: string, input: Record<string, unknown>, storage: PosterStorage,
): Promise<{ eventId: string }> {
  const eventId = requireEventId(input.eventId);
  const ref = database.doc(`${CAMPUS_EVENTS_COLLECTION}/${eventId}`);
  const previousObject = await database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, actorUid, ["good4Admin"]);
    const current = await transaction.get(ref);
    if (!current.exists) throw new HttpsError("not-found", "CAMPUS_EVENT_NOT_FOUND");
    transaction.update(ref, {
      posterUrl: FieldValue.delete(), posterObject: FieldValue.delete(),
      posterUpdatedAt: FieldValue.serverTimestamp(), posterUpdatedBy: actorUid,
    });
    transaction.create(database.collection("auditLogs").doc(), {
      action: "campusEvent.posterCleared", actorUid, targetType: "campusEvent", targetId: eventId,
      metadata: {}, createdAt: FieldValue.serverTimestamp(),
    });
    return String(current.get("posterObject") ?? "");
  });
  if (previousObject) await storage.remove(previousObject).catch(() => undefined);
  return { eventId };
}

export interface CampusEventAdminRow {
  id: string;
  title: string;
  communityName: string;
  location: string;
  startsAtMs: number;
  categoryId: string;
  sourceImageUrl: string;
  posterUrl: string;
}

/** Upcoming published events for the panel, soonest first. */
export async function listCampusEventsForAdminService(
  database: Firestore, actorUid: string, nowMs = Date.now(),
): Promise<{ events: CampusEventAdminRow[]; lastRunAtMs: number | null }> {
  await database.runTransaction((transaction) => requireActiveActor(database, transaction, actorUid, ["good4Admin"]));
  const [snapshot, status] = await Promise.all([
    database.collection(CAMPUS_EVENTS_COLLECTION)
      .where("status", "==", "published")
      .where("endsAt", ">=", Timestamp.fromMillis(nowMs))
      .orderBy("endsAt")
      .limit(MAX_EVENTS)
      .get(),
    database.doc("system/campus_events_import").get(),
  ]);
  const events = snapshot.docs.map((doc) => ({
    id: doc.id,
    title: String(doc.get("title") ?? ""),
    communityName: String(doc.get("communityName") ?? ""),
    location: String(doc.get("location") ?? ""),
    startsAtMs: (doc.get("startsAt") as Timestamp | undefined)?.toMillis() ?? 0,
    categoryId: String(doc.get("categoryId") ?? "other"),
    sourceImageUrl: String(doc.get("sourceImageUrl") ?? ""),
    posterUrl: String(doc.get("posterUrl") ?? ""),
  })).sort((a, b) => a.startsAtMs - b.startsAtMs);
  const lastRun = status.get("lastRunAt");
  return { events, lastRunAtMs: lastRun instanceof Timestamp ? lastRun.toMillis() : null };
}
