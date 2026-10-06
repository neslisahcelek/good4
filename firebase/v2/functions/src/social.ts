import {
  type DocumentReference,
  type DocumentSnapshot,
  FieldPath,
  FieldValue,
  type Firestore,
  type Query,
  Timestamp,
  type Transaction,
} from "firebase-admin/firestore";
import { HttpsError } from "firebase-functions/v2/https";
import { hasVerifiedCampusEmail } from "./campusEmailVerification.js";
import {
  eduDomainOf,
  isBlockedBetween,
  iso,
  MARKET_LIMITS,
  marketDayKey,
  publicName,
  rejectBlockedContent,
  requireId,
  requireOneOf,
  requireText,
  UNIVERSITY_NAMES,
} from "./market.js";
import { checkMarketText, containsPhoneNumber } from "./marketModeration.js";
import type { MarketPhotoStore } from "./market.js";
import type { PushPayload } from "./push.js";
import { requireActiveActor } from "./shared.js";
import {
  decodeProfilePhoto,
  loadSocialProfiles,
  openName,
  processProfilePhoto,
  profileFrom,
  profilePhotoFolder,
  SOCIAL_NAME_MODES,
  SOCIAL_PROFILE_LIMITS,
  type SocialNameMode,
  type SocialProfile,
} from "./socialProfile.js";
import { randomUUID } from "node:crypto";

// Sosyal etkinlikler: students open a social or sport activity, others ask to
// join, and the organizer's approval opens a one-to-one chat, where the two
// agree on the meeting place (activities carry no place of their own). Like Kampüs
// Dolabı, every read and write goes through these services. Blocks and
// suspensions are shared with Kampüs Dolabı through marketUserState.

const DAY_MS = 24 * 60 * 60 * 1000;

/** All social activity limits live here so they can be tuned in one place. */
export const SOCIAL_LIMITS = {
  maxActivitiesPerDay: 3,
  maxActiveActivities: 5,
  maxRequestsPerDay: 20,
  maxPendingRequestsPerActivity: 50,
  maxMessagesPerDay: 200,
  maxMessageLength: 500,
  maxCapacity: 10,
  /** An activity starts at least this far in the future … */
  minLeadMs: 15 * 60 * 1000,
  /** … and at most this far. */
  maxLeadMs: 30 * DAY_MS,
} as const;

/**
 * How long activities and chats are kept (also stated in the KVKK notice),
 * counted from the activity's start. The daily cleanup job enforces these.
 */
export const SOCIAL_RETENTION_DAYS = {
  /** Chats stay writable this long after the activity starts, then read-only. */
  chatWritable: 7,
  /** Activities, their requests and chats are deleted after this. */
  activity: 30,
  /** Reports, counted from the moment they were resolved. */
  resolvedReport: 365,
} as const;

export const SOCIAL_TERMS_VERSION = 1;
export const SOCIAL_KINDS = ["social", "sport"] as const;
export const SOCIAL_ACTIVITY_TYPES = {
  sport: [
    "football", "basketball", "volleyball", "tennis", "table-tennis", "badminton", "running", "hiking",
    "cycling", "swimming", "sup-kayak", "skating", "fitness", "yoga-pilates", "other-sport",
  ],
  social: [
    "board-games", "coffee", "food", "meetup", "movie", "concert", "video-games", "music",
    "trip", "language-exchange", "study", "other-social",
  ],
} as const;
/** Optional game for "board-games" activities. */
export const SOCIAL_BOARD_GAMES = ["okey", "backgammon", "chess", "uno", "taboo", "cards", "other"] as const;
export const SOCIAL_VIDEO_GAMES = ["fifa", "pes"] as const;
export const SOCIAL_LEVELS = ["any", "beginner", "intermediate", "advanced"] as const;
export const SOCIAL_REPORT_REASONS = ["harassment", "inappropriate", "spam", "safety", "other"] as const;

const SOCIAL_ROLES = ["student", "good4Admin"] as const;
/** Activities that can still be joined or are fully booked. */
const LIVE_STATUSES = ["open", "full"];
/** Requests that hold a place in the organizer's list. */
const ACTIVE_REQUEST_STATUSES = ["pending", "accepted"];
const FEED_PAGE_SIZE = 30;
const POLL_PAGE_SIZE = 50;
const CLEANUP_BATCH = 200;
const CLEANUP_MAX_PAGES = 10;

export interface SocialDeps {
  notify(uid: string, payload: PushPayload): Promise<unknown>;
  now?: () => number;
}

/** Profile photos go to Storage, like Kampüs Dolabı photos. */
export type SocialProfileDeps = Pick<SocialDeps, "now"> & { photos: MarketPhotoStore };

function nowOf(deps: Pick<SocialDeps, "now">): number {
  return deps.now?.() ?? Date.now();
}

function optionalText(value: unknown, field: string, max: number): string {
  if (value === undefined || value === null || value === "") return "";
  return requireText(value, field, 1, max);
}

function millisOf(value: unknown): number {
  return value instanceof Timestamp ? value.toMillis() : 0;
}

/** Started activities stay "open" until the cleanup job runs; readers see them as ended. */
function effectiveStatus(activity: DocumentSnapshot, now: number): string {
  const status = String(activity.get("status") ?? "");
  return LIVE_STATUSES.includes(status) && millisOf(activity.get("startsAt")) <= now ? "ended" : status;
}

/**
 * A requester never learns that they were declined, removed or that the
 * activity filled up: all of these read as "closed" ("Yer kalmadı").
 */
function requestStatusForRequester(status: unknown): string {
  switch (status) {
    case "pending":
    case "accepted":
      return status;
    case "withdrawn":
    case "left":
      return "withdrawn";
    default:
      return "closed";
  }
}

function conversationIdOf(activityId: string, participantUid: string): string {
  return `${activityId}_${participantUid}`;
}

function parseConversationId(value: unknown): { conversationId: string; activityId: string; participantUid: string } {
  const match = typeof value === "string" ? /^([A-Za-z0-9]{1,64})_([A-Za-z0-9]{1,128})$/.exec(value) : null;
  if (!match) throw new HttpsError("invalid-argument", "CONVERSATION_ID_INVALID");
  return { conversationId: match[0], activityId: match[1]!, participantUid: match[2]! };
}

function chatWritableUntil(activityStartsAt: unknown): number {
  return millisOf(activityStartsAt) + SOCIAL_RETENTION_DAYS.chatWritable * DAY_MS;
}

// ---------------------------------------------------------------------------
// Membership, counters and terms
// ---------------------------------------------------------------------------

interface SocialMember {
  user: DocumentSnapshot;
  state: DocumentSnapshot;
  moderation: DocumentSnapshot;
  eduDomain: string;
}

/**
 * Every social action needs an active account, a verified school address,
 * the current rules accepted, the feature switched on and no running
 * suspension (shared with Kampüs Dolabı).
 */
async function requireSocialMember(
  database: Firestore,
  transaction: Transaction,
  uid: string,
  now: number,
): Promise<SocialMember> {
  await requireActiveActor(database, transaction, uid, SOCIAL_ROLES);
  const [user, state, moderation, config] = await Promise.all([
    transaction.get(database.doc(`users/${uid}`)),
    transaction.get(database.doc(`socialUserState/${uid}`)),
    transaction.get(database.doc(`marketUserState/${uid}`)),
    transaction.get(database.doc("app_config/social_activities")),
  ]);
  // Fails closed: the feature stays off until the switch is set explicitly.
  if (config.get("enabled") !== true) throw new HttpsError("unavailable", "SOCIAL_DISABLED");
  if (!hasVerifiedCampusEmail(user)) throw new HttpsError("permission-denied", "SOCIAL_EDU_REQUIRED");
  if (Number(state.get("termsVersion") ?? 0) < SOCIAL_TERMS_VERSION) {
    throw new HttpsError("failed-precondition", "SOCIAL_TERMS_REQUIRED");
  }
  const suspendedUntil = moderation.get("suspendedUntil");
  if (suspendedUntil instanceof Timestamp && suspendedUntil.toMillis() > now) {
    throw new HttpsError("permission-denied", "SOCIAL_SUSPENDED");
  }
  return { user, state, moderation, eduDomain: eduDomainOf(String(user.get("eduEmail"))) };
}

async function requireSocialReader(database: Firestore, uid: string): Promise<void> {
  await database.runTransaction((transaction) => requireActiveActor(database, transaction, uid, SOCIAL_ROLES));
}

function dailyCount(state: DocumentSnapshot, field: string, now: number): number {
  return state.get("dayKey") === marketDayKey(now) ? Number(state.get(field) ?? 0) : 0;
}

/** Counter update that starts a fresh day when the stored day has passed. */
function dailyIncrement(state: DocumentSnapshot, field: string, now: number) {
  const today = marketDayKey(now);
  if (state.get("dayKey") === today) return { [field]: FieldValue.increment(1) };
  return { dayKey: today, activitiesToday: 0, requestsToday: 0, messagesToday: 0, photoChangesToday: 0, [field]: 1 };
}

