import { FieldPath, Timestamp, type DocumentSnapshot, type Firestore } from "firebase-admin/firestore";
import { getAuth } from "firebase-admin/auth";
import { HttpsError } from "firebase-functions/v2/https";
import { requireActiveActor, requireNonEmptyString } from "./shared.js";

export interface ApplicantIdentity {
  uid: string;
  email?: string;
  emailVerified: boolean;
  providers: string[];
  disabled?: boolean;
}

export function requireVerifiedGoogleApplicant(identity: ApplicantIdentity): string {
  if (identity.disabled === true) throw new HttpsError("permission-denied", "ACCOUNT_NOT_ACTIVE");
  if (!identity.providers.includes("google.com")) {
    throw new HttpsError("permission-denied", "GOOGLE_SIGN_IN_REQUIRED");
  }
  const email = identity.email?.trim().toLowerCase() ?? "";
  if (!email || !identity.emailVerified) {
    throw new HttpsError("failed-precondition", "VERIFIED_EMAIL_REQUIRED");
  }
  return email;
}

/** A student stays a student; a portal account already belongs to an organization. */
export function requireAccountWithoutRole(user: DocumentSnapshot): void {
  if (!user.exists) return;
  if (user.get("role") === "student") throw new HttpsError("failed-precondition", "STUDENT_ACCOUNT_NOT_ALLOWED");
  throw new HttpsError("failed-precondition", "ACCOUNT_ALREADY_HAS_ROLE");
}

/** Auth can change while an application waits, before its Good4 profile exists. */
export async function requireActiveApplicantForApproval(
  database: Firestore,
  actorUid: string,
  input: Record<string, unknown>,
  getIdentity: (uid: string) => Promise<{ disabled: boolean }> = (uid) => getAuth().getUser(uid),
): Promise<void> {
  // Rejection remains available for disabled/deleted applicants.
  if (input.decision !== "approve") return;
  await database.runTransaction((transaction) => requireActiveActor(database, transaction, actorUid, ["good4Admin"]));
  const applicantUid = requireNonEmptyString(input.applicationId, "applicationId", 128);
  if (!/^[A-Za-z0-9_-]+$/.test(applicantUid)) throw new HttpsError("invalid-argument", "APPLICATIONID_INVALID");
  let identity: { disabled: boolean };
  try {
    identity = await getIdentity(applicantUid);
  } catch (error) {
    if ((error as { code?: string } | null)?.code === "auth/user-not-found") {
      throw new HttpsError("failed-precondition", "APPLICANT_ACCOUNT_NOT_FOUND");
    }
    throw new HttpsError("unavailable", "APPLICANT_ACCOUNT_CHECK_FAILED");
  }
  if (identity.disabled) throw new HttpsError("failed-precondition", "APPLICANT_ACCOUNT_NOT_ACTIVE");
}

type ApplicationCollection = "businessApplications" | "communityApplications";
export interface ApplicationPageInput {
  cursor?: unknown;
  limit?: unknown;
}

function decodeCursor(value: unknown): { createdAt: Timestamp; id: string } | null {
  if (value === undefined || value === null) return null;
  if (typeof value !== "string" || value.length > 512 || !/^[A-Za-z0-9_-]+$/.test(value)) {
    throw new HttpsError("invalid-argument", "APPLICATION_CURSOR_INVALID");
  }
  try {
    const cursor = JSON.parse(Buffer.from(value, "base64url").toString("utf8")) as Record<string, unknown>;
    if (!cursor || typeof cursor.id !== "string" || !/^[A-Za-z0-9_-]{1,128}$/.test(cursor.id)
      || typeof cursor.seconds !== "number" || !Number.isSafeInteger(cursor.seconds)
      || cursor.seconds < 0 || cursor.seconds > 253402300799
      || typeof cursor.nanoseconds !== "number" || !Number.isInteger(cursor.nanoseconds)
      || cursor.nanoseconds < 0 || cursor.nanoseconds > 999999999) throw new Error();
    return { createdAt: new Timestamp(cursor.seconds, cursor.nanoseconds), id: cursor.id };
  } catch {
    throw new HttpsError("invalid-argument", "APPLICATION_CURSOR_INVALID");
  }
}

/** Cursor values survive approval/deletion of the boundary document. */
export async function listPendingApplicationPage(
  database: Firestore,
  actorUid: string,
  collection: ApplicationCollection,
  input: ApplicationPageInput = {},
) {
  await database.runTransaction((transaction) => requireActiveActor(database, transaction, actorUid, ["good4Admin"]));
  const limit = input.limit ?? 200;
  if (typeof limit !== "number" || !Number.isInteger(limit) || limit < 1 || limit > 200) {
    throw new HttpsError("invalid-argument", "APPLICATION_PAGE_LIMIT_INVALID");
  }
  const cursor = decodeCursor(input.cursor);
  let query = database.collection(collection).where("status", "==", "pending")
    .orderBy("createdAt", "asc").orderBy(FieldPath.documentId(), "asc").limit(limit);
  if (cursor) query = query.startAfter(cursor.createdAt, cursor.id);
  const snapshot = await query.get();
  const last = snapshot.docs.at(-1);
  const createdAt = last?.get("createdAt");
  const nextCursor = snapshot.size === limit && last && createdAt instanceof Timestamp
    ? Buffer.from(JSON.stringify({ id: last.id, seconds: createdAt.seconds, nanoseconds: createdAt.nanoseconds })).toString("base64url")
    : null;
  return { documents: snapshot.docs, nextCursor };
}
