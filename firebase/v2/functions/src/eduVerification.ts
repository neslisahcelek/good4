import { createHash, randomInt, randomUUID, timingSafeEqual } from "node:crypto";
import {
  type DocumentSnapshot,
  FieldValue,
  type Firestore,
  Timestamp,
} from "firebase-admin/firestore";
import { HttpsError } from "firebase-functions/v2/https";
import { getAuth } from "firebase-admin/auth";
import { hasCampusEmailProof, requireVerificationActor } from "./campusAccess.js";
import { assertRateLimit, assertRateLimits } from "./rateLimit.js";
import { sendVerificationEmail, VerificationEmailDeliveryError, type VerificationEmailSender } from "./verificationEmail.js";

const CODE_TTL_MS = 10 * 60 * 1000;
// Mail usually lands in 1-2 minutes; a shorter wait invites a second code that voids the first.
// Product decision (10.10.2026): keep 3 minutes.
const RESEND_COOLDOWN_MS = 3 * 60 * 1000;
const SEND_WINDOW_MS = 60 * 60 * 1000;
const MAX_SENDS_PER_WINDOW = 5;
const MAX_ATTEMPTS = 5;
// Abuse limits on top of the per-account cooldown: new accounts are cheap, mail quota is not
// (Brevo free plan: 300/day). Reserve before delivery and refund definite rejections.
const MAX_SENDS_PER_ACCOUNT_DAY = 10;
const MAX_SENDS_PER_RECIPIENT_HOUR = 3;
const MAX_SENDS_TOTAL_DAY = 250;

// Only Akdeniz student addresses: Askıda Yemek, Kampüs Dolabı and Sosyal share one verification.
const EDU_EMAIL_PATTERN = /^[^\s@/<>]+@ogr\.akdeniz\.edu\.tr$/;

export function isEduEmail(email: string): boolean {
  return EDU_EMAIL_PATTERN.test(email.trim().toLowerCase());
}

/** Genuine school-email proof only. Feature authorization uses campusAccess.ts. */
export function hasVerifiedEduEmail(user: DocumentSnapshot): boolean {
  return hasCampusEmailProof(user);
}

export function eduEmailClaimPath(email: string): string {
  return `eduEmailClaims/${createHash("sha256").update(email).digest("hex")}`;
}

function hashCode(uid: string, code: string): Buffer {
  return createHash("sha256").update(`${uid}:${code}`).digest();
}

function normalizeEduEmail(value: unknown): string {
  if (typeof value !== "string") {
    throw new HttpsError("invalid-argument", "EDU_EMAIL_REQUIRED");
  }
  const email = value.trim().toLowerCase();
  if (email.length > 254 || !isEduEmail(email)) {
    throw new HttpsError("invalid-argument", "EDU_EMAIL_INVALID");
  }
  return email;
}

export type RequestEduVerificationResult =
  | { outcome: "sent"; email: string; expiresAt: string; resendAfterSeconds: number; sessionId?: string }
  | { outcome: "already_verified"; email: string };

export interface EduVerificationDeps {
  send: VerificationEmailSender;
  identity?: (uid: string) => Promise<{ email?: string; emailVerified: boolean; disabled?: boolean }>;
}

// The normal student flows accept only campus addresses; test recipients are selected by the central policy.
function requireScope(email: string, campus: boolean): void {
  if (campus && !/^[^\s@/<>]+@ogr\.akdeniz\.edu\.tr$/.test(email)) {
    throw new HttpsError("invalid-argument", "CAMPUS_EMAIL_INVALID");
  }
}

/** Reserve delivery separately so Firestore retries never send duplicate mail.
 * Definite rejections refund delivery quota; ambiguous transport failures retain it.
 * A failed send preserves the previous code. Only the hash is persisted after successful send. */
