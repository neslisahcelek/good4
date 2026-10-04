import {
  FieldPath,
  FieldValue,
  Timestamp,
  type DocumentSnapshot,
  type Firestore,
  type QueryDocumentSnapshot,
} from "firebase-admin/firestore";
import { HttpsError } from "firebase-functions/v2/https";
import { getBusinessContextService } from "./business.js";
import { getCommunityContextService } from "./communityPortal.js";
import { requireActiveActor, requireNonEmptyString } from "./shared.js";

export type PortalContext =
  | { portalRole: "admin"; displayName: string }
  | ({ portalRole: "community" } & Awaited<ReturnType<typeof getCommunityContextService>>)
  | {
    portalRole: "business";
    organizationId: string;
    organizationName: string;
    membershipRole: "owner" | "staff";
  };

export interface AdminBusinessSummary {
  id: string;
  name: string;
  legacyTestBusinessId: string;
}

export interface AdminCommunitySummary {
  id: string;
  name: string;
  university: string;
  legacyTestCommunityId: string;
}

export interface LegacyBusinessSummary {
  id: string;
  name: string;
  linked: boolean;
}

export interface PendingCouponSummary {
  communityId: string;
  entryId: string;
  communityName: string;
  businessId: string;
  businessName: string;
  title: string;
  description: string;
  expiresOn: string;
  discountLabel: string;
  totalLimit: number | null;
}

export interface AuditSummary {
  id: string;
  action: string;
  actorUid: string;
  targetType: string;
  targetId: string;
  createdAt: string | null;
  metadata: Record<string, string | number | boolean | null>;
}

export interface FeedbackSummary {
  id: string;
  subject: string;
  message: string;
  userId: string;
  userEmail: string;
  userDisplayName: string;
  status: string;
  environment: "v2" | "legacyTest";
  createdAt: string | null;
  report: { communityId: string; entryId: string; kind: "event" | "coupon" } | null;
}

export interface DiningMenuDay {
  date: string;
  dayName: string;
  meals: string[];
  calories: number | null;
}

export interface DiningMenuSummary {
  weekLabel: string;
  weekStart: string;
  weekEnd: string;
  days: DiningMenuDay[];
  updatedAt: string | null;
}

export interface HomeBannerSummary {
  imageUrl: string;
  advertiserName: string;
  targetUrl: string;
  startsOn: string;
  endsOn: string;
  active: boolean;
  updatedAt: string | null;
}

export interface AcademicCalendarEventSummary {
  id: string;
  title: string;
  description: string;
  startDate: string;
  endDate: string;
  faculty: string;
  category: string;
  academicYear: string;
  sourcePage: number | null;
  active: boolean;
  updatedAt: string | null;
}

export interface AdminDashboardResult {
  nextCursors: Record<string, string | null>;
  businesses: AdminBusinessSummary[];
  communities: AdminCommunitySummary[];
  legacyBusinesses: LegacyBusinessSummary[];
  pendingCoupons: PendingCouponSummary[];
  feedback: FeedbackSummary[];
  audits: AuditSummary[];
  diningMenu: DiningMenuSummary | null;
  homeBanner: HomeBannerSummary | null;
  /** All four slider slots in order; null where a slot was never saved. */
  homeBanners: Array<HomeBannerSummary | null>;
  calendarEvents: AcademicCalendarEventSummary[];
}

const CALENDAR_FACULTIES = [
  "Genel",
  "Tıp Fakültesi",
  "Diş Hekimliği Fakültesi",
  "Hukuk Fakültesi",
  "Lisansüstü Eğitim",
  "Uzaktan Eğitim",
  "Yaz Okulu",
  "Yabancı Diller Yüksekokulu",
  "Güzel Sanatlar Fakültesi",
  "Antalya Devlet Konservatuvarı",
  "Yatay Geçiş ve ÇAP",
  "Değişim Programları",
  "2547 Ek Sınav",
  "Diğer Birimler",
] as const;

function calendarEventFromDocument(document: QueryDocumentSnapshot | DocumentSnapshot): AcademicCalendarEventSummary {
  const page = document.get("sourcePage");
  return {
    id: document.id,
    title: String(document.get("title") ?? ""),
    description: String(document.get("description") ?? ""),
    startDate: String(document.get("startDate") ?? ""),
    endDate: String(document.get("endDate") ?? ""),
    faculty: String(document.get("faculty") ?? "Genel"),
    category: String(document.get("category") ?? "Akademik"),
    academicYear: String(document.get("academicYear") ?? "2026-2027"),
    sourcePage: typeof page === "number" && Number.isInteger(page) ? page : null,
    active: document.get("active") !== false,
    updatedAt: timestampIso(document.get("updatedAt")),
  };
}

