import assert from "node:assert/strict";
import { after, beforeEach, test } from "node:test";
import { redeemLegacyCouponService } from "./legacyCoupons.js";
import { db } from "./firebase.js";

const now = new Date("2026-09-16T12:00:00.000Z");
const collections = [
  "users", "organizations", "community_coupon_codes", "communities", "legacyTestRedemptions", "auditLogs",
];

beforeEach(async () => {
  for (const collection of collections) {
    await db.recursiveDelete(db.collection(collection));
  }
});

after(async () => {
  await db.terminate();
});

async function seedCoupon(): Promise<void> {
  await Promise.all([
    db.doc("users/business-user").set({ role: "businessOwner", status: "active" }),
    db.doc("organizations/demo-business").set({
      type: "business", status: "active", name: "Demo Business", legacyTestBusinessId: "legacy-business",
    }),
    db.doc("organizations/demo-business/members/business-user").set({
      userId: "business-user", role: "owner", status: "active",
    }),
    db.doc("community_coupon_codes/123456").set({
      value: "123456", communityId: "community-1", entryId: "coupon-1",
      userId: "student-1", businessId: "legacy-business", status: "pending",
      title: "Kahvede %20 indirim", expiresOn: "2026-09-30", usedAt: null,
    }),
    db.doc("communities/community-1/entries/coupon-1").set({
      kind: "coupon", status: "published", businessId: "legacy-business",
      title: "Kahvede %20 indirim", date: "2026-09-30",
    }),
    db.doc("communities/community-1/entries/coupon-1/claims/student-1").set({
      value: "123456", userId: "student-1", businessId: "legacy-business", status: "pending",
    }),
  ]);
}

test("Good4Test code is redeemed once and recorded in V2", async () => {
  await seedCoupon();
  const result = await redeemLegacyCouponService(db, db, "business-user", { code: "123456" }, now);
  assert.equal(result.outcome, "redeemed");
  assert.equal(result.campaignTitle, "Kahvede %20 indirim");
  const [code, claim, audit, history] = await Promise.all([
    db.doc("community_coupon_codes/123456").get(),
    db.doc("communities/community-1/entries/coupon-1/claims/student-1").get(),
    db.doc("legacyTestRedemptions/123456").get(),
    db.doc("auditLogs/legacyCouponRedeemed_123456").get(),
  ]);
  assert.equal(code.get("status"), "used");
  assert.equal(claim.get("status"), "used");
  assert.equal(audit.get("organizationId"), "demo-business");
  assert.equal(history.get("action"), "legacyCoupon.redeemed");
  assert.equal(history.get("targetId"), "123456");
});

test("concurrent redemptions do not use a Good4Test code twice", async () => {
  await seedCoupon();
  const results = await Promise.all([
    redeemLegacyCouponService(db, db, "business-user", { code: "123456" }, now),
    redeemLegacyCouponService(db, db, "business-user", { code: "123456" }, now),
  ]);
  assert.deepEqual(results.map((result) => result.outcome).sort(), ["already_redeemed", "redeemed"]);
});

test("another business cannot redeem the code", async () => {
  await seedCoupon();
  await db.doc("community_coupon_codes/123456").update({ businessId: "other-business" });
  const result = await redeemLegacyCouponService(db, db, "business-user", { code: "123456" }, now);
  assert.equal(result.outcome, "wrong_business");
});

test("expired or unpublished coupons are rejected", async () => {
  await seedCoupon();
  const expired = await redeemLegacyCouponService(
    db, db, "business-user", { code: "123456" }, new Date("2026-10-01T09:00:00.000Z"),
  );
  assert.equal(expired.outcome, "expired");
  await db.doc("communities/community-1/entries/coupon-1").update({ status: "cancelled" });
  const unpublished = await redeemLegacyCouponService(db, db, "business-user", { code: "123456" }, now);
  assert.equal(unpublished.outcome, "campaign_inactive");
});

test("orphan code with a different student claim is rejected as stale", async () => {
  await seedCoupon();
  await db.doc("communities/community-1/entries/coupon-1/claims/student-1").update({ value: "654321" });
  const result = await redeemLegacyCouponService(db, db, "business-user", { code: "123456" }, now);
  assert.equal(result.outcome, "stale_code");
});
