import { FieldValue, Timestamp, type Firestore } from "firebase-admin/firestore";
import { HttpsError } from "firebase-functions/v2/https";
import { cancelEventService, saveEventService } from "./events.js";
import { requireActiveActor, requireNonEmptyString } from "./shared.js";

export interface CommunityContextResult {
  organizationId: string;
  organizationName: string;
  university: string;
  legacyTestCommunityId: string | null;
  membershipRole: "manager" | "staff";
}

export interface CommunityPortalEntry {
  id: string;
  kind: "event" | "coupon";
  title: string;
  description: string;
  date: string;
  time: string;
  location: string;
  categoryId: string;
  businessId: string;
  businessName: string;
  discountType: "percentage" | "fixed" | "freeItem";
  discountValue: number;
  capacity: number;
  totalLimit: number;
  status: "published" | "pending" | "cancelled";
  registrationCount: number;
  attendanceCount: number;
  participants: Array<{ userId: string; displayName: string; registeredAt: string | null; checkedIn: boolean }>;
}

export interface CommunityPortalDashboard {
  community: { name: string; university: string; description: string; logoUrl: string; coverUrl: string };
  followerCount: number;
  businesses: Array<{ id: string; name: string }>;
  entries: CommunityPortalEntry[];
}

function safeId(value: unknown, fieldName: string): string {
  const id = requireNonEmptyString(value, fieldName, 128);
  if (!/^[A-Za-z0-9_-]+$/.test(id)) throw new HttpsError("invalid-argument", `${fieldName.toUpperCase()}_INVALID`);
  return id;
}

function integer(value: unknown, fallback = 0): number {
  return Number.isInteger(value) && Number(value) >= 0 ? Number(value) : fallback;
}

function dateString(value: unknown, fieldName: string): string {
  const result = requireNonEmptyString(value, fieldName, 10);
  if (!/^\d{4}-\d{2}-\d{2}$/.test(result) || Number.isNaN(new Date(`${result}T00:00:00Z`).getTime())) {
    throw new HttpsError("invalid-argument", `${fieldName.toUpperCase()}_INVALID`);
  }
  return result;
}

function instant(value: unknown): string | null {
  if (value instanceof Timestamp) return value.toDate().toISOString();
  if (typeof value === "number") return new Date(value * 1000).toISOString();
  return null;
}

function eventDateTime(value: unknown): { date: string; time: string } {
  if (!(value instanceof Timestamp)) return { date: "", time: "" };
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Europe/Istanbul", year: "numeric", month: "2-digit", day: "2-digit",
    hour: "2-digit", minute: "2-digit", hourCycle: "h23",
  }).formatToParts(value.toDate());
  const part = (type: Intl.DateTimeFormatPartTypes) => parts.find((item) => item.type === type)?.value ?? "";
  return { date: `${part("year")}-${part("month")}-${part("day")}`, time: `${part("hour")}:${part("minute")}` };
}

export async function getCommunityContextService(database: Firestore, actorUid: string): Promise<CommunityContextResult> {
  await database.runTransaction((transaction) => requireActiveActor(
    database, transaction, actorUid, ["communityManager", "communityStaff"],
  ));
  const memberships = await database.collectionGroup("members").where("userId", "==", actorUid).limit(10).get();
  for (const membership of memberships.docs) {
    const role = membership.get("role");
    if (membership.get("status") !== "active" || !["manager", "staff"].includes(role)) continue;
    const organizationRef = membership.ref.parent.parent;
    if (!organizationRef) continue;
    const organization = await organizationRef.get();
    if (!organization.exists || organization.get("type") !== "community" || organization.get("status") !== "active") continue;
    const legacyValue = String(organization.get("legacyTestCommunityId") ?? "");
    return {
      organizationId: organization.id,
      organizationName: String(organization.get("name") ?? "Topluluk"),
      university: String(organization.get("university") ?? ""),
      legacyTestCommunityId: /^[A-Za-z0-9_-]{1,128}$/.test(legacyValue) ? legacyValue : null,
      membershipRole: role as "manager" | "staff",
    };
  }
  throw new HttpsError("failed-precondition", "COMMUNITY_MEMBERSHIP_NOT_FOUND");
}

