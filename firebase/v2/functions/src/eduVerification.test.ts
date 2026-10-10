import { createHash } from "node:crypto";
import assert from "node:assert/strict";
import { after, beforeEach, test } from "node:test";
import { Timestamp } from "firebase-admin/firestore";
import { db, legacyTestDb } from "./firebase.js";
import {
  confirmEduVerificationService,
  eduEmailClaimPath,
  requestEduVerificationService as requestService,
  requestCampusEmailCodeService, confirmCampusEmailCodeService,
} from "./eduVerification.js";
import { brevoVerificationSender } from "./verificationEmail.js";
import { hasVerifiedCampusEmail } from "./campusEmailVerification.js";
import { ensureStudentProfileService } from "./studentAuth.js";

const deliveries: Array<{ email: string; code: string }> = [];
const deps = { send: async (email: string, code: string) => { deliveries.push({ email, code }); } };
const requestEduVerificationService = (database: typeof db, actor: string, input: { email?: unknown }, now = Date.now()) =>
  requestService(database, actor, input, now, deps);

const uid = "gmail-student";
const eduEmail = "can@ogr.akdeniz.edu.tr";

beforeEach(async () => {
  deliveries.length = 0;
  await Promise.all(["users", "eduVerifications", "eduEmailClaims", "mail", "auditLogs", "rateLimits"].map((collection) =>
    db.recursiveDelete(db.collection(collection))));
  await db.doc(`users/${uid}`).set({ role: "student", status: "active", email: "can@gmail.com" });
});

after(async () => {
  await Promise.all([db.terminate(), legacyTestDb.terminate()]);
});

async function sentCode(): Promise<string> {
  const code = deliveries.at(-1)?.code;
  assert.ok(code && /^\d{6}$/.test(code));
  return code;
}

test("a student verifies a .edu.tr address with the e-mailed code", async () => {
  const now = Date.now();
  const sent = await requestEduVerificationService(db, uid, { email: "  Can@OGR.Akdeniz.edu.tr " }, now);
  assert.equal(sent.outcome, "sent");

  const mail = await db.collection("mail").get();
  assert.equal(mail.size, 0);
  assert.equal(deliveries[0]?.email, eduEmail);
  const stored = await db.doc(`eduVerifications/${uid}`).get();
  assert.equal(stored.get("email"), eduEmail);
  assert.equal(typeof stored.get("codeHash"), "string");
  assert.ok(!JSON.stringify(stored.data()).includes(await sentCode()), "the raw code must not be stored");

  const result = await confirmEduVerificationService(db, uid, { code: await sentCode() }, now + 1000);
  assert.deepEqual(result, { outcome: "verified", email: eduEmail });
  const user = await db.doc(`users/${uid}`).get();
  assert.equal(user.get("eduVerified"), true);
  assert.equal(user.get("eduEmail"), eduEmail);
  assert.equal((await db.doc(eduEmailClaimPath(eduEmail)).get()).get("uid"), uid);
  assert.equal((await db.doc(`eduVerifications/${uid}`).get()).get("codeHash"), undefined);
});

test("non .edu.tr addresses are rejected", async () => {
  await assert.rejects(
    requestEduVerificationService(db, uid, { email: "can@gmail.com" }),
    (error: { message?: string }) => error.message === "EDU_EMAIL_INVALID",
  );
  await assert.rejects(
    requestEduVerificationService(db, uid, { email: "can@edu.tr.example.com" }),
    (error: { message?: string }) => error.message === "EDU_EMAIL_INVALID",
  );
});

test("wrong codes are counted and lock the request after five attempts", async () => {
  const now = Date.now();
  await requestEduVerificationService(db, uid, { email: eduEmail }, now);
  const code = await sentCode();
  const wrong = code === "000000" ? "111111" : "000000";

  for (let attempt = 1; attempt <= 5; attempt += 1) {
    const result = await confirmEduVerificationService(db, uid, { code: wrong }, now);
    assert.deepEqual(result, { outcome: "invalid_code", attemptsLeft: 5 - attempt });
  }
  assert.deepEqual(await confirmEduVerificationService(db, uid, { code }, now), { outcome: "too_many_attempts" });
  assert.notEqual((await db.doc(`users/${uid}`).get()).get("eduVerified"), true);
});

