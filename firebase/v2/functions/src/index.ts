import { cleanupTechnicalRecords } from "./maintenance.js";
import { setGlobalOptions } from "firebase-functions/v2";
import { onMessagePublished } from "firebase-functions/v2/pubsub";
import sharp from "sharp";
import { recordBudgetNotification, optionalJobsAllowed, productionJobsEnabled, setCostControlService } from "./costControl.js";
import { assertRateLimit } from "./rateLimit.js";
import { getMessaging } from "firebase-admin/messaging";
import { getFunctions } from "firebase-admin/functions";
import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { onTaskDispatched } from "firebase-functions/v2/tasks";
import { resumeNotificationJobService, registerPushDeviceService as registerNotificationPushDeviceService, unregisterPushDeviceService as unregisterNotificationPushDeviceService, updateNotificationPreferencesService,
  markNotificationsReadService, previewAnnouncementService, sendAnnouncementService,
  notificationHistoryService, processNotificationPage, prepareEventReminders } from "./notifications.js";
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
  getCommunityEventParticipantsService,
  saveCommunityPortalEntryService,
} from "./communityPortal.js";
import { recordEventAttendanceService, setEventRegistrationService } from "./events.js";
import { getFollowingCommunityIdsService, setCommunityFollowingService } from "./communityFollowing.js";
import {
  getMyCommunityApplicationService,
  listCommunityApplicationsService,
  reviewCommunityApplicationService,
  submitCommunityApplicationService,
} from "./communityApplications.js";
import { submitFeedbackService } from "./feedback.js";
import { saveKykMenuService } from "./kykMenu.js";
import { ensureStudentProfileService } from "./studentAuth.js";
import { eraseAccountData } from "./accountDeletion.js";
import { refreshCampusWeatherService } from "./weather.js";
import { importSksDiningMenuService } from "./diningMenuImport.js";
import { confirmEduVerificationService, requestEduVerificationService } from "./eduVerification.js";
import { beginCampusEmailVerificationService, completeCampusEmailVerificationService,
  completeCampusEmailVerificationFromBrowserService, getCampusEmailVerificationStatusService } from "./campusEmailVerification.js";
import { recordLegalAcknowledgementsService } from "./legalAcknowledgements.js";
import {
  acceptMarketTermsService,
  blockMarketUserService,
  cleanupMarketDataService,
  createMarketListingService,
  eraseMarketData,
  listMarketBlockedService,
  listMarketFavoritesService,
  renewMarketListingService,
  setMarketFavoriteService,
  unblockMarketUserService,
  updateMarketListingPriceService,
  getMarketFeedService,
  getMarketSummaryService,
  getMarketListingService,
  getMarketMessagesService,
  getMarketReportConversationService,
  listMarketConversationsService,
  listMyMarketListingsService,
  listMarketModerationQueueService,
  type MarketDeps,
  markMarketConversationReadService,
  reportMarketContentService,
  resolveMarketReportService,
  respondMarketOfferService,
  reviewMarketListingService,
  sendMarketMessageService,
  updateMarketListingStatusService,
} from "./market.js";
import { registerPushDeviceService as registerCampusPushDeviceService, unregisterPushDeviceService as unregisterCampusPushDeviceService, sendPushToUser } from "./push.js";

setGlobalOptions({ minInstances: 0, maxInstances: 5 });

const callableOptions = {
  region: "europe-west1",
  memory: "256MiB" as const,
  timeoutSeconds: 30,
  maxInstances: 5,
  minInstances: 0,
  enforceAppCheck: process.env.ENFORCE_APP_CHECK === "true",
};

async function deleteStoragePrefix(prefix: string): Promise<void> {
  await storageBucket.deleteFiles({ prefix, force: true });
}

const marketDeps: MarketDeps = {
  photos: {
    async save(objectName, bytes) {
      const downloadToken = randomUUID();
      await storageBucket.file(objectName).save(bytes, {
        resumable: false,
        metadata: {
          contentType: "image/jpeg",
          cacheControl: "public,max-age=86400",
          metadata: { firebaseStorageDownloadTokens: downloadToken },
        },
      });
      return `https://firebasestorage.googleapis.com/v0/b/${storageBucket.name}/o/${encodeURIComponent(objectName)}?alt=media&token=${downloadToken}`;
    },
    deletePrefix: deleteStoragePrefix,
  },
  notify: (uid, payload) => sendPushToUser(db, uid, payload),
};

