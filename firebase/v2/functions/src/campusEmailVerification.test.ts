import assert from "node:assert/strict";
import { after, beforeEach, test } from "node:test";
import type { DecodedIdToken } from "firebase-admin/auth";
import { getAuth } from "firebase-admin/auth";
import { initializeApp, deleteApp } from "firebase/app";
import { getAuth as getClientAuth, connectAuthEmulator, sendSignInLinkToEmail,
  signInWithEmailAndPassword, signInWithEmailLink } from "firebase/auth";
import { db, legacyTestDb } from "./firebase.js";
import {
  beginCampusEmailVerificationService as begin,
  completeCampusEmailVerificationService as complete,
  completeCampusEmailVerificationFromBrowserService as completeBrowser,
  getCampusEmailVerificationStatusService as status,
  type CampusBrowserVerificationDeps,
  isCampusEmail,
  type CampusEmailVerificationDeps,
} from "./campusEmailVerification.js";
import { eduEmailClaimPath } from "./eduVerification.js";

const uid = "primary-google-student";
const email = "can@ogr.akdeniz.edu.tr";
const NOW = Date.UTC(2026, 9, 3, 10);
const proof = (overrides: Partial<DecodedIdToken> = {}): DecodedIdToken => ({
  uid: "secondary-university-identity", sub: "secondary-university-identity",
  aud: "demo-good4-v2", iss: "https://securetoken.google.com/demo-good4-v2",
  iat: NOW / 1000 + 10, exp: NOW / 1000 + 3600, auth_time: NOW / 1000 + 10,
  email, email_verified: true, firebase: { identities: {}, sign_in_provider: "password" }, ...overrides,
});
const verifier = (value = proof()) => ({ verifyToken: async (token: string) => {
  assert.equal(token, "signed-university-token");
  return value;
} });
const browserDeps = (value = proof()): CampusBrowserVerificationDeps => ({
  ...verifier(value),
  exchangeEmailLink: async (address, code) => {
    assert.equal(address, email);
    assert.equal(code, "mailbox-code");
    return "signed-university-token";
  },
});
const rejected = (promise: Promise<unknown>, message: string) =>
  assert.rejects(promise, (error: Error) => error.message === message);

beforeEach(async () => {
  await Promise.all(["users", "campusEmailVerifications", "eduEmailClaims", "mail", "auditLogs"].map((name) =>
    db.recursiveDelete(db.collection(name))));
  await db.doc(`users/${uid}`).set({ role: "student", status: "active", email: "can@gmail.com" });
});
after(async () => { await Promise.all([db.terminate(), legacyTestDb.terminate()]); });

async function request() {
  const result = await begin(db, uid, { email: " Can@OGR.AKDENIZ.EDU.TR " }, NOW);
  assert.equal(result.outcome, "ready");
  if (result.outcome !== "ready") throw new Error("unexpected existing verification");
  return result;
}

test("only the exact Akdeniz student domain is accepted", () => {
  assert.ok(isCampusEmail(" CAN@OGR.AKDENIZ.EDU.TR "));
  for (const value of ["can@akdeniz.edu.tr", "can@ogr.other.edu.tr", "can@sub.ogr.akdeniz.edu.tr",
    "can@ogr.akdeniz.edu.tr.evil.com", "can@@ogr.akdeniz.edu.tr", "can @ogr.akdeniz.edu.tr", "can@gmail.com"]) {
    assert.equal(isCampusEmail(value), false, value);
  }
});

test("a request is account-bound, stores only a hash and does not enqueue SMTP mail", async () => {
  const result = await request();
  assert.equal(result.email, email);
  assert.match(result.requestId, /^[a-f0-9]{64}$/);
  assert.equal(new URL(result.continueUrl).searchParams.get("requestId"), result.requestId);
  const stored = (await db.doc(`campusEmailVerifications/${uid}`).get()).data();
  assert.equal(stored?.email, email);
  assert.ok(!JSON.stringify(stored).includes(result.requestId));
  assert.equal((await db.collection("mail").get()).size, 0);
});

