import { randomUUID } from "node:crypto";
import {
  type DocumentReference,
  type DocumentSnapshot,
  FieldValue,
  type Firestore,
  Timestamp,
  type Transaction,
} from "firebase-admin/firestore";
import { HttpsError } from "firebase-functions/v2/https";
import { hasVerifiedCampusEmail } from "./campusEmailVerification.js";
import { checkMarketText, containsPhoneNumber } from "./marketModeration.js";
import type { PushPayload } from "./push.js";
import { requireActiveActor } from "./shared.js";

/** All Kampüs Dolabı limits live here so they can be tuned in one place. */
export const MARKET_LIMITS = {
  maxPhotos: 3,
  maxPhotoBytes: 4 * 1024 * 1024,
  maxActiveListings: 10,
  maxListingsPerDay: 5,
  /**
   * Messages one sender may send about one listing per Istanbul day, across
   * every conversation of that listing. Each listing has its own quota, so
   * talking about five listings allows 5 × 200 = 1000 messages that day.
   */
  maxMessagesPerListingPerDay: 200,
  maxNewConversationsPerDay: 15,
  maxMessageLength: 500,
  maxPrice: 100_000,
  strikeWindowMs: 24 * 60 * 60 * 1000,
  strikesBeforeSuspension: 3,
  suspensionMs: 24 * 60 * 60 * 1000,
  maxBlockedUsers: 200,
  maxFavorites: 100,
} as const;

/** Published listings expire unless the seller renews them; renewals keep the feed position. */
export const MARKET_LISTING_LIFETIME = {
  days: 30,
  reminderDaysBefore: 3,
  maxRenewals: 3,
} as const;

/**
 * How long Kampüs Dolabı keeps data (also stated in the KVKK notice). The
 * daily cleanup job enforces these; account deletion removes everything at once.
 */
export const MARKET_RETENTION_DAYS = {
  /** Removed, rejected or expired listings (removed/rejected photos are already gone). */
  closedListing: 30,
  /** Sold listings lose their photos after this many days … */
  soldPhotos: 30,
  /** … and the listing record itself after this many days. */
  soldListing: 180,
  /** Conversations and their messages, counted from the last message. */
  conversation: 365,
  /** Blocked-content attempts. */
  violation: 365,
  /** Reports, counted from the moment they were resolved. */
  resolvedReport: 365,
  /** Finished or expired school e-mail verification requests. */
  emailVerification: 30,
} as const;

export const MARKET_TERMS_VERSION = 1;
export const MARKET_CATEGORIES = [
  "clothing", "accessories", "electronics", "sports", "books", "dorm", "hobby", "other",
] as const;
export const MARKET_CONDITIONS = ["new", "likeNew", "good", "fair"] as const;
export const MARKET_OFFER_PERCENTS = [10, 15, 20] as const;
export const MARKET_REPORT_REASONS = ["prohibited", "scam", "harassment", "inappropriate", "other"] as const;

const ACTIVE_LISTING_STATUSES = ["pending", "published", "reserved"];
// Expired listings stay readable and messageable for existing conversations; new ones need "published".
const MESSAGEABLE_LISTING_STATUSES = ["published", "reserved", "sold", "expired"];
const MARKET_ROLES = ["student", "good4Admin"] as const;

const UNIVERSITY_NAMES: Record<string, string> = {
  "akdeniz.edu.tr": "Akdeniz Üniversitesi",
};

export interface MarketPhotoStore {
  /** Stores a JPEG and returns a tokenised download URL. */
  save(objectName: string, bytes: Buffer): Promise<string>;
  deletePrefix(prefix: string): Promise<void>;
}

export interface MarketDeps {
  photos: MarketPhotoStore;
  notify(uid: string, payload: PushPayload): Promise<unknown>;
  now?: () => number;
}

function nowOf(deps: Pick<MarketDeps, "now">): number {
  return deps.now?.() ?? Date.now();
}

/** Istanbul calendar day (UTC+3, no DST) used for the daily counters. */
export function marketDayKey(now: number): string {
  return new Date(now + 3 * 60 * 60 * 1000).toISOString().slice(0, 10);
}

/** "ogr.akdeniz.edu.tr" and "akdeniz.edu.tr" are the same campus. */
export function eduDomainOf(eduEmail: string): string {
  const domain = eduEmail.split("@")[1] ?? "";
  return domain.split(".").slice(-3).join(".");
}

const ANONYMOUS_NAME = "Öğrenci";

/**
 * Students see each other only as initials ("Ayşe Yılmaz" → "A.. Y.."): first
 * and last name, never the full name. Idempotent, so it can also mask names
 * stored before this format ("Ayşe Y." → "A.. Y..").
 */
export function publicName(displayName: unknown): string {
  const parts = String(displayName ?? "").trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0 || (parts.length === 1 && parts[0] === ANONYMOUS_NAME)) return ANONYMOUS_NAME;
  const initial = (word: string) => `${Array.from(word)[0]!.toLocaleUpperCase("tr-TR")}..`;
  return parts.length > 1 ? `${initial(parts[0]!)} ${initial(parts.at(-1)!)}` : initial(parts[0]!);
}

const SEARCH_FOLD: Record<string, string> = { ç: "c", ğ: "g", ı: "i", ö: "o", ş: "s", ü: "u", â: "a", î: "i", û: "u" };
const MAX_SEARCH_PREFIX = 15;

/** Lower-case, Turkish letters folded to ASCII, split into words: "Kulaklık" and "kulaklik" match. */
export function searchWords(text: string): string[] {
  return text.toLocaleLowerCase("tr-TR")
    .replace(/[çğıöşüâîû]/g, (letter) => SEARCH_FOLD[letter] ?? letter)
    .normalize("NFKD").replace(/[\u0300-\u036f]/g, "")
    .split(/[^a-z0-9]+/).filter((word) => word.length >= 2);
}

/** Every 2–15 letter prefix of every title word, so a partial word finds the listing. */
export function searchTokensFor(title: string): string[] {
  const tokens = new Set<string>();
  for (const word of searchWords(title)) {
    for (let length = 2; length <= Math.min(word.length, MAX_SEARCH_PREFIX); length += 1) tokens.add(word.slice(0, length));
  }
  return [...tokens].slice(0, 200);
}

function iso(value: unknown): string | null {
  return value instanceof Timestamp ? value.toDate().toISOString() : null;
}

function requireId(value: unknown, field: string): string {
  if (typeof value !== "string" || !/^[A-Za-z0-9]{1,128}$/.test(value)) {
    throw new HttpsError("invalid-argument", `${field}_INVALID`);
  }
  return value;
}

function requireText(value: unknown, field: string, min: number, max: number): string {
  if (typeof value !== "string") throw new HttpsError("invalid-argument", `${field}_REQUIRED`);
  const text = value.replace(/\r\n/g, "\n").replace(/\n{3,}/g, "\n\n").trim();
  if (text.length < min || text.length > max) throw new HttpsError("invalid-argument", `${field}_INVALID`);
  return text;
}

function requireOneOf<T extends string | number>(value: unknown, allowed: readonly T[], code: string): T {
  if (!allowed.includes(value as T)) throw new HttpsError("invalid-argument", code);
  return value as T;
}

interface MarketMember {
  user: DocumentSnapshot;
  state: DocumentSnapshot;
  eduDomain: string;
}

/**
 * Every Kampüs Dolabı action needs an active account, a verified .edu.tr
 * address, the current rules accepted and no running suspension.
 */
async function requireMarketMember(
  database: Firestore,
  transaction: Transaction,
  uid: string,
  now: number,
): Promise<MarketMember> {
  await requireActiveActor(database, transaction, uid, MARKET_ROLES);
  const [user, state, config] = await Promise.all([
    transaction.get(database.doc(`users/${uid}`)),
    transaction.get(database.doc(`marketUserState/${uid}`)),
    transaction.get(database.doc("app_config/campus_closet")),
  ]);
  if (config.get("enabled") === false) throw new HttpsError("unavailable", "MARKET_DISABLED");
  if (!hasVerifiedCampusEmail(user)) throw new HttpsError("permission-denied", "MARKET_EDU_REQUIRED");
  if (Number(state.get("termsVersion") ?? 0) < MARKET_TERMS_VERSION) {
    throw new HttpsError("failed-precondition", "MARKET_TERMS_REQUIRED");
  }
  const suspendedUntil = state.get("suspendedUntil");
  if (suspendedUntil instanceof Timestamp && suspendedUntil.toMillis() > now) {
    throw new HttpsError("permission-denied", "MARKET_SUSPENDED");
  }
  return { user, state, eduDomain: eduDomainOf(String(user.get("eduEmail"))) };
}

function dailyCount(state: DocumentSnapshot, field: string, now: number): number {
  return state.get("dayKey") === marketDayKey(now) ? Number(state.get(field) ?? 0) : 0;
}

/** Counter update that starts a fresh day when the stored day has passed. */
function dailyIncrement(state: DocumentSnapshot, field: string, now: number) {
  const today = marketDayKey(now);
  if (state.get("dayKey") === today) return { [field]: FieldValue.increment(1) };
  return {
    dayKey: today, listingsToday: 0, messagesToday: 0, conversationsToday: 0, messagesByListing: {}, [field]: 1,
  };
}