export async function acceptSocialTermsService(
  database: Firestore,
  uid: string,
  input: { version?: unknown; nameMode?: unknown },
  deps: Pick<SocialDeps, "now"> = {},
): Promise<{ termsVersion: number }> {
  if (input.version !== SOCIAL_TERMS_VERSION) {
    throw new HttpsError("failed-precondition", "SOCIAL_TERMS_VERSION_OUTDATED");
  }
  // Names stay masked unless the student chose to show theirs while accepting the rules.
  const nameMode = input.nameMode === undefined || input.nameMode === null
    ? null
    : requireOneOf(input.nameMode, SOCIAL_NAME_MODES, "SOCIAL_NAME_MODE_INVALID");
  await database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, uid, SOCIAL_ROLES);
    transaction.set(database.doc(`socialUserState/${uid}`), {
      termsVersion: SOCIAL_TERMS_VERSION,
      termsAcceptedAt: Timestamp.fromMillis(nowOf(deps)),
      ...(nameMode ? { nameMode } : {}),
    }, { merge: true });
  });
  return { termsVersion: SOCIAL_TERMS_VERSION };
}

// ---------------------------------------------------------------------------
// Profile: name mode and photo
// ---------------------------------------------------------------------------

/**
 * Changes how the student appears: the name mode and/or the profile photo.
 * A new photo is re-encoded (EXIF dropped) before it is stored; the old files
 * are removed afterwards, and account deletion removes the whole folder.
 */
export async function setSocialProfileService(
  database: Firestore,
  uid: string,
  input: { nameMode?: unknown; photo?: unknown; removePhoto?: unknown },
  deps: SocialProfileDeps,
): Promise<{ profile: SocialProfile }> {
  const now = nowOf(deps);
  const nameMode: SocialNameMode | null = input.nameMode === undefined || input.nameMode === null
    ? null
    : requireOneOf(input.nameMode, SOCIAL_NAME_MODES, "SOCIAL_NAME_MODE_INVALID");
  const hasPhoto = input.photo !== undefined && input.photo !== null;
  const removePhoto = input.removePhoto === true;
  if (hasPhoto && removePhoto) throw new HttpsError("invalid-argument", "SOCIAL_PHOTO_INVALID");
  if (nameMode === null && !hasPhoto && !removePhoto) throw new HttpsError("invalid-argument", "SOCIAL_PROFILE_EMPTY");
  const raw = hasPhoto ? decodeProfilePhoto(input.photo) : null;

  const stateRef = database.doc(`socialUserState/${uid}`);
  const assertCanChange = async (transaction: Transaction) => {
    const member = await requireSocialMember(database, transaction, uid, now);
    if (raw && dailyCount(member.state, "photoChangesToday", now) >= SOCIAL_PROFILE_LIMITS.maxPhotoChangesPerDay) {
      throw new HttpsError("resource-exhausted", "SOCIAL_DAILY_PHOTO_LIMIT");
    }
    return member;
  };
  // Cheap checks first, so a refused request never processes or stores an image.
  await database.runTransaction(assertCanChange);

  let folder: string | null = null;
  let urls: { url: string; thumbUrl: string } | null = null;
  if (raw) {
    const processed = await processProfilePhoto(raw);
    folder = profilePhotoFolder(uid, randomUUID().replace(/-/g, "").slice(0, 16));
    try {
      const [full, thumb] = await Promise.all([
        deps.photos.save(`${folder}photo.jpg`, processed.full),
        deps.photos.save(`${folder}thumb.jpg`, processed.thumb),
      ]);
      urls = { url: full, thumbUrl: thumb };
    } catch (error) {
      await deps.photos.deletePrefix(folder).catch(() => undefined);
      throw error;
    }
  }

  let previousFolder: string | null = null;
  try {
    previousFolder = await database.runTransaction(async (transaction) => {
      const member = await assertCanChange(transaction);
      const old = member.state.get("photoFolder");
      transaction.set(stateRef, {
        ...(nameMode ? { nameMode } : {}),
        ...(urls && folder ? {
          photoUrl: urls.url, photoThumbUrl: urls.thumbUrl, photoFolder: folder,
          ...dailyIncrement(member.state, "photoChangesToday", now),
        } : {}),
        ...(removePhoto ? {
          photoUrl: FieldValue.delete(), photoThumbUrl: FieldValue.delete(), photoFolder: FieldValue.delete(),
        } : {}),
      }, { merge: true });
      return typeof old === "string" && old ? old : null;
    });
  } catch (error) {
    if (folder) await deps.photos.deletePrefix(folder).catch(() => undefined);
    throw error;
  }
  if ((hasPhoto || removePhoto) && previousFolder && previousFolder !== folder) {
    await deps.photos.deletePrefix(previousFolder).catch((error) => console.error("social: old profile photo cleanup failed", error));
  }
  const [user, state] = await Promise.all([database.doc(`users/${uid}`).get(), stateRef.get()]);
  return { profile: profileFrom(user, state) };
}

// ---------------------------------------------------------------------------
// Activities
// ---------------------------------------------------------------------------

interface ActivityInput {
  kind: (typeof SOCIAL_KINDS)[number];
  type: string;
  title: string;
  note: string;
  startsAt: number;
  level: (typeof SOCIAL_LEVELS)[number] | null;
  game: (typeof SOCIAL_BOARD_GAMES)[number] | (typeof SOCIAL_VIDEO_GAMES)[number] | null;
  capacity: number;
}

function parseActivityInput(input: Record<string, unknown>, now: number): ActivityInput {
  const kind = requireOneOf(input.kind, SOCIAL_KINDS, "SOCIAL_KIND_INVALID");
  const type = requireOneOf(input.type, SOCIAL_ACTIVITY_TYPES[kind] as readonly string[], "SOCIAL_TYPE_INVALID");
  const startsAt = typeof input.startsAt === "string" ? Date.parse(input.startsAt) : Number.NaN;
  if (Number.isNaN(startsAt) || startsAt < now + SOCIAL_LIMITS.minLeadMs || startsAt > now + SOCIAL_LIMITS.maxLeadMs) {
    throw new HttpsError("invalid-argument", "SOCIAL_STARTS_AT_INVALID");
  }
  const capacity = input.capacity;
  if (typeof capacity !== "number" || !Number.isInteger(capacity) || capacity < 1 || capacity > SOCIAL_LIMITS.maxCapacity) {
    throw new HttpsError("invalid-argument", "SOCIAL_CAPACITY_INVALID");
  }
  let level: ActivityInput["level"] = null;
  if (kind === "sport") {
    level = input.level === undefined || input.level === null ? "any" : requireOneOf(input.level, SOCIAL_LEVELS, "SOCIAL_LEVEL_INVALID");
  }
  return {
    kind,
    type,
    title: requireText(input.title, "SOCIAL_TITLE", 3, 60),
    note: optionalText(input.note, "SOCIAL_NOTE", 300),
    startsAt,
    level,
    // Game choices are optional and validated against the chosen activity type.
    game: input.game === undefined || input.game === null ? null
      : type === "board-games" ? requireOneOf(input.game, SOCIAL_BOARD_GAMES, "SOCIAL_GAME_INVALID")
      : type === "video-games" ? requireOneOf(input.game, SOCIAL_VIDEO_GAMES, "SOCIAL_GAME_INVALID") : null,
    capacity,
  };
}

async function assertCanCreateActivity(
  database: Firestore,
  transaction: Transaction,
  uid: string,
  now: number,
): Promise<SocialMember> {
  const member = await requireSocialMember(database, transaction, uid, now);
  if (dailyCount(member.state, "activitiesToday", now) >= SOCIAL_LIMITS.maxActivitiesPerDay) {
    throw new HttpsError("resource-exhausted", "SOCIAL_DAILY_ACTIVITY_LIMIT");
  }
  // Started activities stay live until the cleanup job; only upcoming ones count.
  const live = await transaction.get(database.collection("socialActivities")
    .where("organizerUid", "==", uid).where("status", "in", LIVE_STATUSES).limit(50));
  const upcoming = live.docs.filter((activity) => millisOf(activity.get("startsAt")) > now).length;
  if (upcoming >= SOCIAL_LIMITS.maxActiveActivities) {
    throw new HttpsError("resource-exhausted", "SOCIAL_ACTIVE_ACTIVITY_LIMIT");
  }
  return member;
}

export async function createSocialActivityService(
  database: Firestore,
  uid: string,
  input: Record<string, unknown>,
  deps: Pick<SocialDeps, "now"> = {},
): Promise<{ activityId: string; status: "open" }> {
  const now = nowOf(deps);
  const activity = parseActivityInput(input, now);

  // Cheap checks first; membership before content so unverified users never collect strikes.
  await database.runTransaction((transaction) => assertCanCreateActivity(database, transaction, uid, now));
  const check = checkMarketText(activity.title, activity.note);
  if (check.blockedTerm) {
    await rejectBlockedContent(database, uid, check.blockedTerm, "socialActivity",
      [activity.title, activity.note].filter(Boolean).join(" — "), now);
  }
  if (containsPhoneNumber(`${activity.title} ${activity.note}`)) {
    throw new HttpsError("invalid-argument", "SOCIAL_PHONE_IN_ACTIVITY");
  }

  const activityRef = database.collection("socialActivities").doc();
  await database.runTransaction(async (transaction) => {
    const member = await assertCanCreateActivity(database, transaction, uid, now);
    transaction.create(activityRef, {
      organizerUid: uid,
      organizerName: publicName(member.user.get("displayName")),
      eduDomain: member.eduDomain,
      universityName: UNIVERSITY_NAMES[member.eduDomain] ?? member.eduDomain,
      kind: activity.kind,
      type: activity.type,
      title: activity.title,
      note: activity.note,
      startsAt: Timestamp.fromMillis(activity.startsAt),
      level: activity.level,
      game: activity.game,
      capacity: activity.capacity,
      acceptedCount: 0,
      pendingCount: 0,
      status: "open",
      moderationFlags: check.flaggedTerms,
      createdAt: Timestamp.fromMillis(now),
      updatedAt: Timestamp.fromMillis(now),
    });
    transaction.set(database.doc(`socialUserState/${uid}`),
      dailyIncrement(member.state, "activitiesToday", now), { merge: true });
  });
  return { activityId: activityRef.id, status: "open" };
}

