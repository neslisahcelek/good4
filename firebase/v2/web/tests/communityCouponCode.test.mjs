import assert from "node:assert/strict";
import { test } from "node:test";
import {
  normalizeCouponCode,
  couponCodeRoute,
  formatCommunityCouponDate,
} from "../src/communityCouponCode.ts";
test("business verification routes 4, 6 and 8 digits and preserves leading zeroes", () => {
  assert.equal(normalizeCouponCode("00 12"), "0012");
  assert.equal(couponCodeRoute("0012"), "communityCoupon");
  assert.equal(couponCodeRoute("123456"), "legacy");
  assert.equal(couponCodeRoute("12345678"), "campaign");
  for (const code of ["", "123", "12345", "1234567", "abcd"])
    assert.equal(couponCodeRoute(code), null);
});

test("coupon review dates use Turkish text and the Istanbul calendar day", () => {
  assert.equal(formatCommunityCouponDate("2026-10-11T21:30:00Z"), "12 Ekim 2026 00:30");
});