export async function getCommunityPortalDashboardService(
  database: Firestore, legacyDatabase: Firestore, actorUid: string,
): Promise<CommunityPortalDashboard> {
  const context = await getCommunityContextService(database, actorUid);
  const organizationRef = database.doc(`organizations/${context.organizationId}`);
  const legacyCommunityRef = context.legacyTestCommunityId
    ? legacyDatabase.doc(`communities/${context.legacyTestCommunityId}`) : null;
  const [organization, followers, eventsSnapshot, legacyCommunity, legacyEntriesSnapshot, businessesSnapshot] = await Promise.all([
    organizationRef.get(), organizationRef.collection("followers").get(),
    database.collection("events").where("organizationId", "==", context.organizationId).get(),
    legacyCommunityRef?.get() ?? Promise.resolve(null),
    legacyCommunityRef?.collection("entries").where("kind", "==", "coupon").get() ?? Promise.resolve(null),
    legacyDatabase.collection("businesses").get(),
  ]);
  if (!organization.exists) throw new HttpsError("not-found", "COMMUNITY_NOT_FOUND");
  const businessNames = new Map(businessesSnapshot.docs.map((item) => [item.id, String(item.get("name") ?? "İşletme")]));
  const eventEntries = await Promise.all(eventsSnapshot.docs.map(async (entry): Promise<CommunityPortalEntry> => {
    const [registrations, checkins] = await Promise.all([
      entry.ref.collection("registrations").get(), entry.ref.collection("checkins").get(),
    ]);
    const arrived = new Set(checkins.docs.map((item) => item.id));
    const participants = registrations.docs.map((item) => ({
      userId: String(item.get("userId") ?? ""), displayName: String(item.get("displayName") ?? "Good4 öğrencisi"),
      registeredAt: instant(item.get("registeredAt")), checkedIn: arrived.has(item.id),
    })).sort((a, b) => (b.registeredAt ?? "").localeCompare(a.registeredAt ?? ""));
    const dateTime = eventDateTime(entry.get("startsAt"));
    return {
      id: entry.id, kind: "event", title: String(entry.get("title") ?? ""),
      description: String(entry.get("description") ?? ""), date: dateTime.date, time: dateTime.time,
      location: String(entry.get("location") ?? ""), businessId: "", businessName: "",
      categoryId: String(entry.get("categoryId") ?? ""),
      discountType: "percentage", discountValue: 0, capacity: integer(entry.get("capacity")), totalLimit: 0,
      status: entry.get("status") === "cancelled" ? "cancelled" : "published",
      registrationCount: integer(entry.get("registrationCount"), participants.length),
      attendanceCount: integer(entry.get("attendanceCount"), arrived.size), participants,
    };
  }));
  const couponEntries: CommunityPortalEntry[] = (legacyEntriesSnapshot?.docs ?? []).map((entry) => {
    const statusValue = entry.get("status");
    const status = statusValue === "cancelled" || statusValue === "pending" ? statusValue : "published";
    const discountTypeValue = entry.get("discountType");
    const discountType = discountTypeValue === "fixed" || discountTypeValue === "freeItem" ? discountTypeValue : "percentage";
    const businessId = String(entry.get("businessId") ?? "");
    return {
      id: entry.id, kind: "coupon", title: String(entry.get("title") ?? ""),
      description: String(entry.get("description") ?? ""), date: String(entry.get("date") ?? ""), time: "",
      location: String(entry.get("location") ?? ""), businessId, businessName: businessNames.get(businessId) ?? "",
      categoryId: "",
      discountType, discountValue: integer(entry.get("discountValue")), capacity: 0,
      totalLimit: integer(entry.get("totalLimit")), status, registrationCount: 0, attendanceCount: 0, participants: [],
    };
  });
  const entries = [...eventEntries, ...couponEntries];
  return {
    community: {
      name: String(organization.get("name") ?? legacyCommunity?.get("name") ?? context.organizationName),
      university: String(organization.get("university") ?? legacyCommunity?.get("university") ?? context.university),
      description: String(organization.get("description") ?? legacyCommunity?.get("description") ?? ""),
      logoUrl: String(organization.get("logoUrl") ?? legacyCommunity?.get("logoUrl") ?? ""),
      coverUrl: String(organization.get("coverUrl") ?? legacyCommunity?.get("coverUrl") ?? ""),
    },
    followerCount: followers.size,
    businesses: businessesSnapshot.docs.map((item) => ({ id: item.id, name: businessNames.get(item.id) ?? "İşletme" }))
      .sort((a, b) => a.name.localeCompare(b.name, "tr")),
    entries: entries.sort((a, b) => `${b.date}${b.time}`.localeCompare(`${a.date}${a.time}`)),
  };
}

