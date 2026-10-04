import assert from "node:assert/strict";
import { after, beforeEach, test } from "node:test";
import { FieldPath, Timestamp } from "firebase-admin/firestore";
import { db, legacyTestDb } from "./firebase.js";
import { cancelEventService, saveEventService } from "./events.js";
import { enqueueNotification, markNotificationsReadService, prepareEventReminders, processNotificationPage,
  registerPushDeviceService, sendAnnouncementService, unregisterPushDeviceService, updateNotificationPreferencesService,
  type NotificationJob } from "./notifications.js";

beforeEach(async () => {
  process.env.NOTIFICATIONS_ENABLED = "true";
  for (const collection of ["users", "organizations", "events", "auditLogs", "notificationJobs", "notificationDispatches", "notificationQuotas", "notificationAnnouncementQuotas", "system", "rateLimits", "pushDevices"]) await db.recursiveDelete(db.collection(collection));
  await Promise.all([
    db.doc("users/admin").set({ role: "good4Admin", status: "active", mobileNotifications: true }),
    db.doc("users/student").set({ role: "student", status: "active", mobileNotifications: true }),
    db.doc("users/other").set({ role: "student", status: "active", mobileNotifications: true }),
    db.doc("users/manager").set({ role: "communityManager", status: "active" }),
    db.doc("organizations/community").set({ type: "community", status: "active" }),
    db.doc("organizations/community/members/manager").set({ role: "manager", status: "active" }),
    db.doc("organizations/community/followers/student").set({ userId: "student" }),
  ]);
});
after(async () => { await Promise.all([db.terminate(), legacyTestDb.terminate()]); });
const device = { installationId: "phone", secret: "a".repeat(64), token: "token", permission: true, platform: "android", blockedCommunityIds: [] };
const announcement = { title: "Duyuru", body: "İçerik", audience: "all", requestId: "request-1" };
async function makeJob(jobId: string, overrides: Partial<NotificationJob> = {}) {
  await db.runTransaction(async (transaction) => enqueueNotification(db, transaction, jobId, {
    kind: "announcement", title: "Duyuru", body: "İçerik", audience: "all", actorUid: "admin", organizationId: "", eventId: "", startsAt: 0, ...overrides,
  }));
}

test("device ownership, account switching and logout are authenticated", async () => {
  await registerPushDeviceService(db, "student", device);
  await assert.rejects(registerPushDeviceService(db, "other", { ...device, secret: "b".repeat(64) }), /INSTALLATION_OWNERSHIP_REQUIRED/);
  await registerPushDeviceService(db, "other", device);
  assert.equal((await db.doc("pushDevices/phone").get()).get("uid"), "other");
  await assert.rejects(unregisterPushDeviceService(db, "student", device), /INSTALLATION_OWNERSHIP_REQUIRED/);
  await unregisterPushDeviceService(db, "other", device);
  assert.equal((await db.doc("pushDevices/phone").get()).exists, false);
  await db.doc("users/student").update({ status: "deleting" });
  await assert.rejects(registerPushDeviceService(db, "student", device), /ACCOUNT_NOT_ACTIVE/);
});

test("only admin can announce; repeated requests do not create duplicate jobs", async () => {
  await assert.rejects(sendAnnouncementService(db, "student", announcement));
  const first = await sendAnnouncementService(db, "admin", announcement);
  const second = await sendAnnouncementService(db, "admin", announcement);
  assert.equal(first.jobId, second.jobId);
  assert.equal((await db.collection("notificationJobs").get()).size, 1);
  await assert.rejects(sendAnnouncementService(db, "admin", { ...announcement, title: "Changed" }), /REQUEST_ID_REUSED/);
  process.env.NOTIFICATIONS_ENABLED = "false";
  await assert.rejects(sendAnnouncementService(db, "admin", { ...announcement, requestId: "new" }), /NOTIFICATIONS_DISABLED/);
});

