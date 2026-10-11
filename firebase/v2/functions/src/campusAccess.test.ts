import assert from "node:assert/strict";
import { after, beforeEach, test } from "node:test";
import { Timestamp } from "firebase-admin/firestore";
import { db, legacyTestDb } from "./firebase.js";
import { disconnectCampusTestService, getCampusAccessStatusService, requireCampusAccess, resolveCampusAccess } from "./campusAccess.js";
import { requestCampusEmailCodeService, confirmCampusEmailCodeService } from "./eduVerification.js";
import { getMarketSummaryService, acceptMarketTermsService, MARKET_TERMS_VERSION } from "./market.js";
import { getSocialSummaryService } from "./social.js";

const uid = "test-admin";
const email = "admin@example.com";
const deliveries: { email: string; code: string }[] = [];
const deps = { send: async (email: string, code: string) => { deliveries.push({ email, code }); },
  identity: async () => ({ email, emailVerified: true }) };
beforeEach(async () => {
  deliveries.length = 0;
  await Promise.all(["users", "eduVerifications", "eduEmailClaims", "auditLogs", "rateLimits", "app_config", "marketUserState", "socialUserState"]
    .map((collection) => db.recursiveDelete(db.collection(collection))));
  await db.doc(`users/${uid}`).set({ role: "good4Admin", status: "active", email,
    adminEduTest: { enabled: true, email, grantId: "grant-1" } });
  await db.doc("app_config/social_activities").set({ enabled: true });
});
after(async () => { await Promise.all([db.terminate(), legacyTestDb.terminate()]); });
async function send(now = Date.now()) {
  const result = await requestCampusEmailCodeService(db, uid, { email }, now, deps);
  assert.equal(result.outcome, "sent");
  assert.ok(result.outcome === "sent" && result.sessionId);
  return { now, sessionId: result.sessionId, code: deliveries.at(-1)!.code };
}
async function confirm(sent: Awaited<ReturnType<typeof send>>) {
  return confirmCampusEmailCodeService(db, uid, { code: sent.code, sessionId: sent.sessionId }, sent.now + 1);
}

test("real-mail test verification grants campus access without fabricating an edu proof", async () => {
  assert.equal((await getCampusAccessStatusService(db, uid)).campusAccessAllowed, false);
  const sent = await send();
  assert.equal(deliveries[0]!.email, email);
  await confirm(sent);
  const user = await db.doc(`users/${uid}`).get();
  const status = resolveCampusAccess(user);
  assert.equal(status.campusAccessAllowed, true);
  assert.equal(status.accessSource, "admin_test");
  assert.equal(status.eduVerified, false);
  assert.equal(user.get("eduVerified"), undefined);
  assert.equal(user.get("eduEmail"), undefined);
  assert.equal((await db.collection("eduEmailClaims").get()).size, 0);
  assert.equal(requireCampusAccess(user, "EDU_REQUIRED"), "akdeniz.edu.tr");
  assert.equal((await getMarketSummaryService(db, uid)).me.campusAccessAllowed, true);
  assert.equal((await getSocialSummaryService(db, uid)).me.campusAccessAllowed, true);
  // Membership never bypasses the independent terms gate.
  await acceptMarketTermsService(db, uid, { version: MARKET_TERMS_VERSION });
  await disconnectCampusTestService(db, uid);
  assert.equal((await getMarketSummaryService(db, uid)).me.campusAccessAllowed, false);
  assert.equal((await getSocialSummaryService(db, uid)).me.campusAccessAllowed, false);
  assert.equal((await db.doc(`marketUserState/${uid}`).get()).get("termsVersion"), MARKET_TERMS_VERSION);
  await assert.rejects(confirm(sent), /EDU_CODE_NOT_REQUESTED/);
  await assert.rejects(send(sent.now + 179_999), /EDU_CODE_RESEND_TOO_SOON/);
  const next = await send(sent.now + 180_000);
  await assert.rejects(confirmCampusEmailCodeService(db, uid, { code: next.code, sessionId: sent.sessionId }, next.now), /CAMPUS_TEST_SESSION_INVALID/);
  await confirm(next);
  assert.equal((await getCampusAccessStatusService(db, uid)).campusAccessAllowed, true);
});

test("caller cannot choose the test recipient, fake Auth email verification, or omit the session", async () => {
  await assert.rejects(requestCampusEmailCodeService(db, uid, { email: "someone@example.com" }, Date.now(), deps), /CAMPUS_TEST_RECIPIENT_FIXED/);
  await assert.rejects(requestCampusEmailCodeService(db, uid, {}, Date.now(), {
    ...deps, identity: async () => ({ email, emailVerified: false }),
  }), /CAMPUS_TEST_EMAIL_UNAVAILABLE/);
  await assert.rejects(requestCampusEmailCodeService(db, uid, {}, Date.now(), {
    ...deps, identity: async () => ({ email: "changed@example.com", emailVerified: true }),
  }), /CAMPUS_TEST_EMAIL_UNAVAILABLE/);
  assert.equal(deliveries.length, 0);
  const sent = await send();
  await assert.rejects(confirmCampusEmailCodeService(db, uid, { code: sent.code }, sent.now), /CAMPUS_TEST_SESSION_INVALID/);
});

