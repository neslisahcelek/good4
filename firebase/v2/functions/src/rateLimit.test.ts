import assert from "node:assert/strict";
import { after, beforeEach, test } from "node:test";
import {
  assertNotBlocked,
  assertRateLimit,
  clearFailedAttempts,
  recordFailedAttempt,
} from "./rateLimit.js";
import { db, legacyTestDb } from "./firebase.js";

beforeEach(async () => {
  await db.recursiveDelete(db.collection("rateLimits"));
});

after(async () => {
  await Promise.all([db.terminate(), legacyTestDb.terminate()]);
});

test("assertRateLimit allows operations within threshold and blocks excess", async () => {
  const now = 1_000_000;
  const key = "test:user1";

  // 1st attempt: OK
  await db.runTransaction(async (transaction) => {
    const res = await assertRateLimit(db, transaction, { key, limit: 2, windowSeconds: 60 }, now);
    assert.equal(res.remaining, 1);
  });

  // 2nd attempt: OK
  await db.runTransaction(async (transaction) => {
    const res = await assertRateLimit(db, transaction, { key, limit: 2, windowSeconds: 60 }, now + 1000);
    assert.equal(res.remaining, 0);
  });

  // 3rd attempt: Exceeded
  await assert.rejects(
    async () => {
      await db.runTransaction(async (transaction) => {
        await assertRateLimit(db, transaction, { key, limit: 2, windowSeconds: 60 }, now + 2000);
      });
    },
    (err: any) => {
      assert.equal(err.code, "resource-exhausted");
      return true;
    }
  );

  // After window expires: Allowed again
  await db.runTransaction(async (transaction) => {
    const res = await assertRateLimit(db, transaction, { key, limit: 2, windowSeconds: 60 }, now + 65_000);
    assert.equal(res.remaining, 1);
  });
});

test("recordFailedAttempt blocks after threshold and assertNotBlocked enforces cooldown", async () => {
  const now = 2_000_000;
  const key = "failed:user2";

  // 1st failed attempt
  await db.runTransaction(async (transaction) => {
    const res = await recordFailedAttempt(db, transaction, {
      key,
      maxAttempts: 2,
      windowSeconds: 60,
      blockDurationSeconds: 300,
    }, now);
    assert.equal(res.attempts, 1);
    assert.equal(res.blocked, false);
  });

  // Still not blocked
  await db.runTransaction(async (transaction) => {
    await assertNotBlocked(db, transaction, key, now + 1000);
  });

  // 2nd failed attempt -> triggers block
  await assert.rejects(
    async () => {
      await db.runTransaction(async (transaction) => {
        await recordFailedAttempt(db, transaction, {
          key,
          maxAttempts: 2,
          windowSeconds: 60,
          blockDurationSeconds: 300,
        }, now + 2000);
      });
    },
    (err: any) => {
      assert.equal(err.code, "resource-exhausted");
      return true;
    }
  );

  // assertNotBlocked should now reject
  await assert.rejects(
    async () => {
      await db.runTransaction(async (transaction) => {
        await assertNotBlocked(db, transaction, key, now + 3000);
      });
    },
    (err: any) => {
      assert.equal(err.code, "resource-exhausted");
      return true;
    }
  );

  // clearFailedAttempts resets the block
  await db.runTransaction(async (transaction) => {
    await clearFailedAttempts(db, transaction, key);
  });

  // assertNotBlocked should pass now
  await db.runTransaction(async (transaction) => {
    await assertNotBlocked(db, transaction, key, now + 4000);
  });
});
