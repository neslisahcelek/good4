import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { after, beforeEach, test } from "node:test";
import { db, legacyTestDb } from "./firebase.js";
import { beginAccountDeletion, eraseAccountData } from "./accountDeletion.js";
import { reviewCommunityApplicationService } from "./communityApplications.js";

const uid = "delete-me";

beforeEach(async () => {
  await Promise.all([
    ...[
      "app_config", "users", "organizations", "events", "campaignClaims", "campaignCodes",
      "businessDiscountClaims", "businessDiscountRedemptions", "businessDiscounts", "communityCouponClaims", "communityCouponCodes", "communityCouponRedemptions", "communityCoupons", "redemptions", "feedbackSubmissions", "auditLogs", "legacyTestRedemptions",
      "communities", "codes", "orders", "businesses", "community_access",
      "eduEmailClaims", "eduVerifications", "campusEmailVerifications", "mail", "communityApplications",
      "pushDevices", "notificationJobs", "notificationDispatches", "rateLimits",
    ].map((collection) => db.recursiveDelete(db.collection(collection))),
    ...["communities", "community_coupon_codes", "community_access", "users"].map((collection) =>
      legacyTestDb.recursiveDelete(legacyTestDb.collection(collection))),
  ]);
});

after(async () => {
  await Promise.all([db.terminate(), legacyTestDb.terminate()]);
});

for (const status of ["pending", "rejected", "approved"]) {
  test(`deletes ${status} community applications even without a user profile`, async () => {
    await db.doc(`communityApplications/${uid}`).set({
      applicantUid: uid,
      applicantName: "Applicant",
      applicantEmail: "delete@example.com",
      communityName: "Deleted club",
      university: "Akdeniz Üniversitesi",
      status,
    });

    await eraseAccountData(db, legacyTestDb, uid, "delete@example.com");

    assert.equal((await db.doc(`communityApplications/${uid}`).get()).exists, false);
    await db.doc("users/admin-1").set({ role: "good4Admin", status: "active" });
    await assert.rejects(reviewCommunityApplicationService(db, "admin-1", {
      applicationId: uid, decision: "approve",
    }), /APPLICATION_NOT_FOUND/);
    assert.equal((await db.collection("organizations").get()).empty, true);
    assert.equal((await db.doc(`users/${uid}`).get()).exists, false);
  });
}