const ISO_DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

function requireIsoDate(value: unknown, fieldName: string): string {
  const date = requireNonEmptyString(value, fieldName, 10);
  const parsed = new Date(`${date}T00:00:00Z`);
  if (!ISO_DATE_PATTERN.test(date) || Number.isNaN(parsed.getTime()) || parsed.toISOString().slice(0, 10) !== date) {
    throw new HttpsError("invalid-argument", `${fieldName.toUpperCase()}_INVALID`);
  }
  return date;
}

function parseDiningMenuDay(value: unknown, weekStart: string, weekEnd: string): DiningMenuDay {
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    throw new HttpsError("invalid-argument", "MENU_DAY_INVALID");
  }
  const day = value as Record<string, unknown>;
  const date = requireIsoDate(day.date, "menuDayDate");
  if (date < weekStart || date > weekEnd) {
    throw new HttpsError("invalid-argument", "MENU_DAY_OUTSIDE_WEEK");
  }
  const dayName = requireNonEmptyString(day.dayName, "menuDayName", 24);
  if (!Array.isArray(day.meals) || day.meals.length === 0 || day.meals.length > 10) {
    throw new HttpsError("invalid-argument", "MENU_MEALS_INVALID");
  }
  const meals = day.meals.map((meal) => requireNonEmptyString(meal, "menuMeal", 120));
  const calories = day.calories === null || day.calories === undefined || day.calories === ""
    ? null
    : Number(day.calories);
  if (calories !== null && (!Number.isInteger(calories) || calories < 0 || calories > 5000)) {
    throw new HttpsError("invalid-argument", "MENU_CALORIES_INVALID");
  }
  return { date, dayName, meals, calories };
}

function diningMenuFromDocument(document: DocumentSnapshot): DiningMenuSummary | null {
  if (!document.exists) return null;
  const rawDays = document.get("days");
  if (!Array.isArray(rawDays)) return null;
  return {
    weekLabel: String(document.get("weekLabel") ?? ""),
    weekStart: String(document.get("weekStart") ?? ""),
    weekEnd: String(document.get("weekEnd") ?? ""),
    days: rawDays.map((day) => ({
      date: String(day?.date ?? ""),
      dayName: String(day?.dayName ?? ""),
      meals: Array.isArray(day?.meals) ? day.meals.map(String) : [],
      calories: typeof day?.calories === "number" ? day.calories : null,
    })),
    updatedAt: timestampIso(document.get("updatedAt")),
  };
}

export const HOME_BANNER_SLOTS = 4;

/** Slot 1 keeps the original document so older app versions still find their banner. */
export function homeBannerDocumentId(slot: number): string {
  return slot === 1 ? "home_banner" : `home_banner_${slot}`;
}

export function requireBannerSlot(value: unknown): number {
  if (value === undefined) return 1;
  if (!Number.isInteger(value) || (value as number) < 1 || (value as number) > HOME_BANNER_SLOTS) {
    throw new HttpsError("invalid-argument", "BANNER_SLOT_INVALID");
  }
  return value as number;
}

function homeBannerFromDocument(document: DocumentSnapshot): HomeBannerSummary | null {
  if (!document.exists) return null;
  return {
    imageUrl: String(document.get("imageUrl") ?? ""),
    advertiserName: String(document.get("advertiserName") ?? ""),
    targetUrl: String(document.get("targetUrl") ?? ""),
    startsOn: String(document.get("startsOn") ?? ""),
    endsOn: String(document.get("endsOn") ?? ""),
    active: document.get("active") === true,
    updatedAt: timestampIso(document.get("updatedAt")),
  };
}

function safeDocumentId(value: unknown, fieldName: string): string {
  const id = requireNonEmptyString(value, fieldName, 128);
  if (!/^[A-Za-z0-9_-]+$/.test(id)) {
    throw new HttpsError("invalid-argument", `${fieldName.toUpperCase()}_INVALID`);
  }
  return id;
}