test("a fresh signed university proof verifies the original account without changing its email", async () => {
  const result = await request();
  assert.deepEqual(await complete(db, uid, { requestId: result.requestId, universityIdToken: "signed-university-token" },
    verifier(), NOW + 15_000), { outcome: "verified", email });
  const user = await db.doc(`users/${uid}`).get();
  assert.equal(user.get("email"), "can@gmail.com");
  assert.equal(user.get("eduEmail"), email);
  assert.equal(user.get("eduVerified"), true);
  assert.equal((await db.doc(eduEmailClaimPath(email)).get()).get("uid"), uid);
  assert.equal((await db.doc("users/secondary-university-identity").get()).exists, false);
  await rejected(complete(db, uid, { requestId: result.requestId, universityIdToken: "signed-university-token" },
    verifier(), NOW + 16_000), "CAMPUS_EMAIL_REQUEST_MISMATCH");
});

test("only an isolated school Auth identity is deleted after verification commits", async () => {
  const result = await request();
  const deleted: string[] = [];
  const deps: CampusEmailVerificationDeps = {
    ...verifier(),
    getIdentity: async (identityUid) => ({ uid: identityUid, email, providerIds: ["password"] }),
    deleteIdentity: async (identityUid) => {
      assert.equal((await db.doc(`users/${uid}`).get()).get("eduVerified"), true);
      assert.ok((await db.doc(`campusEmailVerifications/${uid}`).get()).get("consumedAt"));
      deleted.push(identityUid);
    },
  };
  await complete(db, uid, { requestId: result.requestId, universityIdToken: "signed-university-token" }, deps, NOW + 15_000);
  assert.deepEqual(deleted, [proof().uid]);
});

test("primary accounts and school identities with Good4 profiles or other providers are kept", async () => {
  const cases = [
    { primary: true, profile: false, providers: ["password"], identityEmail: email },
    { primary: false, profile: true, providers: ["password"], identityEmail: email },
    { primary: false, profile: false, providers: ["password", "google.com"], identityEmail: email },
    { primary: false, profile: false, providers: ["google.com"], identityEmail: email },
    { primary: false, profile: false, providers: [], identityEmail: email },
    { primary: false, profile: false, providers: ["password"], identityEmail: "other@ogr.akdeniz.edu.tr" },
  ];
  for (const [index, scenario] of cases.entries()) {
    const mainUid = `${uid}-${index}`;
    const identityUid = scenario.primary ? mainUid : `school-identity-${index}`;
    await db.doc(`users/${mainUid}`).set({ role: "student", status: "active", email: "primary@example.test" });
    if (scenario.profile) await db.doc(`users/${identityUid}`).set({ role: "student", status: "active" });
    const requestEmail = `can${index}@ogr.akdeniz.edu.tr`;
    const result = await begin(db, mainUid, { email: requestEmail }, NOW);
    assert.equal(result.outcome, "ready");
    if (result.outcome !== "ready") throw new Error("expected new request");
    let deleted = false;
    const deps: CampusEmailVerificationDeps = {
      ...verifier(proof({ uid: identityUid, sub: identityUid, email: requestEmail })),
      getIdentity: async () => ({ uid: identityUid,
        email: scenario.identityEmail === email ? requestEmail : scenario.identityEmail, providerIds: scenario.providers }),
      deleteIdentity: async () => { deleted = true; },
    };
    await complete(db, mainUid, { requestId: result.requestId, universityIdToken: "signed-university-token" }, deps, NOW + 15_000);
    assert.equal(deleted, false, JSON.stringify(scenario));
    assert.equal((await db.doc(`users/${mainUid}`).get()).get("eduVerified"), true);
  }
});

