import { getAuth } from "firebase-admin/auth";
import { randomUUID } from "node:crypto";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import { onSchedule } from "firebase-functions/v2/scheduler";
import {
  assignOrganizationMemberService,
  createCampaignService,
  endCampaignService,
  createOrganizationService,
} from "./admin.js";
import {
  issueCampaignCodeService,
  redeemCampaignCodeService,
} from "./campaigns.js";
import { getBusinessContextService } from "./business.js";
import { db, legacyTestDb, storageBucket } from "./firebase.js";
import { redeemLegacyCouponService } from "./legacyCoupons.js";
import {
  getAdminDashboardService,
  getPortalContextService,
  requireGood4Admin,
  moderateContentReportService,
  reviewLegacyCouponService,
  saveDiningMenuService,
  saveHomeBannerService,
  requireBannerSlot,
  saveAcademicCalendarEventService,
} from "./adminPortal.js";
import { requireAuthenticatedUid, requireNonEmptyString } from "./shared.js";
import {
  cancelCommunityPortalEntryService,
  getCommunityPortalDashboardService,
  saveCommunityPortalEntryService,
} from "./communityPortal.js";
import { recordEventAttendanceService, setEventRegistrationService } from "./events.js";
import { getFollowingCommunityIdsService, setCommunityFollowingService } from "./communityFollowing.js";
import { submitFeedbackService } from "./feedback.js";
import { saveKykMenuService } from "./kykMenu.js";
import { ensureStudentProfileService } from "./studentAuth.js";
import { eraseAccountData } from "./accountDeletion.js";
import { refreshCampusWeatherService } from "./weather.js";
import { importSksDiningMenuService } from "./diningMenuImport.js";
import { confirmEduVerificationService, requestEduVerificationService } from "./eduVerification.js";
import { recordLegalAcknowledgementsService } from "./legalAcknowledgements.js";

const callableOptions = {
  region: "europe-west1",
  memory: "256MiB" as const,
  timeoutSeconds: 30,
  maxInstances: 20,
};

export const getFollowingCommunityIds = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return getFollowingCommunityIdsService(db, uid);
});

export const setCommunityFollowing = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return setCommunityFollowingService(db, uid, {
    communityId: request.data?.communityId, following: request.data?.following,
  });
});

export const createOrganization = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return createOrganizationService(db, uid, request.data);
});

export const assignOrganizationMember = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  // Check the caller before touching Firebase Auth so non-admins cannot probe user IDs.
  await requireGood4Admin(db, uid);
  const userId = typeof request.data?.userId === "string" ? request.data.userId : "";
  const authUser = userId ? await getAuth().getUser(userId) : null;
  return assignOrganizationMemberService(db, uid, {
    ...request.data,
    email: authUser?.email ?? "",
    displayName: authUser?.displayName ?? "",
  });
});

export const assignOrganizationMemberByEmail = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  await requireGood4Admin(db, uid);
  const email = requireNonEmptyString(request.data?.email, "email", 320).toLowerCase();
  if (!/^[^\s@/]+@[^\s@/]+\.[^\s@/]+$/.test(email)) {
    throw new HttpsError("invalid-argument", "EMAIL_INVALID");
  }
  const displayName = typeof request.data?.displayName === "string"
    ? request.data.displayName.trim().slice(0, 120)
    : "";
  const temporaryPassword = typeof request.data?.temporaryPassword === "string"
    ? request.data.temporaryPassword
    : "";
  let authUser;
  let created = false;
  try {
    authUser = await getAuth().getUserByEmail(email);
  } catch (error) {
    if ((error as { code?: string }).code !== "auth/user-not-found") throw error;
    if (temporaryPassword.length < 12) {
      throw new HttpsError("invalid-argument", "TEMPORARY_PASSWORD_REQUIRED");
    }
    authUser = await getAuth().createUser({
      email,
      password: temporaryPassword,
      displayName: displayName || undefined,
    });
    created = true;
  }

  try {
    const result = await assignOrganizationMemberService(db, uid, {
      organizationId: request.data?.organizationId,
      userId: authUser.uid,
      email: authUser.email ?? email,
      displayName: displayName || authUser.displayName || "",
      role: request.data?.role,
    });
    return { ...result, created };
  } catch (error) {
    if (created) await getAuth().deleteUser(authUser.uid).catch(() => undefined);
    throw error;
  }
});