/** Today's messages from this user about one listing (listing IDs are alphanumeric, safe as map keys). */
function listingMessageCount(state: DocumentSnapshot, listingId: string, now: number): number {
  if (state.get("dayKey") !== marketDayKey(now)) return 0;
  const byListing = state.get("messagesByListing");
  return byListing && typeof byListing === "object" ? Number((byListing as Record<string, unknown>)[listingId] ?? 0) : 0;
}

/**
 * Records a blocked attempt outside the rejected request so it is kept, and
 * suspends the account after repeated attempts. Always throws.
 */
async function rejectBlockedContent(
  database: Firestore,
  uid: string,
  term: string,
  context: "listing" | "message",
  excerpt: string,
  now: number,
): Promise<never> {
  const suspended = await database.runTransaction(async (transaction) => {
    const stateRef = database.doc(`marketUserState/${uid}`);
    const state = await transaction.get(stateRef);
    const windowStartedAt = state.get("strikeWindowStartedAt");
    const windowOpen = windowStartedAt instanceof Timestamp
      && now - windowStartedAt.toMillis() < MARKET_LIMITS.strikeWindowMs;
    const strikes = (windowOpen ? Number(state.get("strikeCount") ?? 0) : 0) + 1;
    const suspend = strikes >= MARKET_LIMITS.strikesBeforeSuspension;
    transaction.set(stateRef, {
      strikeCount: suspend ? 0 : strikes,
      strikeWindowStartedAt: windowOpen && !suspend ? windowStartedAt : Timestamp.fromMillis(now),
      totalStrikes: FieldValue.increment(1),
      ...(suspend ? { suspendedUntil: Timestamp.fromMillis(now + MARKET_LIMITS.suspensionMs) } : {}),
    }, { merge: true });
    transaction.create(database.collection("marketViolations").doc(), {
      uid, term, context, excerpt: excerpt.slice(0, 200), suspended: suspend,
      createdAt: Timestamp.fromMillis(now),
    });
    return suspend;
  });
  throw new HttpsError("invalid-argument", suspended ? "MARKET_CONTENT_BLOCKED_SUSPENDED" : "MARKET_CONTENT_BLOCKED", {
    term,
  });
}

// ---------------------------------------------------------------------------
// Rules
// ---------------------------------------------------------------------------

export async function acceptMarketTermsService(
  database: Firestore,
  uid: string,
  input: { version?: unknown },
  deps: Pick<MarketDeps, "now"> = {},
): Promise<{ termsVersion: number }> {
  if (input.version !== MARKET_TERMS_VERSION) {
    throw new HttpsError("failed-precondition", "MARKET_TERMS_VERSION_OUTDATED");
  }
  await database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, uid, MARKET_ROLES);
    transaction.set(database.doc(`marketUserState/${uid}`), {
      termsVersion: MARKET_TERMS_VERSION,
      termsAcceptedAt: Timestamp.fromMillis(nowOf(deps)),
    }, { merge: true });
  });
  return { termsVersion: MARKET_TERMS_VERSION };
}

// ---------------------------------------------------------------------------
// Listings
// ---------------------------------------------------------------------------

const JPEG = Buffer.from([0xff, 0xd8, 0xff]);
const PNG = Buffer.from([0x89, 0x50, 0x4e, 0x47]);

function decodePhotos(value: unknown): Buffer[] {
  if (!Array.isArray(value) || value.length < 1 || value.length > MARKET_LIMITS.maxPhotos) {
    throw new HttpsError("invalid-argument", "MARKET_PHOTOS_INVALID");
  }
  const maxEncoded = Math.ceil(MARKET_LIMITS.maxPhotoBytes / 3) * 4;
  return value.map((encoded) => {
    if (typeof encoded !== "string" || encoded.length === 0 || encoded.length > maxEncoded
      || encoded.length % 4 !== 0 || !/^[A-Za-z0-9+/]+={0,2}$/.test(encoded)) {
      throw new HttpsError("invalid-argument", "MARKET_PHOTO_INVALID");
    }
    const bytes = Buffer.from(encoded, "base64");
    const isWebp = bytes.subarray(0, 4).toString("ascii") === "RIFF"
      && bytes.subarray(8, 12).toString("ascii") === "WEBP";
    if (!bytes.subarray(0, 3).equals(JPEG) && !bytes.subarray(0, 4).equals(PNG) && !isWebp) {
      throw new HttpsError("invalid-argument", "MARKET_PHOTO_CONTENT_INVALID");
    }
    return bytes;
  });
}

/**
 * Re-encodes every photo on the server: applies the camera rotation, strips
 * EXIF (including GPS position), caps the size and makes a list thumbnail.
 */
async function processPhoto(bytes: Buffer): Promise<{ full: Buffer; thumb: Buffer }> {
  const { default: sharp } = await import("sharp");
  try {
    const base = sharp(bytes, { failOn: "error", limitInputPixels: 40_000_000 }).rotate();
    const [full, thumb] = await Promise.all([
      base.clone().resize(1280, 1280, { fit: "inside", withoutEnlargement: true })
        .jpeg({ quality: 78, mozjpeg: true }).toBuffer(),
      base.clone().resize(480, 480, { fit: "cover" }).jpeg({ quality: 72, mozjpeg: true }).toBuffer(),
    ]);
    return { full, thumb };
  } catch {
    throw new HttpsError("invalid-argument", "MARKET_PHOTO_CONTENT_INVALID");
  }
}

interface ListingInput {
  category: string;
  condition: string;
  title: string;
  description: string;
  price: number;
}

function parseListingInput(input: Record<string, unknown>): ListingInput {
  const price = input.price;
  if (typeof price !== "number" || !Number.isInteger(price) || price < 0 || price > MARKET_LIMITS.maxPrice) {
    throw new HttpsError("invalid-argument", "MARKET_PRICE_INVALID");
  }
  return {
    category: requireOneOf(input.category, MARKET_CATEGORIES, "MARKET_CATEGORY_INVALID"),
    condition: requireOneOf(input.condition, MARKET_CONDITIONS, "MARKET_CONDITION_INVALID"),
    title: requireText(input.title, "MARKET_TITLE", 3, 60),
    description: requireText(input.description, "MARKET_DESCRIPTION", 10, 600),
    price,
  };
}

async function assertCanCreateListing(
  database: Firestore,
  transaction: Transaction,
  uid: string,
  now: number,
): Promise<MarketMember> {
  const member = await requireMarketMember(database, transaction, uid, now);
  if (dailyCount(member.state, "listingsToday", now) >= MARKET_LIMITS.maxListingsPerDay) {
    throw new HttpsError("resource-exhausted", "MARKET_DAILY_LISTING_LIMIT");
  }
  const active = await transaction.get(database.collection("marketListings")
    .where("sellerUid", "==", uid)
    .where("status", "in", ACTIVE_LISTING_STATUSES)
    .limit(MARKET_LIMITS.maxActiveListings));
  if (active.size >= MARKET_LIMITS.maxActiveListings) {
    throw new HttpsError("resource-exhausted", "MARKET_ACTIVE_LISTING_LIMIT");
  }
  return member;
}