function simpleMetadata(value: unknown): Record<string, string | number | boolean | null> {
  if (!value || typeof value !== "object" || Array.isArray(value)) return {};
  return Object.fromEntries(Object.entries(value).filter((entry): entry is [string, string | number | boolean | null] => (
    entry[1] === null || ["string", "number", "boolean"].includes(typeof entry[1])
  )));
}

function discountLabel(type: unknown, value: unknown): string {
  if (type === "percentage" && typeof value === "number") return `%${value}`;
  if (type === "fixed" && typeof value === "number") return `${value} TL`;
  if (type === "freeItem") return "Ücretsiz ürün";
  return "İndirim";
}

function timestampIso(value: unknown): string | null {
  if (value instanceof Timestamp) return value.toDate().toISOString();
  if (typeof value === "number" && Number.isFinite(value)) {
    const date = new Date(value * 1000);
    return Number.isNaN(date.getTime()) ? null : date.toISOString();
  }
  return null;
}

async function safeLegacyDocuments(
  request: Promise<QueryDocumentSnapshot[]>,
  label: string,
): Promise<QueryDocumentSnapshot[]> {
  let timeout: NodeJS.Timeout | undefined;
  try {
    return await Promise.race([
      request,
      new Promise<never>((_, reject) => {
        timeout = setTimeout(() => reject(new Error(`${label}_TIMEOUT`)), 4000);
      }),
    ]);
  } catch (error) {
    console.error("[getAdminDashboard] Legacy source unavailable", { label, error: String(error) });
    return [];
  } finally {
    if (timeout) clearTimeout(timeout);
  }
}

async function mirrorDiningMenuToLegacy(
  legacyTestDatabase: Firestore,
  menu: DiningMenuSummary,
  actorUid: string,
): Promise<void> {
  let timeout: NodeJS.Timeout | undefined;
  const write = legacyTestDatabase.doc("app_config/akdeniz_dining_menu").set({
    weekLabel: menu.weekLabel,
    weekStart: menu.weekStart,
    weekEnd: menu.weekEnd,
    days: menu.days,
    updatedAt: FieldValue.serverTimestamp(),
    updatedBy: actorUid,
    sourceEnvironment: "good4tr-v2",
  });
  try {
    await Promise.race([
      write,
      new Promise<never>((_, reject) => {
        timeout = setTimeout(() => reject(new Error("LEGACY_MENU_WRITE_TIMEOUT")), 6000);
      }),
    ]);
  } finally {
    if (timeout) clearTimeout(timeout);
  }
}

async function mirrorHomeBannerToLegacy(
  legacyTestDatabase: Firestore,
  banner: HomeBannerSummary,
  actorUid: string,
): Promise<void> {
  await legacyTestDatabase.doc("app_config/home_banner").set({
    imageUrl: banner.imageUrl,
    advertiserName: banner.advertiserName,
    targetUrl: banner.targetUrl,
    startsOn: banner.startsOn,
    endsOn: banner.endsOn,
    active: banner.active,
    updatedAt: FieldValue.serverTimestamp(),
    updatedBy: actorUid,
    sourceEnvironment: "good4tr-v2",
  });
}

export async function requireGood4Admin(database: Firestore, actorUid: string): Promise<void> {
  await database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, actorUid, ["good4Admin"]);
  });
}

export async function getPortalContextService(
  database: Firestore,
  actorUid: string,
): Promise<PortalContext> {
  const actor = await database.runTransaction((transaction) => (
    requireActiveActor(database, transaction, actorUid)
  ));
  if (actor.role === "good4Admin") {
    const user = await database.doc(`users/${actorUid}`).get();
    return {
      portalRole: "admin",
      displayName: String(user.get("displayName") ?? "Good4 Yöneticisi"),
    };
  }
  if (actor.role === "businessOwner" || actor.role === "businessStaff") {
    return { portalRole: "business", ...await getBusinessContextService(database, actorUid) };
  }
  if (actor.role === "communityManager" || actor.role === "communityStaff") {
    return { portalRole: "community", ...await getCommunityContextService(database, actorUid) };
  }
  throw new HttpsError("permission-denied", "PORTAL_ROLE_NOT_ALLOWED");
}

