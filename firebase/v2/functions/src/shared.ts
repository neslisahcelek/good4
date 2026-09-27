import type { Firestore, Transaction } from "firebase-admin/firestore";
import { HttpsError } from "firebase-functions/v2/https";

export const USER_ROLES = [
  "student",
  "businessOwner",
  "businessStaff",
  "communityManager",
  "communityStaff",
  "good4Admin",
] as const;

export type UserRole = (typeof USER_ROLES)[number];

export interface Actor {
  uid: string;
  role: UserRole;
}

export async function requireActiveActor(
  database: Firestore,
  transaction: Transaction,
  uid: string,
  acceptedRoles?: readonly UserRole[],
): Promise<Actor> {
  const snapshot = await transaction.get(database.doc(`users/${uid}`));
  if (!snapshot.exists || snapshot.get("status") !== "active") {
    throw new HttpsError("permission-denied", "ACCOUNT_NOT_ACTIVE");
  }

  const role = snapshot.get("role") as UserRole;
  if (!USER_ROLES.includes(role)) {
    throw new HttpsError("permission-denied", "ROLE_NOT_CONFIGURED");
  }
  if (acceptedRoles && !acceptedRoles.includes(role)) {
    throw new HttpsError("permission-denied", "ROLE_NOT_ALLOWED");
  }

  return { uid, role };
}

export function requireAuthenticatedUid(uid: string | undefined): string {
  if (!uid) {
    throw new HttpsError("unauthenticated", "AUTHENTICATION_REQUIRED");
  }
  return uid;
}

export function requireNonEmptyString(
  value: unknown,
  fieldName: string,
  maxLength: number,
): string {
  if (typeof value !== "string") {
    throw new HttpsError("invalid-argument", `${fieldName.toUpperCase()}_REQUIRED`);
  }
  const normalized = value.trim();
  if (normalized.length === 0 || normalized.length > maxLength) {
    throw new HttpsError("invalid-argument", `${fieldName.toUpperCase()}_INVALID`);
  }
  return normalized;
}