export const createCampaign = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return createCampaignService(db, uid, request.data);
});

export const endCampaign = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return endCampaignService(db, uid, request.data ?? {});
});

export const issueCampaignCode = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return issueCampaignCodeService(db, uid, request.data);
});

export const redeemCampaignCode = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return redeemCampaignCodeService(db, uid, request.data);
});

export const redeemLegacyTestCoupon = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return redeemLegacyCouponService(db, legacyTestDb, uid, request.data);
});

export const getBusinessContext = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return getBusinessContextService(db, uid);
});

export const getPortalContext = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return getPortalContextService(db, uid);
});

export const getAdminDashboard = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return getAdminDashboardService(db, legacyTestDb, uid);
});

export const reviewLegacyCoupon = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return reviewLegacyCouponService(db, legacyTestDb, uid, request.data);
});

export const moderateContentReport = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return moderateContentReportService(db, legacyTestDb, uid, request.data ?? {});
});

export const saveDiningMenu = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return saveDiningMenuService(db, legacyTestDb, uid, request.data ?? {});
});

export const saveKykMenu = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return saveKykMenuService(db, uid, request.data ?? {});
});

export const saveHomeBanner = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return saveHomeBannerService(db, legacyTestDb, uid, request.data ?? {});
});

export const saveAcademicCalendarEvent = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return saveAcademicCalendarEventService(db, uid, request.data ?? {});
});

export const uploadHomeBannerImage = onCall({
  ...callableOptions,
  memory: "512MiB",
  timeoutSeconds: 60,
}, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  await requireGood4Admin(db, uid);

  const contentType = request.data?.contentType;
  const extensions: Record<string, string> = {
    "image/jpeg": "jpg",
    "image/png": "png",
    "image/webp": "webp",
  };
  if (typeof contentType !== "string" || !extensions[contentType]) {
    throw new HttpsError("invalid-argument", "BANNER_IMAGE_TYPE_INVALID");
  }

  const encoded = request.data?.base64;
  const maxEncodedLength = Math.ceil((5 * 1024 * 1024) / 3) * 4;
  if (typeof encoded !== "string" || encoded.length === 0 || encoded.length > maxEncodedLength
      || encoded.length % 4 !== 0 || !/^[A-Za-z0-9+/]+={0,2}$/.test(encoded)) {
    throw new HttpsError("invalid-argument", "BANNER_IMAGE_INVALID");
  }
  const bytes = Buffer.from(encoded, "base64");
  if (bytes.length === 0 || bytes.length > 5 * 1024 * 1024) {
    throw new HttpsError("invalid-argument", "BANNER_IMAGE_SIZE_INVALID");
  }
  const signaturesMatch = contentType === "image/jpeg"
    ? bytes[0] === 0xff && bytes[1] === 0xd8 && bytes[2] === 0xff
    : contentType === "image/png"
      ? bytes.subarray(0, 8).equals(Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]))
      : bytes.subarray(0, 4).toString("ascii") === "RIFF"
        && bytes.subarray(8, 12).toString("ascii") === "WEBP";
  if (!signaturesMatch) {
    throw new HttpsError("invalid-argument", "BANNER_IMAGE_CONTENT_INVALID");
  }

  const extension = extensions[contentType];
  // Slot 1 keeps the original object name; slots 2-4 feed the home slider.
  const slot = requireBannerSlot(request.data?.slot);
  const objectName = slot === 1 ? `home-banners/current.${extension}` : `home-banners/slot-${slot}.${extension}`;
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
});

