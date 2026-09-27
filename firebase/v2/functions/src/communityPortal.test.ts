import assert from "node:assert/strict";
import { after, beforeEach, test } from "node:test";
import {
  cancelCommunityPortalEntryService,
  getCommunityPortalDashboardService,
  saveCommunityPortalEntryService,
} from "./communityPortal.js";
import { FieldValue, Timestamp } from "firebase-admin/firestore";
import { db, legacyTestDb } from "./firebase.js";

beforeEach(async () => {
  for (const collection of ["users", "organizations", "events", "auditLogs"]) {
    await db.recursiveDelete(db.collection(collection));
  }
  for (const collection of ["communities", "businesses"]) {
    await legacyTestDb.recursiveDelete(legacyTestDb.collection(collection));
  }
  await Promise.all([
    db.doc("users/manager-1").set({ role: "communityManager", status: "active" }),
    db.doc("organizations/community-org").set({
      name: "Kadın Girişimciler Topluluğu", university: "Akdeniz Üniversitesi",
      type: "community", status: "active", legacyTestCommunityId: "demo-toplulugu",
    }),
    db.doc("organizations/community-org/members/manager-1").set({
      userId: "manager-1", role: "manager", status: "active",
    }),
    legacyTestDb.doc("communities/demo-toplulugu").set({
      name: "Kadın Girişimciler Topluluğu", university: "Akdeniz Üniversitesi", description: "Demo",
    }),
    legacyTestDb.doc("businesses/business-1").set({ name: "Demo Kahve" }),
  ]);
});

after(async () => {
  await Promise.all([db.terminate(), legacyTestDb.terminate()]);
});

test("community dashboard returns live registration and attendance totals", async () => {
  await Promise.all([
    db.doc("organizations/community-org/followers/student-1").set({ userId: "student-1" }),
    db.doc("events/event-1").set({
      organizationId: "community-org", title: "Tanışma", description: "Demo",
      startsAt: Timestamp.fromDate(new Date("2026-10-01T15:00:00Z")),
      location: "Kampüs", status: "published", registrationCount: 1, attendanceCount: 1,
    }),
    db.doc("events/event-1/registrations/registration-1").set({
      userId: "student-1", displayName: "Öğrenci", registeredAt: 1,
    }),
    db.doc("events/event-1/checkins/registration-1").set({ userId: "student-1" }),
  ]);
  const dashboard = await getCommunityPortalDashboardService(db, legacyTestDb, "manager-1");
  assert.equal(dashboard.followerCount, 1);
  assert.equal(dashboard.entries[0]?.registrationCount, 1);
  assert.equal(dashboard.entries[0]?.attendanceCount, 1);
  assert.equal(dashboard.entries[0]?.participants[0]?.displayName, "Öğrenci");
});

test("community event writes only to the canonical V2 event collection", async () => {
  await db.doc("organizations/community-org").update({ legacyTestCommunityId: FieldValue.delete() });
  const created = await saveCommunityPortalEntryService(db, legacyTestDb, "manager-1", {
    kind: "event", title: "V2 Buluşması", description: "Tek kaynak", date: "2026-10-01",
    time: "18:00", location: "Kampüs", capacity: 50, categoryId: "career-entrepreneurship",
  });
  assert.equal(created.status, "published");
  const event = await db.doc(`events/${created.entryId}`).get();
  assert.equal(event.get("organizationId"), "community-org");
  assert.equal(event.get("categoryId"), "career-entrepreneurship");
  assert.ok(event.get("startsAt") instanceof Timestamp);
  assert.equal((await legacyTestDb.doc(`communities/demo-toplulugu/entries/${created.entryId}`).get()).exists, false);
  assert.equal((await getCommunityPortalDashboardService(db, legacyTestDb, "manager-1")).entries[0]?.id, created.entryId);
  assert.equal((await getCommunityPortalDashboardService(db, legacyTestDb, "manager-1")).entries[0]?.categoryId, "career-entrepreneurship");
});

test("community manager can create a coupon and cancel it", async () => {
  const created = await saveCommunityPortalEntryService(db, legacyTestDb, "manager-1", {
    kind: "coupon", title: "Kahvede indirim", description: "Öğrencilere özel",
    date: "2026-10-31", businessId: "business-1", discountType: "percentage",
    discountValue: 20, totalLimit: 100,
  });
  assert.equal(created.status, "pending");
  const entry = await legacyTestDb.doc(`communities/demo-toplulugu/entries/${created.entryId}`).get();
  assert.equal(entry.get("status"), "pending");
  assert.equal(entry.get("location"), "Demo Kahve");
  await entry.ref.update({ imageUrl: "https://cdn.good4.test/coupon.jpg", code: "GOOD4" });
  await saveCommunityPortalEntryService(db, legacyTestDb, "manager-1", {
    entryId: created.entryId, kind: "coupon", title: "Güncel indirim", description: "Öğrencilere özel",
    date: "2026-10-31", businessId: "business-1", discountType: "percentage",
    discountValue: 25, totalLimit: 100,
  });
  const edited = await entry.ref.get();
  assert.equal(edited.get("imageUrl"), "https://cdn.good4.test/coupon.jpg");
  assert.equal(edited.get("code"), "GOOD4");
  await cancelCommunityPortalEntryService(db, legacyTestDb, "manager-1", { entryId: created.entryId });
  assert.equal((await entry.ref.get()).get("status"), "cancelled");
});
