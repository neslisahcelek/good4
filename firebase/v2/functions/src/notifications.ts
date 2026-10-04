import { createHash } from "node:crypto";
import { FieldPath, FieldValue, Timestamp, type Firestore, type Transaction } from "firebase-admin/firestore";
import { HttpsError } from "firebase-functions/v2/https";
import { requireActiveActor, requireNonEmptyString } from "./shared.js";
import { optionalJobsAllowed } from "./costControl.js";
import { assertRateLimit } from "./rateLimit.js";
import { requireGood4Admin } from "./adminPortal.js";

export type NotificationKind = "eventPublished" | "eventChanged" | "eventCancelled" | "eventReminder" | "announcement";
export interface NotificationJob {
  kind: NotificationKind;
  title: string;
  body: string;
  organizationId: string;
  eventId: string;
  startsAt: number;
  audience: "all" | "followers" | "registrations" | "self";
  actorUid: string;
  test?: boolean;
}
export const notificationsEnabled = () => process.env.NOTIFICATIONS_ENABLED === "true";
const hash = (value: string) => createHash("sha256").update(value).digest("hex");
function id(value: unknown, name: string, maxLength = 128): string {
  const result = requireNonEmptyString(value, name, maxLength);
  if (!/^[A-Za-z0-9_-]+$/.test(result)) throw new HttpsError("invalid-argument", "INVALID_ID");
  return result;
}

export function enqueueNotification(database: Firestore, transaction: Transaction, jobId: string, job: NotificationJob) {
  transaction.create(database.doc(`notificationJobs/${jobId}`), {
    ...job, status: "queued", createdAt: FieldValue.serverTimestamp(), expiresAt: Timestamp.fromMillis(Date.now() + 86400000), accepted: 0, failed: 0,
    users: 0, skipped: 0, reservedUsers: 0,
  });
  transaction.create(database.doc(`notificationDispatches/${hash(`${jobId}:first`)}`), { jobId, cursor: "", createdAt: FieldValue.serverTimestamp() });
}

/** A random installation secret permits account changes without letting another account steal an installation. */
export async function registerPushDeviceService(database: Firestore, uid: string, input: Record<string, unknown>) {
  await database.runTransaction((transaction) => assertRateLimit(database, transaction, { key: `devices:${uid}`, limit: 10, windowSeconds: 60 }));
  const installationId = id(input.installationId, "installationId");
  const secret = requireNonEmptyString(input.secret, "secret", 256);
  if (secret.length < 32 || !["android", "ios"].includes(String(input.platform)) || typeof input.permission !== "boolean") {
    throw new HttpsError("invalid-argument", "DEVICE_INVALID");
  }
  const token = typeof input.token === "string" ? input.token.trim() : "";
  if (token.length > 4096) throw new HttpsError("invalid-argument", "TOKEN_INVALID");
  const blocked = Array.isArray(input.blockedCommunityIds) ? input.blockedCommunityIds : [];
  if (blocked.length > 1000 || blocked.some((value) => typeof value !== "string" || !/^[A-Za-z0-9_-]{1,128}$/.test(value))) {
    throw new HttpsError("invalid-argument", "BLOCKED_COMMUNITIES_INVALID");
  }
  const ref = database.doc(`pushDevices/${installationId}`);
  await database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, uid);
    const current = await transaction.get(ref);
    const preferenceRef = database.doc(`users/${uid}/notificationPreferences/default`);
    const preferences = await transaction.get(preferenceRef);
    const ownedDevices = await transaction.get(database.collection("pushDevices").where("uid", "==", uid).limit(6));
    if (current.get("uid") !== uid && ownedDevices.size >= 5) throw new HttpsError("resource-exhausted", "DEVICE_LIMIT_REACHED");
    if (current.exists && current.get("secretHash") !== hash(secret)) throw new HttpsError("permission-denied", "INSTALLATION_OWNERSHIP_REQUIRED");
    transaction.set(ref, { uid, secretHash: hash(secret), token, platform: input.platform,
      permission: input.permission, blockedCommunityIds: blocked, updatedAt: FieldValue.serverTimestamp() });
    if (!preferences.exists) transaction.create(preferenceRef, { eventUpdates: true, reminders: true, announcements: false });
    transaction.update(database.doc(`users/${uid}`), { mobileNotifications: true });
  });
  return { registered: true };
}