export async function getAdminDashboardService(
  database: Firestore,
  legacyTestDatabase: Firestore,
  actorUid: string,
  input: Record<string, unknown> = {},
): Promise<AdminDashboardResult> {
  await requireGood4Admin(database, actorUid);
  console.info("[getAdminDashboard] Loading dashboard", { actorUid });

  function pageQuery(db: Firestore, path: string, cursor: unknown, size = 100) {
    let query = db.collection(path).orderBy(FieldPath.documentId()).limit(size);
    if (cursor !== undefined) {
      const id = requireNonEmptyString(cursor, "cursor", 128);
      if (!/^[A-Za-z0-9_-]+$/.test(id)) throw new HttpsError("invalid-argument", "CURSOR_INVALID");
      query = query.startAfter(id);
    }
    return query;
  }
  const [
    organizations,
    auditsSnapshot,
    v2FeedbackSnapshot,
    diningMenuDocument,
    homeBannerDocument,
    calendarEventsSnapshot,
  ] = await Promise.all([
    pageQuery(database, "organizations", input.organizationsCursor).get(),
    database.collection("auditLogs").orderBy("createdAt", "desc").limit(50).get(),
    database.collection("feedbackSubmissions").orderBy("createdAt", "desc").limit(100).get(),
    database.doc("app_config/akdeniz_dining_menu").get(),
    database.doc("app_config/home_banner").get(),
    pageQuery(database, "academic_calendar_events", input.calendarCursor).get(),
  ]);
  const diningMenu = diningMenuFromDocument(diningMenuDocument);
  const homeBanner = homeBannerFromDocument(homeBannerDocument);
  const extraBannerDocuments = await Promise.all(
    [2, 3, 4].map((slot) => database.doc(`app_config/${homeBannerDocumentId(slot)}`).get()),
  );
  const homeBanners = [homeBanner, ...extraBannerDocuments.map(homeBannerFromDocument)];
  const calendarEvents = calendarEventsSnapshot.docs
    .map(calendarEventFromDocument)
    .sort((left, right) => left.startDate.localeCompare(right.startDate) || left.title.localeCompare(right.title, "tr"));
  const [legacyBusinessDocuments, legacyCommunityDocuments] = await Promise.all([
    safeLegacyDocuments(
      input.includeLegacy === true ? pageQuery(legacyTestDatabase, "businesses", input.legacyBusinessesCursor).get().then((snapshot) => snapshot.docs) : Promise.resolve([]),
      "LEGACY_BUSINESSES",
    ),
    safeLegacyDocuments(
      input.includeLegacy === true ? pageQuery(legacyTestDatabase, "communities", input.legacyCommunitiesCursor).get().then((snapshot) => snapshot.docs) : Promise.resolve([]),
      "LEGACY_COMMUNITIES",
    ),


  ]);

  const businesses = organizations.docs
    .filter((document) => document.get("type") === "business" && document.get("status") === "active")
    .map((document) => ({
      id: document.id,
      name: String(document.get("name") ?? "İşletme"),
      legacyTestBusinessId: String(document.get("legacyTestBusinessId") ?? ""),
    }))
    .sort((a, b) => a.name.localeCompare(b.name, "tr"));
  const v2Communities = organizations.docs
    .filter((document) => document.get("type") === "community" && document.get("status") === "active")
    .map((document) => ({
      id: document.id,
      name: String(document.get("name") ?? "Topluluk"),
      university: String(document.get("university") ?? ""),
      legacyTestCommunityId: String(document.get("legacyTestCommunityId") ?? ""),
    }))
    .sort((a, b) => a.name.localeCompare(b.name, "tr"));
  const linkedLegacyIds = new Set(businesses.map((business) => business.legacyTestBusinessId).filter(Boolean));
  const legacyBusinessNames = new Map<string, string>();
  const legacyBusinesses = legacyBusinessDocuments.map((document) => {
    const name = String(document.get("name") ?? "İsimsiz işletme");
    legacyBusinessNames.set(document.id, name);
    return { id: document.id, name, linked: linkedLegacyIds.has(document.id) };
  }).sort((a, b) => a.name.localeCompare(b.name, "tr"));

  const pendingCouponsNested = await Promise.all(legacyCommunityDocuments.map(async (community) => {
    const entries = await safeLegacyDocuments(
      community.ref.collection("entries").where("kind", "==", "coupon").where("status", "==", "pending").limit(50).get().then((snapshot) => snapshot.docs),
      `LEGACY_COMMUNITY_ENTRIES_${community.id}`,
    );
    return entries
      .filter((entry) => entry.get("kind") === "coupon" && entry.get("status") === "pending")
      .map((entry) => {
        const businessId = String(entry.get("businessId") ?? "");
        const totalLimitValue = entry.get("totalLimit");
        return {
          communityId: community.id,
          entryId: entry.id,
          communityName: String(community.get("name") ?? "Topluluk"),
          businessId,
          businessName: legacyBusinessNames.get(businessId) ?? "Bilinmeyen işletme",
          title: String(entry.get("title") ?? "Kupon"),
          description: String(entry.get("description") ?? ""),
          expiresOn: String(entry.get("date") ?? ""),
          discountLabel: discountLabel(entry.get("discountType"), entry.get("discountValue")),
          totalLimit: typeof totalLimitValue === "number" && totalLimitValue > 0 ? totalLimitValue : null,
        };
      });
  }));

  const audits = auditsSnapshot.docs.map((document) => {
    const timestamp = document.get("createdAt");
    return {
      id: document.id,
      action: String(document.get("action") ?? ""),
      actorUid: String(document.get("actorUid") ?? ""),
      targetType: String(document.get("targetType") ?? ""),
      targetId: String(document.get("targetId") ?? ""),
      createdAt: timestamp instanceof Timestamp ? timestamp.toDate().toISOString() : null,
      metadata: simpleMetadata(document.get("metadata")),
    };
  });

  const feedback = [
    ...v2FeedbackSnapshot.docs.map((document) => ({ document, environment: "v2" as const })),

  ].map(({ document, environment }) => ({
    id: `${environment}:${document.id}`,
    subject: String(document.get("subject") ?? "Geri bildirim"),
    message: String(document.get("message") ?? ""),
    userId: String(document.get("userId") ?? ""),
    userEmail: String(document.get("userEmail") ?? ""),
    userDisplayName: String(document.get("userDisplayName") ?? ""),
    status: String(document.get("status") ?? "new"),
    environment,
    createdAt: timestampIso(document.get("createdAt")),
    report: document.get("source") === "contentReport"
      && ["event", "coupon"].includes(String(document.get("report.kind") ?? ""))
      ? {
          communityId: String(document.get("report.communityId") ?? ""),
          entryId: String(document.get("report.entryId") ?? ""),
          kind: document.get("report.kind") as "event" | "coupon",
        }
      : null,
  })).sort((a, b) => (b.createdAt ?? "").localeCompare(a.createdAt ?? ""));

  const result = {
    nextCursors: { organizations: organizations.size === 100 ? organizations.docs.at(-1)!.id : null,
      calendar: calendarEventsSnapshot.size === 100 ? calendarEventsSnapshot.docs.at(-1)!.id : null,
      legacyBusinesses: legacyBusinessDocuments.length === 100 ? legacyBusinessDocuments.at(-1)!.id : null,
      legacyCommunities: legacyCommunityDocuments.length === 100 ? legacyCommunityDocuments.at(-1)!.id : null },
    businesses,
    communities: v2Communities,
    legacyBusinesses,
    pendingCoupons: pendingCouponsNested.flat().sort((a, b) => a.expiresOn.localeCompare(b.expiresOn)),
    feedback,
    audits,
    diningMenu,
    homeBanner,
    homeBanners,
    calendarEvents,
  };
  console.info("[getAdminDashboard] Dashboard ready", {
    businessCount: result.businesses.length,
    communityCount: result.communities.length,
    pendingCouponCount: result.pendingCoupons.length,
  });
  return result;
}

