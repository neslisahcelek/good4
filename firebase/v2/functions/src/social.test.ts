import assert from "node:assert/strict";
import { after, beforeEach, test } from "node:test";
import sharp from "sharp";
import { Timestamp } from "firebase-admin/firestore";
import { db, legacyTestDb } from "./firebase.js";
import { MARKET_LIMITS, sendMarketMessageService } from "./market.js";
import { openName, socialPhotoDownloadUrl } from "./socialProfile.js";
import {
  acceptSocialTermsService,
  blockSocialUserService,
  cancelSocialActivityService,
  cleanupSocialDataService,
  createSocialActivityService,
  eraseSocialData,
  getSocialActivityService,
  getSocialFeedService,
  getSocialMessagesService,
  getSocialReportConversationService,
  getSocialSummaryService,
  listMySocialActivitiesService,
  listSocialActivityRequestsService,
  listSocialConversationsService,
  listSocialModerationQueueService,
  reportSocialContentService,
  requestToJoinSocialActivityService,
  resolveSocialReportService,
  removeSocialReportedProfilePhotoService,
  respondToSocialRequestService,
  sendSocialMessageService,
  setSocialProfileService,
  SOCIAL_LIMITS,
  SOCIAL_RETENTION_DAYS,
  SOCIAL_TERMS_VERSION,
  type SocialDeps,
  unblockSocialUserService,
  withdrawSocialRequestService,
} from "./social.js";

const organizer = "organizer1";
const ali = "ali1";
const zeynep = "zeynep1";
const unverified = "unverified1";
const admin = "admin1";
const NOW = Date.UTC(2026, 9, 6, 9, 0, 0);
const HOUR = 60 * 60 * 1000;
const DAY = 24 * HOUR;

let notified: { uid: string; title: string; data?: Record<string, string> }[];
let deps: SocialDeps;

beforeEach(async () => {
  await Promise.all([
    "users", "socialActivities", "socialConversations", "socialUserState", "socialReports",
    "marketUserState", "marketViolations", "marketListings", "marketConversations", "auditLogs", "app_config",
  ].map((collection) => db.recursiveDelete(db.collection(collection))));
  notified = [];
  deps = {
    notify: async (uid, payload) => {
      notified.push({ uid, title: payload.title, data: payload.data });
    },
    now: () => NOW,
  };
  await Promise.all([
    db.doc("app_config/social_activities").set({ enabled: true }),
    student(organizer, "Ayşe Yılmaz", "ayse@ogr.akdeniz.edu.tr"),
    student(ali, "Ali Demir", "ali@ogr.akdeniz.edu.tr"),
    student(zeynep, "Zeynep Kara", "zeynep@ogr.akdeniz.edu.tr"),
    student(unverified, "Can Ak", null),
    db.doc(`users/${admin}`).set({ role: "good4Admin", status: "active", email: "admin@good4.test" }),
  ]);
});

after(async () => {
  await Promise.all([db.terminate(), legacyTestDb.terminate()]);
});

async function student(uid: string, displayName: string, eduEmail: string | null, terms = true) {
  await db.doc(`users/${uid}`).set({
    role: "student", status: "active", displayName,
    ...(eduEmail ? { eduEmail, eduVerified: true } : {}),
  });
  if (terms) await db.doc(`socialUserState/${uid}`).set({ termsVersion: SOCIAL_TERMS_VERSION });
}

async function rejectsWith(promise: Promise<unknown>, message: string) {
  await assert.rejects(promise, (error: Error) => {
    assert.equal(error.message, message);
    return true;
  });
}

function activityInput(overrides: Record<string, unknown> = {}) {
  return {
    kind: "sport",
    type: "tennis",
    title: "Akşam tenisi",
    note: "Raketim var, top getiriyorum.",
    startsAt: new Date(NOW + 2 * DAY).toISOString(),
    level: "beginner",
    capacity: 1,
    ...overrides,
  };
}

async function openActivity(overrides: Record<string, unknown> = {}, uid = organizer): Promise<string> {
  return (await createSocialActivityService(db, uid, activityInput(overrides), deps)).activityId;
}

async function acceptedChat(capacity = 2): Promise<{ activityId: string; conversationId: string }> {
  const activityId = await openActivity({ capacity });
  await requestToJoinSocialActivityService(db, ali, { activityId }, deps);
  const { conversationId } = await respondToSocialRequestService(db, organizer,
    { activityId, requesterUid: ali, accept: true }, deps);
  return { activityId, conversationId: conversationId! };
}

test("the feature stays closed until it is switched on, and needs a verified school address and the rules", async () => {
  await db.doc("app_config/social_activities").delete();
  await rejectsWith(createSocialActivityService(db, organizer, activityInput(), deps), "SOCIAL_DISABLED");
  await db.doc("app_config/social_activities").set({ enabled: true });

  await rejectsWith(createSocialActivityService(db, unverified, activityInput(), deps), "SOCIAL_EDU_REQUIRED");
  await student("noterms1", "Ece Su", "ece@ogr.akdeniz.edu.tr", false);
  await rejectsWith(createSocialActivityService(db, "noterms1", activityInput(), deps), "SOCIAL_TERMS_REQUIRED");
  await rejectsWith(acceptSocialTermsService(db, "noterms1", { version: 0 }), "SOCIAL_TERMS_VERSION_OUTDATED");
  await acceptSocialTermsService(db, "noterms1", { version: SOCIAL_TERMS_VERSION });
  await createSocialActivityService(db, "noterms1", activityInput(), deps);

  const summary = await getSocialSummaryService(db, unverified, deps);
  assert.equal(summary.me.eduVerified, false);
  assert.equal(summary.me.enabled, true);
});

