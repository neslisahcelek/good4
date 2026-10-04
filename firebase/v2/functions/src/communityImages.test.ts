import assert from "node:assert/strict";
import { after, beforeEach, test } from "node:test";
import { db, legacyTestDb, storageBucket } from "./firebase.js";
import { uploadCommunityEventImageService } from "./communityImages.js";
import { saveEventService } from "./events.js";

const storageSkip = process.env.FIREBASE_STORAGE_EMULATOR_HOST === "127.0.0.1:9295"
  ? false : "Storage emulator at 127.0.0.1:9295 is unavailable or not configured";
const uploadedObjects: string[] = [];
const jpeg = Buffer.from([0xff, 0xd8, 0xff, 0xd9]);
const png = Buffer.from("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a7XcAAAAASUVORK5CYII=", "base64");
const webp = Buffer.concat([Buffer.from("RIFF"), Buffer.alloc(4), Buffer.from("WEBP")]);
const input = (bytes = jpeg, contentType = "image/jpeg") => ({ base64: bytes.toString("base64"), contentType });

beforeEach(async () => {
  assert.equal(process.env.FIRESTORE_EMULATOR_HOST, "127.0.0.1:8285");
  assert.equal(process.env.GCLOUD_PROJECT, "demo-good4-v2");
  for (const collection of ["users", "organizations", "events", "auditLogs"]) {
    await db.recursiveDelete(db.collection(collection));
  }
  await Promise.all([
    db.doc("users/manager-1").set({ role: "communityManager", status: "active" }),
    db.doc("users/staff-1").set({ role: "communityStaff", status: "active" }),
    db.doc("users/student-1").set({ role: "student", status: "active" }),
    db.doc("organizations/community-org").set({ type: "community", status: "active" }),
    db.doc("organizations/community-org/members/manager-1").set({ userId: "manager-1", role: "manager", status: "active" }),
    db.doc("organizations/community-org/members/staff-1").set({ userId: "staff-1", role: "staff", status: "active" }),
  ]);
});

after(async () => {
  if (!storageSkip) {
    await Promise.all(uploadedObjects.map((name) => storageBucket.file(name).delete({ ignoreNotFound: true })));
  }
  await Promise.all([db.terminate(), legacyTestDb.terminate()]);
});

for (const [contentType, extension, bytes] of [
  ["image/jpeg", "jpg", jpeg], ["image/png", "png", png], ["image/webp", "webp", webp],
] as const) {
  test(`active manager uploads ${contentType} with an organization-scoped download token`, { skip: storageSkip }, async () => {
    const result = await uploadCommunityEventImageService(db, "manager-1", {
      ...input(bytes, contentType), organizationId: "other-community", communityId: "other-community",
    });
    const url = new URL(result.imageUrl);
    assert.equal(url.origin, "https://firebasestorage.googleapis.com");
    assert.ok(url.pathname.startsWith(`/v0/b/${storageBucket.name}/o/`));
    const objectName = decodeURIComponent(url.pathname.split("/o/")[1]!);
    uploadedObjects.push(objectName);
    assert.match(objectName, new RegExp(`^community-events/community-org/[0-9a-f-]{36}\\.${extension}$`));
    assert.equal(url.searchParams.get("alt"), "media");
    const [metadata] = await storageBucket.file(objectName).getMetadata();
    assert.equal(metadata.contentType, contentType);
    assert.ok(url.searchParams.get("token"));
    assert.equal(metadata.metadata?.firebaseStorageDownloadTokens, url.searchParams.get("token"));
    assert.deepEqual((await storageBucket.file(objectName).download())[0], bytes);
    const event = await saveEventService(db, "manager-1", "community-org", {
      title: "Uploaded cover", description: "Image upload integration", date: "2030-10-01", time: "18:00",
      location: "Campus", imageUrl: result.imageUrl,
    });
    assert.equal((await db.doc(`events/${event.eventId}`).get()).get("imageUrl"), result.imageUrl);
  });
}

test("a cover of exactly 5 MiB is accepted", { skip: storageSkip }, async () => {
  const bytes = Buffer.alloc(5 * 1024 * 1024);
  jpeg.copy(bytes);
  const result = await uploadCommunityEventImageService(db, "manager-1", input(bytes));
  const objectName = decodeURIComponent(new URL(result.imageUrl).pathname.split("/o/")[1]!);
  uploadedObjects.push(objectName);
  assert.equal(Number((await storageBucket.file(objectName).getMetadata())[0].size), bytes.length);
});

test("students cannot upload community covers", async () => {
  await assert.rejects(uploadCommunityEventImageService(db, "student-1", input()), {
    code: "permission-denied", message: "ROLE_NOT_ALLOWED",
  });
});

test("community staff cannot upload community covers", async () => {
  await assert.rejects(uploadCommunityEventImageService(db, "staff-1", input()), {
    code: "permission-denied", message: "COMMUNITY_MANAGER_REQUIRED",
  });
});

for (const [path, message] of [
  ["users/manager-1", "ACCOUNT_NOT_ACTIVE"],
  ["organizations/community-org/members/manager-1", "COMMUNITY_MEMBERSHIP_NOT_FOUND"],
  ["organizations/community-org", "COMMUNITY_MEMBERSHIP_NOT_FOUND"],
] as const) {
  test(`inactive ${path} cannot upload community covers`, async () => {
    await db.doc(path).update({ status: "inactive" });
    await assert.rejects(uploadCommunityEventImageService(db, "manager-1", input()), { message });
  });
}

test("malformed and noncanonical base64 is rejected", async () => {
  for (const base64 of [undefined, null, 123, "", "!!!!", "abc", "data:image/jpeg;base64,/9j/2Q==", "/9j/2Q==\n", "/9j/2R==", "A==="]) {
    await assert.rejects(uploadCommunityEventImageService(db, "manager-1", { ...input(), base64 }), {
      code: "invalid-argument", message: "COMMUNITY_IMAGE_INVALID",
    });
  }
});

test("unsupported content types are rejected", async () => {
  for (const contentType of [undefined, null, "image/gif", "image/svg+xml", "application/octet-stream", "constructor", "__proto__"]) {
    await assert.rejects(uploadCommunityEventImageService(db, "manager-1", { ...input(), contentType }), {
      code: "invalid-argument", message: "COMMUNITY_IMAGE_TYPE_INVALID",
    });
  }
});

test("the signature must match the declared image type", async () => {
  for (const data of [input(png), input(jpeg, "image/png"), input(jpeg, "image/webp"), input(Buffer.from("not an image")), input(Buffer.from("RIFF"), "image/webp")]) {
    await assert.rejects(uploadCommunityEventImageService(db, "manager-1", data), {
      code: "invalid-argument", message: "COMMUNITY_IMAGE_CONTENT_INVALID",
    });
  }
});

test("images over 5 MiB are rejected before any storage write", async () => {
  const justTooLarge = Buffer.alloc(5 * 1024 * 1024 + 1);
  jpeg.copy(justTooLarge);
  await assert.rejects(uploadCommunityEventImageService(db, "manager-1", input(justTooLarge)), {
    code: "invalid-argument", message: "COMMUNITY_IMAGE_SIZE_INVALID",
  });
  await assert.rejects(uploadCommunityEventImageService(db, "manager-1", input(Buffer.alloc(6 * 1024 * 1024))), {
    code: "invalid-argument", message: "COMMUNITY_IMAGE_INVALID",
  });
});
