import { createHash, randomBytes, timingSafeEqual } from "node:crypto";
import { getAuth, type DecodedIdToken } from "firebase-admin/auth";
import { type DocumentSnapshot, FieldValue, type Firestore, Timestamp } from "firebase-admin/firestore";
import { HttpsError } from "firebase-functions/v2/https";
import { eduEmailClaimPath } from "./eduVerification.js";
import { requireActiveActor } from "./shared.js";

export const CAMPUS_EMAIL_DOMAIN = "ogr.akdeniz.edu.tr";
const LINK_TTL_MS = 15 * 60 * 1000;
const RESEND_COOLDOWN_MS = 5 * 60 * 1000;
const SEND_WINDOW_MS = 60 * 60 * 1000;
const MAX_SENDS = 5;
const CONTINUE_URL = "https://good4tr-v2.firebaseapp.com/campus-email-verification";
// Public project configuration, already used by the V2 web client. Never use
// an API key, e-mail address or primary account ID supplied by the browser.
const CAMPUS_AUTH_API_KEY = "AIzaSyA9OIJZLXKFyzO5C7GZ5RtrQDAGl44fya4";

export function isCampusEmail(email: string): boolean {
  return email.length <= 254 && /^[^\s@/<>]+@ogr\.akdeniz\.edu\.tr$/.test(email.trim().toLowerCase());
}

export function hasVerifiedCampusEmail(user: DocumentSnapshot): boolean {
  const email = user.get("eduEmail");
  return user.get("eduVerified") === true && typeof email === "string" && isCampusEmail(email);
}

function normalizedEmail(value: unknown): string {
  if (typeof value !== "string" || !isCampusEmail(value)) {
    throw new HttpsError("invalid-argument", "CAMPUS_EMAIL_INVALID");
  }
  return value.trim().toLowerCase();
}

const hashRequest = (value: string) => createHash("sha256").update(value).digest();

function matchingHash(stored: unknown, value: string): boolean {
  return typeof stored === "string" && /^[a-f0-9]{64}$/.test(stored)
    && timingSafeEqual(Buffer.from(stored, "hex"), hashRequest(value));
}

export async function beginCampusEmailVerificationService(
  database: Firestore,
  uid: string,
  input: { email?: unknown },
  now = Date.now(),
) {
  const email = normalizedEmail(input.email);
  const requestId = randomBytes(32).toString("hex");
  const requestRef = database.doc(`campusEmailVerifications/${uid}`);
  return database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, uid, ["student"]);
    const [user, previous, claim] = await Promise.all([
      transaction.get(database.doc(`users/${uid}`)),
      transaction.get(requestRef),
      transaction.get(database.doc(eduEmailClaimPath(email))),
    ]);
    if (hasVerifiedCampusEmail(user) && user.get("eduEmail") === email) {
      return { outcome: "already_verified" as const, email };
    }
    if (claim.exists && claim.get("uid") !== uid) {
      throw new HttpsError("already-exists", "EDU_EMAIL_IN_USE");
    }
    const lastSentAt = previous.get("lastSentAt");
    if (lastSentAt instanceof Timestamp && now - lastSentAt.toMillis() < RESEND_COOLDOWN_MS) {
      throw new HttpsError("resource-exhausted", "CAMPUS_EMAIL_RESEND_TOO_SOON");
    }
    const windowStartedAt = previous.get("windowStartedAt");
    const windowOpen = windowStartedAt instanceof Timestamp && now - windowStartedAt.toMillis() < SEND_WINDOW_MS;
    const sendCount = windowOpen ? Number(previous.get("sendCount") ?? 0) : 0;
    if (sendCount >= MAX_SENDS) throw new HttpsError("resource-exhausted", "CAMPUS_EMAIL_SEND_LIMIT");
    transaction.set(requestRef, {
      email,
      requestHash: hashRequest(requestId).toString("hex"),
      requestedAt: Timestamp.fromMillis(now),
      expiresAt: Timestamp.fromMillis(now + LINK_TTL_MS),
      lastSentAt: Timestamp.fromMillis(now),
      windowStartedAt: windowOpen ? windowStartedAt : Timestamp.fromMillis(now),
      sendCount: sendCount + 1,
    });
    return {
      outcome: "ready" as const,
      email,
      requestId,
      continueUrl: `${CONTINUE_URL}?requestId=${requestId}`,
      expiresAtMillis: now + LINK_TTL_MS,
      resendAfterSeconds: RESEND_COOLDOWN_MS / 1000,
    };
  });
}

export interface CampusEmailVerificationDeps {
  verifyToken(token: string): Promise<DecodedIdToken>;
  getIdentity?(uid: string): Promise<{ uid: string; email?: string; providerIds: string[] }>;
  deleteIdentity?(uid: string): Promise<void>;
}

const productionDeps: CampusEmailVerificationDeps = {
  // Read the signed proof from the separate university Auth session, never a client boolean.
  verifyToken: (token) => getAuth().verifyIdToken(token, true),
  getIdentity: async (uid) => {
    const user = await getAuth().getUser(uid);
    return { uid: user.uid, email: user.email, providerIds: user.providerData.map((provider) => provider.providerId) };
  },
  deleteIdentity: (uid) => getAuth().deleteUser(uid),
};