export async function saveAcademicCalendarEventService(
  database: Firestore,
  actorUid: string,
  input: {
    eventId?: unknown;
    title?: unknown;
    description?: unknown;
    startDate?: unknown;
    endDate?: unknown;
    faculty?: unknown;
    category?: unknown;
    academicYear?: unknown;
    sourcePage?: unknown;
    active?: unknown;
  },
): Promise<AcademicCalendarEventSummary> {
  await requireGood4Admin(database, actorUid);
  const title = requireNonEmptyString(input.title, "title", 180);
  const description = typeof input.description === "string" ? input.description.trim().slice(0, 1000) : "";
  const startDate = requireIsoDate(input.startDate, "startDate");
  const endDate = requireIsoDate(input.endDate, "endDate");
  if (startDate > endDate) throw new HttpsError("invalid-argument", "CALENDAR_DATE_RANGE_INVALID");
  const faculty = requireNonEmptyString(input.faculty, "faculty", 80);
  if (!(CALENDAR_FACULTIES as readonly string[]).includes(faculty)) {
    throw new HttpsError("invalid-argument", "CALENDAR_FACULTY_INVALID");
  }
  const category = requireNonEmptyString(input.category, "category", 40);
  const academicYear = requireNonEmptyString(input.academicYear, "academicYear", 20);
  if (!/^\d{4}-\d{4}$/.test(academicYear)) {
    throw new HttpsError("invalid-argument", "ACADEMIC_YEAR_INVALID");
  }
  const sourcePage = input.sourcePage === "" || input.sourcePage === null || input.sourcePage === undefined
    ? null
    : Number(input.sourcePage);
  if (sourcePage !== null && (!Number.isInteger(sourcePage) || sourcePage < 1 || sourcePage > 999)) {
    throw new HttpsError("invalid-argument", "SOURCE_PAGE_INVALID");
  }
  const active = input.active !== false;
  const eventRef = typeof input.eventId === "string" && input.eventId.trim()
    ? database.doc(`academic_calendar_events/${safeDocumentId(input.eventId, "eventId")}`)
    : database.collection("academic_calendar_events").doc();
  await eventRef.set({
    title,
    description,
    startDate,
    endDate,
    faculty,
    category,
    academicYear,
    sourcePage,
    active,
    updatedAt: FieldValue.serverTimestamp(),
    updatedBy: actorUid,
  }, { merge: true });
  await database.collection("auditLogs").add({
    action: input.eventId ? "academicCalendar.updated" : "academicCalendar.created",
    actorUid,
    targetType: "academicCalendarEvent",
    targetId: eventRef.id,
    metadata: { title, startDate, endDate, faculty, active },
    createdAt: FieldValue.serverTimestamp(),
  });
  const saved = await eventRef.get();
  return calendarEventFromDocument(saved);
}

