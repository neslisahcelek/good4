import { type DocumentSnapshot, FieldValue, type Firestore, Timestamp, type Transaction } from "firebase-admin/firestore";
import { HttpsError } from "firebase-functions/v2/https";
import { requireActiveActor } from "./shared.js";

/** The only policy for campus membership. Proof of an edu address remains separate. */
export const CAMPUS_MEMBER_ROLES = ["student", "good4Admin"] as const;
export const CAMPUS_UNIVERSITY_DOMAIN = "akdeniz.edu.tr";
export function isVerifiedCampusAddress(email: unknown): email is string {
  return typeof email === "string" && email.length <= 254
    && /^[^\s@/<>]+@ogr\.akdeniz\.edu\.tr$/.test(email.trim().toLowerCase());
}
export function hasCampusEmailProof(user: DocumentSnapshot): boolean {
  return user.get("eduVerified") === true && isVerifiedCampusAddress(user.get("eduEmail"));
}
export function campusTestGrant(user: DocumentSnapshot): { email: string; grantId: string } | null {
  const grant = user.get("adminEduTest");
  if (!user.exists || user.get("status") !== "active" || user.get("role") !== "good4Admin"
    || grant?.enabled !== true || typeof grant.grantId !== "string" || !grant.grantId
    || typeof grant.email !== "string" || grant.email.length > 254
    || !/^[^\s@/<>]+@[^\s@/<>]+\.[^\s@/<>]+$/.test(grant.email)) return null;
  return { email: grant.email, grantId: grant.grantId };
}
export function resolveCampusAccess(user: DocumentSnapshot) {
  const grant = campusTestGrant(user);
  const proof = user.get("adminEduTestVerification");
  const testVerified = grant !== null && proof?.grantId === grant.grantId
    && proof?.email === grant.email && proof?.verifiedAt instanceof Timestamp;
  const activeMember = user.exists && user.get("status") === "active"
    && CAMPUS_MEMBER_ROLES.some((role) => role === user.get("role"));
  const eduVerified = activeMember && hasCampusEmailProof(user);
  const accessSource = eduVerified ? "edu_email" : testVerified ? "admin_test" : "none";
  return {
    eduVerified,
    eduEmail: eduVerified ? user.get("eduEmail") as string : null,
    campusAccessAllowed: accessSource !== "none",
    studentAccessAllowed: activeMember && (user.get("role") === "student" ? eduVerified : testVerified),
    accessSource,
    universityDomain: accessSource !== "none" ? CAMPUS_UNIVERSITY_DOMAIN : null,
    testAvailable: grant !== null,
    testEmail: grant?.email ?? null,
  };
}
export function requireCampusAccess(user: DocumentSnapshot, errorMessage: string, scope: "member" | "student" = "member"): string {
  const access = resolveCampusAccess(user);
  if (!(scope === "student" ? access.studentAccessAllowed : access.campusAccessAllowed) || !access.universityDomain) throw new HttpsError("permission-denied", errorMessage);
  return access.universityDomain;
}
export async function requireVerificationActor(database: Firestore, transaction: Transaction, uid: string) {
  const actor = await requireActiveActor(database, transaction, uid, CAMPUS_MEMBER_ROLES);
  const user = await transaction.get(database.doc(`users/${uid}`));
  const grant = campusTestGrant(user);
  if (actor.role !== "student" && !grant) throw new HttpsError("permission-denied", "ROLE_NOT_ALLOWED");
  return { user, grant };
}
export async function getCampusAccessStatusService(database: Firestore, uid: string, now = Date.now()) {
  return database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, uid, CAMPUS_MEMBER_ROLES);
    const [user, verification] = await Promise.all([
      transaction.get(database.doc(`users/${uid}`)), transaction.get(database.doc(`eduVerifications/${uid}`)),
    ]);
    const access = resolveCampusAccess(user);
    const lastSentAt = verification.get("lastSentAt");
    return { ...access, verified: access.eduVerified, ...(access.eduEmail ? { email: access.eduEmail } : {}),
      resendAfterSeconds: lastSentAt instanceof Timestamp ? Math.max(0, Math.ceil((lastSentAt.toMillis() + 180_000 - now) / 1000)) : 0 };
  });
}
/** Keep delivery cooldowns/quotas, but invalidate the session even while mail is in flight. */
export async function disconnectCampusTestService(database: Firestore, uid: string) {
  return database.runTransaction(async (transaction) => {
    const { user, grant } = await requireVerificationActor(database, transaction, uid);
    if (!grant) throw new HttpsError("permission-denied", "ROLE_NOT_ALLOWED");
    const ref = database.doc(`eduVerifications/${uid}`);
    const pending = await transaction.get(ref);
    const previousWindow = pending.get("windowStartedAt");
    const reservedWindow = pending.get("pendingWindowStartedAt");
    const sameWindow = previousWindow instanceof Timestamp && reservedWindow instanceof Timestamp
      && previousWindow.isEqual(reservedWindow);
    const reservedCount = pending.get("pendingSendCount");
    transaction.update(user.ref, { adminEduTestGeneration: FieldValue.increment(1), adminEduTestVerification: FieldValue.delete(), updatedAt: FieldValue.serverTimestamp() });
    if (pending.exists) transaction.update(ref, {
      ...(pending.get("pendingSentAt") instanceof Timestamp ? {
        lastSentAt: pending.get("pendingSentAt"), windowStartedAt: pending.get("pendingWindowStartedAt"),
        sendCount: typeof reservedCount === "number" ? reservedCount : (sameWindow ? Number(pending.get("sendCount") ?? 0) + 1 : 1),
      } : {}),
      pendingSentAt: FieldValue.delete(), pendingWindowStartedAt: FieldValue.delete(), pendingSendCount: FieldValue.delete(),
      email: FieldValue.delete(), codeHash: FieldValue.delete(), expiresAt: FieldValue.delete(), attempts: FieldValue.delete(),
      sessionId: FieldValue.delete(), mode: FieldValue.delete(), grantId: FieldValue.delete(),
      pendingSendId: FieldValue.delete(), pendingUntil: FieldValue.delete(),
    });
    transaction.delete(database.doc(`campusEmailVerifications/${uid}`));
    transaction.create(database.collection("auditLogs").doc(), { action: "campusTest.disconnected", actorUid: uid,
      targetType: "user", targetId: uid, createdAt: FieldValue.serverTimestamp() });
    return { outcome: "disconnected" };
  });
}
