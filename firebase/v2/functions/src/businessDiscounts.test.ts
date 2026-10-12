import assert from "node:assert/strict";
import { beforeEach, after, test } from "node:test";
import { Timestamp } from "firebase-admin/firestore";
import { db as database } from "./firebase.js";
import {
  saveBusinessDiscountService,
  endBusinessDiscountService,
  listBusinessDiscountsService,
  issueBusinessDiscountCodeService,
} from "./businessDiscounts.js";
import {
  redeemCommunityCouponCodeService,
  issueCommunityCouponCodeService,
  listBusinessRedemptionsService,
} from "./communityCoupons.js";

const now = Date.parse("2026-10-12T12:00:00Z");
const input = {
  businessId: "business",
  title: "Kahvede indirim",
  conditions: "Sıcak kahvelerde; diğer kampanyalarla birleşmez.",
  discountValue: 20,
  totalLimit: null,
  startsAt: "2026-10-10T00:00:00Z",
  endsAt: "2026-10-20T00:00:00Z",
};
beforeEach(async () => {
  if (
    !/^(localhost|127\.0\.0\.1|\[::1\]):\d+$/.test(
      process.env.FIRESTORE_EMULATOR_HOST ?? "",
    ) ||
    !process.env.GCLOUD_PROJECT?.startsWith("demo-")
  )
    throw new Error("Local demo emulator required");
  await Promise.all(
    [
      "users",
      "organizations",
      "businessDiscounts",
      "businessDiscountClaims",
      "businessDiscountRedemptions",
      "communityCoupons",
      "communityCouponCodes",
      "communityCouponClaims",
      "communityCouponRedemptions",
      "rateLimits",
      "auditLogs",
      "redemptions",
      "campaigns",
    ].map((name) => database.recursiveDelete(database.collection(name))),
  );
  await Promise.all([
    ...["student", "student2"].map((uid) =>
      database
        .doc(`users/${uid}`)
        .set({ status: "active", role: "student", eduVerified: false }),
    ),
    database.doc("users/admin").set({ status: "active", role: "good4Admin" }),
    database
      .doc("users/staff")
      .set({ status: "active", role: "businessStaff" }),
    database
      .doc("users/other")
      .set({ status: "active", role: "businessOwner" }),
    database
      .doc("organizations/business")
      .set({ status: "active", type: "business", name: "Kampüs Kahve" }),
    database
      .doc("organizations/otherbusiness")
      .set({ status: "active", type: "business", name: "Other" }),
    database
      .doc("organizations/business/members/staff")
      .set({ status: "active", role: "staff", userId: "staff" }),
    database
      .doc("organizations/otherbusiness/members/other")
      .set({ status: "active", role: "owner", userId: "other" }),
  ]);
});
after(() => database.terminate());
async function discount(overrides: Record<string, unknown> = {}) {
  return (
    await saveBusinessDiscountService(database, "admin", {
      ...input,
      ...overrides,
    })
  ).discountId;
}
async function issue(
  discountId: string,
  uid = "student",
  code = "0012",
  time = now,
  version = 1,
) {
  return issueBusinessDiscountCodeService(
    database,
    uid,
    { discountId, version },
    time,
    () => code,
  );
}
const redeem = (code: string, time = now, uid = "staff") =>
  redeemCommunityCouponCodeService(database, uid, { code }, time);

