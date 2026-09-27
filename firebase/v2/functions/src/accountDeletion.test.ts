import assert from "node:assert/strict";
import { after, beforeEach, test } from "node:test";
import { db, legacyTestDb } from "./firebase.js";
import { eraseAccountData } from "./accountDeletion.js";

const uid = "delete-me";

beforeEach(async () => {
  await Promise.all([
    ...[
      "users", "organizations", "events", "campaignClaims", "campaignCodes",
      "redemptions", "feedbackSubmissions", "auditLogs", "legacyTestRedemptions",
      "communities", "codes", "orders", "businesses", "community_access",
      "eduEmailClaims", "eduVerifications", "mail",
    ].map((collection) => db.recursiveDelete(db.collection(collection))),
    ...["communities", "community_coupon_codes", "community_access", "users"].map((collection) =>
      legacyTestDb.recursiveDelete(legacyTestDb.collection(collection))),
  ]);
});

after(async () => {
  await Promise.all([db.terminate(), legacyTestDb.terminate()]);
});

test("deletes personal records and removes account identifiers from retained V2 history", async () => {
  await Promise.all([
    db.doc(`users/${uid}`).set({ email: "delete@example.com", status: "active" }),
    db.doc("organizations/community-1").set({ followerCount: 1, createdBy: uid }),
    db.doc(`organizations/community-1/members/${uid}`).set({ userId: uid, role: "manager" }),
    db.doc(`organizations/community-1/followers/${uid}`).set({ userId: uid }),
    db.doc("events/event-1").set({ registrationCount: 1, attendanceCount: 1, createdBy: uid }),
    db.doc("events/event-1/registrations/reg-1").set({ userId: uid, status: "registered" }),
    db.doc("events/event-1/checkins/reg-1").set({ userId: uid, checkedInBy: "staff-1" }),
    db.doc("events/event-2").set({ registrationCount: 1, attendanceCount: 1 }),
    db.doc("events/event-2/registrations/reg-2").set({ userId: "attendee-2", status: "registered" }),
    db.doc("events/event-2/checkins/reg-2").set({ userId: "attendee-2", checkedInBy: uid }),
    db.doc("communities/legacy-community/entries/legacy-event").set({ kind: "event" }),
    db.doc(`communities/legacy-community/entries/legacy-event/registrations/${uid}`).set({
      userId: uid, displayName: "Can",
    }),
    db.doc(`communities/legacy-community/entries/legacy-event/attendance/${uid}`).set({
      userId: uid, checkedInBy: "legacy-staff",
    }),
    db.doc("communities/legacy-community/entries/other-event").set({ kind: "event" }),
    db.doc("communities/legacy-community/entries/other-event/attendance/other-attendee").set({
      userId: "other-attendee", checkedInBy: uid,
    }),
    db.doc("campaigns/campaign-1").set({ createdBy: uid }),
    db.doc("businesses/business-1").set({ ownerId: uid }),
    db.doc("codes/code-pending").set({ userId: uid, status: "pending" }),
    db.doc("codes/code-used").set({ userId: uid, status: "used" }),
    db.doc("orders/order-1").set({ supporterId: uid, supporterName: "Can Kılınç" }),
    db.doc("community_access/delete@example.com").set({ active: true }),
    db.doc("campaignClaims/campaign-1_delete-me").set({ studentId: uid }),
    db.doc("campaignCodes/ABC12345").set({ studentId: uid }),
    db.doc("redemptions/ABC12345").set({ studentId: uid, redeemedBy: uid }),
    db.doc("feedbackSubmissions/feedback-1").set({ userId: uid, userEmail: "delete@example.com" }),
    db.doc(`eduVerifications/${uid}`).set({ email: "delete@example.edu.tr", codeHash: "hashed" }),
    db.doc("eduEmailClaims/claim-1").set({ uid }),
    db.doc("mail/verification-1").set({ uid, to: ["delete@example.edu.tr"] }),
    db.doc("legacyTestRedemptions/123456").set({ legacyStudentId: uid, redeemedBy: uid }),
    db.doc("auditLogs/member-assigned").set({
      actorUid: "admin-1",
      targetType: "organizationMember",
      targetId: `community-1:${uid}`,
      metadata: { userId: uid, organizationId: "community-1" },
    }),
    db.doc("auditLogs/account-action").set({ actorUid: uid, action: "event.created" }),
    db.doc("auditLogs/edu-verified").set({
      actorUid: uid,
      action: "eduEmail.verified",
      targetType: "user",
      targetId: uid,
      metadata: { domain: "example.edu.tr" },
    }),
    legacyTestDb.doc("communities/community-1/entries/coupon-1/claims/delete-me").set({ userId: uid }),
    legacyTestDb.doc("community_coupon_codes/111111").set({ userId: uid, status: "pending" }),
    legacyTestDb.doc("community_coupon_codes/222222").set({ userId: uid, status: "used" }),
    legacyTestDb.doc("community_access/delete@example.com").set({ active: true }),
  ]);

  await eraseAccountData(db, legacyTestDb, uid, "delete@example.com");

  assert.equal((await db.doc(`users/${uid}`).get()).exists, false);
  assert.equal((await db.doc(`organizations/community-1/members/${uid}`).get()).exists, false);
  assert.equal((await db.doc(`organizations/community-1/followers/${uid}`).get()).exists, false);
  assert.equal((await db.doc("organizations/community-1").get()).get("followerCount"), 0);
  assert.equal((await db.doc("events/event-1/registrations/reg-1").get()).exists, false);
  assert.equal((await db.doc("events/event-1/checkins/reg-1").get()).exists, false);
  assert.equal((await db.doc("events/event-1").get()).get("registrationCount"), 0);
  assert.equal((await db.doc("events/event-1").get()).get("attendanceCount"), 0);
  assert.equal((await db.doc("events/event-2/checkins/reg-2").get()).get("checkedInBy"), "deleted-account");
  assert.equal((await db.doc(`communities/legacy-community/entries/legacy-event/registrations/${uid}`).get()).exists, false);
  assert.equal((await db.doc(`communities/legacy-community/entries/legacy-event/attendance/${uid}`).get()).exists, false);
  assert.equal((await db.doc("communities/legacy-community/entries/other-event/attendance/other-attendee").get()).get("checkedInBy"), "deleted-account");
  assert.equal((await db.doc("events/event-1").get()).get("createdBy"), "deleted-account");
  assert.equal((await db.doc("campaigns/campaign-1").get()).get("createdBy"), "deleted-account");
  assert.equal((await db.doc("businesses/business-1").get()).get("ownerId"), "deleted-account");
  assert.equal((await db.doc("codes/code-pending").get()).exists, false);
  assert.equal((await db.doc("codes/code-used").get()).get("userId"), "deleted-account");
  assert.equal((await db.doc("orders/order-1").get()).get("supporterId"), "deleted-account");
  assert.equal((await db.doc("orders/order-1").get()).get("supporterName"), "Silinmiş kullanıcı");
  assert.equal((await db.doc("community_access/delete@example.com").get()).exists, false);
  assert.equal((await db.doc("organizations/community-1").get()).get("createdBy"), "deleted-account");
  assert.equal((await db.doc("campaignClaims/campaign-1_delete-me").get()).exists, false);
  assert.equal((await db.doc("campaignCodes/ABC12345").get()).get("studentId"), "deleted-account");
  assert.equal((await db.doc("redemptions/ABC12345").get()).get("studentId"), "deleted-account");
  assert.equal((await db.doc("redemptions/ABC12345").get()).get("redeemedBy"), "deleted-account");
  assert.equal((await db.doc("feedbackSubmissions/feedback-1").get()).exists, false);
  assert.equal((await db.doc(`eduVerifications/${uid}`).get()).exists, false);
  assert.equal((await db.doc("eduEmailClaims/claim-1").get()).exists, false);
  assert.equal((await db.doc("mail/verification-1").get()).exists, false);
  assert.equal((await db.doc("legacyTestRedemptions/123456").get()).get("legacyStudentId"), "deleted-account");
  assert.equal((await db.doc("legacyTestRedemptions/123456").get()).get("redeemedBy"), "deleted-account");
  assert.equal((await db.doc("auditLogs/account-action").get()).get("actorUid"), "deleted-account");
  assert.equal((await db.doc("auditLogs/edu-verified").get()).get("targetId"), "deleted-account");
  assert.equal((await db.doc("auditLogs/member-assigned").get()).get("metadata.userId"), "deleted-account");
  assert.equal((await db.doc("auditLogs/member-assigned").get()).get("targetId"), "community-1:deleted-account");
  assert.equal((await legacyTestDb.doc("communities/community-1/entries/coupon-1/claims/delete-me").get()).exists, false);
  assert.equal((await legacyTestDb.doc("community_coupon_codes/111111").get()).exists, false);
  assert.equal((await legacyTestDb.doc("community_coupon_codes/222222").get()).get("userId"), "deleted-account");
  assert.equal((await legacyTestDb.doc("community_access/delete@example.com").get()).exists, false);
});
