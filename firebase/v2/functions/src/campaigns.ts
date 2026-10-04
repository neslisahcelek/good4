import { randomInt } from "node:crypto";
import {
  FieldValue,
  type Firestore,
  Timestamp,
} from "firebase-admin/firestore";
import { HttpsError } from "firebase-functions/v2/https";
import { hasVerifiedEduEmail } from "./eduVerification.js";
import { requireActiveActor, requireNonEmptyString } from "./shared.js";
import {
  assertNotBlocked,
  assertRateLimit,
  clearFailedAttempts,
  recordFailedAttempt,
} from "./rateLimit.js";

const CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
const CODE_LENGTH = 8;
const CODE_VALIDITY_MINUTES = 10;

class CodeCollisionError extends Error {}

export function normalizeCampaignCode(value: unknown): string {
  if (typeof value !== "string") {
    throw new HttpsError("invalid-argument", "CODE_REQUIRED");
  }
  const normalized = value.toUpperCase().replace(/[\s-]+/g, "");
  if (!/^[A-Z0-9]{6,12}$/.test(normalized)) {
    throw new HttpsError("invalid-argument", "CODE_FORMAT_INVALID");
  }
  return normalized;
}

function generateCampaignCode(): string {
  return Array.from({ length: CODE_LENGTH }, () => (
    CODE_ALPHABET[randomInt(CODE_ALPHABET.length)]
  )).join("");
}

function timestampField(value: unknown, fieldName: string): Timestamp {
  if (!(value instanceof Timestamp)) {
    throw new HttpsError("failed-precondition", `${fieldName.toUpperCase()}_INVALID`);
  }
  return value;
}

export interface IssueCampaignCodeInput {
  campaignId: unknown;
}

export interface IssueCampaignCodeResult {
  outcome: "issued" | "already_issued" | "already_redeemed";
  code?: string;
  expiresAt?: string;
}

export async function issueCampaignCodeService(
  database: Firestore,
  actorUid: string,
  input: IssueCampaignCodeInput,
  nowMillis = Date.now(),
): Promise<IssueCampaignCodeResult> {
  const campaignId = requireNonEmptyString(input.campaignId, "campaignId", 128);

  for (let attempt = 0; attempt < 5; attempt += 1) {
    const code = generateCampaignCode();
    try {
      return await database.runTransaction(async (transaction) => {
        await requireActiveActor(database, transaction, actorUid, ["student"]);
        if (!hasVerifiedEduEmail(await transaction.get(database.doc(`users/${actorUid}`)))) {
          throw new HttpsError("permission-denied", "EDU_VERIFICATION_REQUIRED");
        }
        const campaignRef = database.doc(`campaigns/${campaignId}`);
        const claimRef = database.doc(`campaignClaims/${campaignId}_${actorUid}`);
        const [campaignSnapshot, claimSnapshot] = await Promise.all([
          transaction.get(campaignRef),
          transaction.get(claimRef),
        ]);
        if (!campaignSnapshot.exists || campaignSnapshot.get("status") !== "published") {
          throw new HttpsError("failed-precondition", "CAMPAIGN_NOT_PUBLISHED");
        }

        if (claimSnapshot.exists) {
          if (claimSnapshot.get("status") === "redeemed") {
            return { outcome: "already_redeemed" };
          }
          const existingCode = claimSnapshot.get("code");
          const existingExpiresAt = claimSnapshot.get("expiresAt");
          if (typeof existingCode === "string" && existingExpiresAt instanceof Timestamp) {
            return {
              outcome: "already_issued",
              code: existingCode,
              expiresAt: existingExpiresAt.toDate().toISOString(),
            };
          }
        }

        const organizationId = campaignSnapshot.get("organizationId");
        if (typeof organizationId !== "string") {
          throw new HttpsError("failed-precondition", "CAMPAIGN_BUSINESS_INVALID");
        }
        const organizationRef = database.doc(`organizations/${organizationId}`);
        const codeRef = database.doc(`campaignCodes/${code}`);
        const [organizationSnapshot, codeSnapshot] = await Promise.all([
          transaction.get(organizationRef),
          transaction.get(codeRef),
        ]);
        if (!organizationSnapshot.exists
          || organizationSnapshot.get("type") !== "business"
          || organizationSnapshot.get("status") !== "active") {
          throw new HttpsError("failed-precondition", "BUSINESS_NOT_ACTIVE");
        }
        if (codeSnapshot.exists) {
          throw new CodeCollisionError();
        }

        const now = Timestamp.now();
        const startsAt = timestampField(campaignSnapshot.get("startsAt"), "startsAt");
        const endsAt = timestampField(campaignSnapshot.get("endsAt"), "endsAt");
        if (now.toMillis() < startsAt.toMillis() || now.toMillis() >= endsAt.toMillis()) {
          throw new HttpsError("failed-precondition", "CAMPAIGN_NOT_ACTIVE");
        }
        const totalLimit = campaignSnapshot.get("totalLimit");
        if (typeof totalLimit === "number" && (campaignSnapshot.get("redemptionCount") ?? 0) >= totalLimit) {
          // Do not send a student to the business for a meal that is already gone.
          throw new HttpsError("failed-precondition", "CAMPAIGN_LIMIT_REACHED");
        }
        const shortExpiry = Timestamp.fromMillis(
          now.toMillis() + CODE_VALIDITY_MINUTES * 60 * 1000,
        );
        const expiresAt = shortExpiry.toMillis() < endsAt.toMillis() ? shortExpiry : endsAt;
        const auditRef = database.collection("auditLogs").doc();

        await assertRateLimit(database, transaction, {
          key: `issue_code:${actorUid}`,
          limit: 5,
          windowSeconds: 60,
          errorMessage: "RATE_LIMIT_EXCEEDED",
        }, nowMillis);
        transaction.create(codeRef, {
          code,
          campaignId,
          organizationId,
          studentId: actorUid,
          status: "issued",
          issuedAt: FieldValue.serverTimestamp(),
          expiresAt,
          redeemedAt: null,
          redeemedBy: null,
        });
        transaction.set(claimRef, {
          campaignId,
          studentId: actorUid,
          code,
          status: "issued",
          issuedAt: FieldValue.serverTimestamp(),
          expiresAt,
        });
        transaction.create(auditRef, {
          action: "campaignCode.issued",
          actorUid,
          targetType: "campaignCode",
          targetId: code,
          metadata: { campaignId, organizationId },
          createdAt: FieldValue.serverTimestamp(),
        });

        return {
          outcome: "issued",
          code,
          expiresAt: expiresAt.toDate().toISOString(),
        };
      });
    } catch (error) {
      if (error instanceof CodeCollisionError) {
        continue;
      }
      throw error;
    }
  }

  throw new HttpsError("resource-exhausted", "CODE_GENERATION_RETRY_EXHAUSTED");
}

