import {
  FieldValue,
  type Firestore,
  Timestamp,
} from "firebase-admin/firestore";
import { HttpsError } from "firebase-functions/v2/https";
import { requireActiveActor, requireNonEmptyString } from "./shared.js";

export type OrganizationType = "business" | "community";

export interface CreateOrganizationInput {
  name: unknown;
  type: unknown;
  university?: unknown;
  legacyTestBusinessId?: unknown;
}

export async function createOrganizationService(
  database: Firestore,
  actorUid: string,
  input: CreateOrganizationInput,
): Promise<{ organizationId: string }> {
  const name = requireNonEmptyString(input.name, "name", 160);
  if (input.type !== "business" && input.type !== "community") {
    throw new HttpsError("invalid-argument", "ORGANIZATION_TYPE_INVALID");
  }
  const type = input.type;
  const university = type === "community"
    ? requireNonEmptyString(input.university, "university", 160)
    : "";
  const legacyTestBusinessId = type === "business" && typeof input.legacyTestBusinessId === "string"
    ? input.legacyTestBusinessId.trim()
    : "";
  if (legacyTestBusinessId && !/^[A-Za-z0-9_-]{1,128}$/.test(legacyTestBusinessId)) {
    throw new HttpsError("invalid-argument", "LEGACY_TEST_BUSINESS_ID_INVALID");
  }
  const organizationRef = database.collection("organizations").doc();
  const legacyLinkRef = legacyTestBusinessId
    ? database.doc(`legacyTestBusinessLinks/${legacyTestBusinessId}`)
    : null;
  const auditRef = database.collection("auditLogs").doc();

  await database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, actorUid, ["good4Admin"]);
    if (legacyLinkRef && (await transaction.get(legacyLinkRef)).exists) {
      throw new HttpsError("already-exists", "LEGACY_TEST_BUSINESS_ALREADY_LINKED");
    }
    transaction.create(organizationRef, {
      name,
      type,
      university,
      status: "active",
      followerCount: 0,
      createdAt: FieldValue.serverTimestamp(),
      createdBy: actorUid,
      updatedAt: FieldValue.serverTimestamp(),
      ...(legacyTestBusinessId ? { legacyTestBusinessId } : {}),
    });
    if (legacyLinkRef) {
      transaction.create(legacyLinkRef, {
        organizationId: organizationRef.id,
        createdAt: FieldValue.serverTimestamp(),
        createdBy: actorUid,
      });
    }
    transaction.create(auditRef, {
      action: "organization.created",
      actorUid,
      targetType: "organization",
      targetId: organizationRef.id,
      metadata: { name, type, legacyTestBusinessId },
      createdAt: FieldValue.serverTimestamp(),
    });
  });

  return { organizationId: organizationRef.id };
}

export interface AssignOrganizationMemberInput {
  organizationId: unknown;
  userId: unknown;
  email: unknown;
  displayName?: unknown;
  role: unknown;
}