test("cleanup failure is logged without secrets and cannot undo a successful verification", async () => {
  const result = await request();
  const warnings: unknown[][] = [];
  const originalWarn = console.warn;
  console.warn = (...args: unknown[]) => { warnings.push(args); };
  try {
    const deps: CampusEmailVerificationDeps = {
      ...verifier(),
      getIdentity: async (identityUid) => ({ uid: identityUid, email, providerIds: ["password"] }),
      deleteIdentity: async () => { throw Object.assign(new Error("private details"), { code: "auth/internal-error" }); },
    };
    assert.deepEqual(await complete(db, uid, { requestId: result.requestId, universityIdToken: "signed-university-token" },
      deps, NOW + 15_000), { outcome: "verified", email });
    assert.equal((await db.doc(`users/${uid}`).get()).get("eduVerified"), true);
    assert.deepEqual(warnings, [["campusEmail: temporary Auth identity cleanup failed", { code: "auth/internal-error" }]]);
  } finally {
    console.warn = originalWarn;
  }
});

test("forged, revoked, unverified, wrong-domain and old-session proofs are rejected", async () => {
  const result = await request();
  const input = { requestId: result.requestId, universityIdToken: "signed-university-token" };
  await rejected(complete(db, uid, input, { verifyToken: async () => { throw new Error("invalid signature"); } }, NOW + 15_000),
    "CAMPUS_EMAIL_PROOF_INVALID");
  for (const bad of [proof({ email_verified: false }), proof({ email: "can@akdeniz.edu.tr" }),
    proof({ auth_time: NOW / 1000 - 1 }), proof({ firebase: { identities: {}, sign_in_provider: "google.com" } })]) {
    await rejected(complete(db, uid, input, verifier(bad), NOW + 15_000), "CAMPUS_EMAIL_PROOF_INVALID");
  }
  await rejected(complete(db, uid, input, verifier(proof({ email: "another@ogr.akdeniz.edu.tr" })), NOW + 15_000),
    "CAMPUS_EMAIL_REQUEST_MISMATCH");
  assert.notEqual((await db.doc(`users/${uid}`).get()).get("eduVerified"), true);
});

test("another primary account cannot consume the link, and requests expire", async () => {
  const result = await request();
  await db.doc("users/another-primary").set({ role: "student", status: "active", email: "other@gmail.com" });
  const input = { requestId: result.requestId, universityIdToken: "signed-university-token" };
  await rejected(complete(db, "another-primary", input, verifier(), NOW + 15_000), "CAMPUS_EMAIL_REQUEST_MISMATCH");
  await rejected(complete(db, uid, input, verifier(), result.expiresAtMillis), "CAMPUS_EMAIL_LINK_EXPIRED");
});

test("a later request invalidates the old link and the send limit cannot be bypassed by changing addresses", async () => {
  const first = await request();
  await rejected(begin(db, uid, { email: "other@ogr.akdeniz.edu.tr" }, NOW + 30_000), "CAMPUS_EMAIL_RESEND_TOO_SOON");
  await begin(db, uid, { email }, NOW + 61_000);
  await rejected(complete(db, uid, { requestId: first.requestId, universityIdToken: "signed-university-token" },
    verifier(proof({ auth_time: NOW / 1000 + 70 })), NOW + 75_000), "CAMPUS_EMAIL_REQUEST_MISMATCH");
  for (let count = 3; count <= 5; count++) await begin(db, uid, { email }, NOW + count * 61_000);
  await rejected(begin(db, uid, { email: "other@ogr.akdeniz.edu.tr" }, NOW + 6 * 61_000), "CAMPUS_EMAIL_SEND_LIMIT");
});

test("a university address claimed during verification cannot be stolen", async () => {
  const result = await request();
  await db.doc(eduEmailClaimPath(email)).set({ uid: "already-owner" });
  await rejected(complete(db, uid, { requestId: result.requestId, universityIdToken: "signed-university-token" },
    verifier(), NOW + 15_000), "EDU_EMAIL_IN_USE");
  assert.equal((await db.doc(eduEmailClaimPath(email)).get()).get("uid"), "already-owner");
});

