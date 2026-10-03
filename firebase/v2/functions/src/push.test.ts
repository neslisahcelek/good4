import assert from "node:assert/strict";
import { after, beforeEach, test } from "node:test";
import type { MulticastMessage } from "firebase-admin/messaging";
import { Timestamp } from "firebase-admin/firestore";
import { db, legacyTestDb } from "./firebase.js";
import { MARKET_TERMS_VERSION, sendMarketMessageService, respondMarketOfferService, reviewMarketListingService, type MarketDeps } from "./market.js";
import {
  erasePushDevices, pushDeviceId, PUSH_DEVICE_MAX_AGE_MS, registerPushDeviceService,
  sendPushToUser, unregisterPushDeviceService,
  type PushBatchResult, type PushSender,
} from "./push.js";

assert.ok(process.env.FIRESTORE_EMULATOR_HOST && process.env.GCLOUD_PROJECT?.startsWith("demo-"), "Push tests require an isolated emulator project");
const NOW = Date.UTC(2026, 9, 3, 12);
const tokenA = "test-fcm-token-aaaaaaaaaaaaaaaa";
const tokenB = "test-fcm-token-bbbbbbbbbbbbbbbb";
const tokenC = "test-fcm-token-cccccccccccccccc";
const payload = { title: "A private listing name", body: "Private message", data: { type: "market_message", conversationId: "listing_buyer" } };

beforeEach(async () => {
  await Promise.all(["pushDevices", "users", "marketListings", "marketConversations", "marketUserState", "auditLogs", "app_config"]
    .map((name) => db.recursiveDelete(db.collection(name))));
  await Promise.all(["a", "b"].map((uid) => db.doc(`users/${uid}`).set({ status: "active", role: "student" })));
});
after(async () => { await Promise.all([db.terminate(), legacyTestDb.terminate()]); });

function sender(handler: (message: MulticastMessage) => Promise<PushBatchResult>): PushSender {
  return { sendEachForMulticast: handler };
}
function success(count: number): PushBatchResult {
  return { successCount: count, failureCount: 0, responses: Array.from({ length: count }, () => ({ success: true, messageId: "test" })) };
}
const ref = (token: string) => db.doc(`pushDevices/${pushDeviceId(token)}`);

test("device registration requires authentication, an active configured account and valid input", async () => {
  await assert.rejects(registerPushDeviceService(db, undefined, { token: tokenA, platform: "ios" }), /AUTHENTICATION_REQUIRED/);
  await assert.rejects(registerPushDeviceService(db, "a", { token: "bad", platform: "ios" }), /PUSH_TOKEN_INVALID/);
  await assert.rejects(registerPushDeviceService(db, "a", { token: tokenA, platform: "web" }), /PUSH_PLATFORM_INVALID/);
  await db.doc("users/a").update({ status: "suspended" });
  await assert.rejects(registerPushDeviceService(db, "a", { token: tokenA, platform: "ios" }), /ACCOUNT_NOT_ACTIVE/);
  assert.equal((await db.collection("pushDevices").get()).size, 0);
});

test("registration is idempotent and tokens have a single owner across account switches", async () => {
  for (let i = 0; i < 2; i++) await registerPushDeviceService(db, "a", { token: tokenA, platform: "ios" }, NOW);
  assert.equal((await db.collection("pushDevices").get()).size, 1);
  await registerPushDeviceService(db, "b", { token: tokenA, platform: "android", uid: "a" } as { token: unknown; platform: unknown }, NOW);
  assert.equal((await ref(tokenA).get()).get("uid"), "b");
  await unregisterPushDeviceService(db, "a", { token: tokenA });
  assert.equal((await ref(tokenA).get()).get("uid"), "b", "old account cannot remove the new owner");
  await unregisterPushDeviceService(db, "b", { token: tokenA });
  await unregisterPushDeviceService(db, "b", { token: tokenA });
  assert.equal((await ref(tokenA).get()).exists, false);
});

test("unregister requires a primary authenticated account", async () => {
  await assert.rejects(unregisterPushDeviceService(db, undefined, { token: tokenA }), /AUTHENTICATION_REQUIRED/);
});