test("codes expire after ten minutes", async () => {
  const now = Date.now();
  await requestEduVerificationService(db, uid, { email: eduEmail }, now);
  const result = await confirmEduVerificationService(db, uid, { code: await sentCode() }, now + 10 * 60 * 1000);
  assert.deepEqual(result, { outcome: "expired" });
});

test("resends are rate limited", async () => {
  const now = Date.now();
  await requestEduVerificationService(db, uid, { email: eduEmail }, now);
  await assert.rejects(
    requestEduVerificationService(db, uid, { email: eduEmail }, now + 30_000),
    (error: { message?: string }) => error.message === "EDU_CODE_RESEND_TOO_SOON",
  );
  await assert.rejects(
    requestEduVerificationService(db, uid, { email: eduEmail }, now + 179_000),
    (error: { message?: string }) => error.message === "EDU_CODE_RESEND_TOO_SOON",
  );
  // Different addresses, so only the account's hourly allowance applies.
  for (let send = 2; send <= 5; send += 1) {
    await requestEduVerificationService(db, uid, { email: `student-${send}@ogr.akdeniz.edu.tr` }, now + send * 181_000);
  }
  await assert.rejects(
    requestEduVerificationService(db, uid, { email: eduEmail }, now + 6 * 181_000),
    (error: { message?: string }) => error.message === "EDU_CODE_SEND_LIMIT",
  );
});

test("an address verified by another account cannot be reused", async () => {
  await db.doc(eduEmailClaimPath(eduEmail)).set({ uid: "someone-else" });
  await assert.rejects(
    requestEduVerificationService(db, uid, { email: eduEmail }),
    (error: { message?: string }) => error.message === "EDU_EMAIL_IN_USE",
  );
});

test("only student accounts can request verification", async () => {
  await db.doc("users/business-1").set({ role: "businessStaff", status: "active" });
  await assert.rejects(
    requestEduVerificationService(db, "business-1", { email: eduEmail }),
    (error: { message?: string }) => error.message === "ROLE_NOT_ALLOWED",
  );
});

test("a verified .edu.tr sign-in counts as a verified university address", async () => {
  await ensureStudentProfileService(db, {
    uid: "edu-google",
    email: "Ada@Akdeniz.edu.tr",
    emailVerified: true,
    providers: ["google.com"],
  }, {
    userAgreementAccepted: true,
    kvkkNoticeAcknowledged: true,
  });
  const user = await db.doc("users/edu-google").get();
  assert.equal(user.get("eduVerified"), true);
  assert.equal(user.get("eduEmail"), "ada@akdeniz.edu.tr");
  assert.equal((await db.doc(eduEmailClaimPath("ada@akdeniz.edu.tr")).get()).get("uid"), "edu-google");
});

test("a Gmail sign-in stays a student without suspended-meal eligibility", async () => {
  await ensureStudentProfileService(db, {
    uid: "plain-google",
    email: "ada@gmail.com",
    emailVerified: true,
    providers: ["google.com"],
  }, {
    userAgreementAccepted: true,
    kvkkNoticeAcknowledged: true,
  });
  const user = await db.doc("users/plain-google").get();
  assert.equal(user.get("status"), "active");
  assert.equal(user.get("eduVerified"), undefined);
});