test("inactive and non-student accounts cannot initiate or finish verification", async () => {
  const result = await request();
  await db.doc(`users/${uid}`).update({ role: "businessStaff" });
  await rejected(begin(db, uid, { email }, NOW + 61_000), "ROLE_NOT_ALLOWED");
  await rejected(complete(db, uid, { requestId: result.requestId, universityIdToken: "signed-university-token" },
    verifier(), NOW + 15_000), "ROLE_NOT_ALLOWED");
  await db.doc(`users/${uid}`).update({ role: "student", status: "suspended" });
  await rejected(begin(db, uid, { email }, NOW + 61_000), "ACCOUNT_NOT_ACTIVE");
});

test("a browser proof verifies the bound Good4 account without a primary browser login", async () => {
  const result = await request();
  assert.deepEqual(await status(db, uid), { verified: false });
  assert.deepEqual(await completeBrowser(db, { requestId: result.requestId, oobCode: "mailbox-code" },
    browserDeps(), NOW + 15_000), { outcome: "verified" });
  assert.deepEqual(await status(db, uid), { verified: true, email });
  assert.equal((await db.doc(`users/${uid}`).get()).get("email"), "can@gmail.com");
  const stored = (await db.doc(`campusEmailVerifications/${uid}`).get()).data();
  assert.ok(!JSON.stringify(stored).includes(result.requestId));
  assert.ok(!JSON.stringify(stored).includes("mailbox-code"));
  assert.ok(!JSON.stringify(stored).includes("signed-university-token"));
});

test("request ID alone, malformed proof and unknown requests cannot approve an account", async () => {
  const result = await request();
  const deps = { ...browserDeps(), exchangeEmailLink: async () => { throw new Error("must not exchange"); } };
  for (const input of [{ requestId: result.requestId }, { requestId: "invalid", oobCode: "mailbox-code" },
    { requestId: result.requestId, oobCode: "x".repeat(2049) }, { requestId: result.requestId, oobCode: "a&b" }]) {
    await rejected(completeBrowser(db, input, deps, NOW + 15_000), "CAMPUS_EMAIL_PROOF_INVALID");
  }
  await rejected(completeBrowser(db, { requestId: "b".repeat(64), oobCode: "mailbox-code" }, deps, NOW + 15_000),
    "CAMPUS_EMAIL_REQUEST_MISMATCH");
  assert.deepEqual(await status(db, uid), { verified: false });
});

test("browser completion rejects forged or mismatched mailbox proofs", async () => {
  const result = await request();
  const input = { requestId: result.requestId, oobCode: "mailbox-code" };
  await rejected(completeBrowser(db, input, { ...browserDeps(), verifyToken: async () => { throw new Error("forged"); } },
    NOW + 15_000), "CAMPUS_EMAIL_PROOF_INVALID");
  await rejected(completeBrowser(db, input, browserDeps(proof({ email: "another@ogr.akdeniz.edu.tr" })),
    NOW + 15_000), "CAMPUS_EMAIL_REQUEST_MISMATCH");
  await rejected(completeBrowser(db, input, browserDeps(proof({ email_verified: false })),
    NOW + 15_000), "CAMPUS_EMAIL_PROOF_INVALID");
  assert.deepEqual(await status(db, uid), { verified: false });
});

test("expired and inactive browser requests are rejected before consuming the Auth code", async () => {
  const result = await request();
  const input = { requestId: result.requestId, oobCode: "mailbox-code" };
  const deps = { ...browserDeps(), exchangeEmailLink: async () => { throw new Error("must not exchange"); } };
  await rejected(completeBrowser(db, input, deps, result.expiresAtMillis), "CAMPUS_EMAIL_LINK_EXPIRED");
  await db.doc(`users/${uid}`).update({ status: "suspended" });
  await rejected(completeBrowser(db, input, deps, NOW + 15_000), "ACCOUNT_NOT_ACTIVE");
});

