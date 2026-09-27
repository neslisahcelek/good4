import assert from "node:assert/strict";
import { Timestamp } from "firebase-admin/firestore";
import { after, beforeEach, test } from "node:test";
import { db, legacyTestDb } from "./firebase.js";
import {
  recordLegalAcknowledgementsService,
  requireLegalAcknowledgements,
} from "./legalAcknowledgements.js";

beforeEach(async () => {
  await db.recursiveDelete(db.collection("users"));
});

after(async () => {
  await Promise.all([db.terminate(), legacyTestDb.terminate()]);
});

test("records agreement, notice, and privacy policy versions with server timestamps", async () => {
  await db.doc("users/legal-student").set({ role: "student", status: "active" });

  assert.deepEqual(await recordLegalAcknowledgementsService(db, "legal-student", {
    userAgreementAccepted: true,
    kvkkNoticeAcknowledged: true,
  }), { recorded: true });

  const stored = await db.doc("users/legal-student").get();
  assert.equal(stored.get("legalAcknowledgements.kvkkNotice.version"), "1.0");
  assert.equal(stored.get("legalAcknowledgements.userAgreement.version"), "1.0");
  assert.equal(stored.get("legalAcknowledgements.privacyPolicy.version"), "1.3");
  assert.ok(stored.get("legalAcknowledgements.kvkkNotice.acknowledgedAt") instanceof Timestamp);
  assert.ok(stored.get("legalAcknowledgements.userAgreement.acceptedAt") instanceof Timestamp);
  assert.ok(stored.get("legalAcknowledgements.privacyPolicy.presentedAt") instanceof Timestamp);
  assert.equal(stored.get("acceptedKvkk"), undefined);
  assert.equal(stored.get("kvkkConsent"), undefined);
});

test("re-recording the same versions keeps the original acknowledgement timestamps", async () => {
  await db.doc("users/legal-business").set({ role: "business", status: "pending" });
  await recordLegalAcknowledgementsService(db, "legal-business", {
    userAgreementAccepted: true,
    kvkkNoticeAcknowledged: true,
  });
  const first = await db.doc("users/legal-business").get();
  const firstNoticeTime = first.get("legalAcknowledgements.kvkkNotice.acknowledgedAt") as Timestamp;
  const firstAgreementTime = first.get("legalAcknowledgements.userAgreement.acceptedAt") as Timestamp;
  const firstPrivacyPresentedTime = first.get("legalAcknowledgements.privacyPolicy.presentedAt") as Timestamp;

  await recordLegalAcknowledgementsService(db, "legal-business", {
    userAgreementAccepted: true,
    kvkkNoticeAcknowledged: true,
  });
  const second = await db.doc("users/legal-business").get();

  assert.deepEqual(
    second.get("legalAcknowledgements.kvkkNotice.acknowledgedAt"),
    firstNoticeTime,
  );
  assert.deepEqual(
    second.get("legalAcknowledgements.userAgreement.acceptedAt"),
    firstAgreementTime,
  );
  assert.deepEqual(
    second.get("legalAcknowledgements.privacyPolicy.presentedAt"),
    firstPrivacyPresentedTime,
  );
});

test("requires explicit selections and an existing profile", async () => {
  assert.throws(() => requireLegalAcknowledgements({
    userAgreementAccepted: true,
    kvkkNoticeAcknowledged: false,
  }), /LEGAL_ACKNOWLEDGEMENTS_REQUIRED/);
  await assert.rejects(() => recordLegalAcknowledgementsService(db, "missing", {
    userAgreementAccepted: true,
    kvkkNoticeAcknowledged: true,
  }), /USER_PROFILE_REQUIRED/);
});