async function notifyAdminsOfListing(
  database: Firestore,
  deps: MarketDeps,
  listingId: string,
  sellerUid: string,
  title: string,
) {
  const admins = await database.collection("users")
    .where("role", "==", "good4Admin")
    .where("status", "==", "active")
    .limit(5)
    .get();
  const emails = admins.docs.map((admin) => admin.get("email")).filter((email): email is string =>
    typeof email === "string" && email.includes("@"));
  const safeTitle = title.replace(/[<>&"]/g, "");
  if (emails.length > 0) {
    await database.collection("mail").add({
      to: emails,
      message: {
        subject: `Kampüs Dolabı: onay bekleyen ilan — ${safeTitle}`,
        text: `"${safeTitle}" ilanı onay bekliyor. Admin panelindeki Kampüs Dolabı sekmesinden inceleyebilirsin.`,
        html: `<p><strong>${safeTitle}</strong> ilanı onay bekliyor.</p>`
          + "<p>Admin panelindeki <strong>Kampüs Dolabı</strong> sekmesinden inceleyebilirsin.</p>",
      },
      purpose: "marketListingPending",
      uid: sellerUid,
      createdAt: FieldValue.serverTimestamp(),
    });
  }
  await Promise.all(admins.docs.map((admin) => deps.notify(admin.id, {
    title: "Kampüs Dolabı: yeni ilan",
    body: `"${title}" onay bekliyor.`,
    data: { type: "market_admin_listing", listingId },
  })));
}

export async function createMarketListingService(
  database: Firestore,
  uid: string,
  input: Record<string, unknown>,
  deps: MarketDeps,
): Promise<{ listingId: string; status: "pending" }> {
  const now = nowOf(deps);
  const listing = parseListingInput(input);
  const rawPhotos = decodePhotos(input.photos);

  // Cheap checks first so a blocked or over-limit request never touches Storage.
  await database.runTransaction((transaction) => assertCanCreateListing(database, transaction, uid, now));
  const check = checkMarketText(listing.title, listing.description);
  if (check.blockedTerm) {
    await rejectBlockedContent(database, uid, check.blockedTerm, "listing",
      `${listing.title} — ${listing.description}`, now);
  }
  if (containsPhoneNumber(`${listing.title} ${listing.description}`)) {
    throw new HttpsError("invalid-argument", "MARKET_PHONE_IN_LISTING");
  }

  const processed = await Promise.all(rawPhotos.map(processPhoto));
  const listingRef = database.collection("marketListings").doc();
  const folder = `market-listings/${listingRef.id}/`;
  let photos: { url: string; thumbUrl: string }[];
  try {
    photos = await Promise.all(processed.map(async (photo, index) => {
      const name = `${index}-${randomUUID().slice(0, 8)}`;
      const [url, thumbUrl] = await Promise.all([
        deps.photos.save(`${folder}${name}.jpg`, photo.full),
        deps.photos.save(`${folder}${name}_thumb.jpg`, photo.thumb),
      ]);
      return { url, thumbUrl };
    }));

    await database.runTransaction(async (transaction) => {
      const member = await assertCanCreateListing(database, transaction, uid, now);
      transaction.create(listingRef, {
        sellerUid: uid,
        sellerName: publicName(member.user.get("displayName")),
        eduDomain: member.eduDomain,
        universityName: UNIVERSITY_NAMES[member.eduDomain] ?? member.eduDomain,
        ...listing,
        searchTokens: searchTokensFor(listing.title),
        photos,
        status: "pending",
        moderationFlags: check.flaggedTerms,
        rejectReason: null,
        createdAt: Timestamp.fromMillis(now),
        updatedAt: Timestamp.fromMillis(now),
        publishedAt: null,
      });
      transaction.set(database.doc(`marketUserState/${uid}`),
        dailyIncrement(member.state, "listingsToday", now), { merge: true });
    });
  } catch (error) {
    await deps.photos.deletePrefix(folder).catch(() => undefined);
    throw error;
  }

  await notifyAdminsOfListing(database, deps, listingRef.id, uid, listing.title).catch((error) => {
    console.error("market: admin notification failed", error);
  });
  return { listingId: listingRef.id, status: "pending" };
}

const SELLER_TRANSITIONS: Record<string, { from: string[]; to: string }> = {
  markReserved: { from: ["published"], to: "reserved" },
  markSold: { from: ["published", "reserved"], to: "sold" },
  markAvailable: { from: ["reserved", "sold"], to: "published" },
  remove: { from: ["pending", "published", "reserved", "sold", "rejected", "expired"], to: "removed" },
};

export async function updateMarketListingStatusService(
  database: Firestore,
  uid: string,
  input: { listingId?: unknown; action?: unknown },
  deps: MarketDeps,
): Promise<{ status: string }> {
  const now = nowOf(deps);
  const listingId = requireId(input.listingId, "LISTING_ID");
  const action = requireOneOf(input.action, Object.keys(SELLER_TRANSITIONS), "MARKET_ACTION_INVALID");
  const transition = SELLER_TRANSITIONS[action]!;
  const listingRef = database.doc(`marketListings/${listingId}`);

  await database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, uid, MARKET_ROLES);
    const listing = await transaction.get(listingRef);
    if (!listing.exists || listing.get("sellerUid") !== uid) {
      throw new HttpsError("not-found", "MARKET_LISTING_NOT_FOUND");
    }
    if (!transition.from.includes(String(listing.get("status")))) {
      throw new HttpsError("failed-precondition", "MARKET_LISTING_STATUS_INVALID");
    }
    transaction.update(listingRef, {
      status: transition.to,
      updatedAt: Timestamp.fromMillis(now),
      ...(transition.to === "sold" ? { soldAt: Timestamp.fromMillis(now) } : {}),
      ...(transition.to === "removed" ? { photos: [] } : {}),
    });
  });
  if (transition.to === "removed") {
    await deps.photos.deletePrefix(`market-listings/${listingId}/`).catch((error) => {
      console.error("market: photo cleanup failed", error);
    });
  }
  return { status: transition.to };
}

const PRICE_EDITABLE_STATUSES = ["pending", "published", "reserved"];

/**
 * Sellers may change only the price after posting. It needs no new review:
 * the text and photos an admin approved stay the same.
 */
export async function updateMarketListingPriceService(
  database: Firestore,
  uid: string,
  input: { listingId?: unknown; price?: unknown },
  deps: Pick<MarketDeps, "now"> = {},
): Promise<{ price: number }> {
  const now = nowOf(deps);
  const listingId = requireId(input.listingId, "LISTING_ID");
  const price = input.price;
  if (typeof price !== "number" || !Number.isInteger(price) || price < 0 || price > MARKET_LIMITS.maxPrice) {
    throw new HttpsError("invalid-argument", "MARKET_PRICE_INVALID");
  }
  const listingRef = database.doc(`marketListings/${listingId}`);
  await database.runTransaction(async (transaction) => {
    await requireMarketMember(database, transaction, uid, now);
    const listing = await transaction.get(listingRef);
    if (!listing.exists || listing.get("sellerUid") !== uid) {
      throw new HttpsError("not-found", "MARKET_LISTING_NOT_FOUND");
    }
    if (!PRICE_EDITABLE_STATUSES.includes(String(listing.get("status")))) {
      throw new HttpsError("failed-precondition", "MARKET_LISTING_STATUS_INVALID");
    }
    transaction.update(listingRef, {
      price, updatedAt: Timestamp.fromMillis(now), priceUpdatedAt: Timestamp.fromMillis(now),
    });
  });
  // Conversations show the listing price; refresh their copies. Pending offers keep their own price.
  const conversations = await database.collection("marketConversations").where("listingId", "==", listingId).get();
  for (let offset = 0; offset < conversations.size; offset += 400) {
    const batch = database.batch();
    conversations.docs.slice(offset, offset + 400).forEach((conversation) => batch.update(conversation.ref, { listingPrice: price }));
    await batch.commit();
  }
  return { price };
}

const RENEWABLE_STATUSES = ["published", "reserved", "expired"];

/**
 * "30 gün daha yayında tut": extends the listing for another lifetime without
 * a new review (nothing in it changed) and without moving it up the feed.
 */
export async function renewMarketListingService(
  database: Firestore,
  uid: string,
  input: { listingId?: unknown },
  deps: Pick<MarketDeps, "now"> = {},
): Promise<{ status: string; expiresAt: string; renewsLeft: number }> {
  const now = nowOf(deps);
  const listingId = requireId(input.listingId, "LISTING_ID");
  const listingRef = database.doc(`marketListings/${listingId}`);
  return database.runTransaction(async (transaction) => {
    await requireMarketMember(database, transaction, uid, now);
    const listing = await transaction.get(listingRef);
    if (!listing.exists || listing.get("sellerUid") !== uid) {
      throw new HttpsError("not-found", "MARKET_LISTING_NOT_FOUND");
    }
    const currentStatus = String(listing.get("status"));
    if (!RENEWABLE_STATUSES.includes(currentStatus)) {
      throw new HttpsError("failed-precondition", "MARKET_LISTING_STATUS_INVALID");
    }
    const renewCount = Number(listing.get("renewCount") ?? 0);
    if (renewCount >= MARKET_LISTING_LIFETIME.maxRenewals) {
      throw new HttpsError("resource-exhausted", "MARKET_RENEW_LIMIT");
    }
    const expiresAt = Timestamp.fromMillis(now + MARKET_LISTING_LIFETIME.days * 24 * 60 * 60 * 1000);
    const status = currentStatus === "expired" ? "published" : currentStatus;
    transaction.update(listingRef, {
      status, expiresAt, renewCount: renewCount + 1,
      expiryReminderSentAt: FieldValue.delete(), updatedAt: Timestamp.fromMillis(now),
    });
    return {
      status,
      expiresAt: expiresAt.toDate().toISOString(),
      renewsLeft: MARKET_LISTING_LIFETIME.maxRenewals - renewCount - 1,
    };
  });
}

// ---------------------------------------------------------------------------
// Conversations, messages and offers
// ---------------------------------------------------------------------------

function parseConversationId(value: unknown): { conversationId: string; listingId: string; buyerUid: string } {
  const match = typeof value === "string" ? /^([A-Za-z0-9]{1,64})_([A-Za-z0-9]{1,128})$/.exec(value) : null;
  if (!match) throw new HttpsError("invalid-argument", "CONVERSATION_ID_INVALID");
  return { conversationId: match[0], listingId: match[1]!, buyerUid: match[2]! };
}

function isBlockedBetween(a: DocumentSnapshot, aUid: string, b: DocumentSnapshot, bUid: string): boolean {
  const blocks = (state: DocumentSnapshot, other: string) =>
    Array.isArray(state.get("blockedUids")) && (state.get("blockedUids") as unknown[]).includes(other);
  return blocks(a, bUid) || blocks(b, aUid);
}

function formatPrice(price: number): string {
  return `${price.toLocaleString("tr-TR")} ₺`;
}