test("a school address verified for Kampüs Dolabı also opens social activities", async () => {
  // campusEmailVerification writes these two fields; nothing social-specific is needed.
  await db.doc(`users/${unverified}`).update({ eduEmail: "can@ogr.akdeniz.edu.tr", eduVerified: true });
  await db.doc(`socialUserState/${unverified}`).set({ termsVersion: SOCIAL_TERMS_VERSION });
  await createSocialActivityService(db, unverified, activityInput(), deps);
  // Any other .edu.tr address (Askıda Yemek accepts these) does not.
  await db.doc(`users/${unverified}`).update({ eduEmail: "can@ege.edu.tr" });
  await rejectsWith(createSocialActivityService(db, unverified, activityInput(), deps), "SOCIAL_EDU_REQUIRED");
});

test("activity input is validated", async () => {
  const cases: [Record<string, unknown>, string][] = [
    [{ kind: "party" }, "SOCIAL_KIND_INVALID"],
    [{ type: "coffee" }, "SOCIAL_TYPE_INVALID"],
    [{ title: "x" }, "SOCIAL_TITLE_INVALID"],
    [{ startsAt: new Date(NOW + 5 * 60 * 1000).toISOString() }, "SOCIAL_STARTS_AT_INVALID"],
    [{ startsAt: new Date(NOW + 31 * DAY).toISOString() }, "SOCIAL_STARTS_AT_INVALID"],
    [{ startsAt: "yarın" }, "SOCIAL_STARTS_AT_INVALID"],
    [{ capacity: 0 }, "SOCIAL_CAPACITY_INVALID"],
    [{ capacity: SOCIAL_LIMITS.maxCapacity + 1 }, "SOCIAL_CAPACITY_INVALID"],
    [{ level: "pro" }, "SOCIAL_LEVEL_INVALID"],
    [{ kind: "social", type: "board-games", game: "poker" }, "SOCIAL_GAME_INVALID"],
    [{ kind: "social", type: "picnic" }, "SOCIAL_TYPE_INVALID"],
    [{ kind: "social", type: "volunteering" }, "SOCIAL_TYPE_INVALID"],
  ];
  for (const [overrides, code] of cases) {
    await rejectsWith(createSocialActivityService(db, organizer, activityInput(overrides), deps), code);
  }
  const socialId = await openActivity({ kind: "social", type: "coffee", level: "advanced", place: "olbia" });
  const social = await db.doc(`socialActivities/${socialId}`).get();
  assert.equal(social.get("level"), null, "social activities have no level");
  assert.equal(social.get("place"), undefined, "activities carry no meeting place");
  assert.equal(social.get("organizerName"), "A.. Y..");
  assert.equal(social.get("eduDomain"), "akdeniz.edu.tr");
  const okey = await db.doc(`socialActivities/${await openActivity({ kind: "social", type: "board-games", game: "okey" })}`).get();
  assert.equal(okey.get("game"), "okey");
  assert.equal(social.get("game"), null, "only board games name a game");
});

test("digital game choice is optional, accepts FIFA/PES and stays specific to the activity type", async () => {
  for (const game of [undefined, "fifa", "pes"]) {
    const id = await openActivity({ kind: "social", type: "video-games", game });
    const detail = await getSocialActivityService(db, ali, { activityId: id });
    assert.equal(detail.activity.game, game ?? null);
    assert.equal((await db.doc(`socialActivities/${id}`).get()).get("game"), game ?? null);
  }
  await rejectsWith(openActivity({ kind: "social", type: "video-games", game: "okey" }), "SOCIAL_GAME_INVALID");
  await rejectsWith(openActivity({ kind: "social", type: "video-games", game: "unknown" }), "SOCIAL_GAME_INVALID");
  await rejectsWith(openActivity({ kind: "social", type: "board-games", game: "fifa" }), "SOCIAL_GAME_INVALID");
});

test("banned words count as strikes shared with Kampüs Dolabı, and phone numbers stay out of public text", async () => {
  await rejectsWith(createSocialActivityService(db, organizer, activityInput({ title: "Karton sigara" }), deps),
    "MARKET_CONTENT_BLOCKED");
  const violation = (await db.collection("marketViolations").get()).docs[0]!;
  assert.equal(violation.get("context"), "socialActivity");
  await rejectsWith(createSocialActivityService(db, organizer,
    activityInput({ note: "0532 123 45 67 arayın" }), deps), "SOCIAL_PHONE_IN_ACTIVITY");

  // A suspension from Kampüs Dolabı holds here too.
  await db.doc(`marketUserState/${organizer}`).set({ suspendedUntil: Timestamp.fromMillis(NOW + HOUR) });
  await rejectsWith(createSocialActivityService(db, organizer, activityInput(), deps), "SOCIAL_SUSPENDED");
});