test("preferences default announcements off, permission denied preserves inbox, and receipt retries are idempotent", async () => {
  await registerPushDeviceService(db, "student", device);
  const job = await sendAnnouncementService(db, "admin", announcement);
  const sent: string[] = [];
  const sender = async (token: string) => { sent.push(token); };
  await processNotificationPage(db, job.jobId, sender);
  assert.equal(sent.length, 0);
  assert.equal((await db.collection("users/student/notifications").get()).size, 1);
  await updateNotificationPreferencesService(db, "student", { eventUpdates: true, reminders: true, announcements: true });
  await makeJob("next");
  const next = { jobId: "next" };
  await processNotificationPage(db, next.jobId, sender);
  await processNotificationPage(db, next.jobId, sender);
  assert.deepEqual(sent, ["token"]);
  await registerPushDeviceService(db, "student", { ...device, permission: false });
  await makeJob("denied");
  const denied = { jobId: "denied" };
  await processNotificationPage(db, denied.jobId, sender);
  assert.equal(sent.length, 1);
  assert.equal((await db.collection("users/student/notifications").get()).size, 3);
});

test("follow removal and device-specific blocks are respected", async () => {
  await registerPushDeviceService(db, "student", { ...device, blockedCommunityIds: ["community"] });
  await updateNotificationPreferencesService(db, "student", { eventUpdates: true, reminders: true, announcements: true });
  await makeJob("blocked", { audience: "followers", organizationId: "community" });
  let sent = 0;
  await processNotificationPage(db, "blocked", async () => { sent++; });
  assert.equal(sent, 0);
  await makeJob("unfollowed", { audience: "followers", organizationId: "community" });
  await db.doc("organizations/community/followers/student").delete();
  await processNotificationPage(db, "unfollowed", async () => { sent++; });
  assert.equal((await db.doc("users/student/notifications/unfollowed").get()).exists, false);
});

test("event writes produce only publication, time/place change and cancellation jobs", async () => {
  const date = new Date(Date.now() + 7 * 86400000).toISOString().slice(0, 10);
  const input = { title: "Etkinlik", description: "Açıklama", date, time: "18:00", location: "Kampüs", status: "draft" };
  const created = await saveEventService(db, "manager", "community", input);
  assert.equal((await db.collection("notificationJobs").get()).size, 0);
  await saveEventService(db, "manager", "community", { ...input, eventId: created.eventId, status: "published" });
  assert.equal((await db.collection("notificationJobs").get()).size, 1);
  await saveEventService(db, "manager", "community", { ...input, eventId: created.eventId, title: "Yeni başlık", status: "published" });
  assert.equal((await db.collection("notificationJobs").get()).size, 1);
  await saveEventService(db, "manager", "community", { ...input, eventId: created.eventId, time: "19:00", status: "published" });
  assert.equal((await db.collection("notificationJobs").get()).size, 2);
  await cancelEventService(db, "manager", "community", created.eventId);
  await cancelEventService(db, "manager", "community", created.eventId);
  assert.equal((await db.collection("notificationJobs").get()).size, 3);
});

test("reminders recheck registration, current start and check-in, and never backfill old events", async () => {
  const start = Timestamp.fromMillis(Date.now() + 3590000);
  await db.doc("events/event").set({ organizationId: "community", title: "Etkinlik", status: "published", startsAt: start, notificationReminderAt: Timestamp.fromMillis(Date.now() - 1000) });
  await db.doc("events/old").set({ status: "published", startsAt: start });
  await db.doc("events/event/registrations/r1").set({ userId: "student", status: "registered" });
  await db.doc("events/event/registrations/r2").set({ userId: "other", status: "registered" });
  await db.doc("events/event/checkins/r2").set({ userId: "other" });
  await registerPushDeviceService(db, "student", device);
  await prepareEventReminders(db);
  await prepareEventReminders(db);
  const jobs = await db.collection("notificationJobs").get();
  assert.equal(jobs.size, 1);
  let sent = 0;
  await processNotificationPage(db, jobs.docs[0]!.id, async () => { sent++; });
  assert.equal(sent, 1);
  assert.equal((await db.collection("users/other/notifications").get()).size, 0);
  await makeJob("stale", { kind: "eventReminder", audience: "registrations", eventId: "event", organizationId: "community", startsAt: start.seconds - 10 });
  await processNotificationPage(db, "stale", async () => { sent++; });
  assert.equal(sent, 1);
  await makeJob("unregistered", { kind: "eventReminder", audience: "registrations", eventId: "event", organizationId: "community", startsAt: start.seconds });
  await db.doc("events/event/registrations/r1").delete();
  await processNotificationPage(db, "unregistered", async () => { sent++; });
  assert.equal(sent, 1);
});

