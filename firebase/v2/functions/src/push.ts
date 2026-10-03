import { createHash } from "node:crypto";
import { Timestamp, type Firestore, type DocumentSnapshot } from "firebase-admin/firestore";
import { getMessaging, type MulticastMessage } from "firebase-admin/messaging";
import { HttpsError } from "firebase-functions/v2/https";
import { warn } from "firebase-functions/logger";
import { requireActiveActor, requireAuthenticatedUid } from "./shared.js";

export interface PushPayload {
  title: string;
  body: string;
  data?: Record<string, string>;
}

export interface PushBatchResult {
  successCount: number;
  failureCount: number;
  responses: { success: boolean; error?: { code: string } }[];
}
export interface PushSender {
  sendEachForMulticast(message: MulticastMessage): Promise<PushBatchResult>;
}

async function boundedSend(sender: PushSender, message: MulticastMessage, timeoutMs: number): Promise<PushBatchResult> {
  let timer: ReturnType<typeof setTimeout> | undefined;
  try {
    if (timeoutMs <= 0) throw new Error("PUSH_TIMEOUT");
    return await Promise.race([
      sender.sendEachForMulticast(message),
      new Promise<PushBatchResult>((_, reject) => { timer = setTimeout(() => reject(new Error("PUSH_TIMEOUT")), timeoutMs); }),
    ]);
  } finally {
    if (timer) clearTimeout(timer);
  }
}

export const PUSH_DEVICE_MAX_AGE_MS = 30 * 24 * 60 * 60 * 1000;
const DEVICES = "pushDevices";
const PERMANENT_TOKEN_ERRORS = new Set([
  "messaging/invalid-registration-token", "messaging/registration-token-not-registered",
]);

function checkedToken(value: unknown): string {
  if (typeof value !== "string" || value.length < 20 || value.length > 4096 || /\s/.test(value)) {
    throw new HttpsError("invalid-argument", "PUSH_TOKEN_INVALID");
  }
  return value;
}

export function pushDeviceId(token: string): string {
  return createHash("sha256").update(token).digest("hex");
}

/** Private Admin SDK records; clients cannot choose the recipient UID. */
export async function registerPushDeviceService(
  database: Firestore, uid: string | undefined,
  input: { token?: unknown; platform?: unknown }, now = Date.now(),
): Promise<{ registered: true }> {
  uid = requireAuthenticatedUid(uid);
  const token = checkedToken(input.token);
  if (input.platform !== "android" && input.platform !== "ios") {
    throw new HttpsError("invalid-argument", "PUSH_PLATFORM_INVALID");
  }
  await database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, uid);
    // One owner per token. Re-registering after switching accounts replaces the
    // old owner rather than leaving this phone attached to two accounts.
    transaction.set(database.doc(`${DEVICES}/${pushDeviceId(token)}`), {
      uid, token, platform: input.platform, updatedAt: Timestamp.fromMillis(now),
    });
  });
  return { registered: true };
}

export async function unregisterPushDeviceService(
  database: Firestore, uid: string | undefined, input: { token?: unknown },
): Promise<{ unregistered: true }> {
  uid = requireAuthenticatedUid(uid);
  const ref = database.doc(`${DEVICES}/${pushDeviceId(checkedToken(input.token))}`);
  await database.runTransaction(async (transaction) => {
    const device = await transaction.get(ref);
    // A late logout from account A must never delete account B's registration.
    if (device.exists && device.get("uid") === uid) transaction.delete(ref);
  });
  return { unregistered: true };
}

async function removeUnchangedDevice(database: Firestore, original: DocumentSnapshot) {
  await database.runTransaction(async (transaction) => {
    const current = await transaction.get(original.ref);
    if (current.exists && current.updateTime?.isEqual(original.updateTime!)) {
      transaction.delete(original.ref);
    }
  });
}

/** Account erasure also removes device addresses without racing account switches. */
export async function erasePushDevices(database: Firestore, uid: string): Promise<void> {
  const page = await database.collection(DEVICES).where("uid", "==", uid).get();
  await Promise.all(page.docs.map((device) => removeUnchangedDevice(database, device)));
}

/** Failure to notify must not turn a committed message/listing into a failed request. */
export async function sendPushToUser(
  database: Firestore,
  uid: string,
  payload: PushPayload,
  messaging?: PushSender,
  now = Date.now(),
  deliveryTimeoutMs = 5_000,
): Promise<{ sent: number; failed: number }> {
  const deadline = Date.now() + deliveryTimeoutMs;
  let sent = 0;
  let failed = 0;
  let inFlight = 0;
  try {
    const user = await database.doc(`users/${uid}`).get();
    if (user.get("status") !== "active") return { sent: 0, failed: 0 };
    const devices = await database.collection(DEVICES).where("uid", "==", uid).get();
    const fresh = devices.docs.filter((device) =>
      typeof device.get("token") === "string"
      && device.get("updatedAt") instanceof Timestamp
      && device.get("updatedAt").toMillis() > now - PUSH_DEVICE_MAX_AGE_MS);
    const stale = devices.docs.filter((device) => !fresh.includes(device));
    await Promise.all(stale.map((device) => removeUnchangedDevice(database, device)));
    if (!fresh.length) return { sent: 0, failed: 0 };

    const sender = messaging ?? getMessaging();
    for (let offset = 0; offset < fresh.length; offset += 500) {
      const batch = fresh.slice(offset, offset + 500);
      inFlight = batch.length;
      const result = await boundedSend(sender, {
        tokens: batch.map((device) => String(device.get("token"))),
        // Keep lock-screen text free of private message content, including if
        // an offline logout has not yet removed its server registration.
        notification: {
          title: "Kampüs Dolabı",
          body: payload.data?.type === "market_message"
            ? "Yeni bir mesajın veya teklifin var."
            : payload.title.slice(0, 100),
        },
        data: { ...payload.data, recipientUid: uid },
        android: {
          priority: "high", ttl: 60 * 60 * 1000,
          notification: { channelId: "campus_closet", sound: "default", icon: "ic_campus_notification" },
        },
        apns: {
          headers: { "apns-push-type": "alert", "apns-priority": "10", "apns-expiration": String(Math.floor(now / 1000) + 3600) },
          payload: { aps: { sound: "default" } },
        },
      }, deadline - Date.now());
      sent += result.successCount;
      failed += result.failureCount;
      inFlight = 0;
      await Promise.all(result.responses.map(async (response, index) => {
        const device = batch[index];
        if (device && response.error && PERMANENT_TOKEN_ERRORS.has(response.error.code)) {
          await removeUnchangedDevice(database, device);
        }
      }));
    }
    if (failed) warn("Campus push delivery failed", { failed, sent });
    return { sent, failed };
  } catch {
    // Never log tokens, payloads or raw provider errors.
    warn("Campus push delivery unavailable");
    return { sent, failed: failed + Math.max(1, inFlight) };
  }
}