export async function assignOrganizationMemberService(
  database: Firestore,
  actorUid: string,
  input: AssignOrganizationMemberInput,
): Promise<{ organizationId: string; userId: string }> {
  const organizationId = requireNonEmptyString(input.organizationId, "organizationId", 128);
  const userId = requireNonEmptyString(input.userId, "userId", 128);
  const email = requireNonEmptyString(input.email, "email", 320).toLowerCase();
  const displayName = typeof input.displayName === "string"
    ? input.displayName.trim().slice(0, 120)
    : "";

  const organizationRef = database.doc(`organizations/${organizationId}`);
  const userRef = database.doc(`users/${userId}`);
  const memberRef = organizationRef.collection("members").doc(userId);
  const auditRef = database.collection("auditLogs").doc();

  await database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, actorUid, ["good4Admin"]);
    const [organizationSnapshot, userSnapshot] = await Promise.all([
      transaction.get(organizationRef),
      transaction.get(userRef),
    ]);
    if (!organizationSnapshot.exists || organizationSnapshot.get("status") !== "active") {
      throw new HttpsError("not-found", "ORGANIZATION_NOT_ACTIVE");
    }

    const organizationType = organizationSnapshot.get("type") as OrganizationType;
    const allowedRoles = organizationType === "business"
      ? ["owner", "staff"]
      : ["manager", "staff"];
    if (typeof input.role !== "string" || !allowedRoles.includes(input.role)) {
      throw new HttpsError("invalid-argument", "MEMBERSHIP_ROLE_INVALID");
    }
    const membershipRole = input.role;
    const userRole = organizationType === "business"
      ? membershipRole === "owner" ? "businessOwner" : "businessStaff"
      : membershipRole === "manager" ? "communityManager" : "communityStaff";

    transaction.set(userRef, {
      email,
      displayName,
      role: userRole,
      status: "active",
      university: userSnapshot.exists ? userSnapshot.get("university") ?? "" : "",
      createdAt: userSnapshot.exists
        ? userSnapshot.get("createdAt") ?? FieldValue.serverTimestamp()
        : FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
    transaction.set(memberRef, {
      userId,
      role: membershipRole,
      status: "active",
      assignedBy: actorUid,
      assignedAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    transaction.create(auditRef, {
      action: "organization.memberAssigned",
      actorUid,
      targetType: "organizationMember",
      targetId: `${organizationId}:${userId}`,
      metadata: { organizationId, userId, membershipRole, userRole },
      createdAt: FieldValue.serverTimestamp(),
    });
  });

  return { organizationId, userId };
}

export interface CreateCampaignInput {
  organizationId: unknown;
  title: unknown;
  description?: unknown;
  startsAt: unknown;
  endsAt: unknown;
  status?: unknown;
  totalLimit?: unknown;
}

function parseTimestamp(value: unknown, fieldName: string): Timestamp {
  if (typeof value !== "string") {
    throw new HttpsError("invalid-argument", `${fieldName.toUpperCase()}_REQUIRED`);
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    throw new HttpsError("invalid-argument", `${fieldName.toUpperCase()}_INVALID`);
  }
  return Timestamp.fromDate(date);
}

/** Good4 admins stop a published campaign early; no new codes are issued or redeemed after this. */
export async function endCampaignService(
  database: Firestore,
  actorUid: string,
  input: { campaignId?: unknown },
): Promise<{ campaignId: string; status: "ended" }> {
  const campaignId = requireNonEmptyString(input.campaignId, "campaignId", 128);
  const campaignRef = database.doc(`campaigns/${campaignId}`);
  await database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, actorUid, ["good4Admin"]);
    const campaign = await transaction.get(campaignRef);
    if (!campaign.exists) throw new HttpsError("not-found", "CAMPAIGN_NOT_FOUND");
    transaction.update(campaignRef, { status: "ended", updatedAt: FieldValue.serverTimestamp() });
    transaction.create(database.collection("auditLogs").doc(), {
      action: "campaign.ended",
      actorUid,
      targetType: "campaign",
      targetId: campaignId,
      metadata: { previousStatus: campaign.get("status") ?? null },
      createdAt: FieldValue.serverTimestamp(),
    });
  });
  return { campaignId, status: "ended" };
}

export async function createCampaignService(
  database: Firestore,
  actorUid: string,
  input: CreateCampaignInput,
): Promise<{ campaignId: string }> {
  const organizationId = requireNonEmptyString(input.organizationId, "organizationId", 128);
  const title = requireNonEmptyString(input.title, "title", 160);
  const description = typeof input.description === "string"
    ? input.description.trim().slice(0, 2000)
    : "";
  const startsAt = parseTimestamp(input.startsAt, "startsAt");
  const endsAt = parseTimestamp(input.endsAt, "endsAt");
  if (endsAt.toMillis() <= startsAt.toMillis()) {
    throw new HttpsError("invalid-argument", "CAMPAIGN_DATE_RANGE_INVALID");
  }
  const status = input.status === "published" ? "published" : "draft";
  const totalLimit = Number.isInteger(input.totalLimit) && Number(input.totalLimit) > 0
    ? Number(input.totalLimit)
    : null;
  const campaignRef = database.collection("campaigns").doc();
  const auditRef = database.collection("auditLogs").doc();

  await database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, actorUid, ["good4Admin"]);
    const organizationSnapshot = await transaction.get(
      database.doc(`organizations/${organizationId}`),
    );
    if (!organizationSnapshot.exists
      || organizationSnapshot.get("type") !== "business"
      || organizationSnapshot.get("status") !== "active") {
      throw new HttpsError("failed-precondition", "BUSINESS_NOT_ACTIVE");
    }
    transaction.create(campaignRef, {
      organizationId,
      title,
      description,
      startsAt,
      endsAt,
      status,
      totalLimit,
      redemptionCount: 0,
      createdAt: FieldValue.serverTimestamp(),
      createdBy: actorUid,
      updatedAt: FieldValue.serverTimestamp(),
    });
    transaction.create(auditRef, {
      action: "campaign.created",
      actorUid,
      targetType: "campaign",
      targetId: campaignRef.id,
      metadata: { organizationId, status },
      createdAt: FieldValue.serverTimestamp(),
    });
  });

  return { campaignId: campaignRef.id };
}
