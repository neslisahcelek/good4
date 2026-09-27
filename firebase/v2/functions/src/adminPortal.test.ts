import assert from "node:assert/strict";
import { after, beforeEach, test } from "node:test";
import { createOrganizationService } from "./admin.js";
import {
  getAdminDashboardService,
  getPortalContextService,
  reviewLegacyCouponService,
  saveDiningMenuService,
  saveHomeBannerService,
} from "./adminPortal.js";
import { db, legacyTestDb } from "./firebase.js";

const collections = [
  "users", "organizations", "legacyTestBusinessLinks", "businesses", "communities", "auditLogs", "feedbackSubmissions", "app_config",
];

beforeEach(async () => {
  for (const collection of collections) {
    await Promise.all([
      db.recursiveDelete(db.collection(collection)),
      legacyTestDb.recursiveDelete(legacyTestDb.collection(collection)),
    ]);
  }
});

after(async () => {
  await Promise.all([db.terminate(), legacyTestDb.terminate()]);
});

async function seedAdmin(): Promise<void> {
  await db.doc("users/admin-1").set({
    role: "good4Admin",
    status: "active",
    displayName: "Good4 Admin",
  });
}

test("admin dashboard returns pending coupons, businesses and audit rows", async () => {
  await seedAdmin();
  await Promise.all([
    db.doc("organizations/v2-business").set({
      name: "Demo İşletme", type: "business", status: "active", legacyTestBusinessId: "legacy-business",
    }),
    db.doc("organizations/v2-community").set({
      name: "Kadın Girişimciler Topluluğu", type: "community", status: "active",
      university: "Akdeniz Üniversitesi", legacyTestCommunityId: "community-1",
    }),
    legacyTestDb.doc("businesses/legacy-business").set({ name: "Eski Demo" }),
    legacyTestDb.doc("businesses/unlinked-business").set({ name: "Bağlanmamış" }),
    legacyTestDb.doc("communities/community-1").set({ name: "Test Topluluğu" }),
    legacyTestDb.doc("communities/community-1/entries/coupon-1").set({
      kind: "coupon", status: "pending", businessId: "legacy-business", title: "Kahve indirimi",
      description: "Test", date: "2026-09-30", discountType: "percentage", discountValue: 20,
      totalLimit: 50,
    }),
    db.doc("auditLogs/audit-1").set({
      action: "seed.action", actorUid: "admin-1", targetType: "test", targetId: "1",
      metadata: { ok: true }, createdAt: new Date("2026-09-16T09:00:00Z"),
    }),
    legacyTestDb.doc("feedbackSubmissions/feedback-1").set({
      subject: "Ana sayfa önerisi", message: "Kartlar daha sade olabilir.", userId: "student-1",
      userEmail: "student@example.com", userDisplayName: "Demo Öğrenci", status: "new",
      environment: "legacyTest", createdAt: new Date("2026-09-16T10:00:00Z"),
    }),
  ]);

  const dashboard = await getAdminDashboardService(db, legacyTestDb, "admin-1");
  assert.equal(dashboard.pendingCoupons.length, 1);
  assert.equal(dashboard.pendingCoupons[0]?.discountLabel, "%20");
  assert.equal(dashboard.businesses[0]?.id, "v2-business");
  assert.equal(dashboard.communities[0]?.id, "v2-community");
  assert.equal(dashboard.communities[0]?.university, "Akdeniz Üniversitesi");
  assert.equal(dashboard.communities[0]?.legacyTestCommunityId, "community-1");
  assert.equal(dashboard.legacyBusinesses.find((item) => item.id === "legacy-business")?.linked, true);
  assert.equal(dashboard.legacyBusinesses.find((item) => item.id === "unlinked-business")?.linked, false);
  assert.equal(dashboard.audits[0]?.action, "seed.action");
  assert.equal(dashboard.feedback[0]?.subject, "Ana sayfa önerisi");
  assert.equal(dashboard.feedback[0]?.environment, "legacyTest");
});

test("admin approves a pending coupon and audit is written", async () => {
  await seedAdmin();
  await legacyTestDb.doc("communities/community-1/entries/coupon-1").set({
    kind: "coupon", status: "pending", title: "Kahve indirimi",
  });

  const result = await reviewLegacyCouponService(db, legacyTestDb, "admin-1", {
    communityId: "community-1", entryId: "coupon-1", decision: "approve",
  });
  assert.equal(result.status, "published");
  assert.equal((await legacyTestDb.doc("communities/community-1/entries/coupon-1").get()).get("status"), "published");
  assert.equal(
    (await db.doc("auditLogs/legacyCoupon_community-1_coupon-1_approve").get()).get("action"),
    "legacyCoupon.approved",
  );
});

test("legacy business can be linked to only one V2 organization", async () => {
  await seedAdmin();
  const first = await createOrganizationService(db, "admin-1", {
    name: "Birinci", type: "business", legacyTestBusinessId: "legacy-business",
  });
  assert.ok(first.organizationId);
  await assert.rejects(
    () => createOrganizationService(db, "admin-1", {
      name: "İkinci", type: "business", legacyTestBusinessId: "legacy-business",
    }),
    (error: unknown) => error instanceof Error && error.message === "LEGACY_TEST_BUSINESS_ALREADY_LINKED",
  );
});