export async function saveHomeBannerService(
  database: Firestore,
  legacyTestDatabase: Firestore,
  actorUid: string,
  input: {
    imageUrl?: unknown;
    advertiserName?: unknown;
    targetUrl?: unknown;
    startsOn?: unknown;
    endsOn?: unknown;
    active?: unknown;
    slot?: unknown;
  },
): Promise<HomeBannerSummary> {
  await requireGood4Admin(database, actorUid);
  const slot = requireBannerSlot(input.slot);
  const documentId = homeBannerDocumentId(slot);
  const imageUrl = requireNonEmptyString(input.imageUrl, "imageUrl", 2048);
  const advertiserName = requireNonEmptyString(input.advertiserName, "advertiserName", 80);
  const targetUrl = typeof input.targetUrl === "string" ? input.targetUrl.trim() : "";
  const startsOn = requireIsoDate(input.startsOn, "startsOn");
  const endsOn = requireIsoDate(input.endsOn, "endsOn");
  if (!imageUrl.startsWith("https://") || (targetUrl && !targetUrl.startsWith("https://"))) {
    throw new HttpsError("invalid-argument", "BANNER_URL_INVALID");
  }
  if (startsOn > endsOn) {
    throw new HttpsError("invalid-argument", "BANNER_DATE_RANGE_INVALID");
  }
  const active = input.active !== false;
  const bannerRef = database.doc(`app_config/${documentId}`);
  await bannerRef.set({
    imageUrl,
    advertiserName,
    targetUrl,
    startsOn,
    endsOn,
    active,
    updatedAt: FieldValue.serverTimestamp(),
    updatedBy: actorUid,
  });
  const banner = { imageUrl, advertiserName, targetUrl, startsOn, endsOn, active, updatedAt: null };
  // The legacy test app only knows one banner.
  if (slot === 1) await mirrorHomeBannerToLegacy(legacyTestDatabase, banner, actorUid);
  await database.collection("auditLogs").add({
    action: active ? "homeBanner.published" : "homeBanner.unpublished",
    actorUid,
    targetType: "appConfig",
    targetId: documentId,
    metadata: { advertiserName, startsOn, endsOn, active, slot },
    createdAt: FieldValue.serverTimestamp(),
  });
  const saved = await bannerRef.get();
  return homeBannerFromDocument(saved) ?? banner;
}