export async function sendMarketMessageService(
  database: Firestore,
  uid: string,
  input: { conversationId?: unknown; text?: unknown; offerPercent?: unknown },
  deps: MarketDeps,
): Promise<{ conversationId: string; messageId: string }> {
  const now = nowOf(deps);
  const { conversationId, listingId, buyerUid } = parseConversationId(input.conversationId);
  const isOffer = input.offerPercent !== undefined && input.offerPercent !== null;
  const text = isOffer ? "" : requireText(input.text, "MARKET_MESSAGE", 1, MARKET_LIMITS.maxMessageLength);
  const offerPercent = isOffer
    ? requireOneOf(input.offerPercent, MARKET_OFFER_PERCENTS, "MARKET_OFFER_PERCENT_INVALID")
    : null;

  // Membership is checked before the content so unverified users never collect strikes.
  await database.runTransaction((transaction) => requireMarketMember(database, transaction, uid, now));
  if (text) {
    const check = checkMarketText(text);
    if (check.blockedTerm) await rejectBlockedContent(database, uid, check.blockedTerm, "message", text, now);
  }

  const conversationRef = database.doc(`marketConversations/${conversationId}`);
  const messageRef = conversationRef.collection("messages").doc();
  const result = await database.runTransaction(async (transaction) => {
    const member = await requireMarketMember(database, transaction, uid, now);
    const [conversation, listing] = await Promise.all([
      transaction.get(conversationRef),
      transaction.get(database.doc(`marketListings/${listingId}`)),
    ]);
    if (!listing.exists) throw new HttpsError("not-found", "MARKET_LISTING_NOT_FOUND");
    const sellerUid = String(listing.get("sellerUid"));
    const isNew = !conversation.exists;
    if (isNew) {
      if (uid !== buyerUid || uid === sellerUid) throw new HttpsError("permission-denied", "MARKET_NOT_PARTICIPANT");
      if (listing.get("status") !== "published") {
        throw new HttpsError("failed-precondition", "MARKET_LISTING_UNAVAILABLE");
      }
      if (listing.get("eduDomain") !== member.eduDomain) {
        throw new HttpsError("permission-denied", "MARKET_OTHER_CAMPUS");
      }
      if (dailyCount(member.state, "conversationsToday", now) >= MARKET_LIMITS.maxNewConversationsPerDay) {
        throw new HttpsError("resource-exhausted", "MARKET_DAILY_CONVERSATION_LIMIT");
      }
    } else {
      if (uid !== conversation.get("sellerUid") && uid !== conversation.get("buyerUid")) {
        throw new HttpsError("permission-denied", "MARKET_NOT_PARTICIPANT");
      }
      if (conversation.get("status") === "blocked") throw new HttpsError("permission-denied", "MARKET_BLOCKED");
      if (!MESSAGEABLE_LISTING_STATUSES.includes(String(listing.get("status")))) {
        throw new HttpsError("failed-precondition", "MARKET_LISTING_UNAVAILABLE");
      }
    }
    // Counted inside this transaction on the sender's state document, so concurrent
    // sends retry against the committed count and can never pass the limit together.
    const sentToday = listingMessageCount(member.state, listingId, now);
    if (sentToday >= MARKET_LIMITS.maxMessagesPerListingPerDay) {
      throw new HttpsError("resource-exhausted", "MARKET_DAILY_MESSAGE_LIMIT");
    }

    const otherUid = uid === sellerUid ? buyerUid : sellerUid;
    const otherStateRef = database.doc(`marketUserState/${otherUid}`);
    const [otherState, sellerUser] = await Promise.all([
      transaction.get(otherStateRef),
      isNew ? transaction.get(database.doc(`users/${sellerUid}`)) : Promise.resolve(null),
    ]);
    if (isBlockedBetween(member.state, uid, otherState, otherUid)) {
      throw new HttpsError("permission-denied", "MARKET_BLOCKED");
    }

    const price = Number(listing.get("price") ?? 0);
    let offer: { percent: number; price: number } | null = null;
    if (offerPercent !== null) {
      if (uid !== buyerUid) throw new HttpsError("permission-denied", "MARKET_OFFER_BUYER_ONLY");
      if (listing.get("status") !== "published" || price <= 0) {
        throw new HttpsError("failed-precondition", "MARKET_OFFER_UNAVAILABLE");
      }
      if (conversation.get("offer.status") === "pending") {
        throw new HttpsError("failed-precondition", "MARKET_OFFER_PENDING");
      }
      offer = { percent: offerPercent, price: Math.round(price * (100 - offerPercent) / 100) };
    }

    const senderName = publicName(member.user.get("displayName"));
    const createdAt = Timestamp.fromMillis(now);
    const preview = offer ? `%${offer.percent} indirimli teklif: ${formatPrice(offer.price)}` : text;
    transaction.create(messageRef, {
      senderUid: uid,
      type: offer ? "offer" : "text",
      text: preview,
      ...(offer ? { offerPercent: offer.percent, offerPrice: offer.price } : {}),
      createdAt,
    });
    const summary = {
      lastMessageText: preview.slice(0, 120),
      lastMessageAt: createdAt,
      lastSenderUid: uid,
      updatedAt: createdAt,
      ...(offer ? { offer: { ...offer, status: "pending", createdAt } } : {}),
    };
    if (isNew) {
      const photos = listing.get("photos") as { thumbUrl?: string }[] | undefined;
      transaction.create(conversationRef, {
        listingId,
        sellerUid,
        buyerUid,
        participants: [sellerUid, buyerUid],
        listingTitle: String(listing.get("title") ?? ""),
        listingThumbUrl: photos?.[0]?.thumbUrl ?? "",
        listingPrice: price,
        sellerName: publicName(sellerUser?.get("displayName")),
        buyerName: senderName,
        status: "open",
        offer: null,
        ...summary,
        unread: { [sellerUid]: 1, [buyerUid]: 0 },
        messageCount: 1,
        createdAt,
      });
    } else {
      transaction.update(conversationRef, {
        ...summary,
        messageCount: FieldValue.increment(1),
        [`unread.${otherUid}`]: FieldValue.increment(1),
      });
    }
    const sameDay = member.state.get("dayKey") === marketDayKey(now);
    const byListing = sameDay ? { ...(member.state.get("messagesByListing") as Record<string, number> | undefined) } : {};
    byListing[listingId] = sentToday + 1;
    const counters: Record<string, unknown> = {
      ...dailyIncrement(member.state, "messagesToday", now),
      // Written whole so a new day drops yesterday's per-listing counts.
      messagesByListing: byListing,
      ...(isNew ? { conversationsToday: dailyCount(member.state, "conversationsToday", now) + 1 } : {}),
    };
    transaction.set(database.doc(`marketUserState/${uid}`), counters, { mergeFields: Object.keys(counters) });
    transaction.set(otherStateRef, { unreadCount: FieldValue.increment(1) }, { merge: true });
    return {
      otherUid,
      listingTitle: String(listing.get("title") ?? ""),
      senderName,
      preview,
    };
  });

  await deps.notify(result.otherUid, {
    title: `${result.senderName} · ${result.listingTitle}`.slice(0, 80),
    body: offerPercent !== null ? `Yeni teklif — ${result.preview}` : result.preview.slice(0, 140),
    data: { type: "market_message", conversationId },
  });
  return { conversationId, messageId: messageRef.id };
}

export async function respondMarketOfferService(
  database: Firestore,
  uid: string,
  input: { conversationId?: unknown; accept?: unknown },
  deps: MarketDeps,
): Promise<{ status: "accepted" | "declined" }> {
  const now = nowOf(deps);
  const { conversationId } = parseConversationId(input.conversationId);
  if (typeof input.accept !== "boolean") throw new HttpsError("invalid-argument", "MARKET_OFFER_RESPONSE_INVALID");
  const status = input.accept ? "accepted" : "declined";
  const conversationRef = database.doc(`marketConversations/${conversationId}`);

  const result = await database.runTransaction(async (transaction) => {
    await requireMarketMember(database, transaction, uid, now);
    const conversation = await transaction.get(conversationRef);
    if (!conversation.exists || conversation.get("sellerUid") !== uid) {
      throw new HttpsError("permission-denied", "MARKET_OFFER_SELLER_ONLY");
    }
    if (conversation.get("offer.status") !== "pending") {
      throw new HttpsError("failed-precondition", "MARKET_OFFER_NOT_PENDING");
    }
    const buyerUid = String(conversation.get("buyerUid"));
    const offerPrice = Number(conversation.get("offer.price") ?? 0);
    const text = input.accept
      ? `Teklif kabul edildi: ${formatPrice(offerPrice)}. Buluşma yerini ve saatini konuşabilirsiniz.`
      : "Teklif reddedildi.";
    const createdAt = Timestamp.fromMillis(now);
    transaction.create(conversationRef.collection("messages").doc(), {
      senderUid: uid, type: "offerResponse", text, offerStatus: status, createdAt,
    });
    transaction.update(conversationRef, {
      "offer.status": status,
      "offer.respondedAt": createdAt,
      lastMessageText: text.slice(0, 120),
      lastMessageAt: createdAt,
      lastSenderUid: uid,
      [`unread.${buyerUid}`]: FieldValue.increment(1),
      updatedAt: createdAt,
    });
    transaction.set(database.doc(`marketUserState/${buyerUid}`), { unreadCount: FieldValue.increment(1) },
      { merge: true });
    return { buyerUid, listingTitle: String(conversation.get("listingTitle") ?? ""), text };
  });

  await deps.notify(result.buyerUid, {
    title: `Kampüs Dolabı · ${result.listingTitle}`.slice(0, 80),
    body: result.text,
    data: { type: "market_message", conversationId },
  });
  return { status };
}

export async function markMarketConversationReadService(
  database: Firestore,
  uid: string,
  input: { conversationId?: unknown },
): Promise<{ unreadCount: number }> {
  const { conversationId } = parseConversationId(input.conversationId);
  const conversationRef = database.doc(`marketConversations/${conversationId}`);
  const stateRef = database.doc(`marketUserState/${uid}`);
  return database.runTransaction(async (transaction) => {
    const [conversation, state] = await Promise.all([transaction.get(conversationRef), transaction.get(stateRef)]);
    const participants = conversation.get("participants");
    if (!conversation.exists || !Array.isArray(participants) || !participants.includes(uid)) {
      throw new HttpsError("permission-denied", "MARKET_NOT_PARTICIPANT");
    }
    const unread = Number(conversation.get(`unread.${uid}`) ?? 0);
    const total = Math.max(0, Number(state.get("unreadCount") ?? 0) - unread);
    if (unread > 0) {
      transaction.update(conversationRef, { [`unread.${uid}`]: 0 });
      transaction.set(stateRef, { unreadCount: total }, { merge: true });
    }
    return { unreadCount: total };
  });
}