test("invalid endpoints are retired; other devices remain usable and read flags are per account", async () => {
  await registerPushDeviceService(db, "student", device);
  await registerPushDeviceService(db, "student", { ...device, installationId: "tablet", token: "tablet-token" });
  await makeJob("multi", { audience: "self", actorUid: "student", test: true });
  await processNotificationPage(db, "multi", async (token) => {
    if (token === "token") throw Object.assign(new Error("invalid"), { code: "messaging/registration-token-not-registered" });
  });
  assert.equal((await db.doc("pushDevices/phone").get()).get("token"), "");
  assert.equal((await db.doc("notificationJobs/multi").get()).get("accepted"), 1);
  assert.equal((await db.doc("notificationJobs/multi").get()).get("failed"), 1);
  await markNotificationsReadService(db, "other", { notificationId: "multi" });
  assert.equal((await db.doc("users/student/notifications/multi").get()).get("readAt"), 0);
  await markNotificationsReadService(db, "student", { notificationId: "multi" });
  assert.ok((await db.doc("users/student/notifications/multi").get()).get("readAt") > 0);
});

test("paged audiences and transient retries preserve inbox and completed endpoint receipts", async () => {
  const batch = db.batch();
  for (let i = 0; i < 55; i++) batch.set(db.doc(`users/recipient-${String(i).padStart(2, "0")}`), { status: "active", role: "student", mobileNotifications: true });
  await batch.commit();
  await makeJob("paged");
  assert.equal(await processNotificationPage(db, "paged", async () => {}), true);
  assert.equal(await processNotificationPage(db, "paged", async () => {}), false);
  assert.equal((await db.doc("notificationJobs/paged").get()).get("users"), 58);
  await registerPushDeviceService(db, "student", device);
  await registerPushDeviceService(db, "student", { ...device, installationId: "tablet", token: "tablet" });
  await makeJob("retry", { audience: "self", actorUid: "student", test: true });
  const sent: string[] = [];
  await assert.rejects(processNotificationPage(db, "retry", async (token) => { if (token === "tablet") throw new Error("temporary"); sent.push(token); }), /temporary/);
  await processNotificationPage(db, "retry", async (token) => { sent.push(token); });
  assert.deepEqual(sent, ["token", "tablet"]);
  assert.equal((await db.collection("users/student/notifications").get()).docs.filter((doc) => doc.id === "retry").length, 1);
});

test("admin preview targets mobile accounts and self-test never broadcasts", async () => {
  await registerPushDeviceService(db, "admin", { ...device, installationId: "admin-phone", token: "admin-token" });
  await registerPushDeviceService(db, "admin", { ...device, installationId: "admin-tablet", token: "admin-tablet-token" });
  await registerPushDeviceService(db, "student", device);
  await registerPushDeviceService(db, "other", { ...device, installationId: "other-phone", token: "other-token" });
  for (const uid of ["student", "other"]) {
    await updateNotificationPreferencesService(db, uid, { eventUpdates: true, reminders: true, announcements: true });
  }
  const job = await sendAnnouncementService(db, "admin", { ...announcement, test: true });
  assert.equal((await db.doc(`notificationJobs/${job.jobId}`).get()).get("audience"), "self");
  const sent: string[] = [];
  await processNotificationPage(db, job.jobId, async (token) => { sent.push(token); });
  assert.deepEqual(sent.sort(), ["admin-tablet-token", "admin-token"].sort());
  assert.equal((await db.collection("users/student/notifications").get()).size, 0);
  assert.equal((await db.collection("users/other/notifications").get()).size, 0);
  assert.equal((await db.collection("users/admin/notifications").get()).size, 1);
});

