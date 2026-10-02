import assert from "node:assert/strict";
import { after, beforeEach, test } from "node:test";
import { Timestamp } from "firebase-admin/firestore";
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
  await registerPushDeviceService(db, "student", device);
  const job = await sendAnnouncementService(db, "admin", { ...announcement, test: true });
  const sent: string[] = [];
  await processNotificationPage(db, job.jobId, async (token) => { sent.push(token); });
  assert.deepEqual(sent, ["admin-token"]);
  assert.equal((await db.collection("users/student/notifications").get()).size, 0);
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
