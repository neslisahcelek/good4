import assert from "node:assert/strict";
import { after, beforeEach, test } from "node:test";
import { Timestamp } from "firebase-admin/firestore";
import { db, legacyTestDb } from "./firebase.js";
import {
  AKDENIZ_EVENTS_BASE_URL,
  CAMPUS_EVENTS_COLLECTION,
  clearCampusEventPosterService,
  decodePosterUpload,
  eventTitleCase,
  fixTurkishUppercase,
  importAkdenizEventsService,
  listCampusEventsForAdminService,
  mapCategory,
  parseEventDetail,
  parseEventList,
  prepareEvent,
  setCampusEventPosterService,
  type AkdenizImportDeps,
  type PosterStorage,
} from "./akdenizEventsImport.js";
import { akdenizDetail149Html, akdenizDetail162Html, akdenizListHtml } from "./akdenizEventsImport.fixture.js";

// Fixture events run 13-18 Nov 2026; tests run "on" 10 Nov 2026.
const NOW = Date.parse("2026-11-10T09:00:00+03:00");
const PNG = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1, 2, 3, 4]).toString("base64");

beforeEach(async () => {
  await Promise.all([
    db.recursiveDelete(db.collection(CAMPUS_EVENTS_COLLECTION)),
    db.recursiveDelete(db.collection("system")),
    db.recursiveDelete(db.collection("auditLogs")),
    db.recursiveDelete(db.collection("users")),
  ]);
  await Promise.all([
    db.doc("users/admin-1").set({ role: "good4Admin", status: "active" }),
    db.doc("users/student-1").set({ role: "student", status: "active" }),
  ]);
});

after(async () => {
  await Promise.all([db.terminate(), legacyTestDb.terminate()]);
});

function siteDeps(list = akdenizListHtml): AkdenizImportDeps & { copies: string[] } {
  const copies: string[] = [];
  return {
    copies,
    fetchText: async (url) => {
      if (url === `${AKDENIZ_EVENTS_BASE_URL}/`) return list;
      if (url.endsWith("/detay/149")) return akdenizDetail149Html;
      if (url.endsWith("/detay/162")) return akdenizDetail162Html;
      throw new Error(`unexpected ${url}`);
    },
    copySourceImage: async (sourceUrl, eventId) => {
      copies.push(sourceUrl);
      return `https://storage.example/${eventId}.webp`;
    },
  };
}

function memoryStorage(): PosterStorage & { saved: string[]; removed: string[] } {
  const saved: string[] = [];
  const removed: string[] = [];
  let counter = 0;
  return {
    saved,
    removed,
    save: async (eventId) => {
      const objectName = `campus-events/posters/${eventId}-${++counter}.webp`;
      saved.push(objectName);
      return { url: `https://storage.example/${objectName}`, objectName };
    },
    remove: async (objectName) => { removed.push(objectName); },
  };
}

test("parses the list page and the detail page of etkinlik.akdeniz.edu.tr", () => {
  const list = parseEventList(akdenizListHtml);
  assert.deepEqual(list.map((item) => item.id), ["149", "162", "155"]);
  const trip = list[0]!;
  assert.equal(trip.community, "Akdeniz ve Türk Uygarlıkları Araştırmaları Topluluğu");
  assert.equal(trip.date, "2026-11-13");
  assert.equal(trip.time, "08:30");

  const detail = parseEventDetail(akdenizDetail162Html);
  assert.ok(detail);
  assert.equal(detail.community, "Akdeniz Üniversitesi Motosiklet Topluluğu");
  assert.equal(detail.startDate, "2026-11-18");
  assert.equal(detail.startTime, "12:00");
  assert.equal(detail.endTime, "17:00");
  assert.match(detail.description, /^Kortej başlangıç noktası Üç Kapılar/);
  assert.equal(detail.imagePath, "/static/uploads/default.jpg");
});