test("deletes personal records and removes account identifiers from retained V2 history", async () => {
  const accountMailKeys = [`eduMail:account:${uid}`, `eduMailRequest:${uid}`];
  const sharedMailKeys = ["eduMail:recipient:recipient-hash", "eduMail:total"];
  const mailLimitRef = (key: string) => db.doc(`rateLimits/${createHash("sha256").update(key).digest("hex")}`);
  await Promise.all([
    ...[...accountMailKeys, ...sharedMailKeys].map((key) => mailLimitRef(key).set({ key, count: 2 })),
    db.doc(`users/${uid}`).set({ email: "delete@example.com", status: "active" }),
    db.doc(`communityApplications/${uid}`).set({ applicantUid: uid, applicantEmail: "delete@example.com", status: "approved" }),
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
    db.doc(`campusEmailVerifications/${uid}`).set({ email: "delete@ogr.akdeniz.edu.tr", requestHash: "hashed" }),
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

  for (const key of accountMailKeys) assert.equal((await mailLimitRef(key).get()).exists, false);
  for (const key of sharedMailKeys) assert.equal((await mailLimitRef(key).get()).get("count"), 2);
  assert.equal((await db.doc(`users/${uid}`).get()).exists, false);
  assert.equal((await db.doc(`communityApplications/${uid}`).get()).exists, false);
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
  assert.equal((await db.doc(`campusEmailVerifications/${uid}`).get()).exists, false);
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


test("account deletion removes push endpoints and private delivery receipts and anonymizes sender history", async () => {
  await db.doc(`users/${uid}`).set({ status: "active", role: "student" });
  await db.doc(`users/${uid}/notifications/n`).set({ title: "Notice" });
  await db.doc(`users/${uid}/notificationPreferences/default`).set({ reminders: true });
  await db.doc("pushDevices/phone").set({ uid, token: "private-token" });
  await db.doc("notificationJobs/job").set({ actorUid: uid });
  await db.doc(`notificationJobs/job/deliveries/${uid}`).set({ uid, done: true });
  await db.doc(`notificationJobs/job/deliveries/${uid}/devices/phone`).set({ outcome: "accepted" });
  await eraseAccountData(db, legacyTestDb, uid);
  assert.equal((await db.doc("pushDevices/phone").get()).exists, false);
  assert.equal((await db.doc(`notificationJobs/job/deliveries/${uid}/devices/phone`).get()).exists, false);
  assert.equal((await db.doc(`users/${uid}/notifications/n`).get()).exists, false);
  assert.equal((await db.doc(`users/${uid}/notificationPreferences/default`).get()).exists, false);
  assert.equal((await db.doc("notificationJobs/job").get()).get("actorUid"), "deleted-account");
});


test("erases community coupon claims and codes and anonymizes retained coupon history", async () => {
  await db.doc(`users/${uid}`).set({status:"active",role:"student"});
  await db.doc(`communityCouponClaims/coupon_${uid}_2026-10-11`).set({studentId:uid});
  await db.doc("communityCouponCodes/business_0001").set({studentId:uid});
  await db.doc("communityCouponCodes/business_0002").set({studentId:"other",redeemedBy:uid});
  await db.doc("communityCouponRedemptions/history").set({studentId:uid,redeemedBy:"staff",title:"Coupon"});
  await db.doc("communityCoupons/retained").set({ title: "Coupon", status: "published" });
  await db.doc("auditLogs/coupon-created").set({ actorUid: uid, targetType: "communityCoupon", targetId: "retained", action: "communityCoupon.created" });
  await eraseAccountData(db,legacyTestDb,uid,null);
  assert.equal((await db.collection("communityCouponClaims").get()).size,0);
  assert.equal((await db.doc("communityCouponCodes/business_0001").get()).exists,false);
  assert.equal((await db.doc("communityCouponCodes/business_0002").get()).exists,true);
  assert.equal((await db.doc("communityCouponCodes/business_0002").get()).get("studentId"),"other");
  assert.equal((await db.doc("communityCouponCodes/business_0002").get()).get("redeemedBy"),"deleted-account");
  const history = await db.doc("communityCouponRedemptions/history").get();
  assert.equal(history.get("studentId"),"deleted-account");
  assert.equal(history.get("title"),"Coupon");
  assert.equal((await db.doc("auditLogs/coupon-created").get()).get("actorUid"), "deleted-account");
  assert.equal((await db.doc("auditLogs/coupon-created").get()).get("action"), "communityCoupon.created");
  assert.equal((await db.doc("communityCoupons/retained").get()).get("createdBy"), undefined);
});


test("account deletion clears its campus test selector without removing another account's selector", async () => {
  await db.doc(`users/${uid}`).set({ role: "good4Admin", status: "active", adminEduTest: { enabled: true } });
  const config = db.doc("app_config/admin_edu_test");
  await config.set({ uid: "another-admin" });
  await beginAccountDeletion(db, uid);
  assert.equal((await config.get()).get("uid"), "another-admin");
  await config.set({ uid });
  await beginAccountDeletion(db, uid);
  assert.equal((await config.get()).exists, false);
});


test("discount account deletion erases claims/shared codes, anonymizes history and preserves public offers", async () => {
  await Promise.all([
    db.doc(`users/${uid}`).set({ role: "student", status: "active" }),
    db.doc("businessDiscounts/discount").set({ status: "published", redemptionCount: 1 }),
    db.doc("businessDiscountClaims/claim").set({ studentId: uid, discountId: "discount" }),
    db.doc("communityCouponCodes/business_0012").set({ source: "businessDiscount", studentId: uid }),
    db.doc("communityCouponCodes/business_0013").set({ source: "businessDiscount", studentId: "survivor", redeemedBy: uid }),
    db.doc("businessDiscountRedemptions/history").set({ studentId: uid, redeemedBy: uid, discountId: "discount", discountLabel: "%20 indirim" }),
    db.doc("auditLogs/discount-created").set({ actorUid: uid, targetId: "discount", action: "businessDiscount.created" }),
  ]);
  await eraseAccountData(db, legacyTestDb, uid);
  assert.equal((await db.doc("businessDiscountClaims/claim").get()).exists, false);
  assert.equal((await db.doc("communityCouponCodes/business_0012").get()).exists, false);
  assert.equal((await db.doc("communityCouponCodes/business_0013").get()).get("studentId"), "survivor");
  assert.equal((await db.doc("communityCouponCodes/business_0013").get()).get("redeemedBy"), "deleted-account");
  const history = await db.doc("businessDiscountRedemptions/history").get();
  assert.equal(history.get("studentId"), "deleted-account");
  assert.equal(history.get("redeemedBy"), "deleted-account");
  assert.equal(history.get("discountLabel"), "%20 indirim");
  assert.equal((await db.doc("businessDiscounts/discount").get()).get("redemptionCount"), 1);
  assert.equal((await db.doc("auditLogs/discount-created").get()).get("actorUid"), "deleted-account");
});