export async function saveDiningMenuService(
  database: Firestore,
  legacyTestDatabase: Firestore,
  actorUid: string,
  input: { weekLabel?: unknown; weekStart?: unknown; weekEnd?: unknown; days?: unknown },
): Promise<DiningMenuSummary> {
  await requireGood4Admin(database, actorUid);
  const weekLabel = requireNonEmptyString(input.weekLabel, "weekLabel", 80);
  const weekStart = requireIsoDate(input.weekStart, "weekStart");
  const weekEnd = requireIsoDate(input.weekEnd, "weekEnd");
  if (weekStart > weekEnd) {
    throw new HttpsError("invalid-argument", "MENU_WEEK_RANGE_INVALID");
  }
  if (!Array.isArray(input.days) || input.days.length === 0 || input.days.length > 7) {
    throw new HttpsError("invalid-argument", "MENU_DAYS_INVALID");
  }
  const days = input.days.map((day) => parseDiningMenuDay(day, weekStart, weekEnd));
  if (new Set(days.map((day) => day.date)).size !== days.length) {
    throw new HttpsError("invalid-argument", "MENU_DAY_DUPLICATE");
  }
  days.sort((left, right) => left.date.localeCompare(right.date));

  return writeDiningMenu(database, legacyTestDatabase, { weekLabel, weekStart, weekEnd, days }, actorUid);
}

/**
 * Stores an already validated weekly menu, mirrors it to the legacy app and records it in the audit log.
 * Shared by the admin panel and the scheduled SKS import.
 */
export async function writeDiningMenu(
  database: Firestore,
  legacyTestDatabase: Firestore,
  menu: { weekLabel: string; weekStart: string; weekEnd: string; days: DiningMenuDay[] },
  actorUid: string,
  extraFields: Record<string, unknown> = {},
): Promise<DiningMenuSummary> {
  const { weekLabel, weekStart, weekEnd, days } = menu;
  const menuRef = database.doc("app_config/akdeniz_dining_menu");
  await menuRef.set({
    weekLabel,
    weekStart,
    weekEnd,
    days,
    updatedAt: FieldValue.serverTimestamp(),
    updatedBy: actorUid,
    ...extraFields,
  });
  await mirrorDiningMenuToLegacy(legacyTestDatabase, { weekLabel, weekStart, weekEnd, days, updatedAt: null }, actorUid);
  await database.collection("auditLogs").add({
    action: "diningMenu.updated",
    actorUid,
    targetType: "appConfig",
    targetId: "akdeniz_dining_menu",
    metadata: { weekStart, weekEnd, dayCount: days.length },
    createdAt: FieldValue.serverTimestamp(),
  });

  const saved = await menuRef.get();
  return diningMenuFromDocument(saved) ?? { weekLabel, weekStart, weekEnd, days, updatedAt: null };
}

