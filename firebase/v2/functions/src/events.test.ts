import assert from "node:assert/strict";
import { after, beforeEach, test } from "node:test";
import { Timestamp } from "firebase-admin/firestore";
import { db, legacyTestDb } from "./firebase.js";
import { recordEventAttendanceService, saveEventService, setEventRegistrationService } from "./events.js";
import { EVENT_CATEGORIES } from "./eventCategories.js";

beforeEach(async () => {
  for (const collection of ["users", "organizations", "events", "auditLogs"]) {
    await db.recursiveDelete(db.collection(collection));
  }
  await Promise.all([
    db.doc("users/manager-1").set({ role: "communityManager", status: "active" }),
    db.doc("users/student-1").set({ role: "student", status: "active", displayName: "Bir" }),
    db.doc("users/student-2").set({ role: "student", status: "active", displayName: "İki" }),
    db.doc("organizations/community-org").set({ type: "community", status: "active" }),
    db.doc("organizations/community-org/members/manager-1").set({ userId: "manager-1", role: "manager", status: "active" }),
    db.doc("events/event-1").set({
      organizationId: "community-org", title: "Etkinlik", status: "published", capacity: 1,
      registrationCount: 0, attendanceCount: 0,
      endsAt: Timestamp.fromDate(new Date(Date.now() + 86_400_000)),
    }),
  ]);
});

after(async () => {
  await Promise.all([db.terminate(), legacyTestDb.terminate()]);
});

const eventInput = {
  title: "Kategori testi", description: "Öğrenci etkinliği", date: "2026-10-01",
  time: "18:00", location: "Kampüs", capacity: 10,
};

test("event categories are validated and preserved for older clients", async () => {
  for (const category of EVENT_CATEGORIES) {
    const created = await saveEventService(db, "manager-1", "community-org", { ...eventInput, categoryId: category.id });
    await saveEventService(db, "manager-1", "community-org", { ...eventInput, eventId: created.eventId });
    assert.equal((await db.doc(`events/${created.eventId}`).get()).get("categoryId"), category.id);
  }
  const legacy = await saveEventService(db, "manager-1", "community-org", eventInput);
  assert.equal((await db.doc(`events/${legacy.eventId}`).get()).get("categoryId"), undefined);
  await saveEventService(db, "manager-1", "community-org", { ...eventInput, eventId: legacy.eventId, categoryId: "technology" });
  assert.equal((await db.doc(`events/${legacy.eventId}`).get()).get("categoryId"), "technology");
  for (const categoryId of ["", "unknown", null, 1, ["technology"]]) {
    await assert.rejects(saveEventService(db, "manager-1", "community-org", { ...eventInput, categoryId }), /EVENT_CATEGORY_INVALID/);
  }
});

test("category updates cannot bypass manager and organization ownership checks", async () => {
  await assert.rejects(saveEventService(db, "student-1", "community-org", { ...eventInput, categoryId: "technology" }));
  await db.doc("events/event-1").update({ organizationId: "other-community", categoryId: "culture-arts" });
  await assert.rejects(saveEventService(db, "manager-1", "community-org", { ...eventInput, eventId: "event-1", categoryId: "technology" }), /EVENT_NOT_FOUND/);
  assert.equal((await db.doc("events/event-1").get()).get("categoryId"), "culture-arts");
});

test("capacity is enforced atomically for concurrent registrations", async () => {
  const results = await Promise.allSettled([
    setEventRegistrationService(db, "student-1", { eventId: "event-1", registered: true }),
    setEventRegistrationService(db, "student-2", { eventId: "event-1", registered: true }),
  ]);
  assert.equal(results.filter((item) => item.status === "fulfilled").length, 1);
  assert.equal((await db.collection("events/event-1/registrations").get()).size, 1);
  assert.equal((await db.doc("events/event-1").get()).get("registrationCount"), 1);
});

test("QR and manual attendance share one duplicate-safe backend", async () => {
  const registered = await setEventRegistrationService(db, "student-1", { eventId: "event-1", registered: true });
  assert.ok(registered.registrationId);
  const first = await recordEventAttendanceService(db, "manager-1", {
    eventId: "event-1", registrationId: registered.registrationId,
  });
  const duplicate = await recordEventAttendanceService(db, "manager-1", {
    eventId: "event-1", userId: "student-1",
  });
  assert.equal(first.changed, true);
  assert.equal(duplicate.changed, false);
  const checkin = await db.doc(`events/event-1/checkins/${registered.registrationId}`).get();
  assert.ok(checkin.get("checkedInAt") instanceof Timestamp);
  assert.equal(checkin.get("method"), "qr");
  assert.equal((await db.doc("events/event-1").get()).get("attendanceCount"), 1);
  await assert.rejects(
    setEventRegistrationService(db, "student-1", { eventId: "event-1", registered: false }),
    /CHECKED_IN_REGISTRATION_LOCKED/,
  );
});
