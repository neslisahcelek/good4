import assert from "node:assert/strict";
import { after, beforeEach, test } from "node:test";
import { Timestamp } from "firebase-admin/firestore";
import { db, legacyTestDb } from "./firebase.js";
import { ensureStudentProfileService } from "./studentAuth.js";

beforeEach(async () => {
  await db.recursiveDelete(db.collection("users"));
});

after(async () => {
  await Promise.all([db.terminate(), legacyTestDb.terminate()]);
});

test("verified Google user gets an active student profile", async () => {
  const result = await ensureStudentProfileService(db, {
    uid: "google-student",
    email: "Student.Gmail@gmail.com",
    emailVerified: true,
    displayName: "  Google Öğrenci  ",
    providers: ["google.com"],
  }, {
    userAgreementAccepted: true,
    kvkkNoticeAcknowledged: true,
  });
  const stored = await db.doc("users/google-student").get();
  assert.deepEqual(result, { created: true, role: "student", status: "active" });
  assert.equal(stored.get("email"), "student.gmail@gmail.com");
  assert.equal(stored.get("displayName"), "Google Öğrenci");
  assert.equal(stored.get("role"), "student");
  assert.equal(stored.get("status"), "active");
  assert.equal(stored.get("legalAcknowledgements.kvkkNotice.version"), "1.0");
  assert.equal(stored.get("legalAcknowledgements.userAgreement.version"), "1.0");
  assert.equal(stored.get("legalAcknowledgements.privacyPolicy.version"), "1.3");
  assert.ok(stored.get("legalAcknowledgements.kvkkNotice.acknowledgedAt") instanceof Timestamp);
  assert.ok(stored.get("legalAcknowledgements.userAgreement.acceptedAt") instanceof Timestamp);
  assert.ok(stored.get("legalAcknowledgements.privacyPolicy.presentedAt") instanceof Timestamp);
});

test("verified Apple user gets an active student profile", async () => {
  const result = await ensureStudentProfileService(db, {
    uid: "apple-student",
    email: "student@privaterelay.appleid.com",
    emailVerified: true,
    providers: ["apple.com"],
  }, {
    userAgreementAccepted: true,
    kvkkNoticeAcknowledged: true,
  });
  const stored = await db.doc("users/apple-student").get();
  assert.deepEqual(result, { created: true, role: "student", status: "active" });
  assert.equal(stored.get("email"), "student@privaterelay.appleid.com");
  assert.equal(stored.get("role"), "student");
  assert.equal(stored.get("status"), "active");
  assert.notEqual(stored.get("eduVerified"), true);
});

test("existing privileged profile is never overwritten", async () => {
  await db.doc("users/manager").set({ role: "communityManager", status: "active" });
  const result = await ensureStudentProfileService(db, {
    uid: "manager",
    email: "manager@gmail.com",
    emailVerified: true,
    providers: ["google.com"],
  });
  const stored = await db.doc("users/manager").get();
  assert.deepEqual(result, { created: false, role: "communityManager", status: "active" });
  assert.equal(stored.get("role"), "communityManager");
});

test("edu password user is pending until email verification, then becomes active", async () => {
  const pending = await ensureStudentProfileService(db, {
    uid: "password-user",
    email: "student@ogr.akdeniz.edu.tr",
    emailVerified: false,
    providers: ["password"],
  }, {
    displayName: "Edu Öğrenci",
    university: "Akdeniz Üniversitesi",
    userAgreementAccepted: true,
    kvkkNoticeAcknowledged: true,
  });
  assert.deepEqual(pending, {
    created: true,
    role: "student",
    status: "pendingEmailVerification",
  });

  const active = await ensureStudentProfileService(db, {
    uid: "password-user",
    email: "student@ogr.akdeniz.edu.tr",
    emailVerified: true,
    providers: ["password"],
  });
  assert.deepEqual(active, { created: false, role: "student", status: "active" });
  const stored = await db.doc("users/password-user").get();
  assert.equal(stored.get("displayName"), "Edu Öğrenci");
  assert.equal(stored.get("university"), "Akdeniz Üniversitesi");
  assert.equal(stored.get("status"), "active");
});

test("non-edu password and unverified Google identities are rejected", async () => {
  await assert.rejects(() => ensureStudentProfileService(db, {
    uid: "password-user-non-edu",
    email: "student@gmail.com",
    emailVerified: true,
    providers: ["password"],
  }));
  await assert.rejects(() => ensureStudentProfileService(db, {
    uid: "unverified-user",
    email: "student@gmail.com",
    emailVerified: false,
    providers: ["google.com"],
  }));
});

test("new student profile requires both legal selections", async () => {
  await assert.rejects(() => ensureStudentProfileService(db, {
    uid: "student-without-legal-selections",
    email: "student@ogr.akdeniz.edu.tr",
    emailVerified: false,
    providers: ["password"],
  }, {
    userAgreementAccepted: true,
    kvkkNoticeAcknowledged: false,
  }), /LEGAL_ACKNOWLEDGEMENTS_REQUIRED/);
  assert.equal(
    (await db.doc("users/student-without-legal-selections").get()).exists,
    false,
  );
});
