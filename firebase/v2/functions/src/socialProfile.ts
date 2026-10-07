import type { Bucket } from "@google-cloud/storage";
import { type DocumentSnapshot, type Firestore } from "firebase-admin/firestore";
import { HttpsError } from "firebase-functions/v2/https";
import { eduDomainOf, publicName } from "./market.js";
import { hasVerifiedCampusEmail } from "./campusEmailVerification.js";

// How a student appears in Sosyal Etkinlikler: a name (masked by default) and an
// optional photo. Names are resolved when read, so a later change of the setting
// also applies to activities and chats that already exist.

export const SOCIAL_NAME_MODES = ["masked", "shown"] as const;
export type SocialNameMode = (typeof SOCIAL_NAME_MODES)[number];

export const SOCIAL_PROFILE_LIMITS = {
  maxPhotoBytes: 4 * 1024 * 1024,
  maxPhotoChangesPerDay: 5,
  photoSize: 512,
  thumbSize: 128,
} as const;

/** Private to the student: where the profile photo files live, so a replaced photo can be removed. */
export function profilePhotoFolder(uid: string, id: string): string {
  return `social-profiles/${uid}/${id}/`;
}

/** "Ayşe Yılmaz" → "Ayşe Y." (first name and the initial of the last name). */
export function openName(displayName: unknown): string {
  const parts = String(displayName ?? "").trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return publicName(displayName);
  if (parts.length === 1) return parts[0]!;
  return `${parts[0]!} ${Array.from(parts.at(-1)!)[0]!.toLocaleUpperCase("tr-TR")}.`;
}

export interface SocialProfile {
  nameMode: SocialNameMode;
  /** The name other students see, following [nameMode]. */
  name: string;
  photoUrl: string | null;
  photoThumbUrl: string | null;
}

export function profileFrom(user: DocumentSnapshot | undefined, state: DocumentSnapshot | undefined): SocialProfile {
  const nameMode: SocialNameMode = state?.get("nameMode") === "shown" ? "shown" : "masked";
  const displayName = user?.get("displayName");

  return {
    nameMode,
    name: nameMode === "shown" ? openName(displayName) : publicName(displayName),
    photoUrl: null,
    photoThumbUrl: null,
  };
}

/** Objects are private; image bytes are delivered by authenticated callables only. */
export interface SocialPhotoStore {
  save(objectName: string, bytes: Buffer): Promise<string>;
  read(objectName: string): Promise<Buffer>;
  deletePrefix(prefix: string): Promise<void>;
}

export function validProfilePhotoFolder(uid: string, value: unknown): value is string {
  return typeof value === "string" && value.startsWith(`social-profiles/${uid}/`)
    && /^[A-Za-z0-9_-]{1,80}\/$/.test(value.slice(`social-profiles/${uid}/`.length));
}

function verifiedStudent(user: DocumentSnapshot | undefined): boolean {
  return user?.exists === true && user.get("status") === "active" && user.get("role") === "student"
    && hasVerifiedCampusEmail(user);
}

/** Never return state-supplied URLs. Evaluate both current accounts, not historical activity data. */
export async function loadSocialProfiles(database: Firestore, uids: readonly string[], viewerUid: string,
  photos?: Pick<SocialPhotoStore, "read">, admin = false): Promise<Map<string, SocialProfile>> {
  const unique = [...new Set(uids.filter(Boolean))];
  const profiles = new Map<string, SocialProfile>();
  if (unique.length === 0) return profiles;
  const [viewer, config, ...snapshots] = await database.getAll(
    database.doc(`users/${viewerUid}`), database.doc("app_config/social_activities"),
    ...unique.flatMap((uid) => [database.doc(`users/${uid}`), database.doc(`socialUserState/${uid}`)]));
  const moderator = admin && viewer?.get("role") === "good4Admin" && viewer.get("status") === "active";
  await Promise.all(unique.map(async (uid, index) => {
    const user = snapshots[index * 2]; const state = snapshots[index * 2 + 1];
    const profile = profileFrom(user, state);
    const eligible = moderator || (config?.get("enabled") === true && verifiedStudent(viewer) && verifiedStudent(user)
      && eduDomainOf(String(viewer!.get("eduEmail"))) === eduDomainOf(String(user!.get("eduEmail"))));
    const folder = state?.get("photoFolder");
    if (eligible && photos && validProfilePhotoFolder(uid, folder)) {
      try {
        const bytes = await photos.read(`${folder}thumb.jpg`);
        if (bytes.length <= 64 * 1024 && bytes.subarray(0, 3).equals(JPEG)) {
          profile.photoUrl = profile.photoThumbUrl = `data:image/jpeg;base64,${bytes.toString("base64")}`;
        }
      } catch (error) {
        if ((error as { code?: unknown }).code !== 404) throw error;
      }
    }
    profiles.set(uid, profile);
  }));
  return profiles;
}