test("ungranted admins, students with forged flags, inactive users and revoked grants lose access", async () => {
  const sent = await send(); await confirm(sent);
  const ref = db.doc(`users/${uid}`);
  for (const update of [
    { role: "student", status: "active" }, { role: "good4Admin", status: "disabled" },
    { role: "good4Admin", status: "deleting" },
    { role: "good4Admin", status: "active", adminEduTest: { enabled: false, email, grantId: "grant-1" } },
    { role: "good4Admin", status: "active", adminEduTest: { enabled: true, email, grantId: "grant-2" } },
  ]) {
    await ref.update(update);
    assert.equal(resolveCampusAccess(await ref.get()).campusAccessAllowed, false);
  }
  await ref.update({ adminEduTest: null });
  await assert.rejects(send(), /ROLE_NOT_ALLOWED/);
  await assert.rejects(disconnectCampusTestService(db, uid), /ROLE_NOT_ALLOWED/);
});

test("disconnect during delivery invalidates the result and keeps the new window's cooldowns", async () => {
  const now = Date.now();
  await db.doc(`eduVerifications/${uid}`).set({
    windowStartedAt: Timestamp.fromMillis(now - 3_600_001),
    lastSentAt: Timestamp.fromMillis(now - 3_600_001), sendCount: 5,
  });
  let entered!: () => void, release!: () => void;
  const started = new Promise<void>((resolve) => { entered = resolve; });
  const pending = new Promise<void>((resolve) => { release = resolve; });
  const request = requestCampusEmailCodeService(db, uid, { email }, now, { ...deps,
    send: async () => { entered(); await pending; } });
  const rejected = assert.rejects(request, /CAMPUS_TEST_SESSION_INVALID/);
  await started;
  await disconnectCampusTestService(db, uid);
  release(); await rejected;
  assert.equal((await getCampusAccessStatusService(db, uid)).campusAccessAllowed, false);
  const stored = await db.doc(`eduVerifications/${uid}`).get();
  assert.equal(stored.get("codeHash"), undefined);
  assert.equal(stored.get("sendCount"), 1);
  assert.equal(stored.get("windowStartedAt").toMillis(), now);
  assert.equal(stored.get("pendingSendCount"), undefined);
  await assert.rejects(send(now + 1000), /EDU_CODE_RESEND_TOO_SOON/);
});

test("test codes use the same wrong-code and expiry limits; stale grants cannot complete", async () => {
  const sent = await send();
  for (let attempt = 0; attempt < 5; attempt++) {
    const result = await confirmCampusEmailCodeService(db, uid, { sessionId: sent.sessionId,
      code: sent.code === "000000" ? "111111" : "000000" }, sent.now);
    assert.deepEqual(result, { outcome: "invalid_code", attemptsLeft: 4 - attempt });
  }
  assert.deepEqual(await confirm(sent), { outcome: "too_many_attempts" });
  const next = await send(sent.now + 180_000);
  assert.deepEqual(await confirmCampusEmailCodeService(db, uid, { code: next.code, sessionId: next.sessionId }, next.now + 600_000), { outcome: "expired" });
  await db.doc(`users/${uid}`).update({ "adminEduTest.grantId": "replacement" });
  await assert.rejects(confirm(next), /CAMPUS_TEST_SESSION_INVALID/);
});

test("disconnect does not erase real edu proof or existing data", async () => {
  await db.doc(`users/${uid}`).update({ eduEmail: "real@ogr.akdeniz.edu.tr", eduVerified: true,
    eduVerifiedAt: Timestamp.now() });
  await db.doc("eduEmailClaims/real-claim").set({ uid });
  await disconnectCampusTestService(db, uid);
  assert.equal((await getCampusAccessStatusService(db, uid)).accessSource, "edu_email");
  assert.equal((await db.doc("eduEmailClaims/real-claim").get()).exists, true);
});


test("disconnect before mail reservation rejects the outstanding Auth lookup", async () => {
  let entered!: () => void, release!: () => void;
  const started = new Promise<void>((resolve) => { entered = resolve; });
  const wait = new Promise<void>((resolve) => { release = resolve; });
  const request = requestCampusEmailCodeService(db, uid, { email }, Date.now(), {
    ...deps, identity: async () => { entered(); await wait; return { email, emailVerified: true }; },
  });
  const rejection = assert.rejects(request, /CAMPUS_TEST_SESSION_INVALID/);
  await started;
  await disconnectCampusTestService(db, uid);
  release(); await rejection;
  assert.equal(deliveries.length, 0);
  assert.equal((await db.doc(`eduVerifications/${uid}`).get()).exists, false);
});


test("student-only campus scope admits verified testers and preserves ungranted admin boundaries", async () => {
  const sent = await send(); await confirm(sent);
  assert.equal(requireCampusAccess(await db.doc(`users/${uid}`).get(), "BENEFIT_REQUIRED", "student"), "akdeniz.edu.tr");
  await db.doc(`users/${uid}`).update({ eduVerified: true, eduEmail: "real@ogr.akdeniz.edu.tr", adminEduTest: null });
  const user = await db.doc(`users/${uid}`).get();
  assert.equal(resolveCampusAccess(user).campusAccessAllowed, true);
  assert.equal(resolveCampusAccess(user).studentAccessAllowed, false);
  assert.throws(() => requireCampusAccess(user, "BENEFIT_REQUIRED", "student"), /BENEFIT_REQUIRED/);
});
