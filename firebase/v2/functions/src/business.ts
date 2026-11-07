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

    let membershipsDocs: FirebaseFirestore.DocumentSnapshot[] = [];
    try {
      const membershipsQuery = database
        .collectionGroup("members")
        .where("userId", "==", actorUid)
        .limit(10);
      const membershipsSnapshot = await transaction.get(membershipsQuery);
      membershipsDocs = membershipsSnapshot.docs;
    } catch {
      // Fallback if collectionGroup index is building or unavailable
      const orgs = await database
        .collection("organizations")
        .where("type", "==", "business")
        .where("status", "==", "active")
        .limit(20)
        .get();
      for (const org of orgs.docs) {
        const member = await transaction.get(org.ref.collection("members").doc(actorUid));
        const role = member.get("role");
        if (member.exists && member.get("status") === "active" && ["owner", "staff"].includes(role)) {
          return {
            organizationId: org.id,
            organizationName: String(org.get("name") ?? "İşletme"),
            membershipRole: role as "owner" | "staff",
          };
        }
      }
      throw new HttpsError("permission-denied", "ROLE_NOT_ALLOWED");
    }

    for (const membership of membershipsDocs) {
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