/**
 * Cancels an upcoming activity. Waiting requests close quietly; accepted
 * participants get a note in their chat, which then becomes read-only.
 */
export async function cancelSocialActivityService(
  database: Firestore,
  uid: string,
  input: { activityId?: unknown },
  deps: SocialDeps,
): Promise<{ status: "cancelled" }> {
  const now = nowOf(deps);
  const activityId = requireId(input.activityId, "ACTIVITY_ID");
  const activityRef = database.doc(`socialActivities/${activityId}`);
  const result = await database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, uid, SOCIAL_ROLES);
    const activity = await transaction.get(activityRef);
    if (!activity.exists || activity.get("organizerUid") !== uid) {
      throw new HttpsError("not-found", "SOCIAL_ACTIVITY_NOT_FOUND");
    }
    if (!LIVE_STATUSES.includes(String(activity.get("status")))) {
      throw new HttpsError("failed-precondition", "SOCIAL_ACTIVITY_STATUS_INVALID");
    }
    const requests = await transaction.get(activityRef.collection("joinRequests")
      .where("status", "in", ACTIVE_REQUEST_STATUSES).limit(SOCIAL_LIMITS.maxPendingRequestsPerActivity + SOCIAL_LIMITS.maxCapacity));
    const accepted = requests.docs.filter((request) => request.get("status") === "accepted").map((request) => request.id);
    const conversations = await Promise.all(accepted.map((participantUid) =>
      transaction.get(database.doc(`socialConversations/${conversationIdOf(activityId, participantUid)}`))));

    const at = Timestamp.fromMillis(now);
    transaction.update(activityRef, { status: "cancelled", pendingCount: 0, updatedAt: at, cancelledAt: at });
    requests.docs.filter((request) => request.get("status") === "pending")
      .forEach((request) => transaction.update(request.ref, { status: "closed", updatedAt: at }));
    conversations.filter((conversation) => conversation.exists && conversation.get("status") === "open")
      .forEach((conversation) => {
        const participantUid = String(conversation.get("participantUid"));
        addSystemMessage(transaction, conversation.ref, "Etkinlik iptal edildi.", participantUid, at, { status: "closed" });
        transaction.set(database.doc(`socialUserState/${participantUid}`), { unreadCount: FieldValue.increment(1) }, { merge: true });
      });
    return { accepted, title: String(activity.get("title") ?? "") };
  });
  await Promise.all(result.accepted.map((participantUid) => deps.notify(participantUid, {
    title: "Etkinlik iptal edildi",
    body: `"${result.title}" iptal edildi.`,
    data: { type: "social_conversation", conversationId: conversationIdOf(activityId, participantUid) },
  })));
  return { status: "cancelled" };
}

/** A note from Good4 inside a chat ("İsteğin kabul edildi", "Etkinlik iptal edildi"). */
function addSystemMessage(
  transaction: Transaction,
  conversationRef: DocumentReference,
  text: string,
  unreadFor: string,
  at: Timestamp,
  extra: Record<string, unknown> = {},
) {
  transaction.create(conversationRef.collection("messages").doc(), { senderUid: null, type: "system", text, createdAt: at });
  transaction.update(conversationRef, {
    ...extra,
    lastMessageText: text,
    lastMessageAt: at,
    lastSenderUid: null,
    messageCount: FieldValue.increment(1),
    [`unread.${unreadFor}`]: FieldValue.increment(1),
    updatedAt: at,
  });
}

/** Frees an accepted participant's place and reopens a full upcoming activity. */
function releasePlace(transaction: Transaction, activity: DocumentSnapshot, now: number) {
  const reopen = activity.get("status") === "full" && millisOf(activity.get("startsAt")) > now;
  transaction.update(activity.ref, {
    acceptedCount: Math.max(0, Number(activity.get("acceptedCount") ?? 0) - 1),
    ...(reopen ? { status: "open" } : {}),
    updatedAt: Timestamp.fromMillis(now),
  });
}

// ---------------------------------------------------------------------------
// Join requests
// ---------------------------------------------------------------------------

export async function requestToJoinSocialActivityService(
  database: Firestore,
  uid: string,
  input: { activityId?: unknown; note?: unknown },
  deps: SocialDeps,
): Promise<{ status: "pending" }> {
  const now = nowOf(deps);
  const activityId = requireId(input.activityId, "ACTIVITY_ID");
  const note = optionalText(input.note, "SOCIAL_REQUEST_NOTE", 200);

  await database.runTransaction((transaction) => requireSocialMember(database, transaction, uid, now));
  if (note) {
    const check = checkMarketText(note);
    if (check.blockedTerm) await rejectBlockedContent(database, uid, check.blockedTerm, "socialRequest", note, now);
  }

  const activityRef = database.doc(`socialActivities/${activityId}`);
  const requestRef = activityRef.collection("joinRequests").doc(uid);
  const result = await database.runTransaction(async (transaction) => {
    const member = await requireSocialMember(database, transaction, uid, now);
    const [activity, request] = await Promise.all([transaction.get(activityRef), transaction.get(requestRef)]);
    if (!activity.exists || activity.get("status") === "removed") {
      throw new HttpsError("not-found", "SOCIAL_ACTIVITY_NOT_FOUND");
    }
    const organizerUid = String(activity.get("organizerUid"));
    if (organizerUid === uid) throw new HttpsError("failed-precondition", "SOCIAL_OWN_ACTIVITY");
    if (request.exists) {
      const status = request.get("status");
      if (ACTIVE_REQUEST_STATUSES.includes(String(status))) {
        throw new HttpsError("already-exists", "SOCIAL_ALREADY_REQUESTED");
      }
      // Only a request the student withdrew may be sent again.
      if (status !== "withdrawn") throw new HttpsError("failed-precondition", "SOCIAL_REQUEST_CLOSED");
    }
    if (effectiveStatus(activity, now) !== "open") {
      throw new HttpsError("failed-precondition", "SOCIAL_ACTIVITY_UNAVAILABLE");
    }
    if (activity.get("eduDomain") !== member.eduDomain) {
      throw new HttpsError("permission-denied", "SOCIAL_OTHER_CAMPUS");
    }
    if (dailyCount(member.state, "requestsToday", now) >= SOCIAL_LIMITS.maxRequestsPerDay) {
      throw new HttpsError("resource-exhausted", "SOCIAL_DAILY_REQUEST_LIMIT");
    }
    if (Number(activity.get("pendingCount") ?? 0) >= SOCIAL_LIMITS.maxPendingRequestsPerActivity) {
      throw new HttpsError("resource-exhausted", "SOCIAL_REQUEST_QUEUE_FULL");
    }
    await requireActiveActor(database, transaction, organizerUid, SOCIAL_ROLES);
    const organizerModeration = await transaction.get(database.doc(`marketUserState/${organizerUid}`));
    if (isBlockedBetween(member.moderation, uid, organizerModeration, organizerUid)) {
      throw new HttpsError("permission-denied", "SOCIAL_BLOCKED");
    }

    const at = Timestamp.fromMillis(now);
    const requesterName = publicName(member.user.get("displayName"));
    transaction.set(requestRef, {
      activityId,
      organizerUid,
      requesterUid: uid,
      requesterName,
      note,
      status: "pending",
      // Copied so "Etkinliklerim" can list a student's requests in start order.
      activityStartsAt: activity.get("startsAt"),
      createdAt: at,
      updatedAt: at,
    });
    transaction.update(activityRef, { pendingCount: FieldValue.increment(1), updatedAt: at });
    transaction.set(database.doc(`socialUserState/${uid}`),
      dailyIncrement(member.state, "requestsToday", now), { merge: true });
    return { organizerUid, requesterName, title: String(activity.get("title") ?? "") };
  });

  await deps.notify(result.organizerUid, {
    title: "Yeni katılım isteği",
    body: `${result.requesterName}, "${result.title}" etkinliğine katılmak istiyor.`.slice(0, 140),
    data: { type: "social_request", activityId },
  });
  return { status: "pending" };
}

/**
 * Takes back a waiting request, or leaves an activity the student was
 * accepted to. Leaving frees the place and closes the chat.
 */
export async function withdrawSocialRequestService(
  database: Firestore,
  uid: string,
  input: { activityId?: unknown },
  deps: SocialDeps,
): Promise<{ status: "withdrawn" }> {
  const now = nowOf(deps);
  const activityId = requireId(input.activityId, "ACTIVITY_ID");
  const activityRef = database.doc(`socialActivities/${activityId}`);
  const requestRef = activityRef.collection("joinRequests").doc(uid);
  const conversationRef = database.doc(`socialConversations/${conversationIdOf(activityId, uid)}`);
  const left = await database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, uid, SOCIAL_ROLES);
    const [activity, request, conversation] = await Promise.all([
      transaction.get(activityRef), transaction.get(requestRef), transaction.get(conversationRef),
    ]);
    const status = request.get("status");
    if (!activity.exists || !request.exists || !ACTIVE_REQUEST_STATUSES.includes(String(status))) {
      throw new HttpsError("failed-precondition", "SOCIAL_REQUEST_NOT_ACTIVE");
    }
    const at = Timestamp.fromMillis(now);
    if (status === "pending") {
      transaction.update(requestRef, { status: "withdrawn", updatedAt: at });
      transaction.update(activityRef, {
        pendingCount: Math.max(0, Number(activity.get("pendingCount") ?? 0) - 1), updatedAt: at,
      });
      return null;
    }
    transaction.update(requestRef, { status: "left", updatedAt: at });
    releasePlace(transaction, activity, now);
    const organizerUid = String(activity.get("organizerUid"));
    const name = String(request.get("requesterName") ?? "Öğrenci");
    // A blocked chat keeps its block; only an open one gets the note and closes.
    if (conversation.exists && conversation.get("status") === "open") {
      addSystemMessage(transaction, conversationRef, `${name} katılımdan vazgeçti.`, organizerUid, at, { status: "closed" });
      transaction.set(database.doc(`socialUserState/${organizerUid}`), { unreadCount: FieldValue.increment(1) }, { merge: true });
    }
    return { organizerUid, name, title: String(activity.get("title") ?? "") };
  });
  if (left) {
    await deps.notify(left.organizerUid, {
      title: "Bir katılımcı ayrıldı",
      body: `${left.name}, "${left.title}" etkinliğinden ayrıldı. Yeri yeniden açıldı.`.slice(0, 140),
      data: { type: "social_activity", activityId },
    });
  }
  return { status: "withdrawn" };
}

