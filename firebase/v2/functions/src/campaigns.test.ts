import assert from "node:assert/strict";
import { after, beforeEach, test } from "node:test";
import { FieldValue, Timestamp } from "firebase-admin/firestore";
import { endCampaignService } from "./admin.js";
import {
  issueCampaignCodeService,
  redeemCampaignCodeService,
} from "./campaigns.js";
import { db } from "./firebase.js";

const topLevelCollections = [
  "users",
  "organizations",
  "campaigns",
  "campaignCodes",
  "campaignClaims",
  "redemptions",
  "auditLogs",
];

beforeEach(async () => {
  await Promise.all(topLevelCollections.map(async (collection) => {
    await db.recursiveDelete(db.collection(collection));
  }));
});

after(async () => {
  await db.terminate();
});

async function seedBase(): Promise<void> {
  const now = Date.now();
  await Promise.all([
    db.doc("users/student-1").set({ role: "student", status: "active", eduVerified: true, eduEmail: "one@ogr.akdeniz.edu.tr" }),
    db.doc("users/student-2").set({ role: "student", status: "active", eduVerified: true, eduEmail: "two@ogr.akdeniz.edu.tr" }),
    db.doc("users/gmail-student").set({ role: "student", status: "active", email: "someone@gmail.com" }),
    db.doc("users/business-1").set({ role: "businessStaff", status: "active" }),
    db.doc("users/outsider-1").set({ role: "businessStaff", status: "active" }),
    db.doc("organizations/business-org-1").set({
      type: "business",
      status: "active",
      name: "Test Business",
    }),
    db.doc("organizations/business-org-1/members/business-1").set({
      role: "staff",
      status: "active",
    }),
    db.doc("campaigns/campaign-1").set({
      organizationId: "business-org-1",
      title: "Student Discount",
      status: "published",
      startsAt: Timestamp.fromMillis(now - 60_000),
      endsAt: Timestamp.fromMillis(now + 3_600_000),
      totalLimit: null,
      redemptionCount: 0,
      updatedAt: FieldValue.serverTimestamp(),
    }),
  ]);
}

test("a student can issue and a business staff member can redeem a code", async () => {
  await seedBase();
  const issued = await issueCampaignCodeService(db, "student-1", {
    campaignId: "campaign-1",
  });
  assert.equal(issued.outcome, "issued");
  assert.ok(issued.code);

  const redeemed = await redeemCampaignCodeService(db, "business-1", {
    code: issued.code,
  });
  assert.deepEqual(redeemed, {
    outcome: "redeemed",
    campaignId: "campaign-1",
    campaignTitle: "Student Discount",
  });
});

test("concurrent redemption attempts consume the code only once", async () => {
  await seedBase();
  const issued = await issueCampaignCodeService(db, "student-1", {
    campaignId: "campaign-1",
  });
  assert.ok(issued.code);

  const outcomes = await Promise.all([
    redeemCampaignCodeService(db, "business-1", { code: issued.code }),
    redeemCampaignCodeService(db, "business-1", { code: issued.code }),
  ]);
  assert.deepEqual(
    outcomes.map((result) => result.outcome).sort(),
    ["already_redeemed", "redeemed"],
  );

  const campaign = await db.doc("campaigns/campaign-1").get();
  assert.equal(campaign.get("redemptionCount"), 1);
  const redemptions = await db.collection("redemptions").get();
  assert.equal(redemptions.size, 1);
});

test("business staff has no per-staff redemption quota", async () => {
  await seedBase();
  const first = await issueCampaignCodeService(db, "student-1", {
    campaignId: "campaign-1",
  });
  const second = await issueCampaignCodeService(db, "student-2", {
    campaignId: "campaign-1",
  });
  assert.ok(first.code);
  assert.ok(second.code);

  const firstResult = await redeemCampaignCodeService(db, "business-1", {
    code: first.code,
  });
  const secondResult = await redeemCampaignCodeService(db, "business-1", {
    code: second.code,
  });
  assert.equal(firstResult.outcome, "redeemed");
  assert.equal(secondResult.outcome, "redeemed");
});

test("staff outside the target business receives wrong_business", async () => {
  await seedBase();
  const issued = await issueCampaignCodeService(db, "student-1", {
    campaignId: "campaign-1",
  });
  assert.ok(issued.code);

  const result = await redeemCampaignCodeService(db, "outsider-1", {
    code: issued.code,
  });
  assert.equal(result.outcome, "wrong_business");
});

test("expired codes cannot be redeemed", async () => {
  await seedBase();
  await db.doc("campaignCodes/ABC23456").set({
    code: "ABC23456",
    campaignId: "campaign-1",
    organizationId: "business-org-1",
    studentId: "student-1",
    status: "issued",
    expiresAt: Timestamp.fromMillis(Date.now() - 1_000),
  });
  await db.doc("campaignClaims/campaign-1_student-1").set({
    campaignId: "campaign-1",
    studentId: "student-1",
    code: "ABC23456",
    status: "issued",
    expiresAt: Timestamp.fromMillis(Date.now() - 1_000),
  });

  const result = await redeemCampaignCodeService(db, "business-1", {
    code: "ABC-23456",
  });
  assert.equal(result.outcome, "expired");
  const code = await db.doc("campaignCodes/ABC23456").get();
  assert.equal(code.get("status"), "expired");
});

test("students without a verified .edu.tr address cannot issue suspended-meal codes", async () => {
  await seedBase();
  await assert.rejects(
    issueCampaignCodeService(db, "gmail-student", { campaignId: "campaign-1" }),
    (error: { message?: string }) => error.message === "EDU_VERIFICATION_REQUIRED",
  );
  const claims = await db.collection("campaignClaims").get();
  assert.equal(claims.size, 0);
});

test("no code is issued once the campaign limit is used up", async () => {
  await seedBase();
  await db.doc("campaigns/campaign-1").update({ totalLimit: 1, redemptionCount: 1 });
  await assert.rejects(
    () => issueCampaignCodeService(db, "student-1", { campaignId: "campaign-1" }),
    (error: unknown) => error instanceof Error && error.message === "CAMPAIGN_LIMIT_REACHED",
  );
});

test("only a Good4 admin can end a campaign, and ended campaigns issue no codes", async () => {
  await seedBase();
  await db.doc("users/admin-1").set({ role: "good4Admin", status: "active" });
  await assert.rejects(
    () => endCampaignService(db, "student-1", { campaignId: "campaign-1" }),
    (error: unknown) => error instanceof Error && error.message === "ROLE_NOT_ALLOWED",
  );
  await endCampaignService(db, "admin-1", { campaignId: "campaign-1" });
  assert.equal((await db.doc("campaigns/campaign-1").get()).get("status"), "ended");
  await assert.rejects(
    () => issueCampaignCodeService(db, "student-1", { campaignId: "campaign-1" }),
    (error: unknown) => error instanceof Error && error.message === "CAMPAIGN_NOT_PUBLISHED",
  );
});
