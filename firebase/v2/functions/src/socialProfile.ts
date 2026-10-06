import { type DocumentSnapshot, type Firestore } from "firebase-admin/firestore";
import { HttpsError } from "firebase-functions/v2/https";
import { publicName } from "./market.js";

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
  const photoUrl = state?.get("photoUrl");
  const photoThumbUrl = state?.get("photoThumbUrl");
  return {
    nameMode,
    name: nameMode === "shown" ? openName(displayName) : publicName(displayName),
    photoUrl: typeof photoUrl === "string" && photoUrl ? photoUrl : null,
    photoThumbUrl: typeof photoThumbUrl === "string" && photoThumbUrl ? photoThumbUrl : null,
  };
}

/** One batched read for every student a screen shows. */
export async function loadSocialProfiles(database: Firestore, uids: readonly string[]): Promise<Map<string, SocialProfile>> {
  const unique = [...new Set(uids.filter(Boolean))];
  const profiles = new Map<string, SocialProfile>();
  if (unique.length === 0) return profiles;
  const snapshots = await database.getAll(
    ...unique.flatMap((uid) => [database.doc(`users/${uid}`), database.doc(`socialUserState/${uid}`)]),
  );
  unique.forEach((uid, index) => {
    const user = snapshots[index * 2];
    const state = snapshots[index * 2 + 1];
    profiles.set(uid, profileFrom(user?.exists ? user : undefined, state?.exists ? state : undefined));
  });
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

/** Local previews must fetch emulator files rather than the production Storage host. */
export function socialPhotoDownloadUrl(url: string, emulatorHost?: string): string {
  if (!emulatorHost || !/^(127\.0\.0\.1|localhost):\d+$/.test(emulatorHost)) return url;
  return url.replace("https://firebasestorage.googleapis.com", `http://${emulatorHost}`);
}