test("an organizer opens at most three activities a day and keeps at most five upcoming", async () => {
  for (let index = 0; index < SOCIAL_LIMITS.maxActivitiesPerDay; index += 1) await openActivity();
  await rejectsWith(createSocialActivityService(db, organizer, activityInput(), deps), "SOCIAL_DAILY_ACTIVITY_LIMIT");

  const tomorrow = { ...deps, now: () => NOW + DAY };
  await createSocialActivityService(db, organizer, activityInput(), tomorrow);
  await createSocialActivityService(db, organizer, activityInput(), tomorrow);
  await rejectsWith(createSocialActivityService(db, organizer, activityInput(), tomorrow), "SOCIAL_ACTIVE_ACTIVITY_LIMIT");
});

test("the feed lists upcoming open activities soonest first, by kind, without blocked organizers", async () => {
  const later = await openActivity({ startsAt: new Date(NOW + 3 * DAY).toISOString() });
  const sooner = await openActivity({ kind: "social", type: "coffee", startsAt: new Date(NOW + HOUR).toISOString() });
  const zeyneps = await openActivity({ title: "Zeynep'in maçı" }, zeynep);

  const feed = await getSocialFeedService(db, ali, {}, deps);
  assert.deepEqual(feed.activities.map((activity) => activity.id), [sooner, zeyneps, later]);
  assert.equal(feed.activities[0]!.spotsLeft, 1);
  assert.equal("note" in feed.activities[0]!, false, "the feed leaves the note to the detail screen");

  const sport = await getSocialFeedService(db, ali, { kind: "sport" }, deps);
  assert.ok(sport.activities.every((activity) => activity.kind === "sport"));

  await db.doc(`marketUserState/${ali}`).set({ blockedUids: [zeynep] });
  const filtered = await getSocialFeedService(db, ali, {}, deps);
  assert.equal(filtered.activities.length, 2);

  // Started activities drop out before the cleanup job runs.
  const afterStart = await getSocialFeedService(db, ali, {}, { now: () => NOW + 2 * HOUR });
  assert.ok(afterStart.activities.every((activity) => activity.id !== sooner));
  await rejectsWith(getSocialFeedService(db, ali, { after: "bozuk" }, deps), "SOCIAL_CURSOR_INVALID");
});

test("asking to join notifies the organizer once, and own, duplicate or blocked requests are refused", async () => {
  const activityId = await openActivity({ capacity: 3 });
  await rejectsWith(requestToJoinSocialActivityService(db, organizer, { activityId }, deps), "SOCIAL_OWN_ACTIVITY");
  await rejectsWith(requestToJoinSocialActivityService(db, unverified, { activityId }, deps), "SOCIAL_EDU_REQUIRED");

  await requestToJoinSocialActivityService(db, ali, { activityId, note: "Haftada iki oynuyorum." }, deps);
  assert.equal(notified.at(-1)?.uid, organizer);
  assert.equal(notified.at(-1)?.data?.type, "social_request");
  await rejectsWith(requestToJoinSocialActivityService(db, ali, { activityId }, deps), "SOCIAL_ALREADY_REQUESTED");

  const detail = await getSocialActivityService(db, ali, { activityId }, deps);
  assert.equal(detail.myRequest?.status, "pending");
  assert.equal(detail.activity.note, "Raketim var, top getiriyorum.");

  const requests = await listSocialActivityRequestsService(db, organizer, { activityId }, deps);
  assert.equal(requests.requests.length, 1);
  assert.equal(requests.requests[0]!.name, "A.. D..");
  assert.equal(requests.requests[0]!.note, "Haftada iki oynuyorum.");
  await rejectsWith(listSocialActivityRequestsService(db, ali, { activityId }, deps), "SOCIAL_ACTIVITY_NOT_FOUND");

  // A block placed in Kampüs Dolabı keeps the blocked student out here too.
  await db.doc(`marketUserState/${organizer}`).set({ blockedUids: [zeynep] }, { merge: true });
  await rejectsWith(requestToJoinSocialActivityService(db, zeynep, { activityId }, deps), "SOCIAL_BLOCKED");
});

test("a student can withdraw a waiting request and ask again", async () => {
  const activityId = await openActivity();
  await requestToJoinSocialActivityService(db, ali, { activityId }, deps);
  await withdrawSocialRequestService(db, ali, { activityId }, deps);
  assert.equal((await db.doc(`socialActivities/${activityId}`).get()).get("pendingCount"), 0);
  assert.equal((await getSocialActivityService(db, ali, { activityId }, deps)).myRequest?.status, "withdrawn");
  await requestToJoinSocialActivityService(db, ali, { activityId }, deps);
  assert.equal((await db.doc(`socialActivities/${activityId}`).get()).get("pendingCount"), 1);
});