export async function blockMarketUserService(
  database: Firestore,
  uid: string,
  input: { conversationId?: unknown },
  deps: Pick<MarketDeps, "now"> = {},
): Promise<{ blocked: true }> {
  const { conversationId } = parseConversationId(input.conversationId);
  const conversationRef = database.doc(`marketConversations/${conversationId}`);
  const stateRef = database.doc(`marketUserState/${uid}`);
  await database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, uid, MARKET_ROLES);
    const [conversation, state] = await Promise.all([transaction.get(conversationRef), transaction.get(stateRef)]);
    const participants = conversation.get("participants");
    if (!conversation.exists || !Array.isArray(participants) || !participants.includes(uid)) {
      throw new HttpsError("permission-denied", "MARKET_NOT_PARTICIPANT");
    }
    const otherUid = String(participants.find((participant) => participant !== uid));
    const blocked = Array.isArray(state.get("blockedUids")) ? state.get("blockedUids") as string[] : [];
    if (!blocked.includes(otherUid) && blocked.length >= MARKET_LIMITS.maxBlockedUsers) {
      throw new HttpsError("resource-exhausted", "MARKET_BLOCK_LIMIT");
    }
    transaction.set(stateRef, { blockedUids: FieldValue.arrayUnion(otherUid) }, { merge: true });
    transaction.update(conversationRef, {
      status: "blocked",
      blockedBy: uid,
      updatedAt: Timestamp.fromMillis(nowOf(deps)),
    });
  });
  return { blocked: true };
}

/**
 * Lifts the caller's own block. A block placed by the other participant stays
 * in force, so unblocking never reopens a conversation the other side closed.
 */
export async function unblockMarketUserService(
  database: Firestore,
  uid: string,
  input: { conversationId?: unknown },
  deps: Pick<MarketDeps, "now"> = {},
): Promise<{ unblocked: true; status: string }> {
  const { conversationId } = parseConversationId(input.conversationId);
  const conversationRef = database.doc(`marketConversations/${conversationId}`);
  const stateRef = database.doc(`marketUserState/${uid}`);
  const status = await database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, uid, MARKET_ROLES);
    const conversation = await transaction.get(conversationRef);
    const participants = conversation.get("participants");
    if (!conversation.exists || !Array.isArray(participants) || !participants.includes(uid)) {
      throw new HttpsError("permission-denied", "MARKET_NOT_PARTICIPANT");
    }
    const otherUid = String(participants.find((participant) => participant !== uid));
    const otherState = await transaction.get(database.doc(`marketUserState/${otherUid}`));
    const otherBlocksMe = Array.isArray(otherState.get("blockedUids"))
      && (otherState.get("blockedUids") as unknown[]).includes(uid);
    transaction.set(stateRef, { blockedUids: FieldValue.arrayRemove(otherUid) }, { merge: true });
    const next = otherBlocksMe ? "blocked" : "open";
    transaction.update(conversationRef, {
      status: next,
      blockedBy: otherBlocksMe ? otherUid : FieldValue.delete(),
      updatedAt: Timestamp.fromMillis(nowOf(deps)),
    });
    return next;
  });
  return { unblocked: true, status };
}

/** Conversations the caller blocked, for the "Engellediklerin" list. */
export async function listMarketBlockedService(database: Firestore, uid: string) {
  const page = await database.collection("marketConversations").where("blockedBy", "==", uid).limit(100).get();
  return {
    blocked: page.docs
      .filter((conversation) => (conversation.get("participants") as string[] | undefined)?.includes(uid))
      .map((conversation) => {
        const isSeller = conversation.get("sellerUid") === uid;
        return {
          conversationId: conversation.id,
          otherName: publicName(conversation.get(isSeller ? "buyerName" : "sellerName")),
          listingTitle: String(conversation.get("listingTitle") ?? ""),
          listingThumbUrl: String(conversation.get("listingThumbUrl") ?? ""),
        };
      }),
  };
}

export async function reportMarketContentService(
  database: Firestore,
  uid: string,
  input: { targetType?: unknown; targetId?: unknown; reason?: unknown; note?: unknown },
  deps: MarketDeps,
): Promise<{ reportId: string }> {
  const now = nowOf(deps);
  const targetType = requireOneOf(input.targetType, ["listing", "conversation"] as const, "MARKET_REPORT_TARGET_INVALID");
  const targetId = targetType === "listing"
    ? requireId(input.targetId, "LISTING_ID")
    : parseConversationId(input.targetId).conversationId;
  const reason = requireOneOf(input.reason, MARKET_REPORT_REASONS, "MARKET_REPORT_REASON_INVALID");
  const note = input.note === undefined || input.note === null || input.note === ""
    ? ""
    : requireText(input.note, "MARKET_REPORT_NOTE", 1, 500);

  const reportRef = database.doc(`marketReports/${targetType}_${targetId}_${uid}`);
  const created = await database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, uid, MARKET_ROLES);
    let listingId: string;
    let reportedUid: string;
    if (targetType === "listing") {
      const listing = await transaction.get(database.doc(`marketListings/${targetId}`));
      if (!listing.exists || !MESSAGEABLE_LISTING_STATUSES.includes(String(listing.get("status")))) {
        throw new HttpsError("not-found", "MARKET_LISTING_NOT_FOUND");
      }
      listingId = targetId;
      reportedUid = String(listing.get("sellerUid"));
    } else {
      const conversation = await transaction.get(database.doc(`marketConversations/${targetId}`));
      const participants = conversation.get("participants");
      if (!conversation.exists || !Array.isArray(participants) || !participants.includes(uid)) {
        throw new HttpsError("permission-denied", "MARKET_NOT_PARTICIPANT");
      }
      listingId = String(conversation.get("listingId"));
      reportedUid = String(participants.find((participant) => participant !== uid));
    }
    if (reportedUid === uid) throw new HttpsError("invalid-argument", "MARKET_REPORT_SELF");
    const existing = await transaction.get(reportRef);
    if (existing.exists && existing.get("status") === "open") return false;
    transaction.set(reportRef, {
      reporterUid: uid, targetType, targetId, listingId, reportedUid, reason, note,
      status: "open", createdAt: Timestamp.fromMillis(now),
    });
    return true;
  });

  if (created) {
    const admins = await database.collection("users").where("role", "==", "good4Admin")
      .where("status", "==", "active").limit(5).get();
    await Promise.all(admins.docs.map((admin) => deps.notify(admin.id, {
      title: "Kampüs Dolabı: yeni şikayet",
      body: "Bir ilan ya da konuşma şikayet edildi.",
      data: { type: "market_admin_report", reportId: reportRef.id },
    })));
  }
  return { reportId: reportRef.id };
}

// ---------------------------------------------------------------------------
// Reads. The app reads Kampüs Dolabı only through these callables, so the
// collections stay closed to clients and campus and block filters run here.
// ---------------------------------------------------------------------------

const FEED_PAGE_SIZE = 20;
const POLL_PAGE_SIZE = 50;

function listingForStudent(listing: DocumentSnapshot, viewerUid: string, withDetail = false, favorites?: Set<string>) {
  const isMine = listing.get("sellerUid") === viewerUid;
  return {
    id: listing.id,
    title: String(listing.get("title") ?? ""),
    price: Number(listing.get("price") ?? 0),
    category: String(listing.get("category") ?? ""),
    condition: String(listing.get("condition") ?? ""),
    photos: (listing.get("photos") ?? []) as { url: string; thumbUrl: string }[],
    sellerName: publicName(listing.get("sellerName")),
    universityName: String(listing.get("universityName") ?? ""),
    status: String(listing.get("status") ?? ""),
    publishedAt: iso(listing.get("publishedAt")),
    createdAt: iso(listing.get("createdAt")),
    isMine,
    isFavorite: favorites?.has(listing.id) ?? false,
    ...(withDetail ? { description: String(listing.get("description") ?? "") } : {}),
    ...(isMine ? {
      rejectReason: listing.get("rejectReason") ?? null,
      expiresAt: iso(listing.get("expiresAt")),
      renewsLeft: Math.max(0, MARKET_LISTING_LIFETIME.maxRenewals - Number(listing.get("renewCount") ?? 0)),
    } : {}),
  };
}

async function viewerContext(database: Firestore, uid: string, now: number) {
  const [user, state, config] = await Promise.all([
    database.doc(`users/${uid}`).get(),
    database.doc(`marketUserState/${uid}`).get(),
    database.doc("app_config/campus_closet").get(),
  ]);
  if (!user.exists || user.get("status") !== "active") throw new HttpsError("permission-denied", "ACCOUNT_NOT_ACTIVE");
  const verified = hasVerifiedCampusEmail(user);
  const eduDomain = verified ? eduDomainOf(String(user.get("eduEmail"))) : null;
  const suspendedUntil = state.get("suspendedUntil");
  return {
    state,
    eduDomain,
    blocked: new Set((state.get("blockedUids") ?? []) as string[]),
    favorites: new Set((state.get("favoriteIds") ?? []) as string[]),
    me: {
      enabled: config.get("enabled") !== false,
      eduVerified: verified,
      eduEmail: verified ? String(user.get("eduEmail")) : null,
      universityName: eduDomain ? UNIVERSITY_NAMES[eduDomain] ?? eduDomain : null,
      termsAccepted: Number(state.get("termsVersion") ?? 0) >= MARKET_TERMS_VERSION,
      termsVersion: MARKET_TERMS_VERSION,
      suspendedUntil: suspendedUntil instanceof Timestamp && suspendedUntil.toMillis() > now ? iso(suspendedUntil) : null,
      unreadCount: Number(state.get("unreadCount") ?? 0),
    },
  };
}