test("send targets only the recipient's devices with private text and primary-account routing", async () => {
  await registerPushDeviceService(db, "a", { token: tokenA, platform: "ios" }, NOW);
  await registerPushDeviceService(db, "a", { token: tokenB, platform: "android" }, NOW);
  await registerPushDeviceService(db, "b", { token: tokenC, platform: "ios" }, NOW);
  const result = await sendPushToUser(db, "a", { ...payload, data: { ...payload.data, recipientUid: "b" } }, sender(async (message) => {
    assert.deepEqual(new Set(message.tokens), new Set([tokenA, tokenB]));
    assert.equal(message.data?.recipientUid, "a");
    assert.equal(message.notification?.title, "Kampüs Dolabı");
    assert.ok(!JSON.stringify(message.notification).includes("Private"));
    assert.equal(message.android?.notification?.channelId, "campus_closet");
    assert.equal(message.apns?.headers?.["apns-push-type"], "alert");
    return success(2);
  }), NOW);
  assert.deepEqual(result, { sent: 2, failed: 0 });
});

test("inactive and erased accounts receive no notification", async () => {
  await registerPushDeviceService(db, "a", { token: tokenA, platform: "ios" }, NOW);
  await db.doc("users/a").update({ status: "deleting" });
  const never = sender(async () => { assert.fail("must not contact FCM"); });
  assert.deepEqual(await sendPushToUser(db, "a", payload, never, NOW), { sent: 0, failed: 0 });
  assert.deepEqual(await sendPushToUser(db, "unknown", payload, never, NOW), { sent: 0, failed: 0 });
});

test("expired device addresses are removed and never sent to", async () => {
  await registerPushDeviceService(db, "a", { token: tokenA, platform: "ios" }, NOW - PUSH_DEVICE_MAX_AGE_MS);
  await registerPushDeviceService(db, "a", { token: tokenB, platform: "ios" }, NOW);
  await sendPushToUser(db, "a", payload, sender(async (message) => {
    assert.deepEqual(message.tokens, [tokenB]); return success(1);
  }), NOW);
  assert.equal((await ref(tokenA).get()).exists, false);
});

test("only permanent token errors prune registrations; transient or APNs errors remain retryable", async () => {
  for (const token of [tokenA, tokenB, tokenC]) await registerPushDeviceService(db, "a", { token, platform: "ios" }, NOW);
  const result = await sendPushToUser(db, "a", payload, sender(async (message) => {
    const errors = new Map([[tokenA, "messaging/registration-token-not-registered"], [tokenB, "messaging/third-party-auth-error"], [tokenC, "messaging/server-unavailable"]]);
    return { successCount: 0, failureCount: 3, responses: message.tokens.map((token) => ({ success: false, error: { code: errors.get(token)!, message: "test" } })) };
  }), NOW);
  assert.deepEqual(result, { sent: 0, failed: 3 });
  assert.equal((await ref(tokenA).get()).exists, false);
  assert.equal((await ref(tokenB).get()).exists, true);
  assert.equal((await ref(tokenC).get()).exists, true);
});

test("late failed delivery cannot delete a token renewed or transferred during the send", async () => {
  await registerPushDeviceService(db, "a", { token: tokenA, platform: "ios" }, NOW);
  await sendPushToUser(db, "a", payload, sender(async () => {
    await registerPushDeviceService(db, "b", { token: tokenA, platform: "ios" }, NOW + 1);
    return { successCount: 0, failureCount: 1, responses: [{ success: false, error: { code: "messaging/registration-token-not-registered", message: "test" } }] };
  }), NOW);
  assert.equal((await ref(tokenA).get()).get("uid"), "b");
});

test("provider outage never fails an already committed market operation", async () => {
  await registerPushDeviceService(db, "a", { token: tokenA, platform: "ios" }, NOW);
  assert.deepEqual(await sendPushToUser(db, "a", payload, sender(async () => { throw Error("Provider failed"); }), NOW), { sent: 0, failed: 1 });
  assert.equal((await ref(tokenA).get()).exists, true);
});

