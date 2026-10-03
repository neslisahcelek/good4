import assert from "node:assert/strict";
import test from "node:test";
import { copyableCampusEmailLink as parse, campusEmailBrowserProof, createCampusEmailVerificationTask,
  campusEmailBrowserError } from "./src/campusEmailLink.ts";

const host = "good4tr-v2.firebaseapp.com";
const requestId = "a".repeat(64);
const continuation = `https://${host}/campus-email-verification?requestId=${requestId}`;
const action = `https://${host}/__/auth/action?mode=signIn&oobCode=test-code&apiKey=test-key&continueUrl=${encodeURIComponent(continuation)}`;

test("Auth links and Hosting wrappers preserve the bound request", () => {
  assert.equal(parse(action), action);
  assert.equal(parse(`https://${host}/__/auth/links?link=${encodeURIComponent(action)}`), action);
});

test("a request-only landing page is not a copyable verification link", () => {
  assert.equal(parse(continuation), null);
});

test("a complete landing URL is normalized to a Firebase action link", () => {
  const landing = `https://good4tr-v2.web.app/campus-email-verification?mode=signIn&oobCode=test-code&apiKey=test-key&requestId=${requestId}`;
  const parsed = parse(landing);
  assert.ok(parsed);
  const url = new URL(parsed);
  assert.equal(url.hostname, host);
  assert.equal(url.pathname, "/__/auth/action");
  assert.equal(new URL(url.searchParams.get("continueUrl")).searchParams.get("requestId"), requestId);
  assert.equal(parse(parsed), parsed);
  assert.equal(parse(landing.replace("oobCode=test-code&", "")), null);
  assert.equal(parse(landing.replace("apiKey=test-key&", "")), null);
});

test("foreign origins, misleading credentials and other action modes are rejected", () => {
  for (const raw of [action.replace(`https://${host}`, "https://evil.example"), action.replace("https:", "http:"),
    action.replace(`https://${host}`, `https://user@${host}`), action.replace("mode=signIn", "mode=verifyEmail"),
    action.replace(encodeURIComponent(continuation), encodeURIComponent(continuation.replace(host, "evil.example")))]) {
    assert.equal(parse(raw), null);
  }
});

test("duplicate codes and mismatched request IDs are rejected", () => {
  assert.equal(parse(`${action}&oobCode=another`), null);
  assert.equal(parse(`${action}&requestId=${"b".repeat(64)}`), null);
  assert.equal(parse(action.replace(encodeURIComponent(continuation), encodeURIComponent(`${continuation}&requestId=another`))), null);
});

test("oversized links and excessive wrappers are rejected", () => {
  assert.equal(parse("x".repeat(16_385)), null);
  let wrapped = action;
  for (let count = 0; count < 4; count++) wrapped = `https://${host}/__/auth/links?link=${encodeURIComponent(wrapped)}`;
  assert.equal(parse(wrapped), null);
});

test("the real Hosting landing shape carries the bound request and mailbox proof", () => {
  const landing = `https://${host}/campus-email-verification?requestId=${requestId}&apiKey=test-key&oobCode=test-code&mode=signIn&lang=tr`;
  assert.deepEqual(campusEmailBrowserProof(landing), { requestId, oobCode: "test-code" });
  assert.equal(campusEmailBrowserProof(continuation), null);
});

test("repeated page effects consume the Auth code only once and wait for server success", async () => {
  let calls = 0;
  let approve;
  const response = new Promise((resolve) => { approve = resolve; });
  const task = createCampusEmailVerificationTask(action, async (proof) => {
    calls++;
    assert.deepEqual(proof, { requestId, oobCode: "test-code" });
    return response;
  });
  let finished = false;
  const first = task();
  first.then(() => { finished = true; });
  assert.equal(task(), first);
  assert.equal(calls, 1);
  await Promise.resolve();
  assert.equal(finished, false);
  approve({ outcome: "verified" });
  await first;
  assert.equal(finished, true);
});

test("invalid, expired, refused or incomplete responses can never display success", async () => {
  const invalid = createCampusEmailVerificationTask(continuation, async () => { throw new Error("must not send"); });
  await assert.rejects(invalid(), { message: "CAMPUS_EMAIL_PROOF_INVALID" });
  for (const outcome of [undefined, "ready", "already_verified", false]) {
    await assert.rejects(createCampusEmailVerificationTask(action, async () => ({ outcome }))(),
      { message: "CAMPUS_EMAIL_PROOF_INVALID" });
  }
  await assert.rejects(createCampusEmailVerificationTask(action, async () => { throw new Error("CAMPUS_EMAIL_LINK_EXPIRED"); })(),
    { message: "CAMPUS_EMAIL_LINK_EXPIRED" });
});

test("student-facing errors stay short and never include private backend details", () => {
  assert.match(campusEmailBrowserError(new Error("CAMPUS_EMAIL_LINK_EXPIRED")), /süresi doldu/);
  assert.match(campusEmailBrowserError({ code: "functions/unavailable" }), /Biraz sonra/);
  const message = campusEmailBrowserError(new Error("private token and Firebase error details"));
  assert.ok(!message.includes("Firebase") && !message.includes("private"));
});
