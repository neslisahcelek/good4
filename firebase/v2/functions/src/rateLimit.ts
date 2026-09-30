import { createHash } from "node:crypto";
import {
  type Firestore,
  type Transaction,
  Timestamp,
} from "firebase-admin/firestore";
import { HttpsError } from "firebase-functions/v2/https";

export interface RateLimitOptions {
  key: string;
  limit: number;
  windowSeconds: number;
  errorMessage?: string;
}

export interface FailedAttemptOptions {
  key: string;
  maxAttempts: number;
  windowSeconds: number;
  blockDurationSeconds: number;
  errorMessage?: string;
}

function rateLimitDocPath(key: string): string {
  const hash = createHash("sha256").update(key).digest("hex");
  return `rateLimits/${hash}`;
}

/**
 * Checks and updates rate limits using a sliding/fixed window in a transaction.
 * Throws HttpsError('resource-exhausted') if the limit is exceeded.
 */
export async function assertRateLimit(
  database: Firestore,
  transaction: Transaction,
  options: RateLimitOptions,
  nowMillis = Date.now(),
): Promise<{ remaining: number; resetAtMillis: number }> {
  const { key, limit, windowSeconds, errorMessage = "RATE_LIMIT_EXCEEDED" } = options;
  const docRef = database.doc(rateLimitDocPath(key));
  const snapshot = await transaction.get(docRef);

  const windowMillis = windowSeconds * 1000;
  const data = snapshot.data();

  if (!snapshot.exists || !data || !data.resetAt) {
    const resetAtMillis = nowMillis + windowMillis;
    transaction.set(docRef, {
      key,
      count: 1,
      resetAt: Timestamp.fromMillis(resetAtMillis),
      updatedAt: Timestamp.fromMillis(nowMillis),
    });
    return { remaining: Math.max(0, limit - 1), resetAtMillis };
  }

  const resetAtMillis = (data.resetAt as Timestamp).toMillis();

  if (nowMillis >= resetAtMillis) {
    const newResetAtMillis = nowMillis + windowMillis;
    transaction.set(docRef, {
      key,
      count: 1,
      resetAt: Timestamp.fromMillis(newResetAtMillis),
      updatedAt: Timestamp.fromMillis(nowMillis),
    });
    return { remaining: Math.max(0, limit - 1), resetAtMillis: newResetAtMillis };
  }

  const currentCount = typeof data.count === "number" ? data.count : 0;
  if (currentCount >= limit) {
    const retryAfterSeconds = Math.max(1, Math.ceil((resetAtMillis - nowMillis) / 1000));
    throw new HttpsError("resource-exhausted", errorMessage, {
      retryAfterSeconds,
    });
  }

  const newCount = currentCount + 1;
  transaction.update(docRef, {
    count: newCount,
    updatedAt: Timestamp.fromMillis(nowMillis),
  });

  return { remaining: Math.max(0, limit - newCount), resetAtMillis };
}

/**
 * Checks if the actor is currently blocked due to repeated failures.
 * Throws HttpsError('resource-exhausted') if blocked.
 */
export async function assertNotBlocked(
  database: Firestore,
  transaction: Transaction,
  key: string,
  nowMillis = Date.now(),
): Promise<void> {
  const docRef = database.doc(rateLimitDocPath(key));
  const snapshot = await transaction.get(docRef);
  if (!snapshot.exists) return;

  const data = snapshot.data();
  if (data?.blockedUntil instanceof Timestamp) {
    const blockedUntilMillis = data.blockedUntil.toMillis();
    if (nowMillis < blockedUntilMillis) {
      const retryAfterSeconds = Math.max(1, Math.ceil((blockedUntilMillis - nowMillis) / 1000));
      throw new HttpsError("resource-exhausted", "TOO_MANY_FAILED_ATTEMPTS", {
        retryAfterSeconds,
      });
    }
  }
}

/**
 * Records a failed attempt (e.g. incorrect verification code) and blocks further attempts
 * if the failure threshold is exceeded.
 */
export async function recordFailedAttempt(
  database: Firestore,
  transaction: Transaction,
  options: FailedAttemptOptions,
  nowMillis = Date.now(),
): Promise<{ attempts: number; blocked: boolean }> {
  const {
    key,
    maxAttempts,
    windowSeconds,
    blockDurationSeconds,
    errorMessage = "TOO_MANY_FAILED_ATTEMPTS",
  } = options;

  const docRef = database.doc(rateLimitDocPath(key));
  const snapshot = await transaction.get(docRef);
  const data = snapshot.data();

  const windowMillis = windowSeconds * 1000;
  const blockMillis = blockDurationSeconds * 1000;

  let currentAttempts = 0;
  let resetAtMillis = nowMillis + windowMillis;

  if (snapshot.exists && data) {
    const storedResetAt = (data.resetAt as Timestamp | undefined)?.toMillis();
    if (storedResetAt && nowMillis < storedResetAt) {
      currentAttempts = typeof data.attempts === "number" ? data.attempts : 0;
      resetAtMillis = storedResetAt;
    }
  }

  const nextAttempts = currentAttempts + 1;
  const shouldBlock = nextAttempts >= maxAttempts;

  const payload: Record<string, unknown> = {
    key,
    attempts: nextAttempts,
    resetAt: Timestamp.fromMillis(shouldBlock ? nowMillis + blockMillis : resetAtMillis),
    updatedAt: Timestamp.fromMillis(nowMillis),
  };

  if (shouldBlock) {
    payload.blockedUntil = Timestamp.fromMillis(nowMillis + blockMillis);
  }

  transaction.set(docRef, payload, { merge: true });

  if (shouldBlock) {
    throw new HttpsError("resource-exhausted", errorMessage, {
      retryAfterSeconds: blockDurationSeconds,
    });
  }

  return { attempts: nextAttempts, blocked: false };
}

/**
 * Clears failed attempts upon a successful operation.
 */
export async function clearFailedAttempts(
  database: Firestore,
  transaction: Transaction,
  key: string,
): Promise<void> {
  const docRef = database.doc(rateLimitDocPath(key));
  transaction.delete(docRef);
}