const readOptions = { ...callableOptions, maxInstances: 3 };
async function limitRequest(uid: string, group: string, limit: number) {
  await db.runTransaction((transaction) => assertRateLimit(db, transaction, { key: `${group}:${uid}`, limit, windowSeconds: 60 }));
}
export const billingBudgetNotification = onMessagePublished({ topic: "good4-billing-budget", region: "europe-west1", maxInstances: 1, retry: true }, async (event) => {
  await recordBudgetNotification(db, event.data.message.json as Record<string, unknown>);
});
export const setCostControl = onCall(callableOptions, async (request) => setCostControlService(db, requireAuthenticatedUid(request.auth?.uid), request.data ?? {}));

export const registerPushDevice = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  const input = request.data ?? {};
  return typeof input.installationId === "string"
    ? registerNotificationPushDeviceService(db, uid, input)
    : registerCampusPushDeviceService(db, uid, input);
});

export const unregisterPushDevice = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  const input = request.data ?? {};
  return typeof input.installationId === "string"
    ? unregisterNotificationPushDeviceService(db, uid, input)
    : unregisterCampusPushDeviceService(db, uid, input);
});

export const getFollowingCommunityIds = onCall(readOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return getFollowingCommunityIdsService(db, uid);
});

export const setCommunityFollowing = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  await limitRequest(uid, "community", 30);
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

export const getBusinessContext = onCall(readOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  await limitRequest(uid, "panel", 6);
  return getBusinessContextService(db, uid);
});

export const getPortalContext = onCall(readOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  await limitRequest(uid, "panel", 6);
  return getPortalContextService(db, uid);
});

export const getAdminDashboard = onCall(readOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  await limitRequest(uid, "panel", 6);
  return getAdminDashboardService(db, legacyTestDb, uid, request.data ?? {});
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

  let normalized: Buffer;
  try {
    normalized = await sharp(bytes, { limitInputPixels: 40_000_000 }).rotate().resize(1600, 1600, { fit: "inside", withoutEnlargement: true }).webp({ quality: 80 }).toBuffer();
    if (normalized.length > 512 * 1024) normalized = await sharp(normalized).webp({ quality: 45 }).toBuffer();
  } catch { throw new HttpsError("invalid-argument", "BANNER_IMAGE_CONTENT_INVALID"); }
  if (normalized.length > 512 * 1024) throw new HttpsError("invalid-argument", "BANNER_IMAGE_SIZE_INVALID");
  // Slot 1 keeps the original object name; slots 2-4 feed the home slider.
  const slot = requireBannerSlot(request.data?.slot);
  const objectName = `home-banners/slot-${slot}-${randomUUID()}.webp`;
  const downloadToken = randomUUID();
  await storageBucket.file(objectName).save(normalized, {
    resumable: false,
    metadata: {
      contentType: "image/webp",
      cacheControl: "public,max-age=31536000,immutable",
      metadata: { firebaseStorageDownloadTokens: downloadToken },
    },
  });
  return {
    imageUrl: `https://firebasestorage.googleapis.com/v0/b/${storageBucket.name}/o/${encodeURIComponent(objectName)}?alt=media&token=${downloadToken}`,
  };
});

export const getCommunityPortalDashboard = onCall(readOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  await limitRequest(uid, "panel", 6);
  return getCommunityPortalDashboardService(db, legacyTestDb, uid, request.data ?? {});
});

export const saveCommunityPortalEntry = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return saveCommunityPortalEntryService(db, legacyTestDb, uid, request.data ?? {});
});

export const cancelCommunityPortalEntry = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return cancelCommunityPortalEntryService(db, legacyTestDb, uid, request.data);
});

export const getMyCommunityApplication = onCall(readOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return getMyCommunityApplicationService(db, uid);
});

export const submitCommunityApplication = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  // Read the provider and verified e-mail from Firebase Auth, never from the request.
  const authUser = await getAuth().getUser(uid);
  return submitCommunityApplicationService(db, {
    uid,
    email: authUser.email,
    emailVerified: authUser.emailVerified,
    providers: authUser.providerData.map((provider) => provider.providerId),
  }, request.data ?? {});
});

export const listCommunityApplications = onCall(readOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  await limitRequest(uid, "panel", 6);
  return listCommunityApplicationsService(db, uid);
});

export const reviewCommunityApplication = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return reviewCommunityApplicationService(db, uid, request.data ?? {});
});

export const setEventRegistration = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  await limitRequest(uid, "community", 30);
  return setEventRegistrationService(db, uid, request.data);
});

export const recordEventAttendance = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  await limitRequest(uid, "community", 30);
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

export const beginCampusEmailVerification = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return beginCampusEmailVerificationService(db, uid, request.data ?? {});
});

export const completeCampusEmailVerification = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return completeCampusEmailVerificationService(db, uid, request.data ?? {});
});