/**
 * The organizer's answer. Accepting opens the chat; when the last place is
 * taken every other waiting request closes. Declining sends nothing.
 */
export async function respondToSocialRequestService(
  database: Firestore,
  uid: string,
  input: { activityId?: unknown; requesterUid?: unknown; accept?: unknown },
  deps: SocialDeps,
): Promise<{ status: "accepted" | "declined"; conversationId: string | null }> {
  const now = nowOf(deps);
  const activityId = requireId(input.activityId, "ACTIVITY_ID");
  const requesterUid = requireId(input.requesterUid, "REQUESTER_UID");
  if (typeof input.accept !== "boolean") throw new HttpsError("invalid-argument", "SOCIAL_RESPONSE_INVALID");
  const accept = input.accept;
  const activityRef = database.doc(`socialActivities/${activityId}`);
  const requestRef = activityRef.collection("joinRequests").doc(requesterUid);
  const conversationId = conversationIdOf(activityId, requesterUid);
  const conversationRef = database.doc(`socialConversations/${conversationId}`);

  const result = await database.runTransaction(async (transaction) => {
    const member = await requireSocialMember(database, transaction, uid, now);
    const [activity, request] = await Promise.all([transaction.get(activityRef), transaction.get(requestRef)]);
    if (!activity.exists || activity.get("organizerUid") !== uid) {
      throw new HttpsError("not-found", "SOCIAL_ACTIVITY_NOT_FOUND");
    }
    if (!request.exists || request.get("status") !== "pending") {
      throw new HttpsError("failed-precondition", "SOCIAL_REQUEST_NOT_PENDING");
    }
    const at = Timestamp.fromMillis(now);
    if (!accept) {
      transaction.update(requestRef, { status: "declined", respondedAt: at, updatedAt: at });
      transaction.update(activityRef, {
        pendingCount: Math.max(0, Number(activity.get("pendingCount") ?? 0) - 1), updatedAt: at,
      });
      return null;
    }

    if (effectiveStatus(activity, now) !== "open") {
      throw new HttpsError("failed-precondition", "SOCIAL_ACTIVITY_UNAVAILABLE");
    }
    await requireActiveActor(database, transaction, requesterUid, SOCIAL_ROLES);
    const requesterModeration = await transaction.get(database.doc(`marketUserState/${requesterUid}`));
    if (isBlockedBetween(member.moderation, uid, requesterModeration, requesterUid)) {
      throw new HttpsError("permission-denied", "SOCIAL_BLOCKED");
    }
    const acceptedCount = Number(activity.get("acceptedCount") ?? 0) + 1;
    const full = acceptedCount >= Number(activity.get("capacity") ?? 1);
    const waiting = full
      ? await transaction.get(activityRef.collection("joinRequests").where("status", "==", "pending")
        .limit(SOCIAL_LIMITS.maxPendingRequestsPerActivity + 10))
      : null;

    transaction.update(requestRef, { status: "accepted", respondedAt: at, updatedAt: at });
    waiting?.docs.filter((other) => other.id !== requesterUid)
      .forEach((other) => transaction.update(other.ref, { status: "closed", updatedAt: at }));
    transaction.update(activityRef, {
      acceptedCount,
      pendingCount: full ? 0 : Math.max(0, Number(activity.get("pendingCount") ?? 0) - 1),
      status: full ? "full" : "open",
      updatedAt: at,
    });
    // Both sides read this note, so it names no one.
    const text = "İstek kabul edildi. Buluşma ayrıntılarını buradan konuşabilirsiniz.";
    transaction.set(conversationRef, {
      activityId,
      organizerUid: uid,
      participantUid: requesterUid,
      participants: [uid, requesterUid],
      activityTitle: String(activity.get("title") ?? ""),
      activityKind: String(activity.get("kind") ?? ""),
      activityType: String(activity.get("type") ?? ""),
      activityStartsAt: activity.get("startsAt"),
      organizerName: publicName(member.user.get("displayName")),
      participantName: publicName(request.get("requesterName")),
      status: "open",
      lastMessageText: text,
      lastMessageAt: at,
      lastSenderUid: null,
      unread: { [uid]: 0, [requesterUid]: 1 },
      messageCount: 1,
      createdAt: at,
      updatedAt: at,
    });
    transaction.create(conversationRef.collection("messages").doc(), { senderUid: null, type: "system", text, createdAt: at });
    transaction.set(database.doc(`socialUserState/${requesterUid}`), { unreadCount: FieldValue.increment(1) }, { merge: true });
    return { title: String(activity.get("title") ?? "") };
  });

  if (!result) return { status: "declined", conversationId: null };
  await deps.notify(requesterUid, {
    title: "İsteğin kabul edildi",
    body: `"${result.title}" için organizatörle mesajlaşabilirsin.`.slice(0, 140),
    data: { type: "social_conversation", conversationId },
  });
  return { status: "accepted", conversationId };
}

// ---------------------------------------------------------------------------
// Reads. The app reads social activities only through these callables, so the
// collections stay closed to clients and campus and block filters run here.
// ---------------------------------------------------------------------------

function activityForStudent(activity: DocumentSnapshot, viewerUid: string, now: number, withDetail = false) {
  const isMine = activity.get("organizerUid") === viewerUid;
  const capacity = Number(activity.get("capacity") ?? 1);
  const acceptedCount = Number(activity.get("acceptedCount") ?? 0);
  return {
    id: activity.id,
    kind: String(activity.get("kind") ?? ""),
    type: String(activity.get("type") ?? ""),
    title: String(activity.get("title") ?? ""),
    startsAt: iso(activity.get("startsAt")),
    level: activity.get("level") ?? null,
    game: activity.get("game") ?? null,
    capacity,
    acceptedCount,
    spotsLeft: Math.max(0, capacity - acceptedCount),
    organizerName: publicName(activity.get("organizerName")),
    universityName: String(activity.get("universityName") ?? ""),
    status: effectiveStatus(activity, now),
    isMine,
    ...(withDetail ? { note: String(activity.get("note") ?? "") } : {}),
    ...(isMine ? { pendingCount: Number(activity.get("pendingCount") ?? 0) } : {}),
  };
}

async function viewerContext(database: Firestore, uid: string, now: number) {
  const [user, state, moderation, config] = await Promise.all([
    database.doc(`users/${uid}`).get(),
    database.doc(`socialUserState/${uid}`).get(),
    database.doc(`marketUserState/${uid}`).get(),
    database.doc("app_config/social_activities").get(),
  ]);
  if (!user.exists || user.get("status") !== "active") throw new HttpsError("permission-denied", "ACCOUNT_NOT_ACTIVE");
  const verified = hasVerifiedCampusEmail(user);
  const eduDomain = verified ? eduDomainOf(String(user.get("eduEmail"))) : null;
  const suspendedUntil = moderation.get("suspendedUntil");
  return {
    eduDomain,
    blocked: new Set((moderation.get("blockedUids") ?? []) as string[]),
    me: {
      enabled: config.get("enabled") === true,
      eduVerified: verified,
      eduEmail: verified ? String(user.get("eduEmail")) : null,
      universityName: eduDomain ? UNIVERSITY_NAMES[eduDomain] ?? eduDomain : null,
      termsAccepted: Number(state.get("termsVersion") ?? 0) >= SOCIAL_TERMS_VERSION,
      termsVersion: SOCIAL_TERMS_VERSION,
      nameMode: profileFrom(user, state).nameMode,
      shownName: openName(user.get("displayName")),
      maskedName: publicName(user.get("displayName")),
      photoUrl: profileFrom(user, state).photoThumbUrl,
      suspendedUntil: suspendedUntil instanceof Timestamp && suspendedUntil.toMillis() > now ? iso(suspendedUntil) : null,
      unreadCount: Number(state.get("unreadCount") ?? 0),
    },
  };
}

/** Small status call for the home tile badge and the screen gates. */
export async function getSocialSummaryService(
  database: Firestore,
  uid: string,
  deps: Pick<SocialDeps, "now"> = {},
) {
  const now = nowOf(deps);
  const viewer = await viewerContext(database, uid, now);
  const mine = await database.collection("socialActivities")
    .where("organizerUid", "==", uid).where("status", "==", "open").limit(20).get();
  const pendingRequestCount = mine.docs
    .filter((activity) => millisOf(activity.get("startsAt")) > now)
    .reduce((sum, activity) => sum + Number(activity.get("pendingCount") ?? 0), 0);
  return { me: { ...viewer.me, pendingRequestCount } };
}

