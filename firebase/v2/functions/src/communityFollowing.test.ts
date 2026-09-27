import assert from "node:assert/strict";
import { after, beforeEach, test } from "node:test";
import { Timestamp } from "firebase-admin/firestore";
import { db, legacyTestDb } from "./firebase.js";
import { getFollowingCommunityIdsService, setCommunityFollowingService } from "./communityFollowing.js";

beforeEach(async () => {
  for (const collection of ["users", "organizations", "unrelated"]) {
    await db.recursiveDelete(db.collection(collection));
  }
  await Promise.all([
    db.doc("users/student-1").set({ role: "student", status: "active" }),
    db.doc("users/student-2").set({ role: "student", status: "active" }),
    db.doc("organizations/community-1").set({ type: "community", status: "active" }),
    db.doc("organizations/community-2").set({ type: "community", status: "active" }),
  ]);
});

after(async () => { await Promise.all([db.terminate(), legacyTestDb.terminate()]); });

test("following IDs include only the actor's canonical follower paths", async () => {
  await Promise.all([
    db.doc("organizations/community-1/followers/student-1").set({ userId: "student-1" }),
    db.doc("organizations/community-2/followers/student-2").set({ userId: "student-2" }),
    db.doc("organizations/community-2/followers/not-the-actor").set({ userId: "student-1" }),
    db.doc("unrelated/example/followers/student-1").set({ userId: "student-1" }),
    db.doc("organizations/community-2/nested/example/followers/student-1").set({ userId: "student-1" }),
  ]);
  assert.deepEqual(await getFollowingCommunityIdsService(db, "student-1"), { communityIds: ["community-1"] });
  assert.deepEqual(await getFollowingCommunityIdsService(db, "student-2"), { communityIds: ["community-2"] });
});

test("inactive and missing actors cannot read or change follows", async () => {
  await db.doc("users/student-1").update({ status: "disabled" });
  for (const uid of ["student-1", "missing-user"]) {
    await assert.rejects(getFollowingCommunityIdsService(db, uid));
    await assert.rejects(setCommunityFollowingService(db, uid, { communityId: "community-1", following: true }));
  }
  assert.equal((await db.collection("organizations/community-1/followers").get()).size, 0);
});

test("concurrent and duplicate follow changes preserve the count and server timestamp", async () => {
  const follow = { communityId: "community-1", following: true };
  const results = await Promise.all([
    setCommunityFollowingService(db, "student-1", follow),
    setCommunityFollowingService(db, "student-1", follow),
    setCommunityFollowingService(db, "student-2", follow),
  ]);
  assert.equal(results.filter((result) => result.changed).length, 2);
  assert.equal((await db.doc("organizations/community-1").get()).get("followerCount"), 2);
  const follower = await db.doc("organizations/community-1/followers/student-1").get();
  assert.equal(follower.get("userId"), "student-1");
  assert.ok(follower.get("followedAt") instanceof Timestamp);
  await Promise.all([
    setCommunityFollowingService(db, "student-1", { ...follow, following: false }),
    setCommunityFollowingService(db, "student-1", { ...follow, following: false }),
  ]);
  assert.equal((await db.doc("organizations/community-1").get()).get("followerCount"), 1);
  assert.deepEqual(await getFollowingCommunityIdsService(db, "student-1"), { communityIds: [] });
});

test("first change initializes an absent counter from existing followers", async () => {
  await db.doc("organizations/community-1/followers/student-2").set({ userId: "student-2" });
  await setCommunityFollowingService(db, "student-1", { communityId: "community-1", following: true });
  assert.equal((await db.doc("organizations/community-1").get()).get("followerCount"), 2);
});

test("only active communities can be followed, but inactive communities can be left", async () => {
  await setCommunityFollowingService(db, "student-1", { communityId: "community-1", following: true });
  await db.doc("organizations/community-1").update({ status: "disabled" });
  await assert.rejects(setCommunityFollowingService(db, "student-2", { communityId: "community-1", following: true }), /COMMUNITY_NOT_ACTIVE/);
  await setCommunityFollowingService(db, "student-1", { communityId: "community-1", following: false });
  await db.doc("organizations/community-2").update({ type: "business" });
  for (const communityId of ["community-2", "missing-community"]) {
    await assert.rejects(setCommunityFollowingService(db, "student-1", { communityId, following: true }), /COMMUNITY_NOT_ACTIVE/);
  }
  assert.equal((await db.doc("organizations/community-1").get()).get("followerCount"), 0);
});

test("invalid follow input cannot create arbitrary paths or coerce a boolean", async () => {
  for (const input of [{ communityId: "community-1", following: "true" }, { communityId: "a/b", following: true }]) {
    await assert.rejects(setCommunityFollowingService(db, "student-1", input), /COMMUNITY_FOLLOW_INPUT_INVALID/);
  }
});