export interface CampusBrowserVerificationDeps extends CampusEmailVerificationDeps {
  exchangeEmailLink(email: string, oobCode: string): Promise<string>;
}

export async function exchangeCampusEmailLink(email: string, oobCode: string): Promise<string> {
  const emulator = process.env.FIREBASE_AUTH_EMULATOR_HOST;
  const base = emulator ? `http://${emulator}/identitytoolkit.googleapis.com` : "https://identitytoolkit.googleapis.com";
  let response: Response;
  try {
    response = await fetch(`${base}/v1/accounts:signInWithEmailLink?key=${CAMPUS_AUTH_API_KEY}`, {
      method: "POST", headers: { "Content-Type": "application/json" },
      // No idToken: never link/replace the main Google or Apple identity.
      body: JSON.stringify({ email, oobCode }), signal: AbortSignal.timeout(10_000),
    });
  } catch {
    throw new HttpsError("unavailable", "CAMPUS_EMAIL_TEMPORARILY_UNAVAILABLE");
  }
  const result = await response.json() as { idToken?: unknown; error?: { message?: string } };
  if (!response.ok) {
    if (result.error?.message === "EXPIRED_OOB_CODE") {
      throw new HttpsError("deadline-exceeded", "CAMPUS_EMAIL_LINK_EXPIRED");
    }
    if (response.status >= 500 || response.status === 429) {
      throw new HttpsError("unavailable", "CAMPUS_EMAIL_TEMPORARILY_UNAVAILABLE");
    }
    throw new HttpsError("permission-denied", "CAMPUS_EMAIL_PROOF_INVALID");
  }
  if (typeof result.idToken !== "string") throw new HttpsError("permission-denied", "CAMPUS_EMAIL_PROOF_INVALID");
  return result.idToken;
}

const productionBrowserDeps: CampusBrowserVerificationDeps = { ...productionDeps, exchangeEmailLink: exchangeCampusEmailLink };

export async function getCampusEmailVerificationStatusService(database: Firestore, uid: string) {
  return database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, uid, ["student"]);
    const user = await transaction.get(database.doc(`users/${uid}`));
    return hasVerifiedCampusEmail(user)
      ? { verified: true, email: user.get("eduEmail") as string }
      : { verified: false };
  });
}

/** The opaque request chooses the original account; the one-time Auth code is
 * the independent proof of mailbox ownership. Neither is sufficient alone. */
export async function completeCampusEmailVerificationFromBrowserService(
  database: Firestore,
  input: { requestId?: unknown; oobCode?: unknown },
  deps: CampusBrowserVerificationDeps = productionBrowserDeps,
  now = Date.now(),
) {
  if (typeof input.requestId !== "string" || !/^[a-f0-9]{64}$/.test(input.requestId)
    || typeof input.oobCode !== "string" || !/^[A-Za-z0-9_-]{1,2048}$/.test(input.oobCode)) {
    throw new HttpsError("invalid-argument", "CAMPUS_EMAIL_PROOF_INVALID");
  }
  const { requestId, oobCode } = input;
  const requestHash = hashRequest(requestId).toString("hex");
  const requests = database.collection("campusEmailVerifications");
  const completedReceipt = async () => {
    const matches = await requests.where("completedRequestHash", "==", requestHash).limit(2).get();
    const request = matches.docs[0];
    if (matches.size !== 1 || !request) return null;
    if (!matchingHash(request.get("completedCodeHash"), oobCode)) return null;
    const status = await getCampusEmailVerificationStatusService(database, request.id);
    return status.verified && status.email === request.get("email") ? { outcome: "verified" as const } : null;
  };
  const receipt = await completedReceipt();
  if (receipt) return receipt;
  const matches = await requests.where("requestHash", "==", requestHash).limit(2).get();
  const request = matches.docs[0];
  if (matches.size !== 1 || !request) throw new HttpsError("failed-precondition", "CAMPUS_EMAIL_REQUEST_MISMATCH");
  const expiresAt = request.get("expiresAt");
  if (!(expiresAt instanceof Timestamp) || expiresAt.toMillis() <= now) {
    throw new HttpsError("deadline-exceeded", "CAMPUS_EMAIL_LINK_EXPIRED");
  }
  await database.runTransaction((transaction) => requireActiveActor(database, transaction, request.id, ["student"]));
  const email = normalizedEmail(request.get("email"));
  try {
    const universityIdToken = await deps.exchangeEmailLink(email, oobCode);
    await completeCampusEmailVerificationService(database, request.id, { requestId, universityIdToken }, deps, now,
      hashRequest(oobCode).toString("hex"));
    return { outcome: "verified" as const };
  } catch (error) {
    // A duplicate tab can succeed only after this same request AND Auth code
    // have committed. Never turn an invalid/consumed foreign code into success.
    const retryReceipt = await completedReceipt();
    if (retryReceipt) return retryReceipt;
    throw error;
  }
}