/**
 * Upcoming open activities, soonest first. Verified students see their own
 * campus; others see every campus read-only until they verify.
 */
export async function getSocialFeedService(
  database: Firestore,
  uid: string,
  input: { kind?: unknown; after?: unknown },
  deps: Pick<SocialDeps, "now"> = {},
) {
  const now = nowOf(deps);
  const viewer = await viewerContext(database, uid, now);
  const kind = input.kind === undefined || input.kind === null || input.kind === ""
    ? null
    : requireOneOf(input.kind, SOCIAL_KINDS, "SOCIAL_KIND_INVALID");
  let query: Query = database.collection("socialActivities").where("status", "==", "open");
  if (viewer.eduDomain) query = query.where("eduDomain", "==", viewer.eduDomain);
  if (kind) query = query.where("kind", "==", kind);
  query = query.where("startsAt", ">", Timestamp.fromMillis(now))
    .orderBy("startsAt", "asc").orderBy(FieldPath.documentId(), "asc");
  if (input.after !== undefined && input.after !== null) {
    const match = typeof input.after === "string" ? /^(.+)\|([A-Za-z0-9]{1,64})$/.exec(input.after) : null;
    const after = match ? Date.parse(match[1]!) : Number.NaN;
    if (!match || Number.isNaN(after)) throw new HttpsError("invalid-argument", "SOCIAL_CURSOR_INVALID");
    query = query.startAfter(Timestamp.fromMillis(after), match[2]!);
  }
  const page = await query.limit(FEED_PAGE_SIZE).get();
  const last = page.docs.at(-1);
  return {
    me: viewer.me,
    activities: page.docs
      .filter((activity) => !viewer.blocked.has(String(activity.get("organizerUid"))))
      .map((activity) => activityForStudent(activity, uid, now)),
    nextAfter: page.size === FEED_PAGE_SIZE && last ? `${iso(last.get("startsAt"))}|${last.id}` : null,
  };
}

export async function getSocialActivityService(
  database: Firestore,
  uid: string,
  input: { activityId?: unknown },
  deps: Pick<SocialDeps, "now"> = {},
) {
  const now = nowOf(deps);
  const activityId = requireId(input.activityId, "ACTIVITY_ID");
  const activityRef = database.doc(`socialActivities/${activityId}`);
  const [viewer, activity, request] = await Promise.all([
    viewerContext(database, uid, now),
    activityRef.get(),
    activityRef.collection("joinRequests").doc(uid).get(),
  ]);
  const isMine = activity.get("organizerUid") === uid;
  const visible = activity.exists && activity.get("status") !== "removed" && (isMine || request.exists
    || (LIVE_STATUSES.includes(String(activity.get("status")))
      && !viewer.blocked.has(String(activity.get("organizerUid")))));
  if (!visible) throw new HttpsError("not-found", "SOCIAL_ACTIVITY_NOT_FOUND");
  const status = request.exists ? requestStatusForRequester(request.get("status")) : null;
  const organizer = (await loadSocialProfiles(database, [String(activity.get("organizerUid"))])).get(String(activity.get("organizerUid")));
  // A photo is shown to verified students of the same university only.
  const sameCampus = viewer.eduDomain !== null && viewer.eduDomain === activity.get("eduDomain");
  return {
    me: viewer.me,
    activity: {
      ...activityForStudent(activity, uid, now, true),
      organizerName: organizer?.name ?? publicName(activity.get("organizerName")),
      organizerPhotoUrl: sameCampus ? organizer?.photoThumbUrl ?? null : null,
    },
    myRequest: status ? {
      status,
      conversationId: status === "accepted" ? conversationIdOf(activityId, uid) : null,
    } : null,
    sameCampus,
  };
}

/** The organizer's list: waiting requests to answer and accepted participants. */
export async function listSocialActivityRequestsService(
  database: Firestore,
  uid: string,
  input: { activityId?: unknown },
  deps: Pick<SocialDeps, "now"> = {},
) {
  const now = nowOf(deps);
  const activityId = requireId(input.activityId, "ACTIVITY_ID");
  await requireSocialReader(database, uid);
  const activityRef = database.doc(`socialActivities/${activityId}`);
  const activity = await activityRef.get();
  if (!activity.exists || activity.get("organizerUid") !== uid) {
    throw new HttpsError("not-found", "SOCIAL_ACTIVITY_NOT_FOUND");
  }
  const requests = await activityRef.collection("joinRequests")
    .where("status", "in", ACTIVE_REQUEST_STATUSES).orderBy("createdAt", "asc")
    .limit(SOCIAL_LIMITS.maxPendingRequestsPerActivity + SOCIAL_LIMITS.maxCapacity).get();
  const profiles = await loadSocialProfiles(database, requests.docs.map((request) => request.id));
  return {
    activity: activityForStudent(activity, uid, now, true),
    requests: requests.docs.map((request) => ({
      requesterUid: request.id,
      name: profiles.get(request.id)?.name ?? publicName(request.get("requesterName")),
      photoUrl: profiles.get(request.id)?.photoThumbUrl ?? null,
      note: String(request.get("note") ?? ""),
      status: String(request.get("status")),
      createdAt: iso(request.get("createdAt")),
      conversationId: request.get("status") === "accepted" ? conversationIdOf(activityId, request.id) : null,
    })),
  };
}

/** "Etkinliklerim": activities the student opened and the ones they asked to join. */
export async function listMySocialActivitiesService(
  database: Firestore,
  uid: string,
  deps: Pick<SocialDeps, "now"> = {},
) {
  const now = nowOf(deps);
  await requireSocialReader(database, uid);
  const [organized, requests] = await Promise.all([
    database.collection("socialActivities").where("organizerUid", "==", uid)
      .orderBy("startsAt", "desc").limit(30).get(),
    database.collectionGroup("joinRequests").where("requesterUid", "==", uid)
      .orderBy("activityStartsAt", "desc").limit(50).get(),
  ]);
  const activityRefs = requests.docs.map((request) => request.ref.parent.parent).filter(
    (ref): ref is DocumentReference => ref !== null);
  const activities = activityRefs.length > 0 ? await database.getAll(...activityRefs) : [];
  const byId = new Map(activities.filter((activity) => activity.exists).map((activity) => [activity.id, activity]));
  return {
    organized: organized.docs
      .filter((activity) => activity.get("status") !== "removed")
      .map((activity) => activityForStudent(activity, uid, now)),
    joined: requests.docs.flatMap((request) => {
      const activity = byId.get(request.ref.parent.parent?.id ?? "");
      if (!activity || activity.get("status") === "removed") return [];
      const status = requestStatusForRequester(request.get("status"));
      return [{
        activity: activityForStudent(activity, uid, now),
        status,
        conversationId: status === "accepted" ? conversationIdOf(activity.id, uid) : null,
      }];
    }),
  };
}

// ---------------------------------------------------------------------------
// Chats
// ---------------------------------------------------------------------------

function requireParticipant(conversation: DocumentSnapshot, uid: string): string {
  const participants = conversation.get("participants");
  if (!conversation.exists || conversation.get("status") === "deleting"
    || !Array.isArray(participants) || !participants.includes(uid)) {
    throw new HttpsError("permission-denied", "SOCIAL_NOT_PARTICIPANT");
  }
  return String(participants.find((participant) => participant !== uid));
}

function otherUidOf(conversation: DocumentSnapshot, uid: string): string {
  return String(conversation.get(conversation.get("organizerUid") === uid ? "participantUid" : "organizerUid") ?? "");
}

function conversationForStudent(
  conversation: DocumentSnapshot, uid: string, now: number, others: Map<string, SocialProfile> = new Map(),
) {
  const isOrganizer = conversation.get("organizerUid") === uid;
  const other = others.get(otherUidOf(conversation, uid));
  const status = String(conversation.get("status") ?? "open");
  return {
    id: conversation.id,
    activityId: String(conversation.get("activityId") ?? ""),
    activityTitle: String(conversation.get("activityTitle") ?? ""),
    activityKind: String(conversation.get("activityKind") ?? ""),
    activityType: String(conversation.get("activityType") ?? ""),
    activityStartsAt: iso(conversation.get("activityStartsAt")),
    role: isOrganizer ? "organizer" : "participant",
    otherName: other?.name ?? publicName(conversation.get(isOrganizer ? "participantName" : "organizerName")),
    otherPhotoUrl: other?.photoThumbUrl ?? null,
    lastMessageText: String(conversation.get("lastMessageText") ?? ""),
    lastMessageAt: iso(conversation.get("lastMessageAt")),
    lastMessageMine: conversation.get("lastSenderUid") === uid,
    unread: Number(conversation.get(`unread.${uid}`) ?? 0),
    status,
    readOnly: status !== "open" || now >= chatWritableUntil(conversation.get("activityStartsAt")),
    blockedByMe: conversation.get("blockedBy") === uid,
  };
}