test("admin publishes without creator UID; audit owns private attribution; unverified student redeems and history is sanitized", async () => {
  const id = await discount();
  const data = (await database.doc(`businessDiscounts/${id}`).get()).data()!;
  assert.equal(data.createdBy, undefined);
  assert.equal(
    (await database.collection("auditLogs").get()).docs[0]!.get("actorUid"),
    "admin",
  );
  const result = await issue(id);
  assert.equal(result.code, "0012");
  assert.equal(Date.parse(result.expiresAt!) - now, 600000);
  assert.equal((await issue(id)).outcome, "already_issued");
  assert.deepEqual(await redeem("0012"), {
    outcome: "redeemed",
    couponTitle: input.title,
    communityName: "",
    discountLabel: "%20 indirim",
    discountConditions: input.conditions,
  });
  const history = await listBusinessRedemptionsService(database, "staff");
  assert.equal(history.redemptions[0]!.source, "businessDiscount");
  assert.equal(JSON.stringify(history).includes("student"), false);
});
for (const actor of ["student", "staff"]) {
  test(`non-admin ${actor} cannot create/list/end discounts`, async () => {
    const id = await discount();
    await assert.rejects(saveBusinessDiscountService(database, actor, input));
    await assert.rejects(listBusinessDiscountsService(database, actor));
    await assert.rejects(
      endBusinessDiscountService(database, actor, { discountId: id }),
    );
  });
}
test("requires active approved business and valid percentage, terms, dates and optional quota", async () => {
  for (const invalid of [
    { discountValue: 0 },
    { discountValue: 101 },
    { discountValue: 1.5 },
    { totalLimit: 0 },
    { totalLimit: -1 },
    { conditions: "" },
    { startsAt: "2026-02-30T00:00:00Z" },
    { startsAt: input.endsAt },
    { businessId: "missing" },
  ]) {
    await assert.rejects(discount(invalid));
  }
  await database.doc("organizations/business").update({ status: "pending" });
  await assert.rejects(discount());
});
test("once per student per Istanbul day, counter increments only on successful redemption", async () => {
  const id = await discount();
  await issue(id);
  await redeem("0012");
  assert.equal((await redeem("0012")).outcome, "already_redeemed");
  assert.equal((await issue(id)).outcome, "already_used_today");
  await issue(id, "student", "0013", now + 86400000);
  assert.equal((await redeem("0013", now + 86400000)).outcome, "redeemed");
  assert.equal(
    (await database.doc(`businessDiscounts/${id}`).get()).get(
      "redemptionCount",
    ),
    2,
  );
});
test("live code across Istanbul midnight counts on redemption day", async () => {
  const id = await discount();
  const before = Date.parse("2026-10-12T20:59:00Z");
  const after = Date.parse("2026-10-12T21:01:00Z");
  await issue(id, "student", "0001", before);
  assert.equal((await redeem("0001", after)).outcome, "redeemed");
  assert.equal(
    (await issue(id, "student", "0002", after)).outcome,
    "already_used_today",
  );
});
test("concurrent redemption spends a code exactly once", async () => {
  const id = await discount();
  await issue(id);
  const results = await Promise.all([redeem("0012"), redeem("0012")]);
  assert.equal(
    results.filter((result) => result.outcome === "redeemed").length,
    1,
  );
  assert.equal(
    (await database.doc(`businessDiscounts/${id}`).get()).get(
      "redemptionCount",
    ),
    1,
  );
});
test("last optional quota is enforced transactionally; exhausted offers do not block business", async () => {
  const id = await discount({ totalLimit: 1 });
  await issue(id);
  await issue(id, "student2", "0013");
  const results = await Promise.all([redeem("0012"), redeem("0013")]);
  assert.equal(
    results.filter((result) => result.outcome === "redeemed").length,
    1,
  );
  const unused = results[0].outcome === "redeemed" ? "0013" : "0012";
  for (let attempt = 0; attempt < 6; attempt++)
    assert.equal((await redeem(unused)).outcome, "limit_reached");
  assert.equal(
    (await issue(id, "student2")).outcome,
    results[1].outcome === "redeemed" ? "already_used_today" : "limit_reached",
  );
});
test("unlimited offer allows multiple students and quota cannot fall below usage", async () => {
  const id = await discount();
  await issue(id);
  await redeem("0012");
  await issue(id, "student2", "0013");
  await redeem("0013");
  await assert.rejects(
    saveBusinessDiscountService(database, "admin", {
      ...input,
      discountId: id,
      totalLimit: 1,
    }),
    /LIMIT_BELOW_USAGE/,
  );
  await assert.rejects(
    saveBusinessDiscountService(database, "admin", {
      ...input,
      discountId: id,
      businessId: "otherbusiness",
    }),
    /BUSINESS_NOT_EDITABLE/,
  );
});
for (const mutation of [
  "ended",
  "future",
  "expired",
  "businessInactive",
  "studentInactive",
]) {
  test(`${mutation} prevents redemption without burning attempt limit`, async () => {
    const id = await discount();
    await issue(id);
    const ref = database.doc(`businessDiscounts/${id}`);
    if (mutation === "ended")
      await endBusinessDiscountService(database, "admin", { discountId: id });
    if (mutation === "future")
      await ref.update({ startsAt: Timestamp.fromMillis(now + 1) });
    if (mutation === "expired")
      await ref.update({ endsAt: Timestamp.fromMillis(now) });
    if (mutation === "businessInactive")
      await database
        .doc("organizations/business")
        .update({ status: "suspended" });
    if (mutation === "studentInactive")
      await database.doc("users/student").update({ status: "suspended" });
    for (let attempt = 0; attempt < 6; attempt++) {
      if (mutation === "businessInactive") await assert.rejects(redeem("0012"));
      else
        assert.equal(
          (await redeem("0012")).outcome,
          mutation === "studentInactive"
            ? "student_inactive"
            : "coupon_inactive",
        );
    }
  });
}
test("expired code never redeems and fresh code preserves daily entitlement", async () => {
  const id = await discount();
  await issue(id);
  assert.equal((await redeem("0012", now + 600000)).outcome, "expired");
  assert.equal(
    (await issue(id, "student", "0013", now + 600000)).outcome,
    "issued",
  );
});
test("offer edits invalidate old code/version; refreshed conditions allow a fresh code", async () => {
  const id = await discount();
  await issue(id);
  await saveBusinessDiscountService(database, "admin", {
    ...input,
    discountId: id,
    discountValue: 30,
  });
  assert.equal((await redeem("0012")).outcome, "coupon_inactive");
  assert.equal((await issue(id)).outcome, "coupon_inactive");
  assert.equal((await issue(id, "student", "0013", now, 2)).outcome, "issued");
  const redeemed = await redeem("0013");
  assert.ok("discountLabel" in redeemed);
  assert.equal(redeemed.discountLabel, "%30 indirim");
});
test("shared pool prevents community and discount collisions in both directions", async () => {
  const id = await discount();
  await database
    .doc("organizations/community")
    .set({ type: "community", status: "active" });
  await database.doc("communityCoupons/coupon").set({
    communityId: "community",
    businessId: "business",
    status: "published",
    startsAt: Timestamp.fromMillis(now - 1),
    endsAt: Timestamp.fromMillis(now + 86400000),
    totalLimit: 100,
    redemptionCount: 0,
  });
  await issueCommunityCouponCodeService(
    database,
    "student",
    { couponId: "coupon" },
    now,
    () => "0012",
  );
  const candidates = ["0012", "0013"];
  assert.equal(
    (
      await issueBusinessDiscountCodeService(
        database,
        "student",
        { discountId: id, version: 1 },
        now,
        () => candidates.shift()!,
      )
    ).code,
    "0013",
  );
  const reverse = ["0013", "0014"];
  assert.equal(
    (
      await issueCommunityCouponCodeService(
        database,
        "student2",
        { couponId: "coupon" },
        now,
        () => reverse.shift()!,
      )
    ).code,
    "0014",
  );
  assert.equal((await redeem("0013")).outcome, "redeemed");
});
test("codes are scoped to owning business; five not-found attempts block", async () => {
  const id = await discount();
  await issue(id);
  assert.equal((await redeem("0012", now, "other")).outcome, "not_found");
  for (let attempt = 0; attempt < 4; attempt++)
    assert.equal((await redeem("9999")).outcome, "not_found");
  await assert.rejects(redeem("9999"), /TOO_MANY_FAILED_ATTEMPTS/);
  await assert.rejects(redeem("0012"), /TOO_MANY_FAILED_ATTEMPTS/);
});
test("admin discounts and approved-business picker each paginate independently", async () => {
  const batch = database.batch();
  for (let index = 0; index < 101; index++) {
    const suffix = String(index).padStart(3, "0");
    batch.set(database.doc(`businessDiscounts/d${suffix}`), {
      ...input,
      startsAt: Timestamp.fromMillis(now),
      endsAt: Timestamp.fromMillis(now + 1),
    });
    batch.set(database.doc(`organizations/b${suffix}`), {
      type: "business",
      status: "active",
      name: suffix,
    });
  }
  await batch.commit();
  const page = await listBusinessDiscountsService(database, "admin");
  assert.equal(page.discounts.length, 100);
  assert.equal(page.businesses.length, 100);
  const next = await listBusinessDiscountsService(database, "admin", {
    cursor: page.nextCursor,
    includeBusinesses: false,
  });
  assert.equal(next.discounts.length, 1);
  assert.equal(next.nextCursor, null);
  assert.equal(next.businesses.length, 0);
  const businesses = await listBusinessDiscountsService(database, "admin", {
    businessesOnly: true,
    businessesCursor: page.businessesNextCursor,
  });
  assert.equal(businesses.discounts.length, 0);
  assert.equal(businesses.businesses.length, 3);
});