/** Small status call for the home tile badge and the screen gates. */
export async function getMarketSummaryService(
  database: Firestore,
  uid: string,
  deps: Pick<MarketDeps, "now"> = {},
) {
  return { me: (await viewerContext(database, uid, nowOf(deps))).me };
}

/**
 * Newest published listings. Verified students see their own campus; others
 * see every campus read-only until they verify.
 */
export async function getMarketFeedService(
  database: Firestore,
  uid: string,
  input: { category?: unknown; before?: unknown; query?: unknown },
  deps: Pick<MarketDeps, "now"> = {},
) {
  const viewer = await viewerContext(database, uid, nowOf(deps));
  const category = input.category === undefined || input.category === null || input.category === ""
    ? null
    : requireOneOf(input.category, MARKET_CATEGORIES, "MARKET_CATEGORY_INVALID");
  if (input.query !== undefined && input.query !== null && (typeof input.query !== "string" || input.query.length > 60)) {
    throw new HttpsError("invalid-argument", "MARKET_QUERY_INVALID");
  }
  // The longest word narrows the query in Firestore; any other words filter that page.
  const words = typeof input.query === "string" ? searchWords(input.query) : [];
  const searchWord = [...words].sort((a, b) => b.length - a.length)[0];
  let query = database.collection("marketListings").where("status", "==", "published");
  if (viewer.eduDomain) query = query.where("eduDomain", "==", viewer.eduDomain);
  if (category) query = query.where("category", "==", category);
  if (searchWord) query = query.where("searchTokens", "array-contains", searchWord.slice(0, MAX_SEARCH_PREFIX));
  query = query.orderBy("publishedAt", "desc");
  if (input.before !== undefined && input.before !== null) {
    const before = Date.parse(String(input.before));
    if (Number.isNaN(before)) throw new HttpsError("invalid-argument", "MARKET_CURSOR_INVALID");
    query = query.startAfter(Timestamp.fromMillis(before));
  }
  const page = await query.limit(FEED_PAGE_SIZE).get();
  const listings = page.docs
    .filter((listing) => !viewer.blocked.has(String(listing.get("sellerUid"))))
    .filter((listing) => {
      if (words.length < 2) return true;
      const tokens = new Set((listing.get("searchTokens") ?? []) as string[]);
      return words.every((word) => tokens.has(word.slice(0, MAX_SEARCH_PREFIX)));
    })
    .map((listing) => listingForStudent(listing, uid, false, viewer.favorites));
  return {
    me: viewer.me,
    listings,
    nextBefore: page.size === FEED_PAGE_SIZE ? iso(page.docs.at(-1)?.get("publishedAt")) : null,
  };
}

export async function getMarketListingService(
  database: Firestore,
  uid: string,
  input: { listingId?: unknown },
  deps: Pick<MarketDeps, "now"> = {},
) {
  const listingId = requireId(input.listingId, "LISTING_ID");
  const [viewer, listing, conversation] = await Promise.all([
    viewerContext(database, uid, nowOf(deps)),
    database.doc(`marketListings/${listingId}`).get(),
    database.doc(`marketConversations/${listingId}_${uid}`).get(),
  ]);
  const visible = listing.exists && (listing.get("sellerUid") === uid || conversation.exists
    || MESSAGEABLE_LISTING_STATUSES.includes(String(listing.get("status"))));
  if (!visible) throw new HttpsError("not-found", "MARKET_LISTING_NOT_FOUND");
  return {
    me: viewer.me,
    listing: listingForStudent(listing, uid, true, viewer.favorites),
    conversationId: conversation.exists ? conversation.id : null,
    sameCampus: viewer.eduDomain === listing.get("eduDomain"),
  };
}

export async function listMyMarketListingsService(database: Firestore, uid: string) {
  const page = await database.collection("marketListings")
    .where("sellerUid", "==", uid)
    .orderBy("createdAt", "desc")
    .limit(50)
    .get();
  return {
    listings: page.docs
      .filter((listing) => !["removed", "removedByAdmin"].includes(String(listing.get("status"))))
      .map((listing) => listingForStudent(listing, uid)),
  };
}

function conversationForStudent(conversation: DocumentSnapshot, uid: string) {
  const isSeller = conversation.get("sellerUid") === uid;
  const offer = conversation.get("offer");
  return {
    id: conversation.id,
    listingId: String(conversation.get("listingId") ?? ""),
    listingTitle: String(conversation.get("listingTitle") ?? ""),
    listingThumbUrl: String(conversation.get("listingThumbUrl") ?? ""),
    listingPrice: Number(conversation.get("listingPrice") ?? 0),
    role: isSeller ? "seller" : "buyer",
    otherName: publicName(conversation.get(isSeller ? "buyerName" : "sellerName")),
    lastMessageText: String(conversation.get("lastMessageText") ?? ""),
    lastMessageAt: iso(conversation.get("lastMessageAt")),
    lastMessageMine: conversation.get("lastSenderUid") === uid,
    unread: Number(conversation.get(`unread.${uid}`) ?? 0),
    messageCount: Number(conversation.get("messageCount") ?? 0),
    status: String(conversation.get("status") ?? "open"),
    blockedByMe: conversation.get("blockedBy") === uid,
    offer: offer && typeof offer === "object"
      ? { percent: Number(offer.percent), price: Number(offer.price), status: String(offer.status) }
      : null,
  };
}

export async function listMarketConversationsService(database: Firestore, uid: string) {
  const page = await database.collection("marketConversations")
    .where("participants", "array-contains", uid)
    .orderBy("lastMessageAt", "desc")
    .limit(50)
    .get();
  const state = await database.doc(`marketUserState/${uid}`).get();
  return {
    unreadCount: Number(state.get("unreadCount") ?? 0),
    conversations: page.docs.map((conversation) => conversationForStudent(conversation, uid)),
  };
}

/**
 * Returns the newest messages (or only those after `after`) and clears the
 * caller's unread counter. The open chat screen polls this every few seconds.
 */
export async function getMarketMessagesService(
  database: Firestore,
  uid: string,
  input: { conversationId?: unknown; after?: unknown },
) {
  const { conversationId } = parseConversationId(input.conversationId);
  const conversationRef = database.doc(`marketConversations/${conversationId}`);
  const conversation = await conversationRef.get();
  const participants = conversation.get("participants");
  if (!conversation.exists || !Array.isArray(participants) || !participants.includes(uid)) {
    throw new HttpsError("permission-denied", "MARKET_NOT_PARTICIPANT");
  }
  let query = conversationRef.collection("messages").orderBy("createdAt", "desc");
  if (input.after !== undefined && input.after !== null) {
    const after = Date.parse(String(input.after));
    if (Number.isNaN(after)) throw new HttpsError("invalid-argument", "MARKET_CURSOR_INVALID");
    query = query.endBefore(Timestamp.fromMillis(after));
  }
  const page = await query.limit(POLL_PAGE_SIZE).get();
  if (Number(conversation.get(`unread.${uid}`) ?? 0) > 0) {
    await markMarketConversationReadService(database, uid, { conversationId });
  }
  return {
    conversation: { ...conversationForStudent(conversation, uid), unread: 0 },
    messages: page.docs.reverse().map((message) => ({
      id: message.id,
      mine: message.get("senderUid") === uid,
      type: String(message.get("type") ?? "text"),
      text: String(message.get("text") ?? ""),
      offerPercent: message.get("offerPercent") ?? null,
      offerPrice: message.get("offerPrice") ?? null,
      createdAt: iso(message.get("createdAt")),
    })),
  };
}

/** Saves or unsaves a listing for the caller ("Kaydedilenler"). */
export async function setMarketFavoriteService(
  database: Firestore,
  uid: string,
  input: { listingId?: unknown; saved?: unknown },
): Promise<{ saved: boolean }> {
  const listingId = requireId(input.listingId, "LISTING_ID");
  if (typeof input.saved !== "boolean") throw new HttpsError("invalid-argument", "MARKET_FAVORITE_INVALID");
  const saved = input.saved;
  const stateRef = database.doc(`marketUserState/${uid}`);
  await database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, uid, MARKET_ROLES);
    const state = await transaction.get(stateRef);
    const favorites = (state.get("favoriteIds") ?? []) as string[];
    if (saved) {
      const listing = await transaction.get(database.doc(`marketListings/${listingId}`));
      if (!listing.exists || !MESSAGEABLE_LISTING_STATUSES.includes(String(listing.get("status")))) {
        throw new HttpsError("not-found", "MARKET_LISTING_NOT_FOUND");
      }
      if (!favorites.includes(listingId) && favorites.length >= MARKET_LIMITS.maxFavorites) {
        throw new HttpsError("resource-exhausted", "MARKET_FAVORITE_LIMIT");
      }
      transaction.set(stateRef, { favoriteIds: FieldValue.arrayUnion(listingId) }, { merge: true });
    } else {
      transaction.set(stateRef, { favoriteIds: FieldValue.arrayRemove(listingId) }, { merge: true });
    }
  });
  return { saved };
}

