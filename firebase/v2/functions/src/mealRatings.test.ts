import assert from "node:assert/strict";
import { after, beforeEach, test } from "node:test";
import { db } from "./firebase.js";
import { rateMealService } from "./mealRatings.js";

// 2026-10-04 18:00 in Istanbul (UTC+3).
const EVENING = Date.UTC(2026, 9, 4, 15, 0);
const MORNING = Date.UTC(2026, 9, 4, 6, 0);

beforeEach(async () => {
  for (const collection of ["users", "kyk_menu_days", "app_config", "meal_ratings"]) {
    await db.recursiveDelete(db.collection(collection));
  }
  await db.doc("users/student-1").set({
    role: "student", status: "active", eduVerified: true, eduEmail: "ayse@ogr.akdeniz.edu.tr",
  });
  await db.doc("kyk_menu_days/2026-10-04").set({ date: "2026-10-04", breakfast: ["Çay"], dinner: ["Pilav"] });
});

after(async () => {
  await db.terminate();
});

test("a vote counts once and a changed vote moves between counters", async () => {
  assert.deepEqual(
    await rateMealService(db, "student-1", { meal: "kyk_dinner", rating: "good" }, EVENING),
    { good: 1, okay: 0, bad: 0, rating: "good" },
  );
  await rateMealService(db, "student-1", { meal: "kyk_dinner", rating: "good" }, EVENING);
  assert.deepEqual(
    await rateMealService(db, "student-1", { meal: "kyk_dinner", rating: "bad" }, EVENING),
    { good: 0, okay: 0, bad: 1, rating: "bad" },
  );
  const summary = await db.doc("meal_ratings/2026-10-04_kyk_dinner").get();
  assert.deepEqual([summary.get("good"), summary.get("okay"), summary.get("bad")], [0, 0, 1]);
  assert.equal((await db.doc("meal_ratings/2026-10-04_kyk_dinner/votes/student-1").get()).get("rating"), "bad");
});

test("meals cannot be rated before they start or when nothing was published", async () => {
  await assert.rejects(
    rateMealService(db, "student-1", { meal: "kyk_dinner", rating: "good" }, MORNING),
    /MEAL_NOT_STARTED/,
  );
  await assert.rejects(
    rateMealService(db, "student-1", { meal: "cafeteria", rating: "good" }, EVENING),
    /MEAL_NOT_PUBLISHED/,
  );
});

test("only active, edu-verified students can vote with a known meal and rating", async () => {
  await assert.rejects(rateMealService(db, "student-1", { meal: "lunch", rating: "good" }, EVENING), /MEAL_INVALID/);
  await assert.rejects(rateMealService(db, "student-1", { meal: "kyk_dinner", rating: "5" }, EVENING), /RATING_INVALID/);
  await db.doc("users/student-2").set({ role: "student", status: "active" });
  await assert.rejects(
    rateMealService(db, "student-2", { meal: "kyk_dinner", rating: "good" }, EVENING),
    /EDU_VERIFICATION_REQUIRED/,
  );
  await db.doc("users/manager-1").set({ role: "communityManager", status: "active", eduVerified: true, eduEmail: "a@akdeniz.edu.tr" });
  await assert.rejects(
    rateMealService(db, "manager-1", { meal: "kyk_dinner", rating: "good" }, EVENING),
    /STUDENT_REQUIRED/,
  );
  assert.equal((await db.doc("meal_ratings/2026-10-04_kyk_dinner").get()).exists, false);
});