export async function completeCampusEmailVerificationService(
  database: Firestore,
  uid: string,
  input: { requestId?: unknown; universityIdToken?: unknown },
  deps: CampusEmailVerificationDeps = productionDeps,
  now = Date.now(),
  completedCodeHash?: string,
) {
  if (typeof input.requestId !== "string" || !/^[a-f0-9]{64}$/.test(input.requestId)
    || typeof input.universityIdToken !== "string" || input.universityIdToken.length > 16_384) {
    throw new HttpsError("invalid-argument", "CAMPUS_EMAIL_PROOF_INVALID");
  }
  // Reject inactive/non-student callers before looking up another Auth identity.
  await database.runTransaction((transaction) => requireActiveActor(database, transaction, uid, ["student"]));
  let proof: DecodedIdToken;
  try {
    proof = await deps.verifyToken(input.universityIdToken);
  } catch {
    throw new HttpsError("permission-denied", "CAMPUS_EMAIL_PROOF_INVALID");
  }
  if (proof.email_verified !== true || typeof proof.email !== "string" || !isCampusEmail(proof.email)
    || !proof.uid || proof.firebase?.sign_in_provider !== "password" || !Number.isFinite(proof.auth_time)) {
    throw new HttpsError("permission-denied", "CAMPUS_EMAIL_PROOF_INVALID");
  }
  const email = normalizedEmail(proof.email);
  const requestId = input.requestId;
  const requestRef = database.doc(`campusEmailVerifications/${uid}`);
  const result = await database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, uid, ["student"]);
    const [request, user, claim] = await Promise.all([
      transaction.get(requestRef),
      transaction.get(database.doc(`users/${uid}`)),
      transaction.get(database.doc(eduEmailClaimPath(email))),
    ]);
    const storedHash = request.get("requestHash");
    if (!matchingHash(storedHash, requestId)
      || request.get("email") !== email) {
      throw new HttpsError("failed-precondition", "CAMPUS_EMAIL_REQUEST_MISMATCH");
    }
    const expiresAt = request.get("expiresAt");
    const requestedAt = request.get("requestedAt");
    if (!(expiresAt instanceof Timestamp) || expiresAt.toMillis() <= now) {
      throw new HttpsError("deadline-exceeded", "CAMPUS_EMAIL_LINK_EXPIRED");
    }
    // An old university session cannot be reused to approve a newly requested address.
    if (!(requestedAt instanceof Timestamp) || proof.auth_time < Math.floor(requestedAt.toMillis() / 1000)
      || proof.auth_time > Math.floor(now / 1000) + 30) {
      throw new HttpsError("permission-denied", "CAMPUS_EMAIL_PROOF_INVALID");
    }
    if (claim.exists && claim.get("uid") !== uid) {
      throw new HttpsError("already-exists", "EDU_EMAIL_IN_USE");
    }
    const oldEmail = user.get("eduEmail");
    const oldClaimRef = typeof oldEmail === "string" && oldEmail !== email
      ? database.doc(eduEmailClaimPath(oldEmail)) : null;
    const oldClaim = oldClaimRef ? await transaction.get(oldClaimRef) : null;
    if (oldClaimRef && oldClaim?.get("uid") === uid) transaction.delete(oldClaimRef);
    transaction.set(database.doc(eduEmailClaimPath(email)), { uid, verifiedAt: FieldValue.serverTimestamp() });
    transaction.update(database.doc(`users/${uid}`), {
      eduEmail: email, eduVerified: true, eduVerifiedAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    transaction.update(requestRef, {
      requestHash: FieldValue.delete(), consumedAt: FieldValue.serverTimestamp(),
      ...(completedCodeHash ? { completedRequestHash: hashRequest(requestId).toString("hex"), completedCodeHash } : {}),
    });
    transaction.create(database.collection("auditLogs").doc(), {
      action: "campusEmail.verified", actorUid: uid, targetType: "user", targetId: uid,
      createdAt: FieldValue.serverTimestamp(), metadata: { domain: CAMPUS_EMAIL_DOMAIN },
    });
    return { outcome: "verified" as const, email };
  });
  // The school login is a proof, not a second Good4 account. Never delete a
  // primary identity, a Good4 profile or an identity linked to another provider.
  if (proof.uid !== uid && deps.getIdentity && deps.deleteIdentity) {
    try {
      const profile = await database.doc(`users/${proof.uid}`).get();
      if (!profile.exists) {
        const identity = await deps.getIdentity(proof.uid);
        if (identity.uid === proof.uid && identity.email?.trim().toLowerCase() === email
          && identity.providerIds.length === 1 && identity.providerIds[0] === "password") {
          await deps.deleteIdentity(proof.uid);
        }
      }
    } catch (error) {
      // Verification has already committed. Log only the category, never tokens,
      // links, request IDs or student e-mail addresses.
      const code = error && typeof error === "object" && "code" in error
        && typeof error.code === "string" ? error.code : "unknown";
      console.warn("campusEmail: temporary Auth identity cleanup failed", { code });
    }
  }
  return result;
}