/** Saved listings that are still visible, newest saved first; vanished ones are dropped from the list. */
export async function listMarketFavoritesService(database: Firestore, uid: string) {
  const state = await database.doc(`marketUserState/${uid}`).get();
  const ids = ((state.get("favoriteIds") ?? []) as string[]).slice().reverse();
  if (ids.length === 0) return { listings: [] };
  const blocked = new Set((state.get("blockedUids") ?? []) as string[]);
  const favorites = new Set(ids);
  const snapshots = await database.getAll(...ids.map((id) => database.doc(`marketListings/${id}`)));
  const visible = snapshots.filter((listing) => listing.exists
    && MESSAGEABLE_LISTING_STATUSES.includes(String(listing.get("status")))
    && !blocked.has(String(listing.get("sellerUid"))));
  const gone = ids.filter((id) => !visible.some((listing) => listing.id === id));
  if (gone.length > 0) {
    await database.doc(`marketUserState/${uid}`).set({ favoriteIds: FieldValue.arrayRemove(...gone) }, { merge: true });
  }
  return { listings: visible.map((listing) => listingForStudent(listing, uid, false, favorites)) };
}

// ---------------------------------------------------------------------------
// Retention
// ---------------------------------------------------------------------------

const DAY_MS = 24 * 60 * 60 * 1000;
const CLEANUP_BATCH = 200;

async function dropConversation(database: Firestore, conversation: DocumentSnapshot): Promise<void> {
  // Keep the participants' unread badges in step with the conversations that remain.
  for (const participant of (conversation.get("participants") ?? []) as string[]) {
    const unread = Number(conversation.get(`unread.${participant}`) ?? 0);
    if (unread <= 0) continue;
    const stateRef = database.doc(`marketUserState/${participant}`);
    await database.runTransaction(async (transaction) => {
      const state = await transaction.get(stateRef);
      if (!state.exists) return;
      transaction.update(stateRef, { unreadCount: Math.max(0, Number(state.get("unreadCount") ?? 0) - unread) });
    });
  }
  await database.recursiveDelete(conversation.ref);
}

/**
 * Daily KVKK cleanup for Kampüs Dolabı (see MARKET_RETENTION_DAYS). Each step
 * handles a bounded batch; anything left over is picked up the next day.
 */
export async function cleanupMarketDataService(
  database: Firestore,
  deletePhotos: (prefix: string) => Promise<void>,
  now = Date.now(),
  notify: MarketDeps["notify"] = async () => undefined,
) {
  const before = (days: number) => Timestamp.fromMillis(now - days * DAY_MS);
  const counts = {
    expired: 0, reminders: 0,
    closedListings: 0, soldPhotos: 0, soldListings: 0, conversations: 0, violations: 0, reports: 0, verifications: 0,
  };

  // Listing lifetime: expire what ran out, and remind sellers a few days before.
  const live = ["published", "reserved"];
  const ranOut = await database.collection("marketListings").where("status", "in", live)
    .where("expiresAt", "<=", Timestamp.fromMillis(now)).limit(CLEANUP_BATCH).get();
  for (const listing of ranOut.docs) {
    await listing.ref.update({ status: "expired", updatedAt: Timestamp.fromMillis(now) });
    counts.expired += 1;
    const renewsLeft = MARKET_LISTING_LIFETIME.maxRenewals - Number(listing.get("renewCount") ?? 0);
    await notify(String(listing.get("sellerUid")), {
      title: "İlanının süresi doldu",
      body: renewsLeft > 0 ? `"${listing.get("title")}" yayından kalktı. İlanlarım'dan 30 gün daha yayında tutabilirsin.`
        : `"${listing.get("title")}" yayından kalktı.`,
      data: { type: "market_listing", listingId: listing.id },
    });
  }
  const soon = await database.collection("marketListings").where("status", "in", live)
    .where("expiresAt", "<=", Timestamp.fromMillis(now + MARKET_LISTING_LIFETIME.reminderDaysBefore * DAY_MS))
    .limit(CLEANUP_BATCH).get();
  for (const listing of soon.docs) {
    const expiresAt = listing.get("expiresAt");
    if (listing.get("expiryReminderSentAt") || !(expiresAt instanceof Timestamp) || expiresAt.toMillis() <= now) continue;
    await listing.ref.update({ expiryReminderSentAt: Timestamp.fromMillis(now) });
    counts.reminders += 1;
    await notify(String(listing.get("sellerUid")), {
      title: "İlanın yakında yayından kalkacak",
      body: `"${listing.get("title")}" ${MARKET_LISTING_LIFETIME.reminderDaysBefore} gün içinde kalkacak. Hâlâ satılıksa İlanlarım'dan süresini uzat.`,
      data: { type: "market_listing", listingId: listing.id },
    });
  }

  const closed = await database.collection("marketListings")
    .where("status", "in", ["removed", "removedByAdmin", "rejected", "expired"])
    .where("updatedAt", "<", before(MARKET_RETENTION_DAYS.closedListing)).limit(CLEANUP_BATCH).get();
  for (const listing of closed.docs) {
    await deletePhotos(`market-listings/${listing.id}/`).catch(() => undefined);
    await listing.ref.delete();
    counts.closedListings += 1;
  }

  const soldOld = await database.collection("marketListings").where("status", "==", "sold")
    .where("soldAt", "<", before(MARKET_RETENTION_DAYS.soldListing)).limit(CLEANUP_BATCH).get();
  for (const listing of soldOld.docs) {
    await deletePhotos(`market-listings/${listing.id}/`).catch(() => undefined);
    await listing.ref.delete();
    counts.soldListings += 1;
  }
  const soldPhotos = await database.collection("marketListings").where("status", "==", "sold")
    .where("soldAt", "<", before(MARKET_RETENTION_DAYS.soldPhotos)).limit(CLEANUP_BATCH).get();
  for (const listing of soldPhotos.docs) {
    if (((listing.get("photos") ?? []) as unknown[]).length === 0) continue;
    await deletePhotos(`market-listings/${listing.id}/`).catch(() => undefined);
    await listing.ref.update({ photos: [], photosDeletedAt: Timestamp.fromMillis(now) });
    counts.soldPhotos += 1;
  }

  const conversations = await database.collection("marketConversations")
    .where("lastMessageAt", "<", before(MARKET_RETENTION_DAYS.conversation)).limit(CLEANUP_BATCH).get();
  for (const conversation of conversations.docs) {
    await dropConversation(database, conversation);
    counts.conversations += 1;
  }

  const deleteAll = async (docs: DocumentSnapshot[]) => {
    for (let offset = 0; offset < docs.length; offset += 400) {
      const batch = database.batch();
      docs.slice(offset, offset + 400).forEach((doc) => batch.delete(doc.ref));
      await batch.commit();
    }
    return docs.length;
  };
  counts.violations = await deleteAll((await database.collection("marketViolations")
    .where("createdAt", "<", before(MARKET_RETENTION_DAYS.violation)).limit(CLEANUP_BATCH).get()).docs);
  counts.reports = await deleteAll((await database.collection("marketReports").where("status", "==", "resolved")
    .where("resolvedAt", "<", before(MARKET_RETENTION_DAYS.resolvedReport)).limit(CLEANUP_BATCH).get()).docs);
  counts.verifications = await deleteAll((await database.collection("campusEmailVerifications")
    .where("expiresAt", "<", before(MARKET_RETENTION_DAYS.emailVerification)).limit(CLEANUP_BATCH).get()).docs);
  return counts;
}

// ---------------------------------------------------------------------------
// Admin moderation
// ---------------------------------------------------------------------------

async function requireAdmin(database: Firestore, uid: string): Promise<void> {
  await database.runTransaction((transaction) => requireActiveActor(database, transaction, uid, ["good4Admin"]));
}

function listingForAdmin(listing: DocumentSnapshot) {
  return {
    id: listing.id,
    sellerUid: listing.get("sellerUid"),
    sellerName: publicName(listing.get("sellerName")),
    universityName: listing.get("universityName"),
    category: listing.get("category"),
    condition: listing.get("condition"),
    title: listing.get("title"),
    description: listing.get("description"),
    price: listing.get("price"),
    photos: listing.get("photos") ?? [],
    status: listing.get("status"),
    moderationFlags: listing.get("moderationFlags") ?? [],
    createdAt: iso(listing.get("createdAt")),
  };
}