test("admin saves Maps share and place links, and listing returns the same location", async () => {
  for (const mapsUrl of [
    "https://maps.app.goo.gl/examplePlace",
    "https://goo.gl/maps/examplePlace",
    "https://www.google.com/maps/place/Akdeniz/",
    "https://google.com/maps/search/?api=1&query=Akdeniz",
    "https://www.google.com.tr/maps/place/Akdeniz/",
    "https://maps.google.com/?q=Akdeniz",
  ]) {
    const id = await discount({ mapsUrl: `  ${mapsUrl}  ` });
    assert.equal(
      (await database.doc(`businessDiscounts/${id}`).get()).get("mapsUrl"),
      mapsUrl,
    );
    const page = await listBusinessDiscountsService(database, "admin");
    assert.equal(
      page.discounts.find((offer) => offer.id === id)?.mapsUrl,
      mapsUrl,
    );
  }
});

test("Maps links reject unsafe schemes, misleading domains, credentials, ports and invalid types without publishing", async () => {
  for (const mapsUrl of [
    "http://maps.app.goo.gl/example",
    "javascript:alert(1)",
    "file:///maps",
    "https://maps.app.goo.gl.evil.example/place",
    "https://evil.example/maps/place",
    "https://www.google.com/search?q=place",
    "https://goo.gl/example",
    "https://admin:password@maps.app.goo.gl/example",
    "https://maps.app.goo.gl:8443/example",
    "https://maps.app.goo.gl/",
    "https://maps.app.goo.gl/" + "x".repeat(2048),
    123,
    null,
    {},
  ]) {
    await assert.rejects(
      discount({ mapsUrl }),
      (error: unknown) =>
        error instanceof Error && error.message === "MAPS_URL_INVALID",
    );
  }
  assert.equal((await database.collection("businessDiscounts").get()).size, 0);
});