test("reopening is idempotent only for the exact request and successfully consumed mailbox code", async () => {
  const result = await request();
  const input = { requestId: result.requestId, oobCode: "mailbox-code" };
  await completeBrowser(db, input, browserDeps(), NOW + 15_000);
  const deps = { ...browserDeps(), exchangeEmailLink: async () => { throw new Error("must not exchange again"); } };
  assert.deepEqual(await completeBrowser(db, input, deps, NOW + 16_000), { outcome: "verified" });
  await rejected(completeBrowser(db, { ...input, oobCode: "another-code" }, deps, NOW + 16_000),
    "CAMPUS_EMAIL_REQUEST_MISMATCH");
  await rejected(completeBrowser(db, { ...input, requestId: "b".repeat(64) }, deps, NOW + 16_000),
    "CAMPUS_EMAIL_REQUEST_MISMATCH");
  await db.doc(`users/${uid}`).update({ eduVerified: false });
  await rejected(completeBrowser(db, input, deps, NOW + 16_000), "CAMPUS_EMAIL_REQUEST_MISMATCH");
});

test("a later browser request invalidates the old link and existing claims remain protected", async () => {
  const first = await request();
  const latest = await begin(db, uid, { email }, NOW + 61_000);
  if (latest.outcome !== "ready") throw new Error("expected new request");
  await rejected(completeBrowser(db, { requestId: first.requestId, oobCode: "mailbox-code" }, browserDeps(), NOW + 75_000),
    "CAMPUS_EMAIL_REQUEST_MISMATCH");
  await db.doc(eduEmailClaimPath(email)).set({ uid: "already-owner" });
  await rejected(completeBrowser(db, { requestId: latest.requestId, oobCode: "mailbox-code" },
    browserDeps(proof({ auth_time: NOW / 1000 + 70 })), NOW + 75_000), "EDU_EMAIL_IN_USE");
  assert.deepEqual(await status(db, uid), { verified: false });
});

test("a historical generic eduVerified flag does not satisfy Akdeniz status", async () => {
  await db.doc(`users/${uid}`).update({ eduVerified: true, eduEmail: "can@other.edu.tr" });
  assert.deepEqual(await status(db, uid), { verified: false });
  await db.doc(`users/${uid}`).update({ role: "businessStaff" });
  await rejected(status(db, uid), "ROLE_NOT_ALLOWED");
});

test("the real REST browser flow leaves primary Google Auth unchanged and cleans up the temporary school identity",
  { skip: !process.env.FIREBASE_AUTH_EMULATOR_HOST }, async () => {
    const emulator = process.env.FIREBASE_AUTH_EMULATOR_HOST!;
    const app = initializeApp({ projectId: "demo-good4-v2", apiKey: "emulator-api-key" }, "campus-browser-mail-sender");
    const schoolAuth = getClientAuth(app);
    connectAuthEmulator(schoolAuth, `http://${emulator}`, { disableWarnings: true });
    try {
      await getAuth().createUser({ uid, email: "primary-google@example.test", emailVerified: true });
      await getAuth().updateUser(uid, { providerToLink: {
        providerId: "google.com", uid: "google-provider-student", email: "primary-google@example.test",
      } });
      const result = await begin(db, uid, { email });
      if (result.outcome !== "ready") throw new Error("expected pending request");
      await sendSignInLinkToEmail(schoolAuth, email, { url: result.continueUrl, handleCodeInApp: true });
      const messages = await fetch(`http://${emulator}/emulator/v1/projects/demo-good4-v2/oobCodes`).then((r) => r.json()) as {
        oobCodes: { email: string; requestType: string; oobCode: string }[];
      };
      const mail = messages.oobCodes.filter((value) => value.email === email && value.requestType === "EMAIL_SIGNIN").at(-1);
      assert.ok(mail);
      assert.deepEqual(await completeBrowser(db, { requestId: result.requestId, oobCode: mail.oobCode }),
        { outcome: "verified" });
      assert.deepEqual(await status(db, uid), { verified: true, email });
      const main = await getAuth().getUser(uid);
      assert.equal(main.email, "primary-google@example.test");
      assert.deepEqual(main.providerData.map((provider) => provider.providerId), ["google.com"]);
      assert.equal(schoolAuth.currentUser, null);
      await assert.rejects(getAuth().getUserByEmail(email), (error: { code?: string }) => error.code === "auth/user-not-found");
    } finally {
      await deleteApp(app);
      await getAuth().deleteUser(uid).catch(() => undefined);
      const school = await getAuth().getUserByEmail(email).catch(() => null);
      if (school) await getAuth().deleteUser(school.uid);
    }
  });

