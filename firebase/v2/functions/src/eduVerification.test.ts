import assert from "node:assert/strict";
import { after, beforeEach, test } from "node:test";
import { db, legacyTestDb } from "./firebase.js";
import {
  confirmEduVerificationService,
  eduEmailClaimPath,
  requestEduVerificationService,
} from "./eduVerification.js";
import { ensureStudentProfileService } from "./studentAuth.js";

const uid = "gmail-student";
const eduEmail = "can@ogr.akdeniz.edu.tr";

beforeEach(async () => {
  await Promise.all(["users", "eduVerifications", "eduEmailClaims", "mail", "auditLogs"].map((collection) =>
    db.recursiveDelete(db.collection(collection))));
  await db.doc(`users/${uid}`).set({ role: "student", status: "active", email: "can@gmail.com" });
});

after(async () => {
  await Promise.all([db.terminate(), legacyTestDb.terminate()]);
});

async function sentCode(): Promise<string> {
  const mail = await db.collection("mail").where("uid", "==", uid).get();
  const text = String(mail.docs.at(-1)?.get("message.text") ?? "");
  const code = /\b(\d{6})\b/.exec(text)?.[1];
  assert.ok(code, "verification e-mail should contain a six-digit code");
  return code;
}

test("a student verifies a .edu.tr address with the e-mailed code", async () => {
  const now = Date.now();
  const sent = await requestEduVerificationService(db, uid, { email: "  Can@OGR.Akdeniz.edu.tr " }, now);
  assert.equal(sent.outcome, "sent");

  const mail = await db.collection("mail").get();
  assert.equal(mail.size, 1);
  assert.deepEqual(mail.docs[0]?.get("to"), [eduEmail]);
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
  assert.equal((await db.doc(`eduVerifications/${uid}`).get()).exists, false);
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
  for (let send = 2; send <= 5; send += 1) {
    await requestEduVerificationService(db, uid, { email: eduEmail }, now + send * 61_000);
  }
  await assert.rejects(
    requestEduVerificationService(db, uid, { email: eduEmail }, now + 6 * 61_000),
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