export async function saveCommunityPortalEntryService(
  database: Firestore, legacyDatabase: Firestore, actorUid: string, input: Record<string, unknown>,
): Promise<{ entryId: string; status: "published" | "pending" }> {
  const context = await getCommunityContextService(database, actorUid);
  if (context.membershipRole !== "manager") throw new HttpsError("permission-denied", "COMMUNITY_MANAGER_REQUIRED");
  const kind = input.kind === "coupon" ? "coupon" : input.kind === "event" ? "event" : null;
  if (!kind) throw new HttpsError("invalid-argument", "ENTRY_KIND_INVALID");
  if (kind === "event") {
    const result = await saveEventService(database, actorUid, context.organizationId, {
      eventId: input.entryId, title: input.title, description: input.description,
      date: input.date, time: input.time, location: input.location, capacity: input.capacity,
      status: "published", imageUrl: input.imageUrl,
      categoryId: input.categoryId,
    });
    return { entryId: result.eventId, status: "published" };
  }
  if (!context.legacyTestCommunityId) throw new HttpsError("failed-precondition", "LEGACY_COUPON_NOT_LINKED");
  const entryId = typeof input.entryId === "string" && input.entryId.trim() ? safeId(input.entryId, "entryId") : "";
  const title = requireNonEmptyString(input.title, "title", 120);
  const description = requireNonEmptyString(input.description, "description", 4000);
  const date = dateString(input.date, "date");
  const businessId = safeId(input.businessId, "businessId");
  const discountType = input.discountType === "fixed" || input.discountType === "freeItem" ? input.discountType : "percentage";
  const discountValue = integer(input.discountValue, -1);
  if ((discountType === "percentage" && (discountValue < 1 || discountValue > 100))
    || (discountType === "fixed" && discountValue < 1)) {
    throw new HttpsError("invalid-argument", "DISCOUNT_VALUE_INVALID");
  }
  const business = await legacyDatabase.doc(`businesses/${businessId}`).get();
  if (!business.exists) throw new HttpsError("not-found", "BUSINESS_NOT_FOUND");
  const entries = legacyDatabase.collection(`communities/${context.legacyTestCommunityId}/entries`);
  const entryRef = entryId ? entries.doc(entryId) : entries.doc();
  const current = entryId ? await entryRef.get() : null;
  if (entryId && (!current?.exists || current.get("kind") !== kind || current.get("status") === "cancelled")) {
    throw new HttpsError("failed-precondition", "ENTRY_NOT_EDITABLE");
  }
  await entryRef.set({
    kind, title, description, date, time: "", location: String(business.get("name") ?? "İşletme"),
    imageUrl: String(current?.get("imageUrl") ?? ""), code: String(current?.get("code") ?? ""), businessId,
    discountType, discountValue, capacity: 0, totalLimit: integer(input.totalLimit),
    perUserLimit: 1, status: "pending",
  });
  await database.collection("auditLogs").add({
    action: entryId ? "communityEntry.updated" : "communityEntry.created",
    actorUid, targetType: "legacyCommunityEntry", targetId: `${context.legacyTestCommunityId}:${entryRef.id}`,
    metadata: { organizationId: context.organizationId, kind, status: "pending" }, createdAt: FieldValue.serverTimestamp(),
  });
  return { entryId: entryRef.id, status: "pending" };
}

export async function cancelCommunityPortalEntryService(
  database: Firestore, legacyDatabase: Firestore, actorUid: string, input: { entryId: unknown },
): Promise<{ status: "cancelled" }> {
  const context = await getCommunityContextService(database, actorUid);
  if (context.membershipRole !== "manager") throw new HttpsError("permission-denied", "COMMUNITY_MANAGER_REQUIRED");
  const entryId = safeId(input.entryId, "entryId");
  const v2Event = await database.doc(`events/${entryId}`).get();
  if (v2Event.exists && v2Event.get("organizationId") === context.organizationId) {
    return cancelEventService(database, actorUid, context.organizationId, entryId);
  }
  if (!context.legacyTestCommunityId) throw new HttpsError("not-found", "ENTRY_NOT_FOUND");
  const entryRef = legacyDatabase.doc(`communities/${context.legacyTestCommunityId}/entries/${entryId}`);
  const entry = await entryRef.get();
  if (!entry.exists || entry.get("kind") !== "coupon") throw new HttpsError("not-found", "ENTRY_NOT_FOUND");
  if (entry.get("status") !== "cancelled") await entryRef.update({ status: "cancelled" });
  await database.collection("auditLogs").add({
    action: "communityEntry.cancelled", actorUid, targetType: "legacyCommunityEntry",
    targetId: `${context.legacyTestCommunityId}:${entryId}`,
    metadata: { organizationId: context.organizationId }, createdAt: FieldValue.serverTimestamp(),
  });
  return { status: "cancelled" };
}