export async function requestEduVerificationService(
  database: Firestore,
  actorUid: string,
  input: { email?: unknown },
  now = Date.now(),
  deps: EduVerificationDeps = { send: sendVerificationEmail },
  campus = false,
): Promise<RequestEduVerificationResult> {
  const initial = await database.runTransaction((transaction) => requireVerificationActor(database, transaction, actorUid));
  const grant = initial.grant;
  const generation = Number(initial.user.get("adminEduTestGeneration") ?? 0);
  let email: string;
  if (grant) {
    const identity = await (deps.identity ?? ((uid: string) => getAuth().getUser(uid)))(actorUid);
    email = identity.email?.trim().toLowerCase() ?? "";
    if (!identity.emailVerified || identity.disabled || email !== grant.email) {
      throw new HttpsError("permission-denied", "CAMPUS_TEST_EMAIL_UNAVAILABLE");
    }
    if (input.email !== undefined && input.email !== "" && input.email !== email) {
      throw new HttpsError("invalid-argument", "CAMPUS_TEST_RECIPIENT_FIXED");
    }
  } else {
    email = normalizeEduEmail(input.email);
    requireScope(email, campus);
  }
  const assertActor = async (transaction: import("firebase-admin/firestore").Transaction) => {
    const current = await requireVerificationActor(database, transaction, actorUid);
    if (current.grant?.grantId !== grant?.grantId || current.grant?.email !== grant?.email
      || (grant && Number(current.user.get("adminEduTestGeneration") ?? 0) !== generation)) {
      throw new HttpsError("permission-denied", "CAMPUS_TEST_SESSION_INVALID");
    }
    return current.user;
  };
  // This shared attempt window also covers failures and both callable aliases.
  // It commits separately so rejected delivery cannot refund an expensive retry.
  await database.runTransaction(async (transaction) => {
    await assertActor(transaction);
    await assertRateLimit(database, transaction, { key: `eduMailRequest:${actorUid}`, limit: 5,
      windowSeconds: 60, errorMessage: "EDU_CODE_REQUEST_LIMIT" }, now);
  });
  const verificationRef = database.doc(`eduVerifications/${actorUid}`);
  const sendId = randomUUID();
  const sendLimits = [
    { key: `eduMail:account:${actorUid}`, limit: MAX_SENDS_PER_ACCOUNT_DAY, windowSeconds: 86_400, errorMessage: "EDU_CODE_ACCOUNT_DAILY_LIMIT" },
    { key: `eduMail:recipient:${createHash("sha256").update(email).digest("hex")}`, limit: MAX_SENDS_PER_RECIPIENT_HOUR,
      windowSeconds: 3_600, errorMessage: "EDU_CODE_RECIPIENT_LIMIT" },
    { key: "eduMail:total", limit: MAX_SENDS_TOTAL_DAY, windowSeconds: 86_400, errorMessage: "EDU_CODE_DAILY_LIMIT" },
  ];
  const limitRefs = sendLimits.map(({ key }) => database.doc(`rateLimits/${createHash("sha256").update(key).digest("hex")}`));
  const reservation = await database.runTransaction(async (transaction) => {
    await assertActor(transaction);
    const user = await transaction.get(database.doc(`users/${actorUid}`));
    const [verification, claim] = await Promise.all([
      transaction.get(verificationRef), grant ? Promise.resolve(null) : transaction.get(database.doc(eduEmailClaimPath(email))),
    ]);
    if (grant ? user.get("adminEduTestVerification.grantId") === grant.grantId
      && user.get("adminEduTestVerification.verifiedAt") instanceof Timestamp
      && user.get("adminEduTestVerification.email") === email
      : hasVerifiedEduEmail(user) && user.get("eduEmail") === email) {
      return { outcome: "already_verified" as const, email };
    }
    if (claim?.exists && claim.get("uid") !== actorUid) throw new HttpsError("already-exists", "EDU_EMAIL_IN_USE");
    const pendingUntil = verification.get("pendingUntil");
    if (pendingUntil instanceof Timestamp && pendingUntil.toMillis() > now) {
      throw new HttpsError("unavailable", "EDU_EMAIL_SEND_IN_PROGRESS");
    }
    const lastSentAt = verification.get("lastSentAt");
    if (lastSentAt instanceof Timestamp && now - lastSentAt.toMillis() < RESEND_COOLDOWN_MS) {
      throw new HttpsError("resource-exhausted", "EDU_CODE_RESEND_TOO_SOON");
    }
    const windowStartedAt = verification.get("windowStartedAt");
    const windowOpen = windowStartedAt instanceof Timestamp && now - windowStartedAt.toMillis() < SEND_WINDOW_MS;
    const sendCount = windowOpen ? Number(verification.get("sendCount") ?? 0) : 0;
    if (sendCount >= MAX_SENDS_PER_WINDOW) throw new HttpsError("resource-exhausted", "EDU_CODE_SEND_LIMIT");
    // Keys are stored in clear text, so the recipient is hashed.
    const reservedLimits = await assertRateLimits(database, transaction, sendLimits, now);
    transaction.set(verificationRef, { pendingSendId: sendId, pendingUntil: Timestamp.fromMillis(now + 30_000), pendingSentAt: Timestamp.fromMillis(now),
      pendingWindowStartedAt: windowOpen ? windowStartedAt : Timestamp.fromMillis(now),
      pendingSendCount: sendCount + 1 }, { merge: true });
    return { outcome: "reserved" as const, sendCount, reservedLimits,
      windowStartedAt: windowOpen ? windowStartedAt : Timestamp.fromMillis(now) };
  });
  if (reservation.outcome === "already_verified") return reservation;
  const code = String(randomInt(0, 1_000_000)).padStart(6, "0");
  try {
    await deps.send(email, code);
  } catch (error) {
    const refund = error instanceof VerificationEmailDeliveryError && error.definitelyRejected;
    await database.runTransaction(async (transaction) => {
      const current = await transaction.get(verificationRef);
      const limits = refund ? await Promise.all(limitRefs.map((ref) => transaction.get(ref))) : [];
      limits.forEach((limit, index) => {
        const resetAt = limit.get("resetAt");
        // Refund only this reservation's window; never erase concurrent sends
        // or subtract a failed old request from a newly opened window.
        if (resetAt instanceof Timestamp && resetAt.toMillis() === reservation.reservedLimits[index]!.resetAtMillis) {
          transaction.update(limit.ref, { count: Math.max(0, Number(limit.get("count") ?? 0) - 1) });
        }
      });
      if (current.get("pendingSendId") !== sendId) return;
      if (!current.get("lastSentAt")) transaction.delete(verificationRef);
      else transaction.update(verificationRef, { pendingSendId: FieldValue.delete(), pendingUntil: FieldValue.delete(), pendingSentAt: FieldValue.delete(), pendingWindowStartedAt: FieldValue.delete(), pendingSendCount: FieldValue.delete() });
    });
    throw new HttpsError("unavailable", "EDU_EMAIL_SEND_FAILED");
  }
  const expiresAt = Timestamp.fromMillis(now + CODE_TTL_MS);
  await database.runTransaction(async (transaction) => {
    await assertActor(transaction);
    const current = await transaction.get(verificationRef);
    if (current.get("pendingSendId") !== sendId) throw new HttpsError("unavailable", "EDU_EMAIL_SEND_FAILED");
    transaction.set(verificationRef, {
      ...(grant ? { mode: "admin_test", grantId: grant.grantId, sessionId: sendId } : {}),
      email, codeHash: hashCode(actorUid, code).toString("hex"), attempts: 0, expiresAt,
      lastSentAt: Timestamp.fromMillis(now), windowStartedAt: reservation.windowStartedAt,
      sendCount: reservation.sendCount + 1,
    });
  });
  return { outcome: "sent", email, expiresAt: expiresAt.toDate().toISOString(), resendAfterSeconds: RESEND_COOLDOWN_MS / 1000, ...(grant ? { sessionId: sendId } : {}) };
}