export const getCommunityPortalDashboard = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return getCommunityPortalDashboardService(db, legacyTestDb, uid);
});

export const saveCommunityPortalEntry = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return saveCommunityPortalEntryService(db, legacyTestDb, uid, request.data ?? {});
});

export const cancelCommunityPortalEntry = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return cancelCommunityPortalEntryService(db, legacyTestDb, uid, request.data);
});

export const setEventRegistration = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return setEventRegistrationService(db, uid, request.data);
});

export const recordEventAttendance = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return recordEventAttendanceService(db, uid, request.data);
});

export const submitFeedback = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return submitFeedbackService(db, uid, request.data ?? {});
});

export const ensureStudentProfile = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  const authUser = await getAuth().getUser(uid);
  return ensureStudentProfileService(db, {
    uid,
    email: authUser.email,
    emailVerified: authUser.emailVerified,
    displayName: authUser.displayName,
    providers: authUser.providerData.map((provider) => provider.providerId),
  }, request.data ?? {});
});

export const recordLegalAcknowledgements = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return recordLegalAcknowledgementsService(db, uid, request.data ?? {});
});

export const requestEduVerification = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return requestEduVerificationService(db, uid, request.data ?? {});
});

export const confirmEduVerification = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return confirmEduVerificationService(db, uid, request.data ?? {});
});

export const deleteMyAccount = onCall({
  ...callableOptions,
  memory: "512MiB",
  timeoutSeconds: 540,
}, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  let email: string | null = null;
  try {
    email = (await getAuth().getUser(uid)).email ?? null;
  } catch (error) {
    if ((error as { code?: string }).code !== "auth/user-not-found") throw error;
  }
  await eraseAccountData(db, legacyTestDb, uid, email);

  try {
    await getAuth().deleteUser(uid);
  } catch (error) {
    if ((error as { code?: string }).code !== "auth/user-not-found") throw error;
  }

  return { deleted: true };
});

// One request per half hour for the whole app keeps us well inside MET Norway's
// fair-use terms and means user devices never contact the weather provider.
export const refreshCampusWeather = onSchedule({
  schedule: "every 30 minutes",
  timeZone: "Europe/Istanbul",
  region: "europe-west1",
  memory: "256MiB",
  timeoutSeconds: 60,
  retryCount: 1,
}, async () => {
  await refreshCampusWeatherService(db);
});

// SKS posts next week's lunch menu as an image on Friday afternoons and sometimes corrects it later,
// so the page is checked twice each weekday; an unchanged image is skipped before any OCR runs.
export const importSksDiningMenu = onSchedule({
  schedule: "30 7,19 * * 1-5",
  timeZone: "Europe/Istanbul",
  region: "europe-west1",
  memory: "1GiB",
  timeoutSeconds: 120,
  retryCount: 1,
}, async () => {
  const { recognizeMenuImage } = await import("./diningMenuOcr.js");
  await importSksDiningMenuService(db, legacyTestDb, {
    fetchText: async (url) => {
      const response = await fetch(url, { signal: AbortSignal.timeout(30_000) });
      if (!response.ok) throw new Error(`SKS_PAGE_${response.status}`);
      return response.text();
    },
    fetchImage: async (url) => {
      // The published menu is ~1 MB; refuse anything far larger before it reaches OCR memory.
      const maxBytes = 15 * 1024 * 1024;
      const response = await fetch(url, { signal: AbortSignal.timeout(30_000) });
      if (!response.ok) throw new Error(`SKS_IMAGE_${response.status}`);
      if (Number(response.headers.get("content-length") ?? 0) > maxBytes) throw new Error("SKS_IMAGE_TOO_LARGE");
      const image = Buffer.from(await response.arrayBuffer());
      if (image.length > maxBytes) throw new Error("SKS_IMAGE_TOO_LARGE");
      return image;
    },
    recognize: recognizeMenuImage,
  });
});