test("restores Turkish capitals, title-cases titles and keeps acronyms", () => {
  assert.equal(fixTurkishUppercase("KAREOKE GECESI"), "KAREOKE GECESİ");
  assert.equal(fixTurkishUppercase("KONYAALTI YER EĞITIMI"), "KONYAALTI YER EĞİTİMİ");
  assert.equal(fixTurkishUppercase("SILLYON-PERGE ANTIK KENT GEZISI"), "SİLLYON-PERGE ANTİK KENT GEZİSİ");
  assert.equal(eventTitleCase("KAREOKE GECESİ"), "Kareoke Gecesi");
  assert.equal(eventTitleCase("LÖSEV FARKINDALIK KORTEJİ"), "LÖSEV Farkındalık Korteji");
  assert.equal(eventTitleCase("ADRASANDA KAMP&SUP"), "Adrasanda Kamp&SUP");
  assert.equal(eventTitleCase(fixTurkishUppercase("SILLYON-PERGE ANTIK KENT GEZISI")), "Sillyon-Perge Antik Kent Gezisi");
  assert.equal(eventTitleCase("VE KÜLTÜR VE SANAT"), "Ve Kültür ve Sanat");
});

test("maps site labels and titles onto Good4 event categories", () => {
  assert.equal(mapCategory("Ortak Etkinlik KAMPÜSTE YOGA"), "sports-nature");
  assert.equal(mapCategory("Gezi"), "culture-arts");
  assert.equal(mapCategory("Konferans"), "academic-science");
  assert.equal(mapCategory("Toplantı"), "social-entertainment");
  assert.equal(mapCategory("Topluluğa Özgü Faaliyet"), "other");
});