export async function moderateContentReportService(
  database: Firestore,
  legacyTestDatabase: Firestore,
  actorUid: string,
  input: { feedbackId: unknown; decision: unknown },
): Promise<{ status: "dismissed" | "resolved" }> {
  await requireGood4Admin(database, actorUid);
  const feedbackId = safeDocumentId(input.feedbackId, "feedbackId");
  if (input.decision !== "dismiss" && input.decision !== "remove") {
    throw new HttpsError("invalid-argument", "REPORT_DECISION_INVALID");
  }
  const feedbackRef = database.doc(`feedbackSubmissions/${feedbackId}`);
  const feedback = await feedbackRef.get();
  if (!feedback.exists || feedback.get("source") !== "contentReport") {
    throw new HttpsError("not-found", "CONTENT_REPORT_NOT_FOUND");
  }
  if (feedback.get("status") !== "new") {
    throw new HttpsError("failed-precondition", "CONTENT_REPORT_ALREADY_REVIEWED");
  }
  const status = input.decision === "remove" ? "resolved" : "dismissed";
  const report = feedback.get("report") as Record<string, unknown> | undefined;
  const communityId = safeDocumentId(report?.communityId, "communityId");
  const entryId = safeDocumentId(report?.entryId, "entryId");
  const kind = report?.kind;
  if (kind !== "event" && kind !== "coupon") {
    throw new HttpsError("failed-precondition", "CONTENT_REPORT_TARGET_INVALID");
  }

  if (input.decision === "remove" && kind === "event") {
    const eventRef = database.doc(`events/${entryId}`);
    await database.runTransaction(async (transaction) => {
      const currentFeedback = await transaction.get(feedbackRef);
      const event = await transaction.get(eventRef);
      if (currentFeedback.get("status") !== "new") {
        throw new HttpsError("failed-precondition", "CONTENT_REPORT_ALREADY_REVIEWED");
      }
      if (!event.exists || event.get("organizationId") !== communityId) {
        throw new HttpsError("not-found", "REPORTED_EVENT_NOT_FOUND");
      }
      transaction.update(eventRef, { status: "cancelled", updatedAt: FieldValue.serverTimestamp() });
      transaction.update(feedbackRef, {
        status, reviewedAt: FieldValue.serverTimestamp(), reviewedBy: actorUid, resolution: "contentRemoved",
      });
      transaction.create(database.collection("auditLogs").doc(), {
        action: "contentReport.contentRemoved", actorUid, targetType: "event", targetId: entryId,
        metadata: { communityId, feedbackId }, createdAt: FieldValue.serverTimestamp(),
      });
    });
    return { status };
  }

  if (input.decision === "remove" && kind === "coupon") {
    const community = await database.doc(`organizations/${communityId}`).get();
    const legacyId = String(community.get("legacyTestCommunityId") ?? "");
    if (!community.exists || community.get("type") !== "community" || !/^[A-Za-z0-9_-]{1,128}$/.test(legacyId)) {
      throw new HttpsError("not-found", "REPORTED_COMMUNITY_NOT_FOUND");
    }
    const entryRef = legacyTestDatabase.doc(`communities/${legacyId}/entries/${entryId}`);
    await legacyTestDatabase.runTransaction(async (transaction) => {
      const entry = await transaction.get(entryRef);
      if (!entry.exists || entry.get("kind") !== "coupon") {
        throw new HttpsError("not-found", "REPORTED_COUPON_NOT_FOUND");
      }
      transaction.update(entryRef, { status: "cancelled" });
    });
  }

  await database.runTransaction(async (transaction) => {
    const currentFeedback = await transaction.get(feedbackRef);
    if (currentFeedback.get("status") !== "new") {
      throw new HttpsError("failed-precondition", "CONTENT_REPORT_ALREADY_REVIEWED");
    }
    transaction.update(feedbackRef, {
      status, reviewedAt: FieldValue.serverTimestamp(), reviewedBy: actorUid,
      resolution: input.decision === "remove" ? "contentRemoved" : "dismissed",
    });
    transaction.create(database.collection("auditLogs").doc(), {
      action: input.decision === "remove" ? "contentReport.contentRemoved" : "contentReport.dismissed",
      actorUid, targetType: kind, targetId: entryId,
      metadata: { communityId, feedbackId }, createdAt: FieldValue.serverTimestamp(),
    });
  });
  return { status };
}

export async function reviewLegacyCouponService(
  database: Firestore,
  legacyTestDatabase: Firestore,
  actorUid: string,
  input: { communityId: unknown; entryId: unknown; decision: unknown },
): Promise<{ status: "published" | "cancelled" }> {
  await requireGood4Admin(database, actorUid);
  const communityId = safeDocumentId(input.communityId, "communityId");
  const entryId = safeDocumentId(input.entryId, "entryId");
  if (input.decision !== "approve" && input.decision !== "reject") {
    throw new HttpsError("invalid-argument", "COUPON_REVIEW_DECISION_INVALID");
  }
  const status = input.decision === "approve" ? "published" : "cancelled";
  const entryRef = legacyTestDatabase.doc(`communities/${communityId}/entries/${entryId}`);

  await legacyTestDatabase.runTransaction(async (transaction) => {
    const entry = await transaction.get(entryRef);
    if (!entry.exists || entry.get("kind") !== "coupon") {
      throw new HttpsError("not-found", "COUPON_NOT_FOUND");
    }
    const currentStatus = entry.get("status");
    if (currentStatus !== "pending" && currentStatus !== status) {
      throw new HttpsError("failed-precondition", "COUPON_ALREADY_REVIEWED");
    }
    if (currentStatus === "pending") {
      transaction.update(entryRef, { status });
    }
  });

  const auditId = `legacyCoupon_${communityId}_${entryId}_${input.decision}`;
  await database.doc(`auditLogs/${auditId}`).set({
    action: input.decision === "approve" ? "legacyCoupon.approved" : "legacyCoupon.rejected",
    actorUid,
    targetType: "legacyCoupon",
    targetId: `${communityId}:${entryId}`,
    metadata: { communityId, entryId, status },
    createdAt: FieldValue.serverTimestamp(),
  }, { merge: true });
  return { status };
}
