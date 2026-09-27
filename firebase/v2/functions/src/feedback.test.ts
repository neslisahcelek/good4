import assert from "node:assert/strict";
import { after, beforeEach, test } from "node:test";
import { submitFeedbackService } from "./feedback.js";
import { db, legacyTestDb } from "./firebase.js";

beforeEach(async () => {
  await Promise.all([
    db.recursiveDelete(db.collection("users")),
    db.recursiveDelete(db.collection("feedbackSubmissions")),
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