test("prepareEvent prefers detail values and falls back to two hours without an end time", () => {
  const listing = parseEventList(akdenizListHtml).find((item) => item.id === "155")!;
  const prepared = prepareEvent(listing, null)!;
  assert.equal(prepared.endsAtMs - prepared.startsAtMs, 2 * 60 * 60 * 1000);
  assert.match(prepared.sourceImageUrl, /^https:\/\/etkinlik\.akdeniz\.edu\.tr\/static\/uploads\//);
  const withDefaultImage = prepareEvent(parseEventList(akdenizListHtml)[1]!, parseEventDetail(akdenizDetail162Html))!;
  assert.equal(withDefaultImage.sourceImageUrl, "");
});

test("import writes campus events, copies each site poster once and keeps the admin design", async () => {
  const deps = siteDeps();
  const first = await importAkdenizEventsService(db, deps, NOW);
  assert.equal(first.imported, 3);
  assert.equal(deps.copies.length, 1, "only the Çakırlar poster is a real upload");

  const trip = await db.doc(`${CAMPUS_EVENTS_COLLECTION}/akdeniz-etkinlik-149`).get();
  assert.equal(trip.get("title"), "Sillyon-Perge Antik Kent Gezisi");
  assert.equal(trip.get("status"), "published");
  assert.equal(trip.get("categoryId"), "culture-arts");
  assert.ok(trip.get("startsAt") instanceof Timestamp);

  await db.doc(`${CAMPUS_EVENTS_COLLECTION}/akdeniz-etkinlik-155`).update({ posterUrl: "https://admin/design.webp", posterObject: "campus-events/posters/x.webp" });
  await importAkdenizEventsService(db, deps, NOW);
  const tour = await db.doc(`${CAMPUS_EVENTS_COLLECTION}/akdeniz-etkinlik-155`).get();
  assert.equal(tour.get("posterUrl"), "https://admin/design.webp", "re-import must not touch the admin poster");
  assert.equal(deps.copies.length, 1, "an unchanged site poster is not downloaded again");
});

test("import hides upcoming events that disappeared from the site and never wipes on an empty page", async () => {
  await importAkdenizEventsService(db, siteDeps(), NOW);
  const withoutTrip = akdenizListHtml.split('<div class="event-card"')
    .filter((part) => !part.includes('href="/detay/149"'))
    .join('<div class="event-card"');
  const result = await importAkdenizEventsService(db, siteDeps(withoutTrip), NOW);
  assert.equal(result.removed, 1);
  assert.equal((await db.doc(`${CAMPUS_EVENTS_COLLECTION}/akdeniz-etkinlik-149`).get()).get("status"), "removed");

  await assert.rejects(importAkdenizEventsService(db, siteDeps("<html></html>"), NOW), /AKDENIZ_EVENTS_LIST_EMPTY/);
  assert.equal((await db.doc(`${CAMPUS_EVENTS_COLLECTION}/akdeniz-etkinlik-162`).get()).get("status"), "published");
});

test("import skips events that already ended", async () => {
  const later = Date.parse("2026-11-16T09:00:00+03:00");
  const result = await importAkdenizEventsService(db, siteDeps(), later);
  assert.equal(result.imported, 1, "only the 18 Nov kortej is still ahead");
});

test("only Good4 admins can set or clear a poster; the old design file is removed", async () => {
  await importAkdenizEventsService(db, siteDeps(), NOW);
  const storage = memoryStorage();
  const input = { eventId: "akdeniz-etkinlik-162", contentType: "image/png", base64: PNG };

  await assert.rejects(setCampusEventPosterService(db, "student-1", input, storage), /ROLE_NOT_ALLOWED/);
  assert.equal(storage.saved.length, 0, "nothing is stored before the role check passes");

  await setCampusEventPosterService(db, "admin-1", input, storage);
  await setCampusEventPosterService(db, "admin-1", input, storage);
  const event = await db.doc(`${CAMPUS_EVENTS_COLLECTION}/akdeniz-etkinlik-162`).get();
  assert.equal(event.get("posterObject"), storage.saved[1]);
  assert.deepEqual(storage.removed, [storage.saved[0]]);

  await assert.rejects(clearCampusEventPosterService(db, "student-1", { eventId: "akdeniz-etkinlik-162" }, storage), /ROLE_NOT_ALLOWED/);
  await clearCampusEventPosterService(db, "admin-1", { eventId: "akdeniz-etkinlik-162" }, storage);
  const cleared = await db.doc(`${CAMPUS_EVENTS_COLLECTION}/akdeniz-etkinlik-162`).get();
  assert.equal(cleared.get("posterUrl"), undefined);
  assert.deepEqual(storage.removed, [storage.saved[0], storage.saved[1]]);

  const audits = await db.collection("auditLogs").where("targetId", "==", "akdeniz-etkinlik-162").get();
  assert.equal(audits.size, 3);
});

test("poster uploads are validated before anything is stored", async () => {
  await importAkdenizEventsService(db, siteDeps(), NOW);
  const storage = memoryStorage();
  await assert.rejects(setCampusEventPosterService(db, "admin-1", { eventId: "missing", contentType: "image/png", base64: PNG }, storage), /CAMPUS_EVENT_NOT_FOUND/);
  await assert.rejects(setCampusEventPosterService(db, "admin-1", { eventId: "../x", contentType: "image/png", base64: PNG }, storage), /CAMPUS_EVENT_ID_INVALID/);
  assert.throws(() => decodePosterUpload({ contentType: "image/gif", base64: PNG }), /CAMPUS_EVENT_POSTER_TYPE_INVALID/);
  assert.throws(() => decodePosterUpload({ contentType: "image/jpeg", base64: PNG }), /CAMPUS_EVENT_POSTER_CONTENT_INVALID/);
  assert.throws(() => decodePosterUpload({ contentType: "image/png", base64: "not base64!" }), /CAMPUS_EVENT_POSTER_INVALID/);
  assert.equal(storage.saved.length, 0);
});

test("admin listing returns upcoming published events soonest first", async () => {
  await importAkdenizEventsService(db, siteDeps(), NOW);
  await assert.rejects(listCampusEventsForAdminService(db, "student-1", NOW), /ROLE_NOT_ALLOWED/);
  const { events, lastRunAtMs } = await listCampusEventsForAdminService(db, "admin-1", NOW);
  assert.deepEqual(events.map((event) => event.id), ["akdeniz-etkinlik-149", "akdeniz-etkinlik-155", "akdeniz-etkinlik-162"]);
  assert.ok(lastRunAtMs);
});