export async function unregisterPushDeviceService(database: Firestore, uid: string, input: Record<string, unknown>) {
  const ref = database.doc(`pushDevices/${id(input.installationId, "installationId")}`);
  const secret = requireNonEmptyString(input.secret, "secret", 256);
  await database.runTransaction(async (transaction) => {
    const current = await transaction.get(ref);
    if (current.exists && (current.get("uid") !== uid || current.get("secretHash") !== hash(secret))) {
      throw new HttpsError("permission-denied", "INSTALLATION_OWNERSHIP_REQUIRED");
    }
    transaction.delete(ref);
  });
  return { unregistered: true };
}

export async function updateNotificationPreferencesService(database: Firestore, uid: string, input: Record<string, unknown>) {
  if (["eventUpdates", "reminders", "announcements"].some((key) => typeof input[key] !== "boolean")) {
    throw new HttpsError("invalid-argument", "PREFERENCES_INVALID");
  }
  await database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, uid);
    transaction.set(database.doc(`users/${uid}/notificationPreferences/default`), {
      eventUpdates: input.eventUpdates, reminders: input.reminders, announcements: input.announcements,
    });
  });
  return { saved: true };
}

export async function markNotificationsReadService(database: Firestore, uid: string, input: Record<string, unknown>) {
  await database.runTransaction((transaction) => requireActiveActor(database, transaction, uid));
  const all = input.all === true;
  const collection = database.collection(`users/${uid}/notifications`);
  if (input.notificationIds !== undefined && (!Array.isArray(input.notificationIds)
    || input.notificationIds.length > 100)) throw new HttpsError("invalid-argument", "NOTIFICATION_IDS_INVALID");
  // New clients capture the visible IDs; old clients use the same newest-100 window as the inbox.
  const references = Array.isArray(input.notificationIds)
    ? [...new Set(input.notificationIds.map((value) => id(value, "notificationId", 256)))].map((value) => collection.doc(value))
    : all ? (await collection.orderBy("createdAt", "desc").limit(100).get()).docs.map((document) => document.ref)
      : [collection.doc(id(input.notificationId, "notificationId", 256))];
  if (references.length > 0) await database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, uid);
    const documents = await transaction.getAll(...references);
    for (const document of documents) if (document.exists && document.get("readAt") === 0) {
      transaction.update(document.ref, { readAt: Math.floor(Date.now() / 1000) });
    }
  });
  return { saved: true };
}

async function validateAnnouncement(database: Firestore, uid: string, input: Record<string, unknown>): Promise<NotificationJob> {
  await requireGood4Admin(database, uid);
  const title = requireNonEmptyString(input.title, "title", 120);
  const body = requireNonEmptyString(input.body, "body", 2000);
  const audience = input.audience === "followers" ? "followers" : input.audience === "all" ? "all" : null;
  if (!audience) throw new HttpsError("invalid-argument", "AUDIENCE_INVALID");
  let organizationId = audience === "followers" ? id(input.organizationId, "organizationId") : "";
  if (organizationId) {
    const organization = await database.doc(`organizations/${organizationId}`).get();
    if (organization.get("type") !== "community" || organization.get("status") !== "active") throw new HttpsError("invalid-argument", "COMMUNITY_INVALID");
  }
  const eventId = input.eventId ? id(input.eventId, "eventId") : "";
  if (eventId) {
    const event = await database.doc(`events/${eventId}`).get();
    if (event.get("status") !== "published" || (organizationId && event.get("organizationId") !== organizationId)) throw new HttpsError("invalid-argument", "EVENT_INVALID");
    organizationId = String(event.get("organizationId"));
  }
  return { kind: "announcement", title, body, audience, organizationId, eventId, startsAt: 0, actorUid: uid };
}

