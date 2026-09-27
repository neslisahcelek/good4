import type { Firestore } from "firebase-admin/firestore";
import { HttpsError } from "firebase-functions/v2/https";
import { requireActiveActor } from "./shared.js";

export interface BusinessContextResult {
  organizationId: string;
  organizationName: string;
  membershipRole: "owner" | "staff";
}

export async function getBusinessContextService(
  database: Firestore,
  actorUid: string,
): Promise<BusinessContextResult> {
  return database.runTransaction(async (transaction) => {
    await requireActiveActor(
      database,
      transaction,
      actorUid,
      ["businessOwner", "businessStaff"],
    );

    const membershipsQuery = database
      .collectionGroup("members")
      .where("userId", "==", actorUid)
      .limit(10);
    const membershipsSnapshot = await transaction.get(membershipsQuery);

    for (const membership of membershipsSnapshot.docs) {
      const role = membership.get("role");
      if (membership.get("status") !== "active" || !["owner", "staff"].includes(role)) {
        continue;
      }

      const organizationRef = membership.ref.parent.parent;
      if (!organizationRef) {
        continue;
      }
      const organization = await transaction.get(organizationRef);
      if (!organization.exists
        || organization.get("type") !== "business"
        || organization.get("status") !== "active") {
        continue;
      }

      return {
        organizationId: organization.id,
        organizationName: String(organization.get("name") ?? "İşletme"),
        membershipRole: role as "owner" | "staff",
      };
    }

    throw new HttpsError("failed-precondition", "BUSINESS_MEMBERSHIP_NOT_FOUND");
  });
}
