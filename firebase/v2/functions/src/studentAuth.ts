import { FieldValue, type Firestore } from "firebase-admin/firestore";
import { HttpsError } from "firebase-functions/v2/https";
import { eduEmailClaimPath, isEduEmail } from "./eduVerification.js";
import {
  createLegalAcknowledgements,
  requireLegalAcknowledgements,
} from "./legalAcknowledgements.js";

export type StudentIdentity = {
  uid: string;
  email?: string;
  emailVerified: boolean;
  displayName?: string;
  providers: string[];
};

export type StudentProfileInput = {
  displayName?: unknown;
  university?: unknown;
  userAgreementAccepted?: unknown;
  kvkkNoticeAcknowledged?: unknown;
};

export async function ensureStudentProfileService(
  database: Firestore,
  identity: StudentIdentity,
  input: StudentProfileInput = {},
): Promise<{ created: boolean; role: string; status: string }> {
  const email = identity.email?.trim().toLowerCase() ?? "";
  if (!email) {
    throw new HttpsError("failed-precondition", "EMAIL_REQUIRED");
  }
  const isGoogle = identity.providers.includes("google.com");
  const isApple = identity.providers.includes("apple.com");
  const isPassword = identity.providers.includes("password");
  if (!isGoogle && !isApple && !isPassword) {
    throw new HttpsError("permission-denied", "SUPPORTED_SIGN_IN_REQUIRED");
  }

  const requestedName = typeof input.displayName === "string" ? input.displayName.trim() : "";
  const requestedUniversity = typeof input.university === "string" ? input.university.trim() : "";
  const hasEduEmail = isEduEmail(email);

  const userRef = database.doc(`users/${identity.uid}`);
  // A verified .edu.tr sign-in address also unlocks suspended meals, as long as
  // no other account has claimed that address through e-mail verification.
  const claimRef = hasEduEmail && identity.emailVerified ? database.doc(eduEmailClaimPath(email)) : null;
  return database.runTransaction(async (transaction) => {
    const [existing, claim] = await Promise.all([
      transaction.get(userRef),
      claimRef ? transaction.get(claimRef) : Promise.resolve(null),
    ]);
    const canClaimEduEmail = claimRef !== null && (!claim?.exists || claim.get("uid") === identity.uid);
    const eduFields = canClaimEduEmail
      ? { eduEmail: email, eduVerified: true, eduVerifiedAt: FieldValue.serverTimestamp() }
      : {};
    const claimEduEmail = () => {
      if (claimRef && canClaimEduEmail && !claim?.exists) {
        transaction.set(claimRef, { uid: identity.uid, verifiedAt: FieldValue.serverTimestamp() });
      }
    };
    if (existing.exists) {
      const existingRole = String(existing.get("role") ?? "student");
      const existingStatus = String(existing.get("status") ?? "active");
      if (
        existingRole === "student" &&
        existingStatus === "pendingEmailVerification" &&
        identity.emailVerified
      ) {
        transaction.update(userRef, {
          status: "active",
          ...eduFields,
          updatedAt: FieldValue.serverTimestamp(),
        });
        claimEduEmail();
        return { created: false, role: existingRole, status: "active" };
      }
      if (existingRole === "student" && canClaimEduEmail && existing.get("eduVerified") !== true) {
        transaction.update(userRef, { ...eduFields, updatedAt: FieldValue.serverTimestamp() });
        claimEduEmail();
      }
      return {
        created: false,
        role: existingRole,
        status: existingStatus,
      };
    }

    if ((isGoogle || isApple) && !identity.emailVerified) {
      throw new HttpsError("failed-precondition", "VERIFIED_EMAIL_REQUIRED");
    }
    if (isPassword && !hasEduEmail) {
      throw new HttpsError("permission-denied", "EDU_EMAIL_REQUIRED");
    }
    requireLegalAcknowledgements(input);

    const fallbackName = email.split("@")[0] ?? "Öğrenci";
    const displayName = (requestedName || identity.displayName?.trim() || fallbackName).slice(0, 120);
    const status = identity.emailVerified ? "active" : "pendingEmailVerification";
    transaction.create(userRef, {
      email,
      displayName,
      role: "student",
      status,
      university: requestedUniversity.slice(0, 160),
      legalAcknowledgements: createLegalAcknowledgements(),
      ...(status === "active" ? eduFields : {}),
      createdAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    if (status === "active") claimEduEmail();
    return { created: true, role: "student", status };
  });
}