test("daily recipient quota waits without inbox writes; resumed retries reserve only once", async () => {
  const quota = db.doc(`notificationQuotas/${new Date().toISOString().slice(0, 10)}`);
  await quota.set({ recipients: 1999 });
  await makeJob("quota-limited");
  await processNotificationPage(db, "quota-limited", async () => assert.fail("No sends while quota exceeded"));
  assert.equal((await db.doc("notificationJobs/quota-limited").get()).get("waitReason"), "dailyQuota");
  assert.equal((await db.doc("users/student/notifications/quota-limited").get()).exists, false);
  await quota.set({ recipients: 0 });
  await processNotificationPage(db, "quota-limited", async () => {});
  await processNotificationPage(db, "quota-limited", async () => assert.fail("Completed job cannot resend"));
  assert.equal((await quota.get()).get("recipients"), 3);
  assert.equal((await db.doc("notificationJobs/quota-limited").get()).get("reservedUsers"), 3);
});

test("job recipient cap, expired jobs and budget pause prevent distribution", async () => {
  await makeJob("job-cap");
  await db.doc("notificationJobs/job-cap").update({ reservedUsers: 999 });
  await processNotificationPage(db, "job-cap", async () => assert.fail("Capped job cannot send"));
  assert.equal((await db.doc("notificationJobs/job-cap").get()).get("waitReason"), "jobQuota");
  await makeJob("expired");
  await db.doc("notificationJobs/expired").update({ expiresAt: Timestamp.fromMillis(Date.now() - 1000) });
  await processNotificationPage(db, "expired", async () => assert.fail("Expired job cannot send"));
  assert.equal((await db.doc("notificationJobs/expired").get()).get("status"), "expired");
  await makeJob("budget-paused");
  await db.doc("system/cost_control").set({ budgetMonth: new Date().toISOString().slice(0, 7), budgetPaused: true });
  await processNotificationPage(db, "budget-paused", async () => assert.fail("Budget paused"));
  assert.equal((await db.doc("notificationJobs/budget-paused").get()).get("waitReason"), "budget");
  await db.doc("system/cost_control").set({ budgetMonth: "2000-01", budgetPaused: true });
  await processNotificationPage(db, "budget-paused", async () => {});
  assert.equal((await db.doc("notificationJobs/budget-paused").get()).get("status"), "complete");
});

test("a second bulk announcement waits instead of consuming another daily allowance", async () => {
  await sendAnnouncementService(db, "admin", announcement);
  const second = await sendAnnouncementService(db, "admin", { ...announcement, requestId: "second" });
  await processNotificationPage(db, second.jobId, async () => assert.fail("Second announcement cannot send"));
  assert.equal((await db.doc(`notificationJobs/${second.jobId}`).get()).get("waitReason"), "announcementQuota");
});

test("mark-read uses captured visible IDs beyond 100 unread notifications and leaves later arrivals unread", async () => {
  const batch = db.batch();
  const visible: string[] = [];
  for (let index = 0; index < 150; index++) {
    const notificationId = `notification-${String(index).padStart(3, "0")}`;
    batch.set(db.doc(`users/student/notifications/${notificationId}`), { createdAt: index, readAt: 0 });
    if (index >= 50) visible.push(notificationId);
  }
  await batch.commit();
  await db.doc("users/student/notifications/later").set({ createdAt: 151, readAt: 0 });
  await db.doc("users/other/notifications/notification-149").set({ createdAt: 149, readAt: 0 });
  await markNotificationsReadService(db, "student", { notificationIds: visible });
  const notifications = await db.collection("users/student/notifications").get();
  assert.equal(notifications.docs.filter((document) => document.get("readAt") > 0).length, 100);
  assert.equal((await db.doc("users/student/notifications/notification-000").get()).get("readAt"), 0);
  assert.equal((await db.doc("users/student/notifications/later").get()).get("readAt"), 0);
  assert.equal((await db.doc("users/other/notifications/notification-149").get()).get("readAt"), 0);
  await assert.rejects(markNotificationsReadService(db, "student", { notificationIds: [...visible, "later"] }), /NOTIFICATION_IDS_INVALID/);
  await markNotificationsReadService(db, "student", { all: true });
  assert.ok((await db.doc("users/student/notifications/later").get()).get("readAt") > 0);
  assert.equal((await db.doc("users/student/notifications/notification-000").get()).get("readAt"), 0);
});