export interface RedeemCampaignCodeInput {
  code: unknown;
}

export type RedeemCampaignCodeResult =
  | { outcome: "redeemed"; campaignId: string; campaignTitle: string }
  | { outcome: "not_found" }
  | { outcome: "wrong_business" }
  | { outcome: "already_redeemed" }
  | { outcome: "expired" }
  | { outcome: "campaign_inactive" }
  | { outcome: "campaign_limit_reached" };

export async function redeemCampaignCodeService(
  database: Firestore,
  actorUid: string,
  input: RedeemCampaignCodeInput,
  nowMillis = Date.now(),
): Promise<RedeemCampaignCodeResult> {
  const code = normalizeCampaignCode(input.code);

  const result = await database.runTransaction<RedeemCampaignCodeResult | { rateLimitError: HttpsError }>(async (transaction) => {
    await requireActiveActor(
      database,
      transaction,
      actorUid,
      ["businessOwner", "businessStaff"],
    );
    await assertNotBlocked(database, transaction, `redeem_fail:${actorUid}`, nowMillis);

    const codeRef = database.doc(`campaignCodes/${code}`);
    const codeSnapshot = await transaction.get(codeRef);
    if (!codeSnapshot.exists) {
      const failure = await recordFailedAttempt(database, transaction, {
        key: `redeem_fail:${actorUid}`,
        maxAttempts: 5,
        windowSeconds: 60,
        blockDurationSeconds: 300,
        errorMessage: "TOO_MANY_FAILED_ATTEMPTS",
      }, nowMillis);
      if (failure.error) return { rateLimitError: failure.error };
      return { outcome: "not_found" };
    }

    const campaignId = codeSnapshot.get("campaignId");
    const organizationId = codeSnapshot.get("organizationId");
    const studentId = codeSnapshot.get("studentId");
    if (typeof campaignId !== "string"
      || typeof organizationId !== "string"
      || typeof studentId !== "string") {
      throw new HttpsError("internal", "CAMPAIGN_CODE_DATA_INVALID");
    }

    const campaignRef = database.doc(`campaigns/${campaignId}`);
    const organizationRef = database.doc(`organizations/${organizationId}`);
    const memberRef = organizationRef.collection("members").doc(actorUid);
    const claimRef = database.doc(`campaignClaims/${campaignId}_${studentId}`);
    const redemptionRef = database.doc(`redemptions/${code}`);
    const [campaignSnapshot, organizationSnapshot, memberSnapshot, claimSnapshot] = await Promise.all([
      transaction.get(campaignRef),
      transaction.get(organizationRef),
      transaction.get(memberRef),
      transaction.get(claimRef),
    ]);

    if (!organizationSnapshot.exists
      || organizationSnapshot.get("type") !== "business"
      || organizationSnapshot.get("status") !== "active"
      || !memberSnapshot.exists
      || memberSnapshot.get("status") !== "active"
      || !["owner", "staff"].includes(memberSnapshot.get("role"))) {
      const failure = await recordFailedAttempt(database, transaction, {
        key: `redeem_fail:${actorUid}`,
        maxAttempts: 5,
        windowSeconds: 60,
        blockDurationSeconds: 300,
        errorMessage: "TOO_MANY_FAILED_ATTEMPTS",
      }, nowMillis);
      if (failure.error) return { rateLimitError: failure.error };
      return { outcome: "wrong_business" };
    }
    if (codeSnapshot.get("status") === "redeemed") {
      return { outcome: "already_redeemed" };
    }

    const now = Timestamp.now();
    const expiresAt = timestampField(codeSnapshot.get("expiresAt"), "expiresAt");
    if (codeSnapshot.get("status") === "expired" || now.toMillis() >= expiresAt.toMillis()) {
      const auditRef = database.collection("auditLogs").doc();
      transaction.update(codeRef, {
        status: "expired",
        updatedAt: FieldValue.serverTimestamp(),
      });
      if (claimSnapshot.exists) {
        transaction.update(claimRef, {
          status: "expired",
          updatedAt: FieldValue.serverTimestamp(),
        });
      }
      transaction.create(auditRef, {
        action: "campaignCode.expired",
        actorUid,
        targetType: "campaignCode",
        targetId: code,
        metadata: { campaignId, organizationId },
        createdAt: FieldValue.serverTimestamp(),
      });
      return { outcome: "expired" };
    }
    if (!campaignSnapshot.exists || campaignSnapshot.get("status") !== "published") {
      return { outcome: "campaign_inactive" };
    }

    const startsAt = timestampField(campaignSnapshot.get("startsAt"), "startsAt");
    const endsAt = timestampField(campaignSnapshot.get("endsAt"), "endsAt");
    if (now.toMillis() < startsAt.toMillis() || now.toMillis() >= endsAt.toMillis()) {
      return { outcome: "campaign_inactive" };
    }
    const totalLimit = campaignSnapshot.get("totalLimit");
    const redemptionCount = campaignSnapshot.get("redemptionCount") ?? 0;
    if (typeof totalLimit === "number" && redemptionCount >= totalLimit) {
      return { outcome: "campaign_limit_reached" };
    }

    const auditRef = database.collection("auditLogs").doc();
    transaction.update(codeRef, {
      status: "redeemed",
      redeemedAt: FieldValue.serverTimestamp(),
      redeemedBy: actorUid,
      updatedAt: FieldValue.serverTimestamp(),
    });
    if (claimSnapshot.exists) {
      transaction.update(claimRef, {
        status: "redeemed",
        redeemedAt: FieldValue.serverTimestamp(),
        redeemedBy: actorUid,
        updatedAt: FieldValue.serverTimestamp(),
      });
    }
    transaction.create(redemptionRef, {
      code,
      campaignId,
      organizationId,
      studentId,
      redeemedBy: actorUid,
      redeemedAt: FieldValue.serverTimestamp(),
    });
    transaction.update(campaignRef, {
      redemptionCount: FieldValue.increment(1),
      updatedAt: FieldValue.serverTimestamp(),
    });
    transaction.create(auditRef, {
      action: "campaignCode.redeemed",
      actorUid,
      targetType: "campaignCode",
      targetId: code,
      metadata: { campaignId, organizationId },
      createdAt: FieldValue.serverTimestamp(),
    });
    await clearFailedAttempts(database, transaction, `redeem_fail:${actorUid}`);

    return {
      outcome: "redeemed",
      campaignId,
      campaignTitle: String(campaignSnapshot.get("title") ?? ""),
    };
  });
  if ("rateLimitError" in result) throw result.rateLimitError;
  return result;
}
