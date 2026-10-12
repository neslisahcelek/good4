import assert from "node:assert/strict";
import { test } from "node:test";
import {
  discountDates,
  discountNumbers,
  istanbulDateTime,
} from "../src/businessDiscountForm.ts";

test("admin dates always use Istanbul rather than browser timezone", () => {
  const dates = discountDates("2026-10-12T18:00", "2026-10-13T19:00");
  assert.equal(dates.startsAt, "2026-10-12T15:00:00.000Z");
  assert.equal(dates.endsAt, "2026-10-13T16:00:00.000Z");
  assert.equal(istanbulDateTime(dates.startsAt), "2026-10-12T18:00");
  assert.throws(() => discountDates("2026-10-13T19:00", "2026-10-12T18:00"));
  assert.throws(() => discountDates("", ""));
});
test("percentage and optional quota reject invalid values before submission", () => {
  assert.deepEqual(discountNumbers("20", ""), {
    discountValue: 20,
    totalLimit: null,
  });
  assert.deepEqual(discountNumbers("100", "50"), {
    discountValue: 100,
    totalLimit: 50,
  });
  for (const number of ["0", "101", "20.5", "abc", ""])
    assert.throws(() => discountNumbers(number, ""));
  for (const quota of ["0", "-1", "2.5", "1000001", "abc"])
    assert.throws(() => discountNumbers("20", quota));
});

test("optional Maps URL validates before submission with a readable error", async () => {
  const { normalizeGoogleMapsUrl } = await import(
    "../../functions/src/businessDiscountMaps.ts"
  );
  assert.equal(
    normalizeGoogleMapsUrl("  https://maps.app.goo.gl/venue  "),
    "https://maps.app.goo.gl/venue",
  );
  assert.equal(normalizeGoogleMapsUrl(""), "");
  for (const value of [
    "javascript:alert(1)",
    "https://evil.example/maps",
    "https://maps.app.goo.gl.evil.example/venue",
  ])
    assert.throws(() => normalizeGoogleMapsUrl(value), /Google Maps/);
});