test("accepting opens a one-to-one chat; filling the last place closes the other requests as 'Yer kalmadı'", async () => {
  const activityId = await openActivity({ capacity: 1 });
  await requestToJoinSocialActivityService(db, ali, { activityId }, deps);
  await requestToJoinSocialActivityService(db, zeynep, { activityId }, deps);
  notified = [];

  const accepted = await respondToSocialRequestService(db, organizer, { activityId, requesterUid: ali, accept: true }, deps);
  assert.equal(accepted.conversationId, `${activityId}_${ali}`);
  assert.deepEqual(notified.map((entry) => [entry.uid, entry.data?.type]), [[ali, "social_conversation"]]);

  const activity = await db.doc(`socialActivities/${activityId}`).get();
  assert.equal(activity.get("status"), "full");
  assert.equal(activity.get("acceptedCount"), 1);
  assert.equal(activity.get("pendingCount"), 0);
  assert.equal((await getSocialActivityService(db, zeynep, { activityId }, deps)).myRequest?.status, "closed");
  assert.equal((await getSocialFeedService(db, zeynep, {}, deps)).activities.length, 0, "full activities leave the feed");

  const thread = await getSocialMessagesService(db, ali, { conversationId: accepted.conversationId }, deps);
  assert.equal(thread.messages[0]!.type, "system");
  assert.equal(thread.conversation.otherName, "A.. Y..");
  assert.equal((await listSocialConversationsService(db, organizer, deps)).conversations.length, 1);
  await rejectsWith(getSocialMessagesService(db, zeynep, { conversationId: accepted.conversationId }, deps),
    "SOCIAL_NOT_PARTICIPANT");
});

test("declining sends nothing and reads as 'Yer kalmadı' to the student", async () => {
  const activityId = await openActivity({ capacity: 2 });
  await requestToJoinSocialActivityService(db, ali, { activityId }, deps);
  notified = [];
  const result = await respondToSocialRequestService(db, organizer, { activityId, requesterUid: ali, accept: false }, deps);
  assert.equal(result.status, "declined");
  assert.equal(notified.length, 0);
  assert.equal((await getSocialActivityService(db, ali, { activityId }, deps)).myRequest?.status, "closed");
  await rejectsWith(requestToJoinSocialActivityService(db, ali, { activityId }, deps), "SOCIAL_REQUEST_CLOSED");
  await rejectsWith(respondToSocialRequestService(db, ali, { activityId, requesterUid: ali, accept: true }, deps),
    "SOCIAL_ACTIVITY_NOT_FOUND");
});

test("a participant who leaves frees the place and the chat turns read-only", async () => {
  const { activityId, conversationId } = await acceptedChat(1);
  assert.equal((await db.doc(`socialActivities/${activityId}`).get()).get("status"), "full");
  await withdrawSocialRequestService(db, ali, { activityId }, deps);

  const activity = await db.doc(`socialActivities/${activityId}`).get();
  assert.equal(activity.get("status"), "open");
  assert.equal(activity.get("acceptedCount"), 0);
  assert.equal(notified.at(-1)?.uid, organizer);
  await rejectsWith(sendSocialMessageService(db, organizer, { conversationId, text: "Görüşürüz" }, deps),
    "SOCIAL_CONVERSATION_CLOSED");
  await rejectsWith(requestToJoinSocialActivityService(db, ali, { activityId }, deps), "SOCIAL_REQUEST_CLOSED");
});

test("chat messages notify the other side, count against the daily limit and close a week after the start", async () => {
  const { conversationId } = await acceptedChat();
  await sendSocialMessageService(db, ali, { conversationId, text: "Numaram 0532 123 45 67" }, deps);
  assert.equal(notified.at(-1)?.uid, organizer);
  assert.equal(notified.at(-1)?.data?.type, "social_message");
  assert.equal((await getSocialSummaryService(db, organizer, deps)).me.unreadCount, 1);
  await getSocialMessagesService(db, organizer, { conversationId }, deps);
  assert.equal((await getSocialSummaryService(db, organizer, deps)).me.unreadCount, 0);

  await db.doc(`socialUserState/${ali}`).set({ messagesToday: SOCIAL_LIMITS.maxMessagesPerDay }, { merge: true });
  await rejectsWith(sendSocialMessageService(db, ali, { conversationId, text: "Bir daha" }, deps),
    "SOCIAL_DAILY_MESSAGE_LIMIT");

  const weekAfter = { ...deps, now: () => NOW + 2 * DAY + SOCIAL_RETENTION_DAYS.chatWritable * DAY };
  await rejectsWith(sendSocialMessageService(db, organizer, { conversationId, text: "Selam" }, weekAfter),
    "SOCIAL_CONVERSATION_CLOSED");
  const list = await listSocialConversationsService(db, organizer, weekAfter);
  assert.equal(list.conversations[0]!.readOnly, true);
});

test("blocking from a chat is shared with Kampüs Dolabı and frees the blocked participant's place", async () => {
  const { activityId, conversationId } = await acceptedChat(1);
  await blockSocialUserService(db, organizer, { conversationId }, deps);

  const activity = await db.doc(`socialActivities/${activityId}`).get();
  assert.equal(activity.get("status"), "open");
  assert.equal(activity.get("acceptedCount"), 0);
  assert.equal((await getSocialActivityService(db, ali, { activityId }, deps)).myRequest?.status, "closed");
  await rejectsWith(sendSocialMessageService(db, ali, { conversationId, text: "?" }, deps), "SOCIAL_CONVERSATION_CLOSED");

  // The same block stops a new Kampüs Dolabı conversation between them.
  await db.doc("marketListings/listing1").set({
    sellerUid: organizer, status: "published", eduDomain: "akdeniz.edu.tr", title: "Kitap", price: 10,
  });
  await db.doc(`marketUserState/${ali}`).set({ termsVersion: 1 }, { merge: true });
  await assert.rejects(sendMarketMessageService(db, ali, { conversationId: `listing1_${ali}`, text: "Merhaba" },
    { photos: { save: async () => "", deletePrefix: async () => undefined }, notify: async () => undefined, now: () => NOW }),
  /MARKET_BLOCKED/);

  const unblocked = await unblockSocialUserService(db, organizer, { conversationId }, deps);
  assert.equal(unblocked.status, "closed", "a removed participant's chat does not reopen");
  assert.ok(MARKET_LIMITS.maxBlockedUsers > 0);
});