test("Good4 admin saves the weekly dining menu used by the mobile app", async () => {
  await seedAdmin();
  const saved = await saveDiningMenuService(db, legacyTestDb, "admin-1", {
    weekLabel: "21–25 Eylül 2026",
    weekStart: "2026-09-21",
    weekEnd: "2026-09-25",
    days: [{
      date: "2026-09-21",
      dayName: "Pazartesi",
      meals: ["Mercimek Çorbası", "Fırın Tavuk"],
      calories: 920,
    }],
  });

  assert.equal(saved.weekStart, "2026-09-21");
  assert.deepEqual(saved.days[0]?.meals, ["Mercimek Çorbası", "Fırın Tavuk"]);
  const document = await db.doc("app_config/akdeniz_dining_menu").get();
  const legacyDocument = await legacyTestDb.doc("app_config/akdeniz_dining_menu").get();
  assert.equal(document.get("updatedBy"), "admin-1");
  assert.equal(document.get("days")[0].dayName, "Pazartesi");
  assert.equal(legacyDocument.get("weekStart"), "2026-09-21");
});

test("Good4 admin publishes the home banner to V2 and legacy mobile environments", async () => {
  await seedAdmin();
  const saved = await saveHomeBannerService(db, legacyTestDb, "admin-1", {
    imageUrl: "https://firebasestorage.googleapis.com/banner.jpg",
    advertiserName: "Kampüs Kahve",
    targetUrl: "https://good4tr.com/kampanya",
    startsOn: "2026-09-20",
    endsOn: "2026-09-27",
    active: true,
  });
  assert.equal(saved.advertiserName, "Kampüs Kahve");
  assert.equal(saved.active, true);
  assert.equal((await db.doc("app_config/home_banner").get()).get("imageUrl"), saved.imageUrl);
  assert.equal((await legacyTestDb.doc("app_config/home_banner").get()).get("imageUrl"), saved.imageUrl);
  const dashboard = await getAdminDashboardService(db, legacyTestDb, "admin-1");
  assert.equal(dashboard.homeBanner?.endsOn, "2026-09-27");
});

test("slider banners 2-4 get their own documents and are not mirrored to the legacy app", async () => {
  await seedAdmin();
  await saveHomeBannerService(db, legacyTestDb, "admin-1", {
    imageUrl: "https://firebasestorage.googleapis.com/slot-3.jpg",
    advertiserName: "Kampüs Kitap",
    startsOn: "2026-09-20",
    endsOn: "2026-09-30",
    active: true,
    slot: 3,
  });
  assert.equal((await db.doc("app_config/home_banner_3").get()).get("advertiserName"), "Kampüs Kitap");
  assert.equal((await db.doc("app_config/home_banner").get()).exists, false);
  assert.equal((await legacyTestDb.doc("app_config/home_banner").get()).exists, false);
  const dashboard = await getAdminDashboardService(db, legacyTestDb, "admin-1");
  assert.equal(dashboard.homeBanners.length, 4);
  assert.equal(dashboard.homeBanners[2]?.advertiserName, "Kampüs Kitap");
  await assert.rejects(
    () => saveHomeBannerService(db, legacyTestDb, "admin-1", {
      imageUrl: "https://example.com/x.jpg", advertiserName: "X", startsOn: "2026-09-20", endsOn: "2026-09-30", slot: 5,
    }),
    (error: unknown) => error instanceof Error && error.message === "BANNER_SLOT_INVALID",
  );
});

test("non-admin cannot save the weekly dining menu", async () => {
  await db.doc("users/student-1").set({ role: "student", status: "active" });
  await assert.rejects(
    () => saveDiningMenuService(db, legacyTestDb, "student-1", {
      weekLabel: "21–25 Eylül 2026",
      weekStart: "2026-09-21",
      weekEnd: "2026-09-25",
      days: [{ date: "2026-09-21", dayName: "Pazartesi", meals: ["Çorba"] }],
    }),
    (error: unknown) => error instanceof Error && error.message === "ROLE_NOT_ALLOWED",
  );
});

test("non-admin cannot publish a home banner", async () => {
  await db.doc("users/student-1").set({ role: "student", status: "active" });
  await assert.rejects(
    () => saveHomeBannerService(db, legacyTestDb, "student-1", {
      imageUrl: "https://example.com/banner.jpg",
      advertiserName: "Yetkisiz reklam",
      startsOn: "2026-09-20",
      endsOn: "2026-09-27",
      active: true,
    }),
    (error: unknown) => error instanceof Error && error.message === "ROLE_NOT_ALLOWED",
  );
});

test("portal context routes admins and business users correctly", async () => {
  await seedAdmin();
  assert.deepEqual(await getPortalContextService(db, "admin-1"), {
    portalRole: "admin", displayName: "Good4 Admin",
  });
  await Promise.all([
    db.doc("users/business-1").set({ role: "businessOwner", status: "active" }),
    db.doc("organizations/business-org").set({ name: "İşletme", type: "business", status: "active" }),
    db.doc("organizations/business-org/members/business-1").set({
      userId: "business-1", role: "owner", status: "active",
    }),
  ]);
  assert.deepEqual(await getPortalContextService(db, "business-1"), {
    portalRole: "business",
    organizationId: "business-org",
    organizationName: "İşletme",
    membershipRole: "owner",
  });
  await Promise.all([
    db.doc("users/community-1").set({ role: "communityManager", status: "active" }),
    db.doc("organizations/community-org").set({
      name: "Topluluk", university: "Üniversite", type: "community", status: "active",
      legacyTestCommunityId: "legacy-community",
    }),
    db.doc("organizations/community-org/members/community-1").set({
      userId: "community-1", role: "manager", status: "active",
    }),
  ]);
  assert.deepEqual(await getPortalContextService(db, "community-1"), {
    portalRole: "community",
    organizationId: "community-org",
    organizationName: "Topluluk",
    university: "Üniversite",
    legacyTestCommunityId: "legacy-community",
    membershipRole: "manager",
  });
});