test("a budget pause and resume cannot bypass an unreserved announcement quota", async () => {
  await sendAnnouncementService(db, "admin", announcement);
  const second = await sendAnnouncementService(db, "admin", { ...announcement, requestId: "budget-second" });
  const jobRef = db.doc(`notificationJobs/${second.jobId}`);
  await db.doc("system/cost_control").set({ manualPaused: true });
  await processNotificationPage(db, second.jobId, async () => assert.fail("Paused job cannot send"));
  assert.equal((await jobRef.get()).get("announcementQuotaReserved"), false);
  await db.doc("system/cost_control").set({ manualPaused: false });
  // Model a resume with a different wait reason; quota ownership is independent of it.
  await jobRef.update({ status: "queued", waitReason: "budget" });
  await processNotificationPage(db, second.jobId, async () => assert.fail("Second announcement cannot send"));
  assert.equal((await jobRef.get()).get("waitReason"), "announcementQuota");
  assert.equal((await db.doc(`notificationAnnouncementQuotas/${new Date().toISOString().slice(0, 10)}`).get()).get("count"), 1);
  assert.equal((await db.doc(`users/student/notifications/${second.jobId}`).get()).exists, false);
  const quotaRef = db.doc(`notificationAnnouncementQuotas/${new Date().toISOString().slice(0, 10)}`);
  await quotaRef.set({ count: 0 });
  await processNotificationPage(db, second.jobId, async () => {});
  assert.equal((await jobRef.get()).get("announcementQuotaReserved"), true);
  assert.equal((await quotaRef.get()).get("count"), 1);
  await processNotificationPage(db, second.jobId, async () => assert.fail("Completed job cannot resend"));
  assert.equal((await quotaRef.get()).get("count"), 1);
});


test("numeric inbox cursors retain equal-time notifications even after the cursor document is deleted", async () => {
  const inbox = db.collection("users/student/notifications");
  const batch = db.batch();
  for (let index = 0; index < 61; index++) batch.set(inbox.doc(`item-${String(index).padStart(3, "0")}`), {
    createdAt: Math.floor(index / 30), readAt: 0,
  });
  await batch.commit();
  const query = inbox.orderBy("createdAt", "desc").orderBy(FieldPath.documentId(), "desc").limit(20);
  const first = await query.get();
  assert.equal(first.size, 20);
  const boundary = first.docs.at(-1)!;
  const cursor = { value: boundary.get("createdAt"), id: boundary.id };
  await boundary.ref.delete();
  await inbox.doc("new-push").set({ createdAt: 100, readAt: 0 });
  const ids = first.docs.map((document) => document.id);
  let next = cursor;
  while (true) {
    const page = await query.startAfter(next.value, next.id).get();
    assert.ok(page.size <= 20);
    ids.push(...page.docs.map((document) => document.id));
    if (page.size < 20) break;
    const last = page.docs.at(-1)!;
    next = { value: last.get("createdAt"), id: last.id };
  }
  assert.equal(ids.length, 61);
  assert.equal(new Set(ids).size, 61);
  assert.equal(ids[0], "item-060");
  assert.equal(ids.at(-1), "item-000");
  assert.equal(ids.includes("new-push"), false);
});
