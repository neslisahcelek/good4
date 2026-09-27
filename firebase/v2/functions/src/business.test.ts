import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";
import test, { after } from "node:test";
import { getApps, initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { getBusinessContextService } from "./business.js";

process.env.GCLOUD_PROJECT = process.env.GCLOUD_PROJECT ?? "demo-good4-v2";
process.env.FIRESTORE_EMULATOR_HOST = process.env.FIRESTORE_EMULATOR_HOST ?? "127.0.0.1:8285";

const app = getApps()[0] ?? initializeApp({ projectId: process.env.GCLOUD_PROJECT });
const database = getFirestore(app);

after(async () => {
  await database.terminate();
});

test("business staff receives only their active business context", async () => {
  const suffix = randomUUID();
  const uid = `staff-${suffix}`;
  const organizationId = `business-${suffix}`;

  await database.doc(`users/${uid}`).set({
    role: "businessStaff",
    status: "active",
  });
  await database.doc(`organizations/${organizationId}`).set({
    name: "Good4 Test İşletmesi",
    type: "business",
    status: "active",
  });
  await database.doc(`organizations/${organizationId}/members/${uid}`).set({
    userId: uid,
    role: "staff",
    status: "active",
  });

  const result = await getBusinessContextService(database, uid);
  assert.deepEqual(result, {
    organizationId,
    organizationName: "Good4 Test İşletmesi",
    membershipRole: "staff",
  });
});

test("users without an active business membership are rejected", async () => {
  const uid = `staff-${randomUUID()}`;
  await database.doc(`users/${uid}`).set({
    role: "businessStaff",
    status: "active",
  });

  await assert.rejects(
    () => getBusinessContextService(database, uid),
    (error: unknown) => (
      error instanceof Error && error.message === "BUSINESS_MEMBERSHIP_NOT_FOUND"
    ),
  );
});