function recipientQuery(database: Firestore, job: NotificationJob) {
  if (job.audience === "followers") return database.collection(`organizations/${job.organizationId}/followers`);
  if (job.audience === "registrations") return database.collection(`events/${job.eventId}/registrations`).where("status", "==", "registered");
  return database.collection("users").where("mobileNotifications", "==", true);
}

export async function previewAnnouncementService(database: Firestore, uid: string, input: Record<string, unknown>) {
  const job = await validateAnnouncement(database, uid, input);
  const count = await recipientQuery(database, job).count().get();
  return { audienceCount: count.data().count, enabled: notificationsEnabled() };
}

export async function sendAnnouncementService(database: Firestore, uid: string, input: Record<string, unknown>) {
  const job = await validateAnnouncement(database, uid, input);
  if (!notificationsEnabled()) throw new HttpsError("failed-precondition", "NOTIFICATIONS_DISABLED");
  const requestId = id(input.requestId, "requestId");
  const test = input.test === true;
  const jobId = `announcement_${hash(`${uid}:${requestId}`)}`;
  const fingerprint = hash(JSON.stringify({ ...job, test }));
  await database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, uid, ["good4Admin"]);
    const current = await transaction.get(database.doc(`notificationJobs/${jobId}`));
    if (current.exists) {
      if (current.get("fingerprint") !== fingerprint) throw new HttpsError("already-exists", "REQUEST_ID_REUSED");
      return;
    }
    const quotaRef = database.doc(`notificationAnnouncementQuotas/${new Date().toISOString().slice(0, 10)}`);
    const quota = await transaction.get(quotaRef);
    const waiting = !test && (quota.get("count") ?? 0) >= 1;
    enqueueNotification(database, transaction, jobId, { ...job, ...(test ? { audience: "self", test: true } : {}) });
    if (waiting) transaction.update(database.doc(`notificationJobs/${jobId}`), { status: "waiting", waitReason: "announcementQuota" });
    else if (!test) transaction.set(quotaRef, { count: FieldValue.increment(1), updatedAt: FieldValue.serverTimestamp() }, { merge: true });
    transaction.update(database.doc(`notificationJobs/${jobId}`), { fingerprint, announcementQuotaReserved: test || !waiting });
    transaction.create(database.collection("auditLogs").doc(), { action: "notification.queued", actorUid: uid,
      targetType: "notification", targetId: jobId, metadata: { audience: job.audience, test }, createdAt: FieldValue.serverTimestamp() });
  });
  return { jobId };
}

export async function notificationHistoryService(database: Firestore, uid: string) {
  await requireGood4Admin(database, uid);
  const jobs = await database.collection("notificationJobs").orderBy("createdAt", "desc").limit(50).get();
  return { jobs: jobs.docs.map((doc) => ({ id: doc.id, title: doc.get("title"), kind: doc.get("kind"),
    audience: doc.get("audience"), actorUid: doc.get("actorUid"), status: doc.get("status"), waitReason: doc.get("waitReason") ?? null,
    createdAt: (doc.get("createdAt") as Timestamp | undefined)?.toDate().toISOString() ?? null,
    accepted: doc.get("accepted") ?? 0, failed: doc.get("failed") ?? 0, users: doc.get("users") ?? 0, skipped: doc.get("skipped") ?? 0 })) };
}