export async function sendSocialMessageService(
  database: Firestore,
  uid: string,
  input: { conversationId?: unknown; text?: unknown },
  deps: SocialDeps,
): Promise<{ conversationId: string; messageId: string }> {
  const now = nowOf(deps);
  const { conversationId } = parseConversationId(input.conversationId);
  const text = requireText(input.text, "SOCIAL_MESSAGE", 1, SOCIAL_LIMITS.maxMessageLength);

  await database.runTransaction((transaction) => requireSocialMember(database, transaction, uid, now));
  const check = checkMarketText(text);
  if (check.blockedTerm) await rejectBlockedContent(database, uid, check.blockedTerm, "socialMessage", text, now);

  const conversationRef = database.doc(`socialConversations/${conversationId}`);
  const messageRef = conversationRef.collection("messages").doc();
  const result = await database.runTransaction(async (transaction) => {
    const member = await requireSocialMember(database, transaction, uid, now);
    const conversation = await transaction.get(conversationRef);
    const otherUid = requireParticipant(conversation, uid);
    if (conversation.get("status") !== "open" || now >= chatWritableUntil(conversation.get("activityStartsAt"))) {
      throw new HttpsError("failed-precondition", "SOCIAL_CONVERSATION_CLOSED");
    }
    if (dailyCount(member.state, "messagesToday", now) >= SOCIAL_LIMITS.maxMessagesPerDay) {
      throw new HttpsError("resource-exhausted", "SOCIAL_DAILY_MESSAGE_LIMIT");
    }
    await requireActiveActor(database, transaction, otherUid, SOCIAL_ROLES);
    const otherModeration = await transaction.get(database.doc(`marketUserState/${otherUid}`));
    if (isBlockedBetween(member.moderation, uid, otherModeration, otherUid)) {
      throw new HttpsError("permission-denied", "SOCIAL_BLOCKED");
    }
    const at = Timestamp.fromMillis(now);
    const senderName = publicName(member.user.get("displayName"));
    transaction.create(messageRef, { senderUid: uid, type: "text", text, createdAt: at });
    transaction.update(conversationRef, {
      lastMessageText: text.slice(0, 120),
      lastMessageAt: at,
      lastSenderUid: uid,
      messageCount: FieldValue.increment(1),
      [`unread.${otherUid}`]: FieldValue.increment(1),
      updatedAt: at,
    });
    transaction.set(database.doc(`socialUserState/${uid}`),
      dailyIncrement(member.state, "messagesToday", now), { merge: true });
    transaction.set(database.doc(`socialUserState/${otherUid}`), { unreadCount: FieldValue.increment(1) }, { merge: true });
    return { otherUid, senderName, title: String(conversation.get("activityTitle") ?? "") };
  });

  await deps.notify(result.otherUid, {
    title: `${result.senderName} · ${result.title}`.slice(0, 80),
    body: text.slice(0, 140),
    data: { type: "social_message", conversationId },
  });
  return { conversationId, messageId: messageRef.id };
}

export async function markSocialConversationReadService(
  database: Firestore,
  uid: string,
  input: { conversationId?: unknown },
): Promise<{ unreadCount: number }> {
  const { conversationId } = parseConversationId(input.conversationId);
  const conversationRef = database.doc(`socialConversations/${conversationId}`);
  const stateRef = database.doc(`socialUserState/${uid}`);
  return database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, uid, SOCIAL_ROLES);
    const [conversation, state] = await Promise.all([transaction.get(conversationRef), transaction.get(stateRef)]);
    requireParticipant(conversation, uid);
    const unread = Number(conversation.get(`unread.${uid}`) ?? 0);
    const total = Math.max(0, Number(state.get("unreadCount") ?? 0) - unread);
    if (unread > 0) {
      transaction.update(conversationRef, { [`unread.${uid}`]: 0 });
      transaction.set(stateRef, { unreadCount: total }, { merge: true });
    }
    return { unreadCount: total };
  });
}

export async function listSocialConversationsService(
  database: Firestore,
  uid: string,
  deps: Pick<SocialDeps, "now"> = {},
) {
  const now = nowOf(deps);
  await requireSocialReader(database, uid);
  const [page, state] = await Promise.all([
    database.collection("socialConversations").where("participants", "array-contains", uid)
      .orderBy("lastMessageAt", "desc").limit(50).get(),
    database.doc(`socialUserState/${uid}`).get(),
  ]);
  const shown = page.docs.filter((conversation) => conversation.get("status") !== "deleting");
  const others = await loadSocialProfiles(database, shown.map((conversation) => otherUidOf(conversation, uid)));
  return {
    unreadCount: Number(state.get("unreadCount") ?? 0),
    conversations: shown.map((conversation) => conversationForStudent(conversation, uid, now, others)),
  };
}

/**
 * Returns the newest messages (or only those after `after`) and clears the
 * caller's unread counter. The open chat screen polls this every few seconds.
 */
export async function getSocialMessagesService(
  database: Firestore,
  uid: string,
  input: { conversationId?: unknown; after?: unknown },
  deps: Pick<SocialDeps, "now"> = {},
) {
  const now = nowOf(deps);
  await requireSocialReader(database, uid);
  const { conversationId } = parseConversationId(input.conversationId);
  const conversationRef = database.doc(`socialConversations/${conversationId}`);
  const conversation = await conversationRef.get();
  requireParticipant(conversation, uid);
  let query = conversationRef.collection("messages").orderBy("createdAt", "desc");
  if (input.after !== undefined && input.after !== null) {
    const after = Date.parse(String(input.after));
    if (Number.isNaN(after)) throw new HttpsError("invalid-argument", "SOCIAL_CURSOR_INVALID");
    query = query.endBefore(Timestamp.fromMillis(after));
  }
  const page = await query.limit(POLL_PAGE_SIZE).get();
  if (Number(conversation.get(`unread.${uid}`) ?? 0) > 0) {
    await markSocialConversationReadService(database, uid, { conversationId });
  }
  return {
    conversation: {
      ...conversationForStudent(conversation, uid, now, await loadSocialProfiles(database, [otherUidOf(conversation, uid)])),
      unread: 0,
    },
    messages: page.docs.reverse().map((message) => ({
      id: message.id,
      mine: message.get("senderUid") === uid,
      type: String(message.get("type") ?? "text"),
      text: String(message.get("text") ?? ""),
      createdAt: iso(message.get("createdAt")),
    })),
  };
}

// ---------------------------------------------------------------------------
// Blocking and reports. The block list lives in marketUserState, so a block
// in one feature also holds in the other.
// ---------------------------------------------------------------------------

/**
 * Blocks the other side of a chat. If they were an accepted participant, the
 * place is freed: the organizer removes them, or the participant leaves.
 */
export async function blockSocialUserService(
  database: Firestore,
  uid: string,
  input: { conversationId?: unknown },
  deps: Pick<SocialDeps, "now"> = {},
): Promise<{ blocked: true }> {
  const now = nowOf(deps);
  const { conversationId, activityId, participantUid } = parseConversationId(input.conversationId);
  const conversationRef = database.doc(`socialConversations/${conversationId}`);
  const moderationRef = database.doc(`marketUserState/${uid}`);
  const activityRef = database.doc(`socialActivities/${activityId}`);
  const requestRef = activityRef.collection("joinRequests").doc(participantUid);
  await database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, uid, SOCIAL_ROLES);
    const [conversation, moderation, activity, request] = await Promise.all([
      transaction.get(conversationRef), transaction.get(moderationRef), transaction.get(activityRef), transaction.get(requestRef),
    ]);
    const otherUid = requireParticipant(conversation, uid);
    const blocked = Array.isArray(moderation.get("blockedUids")) ? moderation.get("blockedUids") as string[] : [];
    if (!blocked.includes(otherUid) && blocked.length >= MARKET_LIMITS.maxBlockedUsers) {
      throw new HttpsError("resource-exhausted", "SOCIAL_BLOCK_LIMIT");
    }
    const at = Timestamp.fromMillis(now);
    transaction.set(moderationRef, { blockedUids: FieldValue.arrayUnion(otherUid) }, { merge: true });
    transaction.update(conversationRef, { status: "blocked", blockedBy: uid, updatedAt: at });
    if (activity.exists && request.exists && request.get("status") === "accepted") {
      const byOrganizer = activity.get("organizerUid") === uid;
      transaction.update(requestRef, { status: byOrganizer ? "removed" : "left", updatedAt: at });
      releasePlace(transaction, activity, now);
    }
  });
  return { blocked: true };
}

/**
 * Lifts the caller's own block. A block placed by the other side stays in
 * force, and a chat whose participant left stays read-only.
 */
export async function unblockSocialUserService(
  database: Firestore,
  uid: string,
  input: { conversationId?: unknown },
  deps: Pick<SocialDeps, "now"> = {},
): Promise<{ unblocked: true; status: string }> {
  const { conversationId, activityId, participantUid } = parseConversationId(input.conversationId);
  const conversationRef = database.doc(`socialConversations/${conversationId}`);
  const requestRef = database.doc(`socialActivities/${activityId}/joinRequests/${participantUid}`);
  const status = await database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, uid, SOCIAL_ROLES);
    const [conversation, request] = await Promise.all([transaction.get(conversationRef), transaction.get(requestRef)]);
    const otherUid = requireParticipant(conversation, uid);
    const otherModeration = await transaction.get(database.doc(`marketUserState/${otherUid}`));
    const otherBlocksMe = Array.isArray(otherModeration.get("blockedUids"))
      && (otherModeration.get("blockedUids") as unknown[]).includes(uid);
    transaction.set(database.doc(`marketUserState/${uid}`), { blockedUids: FieldValue.arrayRemove(otherUid) }, { merge: true });
    const next = otherBlocksMe ? "blocked" : request.get("status") === "accepted" ? "open" : "closed";
    transaction.update(conversationRef, {
      status: next,
      blockedBy: otherBlocksMe ? otherUid : FieldValue.delete(),
      updatedAt: Timestamp.fromMillis(nowOf(deps)),
    });
    return next;
  });
  return { unblocked: true, status };
}

