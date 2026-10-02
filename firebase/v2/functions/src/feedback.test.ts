import assert from "node:assert/strict";
import { after, beforeEach, test } from "node:test";
import { submitFeedbackService } from "./feedback.js";
import { db, legacyTestDb } from "./firebase.js";

beforeEach(async () => {
  await Promise.all([
    db.recursiveDelete(db.collection("users")),
    db.recursiveDelete(db.collection("feedbackSubmissions")),
    db.recursiveDelete(db.collection("rateLimits")),
  ]);
});

after(async () => {
  await Promise.all([db.terminate(), legacyTestDb.terminate()]);
});

test("active user can submit validated feedback", async () => {
  await db.doc("users/student-1").set({
    role: "student",
    status: "active",
    email: "student@example.com",
    displayName: "Demo Öğrenci",
  });
  const result = await submitFeedbackService(db, "student-1", {
    subject: "  Ana sayfa önerisi  ",
    message: "  Kartların sıralaması daha anlaşılır olabilir.  ",
  });
  const stored = await db.doc(`feedbackSubmissions/${result.feedbackId}`).get();
  assert.equal(stored.get("subject"), "Ana sayfa önerisi");
  assert.equal(stored.get("message"), "Kartların sıralaması daha anlaşılır olabilir.");
  assert.equal(stored.get("userId"), "student-1");
  assert.equal(stored.get("status"), "new");
});

test("invalid or inactive feedback submissions are rejected", async () => {
  await db.doc("users/student-1").set({ role: "student", status: "disabled" });
  await assert.rejects(() => submitFeedbackService(db, "student-1", {
    subject: "Öneri",
    message: "Bu yeterince uzun bir geri bildirimdir.",
  }));
  await assert.rejects(() => submitFeedbackService(db, "student-1", {
    subject: "x",
    message: "Bu yeterince uzun bir geri bildirimdir.",
  }));
});

test("feedback submissions are rate limited", async () => {
  await db.doc("users/student-1").set({
    role: "student",
    status: "active",
    email: "student@example.com",
    displayName: "Demo Öğrenci",
  });

  const now = 5_000_000;
  // 1st submit
  await submitFeedbackService(db, "student-1", {
    subject: "Geri Bildirim 1",
    message: "Bu geçerli bir geri bildirim mesajıdır.",
  }, now);

  // 2nd submit
  await submitFeedbackService(db, "student-1", {
    subject: "Geri Bildirim 2",
    message: "Bu da geçerli bir geri bildirim mesajıdır.",
  }, now + 1000);

  // 3rd submit in same minute -> must fail with FEEDBACK_RATE_LIMIT_EXCEEDED
  await assert.rejects(
    () => submitFeedbackService(db, "student-1", {
      subject: "Geri Bildirim 3",
      message: "Bu üçüncü geri bildirim mesajıdır.",
    }, now + 2000),
    (err: any) => {
      assert.equal(err.code, "resource-exhausted");
      assert.equal(err.message, "FEEDBACK_RATE_LIMIT_EXCEEDED");
      return true;
    }
  );
});

test("hourly feedback limit survives minute resets and rolls back rejected writes", async () => {
  await db.doc("users/student-1").set({ role: "student", status: "active" });
  const input = { subject: "Geri bildirim", message: "Bu geçerli bir geri bildirim mesajıdır." };
  const now = 10_000_000;
  for (let index = 0; index < 5; index += 1) {
    await submitFeedbackService(db, "student-1", input, now + index * 61_000);
  }
  const limitsBefore = (await db.collection("rateLimits").get()).docs.map((doc) => doc.data());
  await assert.rejects(() => submitFeedbackService(db, "student-1", input, now + 5 * 61_000),
    (error: unknown) => error instanceof Error && error.message === "FEEDBACK_RATE_LIMIT_EXCEEDED");
  assert.deepEqual((await db.collection("rateLimits").get()).docs.map((doc) => doc.data()), limitsBefore);
  assert.equal((await db.collection("feedbackSubmissions").get()).size, 5);
  await submitFeedbackService(db, "student-1", input, now + 3_601_000);
  assert.equal((await db.collection("feedbackSubmissions").get()).size, 6);
});
