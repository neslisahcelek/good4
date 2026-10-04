import assert from "node:assert/strict";
import { after, beforeEach, test } from "node:test";
import { Timestamp } from "firebase-admin/firestore";
import { db, legacyTestDb, storageBucket } from "./firebase.js";
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
  title: "Kategori testi", description: "Öğrenci etkinliği", date: "2030-10-01",
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

test("events keep a start and an end, and older clients keep the saved duration", async () => {
  const hours = (event: FirebaseFirestore.DocumentSnapshot) =>
    (event.get("endsAt").toMillis() - event.get("startsAt").toMillis()) / 3_600_000;
  const legacy = await saveEventService(db, "manager-1", "community-org", eventInput);
  assert.equal(hours(await db.doc(`events/${legacy.eventId}`).get()), 2);

  const ranged = await saveEventService(db, "manager-1", "community-org", {
    ...eventInput, endDate: "2030-10-02", endTime: "12:00",
  });
  const saved = await db.doc(`events/${ranged.eventId}`).get();
  assert.equal(saved.get("endsAt").toDate().toISOString(), "2030-10-02T09:00:00.000Z");
  assert.equal(hours(saved), 18);

  // An edit without end fields (the web panel) moves the event but keeps its length.
  await saveEventService(db, "manager-1", "community-org", { ...eventInput, eventId: ranged.eventId, time: "20:00" });
  assert.equal(hours(await db.doc(`events/${ranged.eventId}`).get()), 18);

  await assert.rejects(saveEventService(db, "manager-1", "community-org", {
    ...eventInput, endDate: "2030-10-01", endTime: "18:00",
  }), /EVENT_END_BEFORE_START/);
  await assert.rejects(saveEventService(db, "manager-1", "community-org", {
    ...eventInput, endDate: "2030-10-16", endTime: "18:01",
  }), /EVENT_DURATION_TOO_LONG/);
  await assert.rejects(saveEventService(db, "manager-1", "community-org", {
    ...eventInput, endDate: "2030-10-02",
  }), /ENDTIME/);
});

