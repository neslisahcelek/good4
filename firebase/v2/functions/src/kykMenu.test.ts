import assert from "node:assert/strict";
import { after, beforeEach, test } from "node:test";
import { db } from "./firebase.js";
import { parseKykMenuText, saveKykMenuService } from "./kykMenu.js";

beforeEach(async () => {
  for (const collection of ["users", "kyk_menu_days", "auditLogs"]) {
    await db.recursiveDelete(db.collection(collection));
  }
});

after(async () => {
  await db.terminate();
});

const SAMPLE = `27 EYLÜL 2026

KAHVALTI
- Haşlanmış yumurta
- Çay / bitki çayı

AKŞAM YEMEĞİ
- Domates çorbası + kaşar peyniri / Tarhana çorbası
- Kalburabastı

28 Eylül 2026
KAHVALTI
- Sucuklu yumurta
AKŞAM YEMEĞİ
- Yayla çorbası / Ezogelin çorbası`;

test("parses the transcribed wall list into days and meals", () => {
  const { days, errors } = parseKykMenuText(SAMPLE);
  assert.deepEqual(errors, []);
  assert.deepEqual(days, [
    {
      date: "2026-09-27",
      breakfast: ["Haşlanmış yumurta", "Çay / bitki çayı"],
      dinner: ["Domates çorbası + kaşar peyniri / Tarhana çorbası", "Kalburabastı"],
    },
    { date: "2026-09-28", breakfast: ["Sucuklu yumurta"], dinner: ["Yayla çorbası / Ezogelin çorbası"] },
  ]);
});

test("reports items that come before a date or meal heading", () => {
  const { errors } = parseKykMenuText("- Yumurta\n1 EKİM 2026\n- Çay");
  assert.equal(errors.length, 2);
});

test("Good4 admin publishes each KYK day as its own document", async () => {
  await db.doc("users/admin-1").set({ role: "good4Admin", status: "active" });
  const { days } = parseKykMenuText(SAMPLE);
  const saved = await saveKykMenuService(db, "admin-1", { days });
  assert.deepEqual(saved.savedDates, ["2026-09-27", "2026-09-28"]);
  const doc = await db.doc("kyk_menu_days/2026-09-27").get();
  assert.equal(doc.get("breakfast")[0], "Haşlanmış yumurta");
  assert.equal(doc.get("updatedBy"), "admin-1");
  assert.equal((await db.collection("auditLogs").where("action", "==", "kykMenu.updated").get()).size, 1);
});

test("non-admin cannot publish the KYK menu", async () => {
  await db.doc("users/student-1").set({ role: "student", status: "active" });
  await assert.rejects(
    () => saveKykMenuService(db, "student-1", { days: [{ date: "2026-10-01", breakfast: ["Çay"] }] }),
    (error: unknown) => error instanceof Error && error.message === "ROLE_NOT_ALLOWED",
  );
});

test("rejects invalid dates, empty days and duplicates", async () => {
  await db.doc("users/admin-1").set({ role: "good4Admin", status: "active" });
  const reject = (days: unknown[], code: string) => assert.rejects(
    () => saveKykMenuService(db, "admin-1", { days }),
    (error: unknown) => error instanceof Error && error.message === code,
  );
  await reject([{ date: "2026-02-30", breakfast: ["Çay"] }], "KYK_MENU_DATE_INVALID");
  await reject([{ date: "2026-10-01", breakfast: [], dinner: [] }], "KYK_MENU_DAY_EMPTY");
  await reject([{ date: "2026-10-01", breakfast: ["Çay"] }, { date: "2026-10-01", dinner: ["Pilav"] }], "KYK_MENU_DAY_DUPLICATE");
  await reject([{ date: "2026-10-01", breakfast: ["x".repeat(121)] }], "KYK_MENU_ITEMS_INVALID");
});