/** Chats the caller blocked, for the "Engellediklerin" list. */
export async function listSocialBlockedService(database: Firestore, uid: string) {
  await requireSocialReader(database, uid);
  const page = await database.collection("socialConversations").where("blockedBy", "==", uid).limit(100).get();
  return {
    blocked: page.docs
      .filter((conversation) => (conversation.get("participants") as string[] | undefined)?.includes(uid))
      .map((conversation) => {
        const isOrganizer = conversation.get("organizerUid") === uid;
        return {
          conversationId: conversation.id,
          otherName: publicName(conversation.get(isOrganizer ? "participantName" : "organizerName")),
          activityTitle: String(conversation.get("activityTitle") ?? ""),
        };
      }),
  };
}

async function notifyAdmins(database: Firestore, deps: SocialDeps, payload: PushPayload) {
  const admins = await database.collection("users").where("role", "==", "good4Admin")
    .where("status", "==", "active").limit(5).get();
  await Promise.all(admins.docs.map((admin) => deps.notify(admin.id, payload)));
}

export async function reportSocialContentService(
  database: Firestore,
  uid: string,
  input: { targetType?: unknown; targetId?: unknown; reason?: unknown; note?: unknown },
  deps: SocialDeps,
): Promise<{ reportId: string }> {
  const now = nowOf(deps);
  const targetType = requireOneOf(input.targetType, ["activity", "conversation"] as const, "SOCIAL_REPORT_TARGET_INVALID");
  const targetId = targetType === "activity"
    ? requireId(input.targetId, "ACTIVITY_ID")
    : parseConversationId(input.targetId).conversationId;
  const reason = requireOneOf(input.reason, SOCIAL_REPORT_REASONS, "SOCIAL_REPORT_REASON_INVALID");
  const note = optionalText(input.note, "SOCIAL_REPORT_NOTE", 500);

  const reportRef = database.doc(`socialReports/${targetType}_${targetId}_${uid}`);
  const created = await database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, uid, SOCIAL_ROLES);
    let activityId: string;
    let reportedUid: string;
    if (targetType === "activity") {
      const activity = await transaction.get(database.doc(`socialActivities/${targetId}`));
      if (!activity.exists || activity.get("status") === "removed") {
        throw new HttpsError("not-found", "SOCIAL_ACTIVITY_NOT_FOUND");
      }
      activityId = targetId;
      reportedUid = String(activity.get("organizerUid"));
    } else {
      const conversation = await transaction.get(database.doc(`socialConversations/${targetId}`));
      reportedUid = requireParticipant(conversation, uid);
      activityId = String(conversation.get("activityId"));
    }
    if (reportedUid === uid) throw new HttpsError("invalid-argument", "SOCIAL_REPORT_SELF");
    const existing = await transaction.get(reportRef);
    if (existing.exists && existing.get("status") === "open") return false;
    transaction.set(reportRef, {
      reporterUid: uid, targetType, targetId, activityId, reportedUid, reason, note,
      status: "open", createdAt: Timestamp.fromMillis(now),
    });
    return true;
  });

  if (created) {
    await notifyAdmins(database, deps, {
      title: "Sosyal etkinlik: yeni şikayet",
      body: "Bir etkinlik ya da konuşma şikayet edildi.",
      data: { type: "social_admin_report", reportId: reportRef.id },
    });
  }
  return { reportId: reportRef.id };
}

// ---------------------------------------------------------------------------
// Admin moderation
// ---------------------------------------------------------------------------

async function requireAdmin(database: Firestore, uid: string): Promise<void> {
  await database.runTransaction((transaction) => requireActiveActor(database, transaction, uid, ["good4Admin"]));
}

function audit(transaction: Transaction, database: Firestore, actorUid: string, action: string,
  targetType: string, targetId: string, metadata: Record<string, unknown> = {}) {
  transaction.create(database.collection("auditLogs").doc(), {
    action, actorUid, targetType, targetId, metadata, createdAt: FieldValue.serverTimestamp(),
  });
}

export async function listSocialModerationQueueService(database: Firestore, uid: string) {
  await requireAdmin(database, uid);
  const [reports, violations] = await Promise.all([
    database.collection("socialReports").where("status", "==", "open").orderBy("createdAt", "asc").limit(100).get(),
    database.collection("marketViolations").where("context", "in", ["socialActivity", "socialRequest", "socialMessage"])
      .orderBy("createdAt", "desc").limit(50).get(),
  ]);
  const activityIds = [...new Set(reports.docs.map((report) => String(report.get("activityId"))))];
  const activities = activityIds.length > 0
    ? await database.getAll(...activityIds.map((id) => database.doc(`socialActivities/${id}`)))
    : [];
  const byId = new Map(activities.filter((activity) => activity.exists).map((activity) => [activity.id, activity]));
  const profiles = await loadSocialProfiles(database, reports.docs.map((report) => String(report.get("reportedUid"))));
  return {
    reports: reports.docs.map((report) => {
      const activity = byId.get(String(report.get("activityId")));
      return {
        id: report.id,
        targetType: report.get("targetType"),
        reason: report.get("reason"),
        note: report.get("note") ?? "",
        reporterUid: report.get("reporterUid"),
        reportedUid: report.get("reportedUid"),
        reportedProfile: profiles.get(String(report.get("reportedUid"))) ?? null,
        createdAt: iso(report.get("createdAt")),
        activity: activity ? {
          id: activity.id,
          title: activity.get("title"),
          note: activity.get("note") ?? "",
          kind: activity.get("kind"),
          type: activity.get("type"),
          organizerUid: activity.get("organizerUid"),
          organizerName: publicName(activity.get("organizerName")),
          startsAt: iso(activity.get("startsAt")),
          status: activity.get("status"),
        } : null,
      };
    }),
    violations: violations.docs.map((violation) => ({
      id: violation.id,
      uid: violation.get("uid"),
      term: violation.get("term"),
      context: violation.get("context"),
      excerpt: violation.get("excerpt"),
      suspended: violation.get("suspended") === true,
      createdAt: iso(violation.get("createdAt")),
    })),
  };
}

export async function resolveSocialReportService(
  database: Firestore,
  uid: string,
  input: { reportId?: unknown; action?: unknown; suspendDays?: unknown },
  deps: Pick<SocialDeps, "now"> = {},
): Promise<{ resolved: true }> {
  const now = nowOf(deps);
  if (typeof input.reportId !== "string" || !/^[A-Za-z]+_[A-Za-z0-9_]{1,300}$/.test(input.reportId)) {
    throw new HttpsError("invalid-argument", "REPORT_ID_INVALID");
  }
  const action = requireOneOf(input.action, ["dismiss", "removeActivity", "suspendUser"] as const,
    "SOCIAL_REPORT_ACTION_INVALID");
  const suspendDays = action === "suspendUser" ? input.suspendDays : null;
  if (suspendDays !== null && (typeof suspendDays !== "number" || !Number.isInteger(suspendDays)
    || suspendDays < 1 || suspendDays > 3650)) {
    throw new HttpsError("invalid-argument", "SOCIAL_SUSPEND_DAYS_INVALID");
  }
  const reportRef = database.doc(`socialReports/${input.reportId}`);
  await database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, uid, ["good4Admin"]);
    const report = await transaction.get(reportRef);
    if (!report.exists) throw new HttpsError("not-found", "SOCIAL_REPORT_NOT_FOUND");
    const activityId = String(report.get("activityId"));
    const reportedUid = String(report.get("reportedUid"));
    const at = Timestamp.fromMillis(now);
    if (action === "removeActivity") {
      const activityRef = database.doc(`socialActivities/${activityId}`);
      const activity = await transaction.get(activityRef);
      const waiting = activity.exists
        ? await transaction.get(activityRef.collection("joinRequests").where("status", "==", "pending")
          .limit(SOCIAL_LIMITS.maxPendingRequestsPerActivity + 10))
        : null;
      if (activity.exists) {
        transaction.update(activityRef, { status: "removed", pendingCount: 0, updatedAt: at });
        waiting?.docs.forEach((request) => transaction.update(request.ref, { status: "closed", updatedAt: at }));
      }
    }
    if (action === "suspendUser") {
      // Shared with Kampüs Dolabı: a suspension holds in both features.
      transaction.set(database.doc(`marketUserState/${reportedUid}`), {
        suspendedUntil: Timestamp.fromMillis(now + Number(suspendDays) * DAY_MS),
      }, { merge: true });
    }
    transaction.update(reportRef, { status: "resolved", resolution: action, resolvedBy: uid, resolvedAt: at });
    audit(transaction, database, uid, `social.report.${action}`, "socialReport", reportRef.id,
      { activityId, reportedUid, ...(suspendDays ? { suspendDays } : {}) });
  });
  return { resolved: true };
}