test("cancelling closes waiting requests and tells accepted participants in their chat", async () => {
  const { activityId, conversationId } = await acceptedChat(3);
  await requestToJoinSocialActivityService(db, zeynep, { activityId }, deps);
  notified = [];
  // An hour later, so the cancel note sorts after the acceptance note.
  await cancelSocialActivityService(db, organizer, { activityId }, { ...deps, now: () => NOW + HOUR });

  assert.deepEqual(notified.map((entry) => entry.uid), [ali]);
  assert.equal((await getSocialActivityService(db, zeynep, { activityId }, deps)).myRequest?.status, "closed");
  const thread = await getSocialMessagesService(db, ali, { conversationId }, deps);
  assert.equal(thread.conversation.readOnly, true);
  assert.equal(thread.messages.at(-1)?.text, "Etkinlik iptal edildi.");
  await rejectsWith(cancelSocialActivityService(db, organizer, { activityId }, deps), "SOCIAL_ACTIVITY_STATUS_INVALID");
});

test("'Etkinliklerim' lists what the student opened and asked for; the summary counts waiting requests", async () => {
  const mine = await openActivity({ capacity: 3 });
  const zeynepsActivity = await openActivity({ title: "Sahil turu", type: "cycling" }, zeynep);
  await requestToJoinSocialActivityService(db, ali, { activityId: mine }, deps);
  await requestToJoinSocialActivityService(db, organizer, { activityId: zeynepsActivity }, deps);
  await respondToSocialRequestService(db, zeynep, { activityId: zeynepsActivity, requesterUid: organizer, accept: true }, deps);

  const list = await listMySocialActivitiesService(db, organizer, deps);
  assert.deepEqual(list.organized.map((activity) => activity.id), [mine]);
  assert.equal(list.organized[0]!.pendingCount, 1);
  assert.equal(list.joined.length, 1);
  assert.equal(list.joined[0]!.status, "accepted");
  assert.equal(list.joined[0]!.conversationId, `${zeynepsActivity}_${organizer}`);
  assert.equal((await getSocialSummaryService(db, organizer, deps)).me.pendingRequestCount, 1);
});

test("reports reach the admins, who can read a reported chat and suspend in both features", async () => {
  const { conversationId } = await acceptedChat();
  notified = [];
  const { reportId } = await reportSocialContentService(db, ali,
    { targetType: "conversation", targetId: conversationId, reason: "harassment", note: "Rahatsız edici" }, deps);
  assert.equal(notified[0]?.uid, admin);
  await rejectsWith(reportSocialContentService(db, ali, { targetType: "conversation", targetId: conversationId,
    reason: "nope" }, deps), "SOCIAL_REPORT_REASON_INVALID");

  const queue = await listSocialModerationQueueService(db, admin);
  assert.equal(queue.reports.length, 1);
  await rejectsWith(listSocialModerationQueueService(db, ali), "ROLE_NOT_ALLOWED");
  const chat = await getSocialReportConversationService(db, admin, { reportId });
  assert.equal(chat.messages[0]!.type, "system");
  assert.equal((await db.collection("auditLogs").where("action", "==", "social.conversation.viewed").get()).size, 1);

  await resolveSocialReportService(db, admin, { reportId, action: "suspendUser", suspendDays: 3 }, deps);
  const suspendedUntil = (await db.doc(`marketUserState/${organizer}`).get()).get("suspendedUntil") as Timestamp;
  assert.equal(suspendedUntil.toMillis(), NOW + 3 * DAY);
  await rejectsWith(createSocialActivityService(db, organizer, activityInput(), deps), "SOCIAL_SUSPENDED");
});

test("an admin can take an activity down; it disappears for everyone but its waiting requests close", async () => {
  const activityId = await openActivity({ capacity: 2 });
  await requestToJoinSocialActivityService(db, zeynep, { activityId }, deps);
  const { reportId } = await reportSocialContentService(db, ali,
    { targetType: "activity", targetId: activityId, reason: "inappropriate" }, deps);
  await resolveSocialReportService(db, admin, { reportId, action: "removeActivity" }, deps);
  await rejectsWith(getSocialActivityService(db, ali, { activityId }, deps), "SOCIAL_ACTIVITY_NOT_FOUND");
  const request = await db.doc(`socialActivities/${activityId}/joinRequests/${zeynep}`).get();
  assert.equal(request.get("status"), "closed");
});

