import assert from "node:assert/strict";
import { test } from "node:test";
import { budgetDecision } from "./costControl.js";
const now = new Date("2026-10-10T00:00:00Z");
const notification = { currencyCode: "USD", costAmount: 8, costIntervalStart: "2026-10-01T00:00:00Z", budgetAmount: 10 };
test("$8 pauses optional work; lower or duplicate events never reopen the month", () => {
  const stopped = budgetDecision({}, notification, now)!;
  assert.equal(stopped.budgetPaused, true);
  assert.deepEqual(budgetDecision(stopped, { ...notification, costAmount: 3 }, now), stopped);
  assert.deepEqual(budgetDecision(stopped, notification, now), stopped);
});
test("old, future, malformed and other-currency events are ignored; a new month resets budget pause", () => {
  for (const changed of [{ currencyCode: "TRY" }, { costAmount: NaN }, { budgetAmount: 100 }, { costIntervalStart: "2026-09-01" }, { costIntervalStart: "2026-11-01" }]) {
    assert.equal(budgetDecision({}, { ...notification, ...changed }, now), null);
  }
  assert.equal(budgetDecision({ budgetMonth: "2026-09", observedCostUsd: 50 }, { ...notification, costAmount: 1 }, now)?.budgetPaused, false);
});