/** Removes only the photo belonging to the user targeted by an existing report. */
export async function removeSocialReportedProfilePhotoService(
  database: Firestore,
  uid: string,
  input: { reportId?: unknown },
  deps: Pick<SocialDeps, "now"> & { photos: Pick<MarketPhotoStore, "deletePrefix"> },
): Promise<{ removed: true }> {
  if (typeof input.reportId !== "string" || !/^[A-Za-z]+_[A-Za-z0-9_]{1,300}$/.test(input.reportId)) {
    throw new HttpsError("invalid-argument", "REPORT_ID_INVALID");
  }
  const reportRef = database.doc(`socialReports/${input.reportId}`);
  const target = await database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, uid, ["good4Admin"]);
    const report = await transaction.get(reportRef);
    if (!report.exists) throw new HttpsError("not-found", "SOCIAL_REPORT_NOT_FOUND");
    if (report.get("status") !== "open") throw new HttpsError("failed-precondition", "SOCIAL_REPORT_RESOLVED");
    const reportedUid = requireId(report.get("reportedUid"), "SOCIAL_REPORTED_USER");
    const state = await transaction.get(database.doc(`socialUserState/${reportedUid}`));
    const folder = state.get("photoFolder");
    if (folder !== undefined && (typeof folder !== "string" ||
      !new RegExp(`^social-profiles/${reportedUid}/[A-Za-z0-9_-]{1,80}/$`).test(folder))) {
      throw new HttpsError("failed-precondition", "SOCIAL_PHOTO_FOLDER_INVALID");
    }
    if (!folder && (state.get("photoUrl") || state.get("photoThumbUrl"))) {
      throw new HttpsError("failed-precondition", "SOCIAL_PHOTO_FOLDER_INVALID");
    }
    return { reportedUid, folder: typeof folder === "string" ? folder : null };
  });
  if (!target.folder) return { removed: true };
  // Keep the folder in Firestore until deletion succeeds, so a Storage error can be retried.
  await deps.photos.deletePrefix(target.folder);
  const changed = await database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, uid, ["good4Admin"]);
    const stateRef = database.doc(`socialUserState/${target.reportedUid}`);
    const state = await transaction.get(stateRef);
    const replaced = state.get("photoFolder") !== target.folder;
    if (!replaced) transaction.update(stateRef, {
      photoUrl: FieldValue.delete(), photoThumbUrl: FieldValue.delete(), photoFolder: FieldValue.delete(),
    });
    audit(transaction, database, uid, "social.profile.photoRemoved", "socialReport", reportRef.id,
      { reportedUid: target.reportedUid, photoFolder: target.folder, replacedDuringRemoval: replaced });
    return replaced;
  });
  if (changed) throw new HttpsError("failed-precondition", "SOCIAL_PROFILE_CHANGED");
  return { removed: true };
}

/** Lets an admin read a chat only after a participant reported it. */
export async function getSocialReportConversationService(
  database: Firestore,
  uid: string,
  input: { reportId?: unknown },
) {
  await requireAdmin(database, uid);
  if (typeof input.reportId !== "string" || !/^conversation_[A-Za-z0-9_]{1,300}$/.test(input.reportId)) {
    throw new HttpsError("invalid-argument", "REPORT_ID_INVALID");
  }
  const report = await database.doc(`socialReports/${input.reportId}`).get();
  if (!report.exists || report.get("targetType") !== "conversation") {
    throw new HttpsError("not-found", "SOCIAL_REPORT_NOT_FOUND");
  }
  const conversationRef = database.doc(`socialConversations/${report.get("targetId")}`);
  const [conversation, messages] = await Promise.all([
    conversationRef.get(),
    conversationRef.collection("messages").orderBy("createdAt", "desc").limit(100).get(),
  ]);
  await database.collection("auditLogs").add({
    action: "social.conversation.viewed", actorUid: uid, targetType: "socialReport", targetId: report.id,
    metadata: {}, createdAt: FieldValue.serverTimestamp(),
  });
  return {
    organizerUid: conversation.get("organizerUid") ?? null,
    participantUid: conversation.get("participantUid") ?? null,
    organizerName: publicName(conversation.get("organizerName")),
    participantName: publicName(conversation.get("participantName")),
    activityTitle: conversation.get("activityTitle") ?? "",
    messages: messages.docs.reverse().map((message) => ({
      senderUid: message.get("senderUid"),
      type: message.get("type"),
      text: message.get("text"),
      createdAt: iso(message.get("createdAt")),
    })),
  };
}

// ---------------------------------------------------------------------------
// Retention and account deletion
// ---------------------------------------------------------------------------

/** Deletes a chat with its messages and takes its unread messages off the badges. */
async function dropConversation(database: Firestore, conversation: DocumentSnapshot): Promise<void> {
  await database.runTransaction(async (transaction) => {
    const current = await transaction.get(conversation.ref);
    if (!current.exists) return;
    const participants = (current.get("participants") ?? []) as string[];
    const states = await Promise.all(participants.map((uid) => transaction.get(database.doc(`socialUserState/${uid}`))));
    states.forEach((state, index) => {
      const unread = Number(current.get(`unread.${participants[index]}`) ?? 0);
      if (state.exists && unread > 0) {
        transaction.update(state.ref, { unreadCount: Math.max(0, Number(state.get("unreadCount") ?? 0) - unread) });
      }
    });
    transaction.update(current.ref, { status: "deleting", unread: {} });
  });
  await database.recursiveDelete(conversation.ref);
}

/** Marks a started activity as ended and closes its waiting requests. */
async function endActivity(database: Firestore, activity: DocumentSnapshot, now: number): Promise<boolean> {
  return database.runTransaction(async (transaction) => {
    const current = await transaction.get(activity.ref);
    if (!current.exists || !LIVE_STATUSES.includes(String(current.get("status")))
      || millisOf(current.get("startsAt")) > now) return false;
    const waiting = await transaction.get(activity.ref.collection("joinRequests").where("status", "==", "pending")
      .limit(SOCIAL_LIMITS.maxPendingRequestsPerActivity + 10));
    const at = Timestamp.fromMillis(now);
    transaction.update(current.ref, { status: "ended", pendingCount: 0, updatedAt: at });
    waiting.docs.forEach((request) => transaction.update(request.ref, { status: "closed", updatedAt: at }));
    return true;
  });
}

/**
 * Daily cleanup (see SOCIAL_RETENTION_DAYS): ends started activities, then
 * deletes activities, requests and chats once the retention period has passed.
 */
export async function cleanupSocialDataService(database: Firestore, now = Date.now()) {
  const counts = { ended: 0, activities: 0, conversations: 0, reports: 0 };
  const cutoff = Timestamp.fromMillis(now - SOCIAL_RETENTION_DAYS.activity * DAY_MS);

  // Handled records leave each query, so pages are re-read until one comes back
  // short. A record that cannot be handled would repeat; the page cap stops that.
  const drain = async (query: Query, visit: (document: DocumentSnapshot) => Promise<boolean>) => {
    let handled = 0;
    for (let pageIndex = 0; pageIndex < CLEANUP_MAX_PAGES; pageIndex += 1) {
      const page = await query.limit(CLEANUP_BATCH).get();
      for (const document of page.docs) if (await visit(document)) handled += 1;
      if (page.size < CLEANUP_BATCH) break;
    }
    return handled;
  };

  counts.ended = await drain(database.collection("socialActivities").where("status", "in", LIVE_STATUSES)
    .where("startsAt", "<=", Timestamp.fromMillis(now)), (activity) => endActivity(database, activity, now));
  counts.conversations = await drain(database.collection("socialConversations")
    .where("activityStartsAt", "<", cutoff), async (conversation) => {
    await dropConversation(database, conversation);
    return true;
  });
  counts.activities = await drain(database.collection("socialActivities").where("startsAt", "<", cutoff),
    async (activity) => {
      await database.recursiveDelete(activity.ref);
      return true;
    });

  const reports = await database.collection("socialReports").where("status", "==", "resolved")
    .where("resolvedAt", "<", Timestamp.fromMillis(now - SOCIAL_RETENTION_DAYS.resolvedReport * DAY_MS))
    .limit(CLEANUP_BATCH).get();
  for (let offset = 0; offset < reports.size; offset += 400) {
    const batch = database.batch();
    reports.docs.slice(offset, offset + 400).forEach((report) => batch.delete(report.ref));
    await batch.commit();
  }
  counts.reports = reports.size;
  return counts;
}

/**
 * Removes a user's activities, requests, chats, reports and social state.
 * Called from account deletion after eraseMarketData has marked the account.
 */
export async function eraseSocialData(
  database: Firestore,
  uid: string,
  deletePrefix: (prefix: string) => Promise<void>,
  now = Date.now(),
): Promise<void> {
  const organized = await database.collection("socialActivities").where("organizerUid", "==", uid).get();
  for (const activity of organized.docs) {
    const conversations = await database.collection("socialConversations").where("activityId", "==", activity.id).get();
    for (const conversation of conversations.docs) await dropConversation(database, conversation);
    await database.recursiveDelete(activity.ref);
  }

  const requests = await database.collectionGroup("joinRequests").where("requesterUid", "==", uid).get();
  for (const request of requests.docs) {
    await database.runTransaction(async (transaction) => {
      const activityRef = request.ref.parent.parent;
      const [activity, current] = await Promise.all([
        activityRef ? transaction.get(activityRef) : Promise.resolve(null),
        transaction.get(request.ref),
      ]);
      if (!current.exists) return;
      if (activity?.exists && current.get("status") === "accepted") releasePlace(transaction, activity, now);
      if (activity?.exists && current.get("status") === "pending") {
        transaction.update(activity.ref, { pendingCount: Math.max(0, Number(activity.get("pendingCount") ?? 0) - 1) });
      }
      transaction.delete(request.ref);
    });
  }

  const conversations = await database.collection("socialConversations").where("participants", "array-contains", uid).get();
  for (const conversation of conversations.docs) await dropConversation(database, conversation);

  for (const field of ["reporterUid", "reportedUid"]) {
    const reports = await database.collection("socialReports").where(field, "==", uid).get();
    await Promise.all(reports.docs.map((report) => report.ref.delete()));
  }
  // Throws when Storage fails, so the caller retries before the state document that names the folder is gone.
  await deletePrefix(`social-profiles/${uid}/`);
  await database.doc(`socialUserState/${uid}`).delete();
}