export function requestCampusEmailCodeService(
  database: Firestore, uid: string, input: { email?: unknown }, now = Date.now(),
  deps: EduVerificationDeps = { send: sendVerificationEmail },
) {
  return requestEduVerificationService(database, uid, input, now, deps, true);
}

export function confirmCampusEmailCodeService(database: Firestore, uid: string, input: { code?: unknown; sessionId?: unknown }, now = Date.now()) {
  return confirmEduVerificationService(database, uid, input, now, true);
}

export type ConfirmEduVerificationResult =
  | { outcome: "verified"; email: string }
  | { outcome: "invalid_code"; attemptsLeft: number }
  | { outcome: "expired" }
  | { outcome: "too_many_attempts" };

/**
 * Wrong codes are returned as outcomes rather than thrown so the attempt
 * counter increment is committed with the transaction.
 */
export async function confirmEduVerificationService(
  database: Firestore,
  actorUid: string,
  input: { code?: unknown; sessionId?: unknown },
  now = Date.now(),
  campus = false,
): Promise<ConfirmEduVerificationResult> {
  const code = typeof input.code === "string" ? input.code.replace(/\s+/g, "") : "";
  if (!/^\d{6}$/.test(code)) {
    throw new HttpsError("invalid-argument", "EDU_CODE_FORMAT_INVALID");
  }
  const userRef = database.doc(`users/${actorUid}`);
  const verificationRef = database.doc(`eduVerifications/${actorUid}`);

  return database.runTransaction(async (transaction) => {
    const actor = await requireVerificationActor(database, transaction, actorUid);
    const [user, verification] = await Promise.all([
      transaction.get(userRef),
      transaction.get(verificationRef),
    ]);
    const email = verification.get("email");
    const storedHash = verification.get("codeHash");
    const expiresAt = verification.get("expiresAt");
    if (!verification.exists || typeof email !== "string" || typeof storedHash !== "string"
      || !(expiresAt instanceof Timestamp)) {
      throw new HttpsError("failed-precondition", "EDU_CODE_NOT_REQUESTED");
    }
    const testGrant = actor.grant;
    if (testGrant) {
      if (verification.get("mode") !== "admin_test" || verification.get("grantId") !== testGrant.grantId
        || email !== testGrant.email || typeof input.sessionId !== "string" || input.sessionId !== verification.get("sessionId")) {
        throw new HttpsError("permission-denied", "CAMPUS_TEST_SESSION_INVALID");
      }
    } else {
      if (verification.get("mode") === "admin_test") throw new HttpsError("permission-denied", "CAMPUS_TEST_SESSION_INVALID");
      requireScope(email, campus);
    }
    const pendingUntil = verification.get("pendingUntil");
    if (pendingUntil instanceof Timestamp && pendingUntil.toMillis() > now) {
      throw new HttpsError("unavailable", "EDU_EMAIL_SEND_IN_PROGRESS");
    }
    if (now >= expiresAt.toMillis()) {
      return { outcome: "expired" };
    }
    const attempts = Number(verification.get("attempts") ?? 0);
    if (attempts >= MAX_ATTEMPTS) {
      return { outcome: "too_many_attempts" };
    }
    if (!timingSafeEqual(Buffer.from(storedHash, "hex"), hashCode(actorUid, code))) {
      transaction.update(verificationRef, { attempts: FieldValue.increment(1) });
      return { outcome: "invalid_code", attemptsLeft: Math.max(MAX_ATTEMPTS - attempts - 1, 0) };
    }

    if (testGrant) {
      transaction.update(userRef, {
        adminEduTestVerification: { email, grantId: testGrant.grantId, verifiedAt: FieldValue.serverTimestamp() },
        updatedAt: FieldValue.serverTimestamp(),
      });
      transaction.update(verificationRef, { email: FieldValue.delete(), codeHash: FieldValue.delete(),
        expiresAt: FieldValue.delete(), attempts: FieldValue.delete(), sessionId: FieldValue.delete() });
      transaction.create(database.collection("auditLogs").doc(), { action: "campusTest.verified", actorUid,
        targetType: "user", targetId: actorUid, createdAt: FieldValue.serverTimestamp() });
      return { outcome: "verified", email };
    }
    const claimRef = database.doc(eduEmailClaimPath(email));
    const previousEmail = user.get("eduEmail");
    const previousClaimRef = typeof previousEmail === "string" && previousEmail !== email
      ? database.doc(eduEmailClaimPath(previousEmail))
      : null;
    const [claim, previousClaim] = await Promise.all([
      transaction.get(claimRef),
      previousClaimRef ? transaction.get(previousClaimRef) : Promise.resolve(null),
    ]);
    if (claim.exists && claim.get("uid") !== actorUid) {
      throw new HttpsError("already-exists", "EDU_EMAIL_IN_USE");
    }

    if (previousClaimRef && previousClaim?.get("uid") === actorUid) {
      transaction.delete(previousClaimRef);
    }
    transaction.set(claimRef, { uid: actorUid, verifiedAt: FieldValue.serverTimestamp() });
    transaction.update(userRef, {
      eduEmail: email,
      eduVerified: true,
      eduVerifiedAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    transaction.update(verificationRef, { email: FieldValue.delete(), codeHash: FieldValue.delete(),
      expiresAt: FieldValue.delete(), attempts: FieldValue.delete() });
    transaction.create(database.collection("auditLogs").doc(), {
      action: "eduEmail.verified",
      actorUid,
      targetType: "user",
      targetId: actorUid,
      metadata: { domain: email.split("@")[1] ?? "" },
      createdAt: FieldValue.serverTimestamp(),
    });
    return { outcome: "verified", email };
  });
}
