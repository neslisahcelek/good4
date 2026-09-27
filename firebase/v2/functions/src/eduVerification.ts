import { createHash, randomInt, timingSafeEqual } from "node:crypto";
import {
  type DocumentSnapshot,
  FieldValue,
  type Firestore,
  Timestamp,
} from "firebase-admin/firestore";
import { HttpsError } from "firebase-functions/v2/https";
import { requireActiveActor } from "./shared.js";

const CODE_TTL_MS = 10 * 60 * 1000;
const RESEND_COOLDOWN_MS = 60 * 1000;
const SEND_WINDOW_MS = 60 * 60 * 1000;
const MAX_SENDS_PER_WINDOW = 5;
const MAX_ATTEMPTS = 5;

const EDU_EMAIL_PATTERN = /^[^\s@/]+@(?:[a-z0-9-]+\.)*edu\.tr$/;

export function isEduEmail(email: string): boolean {
  return EDU_EMAIL_PATTERN.test(email.trim().toLowerCase());
}

/** Suspended-meal benefits are limited to accounts with a verified university address. */
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

function verificationMail(email: string, code: string) {
  return {
    to: [email],
    message: {
      subject: `Good4 doğrulama kodun: ${code}`,
      text: [
        "Merhaba,",
        "",
        `Good4 Askıda Yemek için .edu.tr adresini doğrulama kodun: ${code}`,
        "",
        "Kod 10 dakika geçerlidir. Bu isteği sen yapmadıysan bu e-postayı yok sayabilirsin.",
      ].join("\n"),
      html: `<p>Merhaba,</p><p>Good4 Askıda Yemek için .edu.tr adresini doğrulama kodun:</p>`
        + `<p style="font-size:28px;font-weight:600;letter-spacing:6px">${code}</p>`
        + "<p>Kod 10 dakika geçerlidir. Bu isteği sen yapmadıysan bu e-postayı yok sayabilirsin.</p>",
    },
  };
}

export type RequestEduVerificationResult =
  | { outcome: "sent"; email: string; expiresAt: string; resendAfterSeconds: number }
  | { outcome: "already_verified"; email: string };

/**
 * Stores a hashed six-digit code and queues the e-mail in `mail`, the
 * collection watched by the "Trigger Email from Firestore" extension.
 */
export async function requestEduVerificationService(
  database: Firestore,
  actorUid: string,
  input: { email?: unknown },
  now = Date.now(),
): Promise<RequestEduVerificationResult> {
  const email = normalizeEduEmail(input.email);
  const userRef = database.doc(`users/${actorUid}`);
  const verificationRef = database.doc(`eduVerifications/${actorUid}`);
  const claimRef = database.doc(eduEmailClaimPath(email));

  return database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, actorUid, ["student"]);
    const [user, verification, claim] = await Promise.all([
      transaction.get(userRef),
      transaction.get(verificationRef),
      transaction.get(claimRef),
    ]);
    if (hasVerifiedEduEmail(user) && user.get("eduEmail") === email) {
      return { outcome: "already_verified", email };
    }
    if (claim.exists && claim.get("uid") !== actorUid) {
      throw new HttpsError("already-exists", "EDU_EMAIL_IN_USE");
    }

    const lastSentAt = verification.get("lastSentAt");
    if (lastSentAt instanceof Timestamp && now - lastSentAt.toMillis() < RESEND_COOLDOWN_MS) {
      throw new HttpsError("resource-exhausted", "EDU_CODE_RESEND_TOO_SOON");
    }
    const windowStartedAt = verification.get("windowStartedAt");
    const windowOpen = windowStartedAt instanceof Timestamp
      && now - windowStartedAt.toMillis() < SEND_WINDOW_MS;
    const sendCount = windowOpen ? Number(verification.get("sendCount") ?? 0) : 0;
    if (sendCount >= MAX_SENDS_PER_WINDOW) {
      throw new HttpsError("resource-exhausted", "EDU_CODE_SEND_LIMIT");
    }

    const code = String(randomInt(0, 1_000_000)).padStart(6, "0");
    const expiresAt = Timestamp.fromMillis(now + CODE_TTL_MS);
    transaction.set(verificationRef, {
      email,
      codeHash: hashCode(actorUid, code).toString("hex"),
      attempts: 0,
      expiresAt,
      lastSentAt: Timestamp.fromMillis(now),
      windowStartedAt: windowOpen ? windowStartedAt : Timestamp.fromMillis(now),
      sendCount: sendCount + 1,
    });
    transaction.create(database.collection("mail").doc(), {
      ...verificationMail(email, code),
      purpose: "eduVerification",
      uid: actorUid,
      createdAt: FieldValue.serverTimestamp(),
    });

    return {
      outcome: "sent",
      email,
      expiresAt: expiresAt.toDate().toISOString(),
      resendAfterSeconds: RESEND_COOLDOWN_MS / 1000,
    };
  });
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
    transaction.delete(verificationRef);
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
