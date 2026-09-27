import { FieldValue, type Firestore } from "firebase-admin/firestore";
import { HttpsError } from "firebase-functions/v2/https";
import { requireActiveActor, requireNonEmptyString } from "./shared.js";

/** One group query; only the authenticated user's canonical follower documents are returned. */
export async function getFollowingCommunityIdsService(
  database: Firestore, actorUid: string,
): Promise<{ communityIds: string[] }> {
  await database.runTransaction((transaction) => requireActiveActor(database, transaction, actorUid));
  const followers = await database.collectionGroup("followers").where("userId", "==", actorUid).get();
  const communityIds = new Set<string>();
  for (const follower of followers.docs) {
    const path = follower.ref.path.split("/");
    if (path.length === 4 && path[0] === "organizations" && path[2] === "followers"
      && path[3] === actorUid && /^[A-Za-z0-9_-]{1,128}$/.test(path[1] ?? "")) {
      communityIds.add(path[1]!);
    }
  }
  // The client intersects these IDs with its existing active community list.
  return { communityIds: [...communityIds].sort() };
}

export async function setCommunityFollowingService(
  database: Firestore, actorUid: string, input: { communityId?: unknown; following?: unknown },
): Promise<{ communityId: string; following: boolean; changed: boolean }> {
  const communityId = requireNonEmptyString(input.communityId, "communityId", 128);
  if (!/^[A-Za-z0-9_-]+$/.test(communityId) || typeof input.following !== "boolean") {
    throw new HttpsError("invalid-argument", "COMMUNITY_FOLLOW_INPUT_INVALID");
  }
  const following = input.following;
  const organizationRef = database.doc(`organizations/${communityId}`);
  const followerRef = organizationRef.collection("followers").doc(actorUid);
  const changed = await database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, actorUid);
    const organization = await transaction.get(organizationRef);
    const follower = await transaction.get(followerRef);
    if (following && (!organization.exists || organization.get("type") !== "community"
      || organization.get("status") !== "active")) {
      throw new HttpsError("failed-precondition", "COMMUNITY_NOT_ACTIVE");
    }
    if (follower.exists && follower.get("userId") !== actorUid) {
      throw new HttpsError("failed-precondition", "COMMUNITY_FOLLOW_INVALID");
    }
    if (following === follower.exists) return false;
    const rawCount = organization.get("followerCount");
    // Older organizations may have followers without a counter. Initialize on the first change.
    const count = !organization.exists ? 0 : Number.isSafeInteger(rawCount) && rawCount >= 0
      ? Number(rawCount) : (await transaction.get(organizationRef.collection("followers"))).size;
    if (following) {
      transaction.create(followerRef, { userId: actorUid, followedAt: FieldValue.serverTimestamp() });
    } else {
      // Also allow leaving a community that has since been disabled or removed.
      transaction.delete(followerRef);
    }
    if (organization.exists) {
      transaction.update(organizationRef, { followerCount: Math.max(0, count + (following ? 1 : -1)) });
    }
    return true;
  });
  return { communityId, following, changed };
}