test("cleanup ends started activities and deletes activities and chats a month after they started", async () => {
  const { activityId, conversationId } = await acceptedChat(3);
  await requestToJoinSocialActivityService(db, zeynep, { activityId }, deps);

  const afterStart = NOW + 2 * DAY + HOUR;
  const first = await cleanupSocialDataService(db, afterStart);
  assert.equal(first.ended, 1);
  assert.equal((await db.doc(`socialActivities/${activityId}`).get()).get("status"), "ended");
  assert.equal((await db.doc(`socialActivities/${activityId}/joinRequests/${zeynep}`).get()).get("status"), "closed");

  await sendSocialMessageService(db, ali, { conversationId, text: "Teşekkürler!" }, { ...deps, now: () => afterStart });
  const later = NOW + 2 * DAY + SOCIAL_RETENTION_DAYS.activity * DAY + HOUR;
  const second = await cleanupSocialDataService(db, later);
  assert.equal(second.activities, 1);
  assert.equal(second.conversations, 1);
  assert.equal((await db.doc(`socialActivities/${activityId}`).get()).exists, false);
  assert.equal((await db.collection(`socialActivities/${activityId}/joinRequests`).get()).size, 0);
  assert.equal((await db.doc(`socialConversations/${conversationId}`).get()).exists, false);
  assert.equal((await db.collection(`socialConversations/${conversationId}/messages`).get()).size, 0);
  assert.equal((await db.doc(`socialUserState/${organizer}`).get()).get("unreadCount"), 0,
    "deleted chats leave no unread badge behind");
});

test("account deletion removes the student's social data and frees their places", async () => {
  const { activityId } = await acceptedChat(1);
  const own = await openActivity({ title: "Benim etkinliğim" }, ali);
  await requestToJoinSocialActivityService(db, zeynep, { activityId: own }, deps);
  await reportSocialContentService(db, ali, { targetType: "activity", targetId: activityId, reason: "spam" }, deps);

  const erased: string[] = [];
  await eraseSocialData(db, ali, async (prefix) => { erased.push(prefix); }, NOW);
  assert.deepEqual(erased, [`social-profiles/${ali}/`]);
  assert.equal((await db.doc(`socialActivities/${own}`).get()).exists, false);
  assert.equal((await db.collection(`socialActivities/${own}/joinRequests`).get()).size, 0);
  const joined = await db.doc(`socialActivities/${activityId}`).get();
  assert.equal(joined.get("acceptedCount"), 0);
  assert.equal(joined.get("status"), "open");
  assert.equal((await db.collection("socialConversations").get()).size, 0);
  assert.equal((await db.collection("socialReports").get()).size, 0);
  assert.equal((await db.doc(`socialUserState/${ali}`).get()).exists, false);
});

// ---------------------------------------------------------------------------
// Profile: name mode and photo
// ---------------------------------------------------------------------------

async function photoBase64(): Promise<string> {
  const image = await sharp({ create: { width: 900, height: 600, channels: 3, background: "#228b57" } }).jpeg().toBuffer();
  return image.toString("base64");
}

function photoDeps() {
  const saved: string[] = [];
  const deleted: string[] = [];
  return {
    saved, deleted,
    deps: {
      now: () => NOW,
      photos: {
        async save(objectName: string, bytes: Buffer) {
          const metadata = await sharp(bytes).metadata();
          assert.equal(metadata.exif, undefined, "photos must be stored without EXIF");
          assert.equal(metadata.width, metadata.height, "profile photos are square");
          saved.push(objectName);
          return `https://example.test/${objectName}`;
        },
        async deletePrefix(prefix: string) { deleted.push(prefix); },
      },
    },
  };
}

test("names are masked until the student shows theirs, and the choice applies to existing activities and chats", async () => {
  assert.equal(openName("Ayşe Yılmaz"), "Ayşe Y.");
  assert.equal(openName("Ayşe Nur Yılmaz"), "Ayşe Y.");
  assert.equal(openName("Zeynep"), "Zeynep");
  assert.equal(openName("ışık özdemir"), "ışık Ö.");

  const { activityId, conversationId } = await acceptedChat(2);
  assert.equal((await getSocialActivityService(db, ali, { activityId }, deps)).activity.organizerName, "A.. Y..");
  assert.equal((await listSocialConversationsService(db, ali, deps)).conversations[0]!.otherName, "A.. Y..");
  assert.equal((await getSocialSummaryService(db, organizer, deps)).me.nameMode, "masked");

  // Accepting the rules can carry the choice; a later change takes effect on what already exists.
  await acceptSocialTermsService(db, organizer, { version: SOCIAL_TERMS_VERSION, nameMode: "shown" }, deps);
  await rejectsWith(acceptSocialTermsService(db, organizer, { version: SOCIAL_TERMS_VERSION, nameMode: "all" }, deps),
    "SOCIAL_NAME_MODE_INVALID");
  const { deps: profileDeps } = photoDeps();
  assert.equal((await getSocialActivityService(db, ali, { activityId }, deps)).activity.organizerName, "Ayşe Y.");
  assert.equal((await getSocialMessagesService(db, ali, { conversationId }, deps)).conversation.otherName, "Ayşe Y.");
  const summary = await getSocialSummaryService(db, organizer, deps);
  assert.deepEqual([summary.me.nameMode, summary.me.shownName, summary.me.maskedName], ["shown", "Ayşe Y.", "A.. Y.."]);

  await setSocialProfileService(db, organizer, { nameMode: "masked" }, profileDeps);
  const requests = await listSocialActivityRequestsService(db, organizer, { activityId }, deps);
  assert.equal(requests.requests[0]!.name, "A.. D..", "the participant's own setting is untouched");
  assert.equal((await getSocialActivityService(db, ali, { activityId }, deps)).activity.organizerName, "A.. Y..");
  await setSocialProfileService(db, ali, { nameMode: "shown" }, profileDeps);
  assert.equal((await listSocialActivityRequestsService(db, organizer, { activityId }, deps)).requests[0]!.name, "Ali D.");
});

