import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { beforeEach, after, test } from "node:test";
import { Timestamp } from "firebase-admin/firestore";
import { db as database } from "./firebase.js";
import {
  saveCommunityCouponService,
  reviewCommunityCouponService,
  endCommunityCouponService,
  listCommunityCouponsService,
  issueCommunityCouponCodeService,
  redeemCommunityCouponCodeService,
  communityCouponDay,
  listBusinessRedemptionsService,
} from "./communityCoupons.js";
const nowMillis = Date.parse("2026-10-11T12:00:00Z");
const input = {
  title: "Kahve kuponu",
  description: "Test kuponu",
  businessId: "business",
  discountType: "percentage",
  discountValue: 20,
  startsAt: "2026-10-10T00:00:00Z",
  endsAt: "2026-10-20T00:00:00Z",
  totalLimit: 50,
};
beforeEach(async () => {
  if (!process.env.FIRESTORE_EMULATOR_HOST?.endsWith(":8395")) {
    throw new Error("Isolated 8395 emulator required");
  }
  await Promise.all(
    [
      "users",
      "organizations",
      "communityCoupons",
      "communityCouponCodes",
      "communityCouponClaims",
      "communityCouponRedemptions",
      "auditLogs",
      "rateLimits",
      "redemptions",
      "campaigns",
    ].map((collectionName) => database.recursiveDelete(database.collection(collectionName))),
  );
  await Promise.all([
    ...["student", "student2", "student3"].map((actorUid) =>
      database.doc(`users/${actorUid}`).set({ role: "student", status: "active" }),
    ),
    database.doc("users/admin").set({ role: "good4Admin", status: "active" }),
    database.doc("users/manager").set({ role: "communityManager", status: "active" }),
    database.doc("users/staff").set({ role: "businessStaff", status: "active" }),
    database.doc("users/staff2").set({ role: "businessStaff", status: "active" }),
    database.doc("users/other").set({ role: "businessOwner", status: "active" }),
  ]);
  for (const [org, type] of [
    ["community", "community"],
    ["business", "business"],
    ["otherbusiness", "business"],
  ]) {
    await database.doc(`organizations/${org}`).set({ type, status: "active", name: org });
  }
  for (const [org, actorUid, role] of [
    ["community", "manager", "manager"],
    ["business", "staff", "staff"],
    ["business", "staff2", "staff"],
    ["otherbusiness", "other", "owner"],
  ]) {
    await database
      .doc(`organizations/${org}/members/${actorUid}`)
      .set({ userId: actorUid, role, status: "active" });
  }
});
after(() => database.terminate());
async function published(overrides: Record<string, unknown> = {}) {
  const result = await saveCommunityCouponService(database, "manager", { ...input, ...overrides });
  await reviewCommunityCouponService(database, "admin", {
    couponId: result.couponId,
    decision: "approve",
  });
  return result.couponId;
}
test("manager creates pending coupon, admin publishes, unverified non-follower student issues and business redeems without PII", async () => {
  const couponId = await published();
  const result = await issueCommunityCouponCodeService(
    database,
    "student",
    { couponId },
    nowMillis,
    () => "0001",
  );
  assert.equal(result.code, "0001");
  assert.equal(Date.parse(result.expiresAt!) - nowMillis, 600000);
  const used = await redeemCommunityCouponCodeService(
    database,
    "staff",
    { code: result.code },
    nowMillis,
  );
  assert.deepEqual(used, {
    outcome: "redeemed",
    couponTitle: input.title,
    communityName: "community",
    discountLabel: "%20 indirim",
  });
  assert.equal(
    (await issueCommunityCouponCodeService(database, "student", { couponId }, nowMillis)).outcome,
    "already_used_today",
  );
  assert.equal(
    (await database.doc(`communityCoupons/${couponId}`).get()).get("redemptionCount"),
    1,
  );
});
test("same live claim is idempotent including concurrent issuance", async () => {
  const couponId = await published();
  const results = await Promise.all([
    issueCommunityCouponCodeService(database, "student", { couponId }, nowMillis),
    issueCommunityCouponCodeService(database, "student", { couponId }, nowMillis),
  ]);
  assert.equal(results[0].code, results[1].code);
  assert.equal((await database.collection("communityCouponCodes").get()).size, 1);
});
test("same coupon can be redeemed again on the next Istanbul day", async () => {
  const couponId = await published();
  const firstCode = await issueCommunityCouponCodeService(
    database,
    "student",
    { couponId },
    nowMillis,
  );
  await redeemCommunityCouponCodeService(database, "staff", { code: firstCode.code }, nowMillis);
  const secondCode = await issueCommunityCouponCodeService(
    database,
    "student",
    { couponId },
    nowMillis + 86400000,
  );
  assert.equal(secondCode.outcome, "issued");
  assert.equal(
    (
      await redeemCommunityCouponCodeService(
        database,
        "staff",
        { code: secondCode.code },
        nowMillis + 86400000,
      )
    ).outcome,
    "redeemed",
  );
});
test("Istanbul day boundary and crossing-midnight codes count the redemption day", async () => {
  assert.equal(communityCouponDay(Date.parse("2026-10-11T20:59:59Z")), "2026-10-11");
  assert.equal(communityCouponDay(Date.parse("2026-10-11T21:00:00Z")), "2026-10-12");
  const couponId = await published();
  const redemptionTime = Date.parse("2026-10-11T20:59:00Z");
  const firstCode = await issueCommunityCouponCodeService(
    database,
    "student",
    { couponId },
    redemptionTime,
  );
  assert.equal(
    (
      await redeemCommunityCouponCodeService(
        database,
        "staff",
        { code: firstCode.code },
        redemptionTime + 120000,
      )
    ).outcome,
    "redeemed",
  );
  assert.equal(
    (
      await issueCommunityCouponCodeService(
        database,
        "student",
        { couponId },
        redemptionTime + 120000,
      )
    ).outcome,
    "already_used_today",
  );
});
test("concurrent redemptions consume a code once", async () => {
  const couponId = await published();
  const result = await issueCommunityCouponCodeService(
    database,
    "student",
    { couponId },
    nowMillis,
  );
  const results = await Promise.all([
    redeemCommunityCouponCodeService(database, "staff", { code: result.code }, nowMillis),
    redeemCommunityCouponCodeService(database, "staff2", { code: result.code }, nowMillis),
  ]);
  assert.deepEqual(results.map((result) => result.outcome).sort(), [
    "already_redeemed",
    "redeemed",
  ]);
  assert.equal((await database.collection("communityCouponRedemptions").get()).size, 1);
});
for (const [limit, overrides, outcome] of [
  ["total", { totalLimit: 1 }, "limit_reached"],
  ["daily", { dailyLimit: 1 }, "daily_limit_reached"],
] as const) {
  test(`${limit} limits checked at issuance and concurrent redemption`, async () => {
    const couponId = await published(overrides);
    const firstCode = await issueCommunityCouponCodeService(
      database,
      "student",
      { couponId },
      nowMillis,
    );
    const secondCode = await issueCommunityCouponCodeService(
      database,
      "student2",
      { couponId },
      nowMillis,
    );
    const results = await Promise.all([
      redeemCommunityCouponCodeService(database, "staff", { code: firstCode.code }, nowMillis),
      redeemCommunityCouponCodeService(database, "staff2", { code: secondCode.code }, nowMillis),
    ]);
    assert.deepEqual(results.map((result) => result.outcome).sort(), [outcome, "redeemed"].sort());
    assert.equal(
      (await issueCommunityCouponCodeService(database, "student3", { couponId }, nowMillis))
        .outcome,
      outcome,
    );
  });
}
test("daily coupon count resets on next calendar day", async () => {
  const couponId = await published({ dailyLimit: 1 });
  const firstCode = await issueCommunityCouponCodeService(
    database,
    "student",
    { couponId },
    nowMillis,
  );
  await redeemCommunityCouponCodeService(database, "staff", { code: firstCode.code }, nowMillis);
  assert.equal(
    (
      await issueCommunityCouponCodeService(
        database,
        "student2",
        { couponId },
        nowMillis + 86400000,
      )
    ).outcome,
    "issued",
  );
});
test("business scoped codes never match another business", async () => {
  const couponId = await published();
  const firstCode = await issueCommunityCouponCodeService(
    database,
    "student",
    { couponId },
    nowMillis,
    () => "1234",
  );
  assert.equal(
    (await redeemCommunityCouponCodeService(database, "other", { code: firstCode.code }, nowMillis))
      .outcome,
    "not_found",
  );
  const other = await published({ businessId: "otherbusiness" });
  assert.equal(
    (
      await issueCommunityCouponCodeService(
        database,
        "student2",
        { couponId: other },
        nowMillis,
        () => "1234",
      )
    ).code,
    "1234",
  );
  assert.equal(
    (await redeemCommunityCouponCodeService(database, "other", { code: "1234" }, nowMillis))
      .outcome,
    "redeemed",
  );
});
test("live issued collision skips a code; expired code is reusable", async () => {
  const couponId = await published();
  await issueCommunityCouponCodeService(database, "student", { couponId }, nowMillis, () => "1111");
  let calls = 0;
  const secondCode = await issueCommunityCouponCodeService(
    database,
    "student2",
    { couponId },
    nowMillis,
    () => (++calls === 1 ? "1111" : "2222"),
  );
  assert.equal(secondCode.code, "2222");
  const reusedCode = await issueCommunityCouponCodeService(
    database,
    "student3",
    { couponId },
    nowMillis + 600000,
    () => "1111",
  );
  assert.equal(reusedCode.code, "1111");
});
test("exhausted candidate space returns explicit busy error", async () => {
  const couponId = await published();
  await issueCommunityCouponCodeService(database, "student", { couponId }, nowMillis, () => "1111");
  await assert.rejects(
    issueCommunityCouponCodeService(database, "student2", { couponId }, nowMillis, () => "1111"),
    /CODE_SPACE_BUSY/,
  );
});
test("expired code returns expired; owner can end a published coupon", async () => {
  const couponId = await published();
  const result = await issueCommunityCouponCodeService(
    database,
    "student",
    { couponId },
    nowMillis,
  );
  assert.equal(
    (
      await redeemCommunityCouponCodeService(
        database,
        "staff",
        { code: result.code },
        nowMillis + 600000,
      )
    ).outcome,
    "expired",
  );
  await endCommunityCouponService(database, "manager", { couponId });
  assert.equal(
    (await issueCommunityCouponCodeService(database, "student2", { couponId }, nowMillis)).outcome,
    "coupon_inactive",
  );
});
test("ended coupon rejects already issued live code", async () => {
  const couponId = await published();
  const result = await issueCommunityCouponCodeService(
    database,
    "student",
    { couponId },
    nowMillis,
  );
  await endCommunityCouponService(database, "manager", { couponId });
  assert.equal(
    (await redeemCommunityCouponCodeService(database, "staff", { code: result.code }, nowMillis))
      .outcome,
    "coupon_inactive",
  );
});
test("date range controls issuance and redemption", async () => {
  const couponId = await published();
  assert.equal(
    (
      await issueCommunityCouponCodeService(
        database,
        "student",
        { couponId },
        Date.parse(input.startsAt) - 1,
      )
    ).outcome,
    "coupon_inactive",
  );
  const result = await issueCommunityCouponCodeService(
    database,
    "student",
    { couponId },
    Date.parse(input.endsAt) - 1000,
  );
  assert.equal(
    (
      await redeemCommunityCouponCodeService(
        database,
        "staff",
        { code: result.code },
        Date.parse(input.endsAt),
      )
    ).outcome,
    "coupon_inactive",
  );
});
test("rejection requires reason and editing returns to pending", async () => {
  const result = await saveCommunityCouponService(database, "manager", input);
  await assert.rejects(
    reviewCommunityCouponService(database, "admin", {
      couponId: result.couponId,
      decision: "reject",
    }),
    /REASON_REQUIRED/,
  );
  await reviewCommunityCouponService(database, "admin", {
    couponId: result.couponId,
    decision: "reject",
    reason: "Açıklama eksik",
  });
  assert.equal(
    (await listCommunityCouponsService(database, "manager")).coupons[0]?.rejectionReason,
    "Açıklama eksik",
  );
  await saveCommunityCouponService(database, "manager", { ...input, couponId: result.couponId });
  assert.equal(
    (await database.doc(`communityCoupons/${result.couponId}`).get()).get("status"),
    "pending",
  );
  assert.equal((await database.collection("auditLogs").get()).size, 2);
});
test("published coupon cannot be edited", async () => {
  const couponId = await published();
  await assert.rejects(
    saveCommunityCouponService(database, "manager", { ...input, couponId: couponId }),
    /COUPON_NOT_EDITABLE/,
  );
});
test("foreign community ownership and spoofed organization inputs rejected or ignored", async () => {
  await database.doc("organizations/foreign").set({ type: "community", status: "active" });
  await database.doc("communityCoupons/foreign").set({ communityId: "foreign", status: "pending" });
  await assert.rejects(
    endCommunityCouponService(database, "manager", { couponId: "foreign" }),
    /COUPON_NOT_OWNED/,
  );
  await assert.rejects(
    saveCommunityCouponService(database, "manager", { ...input, couponId: "foreign" }),
    /COUPON_NOT_EDITABLE/,
  );
  const result = await saveCommunityCouponService(database, "manager", {
    ...input,
    communityId: "foreign",
    uid: "admin",
  });
  assert.equal(
    (await database.doc(`communityCoupons/${result.couponId}`).get()).get("communityId"),
    "community",
  );
});
for (const [actorUid, status, role] of [
  ["student", "disabled", "student"],
  ["student", "deleting", "student"],
  ["student", "active", "businessStaff"],
] as const) {
  test(`rejects actor ${status}/${role}`, async () => {
    const couponId = await published();
    await database.doc(`users/${actorUid}`).update({ status, role });
    await assert.rejects(
      issueCommunityCouponCodeService(database, actorUid, { couponId }, nowMillis),
      /ACCOUNT_NOT_ACTIVE|ROLE_NOT_ALLOWED/,
    );
  });
}
test("only admin reviews and only managers create/list coupons", async () => {
  const result = await saveCommunityCouponService(database, "manager", input);
  await assert.rejects(
    reviewCommunityCouponService(database, "manager", {
      couponId: result.couponId,
      decision: "approve",
    }),
    /ROLE_NOT_ALLOWED/,
  );
  await assert.rejects(saveCommunityCouponService(database, "student", input), /ROLE_NOT_ALLOWED/);
  await assert.rejects(listCommunityCouponsService(database, "student"), /ROLE_NOT_ALLOWED/);
});
test("disabled membership and business prevent coupon use", async () => {
  const couponId = await published();
  const result = await issueCommunityCouponCodeService(
    database,
    "student",
    { couponId },
    nowMillis,
  );
  await database.doc("organizations/business/members/staff").update({ status: "disabled" });
  await assert.rejects(
    redeemCommunityCouponCodeService(database, "staff", { code: result.code }, nowMillis),
    /ORGANIZATION_MEMBERSHIP_REQUIRED/,
  );
  await database.doc("organizations/business").update({ status: "disabled" });
  assert.equal(
    (await issueCommunityCouponCodeService(database, "student2", { couponId }, nowMillis)).outcome,
    "coupon_inactive",
  );
});
test("inactive students cannot redeem an earlier issued code", async () => {
  const couponId = await published();
  const result = await issueCommunityCouponCodeService(
    database,
    "student",
    { couponId },
    nowMillis,
  );
  await database.doc("users/student").update({ status: "deleting" });
  assert.equal(
    (await redeemCommunityCouponCodeService(database, "staff", { code: result.code }, nowMillis))
      .outcome,
    "student_inactive",
  );
});
test("failed attempt throttle shared across staff in a business and expires", async () => {
  for (let i = 0; i < 4; i++)
    assert.equal(
      (
        await redeemCommunityCouponCodeService(
          database,
          i % 2 ? "staff2" : "staff",
          { code: "9999" },
          nowMillis + i,
        )
      ).outcome,
      "not_found",
    );
  await assert.rejects(
    redeemCommunityCouponCodeService(database, "staff2", { code: "9999" }, nowMillis + 4),
    /TOO_MANY_FAILED_ATTEMPTS/,
  );
  await assert.rejects(
    redeemCommunityCouponCodeService(database, "staff", { code: "9999" }, nowMillis + 5),
    /TOO_MANY_FAILED_ATTEMPTS/,
  );
  assert.equal(
    (
      await redeemCommunityCouponCodeService(
        database,
        "staff",
        { code: "9999" },
        nowMillis + 300005,
      )
    ).outcome,
    "not_found",
  );
});
test("invalid input lengths, types, domains, limits and discount fields rejected", async () => {
  for (const overrides of [
    { title: "x".repeat(121) },
    { businessId: "bad/path" },
    { totalLimit: 0 },
    { totalLimit: "50" },
    { dailyLimit: 51 },
    { discountValue: 101 },
    { discountType: "unknown" },
    { discountType: "freeItem", freeItem: "" },
    { endsAt: input.startsAt },
  ]) {
    await assert.rejects(
      saveCommunityCouponService(database, "manager", { ...input, ...overrides }),
    );
  }
  await assert.rejects(
    redeemCommunityCouponCodeService(database, "staff", { code: "12345" }),
    /CODE_INVALID/,
  );
});
for (const [discountType, overrides, label] of [
  ["fixed", { discountValue: 50 }, "50 TL indirim"],
  ["freeItem", { freeItem: "Filtre kahve" }, "Ücretsiz: Filtre kahve"],
] as const) {
  test(`${discountType} returns readable discount`, async () => {
    const couponId = await published({ discountType, ...overrides });
    const result = await issueCommunityCouponCodeService(
      database,
      "student",
      { couponId },
      nowMillis,
    );
    const used = await redeemCommunityCouponCodeService(
      database,
      "staff",
      { code: result.code },
      nowMillis,
    );
    assert.ok("discountLabel" in used);
    assert.equal(used.discountLabel, label);
  });
}
test("business history merges both sources without student or staff data", async () => {
  const couponId = await published();
  const result = await issueCommunityCouponCodeService(
    database,
    "student",
    { couponId },
    nowMillis,
  );
  await redeemCommunityCouponCodeService(database, "staff", { code: result.code }, nowMillis);
  await database.doc("campaigns/meal").set({ title: "Meal" });
  await database.doc("redemptions/meal-code").set({
    organizationId: "business",
    campaignId: "meal",
    studentId: "secret",
    redeemedAt: Timestamp.fromMillis(nowMillis - 1),
  });
  const history = await listBusinessRedemptionsService(database, "staff");
  assert.deepEqual(
    history.redemptions.map((result) => result.source),
    ["communityCoupon", "campaign"],
  );
  assert.ok(
    history.redemptions.every((result) => !("studentId" in result) && !("redeemedBy" in result)),
  );
});
test("community staff can read their own coupon list but cannot create or end coupons", async () => {
  const couponId = await published();
  await database.doc("users/community-staff").set({ role: "communityStaff", status: "active" });
  await database
    .doc("organizations/community/members/community-staff")
    .set({ userId: "community-staff", role: "staff", status: "active" });
  assert.equal(
    (await listCommunityCouponsService(database, "community-staff")).coupons[0]?.id,
    couponId,
  );
  await assert.rejects(
    saveCommunityCouponService(database, "community-staff", input),
    /ROLE_NOT_ALLOWED/,
  );
  await assert.rejects(
    endCommunityCouponService(database, "community-staff", { couponId }),
    /ROLE_NOT_ALLOWED/,
  );
});
for (const outcome of [
  "expired",
  "already_redeemed",
  "coupon_inactive",
  "limit_reached",
  "daily_limit_reached",
  "already_used_today",
  "student_inactive",
]) {
  test(`${outcome} does not increment or clear the business failed-attempt counter`, async () => {
    const couponId = await published({ dailyLimit: 1 });
    const issued = await issueCommunityCouponCodeService(
      database,
      "student",
      { couponId },
      nowMillis,
      () => "1234",
    );
    await redeemCommunityCouponCodeService(database, "staff", { code: "9999" }, nowMillis);
    const counterId = createHash("sha256").update("communityCouponFail:business").digest("hex");
    const counterRef = database.doc(`rateLimits/${counterId}`);
    const before = (await counterRef.get()).data();
    let redemptionTime = nowMillis;
    if (outcome === "expired") {
      redemptionTime += 600000;
    }
    if (outcome === "already_redeemed") {
      await database.doc("communityCouponCodes/business_1234").update({ status: "redeemed" });
    }
    if (outcome === "coupon_inactive") {
      await endCommunityCouponService(database, "manager", { couponId });
    }
    if (outcome === "limit_reached") {
      await database.doc(`communityCoupons/${couponId}`).update({ redemptionCount: 50 });
    }
    if (outcome === "daily_limit_reached") {
      await database
        .doc(`communityCoupons/${couponId}`)
        .update({ dailyRedemptionDay: communityCouponDay(nowMillis), dailyRedemptionCount: 1 });
    }
    if (outcome === "already_used_today") {
      await database
        .doc(`communityCouponClaims/${couponId}_student_${communityCouponDay(nowMillis)}`)
        .update({ status: "redeemed" });
    }
    if (outcome === "student_inactive") {
      await database.doc("users/student").update({ status: "disabled" });
    }
    for (let attempt = 0; attempt < 6; attempt += 1) {
      assert.equal(
        (
          await redeemCommunityCouponCodeService(
            database,
            "staff",
            { code: issued.code },
            redemptionTime,
          )
        ).outcome,
        outcome,
      );
    }
    assert.deepEqual((await counterRef.get()).data(), before);
    // The sixth known-code attempt did not block the business.
    assert.equal(
      (await redeemCommunityCouponCodeService(database, "staff2", { code: "9999" }, nowMillis + 1))
        .outcome,
      "not_found",
    );
  });
}
test("five not_found results block every staff member of the business", async () => {
  for (let attempt = 0; attempt < 4; attempt += 1) {
    assert.equal(
      (
        await redeemCommunityCouponCodeService(
          database,
          "staff",
          { code: "9999" },
          nowMillis + attempt,
        )
      ).outcome,
      "not_found",
    );
  }
  await assert.rejects(
    redeemCommunityCouponCodeService(database, "staff2", { code: "9999" }, nowMillis + 4),
    /TOO_MANY_FAILED_ATTEMPTS/,
  );
  await assert.rejects(
    redeemCommunityCouponCodeService(database, "staff", { code: "9999" }, nowMillis + 5),
    /TOO_MANY_FAILED_ATTEMPTS/,
  );
});
test("coupon creator is retained in a private creation audit rather than the public document", async () => {
  const couponId = await published();
  assert.equal(
    (await database.doc(`communityCoupons/${couponId}`).get()).get("createdBy"),
    undefined,
  );
  const audit = await database
    .collection("auditLogs")
    .where("action", "==", "communityCoupon.created")
    .get();
  assert.equal(audit.size, 1);
  assert.equal(audit.docs[0]?.get("actorUid"), "manager");
  assert.equal(audit.docs[0]?.get("targetId"), couponId);
});
test("publishing an older pending coupon moves its creator into a private audit", async () => {
  const saved = await saveCommunityCouponService(database, "manager", input);
  await database
    .doc(`communityCoupons/${saved.couponId}`)
    .update({ createdBy: "original-manager" });
  await reviewCommunityCouponService(database, "admin", {
    couponId: saved.couponId,
    decision: "approve",
  });
  assert.equal(
    (await database.doc(`communityCoupons/${saved.couponId}`).get()).get("createdBy"),
    undefined,
  );
  const audit = await database
    .collection("auditLogs")
    .where("actorUid", "==", "original-manager")
    .get();
  assert.equal(audit.docs[0]?.get("action"), "communityCoupon.created");
});