export async function prepareEventReminders(database: Firestore, now = Timestamp.now()) {
  if (!notificationsEnabled()) return;
  // Only events enrolled by a post-rollout save are considered; old events are never backfilled.
  const events = await database.collection("events").where("notificationReminderAt", "<=", now).limit(200).get();
  for (const event of events.docs) {
    await database.runTransaction(async (transaction) => {
      const current = await transaction.get(event.ref);
      const start = current.get("startsAt") as Timestamp | undefined;
      if (!current.exists || current.get("status") !== "published" || !start || start.toMillis() <= now.toMillis()) {
        transaction.update(event.ref, { notificationReminderAt: FieldValue.delete() });
        return;
      }
      if (!current.get("notificationReminderAt")) return;
      const jobId = `reminder_${event.id}_${start.seconds}`;
      const job = await transaction.get(database.doc(`notificationJobs/${jobId}`));
      if (!job.exists) enqueueNotification(database, transaction, jobId, {
        kind: "eventReminder", title: "Etkinliğin yaklaşıyor", body: `${current.get("title")} yaklaşık 1 saat sonra başlıyor.`,
        organizationId: current.get("organizationId"), eventId: event.id, startsAt: start.seconds,
        audience: "registrations", actorUid: "system",
      });
      transaction.update(event.ref, { notificationReminderAt: FieldValue.delete() });
    });
  }
}

export type PushSender = (token: string, payload: { title: string; body: string; data: Record<string, string>; kind: NotificationKind }) => Promise<void>;

async function eligible(database: Firestore, uid: string, job: NotificationJob) {
  const user = await database.doc(`users/${uid}`).get();
  if (user.get("status") !== "active") return false;
  if (job.audience === "followers") {
    if (!(await database.doc(`organizations/${job.organizationId}/followers/${uid}`).get()).exists) return false;
    if ((await database.doc(`organizations/${job.organizationId}`).get()).get("status") !== "active") return false;
  }
  if (job.kind !== "announcement") {
    const event = await database.doc(`events/${job.eventId}`).get();
    if (!event.exists) return false;
    if (job.kind === "eventCancelled") { if (event.get("status") !== "cancelled") return false; }
    else if (event.get("status") !== "published") return false;
    if (job.kind === "eventReminder" && ((event.get("startsAt") as Timestamp)?.seconds !== job.startsAt || job.startsAt <= Date.now() / 1000)) return false;
  }
  if (job.audience === "registrations") {
    const registrations = await database.collection(`events/${job.eventId}/registrations`).where("userId", "==", uid).where("status", "==", "registered").get();
    if (registrations.empty) return false;
    if (job.kind === "eventReminder") {
      const checkins = await database.getAll(...registrations.docs.map((registration) => database.doc(`events/${job.eventId}/checkins/${registration.id}`)));
      if (checkins.some((checkin) => checkin.exists)) return false;
    }
  }
  return true;
}

async function deliverToUser(database: Firestore, jobId: string, uid: string, job: NotificationJob, sender: PushSender) {
  if (!(await eligible(database, uid, job))) return;
  const delivery = database.doc(`notificationJobs/${jobId}/deliveries/${uid}`);
  if ((await delivery.get()).get("done") === true) return;
  const inbox = database.doc(`users/${uid}/notifications/${jobId}`);
  const canDeliver = await database.runTransaction(async (transaction) => {
    const user = await transaction.get(database.doc(`users/${uid}`));
    const existing = await transaction.get(inbox);
    if (user.get("status") !== "active") return false;
    transaction.set(delivery, { uid }, { merge: true });
    if (!existing.exists) transaction.create(inbox, { kind: job.kind, title: job.title, body: job.body,
      organizationId: job.organizationId, eventId: job.eventId, createdAt: Math.floor(Date.now() / 1000), readAt: 0 });
    return true;
  });
  if (!canDeliver) return;
  const preferences = await database.doc(`users/${uid}/notificationPreferences/default`).get();
  const category = job.kind === "announcement" ? "announcements" : job.kind === "eventReminder" ? "reminders" : "eventUpdates";
  const allowed = job.test || (preferences.get(category) ?? category !== "announcements");
  const devices = await database.collection("pushDevices").where("uid", "==", uid).limit(5).get();
  for (const device of devices.docs) {
    if (!allowed || device.get("permission") !== true || !device.get("token") ||
      (job.organizationId && (device.get("blockedCommunityIds") ?? []).includes(job.organizationId))) continue;
    const receipt = delivery.collection("devices").doc(device.id);
    if ((await receipt.get()).exists) continue;
    // Recheck ownership immediately before sending (an installation may have changed accounts).
    const freshDevice = await device.ref.get();
    if (!(await eligible(database, uid, job))) continue;
    if (freshDevice.get("uid") !== uid || freshDevice.get("token") !== device.get("token") || freshDevice.get("permission") !== true) continue;
    if (job.organizationId && (freshDevice.get("blockedCommunityIds") ?? []).includes(job.organizationId)) continue;
    let outcome: "accepted" | "failed" = "accepted";
    try {
      await sender(device.get("token"), { title: job.title, body: job.body, kind: job.kind,
        data: { notificationId: jobId, recipientUid: uid, eventId: job.eventId, organizationId: job.organizationId, kind: job.kind } });
    } catch (error) {
      const code = (error as { code?: string }).code;
      if (!["messaging/registration-token-not-registered", "messaging/invalid-registration-token"].includes(code ?? "")) throw error;
      outcome = "failed";
      await database.runTransaction(async (transaction) => {
        const current = await transaction.get(device.ref);
        if (current.get("uid") === uid && current.get("token") === device.get("token")) transaction.update(device.ref, { token: "", permission: false });
      });
    }
    await database.runTransaction(async (transaction) => {
      const current = await transaction.get(receipt);
      if (current.exists) return;
      transaction.create(receipt, { outcome });

    });
  }
  await database.runTransaction(async (transaction) => {
    const current = await transaction.get(delivery);
    if (current.get("done") === true) return;
    transaction.set(delivery, { uid, done: true });

  });
}