// No browser Good4 login is needed; the service validates the one-time Auth
// proof and resolves the original account from its bound verification request.
export const completeCampusEmailVerificationFromBrowser = onCall(callableOptions, async (request) => {
  return completeCampusEmailVerificationFromBrowserService(db, request.data ?? {});
});

export const getCampusEmailVerificationStatus = onCall(callableOptions, async (request) => {
  return getCampusEmailVerificationStatusService(db, requireAuthenticatedUid(request.auth?.uid));
});

export const deleteMyAccount = onCall({
  ...callableOptions,
  memory: "512MiB",
  maxInstances: 1,
  timeoutSeconds: 540,
}, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  let email: string | null = null;
  try {
    email = (await getAuth().getUser(uid)).email ?? null;
  } catch (error) {
    if ((error as { code?: string }).code !== "auth/user-not-found") throw error;
  }
  await eraseMarketData(db, uid, deleteStoragePrefix);
  await eraseAccountData(db, legacyTestDb, uid, email);

  try {
    await getAuth().deleteUser(uid);
  } catch (error) {
    if ((error as { code?: string }).code !== "auth/user-not-found") throw error;
  }

  return { deleted: true };
});

// Kampüs Dolabı: student second-hand market. Every write goes through these callables.
export const acceptMarketTerms = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return acceptMarketTermsService(db, uid, request.data ?? {});
});

export const createMarketListing = onCall({
  ...callableOptions,
  memory: "1GiB",
  timeoutSeconds: 60,
}, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return createMarketListingService(db, uid, request.data ?? {}, marketDeps);
});

export const updateMarketListingStatus = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return updateMarketListingStatusService(db, uid, request.data ?? {}, marketDeps);
});

export const sendMarketMessage = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return sendMarketMessageService(db, uid, request.data ?? {}, marketDeps);
});

export const respondMarketOffer = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return respondMarketOfferService(db, uid, request.data ?? {}, marketDeps);
});

export const markMarketConversationRead = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return markMarketConversationReadService(db, uid, request.data ?? {});
});

export const blockMarketUser = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return blockMarketUserService(db, uid, request.data ?? {});
});

export const unblockMarketUser = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return unblockMarketUserService(db, uid, request.data ?? {});
});

export const listMarketBlocked = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return listMarketBlockedService(db, uid);
});

export const updateMarketListingPrice = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return updateMarketListingPriceService(db, uid, request.data ?? {});
});

export const renewMarketListing = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return renewMarketListingService(db, uid, request.data ?? {});
});

export const setMarketFavorite = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return setMarketFavoriteService(db, uid, request.data ?? {});
});

export const listMarketFavorites = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return listMarketFavoritesService(db, uid);
});

// Kampüs Dolabı lifetime and retention (KVKK): expires listings after 30 days (with a
// reminder 3 days before), then removes old closed listings, sold-listing photos,
// inactive conversations, old reports/violations and finished verification requests.
export const cleanupCampusCloset = onSchedule({
  schedule: "every day 04:30",
  timeZone: "Europe/Istanbul",
  region: "europe-west1",
  memory: "512MiB",
  timeoutSeconds: 540,
  retryCount: 1,
}, async () => {
  const counts = await cleanupMarketDataService(db, deleteStoragePrefix, Date.now(), marketDeps.notify);
  console.log("campusCloset cleanup", counts);
});

export const reportMarketContent = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return reportMarketContentService(db, uid, request.data ?? {}, marketDeps);
});

export const getMarketSummary = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return getMarketSummaryService(db, uid);
});

export const getMarketFeed = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return getMarketFeedService(db, uid, request.data ?? {});
});

export const getMarketListing = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return getMarketListingService(db, uid, request.data ?? {});
});

export const listMyMarketListings = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return listMyMarketListingsService(db, uid);
});

export const listMarketConversations = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return listMarketConversationsService(db, uid);
});

export const getMarketMessages = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return getMarketMessagesService(db, uid, request.data ?? {});
});

export const listMarketModerationQueue = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return listMarketModerationQueueService(db, uid);
});

export const reviewMarketListing = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return reviewMarketListingService(db, uid, request.data ?? {}, marketDeps);
});

export const resolveMarketReport = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return resolveMarketReportService(db, uid, request.data ?? {}, marketDeps);
});

export const getMarketReportConversation = onCall(callableOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  return getMarketReportConversationService(db, uid, request.data ?? {});
});

