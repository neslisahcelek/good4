import assert from "node:assert/strict";
import { after, beforeEach, test } from "node:test";
import { db, legacyTestDb } from "./firebase.js";
import {
  DINING_MENU_IMPORT_DOC,
  importSksDiningMenuService,
  parseSksMenu,
  turkishTitleCase,
  type DiningMenuImportDeps,
  type OcrResult,
} from "./diningMenuImport.js";
import { sksMenuOcr20260928 } from "./diningMenuImport.fixture.js";

const IMAGE_URL = "https://webis.akdeniz.edu.tr/uploads/1019/yemekhane/Slayt1%20copy%207d39f09f.JPG";
const PAGE = `<div class="article-text"><p><img src="${IMAGE_URL}" alt="menu" /></p></div>`;

beforeEach(async () => {
  await Promise.all([
    db.recursiveDelete(db.collection("app_config")),
    db.recursiveDelete(db.collection("system")),
    db.recursiveDelete(db.collection("auditLogs")),
  ]);
});

after(async () => {
  await Promise.all([db.terminate(), legacyTestDb.terminate()]);
});

function deps(ocr: OcrResult = sksMenuOcr20260928, page = PAGE): DiningMenuImportDeps & { recognized: number } {
  const result = {
    recognized: 0,
    fetchText: async () => page,
    fetchImage: async () => Buffer.from("image"),
    recognize: async () => {
      result.recognized += 1;
      return ocr;
    },
  };
  return result;
}

test("reads the five SKS day cards, including wrapped names and misread brackets", () => {
  const menu = parseSksMenu(sksMenuOcr20260928);
  assert.equal(menu.weekStart, "2026-09-28");
  assert.equal(menu.weekEnd, "2026-10-02");
  assert.deepEqual(menu.days.map((day) => [day.date, day.dayName, day.calories]), [
    ["2026-09-28", "Pazartesi", 1085],
    ["2026-09-29", "Salı", 1076],
    ["2026-09-30", "Çarşamba", 988],
    ["2026-10-01", "Perşembe", 1013],
    ["2026-10-02", "Cuma", 1157],
  ]);
  assert.deepEqual(menu.days[0]!.meals, [
    "Domates Çorba",
    "Çıtır Piliç Şinitzel",
    "Garnitür Elma Dilim Patates",
    "Marul Lahana Salata",
    "Sütlaç",
  ]);
  assert.ok(menu.days[2]!.meals.includes("Ayran"));
  assert.ok(menu.days[3]!.meals.includes("Yoğurt"));
});

test("rejects a day whose dish calories do not add up to the printed total", () => {
  const ocr: OcrResult = {
    ...sksMenuOcr20260928,
    lines: sksMenuOcr20260928.lines.filter((line) => !line.text.includes("SÜTLAÇ")),
  };
  assert.throws(() => parseSksMenu(ocr), /MENU_CALORIE_MISMATCH 2026-09-28 903\/1085/);
});

test("turns upper-case dish names into Turkish title case", () => {
  assert.equal(turkishTitleCase("ŞEHRİYELİ BULGUR PİLAVI"), "Şehriyeli Bulgur Pilavı");
  assert.equal(turkishTitleCase("IZGARA İÇLİ KÖFTE"), "Izgara İçli Köfte");
});

test("publishes a new menu image once and skips it afterwards", async () => {
  const fake = deps();
  const now = new Date("2026-09-26T09:00:00Z");
  assert.equal(await importSksDiningMenuService(db, legacyTestDb, fake, now), "imported");

  const menu = await db.doc("app_config/akdeniz_dining_menu").get();
  assert.equal(menu.get("weekStart"), "2026-09-28");
  assert.equal(menu.get("updatedBy"), "system:sks-menu-import");
  assert.equal(menu.get("sourceImageUrl"), IMAGE_URL);
  assert.equal(menu.get("days").length, 5);

  assert.equal(await importSksDiningMenuService(db, legacyTestDb, fake, now), "unchanged");
  assert.equal(fake.recognized, 1);
});

test("keeps the current menu when the image cannot be read reliably", async () => {
  await db.doc("app_config/akdeniz_dining_menu").set({ weekStart: "2026-09-21", weekEnd: "2026-09-25", days: [] });
  const broken: OcrResult = { width: 2933, lines: [{ text: "ÖĞLE MENÜSÜ", x0: 0, y0: 0, x1: 10, y1: 10 }] };

  await assert.rejects(importSksDiningMenuService(db, legacyTestDb, deps(broken), new Date("2026-09-26T09:00:00Z")), /MENU_NO_DAYS/);
  const menu = await db.doc("app_config/akdeniz_dining_menu").get();
  assert.equal(menu.get("weekStart"), "2026-09-21");
  const status = await db.doc(DINING_MENU_IMPORT_DOC).get();
  assert.equal(status.get("lastResult"), "failed");
  assert.equal(status.get("lastError"), "MENU_NO_DAYS");
});

test("does not replace a newer menu with an older week's image", async () => {
  await db.doc("app_config/akdeniz_dining_menu").set({ weekStart: "2026-10-05", weekEnd: "2026-10-09", days: [] });
  assert.equal(await importSksDiningMenuService(db, legacyTestDb, deps(), new Date("2026-09-29T09:00:00Z")), "stale");
  const menu = await db.doc("app_config/akdeniz_dining_menu").get();
  assert.equal(menu.get("weekStart"), "2026-10-05");
});