const JPEG = Buffer.from([0xff, 0xd8, 0xff]);
const PNG = Buffer.from([0x89, 0x50, 0x4e, 0x47]);

/** The app sends one base64 image; anything that is not JPEG, PNG or WebP is refused. */
export function decodeProfilePhoto(value: unknown): Buffer {
  const maxEncoded = Math.ceil(SOCIAL_PROFILE_LIMITS.maxPhotoBytes / 3) * 4;
  if (typeof value !== "string" || value.length === 0 || value.length > maxEncoded
    || value.length % 4 !== 0 || !/^[A-Za-z0-9+/]+={0,2}$/.test(value)) {
    throw new HttpsError("invalid-argument", "SOCIAL_PHOTO_INVALID");
  }
  const bytes = Buffer.from(value, "base64");
  const isWebp = bytes.subarray(0, 4).toString("ascii") === "RIFF" && bytes.subarray(8, 12).toString("ascii") === "WEBP";
  if (!bytes.subarray(0, 3).equals(JPEG) && !bytes.subarray(0, 4).equals(PNG) && !isWebp) {
    throw new HttpsError("invalid-argument", "SOCIAL_PHOTO_CONTENT_INVALID");
  }
  return bytes;
}

/** Re-encodes the photo: camera rotation applied, EXIF (including GPS) dropped, square crop, plus a small thumbnail. */
export async function processProfilePhoto(bytes: Buffer): Promise<{ full: Buffer; thumb: Buffer }> {
  const { default: sharp } = await import("sharp");
  try {
    const base = sharp(bytes, { failOn: "error", limitInputPixels: 40_000_000 }).rotate();
    const [full, thumb] = await Promise.all([
      base.clone().resize(SOCIAL_PROFILE_LIMITS.photoSize, SOCIAL_PROFILE_LIMITS.photoSize, { fit: "cover" })
        .jpeg({ quality: 80, mozjpeg: true }).toBuffer(),
      base.clone().resize(SOCIAL_PROFILE_LIMITS.thumbSize, SOCIAL_PROFILE_LIMITS.thumbSize, { fit: "cover" })
        .jpeg({ quality: 72, mozjpeg: true }).toBuffer(),
    ]);
    return { full, thumb };
  } catch {
    throw new HttpsError("invalid-argument", "SOCIAL_PHOTO_CONTENT_INVALID");
  }
}

/** Separate from market images: no transferable download token or public cache. */
export function privateSocialPhotoStore(bucket: Bucket): SocialPhotoStore {
  return {
    async save(objectName, bytes) {
      await bucket.file(objectName).save(bytes, { resumable: false,
        metadata: { contentType: "image/jpeg", cacheControl: "private,no-store,max-age=0" } });
      return `gs://${bucket.name}/${objectName}`;
    },
    async read(objectName) { const [bytes] = await bucket.file(objectName).download(); return bytes; },
    async deletePrefix(prefix) { await bucket.deleteFiles({ prefix, force: true }); },
  };
}

/** Revoke tokens on every version under the social prefix, including orphaned files. */
export async function privatizeLegacySocialPhotos(bucket: Bucket, apply = false): Promise<{ objects: number; changed: number }> {
  let pageToken: string | undefined; let objects = 0; let changed = 0;
  do {
    const [files, next] = await bucket.getFiles({ prefix: "social-profiles/", versions: true,
      autoPaginate: false, maxResults: 100, pageToken });
    for (const file of files) {
      if (!file.name.startsWith("social-profiles/")) throw new Error("SOCIAL_PHOTO_MIGRATION_PREFIX_INVALID");
      const [metadata] = await file.getMetadata(); objects++;
      if (metadata.metadata?.firebaseStorageDownloadTokens || metadata.cacheControl !== "private,no-store,max-age=0") {
        if (apply) {
          await file.setMetadata({ cacheControl: "private,no-store,max-age=0",
            metadata: { firebaseStorageDownloadTokens: null } });
          // Storage emulator 15.x retains its separate token list on a metadata-null update.
          // Re-upload identical bytes privately in the demo bucket only; production revokes metadata.
          if (process.env.FIREBASE_STORAGE_EMULATOR_HOST && bucket.name === "demo-good4-v2.appspot.com") {
            const [updated] = await file.getMetadata();
            if (updated.metadata?.firebaseStorageDownloadTokens) {
              const [bytes] = await file.download();
              const custom = { ...metadata.metadata }; delete custom.firebaseStorageDownloadTokens;
              await file.save(bytes, { resumable: false, metadata: { contentType: metadata.contentType,
                cacheControl: "private,no-store,max-age=0", metadata: custom } });
            }
          }
        }
        changed++;
      }
    }
    pageToken = next?.pageToken;
  } while (pageToken);
  return { objects, changed };
}