test("a profile photo is re-encoded, shown to verified students of the same university only, and replaced or removed cleanly", async () => {
  const { saved, deleted, deps: profileDeps } = photoDeps();
  const activityId = await openActivity({ capacity: 2 });

  await setSocialProfileService(db, organizer, { photo: await photoBase64() }, profileDeps);
  assert.equal(saved.length, 2);
  assert.ok(saved.every((name) => name.startsWith(`social-profiles/${organizer}/`)));
  const first = (await db.doc(`socialUserState/${organizer}`).get()).get("photoFolder") as string;

  const seen = await getSocialActivityService(db, ali, { activityId }, deps);
  assert.match(String(seen.activity.organizerPhotoUrl), /thumb\.jpg$/);
  // Someone who has not verified a school address sees the activity read-only, without the photo.
  assert.equal((await getSocialActivityService(db, unverified, { activityId }, deps)).activity.organizerPhotoUrl, null);
  await db.doc(`users/${zeynep}`).update({ eduEmail: "zeynep@ege.edu.tr" });
  assert.equal((await getSocialActivityService(db, zeynep, { activityId }, deps)).activity.organizerPhotoUrl, null);

  // The photo reaches the people the student deals with.
  await requestToJoinSocialActivityService(db, ali, { activityId }, deps);
  await setSocialProfileService(db, ali, { photo: await photoBase64() }, profileDeps);
  const list = await listSocialActivityRequestsService(db, organizer, { activityId }, deps);
  assert.match(String(list.requests[0]!.photoUrl), /thumb\.jpg$/);
  const { conversationId } = await respondToSocialRequestService(db, organizer, { activityId, requesterUid: ali, accept: true }, deps);
  const inbox = await listSocialConversationsService(db, ali, deps);
  assert.match(String(inbox.conversations.find((c) => c.id === conversationId!)?.otherPhotoUrl), /thumb\.jpg$/);

  // A new photo replaces the old one and the old files are removed.
  await setSocialProfileService(db, organizer, { photo: await photoBase64() }, profileDeps);
  assert.deepEqual(deleted, [first]);
  assert.notEqual((await db.doc(`socialUserState/${organizer}`).get()).get("photoFolder"), first);

  const second = (await db.doc(`socialUserState/${organizer}`).get()).get("photoFolder") as string;
  await setSocialProfileService(db, organizer, { removePhoto: true }, profileDeps);
  assert.equal(deleted.at(-1), second);
  assert.equal((await getSocialActivityService(db, ali, { activityId }, deps)).activity.organizerPhotoUrl, null);
});

test("profile photos are validated, limited per day, and need a verified student who accepted the rules", async () => {
  const { saved, deps: profileDeps } = photoDeps();
  await rejectsWith(setSocialProfileService(db, organizer, {}, profileDeps), "SOCIAL_PROFILE_EMPTY");
  await rejectsWith(setSocialProfileService(db, organizer, { photo: "###" }, profileDeps), "SOCIAL_PHOTO_INVALID");
  await rejectsWith(setSocialProfileService(db, organizer,
    { photo: Buffer.from("this is not an image at all").toString("base64") }, profileDeps), "SOCIAL_PHOTO_CONTENT_INVALID");
  await rejectsWith(setSocialProfileService(db, organizer, { photo: await photoBase64(), removePhoto: true }, profileDeps),
    "SOCIAL_PHOTO_INVALID");
  await rejectsWith(setSocialProfileService(db, organizer, { nameMode: "everyone" }, profileDeps), "SOCIAL_NAME_MODE_INVALID");
  await rejectsWith(setSocialProfileService(db, unverified, { nameMode: "shown" }, profileDeps), "SOCIAL_EDU_REQUIRED");
  assert.equal(saved.length, 0, "refused requests never store an image");

  const image = await photoBase64();
  for (let index = 0; index < 5; index += 1) await setSocialProfileService(db, organizer, { photo: image }, profileDeps);
  await rejectsWith(setSocialProfileService(db, organizer, { photo: image }, profileDeps), "SOCIAL_DAILY_PHOTO_LIMIT");
  assert.equal(saved.length, 10);
});

test("social profile download URLs use only the local Storage emulator when configured", () => {
  const url = "https://firebasestorage.googleapis.com/v0/b/demo-good4-v2/o/social-profiles%2Fuser1%2Fphoto.jpg?alt=media&token=test";
  assert.equal(socialPhotoDownloadUrl(url), url);
  assert.equal(socialPhotoDownloadUrl(url, "127.0.0.1:9295"), url.replace("https://firebasestorage.googleapis.com", "http://127.0.0.1:9295"));
  assert.equal(socialPhotoDownloadUrl(url, "example.com:9295"), url);
});