test("a hanging provider is bounded so the callable can acknowledge the saved message", async () => {
  await registerPushDeviceService(db, "a", { token: tokenA, platform: "ios" }, NOW);
  const started = Date.now();
  const result = await sendPushToUser(db, "a", payload, sender(() => new Promise(() => {})), NOW, 100);
  assert.deepEqual(result, { sent: 0, failed: 1 });
  assert.ok(Date.now() - started < 2_000);
});

test("account erasure removes only that account's devices", async () => {
  await registerPushDeviceService(db, "a", { token: tokenA, platform: "ios" }, NOW);
  await registerPushDeviceService(db, "b", { token: tokenB, platform: "ios" }, NOW);
  await erasePushDevices(db, "a");
  assert.equal((await ref(tokenA).get()).exists, false);
  assert.equal((await ref(tokenB).get()).get("uid"), "b");
});

test("large device sets respect the FCM batch size", async () => {
  const batch = db.batch();
  for (let i = 0; i < 501; i++) batch.set(db.doc(`pushDevices/test${i}`), { uid: "a", token: `test-fcm-token-${i.toString().padStart(20, "0")}`, updatedAt: Timestamp.fromMillis(NOW) });
  await batch.commit();
  const sizes: number[] = [];
  const result = await sendPushToUser(db, "a", payload, sender(async (message) => { sizes.push(message.tokens.length); return success(message.tokens.length); }), NOW);
  assert.deepEqual(sizes, [500, 1]);
  assert.deepEqual(result, { sent: 501, failed: 0 });
});

test("real market messages, offers, offer replies and listing reviews reach the correct device", async () => {
  await Promise.all(["a", "b"].map(async (uid) => {
    await db.doc(`users/${uid}`).update({ eduVerified: true, eduEmail: `${uid}@ogr.akdeniz.edu.tr`, displayName: "Öğrenci" });
    await db.doc(`marketUserState/${uid}`).set({ termsVersion: MARKET_TERMS_VERSION });
  }));
  await db.doc("users/admin").set({ status: "active", role: "good4Admin" });
  await db.doc("marketListings/book1").set({ sellerUid: "a", status: "published", eduDomain: "akdeniz.edu.tr", title: "Ders kitabı", price: 100, photos: [{ url: "https://example.test/photo.jpg" }] });
  await registerPushDeviceService(db, "a", { token: tokenA, platform: "ios" }, NOW);
  await registerPushDeviceService(db, "b", { token: tokenB, platform: "android" }, NOW);
  const delivered: MulticastMessage[] = [];
  const provider = sender(async (message) => { delivered.push(message); return success(message.tokens.length); });
  let clock = NOW;
  const deps: MarketDeps = {
    photos: { save: async () => "https://example.test/photo.jpg", deletePrefix: async () => {} },
    notify: (uid, notification) => sendPushToUser(db, uid, notification, provider, clock),
    now: () => (clock += 1000),
  };
  await sendMarketMessageService(db, "b", { conversationId: "book1_b", text: "Merhaba, ürün satılık mı?" }, deps);
  await sendMarketMessageService(db, "b", { conversationId: "book1_b", offerPercent: 10 }, deps);
  await respondMarketOfferService(db, "a", { conversationId: "book1_b", accept: true }, deps);
  for (const [listingId, decision] of [["review1", "approve"], ["review2", "reject"]]) {
    await db.doc(`marketListings/${listingId}`).set({ sellerUid: "a", status: "pending", title: "Kitap", photos: [] });
    await reviewMarketListingService(db, "admin", { listingId, decision, reason: "Fotoğrafı yenile" }, deps);
  }
  assert.equal(delivered.length, 5);
  assert.deepEqual(delivered.map((message) => message.tokens), [[tokenA], [tokenA], [tokenB], [tokenA], [tokenA]]);
  assert.deepEqual(delivered.map((message) => message.data?.recipientUid), ["a", "a", "b", "a", "a"]);
  assert.deepEqual(delivered.slice(0, 3).map((message) => message.data?.conversationId), ["book1_b", "book1_b", "book1_b"]);
  assert.equal(delivered[3]?.notification?.body, "İlanın yayında");
  assert.equal(delivered[4]?.notification?.body, "İlanın yayınlanmadı");
});
