import { FieldValue, type Firestore } from "firebase-admin/firestore";
import { HttpsError } from "firebase-functions/v2/https";

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

/**
 * One anonymous 😋/😐/😕 vote per signed-in account, meal and day; voting again replaces the earlier vote.
 * Only the counters on meal_ratings/{date}_{meal} are readable by others.
 */
export async function rateMealService(
  database: Firestore,
  uid: string,
  input: { meal?: unknown; rating?: unknown; date?: unknown },
  nowMillis = Date.now(),
): Promise<{ good: number; okay: number; bad: number; rating: Rating }> {
  const meal = input.meal as Meal;
  const rating = input.rating as Rating;
  if (!MEALS.includes(meal)) throw new HttpsError("invalid-argument", "MEAL_INVALID");
  if (!RATINGS.includes(rating)) throw new HttpsError("invalid-argument", "RATING_INVALID");

  const { date, hour } = istanbulNow(nowMillis);
  if (input.date !== undefined && input.date !== date) throw new HttpsError("failed-precondition", "MEAL_DATE_CHANGED");
  if (hour < OPENS_AT_HOUR[meal]) throw new HttpsError("failed-precondition", "MEAL_NOT_STARTED");

  const summaryRef = database.doc(`meal_ratings/${date}_${meal}`);
  const voteRef = summaryRef.collection("votes").doc(uid);
  return database.runTransaction(async (transaction) => {
    const [user, vote, summary, menu] = await Promise.all([
      transaction.get(database.doc(`users/${uid}`)),
      transaction.get(voteRef),
      transaction.get(summaryRef),
      transaction.get(database.doc(meal === "cafeteria" ? "app_config/akdeniz_dining_menu" : `kyk_menu_days/${date}`)),
    ]);
    // For now any active account may vote; no student role or edu verification is required.
    if (!user.exists || user.get("status") !== "active") throw new HttpsError("permission-denied", "ACCOUNT_NOT_ACTIVE");

    const items = meal === "cafeteria"
      ? (Array.isArray(menu.get("days")) ? menu.get("days") : [])
        .find((day: { date?: unknown }) => day?.date === date)?.meals
      : menu.get(meal === "kyk_breakfast" ? "breakfast" : "dinner");
    if (!Array.isArray(items) || items.length === 0) throw new HttpsError("failed-precondition", "MEAL_NOT_PUBLISHED");

    const counts = { good: 0, okay: 0, bad: 0 };
    for (const key of RATINGS) counts[key] = Math.max(0, Number(summary.get(key) ?? 0));
    const previous = vote.get("rating") as Rating | undefined;
    if (previous === rating) return { ...counts, rating };
    if (previous && RATINGS.includes(previous)) counts[previous] = Math.max(0, counts[previous] - 1);
    counts[rating] += 1;

    transaction.set(summaryRef, { date, meal, ...counts, updatedAt: FieldValue.serverTimestamp() });
    transaction.set(voteRef, { userId: uid, rating, updatedAt: FieldValue.serverTimestamp() });
    return { ...counts, rating };
  });
}