export async function listMarketModerationQueueService(database: Firestore, uid: string) {
  await requireAdmin(database, uid);
  const [pending, reports, violations] = await Promise.all([
    database.collection("marketListings").where("status", "==", "pending").orderBy("createdAt", "asc").limit(100).get(),
    database.collection("marketReports").where("status", "==", "open").orderBy("createdAt", "asc").limit(100).get(),
    database.collection("marketViolations").orderBy("createdAt", "desc").limit(50).get(),
  ]);
  const reportedListingIds = [...new Set(reports.docs.map((report) => String(report.get("listingId"))))];
  const reportedListings = reportedListingIds.length > 0
    ? await database.getAll(...reportedListingIds.map((id) => database.doc(`marketListings/${id}`)))
    : [];
  const listingsById = new Map(reportedListings.filter((listing) => listing.exists)
    .map((listing) => [listing.id, listingForAdmin(listing)]));
  return {
    pendingListings: pending.docs.map(listingForAdmin),
    reports: reports.docs.map((report) => ({
      id: report.id,
      targetType: report.get("targetType"),
      targetId: report.get("targetId"),
      reportedUid: report.get("reportedUid"),
      reason: report.get("reason"),
      note: report.get("note") ?? "",
      createdAt: iso(report.get("createdAt")),
      listing: listingsById.get(String(report.get("listingId"))) ?? null,
    })),
    violations: violations.docs.map((violation) => ({
      id: violation.id,
      uid: violation.get("uid"),
      term: violation.get("term"),
      context: violation.get("context"),
      excerpt: violation.get("excerpt"),
      suspended: violation.get("suspended") === true,
      createdAt: iso(violation.get("createdAt")),
    })),
  };
}

function audit(transaction: Transaction, database: Firestore, actorUid: string, action: string,
  targetType: string, targetId: string, metadata: Record<string, unknown> = {}) {
  transaction.create(database.collection("auditLogs").doc(), {
    action, actorUid, targetType, targetId, metadata, createdAt: FieldValue.serverTimestamp(),
  });
}

export async function reviewMarketListingService(
  database: Firestore,
  uid: string,
  input: { listingId?: unknown; decision?: unknown; reason?: unknown },
  deps: MarketDeps,
): Promise<{ status: "published" | "rejected" }> {
  const now = nowOf(deps);
  const listingId = requireId(input.listingId, "LISTING_ID");
  const decision = requireOneOf(input.decision, ["approve", "reject"] as const, "MARKET_DECISION_INVALID");
  const reason = decision === "reject" ? requireText(input.reason, "MARKET_REJECT_REASON", 3, 300) : null;
  const status = decision === "approve" ? "published" : "rejected";
  const listingRef = database.doc(`marketListings/${listingId}`);

  const listing = await database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, uid, ["good4Admin"]);
    const snapshot = await transaction.get(listingRef);
    if (!snapshot.exists) throw new HttpsError("not-found", "MARKET_LISTING_NOT_FOUND");
    if (snapshot.get("status") !== "pending") throw new HttpsError("failed-precondition", "MARKET_LISTING_NOT_PENDING");
    transaction.update(listingRef, {
      status,
      rejectReason: reason,
      reviewedBy: uid,
      updatedAt: Timestamp.fromMillis(now),
      ...(status === "published" ? {
        publishedAt: Timestamp.fromMillis(now),
        expiresAt: Timestamp.fromMillis(now + MARKET_LISTING_LIFETIME.days * 24 * 60 * 60 * 1000),
        renewCount: 0,
      } : { photos: [] }),
    });
    audit(transaction, database, uid, `market.listing.${decision}`, "marketListing", listingId,
      reason ? { reason } : {});
    return snapshot;
  });

  if (status === "rejected") {
    await deps.photos.deletePrefix(`market-listings/${listingId}/`).catch((error) => {
      console.error("market: photo cleanup failed", error);
    });
  }
  await deps.notify(String(listing.get("sellerUid")), status === "published"
    ? { title: "İlanın yayında", body: `"${listing.get("title")}" artık Kampüs Dolabı'nda.`,
      data: { type: "market_listing", listingId } }
    : { title: "İlanın yayınlanmadı", body: reason ?? "", data: { type: "market_listing", listingId } });
  return { status };
}

export async function resolveMarketReportService(
  database: Firestore,
  uid: string,
  input: { reportId?: unknown; action?: unknown; suspendDays?: unknown },
  deps: MarketDeps,
): Promise<{ resolved: true }> {
  const now = nowOf(deps);
  if (typeof input.reportId !== "string" || !/^[A-Za-z]+_[A-Za-z0-9_]{1,300}$/.test(input.reportId)) {
    throw new HttpsError("invalid-argument", "REPORT_ID_INVALID");
  }
  const action = requireOneOf(input.action, ["dismiss", "removeListing", "suspendUser"] as const,
    "MARKET_REPORT_ACTION_INVALID");
  const suspendDays = action === "suspendUser" ? input.suspendDays : null;
  if (suspendDays !== null && (typeof suspendDays !== "number" || !Number.isInteger(suspendDays)
    || suspendDays < 1 || suspendDays > 3650)) {
    throw new HttpsError("invalid-argument", "MARKET_SUSPEND_DAYS_INVALID");
  }
  const reportRef = database.doc(`marketReports/${input.reportId}`);

  const removedListingId = await database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, uid, ["good4Admin"]);
    const report = await transaction.get(reportRef);
    if (!report.exists) throw new HttpsError("not-found", "MARKET_REPORT_NOT_FOUND");
    const listingId = String(report.get("listingId"));
    const reportedUid = String(report.get("reportedUid"));
    let removed: string | null = null;
    if (action === "removeListing") {
      const listingRef = database.doc(`marketListings/${listingId}`);
      const listing = await transaction.get(listingRef);
      if (listing.exists) {
        transaction.update(listingRef, { status: "removedByAdmin", photos: [], updatedAt: Timestamp.fromMillis(now) });
        removed = listingId;
      }
    }
    if (action === "suspendUser") {
      transaction.set(database.doc(`marketUserState/${reportedUid}`), {
        suspendedUntil: Timestamp.fromMillis(now + Number(suspendDays) * 24 * 60 * 60 * 1000),
      }, { merge: true });
    }
    transaction.update(reportRef, { status: "resolved", resolution: action, resolvedBy: uid,
      resolvedAt: Timestamp.fromMillis(now) });
    audit(transaction, database, uid, `market.report.${action}`, "marketReport", reportRef.id,
      { listingId, reportedUid, ...(suspendDays ? { suspendDays } : {}) });
    return removed;
  });
  if (removedListingId) {
    await deps.photos.deletePrefix(`market-listings/${removedListingId}/`).catch(() => undefined);
  }
  return { resolved: true };
}

/** Lets an admin read a conversation only after a participant reported it. */
export async function getMarketReportConversationService(
  database: Firestore,
  uid: string,
  input: { reportId?: unknown },
) {
  await requireAdmin(database, uid);
  if (typeof input.reportId !== "string" || !/^conversation_[A-Za-z0-9_]{1,300}$/.test(input.reportId)) {
    throw new HttpsError("invalid-argument", "REPORT_ID_INVALID");
  }
  const report = await database.doc(`marketReports/${input.reportId}`).get();
  if (!report.exists || report.get("targetType") !== "conversation") {
    throw new HttpsError("not-found", "MARKET_REPORT_NOT_FOUND");
  }
  const conversationRef = database.doc(`marketConversations/${report.get("targetId")}`);
  const [conversation, messages] = await Promise.all([
    conversationRef.get(),
    conversationRef.collection("messages").orderBy("createdAt", "desc").limit(100).get(),
  ]);
  await database.collection("auditLogs").add({
    action: "market.conversation.viewed", actorUid: uid, targetType: "marketReport", targetId: report.id,
    metadata: {}, createdAt: FieldValue.serverTimestamp(),
  });
  return {
    sellerUid: conversation.get("sellerUid") ?? null,
    buyerUid: conversation.get("buyerUid") ?? null,
    sellerName: publicName(conversation.get("sellerName")),
    buyerName: publicName(conversation.get("buyerName")),
    listingTitle: conversation.get("listingTitle") ?? "",
    messages: messages.docs.reverse().map((message) => ({
      senderUid: message.get("senderUid"),
      type: message.get("type"),
      text: message.get("text"),
      createdAt: iso(message.get("createdAt")),
    })),
  };
}

// ---------------------------------------------------------------------------
// Account deletion
// ---------------------------------------------------------------------------

/** Removes a user's listings, photos, conversations and Kampüs Dolabı state. */
export async function eraseMarketData(
  database: Firestore,
  uid: string,
  deletePhotos: (prefix: string) => Promise<void>,
): Promise<void> {
  const listings = await database.collection("marketListings").where("sellerUid", "==", uid).get();
  for (const listing of listings.docs) {
    await deletePhotos(`market-listings/${listing.id}/`).catch(() => undefined);
    await listing.ref.delete();
  }

  const conversations = await database.collection("marketConversations")
    .where("participants", "array-contains", uid).get();
  for (const conversation of conversations.docs) {
    const participants = conversation.get("participants") as string[];
    const otherUid = participants.find((participant) => participant !== uid);
    const otherUnread = otherUid ? Number(conversation.get(`unread.${otherUid}`) ?? 0) : 0;
    if (otherUid && otherUnread > 0) {
      const otherStateRef: DocumentReference = database.doc(`marketUserState/${otherUid}`);
      await database.runTransaction(async (transaction) => {
        const otherState = await transaction.get(otherStateRef);
        if (!otherState.exists) return;
        transaction.update(otherStateRef, {
          unreadCount: Math.max(0, Number(otherState.get("unreadCount") ?? 0) - otherUnread),
        });
      });
    }
    await database.recursiveDelete(conversation.ref);
  }

  for (const field of ["reporterUid", "reportedUid"]) {
    const reports = await database.collection("marketReports").where(field, "==", uid).get();
    await Promise.all(reports.docs.map((report) => report.ref.delete()));
  }
  const violations = await database.collection("marketViolations").where("uid", "==", uid).get();
  await Promise.all(violations.docs.map((violation) => violation.ref.delete()));
  await database.doc(`marketUserState/${uid}`).delete();
}
