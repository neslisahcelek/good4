import { FieldValue, type Firestore } from "firebase-admin/firestore";
import { HttpsError } from "firebase-functions/v2/https";
import { requireGood4Admin } from "./adminPortal.js";

export const COST_CONTROL_PATH = "system/cost_control";
export const OPTIONAL_STOP_USD = 8;
export function productionJobsEnabled() {
  return process.env.GCLOUD_PROJECT === "good4tr-v2" && process.env.FUNCTIONS_EMULATOR !== "true";
}
export function budgetDecision(current: Record<string, unknown>, input: Record<string, unknown>, now = new Date()) {
  const month = now.toISOString().slice(0, 7);
  if (input.currencyCode !== "USD" || typeof input.costAmount !== "number" || !Number.isFinite(input.costAmount)
    || input.costAmount < 0 || typeof input.costIntervalStart !== "string" || input.costIntervalStart.slice(0, 7) !== month
    || input.budgetAmount !== 10) return null;
  const previous = current.budgetMonth === month ? Number(current.observedCostUsd ?? 0) : 0;
  const cost = Math.max(previous, input.costAmount);
  return { budgetMonth: month, observedCostUsd: cost, budgetPaused: cost >= OPTIONAL_STOP_USD };
}
export async function recordBudgetNotification(database: Firestore, input: Record<string, unknown>, now = new Date()) {
  await database.runTransaction(async (transaction) => {
    const ref = database.doc(COST_CONTROL_PATH);
    const snapshot = await transaction.get(ref);
    const decision = budgetDecision(snapshot.data() ?? {}, input, now);
    if (decision) transaction.set(ref, { ...decision, updatedAt: FieldValue.serverTimestamp() }, { merge: true });
  });
}
export async function optionalJobsAllowed(database: Firestore, now = new Date()) {
  const control = (await database.doc(COST_CONTROL_PATH).get()).data();
  return !control?.manualPaused && !(control?.budgetMonth === now.toISOString().slice(0, 7) && control?.budgetPaused);
}
export async function setCostControlService(database: Firestore, uid: string, input: Record<string, unknown>) {
  await requireGood4Admin(database, uid);
  if (typeof input.manualPaused !== "boolean") throw new HttpsError("invalid-argument", "COST_CONTROL_INVALID");
  await database.doc(COST_CONTROL_PATH).set({ manualPaused: input.manualPaused, updatedAt: FieldValue.serverTimestamp(), actorUid: uid }, { merge: true });
  return { optionalJobsAllowed: await optionalJobsAllowed(database) };
}
