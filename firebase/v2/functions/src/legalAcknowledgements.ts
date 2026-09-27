import { FieldValue, type Firestore } from "firebase-admin/firestore";
import { HttpsError } from "firebase-functions/v2/https";

export const KVKK_NOTICE_VERSION = "1.0";
export const USER_AGREEMENT_VERSION = "1.0";
export const PRIVACY_POLICY_VERSION = "1.3";

export type LegalAcknowledgementInput = {
  userAgreementAccepted?: unknown;
  kvkkNoticeAcknowledged?: unknown;
};

type LegalAcknowledgementsMap = Record<string, unknown>;

export function requireLegalAcknowledgements(input: LegalAcknowledgementInput): void {
  if (input.userAgreementAccepted !== true || input.kvkkNoticeAcknowledged !== true) {
    throw new HttpsError("failed-precondition", "LEGAL_ACKNOWLEDGEMENTS_REQUIRED");
  }
}

export function createLegalAcknowledgements(): LegalAcknowledgementsMap {
  return {
    kvkkNotice: {
      version: KVKK_NOTICE_VERSION,
      acknowledgedAt: FieldValue.serverTimestamp(),
    },
    userAgreement: {
      version: USER_AGREEMENT_VERSION,
      acceptedAt: FieldValue.serverTimestamp(),
    },
    privacyPolicy: {
      version: PRIVACY_POLICY_VERSION,
      presentedAt: FieldValue.serverTimestamp(),
    },
  };
}

function asRecord(value: unknown): Record<string, unknown> {
  return value !== null && typeof value === "object" && !Array.isArray(value)
    ? value as Record<string, unknown>
    : {};
}

function preserveCurrentTimestamp(
  existing: unknown,
  version: string,
  timestampField: "acknowledgedAt" | "acceptedAt" | "presentedAt",
): unknown {
  const record = asRecord(existing);
  return record.version === version && record[timestampField]
    ? record[timestampField]
    : FieldValue.serverTimestamp();
}

export async function recordLegalAcknowledgementsService(
  database: Firestore,
  uid: string,
  input: LegalAcknowledgementInput,
): Promise<{ recorded: true }> {
  requireLegalAcknowledgements(input);
  const userRef = database.doc(`users/${uid}`);

  await database.runTransaction(async (transaction) => {
    const user = await transaction.get(userRef);
    if (!user.exists) {
      throw new HttpsError("failed-precondition", "USER_PROFILE_REQUIRED");
    }

    const current = asRecord(user.get("legalAcknowledgements"));
    transaction.update(userRef, {
      "legalAcknowledgements.kvkkNotice": {
        version: KVKK_NOTICE_VERSION,
        acknowledgedAt: preserveCurrentTimestamp(
          current.kvkkNotice,
          KVKK_NOTICE_VERSION,
          "acknowledgedAt",
        ),
      },
      "legalAcknowledgements.userAgreement": {
        version: USER_AGREEMENT_VERSION,
        acceptedAt: preserveCurrentTimestamp(
          current.userAgreement,
          USER_AGREEMENT_VERSION,
          "acceptedAt",
        ),
      },
      "legalAcknowledgements.privacyPolicy": {
        version: PRIVACY_POLICY_VERSION,
        presentedAt: preserveCurrentTimestamp(
          current.privacyPolicy,
          PRIVACY_POLICY_VERSION,
          "presentedAt",
        ),
      },
      updatedAt: FieldValue.serverTimestamp(),
    });
  });

  return { recorded: true };
}