test("Brevo adapter posts Turkish transactional content with timeout and safe failures", async () => {
  let captured: RequestInit | undefined;
  let calledUrl: unknown;
  const transport = (async (url, init) => {
    calledUrl = url; captured = init;
    return new Response("", { status: 201 });
  }) as typeof fetch;
  await brevoVerificationSender(() => "unit-test-placeholder", transport)(eduEmail, "123456");
  assert.equal(calledUrl, "https://api.brevo.com/v3/smtp/email");
  assert.equal(captured?.method, "POST");
  assert.equal(new Headers(captured?.headers).get("api-key"), "unit-test-placeholder");
  assert.ok(captured?.signal instanceof AbortSignal);
  const body = JSON.parse(String(captured?.body));
  assert.deepEqual(body.sender, { name: "Good4", email: "noreply@good4tr.com" });
  assert.deepEqual(body.to, [{ email: eduEmail }]);
  assert.match(body.subject, /doğrulama/);
  assert.match(body.subject, /123456/);
  for (const field of [body.textContent, body.htmlContent]) {
    assert.match(field, /123456/); assert.match(field, /10 dakika geçerli/);
  }
  for (const transport of [
    (async () => new Response("sensitive provider body", { status: 500 })) as typeof fetch,
    (async () => { throw new Error("sensitive transport error"); }) as typeof fetch,
    (async () => { throw new DOMException("timed out", "TimeoutError"); }) as typeof fetch,
  ]) {
    await assert.rejects(brevoVerificationSender(() => "unit-test-placeholder", transport)(eduEmail, "123456"),
      (error: { message?: string }) => error.message === "EDU_EMAIL_SEND_FAILED");
  }
});

test("failed delivery preserves the previous code, cooldown and send allowance", async () => {
  const now = Date.now();
  const failed = { send: async () => { throw new Error("provider unavailable"); } };
  await assert.rejects(requestCampusEmailCodeService(db, uid, { email: eduEmail }, now, failed), /EDU_EMAIL_SEND_FAILED/);
  assert.equal((await db.doc(`eduVerifications/${uid}`).get()).exists, false);
  await requestCampusEmailCodeService(db, uid, { email: eduEmail }, now, deps);
  const before = (await db.doc(`eduVerifications/${uid}`).get()).data();
  const code = await sentCode();
  await assert.rejects(requestCampusEmailCodeService(db, uid, { email: eduEmail }, now + 181_000, failed), /EDU_EMAIL_SEND_FAILED/);
  assert.deepEqual((await db.doc(`eduVerifications/${uid}`).get()).data(), before);
  assert.deepEqual(await confirmCampusEmailCodeService(db, uid, { code }, now + 182_000), { outcome: "verified", email: eduEmail });
  assert.ok(hasVerifiedCampusEmail(await db.doc(`users/${uid}`).get()));
});

test("campus request and confirmation reject other .edu.tr domains on the server", async () => {
  const now = Date.now();
  for (const email of ["can@akdeniz.edu.tr", "can@ogr.other.edu.tr", "can@ogr.akdeniz.edu.tr.example.com"]) {
    await assert.rejects(requestCampusEmailCodeService(db, uid, { email }, now, deps));
  }
  assert.equal(deliveries.length, 0);
  await requestEduVerificationService(db, uid, { email: "can@other.edu.tr" }, now);
  await assert.rejects(confirmCampusEmailCodeService(db, uid, { code: await sentCode() }, now), /CAMPUS_EMAIL_INVALID/);
  assert.notEqual((await db.doc(`users/${uid}`).get()).get("eduVerified"), true);
});

test("campus codes expose separate wrong, expired and locked outcomes", async () => {
  const now = Date.now();
  await requestCampusEmailCodeService(db, uid, { email: eduEmail }, now, deps);
  const code = await sentCode();
  const wrong = code === "000000" ? "111111" : "000000";
  for (let attempt = 1; attempt <= 5; attempt++) {
    assert.deepEqual(await confirmCampusEmailCodeService(db, uid, { code: wrong }, now), { outcome: "invalid_code", attemptsLeft: 5 - attempt });
  }
  assert.deepEqual(await confirmCampusEmailCodeService(db, uid, { code }, now), { outcome: "too_many_attempts" });
  await requestCampusEmailCodeService(db, uid, { email: eduEmail }, now + 181_000, deps);
  assert.deepEqual(await confirmCampusEmailCodeService(db, uid, { code: await sentCode() }, now + 181_000 + 600_000), { outcome: "expired" });
});

