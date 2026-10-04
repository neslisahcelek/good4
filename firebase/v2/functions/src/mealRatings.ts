import { FieldValue, type Firestore } from "firebase-admin/firestore";
import { HttpsError } from "firebase-functions/v2/https";
import { hasVerifiedEduEmail } from "./eduVerification.js";

export const MEALS = ["kyk_breakfast", "cafeteria", "kyk_dinner"] as const;
export const RATINGS = ["good", "okay", "bad"] as const;
type Meal = (typeof MEALS)[number];
type Rating = (typeof RATINGS)[number];

/** Rating opens once the meal has started (Istanbul time) and stays open for the rest of that day. */
const OPENS_AT_HOUR: Record<Meal, number> = { kyk_breakfast: 6, cafeteria: 11, kyk_dinner: 16 };

function istanbulNow(nowMillis: number): { date: string; hour: number } {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Europe/Istanbul", year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", hourCycle: "h23",
  }).formatToParts(new Date(nowMillis));
  const part = (type: string) => parts.find((p) => p.type === type)?.value ?? "";
  return { date: `${part("year")}-${part("month")}-${part("day")}`, hour: Number(part("hour")) };
}

async function mealIsPublished(database: Firestore, date: string, meal: Meal): Promise<boolean> {
  if (meal === "cafeteria") {
    const menu = await database.doc("app_config/akdeniz_dining_menu").get();
    const days = (menu.get("days") ?? []) as Array<{ date?: unknown; meals?: unknown }>;
    return days.some((day) => day.date === date && Array.isArray(day.meals) && day.meals.length > 0);
  }
  const day = await database.doc(`kyk_menu_days/${date}`).get();
  const items = day.get(meal === "kyk_breakfast" ? "breakfast" : "dinner");
  return Array.isArray(items) && items.length > 0;
}

/**
 * One anonymous 😋/😐/😕 vote per student, meal and day; voting again replaces the earlier vote.
 * Only the counters on meal_ratings/{date}_{meal} are readable by others.
 */
export async function rateMealService(
  database: Firestore,
  uid: string,
  input: { meal?: unknown; rating?: unknown },
  nowMillis = Date.now(),
): Promise<{ good: number; okay: number; bad: number; rating: Rating }> {
  const meal = input.meal as Meal;
  const rating = input.rating as Rating;
  if (!MEALS.includes(meal)) throw new HttpsError("invalid-argument", "MEAL_INVALID");
  if (!RATINGS.includes(rating)) throw new HttpsError("invalid-argument", "RATING_INVALID");

  const { date, hour } = istanbulNow(nowMillis);
  if (hour < OPENS_AT_HOUR[meal]) throw new HttpsError("failed-precondition", "MEAL_NOT_STARTED");
  if (!(await mealIsPublished(database, date, meal))) throw new HttpsError("failed-precondition", "MEAL_NOT_PUBLISHED");

  const summaryRef = database.doc(`meal_ratings/${date}_${meal}`);
  const voteRef = summaryRef.collection("votes").doc(uid);
  return database.runTransaction(async (transaction) => {
    const [user, vote, summary] = await Promise.all([
      transaction.get(database.doc(`users/${uid}`)),
      transaction.get(voteRef),
      transaction.get(summaryRef),
    ]);
    if (!user.exists || user.get("status") !== "active" || user.get("role") !== "student") {
      throw new HttpsError("permission-denied", "STUDENT_REQUIRED");
    }
    if (!hasVerifiedEduEmail(user)) throw new HttpsError("permission-denied", "EDU_VERIFICATION_REQUIRED");

    const counts = { good: 0, okay: 0, bad: 0 };
    for (const key of RATINGS) counts[key] = Math.max(0, Number(summary.get(key) ?? 0));
    const previous = vote.get("rating") as Rating | undefined;
    if (previous === rating) return { ...counts, rating };
    if (previous && RATINGS.includes(previous)) counts[previous] = Math.max(0, counts[previous] - 1);
    counts[rating] += 1;

    transaction.set(summaryRef, { date, meal, ...counts, updatedAt: FieldValue.serverTimestamp() });
    transaction.set(voteRef, { rating, updatedAt: FieldValue.serverTimestamp() });
    return { ...counts, rating };
  });
}
