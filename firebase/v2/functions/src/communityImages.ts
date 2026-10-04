import { randomUUID } from "node:crypto";
import type { Firestore } from "firebase-admin/firestore";
import { HttpsError } from "firebase-functions/v2/https";
import { getCommunityContextService } from "./communityPortal.js";
import { storageBucket } from "./firebase.js";

export async function uploadCommunityEventImageService(
  database: Firestore, actorUid: string, input: Record<string, unknown>,
): Promise<{ imageUrl: string }> {
  const context = await getCommunityContextService(database, actorUid);
  if (context.membershipRole !== "manager") {
    throw new HttpsError("permission-denied", "COMMUNITY_MANAGER_REQUIRED");
  }

  const contentType = input.contentType;
  const extensions: Record<string, string> = {
    "image/jpeg": "jpg",
    "image/png": "png",
    "image/webp": "webp",
  };
  if (typeof contentType !== "string" || !Object.hasOwn(extensions, contentType)) {
    throw new HttpsError("invalid-argument", "COMMUNITY_IMAGE_TYPE_INVALID");
  }

  const encoded = input.base64;
  const maxBytes = 5 * 1024 * 1024;
  const maxEncodedLength = Math.ceil(maxBytes / 3) * 4;
  if (typeof encoded !== "string" || encoded.length === 0 || encoded.length > maxEncodedLength
      || encoded.length % 4 !== 0 || !/^[A-Za-z0-9+/]+={0,2}$/.test(encoded)) {
    throw new HttpsError("invalid-argument", "COMMUNITY_IMAGE_INVALID");
  }
  const bytes = Buffer.from(encoded, "base64");
  if (bytes.toString("base64") !== encoded) {
    throw new HttpsError("invalid-argument", "COMMUNITY_IMAGE_INVALID");
  }
  if (bytes.length === 0 || bytes.length > maxBytes) {
    throw new HttpsError("invalid-argument", "COMMUNITY_IMAGE_SIZE_INVALID");
  }
  const signaturesMatch = contentType === "image/jpeg"
    ? bytes[0] === 0xff && bytes[1] === 0xd8 && bytes[2] === 0xff
    : contentType === "image/png"
      ? bytes.subarray(0, 8).equals(Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]))
      : bytes.subarray(0, 4).toString("ascii") === "RIFF"
        && bytes.subarray(8, 12).toString("ascii") === "WEBP";
  if (!signaturesMatch) {
    throw new HttpsError("invalid-argument", "COMMUNITY_IMAGE_CONTENT_INVALID");
  }

  const objectName = `community-events/${context.organizationId}/${randomUUID()}.${extensions[contentType]}`;
  const downloadToken = randomUUID();
  await storageBucket.file(objectName).save(bytes, {
    resumable: false,
    metadata: {
      contentType,
      cacheControl: "public,max-age=3600",
      metadata: { firebaseStorageDownloadTokens: downloadToken },
    },
  });
  return {
    imageUrl: `https://firebasestorage.googleapis.com/v0/b/${storageBucket.name}/o/${encodeURIComponent(objectName)}?alt=media&token=${downloadToken}`,
  };
}
