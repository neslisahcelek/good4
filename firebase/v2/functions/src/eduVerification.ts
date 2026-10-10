import { createHash, randomInt, randomUUID, timingSafeEqual } from "node:crypto";
import {
  type DocumentSnapshot,
  FieldValue,
  type Firestore,
  Timestamp,
} from "firebase-admin/firestore";
import { HttpsError } from "firebase-functions/v2/https";
import { requireActiveActor } from "./shared.js";
import { assertRateLimits } from "./rateLimit.js";
import { sendVerificationEmail, type VerificationEmailSender } from "./verificationEmail.js";

const CODE_TTL_MS = 10 * 60 * 1000;
// Mail usually lands in 1-2 minutes; a shorter wait invites a second code that voids the first.
const RESEND_COOLDOWN_MS = 3 * 60 * 1000;
const SEND_WINDOW_MS = 60 * 60 * 1000;
const MAX_SENDS_PER_WINDOW = 5;
const MAX_ATTEMPTS = 5;
// Abuse limits on top of the per-account cooldown: new accounts are cheap, mail quota is not
// (Brevo free plan: 300/day). They count when a send is reserved, so provider failures count too.
const MAX_SENDS_PER_ACCOUNT_DAY = 10;
const MAX_SENDS_PER_RECIPIENT_HOUR = 3;
const MAX_SENDS_TOTAL_DAY = 250;

const EDU_EMAIL_PATTERN = /^[^\s@/]+@(?:[a-z0-9-]+\.)*edu\.tr$/;

export function isEduEmail(email: string): boolean {
  return EDU_EMAIL_PATTERN.test(email.trim().toLowerCase());
}

/** Suspended meals and Kampüs Dolabı are limited to accounts with a verified university address. */
export function hasVerifiedEduEmail(user: DocumentSnapshot): boolean {
  const eduEmail = user.get("eduEmail");
  return user.get("eduVerified") === true && typeof eduEmail === "string" && isEduEmail(eduEmail);
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
  | { outcome: "sent"; email: string; expiresAt: string; resendAfterSeconds: number }
  | { outcome: "already_verified"; email: string };

export interface EduVerificationDeps {
  send: VerificationEmailSender;
}

// A server-selected scope, never a trusted client boolean. Keep general .edu.tr
// verification for suspended meals and enforce the campus domain in both steps.
function requireScope(email: string, campus: boolean): void {
  if (campus && !/^[^\s@/<>]+@ogr\.akdeniz\.edu\.tr$/.test(email)) {
    throw new HttpsError("invalid-argument", "CAMPUS_EMAIL_INVALID");
  }
}

/** Reserve delivery separately so Firestore retries never send duplicate mail.
 * A provider failure releases the lease without spending quota or replacing a
 * previously delivered code. Only the hash is persisted after successful send. */
export async function requestEduVerificationService(
  database: Firestore,
  actorUid: string,
  input: { email?: unknown },
  now = Date.now(),
  deps: EduVerificationDeps = { send: sendVerificationEmail },
  campus = false,
): Promise<RequestEduVerificationResult> {
  const email = normalizeEduEmail(input.email);
  requireScope(email, campus);
  const verificationRef = database.doc(`eduVerifications/${actorUid}`);
  const sendId = randomUUID();
  const reservation = await database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, actorUid, ["student"]);
    const user = await transaction.get(database.doc(`users/${actorUid}`));
    const [verification, claim] = await Promise.all([
      transaction.get(verificationRef), transaction.get(database.doc(eduEmailClaimPath(email))),
    ]);
    if (hasVerifiedEduEmail(user) && user.get("eduEmail") === email) {
      return { outcome: "already_verified" as const, email };
    }
    if (claim.exists && claim.get("uid") !== actorUid) throw new HttpsError("already-exists", "EDU_EMAIL_IN_USE");
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
    await assertRateLimits(database, transaction, [
      { key: `eduMail:account:${actorUid}`, limit: MAX_SENDS_PER_ACCOUNT_DAY, windowSeconds: 86_400, errorMessage: "EDU_CODE_ACCOUNT_DAILY_LIMIT" },
      { key: `eduMail:recipient:${createHash("sha256").update(email).digest("hex")}`, limit: MAX_SENDS_PER_RECIPIENT_HOUR,
        windowSeconds: 3_600, errorMessage: "EDU_CODE_RECIPIENT_LIMIT" },
      { key: "eduMail:total", limit: MAX_SENDS_TOTAL_DAY, windowSeconds: 86_400, errorMessage: "EDU_CODE_DAILY_LIMIT" },
    ], now);
    transaction.set(verificationRef, { pendingSendId: sendId, pendingUntil: Timestamp.fromMillis(now + 30_000) }, { merge: true });
    return { outcome: "reserved" as const, sendCount, windowStartedAt: windowOpen ? windowStartedAt : Timestamp.fromMillis(now) };
  });
  if (reservation.outcome === "already_verified") return reservation;
  const code = String(randomInt(0, 1_000_000)).padStart(6, "0");
  try {
    await deps.send(email, code);
  } catch {
    await database.runTransaction(async (transaction) => {
      const current = await transaction.get(verificationRef);
      if (current.get("pendingSendId") !== sendId) return;
      if (!current.get("lastSentAt")) transaction.delete(verificationRef);
      else transaction.update(verificationRef, { pendingSendId: FieldValue.delete(), pendingUntil: FieldValue.delete() });
    });
    throw new HttpsError("unavailable", "EDU_EMAIL_SEND_FAILED");
  }
  const expiresAt = Timestamp.fromMillis(now + CODE_TTL_MS);
  await database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, actorUid, ["student"]);
    const current = await transaction.get(verificationRef);
    if (current.get("pendingSendId") !== sendId) throw new HttpsError("unavailable", "EDU_EMAIL_SEND_FAILED");
    transaction.set(verificationRef, {
      email, codeHash: hashCode(actorUid, code).toString("hex"), attempts: 0, expiresAt,
      lastSentAt: Timestamp.fromMillis(now), windowStartedAt: reservation.windowStartedAt,
      sendCount: reservation.sendCount + 1,
    });
  });
  return { outcome: "sent", email, expiresAt: expiresAt.toDate().toISOString(), resendAfterSeconds: RESEND_COOLDOWN_MS / 1000 };
}

export function requestCampusEmailCodeService(
  database: Firestore, uid: string, input: { email?: unknown }, now = Date.now(),
  deps: EduVerificationDeps = { send: sendVerificationEmail },
) {
  return requestEduVerificationService(database, uid, input, now, deps, true);
}

export function confirmCampusEmailCodeService(database: Firestore, uid: string, input: { code?: unknown }, now = Date.now()) {
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
  input: { code?: unknown },
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
    await requireActiveActor(database, transaction, actorUid, ["student"]);
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
    requireScope(email, campus);
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