test("optional Maps link can be changed or cleared; an older client edit preserves it", async () => {
  const original = "https://maps.app.goo.gl/firstPlace";
  const id = await discount({ mapsUrl: original });
  await discount({ discountId: id, title: "Yeni başlık" });
  assert.equal(
    (await database.doc(`businessDiscounts/${id}`).get()).get("mapsUrl"),
    original,
  );
  const replacement = "https://maps.app.goo.gl/secondPlace";
  await discount({ discountId: id, mapsUrl: replacement });
  assert.equal(
    (await database.doc(`businessDiscounts/${id}`).get()).get("mapsUrl"),
    replacement,
  );
  await discount({ discountId: id, mapsUrl: "   " });
  assert.equal(
    (await database.doc(`businessDiscounts/${id}`).get()).get("mapsUrl"),
    "",
  );
});

test("existing offers and new offers without a Maps link remain usable; only admin can change location", async () => {
  const id = await discount();
  assert.equal(
    (await database.doc(`businessDiscounts/${id}`).get()).get("mapsUrl"),
    "",
  );
  await database
    .doc(`businessDiscounts/${id}`)
    .update({ mapsUrl: "https://maps.app.goo.gl/original" });
  await assert.rejects(
    saveBusinessDiscountService(database, "staff", {
      ...input,
      discountId: id,
      mapsUrl: "",
    }),
  );
  assert.equal(
    (await database.doc(`businessDiscounts/${id}`).get()).get("mapsUrl"),
    "https://maps.app.goo.gl/original",
  );
  assert.equal((await issue(id)).outcome, "issued");
});