/** A reservation survives retries. Counters and the next dispatch commit once per page. */
export async function processNotificationPage(database: Firestore, jobId: string, sender: PushSender): Promise<boolean> {
  const safeJobId = requireNonEmptyString(jobId, "jobId", 256);
  if (!/^[A-Za-z0-9_-]+$/.test(safeJobId)) throw new HttpsError("invalid-argument", "JOB_ID_INVALID");
  const ref = database.doc(`notificationJobs/${safeJobId}`);
  const document = await ref.get();
  if (!document.exists || ["complete", "cancelled", "expired"].includes(document.get("status"))) return false;
  if (!notificationsEnabled()) { await ref.update({ status: "cancelled", completedAt: Timestamp.now() }); return false; }
  if (((document.get("expiresAt") as Timestamp | undefined)?.toMillis() ?? Infinity) < Date.now()) {
    await ref.update({ status: "expired", completedAt: Timestamp.now() }); return false;
  }
  if (!(await optionalJobsAllowed(database))) {
    await ref.update({ status: "waiting", waitReason: document.get("waitReason") === "announcementQuota" ? "announcementQuota" : "budget" });
    return false;
  }
  if (document.get("announcementQuotaReserved") === false || document.get("waitReason") === "announcementQuota") {
    const allowed = await database.runTransaction(async (transaction) => {
      const quotaRef = database.doc(`notificationAnnouncementQuotas/${new Date().toISOString().slice(0, 10)}`);
      const [quota, current] = await Promise.all([transaction.get(quotaRef), transaction.get(ref)]);
      if (current.get("announcementQuotaReserved") !== false && current.get("waitReason") !== "announcementQuota") return true;
      if ((quota.get("count") ?? 0) >= 1) {
        transaction.update(ref, { status: "waiting", waitReason: "announcementQuota" });
        return false;
      }
      transaction.set(quotaRef, { count: FieldValue.increment(1), updatedAt: FieldValue.serverTimestamp() }, { merge: true });
      transaction.update(ref, { announcementQuotaReserved: true, waitReason: FieldValue.delete() }); return true;
    });
    if (!allowed) return false;
  }
  const job = document.data() as NotificationJob;
  const cursor = String(document.get("cursor") ?? "");
  const pageKey = hash(`${jobId}:${cursor || "first"}`);
  const pageRef = ref.collection("pages").doc(pageKey);
  let query = recipientQuery(database, job).orderBy(FieldPath.documentId()).limit(50);
  if (cursor) query = query.startAfter(cursor);
  const page = job.audience === "self" ? null : await query.get();
  const ids = job.audience === "self" ? [job.actorUid] : page!.docs.map((recipient) => job.audience === "all" ? recipient.id : recipient.get("userId"))
    .filter((uid): uid is string => typeof uid === "string" && /^[A-Za-z0-9_-]{1,128}$/.test(uid));
  const dayRef = database.doc(`notificationQuotas/${new Date().toISOString().slice(0, 10)}`);
  const reservation = await database.runTransaction(async (transaction) => {
    const [current, savedPage, quota] = await Promise.all([transaction.get(ref), transaction.get(pageRef), transaction.get(dayRef)]);
    if (savedPage.exists) return savedPage.get("done") ? null : savedPage.data();
    if (String(current.get("cursor") ?? "") !== cursor || ["complete", "cancelled", "expired"].includes(current.get("status"))) return null;
    if ((current.get("reservedUsers") ?? 0) + ids.length > 1000 || (quota.get("recipients") ?? 0) + ids.length > 2000) {
      transaction.update(ref, { status: "waiting", waitReason: (current.get("reservedUsers") ?? 0) + ids.length > 1000 ? "jobQuota" : "dailyQuota" });
      return null;
    }
    const saved = { uids: ids, nextCursor: page?.docs.at(-1)?.id ?? cursor, more: page?.size === 50, done: false };
    transaction.create(pageRef, saved);
    transaction.set(dayRef, { recipients: FieldValue.increment(ids.length), updatedAt: FieldValue.serverTimestamp() }, { merge: true });
    transaction.update(ref, { status: "processing", reservedUsers: FieldValue.increment(ids.length), waitReason: FieldValue.delete() });
    return saved;
  });
  if (!reservation) return false;
  let accepted = 0, failed = 0, users = 0;
  for (const uid of reservation.uids as string[]) {
    await deliverToUser(database, jobId, uid, job, sender);
    const delivery = ref.collection("deliveries").doc(uid);
    if ((await delivery.get()).get("done")) users++;
    const receipts = await delivery.collection("devices").get();
    for (const receipt of receipts.docs) receipt.get("outcome") === "accepted" ? accepted++ : failed++;
  }
  return database.runTransaction(async (transaction) => {
    const [savedPage, current] = await Promise.all([transaction.get(pageRef), transaction.get(ref)]);
    if (savedPage.get("done") || String(current.get("cursor") ?? "") !== cursor) return false;
    transaction.update(pageRef, { done: true });
    transaction.update(ref, { cursor: reservation.nextCursor, status: reservation.more ? "queued" : "complete",
      accepted: FieldValue.increment(accepted), failed: FieldValue.increment(failed), users: FieldValue.increment(users),
      ...(!reservation.more ? { completedAt: Timestamp.now() } : {}) });
    if (reservation.more) transaction.create(database.doc(`notificationDispatches/${hash(`${jobId}:${reservation.nextCursor}`)}`), {
      jobId, cursor: reservation.nextCursor, createdAt: FieldValue.serverTimestamp(),
    });
    return Boolean(reservation.more);
  });
}

export async function resumeNotificationJobService(database: Firestore, uid: string, input: Record<string, unknown>) {
  await requireGood4Admin(database, uid);
  const jobId = id(input.jobId, "jobId", 256);
  if (!(await optionalJobsAllowed(database))) throw new HttpsError("failed-precondition", "OPTIONAL_JOBS_PAUSED");
  await database.runTransaction(async (transaction) => {
    const ref = database.doc(`notificationJobs/${jobId}`);
    const job = await transaction.get(ref);
    if (job.get("status") !== "waiting") return;
    if (job.get("waitReason") === "jobQuota") throw new HttpsError("resource-exhausted", "NOTIFICATION_JOB_RECIPIENT_LIMIT");
    transaction.update(ref, { status: "queued" });
    transaction.create(database.collection("notificationDispatches").doc(), { jobId, cursor: job.get("cursor") ?? "", createdAt: FieldValue.serverTimestamp() });
  });
  return { resumed: true };
}