test("Firebase's real email-link flow uses a separate Auth session and the Admin verifier accepts its proof",
  { skip: !process.env.FIREBASE_AUTH_EMULATOR_HOST }, async () => {
    const emulator = process.env.FIREBASE_AUTH_EMULATOR_HOST!;
    const config = { projectId: "demo-good4-v2", apiKey: "emulator-api-key" };
    const primaryApp = initializeApp(config, "campus-test-primary");
    const universityApp = initializeApp(config, "campus-test-university");
    const primaryAuth = getClientAuth(primaryApp);
    const universityAuth = getClientAuth(universityApp);
    connectAuthEmulator(primaryAuth, `http://${emulator}`, { disableWarnings: true });
    connectAuthEmulator(universityAuth, `http://${emulator}`, { disableWarnings: true });
    let universityUid: string | undefined;
    try {
      await getAuth().createUser({ uid, email: "primary@example.test", password: "emulator-only-password", emailVerified: true });
      await signInWithEmailAndPassword(primaryAuth, "primary@example.test", "emulator-only-password");
      const result = await begin(db, uid, { email });
      assert.equal(result.outcome, "ready");
      if (result.outcome !== "ready") throw new Error("expected pending request");
      await sendSignInLinkToEmail(universityAuth, email, { url: result.continueUrl, handleCodeInApp: true });
      const messages = await fetch(`http://${emulator}/emulator/v1/projects/demo-good4-v2/oobCodes`).then((r) => r.json()) as {
        oobCodes: { email: string; requestType: string; oobLink: string }[];
      };
      const mail = messages.oobCodes.filter((value) => value.email === email && value.requestType === "EMAIL_SIGNIN").at(-1);
      assert.ok(mail);
      const signedIn = await signInWithEmailLink(universityAuth, email, mail.oobLink);
      universityUid = signedIn.user.uid;
      const token = await signedIn.user.getIdToken(true);
      assert.deepEqual(await complete(db, uid, { requestId: result.requestId, universityIdToken: token }),
        { outcome: "verified", email });
      assert.equal(primaryAuth.currentUser?.uid, uid);
      assert.equal(primaryAuth.currentUser?.email, "primary@example.test");
      assert.notEqual(universityUid, uid);
      assert.equal((await getAuth().getUser(uid)).email, "primary@example.test");
      assert.equal((await db.doc(`users/${uid}`).get()).get("email"), "can@gmail.com");
      assert.equal((await db.collection("mail").get()).size, 0);
      await assert.rejects(getAuth().getUser(universityUid), (error: { code?: string }) => error.code === "auth/user-not-found");
    } finally {
      await Promise.all([deleteApp(primaryApp), deleteApp(universityApp)]);
      await getAuth().deleteUser(uid).catch(() => undefined);
      if (universityUid) await getAuth().deleteUser(universityUid).catch((error: { code?: string }) => {
        if (error.code !== "auth/user-not-found") throw error;
      });
    }
  });
