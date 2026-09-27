import { FieldValue, type Firestore } from "firebase-admin/firestore";
import { HttpsError } from "firebase-functions/v2/https";
import { getBusinessContextService } from "./business.js";

export interface LegacyCouponResult {
  outcome: "redeemed" | "not_found" | "wrong_business" | "already_redeemed" | "expired" | "campaign_inactive" | "stale_code";
  campaignTitle?: string;
}

function dateInIstanbul(now: Date): string {
  const parts = new Intl.DateTimeFormat("en-GB", {
    timeZone: "Europe/Istanbul",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(now);
  const value = (type: string) => parts.find((part) => part.type === type)?.value ?? "";
  return `${value("year")}-${value("month")}-${value("day")}`;
}

/** Redeems only six-digit coupons from the Good4Test mobile database. */
export async function redeemLegacyCouponService(
  v2Database: Firestore,
  legacyTestDatabase: Firestore,
  actorUid: string,
  input: { code: unknown },
  now = new Date(),
): Promise<LegacyCouponResult> {
  if (typeof input.code !== "string" || !/^[0-9]{6}$/.test(input.code)) {
    throw new HttpsError("invalid-argument", "CODE_FORMAT_INVALID");
  }

  const context = await getBusinessContextService(v2Database, actorUid);
  const organization = await v2Database.doc(`organizations/${context.organizationId}`).get();
  const legacyBusinessId = organization.get("legacyTestBusinessId");
  if (typeof legacyBusinessId !== "string" || !/^[A-Za-z0-9_-]{1,128}$/.test(legacyBusinessId)) {
    throw new HttpsError("failed-precondition", "LEGACY_TEST_BUSINESS_NOT_LINKED");
  }

  const codeRef = legacyTestDatabase.doc(`community_coupon_codes/${input.code}`);
  const result = await legacyTestDatabase.runTransaction(async (transaction) => {
    const code = await transaction.get(codeRef);
    if (!code.exists) return { outcome: "not_found" } as const;
    if (code.get("businessId") !== legacyBusinessId) return { outcome: "wrong_business" } as const;
    if (code.get("status") === "used") return { outcome: "already_redeemed" } as const;
    if (code.get("status") !== "pending") return { outcome: "campaign_inactive" } as const;

    const communityId = code.get("communityId");
    const entryId = code.get("entryId");
    const studentId = code.get("userId");
    if (![communityId, entryId, studentId].every((value) => typeof value === "string" && /^[A-Za-z0-9_-]{1,128}$/.test(value))) {
      throw new HttpsError("failed-precondition", "LEGACY_COUPON_DATA_INVALID");
    }
    const entryRef = legacyTestDatabase.doc(`communities/${communityId}/entries/${entryId}`);
    const claimRef = entryRef.collection("claims").doc(studentId);
    const [entry, claim] = await Promise.all([
      transaction.get(entryRef),
      transaction.get(claimRef),
    ]);
    if (!entry.exists || entry.get("kind") !== "coupon" || entry.get("status") !== "published"
      || entry.get("businessId") !== legacyBusinessId) {
      return { outcome: "campaign_inactive" } as const;
    }
    if (!claim.exists || claim.get("value") !== input.code
      || claim.get("userId") !== studentId || claim.get("businessId") !== legacyBusinessId
      || claim.get("status") !== "pending") {
      return { outcome: "stale_code" } as const;
    }

    const expiresOn = code.get("expiresOn");
    if (typeof expiresOn !== "string" || !/^\d{4}-\d{2}-\d{2}$/.test(expiresOn)
      || expiresOn !== entry.get("date") || expiresOn < dateInIstanbul(now)) {
      return { outcome: "expired" } as const;
    }

    transaction.update(codeRef, { status: "used", usedAt: Math.floor(now.getTime() / 1000) });
    transaction.update(claimRef, { status: "used" });
    return {
      outcome: "redeemed",
      campaignTitle: String(entry.get("title") ?? code.get("title") ?? "Topluluk kuponu"),
      studentId,
      communityId,
      entryId,
    } as const;
  });

  if (result.outcome === "redeemed") {
    // The old Firestore transaction is the source of truth. Audit failure must
    // not turn a successfully consumed code into a retryable UI error.
    try {
      const writeBatch = v2Database.batch();
      writeBatch.set(v2Database.doc(`legacyTestRedemptions/${input.code}`), {
        code: input.code,
        organizationId: context.organizationId,
        legacyBusinessId,
        legacyCommunityId: result.communityId,
        legacyEntryId: result.entryId,
        legacyStudentId: result.studentId,
        redeemedBy: actorUid,
        redeemedAt: FieldValue.serverTimestamp(),
      });
      writeBatch.set(v2Database.doc(`auditLogs/legacyCouponRedeemed_${input.code}`), {
        action: "legacyCoupon.redeemed",
        actorUid,
        organizationId: context.organizationId,
        targetType: "legacyCouponCode",
        targetId: input.code,
        metadata: {
          legacyBusinessId,
          legacyCommunityId: result.communityId,
          legacyEntryId: result.entryId,
        },
        createdAt: FieldValue.serverTimestamp(),
      });
      await writeBatch.commit();
    } catch (error) {
      console.error("Legacy test redemption audit write failed", error);
    }
  }
  return result.outcome === "redeemed"
    ? { outcome: "redeemed", campaignTitle: result.campaignTitle }
    : result;
}