test("an address can be claimed by only one account even when both already received codes", async () => {
  const now = Date.now();
  await db.doc("users/other").set({ role: "student", status: "active" });
  await requestCampusEmailCodeService(db, uid, { email: eduEmail }, now, deps);
  const first = await sentCode();
  await requestCampusEmailCodeService(db, "other", { email: eduEmail }, now, deps);
  const second = await sentCode();
  await confirmCampusEmailCodeService(db, uid, { code: first }, now);
  await assert.rejects(confirmCampusEmailCodeService(db, "other", { code: second }, now), /EDU_EMAIL_IN_USE/);
  await assert.rejects(requestCampusEmailCodeService(db, "other", { email: eduEmail }, now + 181_000, deps), /EDU_EMAIL_IN_USE/);
});

test("concurrent requests send only once; old link verification remains accepted", async () => {
  const now = Date.now();
  let release!: () => void;
  let entered!: () => void;
  const start = new Promise<void>((resolve) => { entered = resolve; });
  const wait = new Promise<void>((resolve) => { release = resolve; });
  let sends = 0;
  const first = requestCampusEmailCodeService(db, uid, { email: eduEmail }, now, {
    send: async () => { sends++; entered(); await wait; },
  });
  await start;
  await assert.rejects(requestCampusEmailCodeService(db, uid, { email: eduEmail }, now, deps), /EDU_EMAIL_SEND_IN_PROGRESS/);
  release(); await first; assert.equal(sends, 1);
  await db.doc(`users/${uid}`).update({ eduVerified: true, eduEmail });
  assert.deepEqual(await requestCampusEmailCodeService(db, uid, { email: eduEmail }, now, deps), { outcome: "already_verified", email: eduEmail });
  assert.ok(hasVerifiedCampusEmail(await db.doc(`users/${uid}`).get()));
});


test("successful confirmation cannot reset the hourly send allowance", async () => {
  const now = Date.now();
  for (let i = 0; i < 5; i++) {
    const email = `student-${i}@ogr.akdeniz.edu.tr`;
    await requestCampusEmailCodeService(db, uid, { email }, now + i * 181_000, deps);
    await confirmCampusEmailCodeService(db, uid, { code: await sentCode() }, now + i * 181_000);
  }
  await assert.rejects(requestCampusEmailCodeService(db, uid, { email: eduEmail }, now + 5 * 181_000, deps), /EDU_CODE_SEND_LIMIT/);
});

function rateLimitPath(key: string): string {
  return `rateLimits/${createHash("sha256").update(key).digest("hex")}`;
}

test("one address gets at most three codes an hour, whichever accounts ask", async () => {
  const now = Date.now();
  for (let i = 0; i < 3; i++) {
    await db.doc(`users/student-${i}`).set({ role: "student", status: "active", email: `s${i}@gmail.com` });
    await requestCampusEmailCodeService(db, `student-${i}`, { email: eduEmail }, now, deps);
  }
  await db.doc("users/student-3").set({ role: "student", status: "active", email: "s3@gmail.com" });
  await assert.rejects(requestCampusEmailCodeService(db, "student-3", { email: eduEmail }, now, deps), /EDU_CODE_RECIPIENT_LIMIT/);
  assert.equal(deliveries.length, 3);
});

test("an account gets at most ten codes a day", async () => {
  const now = Date.now();
  await db.doc(rateLimitPath(`eduMail:account:${uid}`)).set({ count: 10, resetAt: Timestamp.fromMillis(now + 3_600_000) });
  await assert.rejects(requestCampusEmailCodeService(db, uid, { email: eduEmail }, now, deps), /EDU_CODE_ACCOUNT_DAILY_LIMIT/);
  assert.equal(deliveries.length, 0);
});

test("all codes stop at the daily total that protects the mail quota", async () => {
  const now = Date.now();
  await db.doc(rateLimitPath("eduMail:total")).set({ count: 250, resetAt: Timestamp.fromMillis(now + 3_600_000) });
  await assert.rejects(requestCampusEmailCodeService(db, uid, { email: eduEmail }, now, deps), /EDU_CODE_DAILY_LIMIT/);
  assert.equal(deliveries.length, 0);
  assert.equal((await db.doc(`eduVerifications/${uid}`).get()).exists, false);
});