// One request per half hour for the whole app keeps us well inside MET Norway's
// fair-use terms and means user devices never contact the weather provider.
export const refreshCampusWeather = onSchedule({
  schedule: "every 30 minutes",
  maxInstances: 1,
  timeZone: "Europe/Istanbul",
  region: "europe-west1",
  memory: "256MiB",
  timeoutSeconds: 60,
  retryCount: 1,
}, async () => {
  if (!productionJobsEnabled()) return;
  await refreshCampusWeatherService(db);
});

// SKS posts next week's lunch menu as an image on Friday afternoons and sometimes corrects it later,
// so the page is checked twice each weekday; an unchanged image is skipped before any OCR runs.
export const importSksDiningMenu = onSchedule({
  schedule: "30 7,19 * * 1-5",
  maxInstances: 1,
  timeZone: "Europe/Istanbul",
  region: "europe-west1",
  memory: "1GiB",
  timeoutSeconds: 120,
  retryCount: 1,
}, async () => {
  if (!productionJobsEnabled()) return;
  if (!(await optionalJobsAllowed(db))) return;
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

// Push transport is intentionally disabled until NOTIFICATIONS_ENABLED=true is deployed.
export const updateNotificationPreferences = onCall(callableOptions, async (request) =>
  updateNotificationPreferencesService(db, requireAuthenticatedUid(request.auth?.uid), request.data ?? {}));
export const markNotificationsRead = onCall(callableOptions, async (request) =>
  markNotificationsReadService(db, requireAuthenticatedUid(request.auth?.uid), request.data ?? {}));
export const previewAnnouncement = onCall(readOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid); await limitRequest(uid, "panel", 6);
  return previewAnnouncementService(db, uid, request.data ?? {});
});
export const sendAnnouncement = onCall(callableOptions, async (request) =>
  sendAnnouncementService(db, requireAuthenticatedUid(request.auth?.uid), request.data ?? {}));
export const getNotificationHistory = onCall(readOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid); await limitRequest(uid, "panel", 6);
  return notificationHistoryService(db, uid);
});

export const resumeNotificationJob = onCall(callableOptions, async (request) => resumeNotificationJobService(db, requireAuthenticatedUid(request.auth?.uid), request.data ?? {}));
export const queueNotification = onDocumentCreated({
  document: "notificationDispatches/{dispatchId}", region: "europe-west1", maxInstances: 1, retry: true,
}, async (event) => {
  if (!productionJobsEnabled() || !event.data) return;
  const jobId = event.data.get("jobId");
  if (typeof jobId !== "string") return;
  await getFunctions().taskQueue("locations/europe-west1/functions/deliverNotification").enqueue({ jobId }, { dispatchDeadlineSeconds: 540 });
});
export const deliverNotification = onTaskDispatched({
  region: "europe-west1", maxInstances: 1, timeoutSeconds: 540, memory: "256MiB",
  retryConfig: { maxAttempts: 5, maxRetrySeconds: 86400, minBackoffSeconds: 60 },
  rateLimits: { maxConcurrentDispatches: 1, maxDispatchesPerSecond: 1 },
}, async (request) => {
  if (!productionJobsEnabled()) return;
  await processNotificationPage(db, request.data.jobId, async (token, payload) => {
    await getMessaging().send({ token,
      notification: { title: payload.title, body: payload.body }, data: payload.data,
      android: { notification: { channelId: payload.kind === "announcement" ? "announcements" : "events", tag: payload.data.notificationId }, ttl: payload.kind === "eventReminder" ? 3600000 : 86400000 },
      apns: { headers: { "apns-collapse-id": payload.data.notificationId!.slice(0, 64), "apns-expiration": String(Math.floor(Date.now() / 1000) + (payload.kind === "eventReminder" ? 3600 : 86400)) }, payload: { aps: { sound: "default" } } },
    });
  });
});
export const scheduleEventReminders = onSchedule({
  schedule: "every 5 minutes", maxInstances: 1, region: "europe-west1", timeZone: "Europe/Istanbul", timeoutSeconds: 540,
}, async () => { if (productionJobsEnabled() && await optionalJobsAllowed(db)) await prepareEventReminders(db); });

export const getCommunityEventParticipants = onCall(readOptions, async (request) => {
  const uid = requireAuthenticatedUid(request.auth?.uid);
  await limitRequest(uid, "panel", 6);
  return getCommunityEventParticipantsService(db, uid, request.data ?? {});
});

export const cleanupCostArtifacts = onSchedule({ schedule: "0 4 * * *", region: "europe-west1", timeZone: "Europe/Istanbul", maxInstances: 1, timeoutSeconds: 300 }, async () => {
  if (productionJobsEnabled()) await cleanupTechnicalRecords(db, storageBucket);
});