test("a start in the past is rejected unless an edit leaves it unchanged", async () => {
  await assert.rejects(
    saveEventService(db, "manager-1", "community-org", { ...eventInput, date: "2020-01-01" }),
    /EVENT_START_IN_PAST/,
  );
  await db.doc("events/started").set({
    organizationId: "community-org", title: "Sürüyor", status: "published",
    startsAt: Timestamp.fromDate(new Date("2020-01-01T07:00:00Z")),
    endsAt: Timestamp.fromDate(new Date("2020-01-01T09:00:00Z")),
  });
  await saveEventService(db, "manager-1", "community-org", {
    ...eventInput, eventId: "started", date: "2020-01-01", time: "10:00", location: "Yeni salon",
  });
  assert.equal((await db.doc("events/started").get()).get("location"), "Yeni salon");
  await assert.rejects(saveEventService(db, "manager-1", "community-org", {
    ...eventInput, eventId: "started", date: "2020-01-01", time: "11:00",
  }), /EVENT_START_IN_PAST/);
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

function coverUrl(objectName: string, bucket = storageBucket.name): string {
  return `https://firebasestorage.googleapis.com/v0/b/${bucket}/o/${encodeURIComponent(objectName)}?alt=media&token=test-token`;
}

test("event covers accept only the owning community path in the project bucket", async () => {
  const imageUrl = coverUrl("community-events/community-org/cover.jpg");
  const result = await saveEventService(db, "manager-1", "community-org", { ...eventInput, imageUrl });
  assert.equal((await db.doc(`events/${result.eventId}`).get()).get("imageUrl"), imageUrl);
  await saveEventService(db, "manager-1", "community-org", { ...eventInput, eventId: result.eventId, imageUrl: "" });
  assert.equal((await db.doc(`events/${result.eventId}`).get()).get("imageUrl"), "");
});

test("event covers reject foreign communities both on create and update", async () => {
  for (const imageUrl of [coverUrl("community-events/other-community/cover.jpg"), coverUrl("community-events/community-org-extra/cover.jpg")]) {
    for (const eventId of [undefined, "event-1"]) {
      await assert.rejects(saveEventService(db, "manager-1", "community-org", { ...eventInput, eventId, imageUrl }), {
        code: "invalid-argument", message: "IMAGE_URL_INVALID",
      });
    }
  }
  assert.equal((await db.doc("events/event-1").get()).get("imageUrl"), undefined);
});

test("event covers reject external URLs, buckets, unrelated paths and malformed URLs", async () => {
  const ownUrl = coverUrl("community-events/community-org/cover.jpg");
  for (const imageUrl of [
    "https://example.com/cover.jpg", coverUrl("community-events/community-org/cover.jpg", "foreign-bucket.appspot.com"),
    coverUrl("home-banners/current.jpg"), coverUrl("community_images/community-org/cover.jpg"),
    coverUrl("community-events/community-org/"), coverUrl("community-events/community-org/../other-community/cover.jpg"),
    ownUrl.replace("firebasestorage.googleapis.com", "firebasestorage.googleapis.com.example.com"),
    ownUrl.replace("https://", "http://"), ownUrl.replace("https://", "https://attacker@example.com@"),
    ownUrl.replace("https://", "https://attacker@"), `${ownUrl}#fragment`,
    `https://firebasestorage.googleapis.com/v0/b/${storageBucket.name}/o/%ZZ`, "https://",
  ]) {
    await assert.rejects(saveEventService(db, "manager-1", "community-org", { ...eventInput, imageUrl }), {
      code: "invalid-argument", message: "IMAGE_URL_INVALID",
    });
  }
});

test("unchanged existing covers remain editable and can be cleared", async () => {
  for (const imageUrl of ["https://legacy.example.com/cover.jpg", coverUrl("community-events/other-community/legacy.jpg"), "http://legacy.example.com/cover.jpg"]) {
    await db.doc("events/event-1").update({ imageUrl });
    await saveEventService(db, "manager-1", "community-org", { ...eventInput, eventId: "event-1", imageUrl });
    assert.equal((await db.doc("events/event-1").get()).get("imageUrl"), imageUrl);
    await saveEventService(db, "manager-1", "community-org", { ...eventInput, eventId: "event-1" });
    assert.equal((await db.doc("events/event-1").get()).get("imageUrl"), imageUrl);
    await assert.rejects(saveEventService(db, "manager-1", "community-org", { ...eventInput, eventId: "event-1", imageUrl: "https://different.example.com/cover.jpg" }), /IMAGE_URL_INVALID/);
  }
  await saveEventService(db, "manager-1", "community-org", { ...eventInput, eventId: "event-1", imageUrl: "" });
  assert.equal((await db.doc("events/event-1").get()).get("imageUrl"), "");
});

test("non-string event covers are rejected", async () => {
  for (const imageUrl of [null, 1, {}, [coverUrl("community-events/community-org/cover.jpg")]]) {
    await assert.rejects(saveEventService(db, "manager-1", "community-org", { ...eventInput, imageUrl }), {
      code: "invalid-argument", message: "IMAGE_URL_INVALID",
    });
  }
});

test("attendance is recorded only from a scanned ticket and cannot be undone", async () => {
  const registered = await setEventRegistrationService(db, "student-1", { eventId: "event-1", registered: true });
  assert.ok(registered.registrationId);
  const first = await recordEventAttendanceService(db, "manager-1", {
    eventId: "event-1", registrationId: registered.registrationId,
  });
  const duplicate = await recordEventAttendanceService(db, "manager-1", {
    eventId: "event-1", registrationId: registered.registrationId,
  });
  await assert.rejects(
    recordEventAttendanceService(db, "manager-1", { eventId: "event-1", userId: "student-1" }),
    /TICKET_REQUIRED/,
  );
  await assert.rejects(
    recordEventAttendanceService(db, "manager-1", { eventId: "event-1", registrationId: registered.registrationId, undo: true }),
    /CHECKIN_UNDO_DISABLED/,
  );
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