async function reportedPhoto() {
  const activityId = await openActivity();
  const { reportId } = await reportSocialContentService(db, ali,
    { targetType: "activity", targetId: activityId, reason: "inappropriate" }, deps);
  const photo = photoDeps();
  await setSocialProfileService(db, organizer, { photo: await photoBase64() }, photo.deps);
  return { reportId, photo, folder: (await db.doc(`socialUserState/${organizer}`).get()).get("photoFolder") as string };
}

test("an admin removes the reported user's profile photo and files, preserves their name choice and audits the action", async () => {
  const { reportId, photo, folder } = await reportedPhoto();
  await setSocialProfileService(db, organizer, { nameMode: "shown" }, photo.deps);
  assert.deepEqual(photo.deleted, [], "changing name visibility preserves the photo files");
  const queue = await listSocialModerationQueueService(db, admin);
  assert.equal(queue.reports[0]!.reportedProfile?.name, "Ayşe Y.");
  assert.match(queue.reports[0]!.reportedProfile!.photoThumbUrl!, /thumb.jpg$/);
  await removeSocialReportedProfilePhotoService(db, admin, { reportId }, photo.deps);
  const state = await db.doc(`socialUserState/${organizer}`).get();
  for (const key of ["photoUrl", "photoThumbUrl", "photoFolder"]) assert.equal(state.get(key), undefined);
  assert.equal(state.get("nameMode"), "shown");
  assert.deepEqual(photo.deleted, [folder]);
  assert.equal((await db.doc(`socialReports/${reportId}`).get()).get("status"), "open");
  const audits = await db.collection("auditLogs").where("action", "==", "social.profile.photoRemoved").get();
  assert.equal(audits.size, 1);
  assert.equal(audits.docs[0]!.get("actorUid"), admin);
  assert.equal(audits.docs[0]!.get("metadata.reportedUid"), organizer);
  assert.equal((await listSocialModerationQueueService(db, admin)).reports[0]!.reportedProfile!.photoUrl, null);
  await removeSocialReportedProfilePhotoService(db, admin, { reportId }, photo.deps);
  assert.deepEqual(photo.deleted, [folder], "repeated removal is harmless");
});

test("admin photo removal requires an active admin and an open report and cannot delete another user's folder", async () => {
  const { reportId, photo } = await reportedPhoto();
  await rejectsWith(removeSocialReportedProfilePhotoService(db, ali, { reportId }, photo.deps), "ROLE_NOT_ALLOWED");
  await rejectsWith(removeSocialReportedProfilePhotoService(db, admin, { reportId: "../bad" }, photo.deps), "REPORT_ID_INVALID");
  await rejectsWith(removeSocialReportedProfilePhotoService(db, admin, { reportId: "activity_missing_ali1" }, photo.deps), "SOCIAL_REPORT_NOT_FOUND");
  await db.doc(`socialUserState/${organizer}`).update({ photoFolder: `social-profiles/${ali}/other/` });
  await rejectsWith(removeSocialReportedProfilePhotoService(db, admin, { reportId }, photo.deps), "SOCIAL_PHOTO_FOLDER_INVALID");
  assert.deepEqual(photo.deleted, []);
  await db.doc(`socialReports/${reportId}`).update({ status: "resolved" });
  await rejectsWith(removeSocialReportedProfilePhotoService(db, admin, { reportId }, photo.deps), "SOCIAL_REPORT_RESOLVED");
  assert.deepEqual(photo.deleted, []);
});

test("failed Storage removal keeps the photo folder for retry and does not claim success in audit logs", async () => {
  const { reportId, photo, folder } = await reportedPhoto();
  await assert.rejects(removeSocialReportedProfilePhotoService(db, admin, { reportId }, {
    photos: { deletePrefix: async () => { throw new Error("storage unavailable"); } },
  }), /storage unavailable/);
  assert.equal((await db.doc(`socialUserState/${organizer}`).get()).get("photoFolder"), folder);
  assert.equal((await db.collection("auditLogs").where("action", "==", "social.profile.photoRemoved").get()).size, 0);
  await removeSocialReportedProfilePhotoService(db, admin, { reportId }, photo.deps);
  assert.equal((await db.doc(`socialUserState/${organizer}`).get()).get("photoFolder"), undefined);
});

test("a photo replaced during admin removal is preserved and the admin is asked to review the new photo", async () => {
  const { reportId, folder } = await reportedPhoto();
  const replacement = `social-profiles/${organizer}/replacement/`;
  await rejectsWith(removeSocialReportedProfilePhotoService(db, admin, { reportId }, {
    photos: { deletePrefix: async (prefix) => {
      assert.equal(prefix, folder);
      await db.doc(`socialUserState/${organizer}`).update({ photoFolder: replacement, photoUrl: "new-photo", photoThumbUrl: "new-thumb" });
    } },
  }), "SOCIAL_PROFILE_CHANGED");
  const state = await db.doc(`socialUserState/${organizer}`).get();
  assert.equal(state.get("photoFolder"), replacement);
  assert.equal(state.get("photoUrl"), "new-photo");
  const audit = await db.collection("auditLogs").where("action", "==", "social.profile.photoRemoved").get();
  assert.equal(audit.docs[0]!.get("metadata.replacedDuringRemoval"), true);
});
